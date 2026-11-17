# Plán uživatelského testování MISH APP

Cílem není jen ověřit, že aplikace funguje — to dělají automatické testy. Cílem je zjistit, **kde
uživatel zaváhá, kde udělá chybu a co ho zdrží**, a z toho odvodit konkrétní úpravy.

Testování má dvě úrovně:

1. **Automatizovaný průchod** (`e2e/usability.spec.ts`) — běží při každé změně, měří počet kroků
   a čas na úlohu a hlídá, že se scénáře nezhoršují. Popsáno v části 4.
2. **Testování s lidmi** — to, co automat nezachytí: porozumění pojmům, očekávání, frustraci.

---

## 1. Kdo testuje

| Persona | Kdo to je | Co od aplikace potřebuje |
|---|---|---|
| **Vyučující (Alice)** | Vyučující anatomie, běžná práce s PC, 3D modely zná z prohlížečů | Vytvořit kapitolu s modelem, sestavit kvíz, vidět výsledky studentů |
| **Student (Bart)** | Student 1. ročníku, mobil i notebook | Najít kapitolu, prohlédnout si model, projít kvíz, vidět svůj výsledek |
| **Nový vyučující** | Poprvé v aplikaci, bez zaškolení | Zorientovat se bez návodu |

Doporučený vzorek: **5 vyučujících a 5 studentů**. Pět uživatelů na roli odhalí zhruba 80 % problémů
použitelnosti; větší vzorek už přináší málo nového.

## 2. Úlohy

Úlohy jsou zadané jako cíl, ne jako postup — uživateli se neříká, kam kliknout.

**Vyučující**

| # | Zadání | Úspěch znamená |
|---|---|---|
| T1 | Nahrajte 3D model kosti včetně textury | Model je v seznamu modelů |
| T2 | Vytvořte kapitolu, která tento model používá | Kapitola je v seznamu a model se v ní zobrazí |
| T3 | Přidejte ke kapitole kvíz se třemi otázkami různých typů | Kvíz je uložený a spustitelný |
| T4 | Zjistěte, jak dopadli studenti | Vyučující najde výsledky kvízu |
| T5 | Opravte překlep v názvu kapitoly | Změna je uložená |

**Student**

| # | Zadání | Úspěch znamená |
|---|---|---|
| S1 | Najděte kapitolu o mozku | Otevřel detail správné kapitoly |
| S2 | Otočte modelem a najděte označenou oblast | Práce s 3D scénou |
| S3 | Projděte kvíz u kapitoly | Kvíz odevzdán |
| S4 | Zjistěte, kolik bodů jste získali | Našel svůj výsledek |

## 3. Co se měří

**Kvantitativně** (sbírá i automat):

- **Dokončení úlohy** — dokončeno / dokončeno s obtížemi / nedokončeno
- **Čas na úlohu** — od zadání po splnění
- **Počet kroků** — kolik kliknutí/obrazovek uživatel potřeboval oproti nejkratší cestě
- **Chybné kroky** — kolikrát se vydal špatnou cestou a musel se vrátit

**Kvalitativně** (jen s lidmi):

- Metoda **hlasitého přemýšlení** — uživatel průběžně komentuje, co hledá a co čeká
- Místa zaváhání delší než ~5 s
- Formulace, které uživatel používá, oproti názvosloví v aplikaci
- Po každé úloze: „Jak náročné to bylo?" na stupnici 1–7 (Single Ease Question)
- Na závěr: **SUS dotazník** (10 otázek, výsledek 0–100) pro srovnatelné číslo mezi koly testování

## 4. Automatizovaný průchod

```bash
npx playwright test e2e/usability.spec.ts
```

Spec projde stejné úlohy jako člověk, měří u každé čas a počet kroků a zapíše
`test-results/usability-report.md` plus snímek obrazovky ke každému kroku. Slouží ke třem věcem:

- **regrese použitelnosti** — když u úlohy skokově naroste počet kroků, něco se v toku rozbilo;
- **příprava na testování s lidmi** — snímky ukazují, co uživatel uvidí;
- **doklad do práce** — reprodukovatelná čísla místo dojmů.

Co automat **nezachytí**: zda uživatel rozumí pojmům („Pokročilý model"), zda dokáže cíl vůbec najít,
a jak se u toho cítí. To je důvod, proč testování s lidmi nenahrazuje.

## 5. Průběh sezení s uživatelem

Jedno sezení ~45 minut:

1. **Uvedení (5 min)** — testuje se aplikace, ne uživatel; chyby jsou cenné informace.
2. **Souhlas (2 min)** — se záznamem obrazovky a zvuku, anonymizovaně.
3. **Úvodní otázky (5 min)** — zkušenost s podobnými nástroji.
4. **Úlohy (25 min)** — bez nápovědy; při zaseknutí nad 3 minuty pomoci a poznamenat to.
5. **Závěr (8 min)** — SUS, co bylo nejhorší, co chybí.

Testovací prostředí: čerstvě spuštěný stack se seed daty (`e2e/fixtures.setup.ts`), účty `alice`
a `bart`.

## 6. Vyhodnocení

Nálezy se třídí podle závažnosti:

| Úroveň | Popis | Reakce |
|---|---|---|
| **Kritická** | Uživatel úlohu nedokončí | Opravit před nasazením |
| **Vážná** | Dokončí, ale s obtížemi nebo velkou ztrátou času | Opravit v nejbližší iteraci |
| **Drobná** | Zdrží nebo zmate, uživatel si poradí | Zařadit do backlogu |
| **Námět** | Přání nad rámec zadání | Zvážit |

Výstupem je seznam nálezů s odkazem na záznam a s návrhem úpravy. Nálezy z automatizovaného
průchodu jsou v [user-testing-findings.md](user-testing-findings.md).
