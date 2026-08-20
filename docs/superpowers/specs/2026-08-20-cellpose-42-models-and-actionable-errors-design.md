# Cellpose 4.2 Model Support & Actionable Error System (#1310)

## Overview

This spec addresses GitLab work item #1310: adding support for the new Cellpose 4.2 models (cpsam_v2, cpdino, cpdino-vitb) into JIPipe, extending the error/validation system to support clickable action buttons, and enabling users to auto-update their Cellpose environment from within error messages.

## Scope

Five parts:
1. **Error system extension** — Add `JIPipeNotificationAction` support to `JIPipeValidationReportEntry` and render action buttons in the error UI
2. **Cellpose 4 model updates** — Add new models, change default to `cpsam_v2`, add conditional `bsize` parameter for DINO models
3. **Training node update** — New training node with Cellpose 4.2 defaults; old node deprecated (not hidden)
4. **Version check & auto-update** — Hybrid validation warning + runtime guard with actionable "Update Cellpose" button
5. **Registration** — Register new training node type; no project upgrade logic needed (JIPipe 6 is unreleased)

## Part 1: Error System Extension

### Motivation

The current error system has two parallel mechanisms: `JIPipeValidationReportEntry` (structured, pre-execution validation, no custom actions) and `JIPipeNotification` (runtime, supports `JIPipeNotificationAction` with `Consumer<JIPipeWorkbench>`). This work unifies them by allowing validation entries to carry action buttons.

### Changes to `JIPipeValidationReportEntry`

**File**: `jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportEntry.java`

Add a `List<JIPipeNotificationAction>` field:

```java
private List<JIPipeNotificationAction> actions = new ArrayList<>();
```

New constructor overloads that accept actions. Existing constructors default to an empty list (backward compatible). Add `getActions()` getter.

### Changes to `JIPipeValidationRuntimeException`

**File**: `jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationRuntimeException.java`

Add an actions field that is carried through the exception chain. When a `JIPipeValidationReportEntry` with actions is wrapped into a runtime exception, the actions are preserved.

### Changes to `JIPipeValidationReportContext`

**File**: `jipipe-core/src/main/java/org/hkijena/jipipe/api/validation/JIPipeValidationReportContext.java`

Add a fluent builder method `.action(JIPipeNotificationAction)` to the `ValidationEntryBuilder` inner class, parallel to the existing `.title()`, `.explanation()`, `.solution()` methods. This allows attaching actions when building an entry:

```java
context.error()
    .title("Cellpose version too old")
    .explanation("...")
    .solution("...")
    .action(new JIPipeNotificationAction("Update Cellpose", ...))
    .report(report);
```

### Changes to `JIPipeDesktopUserFriendlyErrorUI`

**File**: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/validation/JIPipeDesktopUserFriendlyErrorUI.java`

In `addEntry()`, after rendering the existing "Go to" button and before "Show details", iterate the entry's actions list and render one button per action. Each button:
- Uses `action.getLabel()` as text, `action.getIcon()` as icon
- On click: calls `action.getAction().accept(getDesktopWorkbench())`
- If `action.isDismiss()` is true, no special handling needed (validation entries are not dismissible; the dismiss flag only affects notifications)

### Reusable Upgrade Dialog

**File**: New utility class (e.g., `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/project/ArtifactUpgradeUtils.java`)

Extract the `ArtifactUpgrade` inner class from `JIPipeDesktopProjectOverviewUI` into a standalone class (e.g., `ArtifactUpgrade` in the same package). Then extract the upgrade dialog logic from `JIPipeDesktopProjectOverviewUI.upgradeArtifacts()` into a reusable static method:

```java
public static void showUpgradeDialog(JIPipeDesktopWorkbench workbench, List<ArtifactUpgrade> upgrades)
```

This method shows the same dialog with version combo boxes and writes the updated `JIPipeArtifactQueryParameter` into the environment. The Project Overview UI calls this extracted method instead of its own implementation.

## Part 2: Cellpose 4 Model Updates

### Model Enum

**File**: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/parameters/cp4/PretrainedCellpose4SegmentationModel.java`

Add new models and change default ordering:

```java
public enum PretrainedCellpose4SegmentationModel {
    cpsam_v2("cpsam_v2", "Cellpose SAM v2"),
    cpdino("cpdino", "Cellpose DINO (ViT-L)"),
    cpdino_vitb("cpdino-vitb", "Cellpose DINO (ViT-B)"),
    cpsam("cpsam", "Cellpose SAM"),
    None(null, "None (only training)");
```

`cpsam_v2` is the first constant and thus the default for new nodes. Existing projects keep their saved model selection.

