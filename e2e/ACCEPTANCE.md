# Akceptační sada e2e

Pokrytí katalogu doménových pravidel ([`zlesak/mish` → `docs/domain-rules.md`](https://github.com/zlesak/mish/blob/main/docs/domain-rules.md))
e2e testy v této složce (zlesak/mish#9). E2E testy nezávisí na jazyku implementace, proto jsou
kritériem hotovosti přenosu: co tady prochází proti starému systému, má procházet i proti novému.

## Rozsah

Akceptace = chování **starého** systému. V rozsahu jsou pravidla se stavem `platí`,
`platí, netestováno` a `změna ve v1`; u `změna ve v1` se ověřuje jen ta část, kterou mají oba systémy
společnou (u řádku je napsané která), záměrně změněná část se neověřuje. Mimo rozsah jsou
`nové ve v1`, `bug ve starém systému` (katalog u nich popisuje zamýšlené chování, které starý systém
nemá), `neplatí (bez migrace, D8)` a `zrušeno`.

**Souhrn:** 202 pravidel v rozsahu (158 `platí`, 28 `platí, netestováno`, 16 `změna ve v1`): **125 má e2e scénář**, 77 má zdůvodnění, proč ne. Mimo rozsah 35 (19 `nové ve v1`, 13 `bug ve starém systému`, 2 `neplatí`, 1 `zrušeno`). Katalog: commit `943aae4` starého repa.

Pravidla s „**ne**“ mají důvod: nepozorovatelné z UI (log, vnitřní API, pořadí zápisů), z UI
nedosažitelné (formulář vstup vůbec nedovolí), vyžaduje manipulaci s časem serveru nebo selhání
infrastruktury, nebo v testovacím realmu chybí potřebný uživatel (jediný učitel, žádný správce). Kde
je chování pokryté JUnit nebo Karibu testem, je třída uvedená — to jsou pravidla, která dnes kryje
**jen** JUnit/Karibu.

## Jak testy psát a spouštět

- Název testu začíná identifikátory pravidel (`'ASM-029, ASM-030: …'`), takže `grep ASM-029 e2e/`
  najde scénář.
- Prvky se hledají přes role a přístupná jména (`getByRole`, `getByLabel`, u pole bez popisku
  `getByPlaceholder`), nikdy přes Vaadin tagy nebo CSS třídy. Ovládání formulářů a přehrávače je
  soustředěné v [`quiz-helpers.ts`](quiz-helpers.ts) a [`content-helpers.ts`](content-helpers.ts):
  když se po přepisu UI změní popisek, mění se tam, ne v testech.
- Každá entita má jedinečný název (`uniqueName`), testy si data zakládají samy a běží proti prázdné
  databázi; výchozí model, kapitolu a kvíz zakládá projekt `setup` (`fixtures.setup.ts`).
- Spuštění: `npx playwright test --project=e2e` (v bug-team prostředí `bt e2e`). Na macOS je potřeba
  `E2E_CHANNEL=chromium`, jinak 3D scéna nemá WebGL.
- `quiz-timer.spec.ts` čeká skutečné dvě minuty limitu; nic nemanipuluje s hodinami.

## Známá omezení scénářů

- **Klik do textury** (`answerTextureClick`): přehrávač studentovi nenabídne seznam oblastí (viz
  Nalezeno, bod 1), takže test kliká do 3D scény. Souřadnice nejsou natvrdo: kandidáti se hledají
  podle barvy oblasti na snímku scény a rozhoduje, jakou barvu přehrávač ohlásí. Po opravě bodu 1 stačí
  helper přepnout na výběr ze seznamu.
- **Výpis modelů nejde prohledávat** (SRC-010, bug), proto názvy modelů v testech začínají „AAA“, aby
  byly na první stránce, a testy modely na konci mažou.
- **Odhlášení na QA stacku (8443)**: Keycloak odmítne `post_logout_redirect_uri=https://localhost:8443`
  (není v redirect URI realmu), takže SSO session v Keycloaku zůstane. AUTH-020 proto ověřuje stranu
  aplikace (zrušená session, správné přesměrování), ne to, že Keycloak znovu chce heslo.
- **SRC-015** počítá „dnes“ v časové zóně prohlížeče; pár sekund kolem půlnoci by mohl dát falešný
  výsledek.

## Nalezeno při psaní (starý systém, neopraveno)

1. **Klik do textury: seznam oblastí se studentovi nezobrazí.** `TextureClickQuestionRendererComponent`
   načítá texturu jako `QuickTextureEntity` jen s id, bez `csvContent`, takže `AbstractEntityView`
   nevystřelí událost s CSV a výběr „Nebo vyberte oblast ze seznamu“ zůstane skrytý. Klávesnicová
   alternativa ke kliknutí do 3D scény (WCAG 2.1.1) v přehrávači nefunguje. Kroky: kvíz s otázkou
   „Kliknutí na texturu“ (fixture model, textura `femur.1001-parts1.jpg`) → student → „Vybrat barvu“
   → model se načte, seznam oblastí se neobjeví.
2. **Klik do textury: správné kliknutí se hodnotí jako špatné u 4 z 20 oblastí fixture textury.**
   Hodnocení porovnává barvu přesně (ASM-036), ale texely JPEG textury se od barvy v CSV liší o jeden
   stupeň: `caput femoris` CSV `#2aff00`, textura `#29ff00`; `trochanter major` `#0007ff`/`#0007fe`;
   `collum femoris` `#d600ff`/`#d600fe`; `linea supracondylaris medialis` `#91fe81`/`#91ff82`. Maska
   ve scéně používá toleranci 8, hodnocení žádnou. Kroky: otázka s oblastí „caput femoris“ → student
   klikne doprostřed zelené hlavice → výsledek 20 / 27, otázka „Špatně (0 bodů)“. Testy proto používají
   oblast `linea intertrochanterica`, kde se barvy shodují.
3. **Po smazání kapitoly ukazuje kvíz dál její název.** Kvíz se odpojí (CH-009), ale `chapterName`
   zůstane, takže výpis kvízů i detail kvízu dál píšou „Kapitola: <smazaná kapitola>“ (ověřeno sondou). Test to neověřuje.
4. **Odmítnutý soubor v nahrávání modelu hlásí „Incorrect File Type.“** anglicky (výchozí text
   Vaadin Upload); vlastní česká hláška `modelUploadForm.dropZone.unsupported` se pro `.txt` nikdy
   neukáže, protože soubor odmítne už klient podle přípony.
5. **Tlačítka „Zvalidovat otázku“ a „Odstranit otázku“ v editoru kvízu nemají přístupné jméno**
   (jen ikona) a **pole názvu kapitoly i modelu má jen placeholder**, žádný popisek.
6. **Prázdná kapitola se uloží** (UI pošle prázdný odstavec) — už známé jako CH-003 /
   mishappfrontend#79; proto CH-002 z UI nejde ověřit.
7. **Hláška „Uložení kapitoly selhalo: : Hlavní model není vybrán!“** má dvojitou dvojtečku (už známé,
   mishappfrontend#93).

## Pokrytí po pravidlech

### Model (`MOD`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| MOD-001 | Kořen popisu nahrávaných souborů musí být 3D model. | platí | **ne** — Nedosažitelné z UI: popis souborů skládá formulář a kořen je vždy model. JUnit `ModelBackendServiceTest`. |
| MOD-002 | Textury a oblasti se spočítají jednou při nahrání a uloží se s modelem: hlavní textura je první potomek s rolí… | platí | `quiz-grading` „QZ-023, QZ-032 …“ (textury a oblasti fixture modelu v editoru otázky); `model-assets-visibility` |
| MOD-003 | Model bez textur má hlavní texturu prázdnou a seznam ostatních prázdný. | platí | **ne** — V UI nerozlišitelné od modelu s texturami jinak než vykreslením. JUnit `ModelBackendServiceTest`. |
| MOD-004 | Autorem nahraného modelu je přihlášený uživatel; created a updated jsou čas nahrání. | platí | `models` „MOD-004, MOD-006 …“ (autor ve výpisu) |
| MOD-005 | Model je „pokročilý“ (isAdvanced) právě tehdy, když má aspoň jednu texturu (hlavní nebo ostatní). | platí, netestováno | **ne** — Vnitřní příznak bez projevu v UI; netestováno ani JUnit. |
| MOD-006 | Název modelu se před uložením ořízne o okrajové mezery (při vytvoření i náhradě). | platí | `models` „MOD-004, MOD-006 …“ |
| MOD-007 | Model bez názvu nebo bez souboru modelu se neuloží a uživatel dostane hlášku, které pole chybí. | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| MOD-008 | Náhrada souborů modelu nejdřív uloží nové soubory a přepne na ně model, teprve potom smaže starý strom souborů. | platí | **ne** — Pořadí zápisů v úložišti, z UI nepozorovatelné. JUnit `ModelBackendServiceTest`. |
| MOD-009 | Náhrada zachová id modelu, autora a created; název se převezme z kořene nového popisu, updated = now. | platí, netestováno | **ne** — Mezera: náhrada souborů v editaci by vyžadovala druhé nahrání; z UI by šlo ověřit jen stejné URL a autora. Netestováno ani JUnit. |
| MOD-010 | Model používaný kapitolou nebo otázkou kvízu nejde smazat; odmítnutí vyjmenuje kapitoly i kvízy a nesmaže nic.… | změna ve v1 | `relations` „MOD-010, MOD-012 …“ — jen společná část (kapitola blokuje, hláška ji jmenuje, nic se nesmaže); blokování kvízy a 409/kód jsou změna ve v1 |
| MOD-011 | Kapitola bez názvu se v hlášce o odmítnutém smazání uvede svým id. | platí, netestováno | **ne** — Nedosažitelné: kapitolu bez názvu UI neuloží (CH-001). Netestováno. |
| MOD-012 | Smazání nepoužívaného modelu smaže jeho dokument i celý strom souborů. | platí | `relations` „MOD-010, MOD-012 …“; `models` „MOD-012, I18N-001 …“ |
| MOD-013 | Čtení modelu: prázdné id je chyba vstupu, neexistující id je „nenalezeno“. | platí | **ne** — Prázdné id nejde z UI poslat; neexistující id ukáže technickou hlášku (bug NAV-002). JUnit `ModelBackendServiceTest`. |
| MOD-014 | Hromadné čtení modelů vrací modely v pořadí požadovaných id a chybějící vynechá. | platí | **ne** — Vnitřní API (hromadné čtení). JUnit `ModelBackendServiceTest`. |
| MOD-015 | V seznamu všech textur modelu je hlavní textura první, za ní ostatní v uloženém pořadí. | platí | **ne** — Pořadí textur není v UI jednoznačně vidět (výběr textur řadí podle názvu). JUnit `QuickModelEntityTest`. |
| MOD-016 | Výběr modelů kapitoly odstraní duplicity podle id souboru modelu; hlavní je model pod klíčem main. | platí | `chapters` „CH-017, CH-020, MOD-016 …“ (model na třech nadpisech a hlavní model i na nadpisu → výběr nabízí každý model jednou, výchozí je hlavní) |
| MOD-017 | Popis modelu nese náhled a nastavení pozadí; bez pozadí, s neplatným pozadím nebo když by zakódovaný popis pře… | platí | **ne** — Náhled a pozadí jsou kódování popisu, v UI nepozorovatelné. JUnit `ModelServiceTest`. |
| MOD-018 | Souběžná úprava modelu se odmítne, ne tiše přepíše: nahrazení nad verzí, která se mezitím změnila, vrátí konfl… | platí | `models` „MOD-018 …“ |

### Soubory modelů (`FILE`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| FILE-001 | Nahrání uloží celý strom souborů a vrátí ho s přidělenými id a rolemi. | platí | `models` „MOD-004, MOD-006 …“ (nahrání modelu s texturami a CSV); `model-assets-visibility`; `model-crud` |
| FILE-002 | Soubor odkazovaný z více větví se uloží jen jednou a větve sdílejí jeho id. | platí | **ne** — Deduplikace v GridFS, z UI nepozorovatelná. JUnit `FileStorageServiceTest`. |
| FILE-003 | Popis odkazující na soubor, který nebyl nahrán, se odmítne s názvem chybějícího souboru. | platí | **ne** — Nedosažitelné z UI (popis skládá formulář). JUnit `FileStorageServiceTest`. |
| FILE-004 | Popis, ve kterém je soubor svým vlastním předkem, se odmítne. | platí | **ne** — Nedosažitelné z UI. JUnit `FileStorageServiceTest`. |
| FILE-005 | Strom souborů smí mít uzly nejvýš v hloubce 8 (kořen má hloubku 0). | platí | **ne** — Nedosažitelné z UI. JUnit `FileStorageServiceTest`. |
| FILE-006 | Smazání stromu smaže všechny binárky pod kořenem včetně kořene. | platí | **ne** — Mazání binárek v GridFS, z UI nepozorovatelné (viditelné je jen smazání modelu, MOD-012). JUnit `FileStorageServiceTest`. |
| FILE-007 | Selhání smazání jednoho souboru nezastaví mazání ostatních. | platí | **ne** — Vyžaduje selhání úložiště. JUnit `FileStorageServiceTest`. |
| FILE-008 | Id, které není platné ObjectId, se chová jako neexistující soubor: čtení nic nenajde, kontrola existence vrátí… | platí, netestováno | `navigation-access` „FILE-008, FILE-014, FILE-015 …“ (HTTP) |
| FILE-009 | Nahrané části se párují s popisem podle původního názvu souboru; při dvou částech se stejným názvem platí prvn… | platí, netestováno | **ne** — Vyžaduje ručně složený multipart se dvěma stejnými názvy; formulář ho nepošle. Netestováno. |
| FILE-010 | CSV oblastí má na každém řádku #barva;Název (oddělovač ;, právě dvě části, okrajové mezery se ořežou, prázdné … | platí | `quiz-grading` „QZ-023, QZ-032 …“ (platné CSV → pojmenované oblasti) |
| FILE-011 | Oblasti pro výběr se berou jen z ostatních (ne hlavní) textur, každý model (podle id souboru) jednou. | platí | `quiz-grading` „QZ-023, QZ-032 …“ (oblasti z ostatní textury); hlavní textura bez oblastí jen JUnit `TextureMapHelperTest` |
| FILE-012 | CSV patří k textuře se stejným základním jménem a příponou .jpg; hlavní textura CSV nemá. | platí | `quiz-grading` „QZ-023, QZ-032 …“ (CSV `femur.1001-parts1.csv` patří k `femur.1001-parts1.jpg`) |
| FILE-013 | Nahrát lze právě jeden soubor modelu (.glb nebo .obj), textury .jpg a oblasti .csv; první textura se stane hla… | platí | `models` „FILE-013 …“ |
| FILE-014 | Stažení přijme jen id souboru ze znaků A–Z a–z 0–9 _ -; jiné id se odmítne dřív, než se sáhne do úložiště. | platí | `navigation-access` „FILE-008, FILE-014, FILE-015 …“ |
| FILE-015 | Stažení neexistujícího souboru vrátí 404. | platí, netestováno | `navigation-access` „FILE-008, FILE-014, FILE-015 …“ |
| FILE-016 | Uvozovka v názvu souboru nerozbije hlavičku Content-Disposition. | platí | **ne** — Vyžaduje soubor s uvozovkou v názvu. JUnit `ModelDownloadControllerTest`. |
| FILE-017 | Název s diakritikou se pošle v parametru filename* kódovaném v UTF-8 (RFC 5987). | platí | **ne** — Vyžaduje nahrát soubor s diakritikou v názvu; hlavička je pokrytá JUnit `ModelDownloadControllerTest`. |
| FILE-018 | Soubor uložený bez názvu se stáhne s hlavičkou Content-Disposition: attachment bez názvu; typ obsahu je uložen… | platí | `navigation-access` „FILE-018, FILE-020 …“ (attachment s názvem); soubor bez názvu jen JUnit |
| FILE-019 | Stažení ověří, že soubor patří do stromu modelu, ke kterému má volající přístup; jinak je odmítnuto a zapsáno … | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| FILE-020 | Adresa souboru pro prohlížeč je /api/model/download/{id}; id, které by mohlo opustit cestu, se odmítne. | platí | `navigation-access` „FILE-018, FILE-020 …“ |
| FILE-021 | Z adresy se id souboru přečte jako úsek za /api/model/download/ do ? nebo /; adresa mimo tuto cestu nebo s prá… | platí | **ne** — Pomocná funkce klienta. JUnit `ModelFileUrlTest`. |

### Kapitola (`CH`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| CH-001 | Kapitola musí mít neprázdný název. | platí | `chapters` „CH-001 …“ |
| CH-002 | Obsah kapitoly nesmí být prázdný řetězec. | platí | **ne** — Z UI nedosažitelné: prázdný editor pošle prázdný odstavec a kapitola se uloží (známý bug CH-003, mishappfrontend#79). JUnit `ChapterBackendServiceTest`. |
| CH-003 | Kapitola bez skutečného obsahu nejde uložit: dokument Editor.js musí obsahovat aspoň jeden blok s neprázdným t… | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| CH-004 | Kapitola musí mít aspoň jeden model. | platí | `chapters` „CH-004 …“ (UI hlásí „Hlavní model není vybrán!“) |
| CH-005 | Každý model, na který kapitola odkazuje, musí existovat. | platí | **ne** — Nedosažitelné z UI (výběr nabízí jen existující modely). JUnit `ChapterBackendServiceTest`. |
| CH-006 | Nová kapitola dostane id od serveru (příchozí id se zahodí), autorem je přihlášený uživatel, created je čas vy… | platí | `chapters` „CH-006, CH-021, AUD-008, I18N-005 …“ |
| CH-007 | Úprava zachová původního autora a čas vytvoření bez ohledu na to, co klient pošle. | platí | **ne** — V realmu je jediný učitel, autora nelze změnit jiným uživatelem. JUnit `ChapterBackendServiceTest`. |
| CH-008 | Úprava bez id se odmítne. | platí, netestováno | **ne** — Nedosažitelné z UI. Netestováno. |
| CH-009 | Smazání kapitoly odpojí její kvízy (zruší jim chapterId), smaže kapitolu a její záznam ve fulltextu. | platí | `relations` „CH-009, QZ-004, QZ-031, SRC-006 …“ |
| CH-010 | Smazání neexistující kapitoly je „nenalezeno“ a nic neodpojí ani nesmaže. | platí | **ne** — Nedosažitelné z UI (maže se jen vypsaná kapitola). JUnit `ChapterBackendServiceTest`. |
| CH-011 | Čtení kapitoly doplní její modely (v pořadí modelIds) a kvízy, které k ní patří. | platí | `chapters` „CH-006, CH-021 …“ (model ve výpisu), `accessibility` (model v detailu); kvízy kapitoly UI nezobrazuje |
| CH-012 | Čtení kapitoly: prázdné id je chyba vstupu, neexistující id je „nenalezeno“. | platí | **ne** — Neexistující kapitola ukáže technickou hlášku (bug NAV-002). JUnit `ChapterBackendServiceTest`. |
| CH-013 | Kapitola ukládá jen id svých modelů; model se při čtení dohledá, kapitola nikdy nenese jeho kopii. Model, kter… | platí | **ne** — Nedosažitelné: použitý model nejde smazat (MOD-010). JUnit `ChapterEntityTest`. |
| CH-014 | Výpis kapitol nenese obsah a modely načte jedním dotazem pro celou stránku. | platí, netestováno | **ne** — Výkonové/vnitřní (jedno čtení modelů). Netestováno. |
| CH-015 | Nadpis s přiřazeným modelem ukládá id dokumentu modelu, ne id jeho souboru. | platí | **ne** — Uložené id v obsahu kapitoly (id dokumentu vs. id souboru) není v UI vidět; projev (správný model podkapitoly) ověřuje CH-020. JUnit `ChapterServiceTest`. |
| CH-016 | Při čtení se id modelu v nadpisu převede na aktuální id souboru modelu; neodpovídá-li žádnému modelu kapitoly,… | platí | **ne** — Převod id v obsahu není v UI vidět; projev ověřuje CH-020, záložní „uložená hodnota beze změny“ z UI nevznikne. JUnit `ChapterServiceTest`. |
| CH-017 | Modely kapitoly jsou hlavní model na prvním místě a za ním různé modely přiřazené nadpisům, každý jednou. | platí | částečně: `chapters` „CH-017, CH-020, MOD-016 …“ (hlavní model + modely nadpisů, každý jednou, výchozí hlavní); pořadí „hlavní první“ výběr v detailu neukazuje (staví ho z HashMap), pořadí JUnit `ChapterServiceTest` |
| CH-018 | Podkapitoly jsou nadpisy úrovně 1 v pořadí dokumentu; nadpis bez id dostane id fallback- + 7 znaků, nadpis bez… | platí | `chapters` „CH-018, CH-019, CH-025, CH-028 …“ (podkapitoly = nadpisy úrovně 1 v pořadí) |
| CH-019 | Osnova kapitoly: nadpisy úrovně ≥ 2 patří pod nejbližší předchozí nadpis úrovně 1, nadpisy před prvním nadpise… | platí | `chapters` „CH-018, CH-019, CH-025, CH-028 …“ (nadpisy pod podkapitolou) |
| CH-020 | Model pro zobrazení: klíč main má první model kapitoly, podkapitola má model, jehož id souboru je rovno modelu… | platí | `chapters` „CH-017, CH-020, MOD-016 …“ (podkapitola ukáže model svého nadpisu; výchozí je hlavní); kapitola bez modelů z UI nevznikne (CH-004) |
| CH-021 | Název kapitoly se před uložením ořízne. | platí, netestováno | `chapters` „CH-006, CH-021 …“ |
| CH-022 | Při uložení kapitoly se autor upozorní (uložení se nezablokuje) na odkazy s nepopisným textem: zde, tady, sem,… | platí | `chapters` „CH-022 …“ |
| CH-023 | Souběžná úprava kapitoly se odmítne, ne tiše přepíše: uložení nad verzí, která se mezitím změnila, vrátí konfl… | platí | `chapters` „CH-023 …“ |
| CH-024 | Import z Moodle ZIP vyžaduje imsmanifest.xml. | platí | `chapters` „CH-024 …“ |
| CH-025 | Import: název organizace je název kapitoly, každá položka s identifierref je podkapitola se svým titulkem a ob… | platí | `chapters` „CH-018, CH-019, CH-025, CH-028 …“; `chapter-import` |
| CH-026 | Import: HTML bez <body> se převezme celé. | platí | **ne** — Vyžaduje speciální ZIP. JUnit `MoodleZipParserTest`. |
| CH-027 | Import: jako obrázky se rozpoznají přípony png, jpg, jpeg, gif, bmp, svg. | platí | **ne** — Vyžaduje speciální ZIP. JUnit `MoodleZipParserTest`. |
| CH-028 | Import: položka bez identifierref ani podkapitola s chybějícím HTML se nepřevezme; pořadí podkapitol je pořadí… | platí | `chapters` „CH-018, CH-019, CH-025, CH-028 …“ (pořadí podkapitol); vynechání položek jen JUnit `MoodleZipParserTest` |

### Kvíz – tvorba a validace (`QZ`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| QZ-001 | Kvíz musí mít neprázdný název. | platí | `quiz-validation` „QZ-001 …“ |
| QZ-002 | Časový limit je v minutách; null nebo 0 znamená bez limitu, záporný limit se odmítne. | platí, netestováno | `quiz-validation` „QZ-002, QZ-003 …“; `quiz-timer` „ASM-003, ASM-012 …“ (0 = bez limitu) |
| QZ-003 | Formulář kvízu navíc vyžaduje popis a vyplněný limit (≥ 0). | platí, netestováno | `quiz-validation` „QZ-003 …“, „QZ-002, QZ-003 …“; prázdný limit UI převede na 0 |
| QZ-004 | Kvíz navázaný na kapitolu: kapitola musí existovat a její název se uloží ke kvízu; kvíz bez kapitoly má název … | platí, netestováno | `relations` „CH-009, QZ-004 …“ (název kapitoly u kvízu); neexistující kapitola nedosažitelná z UI |
| QZ-005 | Kvíz musí mít aspoň jednu otázku. | platí | `quiz-validation` „QZ-005 …“ |
| QZ-006 | Ke každé otázce musí být odpověď (stejný počet, párování podle pozice); otázka ani odpověď nesmí chybět. | platí | **ne** — Nedosažitelné z UI (editor otázku a odpověď páruje vždy). JUnit `QuizAuthoringValidatorTest`. |
| QZ-007 | Každá otázka musí mít kladný počet bodů. | platí | `quiz-validation` „QZ-007 …“ |
| QZ-008 | Každá otázka musí mít znění. | platí | `quiz-validation` „QZ-008, QZ-011 …“ |
| QZ-009 | Typ odpovědi musí odpovídat typu otázky (deklarovaný typ i skutečný druh dat). | platí | **ne** — Nedosažitelné z UI. JUnit `QuizAuthoringValidatorTest`. |
| QZ-010 | Otázka neznámého typu se odmítne. | platí, netestováno | **ne** — Nedosažitelné z UI. Netestováno. |
| QZ-011 | Odmítnutí řekne, která otázka (pořadím od 1) je špatně a proč; editor ukáže stejný důvod ještě před odesláním. | platí | `quiz-validation` „QZ-008, QZ-011 …“, „QZ-011, QZ-019 …“ |
| QZ-012 | Otázky s výběrem, řazením a přiřazením musí nabízet aspoň dvě možnosti (u přiřazení oba sloupce). | platí | `quiz-validation` „QZ-012 …“ |
| QZ-013 | Jedna správná odpověď: správný index je povinný a musí ukazovat na existující možnost. | platí | `quiz-validation` „QZ-013, QZ-014 …“ (chybí správná); index mimo rozsah nedosažitelný z UI |
| QZ-014 | Více správných odpovědí: aspoň jedna, žádná dvakrát, všechny v rozsahu možností (indexy od 0). | platí | `quiz-validation` „QZ-013, QZ-014 …“; `quiz-grading` (správná je i poslední možnost) |
| QZ-015 | Editor čísluje možnosti pro autora od 1, ale ukládá index od 0. | platí | `quiz-grading` „ASM-029, ASM-030 …“ (správně je 2. a 4. ze 4 možností) |
| QZ-016 | Řazení: správné pořadí obsahuje každou položku právě jednou (délka = počet položek, bez duplicit, v rozsahu). | platí | **ne** — Nedosažitelné z UI (editor ukládá vždy identitu, QZ-017). JUnit `QuizAuthoringValidatorTest`. |
| QZ-017 | Editor řazení bere pořadí, ve kterém autor položky zadal, jako správné. | platí | `quiz-grading` (položky řazení zadané ve správném pořadí) |
| QZ-018 | Přiřazení: klíč je mapa {levý index → pravý index} a musí obsahovat každou levou položku právě jednou. | platí | `quiz-validation` „QZ-018, QZ-020 …“ |
| QZ-019 | Přiřazení: žádná pravá položka nesmí být přiřazená dvěma levým. | platí | `quiz-validation` „QZ-011, QZ-019 …“ |
| QZ-020 | Přiřazení: klíč nesmí být prázdný a indexy musí ležet v rozsahu sloupců. | platí, netestováno | `quiz-validation` „QZ-018, QZ-020 …“ (chybí pár); index mimo rozsah nedosažitelný z UI |
| QZ-021 | Editor přiřazení vybírá u každé pravé položky levou (číslovanou od 1) a ukládá klíč obráceně jako {levý → prav… | platí | `quiz-grading` (přiřazení, které není samo sobě inverzí) |
| QZ-022 | Otevřená odpověď: uznávané odpovědi se uklidí (ořez, prázdné pryč, odpovědi lišící se jen velikostí písmen se … | platí | `quiz-editing` „QZ-022, QZ-023 …“; `quiz-validation` „QZ-022 …“ |
| QZ-023 | Nová otevřená otázka má v editoru „přesná shoda“ zapnutou a nová otázka libovolného typu má 1 bod. | platí | `quiz-grading` „QZ-023, QZ-032 …“; `quiz-editing` „QZ-022, QZ-023 …“ |
| QZ-024 | Klik do textury: model i textura otázky musí existovat v úložišti souborů a odpověď musí mít vybranou oblast (… | platí | `quiz-validation` „QZ-024 …“ (chybí oblast); neexistující model/textura nedosažitelné z UI |
| QZ-025 | Otázka a její odpověď dostanou společné id; otázky bez id dostanou nové, navzájem různé. | platí | `quiz-grading` (každá otázka hodnocena svou odpovědí); vlastní id jen JUnit `QuizAuthoringValidatorTest` |
| QZ-026 | Existující id otázky se při uložení zachová, takže pokus rozpracovaný před úpravou kvízu se dál hodnotí správn… | platí | `quiz-editing` „QZ-026 …“ |
| QZ-027 | Dvě otázky se stejným id: první si ho ponechá, druhá dostane nové. | platí | **ne** — Nedosažitelné z UI. JUnit `QuizAuthoringValidatorTest`. |
| QZ-028 | Úprava kvízu zachová původního autora a čas vytvoření; úprava bez id se odmítne. | platí, netestováno | **ne** — V realmu je jediný učitel; úprava bez id nedosažitelná. Netestováno. |
| QZ-029 | Čtení kvízu: prázdné id je chyba vstupu, neexistující id je „nenalezeno“. | platí | **ne** — Neexistující kvíz ukáže technickou hlášku (bug NAV-002). JUnit `QuizBackendServiceTest`. |
| QZ-030 | Smazání neexistujícího kvízu je „nenalezeno“, nic nesmaže a nezapíše audit. | platí | **ne** — Nedosažitelné z UI. JUnit `QuizBackendServiceTest`. |
| QZ-031 | Odpojení od kapitoly zruší chapterId všem kvízům té kapitoly a vrátí jejich počet; prázdné id kapitoly nedělá … | platí | `relations` „CH-009, QZ-004, QZ-031 …“ |
| QZ-032 | Maximální skóre kvízu je součet bodů jeho otázek; otázky bez bodů se nepočítají. | platí | `quiz-grading` („… / 27“); `relations` („… / 8“) |
| QZ-033 | Souběžná úprava kvízu se odmítne, ne tiše přepíše: uložení nad verzí, která se mezitím změnila, vrátí konflikt… | platí | `quiz-editing` „QZ-033 …“ |

### Assessment (`ASM`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| ASM-001 | Otevření kvízu studentem založí pokus, který patří dvojici uživatel + kvíz; pokusy o různé kvízy se navzájem n… | platí | **ne** — Nezávislost pokusů o různé kvízy se z UI ukáže jen s měřením času; JUnit `QuizAttemptServiceTest`. |
| ASM-002 | Pokus o kvíz s limitem má termín = čas spuštění + limit v minutách. | platí | `quiz-timer` „ASM-002, ASM-004 …“ |
| ASM-003 | Kvíz s limitem 0, null nebo záporným je bez limitu a nemá termín. | platí | `quiz-timer` „ASM-003, ASM-012 …“ |
| ASM-004 | Opětovné otevření kvízu běžící pokus nezmění (termín se neposune). | platí | `quiz-timer` „ASM-002, ASM-004 …“ (po minutě a znovuotevření zbývá „méně než minuta“, ne 2 min) |
| ASM-005 | Spuštění vrací termín pokusu, který skutečně běží. | platí | `quiz-timer` „ASM-002, ASM-004 …“ |
| ASM-006 | Student dostane zbývající čas pokusu v celých sekundách, nikdy záporný; kvíz bez limitu zbývající čas nemá. | platí | `quiz-timer` „ASM-002, ASM-004 …“ (zbývající čas po znovuotevření); nezáporná hodnota jen JUnit `QuizApiClientTest` |
| ASM-007 | Zobrazení výsledku pokus nespouští ani nemění. | platí | **ne** — Projev (neposunutý časovač) je v minutové granularitě neměřitelný. Karibu `QuizViewsKaribuTest`. |
| ASM-008 | Nedokončený pokus se smaže den po spuštění. | platí, netestováno | **ne** — TTL jeden den, vyžaduje manipulaci s časem serveru. Netestováno. |
| ASM-009 | Odpočet začíná od zbývajícího času pokusu, ne od celého limitu; bez zbývajícího času (první otevření) od celéh… | platí | `quiz-timer` „ASM-002, ASM-004 …“ |
| ASM-010 | Zbývající čas se zobrazuje v celých minutách zaokrouhlených nahoru; při 60 s a méně „méně než minuta“. | platí | `quiz-timer` „ASM-002, ASM-004 …“ („2 min“, „méně než minuta“) |
| ASM-011 | Minutu před koncem dostane student jednou varování; prodloužení času se nenabízí. | platí | `quiz-timer` „ASM-002, ASM-004 …“ |
| ASM-012 | Kvíz bez limitu časovač nemá. | platí | `quiz-timer` „ASM-003, ASM-012 …“ |
| ASM-013 | Po vypršení se kvíz odešle automaticky a jen jednou, bez souhrnu nezodpovězených otázek. | platí | `quiz-timer` „ASM-002, ASM-004 …“ |
| ASM-014 | Automatické odeslání odešle i rozepsanou odpověď: otevřená odpověď se serveru hlásí průběžně po každé pauze v … | platí | `quiz-timer` „ASM-002, ASM-004 …“ |
| ASM-015 | Klient odesílá při vypršení s rezervou: odpočet startuje o 1 s níž a odešle, když zbývá ≤ 1 s. | platí, netestováno | **ne** — Rezerva 1 s je pod rozlišením e2e. Kód `QuizTimerComponent`, netestováno. |
| ASM-016 | Odevzdání musí nést id kvízu. | platí, netestováno | **ne** — Nedosažitelné z UI. Netestováno. |
| ASM-017 | Odevzdání bez spuštěného pokusu se odmítne dřív, než se kvíz načte. | platí | **ne** — Nedosažitelné z UI (přehrávač pokus vždy spustí). JUnit `QuizResultBackendServiceTest`, `QuizAttemptServiceTest`. |
| ASM-018 | Odevzdání po termínu se odmítne. | platí | **ne** — Klient odešle před termínem; pozdní odevzdání vyžaduje zastavit časovač nebo posunout čas serveru. JUnit `QuizAttemptServiceTest`. |
| ASM-019 | Kvíz bez limitu lze odevzdat kdykoli, dokud pokus existuje. | platí | **ne** — „Kdykoli“ znamená po hodinách; e2e odevzdává kvízy bez limitu po sekundách (`quiz-grading`). JUnit `QuizAttemptServiceTest`. |
| ASM-020 | Jeden pokus lze odevzdat jen jednou. | platí | **ne** — Nedosažitelné z UI (po odeslání se přehrávač zablokuje, nové otevření založí nový pokus). JUnit `QuizAttemptServiceTest`. |
| ASM-021 | Pokus se spotřebuje až těsně před zápisem výsledku: když selže načtení kvízu nebo hodnocení, pokus zůstane a s… | platí | **ne** — Vyžaduje selhání hodnocení. JUnit `QuizResultBackendServiceTest`. |
| ASM-022 | Když odeslání selže, odpovědi zůstanou a přehrávač se znovu povolí. | platí | **ne** — Vyžaduje chybu serveru při odeslání. Karibu `QuizViewsKaribuTest`. |
| ASM-023 | Otázka je zodpovězená, jakmile student poprvé změní její vstup; zůstane zodpovězená, i když hodnotu potom smaž… | platí | `quiz-grading` „ASM-023..025, ASM-038 …“; „napsat a smazat“ jen `QuizPlayerComponentTest` |
| ASM-024 | První stisk „odeslat“ s nezodpovězenými otázkami nic neodešle, vypíše čísla chybějících otázek (od 1) s odkazy… | platí | `quiz-grading` „ASM-023..025, ASM-038 …“ |
| ASM-025 | Když student doplní všechny chybějící otázky, souhrn zmizí, tlačítko se vrátí na „Odeslat kvíz“ a potvrzení „o… | platí | `quiz-grading` „ASM-023..025, ASM-038 …“ (zbude jen nezodpovězená) |
| ASM-026 | Řazení: položky se studentovi ukážou v náhodném pořadí; odeslaný seznam je indexovaný původním pořadím položek… | platí | `quiz-grading` (řazení se zadává po položkách bez ohledu na zamíchané pořadí); `-1` za nevybranou pozici jen `QuestionRendererComponentsTest` |
| ASM-027 | Přiřazení: odeslaná mapa obsahuje jen levé položky, ke kterým student něco vybral. | platí | `quiz-grading` „ASM-029..036, ASM-040 …“ (vynechaná dvojice → špatně) |
| ASM-028 | Více správných: odesílají se vlastní indexy možností od 0. | platí | `quiz-grading` „ASM-029, ASM-030 …“ (poslední možnost) |
| ASM-029 | Každá otázka se hodnotí vše-nebo-nic: správná dostane všechny své body, jinak 0; částečné body neexistují. | platí | `quiz-grading` (všechny typy správně / téměř správně) |
| ASM-030 | Více správných: odpověď je správná, když se množina zaškrtnutých rovná množině správných (pořadí nehraje roli). | platí | `quiz-grading` (jiné pořadí zaškrtnutí, podmnožina) |
| ASM-031 | Jedna správná: zvolený index se musí rovnat správnému. | platí | `quiz-grading` |
| ASM-032 | Přiřazení: odeslaná mapa {levý → pravý} se musí celá rovnat klíči; chybějící dvojice znamená špatně. | platí | `quiz-grading` (vynechaná dvojice) |
| ASM-033 | Řazení: odeslaný seznam se musí rovnat správnému prvek po prvku. | platí | `quiz-grading` (prohozené pozice) |
| ASM-034 | Otevřená bez přesné shody (exactMatch vypnuté nebo nevyplněné): shoda s kteroukoli uznávanou odpovědí po ořezu… | platí | `quiz-editing` „ASM-034, ASM-035 …“ |
| ASM-035 | Otevřená s přesnou shodou: po ořezu mezer musí sedět i velikost písmen. | platí | `quiz-editing` „ASM-034, ASM-035 …“; `quiz-grading` (mezery kolem, jiná velikost písmen) |
| ASM-036 | Klik do textury: barva oblasti se porovná bez ohledu na velikost písmen; otázka bez uložené barvy je vždy špat… | platí | `quiz-grading` (správná a špatná oblast); viz „Nalezeno“ níže — u 4 z 20 oblastí fixture textury správné kliknutí neprojde |
| ASM-037 | Odpověď jiného typu, než je otázka, je špatně. | platí, netestováno | **ne** — Nedosažitelné z UI. Netestováno. |
| ASM-038 | Výsledek obsahuje všechny otázky kvízu v jeho pořadí; nezodpovězená je nesprávná, za 0 bodů a bez odeslané odp… | platí | `quiz-grading` „ASM-023..025, ASM-038 …“; `quiz-timer` |
| ASM-039 | Odpovědi na otázky, které v kvízu nejsou, se ignorují. | platí | **ne** — Nedosažitelné z UI. JUnit `QuizGradingServiceTest`. |
| ASM-040 | Procento = získané / maximální × 100 bez zaokrouhlení; maximum je součet bodů všech otázek kvízu; při maximu 0… | platí | `quiz-grading` (100.00, 0.00, 74.07, 88.89 %); `relations` (37.50 %) |
| ASM-041 | Více odeslaných odpovědí na tutéž otázku: platí první. | platí, netestováno | **ne** — Nedosažitelné z UI. Netestováno. |
| ASM-042 | Výsledek se uloží jako samostatný snímek: student, kvíz, název kvízu, název kapitoly v okamžiku odevzdání (neb… | platí, netestováno | `relations` „ASM-042, ASM-043 …“ (uložený snímek čitelný bez kvízu) |
| ASM-043 | Výsledek zůstane čitelný po smazání kvízu: rozpis se vezme z uloženého výsledku v uloženém pořadí a maximum z … | platí | `relations` „ASM-042, ASM-043 …“ |
| ASM-044 | Výsledek u každé otázky ukáže odpověď studenta; správné odpovědi jen tehdy, když učitel u kvízu zapnul „Po ode… | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| ASM-045 | Slovní hodnocení výsledku: ≥ 90 % „Výborně!“, ≥ 75 % „Dobrá práce!“, ≥ 60 % „Prošel/a jste!“, jinak „Bohužel, … | platí | `quiz-grading` (všechna čtyři hodnocení) |

### Vyhledávání a výpisy (`SRC`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| SRC-001 | Fulltext najde záznamy, jejichž text obsahuje **každé** slovo dotazu jako podřetězec bez ohledu na velikost pí… | platí | `search-listing` „SRC-001, SRC-019 …“, „SRC-007, SRC-001 …“ |
| SRC-002 | Dotaz se bere doslova, znaky regulárních výrazů nemají zvláštní význam. | platí | `search-listing` „SRC-002, SRC-013 …“ |
| SRC-003 | Prázdný dotaz nebo jen mezery nevrátí nic (ne všechno). | platí | **ne** — UI prázdný dotaz neodešle (tlačítko Hledat je zakázané) a výpis bez dotazu ukazuje vše (SRC-012). JUnit `FullTextSearchServiceTest`. |
| SRC-004 | Fulltext hledá jen v záznamech požadovaného druhu (dnes jen kapitoly). | platí | **ne** — Výpis kapitol a výpis kvízů jsou oddělené obrazovky; rozsah fulltextu JUnit `FullTextSearchServiceTest`. |
| SRC-005 | Text kapitoly v indexu je název a obsah spojené mezerou (prázdné části se vynechají); uložení kapitoly záznam … | platí | `search-listing` „SRC-007, SRC-001 …“ (nalezení podle slova z obsahu) |
| SRC-006 | Smazaná kapitola z vyhledávání zmizí. | platí | `relations` „CH-009, … SRC-006 …“ |
| SRC-007 | Hledání kapitol najde kapitolu, když její název obsahuje dotaz **nebo** její text odpovídá fulltextu; filtr au… | platí, netestováno | `search-listing` „SRC-007, SRC-001 …“, „SRC-011 …“, „SRC-014 …“ (filtry s dotazem) |
| SRC-008 | Vyhledávání ignoruje diakritiku (v názvech i ve fulltextu). | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| SRC-009 | Kvízy, modely a výsledky se hledají jen v názvu, jako doslovný podřetězec bez ohledu na velikost písmen. | platí | `search-listing` „SRC-009 …“ |
| SRC-010 | Výpis modelů jde hledat podle názvu stejně jako kapitoly a kvízy. | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| SRC-011 | Filtr kapitol podle obsaženého modelu: název modelu (podřetězec, bez ohledu na velikost písmen) se převede na … | platí | `search-listing` „SRC-011, SRC-012 …“ |
| SRC-012 | Nevyplněné filtry (null, prázdné, jen mezery) se ignorují. | platí | `search-listing` „SRC-011, SRC-012 …“ (nevyplněné filtry se neuplatní) |
| SRC-013 | Hledaný výraz se ořízne; vyplněný název má přednost před volným textem. | platí | `search-listing` „SRC-002, SRC-013 …“ |
| SRC-014 | Autor se filtruje podle zobrazovaného jména (podřetězec, bez ohledu na velikost písmen), ne podle id. | platí | `search-listing` „SRC-014 …“ |
| SRC-015 | Rozmezí data vytvoření platí včetně obou krajních dnů v časové zóně uživatele: „od“ od půlnoci prvního dne, „d… | platí | `search-listing` „SRC-015 …“ |
| SRC-016 | Výchozí stránkování je stránka 0, 10 položek, řazení podle názvu vzestupně; nové hledání vrátí na stránku 0 a … | platí | `search-listing` „SRC-016, SRC-018 …“ (výchozí řazení); návrat na stránku 0 vyžaduje > 10 položek, `FilterParametersTest` |
| SRC-017 | Přechod na jinou stránku zachová velikost stránky i řazení. | platí | **ne** — Vyžaduje víc než 10 položek jednoho druhu (paralelní běh je nezaručí). JUnit `FilterParametersTest`. |
| SRC-018 | Řadit lze jen podle name, created, updated, description, creatorId, creatorName, totalScore, percentage (první… | platí | `search-listing` „SRC-016, SRC-018 …“ (směr řazení); seznam polí JUnit `ListingToolbarTest` |
| SRC-019 | Celkový počet položek se počítá stejným filtrem jako stránka. | platí, netestováno | `search-listing` „SRC-001, SRC-019 …“ („Zobrazeno 1–2 z 2“) |
| SRC-020 | Prázdná stránka neprázdného výpisu hlásí skutečný celkový počet a nabízí stránkování; úplně prázdný výpis strá… | platí | `search-listing` „SRC-020 …“ (prázdný výpis bez stránkování); prázdná stránka neprázdného výpisu Karibu `ChapterListingViewKaribuTest` |

### Přihlášení, role, oprávnění (`AUTH`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| AUTH-001 | Role se berou jen z realm_access.roles; platí jen student, teacher a admin, ostatní hodnoty (default-roles-mis… | změna ve v1 | Společná část (rozhoduje realmová role učitele): `student-no-teacher-routes`, `auth-role-guard` a všechny učitelské scénáře; čtení `resource_access` je změna ve v1, neověřuje se |
| AUTH-002 | Role se přidělují skupinami Keycloaku: MISH-ucitele → teacher, MISH-spravci → admin; student je výchozí role r… | změna ve v1 | Společná část (alice = učitel, bart = student): `student-no-teacher-routes`, učitelské scénáře; skupiny Keycloaku jsou změna ve v1 |
| AUTH-003 | Id uživatele je claim sub (OIDC i bearer token); nepřihlášený nemá id a akce vyžadující přihlášení se mu odmít… | platí | `navigation-access` „AUTH-017, AUTH-018 …“ (nepřihlášený nic chráněného nedostane); claim `sub` JUnit `CurrentUserProviderTest` |
| AUTH-004 | Jméno autora pro zobrazení je preferred_username, jinak name, jinak jméno z přihlášení. | platí, netestováno | `chapters` „CH-006, CH-021, AUD-008 …“ („Autor: alice“ = preferred_username) |
| AUTH-005 | Uživatel má roli X, když je X mezi rolemi z AUTH-001. Role se sčítají; admin neobsahuje teacher (rozhodnutí D2… | změna ve v1 | **ne** — Starý realm nemá uživatele správce; sčítání rolí je změna ve v1. JUnit `CurrentUserProviderTest`. |
| AUTH-006 | Vlastník (ownerId, rozhodnutí D6) s rolí teacher smí upravit, publikovat a smazat svůj obsah. Vlastník bez rol… | změna ve v1 | Společná část (vlastník-učitel smí upravit a smazat svůj obsah): `chapters` „CH-023“, `quiz-editing`, `models`, `relations`; „jen vlastník“ je změna ve v1 |
| AUTH-007 | Cizí kapitolu, model a kvíz smí měnit, publikovat, mazat a předávat jen jejich vlastník a správce; jiný učitel… | změna ve v1 | **ne** — Změna ve v1 (dnes smí každý učitel cokoli); starý realm má jediného učitele. Neověřuje se. |
| AUTH-008 | Kdo nemá roli teacher, obsah nevytvoří, nezmění ani nesmaže, vlastní ani cizí. | platí | `navigation-access` „AUTH-008, AUTH-016 …“ (editor existujícího obsahu); `student-no-teacher-routes` (tvorba) |
| AUTH-009 | Obsah bez vlastníka (ownerId = null) se řídí jako cizí obsah: měnit, publikovat a mazat ho smí jen správce, kt… | změna ve v1 | **ne** — Změna ve v1; obsah bez autora z UI nevznikne. Neověřuje se. |
| AUTH-010 | Pravidla vlastnictví z matice platí pro všech šest zápisů existujícího obsahu: úprava a smazání kapitoly, mode… | změna ve v1 | Společná část (student nemění nic): `navigation-access` „AUTH-008, AUTH-016 …“ |
| AUTH-011 | Odmítnutý zápis nezmění nic: nesmaže se dokument, neodpojí kvízy a u modelu se ani neuloží nové soubory. | platí | **ne** — UI studentovi zápis nenabídne, odmítnutý zápis nejde z UI vyvolat. JUnit `ModelBackendServiceTest`, `ChapterBackendServiceTest`. |
| AUTH-012 | Tvorba kapitol, modelů a kvízů vyžaduje roli teacher (správce bez role učitele ne, rozhodnutí D2). Oprávnění s… | změna ve v1 | Společná část (tvorba jen pro učitele): `student-no-teacher-routes`; vynucení na serveru a správce bez role učitele jsou změna ve v1 |
| AUTH-013 | Student dostane kvíz bez klíče odpovědí; s klíčem (getQuizWithAnswers) jen vlastník kvízu s rolí teacher a spr… | změna ve v1 | Společná část: student neotevře editor kvízu s klíčem (`navigation-access` „AUTH-008 …“); tvar odpovědi API je změna ve v1, JUnit `QuizBackendServiceTest` |
| AUTH-014 | Každý vidí své výsledky; učitel navíc výsledky studentů zapsaných v kurzu, který vlastní (CRS-010), ne všechny… | změna ve v1 | **ne** — Změna ve v1 (dnes učitel vidí všechny výsledky); jeden student v realmu cizí výsledek nevytvoří. Neověřuje se. |
| AUTH-015 | Výpis výsledků nenese rozpis po otázkách a lze ho omezit na jeden kvíz. | platí | `relations` „teacher writes the quiz, student takes it“ (detail kvízu vypíše jen pokusy o tento kvíz, jen součty, žádné „Otázka N:“); `public-routes` |
| AUTH-016 | Učitelské obrazovky (tvorba kapitoly, modelu, kvízu) jsou jen pro roli teacher; administrační centrum se ruší … | změna ve v1 | Společná část (učitelské obrazovky jen pro učitele, student dostane „Přístup odepřen“): `student-no-teacher-routes`, `auth-role-guard`, `navigation-access` „AUTH-008, AUTH-016 …“; zrušení `/administration` a návod na stránce 403 jsou změna ve v1 |
| AUTH-017 | Ostatní routy aplikace (/chapters, /chapter/{id}, /models, /model/{id}, /quizes, /quiz/{id}, /playQuiz/{id}, /… | platí | `navigation-access` „AUTH-017, AUTH-018 …“; `student-access`; `public-routes` |
| AUTH-018 | Nepřihlášený na chráněné routě je přesměrován na přihlášení. | platí | `navigation-access` „AUTH-017, AUTH-018 …“; `auth-role-guard` |
| AUTH-019 | Bez přihlášení jsou dostupné jen /img/**, /skybox/**, /custom-logout a /actuator/health; /api/** (stahování mo… | platí | `navigation-access` „AUTH-019 …“ (HTTP) |
| AUTH-020 | Odhlášení (GET i POST, i opakovaně) zruší session a přesměruje na odhlášení v Keycloaku s id_token_hint a post… | platí | `navigation-access` „AUTH-020 …“ (session aplikace zrušena, přesměrování s `id_token_hint` a `post_logout_redirect_uri`); konec SSO v Keycloaku záleží na konfiguraci realmu |
| AUTH-021 | Oprávnění se kontroluje před validací: role → existence → podmínka matice → tvar a doménová pravidla → podmíně… | změna ve v1 | **ne** — Změna ve v1; pořadí kontrol není z UI pozorovatelné. Neověřuje se. |
| AUTH-022 | Zamítnutí 403 říká proč: chybí role → code: ROLE_REQUIRED s params.roles; role je, ale matice pro tento objekt… | změna ve v1 | **ne** — Změna ve v1 (kódy 403 v API). Neověřuje se. |
| AUTH-023 | Vlastníka kapitoly, modelu nebo kvízu (ownerId, D6) smí změnit jen admin; creatorId zůstává; původní vlastník … | změna ve v1 | **ne** — Změna ve v1 (předání vlastnictví ve starém systému neexistuje). Neověřuje se. |
| AUTH-024 | Koncept kapitoly, kvízu a kurzu vidí jen vlastník a správce (ostatní učitelé podle Q10, do rozhodnutí ne), kon… | změna ve v1 | **ne** — Změna ve v1 (koncepty ve starém systému neexistují). Neověřuje se. |

### Kurzy (nové ve v1) (`CRS`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| CRS-001 | Kapitolu bez platného zpřístupnění vidí jen její vlastník a správce (ostatní učitelé podle Q10). Kapitola nemu… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-002 | Student smí číst publikovanou kapitolu, když má platné zpřístupnění kapitoly on, skupina, jejímž je členem, ne… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-003 | Zpřístupnění s intervalem platí jen uvnitř intervalu od–do včetně mezí; chybějící mez = bez omezení; validTo p… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-004 | Kapitola může být ve více kurzech; kurz drží jen odkaz a pořadí, kapitola se nekopíruje. Kurz určuje pořadí a … | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-005 | Kurz oprávnění nenese: jeho zpřístupnění se při uložení rozepíše na zděděná zpřístupnění kapitol kurzu. Přidan… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-006 | Student vidí kurz, když je publikovaný a smí číst aspoň jednu jeho kapitolu; nepřístupné kapitoly kurzu se mu … | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-007 | Kvíz dědí viditelnost z kapitoly: student ho vidí a spustí, jen když je kvíz publikovaný a jeho kapitolu smí č… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-008 | Skupinu se zdrojem moodle nejde ručně měnit ani smazat; mění ji jen synchronizace. Ručně se zakládají jen skup… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-009 | Role Instructor z LTI (Moodle) nedává globální roli teacher; role se dál čtou jen z tokenu Keycloaku (AUTH-001… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-010 | Učitel vidí výsledky studentů zapsaných (přímo nebo přes skupinu) v kurzu, který vlastní; zda jen u kvízů z ka… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-011 | ~~Migrace dá každé publikované kapitole zpřístupnění „všem přihlášeným“.~~ Zrušeno: nový systém startuje bez d… | zrušeno | mimo rozsah: zrušeno |
| CRS-012 | Zápis sám přístup nedává; odchod ze skupiny nebo zrušení zápisu ukončí přístup, který plynul jen z nich, výsle… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| CRS-013 | Publikování kapitoly (state → published) samo založí přímé zpřístupnění „všem přihlášeným“ bez intervalu, poku… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |

### Asynchronní operace (nové ve v1) (`OPS`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| OPS-001 | Dlouhá operace (zpracování modelu, import z Moodlu, reindex, výmaz dat) odpoví po synchronních kontrolách (opr… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| OPS-002 | Stav jde jen dopředu: queued → running → succeeded / failed / cancelled (nebo queued → cancelled); koncový sta… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| OPS-003 | Operaci vidí a sleduje jen ten, kdo ji spustil, a správce; ostatním je 404 a ve výpisu chybí. | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| OPS-004 | Zrušit jde jen operaci v queued nebo running, jejíž druh to dovoluje; zrušení uklidí, co operace stihla zapsat… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| OPS-005 | Průběh jde přes SSE: po připojení přijde aktuální stav, pak každá změna, po koncovém stavu se proud zavře; po … | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| OPS-006 | Zápis se stejným Idempotency-Key od stejného uživatele a se stejným tělem do 24 h (návrh) vrátí uloženou odpov… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |
| OPS-007 | Model ve zpracování není použitelný (nejde vložit do kapitoly ani otázky) a má processingOperationId; při náhr… | nové ve v1 | mimo rozsah: ve starém systému neexistuje |

### Audit (`AUD`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| AUD-001 | Úspěšné vytvoření, úprava a smazání kapitoly, modelu a kvízu zapíše auditní záznam s akcí, výsledkem success, … | platí | **ne** — Auditní záznam je v logu aplikace, ne v UI. JUnit `QuizBackendServiceTest`. |
| AUD-002 | Odevzdání kvízu zapíše záznam submit / success s druhem quizResult, id výsledku a názvem kvízu. | platí, netestováno | **ne** — Log, ne UI. Netestováno. |
| AUD-003 | Každý auditní záznam nese event.dataset = "mish.audit", akci a výsledek malými písmeny, user.id (sub, ne jméno… | platí, netestováno | **ne** — Log, ne UI. Netestováno. |
| AUD-004 | Požadavek, který nic nezměnil, se do auditu jako úspěch nezapíše. | platí | **ne** — Log, ne UI. JUnit `QuizBackendServiceTest`. |
| AUD-005 | Zamítnuté pokusy se zapisují s výsledkem denied a nepovedené zápisy s failure (smazání kapitoly, úprava a smaz… | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| AUD-006 | Korelační id požadavku se převezme z hlavičky X-Request-Id, má-li nejvýš 64 znaků z A–Z a–z 0–9 . _ -; jinak s… | platí, netestováno | `navigation-access` „AUD-006 …“ (HTTP hlavička) |
| AUD-007 | Každý řádek logu v rámci požadavku nese trace.id a user.id přihlášeného (identifikátor, nikdy jméno). | platí, netestováno | **ne** — Log, ne UI. Netestováno. |
| AUD-008 | Autor se ve výpisech ukazuje jménem, nikdy svým id; obsah bez zaznamenaného jména autora autora neukazuje. | platí | `chapters` „CH-006, CH-021, AUD-008 …“; `models` „MOD-004 …“ |

### Navigace (`NAV`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| NAV-001 | Přihlášený uživatel na neexistující adrese dostane stránku 404 v rozvržení aplikace (s hlavičkou a navigací). | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| NAV-002 | Neexistující záznam na existující routě (kapitola, výsledek kvízu, …) ukáže srozumitelnou hlášku, že záznam ne… | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| NAV-003 | Odepřený přístup vrací 403 se stránkou „Přístup odepřen“, neočekávaná chyba 500. | platí | `student-no-teacher-routes`, `auth-role-guard` (stránka „Přístup odepřen“); stav 500 vyžaduje chybu serveru, JUnit `ErrorViewsTest` |
| NAV-004 | Detail bez id v adrese přesměruje na příslušný výpis. | platí | `navigation-access` „NAV-004 …“ |

### Data (`DATA`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| DATA-001 | Kvíz uložený před restartem jde po restartu přečíst a upravit, aniž by služba předtím cokoli zapsala; platí pr… | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| DATA-002 | Totéž platí pro výsledky kvízů s odeslanými odpověďmi všech šesti typů. | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| DATA-003 | Druh otázky, odpovědi a odeslané odpovědi se v uložených datech pozná podle pole _class s krátkým názvem (Mult… | neplatí (bez migrace, D8) | mimo rozsah: převod dat, D8 |
| DATA-004 | Kvízy uložené před sjednocením konvencí se musí převést: correctItems z 1-based na 0-based, correctMatches z {… | neplatí (bez migrace, D8) | mimo rozsah: převod dat, D8 |

### Texty (`I18N`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| I18N-001 | Aplikace je jen česky; parametry se do textu doplní. | platí | `models` „MOD-012, I18N-001 …“ (parametr v textu dialogu); `accessibility` (jazyk stránky) |
| I18N-002 | Každý klíč použitý v kódu má český text; uživatel nikdy nevidí název klíče. | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| I18N-003 | Hlášky při neúspěšném uložení kvízu mají český text. | platí | `quiz-validation` „QZ-007 …“ (hláška serveru při neúspěšném uložení je česky) |
| I18N-004 | Počet bodů se skloňuje podle čísla: 1 bod, 2–4 body, 0 a 5 a více bodů. | bug ve starém systému | mimo rozsah: starý systém se chová jinak (bug), akceptace ho neověřuje |
| I18N-005 | Datum a čas se zobrazují jako dd.MM.yyyy HH:mm. | platí | `chapters` „CH-006, CH-021, AUD-008, I18N-005 …“ |

### Dokumentace (`DOC`)

| ID | Pravidlo (zkráceně) | Stav | e2e scénář / proč ne |
|---|---|---|---|
| DOC-001 | Záznam bez id (nebo prázdný záznam) se ignoruje. | platí | **ne** — Vnitřní čtení dat dokumentace. JUnit `DocumentationServiceTest`. |
| DOC-002 | Záznam bez rolí (null nebo []) je veřejný, i pro nepřihlášeného. | platí | **ne** — Data dokumentace nemají záznam bez rolí. JUnit `DocumentationServiceTest`. |
| DOC-003 | Záznam s rolemi vidí jen uživatel s některou z nich; prefix ROLE_ se při porovnání nebere v úvahu. | platí | `navigation-access` „DOC-003 …“ |
| DOC-004 | Role ADMINISTRATOR se považuje za ADMIN. | platí | **ne** — Realm nemá roli ADMINISTRATOR. JUnit `DocumentationServiceTest`. |
| DOC-005 | Když nejde zjistit přihlášeného uživatele, vrátí se jen veřejné záznamy. | platí | **ne** — Vyžaduje rozbitý bezpečnostní kontext. JUnit `DocumentationServiceTest`. |
| DOC-006 | Správa dokumentace (ukládání) pracuje se všemi záznamy bez ohledu na role. | platí | **ne** — Správa dokumentace nemá obrazovku. JUnit `DocumentationServiceTest`. |
| DOC-007 | Dotaz s prázdným typem vrátí nic; detail s prázdným, neexistujícím nebo nepovoleným id vrátí nic (ne chybu). | platí | **ne** — Detail záznamu podle id nemá vlastní route. JUnit `DocumentationServiceTest`. |
