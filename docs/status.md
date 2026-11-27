# Stav prací — větev `monolith`

Poslední aktualizace: 31. 7. 2026.

Přehled toho, co je hotové a co ne, napříč všemi zadáními, která na tuto větev dopadla: přenesení
backendu do monolitu, code review, přístupnostní zadání, nálezy z automatizovaného průchodu
a následné úpravy UI.

## Ověření

| Sada | Kde | Počet | Naposledy zelená |
|---|---|---|---|
| Java (JUnit, Mockito, Karibu) | `src/test/java` | 940 | před posledními úpravami CSS a textů |
| TypeScript (Vitest) | `src/main/frontend/js/**/*.test.ts` | 78 | ano |
| E2E (Playwright, projekt `e2e`) | `e2e/*.spec.ts` | 14 | ano |
| Přístupnost (axe, projekt `a11y`) | `e2e/axe.spec.ts` | 10 rout × 2 režimy | ano |
| Použitelnost (projekt `usability`) | `e2e/usability.spec.ts` | 8 úloh | ano |
| Výkon 3D scény (projekt `perf`) | `e2e/*perf*.spec.ts` | 5 | ano |
| Výkon aplikace (k6) | `perf/smoke.js` | 3 profily | ano |

**Co není doběhnuté:** poslední běh Java sady a E2E po třech drobných úpravách — skrytí duplicitního
počtu v hlavičce výpisu, změna placeholderu hledání na příkladovou hodnotu a zalamování řádku
s výběrem modelu, textury a oblasti. Docker Desktop během posledního běhu spadl, takže verifikace
neproběhla. Před merge je nutné spustit:

```bash
mvn test
npx vitest run
npx playwright test
```

## Hotovo

### Monolit a code review

- Backend přenesen do repozitáře, datový model zjednodušen.
- Osm nálezů z code review opraveno. Nejvážnější byl mimo původní hodnocení: vyhledávání kapitol
  používalo Mongo `$text`, který matchuje **kterékoli** slovo dotazu, takže stačilo napsat „kapitola“
  a vrátil se celý seznam. Nahrazeno konjunkcí přes `Pattern.quote`.
- Smazání modelu už tiše nerozbije kapitolu; smazání kapitoly odpojí kvízy.
- Strukturované logování v ECS s korelačním ID, uživatelem a oddělenou auditní stopou
  (`mish.audit`) — čitelné Grafanou i ELK bez mapování. Viz [logging.md](logging.md).
- E2E paralelizované (6,3 min → 1,8 min na čtyřech workerech) a poprvé celá sada zelená; příčinou
  dlouhodobě náhodného pádu byl model, který po smazání zůstal v registru s načteným blobem.
- k6 výkonnostní testy, Kubernetes manifesty s HPA a sticky sessions, katalog testovacích scénářů.

### Přístupnostní zadání (WCAG 2.2 AA)

Všech osm skupin, každá jedním commitem, v pořadí, které zadání doporučovalo. Detailní rozpis
v [accessibility-progress.md](accessibility-progress.md).

- **G** — jazyk dokumentu, tři barevné tokeny pod kontrastem, indikátor zaměření, skip link
  a `<main>`, jména ikonových tlačítek, asertivní chybová oznámení, lišta cookies s rovnocenným
  odmítnutím. Vedlejší nález: `ErrorNotification(String)` volal `new` místo `this`, takže **každá
  jednoargumentová chybová hláška byla neviditelná**.
- **N** — `PageHeader`, `EntityRow`, `ListingToolbar`, `StatusBadge`.
- **L + A** — dlaždice nahrazeny řádky, H1 na každém výpisu, oznamování počtu výsledků, modely
  v kapitole jako skutečné odkazy, stránkování s `aria-current` místo `disabled`, počty v záložkách
  administrace.
- **Q** — parametry kvízu jako `<dl>`, otázka jako `<fieldset>`/`<legend>` s pořadím a body, časovač
  ve statické hlavičce vedle ukazatele postupu, odpočet po minutách, varování minutu před koncem,
  souhrn nezodpovězených otázek před odesláním, klávesová alternativa ke klikání do textury.
- **M** — hierarchie nadpisů, zastavitelné ukázky, zarovnání textu, okraj sekundárního tlačítka,
  popisky obrázků, odkaz na prohlášení o přístupnosti.
- **V** — klávesová obsluha 3D scény (`CameraActions` sdílené s ovládacím panelem), `role="application"`
  a jméno canvasu, jména a velikost cílů v panelu, popisky navázané na pole, nadpis detailu kapitoly,
  upozornění na odkazy bez popisného textu při ukládání.
