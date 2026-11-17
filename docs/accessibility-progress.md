# Přístupnostní zadání — stav rozpracování

Zdroj: *Implementační zadání · MISH APP frontend — Úpravy UI pro soulad s WCAG 2.2 AA a sjednocení
na komponentách Vaadin 25* (projekt „UI Mish design úpravy“).

Zadání se dělí na osm skupin. Doporučené pořadí commitů podle zadání bylo
**G → N → L + A → Q → M → V**; v tomto pořadí je také hotové, každá skupina jedním commitem.

## Hotovo

### G · Globální

| Úkol | Co bylo | Co je |
|---|---|---|
| **G-1** Jazyk dokumentu | `<html>` bez atributu | `<html lang="cs">` |
| **G-2** Barevné tokeny | Chybová červená 4,0 : 1, úspěch 3,6 : 1, primární v tmavém režimu 2,3 : 1 | `#C4141A`, `#1C6B45`, `#FF8F95` — všechny nad 4,5 : 1 |
| **G-3** Indikátor zaměření | `outline: 0` + poloprůhledný stín ≈ 2,3 : 1 | Neprůhledný rámeček 3 px v barvě textu, `outline-offset: 2px` |
| **G-4** Skip link a orientační body | Žádný skip link, chybí `<main>` | Skip link jako první prvek, obsah v `<main id="obsah">`, `<nav>` s `aria-label` |
| **G-5** Jména ikonových tlačítek | Jen `title` | `aria-label` na spouštěči mobilní navigace, na šipkách stránkování a na celém ovládání modelu |
| **G-6** Oznamování stavu | Nikde `setAssertive` | Chyby a varování asertivně, úspěch a info zdvořile |
| **G-6** *(vedlejší nález)* | `new ErrorNotification(...)` místo `this(...)` | Opraveno — dosud byla **každá jednoargumentová chybová hláška neviditelná** |
| **G-7** Lišta cookies | Jediné tlačítko „Rozumím“ | Rovnocenné „Přijmout“ / „Odmítnout“, fokus se přesune do lišty, odmítnutí se respektuje |

### N · Sdílené komponenty

`PageHeader` (H1 + živá oblast pro počet výsledků + slot pro akci), `EntityRow` (řádek místo
dlaždice), `ListingToolbar` (tři ovládací prvky s popisky) a `StatusBadge`, který se bez slova
nenechá vytvořit.

### L + A · Seznamy a administrace

Mřížka dlaždic se změnila na `<ul>` s řádky. Dlaždice byla v pětisloupcové mřížce široká asi 240 px,
takže se v ní vše zkracovalo výpustkou a karta kapitoly potřebovala `ResizeObserver`, který jen
počítal, kolik názvů modelů se vejde. Kapitola dnes ukazuje náhled hlavního modelu, u modelu, kvízu
i kapitoly se zobrazuje autor, datum vytvoření a datum úpravy, modely v kapitole jsou skutečné
odkazy (dříve klikatelné `Span`y, kam se klávesnice nedostala) a stránkování má jméno, popsané
šipky, cíle 44 px a aktuální stranu označenou `aria-current` místo `disabled`.

Administrace má vlastní H1, počty v záložkách a oznámení, když se změní hlavní akce.

### Q · Kvízy

Parametry kvízu jsou `<dl>`, otázka je `<fieldset>` s `<legend>`, který nese i pořadí („Otázka 6
z 12“) a body. Časovač se přestěhoval z plovoucí vrstvy do statické hlavičky vedle ukazatele postupu,
odpočítává po minutách (živá oblast měnící se každou sekundu čte odečítač nepřetržitě) a varuje
minutu před koncem, ne pět sekund. Prodloužení času se nezavádí — časový limit u zkoušení je výjimka
*Essential* kritéria 2.2.1. Před odesláním se vypíšou nezodpovězené otázky s odkazy na ně; druhý
stisk odešle i tak, protože nechat otázku prázdnou je legitimní volba. Otázku s klikáním do textury
lze poprvé zodpovědět klávesnicí.

### M · Úvodní stránka

Tři GIFy (13 MB) jsou nahrazené videem (2,9 MB), které se stahuje a spouští teprve při doscrollování
a jde zastavit vlastním ovládáním prohlížeče. Hierarchie nadpisů už nepřeskakuje úroveň, dlouhé
odstavce nejsou do bloku, sekundární tlačítko má okraj a patička odkazuje na prohlášení
o přístupnosti.

### V · Kapitola a 3D prohlížeč

Kamera se ovládá klávesnicí (šipky, +/−, R) přes `CameraActions`, které používá i ovládací panel, aby
se obě cesty nerozešly. Canvas má `role="application"`, `tabindex="0"` a jméno podle zobrazeného
modelu. Panel má jména u všech tlačítek, cíle 44 px, neprůhledné pozadí a popisky navázané na svá
pole. Detail kapitoly má konečně nadpis; dosud byl název jen v needitovatelném textovém poli.

### Z · Ověření

**Z-1** je hotové: `e2e/axe.spec.ts` projede všech deset rout v obou barevných režimech s nulovou
tolerancí pro `serious` a `critical`. Běží jako vlastní projekt `a11y`, ne uvnitř funkční sady — axe
hodnotí i vnitřek Vaadin komponent, takže jeho nález nemusí být vždy náš, a míchat obojí by
znamenalo, že jeden zakryje druhý.

První běh našel tři nálezy a všechny byly naše: chybějící popisky u časového limitu kvízu, u výběru
kapitoly a typu otázky (měly jen `setHelperText`, což není přístupné jméno) a kontrast textu ve
výzvě k přetažení souboru. Opraveno.

## Rozhodnutí, která zadání nechávalo na zadavateli

**Způsob vykreslení seznamů (kapitola 1).** Zvolena varianta **B — řádky**, tedy výchozí volba
zadání.

**G-7 — zrušit lištu, nebo doplnit odmítnutí?** Aplikace ukládá jen dvě cookies: session
s přihlášením a `themeMode`, který se zapíše až když uživatel sám přepne motiv. Obojí spadá pod
„nezbytné, resp. výslovně vyžádané uživatelem“, takže by šlo lištu zrušit úplně. Zvolen menší
zásah — lištu ponechat a doplnit jí skutečnou volbu.

**V-3 — textová alternativa modelu.** Jediné místo v zadání, které by rozšířilo datový model.
Zadavatel ji vyřadil, takže není implementovaná. Je uvedená jako výjimka v prohlášení
o přístupnosti.

## Co zůstává mimo repozitář

**Přihlašovací obrazovka patří Keycloaku.** `components/buttons/LoginButton.java` jen přesměruje na
`/oauth2/authorization/keycloak`; v repozitáři není žádný login view ani vlastní Keycloak téma.
Popisky, chybové hlášky a jejich kontrast se tedy neopravují ve Vaadin kódu — je k tomu potřeba
nasadit přístupné Keycloak téma. Prohlášení o přístupnosti podle EAA se vztahuje i na identity
provider, takže tento bod nelze vynechat; je proto uvedený jako výjimka.

**Ruční ověření Z-2 až Z-4.** Průchod klávesnicí je pokrytý automatizovaně u 3D scény a ovládacího
panelu (`e2e/accessibility.spec.ts`), plný ruční průchod odečítačem (NVDA nebo VoiceOver) a při 400%
přiblížení je stále na zadavateli. Rozsah, který ověřen nebyl, je popsaný v prohlášení
o přístupnosti, aby prohlášení netvrdilo víc, než je změřeno.
