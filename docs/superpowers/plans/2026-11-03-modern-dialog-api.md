# Modern Dialog API Implementation Plan (#1341)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all ~320 `JOptionPane` call sites with a typed dialog API in which closed/cancel is unrepresentable (`Optional`/`boolean` results), plus new input types (multiline, typed numeric, unique-string, color), leaving zero `JOptionPane` references in main code, enforced by a gate test.

**Architecture:** A new package `org.hkijena.jipipe.desktop.commons.dialogs` in jipipe-core holds: `Button` (immutable button descriptor + `OK/CANCEL/YES/NO` constants), `JIPipeDesktopDialog` (the modal chrome), `JIPipeDesktopDialogs` (static facade + `Builder`). Presentation goes through a package-private `DialogPresenter` seam that tests replace with a capturing presenter, so the whole API is unit-testable headless. Migration proceeds in waves (option/confirm → input → message → sweep), each wave ending with a full compile + green tests.

**Tech Stack:** Java 21, Swing, JUnit 5, Maven multi-module (jipipe-core + `plugins/*`).

**Spec:** `docs/superpowers/specs/2026-11-03-modern-dialog-api-design.md` (traveling on this branch; read it first)

## Global Constraints

- Branch: `1341-modern-dialog-api` (already created, off `origin/master` @ `b8f427c8`). **Remote is authoritative**: before pushing, `git fetch` and `git rebase origin/master` if master moved.
- **No styled/"modern" buttons** (user instruction 2026-11-03): dialog buttons are plain `JButton`s.
- Dismiss semantics: Cancel button, X, and Escape all mean "dismissed" → `Optional.empty()` / `false`. There is no CLOSED_OPTION anywhere.
- `<html>` message strings pass through verbatim to a `JLabel` (existing callers embed `<html>`).
- `JIPipe.RESOURCES.getIcon16(...)` / `getIcon128()` for icons; JIPipe 128px window icon.
- All dialogs application-modal, shown on the EDT (same precondition as today; documented, not enforced).
- Commit per wave per affected module with message prefix `feat(dialogs)` / `refactor(dialogs)`, referencing `(#1341)`.
- Test policy: no Swing-structure tests (no layout/pixel/border assertions), no interaction/robot tests; plumbing via `doClick()` on constructed-but-never-shown dialogs; headful tests skip via `Assumptions` when headless (pattern: `ColorChooserDialogRefreshTest`).
- Verify after every task: `mvn -q compile -pl jipipe-core` (plus the touched plugin modules) and the dialogs test suite.

## Review Focus

The spec's silence the reviewer must actively check:

1. **Custom-button sites that used raw ints instead of constants** — `ExpressionBuilderUI:424` switches on `case 1/2/3` and `FormsDialog:137` treats its "Show errors" button as the *default* with no cancel; after migration these must switch on `Button` identity, and `FormsDialog` must still close-all-tabs when "Show errors" is chosen (it was previously conflated with the closed/cancel path).
2. **Sites that relied on an explicit "Cancel" option string** — `ParameterTableEditorWindow:303` and `HTMLEditor:715` pass an options array without a cancel entry and rely on `JOptionPane`'s implicit `"Cancel"`; migrated `choose()` calls must preserve that implicit cancel (facade appends it).
3. **`showInputDialog` combo-style call sites** (selection from a list, not free text) — `ArrangeChannelsAlgorithm:205`, `REnvironmentFromSystemSetupTool:60`, `PythonEnvironmentFrom*SetupTool:48/108/173`, `DataExportExpressionParameterEditorPathChooserUI:489`; the new API has no `inputChoice` facade method (YAGNI) — they must use `builder()` with a `JComboBox` content, and their `instanceof String` checks must become `Optional` handling.
4. **`JIPipeDesktopProjectWindow:995` (`openProjectInThisOrNewWindow`)** — a `switch` over `JOptionPane.YES_OPTION` whose fall-through returns `null`; migrated to `choose()` it must still return `null` on dismiss (empty) and handle both branches.
5. **The two deliberate behavior changes** (spec §5): X/Escape now dismisses every dialog; empty `inputText` submit = dismiss. W2/W3 reviewers must confirm no migrated site silently changed behavior beyond those two.

---

### Task 1: `Button` descriptor

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/Button.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/ButtonTest.java`

**Interfaces:**
- Produces: `Button` — `public static final Button OK, CANCEL, YES, NO`; `static Button custom(String label)`; `static Button custom(String label, Icon icon)`; `Button asDefault()` (copy, default-flagged); `String getLabel()`; `Icon getIcon()` (nullable); `boolean isDefault()`. Identity equality (default `Object` equals).

- [ ] **Step 1: Write the failing test**

`ButtonTest` (`@BeforeAll`: `JIPipe.ensureInstance()` — needed for `RESOURCES` icons): constants have labels `"OK"`, `"Cancel"`, `"Yes"`, `"No"` and non-null 16px theme icons (assert `getIcon() instanceof Icon` — not image size); `custom("X")` returns a new instance with that label, null icon, not default; `custom("X", icon)` carries the icon; `custom("X").asDefault()` is a *different* instance with `isDefault() == true` and the same label; constants are not default (`Button.OK.isDefault() == false`).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=ButtonTest`
Expected: FAIL (class `Button` does not exist)

- [ ] **Step 3: Implement `Button`**

