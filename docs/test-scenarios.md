# Testovací scénáře

Katalog toho, co se ověřuje, s odkazem na místo, kde je to automatizované. Scénář má stabilní
identifikátor, takže se na něj dá odkázat z obhajoby i z hlášení chyby, i když se soubor přejmenuje.

## Úrovně

| Úroveň | Nástroj | Kde | Kolik |
|---|---|---|---|
| Jednotkové a komponentové (Java) | JUnit 5, Mockito, Karibu | `src/test/java` | 940 |
| Jednotkové (TypeScript, 3D vrstva) | Vitest | `src/main/frontend/js/**/*.test.ts` | 78 |
| End-to-end | Playwright, projekt `e2e` | `e2e/*.spec.ts` | 14 |
| Přístupnost (axe) | Playwright, projekt `a11y` | `e2e/axe.spec.ts` | 10 rout × 2 režimy |
| Použitelnost (měření) | Playwright, projekt `usability` | `e2e/usability.spec.ts` | 8 úloh |
| Výkon (aplikace) | k6 | `perf/smoke.js` | 3 profily |
| Výkon (3D scéna) | Playwright, projekt `perf` | `e2e/*perf*.spec.ts` | 5 |
| Snímky obrazovek | Playwright, projekt `shots` | `e2e/screenshots.spec.ts` | 5 obrazovek × 3 šířky |

Projekty `a11y`, `usability`, `perf` a `shots` se pouštějí samostatně (`npx playwright test
--project=…`). `a11y` proto, že axe hodnotí i vnitřek Vaadin komponent, takže jeho nález nemusí být
vždy náš a neměl by zakrývat funkční regresi; `shots` proto, že zapisuje soubory do repozitáře, což
běžný běh testů dělat nemá — je proto navíc podmíněný proměnnou `E2E_SHOTS=1`, protože Playwright
nemá jak projekt z výchozího běhu vynechat.

## E2E scénáře

| ID | Scénář | Automatizace | Předpoklady |
|---|---|---|---|
| E-AUTH-1 | Přihlášení, hlídání rout podle role, cookies a přepnutí motivu | `auth-role-guard.spec.ts` | Účty `alice`, `bart` |
| E-CHAP-1 | Vyučující vytvoří, zobrazí, najde a smaže kapitolu | `chapter-crud.spec.ts` | Fixture model |
| E-CHAP-2 | Import kapitoly ze ZIP do editoru | `chapter-import.spec.ts` | — |
| E-MOD-1 | Vyučující nahraje, zobrazí, najde a smaže model | `model-crud.spec.ts` | — |
| E-MOD-2 | Model zůstává ve scéně při odebrání textur a CSV, zmizí až s vlastním souborem | `model-assets-visibility.spec.ts` | — |
| E-QUIZ-1 | Vyučující vytvoří kvíz, projde jej, najde a smaže | `quiz-crud-execute.spec.ts` | Fixture kapitola |
| E-STUD-1 | Student vypíše, zobrazí a odehraje obsah | `student-access.spec.ts` | Fixture kapitola a kvíz |
| E-STUD-2 | Student se nedostane na routy vyučujícího | `student-no-teacher-routes.spec.ts` | — |
| E-PUB-1 | Veřejné routy jsou dostupné bez role vyučujícího | `public-routes.spec.ts` | — |
| E-A11Y-1 | Každá routa má jazyk, jeden `<main>` a pojmenovanou navigaci | `accessibility.spec.ts` | — |
| E-A11Y-2 | Skip link je první a vede na obsah | `accessibility.spec.ts` | — |
| E-A11Y-3 | Žádná routa neroluje stránku samotnou, ve třech šířkách | `accessibility.spec.ts` | — |
| E-A11Y-4 | Ukázky na úvodní stránce se stahují až při doscrollování a lze je zastavit | `accessibility.spec.ts` | — |
| E-A11Y-5 | 3D scéna je dosažitelná a ovladatelná klávesnicí | `accessibility.spec.ts` | Fixture kapitola |
| E-A11Y-6 | Ovládání modelu má jména, velikost cílů a funguje z klávesnice | `accessibility.spec.ts` | Fixture model |
| E-A11Y-7 | Lišta cookies nabízí odmítnutí stejně zřetelně jako přijetí | `accessibility.spec.ts` | — |

