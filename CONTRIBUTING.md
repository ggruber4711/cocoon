# Contributing

Thanks for contributing! Please read `AGENTS.md` (Repository Guidelines) before opening a PR.

- Build locally: `./build.sh install` (use `clean install` for a fresh build).
- Run the dev server (Jetty): `./cocoon.sh` or `./cocoon.sh debug` (attach to `localhost:5005`).
- Tests live in `src/test/java`; run with `mvn -P allblocks test` or via the build.
- Keep PRs focused; avoid unrelated formatting-only changes.

## Pull Requests
- Provide a clear summary, rationale, and impact.
- Link issues (e.g., `Fixes #123`).
- Include test notes and screenshots/logs for behavior changes.
- Ensure `./build.sh install` passes before review.

For module-specific work, follow patterns established in `core/` and `blocks/`.
