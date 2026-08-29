# Cocoon → Jakarta EE 10: fork plan for the consuming application

> **Status:** Phases 0-4 implemented on `feature/jakarta-ee10-fork`. Phase 5 not started.
> **Written:** 2026-08-29 · **Basis:** `chore/spring6-jakarta-pass1` @ `390a2f2ba4`
> **Consumer:** the embedding application
>
> Current state: 79 modules, `BUILD SUCCESS`, 316 tests, 0 failures on JDK 17 against
> `jakarta.servlet-api` 6.0.0. With `-P samples`, 102 modules. See **Implementation notes**
> at the end for where reality diverged from this plan.

---

## 1. Context

The consuming application embeds Apache Cocoon and is migrating **Spring 5.3 → 6,
Hibernate 5.6 → 6 and its UI toolkit**. All of those force **Jakarta EE 10 / Servlet 6.0 /
JDK 17**. Cocoon is the one component in that stack with no upstream Jakarta release, so it
needs a **fork** publishing EE 10-compatible artifacts on the same timeline.

There is no mixed mode: a single webapp cannot host `javax.servlet` and `jakarta.servlet` against
one container. The Cocoon fork must be ready **before** the application flips, or the flip is
blocked.

### Measured state of both sides

| | consuming application | cocoon (`chore/spring6-jakarta-pass1`) |
|---|---|---|
| Spring | 5.3.39 | 6.1.10 |
| Servlet | `javax.servlet-api` 3.1.0 | `jakarta.servlet-api` 5.0.0 |
| Jetty | 9.4.54 | 11.0.20 (`jakarta` profile) |
| Hibernate | 5.6.15 | — |
| Vaadin | 8.14.3 | — |
| JDK | 8/11-era | 17 (`targetJdk=17`) |
| Namespace | 100% javax (4357 `javax.persistence`, 1083 `javax.annotation`, 508 `javax.servlet`) | sources rewritten to jakarta, POMs largely not |
| Cocoon artifacts consumed | `2.2.1-workflow-1`, `2.2.2-workflow`, `1.0.1-workflow` | this repo is the `2.3.1-SNAPSHOT` line |

### Decisions taken

1. **Route:** reach a green **Servlet 5.0 / EE 9** build first as a bisectable checkpoint, then flip to EE 10.
2. **Fork identity:** keep `org.apache.cocoon` groupId, version suffix `2.3.1-workflow-jakarta-N`
   (matches the existing `-workflow` convention).
3. **Base line:** fork from this 2.3 trunk. The 2.2 → 2.3 API delta is an explicit workstream.
4. **Out-of-scope blocks:** quarantined, not deleted — `cocoon-portal` **in its entirety**, plus every
   block the consuming application does not consume.
5. **Axis:** keep `cocoon-axis-impl`; jakarta-fy Axis 1.4 + `commons-discovery` with **Eclipse Transformer**
   and publish the transformed jars under the fork's coordinates.
6. **IT harness:** retire the hand-rolled `tools/cocoon-it-fw` mojos in favour of
   `org.eclipse.jetty.ee10:jetty-ee10-maven-plugin`.

---

## 2. Scope: what the fork must actually ship

Derived from the consuming application's Cocoon aggregator module and its parent POM.

**In scope — core:** `cocoon-configuration-api`, `cocoon-util`, `cocoon-jnet`, `cocoon-xml-api`,
`cocoon-xml-impl`, `cocoon-pipeline-api`, `cocoon-pipeline-impl`, `cocoon-pipeline-components`,
`cocoon-sitemap-api`, `cocoon-sitemap-impl`, `cocoon-sitemap-components`, `cocoon-store-impl`,
`cocoon-expression-language-api`, `cocoon-expression-language-impl`, `cocoon-spring-configurator`,
`cocoon-servlet-service-impl`, `cocoon-servlet-service-components`, `cocoon-block-deployment`,
`cocoon-core`.

**In scope — blocks:** `cocoon-forms-impl`, `cocoon-ajax-impl`, `cocoon-apples-impl`,
`cocoon-flowscript-impl`, `cocoon-template-impl`, `cocoon-serializers-impl`,
`cocoon-serializers-charsets`, `cocoon-auth-api`, `cocoon-auth-impl`, `cocoon-mail-impl`,
`cocoon-fop-impl`, `cocoon-batik-impl`, `cocoon-poi-impl`, `cocoon-axis-impl`.

