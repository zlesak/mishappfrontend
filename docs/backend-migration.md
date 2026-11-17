# Převod backendu do monolitu — co a proč se změnilo

Backend z repozitáře [Foglas/mishprototype](https://github.com/Foglas/mishprototype) je převedený do
tohoto repozitáře. Aplikace je jeden proces (Vaadin UI + backend), infrastruktura je v
[`infra/`](../infra/README.md) a repozitář [MISH_SCRIPTS](https://github.com/zlesak/MISH_SCRIPTS) už
není potřeba.

Tento dokument je záznam provedeného převodu: co bylo zděděno, co se zjednodušilo a co se opravilo.

---

## 1. Výchozí stav

**Původní backend** byl samostatná služba: Kotlin, Spring Boot 3.4, ~3 000 řádek, bez testů. Data
v MongoDB (kolekce `chapters`, `quiz`, `quizValidationResult`, `models`, `files`, `fulltext` +
GridFS), Redis pro jediný účel (časovač kvízu), Keycloak pro přihlášení.

**Provoz** obstarávala sada shellových skriptů, která klonovala dva repozitáře a spouštěla sedm
kontejnerů: 3uzlový Mongo replica set s TLS, 9uzlový Redis cluster s TLS, Keycloak s Postgresem,
backend, frontend a nginx.

---

## 2. Výsledná architektura

```
prohlížeč ──HTTPS──> nginx ──┬──> aplikace (Vaadin UI + backend) ──> MongoDB + GridFS
                             └──> Keycloak ──> PostgreSQL
```

Uvnitř aplikace:

| Vrstva | Balíček | Odpovědnost |
|---|---|---|
| UI | `views`, `components` | Vaadin obrazovky |
| Služby UI | `services` | příprava dat pro obrazovky |
| Rozhraní | `api.contracts` | hranice mezi UI a backendem |
| Klienti | `api` | volají backend přímo, bez HTTP |
| Backend | `backend.service` | pravidla domény |
| Perzistence | `backend.persistence` | MongoDB a GridFS |

Jediný HTTP endpoint, který zůstal, je `/api/model/download/{id}` — Three.js si modely stahuje přímo
v prohlížeči.

---

## 3. Zjednodušení datové struktury

Doménové třídy jsou zároveň uložené dokumenty. Zanikla samostatná vrstva dokumentů i mapper mezi
nimi.

**Dědičnost nese rozdíl mezi výpisem a detailem:**

| Výpis | Detail | Detail přidává |
|---|---|---|
| `QuickChapterEntity` | `ChapterEntity` | `content` |
| `QuickModelEntity` | `ModelEntity` | soubory rozpracovaného nahrání (neukládají se) |
| `QuickQuizEntity` | `QuizEntity` | `questions`, `answers` |
| `QuickQuizResult` | `QuizValidationResult` | `questionResults` |

Výpis načítá projekci bez těžkých polí, detail celý dokument. Přístup k datům je díky tomu obyčejné
CRUD: `findById`, `save`, `deleteById`.

**Co konkrétně zmizelo oproti původnímu backendu:**

| Zrušeno | Náhrada |
|---|---|
| Kolekce `files` + `$graphLookup` nad grafem souborů | strom souborů je uložený přímo v modelu |
| `metadataId` duplikující `id` | jen `id` |
| Kopie stromu souborů modelu v každé kapitole | kapitola drží `modelIds`, modely se dohledají |
| Redis (9 uzlů, TLS, certifikáty) | kolekce s TTL indexem pro časovač kvízu |
| 3uzlový Mongo replica set s TLS | jeden Mongo v uzavřené Docker síti |
| Duplicitní pole `totalScore`/`maxScore`/`percentage` v hierarchii výsledků | jen v základní třídě |
| Dopočítávání textur při každém čtení modelu (včetně stahování CSV) | spočítá se jednou při nahrání |

Textury se počítají při nahrání, takže zobrazení modelu je jeden dotaz místo `$graphLookup`
následovaného stahováním CSV souborů.

---

## 4. Opravy proti původnímu chování

| # | Původní chování | Nyní |
|---|---|---|
| 1 | Vyhodnocení „více správných odpovědí" záviselo na pořadí zaškrtnutí | porovnání jako množiny |
| 2 | U otevřené odpovědi se převáděla na malá písmena jen odpověď studenta | normalizuje se obojí (trim + malá písmena) |
| 3 | Odpověď na odevzdání kvízu neobsahovala `maxScore` | obsahuje |
| 4 | Nezodpovězené otázky ve výsledku chyběly | jsou uvedené jako nesprávné |
| 5 | `GET /quiz/{id}/all` vracel správné odpovědi i studentům | odpovědi jsou vynechané, pokud volající není oprávněn |
| 6 | Úprava modelu byla chráněná studentskou rolí | vyžaduje `CREATE_CHAPTER` |
| 7 | Výsledky kvízů nebyly omezené na uživatele | student vidí své, vyučující všechny |
| 8 | Časovač kvízu bez TTL a bez ID kvízu (spuštění druhého kvízu přepsalo první) | klíč `userId:quizId`, TTL index, atomické odebrání |
| 9 | Mazání souboru porovnávalo `_id` jako řetězec proti ObjectId — nemazalo | maže se přes repozitář |
| 10 | Rekurzivní nahrávání bez detekce cyklů | detekce cyklu i limit hloubky |
| 11 | Smazání kapitoly nechávalo záznam ve fulltextu | maže se spolu s kapitolou |
| 12 | `CurrentUser.hasRole` porovnával bez prefixu `ROLE_` (vždy selhal) | nepřevedeno, používají se autority Springu |
| 13 | Chybová hláška vracela celou entitu včetně odpovědí | vrací jen text chyby |
| 14 | Výpis modelů počítal celkový počet bez filtru | počítá se stejným dotazem |
| 15 | Model šlo smazat, i když jej používala kapitola | odmítnuto se jmény kapitol |
| 16 | Hledání kapitol porovnávalo jen název | hledá i v obsahu (fulltext) |

Oprávnění se nově vynucují i v backendu (`@PreAuthorize`), ne pouze v UI.

---

## 5. Infrastruktura

- Celý stack je v `infra/docker-compose.yml`: aplikace, MongoDB, Keycloak, PostgreSQL, nginx.
- **Žádná přihlašovací hodnota nemá výchozí nastavení.** Chybějící proměnná stack nespustí a je
  pojmenovaná v chybě; totéž platí pro aplikaci.
- Šablony prostředí: [`.env.example`](../infra/.env.example) pro vývoj,
  [`.env.production.example`](../infra/.env.production.example) pro produkci (bez použitelných
  hodnot).
- Brána posílá HSTS, `X-Content-Type-Options`, `Referrer-Policy` a `frame-ancestors`; ven jsou
  publikované jen porty 80 a 443.
- Postup nasazení včetně Raspberry Pi: [deployment.md](deployment.md).

---

## 6. Ověření

| Co | Jak | Stav |
|---|---|---|
| Jednotkové a integrační testy | `./mvnw test` | 859 testů, prochází |
| End-to-end scénáře | `npx playwright test` proti běžícímu stacku | prochází, kromě nestabilního `model-assets-visibility` |
| Uživatelské úlohy | `npx playwright test e2e/usability.spec.ts` | 8 z 8 úloh dokončeno |

Test `model-assets-visibility` je nestabilní: po odebrání souboru modelu občas zůstane model ve 3D
scéně. Ve čtyřech bězích třikrát selhal a jednou prošel. Jde o úklid scény v prohlížeči, chování je
starší než tento převod. Podrobnosti v [user-testing-findings.md](user-testing-findings.md), nález 4.

Testovací sada si potřebná data vytváří sama (`e2e/fixtures.setup.ts`), takže běží i nad prázdnou
databází.

---

## 7. Co zbývá

1. Opravit úklid 3D scény (nález 4 v [user-testing-findings.md](user-testing-findings.md)).
2. Provést sezení s uživateli podle [user-testing-plan.md](user-testing-plan.md).
3. Zvážit upgrade Keycloaku z 21.1.1 (ponechán kvůli kompatibilitě importu realmu).
4. Před ostrým provozem projít kontrolní seznam v [deployment.md](deployment.md).

Původní data z běžícího nasazení nejsou s novým schématem přímo kompatibilní (kapitoly nesou
`modelIds` místo kopie stromu souborů, modely nesou vlastní strom místo kolekce `files`). Pokud je
potřeba je zachovat, je nutné napsat převodní skript; pro nové nasazení to neplatí.

Takový skript musí ošetřit i kvízy uložené touto aplikací před sjednocením konvencí v odpovědích:

| Pole | Dřív | Teď |
|---|---|---|
| `correctItems` u výběru z více možností | 1-based | 0-based |
| `correctMatches` u spojovačky | `{pravý → levý}` | `{levý → pravý}`, bijekce přes celý levý sloupec |
| `acceptableAnswers` u otevřené odpovědi | uložené malými písmeny | zachovaná velikost písmen autora |

Bez převodu se starý výběr z více možností hodnotí o jedna vedle, staré spojovačky nelze zodpovědět
správně a otázky s `exactMatch` neuznají ani původní správnou odpověď.
