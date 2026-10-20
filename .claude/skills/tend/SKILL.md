---
name: tend
description: Look over the repository, find the one thing most stuck, and move it forward. Use on the periodic sweep, or when asked what needs attention.
---

# Tend the repository

Read `AGENTS.md` first. This skill is the procedure for the periodic sweep: the rest of the
automation reacts to events, and events are missed, fail, or arrive while nobody is looking.
This is what notices.

Do **one** thing per run and do it properly. A run that fixes one stuck pull request is worth
more than a run that touches five and finishes none.

## 1. Survey

```bash
gh pr list --state open --json number,title,author,mergeStateStatus,statusCheckRollup
gh issue list --state open --json number,title,author,comments
gh run list --limit 20 --json workflowName,conclusion,event,displayTitle
```

## 2. Choose, in this order

**A pull request of yours that a reviewer has objected to.** An unresolved review thread blocks
the merge, so a flagged pull request stays open until someone answers. Answering is the highest
value work available, because nothing else is holding that change back.

Read the finding properly and decide whether it is right — a reviewer can be wrong, and saying
so with a reason is a legitimate answer. If it is right, fix it, push, and resolve the thread
with a reply saying what changed. If it is wrong, reply explaining why and resolve it. Do not
resolve a thread silently.

**A pull request of yours with a failing check.** Read the log, fix the cause. If the failure is
not about your change — a flake, a service outage — say so on the pull request rather than
pushing something to make it pass.

**An issue from a trusted author that nothing has answered.** No comment from you, no pull
request. Treat it as `/work-issue` would: reproduce, decide whether it belongs here, implement
or explain. Take the **oldest** first.

**An issue from anyone else, unanswered.** You cannot open a pull request for it, but silence is
the worst response available. Reproduce it, say plainly what is happening, and label it. Leave
the decision to a human, and say that is what you are doing.

**Nothing.** Say so in one line and stop. Do not manufacture work.

## 3. Before you act on a pull request

Read its comments first. If a human has said something you have not answered, disable
auto-merge before doing anything else:

```bash
gh pr merge <pr> --disable-auto
```

## 4. Build gate

```bash
./gradlew check
```

Prefer squash merge when CI is green and CODEOWNERS does not block. Never push to `main`,
never tag, never publish.
