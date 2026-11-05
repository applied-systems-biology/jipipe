# Modern Dialog API Design (#1341)

**Work item:** [RGerst/jipipe#1341](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1341) — "Modern API for dialogs"
**Status:** design approved in chat 2026-11-03 (API semantics, plain `JButton`s, full migration)
**Branch:** `1341-modern-dialog-api`

## 1. Problem

JIPipe's desktop code uses `JOptionPane` at **~320 call sites in ~113 files**:

| Call | Sites | Notes |
|---|---|---|
| `showMessageDialog` | 136 | |
| `showConfirmDialog` | 112 | returns raw `int` |
| `showInputDialog` | 40 | returns `null` on empty *or* cancel |
| `showOptionDialog` | 30 | the dangerous class — 21 use `YES_NO_CANCEL_OPTION` |

The API's design forces developers to remember things it should guarantee:

1. **CLOSED vs CANCEL.** Window X (and Escape, via the scattered
   `UIUtils.addEscapeListener` workarounds) produce `CLOSED_OPTION`, distinct from
   `CANCEL_OPTION`. Forgetting this treats "user closed the window" as a real
   choice. MR !174 (merged) fixed exactly three such dialogs ("pressing X on
   option dialogs now cancels … see #1341 for the systematic follow-up") — this
   issue is that follow-up. The remaining unpatched sites still have the bug
   latent; some already carry inline `// the window's X / Escape cancels, too`
   case workarounds (`JIPipeDesktopProjectWindow:291`, `:659`, …).
2. **Int results.** `showConfirmDialog`/`showOptionDialog` return option ints;
   comparison mistakes (`==`, `!=`, forgetting one branch) are invisible to the
   compiler.
3. **No input types beyond a single-line `String`.** Typed numeric input,
   multiline text, unique-name entry, and color selection exist only as
   ad-hoc wrappers in `UIUtils` (`getIntegerByDialog`, `getMultiLineStringByDialog`,
   `getUniqueStringByDialog`, `selectColor`), each hand-rolling its own
   `JDialog` + button row with subtly different behavior.

## 2. Goals

1. **One modern dialog API** — a typed facade plus a low-level builder — in which
   the closed/cancel confusion is *unrepresentable*: every input/choice dialog
   returns `Optional<T>` or `boolean`, where "dismissed" (Cancel button, X,
   Escape) maps to `Optional.empty()` / `false` by construction.
2. **New input types** the standard set lacks: multiline input, typed integer /
   double input, unique-string input (re-prompts inline on collision), and color
   selection (delegating to the already-modern `JIPipeDesktopColorChooserDialog`
   from #1339).
3. **Full migration** of all ~320 `JOptionPane` call sites to the new API. After
   the work item, `JOptionPane` has **zero references in main code**, enforced by
   a source-scan gate test.
4. **Behavior preservation** for the migrated calls, with one deliberate,
   systematic exception: X/Escape always dismiss (the !174 fix generalized).

## 3. Non-goals

- No non-modal dialogs (everything stays application-modal).
- No styled/"modern" buttons: dialogs use **plain `JButton`s** (explicit user
  instruction 2026-11-03: "NO MODERN BUTTONS — our normal buttons are fine").
- No restyling of the theme; no changes inside `JIPipeDesktopColorChooserDialog`,
  file choosers, `JIPipeDesktopUserFriendlyErrorUI`, or any dialog the facade does
  not touch.
- No Swing *interaction* tests (no pixel/layout assertions, no robot-driven
  clicking); plumbing is tested by direct `doClick()` on constructed (never
  shown) dialogs.
- No API for headless/CLI use.

## 4. API design

New package `org.hkijena.jipipe.desktop.commons.dialogs` in **jipipe-core**
(next to the other `desktop/commons` components; jipipe-core is on every module's
classpath, so plugins can use it directly).

### 4.1 `Button` — immutable button descriptor

```java
public final class Button {
    public static final Button OK;      // "OK"
    public static final Button CANCEL;  // "Cancel"
    public static final Button YES;     // "Yes"
    public static final Button NO;      // "No"

    public static Button custom(String label);
    public static Button custom(String label, Icon icon);
    public Button asDefault();          // returns a copy flagged as the default

    public String getLabel();
    public Icon getIcon();              // may be null
    public boolean isDefault();
}
```

- The four constants are singletons with JIPipe theme 16px icons, matching what
  the existing hand-rolled dialogs use (`actions/ok.png`, `actions/cancel.png`,
  `actions/checkmark.png`-class icons per existing convention).
- `asDefault()` marks the button that Enter activates when focus is not in a
  text field. For `confirm()`, YES is default (matches current
  `showConfirmDialog` default). For `choose()`, the caller marks one of the
  custom buttons as default; if none is, Enter does nothing.
- `equals`/`hashCode` by identity for constants (call sites compare with `==`);
  custom buttons compare by identity too — the facade returns *the same*
  `Button` instance the caller passed in.

### 4.2 `JIPipeDesktopDialogs` — the facade (the 95%)

```java
public final class JIPipeDesktopDialogs {

    // --- notify ---
    public static void message(Component parent, String title, String message)
    public static void error(Component parent, String title, String message, Throwable cause)

    // --- decide ---
    public static boolean confirm(Component parent, String title, String message)

    // --- choose ---
    public static Optional<Button> choose(Component parent, String title,
            String message, Button... options)

    // --- input ---
    public static Optional<String> inputText(Component parent, String title,
            String message, String initialValue)
    public static Optional<String> inputMultiLine(Component parent, String title,
            String message, String initialValue)
    public static Optional<Integer> inputInteger(Component parent, String title,
            String message, Integer initialValue, int min, int max)
    public static Optional<Double> inputDouble(Component parent, String title,
            String message, Double initialValue, double min, double max)
    public static Optional<String> inputUniqueText(Component parent, String title,
            String message, String initialValue, Predicate<String> exists,
            String errorTemplate)

    // --- pick ---
    public static Optional<Color> chooseColor(Component parent, String title,
            Color initial, boolean withAlpha)

    // --- escape hatch (the 5%) ---
    public static Builder builder(Component parent, String title)
}
```

Semantics, all of which exist to kill the bug class:

- **All methods are modal** and must be called on the EDT (same precondition as
  today's call sites; documented, not enforced).
- **Dismiss = Cancel button, X, or Escape**, all one outcome:
  `Optional.empty()` (input/choose/color) or `false` (`confirm`). There is no
  `CLOSED_OPTION` in this API.
- `confirm` — Yes/No dialog: YES → `true`, NO → `false`, dismissed → `false`.
  (Identical truth table to the current `== JOptionPane.YES_OPTION` idiom, so
  migrations are mechanical.)
- `choose` — buttons: an implicit **Cancel** is added (skipped if the caller
  already included `Button.CANCEL`). Result: the chosen button (same
  instance), or empty on dismiss.
- `message` — OK-only; returns void.
- `error` — message plus, when `cause != null`, a bounded (≈150px max,
  scrollable) read-only monospaced text area with the stack trace below the
  message.
- **`<html>` pass-through:** message strings are handed to a `JLabel` verbatim;
  existing callers that embed `<html>…</html>` keep working unchanged.
- `inputText` — single-line `JTextField`. **Deliberate behavior change:**
  submitting (OK/Enter) with empty or whitespace-only text counts as *dismiss*
  (`empty()`), not as an empty-string value. Rationale: in the existing 40
  `showInputDialog` sites, empty submit is never a wanted value — each caller
  already guards `result != null && !result.isEmpty()`; collapsing the two
  "no value" cases is the point of the API. A caller that genuinely needs an
  empty value uses `builder()`.
- `inputMultiLine` — `JTextArea` in a `JScrollPane` (default size ≈ 480×240,
  overridable only via the builder).
- `inputInteger` / `inputDouble` — plain `JSpinner`
  (`SpinnerNumberModel`, step 1) with the given bounds; OK reads the model
  value. `initialValue` may be `null` (spinner starts at `min`).
- `inputUniqueText` — like `inputText`, but on submit, if `exists(value)` is
  true, an inline error label is shown (rendered from `errorTemplate`, which may
  contain `{value}`) and the dialog **stays open** with the field selected. No
  re-show loop, no separate popup.
- `chooseColor` — delegates to
  `JIPipeDesktopColorChooserDialog.show(parent, title, initial, withAlpha)`;
  its `null`-on-cancel return maps to `Optional.empty()`.

### 4.3 `JIPipeDesktopDialog` — the dialog chrome

```java
public class JIPipeDesktopDialog extends JDialog {
    // built by the facade/builder:
    public String getTitle();
    public Optional<Button> getResult();      // empty until a button is clicked
    public void showAndWait();                // modal; sets result
    void dismiss();                           // package-private; X/Escape/Cancel funnel here
}
```

`dismiss()` is package-private on purpose: tests (same package) drive it
directly, and the API surface exposes no programmatic "close" that a caller
could misuse.

- `ModalityType.APPLICATION_MODAL`, JIPipe 128px icon, `DISPOSE_ON_CLOSE`,
  centered on the owner window derived from `parent` (null `parent` → no owner,
  still works).
- Layout, top to bottom: message `JLabel` (word-wrapped) → optional input
  component → right-aligned button row of **plain `JButtons`** (16px theme
  icons, existing convention: Cancel before the affirmative buttons).
- Escape is bound on the root pane (`WHEN_IN_FOCUSED_WINDOW`) to
  `dismiss()`; every text field's Enter triggers submit (OK) — for
  `inputMultiLine`, Enter inserts a newline and submit is via the OK button.
- `showAndWait()` blocks until `dismiss()` or a non-cancel button sets the
  result and hides the dialog.

### 4.4 `Builder` — the 5%

```java
public final class Builder {
    public Builder message(String message)
    public Builder content(JComponent content)        // custom center component
    public Builder buttons(Button... buttons)         // explicit set, no implicit Cancel
    public Builder cancelable(boolean cancelable)     // default true → implicit Cancel
    public Builder size(int width, int height)
    public JIPipeDesktopDialog build()                // dialog, not yet shown
    public Optional<Button> show()                    // build + showAndWait
}
```

Covers: custom-content confirms (e.g. current
`UIUtils.showConfirmDialog(parent, title, size, component)` and
`showOKCancelDialog`), non-cancelable option dialogs, and future cases
(#1346's key-capture dialog). For typed results from custom content, the caller
keeps its reference to the component and reads it after `show()` returns a
non-empty `Button` — no value-extraction plumbing in v1.

Button-set rules (unambiguous):
1. `buttons(Button...)` replaces the default set with the given buttons, in order.
2. `cancelable(false)` suppresses the Cancel button entirely (non-cancelable
   "proceed" dialogs).
3. Otherwise (default `cancelable(true)`), an implicit `Button.CANCEL` is
   added if the effective set does not already contain one.
4. **Button order contract** (the #1341 UX review): the Cancel button is
   anchored alone at the dialog's left edge; all other actions group at the
   right edge, keeping their relative order with the default-flagged action
   right-most. A default-flagged Cancel is registered for Enter but never
   receives the primary fill (Cancel is a dismissal, not an affirmation).

### 4.5 Test seam — `DialogPresenter`

The facade never calls `setVisible(true)` itself. Presentation goes through one
package-private seam:

```java
interface DialogPresenter {                      // package-private
    JIPipeDesktopDialog show(JIPipeDesktopDialog dialog);
}
```

- Production: `ModalPresenter` → `dialog.showAndWait()`.
- Tests (same package): install a capture presenter that records the dialog and
  may drive it by `doClick()`ing its buttons, so **all facade methods are
  unit-testable headless** — the dialog is constructed but never shown
  (`JDialog.setVisible(false)` and `doClick()` are safe headless;
  `setVisible(true)` is not, and is never reached in tests).

## 5. Migration (all ~320 sites)

Waves, each ending with a full compile + green tests before the next starts:

- **W1 — `showOptionDialog` + `showConfirmDialog` (142 sites).** The
  bug-prone class first. `int`-comparison call sites become `boolean` /
  `Optional<Button>` comparisons; the three !174-style `CLOSED_OPTION` case
  workarounds are deleted (the API handles them); custom
  `JComponent`-message confirms move to `builder().content(…)`.
- **W2 — `showInputDialog` (40 sites).** → `inputText` / `inputInteger` /
  `inputDouble` / `inputUniqueText` as the value demands; the
  `!= null && !isEmpty()` guards collapse to `ifPresent`-style checks. Empty-submit
  semantics change per §4.2 (documented).
- **W3 — `showMessageDialog` (136 sites).** → `message` / `error` (those with a
  `Throwable` in the string); `JIPipeDesktopColorChooserButton`'s leftover
  `JColorChooser.showDialog` → facade `chooseColor`; `UIUtils.selectColor`
  re-implemented as a one-liner over the facade (kept: other code calls it).
- **W4 — sweep & gate.** `grep` to zero `JOptionPane` in main sources of
  jipipe-core and all plugins; `UIUtils` wrappers now unused
  (`showOKCancelDialog`, `getMultiLineStringByDialog`, `getIntegerByDialog`,
  `getDoubleByDialog`, `getMinMaxByDialog`, `getUniqueStringByDialog`,
  `askOpenInCurrentWindow`, `checkAndAskIfFileExists` where replaced) are
  deleted, leaving the facade as the single dialog entry point.

Migration invariants (per wave): `mvn compile` green on all modules; no
user-visible text changes except where the bug fix requires one; each wave is a
separate commit group per module for reviewability.

**Deliberate behavior changes (both are the point of the issue):**
1. X/Escape dismisses *every* dialog (was: undefined per call site).
2. Empty `inputText` submit = dismiss (was: `""` or `null` depending on call).

## 6. Testing

Per the project test policy: no Swing-structure assertions, no trivial
tests. What is non-trivial and therefore tested:

1. **Dismiss-mapping contract** (the whole point of the issue): for each facade
   kind, X/Escape/Cancel → `Optional.empty()` / `false`; affirmative → value.
   Driven headless via the presenter seam: the test captures the dialog, then
   invokes the dialog's package-private `dismiss()` (the single code path that
   X, Escape, and Cancel all funnel into) or `doClick()`s a specific button.
   Separately, one wiring assertion verifies the root-pane Escape action and
   the Cancel button both invoke `dismiss()`.
2. **`choose` button set:** implicit Cancel appended exactly once; explicit
   `Button.CANCEL` not duplicated; chosen button returned as the *same
   instance* passed in.
3. **`inputText` empty-submit rule:** whitespace-only → empty; value →
   `Optional.of`.
4. **`inputUniqueText` re-prompt:** `exists(value)` true → error label rendered
   from `errorTemplate` with `{value}` substituted, dialog still "open"
   (not dismissed); second submit with a free value succeeds.
5. **`error` with cause:** stack-trace area present and contains the exception
   class name; `message`-style dialog has no trace area.
6. **`chooseColor` null→`Optional.empty()` mapping:** covered at the seam level
   (the chooser's static `show(...)` returns `null` on cancel — pinned by the
   existing color-dialog tests; the facade's one-line mapping is verified by
   review, not a new test, since the chooser dialog has no test seam).
7. **`NoJOptionPaneGate` test:** scans all `src/main` `.java` files of
   jipipe-core + plugins for the token `JOptionPane` and fails the build if any
   remain — the fail-closed guard that keeps the API the single entry point.
8. **Headful plumbing (skip when headless**, `Assumptions` pattern from
   `ColorChooserDialogRefreshTest`): one test that actually presents a real
   modal dialog via `ModalPresenter` on a virtual frame and clicks OK — guards
   the EDT/showAndWait path that doClick tests cannot reach.

## 7. File layout

```
jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/
  Button.java                  — immutable descriptor + constants
  JIPipeDesktopDialog.java     — dialog chrome (§4.3)
  JIPipeDesktopDialogs.java    — facade + Builder (§4.2, §4.4)
  (package-private) DialogPresenter.java, ModalPresenter.java

jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/
  DialogDismissMappingTest.java
  ChooseButtonsTest.java
  InputTextEmptySubmitTest.java
  InputUniqueTextTest.java
  ErrorDialogTraceTest.java
  ChooseColorDelegationTest.java
  NoJOptionPaneGateTest.java
  HeadfulPlumbingTest.java     (headless-skipped)
```

Modified: the ~113 call-site files across `jipipe-core` and `plugins/*`, plus
`UIUtils` (wrapper deletions) and `JIPipeDesktopColorChooserButton`
(`JColorChooser` → facade). No changes to the color chooser dialog, theme, or
build files.

## 8. Error handling & edge cases

- `parent == null` → dialog with no owner (top-level); all facades accept it
  (existing code already passes `null` in a few places).
- `message == null` → treated as `""` (no label).
- `choose` with **zero** options → `IllegalArgumentException` (programming
  error; the implicit Cancel would otherwise be the only button).
- `inputInteger`/`inputDouble` with `min > max` → `IllegalArgumentException`.
- Presentation exceptions (e.g. disposed owner) propagate — same as today, no
  swallowing.
- Nested dialogs (dialog from a dialog): fine — both application-modal, same as
  the current `JDialog`-based wrappers already allow.

## 9. Risks & review focus

1. **`showInputDialog` sites that relied on `""` as a real value** — the
   empty-submit rule (§4.2) changes them; W2 migrates them one-by-one, any that
   need empty use the builder. Reviewer check: W2 diff.
2. **YES/NO sites distinguishing NO from dismiss** — none can today (both
   ≠ YES); `boolean` preserves that exactly.
3. **Custom `showOptionDialog` button order and default selection** — 21
   `YES_NO_CANCEL` sites plus a few 3-option sites; mapping table lives in the
   plan, default flagged via `Button.asDefault()`.
4. **`<html>` strings with stray tags** — pass-through, so no change; if a site
   breaks, the fix is at that site, not the API.
5. **The gate test going stale** — `NoJOptionPaneGateTest` must scan the same
   roots as the build; if modules move, the test fails loudly (fail-closed).
