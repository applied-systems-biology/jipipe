# Bootstrap-Style Grid Layout & Card System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a reusable 12-column grid layout system and card component for Swing, then use them to redesign the statistics screen with responsive card layouts.

**Architecture:** Custom `LayoutManager2` divides container width into 12 columns with configurable gutter and responsive breakpoints. Cards are `JPanel`-based components with header/body/footer and contextual color variants driven by the theme system. The statistics screen is the first consumer.

**Tech Stack:** Java 21, Swing, JFreeChart 1.5.0, Jackson, JUnit Jupiter

## Global Constraints

- Java 21 required
- Maven build: `mvn compile -pl jipipe-core` / `mvn test -pl jipipe-core`
- No new external dependencies
- All new layout code in `org.hkijena.jipipe.desktop.commons.components.layouts`
- All new card code in `org.hkijena.jipipe.desktop.commons.components.cards`
- Follow existing code conventions (copyright headers not required for new files unless matching neighboring files)
- JUnit Jupiter inherited from parent POM — no pom changes needed
- No comments in code unless explicitly requested

---

## File Structure

### New files

**Layout system** (`jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/`):
- `JIPipeDesktopBreakpoint.java` — enum with pixel thresholds
- `JIPipeDesktopColumnConstraints.java` — immutable constraint object (span, offset, breakpoint overrides)
- `JIPipeDesktopColumnLayout.java` — `LayoutManager2` implementation (12-column grid)
- `JIPipeDesktopRow.java` — `JPanel` with max-width + `JIPipeDesktopColumnLayout`
- `JIPipeDesktopFluidRow.java` — `JPanel` filling 100% width + `JIPipeDesktopColumnLayout`

**Card system** (`jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/cards/`):
- `JIPipeDesktopCardVariant.java` — enum of contextual variants
- `JIPipeDesktopCard.java` — card component with header/body/footer + builder

**Test files:**
- `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnLayoutTest.java`
- `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCardTest.java`

### Modified files

**Theme:**
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/JIPipeDesktopModernThemeStyle.java` — add `info` color
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Classic Light.json`
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark.json`
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark Neon.json`
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark High Contrast.json`

**Statistics item API:**
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java`
- All 16 statistics item classes in `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/`

**Statistics UI:**
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java`

**Statistics tests:**
- `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistryTest.java`

---

## Task 1: Add `info` color to theme system

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/JIPipeDesktopModernThemeStyle.java`
- Modify: `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Classic Light.json`
- Modify: `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark.json`
- Modify: `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark Neon.json`
- Modify: `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark High Contrast.json`

**Interfaces:**
- Produces: `JIPipeDesktopModernThemeStyle.getInfoColor()` → `Color` (default `#17A2B8`)

- [ ] **Step 1: Add `infoColor` field to `JIPipeDesktopModernThemeStyle`**

In `JIPipeDesktopModernThemeStyle.java`, add the field after the `warningColor` field (after line 35):

```java
    @JsonProperty("info")
    private Color infoColor = new Color(0x17A2B8);
```

- [ ] **Step 2: Add `infoColor` to the copy constructor**

In the copy constructor `JIPipeDesktopModernThemeStyle(JIPipeDesktopModernThemeStyle other)`, add after `this.warningColor = other.warningColor;` (after line 214):

```java
        this.infoColor = other.infoColor;
```

- [ ] **Step 3: Add getter and setter**

Add after the `getWarningColor()`/`setWarningColor()` pair (after line 431):

```java
    public Color getInfoColor() {
        return infoColor;
    }

    public void setInfoColor(Color infoColor) {
        this.infoColor = infoColor;
    }
```

- [ ] **Step 4: Add `"info"` key to all 4 JSON theme files**

In `JIPipe Classic Light.json`, add after the `"warning"` line:
```json
  "info" : "#17A2B8",
```

In `JIPipe Dark.json`, add after the `"warning"` line:
```json
  "info" : "#17A2B8",
```

In `JIPipe Dark Neon.json`, add after the `"warning"` line:
```json
  "info" : "#17A2B8",
```

In `JIPipe Dark High Contrast.json`, add after the `"warning"` line:
```json
  "info" : "#00CCFF",
```

