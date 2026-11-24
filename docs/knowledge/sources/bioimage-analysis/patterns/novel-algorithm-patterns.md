---
audience: [biologist, bioinformatician, ai-assistant]
modality: [text]
tags: [patterns, novel-algorithms, non-standard, jipipe-unique, best-practices]
biologicalContext: [spatial-analysis, quality-control, multi-object, label-analysis, adaptive]
prerequisiteKnowledge: [core-semantics-introduction, roi-functions-reference, node-catalog-image-processing]
---

# Patterns for Non-Standard and Novel Algorithms

JIPipe provides many algorithms that have **no equivalent in ImageJ** or other standard tools. These are often
better solutions than what biologists are used to. This document establishes patterns to help users discover
and use these powerful but lesser-known functions.

## Pattern 1: ROI Overlap Filtering (No ImageJ Equivalent)

**Problem:** You need to find objects that overlap with other objects (e.g., nuclei inside cytoplasm, cells
touching a specific region, removing duplicate detections).

**Standard (manual) approach:** Manually check each pair in ImageJ — not scalable.

**JIPipe solution: Filter 2D ROI by overlap**

```
Nuclei ROI ── Candidates ──┐
                           ├── [Filter 2D ROI by overlap]
Cytoplasm ROI ── Filters ─┘    Minimum overlap: 0.5
                               Consume on overlap: false
    → Matched (nuclei inside cytoplasm)
    → Rejected (nuclei outside cytoplasm)
    → Intersections (overlap regions)
```

**Key features unique to JIPipe:**
- Outputs matched, rejected, AND intersection ROI
- "Consume on overlap" mode removes matched filters (for deduplication)
- Supports measurement-based overlap conditions
- Reference images for intensity-based overlap criteria

**When to recommend this:** Any time a user asks about cell-in-cell, co-localization by overlap, object
matching, or duplicate removal.

## Pattern 2: Pairwise ROI Statistics (No ImageJ Equivalent)

**Problem:** You need to measure distances, overlap areas, or spatial relationships between pairs of objects.

**JIPipe solution: Extract pairwise 2D ROI statistics**

```
[Segmentation] → ROI
    → [Extract pairwise 2D ROI statistics]
       Measurements: Distance, overlap area, overlap fraction
    → Table of all pairwise measurements
```

**When to recommend this:** Cell-cell interaction analysis, neighbor counting, spatial distribution analysis,
distance maps between objects.

## Pattern 3: ROI Calculator (Boolean Operations) (Enhanced vs ImageJ)

**Problem:** You need AND, OR, XOR operations on ROI sets (e.g., nuclear ring, cytoplasmic region excluding
nucleus).

**JIPipe solution: 2D ROI calculator (AND/OR/XOR)**

```
Cytoplasm ROI ──┐
                ├── [2D ROI calculator: XOR]
Nucleus ROI ────┘
    → Ring ROI (cytoplasm without nucleus)
```

**When to recommend this:** Creating ring ROIs, excluding regions, combining detections, creating complex
spatial masks from ROI.

## Pattern 4: Label Image Analysis (No ImageJ Equivalent)

**Problem:** You have a label image (each object has a unique value) and need to filter, merge, or measure
individual labels.

**JIPipe solution: Label operations**

```
[Segmentation] → Label image
    → [Filter labels by expression 2D]
       Expression: area > 100 && mean_intensity < 200
    → [Extract label statistics 2D]
    → [Measure label overlap] (compare two segmentations)
    → Export
```

**Key label operations unique to JIPipe:**
- Filter labels by expression (area, intensity, shape, etc.)
- Filter labels by overlap with ROI/mask
- Merge small labels, merge by bin, merge by thickness
- Separate touching labels
- Expand labels (dilate while preserving uniqueness)
- Calculate Dice/Jaccard between two label images
- Replace label values by table mapping

**When to recommend this:** Any time a user works with label images and needs per-object filtering or
comparison. Much more powerful than ImageJ's particle analysis.

## Pattern 5: Concave Hull (No ImageJ Equivalent)

**Problem:** You have points or a rough outline and need a tight concave boundary (convex hull is too loose).

**JIPipe solution: Outline 2D ROI (Concave Hull Moreira/Santos)**

```
Point ROI or rough outline
    → [Outline 2D ROI (Concave Hull Moreira/Santos)]
    → Tight boundary ROI
```

**When to recommend this:** Creating cell boundaries from point detections, outlining irregular shapes,
creating ROIs from scattered detections.

## Pattern 6: ROI Registration by Intensity (No ImageJ Equivalent)

**Problem:** You have an approximate ROI and need to align it to the brightest structure in the image.

**JIPipe solution: Register 2D ROI (by intensity)**