**In scope — tooling:** `cocoon-maven-plugin` (maintained in its own repository,
currently `1.0.10-workflow`), `tools/cocoon-rcl`.

**Quarantined** (moved to the top-level `legacy-blocks/` directory, outside the reactor, code left in
tree): all of `cocoon-portal`, `cocoon-jsp`, `cocoon-taglib`, `cocoon-xsp`, `cocoon-deli`,
`cocoon-faces`, `cocoon-ojb`, `cocoon-jcr`, `cocoon-jms`, `cocoon-slide`, `cocoon-lucene`,
`cocoon-velocity`, `cocoon-xmldb`, `cocoon-proxy`, `cocoon-profiler`, `cocoon-scratchpad`,
`cocoon-session-fw`, `cocoon-authentication-fw`, `cocoon-databases`, `cocoon-cron`,
`cocoon-eventcache`, `cocoon-html`, and all `*-sample` modules plus `core/cocoon-webapp` and `dists/`.

### Why this matters

A full `mvn -P allblocks -DskipTests -fn install` on JDK 17 today gives **186 success / 34 failure**.
Filtered to the in-scope set, only **9 modules actually fail**: `cocoon-core`,
`cocoon-servlet-service-impl`, `cocoon-servlet-service-components`, `cocoon-template-impl`,
`cocoon-auth-api`, `cocoon-auth-impl`, `cocoon-mail-impl`, `cocoon-serializers-impl`,
`cocoon-serializers-charsets`. The other 25 are blocks the consuming application never loads.

**Reducing the reactor is therefore the single highest-leverage first move** — it converts a
34-failure problem into a 9-failure problem before a line of Java is touched.

---

## 3. Current state, in detail

### 3.1 What is already done

Nine commits on `chore/spring6-jakarta-pass1`: Spring 5.3→6.1.10, JDK 17 toolchain, surefire 3.1.2,
maven-bundle-plugin 5.1.8, an OpenRewrite `jakarta-migration` profile, a Jetty 11 profile on
`core/cocoon-webapp`, and `jakarta.servlet-api` in `dependencyManagement`. Java sources are rewritten
to the `jakarta.*` namespace (239 files import `jakarta.servlet`; **zero** `import javax.servlet` lines remain).

### 3.2 Gap A — OpenRewrite rewrote sources but not module POMs

The root cause of nearly every compile failure. 44 POMs still declare `javax.servlet-api` while their
sources import `jakarta.servlet`, producing `package jakarta.servlet does not exist`.

Confirmed in-scope offenders: `cocoon-mail-impl` (also `javax.mail:mail` → needs `jakarta.mail` +
`jakarta.activation`), `cocoon-auth-impl`, `cocoon-serializers-impl`, `cocoon-serializers-charsets`.
A second tier (`cocoon-auth-api`, `cocoon-template-impl`) has no direct import but still fails with
`cannot access jakarta.servlet.http.HttpServletRequest`, because Cocoon's own API extends the servlet
API (§3.3) and `provided` scope is not transitive.

The root POM keeps `javax.servlet-api:3.1.0` under management ([pom.xml:2249-2253](pom.xml)) alongside
`jakarta.servlet-api`, which is what lets modules silently resolve the wrong artifact. Stale entries also
sit at lines 1344, 1370 (`jstl` 1.2), 2115 (`jsp-api` 2.0) and 2484 (`servlet-api-2.5`).

### 3.3 Gap B — Cocoon's public API extends the servlet API

```
org.apache.cocoon.environment.Context  extends jakarta.servlet.ServletContext        (Context.java:34)
org.apache.cocoon.environment.Request  extends jakarta.servlet.http.HttpServletRequest (Request.java:41)
org.apache.cocoon.environment.Response extends jakarta.servlet.http.HttpServletResponse (Response.java:28)
org.apache.cocoon.environment.Session  extends jakarta.servlet.http.HttpSession        (Session.java:44)
```