Immutable value: `String label`, `Icon icon`, `boolean isDefault`. Constants built in a static initializer with `JIPipe.RESOURCES.getIcon16("actions/ok.png")`, `"actions/cancel.png"`, `"actions/checkmark.png"` (YES), `"actions/no.png"` (NO). NOTE: `JIPipe.RESOURCES` needs a booted instance — if the constant-init throws headless, make the icons a lazy `Supplier<Icon>` resolved on first `getIcon()` call instead (assert in the test only that `getIcon() != null`). `asDefault()` → `new Button(label, icon, true)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=ButtonTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/Button.java jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/ButtonTest.java
git commit -m "feat(dialogs): Button descriptor with OK/CANCEL/YES/NO constants (#1341)"
```

---

### Task 2: `JIPipeDesktopDialog` chrome + `DialogPresenter` seam

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/JIPipeDesktopDialog.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/DialogPresenter.java` (package-private interface)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/ModalPresenter.java` (package-private impl)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/DialogChromeTest.java`

**Interfaces:**
- Produces (consumed by Task 3's facade):
  ```java
  class JIPipeDesktopDialog extends JDialog {
      JIPipeDesktopDialog(Window owner, String title);
      void addMessage(String message);                 // word-wrapped JLabel, NORTH-ish top
      void addContent(JComponent content);             // center
      void setButtons(Button... buttons);              // right-aligned plain JButtons; default-flagged one gets Enter focus
      void setCancelable(boolean cancelable);          // if true and set has no CANCEL, append one (facade uses this)
      Optional<Button> getResult();                     // empty until a button decides
      void showAndWait();                               // modal via ModalPresenter
      void dismiss();                                    // package-private: sets empty result + hides
      JComponent getMessageLabel();                      // for tests
      List<JButton> getActionButtons();                  // for tests (doClick targets)
  }
  interface DialogPresenter { JIPipeDesktopDialog show(JIPipeDesktopDialog dialog); }
  // ModalPresenter: dialog.setModal(true); dialog.setVisible(true); return dialog;
  // The presenter is an instance field with a package-private static setter for tests:
  //   static void setPresenter(DialogPresenter p) / static DialogPresenter getPresenter()
  ```

- [ ] **Step 1: Write the failing tests** (`DialogChromeTest`, headless: never calls `setVisible(true)`)
  - `dismissSetsEmptyResult`: construct dialog, `dismiss()` → `getResult().isEmpty()`.
  - `okButtonClickSetsResultToSameInstance`: `Button b = Button.custom("Do it").asDefault(); setButtons(b, Button.CANCEL); getActionButtons().get(0).doClick()` → `getResult().get() == b`.
  - `cancelClickDismisses`: `setButtons(Button.OK, Button.CANCEL); <cancel button>.doClick()` → `getResult().isEmpty()`.
  - `cancelableAppendsImplicitCancel`: `setCancelable(true); setButtons(Button.YES, Button.NO);` → last action button label `"Cancel"`, size 3; with `setCancelable(false)` no append.
  - `escapeAndCloseFunnelToDismiss`: simulate `JPanel`-level close — assert `getDefaultCloseOperation() == DISPOSE_ON_CLOSE` and dispatch `KeyEvent` ESCAPE to the content pane (or look up the registered `KeyboardAction` on the root pane) → result empty. (If dispatching ESCAPE proves flaky headless, assert instead that the root pane's `KeyboardAction` map contains a bound action on ESCAPE — wiring-level, per spec §6.1.)
  - `messageLabelReceivesText`: `addMessage("Hello <b>world</b>")` → `getMessageLabel().getText()` contains the string.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest=DialogChromeTest`
Expected: FAIL (class does not exist)

- [ ] **Step 3: Implement**

Dialog: `APPLICATION_MODAL`, `JIPipe.RESOURCES` 128px icon, `DISPOSE_ON_CLOSE`, `setLocationRelativeTo(owner)` when owner non-null. Layout: `BorderLayout` content pane with empty border (8,10,8,10); message label top (`addMessage` creates a word-wrapped `JLabel`), content center, button row bottom (plain `JButton`s, icon+label, `BoxLayout`/glue right-aligned, **no** rounded/pill UIs). Enter: for single-line inputs the facade (Task 3) installs the action; here the default-flagged button is registered on the root pane as a bound action for ESCAPE→`dismiss()` only. `showAndWait()` → `DialogPresenter.getPresenter().show(this)`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest=DialogChromeTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/ jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/DialogChromeTest.java
git commit -m "feat(dialogs): JIPipeDesktopDialog chrome + DialogPresenter test seam (#1341)"
```

---

### Task 3: Facade — `message`, `error`, `confirm`, `choose`

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/JIPipeDesktopDialogs.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/DismissMappingTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/ChooseButtonsTest.java`

**Interfaces:**
- Produces (facade, all static):
  ```java
  void        message(Component parent, String title, String message)
  void        error(Component parent, String title, String message, Throwable cause)
  boolean     confirm(Component parent, String title, String message)
  Optional<Button> choose(Component parent, String title, String message, Button... options)
  static void setPresenter(DialogPresenter p)   // re-exposed for facade tests (delegates to dialog's)
  ```
  - `confirm`: buttons `Button.YES.asDefault(), Button.NO` + implicit Cancel; returns `YES` chosen.
  - `choose`: validates `options.length > 0` else `IllegalArgumentException`; buttons = `options` + implicit Cancel (skipped if caller included `Button.CANCEL`); returns chosen instance.
  - `error` with `cause != null`: adds a bounded (preferred height ≤150) scrollable read-only `JTextArea` (monospaced) with `cause`'s stack trace below the message.
  - All build a `JIPipeDesktopDialog` from `parent`'s window ancestor (null-safe) and present via the seam.

