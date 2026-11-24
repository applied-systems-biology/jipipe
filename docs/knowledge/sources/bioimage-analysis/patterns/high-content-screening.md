---
audience: [biologist, intermediate, ai-agent]
modality: pattern
tags: [pattern, high-content-screening, plate, well, multi-site, montage]
biologicalContext: high-content-screening
prerequisiteKnowledge: [bioimage-analysis/patterns/import-split-annotate, bioimage-analysis/patterns/measurement]
---

# Pattern: High-Content Screening (Multi-Well Plates)

High-content screening involves processing multi-well plate images with many sites per well. This pattern covers the complete workflow.

## Data Organization

Typical HCS data structure:
```
plate_001/
  A01/
    site_1_DAPI.tif
    site_1_GFP.tif
    site_2_DAPI.tif
    site_2_GFP.tif
  A02/
    site_1_DAPI.tif
    ...
```

Or as a single multi-dimensional file (CZI, LIF) with positions.

## Step 1: Import and Annotate

Use **List files** or **Import folder**:
- `Folder 1` → `Well` (e.g., A01)
- `File name` → contains site and channel info

Extract site and channel from file name:
```
parts = split(File name, "_")
Site = parts[0]
Channel = parts[1]
```

Or from a single multi-position file:
- Use **Split positions** to extract individual sites as separate data items.
- Each site gets annotations for well and position.

## Step 2: Per-Site Processing

For each site:
1. Split channels (DAPI, GFP, etc.)
2. Preprocess (background subtraction, denoising)
3. Segment nuclei (DAPI → threshold → watershed → label)
4. Segment cytoplasm (GFP → Cellpose `cyto` with DAPI as nuclear marker)
5. Measure intensity in each channel within segmented objects

## Step 3: Aggregate Per Well

After measuring individual cells across all sites:

1. Merge all measurement tables.
2. Aggregate by `Well`:
   - Mean cell count per site
   - Mean intensity per well
   - Std per well
3. This gives one summary row per well.

## Step 4: Plate Visualization

1. Use the well annotations (`Well = A01`) to position data in a plate layout.
2. Use **Heatmap** plot node to visualize per-well values as a plate heatmap.
3. Rows = A-P, Columns = 1-24.

## Pattern: Positive/Negative Control Classification

1. Import control well annotations:
   ```
   Well,Type
   A01,Positive
   A02,Positive
   P23,Negative
   P24,Negative
   ```
2. Join with measurement data by `Well`.
3. Compute Z'-factor:
   ```
   Z_prime = 1 - (3 * (std_positive + std_negative)) / abs(mean_positive - mean_negative)
   ```
4. Filter plates with Z' > 0.5 (good quality).

## Pattern: Site Stitching

If sites overlap and need stitching:
1. Use **Stitch images** (from ImageJ algorithms) with position metadata.
2. Or use **Grid/collection stitching** plugin.
3. Process the stitched image as a single field.

**Caution**: stitching is memory-intensive. Consider processing sites individually and aggregating results instead.
