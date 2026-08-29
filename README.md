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

## Third-party dependency versions

Cocoon 2.3's dependency set was largely frozen around 2008. Where an application on the Spring 6
/ Hibernate 6 stack has already moved a library forward, this fork has been moved to match, so
that the two sides do not fight over a version at assembly time.

Only the **default reactor** matters here — the 38 shipped artifacts. `legacy-blocks/` drags in
another few dozen libraries (asm 2.2.1, hsqldb 1.8, jdom 1.1, jtidy, commons-dbcp, …) that are
deliberately left alone because nothing builds against them.

### Raised in this fork

| Dependency | Cocoon 2.3 | Here | Why |
|---|---|---|---|
| `org.apache.xmlgraphics:batik-*` | 1.16 | **1.18** | Matches the consuming stack. |
| `commons-io:commons-io` | 2.11.0 | **2.21.0** | Matches the consuming stack. |
| `commons-beanutils:commons-beanutils` | 1.9.4 | **1.11.0** | Matches the consuming stack. |
| `org.apache.poi:poi` | 3.2-FINAL | **3.10.1** | Matches the consuming stack. Needed one code change; see below. |
| `org.apache.xmlgraphics:fop` | 0.95-1 | **1.0** | Matches what the consuming stack ships. Used only by `cocoon-fop-ng-impl`. |
| `org.slf4j:slf4j-simple` | 1.7.12 | **2.0.6** | Not an alignment — a mismatch. The tree already used `slf4j-api` 2.0.6, so the binding was a major version behind its API. |
| `org.springframework:*` | 5.x | **6.1.10** | Required by Jakarta EE 10. |
| `jakarta.servlet:jakarta.servlet-api` | `javax.servlet-api` 3.1.0 | **6.0.0** | The point of the fork. Deliberately not 6.1, which removes `Cookie.getComment/getVersion`. |
| `jakarta.mail`, `jakarta.activation` | `javax.mail`, `javax.activation` | **2.1.3** | Jakarta renames. |
| `org.aspectj:aspectjweaver` | 1.8.x | **1.9.19** | Required for JDK 17. |
| `org.acegisecurity:acegi-security` | 1.0.7 | **Spring Security 6.3.3** | Not a version bump; see below. |
| `rhino:js` | 1.6R7 | **`org.mozilla:rhino` 1.7R5** | Coordinate move plus an API break; see below. |

**POI 3.2 → 3.10.1** removed `org.apache.poi.hssf.util.RangeAddress`, which `EPMerge` used to
turn a merge range such as `B3:D7` into coordinates. `CellRangeAddress.valueOf` replaces it, but
is zero-based where `RangeAddress` counted from 1,1 — so the old code's "subtract one" had to go.
Getting that backwards would shift every merged region by a row and a column: a plausible-looking
corruption rather than a failure. `EPMergeTestCase` pins the coordinates that POI 3.2 produced,
so it is a real before/after comparison rather than a restatement of the new code.

**Acegi Security → Spring Security.** `cocoon-acegisecurity-sample` is now
`cocoon-springsecurity-sample`. Acegi could not be upgraded: it was renamed to Spring Security
at 2.0, and its final release (1.0.7) is a `javax.servlet` library with 600 references to that
package, so no version of it runs on Jakarta EE 10. The configuration was rewritten against the
Spring Security 6 namespace, which is also how it shrank from 160 lines to 100 — Acegi required
every filter to be declared as a bean and then listed by name in a whitespace-sensitive string.

The sample was quietly dead before this: it is XML only, has no Java, and is not deployed in the
demo webapp, so nothing ever loaded it — and it used `<ref local="..."/>`, which Spring removed
in 4.0, meaning the context could not have been parsed at all. It now has a test that builds the
application context and authenticates each declared user, so the same rot cannot recur.

### Deliberately ahead of the consuming stack

Not regressions — these are either required by Jakarta EE 10 or simply newer here, and Maven's
nearest-definition rule means the application's own `dependencyManagement` wins in its build:
`icu4j` 72.1, `commons-codec` 1.15, `xalan` 2.7.2, `slf4j-api` 2.0.6, Spring 6.1.10.

### Not raised, and why

