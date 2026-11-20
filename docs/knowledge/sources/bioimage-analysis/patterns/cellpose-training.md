---
audience: [biologist, bioinformatician]
modality: [text]
tags: [cellpose, deep-learning, training, model-training, annotation, omnipose]
biologicalContext: [cell-segmentation, custom-model, fine-tuning]
prerequisiteKnowledge: [cellpose-segmentation, annotation-management, import-split-annotate]
---

# Cellpose / Omnipose Model Training

When pretrained Cellpose models don't perform well enough on your data, you can train a custom model within JIPipe.
This document covers the complete training workflow, including data annotation setup, training set splitting, and
training parameter configuration.

## Training Node Inventory

| Node | ID | Description |
|------|----|-------------|
| Cellpose training (2.x) | `cellpose-training-2.x` | Train a Cellpose 2.x model |
| Cellpose segmentation training (3.x) | `cellpose-segmentation-training-3.x` | Train a Cellpose 3.x segmentation model |
| Cellpose denoise training (3.x) | `cellpose-denoise-training-3.x` | Train a Cellpose 3.x denoising/restoration model |
| Cellpose segmentation training (4.x) | `cellpose-segmentation-training-4.x` | Train a Cellpose 4.x model (MicroSAM-based) |
| Omnipose training | — | Train an Omnipose model |

## Version-Specific Training Considerations

### Cellpose 3.x Training (Recommended for most cases)
- **Easier to train** than v4 — smaller model, faster convergence, less data needed
- Supports training from scratch (model = None) or fine-tuning from a pretrained model
- Supports both segmentation and denoising model training
- Lighter on GPU resources

### Cellpose 4.x Training
- **MicroSAM-based** — larger, more complex model
- **Harder to train** — requires more data, more GPU memory, longer training times
- Best starting point is the `cpsam` pretrained model (fine-tuning)
- Training from scratch is not recommended for v4
- When training is not practical, use v4 inference with the pretrained `cpsam` model — it often performs well
  out of the box

### Cellpose 2.x Training
- Mature, well-documented training pipeline
- Supports size model training (for automatic diameter estimation)
- Good option if you need compatibility with older workflows

## Complete Training Workflow

### Step 1: Prepare Training Data

You need **paired** raw images and label (ground truth) images:

1. **Raw images:** Your original microscopy images (2D or 3D)
2. **Label images:** 16-bit greyscale images where each object has a unique pixel value (1, 2, 3, ...)
   - Background must be 0
   - Each connected component must have a unique label

### Step 2: Create Ground Truth Annotations (Manual Segmentation)

The most critical step. Users need to manually segment training data using interactive tools:

#### Option A: ImageJ manual annotation (recommended)

1. Open your raw image in ImageJ/Fiji
2. Use the ROI tools (oval, polygon, freehand) to manually outline each object
3. Add each ROI to the ROI Manager (`Analyze → Tools → ROI Manager → Add` or `T`)
4. Save the ROI set: `ROI Manager → More → Save` → saves as `.roi` or `.zip`
5. In JIPipe, use **ROI to Labels (by name)** or **2D ROI to Labels (Expression)** to convert ROI to label images
6. Or: Use **Draw/modify mask (interactive)** in JIPipe's Forms plugin for direct mask drawing

#### Option B: JIPipe interactive annotation

1. Use the **Draw/modify mask (interactive)** node in JIPipe
2. Draw masks directly on the image
3. Convert masks to labels using **Connected components labeling 2D**

#### Option C: Import existing annotations from other tools

- If you have label images from Cellpose GUI, QuPath, or other tools → import them directly
- Use **Import image** node with the label images
- Ensure they are 16-bit greyscale with unique values per object

### Step 3: Pair Raw and Label Images

Use the **Annotate with data** node to attach label images as data annotations to the raw images:

```
Raw images ──┐
             ├── [Annotate with data]
Label images ─┘   Annotation name: "Labels"
                   Query: match by file name or annotation
    → Output: raw images with "Labels" data annotation attached
```

The training node automatically looks for a data annotation containing the label image. It will also
automatically ensure that all connected components in the label image are assigned unique values (this can
be disabled in the parameters).

### Step 4: Split into Training and Test Sets

The training node has two input slots:
- **Training data:** Images used to train the model
- **Test data** (optional): Images used to evaluate the model during training

