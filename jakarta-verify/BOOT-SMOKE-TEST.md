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

### Concurrent first requests fail during sitemap compilation (not fixed)

Distinct from the above and much less severe, because it fails loudly. On a cold start,
the first burst of concurrent requests produces:

```
ProcessingException: Failed to process reader
Caused by: IllegalArgumentException: setContentLength(3653) when already written 7462
```

15 of 20 requests failed in a cold burst; an identical burst immediately afterwards
returned 40/40 correct, and it does not recur once the pipelines are warm. The content
length of one response is being applied to a response that has already had another
response's bytes written to it, which points at the environment/response plumbing during
concurrent sitemap compilation rather than at the reader.

Practical impact: requests arriving concurrently in the seconds after a deploy can fail.
Worth a warm-up request before putting an instance into a load balancer.

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
