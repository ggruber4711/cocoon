# The 2.2 → 2.3 artifact delta

Resolves **Gap G** of [jakarta-ee10-fork.md](jakarta-ee10-fork.md). A consuming application on
Cocoon 2.2 makes two jumps at once when it adopts this fork: `javax` → `jakarta`, and 2.2 → 2.3.
The Jakarta half is covered elsewhere. This document covers the 2.2 → 2.3 half — which artifacts
change name, which disappear, and which change behaviour.

**Verdict: the delta is much smaller than the plan assumed.** Of the five artifacts flagged as
missing, none is a blocker. Two were never actually used, one is a byte-identical republish of a
third-party jar, and two are build-time only. The one item the plan called out as real work —
folding a privately forked `cocoon-spring-configurator` back in — turns out to be a no-op.

The genuine risks are the two nobody had written down: a **silent duplicate JEXL on the
classpath**, and the **collapse of per-module versioning**.

## How this was derived

The consuming side was taken from its POMs *and* from the assembled `WEB-INF/lib` of a built
webapp, which is the authoritative record of what actually ships — the two disagree, and that
disagreement is itself a finding. The producing side is every `pom.xml` in this tree, including
`legacy-blocks/`, so "missing" means genuinely absent rather than merely out of the reactor.

39 Cocoon jars ship today. 33 distinct Cocoon artifacts are referenced across the consumer's POMs.

## A. Straight version swap — 34 artifacts

These exist in 2.3 under the same coordinates with the same packages. Nothing to do but change
the version.

`cocoon-ajax-impl`, `cocoon-apples-impl`, `cocoon-auth-api`, `cocoon-auth-impl`,
`cocoon-axis-impl`, `cocoon-batik-impl`, `cocoon-block-deployment`, `cocoon-configuration-api`,
`cocoon-core`, `cocoon-expression-language-api`, `cocoon-expression-language-impl`,
`cocoon-flowscript-impl`, `cocoon-fop-impl`, `cocoon-forms-impl`, `cocoon-jnet`,
`cocoon-linkrewriter-impl`, `cocoon-mail-impl`, `cocoon-pipeline-api`,
`cocoon-pipeline-components`, `cocoon-pipeline-impl`, `cocoon-poi-impl`,
`cocoon-serializers-charsets`, `cocoon-serializers-impl`, `cocoon-servlet-service-components`,
`cocoon-servlet-service-impl`, `cocoon-sitemap-api`, `cocoon-sitemap-components`,
`cocoon-sitemap-impl`, `cocoon-spring-configurator`, `cocoon-store-impl`, `cocoon-template-impl`,
`cocoon-thread-api`, `cocoon-thread-impl`, `cocoon-util`, `cocoon-xml-api`, `cocoon-xml-impl`,
`cocoon-xml-resolver`, `cocoon-xml-util`.

### The version scheme changed, and this is the largest mechanical change

In 2.2 every module carried its own independently incremented version. The consumer therefore
pins **38 different version strings**: `cocoon-core:2.2.1-workflow-1`,
`cocoon-pipeline-impl:1.1.0-workflow-3`, `cocoon-sitemap-impl:1.1.0-workflow-4`,
`cocoon-forms-impl:1.0.0-workflow-3`, `cocoon-servlet-service-impl:1.3.3-workflow`,
`cocoon-configuration-api:1.0.4`, and so on.

**In 2.3 all of them are a single reactor version** (`2.3.1-SNAPSHOT` today). Every one of those
38 pins collapses to one property. This is a simplification, but it is not a search-and-replace:
each `<version>` line has a *different* old value, so the edit cannot be done with one
substitution, and any pin left behind silently resolves to a 2.2 jar that will not link against
Jakarta.

Recommendation: replace the whole block with a single `${cocoon.version}` property and let
`dependencyManagement` carry it once.

## B. Renamed — and never actually used

| 2.2 coordinate | 2.3 equivalent |
|---|---|
| `cocoon-expression-api:1.1.0-workflow` | `cocoon-expression-language-api` |
| `cocoon-expression-impl:1.1.0-workflow` | `cocoon-expression-language-impl` |

The plan listed these as needing an explicit mapping. They do not: **both appear only inside
`<dependencyManagement>` and are never declared as a dependency anywhere.** Maven's
`dependencyManagement` only pins versions for artifacts something actually requests, so these two
entries have no effect on the build, and neither jar is present in the assembled webapp.

The consumer already depends on `cocoon-expression-language-api` / `-impl` directly, and those
ship. Package layout is identical across the rename — `org.apache.cocoon.el`,
`.el.objectmodel`, `.el.parsing`, `.el.util` in both — so there is no code impact either way.

