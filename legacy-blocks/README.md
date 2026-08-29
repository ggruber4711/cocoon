# Legacy blocks — not migrated, not buildable

Nothing in this directory is part of the build. These 43 blocks were left behind when the
reactor was cut down to the Jakarta EE 10 artifact set, and **none of them compiles**.

They are kept because deleting them would lose the only record of how the functionality
worked, and some may be worth reviving. They are out of the reactor rather than out of the
repository, and they are here rather than under `blocks/` so that the distinction is
visible from the directory listing instead of hidden in a Maven profile.

There is deliberately no aggregator POM here. Each block's POM still declares
`cocoon-blocks-modules` as its parent, which no longer resolves from this location — that
is accurate, not an oversight.

## Why each group is out

**Cannot be migrated at all.** These depend on specifications that were never brought into
Jakarta EE, so there is no target to migrate to:

- `cocoon-portal` — the portlet sub-modules use `javax.portlet`. The Portlet specification
  (JSR 362) was never part of Jakarta EE and has no `jakarta.portlet` equivalent. The
  non-portlet parts of the block are plain servlet code and could be revived, but the
  block is quarantined as a unit.
- `cocoon-ojb` — JDO, an Apache specification that stayed on `javax.jdo`.
- `cocoon-xsp`, `cocoon-jsp`, `cocoon-taglib` — JSP and JSTL. Migratable in principle
  (`jakarta.servlet.jsp`), but unused downstream and not worth the work.

**Not used by the consuming application.** Everything else here is simply outside the
shipped artifact set. Most would migrate with the same treatment the shipped blocks got —
swap the servlet/mail/JMS artifacts, chase the Servlet 6.0 API changes — but nothing
depends on them, so nobody has paid for it:

`cocoon-asciiart`, `cocoon-authentication-fw`, `cocoon-bsf`, `cocoon-captcha`,
`cocoon-chaperon`, `cocoon-cron`, `cocoon-databases`, `cocoon-deli`, `cocoon-eventcache`,
`cocoon-faces`, `cocoon-html`, `cocoon-imageop`, `cocoon-itext`, `cocoon-javaflow`,
`cocoon-jcr`, `cocoon-jfor`, `cocoon-jms`, `cocoon-lucene`, `cocoon-midi`, `cocoon-naming`,
`cocoon-petstore`, `cocoon-profiler`, `cocoon-proxy`, `cocoon-python`, `cocoon-qdox`,
`cocoon-querybean`, `cocoon-repository`, `cocoon-scratchpad`, `cocoon-session-fw`,
`cocoon-slide`, `cocoon-slop`, `cocoon-stx`, `cocoon-tour`, `cocoon-validation`,
`cocoon-velocity`, `cocoon-web3`, `cocoon-webdav`, `cocoon-xmldb`.

Two of these were already broken before the migration: `cocoon-portal-portlet-newimpl` and
`cocoon-portal-wsrp-impl` declare a `2.3.0-SNAPSHOT` parent that no longer exists, so they
did not build on the pre-migration trunk either.

## Reviving one

1. Move the directory back under `blocks/`.
2. Add it to the `<modules>` list in `blocks/pom.xml`, and split any `*-sample`
   sub-module into that POM's `samples` profile, following the blocks already there.
3. Expect the same work the shipped blocks needed: `javax.servlet-api` →
   `jakarta.servlet-api` in the POM, then the Servlet 6.0 removals. Section 4 of
   [../plans/jakarta-ee10-fork.md](../plans/jakarta-ee10-fork.md) lists them with the call
   sites that were affected.
4. Check `jakarta-verify` still passes: any third-party dependency the block drags in has
   to be Jakarta-clean, or gain a shim under `../jakarta-shims/`.
