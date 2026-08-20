# Cellpose 4.2 Model Support & Actionable Error System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add support for Cellpose 4.2 models (cpsam_v2, cpdino, cpdino-vitb), extend the validation error system with clickable action buttons, and enable users to auto-update their Cellpose environment from within error messages.

**Architecture:** Three subsystems are touched: (1) the core validation/error system gains `JIPipeNotificationAction` support on `JIPipeValidationReportEntry`, rendered as buttons in the error UI; (2) the Cellpose 4 plugin gains new models, a conditional `bsize` parameter for DINO models, a new training node with 4.2 defaults, and a version-check utility that generates validation warnings + runtime guards with actionable "Update Cellpose" buttons; (3) the Project Overview's upgrade dialog is extracted into a reusable utility.

**Tech Stack:** Java 21, Swing, Maven, JUnit 5

## Global Constraints

- Java 21 (project minimum)
- Maven build system, multi-module project at `/data/src/jipipe-4`
- JIPipe version: `6.0.0-SNAPSHOT`
- Test framework: JUnit 5 (`org.junit.jupiter`)
- No project upgrade logic needed (JIPipe 6 is unreleased)
- The `ArtifactUpgrade` inner class of `JIPipeDesktopProjectOverviewUI` must be extracted to a standalone class
- Cellpose < 4.2 supports only `cpsam` and `None`; Cellpose >= 4.2 supports `cpsam_v2`, `cpdino`, `cpdino-vitb`, `cpsam`, `None`
- Version is parsed from the artifact query string (e.g., `com.github.mouseland.cellpose4:4.2.1.1.1000-*`); custom (non-artifact) environments skip version checking

---

### Task 1: Add Actions to JIPipeValidationReportEntry

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportEntry.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportEntryTest.java`

**Interfaces:**
- Produces: `JIPipeValidationReportEntry.getActions()` returns `List<JIPipeNotificationAction>`; new constructors accept actions

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JIPipeValidationReportEntryTest {

    @Test
    public void testEntryWithActions() {
        JIPipeNotificationAction action1 = new JIPipeNotificationAction("Update", "Update tooltip", null, wb -> {});
        JIPipeNotificationAction action2 = new JIPipeNotificationAction("Fix", "Fix tooltip", null, wb -> {});
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution",
                null,
                Arrays.asList(action1, action2));

        List<JIPipeNotificationAction> actions = entry.getActions();
        assertEquals(2, actions.size());
        assertEquals("Update", actions.get(0).getLabel());
        assertEquals("Fix", actions.get(1).getLabel());
    }

    @Test
    public void testEntryWithoutActionsDefaultsToEmptyList() {
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution");

        assertTrue(entry.getActions().isEmpty());
    }

    @Test
    public void testEntryWithDetailsButNoActionsDefaultsToEmptyList() {
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution",
                "details");

        assertTrue(entry.getActions().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -Dtest=JIPipeValidationReportEntryTest -q`
Expected: FAIL — no constructor accepting `List<JIPipeNotificationAction>`, no `getActions()` method

- [ ] **Step 3: Write minimal implementation**

Add import at top of `JIPipeValidationReportEntry.java`:

```java
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
```

Add field after `private final String details;` (line 30):

```java
private final List<JIPipeNotificationAction> actions;
```

Add new constructor after the existing 4-arg constructor (after line 58):

```java
public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation, String solution, String details, List<JIPipeNotificationAction> actions) {
    this.level = level;
    this.context = context != null ? context : new UnspecifiedValidationReportContext();
    this.title = title;
    this.explanation = explanation;
    this.solution = solution;
    this.details = details;
    this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
}
```

Update the existing 6-arg constructor (line 40) to delegate actions as empty:

```java
public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation, String solution, String details) {
    this(level, context, title, explanation, solution, details, null);
}
```

Update the existing 5-arg constructor (line 56) to delegate:

```java
public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation, String solution) {
    this(level, context, title, explanation, solution, null, null);
}
```

Update the existing 4-arg constructor (line 66) to delegate:

```java
public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation) {
    this(level, context, title, explanation, null, null, null);
}
```

Add getter after `getDetails()` (after line 122):

```java
public List<JIPipeNotificationAction> getActions() {
    return Collections.unmodifiableList(actions);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -Dtest=JIPipeValidationReportEntryTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /data/src/jipipe-4 && git add jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportEntry.java jipipe-core/src/test/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportEntryTest.java
git commit -m "Add action support to JIPipeValidationReportEntry"
```

---

