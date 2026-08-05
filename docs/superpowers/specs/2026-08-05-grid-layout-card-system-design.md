# Bootstrap-Style Grid Layout & Card System for JIPipe Desktop

**Date:** 2026-08-05  
**Branch:** `1304-user-statistics`  
**Status:** Design approved, ready for implementation planning

## Overview

Implement a reusable Bootstrap-inspired grid layout system and card component for Swing, then use them to redesign the statistics screen. The grid system supports 12 columns, configurable gutters, responsive breakpoints, column offsets, row nesting, and a fixed/fluid row distinction. The card component supports header/body/footer with contextual color variants driven by the theme system.

The statistics screen serves as the implementation leader — the first consumer of the new layout system — but both systems are designed as general-purpose reusable components.

## Background

JIPipe's desktop UI currently uses `BoxLayout`, `BorderLayout`, `GridBagLayout`, and two custom flow layouts (`JIPipeDesktopFlowLayout`, `JIPipeDesktopWrapLayout`). Card-like UIs are built inline with `RoundedLineBorder` + `BorderLayout` each time. There is no grid/column layout and no reusable card component. This makes responsive card grids (like the statistics screen) difficult to build and maintain.

The theme system (`JIPipeDesktopModernThemeStyle`) already defines `primary`, `secondary`, `success`, `danger`, and `warning` colors. An `info` color is missing and needs to be added for a complete Bootstrap contextual palette.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Responsive trigger | Spans + optional per-breakpoint overrides | Full Bootstrap semantics; items can specify different spans at different widths |
| Card completeness | Header + body + footer | Covers existing tips/statistics/section card patterns with a clean API |
| Row vs fluid row | Row (max-width, centered) + FluidRow (fill 100%) | Matches Bootstrap's `.container`/`.container-fluid` distinction |
| Gutter | Configurable per-row | Different contexts need different spacing |
| Nesting | Rows inside columns inside rows | Enables complex layouts |
| Column offset | Supported | Alignment flexibility |
| Card visual styles | Contextual color variants (Primary, Secondary, Success, Warning, Danger, Info, Default) | Colors driven by theme system |
| Implementation | Custom `LayoutManager2` | Full control over Bootstrap semantics; natural fit for Swing's invalidate/validate cycle; no external dependencies |

## Part 1: Grid Layout System

### Package

`org.hkijena.jipipe.desktop.commons.components.layouts`

(Alongside existing `JIPipeDesktopFlowLayout` and `JIPipeDesktopWrapLayout`)

### Components

#### `JIPipeDesktopBreakpoint` (enum)

Defines responsive breakpoints with pixel thresholds. Thresholds are configurable via static setters but default to Bootstrap values.

| Enum value | Min width (px) | Bootstrap equivalent |
|------------|----------------|---------------------|
| `XS` | 0 | xs (<576px) |
| `SM` | 576 | sm (≥576px) |
| `MD` | 768 | md (≥768px) |
| `LG` | 992 | lg (≥992px) |
| `XL` | 1200 | xl (≥1200px) |

Static method `fromWidth(int width)` returns the active breakpoint for a given pixel width.

#### `JIPipeDesktopColumnConstraints` (immutable)

Specifies how a component should be laid out within the 12-column grid.

**Fields:**
- `int defaultSpan` — column span when no breakpoint override applies (default: 12)
- `int defaultOffset` — column offset (empty columns before this item, default: 0)
- `Map<JIPipeDesktopBreakpoint, Integer> spanOverrides` — per-breakpoint span overrides
- `Map<JIPipeDesktopBreakpoint, Integer> offsetOverrides` — per-breakpoint offset overrides

**API:**
```java
// Create with default span (offset 0)
new JIPipeDesktopColumnConstraints(6)

// Add breakpoint overrides (returns new immutable instance)
.withSpan(JIPipeDesktopBreakpoint.SM, 12)
.withSpan(JIPipeDesktopBreakpoint.XS, 12)

// Set offset
.withOffset(3)
.withOffset(JIPipeDesktopBreakpoint.MD, 2)

// Resolve effective span/offset for a given breakpoint
int getEffectiveSpan(JIPipeDesktopBreakpoint breakpoint)
int getEffectiveOffset(JIPipeDesktopBreakpoint breakpoint)
```

**Resolution rule:** Walk from the active breakpoint down to `XS`. The first specified override wins. If no override found, use the default.

#### `JIPipeDesktopColumnLayout` (LayoutManager2)

