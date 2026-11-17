# Výkonnostní testy

[k6](https://k6.io) (AGPL-3.0, bez placené části pro lokální běh). Skript je obyčejný JavaScript,
běží v kontejneru, takže na stroji nemusí být nic nainstalované.

## Spuštění

Proti běžícímu stacku z `infra/`:

```bash
docker run --rm --network mish_default -v "$PWD/perf":/perf -v "$PWD/test-results":/test-results -e BASE_URL=https://mish grafana/k6 run --insecure-skip-tls-verify /perf/smoke.js
```

Report se zapíše do `test-results/perf-report.md`.

Parametry se předávají přes prostředí:

| Proměnná | Výchozí | Význam |
|---|---|---|
| `BASE_URL` | `https://mish` | Adresa brány |
| `VUS` | `20` | Počet souběžných uživatelů ve špičce |
| `RAMP_UP` / `HOLD` | `20s` / `40s` | Náběh a výdrž |
| `REALM` | `mock-realm` | Realm Keycloaku pro kontrolu dostupnosti |

## Co se měří a proč zrovna to

Aplikace je Vaadin — přihlášená session je stavová výměna přes websocket, ne posloupnost REST volání.
Test, který by „se přihlásil", by měřil hlavně vlastní simulaci. Měří se proto to, co skutečně
rozhoduje, jestli posluchárna plná studentů otevře kapitolu naráz:

| Scénář | Rozpočet p95 |
|---|---|
| Načtení aplikace (`/`) | 1 500 ms |
| Statický soubor | 800 ms |
| Všechny požadavky | 2 000 ms |
| Chybovost | < 1 % |

Rozpočty jsou záměrně volné. Mají zachytit regresi jiného řádu, ne pár milisekund šumu — k6 skončí
nenulovým kódem, když se překročí, takže se dají zapojit do CI jako každý jiný test.

Stahování modelů (`/api/model/download/{id}`) vyžaduje přihlášenou session, takže není součástí
tohoto profilu. Velikost přenášených dat je vidět z `docs/user-testing-findings.md` — jeden model
s texturami má kolem 3,5 MB a je to největší jednotlivá položka, kterou aplikace posílá.

## Škálování

Výsledky dávají smysl číst spolu s [`../k8s/README.md`](../k8s/README.md): pokud p95 načtení roste
s počtem uživatelů, přidání repliky pomůže, protože stav sezení drží každá instance sama a brána
posílá uživatele konzistentně na tutéž.