### Task 2: Add Action Builder to ValidationEntryBuilder

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportContext.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/validation/ValidationEntryBuilderActionTest.java`

**Interfaces:**
- Consumes: `JIPipeValidationReportEntry.getActions()` from Task 1
- Produces: `ValidationEntryBuilder.action(JIPipeNotificationAction)` fluent method; `build()` and `report()` now create entries with actions

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationEntryBuilderActionTest {

    @Test
    public void testBuilderWithAction() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Update", "Update tooltip", null, wb -> {});
        JIPipeValidationReportContext context = new UnspecifiedValidationReportContext();
        JIPipeValidationReportEntry entry = context.error()
                .title("Test title")
                .explanation("Test explanation")
                .action(action)
                .build();

        assertEquals(1, entry.getActions().size());
        assertEquals("Update", entry.getActions().get(0).getLabel());
    }

    @Test
    public void testBuilderWithMultipleActions() {
        JIPipeNotificationAction action1 = new JIPipeNotificationAction("Update", null, null, wb -> {});
        JIPipeNotificationAction action2 = new JIPipeNotificationAction("Fix", null, null, wb -> {});
        JIPipeValidationReportContext context = new UnspecifiedValidationReportContext();
        JIPipeValidationReportEntry entry = context.error()
                .title("Test title")
                .action(action1)
                .action(action2)
                .build();

        assertEquals(2, entry.getActions().size());
    }

    @Test
    public void testBuilderReportCarriesActions() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Update", null, null, wb -> {});
        JIPipeValidationReportContext context = new UnspecifiedValidationReportContext();
        JIPipeValidationReport report = new JIPipeValidationReport();
        JIPipeValidationReportEntry entry = context.error()
                .title("Test title")
                .action(action)
                .report(report);

        assertEquals(1, entry.getActions().size());
        assertEquals(1, report.size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -Dtest=ValidationEntryBuilderActionTest -q`
Expected: FAIL — no `.action()` method on builder

- [ ] **Step 3: Write minimal implementation**

In `JIPipeValidationReportContext.java`, add import:

```java
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
```

In the `ValidationEntryBuilder` inner class (after line 282), add field:

```java
private final List<JIPipeNotificationAction> actions = new ArrayList<>();
```

Add method after `details()` (after line 331):

```java
public ValidationEntryBuilder action(JIPipeNotificationAction action) {
    this.actions.add(action);
    return this;
}
```

Update `report()` method (line 338) to pass actions:

```java
public JIPipeValidationReportEntry report(JIPipeValidationReport report) {
    JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(level, context, title, explanation, solution, details, actions);
    report.add(entry);
    return entry;
}
```

Update `build()` method (line 350) to pass actions:

```java
public JIPipeValidationReportEntry build() {
    return new JIPipeValidationReportEntry(level, context, title, explanation, solution, details, actions);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -Dtest=ValidationEntryBuilderActionTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /data/src/jipipe-4 && git add jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportContext.java jipipe-core/src/test/java/org/hkijena/jipipe/api/validation/ValidationEntryBuilderActionTest.java
git commit -m "Add action() fluent builder to ValidationEntryBuilder"
```

---

### Task 3: Carry Actions Through JIPipeValidationRuntimeException

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationRuntimeException.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/validation/JIPipeValidationRuntimeExceptionActionsTest.java`

**Interfaces:**
- Consumes: `JIPipeValidationReportEntry.getActions()` from Task 1
- Produces: `JIPipeValidationRuntimeException` preserves actions when wrapping entries

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JIPipeValidationRuntimeExceptionActionsTest {

    @Test
    public void testExceptionFromEntryPreservesActions() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Update", null, null, wb -> {});
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution",
                null,
                java.util.Collections.singletonList(action));

        JIPipeValidationRuntimeException exception = new JIPipeValidationRuntimeException(entry);
        JIPipeValidationReport report = exception.getReport();
        assertEquals(1, report.size());
        assertEquals(1, report.get(0).getActions().size());
        assertEquals("Update", report.get(0).getActions().get(0).getLabel());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -Dtest=JIPipeValidationRuntimeExceptionActionsTest -q`
Expected: FAIL — `JIPipeValidationRuntimeException(JIPipeValidationReportEntry)` constructor does not preserve actions

- [ ] **Step 3: Write minimal implementation**

In `JIPipeValidationRuntimeException.java`, add import:

```java
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import java.util.List;
```

Update the `mergeReport` method to carry actions through. In the `switch` block (lines 59-62), update the Error and Warning cases to also pass actions:

```java
case Error ->
        alternativeContext.error().title(entry.getTitle()).explanation(entry.getExplanation()).solution(entry.getSolution()).details(entry.getDetails()).report(report);
case Warning ->
        alternativeContext.warning().title(entry.getTitle()).explanation(entry.getExplanation()).solution(entry.getSolution()).details(entry.getDetails()).report(report);
```

Change to:

```java
case Error -> {
    ValidationEntryBuilder builder = alternativeContext.error().title(entry.getTitle()).explanation(entry.getExplanation()).solution(entry.getSolution()).details(entry.getDetails());
    for (JIPipeNotificationAction action : entry.getActions()) {
        builder.action(action);
    }
    builder.report(report);
}
case Warning -> {
    ValidationEntryBuilder builder = alternativeContext.warning().title(entry.getTitle()).explanation(entry.getExplanation()).solution(entry.getSolution()).details(entry.getDetails());
    for (JIPipeNotificationAction action : entry.getActions()) {
        builder.action(action);
    }
    builder.report(report);
}
```

The `JIPipeValidationRuntimeException(JIPipeValidationReportEntry entry)` constructor (line 26) already creates a report and adds the entry directly — the entry's actions are already on it. The `mergeReport` path is where actions were being lost. This fix ensures they survive.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -Dtest=JIPipeValidationRuntimeExceptionActionsTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /data/src/jipipe-4 && git add jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationRuntimeException.java jipipe-core/src/test/java/org/hkijena/jipipe/api/validation/JIPipeValidationRuntimeExceptionActionsTest.java
git commit -m "Preserve actions through JIPipeValidationRuntimeException merge"
```

---

### Task 4: Render Action Buttons in JIPipeDesktopUserFriendlyErrorUI

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/validation/JIPipeDesktopUserFriendlyErrorUI.java`