The core layout manager. Manages a 12-column grid within a container.

**Constructor:**
```java
new JIPipeDesktopColumnLayout(int gutter)  // gutter in pixels
```

**Behavior:**

1. **Active breakpoint detection:** On `layoutContainer()`, read the container's inner width and call `JIPipeDesktopBreakpoint.fromWidth()`.

2. **Span/offset resolution:** For each child, resolve the effective span and offset for the active breakpoint via the child's `JIPipeDesktopColumnConstraints` (obtained from the `LayoutManager2` constraints object).

3. **Column width calculation:** Standard Bootstrap gutter math:
   ```
   columnWidth = (containerWidth - gutter * 13) / 12 * span + gutter * (span - 1)
   ```
   Gutter between columns, half-gutter at edges.

4. **Wrapping:** Lay out children left-to-right. Track current column position. When a child's span would exceed column 12, wrap to the next row. Offsets consume column space. Items that individually exceed 12 columns are clamped to 12.

5. **Sizing methods:**
   - `preferredLayoutSize()`: Compute total height of all rows. For each row, use each child's preferred height at its resolved column width. Sum row heights + gutters.
   - `minimumLayoutSize()`: Same but using each child's minimum height.
   - `maximumLayoutSize()`: Width = parent width; height = sum of maximum heights (practically unbounded).

6. **Alignment:** Each child is laid out at its preferred height within its cell. Cells in the same row share the row's height (tallest child determines row height). Children shorter than the row are top-aligned.

**Constraints handling:** Implements `LayoutManager2` so constraints are passed via `addLayoutComponent(Component, Object)`. Accepts `JIPipeDesktopColumnConstraints` instances. If a plain `Component` is added without constraints, defaults to `new JIPipeDesktopColumnConstraints(12)` (full width).

#### `JIPipeDesktopRow` (JPanel)

A `JPanel` pre-configured with `JIPipeDesktopColumnLayout`. Has a max-width (default 1140px) and is centered within its parent. This combines Bootstrap's `.container` + `.row` concepts.

**Constructor:**
```java
new JIPipeDesktopRow()              // default 16px gutter
new JIPipeDesktopRow(int gutter)    // custom gutter
```

**Behavior:**
- Sets `JIPipeDesktopColumnLayout` as layout manager
- `getMaximumSize().width` returns 1140 (configurable via `setMaxWidth(int)`)
- Centers itself within parent via `setAlignmentX(CENTER_ALIGNMENT)` when used in a `BoxLayout`, or via parent's `BorderLayout.CENTER`
- Opaque, background from `UIManager.getColor("Panel.background")`

#### `JIPipeDesktopFluidRow` (JPanel)

Same as `JIPipeDesktopRow` but fills 100% of parent width (no max-width).

**Constructor:**
```java
new JIPipeDesktopFluidRow()             // default 16px gutter
new JIPipeDesktopFluidRow(int gutter)   // custom gutter
```

### Usage example

```java
JIPipeDesktopFluidRow row = new JIPipeDesktopFluidRow(16);

// Small card: col-3 by default, col-6 on md, col-12 on sm
row.add(smallCard, new JIPipeDesktopColumnConstraints(3)
    .withSpan(JIPipeDesktopBreakpoint.MD, 6)
    .withSpan(JIPipeDesktopBreakpoint.SM, 12));

// Chart card: col-6, col-12 on sm
row.add(chartCard, new JIPipeDesktopColumnConstraints(6)
    .withSpan(JIPipeDesktopBreakpoint.SM, 12));

// Full-width card
row.add(wideCard, new JIPipeDesktopColumnConstraints(12));

// Nesting: a column can contain another row
JIPipeDesktopFluidRow innerRow = new JIPipeDesktopFluidRow(8);
innerRow.add(childCard1, new JIPipeDesktopColumnConstraints(6));
innerRow.add(childCard2, new JIPipeDesktopColumnConstraints(6));
row.add(innerRow, new JIPipeDesktopColumnConstraints(12));
```

## Part 2: Card Component

### Package

`org.hkijena.jipipe.desktop.commons.components.cards`

### Components

#### `JIPipeDesktopCardVariant` (enum)