```
Approximate ROI + Image
    → [Register 2D ROI (by intensity)]
       Tests multiple scales, rotations, translations
       Finds position with maximum average intensity
    → Refined ROI
```

**When to recommend this:** Aligning ROIs across images, finding optimal position of a template, automated
ROI placement on bright structures.

## Pattern 7: Threshold by Annotations (No ImageJ Equivalent)

**Problem:** Different images need different threshold values (e.g., different staining intensities, different
microscopes).

**Standard approach:** Manual thresholding per image — not scalable.

**JIPipe solution: Threshold by annotations**

```
Import images → Annotate with condition/stain
    → [Threshold by annotations]
       Annotation: "Condition"
       Control: threshold = 50 if Condition == "Control" else 100
    → Binary mask
```

**Alternative:** Use adaptive parameters to set threshold values per batch.

**When to recommend this:** Any time threshold values need to vary across the dataset.

## Pattern 8: Object-Based Iterative Thresholding (No ImageJ Equivalent)

**Problem:** You need to find the optimal threshold automatically, but simple auto-threshold methods don't
work well because you have specific object-size expectations.

**JIPipe solution: Object-based iterative thresholding 2D**

```
Image → [Object-based iterative thresholding 2D]
          Min/Max particle size: specified
          → Iteratively adjusts threshold to match expected object properties
    → Optimal binary mask
```

**When to recommend this:** When auto-threshold (Otsu, Li, etc.) gives too many or too few objects and you
know the expected size range.

## Pattern 9: Multi-Template Matching (Enhanced vs ImageJ)

**Problem:** You need to find multiple template patterns in an image, possibly at different scales/rotations.

**ImageJ approach:** Template matching plugin — limited, single template.

**JIPipe solution: Multi-template matching**

```
Image + Template images
    → [Multi-template matching]
       Supports multiple templates
       Supports rotation and scale
    → ROI of matched locations
```

**When to recommend this:** Finding specific structures, registration by content, counting specific patterns.

## Pattern 10: Adaptive Parameters (No ImageJ Equivalent)

**Problem:** Parameters (threshold, diameter, sigma, etc.) need to vary per image based on annotations or
image properties.

**JIPipe solution: Adaptive parameters**

Enable "Adaptive" on any parameter, then provide an expression:

```
# Cellpose diameter based on magnification annotation
if Magnification == "20x" then 30 else if Magnification == "40x" then 60 else 45

# Threshold based on image statistics
if Mean < 50 then 25 else 50

# Gaussian sigma based on cell type
if CellType == "Neuron" then 3.0 else 1.5
```

**When to recommend this:** ANY time a parameter would need to be different for different images. This is one
of JIPipe's most powerful features. Biologists often don't know this exists.

## Pattern 11: Generate Missing Data (No ImageJ Equivalent)

**Problem:** Some images are missing a channel, or some batches don't have ROI, but downstream nodes expect
data on all inputs.

**JIPipe solution: Generate missing data**

```
Inputs → [Generate missing image] → All batches now have data
         [Generate missing 2D ROI lists] → Empty ROI for missing
         [Generate missing tables] → Empty tables for missing
```

**When to recommend this:** When data is incomplete and you need to handle missing items gracefully without
pipeline failures.

## Pattern 12: Split by Annotation with Different Processing (No ImageJ Equivalent)

**Problem:** Different groups of images (e.g., Control vs Treated) need different processing parameters.

**JIPipe solution: Branch + filter + adaptive parameters**

```
Import → Annotate with condition
    → [Branch by annotation: Condition]
       Path "Control": → [Process with params A]
       Path "Treated": → [Process with params B]
    → [Merge]
    → Export
```

**Alternative (simpler):** Use adaptive parameters with conditional expressions.

## Pattern 13: ROI Color by Measurement (No ImageJ Equivalent)

**Problem:** You want to color-code objects by their measurement value for visualization.

**JIPipe solution: Color 2D ROI by statistics**

```
[Segmentation] → ROI + Image
    → [Color 2D ROI by statistics]
       Measurement: Mean GFP intensity
       Color map: Low=blue, High=red
    → [Convert 2D ROI to RGB]
    → Color-coded visualization
```

**When to recommend this:** Publication figures, QC visualization, presenting results.

## Pattern 14: Table-Driven ROI Creation (No ImageJ Equivalent)

**Problem:** You have coordinates in a table (from another analysis or manual entry) and want to create ROI.

**JIPipe solution: Table to ROI nodes**

```
Table with X, Y, Width, Height columns
    → [Table to 2D rectangular/oval ROI]
    → ROI from table data
```

