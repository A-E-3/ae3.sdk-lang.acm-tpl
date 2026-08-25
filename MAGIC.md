# MAGIC.md — ae3.sdk-lang.acm-tpl

## For keeper-ae3

- **Stale dependency removed** (2026-08-25): `Requires: acm-base-api` (and the matching `.classpath` entry) was declared but never actually used — grepped all 28 Java files for any import of `acm-base-api`'s real packages (`ru.myx.s4.*`, `ru.myx.ae2.*`), zero hits. Removed from both `project.inf` and `.classpath`, in both the legacy (`/Volumes/workspace/myx`) and modern (`ws-myx.ae3-devel`) checkouts.
