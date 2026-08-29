# Apache Cocoon — Jakarta EE 10 fork

A fork of Apache Cocoon 2.3 migrated to **Jakarta EE 10**, for applications that embed
Cocoon alongside Spring and Hibernate and need to move to Spring 6 / Hibernate 6 / JDK 17.
That move forces Jakarta EE 10 and Servlet 6.0 on the whole stack, and Cocoon is the one
common component in it with no upstream Jakarta release.

There is no mixed mode: a single webapp cannot host `javax.servlet` and `jakarta.servlet`
against one container. Every part of such an application has to flip together, so a
Jakarta-capable Cocoon has to exist before the rest can move.

The full plan, the current state and the open issues are in
**[plans/jakarta-ee10-fork.md](plans/jakarta-ee10-fork.md)**. Applications coming from Cocoon 2.2
should also read **[plans/artifact-delta-2.2-to-2.3.md](plans/artifact-delta-2.2-to-2.3.md)**,
which maps the artifact changes between 2.2 and 2.3 — renames, removals, and two changes that
fail silently.

## Status

| | |
|---|---|
| Baseline | Jakarta EE 10 — `jakarta.servlet-api` 6.0.0, Spring 6.1.10, JDK 17 |
| Runtime | Jetty 12 (`org.eclipse.jetty.ee10`) |
| Default build | 79 modules, 319 tests, green |
| With `-P samples` | 102 modules, green; demo webapp boots and serves |
| Phases 0–4 of the plan | done |
| Phase 5 (publish, wire into the consuming application) | not started |

## Building

