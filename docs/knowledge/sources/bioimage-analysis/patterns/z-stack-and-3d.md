---
audience: [biologist, intermediate, ai-agent]
modality: pattern
tags: [pattern, z-stack, 3d, projection, deconvolution, volumetric]
biologicalContext: 3d-microscopy
prerequisiteKnowledge: [bioimage-analysis/patterns/import-split-annotate, bioimage-analysis/explainers/segmentation-explained]
---

# Pattern: Z-Stack and 3D Data Processing

3D microscopy data (Z-stacks) requires special handling. This pattern covers projection, 3D segmentation, and volumetric measurement.

## When to Project vs Process in 3D

### Project to 2D when:
- Objects don't overlap significantly in Z.
- You only need 2D measurements (area, 2D shape).
- Speed is important (2D is much faster).
- Segmentation works well on projected data.

### Process in 3D when:
- Objects overlap in Z and need to be separated.
- You need volumetric measurements (volume, surface area, 3D shape).
- You need to track depth or Z-position.
- 2D projection loses important information.

## Z-Projection Methods

| Method | When to use | Effect |
|---|---|---|
| Maximum intensity (MIP) | Sparse, bright objects | Shows brightest pixel per column |
| Mean intensity | Dense objects, uniform signal | Shows average pixel |
| Sum intensity | Count-based measurements | Shows total signal |
| Standard deviation | Detecting layers/structures | Shows variation |

**Caution**: MIP can merge objects that are at different Z positions but overlap in X-Y. Use 3D processing if this is a problem.

## 3D Segmentation

### Threshold + 3D Watershed

1. Preprocess (3D background subtraction, 3D denoising).
2. Apply 3D threshold.
3. 3D fill holes.
4. 3D distance transform.
5. 3D watershed (separate touching objects in 3D).
6. 3D connected components → 3D label image.

### Cellpose 3D

Cellpose can process 3D data directly:
1. Set the Cellpose node to 3D mode.
2. Provide the Z-stack (not projected).
3. Cellpose outputs a 3D label image.

**Requirements**: GPU recommended. 3D Cellpose is slower than 2D.

### 3D Object Counter

Use the **3D Object Counter** node (from the 3D plugin):
1. Input: binary mask (3D).
2. Output: 3D label image with object count.

## 3D Measurements

| Measurement | Description |
|---|---|
| Volume | Number of voxels × voxel volume |
| Surface area | Surface area of the 3D object |
| Sphericity | How spherical the object is (1 = perfect sphere) |
| Elongation | Ratio of longest to shortest axis |
| 3D centroid | Center of mass (X, Y, Z) |
| Bounding box (3D) | Width × height × depth |

## Pattern: 3D Nuclear Segation Pipeline

```
Import Z-stack → Split channels → Annotate (Channel, Z-stack info)
    → Filter (Channel == "DAPI")
    → 3D background subtraction
    → 3D Gaussian blur (sigma=1)
    → 3D threshold (Otsu)
    → 3D fill holes
    → 3D watershed
    → 3D connected components → 3D label image
    → Measure 3D morphology (volume, surface area, sphericity)
    → Measure 3D intensity
    → Export results
```

## Pattern: 2D + 3D Hybrid

Process nuclei in 3D but measure cytoplasm in 2D:
1. Segment nuclei in 3D → 3D label image.
2. Z-project the 3D label image (MIP) → 2D label image (nuclear footprints).
3. Use 2D nuclear footprints as seeds for 2D cytoplasmic segmentation (watershed from seeds).
4. Measure cytoplasm in 2D, measure nuclei in 3D.

## Deconvolution

If you have point-spread function (PSF) data:
1. Use **Deconvolution** node (from 3D plugin).
2. Input: raw Z-stack + PSF.
3. Output: deconvolved Z-stack (sharper, less blur).
4. Process deconvolved data normally.

Deconvolution improves segmentation quality for 3D data, especially for confocal microscopy.

## Z-Spacing Considerations

- **Anisotropic voxels**: Z-spacing is often larger than X-Y pixel size. This affects measurements (volume, distance).
- Set the correct pixel dimensions in the image metadata (or use a **Set voxel size** node).
- Some algorithms (watershed, distance transform) assume isotropic voxels. Resample if needed.

## Memory Management for 3D Data

3D data is much larger than 2D:
- A 1024×1024×100 Z-stack = ~100 million voxels (400 MB at 32-bit).
- Use **virtual stacks** for large data.
- Reduce parallelism (fewer threads = less memory).
- Process in tiles/blocks if needed.
- Use **Z-projection** when 3D is not necessary.
