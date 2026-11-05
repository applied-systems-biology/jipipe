# Verification record — modern dialog API migration (#1341)

- **Date:** 2026-11-04
- **Branch:** `1341-modern-dialog-api`, HEAD `50598b41e4`, base `b8f427c89f` (merge-base with `origin/master`)
- **Spec:** `docs/superpowers/specs/2026-11-03-modern-dialog-api-design.md`
- **Note:** push is deliberately deferred until after the final whole-branch review; the remote must receive the reviewed branch.

## Verification battery

| # | Command | Expected | Result |
|---|---|---|---|
| 1 | `mvn -q compile` (full reactor, repo root) | success | **PASS** — exit 0, no diagnostics |
| 2 | `mvn test -pl jipipe-core` (full suite) | 0 failures | **PASS** — `Tests run: 947, Failures: 0, Errors: 0, Skipped: 2`, BUILD SUCCESS |
| 3 | `grep -rn "JOptionPane" --include="*.java" jipipe-core/src/main plugins \| wc -l` | 0 | **PASS** — 0 |
| 4 | `grep -rn "JColorChooser" --include="*.java" jipipe-core/src/main plugins` (non-import) | 0 live references | **PASS** — 4 lines, all comments/javadoc (see below) |
| 5 | `git log --oneline b8f427c89f..HEAD` | commit list | 19 commits (below) |
| 6 | `git diff --stat b8f427c89f..HEAD \| tail -1` | overall size | 145 files changed, 5117 insertions(+), 1549 deletions(-) |

The 2 skipped tests are pre-existing and unrelated to this work:

1. `org.hkijena.jipipe.api.settings.PaletteStorageLifecycleTest` — assumption abort (`Assumptions.assumeFalse(JIPipe.isInstantiated(), …)`); the live-service case is covered by a launcher test.
2. `org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyConversionReportTest` — opt-in gate, only runs with `-Djipipe.taxonomy.report.module=<prefix>`.

## Zero-JOptionPane proof

`grep -rn "JOptionPane" --include="*.java" jipipe-core/src/main plugins` returns nothing: zero occurrences
(imports or uses) across jipipe-core main sources and all plugin modules. The regression gate
`NoJOptionPaneGate` (added in commit `2ffcfef3a1`) enforces this at test time by scanning every module's
`src/main/java` tree and failing on any token occurrence, so the count cannot silently regress.

JColorChooser: the only remaining mentions are prose, not code —

- `JIPipeDesktopColorChooserButton.java:63` — comment (alpha-parity rationale)
- `JIPipeDesktopColorChooserDialog.java:36, 41, 49` — javadoc/comment (`{@link JColorChooser}` contract references)

No `import javax.swing.JColorChooser` and no executable reference remain.

## Branch commits (b8f427c89f..HEAD)

```
50598b41e4 refactor(dialogs): wave 4 — sweep to zero JOptionPane, delete superseded UIUtils wrappers (#1341)
9589de60b0 fix(dialogs): color chooser button keeps no-alpha parity with old JColorChooser (#1341)
6221363447 refactor(dialogs): wave 3 — message dialogs + color chooser (plugins) (#1341)
d8fa888d87 refactor(dialogs): wave 3 — message dialogs + color chooser (core) (#1341)
4a3316ecfe fix(dialogs): migrate remaining showConfirmDialog callers; html pass-through in addMessage (#1341)
7b3ff218b7 refactor(dialogs): wave 2 — input dialogs to typed facade (plugins) (#1341)
da2d5c75e4 refactor(dialogs): wave 2 — input dialogs to typed facade (core) (#1341)
0c6ac507fa fix(dialogs): root pane default button consumes Button.asDefault (#1341)
3ec0f9ef33 refactor(dialogs): wave 1 — option+confirm dialogs to the facade (plugins) (#1341)
a6bdd75752 refactor(dialogs): wave 1 — option+confirm dialogs to the facade (core) (#1341)
2ffcfef3a1 test(dialogs): NoJOptionPaneGate — fails until migration completes (#1341)
d7d51c404e test(dialogs): headful showAndWait plumbing guard (#1341)
0ead95fc5f feat(dialogs): facade unique-text input, color delegation, Builder (#1341)
e01074d000 feat(dialogs): facade typed inputs (text, multiline, integer, double) (#1341)
d0e42d23c0 feat(dialogs): facade message/error/confirm/choose with Optional+Button semantics (#1341)
e9fadc5dab feat(dialogs): JIPipeDesktopDialog chrome + DialogPresenter test seam (#1341)
b7d7bc7e96 feat(dialogs): Button descriptor with OK/CANCEL/YES/NO constants (#1341)
d331b7e1c9 docs(dialogs): implementation plan — 12 tasks, wave-migrated, gate-tested (#1341)
a9d1678934 docs(dialogs): design spec for the modern dialog API (#1341)
```

## Deliberate behavior changes (sanctioned by the spec)

1. **X / Escape dismisses every dialog.** Old `JOptionPane` mapped `CLOSED_OPTION` per call site, and some
   sites left dismissal unhandled — it fell through as if YES (e.g. `AddToROIManagerDataDisplayOperation`).
   Now every dialog dismisses to `Optional.empty()` / not-confirmed, uniformly.
2. **Empty `inputText` submit = dismiss.** Pressing Enter on empty text dismisses the dialog (empty result)
   instead of leaving it open.

## Bug fixes found en route

1. **`ExpressionBuilderUI:424` (raw-int switch).** The original `showOptionDialog` result was switched on
   raw int indices (`case 0/1/2/3`) — fragile against option-order changes. Migrated to an identity switch on
   `Button` constants (`Ignore`, `Insert` (default), `Replace`, explicit `Button.CANCEL`); Ignore and dismissal
   fall through, preserving original routing.
2. **`FormsDialog:137` (Show-errors conflation).** The original `showConfirmDialog` conflated the "Show errors"
   button with window-close: both took the close-tabs → issues-view → `return false` path. The conflation is a
   pre-existing quirk and is preserved verbatim under `choose(Yes, No, Show errors.asDefault())` — dismissal
   takes the same close-tabs path. Not silently "fixed".

## Follow-up candidates

1. **`JIPipeEnvironmentConfigurator:188` swapped args (latent original bug).** The original message-dialog call
   had title/message swapped (the title rendered as the body). Preserved verbatim with a site comment; a
   follow-up can un-swap it as a deliberate UX fix.
2. **Color-button alpha enablement (deliberate future option).** `JIPipeDesktopColorChooserButton` uses
   `chooseColor(..., withAlpha=false)` for parity with the old plain `JColorChooser` (no alpha rail). Flipping
   to `true` enables alpha selection — a one-word change, intentionally deferred.
3. **Deferred minors** are tracked in the SDD ledger:
   `.superpowers/sdd/2026-11-03-modern-dialog-api/progress.md` (per-task "minor (deferred)" entries).