- **Z-1** — axe sweep přes všech deset rout v obou barevných režimech s nulovou tolerancí pro
  `serious` a `critical`. První běh našel tři nálezy, všechny naše, všechny opravené.

### Nálezy z automatizovaného průchodu

Všech šest vyřešeno, viz [user-testing-findings.md](user-testing-findings.md).

- Jedna plocha pro přetažení všech souborů modelu najednou (bylo 21 s a 9 interakcí).
- Výsledek kvízu má vlastní adresu `/quiz-result/{id}`.
- Prázdný výběrový dialog vysvětlí, proč je prázdný, a nabídne cestu dál.

### Úpravy UI po revizi

- Prohlášení o přístupnosti má vlastní **veřejnou** routu `/accessibility`; dokumentace je až po
  přihlášení, a kdo se nedostane přes přihlašovací obrazovku, ten prohlášení potřebuje nejvíc.
  Odkaz je na stejném řádku jako copyright.
- Tři GIFy (13 MB) nahrazeny videem (~3 MB) ve dvou formátech — VP9/WebM a H.264/MP4, protože
  Chromium bez proprietárních kodeků H.264 nepřehraje. Stahuje se až při doscrollování, přehrává se
  samo (ztlumeně) a jde zastavit vlastním ovládáním prohlížeče.
- Výpisy: autor, datum vytvoření a úpravy u modelu i kvízu, náhled hlavního modelu u kapitoly,
  omezená šířka a rozestupy místo roztažení přes celou obrazovku.
- Stránkování: vlevo počet na stránku a „Zobrazeno 1–10 z 31“, vpravo tlačítka stránek.
- Stránka samotná neroluje v žádné ose; roluje se jen uvnitř prvků. Ověřeno testem ve třech šířkách.
- Filtry: hledání první a nejširší, pak řazení, směr jako ikona místo select boxu; navíc filtr podle
  autora, rozmezí data a obsaženého modelu (jen u kapitol, protože jen kapitoly modely mají).
- Detail kapitoly: hledání složené do ikony s křížkem pro zrušení, podkapitoly a podnadpisy na jednom
  řádku, který se při jejich absenci vůbec nezobrazí.
- Výběr oblasti textury: barva jako čtvereček vedle názvu, ne jako barva textu — světlé barvy byly na
  bílém pozadí nečitelné.
- Snímky obrazovek se generují `E2E_SHOTS=1 npx playwright test --project=shots` místo ručně.

## Nehotové

### Vyřazeno zadavatelem

- **V-3, textová alternativa 3D modelu.** Jediné místo zadání, které by rozšířilo datový model.
  Vyřazeno; uvedeno jako výjimka v prohlášení o přístupnosti.

### Mimo tento repozitář

- **Přístupné téma Keycloaku.** Přihlašovací obrazovka patří identity provideru; popisky, chybové
  hlášky ani jejich kontrast nejde opravit ve Vaadin kódu. EAA se přitom na identity provider
  vztahuje, takže bod nelze vynechat. Řetězce v `texts/loginForm_cs.json` jsou dnes nevyužité — buď
  je smazat, nebo použít jako zdroj pro překlad tématu.
- **Obsah prohlášení o přístupnosti.** Kostra je hotová a odpovídá skutečnému stavu; kontaktní údaje
  a datum posledního přezkumu doplní provozovatel.

### Zbývá udělat

- **Osm nedefinovaných překladových klíčů.** `CustomI18NProvider` vrací u neznámého klíče samotný
  klíč, takže uživatel místo hlášky uvidí jeho název. Najity při opravě `quiz.create.error`
  a `quiz.update.error` (ty už doplněné) porovnáním všech literálových volání `text("…")` proti
  `src/main/resources/texts/`:

  | Klíč | Kde se používá |
  |---|---|
  | `chapter.delete.failed` | `ChapterListItem` |
  | `model.delete.failed` | `ModelListItem` |
  | `quiz.delete.failed` | `QuizListItem` |
  | `error.modelLoadFailed` | `ChapterCreateView`, `ChapterDetailView`, `QuizCreateView` |
  | `error.editorDataRetrievalFailed` | `ChapterCreateView` |
  | `error.editorInitializationFailed` | `ChapterCreateView` |
  | `error.unexpectedChapterCreationError` | `ChapterCreateView` |
  | `notification.loading` | `DocumentationView` |

  První tři jsou na cestě mazání, tedy snadno dosažitelné. Spolu s doplněním textů má smysl přidat
  test, který projde literálové klíče proti textům — dokud těchto osm existuje, takový test by
  neprošel, což je důvod, proč zatím napsaný není.

