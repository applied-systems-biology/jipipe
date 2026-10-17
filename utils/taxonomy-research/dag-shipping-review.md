# DAG Pattern Mining — Review Report (steps A–C)

Work item 1324 · branch `1324-node-taxonomy-core` · 2026-10-17

## What was done

1. **(A) Harvest** (`utils/mine-dag-patterns.py`, commit (80)): mined the groupings humans already
   authored from the real corpus (`/data/JIPipe`, 510 projects after excluding Fiji/distribution,
   the vendored `jipipe-release` build tree, scratch and docs): group nodes, canvas-note sections,
   named compartments, and unannotated geometric clusters. 2029 candidates, each a minimal
   `JIPipeGraph` with the source-project layout (origin-normalized) or a longest-path fallback.
2. **(B) Merge** (`utils/merge-dag-patterns.py`, commit (81)): embedded the 624 distinct candidate
   shapes (Qwen 8B endpoint) and agglomerative-clustered (cosine, average linkage) into
   324 near-duplicate families at threshold 0.15 (sweep: chaining begins at 0.20 — the plan's
   generic 0.2–0.3 cut over-merges this corpus).
3. **(C) Gate/name/classify** (`utils/judge-dag-patterns.py`, commit (82)): one LLM call per family
   (`vllm/frontier`, temperature 0, JSON verdict) deciding *general pattern vs plumbing vs noise*,
   the 13-branch function, and a GENERAL name + user-facing description. Per your direction this
   uses the LLM endpoint directly (the imajev MCQ readout's 128-char option cap and lack of
   free-text output fought the naming task).

## Headline numbers

| stage | in | out |
|---|---|---|
| projects scanned | — | 510 |
| (A) candidates | — | 2029 (1243 titled / 786 untitled) |
| (B) families | 2029 | 324 |
| (C) shipped | 324 | **270** |
| (C) rejected | | 54 (43 plumbing, 11 noise) |

**Support (distinct projects) of the shipped set:**

| support | count | cumulative ≥ |

| 92 | 1 | 1 |
| 35 | 1 | 2 |
| 34 | 1 | 3 |
| 31 | 2 | 5 |
| 28 | 1 | 6 |
| 26 | 1 | 7 |
| 22 | 1 | 8 |
| 21 | 1 | 9 |
| 20 | 2 | 11 |
| 18 | 1 | 12 |
| 15 | 1 | 13 |
| 14 | 3 | 16 |
| 13 | 2 | 18 |
| 12 | 3 | 21 |
| 11 | 1 | 22 |
| 10 | 3 | 25 |
| 8 | 9 | 34 |
| 7 | 4 | 38 |
| 6 | 16 | 54 |
| 5 | 11 | 65 |
| 4 | 42 | 107 |
| 3 | 26 | 133 |
| 2 | 75 | 208 |
| 1 | 62 | 270 |→ **62 shipped patterns (23%) come from a single project.**
The plan's noise rule is *name-derivability* (no support floor), so all are valid per the plan —
but a support floor is a one-line (D) change if you want a tighter library:

| option | ships |
|---|---|
| no floor (plan literal) | 270 |
| support ≥ 2 | 208 |
| support ≥ 3 | 133 |
| support ≥ 5 | 65 |

## Shipped by function

| function | shipped |
|---|---|
| data | 11 |
| annotations | 12 |
| images | 105 |
| rois | 64 |
| filaments | 16 |
| machine-learning | 14 |
| tracking | 4 |
| tables | 4 |
| statistics | 10 |
| plotting | 19 |
| export | 10 |
| misc | 1 |

## The shipped patterns (full list, for review)

Grouped by function, then support (descending). Each entry: **name** · support · how it was
harvested (signal) · the node sequence · the model's description. `cluster-XXXX` is the trace id
back into `dag-clusters.json` / the corpus.

### data (11)

- **List folder contents with annotated paths** · support 92 · group-node · `cluster-0001`
  - shape: 9 nodes / 9 edges — Group input -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> Annotate with file/directory name -> List files -…
  - Takes an input folder, enumerates its subfolders and files, annotates each path with relevant file or directory names, and outputs filtered annotated paths along with subfolder information.
- **Folder-based image import with path annotations** · support 14 · geometric-cluster · `cluster-0015`
  - shape: 8 nodes / 7 edges — Folder list -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Bio-Formats importe…
  - Enumerates image files in a folder hierarchy, filters the selected paths, imports the images, and annotates each image with its source path and basic image properties.
- **Import images with source path annotation** · support 10 · geometric-cluster · `cluster-0025`
  - shape: 3 nodes / 2 edges — File list -> Annotate with file/directory name -> Import image
  - Lists image files, annotates each with its file or directory name, and imports the images into the pipeline.
- **Import and denoise images from folders** · support 7 · compartment · `cluster-0035`
  - shape: 10 nodes / 8 edges — Folder list -> Project user path -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -…
  - Lists folders and files under a project path, annotates and filters the resulting paths, then imports the selected images and applies a 2D median filter.
- **Import images from folder structure** · support 4 · compartment · `cluster-0100`
  - shape: 4 nodes / 3 edges — Folder list -> Annotate with file/directory name -> List files -> Import image
  - Lists files in a folder hierarchy, annotates each file with its directory/file name, and imports the images as data.
- **Import images from folders with path annotations** · support 4 · geometric-cluster · `cluster-0101`
  - shape: 5 nodes / 4 edges — Folder list -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Import image
  - Lists folders and files, annotates paths with directory and file names, and imports images, producing image data enriched with source path metadata.
- **List files and annotate with folder/file names** · support 4 · geometric-cluster · `cluster-0102`
  - shape: 5 nodes / 4 edges — Folder list -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Set/Edit annotations
  - Takes a list of folders, enumerates the files inside them, and adds annotations containing directory and file names. Produces a table of file paths enriched with basic path metadata.
- **Import images from folder and convert to 32-bit** · support 2 · compartment · `cluster-0164`
  - shape: 9 nodes / 7 edges — Folder list -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -…
  - Lists files in a folder, attaches annotations to each file, imports the images with Bio-Formats, and converts them to 32-bit for downstream analysis.
- **Load images from folder** · support 1 · canvas-note-section · `cluster-0228`
  - shape: 3 nodes / 2 edges — Folder list -> List files -> Bio-Formats importer
  - Lists files in a folder and imports them as images using an image importer. This provides a reusable data-source step for bringing image files into a pipeline.
- **Import file paths as annotation table** · support 1 · compartment · `cluster-0233`
  - shape: 6 nodes / 5 edges — Project user path -> List files -> Annotate with file/directory name -> Annotate with path properties -> Modify annotation rows (Script) -> Convert to annota…
  - Lists files or folders from a user-selected path, annotates each path with file/directory name and path properties, and converts the annotated paths into an annotation table.
- **OME ZARR directory import/export** · support 1 · geometric-cluster · `cluster-0318`
  - shape: 3 nodes / 2 edges — Folder list -> Import OME ZARR from directory -> Export OME ZARR
  - Lists directories, imports OME ZARR image data from each directory, and exports the imported data as OME ZARR.

### annotations (12)

- **Annotate data with ROI statistics** · support 31 · geometric-cluster · `cluster-0005`
  - shape: 3 nodes / 1 edges — Extract IJ3D ROI statistics -> Add table column -> Annotate data with table values
  - Extracts numeric measurements from ROIs and attaches them to data as annotations, optionally adding derived table columns.
- **Annotate with OME XML metadata** · support 12 · group-node · `cluster-0019`
  - shape: 5 nodes / 4 edges — Group input -> Extract OME XML -> Prettify XML -> Annotate with XML values -> Group output
  - Extracts and formats OME-XML metadata, then attaches selected XML values as annotations to the data.
- **Annotate data and produce cleaned annotation table** · support 8 · geometric-cluster · `cluster-0030`
  - shape: 5 nodes / 4 edges — Annotate data with table values -> Convert to annotation table -> Remove table column -> Merge table columns (simple) -> Rename table column
  - Annotates data with values from a table, converts the resulting annotations into a table, and tidies the table by removing, merging, and renaming columns.
- **Set multiple annotations** · support 8 · canvas-note-section · `cluster-0032`
  - shape: 4 nodes / 3 edges — Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Merge annotations
  - Applies a sequence of expression-based annotation setters to data and merges the resulting annotations into a single annotation set.
- **Filter by annotation** · support 5 · group-node · `cluster-0061`
  - shape: 5 nodes / 6 edges — Group input -> Split & filter by annotation -> Split & filter by annotation -> Split & filter by annotation -> Group output
  - A reusable group that applies annotation-based filtering to multiple data streams, splitting each input according to annotation values and routing the filtered results to separate outputs.
- **Annotation filter chain** · support 5 · geometric-cluster · `cluster-0062`
  - shape: 3 nodes / 0 edges — Filter by annotation (If else) -> Filter by annotation (If else) -> Filter by annotation (If else)
  - Applies multiple annotation-based conditional filters in sequence to select or route data items according to their annotations.
- **Add colored ROI annotations** · support 2 · canvas-note-section · `cluster-0176`
  - shape: 4 nodes / 3 edges — Draw 2D rectangle/oval/line ROI -> Set to color (RGB) -> Convert 2D ROI to RGB -> Merge annotations
  - Draws 2D ROIs, assigns them a color, and merges the resulting ROI annotations with other annotations, optionally converting ROIs to RGB overlays.
- **OCR image text and annotate data** · support 2 · canvas-note-section · `cluster-0177`
  - shape: 5 nodes / 4 edges — Crop 2D image -> Find text (Tesseract OCR) -> Filter table rows -> Filter tables -> Annotate data with table values
  - Crops an image, extracts text with OCR, filters the resulting table rows, and attaches selected text values to data items.
- **Import and annotate image data** · support 2 · geometric-cluster · `cluster-0207`
  - shape: 3 nodes / 2 edges — Bio-Formats importer -> Set annotation (expression) -> Set annotation (expression)
  - Imports image data and adds one or more computed annotation fields to each item.
- **Annotate from JSON files** · support 2 · geometric-cluster · `cluster-0217`
  - shape: 3 nodes / 2 edges — File list -> Import JSON -> Annotate with JSON values
  - Lists JSON files, imports them, and uses their values to annotate data.
- **Create empty image with computed annotation** · support 1 · geometric-cluster · `cluster-0248`
  - shape: 3 nodes / 2 edges — Create empty image -> Set annotation (expression) -> Merge annotations
  - Creates an empty image, assigns an annotation value using an expression, and merges that annotation into the image data.
- **JSON-derived text annotation** · support 1 · geometric-cluster · `cluster-0317`
  - shape: 4 nodes / 3 edges — Extract JSON values as table -> Convert table to CSV text -> Process text (expression) -> Annotate with text values
  - Extracts JSON values into a table, converts the table to CSV text, applies a text expression, and attaches the resulting text as an annotation.

### images (105)

- **Cellpose segmentation to filtered RGB ROIs** · support 31 · geometric-cluster · `cluster-0004`
  - shape: 4 nodes / 3 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB
  - Segments images with a pretrained Cellpose model, filters the resulting ROIs by statistics, and converts the selected ROIs into RGB images.
- **Import and visualize multi-channel images** · support 26 · compartment · `cluster-0007`
  - shape: 9 nodes / 7 edges — Bio-Formats importer -> Project user path -> Reorder dimensions -> Annotate with image properties -> Set/Edit annotations -> Filter by annotation -> Split ch…
  - Imports microscopy images, normalizes dimension order, annotates and filters by image properties, then splits selected channels and assigns color maps for visualization.
- **Cellpose segmentation to cleaned mask hyperstack** · support 20 · canvas-note-section · `cluster-0010`
  - shape: 7 nodes / 6 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Convert 2D ROI to mask -…
  - Runs a pretrained Cellpose model on images, filters the resulting ROIs by statistics, converts them to a binary mask, applies morphological cleanup, and merges the mask into a hyperstack.
- **Particle segmentation and green overlay** · support 20 · canvas-note-section · `cluster-0011`
  - shape: 12 nodes / 11 edges — Remove outliers 2D -> Gaussian blur 3D -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Distance transform watershed 2D -> Connected components …
  - Denoises, thresholds, and segments particles in an image, then converts the detected particle ROIs into a green RGB overlay and blends it with the source image.
- **Multi-channel image channel splitter** · support 15 · group-node · `cluster-0013`
  - shape: 12 nodes / 28 edges — Group input -> Annotate with image properties -> Filter by annotation -> Filter by annotation -> Filter by annotation -> Filter by annotation -> Split channe…
  - Splits an input image into individual channel outputs, with separate paths for images having different numbers of channels.
- **Image and ROI preprocessing** · support 13 · compartment · `cluster-0017`
  - shape: 11 nodes / 11 edges — Folder list -> List subfolders -> Annotate with file/directory name -> List files -> List files -> Import ROI -> Annotate with file/directory name -> Import …
  - Loads paired images and ROIs from a folder hierarchy, crops each image to its ROI, and converts the ROI into a processed mask.
- **Seeded watershed mask postprocessing** · support 13 · compartment · `cluster-0018`
  - shape: 10 nodes / 10 edges — Convert 2D ROI to mask -> Euclidean distance transform 2D -> Distance transform watershed 2D -> Mask to 2D ROI -> Local maxima 2D -> Convert 2D ROI to RGB ->…
  - Postprocesses binary segmentation masks or ROIs by computing distance transforms and local maxima to create seeds, then runs a seeded watershed to separate touching objects and produce labeled RGB output.