- [ ] **Step 5: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Add info color to theme system (#1304)"
```

---

## Task 2: Create `JIPipeDesktopBreakpoint` enum

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopBreakpoint.java`

**Interfaces:**
- Produces: `JIPipeDesktopBreakpoint` enum with `XS(0)`, `SM(576)`, `MD(768)`, `LG(992)`, `XL(1200)`
- Produces: `JIPipeDesktopBreakpoint.fromWidth(int width)` → `JIPipeDesktopBreakpoint`

- [ ] **Step 1: Create the enum**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopBreakpoint.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

public enum JIPipeDesktopBreakpoint {
    XS(0),
    SM(576),
    MD(768),
    LG(992),
    XL(1200);

    private final int minWidth;

    JIPipeDesktopBreakpoint(int minWidth) {
        this.minWidth = minWidth;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public static JIPipeDesktopBreakpoint fromWidth(int width) {
        JIPipeDesktopBreakpoint result = XS;
        for (JIPipeDesktopBreakpoint bp : values()) {
            if (width >= bp.minWidth) {
                result = bp;
            }
        }
        return result;
    }
}
```

- [ ] **Step 2: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Add JIPipeDesktopBreakpoint enum (#1304)"
```

---

## Task 3: Create `JIPipeDesktopColumnConstraints`

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnConstraints.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnConstraintsTest.java` (created in Task 4 with layout tests)

**Interfaces:**
- Consumes: `JIPipeDesktopBreakpoint` from Task 2
- Produces: `JIPipeDesktopColumnConstraints` — immutable constraint object
- Produces: `new JIPipeDesktopColumnConstraints(int defaultSpan)`
- Produces: `withSpan(JIPipeDesktopBreakpoint, int)` → `JIPipeDesktopColumnConstraints` (new instance)
- Produces: `withOffset(int)` → `JIPipeDesktopColumnConstraints` (new instance)
- Produces: `withOffset(JIPipeDesktopBreakpoint, int)` → `JIPipeDesktopColumnConstraints` (new instance)
- Produces: `getEffectiveSpan(JIPipeDesktopBreakpoint)` → `int`
- Produces: `getEffectiveOffset(JIPipeDesktopBreakpoint)` → `int`

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnConstraintsTest.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopColumnConstraintsTest {

    @Test
    void defaultSpan() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XL));
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void withSpanOverride() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(3)
                .withSpan(JIPipeDesktopBreakpoint.MD, 6)
                .withSpan(JIPipeDesktopBreakpoint.SM, 12);
        assertEquals(3, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XL));
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.MD));
        assertEquals(12, c.getEffectiveSpan(JIPipeDesktopBreakpoint.SM));
        assertEquals(12, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void spanResolutionWalksDownToFirstOverride() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(3)
                .withSpan(JIPipeDesktopBreakpoint.MD, 6);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.MD));
        assertEquals(3, c.getEffectiveSpan(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void defaultOffset() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6);
        assertEquals(0, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XL));
    }

    @Test
    void withOffset() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6)
                .withOffset(3);
        assertEquals(3, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XL));
        assertEquals(3, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void withOffsetOverride() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6)
                .withOffset(3)
                .withOffset(JIPipeDesktopBreakpoint.SM, 0);
        assertEquals(3, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XL));
        assertEquals(0, c.getEffectiveOffset(JIPipeDesktopBreakpoint.SM));
        assertEquals(0, c.getEffectiveOffset(JIPipeDesktopBreakpoint.XS));
    }

    @Test
    void immutable() {
        JIPipeDesktopColumnConstraints c = new JIPipeDesktopColumnConstraints(6);
        JIPipeDesktopColumnConstraints c2 = c.withSpan(JIPipeDesktopBreakpoint.SM, 12);
        assertNotSame(c, c2);
        assertEquals(6, c.getEffectiveSpan(JIPipeDesktopBreakpoint.SM));
        assertEquals(12, c2.getEffectiveSpan(JIPipeDesktopBreakpoint.SM));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeDesktopColumnConstraintsTest`
Expected: FAIL — class not found

- [ ] **Step 3: Write the implementation**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnConstraints.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

import java.util.HashMap;
import java.util.Map;

public class JIPipeDesktopColumnConstraints {
    private final int defaultSpan;
    private final int defaultOffset;
    private final Map<JIPipeDesktopBreakpoint, Integer> spanOverrides;
    private final Map<JIPipeDesktopBreakpoint, Integer> offsetOverrides;

    public JIPipeDesktopColumnConstraints(int defaultSpan) {
        this(defaultSpan, 0);
    }

    private JIPipeDesktopColumnConstraints(int defaultSpan, int defaultOffset,
                                           Map<JIPipeDesktopBreakpoint, Integer> spanOverrides,
                                           Map<JIPipeDesktopBreakpoint, Integer> offsetOverrides) {
        this.defaultSpan = defaultSpan;
        this.defaultOffset = defaultOffset;
        this.spanOverrides = spanOverrides;
        this.offsetOverrides = offsetOverrides;
    }

    private JIPipeDesktopColumnConstraints(int defaultSpan, int defaultOffset) {
        this(defaultSpan, defaultOffset, new HashMap<>(), new HashMap<>());
    }

    public JIPipeDesktopColumnConstraints withSpan(JIPipeDesktopBreakpoint breakpoint, int span) {
        Map<JIPipeDesktopBreakpoint, Integer> copy = new HashMap<>(spanOverrides);
        copy.put(breakpoint, span);
        return new JIPipeDesktopColumnConstraints(defaultSpan, defaultOffset, copy, offsetOverrides);
    }

    public JIPipeDesktopColumnConstraints withOffset(int offset) {
        return new JIPipeDesktopColumnConstraints(defaultSpan, offset, spanOverrides, offsetOverrides);
    }

    public JIPipeDesktopColumnConstraints withOffset(JIPipeDesktopBreakpoint breakpoint, int offset) {
        Map<JIPipeDesktopBreakpoint, Integer> copy = new HashMap<>(offsetOverrides);
        copy.put(breakpoint, offset);
        return new JIPipeDesktopColumnConstraints(defaultSpan, defaultOffset, spanOverrides, copy);
    }

    public int getEffectiveSpan(JIPipeDesktopBreakpoint breakpoint) {
        JIPipeDesktopBreakpoint[] order = JIPipeDesktopBreakpoint.values();
        for (int i = breakpoint.ordinal(); i >= 0; i--) {
            Integer override = spanOverrides.get(order[i]);
            if (override != null) {
                return override;
            }
        }
        return defaultSpan;
    }

    public int getEffectiveOffset(JIPipeDesktopBreakpoint breakpoint) {
        JIPipeDesktopBreakpoint[] order = JIPipeDesktopBreakpoint.values();
        for (int i = breakpoint.ordinal(); i >= 0; i--) {
            Integer override = offsetOverrides.get(order[i]);
            if (override != null) {
                return override;
            }
        }
        return defaultOffset;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeDesktopColumnConstraintsTest`
Expected: PASS — 7 tests

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add JIPipeDesktopColumnConstraints with breakpoint resolution (#1304)"
```

---

## Task 4: Create `JIPipeDesktopColumnLayout`

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnLayout.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnLayoutTest.java`

**Interfaces:**
- Consumes: `JIPipeDesktopBreakpoint` from Task 2, `JIPipeDesktopColumnConstraints` from Task 3
- Produces: `new JIPipeDesktopColumnLayout(int gutter)` — `LayoutManager2`
- Produces: Accepts `JIPipeDesktopColumnConstraints` as constraint objects via `addLayoutComponent(Component, Object)`

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnLayoutTest.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopColumnLayoutTest {

    private JPanel createContainer(int width, int gutter) {
        JPanel container = new JPanel();
        container.setLayout(new JIPipeDesktopColumnLayout(gutter));
        container.setSize(width, 600);
        container.setPreferredSize(new Dimension(width, 600));
        container.doLayout();
        return container;
    }

    private Component addLabel(JPanel container, JIPipeDesktopColumnConstraints constraints) {
        JLabel label = new JLabel("X");
        label.setPreferredSize(new Dimension(50, 30));
        label.setMinimumSize(new Dimension(50, 30));
        container.add(label, constraints);
        return label;
    }

    @Test
    void singleFullWidthItem() {
        JPanel container = createContainer(1000, 16);
        Component c = addLabel(container, new JIPipeDesktopColumnConstraints(12));
        container.doLayout();
        assertEquals(1000 - 16, c.getWidth());
    }

    @Test
    void twoHalfWidthItems() {
        JPanel container = createContainer(1000, 16);
        Component c1 = addLabel(container, new JIPipeDesktopColumnConstraints(6));
        Component c2 = addLabel(container, new JIPipeDesktopColumnConstraints(6));
        container.doLayout();
        assertTrue(c1.getX() < c2.getX());
        assertEquals(c1.getWidth(), c2.getWidth(), 1);
    }

    @Test
    void wrappingWhenSpanExceeds12() {
        JPanel container = createContainer(1000, 16);
        Component c1 = addLabel(container, new JIPipeDesktopColumnConstraints(8));
        Component c2 = addLabel(container, new JIPipeDesktopColumnConstraints(8));
        container.doLayout();
        assertTrue(c2.getY() > c1.getY(), "Second item should wrap to next row");
    }

    @Test
    void offsetPushesItemRight() {
        JPanel container = createContainer(1000, 16);
        Component c1 = addLabel(container, new JIPipeDesktopColumnConstraints(6).withOffset(3));
        container.doLayout();
        assertTrue(c1.getX() > 100, "Offset item should start further right");
    }

    @Test
    void breakpointChangesSpan() {
        JPanel container = createContainer(500, 16);
        Component c = addLabel(container, new JIPipeDesktopColumnConstraints(3)
                .withSpan(JIPipeDesktopBreakpoint.SM, 12));
        container.doLayout();
        assertEquals(500 - 16, c.getWidth(), "At 500px width (SM breakpoint), span should be 12");
    }

    @Test
    void defaultConstraintsWhenNoneProvided() {
        JPanel container = createContainer(1000, 16);
        JLabel label = new JLabel("X");
        label.setPreferredSize(new Dimension(50, 30));
        container.add(label);
        container.doLayout();
        assertEquals(1000 - 16, label.getWidth(), "Default should be full width (12 columns)");
    }

    @Test
    void preferredLayoutSizeComputesHeight() {
        JPanel container = new JPanel();
        container.setLayout(new JIPipeDesktopColumnLayout(16));
        container.setSize(1000, 600);
        addLabel(container, new JIPipeDesktopColumnConstraints(6));
        addLabel(container, new JIPipeDesktopColumnConstraints(6));
        addLabel(container, new JIPipeDesktopColumnConstraints(12));
        Dimension pref = container.getLayout().preferredLayoutSize(container);
        assertTrue(pref.height > 0, "Preferred height should be positive");
        assertTrue(pref.width > 0, "Preferred width should be positive");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeDesktopColumnLayoutTest`
Expected: FAIL — class not found

- [ ] **Step 3: Write the implementation**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopColumnLayout.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JIPipeDesktopColumnLayout implements LayoutManager2 {

    private static final int COLUMNS = 12;

    private final int gutter;
    private final Map<Component, JIPipeDesktopColumnConstraints> constraintsMap = new LinkedHashMap<>();

    public JIPipeDesktopColumnLayout(int gutter) {
        this.gutter = gutter;
    }

    public int getGutter() {
        return gutter;
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof JIPipeDesktopColumnConstraints c) {
            constraintsMap.put(comp, c);
        } else if (constraints == null) {
            constraintsMap.put(comp, new JIPipeDesktopColumnConstraints(COLUMNS));
        } else {
            throw new IllegalArgumentException("Invalid constraints: " + constraints);
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            if (width == 0) {
                Container parent = target.getParent();
                while (parent != null && parent.getWidth() == 0) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    width = parent.getWidth();
                }
            }
            if (width == 0) {
                width = 1024;
            }

            Insets insets = target.getInsets();
            int innerWidth = width - insets.left - insets.right;
            JIPipeDesktopBreakpoint bp = JIPipeDesktopBreakpoint.fromWidth(innerWidth);
            int columnUnit = Math.max(0, (innerWidth - COLUMNS * gutter) / COLUMNS);

            int totalHeight = 0;
            int currentCol = 0;
            int rowHeight = 0;

            for (Map.Entry<Component, JIPipeDesktopColumnConstraints> entry : constraintsMap.entrySet()) {
                Component comp = entry.getKey();
                if (!comp.isVisible()) continue;
                JIPipeDesktopColumnConstraints cons = entry.getValue();
                int span = Math.min(COLUMNS, cons.getEffectiveSpan(bp));
                int offset = cons.getEffectiveOffset(bp);

                if (currentCol + offset + span > COLUMNS) {
                    totalHeight += rowHeight + gutter;
                    currentCol = 0;
                    rowHeight = 0;
                }

                currentCol += offset;

                int compWidth = span * columnUnit + (span - 1) * gutter;
                Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                if (d.width > 0 && d.height > 0) {
                    int compHeight = d.height;
                    if (compWidth > 0) {
                        compHeight = Math.max(compHeight, comp.getMinimumSize().height);
                    }
                    rowHeight = Math.max(rowHeight, compHeight);
                }

                currentCol += span;
            }
            totalHeight += rowHeight;

            return new Dimension(width, totalHeight + insets.top + insets.bottom);
        }
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int innerWidth = target.getWidth() - insets.left - insets.right;
            JIPipeDesktopBreakpoint bp = JIPipeDesktopBreakpoint.fromWidth(innerWidth);
            int columnUnit = Math.max(0, (innerWidth - COLUMNS * gutter) / COLUMNS);

            int startX = insets.left;
            int startY = insets.top;
            int currentCol = 0;
            int rowHeight = 0;

            for (Component comp : target.getComponents()) {
                if (!comp.isVisible()) {
                    continue;
                }
                JIPipeDesktopColumnConstraints cons = constraintsMap.getOrDefault(comp,
                        new JIPipeDesktopColumnConstraints(COLUMNS));
                int span = Math.min(COLUMNS, cons.getEffectiveSpan(bp));
                int offset = cons.getEffectiveOffset(bp);

                if (currentCol + offset + span > COLUMNS) {
                    startY += rowHeight + gutter;
                    currentCol = 0;
                    rowHeight = 0;
                }

                currentCol += offset;

                int compWidth = span * columnUnit + (span - 1) * gutter;
                int compX = startX + currentCol * (columnUnit + gutter);
                Dimension pref = comp.getPreferredSize();
                int compHeight = pref.height;
                rowHeight = Math.max(rowHeight, compHeight);

                comp.setBounds(compX, startY, compWidth, compHeight);

                currentCol += span;
                if (currentCol >= COLUMNS) {
                    startY += rowHeight + gutter;
                    currentCol = 0;
                    rowHeight = 0;
                }
            }
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        constraintsMap.put(comp, new JIPipeDesktopColumnConstraints(COLUMNS));
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        constraintsMap.remove(comp);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0f;
    }

    @Override
    public void invalidateLayout(Container target) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeDesktopColumnLayoutTest`
Expected: PASS — 7 tests

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add JIPipeDesktopColumnLayout 12-column grid LayoutManager2 (#1304)"
```

---

## Task 5: Create `JIPipeDesktopRow` and `JIPipeDesktopFluidRow`

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopRow.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopFluidRow.java`

**Interfaces:**
- Consumes: `JIPipeDesktopColumnLayout` from Task 4
- Produces: `new JIPipeDesktopRow()` / `new JIPipeDesktopRow(int gutter)` — `JPanel` with max-width 1140px
- Produces: `new JIPipeDesktopFluidRow()` / `new JIPipeDesktopFluidRow(int gutter)` — `JPanel` filling 100% width

- [ ] **Step 1: Create `JIPipeDesktopRow`**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopRow.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopRow extends JPanel {
    private int maxWidth = 1140;

    public JIPipeDesktopRow() {
        this(16);
    }

    public JIPipeDesktopRow(int gutter) {
        setLayout(new JIPipeDesktopColumnLayout(gutter));
        setOpaque(false);
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(maxWidth, Integer.MAX_VALUE);
    }
}
```

- [ ] **Step 2: Create `JIPipeDesktopFluidRow`**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/JIPipeDesktopFluidRow.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.layouts;

import javax.swing.*;

public class JIPipeDesktopFluidRow extends JPanel {
    public JIPipeDesktopFluidRow() {
        this(16);
    }

    public JIPipeDesktopFluidRow(int gutter) {
        setLayout(new JIPipeDesktopColumnLayout(gutter));
        setOpaque(false);
    }
}
```

- [ ] **Step 3: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Add JIPipeDesktopRow and JIPipeDesktopFluidRow containers (#1304)"
```

---

## Task 6: Create `JIPipeDesktopCardVariant` enum

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCardVariant.java`

**Interfaces:**
- Consumes: `JIPipeDesktopModernThemeStyle` from Task 1
- Produces: `JIPipeDesktopCardVariant` enum: `Default`, `Primary`, `Secondary`, `Success`, `Warning`, `Danger`, `Info`
- Produces: `resolveColor(JIPipeDesktopModernThemeStyle)` → `Color` (null for Default)

- [ ] **Step 1: Create the enum**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCardVariant.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.cards;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;

import java.awt.*;

public enum JIPipeDesktopCardVariant {
    Default,
    Primary,
    Secondary,
    Success,
    Warning,
    Danger,
    Info;

    public Color resolveColor(JIPipeDesktopModernThemeStyle style) {
        return switch (this) {
            case Default -> null;
            case Primary -> style.getPrimaryColor();
            case Secondary -> style.getSecondaryColor();
            case Success -> style.getSuccessColor();
            case Warning -> style.getWarningColor();
            case Danger -> style.getDangerColor();
            case Info -> style.getInfoColor();
        };
    }
}
```

- [ ] **Step 2: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Add JIPipeDesktopCardVariant enum (#1304)"
```

---

## Task 7: Create `JIPipeDesktopCard` component

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCard.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCardTest.java`

**Interfaces:**
- Consumes: `JIPipeDesktopCardVariant` from Task 6, `RoundedLineBorder`, `UIUtils`, `ThemeUtils`, `ColorUtils`
- Produces: `new JIPipeDesktopCard()` / `new JIPipeDesktopCard(String title)` / `new JIPipeDesktopCard(String title, Icon icon)`
- Produces: `setTitle(String)`, `setIcon(Icon)`, `setBody(JComponent)`, `setVariant(JIPipeDesktopCardVariant)`
- Produces: `addHeaderAction(JButton)`, `addFooterAction(JButton)`
- Produces: `JIPipeDesktopCard.builder(String title)` → `Builder`

- [ ] **Step 1: Write the failing test**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCardTest.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.cards;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopCardTest {

    @Test
    void createWithTitleAndIcon() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test Card", UIManager.getIcon("Tree.leafIcon"));
        assertEquals("Test Card", card.getTitle());
        assertNotNull(card.getIcon());
    }

    @Test
    void setBody() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        JLabel body = new JLabel("Body content");
        card.setBody(body);
        assertSame(body, card.getBody());
    }

    @Test
    void setVariant() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        card.setVariant(JIPipeDesktopCardVariant.Primary);
        assertEquals(JIPipeDesktopCardVariant.Primary, card.getVariant());
    }

    @Test
    void addHeaderAction() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        JButton button = new JButton("Action");
        card.addHeaderAction(button);
        assertEquals(1, card.getHeaderActionCount());
    }

    @Test
    void addFooterAction() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Test");
        JButton button = new JButton("Action");
        card.addFooterAction(button);
        assertEquals(1, card.getFooterActionCount());
    }

    @Test
    void builderPattern() {
        JButton footerBtn = new JButton("OK");
        JIPipeDesktopCard card = JIPipeDesktopCard.builder("Built Card")
                .icon(UIManager.getIcon("Tree.leafIcon"))
                .variant(JIPipeDesktopCardVariant.Success)
                .body(new JLabel("Body"))
                .footerButton(footerBtn)
                .build();
        assertEquals("Built Card", card.getTitle());
        assertEquals(JIPipeDesktopCardVariant.Success, card.getVariant());
        assertNotNull(card.getBody());
        assertEquals(1, card.getFooterActionCount());
    }

    @Test
    void noHeaderWhenTitleAndIconNull() {
        JIPipeDesktopCard card = new JIPipeDesktopCard();
        card.setBody(new JLabel("Body"));
        assertFalse(card.hasHeader());
    }

    @Test
    void hasHeaderWhenTitleSet() {
        JIPipeDesktopCard card = new JIPipeDesktopCard("Title");
        assertTrue(card.hasHeader());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeDesktopCardTest`
Expected: FAIL — class not found

- [ ] **Step 3: Write the implementation**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/cards/JIPipeDesktopCard.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.cards;

import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopCard extends JPanel {
    private String title;
    private Icon icon;
    private JComponent body;
    private JIPipeDesktopCardVariant variant = JIPipeDesktopCardVariant.Default;
    private final JToolBar headerToolBar = new JToolBar();
    private final JLabel titleLabel = new JLabel();
    private final JToolBar footerToolBar = new JToolBar();
    private boolean headerVisible = false;
    private boolean footerVisible = false;

    public JIPipeDesktopCard() {
        this(null, null);
    }

    public JIPipeDesktopCard(String title) {
        this(title, null);
    }

    public JIPipeDesktopCard(String title, Icon icon) {
        this.title = title;
        this.icon = icon;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UIManager.getColor("Panel.background"));
        updateBorder();
        initializeHeader();
        initializeFooter();
        updateHeader();
    }

    private void updateBorder() {
        int radius = ThemeUtils.getCurrentStyle().getIslandsCornerRadius();
        setBorder(new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, radius));
    }

    private void initializeHeader() {
        headerToolBar.setFloatable(false);
        headerToolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getControlBorderColor()));
        headerToolBar.add(titleLabel);
        headerToolBar.add(Box.createHorizontalGlue());
    }

    private void initializeFooter() {
        footerToolBar.setFloatable(false);
        footerToolBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.getControlBorderColor()));
        footerToolBar.add(Box.createHorizontalGlue());
    }

    private void updateHeader() {
        boolean hasTitleOrIcon = title != null || icon != null;
        boolean hasActions = headerToolBar.getComponentCount() > 2;
        headerVisible = hasTitleOrIcon || hasActions;

        if (headerVisible) {
            titleLabel.setText(title != null ? title : "");
            titleLabel.setIcon(icon);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD,
                    ThemeUtils.getCurrentStyle().getFontSizeLarge()));
            titleLabel.setBorder(UIUtils.createEmptyBorder(8));

            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            Color variantColor = variant.resolveColor(style);
            if (variantColor != null) {
                headerToolBar.setBackground(ColorUtils.mix(variantColor,
                        UIManager.getColor("Panel.background"), 0.92));
            } else {
                headerToolBar.setBackground(UIManager.getColor("Panel.background"));
            }

            add(headerToolBar, BorderLayout.NORTH);
        } else {
            remove(headerToolBar);
        }
        revalidate();
        repaint();
    }

    private void updateFooter() {
        footerVisible = footerToolBar.getComponentCount() > 1;
        if (footerVisible) {
            add(footerToolBar, BorderLayout.SOUTH);
        } else {
            remove(footerToolBar);
        }
        revalidate();
        repaint();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        updateHeader();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        updateHeader();
    }

    public JComponent getBody() {
        return body;
    }

    public void setBody(JComponent body) {
        if (this.body != null) {
            remove(this.body);
        }
        this.body = body;
        if (body != null) {
            add(body, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public JIPipeDesktopCardVariant getVariant() {
        return variant;
    }

    public void setVariant(JIPipeDesktopCardVariant variant) {
        this.variant = variant;
        updateHeader();
    }

    public void addHeaderAction(JButton button) {
        headerToolBar.add(button);
        updateHeader();
    }

    public void removeHeaderAction(JButton button) {
        headerToolBar.remove(button);
        updateHeader();
    }

    public int getHeaderActionCount() {
        return Math.max(0, headerToolBar.getComponentCount() - 2);
    }

    public void addFooterAction(JButton button) {
        footerToolBar.add(button);
        updateFooter();
    }

    public void removeFooterAction(JButton button) {
        footerToolBar.remove(button);
        updateFooter();
    }

    public int getFooterActionCount() {
        return Math.max(0, footerToolBar.getComponentCount() - 1);
    }

    public boolean hasHeader() {
        return headerVisible;
    }

    public boolean hasFooter() {
        return footerVisible;
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static class Builder {
        private final JIPipeDesktopCard card;

        public Builder(String title) {
            card = new JIPipeDesktopCard(title);
        }

        public Builder icon(Icon icon) {
            card.setIcon(icon);
            return this;
        }

        public Builder variant(JIPipeDesktopCardVariant variant) {
            card.setVariant(variant);
            return this;
        }

        public Builder body(JComponent body) {
            card.setBody(body);
            return this;
        }

        public Builder headerButton(JButton button) {
            card.addHeaderAction(button);
            return this;
        }

        public Builder footerButton(JButton button) {
            card.addFooterAction(button);
            return this;
        }

        public JIPipeDesktopCard build() {
            return card;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeDesktopCardTest`
Expected: PASS — 8 tests

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Add JIPipeDesktopCard component with header/body/footer and builder (#1304)"
```

---

## Task 8: Add layout defaults to `JIPipeStatisticsItem`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java`

**Interfaces:**
- Consumes: `JIPipeDesktopCardVariant` from Task 6
- Produces: `JIPipeStatisticsItem.getIcon32()` → `String` (default `"status/starred.png"`)
- Produces: `JIPipeStatisticsItem.getCardVariant()` → `JIPipeDesktopCardVariant` (default `Default`)
- Produces: `JIPipeStatisticsItem.getDefaultColumnSpan()` → `int` (default `3`)

- [ ] **Step 1: Add default methods to the interface**

In `JIPipeStatisticsItem.java`, add the import for `JIPipeDesktopCardVariant` and three new default methods at the end of the interface (before the closing brace, after `isTimeTracked()`):

```java
    default String getIcon32() {
        return "status/starred.png";
    }

    default org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant getCardVariant() {
        return org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant.Default;
    }

    default int getDefaultColumnSpan() {
        return 3;
    }
```

Note: Using fully-qualified name for `JIPipeDesktopCardVariant` to avoid adding an import to the interface file, keeping the diff minimal. If the implementer prefers to add an import statement, that's fine too.

- [ ] **Step 2: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Add layout defaults to JIPipeStatisticsItem interface (#1304)"
```

---

## Task 9: Override layout defaults in all 16 statistics items

**Files:**
- Modify all 16 files in `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/`:
  - `MachineIdStatisticsItem.java`
  - `JIPipeVersionStatisticsItem.java`
  - `OperatingSystemStatisticsItem.java`
  - `AccelerationStatisticsItem.java`
  - `GpuModelStatisticsItem.java`
  - `GpuVramStatisticsItem.java`
  - `TotalRamStatisticsItem.java`
  - `RecentProjectsCountStatisticsItem.java`
  - `WorkflowRunsStatisticsItem.java`
  - `RoCratesCreatedStatisticsItem.java`
  - `LargestProjectNodesStatisticsItem.java`
  - `LargestProjectCompartmentsStatisticsItem.java`
  - `PopularNodesStatisticsItem.java`
  - `NodeMoveDistanceStatisticsItem.java`
  - `LongestNodeWidthStatisticsItem.java`
  - `NoodleScoreStatisticsItem.java`
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistryTest.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsItem.getIcon32()`, `getCardVariant()`, `getDefaultColumnSpan()` from Task 8

**Overrides table (from the design spec):**

| Class | Icon | Variant | Span |
|-------|------|---------|------|
| `MachineIdStatisticsItem` | `status/dialog-password.png` | `Default` | 3 |
| `JIPipeVersionStatisticsItem` | `actions/help-about.png` | `Default` | 3 |
| `OperatingSystemStatisticsItem` | `actions/computer.png` | `Default` | 3 |
| `AccelerationStatisticsItem` | `actions/gears.png` | `Primary` | 3 |
| `GpuModelStatisticsItem` | `actions/display.png` | `Info` | 3 |
| `GpuVramStatisticsItem` | `actions/memory.png` | `Info` | 3 |
| `TotalRamStatisticsItem` | `actions/memory.png` | `Info` | 3 |
| `RecentProjectsCountStatisticsItem` | `actions/document-open-folder.png` | `Secondary` | 3 |
| `WorkflowRunsStatisticsItem` | `actions/debug-run.png` | `Success` | 6 |
| `RoCratesCreatedStatisticsItem` | `actions/document-export.png` | `Warning` | 6 |
| `LargestProjectNodesStatisticsItem` | `actions/network-server-database.png` | `Primary` | 6 |
| `LargestProjectCompartmentsStatisticsItem` | `actions/window.png` | `Primary` | 6 |
| `PopularNodesStatisticsItem` | `actions/starred.png` | `Secondary` | 12 |
| `NodeMoveDistanceStatisticsItem` | `actions/transform-move.png` | `Default` | 3 |
| `LongestNodeWidthStatisticsItem` | `actions/resizecol.png` | `Default` | 3 |
| `NoodleScoreStatisticsItem` | `actions/bezier-curve.png` | `Default` | 6 |

- [ ] **Step 1: Add overrides to each of the 16 item classes**

For each item class, add the import and override methods. Example for `WorkflowRunsStatisticsItem` (add after the existing `isTimeTracked()` override):

```java
    @Override
    public String getIcon32() { return "actions/debug-run.png"; }
    @Override
    public org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant getCardVariant() {
        return org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant.Success;
    }
    @Override
    public int getDefaultColumnSpan() { return 6; }
```

Apply the same pattern to all 16 classes using the overrides table above. For classes where variant is `Default` and span is `3`, they inherit the defaults — but still override `getIcon32()`.

Classes that need ONLY `getIcon32()` override (variant=Default, span=3):
- `MachineIdStatisticsItem` → `"status/dialog-password.png"`
- `JIPipeVersionStatisticsItem` → `"actions/help-about.png"`
- `OperatingSystemStatisticsItem` → `"actions/computer.png"`
- `NodeMoveDistanceStatisticsItem` → `"actions/transform-move.png"`
- `LongestNodeWidthStatisticsItem` → `"actions/resizecol.png"`

Classes that need `getIcon32()` + `getCardVariant()` (span=3):
- `AccelerationStatisticsItem` → `"actions/gears.png"`, `Primary`
- `GpuModelStatisticsItem` → `"actions/display.png"`, `Info`
- `GpuVramStatisticsItem` → `"actions/memory.png"`, `Info`
- `TotalRamStatisticsItem` → `"actions/memory.png"`, `Info`
- `RecentProjectsCountStatisticsItem` → `"actions/document-open-folder.png"`, `Secondary`

Classes that need `getIcon32()` + `getCardVariant()` + `getDefaultColumnSpan()` (span=6):
- `WorkflowRunsStatisticsItem` → `"actions/debug-run.png"`, `Success`, 6
- `RoCratesCreatedStatisticsItem` → `"actions/document-export.png"`, `Warning`, 6
- `LargestProjectNodesStatisticsItem` → `"actions/network-server-database.png"`, `Primary`, 6
- `LargestProjectCompartmentsStatisticsItem` → `"actions/window.png"`, `Primary`, 6
- `NoodleScoreStatisticsItem` → `"actions/bezier-curve.png"`, `Default`, 6

Classes that need all three (span=12):
- `PopularNodesStatisticsItem` → `"actions/starred.png"`, `Secondary`, 12

- [ ] **Step 2: Add test for item defaults to `JIPipeStatisticsRegistryTest`**

Add this test method to `JIPipeStatisticsRegistryTest.java`:

```java
    @Test
    void itemsHaveValidIconAndVariant() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.MachineIdStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.JIPipeVersionStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.OperatingSystemStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.AccelerationStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.GpuModelStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.GpuVramStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.TotalRamStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RecentProjectsCountStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.WorkflowRunsStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RoCratesCreatedStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectNodesStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectCompartmentsStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.PopularNodesStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NodeMoveDistanceStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LongestNodeWidthStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NoodleScoreStatisticsItem());

        for (JIPipeStatisticsItem item : registry.getItems()) {
            assertNotNull(item.getIcon32(), "Icon must not be null for " + item.getId());
            assertFalse(item.getIcon32().isEmpty(), "Icon must not be empty for " + item.getId());
            assertNotNull(item.getCardVariant(), "Variant must not be null for " + item.getId());
            assertTrue(item.getDefaultColumnSpan() > 0 && item.getDefaultColumnSpan() <= 12,
                    "Span must be 1-12 for " + item.getId());
        }
    }
```

- [ ] **Step 3: Compile and run tests**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeStatisticsRegistryTest`
Expected: PASS — 4 tests (3 existing + 1 new)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Override layout defaults in all 16 statistics items (#1304)"
```

---

## Task 10: Rewrite `JIPipeDesktopStatisticsUI` using new grid + card system

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java`

**Interfaces:**
- Consumes: `JIPipeDesktopFluidRow`, `JIPipeDesktopColumnConstraints`, `JIPipeDesktopBreakpoint` from Tasks 2-5
- Consumes: `JIPipeDesktopCard`, `JIPipeDesktopCardVariant` from Tasks 6-7
- Consumes: `JIPipeStatisticsItem.getIcon32()`, `getCardVariant()`, `getDefaultColumnSpan()` from Tasks 8-9

- [ ] **Step 1: Rewrite the statistics UI**

Replace the entire contents of `JIPipeDesktopStatisticsUI.java` with:

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCard;
import org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopBreakpoint;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopColumnConstraints;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopFluidRow;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import com.fasterxml.jackson.databind.JsonNode;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

public class JIPipeDesktopStatisticsUI extends JIPipeDesktopProjectWorkbenchPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private JPanel headerPanel;
    private JIPipeDesktopFluidRow cardsRow;
    private JTextField machineIdField;

    public JIPipeDesktopStatisticsUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        refresh();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeHeaderPanel();
        add(createCenterPanel(), BorderLayout.CENTER);
    }

    private void initializeHeaderPanel() {
        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();

        headerPanel = new JPanel();
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 16, 0),
                BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor())));
        headerPanel.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 150));

        JIPipeDesktopFormPanel nameAndIdPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        nameAndIdPanel.setLayout(new BoxLayout(nameAndIdPanel, BoxLayout.Y_AXIS));

        JTextField titleField = UIUtils.createReadonlyBorderlessTextField("Statistics");
        titleField.setOpaque(false);
        titleField.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeHuge()));
        titleField.setBorder(UIUtils.createEmptyBorder(4));
        nameAndIdPanel.addWideToForm(UIUtils.makeNonOpaque(UIUtils.boxHorizontal(titleField)), null);

        machineIdField = UIUtils.createReadonlyBorderlessTextField("Machine ID " + StringUtils.nullToEmpty(service.getMachineId()));
        machineIdField.setOpaque(false);
        machineIdField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        nameAndIdPanel.addWideToForm(UIUtils.makeNonOpaque(UIUtils.boxHorizontal(machineIdField,
                UIUtils.makeButtonTransparent(UIUtils.createButton("", JIPipe.RESOURCES.getIcon16("actions/random.png"), () -> {
                    service.rerollMachineId();
                    machineIdField.setText("Machine ID " + StringUtils.nullToEmpty(service.getMachineId()));
                    refresh();
                })))), null);

        LocalDateTime idCreated = service.getMachineIdCreatedTimestamp();
        JLabel idDateLabel = new JLabel("ID created: " + (idCreated != null ? idCreated.format(FORMATTER) : "Unknown"));
        idDateLabel.setFont(idDateLabel.getFont().deriveFont(Font.ITALIC, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        idDateLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        idDateLabel.setBorder(UIUtils.createEmptyBorder(4));
        nameAndIdPanel.addWideToForm(UIUtils.makeNonOpaque(idDateLabel), null);

        nameAndIdPanel.addVerticalGlue();
        headerPanel.add(nameAndIdPanel, BorderLayout.WEST);

        JIPipeDesktopFormPanel technicalInfo = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);

        LocalDateTime firstLaunch = service.getFirstLaunchTimestamp();
        JTextField firstLaunchField = UIUtils.createReadonlyBorderlessTextField(firstLaunch != null ? firstLaunch.format(FORMATTER) : "Unknown");
        technicalInfo.addToForm(firstLaunchField, new JLabel("First launch"), null);

        LocalDateTime lastSent = service.getLastSentTimestamp();
        JTextField lastSentField = UIUtils.createReadonlyBorderlessTextField(lastSent != null ? lastSent.format(FORMATTER) : "Never");
        technicalInfo.addToForm(lastSentField, new JLabel("Last sent"), null);

        JTextField privacyField = UIUtils.createReadonlyBorderlessTextField(settings.getPrivacyLevel().toString());
        technicalInfo.addToForm(privacyField, new JLabel("Privacy level"), null);

        technicalInfo.addVerticalGlue();
        headerPanel.add(technicalInfo, BorderLayout.EAST);

        initializeToolbar(headerPanel);
        add(headerPanel, BorderLayout.NORTH);
    }

    private void initializeToolbar(JPanel topPanel) {
        JPanel toolBar = new JPanel();
        toolBar.setBorder(UIUtils.createEmptyBorder(4));
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton reloadButton = new JButton("Reload", JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"));
        reloadButton.addActionListener(e -> refresh());
        reloadButton.setOpaque(false);
        reloadButton.setBackground(new Color(0, 0, 0, 0));
        reloadButton.setToolTipText("Updates the contents of this page.");
        toolBar.add(reloadButton);

        JButton configureButton = new JButton("Configure", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        configureButton.addActionListener(e -> {
            JIPipeDesktopStatisticsConfigurationUI dialog = new JIPipeDesktopStatisticsConfigurationUI(getDesktopProjectWorkbench().getWindow());
            dialog.setVisible(true);
            if (dialog.isConfigured()) {
                refresh();
            }
        });
        configureButton.setOpaque(false);
        configureButton.setBackground(new Color(0, 0, 0, 0));
        configureButton.setToolTipText("Configure statistics privacy settings.");
        toolBar.add(configureButton);

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

    private JComponent createCenterPanel() {
        cardsRow = new JIPipeDesktopFluidRow(16);
        cardsRow.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane scrollPane = new JScrollPane(cardsRow);
        scrollPane.setOpaque(false);
        scrollPane.setMinimumSize(new Dimension(300, 300));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        wrapper.add(UIUtils.wrapInIslandPanelIfNeeded(scrollPane), BorderLayout.CENTER);

        return wrapper;
    }

    public void refresh() {
        remove(headerPanel);
        initializeHeaderPanel();

        cardsRow.removeAll();

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        for (JIPipeStatisticsItem item : service.getRegistry().getItems()) {
            JIPipeDesktopCard card = createCard(item, service);
            int span = item.getDefaultColumnSpan();
            JIPipeDesktopColumnConstraints constraints = new JIPipeDesktopColumnConstraints(span)
                    .withSpan(JIPipeDesktopBreakpoint.MD, Math.min(span * 2, 12))
                    .withSpan(JIPipeDesktopBreakpoint.SM, 12);
            cardsRow.add(card, constraints);
        }

        cardsRow.revalidate();
        cardsRow.repaint();
        revalidate();
        repaint();
    }

    private JIPipeDesktopCard createCard(JIPipeStatisticsItem item, JIPipeStatisticsServiceComponent service) {
        JIPipeDesktopCard card = new JIPipeDesktopCard(item.getName(),
                JIPipe.RESOURCES.getIcon32(item.getIcon32()));
        card.setVariant(item.getCardVariant());

        JsonNode serialized = item.serialize();
        String valueStr = formatValue(serialized);
        card.setBody(UIUtils.createReadonlyBorderlessTextArea(valueStr));

        if (item.isTimeTracked()) {
            ChartPanel chartPanel = createHistoryChart(item, service);
            if (chartPanel != null) {
                chartPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
                JPanel bodyPanel = new JPanel(new BorderLayout());
                bodyPanel.setOpaque(false);
                bodyPanel.add(UIUtils.createReadonlyBorderlessTextArea(valueStr), BorderLayout.NORTH);
                bodyPanel.add(chartPanel, BorderLayout.CENTER);
                card.setBody(bodyPanel);
            }
        }

        return card;
    }

    private String formatValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "N/A";
        }
        if (node.isNumber()) {
            return node.toString();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isObject()) {
            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (sb.length() > 0) sb.append("\n");
                sb.append(entry.getKey()).append(": ").append(entry.getValue().toString());
            }
            return sb.toString();
        }
        if (node.isArray()) {
            return node.size() + " entries";
        }
        return node.toString();
    }

    private ChartPanel createHistoryChart(JIPipeStatisticsItem item, JIPipeStatisticsServiceComponent service) {
        JsonNode historyNode = service.getHistory();
        if (historyNode == null) {
            return null;
        }
        JsonNode history = historyNode.get(item.getId());
        if (history == null || !history.isArray() || history.isEmpty()) {
            return null;
        }

        XYSeries series = new XYSeries(item.getName());
        boolean hasNumeric = false;
        for (int i = 0; i < history.size(); i++) {
            JsonNode point = history.get(i);
            JsonNode valueNode = point.get("value");
            if (valueNode != null && valueNode.isNumber()) {
                series.add(i, valueNode.asDouble());
                hasNumeric = true;
            }
        }
        if (!hasNumeric) {
            return null;
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
        chart.removeLegend();
        chart.setBackgroundPaint(UIManager.getColor("Panel.background"));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(UIManager.getColor("Panel.background"));
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setVisible(false);
        plot.setInsets(new org.jfree.chart.ui.RectangleInsets(2, 2, 2, 2));

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultStroke(new BasicStroke(2f));
        renderer.setSeriesPaint(0, ThemeUtils.getCurrentStyle().getPrimaryColor());
        renderer.setDefaultShapesVisible(false);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(260, 80));
        panel.setMinimumDrawWidth(0);
        panel.setMaximumDrawWidth(Integer.MAX_VALUE);
        panel.setMinimumDrawHeight(0);
        panel.setMaximumDrawHeight(Integer.MAX_VALUE);
        return panel;
    }
}
```

- [ ] **Step 2: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Run all tests**

Run: `mvn test -pl jipipe-core`
Expected: All tests pass (145+ existing + new layout/card tests)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Rewrite statistics UI using grid layout and card components (#1304)"
```
