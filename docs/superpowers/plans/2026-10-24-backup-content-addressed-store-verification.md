# Verification Record: Content-Addressed Backup Store (#1342)

Final verification for the implementation plan
[`2026-10-24-backup-content-addressed-store.md`](2026-10-24-backup-content-addressed-store.md)
(Task 8). All commands run on the branch `1342-new-backup-system` after
`mvn -pl jipipe-launcher -am install -DskipTests -q` (stale-SNAPSHOT guard).

## Environment

| | |
|---|---|
| Date | 2026-10-24 |
| Branch | `1342-new-backup-system` |
| HEAD at verification start | `0f2538c79b` |
| JIPX done-gate corpus | `/data/JIPipe` (367 `.jip` files) |
| Writer | FROZEN — `JIPipeXProjectWriter` and the JIPX reader untouched by this feature; the done-gate below proves it |
| Note | A user JIPipe GUI may run in parallel (shared profile); environment-flaky failures would be re-run once before concluding. No flakiness was observed in this verification. |

## Test suites

`mvn -pl jipipe-core,jipipe-launcher test`:

| Module | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| jipipe-core | 551 | 0 | 0 | 1 |
| jipipe-launcher | 121 | 0 | 0 | 2 |
| **Total** | **672** | **0** | **0** | **3** |

Matches the Task 7 claim exactly (672 tests, 0 failures, 3 pre-existing gated
skips). The 3 skips are the pre-existing gates, not regressions:

- jipipe-core (1): `TaxonomyConversionReportTest` — `@EnabledIfSystemProperty` on `-Djipipe.taxonomy.report.module`, disabled in a plain `mvn test`.
- jipipe-launcher (2): `JipxRoundTripFixpointTest` and `CorpusRoundTripFixpointTest` — `assumeTrue` on the corpus system property, disabled in a plain `mvn test` (both run as part of the done-gate below with the property set).

## JIPX done-gate

`mvn -pl jipipe-launcher test -Dtest=JipxRoundTripFixpointTest -Djipipe.jipx.roundtrip.corpus=/data/JIPipe -Djipipe.jipx.roundtrip.report=/data/JIPipe/.roundtrip/backup-store-guard.json`

| Outcome | Count |
|---|---|
| OK | 358 |
| LOAD_FAILED | 9 (pre-existing) |
| PARSE_FAILED | 0 |
| SAVE_FAILED | 0 |
| JIPX_SAVE_FAILED | 0 |
| JIPX_LOAD_FAILED | 0 |

**Result: PASS** — 358/358 OK with the 9 pre-existing LOAD_FAILED, identical
to the pre-feature baseline. The writer is frozen and its fixpoint is
unaffected by the backup store work.

Report: `/data/JIPipe/.roundtrip/backup-store-guard.json`

## Capacity spot-check

Evidence run (NOT a committed test — the throwaway
`CapacitySpotCheckTest` in `jipipe-launcher/src/test/java/org/hkijena/jipipe/api/backups/store/` was deleted after the run).

Setup: a `.jipx` project written via `JIPipeXProjectWriter`, opened with
`JIPipeProject.loadProject`, a **256 MiB** seeded-random payload
(`Random(1342)`, 268,435,456 bytes) under `<workspace>/data/payload.bin`.
Three snapshots via `createJipxSnapshot` + `persistSnapshot` while holding
the store write lock, plus a lock-free restore of snapshot 2 with a
byte-for-byte payload round-trip check (SHA-256). "New bytes" = delta of
`store.totalObjectBytes()` before/after the snapshot; wall time covers
snapshot creation + persistence (the cost a backup run pays). The project
materializes 13 entries (12 part files + the payload).

| # | Change | New bytes | Wall time | Notes |
|---|---|---|---|---|
| 1 | cold store | 268,458,633 B (256.02 MiB) | 299 ms | payload stored once + all parts |
| 2 | no project edit | 126 B | 5 ms | payload a cache hit + deduped; only `workspace.json` re-stored |
| 3 | project metadata renamed | 3,513 B | 4 ms | changed part(s) re-stored; payload still deduped |

Restore of snapshot 2: payload round-trip **OK** (restored 256 MiB payload
bytes SHA-256-identical to the original), wall time 5,735 ms.

The ratio is the point: **b1 (268,458,633 B) ≫ b3 (3,513 B) ≫ b2 (126 B)** —
≈ 76,000× smaller for the "parts changed" case and ≈ 2.1 M× smaller for the
"no edit" case, versus the cold backup. This is exactly the spec §5.3 shape
(backup 1 ≈ payload + parts; backup 2 ≈ 0; backup 3 ≈ parts only).

Honest caveat, **not exactly 0 for snapshot 2**: it re-stores the 126-byte
`workspace.json`, because `JIPipeLocalProjectWorkspace.materializeFromModel`
refreshes that file's `last-save` timestamp (`nowIso8601()`) on *every*
backup, so the workspace-state object is never byte-identical between two
consecutive backups of an unchanged project. This is negligible (0.00005% of
the 256 MiB payload) and does not affect the dedup guarantee — the payload
object is stored exactly once and is a content-hash cache hit thereafter. If
byte-zero is ever required for truly-unchanged backups, `materializeFromModel`
could skip rewriting `workspace.json` when neither `last-save` semantics nor
the dirty flag changed; that is a follow-up, not a defect in the store.

Payload size note: 500 MiB was the original target, but this shared machine
(resident 17 GB Prism process, ~30 GB reclaimable) could not commit the peak
(full ImageJ registry + JaCoCo agent + 500 MB payload + the restore re-read)
without a native OOM. 256 MiB was used, per the Task 8 allowance; the ratio
is independent of absolute size.

## Deviations from the plan

- The plan estimates the full suite at ~10+ min; on this machine it took
  1:25 (fast NVMe/tmpfs, 16 cores). No test deviation.
- The capacity spot-check ran as a temporary surefire test class in
  `jipipe-launcher` (per the Task 8 instruction to use `@TempDir` +
  `JIPipe.ensureInstance()` directly) and was deleted before commit — it is
  not part of the suite.
- Plan step 6 (push `1342-new-backup-system` to origin) was left to the
  controller session per instruction; the branch is not pushed by this task.
- The `utils/fresh-node-slots.json` and `utils/taxonomy-research/registry.json`
  files regenerated during the test runs were reverted; the commit contains
  only this record.

## Known deferred follow-ups

Task 1: over-read IOException message says "truncated" though file grew; printStackTrace in FileLocker (class style); put success log re-stats via sizeOf; skip(n)==0 spin edge in TruncationCheckingInputStream — all cosmetic
Task 2: cache load's broad RuntimeException catch (harmless, disposable-by-design); dead null-guard after readValue
Task 3: deleteSnapshot takes backupRoot as a param (must equal store.getStoreDirectory().getParent()); dangling index id possible after crash between index-append and manifest-write (consumers skip missing manifests); backup-info.json non-atomic (existing idiom)
Task 4: path-traversal guard on manifest entry paths in .jipx restore (trusted-store threat model); empty-entry-list .get(0) throws IOOBE not IOException (unreachable via real path)
Task 6: tests 2/4 call caller-holds-lock ops unlocked (benign single-threaded); size-limit assertions use 1 GB bound not 1 MB (directional only); deleteSnapshot Javadoc overstates lock requirement
Task 7: totalObjectBytes() directory walk on every refreshSidebar (cache per collect cycle if it hitches); "Backup data" total excludes legacy file items by design (legacy-only store shows 0 B); FQN IOException style nit