**Interfaces:**
- Consumes: `JIPipeValidationReportEntry.getActions()` from Task 1
- Produces: Action buttons rendered in the error UI between "Go to" and "Show details"

- [ ] **Step 1: Implement action button rendering**

In `JIPipeDesktopUserFriendlyErrorUI.java`, add import:

```java
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
```

In the `addEntry()` method, after the "Go to" button block (after line 284, before `actionBar.add(Box.createHorizontalGlue());` on line 286), add:

```java
for (JIPipeNotificationAction action : entry.getActions()) {
    JButton actionButton = new JButton(action.getLabel(), action.getIcon());
    actionButton.setOpaque(false);
    actionButton.setToolTipText(action.getTooltip());
    JIPipeNotificationAction finalAction = action;
    actionButton.addActionListener(e -> {
        finalAction.getAction().accept(getDesktopWorkbench());
    });
    actionBar.add(actionButton);
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl jipipe-core -q`
Expected: SUCCESS

- [ ] **Step 3: Run existing tests to ensure no regressions**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -q`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
cd /data/src/jipipe-4 && git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/validation/JIPipeDesktopUserFriendlyErrorUI.java
git commit -m "Render action buttons in error UI"
```

---

### Task 5: Extract ArtifactUpgrade and Upgrade Dialog into Reusable Utilities

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgrade.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgradeUtils.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopProjectOverviewUI.java`

**Interfaces:**
- Produces: `ArtifactUpgrade` standalone data class; `ArtifactUpgradeUtils.showUpgradeDialog(JIPipeDesktopWorkbench, List<ArtifactUpgrade>)` static method

- [ ] **Step 1: Create the ArtifactUpgrade standalone class**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgrade.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.project;

import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;

import java.util.List;

public class ArtifactUpgrade {
    private final JIPipeArtifactEnvironment environment;
    private final JIPipeArtifact current;
    private final List<JIPipeArtifact> revisionUpgrades;
    private final List<JIPipeArtifact> accelerationUpgrades;

    public ArtifactUpgrade(JIPipeArtifactEnvironment environment, JIPipeArtifact current, List<JIPipeArtifact> revisionUpgrades, List<JIPipeArtifact> accelerationUpgrades) {
        this.environment = environment;
        this.current = current;
        this.revisionUpgrades = revisionUpgrades;
        this.accelerationUpgrades = accelerationUpgrades;
    }

    public JIPipeArtifactEnvironment getEnvironment() {
        return environment;
    }

    public JIPipeArtifact getCurrent() {
        return current;
    }

    public List<JIPipeArtifact> getRevisionUpgrades() {
        return revisionUpgrades;
    }

    public List<JIPipeArtifact> getAccelerationUpgrades() {
        return accelerationUpgrades;
    }
}
```

