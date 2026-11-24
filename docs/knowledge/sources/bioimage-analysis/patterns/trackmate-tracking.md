---
audience: [biologist, intermediate, ai-agent]
modality: pattern
tags: [pattern, tracking, trackmate, time-lapse, particle-tracking]
biologicalContext: time-lapse-microscopy
prerequisiteKnowledge: [bioimage-analysis/patterns/import-split-annotate, bioimage-analysis/patterns/cellpose-segmentation]
---

# Pattern: Object Tracking with TrackMate

TrackMate is a popular ImageJ/Fiji plugin for particle tracking. JIPipe integrates TrackMate through dedicated nodes, enabling fully automated tracking of moving objects (cells, vesicles, particles) across time-lapse data.

## When to Use

- Tracking cell migration in time-lapse microscopy
- Tracking vesicles or organelles
- Measuring motility, velocity, displacement
- Lineage tracking (cell division)

## TrackMate Workflow in JIPipe

```
Import time-lapse → Split channels → Annotate → Filter (target channel)
    → TrackMate (detect + track) → Export tracks → Analyze tracks
```

### TrackMate Node Configuration

TrackMate operates in two stages:

1. **Detection**: identifies objects in each frame.
2. **Tracking**: links objects across frames into tracks.

### Detection Settings

| Detector | When to use |
|---|---|
| Laplacian of Gaussian (LoG) | Round, bright spots on dark background (e.g., fluorescent dots) |
| Difference of Gaussian (DoG) | Similar to LoG, faster |
| Label image detection | Use pre-segmented label images (e.g., from Cellpose) |

**Key detection parameters**:
- **Blob diameter**: expected object diameter in pixels
- **Threshold**: detection sensitivity (higher = fewer detections)

### Tracking Settings

| Tracker | When to use |
|---|---|
| Simple LAP tracker | General purpose; good for most cases |
| LAP tracker with gap closing | When objects temporarily disappear |
| Kalman tracker | When objects move predictably |
| Nearest neighbor | Fast, simple; only for very sparse data |

**Key tracking parameters**:
- **Linking max distance**: maximum distance an object can move between frames
- **Gap-closing max distance**: max distance for closing gaps (temporary disappearance)
- **Gap-closing max frame gap**: max number of frames an object can disappear

### Pre-Segmented Tracking (Recommended for Cells)

For cell tracking, it's often better to segment first, then track:

1. Segment each frame with Cellpose → label images
2. Feed label images to TrackMate with the **Label image detector**
3. Track with LAP tracker

This gives more reliable results than TrackMate's built-in detectors for large, irregular objects.

## Track Analysis

After tracking, TrackMate produces:
- **Track tables**: per-track statistics (duration, displacement, mean speed, etc.)
- **Spot tables**: per-spot statistics (position, intensity, frame, track ID)

In JIPipe, these are output as `ResultsTableData` and can be:
- Filtered by track duration (remove very short tracks)
- Grouped by condition
- Exported to CSV/Excel
- Plotted (displacement over time, speed distributions)

## Pattern: Migration Assay

```
Import time-lapse → Split channels → Annotate (Timepoint, Condition)
    → Filter (Channel == "GFP") → Cellpose segmentation → Label image
    → TrackMate (label detector, LAP tracker) → Export track statistics
    → Filter tracks (duration > 10 frames) → Plot displacement
```

Key annotations: `Condition` (control vs treated), `Timepoint` (frame number).

Use the `Condition` annotation to compare migration speeds between conditions.

## Pattern: Lineage Tracking

For tracking cell divisions:

1. Segment each frame (Cellpose or thresholding).
2. Use TrackMate with LAP tracker and **allow splitting** (cell division).
3. Set "Splitting max distance" to allow a daughter cell to be linked to its parent.
4. Analyze lineage trees from the track output.

## Adaptive Tracking Parameters

If objects move at different speeds across conditions:

```
# Linking max distance as adaptive parameter
if(Condition == "Fast", 50, 20)
```

## Troubleshooting TrackMate

| Problem | Fix |
|---|---|
| Too many fragmented tracks | Increase gap-closing max frame gap |
| Objects not linked | Increase linking max distance |
| Wrong objects linked | Decrease linking max distance |
| Detection misses objects | Lower threshold or increase blob diameter |
| Too many false detections | Raise threshold |
