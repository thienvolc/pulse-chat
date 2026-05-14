# Tool Routing Policy

Use no tool if the answer can be handled confidently from the current context.

Use the lightest portable tool that can answer the task correctly.

## Routing

- Direct answer: no tool.
- Exact short output: raw shell with portable commands first (`rg`, `fd`, `jq`, `yq`, `git`).
- Exact text search: raw `rg`.
- Noisy search, reduced shell output or repo discovery, shell, or test commands: `rtk rg`, `rtk fd`, `rtk rg --files`, `rtk mvn -q test`, `rtk <command>`.
- Focused source reading: `tilth`.
- Large, multi-file or multi-step repo analysis: `context-mode` (prefer javascript runtime).
- Callers, callees, dependencies, impact, or blast-radius analysis: `code-review-graph`.
- Read or filter JSON: `jq`.
- Read or filter YAML: `yq`.

## Guardrails

- Use PowerShell only for Windows-specific tasks or when portable tools are unavailable.
- Do not use `rtk` when exact raw output matters
- Do not use `context-mode` by default
- Do not use `code-review-graph` for trivial local edits.