Every servlet-spec change therefore ripples into ~25 implementors. The good news: almost all of them
descend from four base classes in `cocoon-pipeline-impl` — `AbstractContext`, `AbstractRequest`,
`AbstractResponse`, `AbstractSession`. **Those four files are the leverage point**; fixes applied there
are inherited by `HttpContext`/`HttpRequest`/`HttpResponse`/`HttpSession`, `BackgroundRequest`/
`Response`/`Session`, `CommandLineRequest`/`Context`/`Session`, `AbstractRequestWrapper`,
`ResponseWrapper` and the mocks.

Only these bypass the base classes and need individual attention (all in scope):
`core/cocoon-blocks-fw/.../util/ServletContextWrapper.java`, `BlockCallHttpServletRequest.java`,
`BlockCallHttpServletResponse.java`, `core/cocoon-servlet-service/.../util/ServletContextWrapper.java`,
`ServletServiceRequest.java`, `ServletServiceResponse.java`, `ServletServiceContext.java`, and
`core/cocoon-sitemap/.../processing/impl/MockProcessInfoProvider.java`.

### 3.4 Gap C — the in-progress uncommitted diff is aimed at EE 9, not EE 10

Working tree changes to `HttpContext`, `HttpRequest`, `HttpResponse`, `HttpSession`, `AbstractSession`
re-add `HttpSessionContext`, `getValue`/`putValue`/`removeValue`/`getValueNames` — precisely the members
**Servlet 6.0 removes**. Under the agreed route this is correct as a temporary EE 9 checkpoint, but it is
explicitly throwaway work; Phase 3 deletes it.

The build currently hard-stops before all of it anyway:

```
HttpContext.java:[45,14] org.apache.cocoon.environment.http.HttpContext is not abstract and does not
override abstract method setSessionTimeout(int) in jakarta.servlet.ServletContext
```

### 3.5 Gap D — runtime breakage the compiler cannot see

These pass `mvn install` and fail on first boot:

- **Spring bean-name mismatch.** `SettingsElementParser.java:136` registers the servlet context under
  `ServletContext.class.getName()`, now `jakarta.servlet.ServletContext`. Eight Spring XML files still
  reference `javax.servlet.ServletContext`, including
  `core/cocoon-core/src/main/resources/META-INF/cocoon/spring/cocoon-core-applicationContext.xml:25`,
  `cocoon-ssf-context.xml:26`, `cocoon-ssf-callstack.xml:34,42,49,56`,
  `cocoon-ssf-servlet-map.xml:25`, `cocoon-servlet-service-{property,path}-module.xml:25`,
  `cocoon-blockdeployment-resourcesholder.xml:27,33`. → `NoSuchBeanDefinitionException`.
- **Servlet attribute-name string literals.** 13 occurrences of `"javax.servlet.context.tempdir"` and
  `"javax.servlet.include.*"`, e.g. `BlockDeploymentServletContextListener.java:51`. A Jakarta container
  publishes these as `jakarta.servlet.*`; every lookup silently returns `null`.
- **`web.xml` is still `version="2.4"`** with the `java.sun.com` namespace
  (`core/cocoon-webapp/src/main/webapp/WEB-INF/web.xml:24`).

### 3.6 Gap E — tooling still on Jetty 6 / Servlet 2.5

`tools/cocoon-it-fw` is a Cocoon-owned Maven plugin whose `JettyContainer` imports
`org.mortbay.jetty.*` and whose POM depends on `org.mortbay.jetty:servlet-api-2.5`. It is a dependency
of `core/cocoon-webapp`. `tools/cocoon-rcl` exists on disk but is **not listed in `tools/pom.xml`**, so
the `cocoon-rcl-webapp-wrapper` / `cocoon-rcl-spring-reloader` artifacts the consuming application consumes are 2.2-era
javax builds with no source in this reactor.

### 3.7 Gap F — third-party jars compiled against javax

`cocoon-serializers-charsets` additionally fails in `maven-bundle-plugin` with
*"package(s) import from the default package"* — an OSGi manifest problem, unrelated to Jakarta, but
in scope because `cocoon-serializers-impl` depends on it.

### 3.8 Gap G — 2.2 → 2.3 artifact delta

