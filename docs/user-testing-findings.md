# Nálezy z automatizovaného průchodu

Zdroj: `e2e/usability.spec.ts` spuštěný proti čerstvě nasazenému stacku (`infra/docker-compose.yml`),
plán viz [user-testing-plan.md](user-testing-plan.md). Surová data se zapisují do
`test-results/usability-report.md`, snímky obrazovek do `test-results/usability/`.

## Naměřené hodnoty

| Úloha | Role | Cíl | Dokončeno | Čas (s) | Interakcí |
|---|---|---|---|---|---|
| T1 | vyučující | Nahrát 3D model s texturami a CSV | ano | 20,8 | 9 |
| T2 | vyučující | Vytvořit kapitolu s modelem | ano | 6,8 | 5 |
| T4 | vyučující | Najít výsledky studentů | ano | 0,9 | 0 |
| T5 | vyučující | Najít kapitolu ve výpisu | ano | 2,2 | 2 |
| S1 | student | Najít a otevřít kapitolu | ano | 2,3 | 1 |
| S2 | student | Zobrazit 3D model kapitoly | ano | 0,2 | 0 |
| S3 | student | Najít seznam kvízů | ano | 0,7 | 0 |
| S4 | student | Najít vlastní výsledky | ano | 0,7 | 0 |

Všech 8 úloh dokončeno. Studentské úlohy jsou pod 1,5 s a stojí nejvýše jedno kliknutí — ta část
aplikace je rychlá a přímočará. Veškerá zátěž je na straně vyučujícího.

Čas se měří až po instalaci počítadla kliknutí, takže první úloha neplatí navíc jedno načtení
stránky; a počet interakcí se zapisuje i u úlohy, která se nedokončí, aby se selhání nedalo splést
s „nevyžadovalo žádné kliknutí".

## Nálezy

### 1. Nahrání modelu je nejdražší úkon aplikace — *opraveno*

T1 trvá **21 s a stojí 9 interakcí**, tedy zhruba tolik jako všech sedm zbývajících úloh dohromady.
Příčina je v tom, že každý typ souboru má vlastní tlačítko a vlastní dialog: model, hlavní textura,
další textura, CSV. Vyučující musí čtyřikrát projít výběrem souboru a pokaždé počkat na náhled.

*Oprava:* nad jednotlivými sekcemi je jedna plocha, kam se dá přetáhnout všechno najednou; soubory se
zařadí podle přípony (`.obj`/`.glb` → model, `.jpg` → hlavní textura, pokud ještě žádná není, jinak
další, `.csv` → mapa oblastí). Sekce zůstaly, aby se dalo zařazení opravit a jednotlivé soubory
vyměnit; druhý model se odmítne se zprávou místo aby přepsal první.

### 2. Výsledek kvízu nemá vlastní adresu — *opraveno*

Po odeslání se výsledek vykreslí na místě, na adrese `/playQuiz/{id}`. Student si výsledek nemůže
uložit do záložek ani poslat odkaz, a tlačítko Zpět v prohlížeči jej vrátí do rozpracovaného kvízu,
který už nelze odeslat. Route `/quiz-result/{id}` přitom existuje a používá ji výpis pokusů.

*Oprava:* po odeslání se přejde na `/quiz-result/{id}`, tedy na tu samou adresu, kterou používá výpis
pokusů. Pokud by výsledek přišel bez id, vykreslí se na místě — přijít o právě získaný výsledek by
bylo horší než mít ho na nesdílitelné adrese.

Tento nález vyšel najevo při opravě testu `student-access`, který na změnu adresy čekal.

### 3. Prázdná aplikace nemá kudy začít — *opraveno*

Na čerstvé instalaci nelze vytvořit kapitolu: dialog „Vybrat model" je prázdný, bez vysvětlení
a bez cesty dál. Uživatel musí sám uhodnout, že nejdřív musí do jiné sekce nahrát model.