**Action: delete the two `dependencyManagement` entries.** They are dead weight that made the
migration look larger than it is.

## C. `cocoon-commons-jexl` — a republish, and a classpath trap

`org.apache.cocoon:cocoon-commons-jexl:1.0` is **byte-identical to `commons-jexl:commons-jexl:1.0`**.
Verified: same 89 class entries, same sizes, and the same SHA-1 over the concatenated class
bytes. It is not a fork; 2.2 simply republished the upstream jar under Cocoon's groupId.

2.3 dropped the republish and depends on `commons-jexl:commons-jexl:1.1` directly, managed in the
root POM. That is a *newer* JEXL than the consumer has today.

> **The trap.** The consumer declares `cocoon-commons-jexl` as a real dependency in two modules.
> Today that is the only JEXL jar in `WEB-INF/lib`. After the switch, `commons-jexl:1.1` arrives
> transitively via `cocoon-core` and `cocoon-expression-language-impl`. If the old declaration is
> left in place, **both jars ship, and both contain `org.apache.commons.jexl.*`** — 98 classes
> against 93, two versions of the same classes. Maven will not flag this: the groupIds differ, so
> it is not a conflict it can see. Whichever jar the container's classpath ordering happens to
> load first wins, which makes it environment-dependent and effectively undebuggable.

**Action: remove both `cocoon-commons-jexl` dependency declarations and the
`dependencyManagement` entry.** Take `commons-jexl` transitively. This is the one item in the
delta that fails silently rather than loudly, so it should be done first.

## D. `cocoon-rcl-*` — build-time only, not shipped

`cocoon-rcl-webapp-wrapper:1.0.2` and `cocoon-rcl-spring-reloader:1.0.2` are absent from this
tree (`tools/cocoon-rcl` exists on disk but is still not in `tools/pom.xml`).

They are **not runtime dependencies**. Both are declared only by `cocoon-maven-plugin`, and
neither appears in the assembled webapp. They serve the reloading-classloader development loop.

This lowers the priority of that Phase 5 item considerably: it cannot break production, only the
developer inner loop, and only for developers who use RCL. It still has to be built from source
eventually, because the published 1.0.2 jars are 2.2-era `javax` builds.

## E. `cocoon-spring-configurator` — the fork is a no-op

The plan called for folding the privately released `2.2.2-workflow` fork's changes into the 2.3
module. A three-way source diff (upstream `2.2.1` → `2.2.2-workflow` → this tree) shows
**nothing needs folding.**

The `-workflow` release is not a fork with local patches. It is Apache SVN r1711427 (2015-10-30),
i.e. upstream trunk code released early under a `-workflow` version — almost certainly to obtain
Spring 3 compatibility without moving the rest of the stack to 2.3. Its four substantive
differences from upstream 2.2.1 are all generics modernisation and Spring 3 API renames.

Against this tree, file by file:

| File | Upstream rev here | vs the fork | Assessment |
|---|---|---|---|
| `AbstractElementParser` | r1907094 (2023) | **2.3 is newer** | Already has `MULTI_VALUE_ATTRIBUTE_DELIMITERS`. Nothing to fold. |
| `BeanMapElementParser` | r1907094 (2023) | **2.3 is newer** | Uses `ClassUtils.forName(type, null)`; the fork passes an explicit loader. Both valid; `null` means default loader. |
| `ResourceUtils` | r677626 (2008) | 2.3 is older | The fork's change is generifying `Comparator` → `Comparator<Resource>`. Erasure-identical at runtime. No impact. |
| `WildcardBeanMap` | — | equivalent | 2.3 calls `getBeanNamesForType((Class<?>) null)`; the fork calls `(null, true, true)`. Those are the documented defaults of the one-arg form — the same call. |

The only real caveat is `BeanMapElementParser`: `forName(type, null)` resolves against the default
class loader, whereas the fork resolved against the parser's own. In a container that gives blocks
their own class loaders these can differ. It has not been observed to matter, and 2.3's form is
the upstream one, but it is the single line in this module worth watching if a `bean-map` element
fails to resolve its `type` after the switch.

## Summary of edits to the consuming POMs

1. Remove the `cocoon-commons-jexl` dependency from both modules that declare it, and its
   `dependencyManagement` entry. *(Do this first — it is the silent one.)*
2. Delete the `cocoon-expression-api` and `cocoon-expression-impl` `dependencyManagement`
   entries. They were never used.
3. Replace the 38 individual version pins with one `${cocoon.version}` property.
4. Leave `cocoon-rcl-*` alone for now; it is build-time only and cannot affect the deployed app.
5. Nothing to do for `cocoon-spring-configurator` beyond the version change.