the consuming application references three artifacts that **do not exist in this 2.3 tree**: `cocoon-commons-jexl`,
`cocoon-expression-api`, `cocoon-expression-impl`. `cocoon-spring-configurator` is consumed as
`2.2.2-workflow`, a separately maintained fork. These need explicit mapping before the consuming application can switch.

---

## 4. The Servlet 6.0 delta (what Phase 3 must absorb)

### Removed by Servlet 6.0 — sites that *delegate* to the removed method break at compile time

| Removed member | Delegating call sites (in scope) |
|---|---|
| `ServletContext.getServlets/getServletNames/getServlet` | `blocks-fw/util/ServletContextWrapper.java:139,148,157`; `servletservice/util/ServletContextWrapper.java:89,93,97`; `AbstractContext.java:171,185,192` |
| `ServletContext.log(Exception,String)` | `HttpContext.java:207`; both `ServletContextWrapper`s; `CommandLineContext.java:50` |
| `ServletRequest.getRealPath` | `AbstractRequest.java:128`; `AbstractRequestWrapper.java:597`; `BlockCallHttpServletRequest.java:258`; `ServletServiceRequest.java:387`; `HttpRequest.java:525` |
| `HttpServletRequest.isRequestedSessionIdFromUrl` | `AbstractRequest.java:112`; `AbstractRequestWrapper.java:555`; `HttpRequest.java:331`; `BlockCallHttpServletRequest.java:466`; `ServletServiceRequest.java:462` |
| `HttpServletResponse.encodeUrl/encodeRedirectUrl` | `AbstractResponse.java:39,44`; `HttpResponse.java:146,153`; `BlockCallHttpServletResponse.java:86,102`; `ServletServiceResponse.java:88,98`; `MockProcessInfoProvider.java:476,481` |
| `HttpServletResponse.setStatus(int,String)` | `HttpResponse.java` (already patched in the working tree) |
| `HttpSession.getSessionContext/getValue/putValue/removeValue/getValueNames`, type `HttpSessionContext` | `HttpSession.java:22,282`; `AbstractSession.java`; `MockProcessInfoProvider.java:35,634`; `BlockCallHttpServletRequest.java:41,393`; `ServletServiceRequest.java:55,815`; `BackgroundSession.java` |

Note the distinction: an override that returns `null`/no-ops is harmless once the interface drops the
method (it just stops being an override) — delete it for hygiene. An override that **delegates** to the
wrapped object's removed method is a hard compile error.

### Newly abstract in Servlet 6.0 — must be implemented

`ServletRequest.getRequestId()`, `getProtocolRequestId()`, `getServletConnection()`.
Add to `AbstractRequest`; then individually to `BlockCallHttpServletRequest`, `ServletServiceRequest`,
and `MockProcessInfoProvider`'s inner request.

### Already abstract at Servlet 5.0 and still unimplemented (the current build stop)

`ServletContext.setSessionTimeout/getSessionTimeout`, `addJspFile`, and the four
`get/setRequestCharacterEncoding` / `get/setResponseCharacterEncoding` methods — needed on
`AbstractContext` and on both `ServletContextWrapper`s.

### Keep on Cocoon's own interfaces

Because `Context`/`Request`/`Response`/`Session` extend the servlet types, spec removals also strip them
from Cocoon's API. Where sitemap components rely on a removed method, **declare it explicitly on the
Cocoon interface** — the existing `Context.java:55 String getRealPath(String path);` is the pattern to follow.

### Version pin

Target **`jakarta.servlet-api:6.0.0`**, not 6.1.0. Servlet 6.1 additionally removes
`Cookie.getComment/setComment/getVersion/setVersion`, which `cocoon-poi`-adjacent code and
`XSPCookieHelper` still use. An earlier commit on this branch used 6.1.0; do not go back to it.

---

## 5. Execution phases

### Phase 0 — Fork setup and scope fencing

