# Logování a auditní stopa

Aplikace píše všechno na standardní výstup — nikam jinam. Sběr, rotace a uchování jsou věcí okolí
(`docker logs`, Promtail, Filebeat), takže kontejner nepotřebuje žádný zapisovatelný adresář.

## Formát

Na vývojářském stroji je výstup čitelný pro člověka. V kontejneru se přepne proměnnou
`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (nastavuje ji [`infra/docker-compose.yml`](../infra/docker-compose.yml))
na **jeden JSON objekt na řádek v Elastic Common Schema**. ECS čte jak ELK stack, tak Grafana/Loki
bez další mapovací vrstvy.

```json
{"@timestamp":"2026-07-30T22:34:36.792Z","log":{"level":"INFO","logger":"cz.uhk...ChapterBackendService"},
 "trace.id":"a3f1…","user.id":"c417f207-…","message":"…","ecs":{"version":"8.11"}}
```

Formát jde přepnout proměnnou `LOG_FORMAT` v `.env` (`ecs`, `logstash`, `gelf`, nebo prázdno pro
čitelný text). Jde o vestavěnou funkci Spring Bootu, ne o vlastní kód ani další závislost.

## Kontext požadavku

[`LoggingContextFilter`](../src/main/java/cz/uhk/zlesak/threejslearningapp/common/logging/LoggingContextFilter.java)
doplní každému požadavku dvě pole:

| Pole | Význam |
|---|---|
| `trace.id` | Spojuje všechny řádky jednoho požadavku. Přebírá se z hlavičky `X-Request-Id`, pokud dorazí a projde validací; jinak se vygeneruje. Vrací se zpět v odpovědi, takže uživatel má co nahlásit. |
| `user.id` | Kdo požadavek provedl. Identifikátor, nikdy jméno — v logu nemá být víc, než je nutné. |

Vaadin pouští čtení a zápisy na worker vláknech. MDC je thread-local, takže by tam kontext chyběl;
[`AbstractView.runAsync`](../src/main/java/cz/uhk/zlesak/threejslearningapp/views/abstractViews/AbstractView.java)
jej přenáší přes [`LogContext`](../src/main/java/cz/uhk/zlesak/threejslearningapp/common/logging/LogContext.java)
stejně jako přihlášení uživatele.

## Auditní stopa

[`AuditLog`](../src/main/java/cz/uhk/zlesak/threejslearningapp/common/logging/AuditLog.java) píše na
vlastní logger `mish.audit`, takže se dá odvést a uchovávat odděleně od diagnostiky. Každý záznam má
stejná pole:

| Pole | Hodnoty |
|---|---|
| `event.dataset` | vždy `mish.audit` |
| `event.action` | `create`, `update`, `delete`, `submit`, `download` |
| `event.outcome` | `success`, `failure`, `denied` |
| `mish.entity.type` | `chapter`, `model`, `quiz`, `quizResult` |
| `mish.entity.id`, `mish.entity.name` | co bylo cílem |
| `user.id`, `trace.id` | kdo a v jakém požadavku |

Obsah, který uživatel napsal, se neloguje — jen názvy entit. Z jednoho auditního řádku se přes
`trace.id` dá rozbalit všechno ostatní, co se v témže požadavku dělo.

### Příklady dotazů

Loki:

```logql
{container="mish-app-1"} | json | event_dataset = "mish.audit" | event_action = "delete"
```

Elasticsearch:

```text
event.dataset:"mish.audit" and event.outcome:"denied" and user.id:"c417f207-*"
```

## Úrovně

| Úroveň | Kdy |
|---|---|
| `ERROR` | Operace selhala a uživatel to poznal. Vždy s výjimkou, ne jen s jejím textem. |
| `WARN` | Aplikace pokračuje, ale něco není v pořádku — nepodařilo se smazat osiřelý soubor, nešel přečíst CSV. |
| `INFO` | Start, konfigurace a auditní události. Ne průběh běžného požadavku. |
| `DEBUG` | Vypnuto v produkci. |

Ladicí hodnoty se do zpráv nevkládají konkatenací; používá se `{}` a strukturovaná pole, aby zůstaly
dotazovatelné i po serializaci do JSON.
