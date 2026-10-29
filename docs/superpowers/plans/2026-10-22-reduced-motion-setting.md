# Reduced Motion Setting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Reduced motion" application setting that disables UI motion — caret blinking in text/script editors,
the progress spinner animation (replaced by a static hourglass), and the notification badge's color cross-fade (kept a
static orange).

**Architecture:** A single boolean lives on the existing General-UI settings sheet
(`JIPipeGeneralUIApplicationSettings`, category `UI`) with parameter id `reduced-motion`, default `false` (animations
on, current behavior). A guarded read helper `ThemeUtils.isReducedMotion()` is the single source for every animated
surface. Each surface captures the flag at construction (from the helper), re-applies it live via the settings service's
`ChangedEventEmitter` (fired on every settings save/reload), and exposes a pure decision path that is testable
headlessly without a live `JIPipe` instance. The big Settings dialog renders the field automatically; the "Customize
JIPipe" dialog gets an explicit checkbox wire. A full desktop motion audit added two more surfaces: indeterminate
progress bars (made static via the app's custom LAF) and the remaining decorative animations (splash icon showcase,
search attention fade, notification heading rotation, caret blink in the HTML editor and read-only text panes).

**Tech Stack:** Java 21, Swing (`javax.swing.Timer`, `DefaultCaret`/RSyntaxTextArea `ConfigurableCaret`, LAF
`BasicProgressBarUI`), JUnit 5 (Jupiter) in `jipipe-core`, Jackson (existing) for settings serialization.

**Spec:** GitLab work item RGerst/jipipe#1335 — "Add into settings and theme settings a reduced motion setting that (1)
disables caret blinking in text/script editors, (2) shows a static hourglass in spinner instead of an animation, (3)
disables notification icon animation color change (stays static orange)." Plus maintainer notes: the setting also needs
a wire in the "Customize JIPipe" dialog, and any other UI element that produces motion must have its motion reduced
(elements that do not produce motion are left alone).

## Global Constraints

- **All files in `jipipe-core/`** (the "desktop" UI lives under `jipipe-core/src/main/java/org/hkijena/jipipe/desktop`
  and `.../utils`; there is **no** separate `jipipe-desktop` module).
- **Registration/parameter IDs are stable string literals.** The new parameter id is exactly `"reduced-motion"`. Never
  derive it from code.
- **Test policy (AGENTS.md):** No Swing *structure* tests (no asserting component hierarchy, pixels, tab titles,
  visibility toggles). Tests pin **non-trivial behavior only** — here: the setting's default/serialization contract, and
  the "reduced motion ⇒ no running timer / static state" decision on each surface. Pixel/drawing output is verified by
  the user by eye, not tested.
- **ImageJ legacy patcher:** New tests must NOT call `LegacyInjector.preinit()`, must NOT add `--add-opens`/javaagent
  flags, and must NOT touch `ij.*` classes or call `JIPipe.ensureInstance()`. The existing
  `ImageJLegacyPatcherLauncherSessionListener` already protects the suite; these tests stay pure/headless so they never
  trigger it.
- **Headless-safe:** Every test runs with `-Djava.awt.headless=true` implicitly (Swing `Timer`/`ImageIcon`/
  `JTextComponent` construction is headless-safe; do not call `setVisible(true)` or paint to a real screen).
- **No live-`JIPipe` dependency in the pure decision paths.** Tests force the flag via a setter; the default constructor
  path reads `ThemeUtils.isReducedMotion()`, which safely returns `false` when `JIPipe.getInstance() == null`.

## Review Focus

Inputs/conditions the spec implies but that no unit test exercises (the user checks these by eye in the running app):

1. **Hourglass renders correctly at all spinner sizes** (16/24/32/48 px, light + dark theme) — expect a crisp,
   theme-aware static hourglass, not a stretched/blank icon, in the run queue, search field, splash, AI/server status
   controls. *Owned by Task 2 (drawing); verify by eye, no pixel test.*
2. **Static orange notification badge** — with reduced motion on and notifications present, the badge is a solid orange
   `warning.png` (full opacity), not a pulsing cross-fade and not the blue-gray `emblem-important.png`. *Owned by Task
   3; verify by eye.*
3. **Caret is visible but not blinking** while typing in the code editor, the inline script parameter editor, and the
   HTML editor; the read-only panes (markdown reader, log view, project overview, run-next) show no blinking caret when
   clicked. Turning the setting off restores the normal ~500 ms blink. *Owned by Tasks 4 + 8; verify by eye.*
4. **Indeterminate progress bars are static** (a centered hourglass, sized to the bar) in the run queue logger and the
   queue menu item, and determinate bars still fill normally. *Owned by Task 6; verify by eye.*
5. **Live re-apply without restart** — toggling the setting (in either the Settings dialog or Customize JIPipe) and
   saving changes already-open spinners/badges/editors/progress bars immediately, including resuming animation for an
   in-flight run or present notification when the setting is turned off. *Owned by Tasks 2–6 (the
   `ChangedEventEmitter` / per-paint read); verify by eye.*
6. **Default/upgrade safety** — a user with an existing `settings.json` that has no `reduced-motion` key gets animations
   ON (no behavior change), and the key round-trips through save. *Partially tested in Task 1 (missing-key fallback);
   the full-app path is verified by eye.*
7. **Splash screen honors the setting pre-init** — because the splash shows before `JIPipe` is initialized, the "Powered
   by" icon showcase must be frozen via the raw-settings read (`isReducedMotionFromRaw`), not the live
   `ThemeUtils.isReducedMotion()`. *Owned by Task 7; verify by eye on a fresh startup with the setting ON.*

---

### Task 1: Add the `reduced-motion` setting + `ThemeUtils.isReducedMotion()` helper

**Files:**

- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettings.java`
  (add field after line 47 `searchImplementation`, add getter/setter near the other booleans)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ThemeUtils.java` (add the `isReducedMotion` static
  method + imports)
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettingsReducedMotionTest.java`

**Interfaces:**

- Produces:
    - `JIPipeGeneralUIApplicationSettings.isReducedMotion(): boolean`
    - `JIPipeGeneralUIApplicationSettings.setReducedMotion(boolean)`
    - `ThemeUtils.isReducedMotion(): boolean` — returns the setting when a JIPipe instance is available, else `false`
      (never throws).
- Consumes: none (foundation task).

- [ ] **Step 1: Write the failing test**

Create
`jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettingsReducedMotionTest.java`:

```java
package org.hkijena.jipipe.plugins.settings.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Pure/headless: constructs the sheet directly, never calls JIPipe.ensureInstance().
// Pins the reduced-motion setting contract: default, serialization key, round-trip,
// and back-compat fallback for settings.json files that predate the key.
class JIPipeGeneralUIApplicationSettingsReducedMotionTest {

    @Test
    void defaultIsAnimationsOn() {
        assertFalse(new JIPipeGeneralUIApplicationSettings().isReducedMotion());
    }

    @Test
    void serializesUnderStableKey() throws Exception {
        JIPipeGeneralUIApplicationSettings s = new JIPipeGeneralUIApplicationSettings();
        s.setReducedMotion(true);
        JsonNode node = JsonUtils.getObjectMapper().readTree(s.serializeToJsonString());
        assertTrue(node.has("reduced-motion"), "expected stable key 'reduced-motion'");
        assertTrue(node.get("reduced-motion").asBoolean());
    }

    @Test
    void roundTripPreservesValue() throws Exception {
        JIPipeGeneralUIApplicationSettings s = new JIPipeGeneralUIApplicationSettings();
        s.setReducedMotion(true);
        JsonNode node = JsonUtils.getObjectMapper().readTree(s.serializeToJsonString());
        JIPipeGeneralUIApplicationSettings back = new JIPipeGeneralUIApplicationSettings();
        back.deserializeFromJsonNode(node);
        assertTrue(back.isReducedMotion());
    }