1. ~~Copy this plan to `plans/jakarta-ee10-fork.md`~~ — done; commit it.
2. Branch `feature/jakarta-ee10-fork` from `chore/spring6-jakarta-pass1`.
3. Set version to `2.3.1-workflow-jakarta-1-SNAPSHOT` across the reactor.
4. Rewrite the reactor: default build = the §2 in-scope set only. Move everything else out to `legacy-blocks/`. Critically, `blocks/cocoon-portal/pom.xml:36-47` still lists the portlet modules
   unconditionally — that is why the earlier "moved to legacy-blocks" commit had no effect. Quarantine
   the whole `cocoon-portal` aggregate.
5. Delete `javax.servlet-api`, `jsp-api`, `jstl`, `servlet-api-2.5` from root `dependencyManagement`
   so wrong-artifact resolution becomes a hard error.
6. **Checkpoint:** `mvn -DskipTests -fn install` failure count drops from 34 to ≤ 9.

### Phase 1 — Green build on Servlet 5.0 (EE 9 checkpoint)

7. Sweep in-scope module POMs: `jakarta.servlet-api` (5.0.0), `jakarta.mail`/`jakarta.activation`
   for `cocoon-mail-impl`. Add servlet-api to modules that need it only transitively.
8. Complete the Servlet 5.0 abstract surface on `AbstractContext` and both `ServletContextWrapper`s;
   finish `HttpContext` (`setSessionTimeout`/`getSessionTimeout`). Keep the working tree's
   `HttpSessionContext`/`getValue` shims — they are valid at 5.0.
9. Fix the `cocoon-serializers-charsets` OSGi manifest (`maven-bundle-plugin` `<Private-Package>` /
   default-package import).
10. **Checkpoint:** `mvn -DskipTests install` is fully green. Tag it. This is the bisect anchor.

### Phase 2 — Runtime correctness on EE 9

11. Rewrite the 8 Spring XML `javax.servlet.ServletContext` bean refs to `jakarta.servlet.ServletContext`.
12. Rewrite the 13 `"javax.servlet.*"` attribute-name literals to `"jakarta.servlet.*"`.
13. Update `web.xml` to the `jakarta.ee` 5.0 schema.
14. Replace `tools/cocoon-it-fw`'s mojos with `jetty-ee10-maven-plugin` start/stop goals
    (do it once, at EE 10 coordinates, rather than porting to Jetty 11 then Jetty 12).
15. Add `tools/cocoon-rcl` to `tools/pom.xml` and build `cocoon-rcl-webapp-wrapper` /
    `cocoon-rcl-spring-reloader` from source under the fork's version.
16. **Checkpoint:** the webapp boots under Jetty 11 with no `NoSuchBeanDefinitionException` and serves
    a sitemap request.

### Phase 3 — Flip to Servlet 6.0 / EE 10

17. `jakarta.servlet-api` → `6.0.0`; Jetty 11 → `org.eclipse.jetty.ee10` 12.x.
18. Delete every shim from the §4 removal table; add the `getRequestId`/`getProtocolRequestId`/
    `getServletConnection` trio; promote any still-needed method onto Cocoon's own interfaces.
19. Re-verify Spring 6.1.10 (it supports Servlet 5.0+; no change required, but confirm before
    considering a 6.2.x bump, which would raise the floor to Servlet 6.0 anyway).
20. **Checkpoint:** green build + webapp boots under Jetty 12 EE10.

### Phase 4 — Third-party jars compiled against javax

21. Scan the in-scope dependency closure for `javax/servlet` and other EE classes inside jars:
    `mvn dependency:list` → for each jar, `unzip -l` grep `javax/(servlet|mail|jms|xml/rpc|annotation)/`.
22. For each hit, prefer an upstream Jakarta release; where none exists, run **Eclipse Transformer**
    and publish under `org.apache.cocoon` with a `-workflow-jakarta` classifier/version.
    Known targets: **Axis 1.4** and **commons-discovery 0.4** (both required by `cocoon-axis-impl`;
    the consuming application already pairs them with `jakarta.xml.rpc-api`).
23. Add a build-time guard (enforcer rule or a small script in CI) that fails if any `javax/servlet`
    class reaches the runtime classpath.

### Phase 5 — Publish and integrate with the consuming application

24. Release `2.3.1-workflow-jakarta-1` to the internal repository.
25. Re-release `cocoon-maven-plugin` from its own repository against the fork; audit it for servlet/Jetty coupling (`cocoon:prepare-jetty-webapp` and the RCL packaging are
    both container-facing).