Dokládá to i testovací sada: dokud jsem nedoplnil `e2e/fixtures.setup.ts`, který si model a kapitolu
předem vytvoří, polovina scénářů nad prázdnou databází spadla přesně na tomto místě.

*Oprava:* prázdný dialog vysvětlí, proč je prázdný, a nabídne tlačítko, které vede rovnou na
příslušný formulář — „Nahrát nový model“ u modelů, „Vytvořit kapitolu“ u kapitol.

### 4. Odebraný model se občas vrátil do 3D scény — *opraveno*

Ve formuláři pro model po odebrání souboru `.obj` model občas zůstal ve 3D scéně. E2e test
`model-assets-visibility` na tomto kroku ve třech ze čtyř běhů selhal a v jednom prošel.

Příčiny byly dvě, obě v prohlížeči:

1. **Model zůstal zobrazitelný.** Odebrání ze scény záměrně ponechává model zaregistrovaný — na tom
   stojí otázky kvízu, které model schovají a zase ukážou. Při smazání souboru to ale neplatí: model
   zůstal v registru i s načteným blobem v cache, takže ho následující požadavek na zobrazení znovu
   načetl a smazaný model se objevil zpátky.
2. **Závod s načítáním.** Zobrazení modelu nejdřív vyprázdní scénu, pak čeká na síť a parser a
   teprve potom výsledek přidá. Načítání spuštěné před odebráním souboru tak stihlo svůj výsledek
   přidat až po něm.

*Oprava:* smazání souboru navíc ruší registraci modelu i jeho cache, a každý požadavek na zobrazení
nese číslo generace — když se mezitím obsah scény změnil, dokončené načítání už nic nepřidá.
Pokryto dvěma jednotkovými testy, které bez opravy padají.

### 5. Hledání nesahalo do obsahu, a poté vracelo všechno — *opraveno*

Vyhledávací pole u výpisu kapitol původně porovnávalo pouze název, takže „najdi kapitolu o mozku"
fungovalo jen tehdy, když uživatel trefil název. Rozšíření na obsah ale přineslo opačný problém:
dotaz šel do Mongo přes `$text`, který matchuje **kteroukoli** shodu slova z dotazu. Stačilo, aby se
v dotazu objevilo běžné slovo („kapitola"), a výsledkem byl celý seznam. Slučovací větev navíc při
nálezu v obsahu přestavovala dotaz od nuly, čímž zahodila i filtr na název.

Projevilo se to až na databázi s víc než jednou stránkou kapitol — T5 kvůli tomu v jednom běhu
nedokončilo (61 s místo 1,7 s), protože hledání konkrétní kapitoly vrátilo úplně všechny.

*Oprava:* dotaz se vyhodnocuje jako „všechna zadaná slova musí být přítomna", zadaný text se bere
doslova (ne jako regulární výraz) a ostatní filtry — autor, časové rozmezí — zůstávají v platnosti.

### 6. Smazání modelu tiše rozbíjelo kapitoly — *opraveno*

Model šlo smazat i tehdy, když jej používala kapitola; ta se pak vykreslila rozbitá. Nově je mazání
odmítnuto se zprávou, která kapitoly vyjmenuje.

## Co automat neověří

Čísla výše říkají, kolik kroků úloha stojí, ale ne, jestli uživatel ví, **který** krok udělat.
Nálezy 1 a 3 byly přesně toho druhu, kde měření jen potvrzuje podezření; opravy vycházejí z toho, co
šlo z čísel odvodit, ale skutečnou váhu jim dá až sezení s pěti vyučujícími podle plánu
v [user-testing-plan.md](user-testing-plan.md). Doporučuji je uspořádat před obhajobou; každé sezení
trvá 45 minut a odhalí i pojmy, které jsou pro uživatele nesrozumitelné (například „Pokročilý
model“).

Zejména u nálezu 1 platí, že měření po opravě samo o sobě nestačí: automat přetáhne všechny soubory
jedním voláním a naměří tak dvě interakce, ale jestli vyučující tu plochu najde a jestli mu zařazení
podle přípony přijde srozumitelné, řekne jen člověk.
