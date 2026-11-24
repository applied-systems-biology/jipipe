---
audience: [biologist, beginner, ai-agent]
modality: pattern
tags: [pattern, import, split-channels, annotate, file-hierarchy, workflow-template]
biologicalContext: fluorescence-microscopy
prerequisiteKnowledge: [core-semantics/introduction, data-management/annotation-management]
---

# Pattern: Import → Split Channels → Annotate → Process

This is the most fundamental workflow pattern in JIPipe for fluorescence microscopy data. Nearly every bioimage analysis pipeline begins with this sequence.

## When to Use

You have a folder of multi-channel fluorescence images organized in a directory hierarchy, and you need to:
- Split images into single channels
- Annotate each channel with a meaningful name
- Process each channel separately or in matched groups

## Step-by-Step

### Step 1: Import Data

Use the **Import folder** node (from the File system plugin):

1. Point it to the root data directory.
2. Set "Folder depth" to match your directory structure.
3. Enable annotation extraction from folder names — each folder level becomes an annotation (`Folder 1`, `Folder 2`, etc.).
4. Set the file extension filter (e.g., `*.tif`, `*.czi`).

**Example directory structure**:
```
data/
  Control/
    DAPI_GFP.tif
  Treated/
    DAPI_GFP.tif
```

After import, each image has annotations:
- `Folder 1` = `Control` (or `Treated`)
- `File name` = `DAPI_GFP.tif`

### Step 2: Rename Annotations

Use **Edit annotations** to rename:
- `Folder 1` → `Condition`

### Step 3: Split Channels

Use the **Split channels** node:
- Input: multi-channel images
- Output: single-channel images, each annotated with `Channel = C1`, `Channel = C2`, etc.

If the image has channel names embedded in metadata (OME-TIFF), the channel names are used instead.

### Step 4: Annotate Channels with Meaningful Names

Use **Generate annotation** to map channel indices to fluorophore names:

```
Channel = if(Channel == "C1", "DAPI", 
          if(Channel == "C2", "GFP", Channel))
```

Or, if you know the channel order is always DAPI (C1) and GFP (C2):

```
Channel = if(Channel == "C1", "DAPI", "GFP")
```

### Step 5: Process Each Channel

Now you can:
- **Filter** by channel: `Channel == "DAPI"` to process only DAPI
- **Match** channels: connect to a node with column matching on `Channel` to process matched channel pairs
- **Branch** by channel using Split by annotation

## Complete Example: Nuclear Segmentation Pipeline

```
Import folder → Edit annotations → Split channels → Generate annotation (Channel name)
    → Filter (Channel == "DAPI") → Threshold (Otsu) → Watershed → Connected components
    → Measure intensity → Export results
```

## Variant: Multi-Site Acquisition

If data comes from multiple sites/positions:

```
data/
  Plate1/
    Site01/
      DAPI_GFP_RFP.tif
    Site02/
      DAPI_GFP_RFP.tif
```

Annotations after import: `Folder 1` = Plate, `Folder 2` = Site, `File name` = image.

Rename: `Folder 1` → `Plate`, `Folder 2` → `Site`.

After splitting and annotating channels, each image is uniquely identified by `Plate + Site + Channel`.

## Variant: Time-Lapse Data

For time-lapse experiments:

```
data/
  Condition_A/
    t001_DAPI_GFP.tif
    t002_DAPI_GFP.tif
    t003_DAPI_GFP.tif
```

Use **Generate annotation** to extract timepoint from file name:
```
Timepoint = to_int(substring(File name, 1, 3))
```

Then split channels and annotate as above. Each image is identified by `Condition + Timepoint + Channel`.
