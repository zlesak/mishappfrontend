# Bug team — environment

Read by the orchestrator and by DEV, DEVCR and QA before they start.

## Project

- `zlesak/mishappfrontend`: Vaadin 25 + Spring Boot 4 monolith, Java 21, MongoDB + GridFS, Keycloak; infra in `infra/`.
- Tests: JUnit / Mockito / Karibu in `src/test/java`, Vitest in `src/main/frontend/js/**/*.test.ts`, Playwright in `e2e/`.
- Conventions: comments explain *why*; Czech UI texts in `src/main/resources/texts/*_cs.json`; commit messages say
  why in prose (see `git log`). Project state and known gaps: `docs/status.md`.
- `main` is protected: PR required, checks `java-tests`, `vitest`, `build`, `e2e` must pass. Repo auto-merge is on.

Baselines on `main` (2026-11-28): Java **1047** tests by Maven's summary line, Vitest **83**, Playwright
`--project=e2e` **76** + setup (acceptance suite, zlesak/mish#9; coverage in `e2e/ACCEPTANCE.md`). A full `bt e2e`
takes ~5 min locally (2 workers; the timer spec alone waits a real 2 min), ~20 min on CI. If a count differs, compare like with like — summing surefire `.txt` files misses the
`@Nested` class `SecurityConfigTest$HttpFilterChainIntegrationTest` (11 tests).

## Where things are

| Path | What |
|---|---|
| `.claude/bug-team/bt` | the toolbox — always use it instead of hand-written docker/maven/playwright commands |
| `.claude/worktrees/bug-<n>/` | one worktree per bug, branch `fix/<n>-<slug>` (git-ignored) |
| `.claude/bug-team/runs/<n>/` | reports `DEV.md`, `DEVCR.md`, `QA.md`, screenshots, probes, mutants (git-ignored) |
| `.claude/bug-team/local/` | this machine's settings `env.sh` and notes `NOTES.md` (git-ignored) |

Run `bt` from inside the worktree you work on (`<main>/.claude/bug-team/bt …` works from any worktree; `bt paths`
shows what it resolved). Throwaway scripts, probes and mutants go in the run folder, never into the repo.

## `bt` commands

| Command | Use |
|---|---|
| `bt java [TestClass …]` | Maven tests (whole suite without classes, ~10 min). Prints the summary and failing tests, full log in `runs/`. Uses JDK 21 in Docker when `local/env.sh` says so. |
| `bt java --replace <repo-path>=<file> [TestClass …]` | Same, with a repo file swapped in a throwaway copy — for mutants. Repeatable. The repo is never touched. |
| `bt vitest` | Vitest. |
| `bt image <tag>` | Build the app image `mish-app:<tag>` from the current worktree (adds the machine's extra CA if configured). |
| `bt qa-up <tag> [--wait]` | Fresh QA stack from that image on <https://localhost:8443>: own containers and volumes, **empty database**. Takes a lock; `--wait` queues behind another QA. |
| `bt e2e [args]` | Playwright against the QA stack (default `--project=e2e`). |
| `bt restart-probe` | Restart the QA app, then open the first chapter, model and quiz written by the previous process. Exit 1 if any shows an error. |
| `bt qa-restart-app` | Just the restart (app + gateway). |
| `bt qa-down` | Remove the QA stack and its data, release the lock. Always run it when QA is done. |
| `bt qa-status` | Who holds the QA stack. |
| `bt node <script.mjs>` | Run your own Node/Playwright script (kept in `runs/<n>/qa/`) with the machine settings and `QA_URL`. |
| `bt wt-new <n> <slug>` / `bt wt-rm <n>` | Worktree per bug (orchestrator only). |

## The running stacks

- **Developer stack** on <https://localhost> (443) from `infra/` with `.env` — the user's. Do not rebuild, restart
  or put test data into it.
- **QA stack** on <https://localhost:8443>, project `mish-qa`, managed only through `bt qa-*`.
- Test realm users on both: teacher `alice` / `password`, student `bart` / `password`.
- Fixture data from the e2e `setup` project: "E2E Fixture Model", "E2E Fixture Kapitola", "E2E Fixture Kvíz" (one
  open-text question, accepted answer `správná odpověď`). On an empty QA stack, run `bt e2e` (or the setup project)
  first if you need them.

## Known traps

- **#96** — after an app restart, chapter detail and quizzes fail to load ("Failed to instantiate … Class is
  abstract") for the rest of the process, unless a quiz was written before anything read one. `bt restart-probe`
  shows it. Until #96 is fixed, a restart-probe failure on *chapter* and *quiz* with that message is the known bug,
  not a regression — any other failure is.
- The gateway (nginx) resolves the app once at start; `bt` restarts it after app restarts. Do the same if you ever
  recreate the app container by hand.
- A wrong Keycloak password in Playwright looks like a timeout (#61).
- CI runs e2e with **2 workers** and longer CI-only timeouts (`playwright.config.ts`, `ciScaled` in the helpers): the 4-vCPU
  runner also hosts the whole stack and renders 3D in software. Watch the report for **flaky** tests, not only green
  (SRC-015 was flaky on the first green run, #108). CI traces are overwritten by the usability step (#107).
- On the QA stack (8443) Keycloak rejects the post-logout redirect (`https://localhost:8443` is not in `dev-client`),
  so the SSO session survives logout; AUTH-020 checks only the app side.
