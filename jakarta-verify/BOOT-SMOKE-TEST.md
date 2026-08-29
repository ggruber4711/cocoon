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

As of the Phase 4 commit, the webapp deploys on Jetty 12 EE 10 and:

- Spring's root `WebApplicationContext` initialises;
- there is **no** `NoSuchBeanDefinitionException`, which is what a missed
  `ref="javax.servlet.ServletContext"` would produce (Gap D);
- there is no `javax.servlet` reference, `ClassCastException`, `NoClassDefFoundError`
  or `LinkageError` anywhere in the startup log.

In other words the Jakarta namespace change is complete as far as container startup and
Spring wiring are concerned.

## What still fails, and why it is not a migration problem

Bean `org.apache.cocoon.Processor` fails with:

```
ConfigurationException: Cannot resolve context://sitemap.xmap
MalformedURLException: context://sitemap.xmap could not be found. (possible context problem)
```

`core/cocoon-webapp` has no root `sitemap.xmap`, in the source tree or in the assembled
war -- the war contains only `test-suite/sitemap.xmap`. The root sitemap and the mounted
blocks are produced by `cocoon-maven-plugin`, which lives in a separate repository and is
not configured for this module in this reactor. Wiring that up is Phase 5, where the
plugin is re-released against the fork.

Until then this test verifies deployment and context initialisation, not request serving.
