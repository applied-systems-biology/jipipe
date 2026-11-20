---
audience: [biologist, bioinformatician]
modality: [text]
tags: [cellpose, deep-learning, segmentation, omnipose, cellpose-3, cellpose-4, cellpose-2]
biologicalContext: [cell-segmentation, nucleus-segmentation, bacteria-segmentation, tissue-segmentation]
prerequisiteKnowledge: [import-split-annotate, segmentation-explained]
---

# Cellpose / Omnipose Segmentation

JIPipe provides dedicated integration for Cellpose (versions 1, 2, 3, 4) and Omnipose. Each version has its own
node set and Python environment. This document covers model selection, version differences, inference settings,
training workflows, and troubleshooting.

## Version Overview

| Version | Environment | Key models | Strengths | Limitations |
|---------|-------------|------------|-----------|-------------|
| **Cellpose 2.x** | `cellpose2` | cyto, cyto2, nuclei, tissuenet, livecell, LC1-LC4, TN1-TN3, CP, CPx, general | Mature, well-tested, many pretrained models | Fewer cells detected in base config; older architecture |
| **Cellpose 3.x** | `cellpose3` | cyto3, nuclei, cyto2_cp3, tissuenet_cp3, livecell_cp3, yeast_PhC_cp3, yeast_BF_cp3, bact_phase_cp3, bact_fluor_cp3, deepbacs_cp3, transformer_cp3, neurips_cellpose_default, neurips_cellpose_transformer, neurips_grayscale_cyto2, CP, CPx, TN1-TN3, LC1-LC4 | Easier to train, lighter on resources, many specialized models (yeast, bacteria), transformer model available, also supports denoising models | Fewer cells in base configuration vs v4 |
| **Cellpose 4.x** | `cellpose4` | cpsam (Cellpose SAM), None | MicroSAM-based — sees more cells in base configuration, better generalization out of the box | Harder to train (larger model), fewer pretrained model variants, heavier resource requirements |
| **Omnipose 0.x** | `omnipose0` | bact_phase, bact_fluor, cyto2, nuclei | Specialized for elongated/thin objects (bacteria, filaments) | Older codebase, limited model variety |

### When to use which version

- **Default for new projects: Cellpose 3.x** — best balance of ease of use, training, and resource requirements.
- **When you need maximum out-of-the-box detection: Cellpose 4.x** — the MicroSAM-based `cpsam` model detects more
  cells than v3's base models without training. Best when you don't have training data.
- **When you have training data and want fine control: Cellpose 3.x** — training is well-supported, lighter, and
  faster than v4 training.
- **For bacteria or elongated objects: Omnipose 0.x** — specifically designed for non-convex, thin, and elongated
  shapes that Cellpose struggles with.
- **Legacy compatibility: Cellpose 2.x** — use if you have existing v2 models or need the older pretrained model set.

## Node Inventory

### Cellpose 2.x nodes
- **Cellpose segmentation (2.x)** — `cellpose-inference-2.x` — Inference with v2 models
- **Cellpose training (2.x)** — `cellpose-training-2.x` — Train a v2 model
- **Import pretrained Cellpose 2.x model** — `import-cellpose-2.x-pretrained-model`
- **Import Cellpose model** — `import-cellpose-model-v2` (from file)
- **Import Cellpose size model** — `import-cellpose-size-model-v2` (from file)

### Cellpose 3.x nodes
- **Cellpose segmentation (3.x)** — `cellpose-segmentation-inference-3.x` — Inference with v3 segmentation models
- **Cellpose denoise (3.x)** — `cellpose-denoise-inference-3.x` — Image denoising/restoration
- **Cellpose segmentation training (3.x)** — `cellpose-segmentation-training-3.x`
- **Cellpose denoise training (3.x)** — `cellpose-denoise-training-3.x`
- **Import pretrained Cellpose 3.x segmentation model** — `import-cellpose-3.x-pretrained-segmentation-model`
- **Import pretrained Cellpose 3.x denoise model** — `import-cellpose-3.x-pretrained-denoise-model`