- **Zahřívací přihlášení v CI.** Workflow čeká na bránu a na realm Keycloaku, jenže obojí odpoví
  dřív, než je přihlašovací cesta použitelná, a healthcheck míří na `/actuator/health`, který se do
  Vaadin ani do bezpečnostní cesty netrefí. Krok, který jednou projde celým přihlášením a teprve
  pak pustí sadu, by tu cestu zahřál. Je to změna CI, ne testu, proto zvlášť; samotný flake už
  řeší helper, který umí rozpoznat vypršelý pokus a jednou ho zopakovat.

- **Chybné heslo vypadá v e2e jako timeout.** Keycloak na něj překreslí formulář s vlastní chybovou
  hláškou, která není ani `denied`, ani `profile`, ani text o vypršení, takže `login()` dojede do
  limitu. Specy heslo nikdy nepletou, takže to dnes nikoho netrápí; záchytný bod by byl chybový
  element Keycloaku.

- **Ruční ověření Z-2 až Z-4.** Průchod klávesnicí je automatizovaný jen u 3D scény a ovládacího
  panelu. Chybí plný průchod odečítačem (NVDA nebo VoiceOver) na dvou scénářích a kontrola při 400%
  přiblížení. Rozsah, který ověřen nebyl, je poctivě přiznaný v prohlášení.
- **Sezení s pěti vyučujícími** podle [user-testing-plan.md](user-testing-plan.md). Automat změří,
  kolik kroků úloha stojí, ne jestli uživatel ví, který krok udělat. Zejména u nahrávání modelu platí,
  že automat přetáhne soubory jedním voláním a naměří dvě interakce, ale jestli vyučující tu plochu
  najde, řekne jen člověk.
- **Doběhnout testy** po posledních třech úpravách, viz výše.
- **Allure Report** (Apache 2.0) jako archiv výsledků. Zatím se zapisuje JUnit XML, které přečte
  každý nástroj; Kiwi TCMS byl zvážen a zamítnut kvůli GPL-2.0.

## Rozhodnutí, která si zaslouží zmínku

- **Souběžné úpravy se neztrácejí (#76).** Kapitola, kvíz i model se ukládají jen tehdy, když jsou
  pořád ve verzi, kterou editor načetl. Verzí je existující časové razítko `updated` (žádné nové pole,
  žádná migrace) a kontrola se zápisem je jedna podmíněná operace v Mongu (`VersionedWrites`), takže
  dvě uložení nemohou projít obě. Při neshodě aplikace neohlásí úspěch, ale otevře porovnání: vedle
  sebe uživatelova a uložená verze po položkách, u každé volba „moje / uložená“, dále „zahodit mé
  změny a načíst uloženou verzi“ a „zpět k úpravám“. Uložení z porovnání se kontroluje znovu.
  Obsah kapitoly (s modely u nadpisů) a otázky kvízu (se správnými odpověďmi) se volí jako celek.
  U modelu se volí název a „soubory a vzhled“ jako celek: uložená verze si soubory ponechá beze
  změny, odmítnuté nahrání po sobě v GridFS smaže jen to, co samo uložilo.

- **Řádkové výpisy místo dlaždic** (varianta B zadání). Dlaždice v pětisloupcové mřížce byla široká
  asi 240 px, takže se v ní všechno zkracovalo výpustkou a karta kapitoly potřebovala
  `ResizeObserver`, který jen počítal, kolik názvů modelů se vejde.
- **Lišta cookies ponechána** a doplněna o rovnocenné odmítnutí. Aplikace ukládá jen session a volbu
  motivu, takže by šlo lištu zrušit úplně; zvolen menší zásah.
- **Prodloužení času kvízu se nezavádí.** Časový limit u zkoušení je výjimka *Essential* kritéria
  2.2.1 — prodloužení by měřený výkon znehodnotilo. Místo toho varování minutu předem a odpočet po
  minutách, aby odečítač nemluvil nepřetržitě.
- **Odeslání kvízu s prázdnými otázkami nejde zablokovat.** První stisk vypíše, co chybí; druhý
  odešle. Nechat otázku prázdnou je legitimní volba a odmítnout odeslání by uživatele uvěznilo.
- **Klávesová alternativa u otázek s klikáním do textury usnadňuje otázku.** Seznam pojmenovaných
  oblastí je snazší než trefit oblast v 3D. Bez něj ale otázku nešlo zodpovědět klávesnicí vůbec.
  Formulace zadání otázky je tím pádem důležitější než dřív — to je na autorovi kvízu.
- **Autoplay videí je požadavek, ne záruka.** Test ověřuje, že o něj žádáme (ztlumeno, ve smyčce,
  `autoplay`), ne že ho prohlížeč vyhoví — headless Chromium i iOS ho běžně odmítnou.
