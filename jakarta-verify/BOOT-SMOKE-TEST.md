# Booting the demo webapp on Jetty 12 (EE 10)

Phase 2 checkpoint 16 and Phase 3 checkpoint 20 of `../plans/jakarta-ee10-fork.md`.
This is a manual smoke test; it is not wired into the build, because the demo webapp
cannot currently serve a request for a reason unrelated to the migration (see below).

## Running it

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -P samples -DskipTests -Drat.skip=true clean install
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -P samples -pl core/cocoon-webapp \
  org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.16:run-war -Djetty.http.port=8888
```

Use `run-war`, not `run`. The `run` goal adds the project's dependency classpath on top
of the already-assembled `WEB-INF/lib` and Jetty then rejects the duplicated
`spring-web` web-fragment with `IllegalStateException: Duplicate fragment name`.

## What it establishes

The samples webapp **runs** on Jakarta EE 10 / Jetty 12, end to end:

- Spring's root `WebApplicationContext` initialises with no exception in the startup log;
- no `NoSuchBeanDefinitionException`, which is what a missed
  `ref="javax.servlet.ServletContext"` would have produced (Gap D);
- no `javax.servlet` reference, `ClassCastException`, `NoClassDefFoundError` or
  `LinkageError` anywhere;
- `GET /` serves Cocoon's own "your Apache Cocoon installation was successful" page;
- `GET /samples/` serves the block index, fully styled, aggregated across the mounted
  blocks -- so block mounting, the servlet-service dispatcher, XSLT pipelines and static
  resource serving all work;
- `GET /samples/forms/` and the Cocoon Forms samples render, and submitting the
  Registration form POSTs through to `/samples/forms/continue` with values retained,
  which exercises flowscript continuations and session handling through the migrated
  request/response wrappers.

### The root sitemap

`core/cocoon-webapp/src/main/webapp/sitemap.xmap` had to be added. COCOON-2120 deleted the
previous one when samples handling moved into the `cocoon-welcome` block and the
`DispatcherServlet` took over `/*`, but `cocoon-sitemap-impl` still registers a root
`TreeProcessor` on `context://sitemap.xmap` unconditionally
(`META-INF/cocoon/avalon/cocoon-core-sitemap.xconf`), and the Avalon bridge instantiates
it eagerly. Without the file the Spring context fails with `Cannot resolve
context://sitemap.xmap` and the webapp never deploys. Request routing does not go through
it. The alternative fix -- making that xconf declaration conditional -- is a change to
core behaviour that webdesk also depends on, so the file is the safer option here.

## Sample detail pages: fixed

Detail pages initially rendered unstyled. `simple-page2html.xsl` defaulted its
`contextPath` parameter to `servlet:/` while its sibling `simple-samples2html.xsl` used
`servlet:` -- a one-character difference. The servlet-service link rewriter appends a
URI's scheme-specific part verbatim to the block mount path
(`ServletServiceContext.absolutizeURI`), so `servlet:/` + `/styles/main.css` became
`/cocoon-samples-style-default//styles/main.css`. **Jetty 9 served that; Jetty 12 rejects
it with `400 Ambiguous URI empty segment`.**

Fixed in two places:

1. `simple-page2html.xsl` now defaults `contextPath` to `servlet:`, matching its sibling.
   That is correct when the stylesheet runs inside the style block, via
   `service/common/simple-page2html`.
2. The 15 sitemap sites that apply the stylesheet as a plain `<map:transform>` from
   another block now pass `contextPath` explicitly as `servlet:style-default:`. The
   default cannot serve them: `servlet:` means "this block", which for those pages is the
   caller, not the style block.

The same sites also now pass `version` and `year` from `{cocoon-properties:*}`, which the
style block's own service always did. Without them the pages rendered "Apache Cocoon 2"
and "Copyright (c) ????" -- the same unset-parameter defect, just less visible than the
missing stylesheet.

Worth carrying into the webdesk migration: any generated link containing `//` is a 400 on
Jetty 12, anywhere in an application. It is a Jetty 9 -> 12 behaviour change, unrelated to
the Jakarta namespace.

## Operational note: clear the temp directory between runs, and hard-refresh the browser

Cocoon's pipeline cache lives under the servlet temp directory. Reusing it across restarts
that changed block content produced a cross-block mix-up: a request for the style block's
`styles/main.css` returned the forms block's `htmlarea.js`, with HTTP 200 and
`Content-Type: text/javascript`.

Two things make this worse than an ordinary stale cache:

- It is served as a **success**, so nothing upstream treats it as an error.
- **Browsers then cache it.** This cost real debugging time here: after the server was
  fixed, the Cocoon Forms tab bar still did nothing, because the browser held a poisoned
  `forms-lib.js` and `forms_showTab` was never defined. `curl` showed the correct file the
  whole time; only a cache-bypassing reload made the page work. If you see JavaScript
  behaving as though a library did not load, check `performance.getEntriesByType('resource')`
  for a `transferSize` of 0 before suspecting the code.

Worth carrying into the webdesk migration: if a deployment reuses its work directory across
an upgrade, clients can cache cross-wired resources, and no server-side check will catch it.

Start with a fresh temp directory when the deployed blocks have changed:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -P samples -pl core/cocoon-webapp \
  org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.16:run-war \
  -Djetty.http.port=8888 -Djava.io.tmpdir=$(mktemp -d)
```


## Two defects found by running the samples

Both predate the Jakarta migration. Neither is caused by anything on this branch, but
both are in code webdesk consumes.

### LinkRewriterReader was a Spring singleton (fixed)

`cocoon-servlet-linkRewritingReader.xml` declared
`org.apache.cocoon.reading.Reader/servletLinkRewriter` with no `scope`, so Spring made it
a singleton. Every other Reader, Generator, Transformer and Serializer in the codebase
declares `scope="prototype"`, and for good reason: a Cocoon Reader holds per-request state
(this one keeps `request`, `response`, `inputSource`, `encoding`, `expires`).

Concurrent requests therefore overwrote each other's source and content type. Measured on
the samples webapp, 40 concurrent requests alternating between a `.css` and a `.js` URL:
**18 of 20 requests for the JavaScript URL returned the CSS**, with HTTP 200 and
`Content-Type: text/css`. The instance then kept the last request's state, so afterwards
*every* resource URL returned the same wrong content until restart -- and browsers cached
it.

Only `.css`, `.js` and the catch-all `**` are affected, because only those use
`<map:read type="servletLinkRewriter">`. Plain `map:read` matches for `.gif`, `.jpg` and
`.ico` were correct throughout, which is what localised the fault.

Fixed by adding `scope="prototype"`. After the fix, three rounds of 40 concurrent mixed
requests returned 120/120 correct responses, and the state no longer persists.

This is the one to carry into webdesk: it is silent, it serves one user's resource for
another's URL, and it survives in client caches.

### Concurrent requests against a cold cache corrupt the response (NOT fixed)

This is the `setContentLength` failure that has bitten webdesk before. It is not fixed
here. What follows is what was established while trying to, so the next attempt does not
start from scratch.

**Symptom.** On a cold cache, a burst of concurrent requests for the same resource fails:

```
ProcessingException: Failed to process reader
Caused by: IllegalArgumentException: setContentLength(3653) when already written 7252
```

Measured: 15-17 of 20 concurrent first requests fail. An identical burst once the cache is
warm returns 20/20, repeatably. Sequential cold requests are always fine.

**It is not the reader, and not link rewriting.** A plain `<map:read>` of a JPEG, with no
`LinkRewriterReader` involved, fails the same way: 15 of 20. Any reader that implements
`CacheableProcessingComponent` is affected. This matters, because a per-reader workaround
cannot fix it in general -- which is presumably why bundling resources was what finally
made it go away in webdesk: fewer concurrent resource requests, so the window closes.

**The response is written more than once.** The "already written" figure is consistently
close to a multiple of the resource size (7252 and 7462 against a 3653-byte file). The
exception is therefore a symptom of duplication, not the fault itself.

**The exception is load-bearing. Do not suppress it.** Making the reader non-cacheable
(`getKey()` and `getValidity()` returning null) removes the exception completely -- and
the cold burst then returns HTTP 200 `text/css` with 9987, 10905 and 11115 byte bodies for
a 3653-byte file. Silently serving triplicated content is worse than failing loudly. This
was tried and reverted.

**Where it lives.** `AbstractCachingProcessingPipeline.processReader`, in the
cache/lock protocol around `waitForLock` / `generateLock`. The two throw sites are the
cache-hit branch and the `shouldSetContentLength()` branch; which one fires depends only
on how the reader is configured, so neither is the cause. Ruled out along the way:
`PoolableProxyHandler` holds its pooled component in a `ThreadLocal`, and the
request/response/context beans in `cocoon-ssf-callstack.xml` are correctly
`scope="call"` with scoped proxies.

**A harness exists, and it does not reproduce it.**
`core/cocoon-pipeline/cocoon-pipeline-impl/src/test/.../ReaderColdCacheConcurrencyTestCase`
drives `processReader` with 20 concurrent threads against a shared cache and transient
store, each thread with its own environment and pipeline instance, exactly as the Avalon
pool arranges it. It also installs per-thread Spring request attributes, without which
`generateLock` stores a null lock and the whole protocol is inert -- a test omitting that
passes vacuously. A third case reuses one pipeline instance for two requests without
`recycle()`, which is what `PoolableProxyHandler` does while the request scope has not
been destroyed.

All three pass: every response is written exactly once. So the duplication does not
originate in the pipeline's cache and lock protocol. It must come from a layer the
harness does not exercise -- the servlet-service block dispatch, `HttpEnvironment`,
`HttpServletResponseBufferingWrapper`, or the call stack. Also checked and cleared by
reading: `HttpServletResponseBufferingWrapper.resetBufferedResponse` looks unsafe because
it silently does nothing when `bufferResponse` is false, but that flag is only false
outside the 404-plus-super path that calls it.

**This is a known defect, not a migration regression.** webdesk tracked it as WD-2490,
opened 2015-12-23 against Jetty in dev mode and closed in 2019 with "since we are using
WRO (Web resource optimizer) this problem is not relevant for production. For DEV mode we
live with the problem." The 2015 diagnosis was the same shape as what is seen here: *"It
seems that 2 resources were messed up here: manifest.js and common.js"* -- one URL
delivering another URL's content -- and the same pipeline frames
(`AbstractCachingProcessingPipeline.processReader`, `PoolableProxyHandler`,
`ServletServiceContext$PathDispatcher.forward`). Only the final cause differs with the
container: `IOException: Closed` on Jetty 6, `setContentLength(N) when already written M`
on Jetty 12. Ten years and two servlet stacks apart, so it is neither Jakarta nor Jetty 12
specific.

**Measured on a running samples webapp with temporary instrumentation** (removed again;
see git history of this file for what was added):

- *Response objects are not shared and not recycled.* Logging `identityHashCode` of the
  whole wrapper chain -- `HttpResponse` / `HttpServletResponseBufferingWrapper` /
  `ServletApiResponse` -- gave a unique identity at every level for all 25 requests
  across two bursts, with no value reappearing. Cross-request sharing and Jetty response
  recycling are both ruled out.
- *The reader is not run twice.* Instrumenting `ResourceReader.generate()` gave three
  invocations for twelve concurrent requests, each with its own reader instance, response
  and output stream. The cache lock does what it is meant to: one thread generates, the
  rest reuse.
- *The failing requests are the ones served from cache*, so they fail on the cache-hit
  branch of `processReader` at `environment.setContentLength(response.length)`.
- *The `getOutputStream` FIXME is a red herring.* Each environment calls
  `getOutputStream` exactly once, with either the buffer size or 0, never both, so the
  inconsistent second-call behaviour the FIXME describes never occurs on this path.

**One measurement that looks like evidence and is not.** An instrumented
`setContentLength` reported that the environment had written 0 bytes at the moment it
failed. That is true by construction -- the cache-hit branch sets the length before
writing the body -- so it says nothing about who wrote the bytes Jetty is counting. Noted
here because it is an easy trap to fall into twice.

**Next step for whoever picks this up.** Reproduce one layer higher, with a real
`HttpEnvironment` and the buffering wrapper in place, driving `BlockServlet` rather than
the pipeline. The harness above is the starting point and shows what does not need
re-testing.

**Mitigation until it is fixed.** Warm the resource URLs before an instance takes traffic,
or serve them through a bundler so there is no concurrent burst of first requests.

### CAPTCHA sample (fixed)

The CAPTCHA image 500'd with
`ClassCastException: org.apache.batik.dom.GenericElement cannot be cast to
org.w3c.dom.svg.SVGSVGElement`. Two causes, both fixed:

- `captcha-image.xml` declared no SVG namespace on its root `<svg>` element.
- More importantly, `SVGBuilder.startDocument` seeded Batik's namespace map with the
  `svg` prefix but not the default (empty) prefix. Batik's `SAXDocumentFactory` resolves
  an element's namespace from that map by prefix, not from the URI in the SAX event, and
  a Cocoon pipeline reports namespaces through `startPrefixMapping` rather than repeating
  them as `xmlns` attributes. Any unprefixed SVG document therefore built
  `GenericElement`s. This affects `cocoon-batik-impl` generally, not just the sample.
