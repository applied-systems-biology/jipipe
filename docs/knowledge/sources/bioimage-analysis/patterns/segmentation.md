---
audience: biologist, bioinformatician
modality: fluorescence
tags: [segmentation, nuclei, cells, fluorescence]
---

# Segmentation in Fluorescence Microscopy

## What is segmentation and why do I need it?

If you have fluorescence microscopy images — for example, nuclei stained with DAPI, or
cells expressing a fluorescent marker — you often need to identify and delineate each
individual object (nucleus, cell, organelle) so you can count them, measure their
intensity, shape, or track them over time. **Segmentation** is the process of assigning
each pixel in the image to a specific object (or to the background).

## Biological context

You might be asking questions like:
- "How many cells are in this field of view?"
- "What is the expression level of my marker in each cell?"
- "Are my cells larger or smaller after treatment?"

All of these require segmentation as a first step to separate individual objects.

## Choosing a segmentation approach

### Thresholding-based segmentation

For well-contrasted objects (bright objects on a dark background, like DAPI-stained
nuclei), a simple thresholding approach often works:

1. **Preprocess:** Apply a smoothing filter (e.g., Gaussian blur) to reduce noise.
2. **Threshold:** Separate foreground from background. Otsu's method is a good default
   that automatically finds an optimal threshold.
3. **Watershed:** If objects are touching, apply a watershed transform to split them.
4. **Label:** Assign a unique label to each connected object.
5. **Measure:** Extract measurements (area, intensity, shape) per object.

**When to use:** Objects are bright, roughly circular, and have good contrast relative
to background. Works well for DAPI nuclei, but may struggle with irregular cell shapes
or low-contrast images.

### Machine-learning-based segmentation

For more complex cases (irregular shapes, low contrast, multiple channels), a
machine-learning approach like **Cellpose** or **StarDist** can be more robust:

1. These models are trained on diverse cell images and generalize across modalities.
2. They handle irregular shapes and touching objects better than thresholding.
3. JIPipe provides nodes for Cellpose and other ML segmentation tools.

**When to use:** Objects have irregular shapes, low contrast, or you need to segment
cytoplasm (not just nuclei). Also good when you have multi-channel images and want to
segment different structures simultaneously.

## Decision guidance

| Situation | Recommended approach |
|-----------|---------------------|
| DAPI nuclei, good contrast | Thresholding + watershed |
| Cells with irregular shapes | Cellpose or StarDist |
| Touching objects | Watershed (after thresholding) or ML |
| 3D Z-stacks | 3D-aware segmentation (Cellpose 3D, or 3D watershed) |
| Low signal-to-noise | Denoise first, then ML segmentation |

## JIPipe workflow pattern

A typical cell-counting pipeline in JIPipe:

1. **Import** the image (e.g., via the image import node).
2. **Preprocess** with a Gaussian blur or median filter node.
3. **Segment** using a thresholding node or a Cellpose node.
4. **Watershed** to split touching objects (if using thresholding).
5. **Label** the segmentation result.
6. **Measure** area, mean intensity, etc. per labeled object.
7. **Export** results as a table (CSV/TSV).

Use node search to find the specific nodes for each step (e.g., search for "threshold",
"watershed", "measure", "Cellpose").

## Parameter guidance

### Thresholding

- **Otsu's method** assumes a bimodal intensity histogram (two peaks: background and
  foreground). If your image has a single broad peak, Otsu may not work well.
- **Manual thresholding** gives you full control but requires you to inspect the histogram.
- For 3D data, consider whether to threshold per-slice or on the full 3D volume.

### Watershed

- Watershed can **over-segment** (split one object into many) if the objects have
  irregular intensity. Using markers (e.g., distance-transform peaks) helps.
- The watershed seed distance parameter controls how close two seeds can be — larger
  values merge more aggressively.

### Image-based query guidance

If the user provides an example image, the LLM should:
- Assess signal-to-noise ratio (SNR): is the background truly dark?
- Check for touching objects: are there many merged blobs?
- Determine if the objects are roughly circular (good for thresholding) or irregular
  (may need ML).