- **FOP 2.x.** `cocoon-fop-ng-impl` does not compile against it: FOP 2 dropped the no-arg
  `FopFactory.newInstance()` along with `setUserConfig` and `setURIResolver`. That is three call
  sites in one class, so it is a contained piece of work rather than a blocker — but it is a code
  change, not a version bump. (The POM's older `TODO: 2.8 … doesn't work` note gave no reason;
  this is the reason.)
- **`fop:fop:0.20.5`**, used by `cocoon-fop-impl`, is a different artifact from
  `org.apache.xmlgraphics:fop` and is stuck at the FOP 0.20 API — `Driver`, `Options`,
  `ConfigurationParser`, `MessageHandler`, all removed in FOP 1.0. Migrating that block means
  rewriting it against `FopFactory`, which is what `cocoon-fop-ng-impl` already is. Prefer the
  `-ng` block; see the warning below.
- **commons-lang3 / commons-collections4.** See below.

### Watch for duplicate packages under different coordinates

Several libraries in this dependency set were re-released under new Maven coordinates while
keeping their Java package names. Maven cannot detect the conflict — the groupIds differ, so it
sees two unrelated artifacts — and **both jars end up on the classpath**, with load order
deciding which class wins. An application that overrides any of these must exclude Cocoon's:

| Package | Cocoon's coordinate | The newer coordinate |
|---|---|---|
| `org.apache.commons.jexl` | `cocoon-commons-jexl` *(2.2 only)* | `commons-jexl:commons-jexl` |
| `org.apache.fop` | `fop:fop` (1032 classes) | `org.apache.xmlgraphics:fop-core` (2736 classes) |

This is a class of problem rather than three incidents; it is worth grepping for before an
upgrade. `jakarta-verify` catches the `javax.*` version of it, but not this one.

### Rhino 1.7

This fork was on `rhino:js:1.6R7`; an application embedding it typically runs
`org.mozilla:rhino:1.7R5`. That is not just a version difference — the two are different Maven
coordinates carrying the same `org.mozilla.javascript` packages, so both jars ship and load order
decides. Worse, **flowscript could not run on 1.7 at all**: Rhino removed
`org.mozilla.javascript.continuations.Continuation` in favour of `NativeContinuation`, and
continuations are what `sendPageAndWait` is built on.

Now on `org.mozilla:rhino:1.7R5`. Four API changes, in six files:

- `Continuation` → `NativeContinuation` — `FOM_Cocoon`, `FOM_WebContinuation`,
  `FOM_JavaScriptInterpreter`, and in Forms `Form` and `SuggestionListGenerator`.
- `Context.setCompileFunctionsWithDynamicScope` was removed. Its replacement is a
  `ContextFactory` feature flag that is global to the JVM, which is a poor thing for a library
  to install. It is not reinstated, because the interpreter parents its scopes explicitly
  through `ThreadScope` — which is what dynamic scoping would otherwise have provided.
- `DebugFrame` gained `onDebuggerStatement`, implemented as a no-op in
  `LocationTrackingDebugger`; that class only tracks locations for stack traces.
- Rhino 1.7 assigns `Undefined` to a not-yet-declared name while unwinding a continuation.
  `ThreadScope.put` treated that as an implicit global declaration and refused it, which broke
  resuming; `Undefined` is now excluded from that check.

Compiling against the new class proves nothing here, because the cast and the scope restore only
happen on the *second* request. `FlowscriptContinuationTest` therefore drives both halves against
a running container: it fetches a page that suspends a flowscript, asserts a continuation id was
really created, then posts back to it and asserts the script resumed.

### On `commons-lang3` and `commons-collections4`

Worth stating plainly, because it looks like it should be a version bump and is not.
`commons-lang3` is a **different artifact with a different package**
(`org.apache.commons.lang3`), designed to coexist with `commons-lang` 2.x rather than replace it.
So there is no classpath collision and nothing forces the move — an application can and commonly
does run both generations side by side.

Migrating would touch 96 files (109 imports) for `commons-lang` and 28 files (48 imports) for
`commons-collections`. Most of that is mechanical, but the residue lands in the **public API**:

- `WidgetState`, `Whitespace`, `RepeaterEventAction` and `ProcessingPhase` in `cocoon-forms-impl`,
  and `Deprecation.LogLevel` in `cocoon-util`, are public classes that `extend` commons-lang 2's
  `Enum` / `ValuedEnum`. The whole `org.apache.commons.lang.enums` package was **removed** in
  lang3; there is no equivalent, so these become real Java enums and their supertype changes.
- `LocatedRuntimeException` in `cocoon-pipeline-api` extends `NestableRuntimeException`, also
  **removed** in lang3. Downstream `catch` and `instanceof` on that type would stop matching.
- `ArrayStack` and `FastHashMap` are gone from `commons-collections4`.

So it is an API break for consumers of this fork, in exchange for no compatibility gain. That is
the wrong trade for a fork whose purpose is to be adoptable. Left as is, deliberately.

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