**Recommended approach — randomized splitting:**

1. Import all annotated images
2. Use **Annotate with index** or **Add identity field at end** to assign a random number
3. Use **Filter by annotation (expression)** to split:
   ```
   # Training set (80%)
   index % 10 < 8
   
   # Test set (20%)
   index % 10 >= 8
   ```
4. Connect training set to "Training data" and test set to "Test data"

**Alternative:** Use JIPipe's **Branch** node to create two paths, then filter each.

### Step 5: Configure Training Parameters

| Parameter | Description | Typical value |
|-----------|-------------|---------------|
| Pretrained model | Starting model (None = from scratch, or a pretrained model) | cyto3 (for fine-tuning) |
| Number of epochs | Training iterations | 500 (v2/v3), 100-200 (v4) |
| Diameter | Object diameter in pixels | 30 (nuclei), 60 (cytoplasm) |
| 3D segmentation | Enable for 3D data | True/False |
| Train size model | Also train a size model for auto-diameter | True (v2 only) |
| Clean up afterwards | Remove temporary training files | True |
| GPU settings | Enable GPU, select device | GPU 0 |

**Channel settings** (same as inference):
- Channel 1: Target channel
- Channel 2: Optional nuclear channel

### Step 6: Run Training

1. Connect everything:
   ```
   Annotated training images → [Cellpose 3.x segmentation training]
   Annotated test images    →     Model: cyto3 (fine-tune)
   Pretrained model (opt)   →     Epochs: 500
                                    Diameter: 30
   ```
2. The node outputs:
   - **Model:** The trained Cellpose model (can be used directly in inference nodes)
   - **Size model** (v2 only): For automatic diameter estimation

### Step 7: Use the Trained Model

1. Connect the "Model" output to a Cellpose inference node's "Pretrained model" input
2. Or: Use **Import Cellpose model from file** to load the saved model in other pipelines

```
[Training node] → Model → [Cellpose 3.x segmentation inference]
                            Pretrained model: from input
                            Channel 1: DAPI
                            Diameter: 30
```

## Training Data Requirements

| Factor | Minimum | Recommended |
|--------|---------|-------------|
| Number of images | 10 | 50-100+ |
| Objects per image | 10 | 50+ |
| Diversity | Single condition | Multiple conditions, magnifications |
| Label quality | Rough outlines | Precise boundaries |
| Image size | Any | Consistent sizes help |

## Fine-Tuning vs. Training from Scratch

| Approach | When to use | Model setting |
|----------|-------------|---------------|
| **Fine-tuning** (recommended) | When a pretrained model is close but not perfect | Set model to cyto3/nuclei/cpsam |
| **Training from scratch** | When no pretrained model exists for your data type | Set model to "None" |

Fine-tuning is almost always better — it starts from learned features and adapts them to your data.
Training from scratch requires significantly more data and training time.

## Cellpose 3.x Denoise Training

Cellpose 3.x can also train **denoising/restoration models** (not segmentation):

1. Input: noisy images + clean images (as data annotation)
2. The model learns to remove noise/artifacts
3. Use **Cellpose denoise (3.x)** for inference

Noise types supported:
- `normal` — Gaussian noise
- `poisson` — Shot noise (common in fluorescence microscopy)
- `denoise` — General denoising
- `deblur` — Deconvolution-like
- `upsample` — Super-resolution

## Omnipose Training

Same workflow as Cellpose training, but optimized for elongated/thin objects. Particularly useful for:
- Bacteria
- Filaments
- Neurites
- Any non-convex shapes

## Common Mistakes

| Mistake | Consequence | Fix |
|---------|-------------|-----|
| Label image not 16-bit | Training fails | Convert to 16-bit before annotation |
| Label image has overlapping labels | Training fails or poor results | Ensure unique connected components |
| Label image has 0 as a valid label | Background confused with objects | Background must be 0; objects start at 1 |
| Too few training images | Overfitting | Use 50+ images; enable test data to monitor |
| Not fine-tuning | Slow convergence | Always start from a pretrained model |
| Inconsistent annotation quality | Poor model | Ensure consistent, accurate manual segmentation |
| Diameter set wrong | Model learns wrong scale | Set diameter to match your objects |
| No test data | Can't evaluate quality | Always hold out 20% as test data |