- **Preprocess imported image hyperstacks** · support 10 · compartment · `cluster-0023`
  - shape: 10 nodes / 5 edges — Set LUT (color map) -> Bio-Formats importer -> Set LUT (color map) -> Set LUT (color map) -> Project user path -> Reduce & split hyperstack (Expression) -> A…
  - Imports image stacks, applies color maps, splits/reduces hyperstacks, and annotates images with metadata.
- **Threshold-based object segmentation** · support 8 · canvas-note-section · `cluster-0031`
  - shape: 9 nodes / 8 edges — Gaussian blur 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Median filter 2D -> Morphological hole filling 2D -> Connected components labeling 3D…
  - Applies smoothing, thresholding, and morphological cleanup to an image, then labels connected components and converts filtered labels into a binary mask.
- **Ridge detection with mask enhancement** · support 7 · geometric-cluster · `cluster-0036`
  - shape: 9 nodes / 8 edges — Folder list -> Import image -> Annotate with image properties -> Split & filter by annotation -> Crop 2D image -> Ridge detector -> Gaussian blur 2D -> Histo…
  - Crops imported images, detects ridge-like structures, blurs and contrast-enhances the resulting mask, and combines it with the cropped image.
- **Morphological preprocessing and Cellpose segmentation** · support 7 · geometric-cluster · `cluster-0038`
  - shape: 3 nodes / 2 edges — Morphological operation 2D -> Cellpose prediction (2.x) -> Convert 2D ROI to RGB
  - Applies a 2D morphological operation to an image, runs Cellpose segmentation to produce ROIs, and converts those ROIs to an RGB image.
- **Import, denoise, and auto-threshold images** · support 6 · geometric-cluster · `cluster-0039`
  - shape: 5 nodes / 4 edges — File list -> Annotate with file/directory name -> Import image -> Median filter 2D -> Auto threshold 2D (8-bit)
  - Loads image files from a file list, annotates them with source file or directory names, applies a 2D median filter to reduce noise, and generates a binary mask by automatic 8-bit thresholding.
- **Table to image conversion** · support 6 · geometric-cluster · `cluster-0041`
  - shape: 6 nodes / 5 edges — File list -> Import results table -> Rename table column -> Convert table to image -> Tile image 2D -> Un-Tile image 2D
  - Imports tabular result files, normalizes a selected column, and converts the table into an image. It then applies a tile/un-tile step to produce a final image representation of the table data.
- **Particle segmentation using orientation features** · support 6 · canvas-note-section · `cluster-0042`
  - shape: 7 nodes / 6 edges — Median filter 2D -> Auto threshold 2D (8-bit) -> Median filter 2D -> Orientation features 2D (OrientationJ) -> Auto threshold 2D (8-bit) -> Morphological ope…
  - Denoises an image, thresholds it, extracts orientation energy, thresholds that result, and applies morphology and hole filling to produce a particle mask.
- **Multichannel image colorization and scale-bar export** · support 6 · canvas-note-section · `cluster-0045`
  - shape: 9 nodes / 10 edges — Split channels -> Set LUT (color map) -> Set LUT (color map) -> Set LUT (color map) -> Blend images -> Copy physical dimensions -> Draw scale bar -> Export i…
  - Splits image channels, applies color maps, blends the channels, copies physical dimensions, draws a scale bar, and exports the prepared image.
- **Annotated Bio-Formats image import with 32-bit conversion** · support 6 · geometric-cluster · `cluster-0049`
  - shape: 4 nodes / 3 edges — Set/Edit annotations -> Set annotation (expression) -> Bio-Formats importer -> Convert image to 32-bit
  - Prepares annotated data for Bio-Formats image import and converts the imported images to 32-bit depth.
- **Segmentation probability thresholding and overlay** · support 6 · geometric-cluster · `cluster-0053`
  - shape: 4 nodes / 3 edges — Pretrained Cellpose 2.x model -> Cellpose segmentation (2.x) -> Auto threshold 2D (8-bit) -> Blend images
  - Runs a pretrained segmentation model on an image, thresholds the probability map into a binary ROI, and blends the ROI over the original image.
- **Render tracks and spots to RGB images** · support 5 · compartment · `cluster-0056`
  - shape: 3 nodes / 1 edges — Convert spots to RGB -> Render track scheme -> Convert tracks to RGB
  - Converts spot and track data into RGB images, with optional track-scheme rendering, for visualization or further image analysis.
- **Cell segmentation and spot detection** · support 5 · geometric-cluster · `cluster-0057`
  - shape: 7 nodes / 7 edges — Split by dimension -> Filter by annotation -> Cellpose prediction (2.x) -> Convert 2D ROI to mask -> Merge 2D slices into hyperstack -> Detect spots -> Filte…
  - Segments cells with Cellpose on filtered image slices, converts the predicted ROIs into a mask hyperstack, and detects/filters spots.
- **Image stack preprocessing with depth-of-focus projection and illumination correction** · support 5 · compartment · `cluster-0058`
  - shape: 7 nodes / 5 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumi…
  - Imports an image, splits it into slices, applies a depth-of-focus projection and illumination correction, then merges the corrected slices back into a hyperstack.
- **Create cleaned inverted mask from filtered ROIs** · support 5 · geometric-cluster · `cluster-0064`
  - shape: 4 nodes / 3 edges — Filter 2D ROI by statistics -> Convert 2D ROI to mask -> Invert pixel colors/values -> Morphological operation 2D
  - Filters ROIs by a statistic, converts them to a binary mask, inverts the mask, and applies a morphological operation to clean or adjust the resulting image.
- **Batch image import/export** · support 5 · geometric-cluster · `cluster-0065`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Export image
  - Reads image files from a file list, imports them as images, and writes them out again. Useful for batch image I/O, copying, or simple format conversion.
- **Particle detection and skeletonization in image stacks** · support 4 · geometric-cluster · `cluster-0066`
  - shape: 12 nodes / 12 edges — Filter paths -> Import image stack -> Set physical dimensions -> Auto threshold 2D (8-bit) -> Morphological operation 3D -> Gaussian blur 3D -> Auto threshol…
  - Imports image stacks, applies thresholding and morphological cleaning, detects particles, and skeletonizes the resulting masks into line-like structures.
- **Import images and apply median filter** · support 4 · compartment · `cluster-0067`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Median filter 2D
  - Loads images from a file list and applies a 2D median filter to reduce noise in the imported image data.
- **Subtract filament-derived masks from image** · support 4 · canvas-note-section · `cluster-0070`
  - shape: 11 nodes / 13 edges — Invert pixel colors/values -> Morphological operation 2D -> Set to color (RGB) -> Set to value (grayscale) -> Euclidean distance transform 2D -> Set filament…
  - Inverts and preprocesses an image, derives distance-based filament radii, converts filaments to masks, and subtracts those masks to isolate residual structures.
- **Export ROIs as RGB image** · support 4 · geometric-cluster · `cluster-0071`
  - shape: 6 nodes / 4 edges — Set IJ3D ROI properties -> Run Python script (iterating) -> Create empty image -> Convert IJ3D ROI to RGB -> Copy physical dimensions -> Export image
  - Converts ROIs into an RGB image, copies physical dimensions to the image, and exports the resulting image.
- **Split, project, and re-merge image stack** · support 4 · group-node · `cluster-0072`
  - shape: 7 nodes / 6 edges — Group input -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumination correction 2D -> Merge …
  - Splits an image stack by a dimension, applies extended depth of focus projection and 2D illumination correction to each slice, then merges the corrected slices back into a hyperstack.
- **Composite RGB visualization of filaments and images** · support 4 · canvas-note-section · `cluster-0083`
  - shape: 10 nodes / 11 edges — Set LUT (color map) -> Set LUT (color map) -> Convert filaments to RGB -> Convert filaments to mask -> Convert filaments to mask -> Blend images -> Blend ima…
  - Applies color maps to input images, converts filaments into RGB or mask overlays, blends the layers, and renders final composite RGB images.
- **Annotation-based image normalization and arithmetic** · support 4 · geometric-cluster · `cluster-0085`
  - shape: 9 nodes / 10 edges — Split & filter by annotation -> Filter by annotation (If else) -> Filter by annotation (If else) -> Divide by maximum -> Divide by maximum -> Divide by maxim…
  - Separates images by annotation, normalizes each branch by maximum intensity, and combines the normalized images using 2D image arithmetic.
