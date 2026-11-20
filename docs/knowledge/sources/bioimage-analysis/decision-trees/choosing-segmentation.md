---
audience: [biologist, beginner, ai-agent]
modality: decision-tree
tags: [decision-tree, segmentation, choosing-method, thresholding, deep-learning]
biologicalContext: general
prerequisiteKnowledge: [bioimage-analysis/explainers/thresholding-explained, bioimage-analysis/explainers/segmentation-explained]
---

# Decision Tree: Choosing a Segmentation Method

Use this guide to select the right segmentation approach for your data.

## Step 1: What are you segmenting?

### Nuclei (DAPI-stained, round, well-separated)

→ **Start with thresholding + watershed** (fast, no GPU needed, works well for most nuclear images)

If results are poor (irregular shapes, touching nuclei, variable intensity):
→ Try **Cellpose** with `nuclei` model

### Cytoplasm / whole cell

→ **Cellpose** with `cyto` or `cyto2` model (provides nuclear marker channel for better results)

If Cellpose results are poor or you don't have a nuclear marker:
→ Train **Ilastik pixel classifier** → threshold probability map → watershed

### Bacteria (rod-shaped, high aspect ratio)

→ **Omnipose** with `bact_omnipro` model

### Filamentous structures (neurons, vessels, actin)

→ **Filaments** plugin (dedicated filament tracing)

### Puncta / dots (small bright spots)

→ **Find maxima** (ImageJ) with appropriate noise tolerance, or **TrackMate** with LoG detector (even for single frames)

### Objects on a complex background

→ Train **Ilastik** or **Weka** classifier → threshold → watershed

## Step 2: Is the data 2D or 3D?

### 2D data

→ Any method above works. Thresholding is fastest.

### 3D data (Z-stacks)

→ **Option A**: Z-project (MIP) → segment in 2D (fast, loses Z information)
→ **Option B**: Cellpose in 3D mode (if GPU available)
→ **Option C**: 3D threshold + 3D watershed (slower, no GPU needed)
→ **Option D**: Ilastik 3D pixel classification

**Choose Z-projection (Option A)** if objects don't overlap significantly in Z.
**Choose full 3D (Options B-D)** if you need volumetric measurements or objects overlap in Z.

## Step 3: How variable is the data?

### Uniform intensity, single condition

→ Classical thresholding (Otsu auto-threshold) works well.

### Variable intensity across conditions

→ **Adaptive thresholding**: use adaptive parameters to set different thresholds per condition.
→ Or use **Cellpose** (handles intensity variation automatically).

### Very dim signal, high noise

→ Preprocess aggressively (denoise, background subtraction) → then try thresholding.
→ If still poor: Cellpose or Ilastik.

## Step 4: Do you have a GPU?

### Yes (NVIDIA GPU)

→ Cellpose/Omnipose (fast, good results for most cases).

### No

→ Thresholding + watershed (CPU-based, very fast).
→ Ilastik (CPU-based, slower than Cellpose but works).
→ Avoid Cellpose/Omnipose (will be very slow on CPU).

## Quick Reference

| Your situation | Recommended approach |
|---|---|
| Round nuclei, good contrast | Threshold (Otsu) + Watershed |
| Touching nuclei | Threshold + Watershed (adjust seed distance) |
| Cytoplasm, nuclear marker available | Cellpose `cyto` |
| Cytoplasm, no nuclear marker | Cellpose `cyto2` or Ilastik |
| Bacteria | Omnipose |
| Neurons/vessels | Filaments plugin |
| Puncta/dots | Find maxima or TrackMate |
| Complex, unusual data | Ilastik (train custom classifier) |
| 3D volumetric needed | Cellpose 3D or 3D threshold + watershed |
| No GPU, need speed | Threshold + watershed |
| Need to measure fine texture | Don't segment — use intensity metrics directly |
