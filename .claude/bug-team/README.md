# Bug team (DEV → DEVCR → QA)

Tým tří agentů, který odbavuje bugy v tomto repozitáři. Hlavní session dělá orchestrátora.

```
issue ─► připravenost ─┬─ NEEDS DECISION ─► otázka na člověka (label needs-decision)
                       └─ READY ─► úroveň rizika ─► worktree
                                                        │
          ┌──────────── nálezy (přímo DEV ↔ DEVCR) ─────┤
          ▼                                             │
         DEV ──(oprava + testy + docs)──► DEVCR ──┬─ CHANGES REQUESTED
          ▲                                       └─ APPROVED ─► QA (čistý stack :8443)
          └──────────────── REJECTED ◄─────────────────────────────┤
                                                                   └─ VERIFIED ─► PR
PR ─► LIGHT/STANDARD: auto-merge po zeleném CI
   └► RISKY: label needs-human-review, merge až po schválení člověkem a zeleném CI
```

## Úrovně rizika

| Úroveň | Co mění | QA | Merge |
|---|---|---|---|
| LIGHT | texty, překlady, CSS | dotčené testy, Vitest, prohlížeč | automaticky po CI |
| STANDARD | UI logika, komponenty | celá sada, Vitest, prohlížeč, e2e | automaticky po CI |
| RISKY | backend, perzistence, bezpečnost, mazání, infra/CI | navíc restart probe | po schválení člověkem |

## Soubory

| Soubor | K čemu |
|---|---|
| `../agents/dev.md`, `devcr.md`, `qa.md` | role; DEVCR a QA kód nemění — hlídá to otisk diffu u orchestrátora (`disallowedTools` je jen pojistka navíc) |
| `../skills/bug-team/SKILL.md` | postup orchestrátora |
| `ENVIRONMENT.md` | příkazy, stacky, baseline, známé pasti |
| `bt` | jediný vstupní bod: testy, mutanty, image, QA stack, e2e, restart probe, worktree |
| `compose.qa.yml`, `restart-probe.mjs` | izolovaný QA stack a sonda po restartu |
| `local/` *(neverzováno)* | nastavení stroje (`env.sh`: JDK v Dockeru, extra CA, Playwright channel) |
| `runs/<n>/` *(neverzováno)* | zprávy rolí, snímky, mutanty jednoho bugu |

Spuštění: `/bug-team 87`, „odbav #87 týmem“, nebo víc bugů najednou (max. 3 paralelně, QA se řadí za sebou).

Nový stroj: zkopíruj `local/env.sh` podle svého prostředí (bez něj `bt` použije `./mvnw` a výchozí Playwright).
