---
audience: [biologist, intermediate, ai-agent]
modality: pattern
tags: [pattern, measurement, colocalization, intensity, morphology, region-of-interest]
biologicalContext: fluorescence-microscopy
prerequisiteKnowledge: [bioimage-analysis/patterns/import-split-annotate, bioimage-analysis/patterns/cellpose-segmentation]
---

# Pattern: Measurement (Intensity, Morphology, Colocalization)

After segmentation, the next step is extracting quantitative measurements. JIPipe provides comprehensive measurement nodes.

## Intensity Measurements

### Basic Intensity in Segmented Regions

1. Connect the **original image** and the **segmentation mask** (or label image) to a **Measure intensity** node.
2. The node measures intensity statistics (mean, median, min, max, std, sum, integrated density) for each labeled object.
3. Use annotation matching (column matching on shared annotations like `Channel`, `Condition`) to pair images with their masks.

### Multi-Channel Intensity

To measure intensity in multiple channels for the same objects:

1. Segment in one channel (e.g., DAPI for nuclei).
2. Connect the segmentation mask to a Measure intensity node.
3. Connect all channel images (DAPI, GFP, RFP) as additional inputs.
4. Set column matching so all channels are matched by common annotations (e.g., `Condition`, `File name`).
5. The node measures intensity in each channel within the segmented regions.

### Background Subtraction

Before measuring intensity:
1. Use **Subtract background** (rolling ball) to remove uneven illumination.
2. Or use **Median filter** / **Gaussian blur** to estimate background, then subtract.
3. For flat-field correction, use **Divide** with an illumination correction image.

## Morphological Measurements

### Standard Morphology Features

The **Measure morphology** node extracts:
- **Area** (pixel count)
- **Perimeter**
- **Bounding box** (width, height)
- **Centroid** (X, Y)
- **Aspect ratio** (width/height)
- **Circularity** (4π × area / perimeter²)
- **Solidity** (area / convex hull area)
- **Eccentricity**
- **Feret's diameter**

### Shape Filtering

After measuring morphology, use **Filter table rows** to remove objects by shape:
```
# Remove debris (too small)
Area > 50

# Remove clumps (too large or irregular)
Area < 5000 and Circularity > 0.5

# Keep only round nuclei
Circularity > 0.8
```

## Colocalization Analysis

### Pattern: Two-Channel Colocalization

1. Import and split channels (e.g., GFP and RFP).
2. Segment regions of interest (e.g., cell outlines from GFP).
3. Measure RFP intensity within GFP-segmented regions.
4. Compute colocalization metrics:
   - **Pearson's correlation coefficient**
   - **Manders' M1 and M2 coefficients**
   - **Overlap coefficient**

JIPipe provides dedicated colocalization nodes that compute these metrics from two-channel images (optionally within a mask).

### Pattern: Object-Based Colocalization

1. Segment objects in channel A (e.g., nuclei from DAPI).
2. Segment objects in channel B (e.g., puncta from GFP).
3. Use **Match objects** or **Count objects in regions** to count how many channel B objects fall within each channel A object.
4. Output: per-nucleus puncta count.

## Region of Interest (ROI) Operations

### Creating ROIs

- **ROI from threshold**: create ROIs from thresholded regions.
- **ROI from labels**: convert a label image to individual ROIs.
- **Draw ROI**: manually draw ROIs (for interactive use).
- **Import ROI**: load ROI files (ImageJ .roi, .zip).

### ROI Measurements

- Measure intensity, morphology within ROIs.
- ROI operations: dilate, erode, fill holes, smooth.
- **Expand ROI**: grow ROIs by a specified margin (e.g., create a ring around each nucleus for cytoplasmic measurement).

### Pattern: Nuclear Ring (Perinuclear Measurement)

1. Segment nuclei from DAPI.
2. Use **Expand ROI** to grow each nucleus by N pixels.
3. Use **Subtract ROI** to remove the original nucleus from the expanded ROI.
4. This creates a ring-shaped ROI around each nucleus.
5. Measure intensity in the ring (perinuclear region).

## Pattern: Complete Measurement Pipeline

```
Import → Split channels → Annotate → 
    [Branch 1: DAPI → Threshold → Watershed → Label]
    [Branch 2: GFP (raw)]
    [Branch 3: RFP (raw)]
    → Merge (match by Condition, File name) →
    Measure intensity (labels + all channels) →
    Measure morphology (labels) →
    Filter (Area > 50, Circularity > 0.5) →
    Export results (organized by Condition)
```

## Working with Results Tables

Measurements produce `ResultsTableData`. Key operations:

- **Merge tables**: combine multiple measurement tables (matched by object ID).
- **Filter table rows**: remove unwanted objects.
- **Add computed columns**: create new columns from expressions (e.g., `N_C_ratio = GFP_nuclear / GFP_cytoplasmic`).
- **Aggregate**: compute per-condition averages, std, etc.
- **Export**: save as CSV, Excel, or JSON.

### Pattern: Per-Condition Aggregation

1. Measure individual objects → table with `Condition`, `Object_ID`, `Area`, `Intensity_mean`.
2. Use **Aggregate table** grouped by `Condition`.
3. Compute mean, std, median, count per condition.
4. Export aggregated results.