- [ ] **Step 1: Write the failing tests**

`DismissMappingTest` (install a capturing presenter in `@BeforeEach`, restore in `@AfterEach`; `JIPipe.ensureInstance()` in `@BeforeAll` — needed for `RESOURCES`):
  - `confirmDismissIsFalse`: capture dialog, `dismiss()` → `confirm(...)` returns `false`; click YES → `true`; click NO → `false`.
  - `chooseDismissIsEmpty`: capture, `dismiss()` → `Optional.empty()`; click a custom button → same instance.
  - `messageDoesNotBlockOrReturn`: runs without presenter interaction (capture, dismiss) — smoke.
  - `errorWithCauseShowsTrace`: capture; assert a `JTextArea` (found in the dialog's component tree by class) contains the exception's class name; with `cause == null` no such area.

`ChooseButtonsTest`:
  - `implicitCancelAppendedExactlyOnce`: choose with 2 custom options → 3 action buttons, last labeled `"Cancel"`.
  - `explicitCancelNotDuplicated`: choose with `Button.custom("A"), Button.CANCEL` → 2 buttons.
  - `zeroOptionsThrows`: `assertThrows(IllegalArgumentException.class, () -> choose(parent, "t", "m"))`.
  - `chosenButtonIsSameInstance`: click custom → returned `==` the passed instance.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='DismissMappingTest,ChooseButtonsTest'`
Expected: FAIL (facade does not exist)

- [ ] **Step 3: Implement the four methods** per the Interfaces block.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='DismissMappingTest,ChooseButtonsTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/JIPipeDesktopDialogs.java jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/DismissMappingTest.java jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/ChooseButtonsTest.java
git commit -m "feat(dialogs): facade message/error/confirm/choose with Optional+Button semantics (#1341)"
```

---

### Task 4: Facade — typed inputs (`inputText`, `inputMultiLine`, `inputInteger`, `inputDouble`)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/JIPipeDesktopDialogs.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/InputTextEmptySubmitTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/TypedInputsTest.java`

**Interfaces:**
- Produces:
  ```java
  Optional<String>  inputText(Component parent, String title, String message, String initialValue)
  Optional<String>  inputMultiLine(Component parent, String title, String message, String initialValue)
  Optional<Integer> inputInteger(Component parent, String title, String message, Integer initialValue, int min, int max)
  Optional<Double>  inputDouble(Component parent, String title, String message, Double initialValue, double min, double max)
  ```
  - `inputText`: `JTextField` (initial value, selected-on-open); OK/Enter submits; **empty or whitespace-only submit → `Optional.empty()`** (spec §4.2 rule).
  - `inputMultiLine`: `JTextArea` in `JScrollPane` (≈480×240); Enter inserts newline; OK submits; empty → `empty()`.
  - `inputInteger`/`inputDouble`: `JSpinner` (`SpinnerNumberModel`, step 1, bounds); `initialValue == null` → start at `min`; `min > max` → `IllegalArgumentException`; OK reads model value → `Optional.of`.

- [ ] **Step 1: Write the failing tests**

`InputTextEmptySubmitTest` (capturing presenter; find the `JTextField` in the dialog tree):
  - `emptySubmitIsDismissed`: field text `""`, click OK → `empty()`; `"   "` → `empty()`.
  - `valueSubmitIsPresent`: field text `"abc"`, click OK → `Optional.of("abc")`.
  - `initialValueIsSet`: `inputText(..., "seed")` → field text `"seed"`.

`TypedInputsTest`:
  - `integerBoundsEnforced`: `inputInteger(parent,"t","m",5,0,10)` → model min 0 max 10; click OK → `of(5)` (or the model's value after the test sets it via `setNumber`).
  - `integerNullInitialStartsAtMin`: `null` initial → model value == min.
  - `integerMinMaxReversedThrows`: `assertThrows(IllegalArgumentException.class, ... inputInteger(parent,"t","m",5,10,0))`.
  - `doubleValueRoundTrips`: set spinner number 3.5 → OK → `of(3.5)`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='InputTextEmptySubmitTest,TypedInputsTest'`
Expected: FAIL (methods do not exist)

- [ ] **Step 3: Implement** the four methods.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='InputTextEmptySubmitTest,TypedInputsTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/ jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/
git commit -m "feat(dialogs): facade typed inputs (text, multiline, integer, double) (#1341)"
```

---

### Task 5: Facade — `inputUniqueText`, `chooseColor`, `Builder`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/JIPipeDesktopDialogs.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/InputUniqueTextTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/BuilderTest.java`

**Interfaces:**
- Produces:
  ```java
  Optional<String> inputUniqueText(Component parent, String title, String message,
          String initialValue, Predicate<String> exists, String errorTemplate)
  Optional<Color>  chooseColor(Component parent, String title, Color initial, boolean withAlpha)
  static Builder   builder(Component parent, String title)
  // Builder:
  public Builder message(String message)
  public Builder content(JComponent content)
  public Builder buttons(Button... buttons)      // replaces the default set
  public Builder cancelable(boolean cancelable)  // default true
  public Builder size(int width, int height)
  public JIPipeDesktopDialog build()
  public Optional<Button> show()                 // build + present
  ```
  - `inputUniqueText`: on submit, if `exists(value)` → set an error `JLabel` (text = `errorTemplate` with `{value}` replaced) below the field, keep the dialog open, reselect the field text; otherwise OK. Same empty→dismiss rule as `inputText`.
  - `chooseColor`: `Optional.ofNullable(JIPipeDesktopColorChooserDialog.show(parent, title, initial, withAlpha))` — pure delegation (spec §6.6: no new test; covered by review).
  - `Builder` button rules (spec §4.4, unambiguous): `buttons(...)` replaces the default set in order; `cancelable(false)` suppresses Cancel entirely; else implicit `Button.CANCEL` appended if absent. `content(null)` → no center. `size(w,h)` → `dialog.setSize`.

- [ ] **Step 1: Write the failing tests**

`InputUniqueTextTest` (capturing presenter; `Set<String> taken = {"a"}`):
  - `takenValueShowsErrorAndStaysOpen`: field `"a"`, click OK → `getResult()` still empty (dialog not decided) AND an error `JLabel` in the tree contains the substituted text.
  - `freeValueAfterErrorSucceeds`: field `"a"` → click OK → (still open) set field `"b"` → click OK → `of("b")`.
  - `errorTemplateSubstitutesValue`: `errorTemplate = "Name {value} is already in use"` with value `"a"` → label text `"Name a is already in use"`.

`BuilderTest`:
  - `customContentAndExplicitButtons`: `builder(parent,"t").message("m").content(new JPanel()).buttons(Button.custom("Apply").asDefault()).cancelable(false).size(400,300).build()` → dialog has 1 action button (`"Apply"`), message set, size 400×300.
  - `showReturnsChosenButton`: capture presenter; `show()` after clicking Apply → `of(the Apply instance)`; after `dismiss()` → empty.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='InputUniqueTextTest,BuilderTest'`
Expected: FAIL (methods do not exist)

- [ ] **Step 3: Implement** per Interfaces.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='InputUniqueTextTest,BuilderTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/dialogs/ jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/
git commit -m "feat(dialogs): facade unique-text input, color delegation, Builder (#1341)"
```

---

### Task 6: Headful plumbing test

**Files:**
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/HeadfulPlumbingTest.java`

**Interfaces:**
- Consumes: full facade (Tasks 1–5).

- [ ] **Step 1: Write the test**

`@BeforeAll`: if `GraphicsEnvironment.isHeadless()` → `Assumptions.assumeTrue(false, "headless environment")`; else `JIPipe.ensureInstance()`.
One test `realModalOkClickReturnsValue`: on the EDT (`SwingUtilities.invokeAndWait`), call `JIPipeDesktopDialogs.confirm(frame, "t", "m")` inside a background thread that the test controls — or, more simply and deterministically: construct a `JIPipeDesktopDialog` directly, show it on the EDT, and from the test thread `doClick()` the YES button, then assert `getResult().isPresent()`. (Goal per spec §6.8: exercise the real `ModalPresenter`/`showAndWait` path that doClick-only tests cannot.) If the EDT/showAndWait combination proves flaky in this CI, fall back to asserting `ModalPresenter` presents a dialog with `isModal() == true` (document the fallback in the test javadoc).

- [ ] **Step 2: Run it**

Run: `mvn -q test -pl jipipe-core -Dtest=HeadfulPlumbingTest`
Expected: PASS (or SKIP with assumption message when headless — both acceptable)

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/HeadfulPlumbingTest.java
git commit -m "test(dialogs): headful showAndWait plumbing guard (#1341)"
```

---

### Task 7: `NoJOptionPaneGateTest` (failing until Task 11)

**Files:**
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/NoJOptionPaneGateTest.java`

**Interfaces:**
- Produces: a fail-closed build gate.

- [ ] **Step 1: Write the test**

Walk `src/main/java` of the current module (jipipe-core, resolved via the class's protection domain or a relative path from the working dir — pick whichever is robust under surefire) **and** every `plugins/*/src/main/java` (resolved from the repo root: two levels up from the test's module dir) for `*.java`; fail with a list of `file:line` matches if the token `JOptionPane` appears (skip import lines? NO — count them too; zero references is the goal).

- [ ] **Step 2: Run it to verify it FAILS**

Run: `mvn -q test -pl jipipe-core -Dtest=NoJOptionPaneGateTest`
Expected: FAIL — many `JOptionPane` matches (this is the gate working)

- [ ] **Step 3: Commit (red gate, expected)**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/NoJOptionPaneGateTest.java
git commit -m "test(dialogs): NoJOptionPaneGate — fails until migration completes (#1341)"
```

> ⚠️ The gate stays red through Tasks 8–10. **Do not run the full jipipe-core test suite between Task 7 and Task 11** — run only targeted `-Dtest=` invocations and `mvn compile`. The gate goes green at Task 11 and the full suite runs there.

---

### Task 8: Wave 1 — migrate `showOptionDialog` + `showConfirmDialog` (142 sites)

**Files:** modify the call sites listed below (jipipe-core + 6 plugins).

**Interfaces:**
- Consumes: `JIPipeDesktopDialogs.confirm/choose`, `Button`, `Builder` (Tasks 1–5).

**Mapping rules (apply mechanically, per site):**
- `showConfirmDialog(...) == JOptionPane.YES_OPTION` with `YES_NO_OPTION` → `JIPipeDesktopDialogs.confirm(parent, title, message)` (92 sites).
- `showConfirmDialog` with `OK_CANCEL_OPTION` (5 sites, e.g. `JIPipeDesktopBackupManagerPanel:281`, `JIPipeDesktopExpressionCalculatorUI:135`) → `confirm(...)` as well (both return boolean for the affirmative).
- `showOptionDialog` with 2 real buttons + Cancel (`YES_NO_CANCEL_OPTION`, the common case) → `choose(parent, title, message, Button.custom("…").asDefault(), Button.custom("…"))`; map `== YES_OPTION` → `.map(b -> b == A).orElse(false)` / `== NO_OPTION` → `.map(b -> b == B).orElse(false)`; `!= YES && != NO` guards → `.isEmpty()` checks. Delete the inline `CLOSED_OPTION` workarounds as you go.
- `showOptionDialog` with custom components as the *message* (`ExpressionBuilderUI:306`, `MultiTemplateMatchingAlgorithm:465`) → `builder(parent, title).content(…).buttons(Button.OK, Button.CANCEL).show()`.
- `showOptionDialog` with `DEFAULT_OPTION` 3-button choice (`ExpressionBuilderUI:424` — the raw `case 1/2/3` bug) → `choose(parent, title, message, Button.custom("Ignore"), Button.custom("Insert").asDefault(), Button.custom("Replace"), Button.CANCEL)`; switch on identity.
- `FormsDialog:137` (no cancel; "Show errors" is default) → `choose(parent, title, message, Button.custom("Yes"), Button.custom("No"), Button.custom("Show errors").asDefault())` with `cancelable` handled by `builder()` (this site has no implicit cancel today because all three options are "choices" — verify against the original: it used `YES_NO_CANCEL_OPTION` with a 3rd option replacing Cancel, so "Show errors" *is* the cancel-like role; migrated: treat `empty()` OR "Show errors" → the close-tabs path). **Preserve exact original semantics.**
- `askOpenInCurrentWindow` + its `JIPipeDesktopProjectWindow:995` caller → `choose(..., Button.custom("This window").asDefault(), Button.custom("New window"))`; caller: `result.map(b -> b == THIS_WINDOW).orElse(false)` for this-window, `result.map(b -> b == NEW_WINDOW).orElse(true)`-style for new-window, `isEmpty()` → return null. Delete `UIUtils.askOpenInCurrentWindow` in Task 11.
- `PathUtils.ensureEmptyFolder:548` (3-way YES/NO/CANCEL) → `choose(..., Button.YES, Button.NO, Button.CANCEL)` — keep the tri-state (Yes→delete+continue, No→continue, else→false).
- `JIPipeNodeTemplate:117` (Globally/Inside project/Cancel) → `choose`.
- `NodeTemplateDownloaderDialog:145`, `JIPipeDesktop.java:352/440/960` (file/directory/Cancel ×3), `JIPipeDesktopProjectWorkbench:1205/1238` (ZIP/directory/Cancel ×2), `JIPipeDesktopProjectWindow:280/386/546/650/1156` (the five 3-way option sites, defaults as flagged in the original `initialSelectionValue`), `JIPipeDesktopLegacyImageViewerPanel2D:602/647`, `AddToROIManagerDataDisplayOperation:69`, `ROIManagerPlugin2D:147`, `ROIManagerPlugin3D:157`, `SpotsManagerPlugin2D:239`, `TracksManagerPlugin2D:117`, `FilamentsManagerPlugin2D:175`, `GetJIPipeSlotFolderAlgorithm:130` → `choose` per their existing button arrays and defaults.

**Site list (all files touched):** `jipipe-core`: `UIUtils.java` (the 5 internal wrappers), `JIPipeDesktopProjectWindow.java`, `JIPipeDesktopProjectWorkbench.java`, `JIPipeDesktop.java`, `JIPipeNodeTemplate.java`, `PathUtils.java`, `JIPipeDesktopBackupManagerPanel.java`, `JIPipeDesktopExpressionCalculatorUI.java`, `JIPipeDesktopRawSettingsSection.java`, `ExportCompartmentAsJsonNodeUIContextAction.java`, `ExpressionBuilderUI.java`, `NodeTemplateDownloaderDialog.java`, `JIPipeDesktopHTMLEditor.java` (its confirms). `plugins`: `jipipe-plugin-forms` (`FormsDialog.java`), `jipipe-plugin-ij` (`JIPipeDesktopLegacyImageViewerPanel2D.java`, `AddToROIManagerDataDisplayOperation.java`, `ROIManagerPlugin2D.java`), `jipipe-plugin-ij-3d` (`ROIManagerPlugin3D.java`), `jipipe-plugin-ij-trackmate` (`SpotsManagerPlugin2D.java`, `TracksManagerPlugin2D.java`), `jipipe-plugin-ij-filaments` (`FilamentsManagerPlugin2D.java`), `jipipe-plugin-ij-multi-template-matching` (`MultiTemplateMatchingAlgorithm.java`), `jipipe-plugin-utils` (`GetJIPipeSlotFolderAlgorithm.java`).

- [ ] **Step 1: Migrate jipipe-core sites** (per the mapping rules; `UIUtils`'s internal wrappers get migrated to the facade in-place first, so their external callers keep compiling until Task 11)

- [ ] **Step 2: Compile core**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS

- [ ] **Step 3: Migrate plugin sites** (one module at a time, compiling each)

Run per module: `mvn -q compile -pl plugins/<module> -am -o 2>&1 | tail -3` (add `-am` only if the module needs core rebuilt; core is already built)
Expected: BUILD SUCCESS for each: `jipipe-plugin-forms`, `jipipe-plugin-ij`, `jipipe-plugin-ij-3d`, `jipipe-plugin-ij-trackmate`, `jipipe-plugin-ij-filaments`, `jipipe-plugin-ij-multi-template-matching`, `jipipe-plugin-utils`

- [ ] **Step 4: Run the dialogs test suite (targeted only — gate still red)**

Run: `mvn -q test -pl jipipe-core -Dtest='*Dialog*,ButtonTest,DialogChromeTest' 2>&1 | tail -5`
Expected: PASS (the `NoJOptionPaneGateTest` is excluded by the `-Dtest` pattern)

- [ ] **Step 5: Commit (per module for reviewability)**

```bash
git add -A jipipe-core/src/main && git commit -m "refactor(dialogs): wave 1 — option+confirm dialogs to the facade (core) (#1341)"
git add -A plugins && git commit -m "refactor(dialogs): wave 1 — option+confirm dialogs to the facade (plugins) (#1341)"
```

---

### Task 9: Wave 2 — migrate `showInputDialog` (40 sites)

**Files:** modify the call sites (jipipe-core + 4 plugins).

**Interfaces:**
- Consumes: `inputText`, `inputInteger`, `inputDouble`, `inputUniqueText`, `Builder` (Tasks 4–5).

**Mapping rules:**
- Plain `showInputDialog(parent, message, title)` or `(parent, message, initialValue)` → `JIPipeDesktopDialogs.inputText(parent, title, message, initialValue)`. **Note the existing (message, title) argument-order trap** — the 4-arg forms pass message then title; the facade takes `title, message`. Re-read each call's arguments, don't shift mechanically.
- Existing `!= null && !isEmpty()` guards → `ifPresent` / `map` + `isEmpty()` checks (empty submit is now dismiss).
- Combo-style `showInputDialog` (selection values array — `JOptionPane` appends an implicit `"Cancel"` value; callers like `ParameterTableEditorWindow:303` compare against it): `ArrangeChannelsAlgorithm:205` (channel reorder strings), `REnvironmentFromSystemSetupTool:60`, `PythonEnvironmentFromSystemSetupTool:48`, `PythonEnvironmentFromCondaSetupTool:108/173`, `DataExportExpressionParameterEditorPathChooserUI:489` (user-path keys), `ParameterTableEditorWindow:303` (row-replace strategy), `JIPipeDesktopHTMLEditor:715` (image downscale options) → `choose(parent, title, message, Button.custom(opt1).asDefault() /* original initialSelectionValue */, Button.custom(opt2), …, Button.CANCEL)`; switch on `Button` identity; drop the `instanceof String` / `equals("Cancel")` checks (`isEmpty()` covers dismiss).
- Numeric-string inputs (`JIPipeDesktopGraphEditorUI:562` zoom `%`, `JIPipeDesktopLegacyImageViewerPanel2D:410` zoom `%`) — these return strings with a `%` suffix; keep them as `inputText` (not `inputInteger`) to preserve the parsing behavior.
- `UIUtils.getUniqueStringByDialog` callers (`JIPipeDesktopTableEditor:448/470/495/511`, `JIPipeDesktopProjectWorkbench:1336`) → `inputUniqueText` with the same `Predicate`; keep the wrapper until Task 11 (its body becomes a facade call).
- `UIUtils.getMinMaxByDialog` caller (`CalibrationPlugin2D:140`) → `builder` with two `JSpinner`s (or keep the wrapper as a facade-composite until Task 11 — pick builder, it's the only caller).
- `UIUtils.getSelectionByDialog` (2 callers) — JList-based multi-select; **out of facade scope** (YAGNI per spec): leave it as-is for now; it uses `showOptionDialog` internally → migrate its *internal* call to `builder(...).content(list)…` so the file has no `JOptionPane` import; the wrapper stays until it's the last JOptionPane user, then delete it in Task 11 if unused, else keep it as a non-JOptionPane utility. **Decision: migrate its internals to the builder; keep the public method (callers unchanged).**
- `UIUtils.getIntegerByDialog` / `getDoubleByDialog` callers (`JIPipeDesktopDataPreviewControlUI:86`, `ROIManagerPlugin2D:529/559/573`, `ROIManagerPlugin3D:501/515`) → `inputInteger`/`inputDouble`; wrappers become facade delegations (deleted Task 11).
- `UIUtils.getMultiLineStringByDialog` caller (`JIPipeDesktopRawSettingsSection:323`) → `inputMultiLine`.
- `UIUtils.showOKCancelDialog` callers (`JIPipeDesktopWekaClassifierParameterEditorUI:83`, `ROIManagerPlugin2D:201`) → `builder(parent, title).content(panel).buttons(Button.OK, Button.CANCEL).size(800,600).show()` → `boolean` via `result.isPresent()`-style (OK vs anything).
- `UIUtils.showConfirmDialog(parent, title, size, component)` callers (`JIPipeDesktopProjectOverviewUI:260`, `DescriptionAssistantCondition:40`, `SummaryAssistantCondition:40`) → same builder pattern.
- `UIUtils.getHTMLByDialog` callers (`JIPipeDesktopHTMLEditor:621`, `JIPipeDesktopHTMLTextParameterEditorUI:118`) → builder with the `JIPipeDesktopHTMLEditor` as content; read `getHTML()` only on OK. Wrapper stays (non-trivial HTMLText plumbing) but its body loses the hand-rolled buttons.

**Site list:** `jipipe-core`: `UIUtils.java`, `JIPipeDesktopAdvancedFileChooser.java`, `JIPipeDesktopFileChooserNext.java` (×3), `JIPipeDesktopTabPane.java`, `JIPipeDesktopHTMLEditor.java` (×2), `JIPipeDesktopProjectOverviewUI.java`, `JIPipeDesktopRunSetsListEditor.java`, `JIPipeDesktopThemeEditor.java` (×2), `JIPipeDesktopLegacyCacheDataViewerWindow.java`, `JIPipeDesktopCompartmentsGraphEditorUI.java`, `JIPipeDesktopGraphNodeUISlotManager.java`, `JIPipeDesktopGraphEditorUI.java` (×2), `JIPipeDesktopGraphNodeSlotEditorUI.java`, `JIPipeDesktopTableEditor.java`, `JIPipeDesktopProjectWorkbench.java`, `PythonEnvironmentFromSystemSetupTool.java`, `PythonEnvironmentFromCondaSetupTool.java` (×2), `DataExportExpressionParameterEditorPathChooserUI.java` (×6), `ParameterTableEditorWindow.java`, `JIPipeDesktopExternalEnvironmentParameterEditorUI.java`, `TextBoxNoteGraphNodeTool.java`, `TextBoxNoteGraphNodeToolProperties.java`, `TitleAssistantCondition.java`, `LicenseAssistantCondition.java`, `JIPipeDesktopHTMLTextParameterEditorUI.java`. `plugins`: `jipipe-plugin-ij-3d` (`ROIManagerPlugin3D:485`), `jipipe-plugin-ij` (`JIPipeDesktopLegacyImageViewerPanel2D:318/410`, `ROIManagerPlugin2D:543`), `jipipe-plugin-ij-algorithms` (`ArrangeChannelsAlgorithm:205`), `jipipe-plugin-r` (`REnvironmentFromSystemSetupTool:60`).

- [ ] **Step 1: Migrate jipipe-core sites**

- [ ] **Step 2: Compile core**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS

- [ ] **Step 3: Migrate plugin sites + compile each module**

Run: `mvn -q compile -pl plugins/jipipe-plugin-ij-3d,plugins/jipipe-plugin-ij,plugins/jipipe-plugin-ij-algorithms,plugins/jipipe-plugin-r`
Expected: BUILD SUCCESS

- [ ] **Step 4: Targeted dialogs tests (gate still red — do NOT run full suite)**

Run: `mvn -q test -pl jipipe-core -Dtest='*Dialog*,ButtonTest,DialogChromeTest' 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core/src/main && git commit -m "refactor(dialogs): wave 2 — input dialogs to typed facade (core) (#1341)"
git add -A plugins && git commit -m "refactor(dialogs): wave 2 — input dialogs to typed facade (plugins) (#1341)"
```

---

### Task 10: Wave 3 — migrate `showMessageDialog` (136 sites) + `JColorChooser`

**Files:** modify call sites (jipipe-core + 4 plugins).

**Interfaces:**
- Consumes: `message`, `error`, `chooseColor` (Tasks 3, 5).

**Mapping rules:**
- `showMessageDialog(parent, message, title, INFORMATION_MESSAGE)` → `JIPipeDesktopDialogs.message(parent, title, message)`.
- `showMessageDialog(parent, message, title, ERROR_MESSAGE)` → same → `message(...)` **unless** the message embeds an exception string — then `error(parent, title, message, cause)` where a `Throwable` is in scope (check each; most error messages are plain strings → `message` with the existing text verbatim).
- `showMessageDialog` with `WARNING_MESSAGE`/`QUESTION_MESSAGE` → `message` (iconless, spec keeps it simple).
- `JIPipeDesktopColorChooserButton:62` (`JColorChooser.showDialog`) → `JIPipeDesktopDialogs.chooseColor(this, selectColorPrompt, selectedColor, true).orElse(selectedColor)`-style (keep current value on dismiss, matching `JColorChooser`'s null behavior).
- `UIUtils.selectColor` body → `return JIPipeDesktopDialogs.chooseColor(parent, title, defaultValue, withTransparency).orElse(null);` (keep signature: 5 callers use its `null`-on-cancel return).

**Site list (136 `showMessageDialog`):** concentrate in `JIPipeDesktopProjectWindow.java` (~20), `JIPipeDesktopProjectWorkbench.java` (~18), `JIPipeDesktop.java`, `UIUtils.java`, `ExpressionBuilderUI.java`, `JIPipeDesktopHTMLEditor.java`, `DataExportExpressionParameterEditorPathChooserUI.java`, `JIPipeDesktopBackupManagerPanel.java`, `JIPipeDesktopAlgorithmCacheBrowserUI.java`, `JIPipeDesktopThemeManager.java`, `CreateLaunchersTool.java`, `BuildAIEmbeddingIndexTool.java`, `ExportGlobalEmbeddingDatabaseTool.java`, `JIPipeDesktopParameterExplorerWindow.java`, `JIPipeDesktopAISetupDialog.java`, `GetJIPipeSlotFolderAlgorithm.java`, `REnvironmentFromSystemSetupTool.java`, `PythonEnvironmentFromCondaSetupTool.java`, `JIPipeDesktopLegacyImageViewerPanel2D.java`, `ROIManagerPlugin2D.java`/`3D`, `SpotsManagerPlugin2D.java`/`TracksManagerPlugin2D.java`, `FilamentsManagerPlugin2D.java`, `MultiTemplateMatchingAlgorithm.java`, `FormsDialog.java`, `JIPipeDesktopColorChooserButton.java` (+`JColorChooser`). **Get the exact remaining list at execution time:** `grep -rn "showMessageDialog\|JColorChooser" --include="*.java" jipipe-core/src/main plugins` — migrate every hit.

- [ ] **Step 1: Migrate jipipe-core sites + `JColorChooser`/`selectColor`**

- [ ] **Step 2: Compile core**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS

- [ ] **Step 3: Migrate plugin sites + compile each module**

- [ ] **Step 4: Targeted dialogs tests**

Run: `mvn -q test -pl jipipe-core -Dtest='*Dialog*,ButtonTest,DialogChromeTest' 2>&1 | tail -5`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A jipipe-core/src/main && git commit -m "refactor(dialogs): wave 3 — message dialogs + color chooser (core) (#1341)"
git add -A plugins && git commit -m "refactor(dialogs): wave 3 — message dialogs + color chooser (plugins) (#1341)"
```

---

### Task 11: Wave 4 — sweep, delete wrappers, make the gate green

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/UIUtils.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/dialogs/NoJOptionPaneGateTest.java` (already written, Task 7)

**Interfaces:**
- Consumes: the whole facade; produces: a `JOptionPane`-free main codebase.

- [ ] **Step 1: Find every remaining `JOptionPane` reference in main code**

Run: `grep -rn "JOptionPane" --include="*.java" jipipe-core/src/main plugins | grep -v import`
Expected: only `UIUtils` internal wrappers (if any survived as non-delegating) — migrate or delete each.

- [ ] **Step 2: Delete now-unused `UIUtils` dialog wrappers** (only those with zero remaining callers — check each with `grep -rn "UIUtils.<name>"` first):
`askOpenInCurrentWindow`, `checkAndAskIfFileExists`, `getIntegerByDialog`, `getDoubleByDialog`, `getMinMaxByDialog`, `getUniqueStringByDialog`, `getMultiLineStringByDialog`, `showOKCancelDialog`, the `showConfirmDialog(parent,title,size,component)` overload, `getHTMLByDialog` (if its body is now a thin facade call and callers migrated — prefer migrating the 2 callers to the builder here and deleting). **Keep** `selectColor` (5 callers, now a facade one-liner) and `getSelectionByDialog` (JList multi-select, no facade equivalent; its internals were migrated in Task 9).

- [ ] **Step 3: Remove `JOptionPane` imports from every touched file**

Run: `grep -rln "import javax.swing.JOptionPane" --include="*.java" jipipe-core/src/main plugins`
Expected: empty. If not, fix each file.

- [ ] **Step 4: Run the gate — it must be GREEN**

Run: `mvn -q test -pl jipipe-core -Dtest=NoJOptionPaneGateTest`
Expected: PASS (zero matches)

- [ ] **Step 5: Full compile of everything + full jipipe-core test suite (first full run since Task 7)**

Run: `mvn -q compile 2>&1 | tail -3` then `mvn -q test -pl jipipe-core 2>&1 | tail -8`
Expected: BUILD SUCCESS; all jipipe-core tests pass (the ImageJ patcher listener handles ordering; see AGENTS.md).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor(dialogs): wave 4 — sweep to zero JOptionPane, delete superseded UIUtils wrappers (#1341)"
```

---

### Task 12: Verification record + push

**Files:**
- Create: `docs/superpowers/plans/2026-11-03-modern-dialog-api-verification.md`

- [ ] **Step 1: Run the full verification battery and record it**

Run and capture output:
- `mvn -q compile` (all modules)
- `mvn -q test -pl jipipe-core` (full suite)
- `grep -rc "JOptionPane" --include="*.java" jipipe-core/src/main plugins | grep -v ":0" | wc -l` → expected `0`
- `git log --oneline origin/master..HEAD` (the commit list)

Write `docs/superpowers/plans/2026-11-03-modern-dialog-api-verification.md`: date, commands, pass/fail per command, the zero-JOptionPane proof, the two deliberate behavior changes (X/Escape dismiss; empty inputText = dismiss), and the bug fixes found en route (`ExpressionBuilderUI` raw-int switch, `FormsDialog` Show-errors conflation).

- [ ] **Step 2: Commit + push (remote is authoritative)**

```bash
git add docs/superpowers/plans/2026-11-03-modern-dialog-api-verification.md
git commit -m "docs(dialogs): verification record (#1341)"
git fetch origin
git rebase origin/master   # no-op if master hasn't moved
git push -u origin 1341-modern-dialog-api
```

Expected: push succeeds; branch `1341-modern-dialog-api` on origin.
