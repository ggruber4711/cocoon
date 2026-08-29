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

## Operational note: clear the temp directory between runs

Cocoon's pipeline cache lives under the servlet temp directory. Reusing it across restarts
that changed block content produced a cross-block mix-up -- a request for the style block's
`styles/main.css` returned the forms block's `htmlarea.js`, served as `text/javascript`.
Start with a fresh temp directory when the deployed blocks have changed:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -P samples -pl core/cocoon-webapp \
  org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.16:run-war \
  -Djetty.http.port=8888 -Djava.io.tmpdir=$(mktemp -d)
```