Model availability by Cellpose version:
- Cellpose < 4.2: only `cpsam` and `None` are available
- Cellpose >= 4.2: all five models are available

### Conditional bsize Parameter

**File**: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationInferenceAlgorithm.java`

Add an `OptionalInteger` parameter for tile size:

```java
@JIPipeParameter(value = "bsize", uiOrder = 50)
@SetJIPipeDocumentation(name = "Tile size (DINO models only)", description = ...)
public OptionalInteger getBsize() { ... }
```

Default value: 384 (enabled).

Visibility: Override `isParameterUIVisible()` to show `bsize` only when a DINO model (`cpdino` or `cpdino_vitb`) is selected. Hidden for SAM models (`cpsam`, `cpsam_v2`).

CLI argument generation in `runCellpose()`:
- When a DINO model is selected: add `--bsize` with the user value (or 384 if enabled, no flag if disabled)
- When a SAM model is selected: do not pass `--bsize` (Cellpose uses its fixed 256)

### Import Algorithm

**File**: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/ImportPretrainedCellpose4SegmentationModelAlgorithm.java`

No changes needed — it iterates the enum, so new models are included automatically.

## Part 3: Training Node Update

### New Node: `Cellpose4SegmentationTraining2Algorithm`

**File**: New file at `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTraining2Algorithm.java`

Based on the existing `Cellpose4SegmentationTrainingAlgorithm` with updated defaults and CLI:

| Parameter | Old default | New default |
|-----------|------------|-------------|
| learning_rate | 0.2 | 0.00001 (1e-5) |
| weight_decay | 1e-05 | 0.1 |
| n_epochs | 500 | 100 |
| batch_size | 8 | 1 |

New CLI parameters added:
- `--bsize` (tile size, default 256 for training)
- `--save_every` (epochs between saves, default 100)
- `--model_name_out` (output model name, optional)

Deprecated CLI parameters not passed by the new node:
- `--diam_mean` (deprecated in Cellpose 4.0.1+)
- `--train_size` (deprecated in Cellpose 4.0.1+)
- `--SGD` (deprecated in Cellpose 4.0.1+)
- `--chan`, `--chan2`, `--all_channels`, `--invert` (deprecated in Cellpose 4.0.1+)

### Training Model Restriction

The Cellpose 4.2 training docs state: "You should only start training with the built-in cpsam model."

Enforcement in the new training node:
1. **Validation** (`reportValidity()`): If a non-`cpsam` model is selected as the pretrained starting model, generate a Warning-level validation entry explaining that only `cpsam` is recommended for training.
2. **Runtime** (in `processModel()` or `run()`): If a non-`cpsam` model is detected, call `progressInfo.aggressiveError()` with a message stating that only `cpsam` is recommended and training results may be suboptimal. Execution continues (not blocking).

### Old Node: `Cellpose4SegmentationTrainingAlgorithm`

**File**: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/algorithms/cp4/Cellpose4SegmentationTrainingAlgorithm.java`

- Annotate with `@Deprecated`
- Rename node display name to `"Cellpose segmentation training 4.x (old)"` via `@SetJIPipeDocumentation(name = ...)`
- Not hidden — existing workflows remain functional
- No changes to its behavior or parameters

## Part 4: Version Check & Auto-Update

### Approach: Hybrid (validation warning + runtime guard)

Validation may be skipped in some cases, so a runtime guard is also needed.

### Version Detection Utility

**File**: New file at `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/utils/CellposeVersionUtils.java`

```java
public class CellposeVersionUtils {
    // Parses version from the Cellpose4Environment's artifact query
    // e.g., "com.github.mouseland.cellpose4:4.2.1.1.1000-*" -> "4.2.1.1.1000"
    // Returns null for non-artifact (custom) environments
    public static String getInstalledVersion(Cellpose4Environment env);

    // Returns true if the model is available in the given Cellpose version
    // cpsam, None: always available
    // cpsam_v2, cpdino, cpdino_vitb: require version >= 4.2
    public static boolean isModelSupported(String modelId, String version);

    // Returns true if the model requires Cellpose >= 4.2
    public static boolean requiresCellpose42(String modelId);