### Cellpose 4.x nodes
- **Cellpose segmentation (4.x)** — `cellpose-segmentation-inference-4.x` — Inference with v4 (MicroSAM) models
- **Cellpose segmentation training (4.x)** — `cellpose-segmentation-training-4.x`
- **Import pretrained Cellpose 4.x segmentation model** — `import-cellpose-4.x-pretrained-segmentation-model`

### Omnipose 0.x nodes
- **Omnipose inference** — Inference with Omnipose models
- **Omnipose training** — Train an Omnipose model
- **Import pretrained Omnipose model**

### Legacy nodes (deprecated, but still available)
- `cellpose` — Cellpose 1.x inference (use 2.x+ instead)
- `cellpose-2` — Legacy Cellpose 2.x inference (use modern `cellpose-inference-2.x` instead)
- `cellpose-training`, `cellpose-training-2` — Legacy training nodes
- `import-cellpose-model`, `import-cellpose-size-model` — Legacy model import

## Environment Setup

Each Cellpose version requires its own Python environment. JIPipe manages these via **artifact environments**:

1. Go to **Project → Environments** (or **Manage environments** in the node settings)
2. Add a new environment of type `Cellpose 3.x` (or the version you need)
3. JIPipe will automatically install the correct Python package from the configured artifact repository
4. In the inference/training node, select this environment in the "Environment" parameter

**GPU settings** are configured per-node:
- Enable/disable GPU
- Select GPU device index
- If no GPU is available, CPU inference works but is much slower

## Inference: Channel Configuration

Cellpose expects 1 or 2 channels:
- **Channel 1 (cytoplasm/target):** The channel to segment
- **Channel 2 (optional nucleus):** A nuclear stain (e.g., DAPI) that helps guide segmentation

Common configurations:
| Sample type | Channel 1 | Channel 2 | Notes |
|-------------|-----------|-----------|-------|
| Nuclei only | DAPI/greyscale | None | Use `nuclei` model |
| Cytoplasm + nucleus | GFP/cytoplasmic | DAPI | Use `cyto`/`cyto2`/`cyto3` model |
| Cytoplasm only | GFP/cytoplasmic | None | Use `cyto` model (less accurate without nucleus) |
| Bacteria (phase) | Phase contrast | None | Use `bact_phase` model |
| Bacteria (fluorescence) | Fluorescence | None | Use `bact_fluor` model |
| Yeast (phase) | Phase contrast | None | Use `yeast_PhC` model |
| Yeast (brightfield) | Brightfield | None | Use `yeast_BF` model |

## Inference: Diameter

The **diameter** parameter is critical. It tells Cellpose the expected object diameter in pixels.

- **If too small:** Objects are over-split (one cell becomes many fragments)
- **If too large:** Objects are merged (multiple cells become one)
- **Auto-estimate:** Cellpose can estimate diameter automatically using the size model (for cyto/cyto2 models)
- **Adaptive diameter:** Use JIPipe's adaptive parameters to set diameter per-image based on annotations (e.g.,
  different magnifications, different cell types)

```
# Adaptive diameter expression (if annotation "Magnification" exists):
if Magnification == "20x" then 30 else if Magnification == "40x" then 60 else 45
```

## Inference: Outputs

Cellpose inference nodes can generate multiple outputs (enable in the "Outputs" parameter section):

| Output | Description | Use case |
|--------|-------------|----------|
| **ROI** (default) | ROI of segmented areas | Most common — use for measurement |
| **Labels** | Grayscale image where each object has a unique value | For downstream label-based processing |
| **Flows XY** | RGB image showing x/y flow per pixel | Debugging segmentation quality |
| **Flows Z** | Z-direction flows (black for 2D) | 3D segmentation analysis |
| **Flows d** | Multi-channel flow image [dZ/dY/dX, cell prob] | Advanced analysis |
| **Probabilities** | Cell probability per pixel | Thresholding, QC |

