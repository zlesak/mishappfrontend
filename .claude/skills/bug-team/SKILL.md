---
name: bug-team
description: Orchestrate the DEV → DEVCR → QA agent team on GitHub bugs in zlesak/mishappfrontend — readiness check, risk tier, worktree per bug, DEV fixes test-first, DEVCR reviews independently, QA verifies on a fresh isolated stack, findings loop until fixed/approved/verified, then PR merged only after CI (and a person, for risky changes). Use when the user asks to have the team handle, fix, odbavit or process one or more bugs/issues (e.g. "/bug-team 87", "odbav #88 týmem", "pusť tým na #87 a #93").
---

# Bug team orchestration

You are the **orchestrator**. The agents `dev`, `devcr` and `qa` (`.claude/agents/`) do the fixing, reviewing and
testing. You decide whether and how a bug is worked on, set up the worktree, connect the agents, keep the user
informed, and own git and GitHub.

Tools and commands: `.claude/bug-team/ENVIRONMENT.md` and the script `.claude/bug-team/bt` (run `bt help`).
Machine quirks: `.claude/bug-team/local/NOTES.md` if present.

## 0. Readiness — is this a bug the team may fix on its own?

Read the issue and the code it points at. Classify:

- **READY** — the expected behaviour is clear from the issue, the docs or the rest of the app.
- **NEEDS DECISION** — fixing it means choosing behaviour someone should own: what users may see (e.g. show correct
  quiz answers?), enabling vs removing a feature, a security or privacy trade-off, a data-model change, anything
  `docs/status.md` lists as a deliberate decision.

NEEDS DECISION → do not start. Ask the user with concrete options (and your recommendation), label the issue
`needs-decision`, and move on to the next bug. Start only after the user has chosen; put the choice in the brief.

Also skip (and say why) a bug that cannot be done here: missing assets nobody has, needs credentials, already fixed
or in an open PR (`gh pr list --search "<n>"`).

## 1. Risk tier

| Tier | Typical changes | DEVCR | QA | Merge |
|---|---|---|---|---|
| **LIGHT** | texts, translations, CSS/layout, comments, no logic | yes | `light` | auto after CI |
| **STANDARD** | UI logic, components, views, front-end behaviour | yes | `full` | auto after CI |
| **RISKY** | backend services, persistence/Mongo mapping, security config, deletion paths, data migrations, Docker/CI/infra | yes | `full` + restart probe | **a person approves**, then auto after CI |

When unsure, take the higher tier. If DEV's change turns out to touch RISKY areas, raise the tier.

Tell the user in one line: bug, tier, why.

## 2. Prepare

```bash
.claude/bug-team/bt wt-new <n> <short-slug>
```

That creates `.claude/worktrees/bug-<n>` on branch `fix/<n>-<slug>` from `origin/main`, installs node modules and
the run folder `.claude/bug-team/runs/<n>/`. Save the issue there:

```bash
gh issue view <n> -R zlesak/mishappfrontend --json number,title,body -q '"# #\(.number) \(.title)\n\n\(.body)"' > .claude/bug-team/runs/<n>/ISSUE.md
```

## 3. DEV

Spawn `dev` in the background with: issue number, tier, worktree path, run folder, a two-line summary of the bug,
the files it points at, what "fixed" looks like, and the user's decision if there was one.

## 4. DEVCR

When DEV reports done, check `git -C <worktree> diff --stat` yourself (expected files only), then spawn `devcr`
with issue, tier, worktree, run folder — **not** DEV's conclusions.

**Hands-off check.** DEVCR and QA must not change the code under review, and a tool ban alone cannot guarantee it
(`Bash` can still write). After each DEV round record
`git -C <worktree> diff HEAD | shasum` and `git -C <worktree> status --porcelain | shasum`; when DEVCR or QA
finishes, both must be unchanged. If they changed, stop: show the user the difference and send the round back.

**Direct channel.** As soon as DEVCR runs, send each agent the other's agent id:
"DEV is `<id>`" / "DEVCR is `<id>`". They may message each other directly to clarify a finding or a design
question; verdicts and reports still go to you. A message to a finished agent resumes it.

- `VERDICT: CHANGES REQUESTED` → tell DEV which findings to address (BLOCKER/MAJOR always, MINOR your call,
  out-of-scope never) and resume DEVCR for the next round when DEV is done.
- `VERDICT: APPROVED` → QA.

## 5. QA

Spawn `qa` with issue, tier, worktree, run folder, QA mode (`light` / `full` / `full+restart`) and the
user-visible scenario to reproduce. QA works on its own fresh stack (`bt qa-up`, port 8443); the QA stack is shared
by all bugs, `bt` serialises it with a lock.

- `QA VERDICT: REJECTED` → defect to DEV; after the rework DEVCR reviews the delta, then QA re-verifies.
- `QA VERDICT: VERIFIED` → land it.

**Round limits:** 3 DEV↔DEVCR rounds, 2 QA rounds. Beyond that stop and ask the user — repeated findings mean the
approach is wrong.

## 6. Land

1. In the worktree: `git status` shows only what the fix needs; read the diff once yourself. Generated files and
   test output must not be in it.
2. Commit in the repo's style (why in prose, what review caught, `Fixes #<n>`) with the attribution line from the
   system reminder. Push.
3. `gh pr create` — problem / change / tests / verification with real numbers / not verified / found along the way;
   state the tier.
4. Merge:
   - LIGHT, STANDARD: `gh pr merge <pr> --auto --merge`. `main` requires `java-tests`, `vitest`, `build`, `e2e`,
     so GitHub waits for green CI. Confirm the PR is `BLOCKED` with auto-merge set, not already `MERGED`.
   - RISKY: label `needs-human-review`, **no auto-merge**. Ask the user to review the PR (link it). Only after their
     explicit yes: `gh pr merge <pr> --auto --merge`.
5. Wait for CI with a background `until` loop. Red CI → the failing job's log to DEV, same loop as review findings.
6. After the merge: `.claude/bug-team/bt wt-rm <n>`, delete the local branch, `git -C <main> pull --ff-only`.

## 7. Follow-ups

Out-of-scope findings from DEVCR or QA: confirm each in the code, then file real ones as issues (`bug` +
`java`/`javascript`) in the style of existing issues — symptom, why, why it matters, what to do, where found. Search
first (`gh issue list --state all`), comment on an existing issue instead of duplicating it.

## Several bugs

- Bugs run in parallel, each in its own worktree with its own DEV and DEVCR. Never put two bugs in one worktree.
- QA runs are serialised by the `bt` lock; a second QA simply waits (`bt qa-up <tag> --wait`).
- Run at most 3 bugs at once — every agent costs tokens and the machine runs every Java suite and stack.
- Bugs touching the same files go one after another; the second starts from `main` after the first merged.

## Keeping the user informed

- Agents send you progress messages. Pass the meaningful ones on in a line or two as they come (phase started, test
  numbers, findings, verdicts); drop noise.
- Never call a phase done before its agent's final report says so; never predict results.
- At the end per bug: tier, a short table of rounds and verdicts with key numbers, what review/QA caught, what is not
  verified, PR link and CI/merge state, issues filed.