    // Collects available artifact upgrades for the given environment
    // Reuses the same logic as JIPipeDesktopProjectOverviewUI.createArtifactUpgradeTipsIfNeeded()
    public static List<ArtifactUpgrade> findAvailableUpgrades(Cellpose4Environment env);
}
```

Version comparison uses `StringUtils.compareVersions()` (already used elsewhere in the codebase).

### Validation-Time Warning (non-blocking)

In `Cellpose4SegmentationInferenceAlgorithm.reportValidity()` and `Cellpose4SegmentationTraining2Algorithm.reportValidity()`:

1. Get the Cellpose4Environment from the algorithm's environment parameter
2. If the environment is artifact-based, parse the version
3. If version < 4.2 and a model requiring >= 4.2 (`cpsam_v2`, `cpdino`, `cpdino_vitb`) is selected:
   - Find available upgrades via `CellposeVersionUtils.findAvailableUpgrades()`
   - If upgrades are available, create a `JIPipeNotificationAction("Update Cellpose")` whose action calls `ArtifactUpgradeUtils.showUpgradeDialog(workbench, upgrades)`
   - Add a Warning-level entry:
     - Title: "Cellpose version may be too old"
     - Explanation: "The selected model '{modelId}' requires Cellpose 4.2 or later, but the current environment uses Cellpose {version}."
     - Solution: "Update to Cellpose 4.2 or later by clicking the 'Update Cellpose' button."
     - Action: the `JIPipeNotificationAction` created above
   - If no upgrades are available, the warning is still added but without the action button, and the solution text says "Please install a newer Cellpose artifact manually."

### Runtime Guard (blocking)

In the algorithm's execution path (before invoking Cellpose via `runCellpose()` / `processModel()`):

1. Re-check the version (same logic as validation)
2. If version < 4.2 and a >= 4.2 model is selected:
   - Find available upgrades
   - Throw `JIPipeValidationRuntimeException` with `GraphNodeValidationReportContext` (navigable — enables "Go to" button)
   - Attach actions (including "Update Cellpose" if upgrades are available)
   - Title: "Cellpose version too old for selected model"
   - Explanation: "The selected model '{modelId}' requires Cellpose 4.2 or later, but the current environment uses Cellpose {version}. Execution was aborted."
   - Solution: "Update to Cellpose 4.2 or later."
   - This prevents wasted execution when validation was skipped

### Custom Environments

If the Cellpose4Environment is not artifact-based (custom environment), version checking is skipped entirely. The user is assumed to know what they are doing.

## Part 5: Registration

### `CellposePlugin.register()`

**File**: `plugins/jipipe-plugin-cellpose/src/main/java/org/hkijena/jipipe/plugins/cellpose/CellposePlugin.java`

- Register new training node:
  ```java
  registerNodeType("cellpose-segmentation-training-4.x-v2",
      Cellpose4SegmentationTraining2Algorithm.class, ...);
  ```
- Existing training node registration stays unchanged (deprecated, not hidden)
- No new inference node — the existing `Cellpose4SegmentationInferenceAlgorithm` is updated in-place

### Project Upgrade

No project upgrade logic is needed. Cellpose 4 is a JIPipe 6 feature, and JIPipe 6 is not yet released. No existing JIPipe 5 projects have Cellpose 4 nodes to upgrade.

## Files Changed

### jipipe-core (5 files modified, 2 new files)

| File | Change |
|------|--------|
| `api/validation/JIPipeValidationReportEntry.java` | Add `List<JIPipeNotificationAction>` field, constructors, getter |
| `api/validation/JIPipeValidationRuntimeException.java` | Add actions field, carry through exception chain |
| `api/validation/JIPipeValidationReportContext.java` | Add `.action()` to `ValidationEntryBuilder` |
| `desktop/commons/components/validation/JIPipeDesktopUserFriendlyErrorUI.java` | Render action buttons in `addEntry()` |
| `desktop/app/settings/JIPipeDesktopProjectOverviewUI.java` | Replace `upgradeArtifacts()` with call to extracted utility; remove `ArtifactUpgrade` inner class |
| `desktop/commons/components/project/ArtifactUpgradeUtils.java` (new) | Extracted reusable upgrade dialog method |
| `desktop/commons/components/project/ArtifactUpgrade.java` (new) | Extracted standalone data class |

### jipipe-plugin-cellpose (4 files modified, 2 new files)

| File | Change |
|------|--------|
| `parameters/cp4/PretrainedCellpose4SegmentationModel.java` | Add `cpsam_v2`, `cpdino`, `cpdino_vitb`; reorder |
| `algorithms/cp4/Cellpose4SegmentationInferenceAlgorithm.java` | Add `bsize` param; add validation warning + runtime guard; pass `--bsize` for DINO |
| `algorithms/cp4/Cellpose4SegmentationTrainingAlgorithm.java` | `@Deprecated`, rename to "(old)" |
| `CellposePlugin.java` | Register new training node type |
| `algorithms/cp4/Cellpose4SegmentationTraining2Algorithm.java` (new) | New training node with 4.2 defaults |
| `utils/CellposeVersionUtils.java` (new) | Version parsing and model compatibility checks |