Requires **JDK 17** and Maven 3. Note that `./mvnw` is currently broken on macOS
(the wrapper's `wget` fails on a missing `libunistring`), so use a system Maven.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean install
```

**Always build clean.** Non-clean builds have previously packaged stale Eclipse (ECJ)
output from `target/classes` into jars; those jars carry `Unresolved compilation problems`
and break consumers with errors that look like Jakarta breakage but are not. Eclipse
project files are git-ignored for the same reason. Do not run `mvn eclipse:eclipse` in a
tree you then build from without cleaning.

Use `-DskipTests`, never `-Dmaven.test.skip=true`: the latter also skips test-jar
creation, and several modules — and downstream consumers — depend on `cocoon-*:test-jar`.

## What is in the build

The default reactor is **only the artifact set this fork ships**, chosen from what a real
consuming application actually loads. Everything else is in a profile.

- **default** — the migrated artifacts, listed below.
- **`-P samples`** — the sample blocks, the demo webapp and the distribution assemblies.
  Only needed to run or verify the demo.

### Migrated blocks

All of these build clean on Jakarta EE 10 and are published by this fork.

| Block | Modules | Notes |
|---|---|---|
| `cocoon-ajax` | `-impl` | |
| `cocoon-apples` | `-impl` | |
| `cocoon-auth` | `-api`, `-impl` | |
| `cocoon-axis` | `-impl` | SOAP. Builds against the transformed Axis 1.4 in `jakarta-shims/`; see the note below. |
| `cocoon-batik` | `-impl` | SVG. `SVGBuilder` was fixed to seed Batik's default namespace, without which unprefixed SVG failed to transcode. |
| `cocoon-flowscript` | `-impl` | |
| `cocoon-fop` | `-impl` | `cocoon-fop-ng` is *not* migrated. |
| `cocoon-forms` | `-impl` | |
| `cocoon-linkrewriter` | `-impl` | Needed by `cocoon-servlet-service-components`, not only by samples. |
| `cocoon-mail` | `-impl` | Moved to `jakarta.mail-api` + `jakarta.activation-api`. API only: supply an implementation at runtime. |
| `cocoon-poi` | `-impl` | |
| `cocoon-serializers` | `-charsets`, `-impl` | |
| `cocoon-template` | `-impl` | |

Core, all migrated: `cocoon-core`, `cocoon-configuration-api`, `cocoon-util`,
`cocoon-jnet`, `cocoon-container`, `cocoon-block-deployment`, `cocoon-spring-configurator`,
`cocoon-store`, `cocoon-thread`, `cocoon-xml` (`-api`, `-impl`, `-resolver`, `-util`),
`cocoon-pipeline` (`-api`, `-impl`, `-components`), `cocoon-sitemap` (`-api`, `-impl`,
`-components`), `cocoon-expression-language` (`-api`, `-impl`), `cocoon-servlet-service`
(`-impl`, `-components`).

The Axis block is the one entry that is not self-contained: Apache Axis 1.4 has no Jakarta
release, so `jakarta-shims/axis-jakarta` republishes it with the namespace rewritten. The
JAX-RPC API it needs keeps its `javax.xml.rpc` packages on purpose — that specification was
dropped from Jakarta EE and never renamed.

`legacy-blocks/` holds the 43 blocks that are **not migrated and do not compile**. They are
outside the reactor entirely — a separate directory rather than a Maven profile, so the
status is visible without reading a POM. Several can never move: `javax.portlet` has no
Jakarta equivalent, and JAX-RPC and JDO were never part of Jakarta EE. See
[legacy-blocks/README.md](legacy-blocks/README.md).

### Expression languages

JEXL 3 is available as the `jexl3` expression language, registered alongside the existing
JEXL 1 based `jexl`. JEXL 1 has no conditional operator — `a ? b : c` does not parse, because
`?` is not a token in its grammar — so conditionals had to be written as `<jx:choose>`. `jexl3`
brings the ternary and `?:`.

Nothing switches over automatically: expression languages are selected per expression and the
JXTemplate default is JXPath, so existing templates keep using JEXL 1 until they are changed.
The `jexl3` defaults are tuned to JEXL 1 semantics rather than JEXL 3's, including turning off
the JEXL 3.3 sandbox, which otherwise denies access to application beans *silently*. The
reasoning and the measurements are in
[plans/artifact-delta-2.2-to-2.3.md](plans/artifact-delta-2.2-to-2.3.md).

`jakarta-shims/` republishes third-party jars that have no Jakarta release, with the
namespace rewritten by Eclipse Transformer — currently Apache Axis 1.4 and the JAX-RPC API.

`jakarta-verify/` ships no code. It depends on the whole artifact set and asserts things no
single module can check: that no jar carries its own copy of a renamed EE spec package,
that only reviewed jars even reference one, and that no shipped Spring XML still names
`javax.servlet`. It also holds
[BOOT-SMOKE-TEST.md](jakarta-verify/BOOT-SMOKE-TEST.md), which documents how to boot the
demo webapp and what is known to be broken.

## Running the demo webapp

```bash
./cocoon.sh
```

Then <http://localhost:8888/>. See
[jakarta-verify/BOOT-SMOKE-TEST.md](jakarta-verify/BOOT-SMOKE-TEST.md) for what this
verifies and for the manual invocation.

Start with a **fresh temp directory** whenever block content has changed. Cocoon's pipeline
cache lives under the servlet temp directory, and reusing it across such a change has
produced one block's resource being served for another block's URL — with HTTP 200 and the
wrong content type, which browsers then cache. `cocoon.sh` does this for you.

## Known issues

- **Concurrent requests against a cold cache corrupt the response.** A burst of concurrent
  first requests for an uncached resource fails with
  `IllegalArgumentException: setContentLength(N) when already written M`, or silently
  serves one URL's content for another. Warm caches are clean. This is a long-standing
  Cocoon defect, reported downstream as far back as 2015 against Jetty 6 and worked around
  there by serving static resources through a bundler (wro4j) instead. It predates this
  migration by a decade and two servlet stacks. Not fixed. The investigation, including
  what has been ruled out and a concurrency harness that deliberately does *not* reproduce
  it, is written up in [BOOT-SMOKE-TEST.md](jakarta-verify/BOOT-SMOKE-TEST.md).
- Any generated link containing `//` is rejected by Jetty 12 with
  `400 Ambiguous URI empty segment`. Jetty 9 served those. Worth grepping for before a
  container upgrade.

## Relationship to upstream

This is a fork. It is not tracking Apache Cocoon trunk and is not intended to be merged
back: the reactor has been cut down to one consumer's needs and 43 blocks have been moved
to `legacy-blocks/` and left unbuildable. Upstream is at <https://cocoon.apache.org/>.

Contributor guidance for this repository is in [AGENTS.md](AGENTS.md) and
[CONTRIBUTING.md](CONTRIBUTING.md); note that both predate the fork and still describe the
`allblocks` build.
