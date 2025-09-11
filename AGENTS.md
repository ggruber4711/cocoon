# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Maven project. Core directories:
  - `core/` — runtime, including `core/cocoon-webapp` (Jetty dev webapp).
  - `blocks/` — feature blocks (often split into `*-api`, `*-impl`, and `*-sample`).
  - `commons/` — shared resources and legal notices.
  - `dists/` — distribution assemblies.
  - `tools/` — helper scripts and utilities.
  - `site/` — site and docs sources.
- Module layout follows Maven conventions: `src/main/java|resources` and `src/test/java`.

## Build, Test, and Development Commands
- Build all modules: `./build.sh install` (uses profile `allblocks`).
- Clean and rebuild: `./build.sh clean install`.
- Skip tests during build: `./build.sh notest install`.
- Run the webapp locally (Jetty on 8888): `./cocoon.sh`.
- Debug server (attach to `localhost:5005`): `./cocoon.sh debug`.
- Eclipse project files: `./build.sh eclipse:clean eclipse:eclipse`.
- IntelliJ IDEA: Open the root `pom.xml` (no extra generation needed).

## Coding Style & Naming Conventions
- Java 8–11, Maven 3+. Use 4-space indentation, UTF-8, Unix newlines.
- Packages under `org.apache.cocoon.*`; avoid new top-level packages.
- Module naming pattern where applicable: `cocoon-<area>-api`, `cocoon-<area>-impl`.
- Prefer SLF4J logging; avoid `System.out`/`System.err`.
- XML/POM files: 2-space indent; keep formatting minimal and consistent.

## Testing Guidelines
- Framework: JUnit (mix of JUnit 3-style `TestCase` and newer styles).
- Location: `src/test/java` within each module.
- Run tests: `mvn -P allblocks test` or as part of `./build.sh install`.
- Naming: suffix unit tests with `*Test`; keep tests near the code they cover.
- Mock external integrations; add regression tests for fixes; aim to maintain or improve coverage.

## Commit & Pull Request Guidelines
- Commit summary: imperative and scoped, e.g., `core: fix NPE in sitemap resolver`.
- Reference related issue IDs where applicable.
- PRs should be focused, with a clear description, rationale, and test notes.
- Include screenshots or before/after notes for behavior changes.
- Ensure `./build.sh install` passes before requesting review; avoid unrelated formatting-only changes.

## Security & Configuration Tips
- Memory: adjust `MAVEN_OPTS` if needed (scripts default to `-Xmx512m`).
- Mirrors: not required; see `settings.xml` for optional customization.