26. **Resolve the 2.2 → 2.3 delta (Gap G)** before the consuming application switches: map `cocoon-commons-jexl`,
    `cocoon-expression-api`, `cocoon-expression-impl` to their 2.3 equivalents, and fold the
    `cocoon-spring-configurator:2.2.2-workflow` fork's changes into the 2.3 module.
27. In the consuming application's parent POM, point the Cocoon coordinates at the new
    version; in its Cocoon aggregator module, swap the transformed Axis artifacts in.

---

## 6. Verification

Per-phase, in the `cocoon` repo (note `./mvnw` is broken on this machine — wget cannot load
`libunistring.2.dylib`; and the default `mvn` runs on JDK 26, so pin JDK 17):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -DskipTests -Drat.skip=true -fn install
```

Failure count must move 34 → ≤9 (Phase 0) → 0 (Phase 1), and stay 0 through Phases 3–4.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
```

Run the unit suites from Phase 1 onward — the `cocoon-sitemap-impl` mock environment
(`MockRequest`/`MockResponse`/`MockContext`/`MockSession`) exercises exactly the `Abstract*` surface
being changed, so it is the fastest signal that §4 was done consistently.

Runtime check (Phase 2 onward) — the boot path is what catches Gap D, which no test covers:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -pl core/cocoon-webapp -P jakarta jetty:run
```

Then `curl -sS http://localhost:8888/` and confirm a rendered sitemap response, plus a clean startup
log with no `NoSuchBeanDefinitionException` and no `getAttribute` returning null for the tempdir.