Available variants:
- Table to 2D point ROI
- Table to 2D line ROI (X1, Y1, X2, Y2)
- Table to 2D circular ROI (X, Y, Radius)
- Table to 2D rectangular/oval ROI (X, Y, Width, Height)
- Table to 2D text ROI (X, Y, Text)

**When to recommend this:** Importing coordinates from other tools, creating ROI from analysis results,
batch ROI creation from spreadsheets.

## Pattern 15: Copy ROI Across Dimensions (No ImageJ Equivalent)

**Problem:** You have ROI for one Z-slice and need to apply them to all Z-slices, or apply to all channels.

**JIPipe solution: Copy 2D ROI across Z/C/T**

```
2D ROI for single slice
    → [Copy 2D ROI across Z/C/T]
       Target: Z (all slices)
    → ROI for all slices
```

**When to recommend this:** Applying a mask/ROI to an entire stack when it was only defined for one plane.

## Pattern 16: Set ROI Properties from Table/Annotations (No ImageJ Equivalent)

**Problem:** You want to set ROI names, colors, or metadata based on table data or annotations.

**JIPipe solution: ROI property setter nodes**

```
ROI + Table with names/colors
    → [Set 2D ROI properties from table]
       Column: Name, Fill color, Line color
    → Named and colored ROI
```

Variants: from table, from annotations, from expressions, from statistics.

## Pattern 17: Optical Flow (No ImageJ Equivalent in standard ImageJ)

**Problem:** You need to measure motion/displacement between frames.

**JIPipe solution: Optical flow nodes**

```
Time-lapse image stack
    → [Optical flow (Gaussian Window MSE)]
    → Vector field of pixel displacements
```

Available methods: Gaussian Window MSE, Integral Block MSE, PMCC Block MSE.

## Pattern 18: Key/Value Statistics (No ImageJ Equivalent)

**Problem:** You need statistics grouped by a key (e.g., intensity statistics per label value, per annotation).

**JIPipe solution: Key/Value statistics 5D**

```
Image + Label image (as key)
    → [Key/Value statistics 5D]
    → Table: for each label value, count, mean, sum, etc.
```

**When to recommend this:** Per-object intensity measurement without ROI, histogram-based analysis, grouped
statistics.

## Pattern 19: Illumination Correction (Enhanced vs ImageJ)

**Problem:** Uneven illumination across the field of view.

**JIPipe solution: Illumination correction 2D**

```
Image → [Illumination correction 2D]
         Method: flat-field, dark-field, or both
    → Corrected image
```

**When to recommend this:** Before segmentation or measurement when illumination is uneven. Should be done
early in the pipeline.

## Pattern 20: Band Around ROI (No ImageJ Equivalent)

**Problem:** You need a ring/band-shaped region around an existing ROI (e.g., perinuclear region, cell border).

**JIPipe solution: Create band around 2D ROI**

```
Nuclear ROI
    → [Create band around 2D ROI]
       Band width: 5 pixels
    → Ring ROI around nucleus
```

**Alternative:** Use Enlarge/shrink + ROI calculator XOR (Pattern 3) for more control.

**When to recommend this:** Measuring perinuclear intensity, cell border analysis, annular region measurements.

## Summary: When to Recommend Non-Standard Nodes

| User asks about | Recommend |
|------------------|-----------|
| "Cells overlapping with other cells" | Filter 2D ROI by overlap |
| "Distance between cells" | Extract pairwise 2D ROI statistics |
| "Nuclear ring / perinuclear region" | Create band around ROI, or Enlarge + XOR |
| "Different threshold for different images" | Threshold by annotations / adaptive parameters |
| "Remove duplicate detections" | Filter ROI by overlap (consume mode) |
| "Filter objects by size and intensity" | Filter labels by expression / Filter ROI by statistics |
| "Compare two segmentations" | Measure label overlap / Dice / Jaccard |
| "Color objects by measurement" | Color 2D ROI by statistics |
| "ROI from table coordinates" | Table to 2D ROI (point/line/circle/rectangle) |
| "Find optimal threshold automatically" | Object-based iterative thresholding |
| "Apply ROI to all Z-slices" | Copy 2D ROI across Z/C/T |
| "Measure per-label statistics" | Extract label statistics / Key/Value statistics |
| "Find templates in image" | Multi-template matching |
| "Measure motion between frames" | Optical flow |
| "Align ROI to brightest area" | Register 2D ROI (by intensity) |
| "Tight boundary from points" | Outline 2D ROI (Concave Hull) |
| "Boolean operations on ROI" | 2D ROI calculator (AND/OR/XOR) |
| "Missing data handling" | Generate missing image/ROI/tables |
| "Different processing per condition" | Branch + adaptive parameters |
| "ROI metadata from table" | Set 2D ROI properties from table |