## Appendix: JEXL, and the ternary operator

The `commons-jexl` finding above raises a question worth answering here, because the delta is the
natural moment to decide it: JEXL 1.x **has no ternary operator**, and JX templates are therefore
stuck writing `<jx:choose>` for what should be an inline conditional.

This was measured, not assumed. Against both 1.0 and 1.1 the `?` character is not even a token:

```
x > 3 ? 'big' : 'small'   ->  Lexical error at line 1, column 7
nil ?: 'fallback'         ->  Lexical error at line 1, column 5
```

Moving to 1.1 (which 2.3 already does) changes nothing here — it only turns `TokenMgrError` into
`ParseException`. Ternary support arrived in JEXL 2.

### JEXL 3.5 is compatible, but only when configured to be

The same 19 expressions were run through JEXL 1.1 and JEXL 3.5.0. **Out of the box, JEXL 3 would
break Cocoon templates badly** — it is strict where JEXL 1 was silent:

| Expression | JEXL 1.1 | JEXL 3.5 default | JEXL 3.5 lenient |
|---|---|---|---|
| `undefinedVar` | `null` | **throws** | `null` |
| `nil + 1` | `1` | **throws** | `1` |
| `x / 0` | `0.0` | **throws** | `0.0` |
| `list[99]` | `null` | **throws** | `null` |
| `x > 3 ? 'big' : 'small'` | **parse error** | `big` | `big` |

Configured as `strict(false).silent(true).safe(true).arithmetic(new JexlArithmetic(false))`,
JEXL 3.5 reproduced JEXL 1.1 **exactly on every comparable expression**, including the quirks a
template might unknowingly rely on: division by zero yielding `0.0`, null coercing to zero in
arithmetic, out-of-range index yielding null.

One real difference remains: **integer widening**. `x + 1` where `x` is 5 returns `Long` under
JEXL 1.1 and `Integer` under JEXL 3.5. This is invisible in rendered output but not invisible to
Java code that casts a result or compares it with `.equals`.

### The upgrade is additive, which is what makes it safe

Expression languages are registered as individually named Spring beans
(`org.apache.cocoon.el.ExpressionCompiler/<lang>`), and the default for JX is **JXPath**, not
JEXL. JEXL 1 lives in `org.apache.commons.jexl`, JEXL 3 in `org.apache.commons.jexl3` — different
packages, so both can sit on the classpath at once.

So a `jexl3` compiler can be registered *alongside* the existing `jexl` one. No existing template
changes behaviour; templates opt in one at a time; the default flips only once everything has
been moved. That is a far better risk profile than a swap.

The work is bounded and sits in one module (`cocoon-expression-language-impl`):

- **`JexlCompiler` / `JexlExpression`** — small. The JEXL 3 context interface (`get`/`set`/`has`)
  is a better fit for Cocoon's `ObjectModel` than JEXL 1's `getVars(): Map`, and the engine is
  built once via `JexlBuilder` rather than through static factories.
- **`JSIntrospector`** (~330 lines) — the real work. It teaches JEXL to walk Rhino JavaScript
  objects, which is what makes JEXL expressions work against flowscript variables. It must be
  rewritten against JEXL 3's introspection API, but the mapping is close to one-to-one:
  `UberspectImpl`→`Uberspect`, `VelMethod`→`JexlMethod`, `VelPropertyGet`→`JexlPropertyGet`,
  `VelPropertySet`→`JexlPropertySet`. JEXL 3 adds `tryInvoke`/`tryFailed` to those interfaces
  (both can delegate to the existing invoke path).
- **One thing gets deleted.** `JexlExpression` currently installs its introspector by reflecting
  into a private static field:
  ```java
  Field field = Introspector.class.getDeclaredField("uberSpect");
  field.setAccessible(true);
  field.set(null, new JSIntrospector());
  ```
  with the comment *"Hack: there's no _nice_ way to add my introspector to Jexl right now"*. That
  hack is global, order-dependent, and swallows its own failure with `printStackTrace`. JEXL 3
  takes the uberspect as a constructor argument on `JexlBuilder`. The hack goes away.

Note also that JEXL 3.3 introduced a sandboxing permissions model that restricts reflective reach
into JDK internals by default. That is a security improvement, but it is a behavioural change:
templates that call unusual Java APIs should be exercised before the default is flipped.

**Recommendation:** add `jexl3` as an additional language rather than upgrading in place. It is
the only option that delivers the ternary without putting every existing JX template at risk, and
it can be done independently of the Jakarta work.