- **Image preprocessing with extended depth of focus projection** · support 4 · compartment · `cluster-0091`
  - shape: 7 nodes / 4 edges — Bio-Formats importer -> Custom extended depth of focus (Z/C/T) -> Project user path -> Annotate with image properties -> Set/Edit annotations -> Set LUT (col…
  - Imports image data, annotates it with image properties, applies a color map, and computes an extended depth of focus projection.
- **Label boundary ring overlay** · support 4 · canvas-note-section · `cluster-0092`
  - shape: 6 nodes / 5 edges — Get label boundaries -> Dilate labels -> Blend images -> Adjust displayed contrast -> Render to RGB -> Export image
  - Extracts label boundaries, dilates them into rings, and blends them over an image. It then adjusts contrast, renders to RGB, and exports the annotated image.
- **Normalized thresholded binary mask** · support 4 · canvas-note-section · `cluster-0095`
  - shape: 4 nodes / 3 edges — Divide by maximum -> CLIJ2 Gaussian Blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D
  - Normalizes an image by its maximum value, smooths it with Gaussian blur, applies automatic thresholding, and fills holes to produce a clean binary mask.
- **Two-pass Cellpose segmentation** · support 4 · canvas-note-section · `cluster-0097`
  - shape: 7 nodes / 6 edges — File list -> Pretrained Cellpose 3.x segmentation model -> Import Cellpose model from file -> Cellpose segmentation (3.x) -> Cellpose segmentation (3.x) -> F…
  - Applies Cellpose segmentation in two passes, using the first pass's flow field as input to the second, then filters the resulting ROIs by statistics and converts them to RGB.
- **Denoise and threshold 2D images** · support 4 · canvas-note-section · `cluster-0098`
  - shape: 5 nodes / 4 edges — Local auto threshold 2D (Bernsen, 8-bit) -> Median filter 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Set LUT (two colors)
  - Applies local thresholding, median filtering, Gaussian blur, global thresholding, and a two-color LUT to convert 2D images into a clean binary mask.
- **Oriented particle label edge extraction** · support 4 · canvas-note-section · `cluster-0103`
  - shape: 12 nodes / 11 edges — Gaussian blur 2D -> Divide by maximum -> Median filter 2D -> Histogram-based contrast enhancer -> Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2…
  - Preprocesses an image, detects particles, and uses orientation features to create a mask for 3D particle labeling. It then converts the particle labels into label edges.
- **Morphological mask cleanup** · support 3 · group-node · `cluster-0110`
  - shape: 5 nodes / 4 edges — Group input -> Morphological operation 2D -> Morphological hole filling 2D -> Morphological operation 2D -> Group output
  - Applies a dilate, hole-fill, and erode sequence to a binary mask. This closes small gaps and fills enclosed holes while preserving the overall mask shape.
- **Import images from folder tree and convert ROI overlays to masks** · support 3 · geometric-cluster · `cluster-0115`
  - shape: 9 nodes / 8 edges — Project user path -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import image …
  - Traverses a project folder tree to locate image files, imports them, removes overlay annotations, and converts 2D ROIs into mask images.
- **2D image preprocessing and thresholding** · support 3 · canvas-note-section · `cluster-0117`
  - shape: 11 nodes / 10 edges — Split by dimension -> Set/Edit annotations -> Set/Edit annotations -> Set LUT (color map) -> Find or subtract background 2D -> Histogram-based contrast enhan…
  - Preprocesses 2D image slices by applying annotations, color mapping, background subtraction, contrast enhancement, denoising, and thresholding. It then combines the processed slices into a hyperstack.
- **Threshold-based image segmentation** · support 3 · canvas-note-section · `cluster-0118`
  - shape: 6 nodes / 5 edges — Find or subtract background 2D -> Set LUT (color map) -> Morphological Feature Contrast (MFC) 2D -> Median filter 2D -> Auto threshold 2D (8-bit) -> Connecte…
  - Preprocesses 2D images with background subtraction, contrast enhancement, and median filtering, then applies automatic thresholding and connected-component labeling to segment objects.
- **3D image thresholding and component labeling** · support 3 · canvas-note-section · `cluster-0119`
  - shape: 11 nodes / 10 edges — Divide by maximum -> Divide by maximum -> Split stack into 2D -> Z-Project -> Fast image arithmetics 2D -> Merge 2D slices into hyperstack -> Median filter 2…
  - Normalizes and projects a 3D image stack, applies thresholding and median filtering, then labels connected components.
- **Export contrast-enhanced images** · support 3 · geometric-cluster · `cluster-0122`
  - shape: 3 nodes / 2 edges — Copy physical dimensions -> Histogram-based contrast enhancer -> Export image
  - Enhances image contrast while preserving physical dimensions and exports the processed images.
- **Image import and preprocessing** · support 3 · compartment · `cluster-0125`
  - shape: 7 nodes / 3 edges — Set LUT (color map) -> Set LUT (color map) -> Set/Edit annotations -> Folder list -> Bio-Formats importer -> Histogram-based contrast enhancer -> Convert ima…
  - Imports image files from a folder and applies basic preprocessing steps such as annotations, color map settings, contrast enhancement, and conversion to 32-bit images.
- **Image preprocessing** · support 3 · geometric-cluster · `cluster-0129`
  - shape: 6 nodes / 3 edges — Remove outliers 2D -> Find or subtract background 2D -> Legacy image calculator 2D -> Find or subtract background 2D -> Median filter 3D -> Histogram-based c…
  - Applies a sequence of image cleaning and enhancement steps, including outlier removal, background subtraction, filtering, and contrast adjustment, to prepare images for downstream analysis.
- **Import images and subtract background** · support 3 · geometric-cluster · `cluster-0131`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Find or subtract background 2D
  - Loads image files from a file list and applies 2D background subtraction to each imported image.
- **Import and scale images** · support 3 · geometric-cluster · `cluster-0132`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Scale 2D image
  - Loads images from a file list and applies 2D scaling to each imported image.
- **Import and tile images** · support 3 · geometric-cluster · `cluster-0133`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Tile image
  - Loads images from a file list and combines them into a tiled image.
- **Global threshold mask overlay** · support 2 · canvas-note-section · `cluster-0146`
  - shape: 6 nodes / 5 edges — Auto threshold 2D (8-bit) -> Equalize hyperstack dimensions (maximum) -> Median filter 2D -> Equalize hyperstack dimensions (maximum) -> Set LUT (color map) …
  - Applies automatic global thresholding and median filtering to an image, then overlays the resulting threshold mask on the raw image using a color map.
- **Local image thresholding** · support 2 · canvas-note-section · `cluster-0147`
  - shape: 10 nodes / 9 edges — Enhance local contrast (CLAHE) -> Equalize hyperstack dimensions (maximum) -> Find or subtract background 2D -> Divide by maximum -> Gaussian blur 2D -> Morp…
  - Enhances local contrast, corrects background, normalizes and smooths the image, then applies a local auto threshold to generate a binary mask. The mask can be color-mapped and blended with the original image for visualization.
- **Preprocess and register 2D images** · support 2 · geometric-cluster · `cluster-0148`
  - shape: 10 nodes / 10 edges — Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Filter by annotation (If else) -> Bio-Formats importer -> Annota…
  - Imports and filters image data, applies denoising and background subtraction, reorders dimensions, and registers 2D images against a reference.
- **Extended depth of focus projection** · support 2 · canvas-note-section · `cluster-0152`
  - shape: 3 nodes / 2 edges — Reduce & split hyperstack (Expression) -> Reorder dimensions -> Cli Jx Extended Depth Of Focus Sobel Projection
  - Prepares a hyperstack by reducing/splitting and reordering dimensions, then computes an extended depth-of-focus projection using a Sobel-based CLI.
- **Particle detection from image stacks** · support 2 · compartment · `cluster-0153`
  - shape: 11 nodes / 9 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Median filter 2D -> Reduce & split hyperstack (Expression) -> Reorder dimensio…
  - Preprocesses imported image stacks with median filtering and dimension normalization, projects them using extended depth of focus, thresholds the projection, and detects 2D particles.
- **Create and export image montage** · support 2 · geometric-cluster · `cluster-0155`
  - shape: 3 nodes / 2 edges — Create montage -> Render to RGB -> Export image
  - Creates a montage from input images, renders it to RGB, and exports the resulting image.
- **Merge ROI and image into RGB** · support 2 · compartment · `cluster-0157`
  - shape: 3 nodes / 0 edges — Convert 2D ROI to RGB -> Convert image to RGB -> Merge channels (classic)
  - Converts a 2D ROI and an image to RGB and merges the channels into a single RGB image. This is useful for visualizing or combining region-of-interest data with image data.
- **Enhance and blend image channel** · support 2 · compartment · `cluster-0158`
  - shape: 3 nodes / 1 edges — Convert image to 8-bit -> Histogram-based contrast enhancer -> Blend images
  - Converts an image to 8-bit, applies histogram-based contrast enhancement, and blends the enhanced result into a target image channel.
- **Combine slices and overlay particle mask** · support 2 · canvas-note-section · `cluster-0170`
  - shape: 9 nodes / 8 edges — CLIJ2 Add Images -> Morphological operation 3D -> Auto threshold 2D (8-bit) -> Morphological operation 3D -> IJ3D Find particles 3D -> Outline IJ3D ROI -> IJ…
  - Combines multiple image slices, segments particles, and overlays a colorized particle mask on the combined image.
- **Reslice, color-map, and blend images** · support 2 · geometric-cluster · `cluster-0171`
  - shape: 3 nodes / 2 edges — Reslice -> Set LUT (color map) -> Blend images
  - Reslices an image, applies a color map, and blends the result with another image for visualization.
- **Quick RGB composite with ROI overlay** · support 2 · canvas-note-section · `cluster-0173`
  - shape: 5 nodes / 5 edges — Convert 2D ROI to RGB -> Set LUT (color map) -> Set LUT (color map) -> Blend images -> Blend images
  - Applies color maps to images and converts a 2D ROI to RGB, then blends them into a simple composite for quick visual inspection.
- **Extract central z-substack and z-project** · support 2 · group-node · `cluster-0181`
  - shape: 10 nodes / 10 edges — Group input -> Annotate with image properties -> Set annotation (expression) -> Unique annotation -> Set annotation (expression) -> Unique annotation -> Set …
  - Takes an image hyperstack, reduces it to a substack centered around the middle z-layer, and produces a z-projection of that substack.
- **Background removal** · support 2 · canvas-note-section · `cluster-0183`
  - shape: 6 nodes / 7 edges — Run ImageJ Macro -> Illumination correction 2D -> Z-Project -> Equalize hyperstack dimensions (referenced) -> Fast image arithmetics 2D -> Illumination corre…
  - Estimates and removes background illumination from images using illumination correction, z-projection, and image arithmetic.
- **EDOF Sobel projection visualization** · support 2 · geometric-cluster · `cluster-0186`
  - shape: 3 nodes / 2 edges — CLIJ2 Extended Depth Of Focus Sobel Projection -> Set LUT (color map) -> Blend images
  - Applies an extended depth of focus Sobel projection to an image stack, assigns a color map, and blends the result with another image for visualization.
- **Combine directory channel slices to RGB** · support 2 · geometric-cluster · `cluster-0187`
  - shape: 3 nodes / 1 edges — Folder list -> Import data table (directory) -> Combine channel slices to RGB
  - Lists a folder, imports its channel slice data, and combines the channels into RGB images.
- **Image preprocessing for orientation analysis** · support 2 · canvas-note-section · `cluster-0189`
  - shape: 4 nodes / 3 edges — Find or subtract background 2D -> Median filter 2D -> Gaussian blur 2D -> Orientation features 2D (OrientationJ)
  - Subtracts background from 2D images, denoises with median and Gaussian filters, and extracts orientation features.
- **Import and split image channels** · support 2 · compartment · `cluster-0191`
  - shape: 5 nodes / 3 edges — Bio-Formats importer -> Folder list -> Reduce & split hyperstack (Expression) -> Split channels -> Remove LUT
  - Imports image files or folders, reduces/splits hyperstacks, separates channels, and removes LUTs to produce clean single-channel images.
- **Correct, enhance, and export image** · support 2 · geometric-cluster · `cluster-0196`
  - shape: 4 nodes / 3 edges — Illumination correction 2D -> Histogram-based contrast enhancer -> Set LUT (color map) -> Export image
  - Applies 2D illumination correction and histogram-based contrast enhancement to an image, assigns a color map, and exports the processed image.
- **Image import, annotation, and channel filtering** · support 2 · compartment · `cluster-0199`
  - shape: 7 nodes / 5 edges — Filter paths -> Folder list -> Filter paths -> Import image -> Annotate with image properties -> Split channels -> Split & filter by annotation
  - Imports image files from a filtered folder, annotates them with image properties, splits channels, and filters the resulting images by annotation.
- **ROI image montage** · support 2 · geometric-cluster · `cluster-0201`
  - shape: 11 nodes / 5 edges — Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Merge 2D ROI lists -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D…
  - Converts 2D ROI lists into RGB images and combines multiple ROI-derived images into a single montage.
- **Color-map ROI images** · support 2 · geometric-cluster · `cluster-0204`
  - shape: 3 nodes / 1 edges — Convert 2D ROI to RGB -> Set LUT (color map) -> Convert 2D ROI to RGB
  - Converts 2D ROIs into RGB images and applies a lookup table/color map for visualization.
- **Normalize image by maximum** · support 2 · geometric-cluster · `cluster-0206`
  - shape: 3 nodes / 2 edges — Set annotation (expression) -> Annotate with image properties -> Divide by maximum
  - Annotates an image with its properties and divides pixel values by the maximum intensity to normalize the image.
- **2D particle segmentation and montage** · support 2 · compartment · `cluster-0208`
  - shape: 10 nodes / 7 edges — Define multiple parameters -> Remove outliers 2D -> Define multiple parameters -> Enhance local contrast (CLAHE) -> Find or subtract background 2D -> Auto th…
  - Preprocesses 2D images, segments particles using background subtraction, thresholding, and watershed, and creates a montage showing the detected ROIs.
- **Morphological thresholding of image stacks** · support 2 · geometric-cluster · `cluster-0209`
  - shape: 6 nodes / 5 edges — Folder -> Annotate with file/directory name -> Import image stack -> Morphological operation 2D -> Auto threshold 2D (8-bit) -> Morphological operation 2D
  - Imports an image stack from a folder and applies morphological operations with automatic thresholding to produce a segmented or binarized image.
- **Automatic particle detection** · support 2 · group-node · `cluster-0213`
  - shape: 4 nodes / 4 edges — Group input -> Auto threshold 2D (8-bit) -> Find particles 2D -> Group output
  - Applies automatic thresholding to an 8-bit image and detects particles, producing particle ROIs and measurements.
- **Apply LUT and contrast to images** · support 2 · geometric-cluster · `cluster-0215`
  - shape: 11 nodes / 10 edges — File list -> File list -> File list -> Import image -> Import image -> Import image -> Set LUT (two colors) -> Adjust displayed contrast -> Apply LUT -> IO I…
  - Imports image files, applies a two-color lookup table and display contrast adjustment, and renders the processed images to RGB.
- **Preprocessed image thresholding** · support 2 · compartment · `cluster-0220`
  - shape: 8 nodes / 8 edges — Adjust displayed contrast -> Convert image to 8-bit -> Invert pixel colors/values -> Auto threshold 2D (8-bit) -> Median filter 2D -> Find or subtract backgr…
  - Normalizes an image by adjusting contrast and converting to 8-bit, then applies inversion, median filtering, and background subtraction before auto-thresholding to produce a binary mask.
- **Load image and extract first channel** · support 2 · geometric-cluster · `cluster-0221`
  - shape: 3 nodes / 2 edges — Import image -> Split channels -> Remove LUT
  - Imports an image, splits it into channels, and removes the LUT from the first channel to produce a clean single-channel image.
- **Import and register 2D images** · support 2 · geometric-cluster · `cluster-0222`
  - shape: 3 nodes / 3 edges — File list -> Import image -> TurboReg registration 2D (simple, referenced)
  - Imports images from a file list and performs simple 2D registration against a reference image.
- **Import and split image stacks** · support 2 · geometric-cluster · `cluster-0223`
  - shape: 3 nodes / 1 edges — File list -> Import image -> Reduce & split stacks (slice)
  - Imports image files and reduces or splits image stacks into individual slices. Useful for preparing per-slice image data from multi-slice files.
- **Import images with scale bar and overlay** · support 2 · geometric-cluster · `cluster-0224`
  - shape: 5 nodes / 4 edges — File list -> Import image -> Set physical dimensions -> Draw scale bar -> Set 2D overlay
  - Imports images from a file list, sets their physical dimensions, and adds a scale bar and 2D overlay for display.
- **Distance transform label overlay** · support 2 · geometric-cluster · `cluster-0225`
  - shape: 10 nodes / 9 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Invert pixel colors/values -> Euclidean distance transform 2D -> Fast…
  - Applies thresholding and morphological cleanup to an image, computes a Euclidean distance transform, converts the result into labeled RGB, and blends it with the source image.
- **Auto threshold parameter sweep** · support 1 · group-node · `cluster-0231`
  - shape: 5 nodes / 5 edges — Group input -> Define multiple parameters -> Auto threshold 2D (8-bit) -> Create montage -> Group output
  - Applies an 8-bit auto threshold to input images using multiple parameter sets and creates a montage of the thresholded results.
- **Image segmentation to ROIs** · support 1 · compartment · `cluster-0239`
  - shape: 12 nodes / 11 edges — Hessian segmentation 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Distance transform watershed 2D -> Morphological…
  - Segments an image using filtering, thresholding, watershed, and morphology, then converts the resulting mask into separate ROIs.
- **Mask-to-RGB image overlay** · support 1 · compartment · `cluster-0240`
  - shape: 5 nodes / 4 edges — Set to value (grayscale) -> IO Interface -> Convert 2D ROI to RGB -> Mask to 2D ROI -> Convert 2D ROI to RGB
  - Converts a binary mask into a 2D ROI/RGB overlay and combines it with a raw image for visualization.
- **Tiled ridge detection** · support 1 · geometric-cluster · `cluster-0242`
  - shape: 3 nodes / 2 edges — Scale 2D image -> Tile image -> Ridge detector
  - Scales a 2D image and splits it into tiles before applying ridge detection, enabling memory-efficient detection of ridge/line structures in large images.
- **Import and Z-project image files** · support 1 · geometric-cluster · `cluster-0252`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Z-Project
  - Loads image files from a file list and computes a Z-projection of the imported image stack.
- **Image derivative feature extraction** · support 1 · geometric-cluster · `cluster-0258`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Derivatives features 2D/3D (FeatureJ)
  - Imports images from a file list and computes 2D/3D derivative features for each image.
- **Import images and split connected components** · support 1 · geometric-cluster · `cluster-0264`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Split labels (connected components) 2D
  - Loads images from a file list and splits each image into connected component labels.
- **Create normalized image montage** · support 1 · geometric-cluster · `cluster-0267`
  - shape: 7 nodes / 6 edges — File list -> Annotate with file/directory name -> Import image -> Scale 2D image -> Convert image to 8-bit -> Render to RGB -> Create montage
  - Takes a list of image files, imports and normalizes them to 8-bit RGB, and combines them into a single montage image.
- **Import and threshold images** · support 1 · geometric-cluster · `cluster-0268`
  - shape: 4 nodes / 3 edges — File list -> Import image -> Remove LUT -> Manual threshold 2D (16-bit)
  - Loads images from a file list, removes LUT information, and applies a manual 2D threshold to the imported 16-bit images.
- **Morphological feature contrast extraction** · support 1 · geometric-cluster · `cluster-0269`
  - shape: 8 nodes / 7 edges — File list -> Define multiple parameters -> Multiply parameters -> Annotate with file/directory name -> Import image -> Morphological operation 2D -> Morpholo…
  - Imports a batch of images, applies morphological preprocessing, and computes morphological feature contrast (MFC) descriptors for each image.
- **Image inpainting with mask** · support 1 · geometric-cluster · `cluster-0276`
  - shape: 9 nodes / 8 edges — File list -> File list -> Import image -> Import image -> Convert -> Convert image to 8-bit -> Convert image to 8-bit -> Image inpainting -> OpenCV Convert D…
  - Imports an image and a mask, converts them to 8-bit, applies image inpainting, and converts the result to a depth image.
- **Extract top-left image tile** · support 1 · group-node · `cluster-0279`
  - shape: 8 nodes / 8 edges — Group input -> Tile image -> Split & filter by annotation -> Remove annotation -> Remove annotation -> Remove annotation -> Remove annotation -> Group output
  - Tiles an input image, selects the top-left tile, and removes annotations from the selected tile.
- **Detect circular markers using Hough transform** · support 1 · group-node · `cluster-0280`
  - shape: 8 nodes / 11 edges — Group input -> Detect circles 2D (Hough, fast) -> Bitwise operation -> Morphological operation 2D -> Distance transform watershed 2D -> Morphological operati…
  - Detects circular markers in an image using a fast Hough transform, then refines the detected mask with morphology and watershed segmentation and labels connected components. Outputs the refined marker mask and circle measurements.
- **Mask cleanup and component labeling** · support 1 · canvas-note-section · `cluster-0286`
  - shape: 4 nodes / 3 edges — Despeckle 2D -> Area opening 2D -> Connected components labeling 2D -> Remove border labels
  - Cleans a binary or segmentation image by removing speckle and small objects, labels connected components, and discards labels touching the image border.
- **2D image preprocessing** · support 1 · canvas-note-section · `cluster-0287`
  - shape: 3 nodes / 2 edges — Find or subtract background 2D -> Morphological operation 2D -> Box filter 2D (3x3)
  - Subtracts or estimates background in 2D images, applies a morphological operation, and smooths the result with a small box filter.
- **Render and export ROI overlay** · support 1 · canvas-note-section · `cluster-0288`
  - shape: 4 nodes / 3 edges — Merge 2D ROI lists -> Set 2D overlay -> Render overlay -> Export image
  - Merges 2D ROI lists, applies them as an overlay, renders the overlay to an image, and exports the resulting image.
- **Image stack registration** · support 1 · geometric-cluster · `cluster-0298`
  - shape: 5 nodes / 4 edges — File list -> Import image -> Scale 2D image -> Merge hyperstack C/Z/T -> TurboReg registration 2D (simple, referenced)
  - Imports images from a file list, scales them, merges them into a hyperstack, and registers the stack to a reference image.
- **Colorize labeled components and export overlay** · support 1 · canvas-note-section · `cluster-0302`
  - shape: 6 nodes / 5 edges — Connected components labeling 2D -> Morphological operation 2D -> Convert labels to RGB -> Set to color (RGB) -> Blend images -> Export image
  - Converts 2D connected-component labels into colored masks, blends them with an image, and exports the resulting visualization.
- **Import and rotate image** · support 1 · geometric-cluster · `cluster-0303`
  - shape: 3 nodes / 2 edges — File -> Import image -> Rotate 2D image (free)
  - Loads an image from a file and applies a free 2D rotation to it.
- **Image montage visualization** · support 1 · group-node · `cluster-0304`
  - shape: 6 nodes / 5 edges — Group input -> Stack to montage -> Make images equal 2D (single input) -> Create 3D stack -> Stack to montage -> Group output
  - Takes input images, creates a normalized 2D montage, builds a 3D stack, and outputs a final montage for visual inspection.
- **Interpolate images** · support 1 · group-node · `cluster-0305`
  - shape: 5 nodes / 6 edges — Group input -> Legacy image calculator 2D -> Legacy image math operation (scalar) -> Legacy image calculator 2D -> Group output
  - Takes two image inputs and produces an interpolated image using image calculation and scalar math operations.
- **Math expression image to pixel table** · support 1 · geometric-cluster · `cluster-0306`
  - shape: 4 nodes / 3 edges — Generate image from math expression -> Get pixels as table -> Rename table column -> Remove table column
  - Generates a synthetic image from a mathematical expression and extracts its pixel values as a table, applying simple column renaming and removal.
- **Measure and plot spots** · support 1 · geometric-cluster · `cluster-0310`
  - shape: 3 nodes / 1 edges — Measure spots -> Set 2D overlay -> Plot tables
  - Measures spots in images and plots the resulting measurements, optionally setting a 2D overlay for visualization.
- **RGB visualization of tracked spots** · support 1 · geometric-cluster · `cluster-0311`
  - shape: 4 nodes / 3 edges — Visualize tracked spots -> Branch analysis -> Merge channels (classic) -> Combine channel slices to RGB
  - Converts tracked spots into image channels and combines them into an RGB image for visual inspection.
- **Import and normalize images to 8-bit RGB** · support 1 · geometric-cluster · `cluster-0315`
  - shape: 5 nodes / 4 edges — File list -> Import image -> Convert image to 8-bit -> Adjust displayed contrast -> Render to RGB
  - Loads images from a file list, converts them to 8-bit, adjusts displayed contrast, and renders them as RGB images.
- **LUT-based image visualization** · support 1 · canvas-note-section · `cluster-0323`
  - shape: 5 nodes / 4 edges — Histogram-based contrast enhancer -> Set 2D ROI properties -> Set LUT (color map) -> Apply LUT -> Convert 2D ROI to RGB
  - Enhances image contrast, applies a color map/LUT, and converts a 2D ROI image to RGB for display.
- **Import, convert, and export image stacks** · support 1 · geometric-cluster · `cluster-0324`
  - shape: 6 nodes / 4 edges — Folder list -> Folder list -> Annotate with file/directory name -> Import image stack -> Convert image to 8-bit -> Export image
  - Lists image folders, imports them as image stacks, converts the images to 8-bit, and exports the resulting images.

### rois (64)

- **Threshold-based particle detection** · support 35 · geometric-cluster · `cluster-0002`
  - shape: 4 nodes / 3 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Import image files, automatically threshold 2D images, and detect particles as ROIs.
- **Filter ROIs by overlap** · support 34 · compartment · `cluster-0003`
  - shape: 6 nodes / 1 edges — Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by over…
  - Filters or combines 2D ROIs based on spatial overlap, such as including/excluding edge ROIs or matching multiple ROI sets.
- **2D particle detection and ROI overlay** · support 28 · geometric-cluster · `cluster-0006`
  - shape: 4 nodes / 3 edges — Morphological operation 2D -> Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB
  - Applies morphological preprocessing and automatic thresholding to a 2D image, detects particles as ROIs, and converts those ROIs to an RGB overlay for visualization.
- **Color ROIs by statistics and render as RGB** · support 22 · canvas-note-section · `cluster-0008`
  - shape: 4 nodes / 3 edges — Color IJ3D ROI by statistics -> Convert IJ3D ROI to RGB -> Convert IJ3D ROI to RGB -> Convert IJ3D ROI to RGB
  - Colors ROIs according to statistical values and converts them into RGB images for visualization.
- **2D particle segmentation** · support 18 · canvas-note-section · `cluster-0012`
  - shape: 7 nodes / 6 edges — Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Morphological operation 2D -> Distance transform watershed 2D -> Find parti…
  - Detects particles in 2D images by blurring, thresholding, morphological cleanup, watershed separation, and particle finding. It outputs particle ROIs, optionally converted to RGB for visualization.
- **Detect and filter 2D particles** · support 10 · geometric-cluster · `cluster-0024`
  - shape: 7 nodes / 6 edges — Find or subtract background 2D -> Auto threshold 2D (8-bit) -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Find particl…
  - Subtracts background, thresholds and cleans the image, finds particle ROIs, and filters them by statistical properties.
- **Segment objects and extract ROI statistics** · support 8 · canvas-note-section · `cluster-0027`
  - shape: 6 nodes / 5 edges — Auto threshold 2D (8-bit) -> Median filter 2D -> Connected components labeling 3D -> IJ3D Labels to 3D ROI -> Filter IJ3D ROI by statistics -> Extract IJ3D R…
  - Thresholds an image, denoises it, labels connected components, converts labels to ROIs, filters ROIs by statistics, and extracts ROI statistics.
- **Object-based colocalization** · support 8 · canvas-note-section · `cluster-0028`
  - shape: 12 nodes / 12 edges — Create empty image -> Create empty image -> Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB…
  - Filters two ROI sets by overlap, converts them to colored RGB images, blends them, and exports a colocalization image.
- **2D ROI overlap quality check** · support 6 · compartment · `cluster-0040`
  - shape: 9 nodes / 9 edges — Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI…
  - Evaluates agreement between two 2D ROI sets by overlap, counting true positives, false positives, false negatives, and detected ground-truth ROIs, then computes per-row summary metrics.
- **Per-ROI statistics and mask generation** · support 6 · canvas-note-section · `cluster-0043`
  - shape: 4 nodes / 3 edges — Split into individual IJ3D ROI lists -> Extract IJ3D ROI statistics -> Annotate data with table values -> IJ3D ROI to mask
  - Splits a collection of 3D ROIs into individual ROI records, extracts measurements for each ROI, annotates the ROI data with those measurements, and converts the annotated ROIs into masks.
- **Summarize ROIs with annotations and statistics** · support 6 · canvas-note-section · `cluster-0048`
  - shape: 9 nodes / 9 edges — Filter 2D ROI by overlap (old) -> Annotate with data -> Extract data annotations -> Annotate with 2D ROI properties -> Extract 2D ROI statistics -> Add annot…
  - Filters ROIs by overlap, attaches data annotations and ROI properties, extracts ROI statistics, and compiles the results into a table.
- **Connected component label overlap filter** · support 4 · geometric-cluster · `cluster-0068`
  - shape: 4 nodes / 3 edges — Filter 2D ROI by statistics -> Get label boundaries -> Connected components labeling 2D -> Filter Labels by overlap 2D
  - Extracts label boundaries, labels connected components in 2D, and filters the resulting labels based on overlap.
- **Extract annotated ROI statistics** · support 4 · geometric-cluster · `cluster-0077`
  - shape: 6 nodes / 5 edges — 2D ROI to Labels (Expression) -> CLIJ2 Exclude Labels On Edges -> Labels to ROI -> Extract 2D ROI statistics -> Add annotations as columns -> Merge table rows
  - Converts ROIs to labels, removes edge-touching labels, and extracts per-ROI measurements. It then merges those measurements with annotation columns into a table.
- **Enrich 2D ROI annotations** · support 4 · geometric-cluster · `cluster-0078`
  - shape: 6 nodes / 3 edges — Rename annotation -> Filter 2D ROI by overlap (old) -> Split into individual 2D ROI lists -> Annotate with 2D ROI properties -> Outline 2D ROI (Hull) -> Set …
  - Filters and splits 2D ROI lists, adds ROI property annotations, and generates hull outline annotations for downstream analysis.
- **2D particle segmentation and ROI overlay** · support 4 · compartment · `cluster-0080`
  - shape: 8 nodes / 7 edges — Image box canvas note -> Auto threshold 2D (8-bit) -> Median filter 2D -> Illumination correction 2D -> Auto threshold 2D (8-bit) -> Find particles 2D -> Con…
  - Segments particles or objects in 2D images using thresholding, median filtering, and illumination correction. Converts the detected ROIs into an RGB overlay for visualization.
- **Particle detection via MFC thresholding** · support 4 · canvas-note-section · `cluster-0087`
  - shape: 6 nodes / 5 edges — Median filter 2D -> Set LUT (color map) -> Morphological Feature Contrast (MFC) 2D -> Auto threshold 2D (8-bit) -> IJ3D Find particles 3D -> Set IJ3D ROI pro…
  - Denoises an image, applies morphological feature contrast, thresholds the result, and extracts particle ROIs with properties.
- **Segment and extract particles from images** · support 4 · canvas-note-section · `cluster-0088`
  - shape: 6 nodes / 5 edges — Median filter 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Connected components labeling 3D -> Filter labels by expression 3D -> IJ3D Find parti…
  - Denoise and threshold an image, label connected components, filter labels by an expression, and extract particle objects.
- **Particle detection from labeled mask** · support 4 · canvas-note-section · `cluster-0089`
  - shape: 8 nodes / 7 edges — Convert labels to mask -> Set to color (RGB) -> Median filter 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> IJ3D…
  - Processes a labeled mask through colorization, denoising, thresholding, and hole filling to detect and annotate particle ROIs.
- **Convert 2D ROIs to RGB images** · support 4 · canvas-note-section · `cluster-0096`
  - shape: 4 nodes / 3 edges — Set 2D ROI properties -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB
  - Sets properties on 2D ROIs and converts them into RGB images for visualization.
- **Convert 2D ROIs to RGB** · support 4 · geometric-cluster · `cluster-0099`
  - shape: 3 nodes / 0 edges — Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB
  - Converts 2D region-of-interest data into RGB image representations for visualization or downstream image processing.
- **Cellpose segmentation to ROIs** · support 4 · canvas-note-section · `cluster-0104`
  - shape: 12 nodes / 12 edges — Set to color (RGB) -> Pretrained Cellpose 3.x segmentation model -> Divide by maximum -> Set LUT (color map) -> Histogram-based contrast enhancer -> Cellpose…
  - Preprocesses a color image and runs a pretrained Cellpose model to segment cells, then converts the resulting segmentations into 3D ROIs.
- **Segment and filter ROIs** · support 4 · canvas-note-section · `cluster-0109`
  - shape: 4 nodes / 3 edges — Cellpose segmentation (2.x) -> Filter 2D ROI by statistics -> Filter 2D ROI by overlap (old) -> Merge 2D ROI lists (ordered)
  - Runs image segmentation, filters the resulting ROIs by statistics and overlap, and merges the filtered ROI lists into a single ordered list.
- **Particle detection from images** · support 3 · canvas-note-section · `cluster-0114`
  - shape: 4 nodes / 3 edges — Import image -> Median filter 2D -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Imports an image, denoises it with a median filter, thresholds it, and detects particles or regions of interest from the resulting mask.
- **3D particle detection and measurement export** · support 3 · canvas-note-section · `cluster-0120`
  - shape: 4 nodes / 3 edges — IJ3D Find particles 3D -> Extract IJ3D ROI statistics -> Filter table rows -> Export data
  - Finds particles in 3D images, extracts ROI statistics such as volume and center, filters the resulting measurements, and exports them as data.
- **Filter and export ROIs as image** · support 3 · geometric-cluster · `cluster-0123`
  - shape: 5 nodes / 4 edges — Split into individual 2D ROI lists -> Filter 2D ROI by statistics -> Merge 2D ROI lists -> Convert 2D ROI to RGB -> Export image
  - Filters 2D ROIs by statistical criteria, merges the remaining ROIs, and exports them as an RGB image.
- **Render 2D ROIs to RGB images** · support 3 · canvas-note-section · `cluster-0127`
  - shape: 9 nodes / 8 edges — Merge 2D ROI lists -> Set 2D ROI properties -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Render to RGB -> Export image -> Me…
  - Merges 2D ROI lists, applies ROI styling, converts the ROIs to RGB, and renders/exports the result as images or a hyperstack.
- **Overlay ROIs on image** · support 3 · group-node · `cluster-0128`
  - shape: 4 nodes / 4 edges — Group input -> Histogram-based contrast enhancer -> Convert 2D ROI to RGB -> Group output
  - Takes an image and a 2D ROI, enhances the image contrast, and renders the ROI as an RGB overlay on the image.
- **Annotate and filter imported ROIs** · support 2 · geometric-cluster · `cluster-0139`
  - shape: 7 nodes / 6 edges — Annotate with file/directory name -> Import ROI -> Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Set annotatio…
  - Imports ROI files, derives annotations from file/directory names and expressions, then filters ROIs based on annotation values.
- **Seeded watershed segmentation from color images** · support 2 · geometric-cluster · `cluster-0140`
  - shape: 12 nodes / 12 edges — CLIJ2 Extended Depth Of Focus Sobel Projection: Green -> Filter by annotation -> Filter by annotation -> Histogram-based contrast enhancer -> CLIJ2 Extended …
  - Enhances and thresholds selected color channels, then uses annotation markers to segment objects via seeded watershed.
- **ROI measurement expression pipeline** · support 2 · geometric-cluster · `cluster-0141`
  - shape: 3 nodes / 2 edges — Set 2D ROI properties from expressions -> Extract 2D ROI statistics -> Apply expression per label
  - Sets ROI properties from expressions, extracts 2D ROI statistics, and applies per-label expressions to the resulting measurements.
- **Filter and convert ROIs to RGB** · support 2 · geometric-cluster · `cluster-0142`
  - shape: 11 nodes / 12 edges — Filter by annotation -> Filter 2D ROI by statistics -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert only 2D ROI to RGB …
  - Filters 2D ROIs by annotation or statistics, updates ROI properties, converts selected ROIs to RGB images, and extracts ROI statistics.
- **Filter and split 2D ROIs by statistics** · support 2 · geometric-cluster · `cluster-0143`
  - shape: 7 nodes / 4 edges — Extract 2D ROI statistics -> Filter labels by statistics 2D -> Morphological operation 2D -> Legacy image calculator 2D -> CLIJ2 Mask -> Mask to 2D ROI -> Sp…
  - Extracts statistics from 2D ROIs, filters labels using those statistics, cleans the resulting mask, and converts it into individual single-component ROIs.
- **Import and color ROIs** · support 2 · canvas-note-section · `cluster-0145`
  - shape: 3 nodes / 2 edges — Annotate with file/directory name -> Import ROI -> Set to color (RGB)
  - Imports ROI files, annotates them with their source file or directory name, and assigns a display color.
- **Detect 2D particles by thresholding** · support 2 · canvas-note-section · `cluster-0156`
  - shape: 9 nodes / 8 edges — Divide by maximum -> CLIJ2 Gaussian Blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> CLIJ2 Gaussian Blur 2D -> Illumination correctio…
  - Preprocess a 2D image with normalization, blurring, hole filling, and illumination correction, then threshold and find particles to produce colored ROIs.
- **Segmentation to annotated ROIs** · support 2 · geometric-cluster · `cluster-0159`
  - shape: 5 nodes / 6 edges — Merge channels (classic) -> Histogram-based contrast enhancer -> Cellpose segmentation (3.x) -> Set 2D overlay -> Annotate with 2D ROI properties
  - Prepares an image by merging channels and enhancing contrast, runs Cellpose segmentation, and attaches the resulting ROIs to the image with property annotations.
- **Object-based ROI statistics** · support 2 · canvas-note-section · `cluster-0161`
  - shape: 5 nodes / 4 edges — Split stack into 2D -> Extract 2D ROI statistics -> Apply expression per row -> Merge table rows -> Split table by columns
  - Splits an image stack into 2D frames, extracts ROI statistics for each frame, applies row-wise expressions, and reshapes the results into a per-object table.
- **ROI image statistics plot** · support 2 · canvas-note-section · `cluster-0162`
  - shape: 4 nodes / 3 edges — Remove annotation -> Extract image statistics -> JFreeChart XY line plot -> Export image
  - Removes annotations, extracts image statistics for ROIs, plots the measurements as an XY line chart, and exports the resulting plot image.
- **Particle detection and ROI statistics** · support 2 · geometric-cluster · `cluster-0172`
  - shape: 10 nodes / 8 edges — Set/Edit annotations -> Apply transformation -> Run ImageJ Macro -> Morphological operation 2D -> Morphological hole filling 2D -> Find particles 2D -> Conve…
  - Preprocesses an image, detects particle ROIs, converts and merges the ROI list, and extracts per-ROI statistics.
- **Variance-based ROI filtering and statistics** · support 2 · geometric-cluster · `cluster-0175`
  - shape: 7 nodes / 6 edges — Local variance 2D -> Divide by maximum -> Filter 2D ROI by statistics -> Convert 2D ROI to mask -> Morphological operation 2D -> Mask to 2D ROI -> Extract 2D…
  - Computes local image variance, normalizes it, filters ROIs by variance statistics, cleans the resulting mask morphologically, and extracts ROI statistics.
- **Channel object segmentation and ROI statistics** · support 2 · canvas-note-section · `cluster-0182`
  - shape: 9 nodes / 8 edges — Reduce & split hyperstack (Expression) -> Z-Project -> Gaussian blur 2D -> Equalize hyperstack dimensions (referenced) -> Fast image arithmetics 2D -> Auto t…
  - Preprocesses a selected image channel, thresholds and labels connected objects, converts the labels to 3D ROIs, and extracts ROI statistics.
- **Pairwise ROI distance measurement** · support 2 · compartment · `cluster-0184`
  - shape: 10 nodes / 6 edges — Extract IJ3D ROI statistics -> Extract pairwise IJ3D ROI statistics -> Connected components labeling 3D -> Extract pairwise IJ3D ROI statistics -> Extract IJ…
  - Extracts ROI and pairwise ROI statistics from labeled/ROI data and computes distance measurements between regions of interest.
- **ROI visualization overlay** · support 2 · compartment · `cluster-0192`
  - shape: 10 nodes / 12 edges — Set 2D ROI properties -> Illumination correction 2D -> Set 2D ROI properties -> Adjust displayed contrast -> Filter 2D ROI by overlap (old) -> Convert 2D ROI…
  - Applies illumination correction and contrast adjustment to an image, filters and sets ROIs, and converts ROIs to RGB images for visualization.
- **ROI mask hyperstack** · support 2 · canvas-note-section · `cluster-0193`
  - shape: 4 nodes / 3 edges — Merge 2D ROI lists -> Convert 2D ROI to mask -> Morphological operation 2D -> Merge hyperstack C/Z/T (single input)
  - Merges 2D ROI lists, converts them into a binary mask, applies a morphological operation, and stacks the result into a hyperstack.
- **Filter and merge ROIs by overlap** · support 2 · compartment · `cluster-0194`
  - shape: 8 nodes / 4 edges — Filter 2D ROI by overlap (old) -> Merge 2D ROI lists -> Filter 2D ROI by overlap (old) -> Merge 2D ROI lists -> Filter 2D ROI by overlap (old) -> Filter 2D R…
  - Filters 2D ROIs based on overlap with other ROI lists and merges the resulting ROI lists, useful for selecting ROIs that intersect or are contained in reference regions.
- **Annotate, plot, and filter ROI statistics** · support 2 · geometric-cluster · `cluster-0200`
  - shape: 6 nodes / 5 edges — Extract 2D ROI statistics -> Set/Edit annotations -> JFreeChart Histogram plot -> JFreeChart Histogram plot -> Split & filter by annotation -> Split & filter…
  - Extracts 2D ROI measurements, attaches or edits annotations, visualizes the measurements as histograms, and splits/filters the data by annotation.
- **Pairwise ROI statistics summary and plotting** · support 2 · geometric-cluster · `cluster-0202`
  - shape: 9 nodes / 10 edges — Extract pairwise 2D ROI statistics -> Filter table rows -> Table column to histogram -> Filter table rows -> Apply expression to columns -> Annotate data wit…
  - Computes pairwise 2D ROI measurements, filters and summarizes them into histograms, and annotates data with derived table values for visualization.
- **Particle ROI measurement and plotting** · support 2 · geometric-cluster · `cluster-0210`
  - shape: 8 nodes / 7 edges — Find particles 2D -> Split 2D ROI into connected components -> Extract 2D ROI statistics -> Summarize table -> Add annotations as columns -> Merge table rows…
  - Detects 2D particles, splits them into connected components, extracts ROI statistics, and summarizes, annotates, and filters the measurements into a table for plotting.
- **Skeletonized particle ROI extraction** · support 2 · geometric-cluster · `cluster-0211`
  - shape: 3 nodes / 2 edges — Morphological skeletonize 2D -> Find particles 2D -> Convert 2D ROI to RGB
  - Skeletonizes a 2D mask, detects particle ROIs from the skeleton, and converts those ROIs to an RGB image for visualization.
- **Connected component and combined ROI statistics** · support 2 · geometric-cluster · `cluster-0212`
  - shape: 11 nodes / 11 edges — Split 2D ROI into connected components -> Extract 2D ROI statistics -> 2D ROI calculator (AND/OR/XOR) -> Extract 2D ROI statistics -> Rename & modify columns…
  - Splits 2D ROIs into connected components, extracts statistics for the components and for a boolean-combined ROI, merges the measurements into a sorted table, and visualizes the results with a plot and image montage.
- **Create mask from image and ROI** · support 1 · compartment · `cluster-0232`
  - shape: 4 nodes / 4 edges — Bio-Formats importer -> Histogram-based contrast enhancer -> Run ImageJ Macro -> Convert 2D ROI to mask
  - Imports an image, enhances contrast, runs an ImageJ macro to produce a 2D ROI, and converts that ROI into a mask.
- **Particle segmentation with colored ROI overlay** · support 1 · geometric-cluster · `cluster-0236`
  - shape: 9 nodes / 9 edges — File list -> Import image -> Z-Project (classic) -> Distance transform watershed 2D -> Find particles 2D -> 2D ROI to Labels (by name) -> Labels to ROI -> Co…
  - Imports image data, projects it to 2D, segments particles using distance-transform watershed, and converts the detected particles into labeled, color-coded ROIs for RGB visualization.
- **Create 2D ROI overlay** · support 1 · geometric-cluster · `cluster-0256`
  - shape: 4 nodes / 3 edges — Draw 2D rectangular ROI (old) -> Draw 2D oval ROI (old) -> Draw 2D text ROI -> Set 2D overlay
  - Draws multiple 2D ROIs, such as rectangles, ovals, and text labels, and sets them as an image overlay.
- **OCR text ROI overlay** · support 1 · geometric-cluster · `cluster-0272`
  - shape: 7 nodes / 7 edges — File list -> Annotate with file/directory name -> Import image -> Find text (Tesseract OCR) -> Filter table rows -> Table to 2D text ROI -> Set 2D overlay
  - Runs OCR on imported images, filters detected text rows, converts them to 2D text ROIs, and overlays the text on the images.
- **Thresholded particle detection from OMERO images** · support 1 · geometric-cluster · `cluster-0273`
  - shape: 6 nodes / 5 edges — List OMERO projects -> List OMERO datasets -> List OMERO images in dataset -> Download image from OMERO -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Downloads OMERO images, applies 8-bit auto thresholding, and detects particles as ROIs.
- **Annotated table to hull ROI** · support 1 · geometric-cluster · `cluster-0277`
  - shape: 5 nodes / 4 edges — File list -> Import data table (archive) -> Set/Edit annotations -> Outline 2D ROI (Hull) -> Set 2D ROI properties
  - Imports a table from an archive, applies or edits annotations, and creates a 2D hull ROI from the annotated points. It then sets properties on the resulting ROI.
- **Filter particles by summary statistic** · support 1 · canvas-note-section · `cluster-0281`
  - shape: 4 nodes / 3 edges — Find particles 2D -> Summarize table -> Annotate data with table values -> Filter by annotation (If else)
  - Detects 2D particles, summarizes their measurements into a single value, annotates the particles with that value, and filters them based on the annotation.
- **Aggregate ROI counts** · support 1 · canvas-note-section · `cluster-0282`
  - shape: 5 nodes / 4 edges — Count 2D ROI -> Count 2D ROI -> Add table column -> Add table column -> Merge table rows
  - Counts 2D ROIs from separate ROI inputs, adds identifying table columns, and merges the count results into a single summary table.
- **Watershed particle detection** · support 1 · canvas-note-section · `cluster-0283`
  - shape: 6 nodes / 5 edges — Seeded watershed -> Manual threshold 2D (8-bit) -> Legacy image calculator 2D -> Find particles 2D -> Find particles 2D -> Set 2D ROI properties
  - Segments images with seeded watershed and thresholding, then detects particles and sets their ROI properties.
- **Detect marker ROIs in 2D images** · support 1 · canvas-note-section · `cluster-0291`
  - shape: 9 nodes / 10 edges — Laplacian of Gaussian 2D -> Custom auto threshold 2D (32-bit) -> Legacy image calculator 2D -> Euclidean distance transform 2D -> Find particles 2D -> Filter…
  - Detects marker-like particles in 2D images using LoG filtering, thresholding, and distance transform, then filters, annotates, and labels the resulting ROIs.
- **Render 2D ROI overlays and masks** · support 1 · geometric-cluster · `cluster-0296`
  - shape: 7 nodes / 7 edges — Merge 2D ROI lists -> Remove annotation -> Set 2D overlay -> Render overlay -> Convert 2D ROI to mask -> Export image -> Export image
  - Merges 2D ROI lists, removes annotations, renders an overlay image and a mask image from the ROIs, and exports both images.
- **Detect and visualize spots as ROIs** · support 1 · geometric-cluster · `cluster-0309`
  - shape: 4 nodes / 3 edges — Detect spots -> Filter spots -> Convert spots to ROI -> Convert 2D ROI to RGB
  - Detects and filters spots, converts them into ROIs, and renders those ROIs as an RGB image.
- **Threshold-based particle segmentation** · support 1 · compartment · `cluster-0316`
  - shape: 5 nodes / 4 edges — Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D -> Find particles 2D
  - Segments objects in an image by blurring, automatic thresholding, and morphological cleanup, then extracts connected particles as ROIs.
- **2D particle segmentation** · support 1 · canvas-note-section · `cluster-0320`
  - shape: 6 nodes / 5 edges — Apply ImageJ auto-contrast per slice -> Median filter 2D -> Laplacian of Gaussian 2D -> Fast image arithmetics 2D -> Auto threshold 2D (8-bit) -> Find partic…
  - Enhances 2D images with contrast normalization, median denoising, LoG filtering, and thresholding, then detects particles as ROIs.
- **ROI visualization** · support 1 · canvas-note-section · `cluster-0322`
  - shape: 4 nodes / 3 edges — Split 2D ROI into connected components -> Set 2D ROI properties from expressions -> Merge 2D ROI lists -> Convert 2D ROI to RGB
  - Splits 2D ROIs into connected components, assigns ROI properties, merges the components, and converts the resulting ROIs to an RGB image for display.

### filaments (16)

- **Skeletonize and measure filaments** · support 8 · canvas-note-section · `cluster-0034`
  - shape: 12 nodes / 11 edges — Morphological operation 2D -> Morphological operation 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological skeletonize 2D -> Set to value (gr…
  - Preprocesses a 2D image with morphology, blurring, thresholding, and skeletonization to extract filaments, then filters and measures filament components and outputs a mask plus a merged annotation table.
- **Frangi vesselness filament mask** · support 6 · geometric-cluster · `cluster-0054`
  - shape: 6 nodes / 5 edges — Find or subtract background 2D -> Auto threshold 2D (8-bit) -> Median filter 2D -> Frangi vesselness -> Divide by maximum -> Auto threshold 2D (8-bit)
  - Preprocesses 2D images with background subtraction, thresholding, and median filtering, then applies Frangi vesselness and normalization to generate a binary mask of tubular or filament-like structures.
- **Extract and render filaments from images** · support 5 · geometric-cluster · `cluster-0055`
  - shape: 7 nodes / 7 edges — File list -> Import image -> Morphological skeletonize 2D -> Binary skeleton to 2D filaments -> Smooth filaments -> Set filament vertex radius from image -> …
  - Skeletonizes input images, converts the resulting binary skeleton into 2D filaments, smooths them, sets vertex radii from the image, and produces RGB filament images.
- **Split filaments into connected components** · support 5 · geometric-cluster · `cluster-0063`
  - shape: 3 nodes / 1 edges — Convert filaments to mask -> Split filaments into connected components -> Convert filaments to mask
  - Rasterizes filament structures into a binary mask and applies connected-component splitting to separate individual filament segments, then converts the resulting components back into a mask. Useful for breaking up filament networks or touching structures into distinct objects.
- **Cellpose-based filament extraction** · support 4 · canvas-note-section · `cluster-0082`
  - shape: 10 nodes / 10 edges — Pretrained Cellpose 2.x model -> Cellpose segmentation (2.x) -> Auto threshold 2D (8-bit) -> Euclidean distance transform 2D -> Morphological skeletonize 2D …
  - Segments objects with a pretrained Cellpose model, thresholds the probability image, skeletonizes the binary mask, and builds smoothed 2D filaments whose vertex radius is derived from a distance transform. The final filament mask is colored for display.
- **Filament component measurement and scatter plotting** · support 4 · canvas-note-section · `cluster-0084`
  - shape: 10 nodes / 9 edges — Split filaments into connected components -> Filter filament components -> Find linear filament vertex order/distance -> Measure filament vertices -> Filter …
  - Splits filaments into components, orders and measures their vertices, then filters the resulting table and visualizes selected measurements as XY scatter plots.
- **Measure total edge length** · support 4 · canvas-note-section · `cluster-0106`
  - shape: 5 nodes / 4 edges — Morphological skeletonize 2D -> Extract image statistics (Expression) -> Merge table rows -> Apply expression per row -> Export table as multi-sheet XLSX
  - Skeletonizes images to isolate line-like structures, measures their total length from skeleton pixel counts, aggregates the measurements, and exports a summary table.
- **Z-projected filament detection** · support 3 · geometric-cluster · `cluster-0126`
  - shape: 3 nodes / 3 edges — Z-Project (classic) -> Find and track filaments 3D+T (TSOAX) -> Convert filaments to mask
  - Projects a 3D+T image along Z, detects filaments in the projected image, and converts the detected filaments into a mask.
- **Skeletonize and refine 2D filaments** · support 3 · compartment · `cluster-0135`
  - shape: 8 nodes / 7 edges — Euclidean distance transform 2D -> Morphological skeletonize 2D -> Binary skeleton to 2D filaments -> Smooth filaments -> Fix overlapping filaments (non-bran…
  - Converts a binary image into cleaned 2D filaments by skeletonizing, smoothing, resolving overlaps, assigning vertex radii from a distance transform, and removing border or invalid components.
- **Filament detection** · support 2 · canvas-note-section · `cluster-0169`
  - shape: 7 nodes / 6 edges — Reduce & split hyperstack (Expression) -> Extended DoF Sobel -> Set/Edit annotations -> Merge 2D slices into hyperstack -> Custom auto threshold 2D (16-bit) …
  - Detects filament-like structures in hyperstack images by applying edge detection, thresholding, and morphological cleanup.
- **Background-subtracted ridge detection** · support 2 · geometric-cluster · `cluster-0179`
  - shape: 7 nodes / 5 edges — Folder list -> Bio-Formats importer -> Annotate with image properties -> Filter by annotation -> Split channels -> Find or subtract background 2D -> Ridge de…
  - Imports image stacks, filters and selects a channel, subtracts background, and detects ridge-like line structures.
- **Filament component measurement table** · support 2 · compartment · `cluster-0219`
  - shape: 3 nodes / 2 edges — Measure filament components -> Remove table column -> Rename table column
  - Measures properties of filament components and then removes or renames table columns to produce a clean, standardized measurement table.
- **Fix and convert filaments to 3D ROIs** · support 1 · geometric-cluster · `cluster-0259`
  - shape: 3 nodes / 2 edges — Fix overlapping filaments (non-branching) -> Set 2D overlay -> Convert filaments to 3D ROI
  - Corrects overlapping non-branching filaments, sets a 2D overlay, and converts the filaments into 3D ROIs.
- **Detect line segments in images** · support 1 · geometric-cluster · `cluster-0266`
  - shape: 5 nodes / 5 edges — File list -> Import image -> Binarize -> Detect line segments 2D (Hough) -> Set 2D overlay
  - Imports images, binarizes them, detects 2D line segments using a Hough transform, and overlays the detected lines as ROIs.
- **Quantify filaments** · support 1 · canvas-note-section · `cluster-0285`
  - shape: 8 nodes / 7 edges — Measure filament components -> Remove table column -> Add annotations as columns -> Rename table column -> Apply expression per row -> Filter table rows -> M…
  - Processes measured filament components into a cleaned, annotated, and filtered table, then merges rows and exports a filament quantification summary.
- **Visualize filaments as labeled images** · support 1 · canvas-note-section · `cluster-0292`
  - shape: 8 nodes / 7 edges — Table to 2D text ROI -> Convert filaments to labels (legacy) -> Convert labels to RGB -> Blend images -> Export image -> Export image -> Convert 2D ROI to RG…
  - Converts filament data into a label image, blends it with a base image, optionally overlays text ROIs, and exports the resulting images.

### machine-learning (14)

- **Cellpose segmentation with imported model** · support 21 · geometric-cluster · `cluster-0009`
  - shape: 3 nodes / 2 edges — File list -> Import Cellpose model from file -> Cellpose segmentation (3.x)
  - Loads a Cellpose model from a file and connects it to a Cellpose segmentation step for inference.
- **Cellpose segmentation** · support 14 · canvas-note-section · `cluster-0014`
  - shape: 9 nodes / 8 edges — Auto threshold 2D (8-bit) -> Set annotation (expression) -> Auto threshold 2D (8-bit) -> Gaussian blur 2D -> Gaussian blur 2D -> Cellpose prediction (2.x) ->…
  - Preprocesses 2D images and runs Cellpose inference to generate cell ROIs, optionally combining the resulting ROIs with other image channels.
- **Cellpose segmentation with image preprocessing** · support 7 · geometric-cluster · `cluster-0037`
  - shape: 9 nodes / 9 edges — Ridge detector -> File list -> Import Cellpose model from file -> Gaussian blur 2D -> Histogram-based contrast enhancer -> Fast image arithmetics 2D -> Cellp…
  - Loads a Cellpose model from files, preprocesses input images with blurring, contrast enhancement, and arithmetic, then segments them and converts the resulting ROIs to RGB images.
- **Retrain and test a Cellpose segmentation model** · support 6 · canvas-note-section · `cluster-0044`
  - shape: 12 nodes / 11 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation training (3.x) -> Remove annotation -> Cellpose segmentation (3.x) -> Gaussian blur 2D ->…
  - Fine-tunes a pretrained Cellpose model and runs inference on images, converting the probability output into thresholded, watershed-separated particle ROIs.
- **Pretrained segmentation model inference with ROI conversion** · support 6 · canvas-note-section · `cluster-0046`
  - shape: 6 nodes / 5 edges — Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Cellpose segmentation (3.x) -> Con…
  - Runs a pretrained segmentation model on images and converts the resulting ROIs to RGB for visualization.
- **Cellpose training from annotated images** · support 6 · geometric-cluster · `cluster-0050`
  - shape: 7 nodes / 7 edges — Illumination correction 2D -> Scale 2D image -> Annotate with data -> Split data randomly (percentage) -> Image to OME Image -> Cellpose training (2.x) -> Im…
  - Preprocesses and annotates images, randomly splits them into training, validation, and test sets, and trains a Cellpose model while exporting test images as OME images.
- **Cellpose training and prediction** · support 4 · compartment · `cluster-0081`
  - shape: 10 nodes / 11 edges — Remove overlay -> File list -> Import Cellpose model -> Annotate with data -> Split data randomly (percentage) -> Cellpose training (2.x) -> Cellpose predict…
  - Splits annotated data into training, validation, and test sets, trains a Cellpose model, runs prediction on the test set, exports the trained model, and converts predicted ROIs to RGB images for visualization.
- **Cellpose segmentation with ROI filtering** · support 4 · canvas-note-section · `cluster-0093`
  - shape: 12 nodes / 14 edges — Divide by maximum -> Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x segmentation model -> CLIJ2 Gaussian Blur 2D -> Cellpose segmentat…
  - Runs a pretrained Cellpose model on normalized and blurred images, filters the resulting ROIs by statistics, and converts selected ROIs into binary masks for downstream image operations.
- **Image prediction from file list** · support 3 · geometric-cluster · `cluster-0130`
  - shape: 3 nodes / 2 edges — File list -> Import image -> Omnipose prediction (0.x)
  - Imports images from a file list and runs them through an image prediction model to produce model outputs.
- **Generative model create-train-predict** · support 3 · geometric-cluster · `cluster-0134`
  - shape: 3 nodes / 2 edges — Create generation model (JIPipe2AI) -> JIPipe2AI train generation -> JIPipe2AI predict generation
  - Creates a generative model, trains it on input data, and applies the trained model to produce predictions. This is the standard end-to-end create-train-predict workflow for building and deploying a generative AI model.
- **Cellpose nuclei segmentation** · support 2 · compartment · `cluster-0190`
  - shape: 12 nodes / 11 edges — Pretrained Cellpose 3.x image restoration model -> Find or subtract background 2D -> Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x im…
  - Preprocesses images and applies pretrained Cellpose restoration and segmentation models to detect nuclei, then filters and converts the resulting ROIs.
- **Ilastik pixel classification of imported images** · support 2 · geometric-cluster · `cluster-0214`
  - shape: 4 nodes / 3 edges — File list -> Annotate with file/directory name -> Import image -> Ilastik pixel classification
  - Imports a list of image files, annotates each with its file/directory name, and applies Ilastik pixel classification to the images.
- **Train and predict segmentation model** · support 2 · compartment · `cluster-0216`
  - shape: 5 nodes / 5 edges — Create segmentation model (JIPipe2AI) -> Annotate with data -> Split data randomly (percentage) -> JIPipe2AI train segmentation -> JIPipe2AI predict segmenta…
  - Creates a segmentation model, annotates input data, splits it into training and validation sets, trains the model, and applies it to predict segmentation.
- **Train image classifier from folder-labeled images** · support 1 · geometric-cluster · `cluster-0254`
  - shape: 6 nodes / 6 edges — File list -> Annotate with file/directory name -> Import image -> Annotate with data -> Split data randomly (percentage) -> Train model (labeled images)
  - Builds a labeled image dataset by importing images from a file list and deriving class labels from directory names, then randomly splits the data into training and test subsets and trains a classification model on the labeled images.

### tracking (4)

- **Spot tracking and measurement** · support 14 · canvas-note-section · `cluster-0016`
  - shape: 8 nodes / 7 edges — LAP Tracker -> Mask detector -> Detect spots -> Annotate with source slot -> Track spots -> Filter tracks -> Measure tracks -> JFreeChart Histogram plot
  - Detects spots in images, tracks them over time, filters and measures the resulting tracks, and plots a histogram of track measurements.
- **Spot tracking and measurement pipeline** · support 12 · canvas-note-section · `cluster-0021`
  - shape: 11 nodes / 10 edges — Mask detector -> LAP Tracker -> Convert 2D ROI to mask -> Annotate with source slot -> Morphological operation 2D -> Merge hyperstack C/Z/T (single input) ->…
  - Detects spots in image stacks, tracks them over time, filters and measures tracks, and plots a histogram of the measured track values.
- **Track and annotate spots** · support 3 · geometric-cluster · `cluster-0116`
  - shape: 4 nodes / 3 edges — Track spots -> Filter tracks -> Rename annotation -> Set annotation (expression)
  - Tracks spots, filters the resulting tracks, and renames or sets a track annotation using an expression.
- **Spot tracking pipeline** · support 1 · geometric-cluster · `cluster-0244`
  - shape: 6 nodes / 5 edges — File list -> Import image -> Detect spots -> Filter spots -> Track spots -> Filter tracks
  - Imports a set of images, detects and filters spots, then tracks spots across frames and filters the resulting tracks.

### tables (4)

- **Merge prepared table columns** · support 3 · canvas-note-section · `cluster-0111`
  - shape: 4 nodes / 3 edges — Rename & modify columns (expression) -> Remove table column -> Rename table column -> Merge table columns (simple)
  - Prepares two table inputs by renaming or removing columns, then merges the selected columns into a single table.
- **Clean and reorder table columns** · support 3 · canvas-note-section · `cluster-0112`
  - shape: 3 nodes / 2 edges — Remove table column -> Apply expression per row -> Sort table columns
  - Removes an unwanted column, applies a row-wise expression to transform or derive values, and sorts columns into a consistent order.
- **Table row selection, sorting, and column renaming** · support 2 · geometric-cluster · `cluster-0165`
  - shape: 3 nodes / 2 edges — Select table rows -> Sort table rows -> Rename table column
  - Selects or filters table rows, sorts the selected rows, and renames a column to produce a cleaned and ordered table.
- **Import and normalize result tables** · support 1 · geometric-cluster · `cluster-0260`
  - shape: 3 nodes / 2 edges — File list -> Import results table -> Add missing rows (series)
  - Imports tabular results from a list of files and adds missing rows for expected series, producing a consistent table for downstream analysis.

### statistics (10)

- **Aggregate 2D ROI statistics** · support 11 · geometric-cluster · `cluster-0022`
  - shape: 4 nodes / 3 edges — Merge 2D ROI lists -> Convert 2D ROI to RGB -> Extract 2D ROI statistics -> Merge table rows
  - Merges multiple 2D ROI lists, extracts per-ROI measurements, and combines the resulting measurement tables into a single table.
- **Colocalization analysis with histogram plots** · support 6 · canvas-note-section · `cluster-0052`
  - shape: 6 nodes / 5 edges — Mask to 2D ROI -> Coloc 2 -> Add annotations as columns -> Merge table rows -> JFreeChart Histogram plot -> JFreeChart Histogram plot
  - Computes colocalization between two mask-derived ROIs, tabulates the results, and visualizes them as histograms.
- **Measure and export label overlap** · support 4 · geometric-cluster · `cluster-0086`
  - shape: 6 nodes / 4 edges — Divide by maximum -> Measure label overlap -> Add annotations as columns -> Add annotations as columns -> Merge table rows -> Export table
  - Normalizes values by their maximum, measures overlap between labels, and exports the resulting metrics as a table.
- **Extract and export filtered label statistics** · support 4 · canvas-note-section · `cluster-0094`
  - shape: 9 nodes / 8 edges — Connected components labeling 2D -> Filter labels by statistics 2D -> Extract label statistics 2D -> Rename table column -> Export data -> Add annotations as…
  - Labels connected components in 2D images, filters labels by statistics, extracts per-label measurements, and exports the resulting table with added annotations.
- **Thresholded local maxima histogram** · support 4 · canvas-note-section · `cluster-0108`
  - shape: 9 nodes / 8 edges — Local maxima 2D -> Auto threshold 2D (8-bit) -> Set to value (grayscale) -> Image to pixel value/count histogram -> Filter table rows -> JFreeChart XY line p…
  - Detects local maxima, thresholds them into a mask, and computes a pixel-value/count histogram for the masked region, then exports the histogram table and plot.
- **Table statistics histogram** · support 3 · compartment · `cluster-0113`
  - shape: 12 nodes / 9 edges — Rename & modify columns (expression) -> Remove table column -> Remove table column -> Annotate data with table values -> Rename table column -> Apply express…
  - Cleans and reshapes a table, computes per-row values, and produces a histogram plot of the resulting statistics.
- **Particle nearest-neighbor distance histogram** · support 3 · canvas-note-section · `cluster-0121`
  - shape: 7 nodes / 6 edges — IJ3D Find particles 3D -> Extract IJ3D ROI statistics -> Export data -> Run Python script (iterating) -> JFreeChart Histogram plot -> Export data -> Export i…
  - Detects particles in 3D images, extracts their measurements, computes nearest-neighbor distances, and summarizes the distances in a histogram with exported data and plot.
- **Pairwise ROI statistics histogram** · support 2 · compartment · `cluster-0137`
  - shape: 4 nodes / 4 edges — Convert spots to ROI -> Extract pairwise 2D ROI statistics -> JFreeChart Histogram plot -> Export image
  - Converts spot annotations into ROIs, computes pairwise 2D ROI measurements, and exports a histogram of those measurements.
- **Thresholded ROI image statistics** · support 2 · compartment · `cluster-0163`
  - shape: 11 nodes / 11 edges — Auto threshold 2D (8-bit) -> Morphological operation 2D -> Find particles 2D -> Merge 2D ROI lists -> Set 2D ROI properties -> 2D ROI calculator (AND/OR/XOR)…
  - Creates particle ROIs from a thresholded and morphologically processed image, then extracts and sorts image statistics for those ROIs.
- **Measure object features** · support 1 · compartment · `cluster-0243`
  - shape: 3 nodes / 0 edges — Measure spots -> Measure tracks -> Measure edges
  - Applies measurement components to spots, tracks, and edges to extract numeric features from image-analysis objects.

### plotting (19)

- **Pairwise ROI statistics histogram** · support 8 · compartment · `cluster-0029`
  - shape: 11 nodes / 12 edges — Extract pairwise IJ3D ROI statistics -> Extract pairwise IJ3D ROI statistics -> JFreeChart Histogram plot -> Export data -> JFreeChart Histogram plot -> Expo…
  - Compute pairwise ROI measurements and summarize their distributions as histogram plots, exporting both the data and rendered chart images.
- **Colocalization result histograms** · support 8 · canvas-note-section · `cluster-0033`
  - shape: 5 nodes / 4 edges — Coloc 2 -> Add annotations as columns -> Merge table rows -> JFreeChart Histogram plot -> JFreeChart Histogram plot
  - Takes colocalization results, adds annotation columns, merges the resulting rows, and displays the values as histogram plots.
- **Export histogram plot as image** · support 5 · geometric-cluster · `cluster-0059`
  - shape: 3 nodes / 2 edges — JFreeChart Histogram plot -> Create montage -> Export image
  - Creates a histogram plot, wraps it in a montage, and exports the resulting image.
- **Pairwise ROI statistics histogram** · support 5 · canvas-note-section · `cluster-0060`
  - shape: 4 nodes / 3 edges — Extract pairwise IJ3D ROI statistics -> Export data -> JFreeChart Histogram plot -> Export image
  - Extracts pairwise ROI statistics, exports the measurements, and generates a histogram plot of the values.
- **Filament measurement histogram** · support 4 · canvas-note-section · `cluster-0069`
  - shape: 4 nodes / 3 edges — Measure filament components -> Filter tables -> Filter table rows -> JFreeChart Histogram plot
  - Measures properties of filament components, filters the resulting table, and plots a histogram of the selected measurement values.
- **Annotated statistical plots from table data** · support 4 · canvas-note-section · `cluster-0073`
  - shape: 11 nodes / 10 edges — Merge table columns (simple) -> Merge table rows -> Apply expression per row -> JFreeChart Statistical line category plot -> Annotate with source slot -> JFr…
  - Merges and transforms tabular data, computes per-row values, and produces annotated line and bar category plots with exported table and image outputs.
- **Multi-chart visualization** · support 4 · geometric-cluster · `cluster-0074`
  - shape: 3 nodes / 0 edges — JFreeChart XY scatter plot -> JFreeChart Box plot -> JFreeChart XY line plot
  - Arranges several chart nodes to display data as scatter, box, and line plots for exploratory visualization.
- **Summarized XY line plot** · support 4 · geometric-cluster · `cluster-0079`
  - shape: 4 nodes / 3 edges — Summarize table -> Add annotations as columns -> Merge table rows -> JFreeChart XY line plot
  - Summarizes tabular data, attaches annotations as columns, merges the resulting rows, and creates an XY line plot.
- **Histogram plot export** · support 4 · canvas-note-section · `cluster-0090`
  - shape: 5 nodes / 4 edges — Run Python script (iterating) -> JFreeChart Histogram plot -> Export data -> Render to RGB -> Export image
  - Generates a histogram from script-produced data and exports both the underlying data and a rendered image of the plot.
- **Pixel color vs. X scatter plot** · support 4 · canvas-note-section · `cluster-0107`
  - shape: 9 nodes / 8 edges — Morphological operation 2D -> Set to color (RGB) -> Get pixels as table -> Filter table rows -> Apply expression per row -> JFreeChart XY scatter plot -> JFr…
  - Extracts pixel data from a masked image, computes a per-pixel expression, and plots color values against a selected variable as scatter charts.
- **Histogram plot from table data** · support 3 · geometric-cluster · `cluster-0124`
  - shape: 5 nodes / 4 edges — Merge table columns (simple) -> Add annotations as columns -> Merge table rows -> Split table by columns -> JFreeChart Histogram plot
  - Merges and annotates tabular data, splits it by columns, and creates a histogram plot of the resulting values.
- **Export histogram plot image** · support 2 · geometric-cluster · `cluster-0154`
  - shape: 5 nodes / 4 edges — Extract 2D ROI statistics -> JFreeChart Histogram plot -> Convert image to RGB -> Create montage -> Export image
  - Creates a histogram from 2D ROI statistics and exports the resulting plot as an RGB image montage.
- **Image pixel histogram pie plot** · support 2 · canvas-note-section · `cluster-0178`
  - shape: 8 nodes / 7 edges — Image to pixel value/count histogram -> Apply expression per row -> Summarize table -> Add annotations as columns -> Merge table rows -> JFreeChart 2D pie pl…
  - Summarizes an image's pixel value/count histogram into a table and renders pie plots of the resulting pixel categories.
- **Render and export XY scatter plot** · support 2 · geometric-cluster · `cluster-0195`
  - shape: 3 nodes / 2 edges — JFreeChart XY scatter plot -> Render to RGB -> Export image
  - Creates an XY scatter plot, renders it to an RGB image, and exports the image file.
- **Measurement histogram export** · support 2 · compartment · `cluster-0197`
  - shape: 11 nodes / 8 edges — Measure spots -> Measure tracks -> Measure edges -> JFreeChart Histogram plot -> Iteration step slicer -> Iteration step slicer -> Export image -> JFreeChart…
  - Computes measurements for image features and creates histogram plots, optionally sliced by iteration step, then exports the plots as images.
- **Histogram plot of ROI measurements** · support 2 · canvas-note-section · `cluster-0226`
  - shape: 4 nodes / 3 edges — Extract IJ3D ROI statistics -> JFreeChart Histogram plot -> Export data -> Export image
  - Takes ROI measurement data, creates a histogram plot, and exports both the measurement table and the plot image.
- **XY line plot from ROI annotations** · support 1 · canvas-note-section · `cluster-0229`
  - shape: 3 nodes / 2 edges — Annotate with 2D ROI properties -> Convert to annotation table -> JFreeChart XY line plot
  - Annotates 2D ROIs with their properties, converts the annotations into a table, and plots them as an XY line chart.
- **Histogram line plot** · support 1 · geometric-cluster · `cluster-0262`
  - shape: 4 nodes / 3 edges — Key/Value Histogram 5D -> Merge table columns (simple) -> Apply expression per row -> JFreeChart XY line plot
  - Converts a key/value histogram into an XY line plot by merging histogram columns, applying a row expression, and feeding the result to a line chart.
- **Export histogram plot from merged table rows** · support 1 · canvas-note-section · `cluster-0301`
  - shape: 3 nodes / 2 edges — Merge table rows -> JFreeChart Histogram plot -> Export image
  - Merges table rows into a single dataset, renders a histogram plot, and exports the plot as an image.

### export (10)

- **Export annotated summary tables** · support 8 · compartment · `cluster-0026`
  - shape: 5 nodes / 2 edges — Export table as multi-sheet XLSX -> Merge table rows -> Add annotations as columns -> Merge table rows -> Export table as multi-sheet XLSX
  - Merges table rows, adds annotation columns, and writes the resulting tables to a multi-sheet XLSX file.
- **Export cleaned annotation table** · support 6 · geometric-cluster · `cluster-0047`
  - shape: 11 nodes / 10 edges — Convert to annotation table -> Remove table column -> Remove table column -> Remove table column -> Remove table column -> Remove table column -> Remove tabl…
  - Converts annotations to a table, removes selected columns, applies a script-based table modification, and exports the resulting table.
- **Export tables and images** · support 6 · compartment · `cluster-0051`
  - shape: 11 nodes / 0 edges — Export table -> Export table -> Export image -> Export table -> Export image -> Export image -> Export table -> Export table -> Export image -> Export table …
  - Exports tabular data and image results from a pipeline, typically as a final saving step.
- **Annotate and export multiple image inputs** · support 4 · geometric-cluster · `cluster-0076`
  - shape: 7 nodes / 6 edges — IO Interface -> Annotate with source slot -> Annotate with source slot -> Annotate with source slot -> Export image -> Export image -> Export image
  - Takes several image inputs, annotates each with its source slot, and exports the annotated images.
- **Export mask as 3D scene** · support 2 · geometric-cluster · `cluster-0136`
  - shape: 3 nodes / 2 edges — Generate structure element (3D) -> Mask to 3D scene -> Export 3D scene
  - Converts a mask into a 3D scene using a generated 3D structure element and exports the resulting scene.
- **Image visualization export** · support 2 · compartment · `cluster-0218`
  - shape: 11 nodes / 9 edges — Set 2D overlay -> JFreeChart Histogram plot -> Morphological operation 2D -> Connected components labeling 2D -> Render overlay -> Convert labels to RGB -> S…
  - Renders overlays and colored connected-component labels, combines them with a histogram in a montage, and exports the resulting visualization images.
- **Export 3D ROI scene** · support 1 · geometric-cluster · `cluster-0227`
  - shape: 3 nodes / 2 edges — Color IJ3D ROI by statistics -> IJ3D ROI to 3D scene -> Export 3D scene
  - Converts 3D ROIs into a 3D scene, colors them by a selected statistic, and exports the scene.
- **Export annotated image statistics** · support 1 · geometric-cluster · `cluster-0235`
  - shape: 5 nodes / 4 edges — Extract image statistics -> Add annotations as columns -> Merge table rows -> Rename table column -> Export data (path input)
  - Extracts image statistics, joins annotation columns into the table, merges and renames the result, and exports it to a file.
- **Export images to OMERO with attached table** · support 1 · geometric-cluster · `cluster-0274`
  - shape: 3 nodes / 2 edges — Create OMERO dataset -> Upload image to OMERO dataset -> Attach table to OMERO image
  - Creates an OMERO dataset, uploads an image to it, and attaches a table to the uploaded image. It exports image data together with associated tabular metadata to OMERO.
- **Upload images from folders to OMERO** · support 1 · geometric-cluster · `cluster-0275`
  - shape: 5 nodes / 4 edges — Folder list -> List files -> Annotate with file/directory name -> Import image -> Upload image to OMERO dataset
  - Lists image files from folders, annotates them with their file/directory names, imports the images, and uploads them to an OMERO dataset.

### misc (1)

- **Merge multiple parameters** · support 12 · group-node · `cluster-0020`
  - shape: 6 nodes / 4 edges — Define multiple parameters -> Define multiple parameters -> Define multiple parameters -> Group input -> Merge parameters -> Group output
  - Defines several parameter sets and merges them into a single parameter output. This reusable group lets combined or multiplied parameter values be passed downstream.

## Rejected (for false-negative check)

54 families the model cut. The ones with high support or a good-looking title are the
places a false negative would hide.

- `cluster-0075` support 4 — **gate=noise** — shape 4n/0e
- `cluster-0105` support 4 — **gate=noise** — shape 8n/4e
- `cluster-0138` support 2 — **gate=workflow_plumbing** — shape 11n/11e
- `cluster-0144` support 2 — **gate=workflow_plumbing** — shape 7n/4e
- `cluster-0149` support 2 — **gate=workflow_plumbing** — shape 4n/3e
  - model name: 'Plot Python script outputs'
- `cluster-0150` support 2 — **gate=workflow_plumbing** — shape 9n/8e
- `cluster-0151` support 2 — **gate=workflow_plumbing** — shape 11n/10e
- `cluster-0160` support 2 — **gate=noise** — shape 7n/3e
- `cluster-0166` support 2 — **gate=workflow_plumbing** — shape 7n/5e
- `cluster-0167` support 2 — **gate=workflow_plumbing** — shape 8n/7e
- `cluster-0168` support 2 — **gate=workflow_plumbing** — shape 8n/8e
  - titles: ['Pores enclosing hyphae']
- `cluster-0174` support 2 — **gate=workflow_plumbing** — shape 10n/14e
  - titles: ['Red channel with Cellpose and variance filtering']
- `cluster-0180` support 2 — **gate=workflow_plumbing** — shape 11n/11e
- `cluster-0185` support 2 — **gate=noise** — shape 4n/1e
- `cluster-0188` support 2 — **gate=workflow_plumbing** — shape 12n/11e
  - titles: ['Time dependence']
- `cluster-0198` support 2 — **gate=workflow_plumbing** — shape 10n/8e
  - titles: ['Segmentation: nuclei']
- `cluster-0203` support 2 — **gate=workflow_plumbing** — shape 9n/9e
- `cluster-0205` support 2 — **gate=workflow_plumbing** — shape 3n/1e
- `cluster-0230` support 1 — **gate=workflow_plumbing** — shape 6n/5e
- `cluster-0234` support 1 — **gate=workflow_plumbing** — shape 6n/5e
  - titles: ['Pre-processing']
- `cluster-0237` support 1 — **gate=workflow_plumbing** — shape 7n/6e
- `cluster-0238` support 1 — **gate=workflow_plumbing** — shape 8n/7e
  - titles: ['Quantification']
- `cluster-0241` support 1 — **gate=noise** — shape 3n/2e
- `cluster-0245` support 1 — **gate=noise** — shape 3n/1e
- `cluster-0246` support 1 — **gate=workflow_plumbing** — shape 12n/11e
  - titles: ['Preprocessing']
- `cluster-0247` support 1 — **gate=workflow_plumbing** — shape 8n/6e
  - titles: ['Postprocessing']
- `cluster-0249` support 1 — **gate=workflow_plumbing** — shape 4n/3e
- `cluster-0250` support 1 — **gate=noise** — shape 5n/2e
- `cluster-0251` support 1 — **gate=workflow_plumbing** — shape 8n/7e
- `cluster-0253` support 1 — **gate=workflow_plumbing** — shape 7n/6e
- `cluster-0255` support 1 — **gate=workflow_plumbing** — shape 3n/2e
- `cluster-0257` support 1 — **gate=workflow_plumbing** — shape 5n/5e
- `cluster-0261` support 1 — **gate=workflow_plumbing** — shape 4n/3e
- `cluster-0263` support 1 — **gate=workflow_plumbing** — shape 3n/3e
- `cluster-0265` support 1 — **gate=workflow_plumbing** — shape 7n/6e
- `cluster-0270` support 1 — **gate=noise** — shape 3n/0e
- `cluster-0271` support 1 — **gate=noise** — shape 3n/0e
- `cluster-0278` support 1 — **gate=workflow_plumbing** — shape 3n/3e
  - titles: ['Connect with yeast heads']
- `cluster-0284` support 1 — **gate=workflow_plumbing** — shape 10n/9e
  - titles: ['Determine approx. yeast size by PBS']
  - model name: 'Particle size distribution from annotated region'
- `cluster-0289` support 1 — **gate=workflow_plumbing** — shape 3n/3e
  - titles: ['Classify for yeast cells and hyphae']
- `cluster-0290` support 1 — **gate=workflow_plumbing** — shape 12n/12e
  - titles: ['Determine thickness of hyphae by RPMI']
  - model name: 'Estimate object thickness from distance transform'
- `cluster-0293` support 1 — **gate=workflow_plumbing** — shape 12n/7e
  - titles: ['Hyphae analysis - AnaMorf']
- `cluster-0294` support 1 — **gate=workflow_plumbing** — shape 11n/9e
  - titles: ['File handler [1] -1st tile', 'File handler [3] - 1th tile']
- `cluster-0295` support 1 — **gate=workflow_plumbing** — shape 5n/4e
  - titles: ['Differ for object size']
- `cluster-0297` support 1 — **gate=workflow_plumbing** — shape 6n/5e
- `cluster-0299` support 1 — **gate=workflow_plumbing** — shape 6n/5e
  - titles: ['Find CZI and metadata extraction']
- `cluster-0300` support 1 — **gate=workflow_plumbing** — shape 5n/4e
  - titles: ['Cell length histogram and filaments']
- `cluster-0307` support 1 — **gate=workflow_plumbing** — shape 5n/3e
- `cluster-0308` support 1 — **gate=noise** — shape 4n/0e
  - titles: ['CellPose']
- `cluster-0312` support 1 — **gate=workflow_plumbing** — shape 6n/4e
- `cluster-0313` support 1 — **gate=workflow_plumbing** — shape 4n/3e
- `cluster-0314` support 1 — **gate=noise** — shape 3n/2e
- `cluster-0319` support 1 — **gate=workflow_plumbing** — shape 9n/8e
  - titles: ['3D inputs']
- `cluster-0321` support 1 — **gate=workflow_plumbing** — shape 6n/5e
  - titles: ['2D inputs']

## Flags (auto-noticed while inspecting the set)

**Same name, different graph** (legal — the consumer dedups on (name, graph data); listed so the
names don't read as duplicates in the browser):
- '2D particle segmentation' × 2: cluster-0012, cluster-0320
- 'Pairwise ROI statistics histogram' × 3: cluster-0029, cluster-0060, cluster-0137

**Mentions of custom macros / bespoke content in shipped descriptions** (possible one-offs in disguise):
- `cluster-0232` 'Create mask from image and ROI' (support 1) — Imports an image, enhances contrast, runs an ImageJ macro to produce a 2D ROI, and converts that ROI into a mask.

**The one `misc` assignment** (a function the LLM found unsatisfying):
- `cluster-0020` 'Merge multiple parameters' (support 12) — Defines several parameter sets and merges them into a single parameter output. This reusable group lets combined or multiplied parameter values be pas

## Artifacts

| file | what |
|---|---|
| `utils/mine-dag-patterns.py` (+ `_tests.py`) | (A) harvester, 11 tests, `selftest` |
| `utils/merge-dag-patterns.py` (+ `_tests.py`) | (B) embedder+clustering, 12 tests, `selftest` |
| `utils/judge-dag-patterns.py` (+ `_tests.py`) | (C) LLM gate, 15 tests, `selftest` |
| `utils/taxonomy-research/dag-candidates.json` (11 MB) | (A) 2029 candidates w/ locations + provenance |
| `utils/taxonomy-research/dag-clusters.json` (2.4 MB) | (B) 324 families w/ title/name evidence |
| `utils/taxonomy-research/dag-judgments.json` (476 KB) | (C) all 324 verdicts — the audit trail |
| `dag-embeddings.json`, `dag-llm-cache.json` | regenerable endpoint caches (gitignored) |