Fixtures zakládá `e2e/fixtures.setup.ts` (projekt `setup`), idempotentně — opakovaný běh je
přeskočí. Sada běží na čtyřech workerech; každý scénář si své entity pojmenovává jednoznačně
a fixtures vybírá jménem, takže si scénáře navzájem nemažou data.

## Úlohy uživatelského testování

Měří cenu úlohy v čase a počtu interakcí, ne správnost. Plán a metodika jsou v
[user-testing-plan.md](user-testing-plan.md), naměřené hodnoty a nálezy v
[user-testing-findings.md](user-testing-findings.md).

| ID | Role | Cíl |
|---|---|---|
| T1 | vyučující | Nahrát 3D model s texturami a CSV |
| T2 | vyučující | Vytvořit kapitolu s modelem |
| T4 | vyučující | Najít výsledky studentů |
| T5 | vyučující | Najít kapitolu ve výpisu |
| S1 | student | Najít a otevřít kapitolu |
| S2 | student | Zobrazit 3D model kapitoly |
| S3 | student | Najít seznam kvízů |
| S4 | student | Najít vlastní výsledky |

## Co zatím není pokryté automaticky

| Oblast | Proč a čím se ověřuje |
|---|---|
| Průchod odečítačem obrazovky | NVDA / VoiceOver ručně; automat pozná chybějící popisek, ne nesrozumitelný |
| Ovládání pouze klávesnicí | Ručně, každá routa |
| Přiblížení na 400 % | Ručně; automat neposoudí překrytý obsah |
| Chování na skutečném dotykovém zařízení | Ručně |
| Obnova ze zálohy | Ručně podle [deployment.md](deployment.md) |

---

# Kde vést scénáře a uchovávat výsledky

Zadání znělo najít řešení pod svobodnou licencí (Apache 2.0 / MIT), které není placené. Stav
k červenci 2026:

| Nástroj | Licence | Co umí | Poznámka |
|---|---|---|---|
| **Allure Report** | Apache 2.0 | Výsledky, historie, trendy, přílohy | Generuje statický web, žádný server. Reportéry pro Playwright i JUnit 5. Historie mezi běhy z uložené složky. |
| **ReportPortal** | Apache 2.0 | Výsledky, analýza selhání, dashboardy | Plnohodnotný server. Vyžaduje PostgreSQL, RabbitMQ a OpenSearch — na tento rozsah nepoměrně těžké. |
| **Kiwi TCMS** | **GPL-2.0** | Správa scénářů i výsledků, ruční i automatizované | Nejbližší „klasickému“ TCMS. Licence ale nesplňuje zadání. |
| **TestLink** | GPL-2.0 | Správa scénářů | Nesplňuje licenci; vývoj v podstatě stojí. |
| Qase, TestRail, Xray, Zephyr, Testomat.io | proprietární | — | Placené, byť s bezplatnou hladinou. |
| Drobné projekty z GitHub topicu `test-management` | různé | — | Většinou desítky hvězd a jeden autor; pro dlouhodobé vedení scénářů riziko. |

## Rozhodnutí

**Scénáře zůstávají v repozitáři** v tomto souboru. Důvod je prostý: scénář popisuje totéž chování,
které vynucuje testovací kód, a když obojí žije v jednom commitu, nemůže se rozejít. Externí nástroj
by zavedl druhý zdroj pravdy, který by musel někdo ručně srovnávat.

**Výsledky se ukládají ve strojově čitelném formátu**, aby se daly kdykoli načíst do libovolného
nástroje bez přepisování testů:

- `test-results/results.xml` — JUnit XML z Playwrightu; formát, kterému rozumí Allure, ReportPortal,
  Kiwi TCMS i GitHub Actions
- `target/surefire-reports/*.xml` — totéž z Maven
- `test-results/usability-report.md` a `test-results/perf-report.md` — měření
- Playwright HTML report — pro prohlížení konkrétního selhání

CI je publikuje jako artefakty každého běhu.

**Kdyby přibyla potřeba historie a trendů** (například pro doložení, že se stabilita v čase
zlepšovala), je nejlevnějším krokem Allure Report: přidá se reportér, výsledky se generují do
statického webu a publikují na GitHub Pages. Nepotřebuje server ani databázi a licenčně vyhovuje.
ReportPortal dává smysl až pro tým, který sleduje víc projektů najednou.
