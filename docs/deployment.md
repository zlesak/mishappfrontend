# Nasazení MISH APP

Aplikace je jeden kontejner (`app`) doplněný o databázi, Keycloak a bránu. Celý stack se spouští
z adresáře [`infra/`](../infra/README.md).

---

## 1. Dá se nasazovat rovnou z GitHubu?

Ano, a repozitář je na to už připravený ve dvou úrovních.

### a) Sestavení image v GitHub Actions

Workflow [`docker-image.yml`](../.github/workflows/docker-image.yml) sestaví image a publikuje jej do
GitHub Container Registry (`ghcr.io/zlesak/mishappfrontend`). `Dockerfile` je vícefázový, takže build
nepotřebuje nic předpřipraveného — stačí `docker build .`.

Server pak nasazuje **hotový image**, nikoli zdrojový kód:

```bash
docker compose -f infra/docker-compose.yml pull app && docker compose -f infra/docker-compose.yml up -d
```

Aby `app` používal image z registru místo lokálního buildu, přidejte na server soubor
`infra/docker-compose.override.yml`:

```yaml
services:
  app:
    image: ghcr.io/zlesak/mishappfrontend:latest
    build: !reset null
```

### b) Automatické nasazení po pushi

Tady jsou tři reálné možnosti, seřazené podle toho, jak dobře se hodí pro server doma nebo ve škole:

| Způsob | Jak funguje | Kdy dává smysl |
|---|---|---|
| **Pull na straně serveru** (doporučeno) | Na serveru běží [Watchtower](https://containrrr.dev/watchtower/) nebo cron, který jednou za čas zkusí `docker compose pull && up -d` | Server je za NATem nebo doma — nepotřebuje veřejnou IP ani otevřený port |
| **SSH deploy z Actions** | Workflow se přes SSH připojí na server a spustí `pull && up -d` | Server má veřejnou adresu a můžete do GitHubu uložit SSH klíč |
| **Self-hosted runner** | Na serveru běží GitHub Actions runner, který nasazení provede lokálně | Chcete v Actions vidět i log nasazení a nevadí vám runner navíc |

Pro Raspberry Pi doma je jednoznačně nejjednodušší **první varianta**: nic se nemusí zvenku
otevírat.

> Pozor na architekturu: Raspberry Pi je ARM64. Buď image sestavujte multiplatformně
> (`docker/build-push-action` s `platforms: linux/amd64,linux/arm64`), nebo nechte Pi sestavit image
> lokálně přes `docker compose up -d --build`. První build na Pi trvá desítky minut.

---

## 2. Dají se na GitHubu ukládat produkční tajné klíče?

**Ano — v GitHub Actions Secrets, ale s omezeními, která je potřeba znát.**

Co GitHub nabízí:

- **Repository secrets** – šifrované, dostupné jen běžícím workflow. V logu se automaticky maskují.
- **Environment secrets** – navíc s možností vyžádat schválení před nasazením (`production`
  environment s required reviewers). Pro obhajobu je to hezky doložitelné.
- **Organization secrets** – sdílené napříč repozitáři.

Co GitHub **negarantuje** a co je proto potřeba dodržet:

1. **Kdokoli s právem pushnout workflow si secret může vypsat.** Na veřejném repozitáři proto nikdy
   nespouštějte workflow se secrety nad pull requesty z forků (`pull_request_target` je past).
2. **Secrety nepatří do souborů v repozitáři.** Proto je `infra/.env` v `.gitignore` a v repozitáři
   jsou jen šablony [`.env.example`](../infra/.env.example) (vývoj) a
   [`.env.production.example`](../infra/.env.production.example) (produkce, bez jediné použitelné
   hodnoty).
3. **Secret nejde přečíst zpět** — jde jen přepsat. Uložte si je i do správce hesel.
4. **Rotace**: po obhajobě nebo odchodu spolupracovníka klíče vyměňte. Aplikace je na to připravená,
   všechny hodnoty jsou v jednom `.env`.

Pro tento projekt doporučuji jednoduchý model: **secrety pro produkci žijí v `.env` na serveru**
(vlastník `600`), GitHub Secrets obsahují jen přístup potřebný pro CI (token do registru, případně
SSH klíč pro deploy). Aplikační hesla tak nikdy neopustí server.

Vygenerování hodnot:

```bash
openssl rand -base64 32
```

---

## 3. Dá se nasazení simulovat na Raspberry Pi?

**Ano, a je to dobrý způsob, jak ověřit produkční nasazení nanečisto.** Celý stack je běžný
`docker compose`, žádná služba nevyžaduje x86.

### Co budete potřebovat

- Raspberry Pi 4 nebo 5, ideálně **8 GB RAM** (4 GB stačí, ale Keycloak + Mongo + JVM jsou těsné)
- **64bitový** systém (Raspberry Pi OS 64-bit nebo Ubuntu Server) — 32bit nestačí, MongoDB 7 na něm
  neběží
- SSD nebo kvalitní SD kartu; MongoDB a GridFS zapisují modely v řádu stovek MB
- Docker a Docker Compose

### Postup

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER   # a odhlásit/přihlásit
```

```bash
git clone https://github.com/zlesak/mishappfrontend && cd mishappfrontend/infra
cp .env.production.example .env && chmod 600 .env   # a vyplnit hodnoty
docker compose up -d --build
```

Do `.env` nastavte `EXTERNAL_BASE_URL` na adresu, pod kterou budete Pi otevírat — buď hostname
v lokální síti (`https://mish.local`), nebo veřejnou doménu.

### Na co si dát pozor

| Věc | Poznámka |
|---|---|
| **Paměť** | JVM si vezme, co může. Přidejte `JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50"` do prostředí služby `app`. |
| **Uživatel v kontejneru** | Aplikace běží jako `mish` (uid 1001), ne jako root. Pokud do kontejneru přimountujete adresář z hostitele, musí být pro toto uid zapisovatelný. |
| **První build** | Sestavení Vaadin frontendu na Pi trvá 20–40 minut. Rychlejší je sestavit image v Actions pro `linux/arm64` a jen jej stáhnout. |
| **Certifikát** | `cert-init` vyrobí self-signed certifikát, u kterého bude prohlížeč varovat. Pro reálný provoz použijte Let's Encrypt (viz níže). |
| **Porty** | Pokud Pi visí na veřejné IP, propusťte jen 80 a 443. Mongo ani Keycloak se ven nepublikují. |
| **Zálohy** | Vše podstatné je ve volume `mongo-data`. Zálohujte jej (`docker run --rm -v mish_mongo-data:/data -v $PWD:/backup alpine tar czf /backup/mongo.tgz /data`). |
| **Obnova samotné aplikace** | `docker compose up -d --force-recreate app` nechá stack na 502. Nový kontejner dostane jinou adresu v síti, ale nginx si tu původní drží z doby, kdy startoval, a sám ji nepřeloží znovu. Restartujte proto i bránu: `docker compose restart gateway`. |

### Skutečný certifikát

Nejjednodušší varianta bez zásahu do compose: vygenerujte certifikát pomocí `certbot` na hostiteli
a nakopírujte jej do volume brány:

```bash
docker run --rm -v mish_gateway-certs:/certs -v /etc/letsencrypt/live/DOMENA:/le:ro alpine sh -c "cp /le/fullchain.pem /certs/ && cp /le/privkey.pem /certs/"
```

Potom `docker compose restart gateway`. `cert-init` vlastní certifikát nepřepíše — generuje jen
tehdy, když žádný neexistuje.

---

## Kontrolní seznam před ostrým během

- [ ] `.env` vychází z `.env.production.example` a nemá jedinou prázdnou hodnotu
- [ ] `KEYCLOAK_COMMAND` začíná na `start`, nikoli `start-dev` — dev režim vypíná kontroly, které
      produkce potřebuje, a Keycloak jej pro ostrý provoz nepodporuje
- [ ] `KEYCLOAK_COMMAND` neobsahuje `--import-realm`, jinak by se do čerstvého Keycloaku naimportovali
      demo uživatelé `alice` a `bart` se známými hesly (odstranit `infra/keycloak/realms/*.json`
      z nasazení nic nestojí a je to druhá pojistka)
- [ ] V Keycloaku vytvořen vlastní realm a *confidential* klient, jeho secret vyplněn v `.env`
- [ ] Brána má důvěryhodný certifikát
- [ ] Nastavené zálohování volume `mongo-data`
- [ ] Ověřeno přihlášení, vytvoření kapitoly, nahrání modelu a průchod kvízem