Classpath purity check (Phase 4):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
tr ':' '\n' < /tmp/cp.txt | while read j; do unzip -l "$j" 2>/dev/null | grep -q 'javax/servlet/' && echo "JAVAX: $j"; done
```

Integration check (Phase 5): build `the consuming application-tools-webclient-cocoon` against the published fork and run
the existing the consuming application Jetty dev target.

---

## 7. Risks and open items

- **Vaadin 8 → 24 is a rewrite, not an upgrade.** Vaadin 8 has no Jakarta build. This is the consuming application-side
  work, but it gates the flip date; the Cocoon fork should be ready and shelved rather than blocking on it.
- **Two jumps at once.** the consuming application moves Cocoon 2.2.1-workflow-1 → 2.3.x *and* javax → jakarta in one
  release. Gap G (§3.8) is the mitigation and must not be deferred to the end.
- **Axis 1.4 under Transformer is unproven here.** Axis 1.x does its own reflection over servlet types;
  a transformed jar may need runtime testing beyond "it links". Phase 4 should prove the SOAP endpoints
  before Phase 5 commits to them. Worth timeboxing, with "drop SOAP from the consuming application" as the fallback.
- **`cocoon-maven-plugin` is a separate repo** on its own release cadence — coordinate the two releases.
- **Quarantined blocks rot.** Once out of the reactor they stop compiling entirely. That is accepted,
  They now live in the top-level `legacy-blocks/` directory, with a README stating that
  nothing there compiles, so the status is visible without reading a POM.


---

## 8. Implementation notes (written after Phases 0-4)

What the plan got wrong or missed, recorded so the remaining phases are planned against
reality rather than against section 3.

### The baseline measurement was contaminated

The "186 success / 34 failure" figure in section 2 came from a build without `clean`.
`target/classes` held Eclipse-compiled classes from a `mvn eclipse:eclipse` run months
earlier, and Maven packaged them because they were newer than the sources. Those jars
carried ECJ's `Unresolved compilation problems` markers and broke downstream modules with
errors that looked exactly like Jakarta breakage (`cannot access ContentHandler`) but were
not. Two of the 34 "failures" — including the `cocoon-serializers-charsets` OSGi failure
written up as Gap F — evaporated on a clean build.

`.classpath`, `.project`, `.settings/` and `.DS_Store` are now git-ignored, and **every
gate must run `clean`**.

Related: `-Dmaven.test.skip=true` cannot be used at all. It skips test-jar creation, and
several modules — plus the consuming application — depend on `cocoon-*:test-jar`. Use `-DskipTests`.

### Scope corrections

- `cocoon-linkrewriter-impl` is a compile dependency of `cocoon-servlet-service-components`
  and had to come back into the default reactor. Section 2 had it out.
- `core/cocoon-blocks-fw` turned out to be referenced only by `dists`, so it was
  quarantined. That removes `BlockCallHttpServletRequest`, `BlockCallHttpServletResponse`
  and one of the two `ServletContextWrapper`s from the work in section 4 — a real
  reduction against what that table lists.
- The `legacy-blocks` profile does not build, and did not before this work either:
  `cocoon-portal-portlet-newimpl` and `-wsrp-impl` still declare a `2.3.0-SNAPSHOT`
  parent that no longer exists. Quarantine, as agreed, is not preservation.

### Phase 4 findings

- **The transformer silently does nothing without `jakartaDefaults`.**
  `transformer-maven-plugin` runs, logs success, and copies the jar through unchanged
  unless `<rules><jakartaDefaults>true</jakartaDefaults></rules>` is set. The first
  build "passed" with completely untransformed jars. Always verify the output.
- **Only `axis-1.4` should be transformed.** `axis-jaxrpc` and `axis-saaj` are spec APIs;
  transforming them would put a second copy of a spec package next to the genuine one.
  They are replaced by `jakarta.xml.rpc-api` and `jakarta.xml.soap-api`.
- **JAX-RPC keeps its javax packages.** `jakarta.xml.rpc:jakarta.xml.rpc-api` renamed only
  the Maven coordinates; the spec was dropped from Jakarta EE and never got a package
  rename. `javax.xml.rpc` on the classpath is correct. This validates the coordinate
  the consuming application already pins (1.1.4).
- **But that artifact still needed a shim.** Three of its classes reach into APIs that
  *were* renamed (`ServletEndpointContext`, `SOAPMessageContext`, `SOAPFaultException`)
  and would be `NoClassDefFoundError` on an EE 10 classpath. `jaxrpc-api-jakarta` fixes
  exactly that while preserving `javax.xml.rpc`.
- **`javax.annotation` cannot be matched by package.** The name is shared between Jakarta
  Annotations (renamed) and JSR-305 / `javax.annotation.processing` (not renamed). Spring
  and Micrometer reference the latter legitimately. Match by type name.
- **ehcache is a false positive.** It ships a servlet stack under
  `rest-management-private-classpath/`, deliberately off the classpath.

### Bugs found that predate the migration

- `ServletServiceRequest.Session.removeAttribute()` delegated to `removeValue()`, which
  delegated back. Any session-attribute removal inside a servlet-service call was an
  unbounded recursion that removed nothing. Fixed, with a regression test.
- `tools/cocoon-it-fw` put Jetty 6 and `servlet-api-2.5` on `cocoon-webapp`'s classpath.
  Retired; integration tests use `jetty-ee10-maven-plugin`.

### Deviations from the phase plan

- Phase 2 step 14 (the IT harness) was done as part of Phase 4, since removing Jetty 6 is
  what the classpath scan demanded.
- Phase 2 step 15 (`tools/cocoon-rcl`) is **not done**. It is still absent from
  `tools/pom.xml`, so `cocoon-rcl-webapp-wrapper` and `cocoon-rcl-spring-reloader` are
  still only available as 2.2-era javax builds. This belongs with Phase 5, next to the
  `cocoon-maven-plugin` re-release, because the two are used together.
- `core/cocoon-webapp` was kept buildable under `-P samples` rather than quarantined as
  section 2 said, because the plan's own runtime checkpoint needs it.

### State of the runtime checkpoint

The webapp deploys on Jetty 12 EE 10 and Spring's root context initialises with no
`NoSuchBeanDefinitionException`, no `javax.servlet` reference and no linkage error — so
the namespace work is complete as far as startup goes. It cannot yet serve a request:
`core/cocoon-webapp` has no root `sitemap.xmap`, because that is assembled by
`cocoon-maven-plugin`, which is a separate repository and unconfigured here. See
`jakarta-verify/BOOT-SMOKE-TEST.md`. Serving a request is a Phase 5 checkpoint, not a
Phase 3 one.
