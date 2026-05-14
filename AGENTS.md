# Vibe Coding Agent Instructions

Be a careful coding agent. Optimize for correctness, small diffs, safe context usage, and reproducible verification.

Use a small runtime instruction pack.
Reuse guidance already loaded in the current task unless the concern changes.

## Universal rules

- Understand the task before editing.
- Inspect existing conventions before adding patterns.
- Prefer targeted reads over broad scans.
- Prefer project-local tooling when both local and global options exist.
- Keep changes small and easy to review.
- Never overwrite unexpected user changes.
- Never dump large command output into context.
- Never claim success without relevant verification.

## Never scan by default

Avoid:

```text
node_modules, .git, dist, build, .next, coverage, venv, target,
__pycache__, .cache, .turbo, .vercel, vendor, out, bin, obj
```

## File triggers

Read `docs/agent/TOOLS.md` when:

- choosing tools
- searching, reading, diffing, or inventorying the repo
- handling routine shell output

Read `docs/agent/CONTEXT.md` only when:

- using `ctx_*`
- shell behavior is unclear
- reduced output is still too large
- the same output must be queried multiple times
- indexing or code-based analysis is needed

Read `docs/agent/CODING_BEHAVIOR.md`, `docs/agent/SPRING_CODING_GUIDE.md` when:

- implementing or modifying code
- fixing bugs
- refactoring
- changing public behavior
- making multi-file changes
- the task has ambiguous scope or multiple valid approaches

## Completion rule

After edits, run the smallest relevant verification and report:

- what changed
- what ran
- what was skipped
- remaining risk