| Value | Theme color getter | Description |
|-------|-------------------|-------------|
| `Default` | (none — panel background) | No header tint |
| `Primary` | `getPrimaryColor()` | Blue header tint |
| `Secondary` | `getSecondaryColor()` | Purple header tint |
| `Success` | `getSuccessColor()` | Green header tint |
| `Warning` | `getWarningColor()` | Orange header tint |
| `Danger` | `getDangerColor()` | Red header tint |
| `Info` | `getInfoColor()` | Cyan header tint (new theme property) |

#### `JIPipeDesktopCard` (JPanel)

A card container with optional header, body, and footer. Uses `BorderLayout`.

**Structure:**
```
┌─────────────────────────────┐
│ [Icon] Title     [actions]  │  ← Header (JToolBar, optional)
├─────────────────────────────┤
│                             │
│  Body (any JComponent)      │  ← CENTER
│                             │
├─────────────────────────────┤
│        [Button] [Button]    │  ← Footer (JToolBar, optional)
└─────────────────────────────┘
```

**Constructors:**
```java
new JIPipeDesktopCard()                                    // empty card
new JIPipeDesktopCard(String title)                        // title only
new JIPipeDesktopCard(String title, Icon icon)             // title + icon
```

**API:**
```java
// Header
void setTitle(String title)
void setIcon(Icon icon)
void addHeaderAction(JButton button)
void removeHeaderAction(JButton button)

// Body
void setBody(JComponent component)
JComponent getBody()

// Footer
void addFooterAction(JButton button)
void removeFooterAction(JButton button)

// Variant
void setVariant(JIPipeDesktopCardVariant variant)
JIPipeDesktopCardVariant getVariant()

// Builder
static Builder builder(String title)
```

**Builder API:**
```java
JIPipeDesktopCard card = JIPipeDesktopCard.builder("Workflow runs")
    .icon(JIPipe.RESOURCES.getIcon32("actions/debug-run.png"))
    .variant(JIPipeDesktopCardVariant.Success)
    .body(chartPanel)
    .footerButton(resetButton)
    .build();
```

**Visual details:**
- Border: `RoundedLineBorder(UIUtils.getControlBorderColor(), 1, ThemeUtils.getCurrentStyle().getIslandsCornerRadius())`
- Header toolbar: bold large font for title, 32px icon, bottom matte border (1px, border color), background tinted by variant color via `ColorUtils.mix(variantColor, panelBg, 0.92)` (same pattern as `addPanelToCenterPanel` in project overview). `Default` variant uses untinted panel background.
- Footer toolbar: top matte border (1px, border color), right-aligned buttons, untinted panel background.
- When no header actions or footer actions are added, the respective toolbars are not rendered (no empty bars).
- When no title and no icon are set, the header is not rendered.

### Theme addition

Add `info` color to `JIPipeDesktopModernThemeStyle`:

| JSON key | Getter | Type | Default |
|----------|--------|------|---------|
| `info` | `getInfoColor()` | Color | `#17A2B8` |

Update all 4 JSON theme files (`JIPipe Classic Light.json`, `JIPipe Dark.json`, `JIPipe Dark Neon.json`, `JIPipe Dark High Contrast.json`) with an appropriate `"info"` value per theme.

## Part 3: Statistics Screen Redesign

### Layout structure

The statistics tab replaces the current `FlowLayout`-based card container with a `JIPipeDesktopFluidRow` inside the existing island panel. Statistics items are organized into three tiers:

1. **Small info cards (col-3):** Non-time-tracked hardware/system stats — Machine ID, JIPipe version, OS, Acceleration, GPU model, GPU VRAM, Total RAM, Recent projects. Displayed in a 4-per-row grid (3-per-row on LG, 2-per-row on MD, 1-per-row on SM/XS).

2. **Chart cards (col-6):** Time-tracked usage stats with history charts — Workflow runs, RO-Crates created, Largest project (nodes), Largest project (compartments), Noodle score. Displayed 2-per-row (1-per-row on SM/XS).

3. **Full-width chart card (col-12):** Popular nodes — needs full width for a readable chart with 5 node labels.

4. **Small metric cards (col-3):** Node move distance, Longest node width — simple numeric values without charts.

### Responsive column spans

| Tier | Default (XL/LG) | MD | SM/XS |
|------|-----------------|----|----|
| Small info cards | col-3 | col-6 | col-12 |
| Chart cards (col-6) | col-6 | col-6 | col-12 |
| Full-width chart | col-12 | col-12 | col-12 |
| Small metric cards | col-3 | col-6 | col-12 |

### Statistics item API additions

Add default methods to `JIPipeStatisticsItem` so items declare their own presentation:

```java
default String getIcon32() { return "status/starred.png"; }
default JIPipeDesktopCardVariant getCardVariant() { return JIPipeDesktopCardVariant.Default; }
default int getDefaultColumnSpan() { return 3; }
```

### Per-item configuration

| Item | Icon (32px) | Variant | Default span |
|------|------------|---------|-------------|
| Machine ID | `status/dialog-password.png` | Default | 3 |
| JIPipe version | `actions/help-about.png` | Default | 3 |
| Operating system | `actions/computer.png` | Default | 3 |
| Acceleration | `actions/gears.png` | Primary | 3 |
| GPU model | `actions/display.png` | Info | 3 |
| GPU VRAM | `actions/memory.png` | Info | 3 |
| Total RAM | `actions/memory.png` | Info | 3 |
| Recent projects | `actions/document-open-folder.png` | Secondary | 3 |
| Workflow runs | `actions/debug-run.png` | Success | 6 |
| RO-Crates created | `actions/document-export.png` | Warning | 6 |
| Largest project (nodes) | `actions/network-server-database.png` | Primary | 6 |
| Largest project (compartments) | `actions/window-flow.png` | Primary | 6 |
| Popular nodes | `actions/starred.png` | Secondary | 12 |
| Node move distance | `actions/transform-move.png` | Default | 3 |
| Longest node width | `actions/resize.png` | Default | 3 |
| Noodle score | `actions/spline.png` | Default | 6 |

Items with `isTimeTracked() == true` get a history chart in the card body. Items with `isTimeTracked() == false` get a text representation of their serialized value.

### Header

Keeps the existing header design (title, machine ID + dice reroll, ID creation date, technical info panel with first launch / last sent / privacy level, toolbar with reload + configure buttons). The header is built via `initializeHeaderPanel()` pattern matching `JIPipeDesktopProjectOverviewUI`.

### Island panel

The island panel wrapping (RoundedLineBorder, 16px padding, titled toolbar header) is kept. The `JIPipeDesktopFluidRow` replaces the `FlowLayout` container inside the island panel's scroll pane.

## Part 4: File Structure

### New files

**Layout system** (`jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/layouts/`):
- `JIPipeDesktopColumnLayout.java`
- `JIPipeDesktopColumnConstraints.java`
- `JIPipeDesktopBreakpoint.java`
- `JIPipeDesktopRow.java`
- `JIPipeDesktopFluidRow.java`

**Card system** (`jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/cards/`):
- `JIPipeDesktopCard.java`
- `JIPipeDesktopCardVariant.java`

### Modified files

**Theme:**
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/theme/JIPipeDesktopModernThemeStyle.java` — add `info` color property
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Classic Light.json`
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark.json`
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark Neon.json`
- `jipipe-core/src/main/resources/org/hkijena/jipipe/styles/JIPipe Dark High Contrast.json`

**Statistics item API:**
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java` — add default methods
- All 16 statistics item classes in `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/` — override defaults

**Statistics UI:**
- `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java` — rewrite using new system

### Test files

**Layout tests** (`jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/layouts/`):
- `JIPipeDesktopColumnLayoutTest.java` — column math, wrapping, offsets, breakpoint resolution, nesting

**Card tests** (`jipipe-core/src/test/java/org/hkijena/jipipe/desktop/commons/components/cards/`):
- `JIPipeDesktopCardTest.java` — header/body/footer structure, variant coloring, builder API

**Statistics tests:**
- Extend `JIPipeStatisticsRegistryTest.java` — verify each item returns non-null icon, valid variant, reasonable column span

## Part 5: Implementation Order

1. **Theme:** Add `info` color to `JIPipeDesktopModernThemeStyle` + update 4 JSON files
2. **Breakpoint enum + column constraints:** `JIPipeDesktopBreakpoint`, `JIPipeDesktopColumnConstraints`
3. **Column layout manager:** `JIPipeDesktopColumnLayout` + tests
4. **Row containers:** `JIPipeDesktopRow`, `JIPipeDesktopFluidRow`
5. **Card component:** `JIPipeDesktopCard`, `JIPipeDesktopCardVariant` + tests
6. **Statistics item API:** Add default methods to `JIPipeStatisticsItem`
7. **Override defaults:** Update all 16 statistics items with icon, variant, span
8. **Rewrite statistics UI:** Use `JIPipeDesktopFluidRow` + `JIPipeDesktopCard`