    @Test
    void missingKeyFallsBackToDefault() throws Exception {
        // settings.json predating reduced-motion: only an unrelated key is present.
        JsonNode node = JsonUtils.getObjectMapper().readTree("{\"maximize-windows\":true}");
        JIPipeGeneralUIApplicationSettings back = new JIPipeGeneralUIApplicationSettings();
        back.deserializeFromJsonNode(node);
        assertFalse(back.isReducedMotion());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeGeneralUIApplicationSettingsReducedMotionTest`
Expected: FAIL — `isReducedMotion`/`setReducedMotion` do not exist (compilation error).

- [ ] **Step 3: Add the field + accessors to the settings sheet**

In `JIPipeGeneralUIApplicationSettings.java`, add the field after the `searchImplementation` field (line 47):

```java
    private boolean reducedMotion = false;
```

Add the accessors (place them with the other booleans, e.g. right after the `searchImplementation` setter, keeping the
getter/setter pair together). The name/description below are what the Settings UI shows:

```java
    @SetJIPipeDocumentation(name = "Reduced motion",
            description = "If enabled, UI animations are disabled: the caret in text/script editors stays fixed, "
                    + "progress spinners show a static hourglass, and the notification icon stays a static orange "
                    + "icon instead of animating.")
    @JIPipeParameter("reduced-motion")
    public boolean isReducedMotion() {
        return reducedMotion;
    }

    @JIPipeParameter("reduced-motion")
    public void setReducedMotion(boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
    }
```

- [ ] **Step 4: Add the guarded read helper to `ThemeUtils`**

In `ThemeUtils.java`, add imports if absent:
`org.hkijena.jipipe.JIPipe` and `org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings`.

Add this static method (the caret policy method `applyCaretMotion` is added in Task 4, next to its test, so that task's
TDD cycle is genuine):

```java
    /**
     * @return true when the user has enabled reduced motion. Safe to call at any time:
     * returns false (animations on) when JIPipe is not initialized or the setting is unavailable.
     */
    public static boolean isReducedMotion() {
        if (JIPipe.getInstance() == null) {
            return false;
        }
        try {
            return JIPipeGeneralUIApplicationSettings.getInstance().isReducedMotion();
        } catch (Throwable t) {
            return false;
        }
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeGeneralUIApplicationSettingsReducedMotionTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Note (no extra UI code for the main Settings dialog)**

`JIPipeDesktopApplicationSettingsUI` renders every `@JIPipeParameter` of a sheet generically, so "Reduced motion" now
automatically appears under **Settings → UI → General** (a checkbox) with no extra code. The only place needing an
explicit wire is the "Customize JIPipe" dialog — that is Task 5. (Verify by eye: open Settings, expand the UI category →
General sheet, expect the new checkbox.)

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettings.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/utils/ThemeUtils.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettingsReducedMotionTest.java
git commit -m "feat(settings): add reduced-motion application setting + ThemeUtils helper"
```

---

### Task 2: `SpinnerIcon` shows a static hourglass under reduced motion

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/icons/SpinnerIcon.java`
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/icons/SpinnerIconReducedMotionTest.java`

**Interfaces:**

- Consumes: `ThemeUtils.isReducedMotion(): boolean` (Task 1).
- Produces:
    - `SpinnerIcon.setReducedMotion(boolean)`, `SpinnerIcon.isReducedMotion(): boolean`
    - `SpinnerIcon` implements `JIPipeApplicationSettingsServiceComponent.ChangedEventListener` →
      `onApplicationSettingsChanged()` (re-reads the setting).
    - A `shouldAnimate` intent flag (set by `start()`/`stop()`) is kept separate from the physical timer, so toggling
      reduced motion **off** mid-run resumes animation, and toggling it **on** pauses it.
    - `start()` records intent and only runs the timer when reduced motion is off.
- The existing constructors `SpinnerIcon(Component)` and `SpinnerIcon(Component, int)` keep their signatures; the
  subclass `JIPipeDesktopRunnableQueueSpinnerIcon` and all call sites (`start()`/`stop()`/`getTimer()`) are unchanged.

- [ ] **Step 1: Write the failing test**

Create
`jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/icons/SpinnerIconReducedMotionTest.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.icons;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.*;

// Headless: Timer start/stop and isRunning() are screen-independent. We force the flag
// via setReducedMotion and assert the animation decision, not the drawn pixels
// (the static hourglass is verified by the user by eye).
class SpinnerIconReducedMotionTest {

    @Test
    void reducedMotionSuppressesAnimation() {
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(true);
        icon.start();
        assertFalse(icon.getTimer().isRunning(), "spinner must not animate under reduced motion");
        assertTrue(icon.isReducedMotion());
    }

    @Test
    void animationRunsWhenNotReduced() {
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(false);
        icon.start();
        assertTrue(icon.getTimer().isRunning(), "spinner animates when reduced motion is off");
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }

    @Test
    void stoppingWhileReducedKeepsItStatic() {
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(true);
        icon.start();
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }

    @Test
    void turningReducedMotionOffMidRunResumesAnimation() {
        // A run is in progress (start() already called) while reduced motion is on:
        // static. When the user disables reduced motion, the in-flight run must resume animating
        // immediately (no waiting for the run to finish).
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(true);
        icon.start();               // run started
        assertFalse(icon.getTimer().isRunning());
        icon.setReducedMotion(false); // user turns the setting off
        assertTrue(icon.getTimer().isRunning(), "in-flight run must resume animating");
        icon.stop();                // run finished
        assertFalse(icon.getTimer().isRunning());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=SpinnerIconReducedMotionTest`
Expected: FAIL — `setReducedMotion`/`isReducedMotion` do not exist (compilation error).

- [ ] **Step 3: Implement reduced motion on `SpinnerIcon`**

Edit `SpinnerIcon.java`:

- Import `org.hkijena.jipipe.JIPipe` and
  `org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent`.
- Make the class also implement `JIPipeApplicationSettingsServiceComponent.ChangedEventListener` (add to the
  `class ... implements Icon` line).
- Add fields `private boolean reducedMotion;` and `private boolean shouldAnimate = false;` (the intent). In the
  two-constructor chain, at the end of `SpinnerIcon(Component parent, int size)`, after the timer/line/gradient setup,
  add:
  ```java
  setReducedMotion(ThemeUtils.isReducedMotion());
  try {
      JIPipe.getSettings().getChangedEventEmitter().subscribeWeak(this);
  } catch (Throwable ignored) {
      // JIPipe not initialized (e.g. headless/test) — no live re-apply needed.
  }
  ```
  Do NOT call `parent.repaint()` from inside the constructor (the parent may not be displayable yet); defer repaints to
  the setters/`start()`.
- Add the methods:
  ```java
  public boolean isReducedMotion() {
      return reducedMotion;
  }

  public void setReducedMotion(boolean reducedMotion) {
      this.reducedMotion = reducedMotion;
      if (reducedMotion) {
          if (timer.isRunning()) {
              timer.stop();
          }
      } else if (shouldAnimate) {
          // Setting turned off while a run is in flight: resume immediately.
          timer.start();
      }
      if (parent != null) {
          parent.repaint();
      }
  }

  @Override
  public void onApplicationSettingsChanged() {
      setReducedMotion(ThemeUtils.isReducedMotion());
  }
  ```
- Rework `start()`/`stop()` to separate intent from the physical timer:
  ```java
  public void start() {
      shouldAnimate = true;
      if (!reducedMotion) {
          timer.start();
      }
      if (parent != null) {
          parent.repaint();
      }
  }

  public void stop() {
      shouldAnimate = false;
      timer.stop();
      if (parent != null) {
          parent.repaint();
      }
  }
  ```
- Change `paintIcon(...)` so the static state draws a **hourglass** when reduced motion is on, keeping the existing
  spokes/oval for the normal cases:
  ```java
  if (reducedMotion) {
      javax.swing.Icon hourglass = JIPipe.RESOURCES.getIcon16("emblems/emblem-hourglass.png");
      graphics2D.drawImage(hourglass.getImage(), x, y, getIconWidth(), getIconHeight(), null);
  } else if (timer.isRunning()) {
      // ... existing spokes loop ...
  } else {
      // ... existing oval ...
  }
  ```
  (Keep the antialiasing/stroke rendering hints at the top of the method unchanged.)

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=SpinnerIconReducedMotionTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/icons/SpinnerIcon.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/icons/SpinnerIconReducedMotionTest.java
git commit -m "feat(ui): static hourglass spinner under reduced motion"
```

Note on the subclass: `JIPipeDesktopRunnableQueueSpinnerIcon` already calls the inherited `start()`/`stop()`, so the
`shouldAnimate` intent and the reduced-motion gating work for it and every other call site with no changes.

---

### Task 3: `JIPipeDesktopAnimatedIcon` shows a static orange icon under reduced motion

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/icons/JIPipeDesktopAnimatedIcon.java`
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/icons/JIPipeDesktopAnimatedIconReducedMotionTest.java`

**Interfaces:**

- Consumes: `ThemeUtils.isReducedMotion(): boolean` (Task 1).
- Produces:
    - `JIPipeDesktopAnimatedIcon.setReducedMotion(boolean)`, `isReducedMotion(): boolean`, `getOpacity(): double`
    - Implements `JIPipeApplicationSettingsServiceComponent.ChangedEventListener` → `onApplicationSettingsChanged()`.
    - A `shouldAnimate` intent flag (set by `start()`/`stop()`) so toggling reduced motion **off** while a notification
      is present resumes the cross-fade, and toggling it **on** pins a static orange icon.
    - When reduced motion: `opacity` pinned to `1.0` (top icon = orange `warning.png` fully visible) and the timer is
      stopped; the cross-fade resumes when the setting is turned off and a notification is still present.
- The existing 5-arg constructor keeps its signature; the three notification/log button call sites are unchanged.

- [ ] **Step 1: Write the failing test**

Create
`jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/icons/JIPipeDesktopAnimatedIconReducedMotionTest.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.icons;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

// Headless. Two 1x1 ImageIcons stand in for the real orange/blue badges. We assert the
// static-state contract (opacity pinned to full, timer never running) — the actual
// orange glyph is verified by the user by eye.
class JIPipeDesktopAnimatedIconReducedMotionTest {

    private static ImageIcon pixel() {
        return new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
    }

    @Test
    void reducedMotionShowsStaticTopIconAndNoTimer() {
        JIPipeDesktopAnimatedIcon icon = new JIPipeDesktopAnimatedIcon(new JPanel(), pixel(), pixel(), 100, 0.05);
        icon.setReducedMotion(true);
        icon.start();
        assertFalse(icon.getTimer().isRunning(), "badge must not animate under reduced motion");
        assertEquals(1.0, icon.getOpacity(), 1e-9, "top (orange) icon must be fully opaque when static");
        assertTrue(icon.isReducedMotion());
    }

    @Test
    void animationRunsWhenNotReduced() {
        JIPipeDesktopAnimatedIcon icon = new JIPipeDesktopAnimatedIcon(new JPanel(), pixel(), pixel(), 100, 0.05);
        icon.setReducedMotion(false);
        icon.start();
        assertTrue(icon.getTimer().isRunning());
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }

    @Test
    void turningReducedMotionOffWhilePresentResumesCrossfade() {
        // A notification is present (start() called) while reduced motion is on: static orange.
        // When the user disables reduced motion, the cross-fade resumes immediately.
        JIPipeDesktopAnimatedIcon icon = new JIPipeDesktopAnimatedIcon(new JPanel(), pixel(), pixel(), 100, 0.05);
        icon.setReducedMotion(true);
        icon.start();
        assertFalse(icon.getTimer().isRunning());
        assertEquals(1.0, icon.getOpacity(), 1e-9);
        icon.setReducedMotion(false);
        assertTrue(icon.getTimer().isRunning(), "cross-fade must resume when reduced motion is off");
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeDesktopAnimatedIconReducedMotionTest`
Expected: FAIL — `setReducedMotion`/`getOpacity` do not exist (compilation error).

- [ ] **Step 3: Implement reduced motion on `JIPipeDesktopAnimatedIcon`**

Edit `JIPipeDesktopAnimatedIcon.java`:

- Import `org.hkijena.jipipe.JIPipe`,
  `org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent`, and
  `org.hkijena.jipipe.utils.ThemeUtils`.
- Make the class also implement `JIPipeApplicationSettingsServiceComponent.ChangedEventListener`.
- Add fields `private boolean reducedMotion;` and `private boolean shouldAnimate = false;`. In the existing constructor,
  after `setCoalesce(false)`, add:
  ```java
  setReducedMotion(ThemeUtils.isReducedMotion());
  try {
      JIPipe.getSettings().getChangedEventEmitter().subscribeWeak(this);
  } catch (Throwable ignored) {
      // JIPipe not initialized (headless/test) — no live re-apply needed.
  }
  ```
- Add methods:
  ```java
  public boolean isReducedMotion() {
      return reducedMotion;
  }

  public double getOpacity() {
      return opacity;
  }

  public void setReducedMotion(boolean reducedMotion) {
      this.reducedMotion = reducedMotion;
      if (reducedMotion) {
          if (timer.isRunning()) {
              timer.stop();
          }
          opacity = 1.0; // static: show the top (orange) icon fully
      } else {
          opacity = 0.0; // restart the cross-fade from the bottom icon
          if (shouldAnimate && !timer.isRunning()) {
              // Setting turned off while a notification is still present: resume.
              timer.start();
          }
      }
      if (parent != null) {
          parent.repaint();
      }
  }

  @Override
  public void onApplicationSettingsChanged() {
      setReducedMotion(ThemeUtils.isReducedMotion());
  }
  ```
- Rework `start()`/`stop()` to separate intent from the physical timer:
  ```java
  public void start() {
      shouldAnimate = true;
      if (!reducedMotion) {
          timer.start();
      }
      if (parent != null) {
          parent.repaint();
      }
  }

  public void stop() {
      shouldAnimate = false;
      timer.stop();
  }
  ```
- `paintIcon` needs **no change**: with `opacity == 1.0` it already draws the bottom icon then the top icon at full
  alpha (i.e., the orange icon), which is the desired static result.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeDesktopAnimatedIconReducedMotionTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/icons/JIPipeDesktopAnimatedIcon.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/icons/JIPipeDesktopAnimatedIconReducedMotionTest.java
git commit -m "feat(ui): static orange notification badge under reduced motion"
```

---

### Task 4: Disable caret blinking in text/script editors under reduced motion

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ThemeUtils.java` (add `applyCaretMotion` +
  `watchCaretMotion`)
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/codeeditor/JIPipeDesktopCodeEditorRSyntaxTextEditorUI.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopScriptParameterEditorUI.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ThemeUtilsCaretMotionTest.java`

**Interfaces:**

- Consumes: `ThemeUtils.isReducedMotion()` (Task 1); `CustomEditorPane` (existing, in `org.hkijena.jipipe.utils`).
- Produces:
    - `ThemeUtils.applyCaretMotion(javax.swing.text.JTextComponent, boolean)` — reduced motion ⇒
      `DefaultCaret.setBlinkRate(0)` + `ConfigurableCaret.setAlwaysVisible(true)`; otherwise restore
      `setBlinkRate(500)` + `setAlwaysVisible(false)`. No-op if the caret is not a `DefaultCaret`. (Works for
      `BasicCaret` too, which is what plain `JTextPane`/`JTextField` use.)
    - `ThemeUtils.watchCaretMotion(javax.swing.text.JTextComponent)` — applies the current value now and subscribes for
      live re-apply, keeping the listener retained for the component's lifetime via a static `WeakHashMap` (an
      inline/local listener passed to `subscribeWeak` would be GC'd immediately; the map holds it while the component is
      strongly reachable). **This is the one-liner every text surface (Tasks 4 + 8) calls.**
    - Both `CustomEditorPane` editors call `watchCaretMotion` at construction.

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ThemeUtilsCaretMotionTest.java`:

```java
package org.hkijena.jipipe.utils;

import org.fife.ui.rtextarea.ConfigurableCaret;
import org.junit.jupiter.api.Test;

import javax.swing.JTextPane;
import javax.swing.text.DefaultCaret;

import static org.junit.jupiter.api.Assertions.*;

// Headless: a CustomEditorPane (RSyntaxTextArea subclass) has a ConfigurableCaret, and a plain
// JTextPane has a BasicCaret. We pin the blink mechanism on both, and the no-op guard.
class ThemeUtilsCaretMotionTest {

    @Test
    void reducedMotionDisablesBlinkingOnRsyntaxCaret() {
        CustomEditorPane pane = new CustomEditorPane();
        DefaultCaret caret = (DefaultCaret) pane.getCaret();
        caret.setBlinkRate(500); // known starting state
        ThemeUtils.applyCaretMotion(pane, true);
        assertEquals(0, caret.getBlinkRate(), "blink rate must be 0 under reduced motion");
        assertTrue(((ConfigurableCaret) pane.getCaret()).isAlwaysVisible(),
                "caret must stay visible (no flicker) under reduced motion");
    }

    @Test
    void clearingReducedMotionRestoresBlinking() {
        CustomEditorPane pane = new CustomEditorPane();
        DefaultCaret caret = (DefaultCaret) pane.getCaret();
        ThemeUtils.applyCaretMotion(pane, true);
        ThemeUtils.applyCaretMotion(pane, false);
        assertNotEquals(0, caret.getBlinkRate(), "blink rate restored when reduced motion is off");
        assertFalse(((ConfigurableCaret) pane.getCaret()).isAlwaysVisible());
    }

    @Test
    void reducedMotionDisablesBlinkingOnPlainBasicCaret() {
        // Plain JTextPane uses a BasicCaret (a DefaultCaret subclass) — the HTML editor and
        // read-only panes rely on the same path.
        JTextPane pane = new JTextPane();
        DefaultCaret caret = (DefaultCaret) pane.getCaret();
        ThemeUtils.applyCaretMotion(pane, true);
        assertEquals(0, caret.getBlinkRate(), "plain BasicCaret must not blink under reduced motion");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl jipipe-core test -Dtest=ThemeUtilsCaretMotionTest`
Expected: FAIL — `ThemeUtils.applyCaretMotion` does not exist (compilation error).

- [ ] **Step 3: Add `applyCaretMotion` + `watchCaretMotion` to `ThemeUtils`**

In `ThemeUtils.java`, add imports if absent: `javax.swing.text.JTextComponent`, `javax.swing.text.Caret`,
`javax.swing.text.DefaultCaret`, `org.fife.ui.rtextarea.ConfigurableCaret`,
`org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent`, `java.util.Collections`,
`java.util.Map`, `java.util.WeakHashMap`. Add next to `isReducedMotion` (Task 1):

```java
    private static final Map<JTextComponent, JIPipeApplicationSettingsServiceComponent.ChangedEventListener> CARET_MOTION_WATCHERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Applies or clears reduced-motion behavior on a text component's caret.
     * Reduced motion: no blinking, caret always visible (RSyntax carets). Otherwise: restore the
     * standard 500 ms blink. No-op if the component's caret is not a {@link DefaultCaret}.
     */
    public static void applyCaretMotion(JTextComponent component, boolean reducedMotion) {
        Caret caret = component.getCaret();
        if (!(caret instanceof DefaultCaret defaultCaret)) {
            return;
        }
        defaultCaret.setBlinkRate(reducedMotion ? 0 : 500);
        if (caret instanceof ConfigurableCaret configurableCaret) {
            configurableCaret.setAlwaysVisible(reducedMotion);
        }
    }

    /**
     * Applies the current reduced-motion value to a text component's caret and keeps it in sync
     * for the component's lifetime via a settings change listener. Call once per text surface.
     * The listener is retained while the component is strongly reachable (weak-map key), then
     * released, so closing a dialog leaks nothing.
     */
    public static void watchCaretMotion(JTextComponent component) {
        applyCaretMotion(component, isReducedMotion());
        JIPipeApplicationSettingsServiceComponent.ChangedEventListener listener =
                () -> applyCaretMotion(component, isReducedMotion());
        CARET_MOTION_WATCHERS.put(component, listener);
        try {
            JIPipe.getSettings().getChangedEventEmitter().subscribeWeak(listener);
        } catch (Throwable ignored) {
            // JIPipe not initialized — no live re-apply needed.
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=ThemeUtilsCaretMotionTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Wire the code editor UI**

In `JIPipeDesktopCodeEditorRSyntaxTextEditorUI.java` (`ThemeUtils` is already imported), in `initialize()`, immediately
after `textArea.setTabSize(4);` (line ~39), add:

```java
ThemeUtils.watchCaretMotion(textArea);
```

- [ ] **Step 6: Wire the script parameter editor UI**

In `JIPipeDesktopScriptParameterEditorUI.java` (ensure `ThemeUtils` is imported), in `initializeTextEditor(...)`,
immediately after `textArea.setTabSize(4);` (line ~120), add:

```java
ThemeUtils.watchCaretMotion(textArea);
```

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/ThemeUtils.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/codeeditor/JIPipeDesktopCodeEditorRSyntaxTextEditorUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/ui/api/JIPipeDesktopScriptParameterEditorUI.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/utils/ThemeUtilsCaretMotionTest.java
git commit -m "feat(ui): disable caret blinking in code/script editors under reduced motion"
```

---

### Task 5: Wire the setting into the "Customize JIPipe" dialog

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/customizer/JIPipeDesktopCustomizerDialog.java`

**Interfaces:**

- Consumes: `JIPipeGeneralUIApplicationSettings.isReducedMotion()`/`setReducedMotion(boolean)` (Task 1).
- Produces: a checkbox in the Customize JIPipe dialog; on Save it persists via the existing
  `JIPipe.getSettings().save()` (which also fires the `ChangedEvent` that Tasks 2–4, 6–8 use to live re-apply).
- No unit test (pure Swing form structure — per test policy, verified by the user by eye).

- [ ] **Step 1: Add the checkbox field**

In `JIPipeDesktopCustomizerDialog.java`, add a field near the other controls (after `scaleLabel`, ~line 30):

```java
    private final JCheckBox reducedMotionCheckBox = new JCheckBox("Reduce motion (disable UI animations)");
```

(`javax.swing.*` is already imported, so `JCheckBox` can be used without qualification.)

- [ ] **Step 2: Load its initial state**

In `loadDefaults()`, after the `fileChooserTypeJComboBox`/`fallback...` selections (before the scale slider lines), add:

```java
        reducedMotionCheckBox.setSelected(uiSettings.isReducedMotion());
```

- [ ] **Step 3: Add it to the settings panel**

In `initializeSettingsPanel()`, after the "Theme" group block (after
`settingsPanel.addToForm(themeStyleJComboBox, new JLabel("Style"));`, ~line 145) and before the "UI Scale" group, insert
a Motion group:

```java
        settingsPanel.addGroupHeader("Motion", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        settingsPanel.addWideToForm(reducedMotionCheckBox);
```

- [ ] **Step 4: Persist it on Save**

In `doActionSave()`, right after `uiSettings.setThemeStyle(...)` (line ~174), add:

```java
        uiSettings.setReducedMotion(reducedMotionCheckBox.isSelected());
```

(The existing `JIPipe.getSettings().save();` on line ~183 then persists it and fires the change event that live
re-applies Tasks 2–4, 6–8.)

- [ ] **Step 5: Verify it compiles**

Run: `mvn -q -pl jipipe-core test-compile`
Expected: BUILD SUCCESS (no compile errors).

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/customizer/JIPipeDesktopCustomizerDialog.java
git commit -m "feat(ui): wire reduced-motion into the Customize JIPipe dialog"
```

---

### Task 6: Static indeterminate progress bars

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/ui/JIPipeDesktopModernProgressBarUI.java`
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/theme/ui/JIPipeDesktopModernProgressBarUIReducedMotionTest.java`

**Why this one file:** `installModernUIs()` (in `ThemeUtils`, already run for every modern-theme app) registers this as
the global `ProgressBarUI`, so it governs **all** `JProgressBar`s in the app — including the indeterminate ones in
`JIPipeDesktopRunQueueLoggerPanel`, `JIPipeDesktopRunnableQueueButtonMenuItem`, and `JIPipeDesktopProgressLoggerPanel`.
`JIPipeDesktopMemoryStatusUI` (a `JProgressBar` subclass) inherits the same UI.

**Interfaces:**

- Consumes: `ThemeUtils.isReducedMotion()` (Task 1); `JIPipe.RESOURCES.getIcon16("actions/hourglass-half.png")` (exists
  in the light + dark icon sets).
- Produces: under reduced motion, `paintIndeterminate` draws a static hourglass icon (centered) instead of the moving
  bar. Determinate bars are unchanged. No changes at the `setIndeterminate(true)` call sites — the LAF owns the
  rendering.

- [ ] **Step 1: Write the headless regression-guard test**

The reduced-motion *paint branch* cannot be unit-tested: `ThemeUtils.isReducedMotion()` returns true only with a live
`JIPipe` instance (which the test policy forbids), and asserting the hourglass pixels is exactly the kind of
Swing-structure test the policy rules out. So this task carries a **guard test, not a TDD test**: it proves the custom
LAF installs and paints headlessly without throwing, and that indeterminate bars route through our `paintIndeterminate`
(i.e. the call sites need no changes). The actual "static hourglass" result is verified by the user by eye.

Create
`jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/theme/ui/JIPipeDesktopModernProgressBarUIReducedMotionTest.java`:

```java
package org.hkijena.jipipe.desktop.commons.theme.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JProgressBar;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

// Headless regression guard: the app's custom ProgressBarUI (installed for all JProgressBars in
// the modern theme) must install and paint an indeterminate bar without throwing. We do NOT
// assert the hourglass pixels (Swing-drawing tests are out of scope per test policy) and we
// cannot force the reduced-motion branch (it needs a live JIPipe instance); the static-hourglass
// result is verified by the user by eye.
class JIPipeDesktopModernProgressBarUIReducedMotionTest {

    @Test
    void customUiInstallsAndPaintsIndeterminateBarHeadlessly() {
        JProgressBar bar = new JProgressBar();
        try {
            bar.setUI(new JIPipeDesktopModernProgressBarUI());
            assertInstanceOf(JIPipeDesktopModernProgressBarUI.class, bar.getUI());
            bar.setIndeterminate(true);
            bar.setSize(100, 8);
            BufferedImage img = new BufferedImage(100, 8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                bar.paint(g); // routes through JIPipeDesktopModernProgressBarUI.paintIndeterminate
            } finally {
                g.dispose();
            }
            assertTrue(bar.isIndeterminate());
        } finally {
            bar.updateUI(); // restore the default UI
        }
    }
}
```

- [ ] **Step 2: Run test — it passes before AND after the change (documented guard)**

Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeDesktopModernProgressBarUIReducedMotionTest`
Expected: PASS (this is a guard, not a red→green TDD step — the base `paintIndeterminate` is also headless-safe).
Proceed to Step 3 to make the behavior change; re-run in Step 4 to confirm the new reduced-motion branch still paints
headlessly.

- [ ] **Step 3: Implement the static indeterminate render**

Replace the body of `JIPipeDesktopModernProgressBarUI.java` (keep the license header and package) with:

```java
public class JIPipeDesktopModernProgressBarUI extends BasicProgressBarUI {

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        if (ThemeUtils.isReducedMotion()) {
            JProgressBar bar = (JProgressBar) c;
            java.awt.Image hourglass = JIPipe.RESOURCES.getIcon16("actions/hourglass-half.png").getImage();
            int w = bar.getWidth();
            int h = bar.getHeight();
            int iconSize = Math.min(w, h);
            int x = (w - iconSize) / 2;
            int y = (h - iconSize) / 2;
            g.drawImage(hourglass, x, y, iconSize, iconSize, null);
            return;
        }
        super.paintIndeterminate(g, c);
    }
}
```

Add the two imports: `org.hkijena.jipipe.JIPipe` and `org.hkijena.jipipe.utils.ThemeUtils`.

Note: reading `ThemeUtils.isReducedMotion()` per paint is cheap (static-field read); a live settings toggle takes effect
on the next repaint of the bar with no extra wiring.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl jipipe-core test -Dtest=JIPipeDesktopModernProgressBarUIReducedMotionTest`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/ui/JIPipeDesktopModernProgressBarUI.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/theme/ui/JIPipeDesktopModernProgressBarUIReducedMotionTest.java
git commit -m "feat(ui): static indeterminate progress bars under reduced motion"
```

---

### Task 7: Stop the remaining decorative timers (splash, search attention, notification heading rotation)

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/app/JIPipeDesktopSplashScreen.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/search/JIPipeDesktopSearchTextField.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/notifications/JIPipeDesktopNotificationButton.java`

**Interfaces:**

- Consumes: `JIPipeGeneralUIApplicationSettings.isReducedMotion()` (Task 1).
- Produces: no new API; the three decorative timers simply do not run when the setting is on. Each is re-checked at its
  trigger point, so a live toggle takes effect at the next trigger (no subscription needed — these are event-driven, not
  persistent animations).
- No unit tests: each change is a single `if` guard on a timer `start()` — pure Swing structure, verified by the user by
  eye (test policy: no tests for trivial guards).

**Design note — splash startup ordering:** `JIPipeGUICommand.run()` shows the splash *before* `JIPipe.ensureInstance()`
completes, so `JIPipe.getInstance()` may be null at that point. Reading the setting there must therefore go through the
**raw settings file**, not `ThemeUtils.isReducedMotion()`. Add a small static helper on
`JIPipeGeneralUIApplicationSettings` (Task 7 owns it):

```java
    /**
     * Reads the reduced-motion flag straight from the settings file, usable before JIPipe is
     * initialized (e.g. on the splash screen). Returns false on any error or missing key.
     */
    public static boolean isReducedMotionFromRaw() {
        try {
            JsonNode node = JIPipeApplicationSettingsServiceComponent.getRawSheetsNode()
                    .path(ID).path("reduced-motion");
            return node.isBoolean() && node.asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
```

(imports: `com.fasterxml.jackson.databind.JsonNode`,
`org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent` — the latter is already used by
`JIPipeExtensionApplicationSettings.getInstanceFromRaw`, the same pattern.)

- [ ] **Step 1: Splash — freeze the "Powered by" icon showcase**

In `JIPipeDesktopSplashScreen.java`, add the `isReducedMotionFromRaw` call. In the constructor, where `iconTimer` is
created (line ~64), guard it:

```java
        if (!JIPipeGeneralUIApplicationSettings.isReducedMotionFromRaw()) {
            Timer iconTimer = new Timer(3000, e -> updateIconShowcase());
            iconTimer.setRepeats(true);
            iconTimer.start();
        }
```

Add the import `org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings`.
(`updateIconShowcase()` still runs once from `addIcon(...)` so a single icon shows; it just no longer *rotates*.)

- [ ] **Step 2: Search field — freeze the attention border animation**

In `JIPipeDesktopSearchTextField.java`, in `grabAttentionAnimation()` (line ~228), when reduced motion is on, jump
straight to the final state instead of animating:

```java
    public void grabAttentionAnimation() {
        isProgrammaticFocusChange = true;
        try {
            if (ThemeUtils.isReducedMotion()) {
                attentionAnimationStatus = 1;
                setBorder(UIUtils.createControlBorder());
            } else {
                attentionAnimationStatus = 0;
                attentionAnimationTimer.restart();
            }
            textField.requestFocusInWindow();
        } finally {
            isProgrammaticFocusChange = false;
        }
    }
```

Add imports `org.hkijena.jipipe.utils.ThemeUtils` (and confirm `UIUtils` is already imported — it is). The timer is
still *created* (unchanged constructor) but never started under reduced motion.

- [ ] **Step 3: Notification button — freeze the heading rotation**

In `JIPipeDesktopNotificationButton.java`, in `updateStatus()` where `timer.start()` is called (line ~100), guard it:

```java
            warningIcon.start();
            if (!JIPipeGeneralUIApplicationSettings.getInstance().isReducedMotion()) {
                timer.start();
            }
```

(Import `org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings` — already imported for the
notification settings? No — it imports `JIPipeNotificationUIApplicationSettings`. Add the General one.) The heading
still shows the first notification; it just stops cycling. `warningIcon.start()` stays unconditional because Task 3
already makes the icon static under reduced motion.

- [ ] **Step 4: Verify compile + run the whole feature test set so far**

Run: `mvn -q -pl jipipe-core test-compile`
Expected: BUILD SUCCESS.
Run:
`mvn -q -pl jipipe-core test -Dtest='JIPipeGeneralUIApplicationSettingsReducedMotionTest,SpinnerIconReducedMotionTest,JIPipeDesktopAnimatedIconReducedMotionTest,ThemeUtilsCaretMotionTest,JIPipeDesktopModernProgressBarUIReducedMotionTest'`
Expected: PASS (15 tests across the five classes).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/app/JIPipeDesktopSplashScreen.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/search/JIPipeDesktopSearchTextField.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/notifications/JIPipeDesktopNotificationButton.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeGeneralUIApplicationSettings.java
git commit -m "feat(ui): freeze splash/search/notification decorative animations under reduced motion"
```

---

### Task 8: Caret blinking in the HTML editor and read-only text panes

**Files:**

- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/markup/JIPipeDesktopHTMLEditor.java`
- Modify:
  `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/markup/JIPipeDesktopMarkdownReader.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/logs/JIPipeDesktopLogViewLogUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopProjectOverviewUI.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/JIPipeDesktopRunNextWindow.java`

**Interfaces:**

- Consumes: `ThemeUtils.watchCaretMotion(JTextComponent)` (Task 4).
- Produces: each of the five `JTextPane`s calls `watchCaretMotion` right after construction. No new API, no tests
  (mechanism already pinned in Task 4; these are one-line wirings).

Rationale: a plain `JTextPane` (editable *or* not, with a plain or HTML kit) uses a blinking `BasicCaret` (verified:
`blinkRate=500`, `focusable=true`), so every one of these surfaces blinks. Task 4's `watchCaretMotion` handles
`BasicCaret` via the `DefaultCaret` branch, so the same one-liner applies to all five.

- [ ] **Step 1: HTML WYSIWYG editor**

In `JIPipeDesktopHTMLEditor.java`, in `initialize(int flags)`, immediately after the `wysiwygEditorPane` is created and
configured (right after the `availableEditorKitActions` loop, before it is added to the layout), add:

```java
        ThemeUtils.watchCaretMotion(wysiwygEditorPane);
```

(`ThemeUtils` is in `org.hkijena.jipipe.utils` — confirm the import; add if missing.) This file is the shared WYSIWYG
used by `JIPipeDesktopHTMLTextParameterEditorUI` and others, so fixing it here covers the parameter editors too.

- [ ] **Step 2: Markdown reader**

In `JIPipeDesktopMarkdownReader.java`, in `initialize(boolean withToolbar)`, immediately after
`content.setEditable(false);`, add:

```java
        ThemeUtils.watchCaretMotion(content);
```

(import `org.hkijena.jipipe.utils.ThemeUtils` if missing.)

- [ ] **Step 3: Log viewer**

In `JIPipeDesktopLogViewLogUI.java`, where `logReader` is first used/configured (after `logReader.setFont(...)` in the
constructor), add:

```java
        ThemeUtils.watchCaretMotion(logReader);
```

(import `ThemeUtils` if missing.)

- [ ] **Step 4: Project overview description**

In `JIPipeDesktopProjectOverviewUI.java`, immediately after `descriptionReader.setEditable(false);`, add:

```java
        ThemeUtils.watchCaretMotion(descriptionReader);
```

(import `ThemeUtils` if missing.)

- [ ] **Step 5: Run-next window description**

In `JIPipeDesktopRunNextWindow.java`, in `addDescriptionPanelToStartingOptionsPanel(...)`, immediately after
`descriptionReader.setEditable(false);`, add:

```java
        ThemeUtils.watchCaretMotion(descriptionReader);
```

(import `ThemeUtils` if missing.)

- [ ] **Step 6: Verify compile**

Run: `mvn -q -pl jipipe-core test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/markup/JIPipeDesktopHTMLEditor.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/markup/JIPipeDesktopMarkdownReader.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/logs/JIPipeDesktopLogViewLogUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopProjectOverviewUI.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/running/JIPipeDesktopRunNextWindow.java
git commit -m "feat(ui): disable caret blinking in HTML editor and read-only text panes"
```

---

### Task 9: Final integration verification (no new code)

**Files:** none (verification only).

**Interfaces:** consumes all prior tasks.

- [ ] **Step 1: Full module test run**

Run: `mvn -q -pl jipipe-core test`
Expected: BUILD SUCCESS. If a pre-existing (unrelated) test fails, report it — do **not** delete or skip tests (see the
ImageJ legacy patcher constraint in Global Constraints).

- [ ] **Step 2: Run the whole new feature test set**

Run:
`mvn -q -pl jipipe-core test -Dtest='JIPipeGeneralUIApplicationSettingsReducedMotionTest,SpinnerIconReducedMotionTest,JIPipeDesktopAnimatedIconReducedMotionTest,ThemeUtilsCaretMotionTest,JIPipeDesktopModernProgressBarUIReducedMotionTest'`
Expected: PASS (15 tests across the five classes).

- [ ] **Step 3: Manual verification checklist for the user**

Run the app. Toggle the setting in **Settings → UI → General** and in **Customize JIPipe**. Confirm, both with the
setting ON and OFF:

| # | Surface                                                                          | ON (reduced)                                            | OFF (normal)             |
|---|----------------------------------------------------------------------------------|---------------------------------------------------------|--------------------------|
| 1 | Code editor + inline script editor                                               | caret solid, no blink                                   | ~500 ms blink            |
| 2 | HTML editor (e.g. node description field) + markdown/log/overview/run-next panes | caret no blink (read-only panes: no caret when clicked) | blink                    |
| 3 | Run queue / status spinners (run queue panel, search field, AI/server status)    | static hourglass, sized to the slot                     | animated spokes          |
| 4 | Indeterminate progress bars (run queue logger, queue menu item)                  | static hourglass in the bar                             | sweeping Metal block     |
| 5 | Notification badge                                                               | solid orange, no pulsing                                | pulsing cross-fade       |
| 6 | Splash screen "Powered by"                                                       | single static icon                                      | rotating icons every 3 s |
| 7 | Search field after adding a node                                                 | static border                                           | green fade-in            |
| 8 | Notification button text (2+ notifications)                                      | static first heading                                    | cycling headings         |
| 9 | Toggling the setting while a run/notification is active                          | resumes animation immediately on OFF                    | stops immediately on ON  |

---

## Motion audit (appendix — what was checked and deliberately left alone)

Full sweep of `jipipe-core/.../desktop` for `new Timer(...)`, `setIndeterminate`, and animation keywords. **Not**
covered (functional data refresh, not decorative motion — left alone per the maintainer's rule "left alone IF they do
not produce motion"; these update content, not animate a visual):

- `JIPipeDesktopAIMonitorWindow` / `JIPipeDesktopServerMonitorWindow` / `...OverviewPage` (5 s data refresh)
- `JIPipeDesktopMemoryStatusUI` (1 s memory value update; the bar is determinate, so no sweeping)
- `JIPipeDesktopBookmarkListPanel` (1 s bookmark reload)
- `JIPipeDesktopProjectSaveManagerButton` (autosave watcher)
- `JIPipeDesktopDataViewerWindow.revalidateDockLater` / `JIPipeDesktopReadonlyCopyableTextField` (one-shot 250/500 ms
  timers)
- `JIPipeDesktopProjectWorkbench` run-update debouncer (250 ms, functional)

Covered (decorative motion): `SpinnerIcon` (Task 2), `JIPipeDesktopAnimatedIcon` (Task 3), code + script editors (Task
4), indeterminate progress bars (Task 6), splash/search/notification decorative timers (Task 7), HTML editor + read-only
panes (Task 8).

If a future surface is found to animate, add it here and to a task — the `ThemeUtils.isReducedMotion()` +
`ChangedEventEmitter` pattern is the standard to follow.

---

## After all tasks (do not commit; run once, report results)

- Full module build + existing suite (guards against regressions in the settings/theme area and the ImageJ patcher
  ordering):
  Run: `mvn -q -pl jipipe-core test`
  Expected: BUILD SUCCESS. If a pre-existing (unrelated) test fails, report it but do not "fix" it by deleting tests
  (see the ImageJ legacy patcher constraint).
- Manual verification checklist for the user — see Task 9, Step 3.
