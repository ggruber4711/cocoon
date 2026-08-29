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

## Known defect: detail pages lose their stylesheet

Pages rendered through the style block's servlet service (`/`, `/samples/`) are fully
styled. Pages that instead apply `simple-page2html.xsl` as a plain transform -- most
sample detail pages, including the forms samples -- come out unstyled.

Cause: `simple-page2html.xsl` declares

```xml
<xsl:param name="contextPath">servlet:/</xsl:param>
```

and then emits `{$contextPath}/styles/main.css`, so when the caller does not set the
parameter the link becomes `/cocoon-samples-style-default//styles/main.css`. **Jetty 9
served that; Jetty 12 rejects it with `400 Ambiguous URI empty segment`.** The same page
also shows "Apache Cocoon 2" and "Copyright (c) ????", the other two unset defaults, so
these pages have been rendering with default parameters for a long time -- only the
consequence changed.

This is a Jetty 9 -> 12 behaviour change, not a Jakarta namespace problem, and it is
worth knowing about for webdesk: any generated link containing `//` becomes a 400 on
Jetty 12, anywhere in an application.

Two fixes were tried and neither is right yet:

- Changing the parameter default to `servlet:` removes the double slash but resolves
  against the *calling* block, so the links point at `/samples/forms/styles/main.css`
  instead of the style block. Reverted.
- A `jetty.xml` relaxing `UriCompliance` (even to `UNSAFE`) is loaded by the plugin --
  the log confirms it -- but has no effect, because the `jetty-ee10-maven-plugin`
  serves from its own connector rather than the one declared in the XML. Reverted.

The durable fix is to stop generating the URI: set `contextPath` explicitly where
`simple-page2html.xsl` is applied as a transform, or normalise the concatenation in the
stylesheet. Sample cosmetics only -- it does not affect the artifact set webdesk
consumes.
