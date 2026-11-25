# MAGIC.md — ae3.sdk-lang.acm-tpl

## For keeper-ae3

- **Stale dependency removed** (2026-08-25): `Requires: acm-base-api` (and the matching `.classpath` entry) was declared but never actually used — grepped all 28 Java files for any import of `acm-base-api`'s real packages (`ru.myx.s4.*`, `ru.myx.ae2.*`), zero hits. Removed from both `project.inf` and `.classpath`, in both the legacy (`/Volumes/workspace/myx`) and modern (`ws-myx.ae3-devel`) checkouts.
- **Correction to the above (2026-08-26): that grep missed a real usage.** `java/ru/myx/renderer/tpl/TplParser.java` (lines 8, 367, 563) imports and uses `ru.myx.ae3.act.Context` — which also lives in `acm-base-api`, a package the prior pass's grep (`ru.myx.s4.*`/`ru.myx.ae2.*` only) never checked for. With `acm-base-api` off this project's own `.classpath`, the checked-in `bin/ru/myx/renderer/tpl/TplParser.class` is an Eclipse "unresolved compilation problem" stub: it throws `java.lang.Error: Unresolved compilation problems: Context cannot be resolved` at runtime, at the exact `TplParser.java:367`/`:563` call sites, regardless of what's on the *runtime* classpath — confirmed empirically while building a standalone `.tpl`-render harness (see `keeper-ae3.armed.md`'s own Domain knowledge for the harness technique itself). Recompiling `java/` fresh with `acm-base-api/bin` added back to the classpath produces a clean compile, zero errors. Reinstated (2026-08-28) by commit `6db8e15` (`* minor: project meta-data`): `acm-base-api` is declared again in `project.inf`'s `Requires:` and as a `/acm-base-api` entry in `.classpath`. The dependency is real and must not be removed again.
- **A template that opens `FINAL` cannot be rendered whole on a synthetic context, so render a region of it
  instead.** `<%FINAL: "text/html" %>` at `html-page.tpl:2` is answered by `TagFINAL_EXIT`, whose
  `Reply.string("TPL_FINAL", Context.getRequest(ctx), content)` at `TagFINAL_EXIT.java:34` does not return on
  a context built by `Exec.createProcess` alone: the run reaches `callNE0` and then sits with no exception and
  no output. A two-line template carrying the tag hangs identically, so the tag is the cause. The working
  shape for a single file is to cut the region under test out of the template, from its `<%CODE: ... %>` line
  to a line past the region, close it with `<%/CODE%>`, and compare two versions of that cut against each
  other. Both versions of the YUI block removal in `html-page.tpl` rendered this way, and their bodies matched
  byte for byte once the one removed tag was taken out.
- **The order that avoids the static-init trap, and the four names a synthetic scope needs.**
  `Engine.createGuid()` runs before `Evaluate.registerLanguage(AcmTplLanguageImpl.INSTANCE)`, otherwise a
  circular static-init between `ImplementBase` and `ImplementEngine` throws `NullPointerException`. The scope
  then needs `context` (whose `query` carries `protocolVariant`, `parameters`, `settings`, `attributes`,
  `urlBase`, `resourcePrefix`, `resourceIdentifier` and `target`), plus `content`, `attributes` and `Format`.
  A template's later blocks also read `requireJs`, `requireCss` and `require` off `content`, and `useDebug`
  must stay off because its block reads a bare `attributes` the template itself never binds.