## 3D Segmentation

All modern Cellpose inference nodes support 3D segmentation:
- Enable "3D segmentation" in the node parameters
- The node processes the entire Z-stack as a 3D volume
- Diameter refers to XY diameter; Z-scaling is handled automatically if physical dimensions are set

## Model Selection Quick Reference

| What you're segmenting | Recommended model | Version |
|------------------------|-------------------|---------|
| Nuclei (fluorescence) | `nuclei` | 2.x, 3.x, 4.x |
| Cytoplasm + nucleus | `cyto3` or `cpsam` | 3.x, 4.x |
| Cytoplasm only | `cyto2` or `cyto3` | 2.x, 3.x |
| Tissue (brightfield/IF) | `tissuenet` / `tissuenet_cp3` | 2.x, 3.x |
| Live cells | `livecell` / `livecell_cp3` / LC1-LC4 | 2.x, 3.x |
| Bacteria (phase) | `bact_phase_cp3` or Omnipose | 3.x / Omnipose |
| Bacteria (fluorescence) | `bact_fluor_cp3` or `deepbacs_cp3` | 3.x |
| Yeast (phase) | `yeast_PhC_cp3` | 3.x |
| Yeast (brightfield) | `yeast_BF_cp3` | 3.x |
| General/unknown | `cpsam` (Cellpose SAM) | 4.x |
| Elongated/thin objects | Omnipose | Omnipose |

## Common Patterns

### Pattern 1: Basic nuclear segmentation with Cellpose 3

```
Import images → Split channels → [Cellpose 3.x segmentation]
                                 Model: nuclei
                                 Channel 1: DAPI
                                 Diameter: 30 (or auto)
                                 Output: ROI
    → Extract ROI statistics → Export
```

### Pattern 2: Cytoplasmic segmentation with nuclear guidance

```
Import images → Split channels → [Cellpose 3.x segmentation]
                                 Model: cyto3
                                 Channel 1: GFP (cytoplasm)
                                 Channel 2: DAPI (nucleus)
                                 Diameter: 60
    → Extract ROI statistics → Export
```

### Pattern 3: Adaptive diameter for mixed magnification datasets

```
Import images → Annotate with magnification → Split channels
    → [Cellpose 3.x segmentation]
       Model: nuclei
       Diameter: ADAPTIVE (expression: if Magnification == "20x" then 30 else 60)
    → Extract ROI statistics → Export
```

### Pattern 4: Bacteria with Omnipose

```
Import images → [Omnipose inference]
                 Model: bact_phase
                 Channel 1: Phase contrast
                 Diameter: 15
    → Filter ROI by statistics (remove too-small objects)
    → Extract ROI statistics → Export
```

### Pattern 5: Cellpose 4 for maximum detection without training

```
Import images → Split channels → [Cellpose 4.x segmentation]
                                 Model: cpsam
                                 Channel 1: DAPI
                                 Channel 2: (none)
    → Extract ROI statistics → Export
```

## Troubleshooting

| Problem | Likely cause | Solution |
|---------|-------------|----------|
| No objects detected | Wrong channel selected | Verify channel assignment; Cellpose expects greyscale |
| No objects detected | Diameter too large | Reduce diameter; try auto-estimate |
| Too many fragments | Diameter too small | Increase diameter |
| Objects merged | Diameter too large | Decrease diameter |
| Objects merged | Flow threshold too low | Increase flow threshold |
| Cell count much lower than expected | Using v2/v3 base model on hard data | Try Cellpose 4 (cpsam) or train a custom model |
| Very slow | CPU only | Enable GPU; reduce image size |
| Out of memory | Image too large for GPU | Split into tiles, reduce Z-depth, use 2D mode |
| Error: environment not found | Cellpose not installed | Set up the environment in Project → Environments |
| ROI output is empty but labels look fine | Output not enabled | Enable "ROI" output in the Outputs section |
