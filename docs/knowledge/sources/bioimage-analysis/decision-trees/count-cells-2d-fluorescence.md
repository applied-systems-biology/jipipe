---
audience: biologist, bioinformatician
modality: fluorescence
tags: [decision-tree, counting, cells, 2d, fluorescence]
---

# Counting Cells in 2D Fluorescence Images

## The biological question

"I have 2D fluorescence images of cells (e.g., DAPI-stained nuclei) and I want to count
how many cells are in each image."

## Decision tree

### Step 1: Assess image quality

- **Is the background dark and the cells bright?** → Good, thresholding may work.
- **Is the signal-to-noise ratio low (noisy, low contrast)?** → Denoise first, consider
  ML segmentation.
- **Are the cells touching or overlapping?** → You'll need watershed or ML.

### Step 2: Choose segmentation method

| Image characteristic | Recommended method |
|---------------------|-------------------|
| Bright round objects, dark background, well-separated | Threshold (Otsu) + watershed |
| Bright round objects, some touching | Threshold (Otsu) + watershed |
| Irregular shapes, low contrast | Cellpose or StarDist |
| Very dense, many overlapping | ML segmentation (Cellpose) |

### Step 3: Preprocessing

1. **Convert to grayscale** if needed (or extract the relevant channel).
2. **Smooth** with a Gaussian blur (sigma ~1-2 pixels) to reduce noise.
3. **Background subtraction** if there is uneven illumination.

### Step 4: Segmentation

#### Thresholding approach
1. Apply Otsu threshold.
2. Fill holes (morphological close).
3. Apply watershed to split touching objects (use distance transform seeds).

#### ML approach (Cellpose)
1. Use the Cellpose node with an appropriate model (e.g., "nuclei" or "cyto").
2. Adjust diameter parameter to match your cell size.

### Step 5: Counting

After segmentation, count the number of labeled objects. Each unique label = one cell.

**Important:** Filter out very small objects (noise/artifacts) and very large objects
(clumps) by setting a minimum and maximum area threshold.

### Step 6: Export

Export the count (and optionally per-cell measurements) as a table.

## JIPipe workflow pattern

```
Import image → Extract channel → Gaussian blur → Threshold → Fill holes →
Watershed → Label → Measure (count, area) → Filter by area → Export table
```

Search for nodes: "import image", "split channels", "Gaussian blur", "threshold",
"fill holes", "watershed", "label", "measure", "filter table", "export table".

## Common issues

- **Over-counting:** Watershed over-segments, splitting one cell into multiple.
  Increase the minimum seed distance.
- **Under-counting:** Touching cells not separated. Ensure watershed is applied.
- **Noise counted as cells:** Set a minimum area filter.
- **Uneven illumination:** Apply flat-field correction or adaptive thresholding.
