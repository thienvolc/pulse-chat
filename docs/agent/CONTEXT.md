# Context and Shell Policy

Use this only after choosing a context tool or `ctx_*` command.

Keep context output filtered, bounded, and queryable.

## `ctx_*` shell semantics

- Always set repo `cwd`, or use absolute repo paths.
- Treat `ctx_*` as bash-like POSIX/MSYS, not PowerShell, even on Windows.
- Prefer portable commands: `pwd`, `ls`, `cat`, `rg`, `fd`.
- If PowerShell is required, call it explicitly:
  `pwsh -NoProfile -Command "..."`
- Avoid JS regex one-liners with backslash escaping; use split/join or `functions.shell_command`.

## Output handling

- Never dump large raw output into the conversation.
- Prefer reduced, filtered, or indexed output.
- If neither `context-mode` nor `rtk` is available, use bounded output only.

PowerShell:

```powershell
command | Select-Object -First 200
```

Bash-like shells:

```bash
command | head -200
command | tail -200
```