- [ ] **Step 2: Create the ArtifactUpgradeUtils class**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgradeUtils.java`:

```java
package org.hkijena.jipipe.desktop.commons.components.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtifactUpgradeUtils {

    public static void showUpgradeDialog(JIPipeDesktopWorkbench workbench, List<ArtifactUpgrade> upgrades) {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        List<JComboBox<String>> comboBoxes = new java.util.ArrayList<>();

        formPanel.addToForm(UIUtils.createJLabel("New version", 16), UIUtils.createJLabel("Old version", 16));
        formPanel.addWideToForm(new JSeparator(JSeparator.HORIZONTAL));

        for (ArtifactUpgrade artifactUpgrade : upgrades) {
            JComboBox<String> comboBox = new JComboBox<>();
            for (JIPipeArtifact revisionUpgrade : artifactUpgrade.getRevisionUpgrades()) {
                comboBox.addItem(revisionUpgrade.getFullId());
            }
            for (JIPipeArtifact accelerationUpgrade : artifactUpgrade.getAccelerationUpgrades()) {
                comboBox.addItem(accelerationUpgrade.getFullId());
            }
            comboBox.addItem("Keep as-is");
            comboBoxes.add(comboBox);

            formPanel.addToForm(comboBox, new JLabel(artifactUpgrade.getCurrent().getFullId(), JIPipe.RESOURCES.getIcon16("actions/run-build-install.png"), JLabel.LEFT));
        }
        int numSuccesses = 0;
        if (JIPipeDesktopFormPanel.showDialog(workbench.getWindow(), formPanel, "Update third-party artifacts")) {
            for (int i = 0; i < upgrades.size(); i++) {
                ArtifactUpgrade upgrade = upgrades.get(i);
                JComboBox<String> comboBox = comboBoxes.get(i);
                String selectedItem = StringUtils.nullToEmpty(comboBox.getSelectedItem());

                if (!StringUtils.isNullOrEmpty(selectedItem) && !"Keep as-is".equals(selectedItem)) {
                    upgrade.getEnvironment().setArtifactQuery(new JIPipeArtifactQueryParameter(selectedItem));
                    ++numSuccesses;
                }
            }

            if (numSuccesses > 0) {
                JOptionPane.showMessageDialog(workbench.getWindow(), StringUtils.wrapHtml(StringUtils.formatPluralS(numSuccesses, "artifact") + " were updated.<br/>JIPipe will automatically take care of downloading and setting up the artifacts."));
            }
        }
    }

    public static java.util.List<ArtifactUpgrade> findAvailableUpgrades(JIPipeArtifactEnvironment environment) {
        java.util.List<ArtifactUpgrade> upgrades = new java.util.ArrayList<>();
        if (environment.isLoadFromArtifact() && !StringUtils.isNullOrEmpty(environment.getArtifactQuery().getQuery())) {
            JIPipeArtifact queriedArtifact = environment.getArtifactQuery().toArtifact();
            if (queriedArtifact.getResolutionStatus() == JIPipeArtifact.ResolutionStatus.GroupNameVersion || queriedArtifact.getResolutionStatus() == JIPipeArtifact.ResolutionStatus.Full) {
                try {
                    JIPipeArtifact current = JIPipe.getArtifacts().queryPreferredCachedArtifact(environment.getArtifactQuery().getQuery());
                    List<JIPipeArtifact> candidates = JIPipe.getArtifacts().queryCachedArtifacts(queriedArtifact.getFullId(JIPipeArtifact.ResolutionStatus.GroupName));
                    List<JIPipeArtifact> revisionUpgrades = new java.util.ArrayList<>();
                    if (current != null) {
                        int revisionVersion = current.getVersionRevision();
                        String baseVersion = current.getVersionWithoutRevision();

                        Set<String> alreadyAdded = new HashSet<>();
                        for (JIPipeArtifact candidate : candidates) {
                            if (candidate.isCompatible()) {
                                String candidateBaseVersion = candidate.getVersionWithoutRevision();
                                int candidateRevision = candidate.getVersionRevision();
                                if (StringUtils.compareVersions(candidateBaseVersion, baseVersion) == 0) {
                                    if (candidateRevision > revisionVersion && !candidate.getFullId().equals(current.getFullId())) {
                                        JIPipeArtifact candidate1 = new JIPipeArtifact(candidate);
                                        candidate1.setClassifier("*");
                                        String candidate1Query = candidate1.getFullId(JIPipeArtifact.ResolutionStatus.GroupNameVersion);
                                        if (!alreadyAdded.contains(candidate1Query)) {
                                            revisionUpgrades.add(candidate1);
                                            alreadyAdded.add(candidate1Query);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!revisionUpgrades.isEmpty()) {
                        upgrades.add(new ArtifactUpgrade(environment, current, revisionUpgrades, java.util.Collections.emptyList()));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return upgrades;
    }
}
```

- [ ] **Step 3: Update JIPipeDesktopProjectOverviewUI to use the extracted utilities**

In `JIPipeDesktopProjectOverviewUI.java`:

1. Remove the inner class `ArtifactUpgrade` (lines 1114-1126).

2. Add import:
```java
import org.hkijena.jipipe.desktop.commons.components.project.ArtifactUpgrade;
import org.hkijena.jipipe.desktop.commons.components.project.ArtifactUpgradeUtils;
```

3. Replace the body of `upgradeArtifacts()` method (lines 332-372) with:
```java
private void upgradeArtifacts(List<ArtifactUpgrade> upgrades) {
    ArtifactUpgradeUtils.showUpgradeDialog(getDesktopWorkbench(), upgrades);
    refreshCenterPanel();
}
```

4. Update `createArtifactUpgradeTipsIfNeeded()` to use `ArtifactUpgradeUtils.findAvailableUpgrades()` instead of the inline logic. Replace the for-loop body (lines 282-322) with:
```java
for (JIPipeArtifactEnvironment environment : environments) {
    upgrades.addAll(ArtifactUpgradeUtils.findAvailableUpgrades(environment));
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl jipipe-core -q`
Expected: SUCCESS

- [ ] **Step 5: Run existing tests**

Run: `cd /data/src/jipipe-4 && mvn test -pl jipipe-core -q`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
cd /data/src/jipipe-4 && git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgrade.java jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgradeUtils.java jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/settings/JIPipeDesktopProjectOverviewUI.java
git commit -m "Extract ArtifactUpgrade and upgrade dialog into reusable utilities"
```

---

### Task 6: Add New Cellpose 4 Models to PretrainedCellpose4SegmentationModel

**Files:**
- Modify: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/parameters/cp4/PretrainedCellpose4SegmentationModel.java`

**Interfaces:**
- Produces: `PretrainedCellpose4SegmentationModel.cpsam_v2`, `.cpdino`, `.cpdino_vitb` enum constants; `cpsam_v2` is the default (first ordinal)

- [ ] **Step 1: Update the enum**

Replace the enum constants in `PretrainedCellpose4SegmentationModel.java` (lines 22-24):

```java
public enum PretrainedCellpose4SegmentationModel {
    cpsam_v2("cpsam_v2", "Cellpose SAM v2"),
    cpdino("cpdino", "Cellpose DINO (ViT-L)"),
    cpdino_vitb("cpdino-vitb", "Cellpose DINO (ViT-B)"),
    cpsam("cpsam", "Cellpose SAM"),
    None(null, "None (only training)");
```

- [ ] **Step 2: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl jipipe-core,plugins/jipipe-plugin-cellpose -q`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /data/src/jipipe-4 && git add plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/parameters/cp4/PretrainedCellpose4SegmentationModel.java
git commit -m "Add cpsam_v2, cpdino, cpdino_vitb to PretrainedCellpose4SegmentationModel"
```

---

### Task 7: Create CellposeVersionUtils

**Files:**
- Create: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/utils/CellposeVersionUtils.java`
- Test: `plugins/jipipe-plugin-cellpose/src/test/java/org/hkijena/jipipe/plugins/cellpose/utils/CellposeVersionUtilsTest.java`

**Interfaces:**
- Produces: `CellposeVersionUtils.getInstalledVersion(Cellpose4Environment)`, `CellposeVersionUtils.isModelSupported(String, String)`, `CellposeVersionUtils.requiresCellpose42(String)`, `CellposeVersionUtils.findAvailableUpgrades(Cellpose4Environment)`

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.plugins.cellpose.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CellposeVersionUtilsTest {

    @Test
    public void testRequiresCellpose42() {
        assertTrue(CellposeVersionUtils.requiresCellpose42("cpsam_v2"));
        assertTrue(CellposeVersionUtils.requiresCellpose42("cpdino"));
        assertTrue(CellposeVersionUtils.requiresCellpose42("cpdino-vitb"));
        assertFalse(CellposeVersionUtils.requiresCellpose42("cpsam"));
        assertFalse(CellposeVersionUtils.requiresCellpose42(null));
    }

    @Test
    public void testIsModelSupportedOldVersion() {
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam", "4.0.7"));
        assertTrue(CellposeVersionUtils.isModelSupported(null, "4.0.7"));
        assertFalse(CellposeVersionUtils.isModelSupported("cpsam_v2", "4.0.7"));
        assertFalse(CellposeVersionUtils.isModelSupported("cpdino", "4.0.7"));
        assertFalse(CellposeVersionUtils.isModelSupported("cpdino-vitb", "4.0.7"));
    }

    @Test
    public void testIsModelSupportedNewVersion() {
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam_v2", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported("cpdino", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported("cpdino-vitb", "4.2.1"));
        assertTrue(CellposeVersionUtils.isModelSupported(null, "4.2.1"));
    }

    @Test
    public void testIsModelSupportedNullVersion() {
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam", null));
        assertTrue(CellposeVersionUtils.isModelSupported("cpsam_v2", null));
        assertTrue(CellposeVersionUtils.isModelSupported(null, null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /data/src/jipipe-4 && mvn test -pl plugins/jipipe-plugin-cellpose -Dtest=CellposeVersionUtilsTest -q`
Expected: FAIL — class does not exist

- [ ] **Step 3: Write minimal implementation**

Create `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/utils/CellposeVersionUtils.java`:

```java
package org.hkijena.jipipe.plugins.cellpose.utils;

import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.plugins.cellpose.environments.cp4.Cellpose4Environment;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CellposeVersionUtils {

    private static final Set<String> MODELS_REQUIRING_42 = Set.of("cpsam_v2", "cpdino", "cpdino-vitb");

    public static String getInstalledVersion(Cellpose4Environment env) {
        if (env == null || !env.isLoadFromArtifact()) {
            return null;
        }
        String query = env.getArtifactQuery().getQuery();
        if (StringUtils.isNullOrEmpty(query)) {
            return null;
        }
        JIPipeArtifact artifact = env.getArtifactQuery().toArtifact();
        return artifact.getVersion();
    }

    public static boolean requiresCellpose42(String modelId) {
        return modelId != null && MODELS_REQUIRING_42.contains(modelId);
    }

    public static boolean isModelSupported(String modelId, String version) {
        if (version == null) {
            return true;
        }
        if (!requiresCellpose42(modelId)) {
            return true;
        }
        return org.hkijena.jipipe.utils.StringUtils.compareVersions(version, "4.2") >= 0;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /data/src/jipipe-4 && mvn test -pl plugins/jipipe-plugin-cellpose -Dtest=CellposeVersionUtilsTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /data/src/jipipe-4 && git add plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/utils/CellposeVersionUtils.java plugins/jipipe-plugin-cellpose/src/test/java/org/hkijena/jipipe/plugins/cellpose/utils/CellposeVersionUtilsTest.java
git commit -m "Add CellposeVersionUtils for version detection and model compatibility"
```

---

### Task 8: Add bsize Parameter and Version Checks to Cellpose4SegmentationInferenceAlgorithm

**Files:**
- Modify: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationInferenceAlgorithm.java`

**Interfaces:**
- Consumes: `CellposeVersionUtils` from Task 7, `ArtifactUpgradeUtils` from Task 5, `JIPipeNotificationAction` action support from Tasks 1-4
- Produces: `bsize` parameter on CP4 inference; validation warning + runtime guard for version compatibility

- [ ] **Step 1: Add bsize field and imports**

Add imports at the top of `Cellpose4SegmentationInferenceAlgorithm.java`:

```java
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.desktop.commons.components.project.ArtifactUpgrade;
import org.hkijena.jipipe.desktop.commons.components.project.ArtifactUpgradeUtils;
import org.hkijena.jipipe.plugins.cellpose.parameters.cp4.PretrainedCellpose4SegmentationModel;
import org.hkijena.jipipe.plugins.cellpose.utils.CellposeVersionUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
```

Add field after `private boolean enableMultiChannel = true;` (line 112):

```java
private OptionalIntegerParameter bsize = new OptionalIntegerParameter(384, true);
```

- [ ] **Step 2: Add bsize parameter getters/setters and copy constructor updates**

In the copy constructor (after line 144, `this.cleanUpAfterwards = other.cleanUpAfterwards;`), add:

```java
this.bsize = new OptionalIntegerParameter(other.bsize);
```

After the `getGpuSettings()` method (after line 538), add parameter accessors:

```java
@SetJIPipeDocumentation(name = "Tile size (DINO models only)", description = "Block size for tiles when using DINO models (cpdino, cpdino-vitb). Default is 384. SAM models use a fixed tile size of 256 and this parameter is ignored.")
@JIPipeParameter(value = "bsize", uiOrder = 50)
public OptionalIntegerParameter getBsize() {
    return bsize;
}

@JIPipeParameter("bsize")
public void setBsize(OptionalIntegerParameter bsize) {
    this.bsize = bsize;
}

@Override
public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
    if ("bsize".equals(access.getKey())) {
        return isDinoModelSelected();
    }
    return super.isParameterUIVisible(tree, access);
}

private boolean isDinoModelSelected() {
    return false;
}
```

Note: The model comes from input data (a `CellposeModelData` input slot), not a parameter. Since the model is not known at UI rendering time, `isDinoModelSelected()` returns `true` so the bsize parameter is always visible. The documentation explains it only applies to DINO models. At runtime, `--bsize` is only passed when the model name is `cpdino` or `cpdino-vitb`.

Update `isDinoModelSelected()` to `return true;`.

- [ ] **Step 3: Pass --bsize in runCellpose() for DINO models**

In `runCellpose()` method, after the model arguments (after line 422), add:

```java
// Tile size for DINO models
String modelName = modelNameOrPath;
if (modelName != null && (modelName.equals("cpdino") || modelName.equals("cpdino-vitb"))) {
    if (bsize.isEnabled()) {
        arguments.add("--bsize");
        arguments.add(String.valueOf(bsize.getContent()));
    }
}
```

- [ ] **Step 4: Add validation warning for version compatibility**

Update `reportValidity()` (line 199) to:

```java
@Override
public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
    super.reportValidity(reportContext, reportSettings, report, progressInfo);
    Cellpose4Environment environment = getEnvironment(Cellpose4Environment.class);
    String version = CellposeVersionUtils.getInstalledVersion(environment);
    if (version != null) {
        JIPipeInputDataSlot modelSlot = getInputSlot("Model");
        for (int row = 0; row < modelSlot.getRowCount(); row++) {
            CellposeModelData modelData = modelSlot.getData(row, CellposeModelData.class, progressInfo);
            String modelId = modelData.getModelName();
            if (!CellposeVersionUtils.isModelSupported(modelId, version)) {
                List<ArtifactUpgrade> upgrades = ArtifactUpgradeUtils.findAvailableUpgrades(environment);
                JIPipeValidationReportContext context = new GraphNodeValidationReportContext(this);
                if (!upgrades.isEmpty()) {
                    context.warning()
                            .title("Cellpose version may be too old")
                            .explanation("The selected model '" + modelId + "' requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ".")
                            .solution("Update to Cellpose 4.2 or later by clicking the 'Update Cellpose' button.")
                            .action(new JIPipeNotificationAction("Update Cellpose", "Update to a newer Cellpose version",
                                    JIPipe.RESOURCES.getIcon16("actions/list-check.png"),
                                    wb -> ArtifactUpgradeUtils.showUpgradeDialog((org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench) wb, upgrades)))
                            .report(report);
                } else {
                    context.warning()
                            .title("Cellpose version may be too old")
                            .explanation("The selected model '" + modelId + "' requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ".")
                            .solution("Please install a newer Cellpose artifact manually.")
                            .report(report);
                }
                break;
            }
        }
    }
}
```

- [ ] **Step 5: Add runtime guard in runIteration()**

In `runIteration()`, after getting the environment (after line 207), add:

```java
// Runtime version guard
String version = CellposeVersionUtils.getInstalledVersion(environment);
if (version != null) {
    JIPipeInputDataSlot modelSlot = getInputSlot("Model");
    for (int row = 0; row < modelSlot.getRowCount(); row++) {
        CellposeModelData modelData = modelSlot.getData(row, CellposeModelData.class, progressInfo);
        String modelId = modelData.getModelName();
        if (!CellposeVersionUtils.isModelSupported(modelId, version)) {
            List<ArtifactUpgrade> upgrades = ArtifactUpgradeUtils.findAvailableUpgrades(environment);
            GraphNodeValidationReportContext context = new GraphNodeValidationReportContext(this);
            if (!upgrades.isEmpty()) {
                throw new JIPipeValidationRuntimeException(context.error()
                        .title("Cellpose version too old for selected model")
                        .explanation("The selected model '" + modelId + "' requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ". Execution was aborted.")
                        .solution("Update to Cellpose 4.2 or later.")
                        .action(new JIPipeNotificationAction("Update Cellpose", "Update to a newer Cellpose version",
                                JIPipe.RESOURCES.getIcon16("actions/list-check.png"),
                                wb -> ArtifactUpgradeUtils.showUpgradeDialog((org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench) wb, upgrades)))
                        .build());
            } else {
                throw new JIPipeValidationRuntimeException(context.error()
                        .title("Cellpose version too old for selected model")
                        .explanation("The selected model '" + modelId + "' requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ". Execution was aborted.")
                        .solution("Please install a newer Cellpose artifact manually.")
                        .build());
            }
        }
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl jipipe-core,plugins/jipipe-plugin-cellpose -q`
Expected: SUCCESS

- [ ] **Step 7: Commit**

```bash
cd /data/src/jipipe-4 && git add plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationInferenceAlgorithm.java
git commit -m "Add bsize parameter and version checks to CP4 inference"
```

---

### Task 9: Deprecate Old Training Node

**Files:**
- Modify: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTrainingAlgorithm.java`

**Interfaces:**
- Produces: Old training node annotated `@Deprecated` with display name "(old)"

- [ ] **Step 1: Deprecate and rename the old training node**

In `Cellpose4SegmentationTrainingAlgorithm.java`:

1. Add import:
```java
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
```

2. Change the `@SetJIPipeDocumentation` annotation (line 77) from:
```java
@SetJIPipeDocumentation(name = "Cellpose segmentation training (4.x)", description = ...)
```
to:
```java
@Deprecated
@SetJIPipeDocumentation(name = "Cellpose segmentation training 4.x (old)", description = ...)
```

- [ ] **Step 2: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl plugins/jipipe-plugin-cellpose -q`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /data/src/jipipe-4 && git add plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTrainingAlgorithm.java
git commit -m "Deprecate old CP4 training node and rename to (old)"
```

---

### Task 10: Create Cellpose4SegmentationTraining2Algorithm

**Files:**
- Create: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTraining2Algorithm.java`

**Interfaces:**
- Consumes: `CellposeVersionUtils` from Task 7, `ArtifactUpgradeUtils` from Task 5
- Produces: New CP4 training node with 4.2 defaults, aggressive log for non-cpsam, validation warning + runtime guard

- [ ] **Step 1: Create the new training algorithm**

Create `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTraining2Algorithm.java`.

This is a copy of `Cellpose4SegmentationTrainingAlgorithm` with the following changes:
- Class name: `Cellpose4SegmentationTraining2Algorithm`
- `@SetJIPipeDocumentation(name = "Cellpose segmentation training (4.x)")` 
- Remove `Cellpose2ChannelSettings` (channels are deprecated in 4.0.1+)
- Updated training defaults: `learningRate = 0.00001`, `weightDecay = 0.1`, `batchSize = 1`, `numEpochs = 100`
- Add `--bsize`, `--save_every`, `--model_name_out` CLI params
- Remove `--diam_mean`, `--train_size`, `--SGD`, `--chan`, `--chan2`, `--all_channels`, `--invert` CLI params
- Add validation warning for non-cpsam starting model
- Add `progressInfo.aggressiveError()` for non-cpsam model at runtime
- Add version check validation warning + runtime guard (same as inference)

Use the full source of `Cellpose4SegmentationTrainingAlgorithm` as the base, applying all changes above. The key differences from the old class:

```java
@SetJIPipeDocumentation(name = "Cellpose segmentation training (4.x)", description =
        "Trains a segmentation model with Cellpose 4.2+. You start from the cpsam model or train from scratch. " +
                "Incoming images are automatically converted to greyscale. Only 2D or 3D images are supported. For this node to work, you need to annotate a greyscale 16-bit or 8-bit label image column to each raw data input. " +
                "To do this, you can use the node 'Annotate with data'. By default, JIPipe will ensure that all connected components of this image are assigned a unique component. You can disable this feature via the parameters. " +
                "Does not support the training of image restoration models.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Training data", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Test data", create = true, optional = true)
@AddJIPipeInputSlot(value = CellposeModelData.class, name = "Pretrained model", create = true, description = "The pretrained model. If you want to train from scratch, provide a pretrained model 'None'. Only cpsam is recommended for training.", role = JIPipeDataSlotRole.ParametersLooping)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Deep learning")
@AddJIPipeOutputSlot(value = CellposeModelData.class, name = "Model", create = true, description = "The trained model")
@AddJIPipeOutputSlot(value = CellposeSizeModelData.class)
@RegisterJIPipeEnvironmentUsage(Cellpose4Environment.class)
public class Cellpose4SegmentationTraining2Algorithm extends JIPipeSingleIterationAlgorithm {
```

Field changes:
- Remove `channelSettings` field and all its registrations
- `numEpochs = 100` (was 500)
- Add `private OptionalIntegerParameter saveEvery = new OptionalIntegerParameter(100, true);`
- Add `private String modelNameOut = "";`

In `processModel()`, the CLI argument building changes:
- Remove all channel arguments (`--chan`, `--chan2`, `--all_channels`, `--invert`)
- Remove `--diam_mean`, `--train_size`, `--SGD`
- Add after diameter: `--bsize 256`
- Add: `--save_every` with value
- Add: `--model_name_out` if non-empty
- Update `--learning_rate` default from `tweaksSettings.getLearningRate()` (which defaults to 0.2 in the settings class) — instead, the new algorithm overrides the defaults by setting `tweaksSettings.setLearningRate(0.00001)`, `tweaksSettings.setWeightDecay(0.1)`, `tweaksSettings.setBatchSize(1)` in the constructor.

In `runIteration()`, after getting the environment, add aggressive log:

```java
for (CellposeModelInfo modelInfo : modelInfos) {
    if (modelInfo.getModelNameOrPath() != null && !modelInfo.getModelNameOrPath().equals("cpsam") && !modelInfo.getModelNameOrPath().equals("None")) {
        progressInfo.aggressiveError("WARNING: Only the 'cpsam' model is recommended for training. Using '" + modelInfo.getModelNameOrPath() + "' may produce suboptimal results.");
    }
}
```

Add `reportValidity()` with the non-cpsam warning:

```java
@Override
public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
    super.reportValidity(reportContext, reportSettings, report, progressInfo);
    JIPipeInputDataSlot modelSlot = getInputSlot("Pretrained model");
    for (int row = 0; row < modelSlot.getRowCount(); row++) {
        CellposeModelData modelData = modelSlot.getData(row, CellposeModelData.class, progressInfo);
        String modelId = modelData.getModelName();
        if (modelId != null && !modelId.equals("cpsam") && !modelId.equals("None")) {
            reportContext.warning()
                    .title("Non-cpsam model for training")
                    .explanation("Only the 'cpsam' model is recommended for training. Using '" + modelId + "' may produce suboptimal results.")
                    .solution("Use the 'cpsam' model as the pretrained starting model.")
                    .report(report);
        }
    }
    // Version check (same as inference)
    Cellpose4Environment environment = getEnvironment(Cellpose4Environment.class);
    String version = CellposeVersionUtils.getInstalledVersion(environment);
    if (version != null && !CellposeVersionUtils.isModelSupported(null, version) == false) {
        // Version check for any model requiring 4.2
    }
}
```

Note: The training node uses models from the input slot, not a fixed enum. The version check focuses on whether the Cellpose version itself is >= 4.2 (since this is a 4.2+ training node). If version < 4.2, warn that this node requires Cellpose 4.2+.

Simplify the version check for training:

```java
    Cellpose4Environment environment = getEnvironment(Cellpose4Environment.class);
    String version = CellposeVersionUtils.getInstalledVersion(environment);
    if (version != null && StringUtils.compareVersions(version, "4.2") < 0) {
        List<ArtifactUpgrade> upgrades = ArtifactUpgradeUtils.findAvailableUpgrades(environment);
        JIPipeValidationReportContext context = new GraphNodeValidationReportContext(this);
        if (!upgrades.isEmpty()) {
            context.warning()
                    .title("Cellpose version may be too old")
                    .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ".")
                    .solution("Update to Cellpose 4.2 or later by clicking the 'Update Cellpose' button.")
                    .action(new JIPipeNotificationAction("Update Cellpose", "Update to a newer Cellpose version",
                            JIPipe.RESOURCES.getIcon16("actions/list-check.png"),
                            wb -> ArtifactUpgradeUtils.showUpgradeDialog((org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench) wb, upgrades)))
                    .report(report);
        } else {
            context.warning()
                    .title("Cellpose version may be too old")
                    .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ".")
                    .solution("Please install a newer Cellpose artifact manually.")
                    .report(report);
        }
    }
```

Also add a runtime guard in `runIteration()` after getting the environment:

```java
String version = CellposeVersionUtils.getInstalledVersion(environment);
if (version != null && StringUtils.compareVersions(version, "4.2") < 0) {
    List<ArtifactUpgrade> upgrades = ArtifactUpgradeUtils.findAvailableUpgrades(environment);
    GraphNodeValidationReportContext context = new GraphNodeValidationReportContext(this);
    if (!upgrades.isEmpty()) {
        throw new JIPipeValidationRuntimeException(context.error()
                .title("Cellpose version too old for training")
                .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ". Execution was aborted.")
                .solution("Update to Cellpose 4.2 or later.")
                .action(new JIPipeNotificationAction("Update Cellpose", "Update to a newer Cellpose version",
                        JIPipe.RESOURCES.getIcon16("actions/list-check.png"),
                        wb -> ArtifactUpgradeUtils.showUpgradeDialog((org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench) wb, upgrades)))
                .build());
    } else {
        throw new JIPipeValidationRuntimeException(context.error()
                .title("Cellpose version too old for training")
                .explanation("This training node requires Cellpose 4.2 or later, but the current environment uses Cellpose " + version + ". Execution was aborted.")
                .solution("Please install a newer Cellpose artifact manually.")
                .build());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl jipipe-core,plugins/jipipe-plugin-cellpose -q`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /data/src/jipipe-4 && git add plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTraining2Algorithm.java
git commit -m "Add Cellpose4SegmentationTraining2Algorithm with 4.2 defaults"
```

---

### Task 11: Register New Training Node in CellposePlugin

**Files:**
- Modify: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/CellposePlugin.java`

**Interfaces:**
- Produces: New training node registered as `cellpose-segmentation-training-4.x-v2`

- [ ] **Step 1: Register the new node type**

In `CellposePlugin.java`, after line 230 (`registerNodeType("cellpose-segmentation-training-4.x", ...)`), add:

```java
registerNodeType("cellpose-segmentation-training-4.x-v2", Cellpose4SegmentationTraining2Algorithm.class, JIPipe.RESOURCES.getIcon16URL("apps/cellpose.png"));
```

- [ ] **Step 2: Verify compilation**

Run: `cd /data/src/jipipe-4 && mvn compile -pl plugins/jipipe-plugin-cellpose -q`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /data/src/jipipe-4 && git add plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/CellposePlugin.java
git commit -m "Register Cellpose4SegmentationTraining2Algorithm node type"
```

---

### Task 12: Final Compilation and Test Run

**Files:**
- None (verification only)

- [ ] **Step 1: Compile the full project**

Run: `cd /data/src/jipipe-4 && mvn compile -q`
Expected: SUCCESS

- [ ] **Step 2: Run all tests**

Run: `cd /data/src/jipipe-4 && mvn test -q`
Expected: All tests PASS

- [ ] **Step 3: Final commit if any remaining changes**

```bash
cd /data/src/jipipe-4 && git status
# Only commit if there are uncommitted changes
```
