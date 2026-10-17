# DAG Pattern Mining — Shipping Review (steps A–C)

Work item 1324 · branch `1324-node-taxonomy-core` · 2026-10-17

This is the **manual-review gate before step (D) ships the survivors**.
Regenerate after any re-run: `python3 utils/make-dag-review-report.py`.

## Funnel

| stage | in | out |
|---|---|---|
| projects scanned | — | 510 |
| (A) candidates | — | 2029 |
| (B) families | 2029 | 324 |
| **(C) shipped** | 324 | **265** |
| (C) rejected | | 59 ({'gate_workflow_plumbing': 43, 'gate_noise': 16}) |

## The (C) naming-review fixes (measured)

| issue (from the (C) review) | before (v1) | after (this run) |
|---|---|---|
| bare "ROI" names (no 2D/3D) | 57 | **0** |
| JIPipe-internal jargon in names | 8 | **0** |
| descriptions with no in/out anchor | 197 | **9** |
| shipped count | 270 | 265 |

(The gate now sees each node's real description + ROI dimension, so it
re-judged some families — the count shift is the model using real
function evidence, not noise.)

## Support distribution of the shipped set

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
| 6 | 15 | 53 |
| 5 | 11 | 64 |
| 4 | 41 | 105 |
| 3 | 25 | 130 |
| 2 | 71 | 201 |
| 1 | 64 | 265 |

**The (D) curation call (your decision 1):** the plan specifies no support
floor (the §1 rule is name-derivability, which the gate enforces). If you
want a tighter library:

| option | ships |
|---|---|
| no floor (plan literal) | 265 |
| support ≥ 2 | 201 |
| support ≥ 3 | 130 |
| support ≥ 5 | 64 |

## Shipped by function

| function | shipped |
|---|---|
| data | 7 |
| annotations | 5 |
| images | 91 |
| rois | 65 |
| filaments | 14 |
| machine-learning | 21 |
| tracking | 7 |
| tables | 8 |
| statistics | 15 |
| plotting | 17 |
| export | 15 |

## (D) hard blocker: patterns on unregistered node ids

**15 shipped patterns reference a node id not in the 6.0.0 launcher
classpath** (the corpus predates 6.0.0; some IDs were renamed, some live in
optional plugins). The launcher guard `everyPatternGraphDeserializes` asserts
every node id resolves, so these FAIL unless remapped or excluded in (D).
See `dag-stale-id-plan.json` for the remap/exclude list.

**Renamable** (all stale ids have a clean current successor): 3
- `cluster-0133` support 3 'Tile multiple images into a grid' → ['ij1-transform-tile']
- `cluster-0318` support 1 'Batch export OME-ZARR images from folders' → ['import-ome-zarr-from-directory-as-imgplus']
- `cluster-0242` support 1 'Detect lines in 2D images' → ['ij1-transform-tile']

**Exclude or plugin-gate** (optional-plugin nodes absent from the default launcher): 12
- `cluster-0058` support 5 'Preprocess microscopy image stacks' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0072` support 4 'Project and correct a multi-dimensional image' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0091` support 4 'Create extended depth of focus projections from microscopy images' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0103` support 4 'Create 2D/3D particle ROIs and label edge images' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ReduceLabelsToLabelEdges']
- `cluster-0134` support 3 'Train and predict with a generative model' → ['create-jipipe2ai-generation-model', 'jipipe2ai-predict-generation', 'jipipe2ai-train-generation-model']
- `cluster-0140` support 2 'Segment 2D images with edge projection and watershed' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0152` support 2 'Compute extended depth-of-focus projection from selected image planes' → ['clij:plugin:net.haesleinhuepf.clijx.clij2wrappers.ExtendedDepthOfFocusSobelProjection']
- `cluster-0169` support 2 'Create filament masks from 3D image stacks' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0186` support 2 'Colorized extended depth-of-focus image projection' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0216` support 2 'Train and predict image segmentation' → ['create-jipipe2ai-segmentation-model', 'jipipe2ai-predict-image', 'jipipe2ai-train-image-model']
- `cluster-0153` support 2 'Detect 2D particle ROIs from microscopy image stacks' → ['clij:plugin:net.haesleinhuepf.clijx.clij2wrappers.ExtendedDepthOfFocusSobelProjection']
- `cluster-0254` support 1 'Train a deep-learning model from labeled images' → ['deep-learning-train-image-model']

Shippable after remap + exclude: **253**.

## All shipped patterns (for review)

Grouped by function, then support (descending). `cluster-XXXX` traces back
into `dag-clusters.json` / the corpus.

### data (7)

- **List subfolders and files from input folders** · support 92 · group-node · `cluster-0001`
  - 9 nodes / 9 edges — Group input -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> Annotate with file/directory name -> List files -…
  - Takes input folders, lists their subfolders and contained files, annotates each path with its name, and filters the resulting paths. It produces annotated file paths and subfolder paths for downstream steps.
- **Import microscopy images from folders** · support 14 · geometric-cluster · `cluster-0015`
  - 8 nodes / 7 edges — Folder list -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Bio-Formats importe…
  - Takes a root folder, enumerates its subfolders and image files, filters the selected file paths, and imports the microscopy images with Bio-Formats. It produces image data annotated with file/folder names and basic image properties.
- **Import images from folders with folder labels** · support 4 · compartment · `cluster-0100`
  - 4 nodes / 3 edges — Folder list -> Annotate with file/directory name -> List files -> Import image
  - Takes a set of folders and produces imported image data for each image file found in them, with the folder/file name attached as an annotation. It lists the folders, annotates each path by name, lists matching files, and imports them as ImageJ images.
- **Load images from folders** · support 4 · geometric-cluster · `cluster-0101`
  - 5 nodes / 4 edges — Folder list -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Import image
  - Takes a list of folders, enumerates the image files inside each folder, and imports them as image data. It preserves the folder and file names as annotations so downstream steps can identify each image.
- **List files in folders and extract name-based metadata** · support 4 · geometric-cluster · `cluster-0102`
  - 5 nodes / 4 edges — Folder list -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Set/Edit annotations
  - Takes specified folders, lists the files in each folder, and creates or edits annotations from folder and file names, producing a table of file paths with name-derived metadata.
- **Load JSON files and extract selected fields** · support 2 · geometric-cluster · `cluster-0217`
  - 3 nodes / 2 edges — File list -> Import JSON -> Annotate with JSON values
  - Takes a list of files, imports each file as JSON, and extracts chosen JSON fields to attach as metadata to the corresponding data items.
- **Load images from folders** · support 1 · canvas-note-section · `cluster-0228`
  - 3 nodes / 2 edges — Folder list -> List files -> Bio-Formats importer
  - Takes a set of folders, enumerates the image files inside them, and imports those files with Bio-Formats to produce microscopy image data with OME metadata.

### annotations (5)

- **Extract OME image metadata as annotations** · support 12 · group-node · `cluster-0019`
  - 5 nodes / 4 edges — Group input -> Extract OME XML -> Prettify XML -> Annotate with XML values -> Group output
  - Takes an OME image and produces its OME-XML metadata as formatted XML data annotated with selected XPath values. It extracts the OME-XML, prettifies it, and attaches chosen metadata values as annotations.
- **Add computed annotations to data** · support 8 · canvas-note-section · `cluster-0032`
  - 4 nodes / 3 edges — Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Merge annotations
  - Takes a data item, sets several annotations using expressions, and merges those annotations back onto the same data.
- **Filter three inputs by annotation** · support 5 · group-node · `cluster-0061`
  - 5 nodes / 6 edges — Group input -> Split & filter by annotation -> Split & filter by annotation -> Split & filter by annotation -> Group output
  - Takes three separate data inputs and applies a split/filter-by-annotation step to each, producing three corresponding outputs that have been selected or split according to the chosen annotation values.
- **Read text from images and attach it to data** · support 2 · canvas-note-section · `cluster-0177`
  - 5 nodes / 4 edges — Crop 2D image -> Find text (Tesseract OCR) -> Filter table rows -> Filter tables -> Annotate data with table values
  - Crops 2D images, detects text with Tesseract OCR, filters the OCR result table, and attaches the remaining text values to the input data as annotations.
- **Import images and attach metadata** · support 2 · geometric-cluster · `cluster-0207`
  - 3 nodes / 2 edges — Bio-Formats importer -> Set annotation (expression) -> Set annotation (expression)
  - Takes microscopy image files and produces image data with two expression-derived annotations attached to each image.

### images (91)

- **Import, split, and colorize microscopy images** · support 26 · compartment · `cluster-0007`
  - 9 nodes / 7 edges — Bio-Formats importer -> Project user path -> Reorder dimensions -> Annotate with image properties -> Set/Edit annotations -> Filter by annotation -> Split ch…
  - Takes microscopy image files from a project path, reorders stack dimensions, annotates image properties, filters for raw datasets, splits multichannel images into separate greyscale channels, and assigns color maps to selected channels.
- **Split multichannel images into separate channels** · support 15 · group-node · `cluster-0013`
  - 12 nodes / 28 edges — Group input -> Annotate with image properties -> Filter by annotation -> Filter by annotation -> Filter by annotation -> Filter by annotation -> Split channe…
  - Takes multichannel images and produces separate single-channel images, using image properties to route 2-, 3-, 4-, or 5-channel images to the appropriate channel-splitting step. The result is up to five individual channel images for downstream analysis.
- **Prepare cropped images and cleaned 2D ROI masks from folders** · support 13 · compartment · `cluster-0017`
  - 11 nodes / 11 edges — Folder list -> List subfolders -> Annotate with file/directory name -> List files -> List files -> Import ROI -> Annotate with file/directory name -> Import …
  - Takes folders containing image files and 2D ROI files, imports each image and ROI list, crops the image, and converts the 2D ROIs into a mask that is cleaned with a morphological operation. It produces cropped images and cleaned 2D ROI masks for downstream analysis.
- **Import and split microscopy images with color maps and metadata** · support 10 · compartment · `cluster-0023`
  - 10 nodes / 5 edges — Set LUT (color map) -> Bio-Formats importer -> Set LUT (color map) -> Set LUT (color map) -> Project user path -> Reduce & split hyperstack (Expression) -> A…
  - Takes microscopy image files and produces separate image stacks for selected channels or time points, each with a predefined color map and annotations containing image properties. It imports files with Bio-Formats, applies color maps, splits dimensions by expression, and adds or edits annotations.
- **Import images with filename labels** · support 10 · geometric-cluster · `cluster-0025`
  - 3 nodes / 2 edges — File list -> Annotate with file/directory name -> Import image
  - Takes a list of image file names, labels each file with its file or directory name, and imports each file as an ImageJ image. It produces image data items that carry the source file name as a label.
- **Segment objects from images by thresholding and labeling** · support 8 · canvas-note-section · `cluster-0031`
  - 9 nodes / 8 edges — Gaussian blur 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Median filter 2D -> Morphological hole filling 2D -> Connected components labeling 3D…
  - Takes an image and produces a binary mask of segmented objects. It smooths and auto-thresholds the image, cleans the mask, labels connected components in 3D, filters labels by an expression, and converts the selected labels back to a mask.
- **Load and denoise images from folders** · support 7 · compartment · `cluster-0035`
  - 10 nodes / 8 edges — Folder list -> Project user path -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -…
  - Takes a configured project folder, lists subfolders and image files, annotates and filters the paths, imports each image, and applies a 2D median filter to reduce noise in each channel or slice. It produces denoised image data with folder and file annotations.
- **Detect and enhance line-like structures in 2D images** · support 7 · geometric-cluster · `cluster-0036`
  - 9 nodes / 8 edges — Folder list -> Import image -> Annotate with image properties -> Split & filter by annotation -> Crop 2D image -> Ridge detector -> Gaussian blur 2D -> Histo…
  - Takes image files from folders, imports and crops the 2D images, detects a line/ridge mask, smooths and contrast-enhances that mask, and combines it with the cropped image to produce images highlighting line-like structures.
- **Import, denoise, and threshold 8-bit images** · support 6 · geometric-cluster · `cluster-0039`
  - 5 nodes / 4 edges — File list -> Annotate with file/directory name -> Import image -> Median filter 2D -> Auto threshold 2D (8-bit)
  - Takes a list of image files, imports each as an ImageJ image, applies a 2D median filter to reduce noise, and auto-thresholds each 8-bit slice. It produces thresholded images ready for downstream segmentation or measurement.
- **Convert a table of pixel values into an image** · support 6 · geometric-cluster · `cluster-0041`
  - 6 nodes / 5 edges — File list -> Import results table -> Rename table column -> Convert table to image -> Tile image 2D -> Un-Tile image 2D
  - Takes a list of CSV/XLSX results-table files, imports and renames the table, then converts the table of pixel values into a 2D image. It tiles and untiles the image to assemble the final image.
- **Create particle masks from images** · support 6 · canvas-note-section · `cluster-0042`
  - 7 nodes / 6 edges — Median filter 2D -> Auto threshold 2D (8-bit) -> Median filter 2D -> Orientation features 2D (OrientationJ) -> Auto threshold 2D (8-bit) -> Morphological ope…
  - Takes a 2D image and produces a cleaned binary mask of particles by median filtering, automatic thresholding, extracting orientation energy, re-thresholding, and applying morphological operations and hole filling.
- **Create 3D masks from measured 3D ROIs** · support 6 · canvas-note-section · `cluster-0043`
  - 4 nodes / 3 edges — Split into individual IJ3D ROI lists -> Extract IJ3D ROI statistics -> Annotate data with table values -> IJ3D ROI to mask
  - Takes 3D ROI lists and an optional reference image, splits them into individual 3D ROIs, extracts 3D ROI statistics, and annotates each ROI with those values. It then converts the annotated 3D ROIs into a greyscale 3D mask image marking the pixels covered by the ROIs.
- **Export colorized multichannel images with scale bars** · support 6 · canvas-note-section · `cluster-0045`
  - 9 nodes / 10 edges — Split channels -> Set LUT (color map) -> Set LUT (color map) -> Set LUT (color map) -> Blend images -> Copy physical dimensions -> Draw scale bar -> Export i…
  - Takes a multichannel image, splits it into individual channels, applies a color map to each channel, blends the channels into one RGB image, copies physical pixel sizes, draws a scale bar, and exports the final image.
- **Import microscopy images and convert to 32-bit grayscale** · support 6 · geometric-cluster · `cluster-0049`
  - 4 nodes / 3 edges — Set/Edit annotations -> Set annotation (expression) -> Bio-Formats importer -> Convert image to 32-bit
  - Takes microscopy image files, imports them with Bio-Formats, applies annotation expressions, and produces 32-bit grayscale images for quantitative analysis.
- **Detect tubular structures in 2D images** · support 6 · geometric-cluster · `cluster-0054`
  - 6 nodes / 5 edges — Find or subtract background 2D -> Auto threshold 2D (8-bit) -> Median filter 2D -> Frangi vesselness -> Divide by maximum -> Auto threshold 2D (8-bit)
  - Takes 2D image data and produces a binary mask highlighting tubular or vessel-like structures. It applies background subtraction, thresholding, median filtering, Frangi vesselness, normalization, and a final threshold to isolate line-shaped features.
- **Detect spots from cell segmentation 2D ROI masks** · support 5 · geometric-cluster · `cluster-0057`
  - 7 nodes / 7 edges — Split by dimension -> Filter by annotation -> Cellpose prediction (2.x) -> Convert 2D ROI to mask -> Merge 2D slices into hyperstack -> Detect spots -> Filte…
  - Produces a filtered set of spots from cell segmentation masks. It takes an image stack, selects matching slices by annotation, runs Cellpose to segment cells, converts the resulting 2D ROIs to masks, merges the masks into a stack, and detects and quality-filters spots with TrackMate.
- **Preprocess microscopy image stacks** ⚠ unregistered id · support 5 · compartment · `cluster-0058`
  - 7 nodes / 5 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumi…
  - Takes microscopy image files and produces a corrected image stack by importing with Bio-Formats, annotating image properties, splitting a chosen dimension, applying an extended depth-of-focus Sobel projection, correcting 2D illumination, and merging the processed slices back into a stack.
- **Import images and denoise with median filter** · support 4 · compartment · `cluster-0067`
  - 3 nodes / 2 edges — File list -> Import image -> Median filter 2D
  - Takes a list of image files, imports each as an image, and applies a 2D median filter to each channel or slice. It produces denoised images suitable for downstream analysis.
- **Subtract a filament mask from an image to highlight nodules** · support 4 · canvas-note-section · `cluster-0070`
  - 11 nodes / 13 edges — Invert pixel colors/values -> Morphological operation 2D -> Set to color (RGB) -> Set to value (grayscale) -> Euclidean distance transform 2D -> Set filament…
  - Takes an image and filament data, derives a binary mask from the inverted image, computes a distance transform to set each filament vertex radius, converts the filaments to a mask, and subtracts that mask from the inverted image to produce an image highlighting nodule-like structures.
- **Project and correct a multi-dimensional image** ⚠ unregistered id · support 4 · group-node · `cluster-0072`
  - 7 nodes / 6 edges — Group input -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumination correction 2D -> Merge …
  - Takes a multi-dimensional image, splits it by a selected dimension, applies an extended depth-of-focus Sobel projection and 2D illumination correction to each part, and merges the results into a 2D image stack.
- **Normalize and combine labeled images** · support 4 · geometric-cluster · `cluster-0085`
  - 9 nodes / 10 edges — Split & filter by annotation -> Filter by annotation (If else) -> Filter by annotation (If else) -> Divide by maximum -> Divide by maximum -> Divide by maxim…
  - Takes image data with categorical labels and separates it into branches by label. It normalizes each branch by its global maximum and applies 2D image arithmetic to produce combined or transformed normalized images.
- **Create extended depth of focus projections from microscopy images** ⚠ unregistered id · support 4 · compartment · `cluster-0091`
  - 7 nodes / 4 edges — Bio-Formats importer -> Custom extended depth of focus (Z/C/T) -> Project user path -> Annotate with image properties -> Set/Edit annotations -> Set LUT (col…
  - Takes microscopy image files, imports them with OME metadata, adds image-property annotations, applies a color map, and produces an extended depth of focus projection by selecting the sharpest pixel along Z/C/T using a Sobel-based score.
- **Add a ring around labeled objects and export RGB image** · support 4 · canvas-note-section · `cluster-0092`
  - 6 nodes / 5 edges — Get label boundaries -> Dilate labels -> Blend images -> Adjust displayed contrast -> Render to RGB -> Export image
  - Produces an exported RGB image in which labeled objects are outlined by a dilated boundary ring. It creates a label-boundary mask, dilates it, blends it with the input image, adjusts display contrast, renders to RGB, and writes the image to a file.
- **Create a binary mask from an image** · support 4 · canvas-note-section · `cluster-0095`
  - 4 nodes / 3 edges — Divide by maximum -> CLIJ2 Gaussian Blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D
  - Takes a greyscale image and produces a binary mask by normalizing intensities, smoothing, auto-thresholding, and filling holes.
- **Create binary masks from 8-bit images** · support 4 · canvas-note-section · `cluster-0098`
  - 5 nodes / 4 edges — Local auto threshold 2D (Bernsen, 8-bit) -> Median filter 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Set LUT (two colors)
  - Takes 8-bit 2D images and applies local thresholding, median filtering, Gaussian smoothing, and global auto-thresholding to produce a binary mask. It then assigns a two-color display LUT for visualization.
- **Dilate, fill holes, and erode a binary mask** · support 3 · group-node · `cluster-0110`
  - 5 nodes / 4 edges — Group input -> Morphological operation 2D -> Morphological hole filling 2D -> Morphological operation 2D -> Group output
  - Takes a 2D binary mask and produces a cleaned mask by applying a 2D morphological operation, filling holes in the binary image, and applying a second 2D morphological operation, typically dilation followed by erosion.
- **Preprocess and threshold 2D image slices** · support 3 · canvas-note-section · `cluster-0117`
  - 11 nodes / 10 edges — Split by dimension -> Set/Edit annotations -> Set/Edit annotations -> Set LUT (color map) -> Find or subtract background 2D -> Histogram-based contrast enhan…
  - Takes a multi-dimensional image, splits it into 2D slices, sets annotations and a color map, then applies background subtraction, contrast enhancement, thresholding, and median/Gaussian smoothing before merging the slices back into a stack. The result is a cleaned, thresholded 2D image stack ready for downstream analysis.
- **Segment and label objects in images** · support 3 · canvas-note-section · `cluster-0118`
  - 6 nodes / 5 edges — Find or subtract background 2D -> Set LUT (color map) -> Morphological Feature Contrast (MFC) 2D -> Median filter 2D -> Auto threshold 2D (8-bit) -> Connecte…
  - Takes an image, subtracts background, sets a display color map, enhances morphological feature contrast, median-filters, auto-thresholds, and outputs a connected-component label image, with 3D components when 3D data is supplied.
- **Label connected components in 3D images** · support 3 · canvas-note-section · `cluster-0119`
  - 11 nodes / 10 edges — Divide by maximum -> Divide by maximum -> Split stack into 2D -> Z-Project -> Fast image arithmetics 2D -> Merge 2D slices into hyperstack -> Median filter 2…
  - Takes a 3D image stack, normalizes it, combines it with a Z-projection, then thresholds and median-filters the result to produce a 3D label image where each connected object has a unique value.
- **Prepare 32-bit contrast-adjusted microscopy images from folders** · support 3 · compartment · `cluster-0125`
  - 7 nodes / 3 edges — Set LUT (color map) -> Set LUT (color map) -> Set/Edit annotations -> Folder list -> Bio-Formats importer -> Histogram-based contrast enhancer -> Convert ima…
  - Takes folders of microscopy image files, imports them with Bio-Formats, sets annotations, converts them to 32-bit grayscale, applies histogram-based contrast enhancement, and assigns a color map. It produces 32-bit contrast-adjusted images with annotations and a display color map for downstream analysis.
- **Overlay 2D ROIs on an image** · support 3 · group-node · `cluster-0128`
  - 4 nodes / 4 edges — Group input -> Histogram-based contrast enhancer -> Convert 2D ROI to RGB -> Group output
  - Takes an input image and a list of 2D ROIs, applies histogram-based contrast enhancement to the image, and draws the ROIs as colored outlines, fills, and optional labels. It produces an RGB image showing the ROIs overlaid on the enhanced image.
- **Clean and enhance images** · support 3 · geometric-cluster · `cluster-0129`
  - 6 nodes / 3 edges — Remove outliers 2D -> Find or subtract background 2D -> Legacy image calculator 2D -> Find or subtract background 2D -> Median filter 3D -> Histogram-based c…
  - Takes image data, removes outliers and background, smooths with a 3D median filter, and enhances contrast, producing cleaned and contrast-adjusted images.
- **Import images and subtract 2D background** · support 3 · geometric-cluster · `cluster-0131`
  - 3 nodes / 2 edges — File list -> Import image -> Find or subtract background 2D
  - Takes a list of image files, imports each file as an ImageJ image, and outputs images with 2D background removed using a rolling-ball or paraboloid filter applied to each 2D slice.
- **Import and scale 2D images** · support 3 · geometric-cluster · `cluster-0132`
  - 3 nodes / 2 edges — File list -> Import image -> Scale 2D image
  - Takes a list of image file names, imports each file as an ImageJ image, and applies a 2D scaling transform to each image or 2D slice, producing rescaled 2D images.
- **Tile multiple images into a grid** ⚠ unregistered id · support 3 · geometric-cluster · `cluster-0133`
  - 3 nodes / 2 edges — File list -> Import image -> Tile image
  - Takes a list of image file names, imports each file as an ImageJ image, and arranges the imported images into a single tiled composite image.
- **Segment 2D images with edge projection and watershed** ⚠ unregistered id · support 2 · geometric-cluster · `cluster-0140`
  - 12 nodes / 12 edges — CLIJ2 Extended Depth Of Focus Sobel Projection: Green -> Filter by annotation -> Filter by annotation -> Histogram-based contrast enhancer -> CLIJ2 Extended …
  - Takes annotated image channels, applies extended depth of focus Sobel projection, contrast enhancement, thresholding, and median filtering, and produces labeled 2D segments using seeded watershed.
- **Create colored images from 2D ROIs** · support 2 · canvas-note-section · `cluster-0145`
  - 3 nodes / 2 edges — Annotate with file/directory name -> Import ROI -> Set to color (RGB)
  - Takes image paths and matching 2D ROI files, imports the ROIs, and applies a specified color to the image using those ROIs, producing a colored image that visualizes manual annotations.
- **Thresholded image overlay** · support 2 · canvas-note-section · `cluster-0146`
  - 6 nodes / 5 edges — Auto threshold 2D (8-bit) -> Equalize hyperstack dimensions (maximum) -> Median filter 2D -> Equalize hyperstack dimensions (maximum) -> Set LUT (color map) …
  - Takes 8-bit image data, applies auto thresholding and median filtering to create a mask, equalizes dimensions, assigns a color map, and blends the colored mask over the original image to produce a single RGB overlay.
- **Locally threshold images and overlay the mask** · support 2 · canvas-note-section · `cluster-0147`
  - 10 nodes / 9 edges — Enhance local contrast (CLAHE) -> Equalize hyperstack dimensions (maximum) -> Find or subtract background 2D -> Divide by maximum -> Gaussian blur 2D -> Morp…
  - Takes in images or image stacks, preprocesses each 2D slice with CLAHE, background subtraction, normalization, Gaussian smoothing, and MFC, then applies a local Phansalkar threshold and outputs a colored threshold mask blended over the image.
- **Import, denoise, and align microscopy images** · support 2 · geometric-cluster · `cluster-0148`
  - 10 nodes / 10 edges — Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Filter by annotation (If else) -> Bio-Formats importer -> Annota…
  - Takes a list of files, tags them with annotations, and imports only those matching a filter via Bio-Formats; each image is then denoised with a 2D median filter, background-corrected with a rolling-ball algorithm, has its stack dimensions reordered (e.g., swapping depth and time), and is aligned to a reference image with TurboReg. The result is cleaned-up, aligned microscopy images annotated with their image properties.
- **Compute extended depth-of-focus projection from selected image planes** ⚠ unregistered id · support 2 · canvas-note-section · `cluster-0152`
  - 3 nodes / 2 edges — Reduce & split hyperstack (Expression) -> Reorder dimensions -> Cli Jx Extended Depth Of Focus Sobel Projection
  - Takes a multi-plane image, selects/splits planes using expressions and reorders dimensions, then applies an extended depth-of-focus Sobel projection to produce a projected image.
- **Enhance and blend images** · support 2 · compartment · `cluster-0158`
  - 3 nodes / 1 edges — Convert image to 8-bit -> Histogram-based contrast enhancer -> Blend images
  - Takes image inputs, converts them to 8-bit grayscale, applies histogram-based contrast enhancement, and blends the enhanced images into one RGB image.
- **Load microscopy images from folders and convert to 32-bit** · support 2 · compartment · `cluster-0164`
  - 9 nodes / 7 edges — Folder list -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -…
  - Takes a list of folders, annotates each folder entry, imports microscopy image files with Bio-Formats, and converts each imported image to a 32-bit grayscale image.
- **Create filament masks from 3D image stacks** ⚠ unregistered id · support 2 · canvas-note-section · `cluster-0169`
  - 7 nodes / 6 edges — Reduce & split hyperstack (Expression) -> Extended DoF Sobel -> Set/Edit annotations -> Merge 2D slices into hyperstack -> Custom auto threshold 2D (16-bit) …
  - Takes a 3D image stack, projects it into 2D slices using an extended depth-of-focus Sobel projection, annotates and reassembles those slices, then thresholds, blurs, and applies morphology to produce a cleaned binary mask of filament-like structures.
- **Composite a resliced, color-mapped image** · support 2 · geometric-cluster · `cluster-0171`
  - 3 nodes / 2 edges — Reslice -> Set LUT (color map) -> Blend images
  - Takes an image, redefines its Z axis, applies a color map, and blends it with another image to produce a single RGB composite.
- **Quick RGB preview of 2D ROIs on images** · support 2 · canvas-note-section · `cluster-0173`
  - 5 nodes / 5 edges — Convert 2D ROI to RGB -> Set LUT (color map) -> Set LUT (color map) -> Blend images -> Blend images
  - Takes a 2D ROI list and images, draws the 2D ROIs as colored outlines, applies color maps to the images, and blends them into one RGB image for quick visual inspection.
- **Extract central Z substack and projection** · support 2 · group-node · `cluster-0181`
  - 10 nodes / 10 edges — Group input -> Annotate with image properties -> Set annotation (expression) -> Unique annotation -> Set annotation (expression) -> Unique annotation -> Set …
  - Takes a multi-Z image and produces a central Z substack plus a Z projection of that substack. It adds image-property and unique annotations, selects the central Z range with an expression slicer, and projects the selected slices.
- **Background-subtracted image stack** · support 2 · canvas-note-section · `cluster-0183`
  - 6 nodes / 7 edges — Run ImageJ Macro -> Illumination correction 2D -> Z-Project -> Equalize hyperstack dimensions (referenced) -> Fast image arithmetics 2D -> Illumination corre…
  - Produces a background-subtracted image stack from an input image stack by normalizing illumination, estimating a background from a Z-projection, matching dimensions, and applying image arithmetic. A final illumination correction normalizes the output.
- **Colorized extended depth-of-focus image projection** ⚠ unregistered id · support 2 · geometric-cluster · `cluster-0186`
  - 3 nodes / 2 edges — CLIJ2 Extended Depth Of Focus Sobel Projection -> Set LUT (color map) -> Blend images
  - Takes an image stack, computes an extended depth-of-focus Sobel projection, applies a color map, and blends the result into a single RGB image.
- **Load three-channel images from folders and combine them into RGB** · support 2 · geometric-cluster · `cluster-0187`
  - 3 nodes / 1 edges — Folder list -> Import data table (directory) -> Combine channel slices to RGB
  - Takes a list of folders containing data-table directories, imports each directory, and merges three channel slices in each image into a single RGB image.
- **Preprocess 2D images and compute orientation features** · support 2 · canvas-note-section · `cluster-0189`
  - 4 nodes / 3 edges — Find or subtract background 2D -> Median filter 2D -> Gaussian blur 2D -> Orientation features 2D (OrientationJ)
  - Takes 2D image data, removes background, denoises with a median filter, smooths with a Gaussian filter, and outputs orientation-related image features.
- **Create refined 2D ROI mask stacks** · support 2 · canvas-note-section · `cluster-0193`
  - 4 nodes / 3 edges — Merge 2D ROI lists -> Convert 2D ROI to mask -> Morphological operation 2D -> Merge hyperstack C/Z/T (single input)
  - Takes one or more 2D ROI lists, merges them, converts the ROIs to binary mask images, applies a morphological operation to refine the masks, and combines the resulting masks into a single image stack.
- **Create RGB montage of 2D ROIs** · support 2 · geometric-cluster · `cluster-0201`
  - 11 nodes / 5 edges — Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Merge 2D ROI lists -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D…
  - Takes multiple 2D ROI lists, converts them into RGB images with colored outlines, fills, and optional labels, merges ROI lists, and combines the resulting images into a montage.
- **Render 2D ROIs as an RGB image** · support 2 · geometric-cluster · `cluster-0204`
  - 3 nodes / 1 edges — Convert 2D ROI to RGB -> Set LUT (color map) -> Convert 2D ROI to RGB
  - Takes a 2D ROI list and produces an RGB image that draws the ROIs as colored outlines, fills, or labels. It renders the ROIs to RGB, applies a predefined color map LUT, and renders them again using the first rendered image as the reference canvas.
- **Normalize image intensity and attach metadata** · support 2 · geometric-cluster · `cluster-0206`
  - 3 nodes / 2 edges — Set annotation (expression) -> Annotate with image properties -> Divide by maximum
  - Takes an image, adds a single expression-defined annotation and image-property annotations, then divides greyscale pixel values by the global maximum across all slices. Produces an annotated image with intensity normalized relative to the maximum.
- **Create binary masks from a folder of 2D images** · support 2 · geometric-cluster · `cluster-0209`
  - 6 nodes / 5 edges — Folder -> Annotate with file/directory name -> Import image stack -> Morphological operation 2D -> Auto threshold 2D (8-bit) -> Morphological operation 2D
  - Takes a folder of 2D image stacks, imports each image, applies a morphological operation, auto-thresholds it, and applies a second morphological operation to produce cleaned binary masks.
- **Skeletonize binary image and visualize 2D ROI as RGB** · support 2 · geometric-cluster · `cluster-0211`
  - 3 nodes / 2 edges — Morphological skeletonize 2D -> Find particles 2D -> Convert 2D ROI to RGB
  - Takes a binary image, applies 2D morphological skeletonization, extracts particle 2D ROIs from the skeletonized mask, and produces an RGB image that draws the 2D ROIs as colored outlines, fills, or labels.
- **Load images and render RGB images** · support 2 · geometric-cluster · `cluster-0215`
  - 11 nodes / 10 edges — File list -> File list -> File list -> Import image -> Import image -> Import image -> Set LUT (two colors) -> Adjust displayed contrast -> Apply LUT -> IO I…
  - Takes image file paths, imports each image, applies a two-color LUT and display contrast calibration, and produces RGB images for viewing or downstream use.
- **Create binary masks from images** · support 2 · compartment · `cluster-0220`
  - 8 nodes / 8 edges — Adjust displayed contrast -> Convert image to 8-bit -> Invert pixel colors/values -> Auto threshold 2D (8-bit) -> Median filter 2D -> Find or subtract backgr…
  - Takes an image and produces a binary mask by adjusting display contrast, converting to 8-bit, and combining two auto-thresholded masks: one from the original 8-bit image and one from the inverted, median-filtered, background-subtracted image.
- **Load multichannel images as grayscale channels** · support 2 · geometric-cluster · `cluster-0221`
  - 3 nodes / 2 edges — Import image -> Split channels -> Remove LUT
  - Takes image files, imports them as ImageJ images, splits multichannel images into separate grayscale channel images, and removes color lookup tables (LUTs). It produces clean grayscale channel images ready for downstream image analysis.
- **Register 2D images to a reference** · support 2 · geometric-cluster · `cluster-0222`
  - 3 nodes / 3 edges — File list -> Import image -> TurboReg registration 2D (simple, referenced)
  - Takes image file names, imports each file as an ImageJ image, and aligns the target image to a reference image using 2D TurboReg registration. It produces registered images that are geometrically aligned to the reference.
- **Load and split image stacks by slice position** · support 2 · geometric-cluster · `cluster-0223`
  - 3 nodes / 1 edges — File list -> Import image -> Reduce & split stacks (slice)
  - Takes a list of image files, imports each file as an image, and splits each image stack into separate stacks according to user-defined slice index ranges.
- **Import images with scale bar and 2D ROI overlay** · support 2 · geometric-cluster · `cluster-0224`
  - 5 nodes / 4 edges — File list -> Import image -> Set physical dimensions -> Draw scale bar -> Set 2D overlay
  - Takes a list of image files, imports them as ImageJ images, sets their physical dimensions, draws a scale bar, and applies a 2D ROI overlay, producing calibrated images ready for display or further analysis.
- **Create a colorized distance map from 8-bit images** · support 2 · geometric-cluster · `cluster-0225`
  - 10 nodes / 9 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Invert pixel colors/values -> Euclidean distance transform 2D -> Fast…
  - Takes image files, thresholds and morphologically cleans them, computes a Euclidean distance transform, remaps the result as labels, and outputs a colorized RGB image.
- **Compare 2D auto-threshold methods** · support 1 · group-node · `cluster-0231`
  - 5 nodes / 5 edges — Group input -> Define multiple parameters -> Auto threshold 2D (8-bit) -> Create montage -> Group output
  - Takes 8-bit 2D images, applies multiple auto-thresholding methods, and produces thresholded images plus a montage for visual comparison.
- **Segment particles and draw colored 2D ROIs** · support 1 · geometric-cluster · `cluster-0236`
  - 9 nodes / 9 edges — File list -> Import image -> Z-Project (classic) -> Distance transform watershed 2D -> Find particles 2D -> 2D ROI to Labels (by name) -> Labels to ROI -> Co…
  - Takes image files, imports and Z-projects them, segments particles using distance-transform watershed and Find particles, then colors the resulting 2D ROIs by name and draws them as an RGB image.
- **Detect lines in 2D images** ⚠ unregistered id · support 1 · geometric-cluster · `cluster-0242`
  - 3 nodes / 2 edges — Scale 2D image -> Tile image -> Ridge detector
  - Takes a 2D image, rescales and tiles it, then applies a ridge detector to produce a mask marking detected lines or ridges.
- **Preprocess microscopy images** · support 1 · compartment · `cluster-0246`
  - 12 nodes / 11 edges — Folder -> List files -> Annotate with file/directory name -> Bio-Formats importer -> Split channels (deprecated) -> Convert -> Enhance local contrast (CLAHE)…
  - Takes a folder of microscopy image files, imports them, splits channels, and applies contrast enhancement, illumination correction, background subtraction, and sharpening to produce enhanced 2D image slices.
- **Create blank image with computed label** · support 1 · geometric-cluster · `cluster-0248`
  - 3 nodes / 2 edges — Create empty image -> Set annotation (expression) -> Merge annotations
  - Takes no input image and produces an empty image of specified size and pixel type, then attaches one expression-defined metadata label and merges it into the output image.
- **Best-focus projection and bleach correction** · support 1 · geometric-cluster · `cluster-0249`
  - 4 nodes / 3 edges — File list -> Import image -> Custom extended depth of focus (Z/C/T) -> Bleach correction
  - Takes a list of image files, imports them as image stacks, and produces a projected image by selecting the best-scoring pixel along Z, C, or T. It then applies per-channel bleach correction to reduce fading in time-series images.
- **Smooth images with Gaussian blur** · support 1 · geometric-cluster · `cluster-0250`
  - 5 nodes / 2 edges — Tunnel entrance -> Tunnel exit -> Tunnel exit -> Tunnel entrance -> Gaussian blur 2D
  - Takes image data and produces smoothed images by applying a 2D Gaussian blur to each slice.
- **Create Z-projections from image files** · support 1 · geometric-cluster · `cluster-0252`
  - 3 nodes / 2 edges — File list -> Import image -> Z-Project
  - Takes a list of image files, imports each as an ImageJ image, and produces a Z-projection for each image stack. It can also be used for other axis projections such as channel or time projections.
- **Create a thresholded 2D image with statistics** · support 1 · geometric-cluster · `cluster-0257`
  - 5 nodes / 5 edges — Generate image from math expression -> Annotate by image statistics (Expression) -> Auto threshold 2D (8-bit) -> Set/Edit annotations -> Merge annotations
  - Creates an 8-bit image from a pixel expression, computes image statistics, applies 2D auto-thresholding, and merges the resulting annotations with the original image.
- **Compute image derivative features** · support 1 · geometric-cluster · `cluster-0258`
  - 3 nodes / 2 edges — File list -> Import image -> Derivatives features 2D/3D (FeatureJ)
  - Produces derivative feature images that emphasize local intensity changes. It takes a list of image files, imports each as an image, and computes multi-dimensional Gaussian-scaled derivatives for each time frame and channel.
- **Split 2D label images into connected components** · support 1 · geometric-cluster · `cluster-0264`
  - 3 nodes / 2 edges — File list -> Import image -> Split labels (connected components) 2D
  - Takes image files, imports each as a 2D image, and splits label values into separate connected components. It produces label images in which each connected component receives a unique label.
- **Create image montage from files** · support 1 · geometric-cluster · `cluster-0267`
  - 7 nodes / 6 edges — File list -> Annotate with file/directory name -> Import image -> Scale 2D image -> Convert image to 8-bit -> Render to RGB -> Create montage
  - Takes a list of image files, imports each image, scales and converts it to 8-bit RGB, and combines the results into a single montage image.
- **Threshold 2D images from files** · support 1 · geometric-cluster · `cluster-0268`
  - 4 nodes / 3 edges — File list -> Import image -> Remove LUT -> Manual threshold 2D (16-bit)
  - Takes a list of image files, imports them as 2D images, removes lookup tables, and applies a manual 2D threshold to produce thresholded images.
- **Fill masked areas in an image** · support 1 · geometric-cluster · `cluster-0276`
  - 9 nodes / 8 edges — File list -> File list -> Import image -> Import image -> Convert -> Convert image to 8-bit -> Convert image to 8-bit -> Image inpainting -> OpenCV Convert D…
  - Imports an image and a mask, converts them to 8-bit, applies OpenCV inpainting to fill the masked regions, and outputs the filled image with converted depth.
- **Extract an image tile** · support 1 · group-node · `cluster-0279`
  - 8 nodes / 8 edges — Group input -> Tile image -> Split & filter by annotation -> Remove annotation -> Remove annotation -> Remove annotation -> Remove annotation -> Group output
  - Takes an image, splits it into fixed-size tiles, selects the tile matching a configured annotation (for example, the top-left tile), removes tile annotations, and outputs the selected tile.
- **Detect circular markers in 2D images** · support 1 · group-node · `cluster-0280`
  - 8 nodes / 11 edges — Group input -> Detect circles 2D (Hough, fast) -> Bitwise operation -> Morphological operation 2D -> Distance transform watershed 2D -> Morphological operati…
  - Takes a 2D image and a second mask, detects circular markers by Hough transform, combines the detected mask with the second mask, and refines the result with morphology, distance-transform watershed, and connected-component labeling. It produces a labeled marker image, a combined mask, the Hough accumulator, and a table of circle positions, diameters, and scores.
- **Clean and label 2D mask objects** · support 1 · canvas-note-section · `cluster-0286`
  - 4 nodes / 3 edges — Despeckle 2D -> Area opening 2D -> Connected components labeling 2D -> Remove border labels
  - Takes a 2D mask, applies median despeckling and area opening to remove noise and small objects, then labels connected components and removes labels touching the border, producing a clean label image of interior objects.
- **Clean and smooth 2D images** · support 1 · canvas-note-section · `cluster-0287`
  - 3 nodes / 2 edges — Find or subtract background 2D -> Morphological operation 2D -> Box filter 2D (3x3)
  - Takes 2D images, subtracts background, applies a morphological operation, and smooths each 2D slice with a 3x3 box filter, producing cleaned images for downstream analysis.
- **Import, calibrate, and crop 2D images** · support 1 · geometric-cluster · `cluster-0297`
  - 6 nodes / 5 edges — Remove annotation -> Remove annotation -> Remove annotation -> Import image -> Set physical dimensions -> Crop 2D image
  - Produces cropped, physically scaled 2D images from a list of image files. It removes selected text annotations from the file items, imports each image, sets its physical pixel dimensions, and crops the 2D image.
- **Create a combined panel of 2D ROI-annotated images and a histogram** · support 1 · canvas-note-section · `cluster-0300`
  - 5 nodes / 4 edges — Set 2D overlay -> JFreeChart Histogram plot -> Render overlay -> Montage of input images -> Export image
  - Takes images with 2D ROIs and a histogram plot, draws the 2D ROIs onto the images, arranges the rendered images and histogram into one panel, and exports the panel as an image file.
- **Segment 2D objects and export colored image** · support 1 · canvas-note-section · `cluster-0302`
  - 6 nodes / 5 edges — Connected components labeling 2D -> Morphological operation 2D -> Convert labels to RGB -> Set to color (RGB) -> Blend images -> Export image
  - Takes a 2D binary mask, labels connected components, applies a 2D morphological operation, colors each labeled object, blends the result, and exports an RGB image.
- **Rotate an image** · support 1 · geometric-cluster · `cluster-0303`
  - 3 nodes / 2 edges — File -> Import image -> Rotate 2D image (free)
  - Takes a single image file, imports it as an image, and rotates each 2D slice by a specified angle, producing a rotated image with an expanded canvas if needed.
- **Create image montages for visualization** · support 1 · group-node · `cluster-0304`
  - 6 nodes / 5 edges — Group input -> Stack to montage -> Make images equal 2D (single input) -> Create 3D stack -> Stack to montage -> Group output
  - Takes image stacks or 2D image planes, pads them to a common size, merges them into a 3D stack, and produces montage images for visual inspection.
- **Interpolate two images** · support 1 · group-node · `cluster-0305`
  - 5 nodes / 6 edges — Group input -> Legacy image calculator 2D -> Legacy image math operation (scalar) -> Legacy image calculator 2D -> Group output
  - Takes two input images and produces a single interpolated image. It applies pixel-wise image arithmetic to the first image, a scalar pixel operation, and then combines the result with the second image.
- **Create RGB image of detected spots as 2D ROIs** · support 1 · geometric-cluster · `cluster-0309`
  - 4 nodes / 3 edges — Detect spots -> Filter spots -> Convert spots to ROI -> Convert 2D ROI to RGB
  - Takes an image, detects and filters spots, converts the remaining spots to 2D ROIs, and outputs an RGB image that draws those ROIs.
- **Import image files and render as 8-bit RGB** · support 1 · geometric-cluster · `cluster-0315`
  - 5 nodes / 4 edges — File list -> Import image -> Convert image to 8-bit -> Adjust displayed contrast -> Render to RGB
  - Takes a list of image file names and imports each file as an image, then converts it to 8-bit, adjusts the displayed contrast, and renders it to RGB. The result is a set of 8-bit RGB images suitable for display or export.
- **Create RGB image from 2D ROIs** · support 1 · canvas-note-section · `cluster-0322`
  - 4 nodes / 3 edges — Split 2D ROI into connected components -> Set 2D ROI properties from expressions -> Merge 2D ROI lists -> Convert 2D ROI to RGB
  - Takes 2D ROIs, splits them into connected components, assigns expression-based properties, merges the component lists, and renders them as an RGB image with colored outlines, fills, and optional labels.

### rois (65)

- **Detect 2D particle ROIs in images** · support 35 · geometric-cluster · `cluster-0002`
  - 4 nodes / 3 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Takes image files, imports them, auto-thresholds each 8-bit image, and extracts connected components as 2D ROIs with measurements.
- **Filter 2D ROIs by overlap** · support 34 · compartment · `cluster-0003`
  - 6 nodes / 1 edges — Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by over…
  - Takes two lists of 2D ROIs and produces filtered 2D ROI lists containing the ROIs that overlap with ROIs in the other list. Chained overlap filters can be used to include or exclude ROIs based on their spatial relationship to reference ROIs.
- **Attach 3D ROI measurements to data** · support 31 · geometric-cluster · `cluster-0005`
  - 3 nodes / 1 edges — Extract IJ3D ROI statistics -> Add table column -> Annotate data with table values
  - Takes 3D ROIs and optionally a reference image, and produces data annotated with 3D ROI statistics. It extracts the ROI measurements, adds a table column, and attaches the resulting table values to the incoming data.
- **Detect 2D particles and render 2D ROIs** · support 28 · geometric-cluster · `cluster-0006`
  - 4 nodes / 3 edges — Morphological operation 2D -> Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB
  - Takes greyscale 2D images, applies morphological filtering and auto-thresholding, and extracts connected particles as 2D ROIs with measurements. It then draws those 2D ROIs as colored outlines or fills on an RGB image.
- **Visualize 3D ROIs as colored label images** · support 22 · canvas-note-section · `cluster-0008`
  - 4 nodes / 3 edges — Color IJ3D ROI by statistics -> Convert IJ3D ROI to RGB -> Convert IJ3D ROI to RGB -> Convert IJ3D ROI to RGB
  - Takes a list of 3D ROIs and an optional reference image, colors each ROI by a selected measurement, and converts the colored 3D ROIs into RGB label images for visualization.
- **Find 3D particles and create an outlined 3D ROI overlay** · support 20 · canvas-note-section · `cluster-0011`
  - 12 nodes / 11 edges — Remove outliers 2D -> Gaussian blur 3D -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Distance transform watershed 2D -> Connected components …
  - Takes a 3D image, denoises and thresholds it, segments particles with morphology, watershed, and connected-component labeling, and produces outlined 3D ROIs rendered as an RGB overlay with physical dimensions copied.
- **Segment 2D images into 2D ROIs with RGB visualization** · support 18 · canvas-note-section · `cluster-0012`
  - 7 nodes / 6 edges — Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Morphological operation 2D -> Distance transform watershed 2D -> Find parti…
  - Takes 8-bit 2D images and produces detected objects as 2D ROIs, plus an RGB image showing those ROIs. It smooths the image, auto-thresholds it, fills holes, applies a morphological operation, separates touching objects with distance-transform watershed, extracts particle ROIs, and draws them as colored outlines or fills.
- **Split merged 2D ROIs with watershed** · support 13 · compartment · `cluster-0018`
  - 10 nodes / 10 edges — Convert 2D ROI to mask -> Euclidean distance transform 2D -> Distance transform watershed 2D -> Mask to 2D ROI -> Local maxima 2D -> Convert 2D ROI to RGB ->…
  - Takes a list of 2D ROIs and produces separated 2D ROIs by converting them to a mask, applying distance-transform watershed and seeded watershed using local maxima of the distance transform, and also creates RGB images showing the ROIs and final labels.
- **Merge 2D ROIs and create an RGB overlay and statistics table** · support 11 · geometric-cluster · `cluster-0022`
  - 4 nodes / 3 edges — Merge 2D ROI lists -> Convert 2D ROI to RGB -> Extract 2D ROI statistics -> Merge table rows
  - Takes multiple 2D ROI lists, merges them into one 2D ROI list, then draws the merged ROIs as an RGB image and computes ROI measurements into a merged results table.
- **Detect 2D particle ROIs from images** · support 10 · geometric-cluster · `cluster-0024`
  - 7 nodes / 6 edges — Find or subtract background 2D -> Auto threshold 2D (8-bit) -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Find particl…
  - Takes 2D images, applies background subtraction, thresholding, Gaussian smoothing, hole filling, and connected-component particle detection, then filters the resulting 2D ROIs by measured statistics. It produces a cleaned set of 2D particle ROIs for downstream analysis.
- **Segment and measure 3D ROIs from an image** · support 8 · canvas-note-section · `cluster-0027`
  - 6 nodes / 5 edges — Auto threshold 2D (8-bit) -> Median filter 2D -> Connected components labeling 3D -> IJ3D Labels to 3D ROI -> Filter IJ3D ROI by statistics -> Extract IJ3D R…
  - Takes an 8-bit image, thresholds and median-filters it to form a mask, labels connected components in 3D, and converts the labels into 3D ROIs. It then filters the 3D ROIs by measured statistics and outputs a table of 3D ROI measurements.
- **Color overlay of overlapping 3D ROIs** · support 8 · canvas-note-section · `cluster-0028`
  - 12 nodes / 12 edges — Create empty image -> Create empty image -> Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB…
  - Takes two lists of 3D ROIs and produces an exported RGB image showing which objects overlap. It filters each list to ROIs that overlap the other, rasterizes them onto blank images, blends the two label images in different colors, copies physical pixel sizes, and exports the result.
- **Pairwise 3D ROI distance statistics** · support 8 · compartment · `cluster-0029`
  - 11 nodes / 12 edges — Extract pairwise IJ3D ROI statistics -> Extract pairwise IJ3D ROI statistics -> JFreeChart Histogram plot -> Export data -> JFreeChart Histogram plot -> Expo…
  - Takes 3D ROIs, extracts all pairwise ROI statistics such as distances, plots the measurements as histograms, and exports the measurement table and rendered plot images.
- **2D ROI overlap counts and overlays** · support 6 · compartment · `cluster-0040`
  - 9 nodes / 9 edges — Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI…
  - Takes two 2D ROI lists and produces a table of overlap-based counts (true positives, false positives, false negatives, and ground-truth found) along with RGB images showing the corresponding ROIs. It filters each ROI list by mutual overlap, converts the filtered ROI sets to RGB, and applies a per-row expression to the counts.
- **Summarize overlapping 2D ROIs into a table** · support 6 · canvas-note-section · `cluster-0048`
  - 9 nodes / 9 edges — Filter 2D ROI by overlap (old) -> Annotate with data -> Extract data annotations -> Annotate with 2D ROI properties -> Extract 2D ROI statistics -> Add annot…
  - Produces a table summarizing 2D ROIs that overlap between two ROI sets. It filters the two 2D ROI lists by mutual overlap, annotates them with ROI properties and statistics, then converts those annotations into table columns and removes unwanted columns or selects specific rows.
- **Create a cleaned mask from selected 2D ROIs** · support 5 · geometric-cluster · `cluster-0064`
  - 4 nodes / 3 edges — Filter 2D ROI by statistics -> Convert 2D ROI to mask -> Invert pixel colors/values -> Morphological operation 2D
  - Takes a list of 2D ROIs and a reference image, keeps only ROIs matching a user-defined statistics expression, converts the remaining ROIs into a mask, inverts it, and applies a 2D morphological operation to output a refined mask image.
- **Detect 3D particle ROIs and skeletonize their masks** · support 4 · geometric-cluster · `cluster-0066`
  - 12 nodes / 12 edges — Filter paths -> Import image stack -> Set physical dimensions -> Auto threshold 2D (8-bit) -> Morphological operation 3D -> Gaussian blur 3D -> Auto threshol…
  - Takes selected image-stack files, imports and calibrates them, and preprocesses them with thresholding, morphology, and Gaussian blur to create a binary mask. It then finds 3D particle ROIs, converts those ROIs to a mask, and applies morphological skeletonization, producing 3D particle ROIs and a skeletonized mask.
- **Find boundary regions of selected 2D ROIs** · support 4 · geometric-cluster · `cluster-0068`
  - 4 nodes / 3 edges — Filter 2D ROI by statistics -> Get label boundaries -> Connected components labeling 2D -> Filter Labels by overlap 2D
  - Takes 2D ROIs, keeps those matching a statistics expression, extracts label boundaries as connected components, and outputs the boundary components that pass a mutual-overlap filter.
- **Measure 2D ROIs after excluding edge-touching objects** · support 4 · geometric-cluster · `cluster-0077`
  - 6 nodes / 5 edges — 2D ROI to Labels (Expression) -> CLIJ2 Exclude Labels On Edges -> Labels to ROI -> Extract 2D ROI statistics -> Add annotations as columns -> Merge table rows
  - Takes 2D ROIs and an optional reference image, converts them to a label image, removes labels touching image edges, converts the remaining labels back to 2D ROIs, extracts ROI statistics, adds annotation columns, and merges the resulting tables into one table.
- **Filter and summarize 2D ROI lists** · support 4 · geometric-cluster · `cluster-0078`
  - 6 nodes / 3 edges — Rename annotation -> Filter 2D ROI by overlap (old) -> Split into individual 2D ROI lists -> Annotate with 2D ROI properties -> Outline 2D ROI (Hull) -> Set …
  - Takes 2D ROI lists and produces individual 2D ROIs that have been filtered by overlap, annotated with geometric properties, and given hull outlines. It renames annotations, filters mutually overlapping 2D ROIs, splits ROI lists into individual 2D ROIs, extracts ROI properties into annotations, computes hull outlines, and sets expression-based annotations.
- **Segment 2D objects and overlay 2D ROIs** · support 4 · compartment · `cluster-0080`
  - 8 nodes / 7 edges — Image box canvas note -> Auto threshold 2D (8-bit) -> Median filter 2D -> Illumination correction 2D -> Auto threshold 2D (8-bit) -> Find particles 2D -> Con…
  - Takes 8-bit 2D images and produces an image with detected objects drawn as a 2D ROI overlay, along with the extracted 2D ROI list. It preprocesses each image with auto thresholding, median filtering, and illumination correction, uses Find particles 2D to create 2D ROIs, then converts those ROIs to RGB and sets them as the image overlay.
- **Detect 3D particle ROIs in images** · support 4 · canvas-note-section · `cluster-0087`
  - 6 nodes / 5 edges — Median filter 2D -> Set LUT (color map) -> Morphological Feature Contrast (MFC) 2D -> Auto threshold 2D (8-bit) -> IJ3D Find particles 3D -> Set IJ3D ROI pro…
  - Takes image stacks and produces a list of 3D ROIs for particle-like objects by denoising, enhancing morphological feature contrast, auto-thresholding, and finding 3D particles. It then assigns basic properties to each 3D ROI.
- **Extract 3D particle ROIs from images** · support 4 · canvas-note-section · `cluster-0088`
  - 6 nodes / 5 edges — Median filter 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Connected components labeling 3D -> Filter labels by expression 3D -> IJ3D Find parti…
  - Takes an image, applies median filtering and automatic 2D thresholding, labels connected components in 3D, and filters labels by an expression to output 3D ROIs for the selected particles.
- **Extract 3D ROIs from segmented objects** · support 4 · canvas-note-section · `cluster-0089`
  - 8 nodes / 7 edges — Convert labels to mask -> Set to color (RGB) -> Median filter 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> IJ3D…
  - Takes a segmentation label image and produces a list of 3D ROIs for each object. It converts positive labels to a binary mask, colors and cleans the mask with median filtering, auto-thresholding, and hole filling, then finds 3D particles and updates their 3D ROI properties.
- **Render 2D ROIs as RGB images** · support 4 · canvas-note-section · `cluster-0096`
  - 4 nodes / 3 edges — Set 2D ROI properties -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB
  - Takes 2D ROI lists, assigns user-defined properties to the ROIs, and converts them into RGB images that draw the ROIs as colored outlines, fills, and optional labels. It can combine two ROI visualizations by using one rendered image as the reference canvas for the other.
- **Render 2D ROIs as an RGB image** · support 4 · geometric-cluster · `cluster-0099`
  - 3 nodes / 0 edges — Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB
  - Takes a 2D ROI list and an optional reference image, and produces an RGB image in which the ROIs are drawn as colored outlines, fills, and optional labels.
- **Create 2D/3D particle ROIs and label edge images** ⚠ unregistered id · support 4 · canvas-note-section · `cluster-0103`
  - 12 nodes / 11 edges — Gaussian blur 2D -> Divide by maximum -> Median filter 2D -> Histogram-based contrast enhancer -> Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2…
  - Takes an image and produces a 3D label-edge image of particles by first finding 2D particle ROIs, converting them to a mask, extracting orientation features, thresholding the orientation energy, and then finding 3D particle ROIs that are converted to labels and reduced to edges.
- **Detect 2D particle ROIs in images** · support 3 · canvas-note-section · `cluster-0114`
  - 4 nodes / 3 edges — Import image -> Median filter 2D -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Takes imported image files, denoises them with a median filter, auto-thresholds 8-bit images, and converts thresholded regions into 2D ROIs with measurements.
- **Create masks from 2D ROIs in images** · support 3 · geometric-cluster · `cluster-0115`
  - 9 nodes / 8 edges — Project user path -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import image …
  - Takes image files from filtered subfolders of a project path, imports each image, removes its overlay, and converts the 2D ROIs into a mask image.
- **Detect and measure 3D particle ROIs** · support 3 · canvas-note-section · `cluster-0120`
  - 4 nodes / 3 edges — IJ3D Find particles 3D -> Extract IJ3D ROI statistics -> Filter table rows -> Export data
  - Takes a binary mask image and produces an exported table of 3D particle measurements. It finds 3D particles as 3D ROIs, extracts 3D ROI statistics such as volume and center, filters the resulting table rows, and exports the table.
- **Export filtered 2D ROI image** · support 3 · geometric-cluster · `cluster-0123`
  - 5 nodes / 4 edges — Split into individual 2D ROI lists -> Filter 2D ROI by statistics -> Merge 2D ROI lists -> Convert 2D ROI to RGB -> Export image
  - Takes 2D ROI lists, splits them into individual ROI lists, filters ROIs by measured statistics, merges the remaining ROIs, and exports an RGB image that draws the filtered 2D ROIs, optionally over a reference image.
- **Export 2D ROI overlay images** · support 3 · canvas-note-section · `cluster-0127`
  - 9 nodes / 8 edges — Merge 2D ROI lists -> Set 2D ROI properties -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Render to RGB -> Export image -> Me…
  - Takes 2D ROI lists, merges and styles them, then draws them as colored RGB overlays (optionally on reference images) and exports the resulting images, including merging multiple 2D slices into a single multi-slice image when needed.
- **Load and filter 2D ROI files** · support 2 · geometric-cluster · `cluster-0139`
  - 7 nodes / 6 edges — Annotate with file/directory name -> Import ROI -> Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Set annotatio…
  - Produces filtered 2D ROI data by importing ROI files from file paths, adding metadata from file/directory names and expressions, and splitting the ROIs based on an annotation value.
- **Summarize 2D ROI measurements by label** · support 2 · geometric-cluster · `cluster-0141`
  - 3 nodes / 2 edges — Set 2D ROI properties from expressions -> Extract 2D ROI statistics -> Apply expression per label
  - Takes 2D ROIs, sets ROI properties from expressions, extracts per-ROI statistics optionally from a reference image, and produces a table summarizing those statistics by label.
- **Filter and visualize 2D ROIs** · support 2 · geometric-cluster · `cluster-0142`
  - 11 nodes / 12 edges — Filter by annotation -> Filter 2D ROI by statistics -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert only 2D ROI to RGB …
  - Produces RGB images and a statistics table for filtered 2D ROIs by taking 2D ROI data, keeping items that match annotation and measurement conditions, setting ROI display properties, and rendering the remaining ROIs.
- **Detect 2D particle ROIs from microscopy image stacks** ⚠ unregistered id · support 2 · compartment · `cluster-0153`
  - 11 nodes / 9 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Median filter 2D -> Reduce & split hyperstack (Expression) -> Reorder dimensio…
  - Takes imported microscopy image stacks, applies median filtering and dimension reduction/projection to create 2D extended-depth-of-focus images, then auto-thresholds them and extracts 2D particle ROIs with measurements.
- **Extract 2D particle ROIs from images** · support 2 · canvas-note-section · `cluster-0156`
  - 9 nodes / 8 edges — Divide by maximum -> CLIJ2 Gaussian Blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> CLIJ2 Gaussian Blur 2D -> Illumination correctio…
  - Takes 2D greyscale images and produces 2D particle ROIs by normalizing intensity, blurring, thresholding, filling holes, applying illumination correction, and using Find particles 2D. The resulting ROIs are then set to a specified color.
- **Compute 2D ROI statistics from image stacks** · support 2 · canvas-note-section · `cluster-0161`
  - 5 nodes / 4 edges — Split stack into 2D -> Extract 2D ROI statistics -> Apply expression per row -> Merge table rows -> Split table by columns
  - Takes image stacks and 2D ROIs and produces a table of per-plane ROI measurements. It splits the stack into 2D planes, extracts 2D ROI statistics, applies a row expression, then merges and splits the resulting table by selected columns.
- **Create a colored 3D ROI mask from images** · support 2 · canvas-note-section · `cluster-0170`
  - 9 nodes / 8 edges — CLIJ2 Add Images -> Morphological operation 3D -> Auto threshold 2D (8-bit) -> Morphological operation 3D -> IJ3D Find particles 3D -> Outline IJ3D ROI -> IJ…
  - Takes one or more same-size images, adds them, cleans and thresholds the result, and finds 3D ROIs. It outlines the 3D ROIs, converts them to a colored mask, and produces an RGB image.
- **Detect 2D particles and extract 2D ROI statistics** · support 2 · geometric-cluster · `cluster-0172`
  - 10 nodes / 8 edges — Set/Edit annotations -> Apply transformation -> Run ImageJ Macro -> Morphological operation 2D -> Morphological hole filling 2D -> Find particles 2D -> Conve…
  - Takes images, applies a pixel transformation and an ImageJ macro, then uses morphological cleaning and hole filling to find 2D particle ROIs. It merges the 2D ROIs, draws them as an RGB overlay, and extracts 2D ROI statistics.
- **Segment cells with Cellpose and overlay filtered 2D ROIs** · support 2 · canvas-note-section · `cluster-0174`
  - 10 nodes / 14 edges — Filter by annotation -> Cellpose prediction (2.x) -> Set 2D ROI properties from expressions -> Split into individual 2D ROI lists -> Filter 2D ROI by statist…
  - Takes annotated images, runs Cellpose to generate 2D ROIs, filters those ROIs using measured statistics and annotation conditions, and produces an RGB image drawing the retained 2D ROIs as an overlay.
- **Filter 2D ROIs by local variance and extract statistics** · support 2 · geometric-cluster · `cluster-0175`
  - 7 nodes / 6 edges — Local variance 2D -> Divide by maximum -> Filter 2D ROI by statistics -> Convert 2D ROI to mask -> Morphological operation 2D -> Mask to 2D ROI -> Extract 2D…
  - Takes an image and a list of 2D ROIs, computes a normalized local-variance image, and keeps only ROIs whose variance-based measurements match a condition. It converts the kept 2D ROIs to a mask, applies a morphological operation, converts the cleaned mask back to 2D ROIs, and outputs a table of ROI statistics.
- **Create RGB image showing 2D ROIs** · support 2 · canvas-note-section · `cluster-0176`
  - 4 nodes / 3 edges — Draw 2D rectangle/oval/line ROI -> Set to color (RGB) -> Convert 2D ROI to RGB -> Merge annotations
  - Takes 2D ROIs and produces an RGB image that draws them as colored outlines/fills, combines it with a solid-color image, and merges annotations from the resulting image layers.
- **Segment and measure 3D ROIs from image stacks** · support 2 · canvas-note-section · `cluster-0182`
  - 9 nodes / 8 edges — Reduce & split hyperstack (Expression) -> Z-Project -> Gaussian blur 2D -> Equalize hyperstack dimensions (referenced) -> Fast image arithmetics 2D -> Auto t…
  - Takes a hyperstack, extracts a channel, Z-projects, smooths, aligns dimensions, applies image arithmetic, auto-thresholds, labels connected components in 3D, and converts labels to 3D ROIs. It produces a table of 3D ROI statistics, optionally measured against a reference image.
- **Overlay 2D ROIs on corrected images** · support 2 · compartment · `cluster-0192`
  - 10 nodes / 12 edges — Set 2D ROI properties -> Illumination correction 2D -> Set 2D ROI properties -> Adjust displayed contrast -> Filter 2D ROI by overlap (old) -> Convert 2D ROI…
  - Takes images and 2D ROI lists, applies illumination correction and display contrast adjustment, filters ROIs by overlap, and produces RGB images with the 2D ROIs drawn as colored outlines, fills, or labels.
- **Filter and merge overlapping 2D ROIs** · support 2 · compartment · `cluster-0194`
  - 8 nodes / 4 edges — Filter 2D ROI by overlap (old) -> Merge 2D ROI lists -> Filter 2D ROI by overlap (old) -> Merge 2D ROI lists -> Filter 2D ROI by overlap (old) -> Filter 2D R…
  - Produces 2D ROI lists containing only ROIs that overlap with other ROIs, with overlapping ROIs merged together. It filters 2D ROI lists by mutual overlap and then merges the retained ROIs into combined 2D ROI lists.
- **Create 2D ROI segmentation image and measurements table** · support 2 · compartment · `cluster-0198`
  - 10 nodes / 8 edges — 2D ROI to Labels (Expression) -> Extract 2D ROI statistics -> Morphological operation 2D -> Filter tables -> Set/Edit annotations -> Add border 2D -> JFreeCh…
  - Takes 2D ROIs and converts them into a 2D label image (with morphological cleanup and border) while extracting a table of ROI statistics; the table is filtered, annotated, histogrammed, and split by annotation.
- **Detect 2D particles and create 2D ROI overlays** · support 2 · compartment · `cluster-0208`
  - 10 nodes / 7 edges — Define multiple parameters -> Remove outliers 2D -> Define multiple parameters -> Enhance local contrast (CLAHE) -> Find or subtract background 2D -> Auto th…
  - Takes 2D images, applies outlier removal, CLAHE contrast enhancement, background subtraction, auto-thresholding, and distance-transform watershed, then extracts 2D particle ROIs and renders them as an RGB overlay montage.
- **Measure 2D ROI particles in mask images** · support 2 · geometric-cluster · `cluster-0210`
  - 8 nodes / 7 edges — Find particles 2D -> Split 2D ROI into connected components -> Extract 2D ROI statistics -> Summarize table -> Add annotations as columns -> Merge table rows…
  - Takes thresholded mask images and produces a summarized table of 2D ROI particle measurements plus a plot. It finds 2D particles, splits each 2D ROI into connected components, extracts 2D ROI statistics, then summarizes, annotates, merges, filters, and plots the table.
- **Measure 2D ROI components and combined regions** · support 2 · geometric-cluster · `cluster-0212`
  - 11 nodes / 11 edges — Split 2D ROI into connected components -> Extract 2D ROI statistics -> 2D ROI calculator (AND/OR/XOR) -> Extract 2D ROI statistics -> Rename & modify columns…
  - Takes a list of 2D ROIs and produces a sorted table of measurements for each connected component and for a logical combination of the ROIs, along with a plot and a montage. It splits the 2D ROIs into connected components, applies an AND/OR/XOR ROI calculator, extracts 2D ROI statistics, merges/renames/sorts the resulting tables, and plots the table before creating a montage.
- **Detect 2D particle ROIs** · support 2 · group-node · `cluster-0213`
  - 4 nodes / 4 edges — Group input -> Auto threshold 2D (8-bit) -> Find particles 2D -> Group output
  - Takes 8-bit 2D images, applies auto-thresholding, and converts the thresholded regions into 2D particle ROIs with measurements.
- **Overlay filtered 2D point ROIs from results tables on images** · support 1 · geometric-cluster · `cluster-0237`
  - 7 nodes / 6 edges — File list -> Annotate with file/directory name -> Import results table -> Filter table rows -> Table to 2D point ROI -> Set 2D overlay -> Set 2D overlay
  - Takes a list of results-table files and image inputs, imports and filters the tables, converts selected rows to 2D point ROIs, and sets those ROIs as overlays on the images. It produces images carrying the filtered point ROIs for display or further analysis.
- **Segment 2D images into 2D ROIs** · support 1 · compartment · `cluster-0239`
  - 12 nodes / 11 edges — Hessian segmentation 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Distance transform watershed 2D -> Morphological…
  - Takes a 2D image (or applies per slice to higher-dimensional data) and produces individual 2D ROIs by applying Hessian-based segmentation, Gaussian smoothing, auto-thresholding, hole filling, distance-transform watershed, and morphological cleanup, then converting masks to ROIs and splitting multi-component ROIs.
- **Overlay 2D ROIs on an image** · support 1 · geometric-cluster · `cluster-0256`
  - 4 nodes / 3 edges — Draw 2D rectangular ROI (old) -> Draw 2D oval ROI (old) -> Draw 2D text ROI -> Set 2D overlay
  - Takes an image, builds a 2D ROI list from rectangular, oval, and text ROIs, and applies that list as the image overlay. It produces an image with visible 2D ROI annotations.
- **Detect text in images as 2D ROIs** · support 1 · geometric-cluster · `cluster-0272`
  - 7 nodes / 7 edges — File list -> Annotate with file/directory name -> Import image -> Find text (Tesseract OCR) -> Filter table rows -> Table to 2D text ROI -> Set 2D overlay
  - Takes image file paths, imports each image, runs Tesseract OCR to find text, filters the OCR result table, converts the remaining detections into 2D text ROIs, and overlays those ROIs on the images.
- **Detect 2D particle ROIs from OMERO images** · support 1 · geometric-cluster · `cluster-0273`
  - 6 nodes / 5 edges — List OMERO projects -> List OMERO datasets -> List OMERO images in dataset -> Download image from OMERO -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Takes OMERO project, dataset, and image IDs, downloads the selected OME image, auto-thresholds the 8-bit image, and converts thresholded regions into 2D particle ROIs with measurements.
- **Count 2D ROIs into a summary table** · support 1 · canvas-note-section · `cluster-0282`
  - 5 nodes / 4 edges — Count 2D ROI -> Count 2D ROI -> Add table column -> Add table column -> Merge table rows
  - Takes one or more lists of 2D ROIs and produces a single table summarizing how many ROIs are present in each list. It counts each 2D ROI list, adds the resulting count columns, and merges the rows into one table.
- **Create 2D ROIs from watershed segmentation** · support 1 · canvas-note-section · `cluster-0283`
  - 6 nodes / 5 edges — Seeded watershed -> Manual threshold 2D (8-bit) -> Legacy image calculator 2D -> Find particles 2D -> Find particles 2D -> Set 2D ROI properties
  - Takes a 2D image and seed labels, applies seeded watershed and thresholding, then extracts connected components as 2D ROIs and assigns a common ROI property.
- **Draw 2D ROIs on images and export as image files** · support 1 · canvas-note-section · `cluster-0288`
  - 4 nodes / 3 edges — Merge 2D ROI lists -> Set 2D overlay -> Render overlay -> Export image
  - Takes an image and multiple 2D ROI lists, merges the ROI lists (grouping ROIs by matching annotations), draws the ROIs as a colored overlay with outlines, fills, and labels on the image, and exports the resulting picture as an image file such as PNG or TIFF.
- **Detect and label 2D marker ROIs** · support 1 · canvas-note-section · `cluster-0291`
  - 9 nodes / 10 edges — Laplacian of Gaussian 2D -> Custom auto threshold 2D (32-bit) -> Legacy image calculator 2D -> Euclidean distance transform 2D -> Find particles 2D -> Filter…
  - Takes 2D images, applies LoG filtering, thresholding, and Euclidean distance transform to find marker-like objects, then extracts and filters 2D ROIs and produces a connected-component label mask of the detected markers.
- **Filter 2D particle ROIs by size** · support 1 · compartment · `cluster-0295`
  - 5 nodes / 4 edges — Find particles 2D -> JFreeChart Histogram plot -> Summarize table -> Annotate data with table values -> Filter by annotation (If else)
  - Takes thresholded images, extracts 2D particle ROIs and measurements, summarizes size statistics, and filters the ROIs by size; it also plots a histogram of the measurements.
- **Export 2D ROI overlay and mask images** · support 1 · geometric-cluster · `cluster-0296`
  - 7 nodes / 7 edges — Merge 2D ROI lists -> Remove annotation -> Set 2D overlay -> Render overlay -> Convert 2D ROI to mask -> Export image -> Export image
  - Takes an image and multiple 2D ROI lists, merges the ROIs and removes selected annotations, then renders the ROIs as a color overlay image and converts them to a mask image, exporting both.
- **Segment 2D images into 2D ROIs** · support 1 · compartment · `cluster-0316`
  - 5 nodes / 4 edges — Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D -> Find particles 2D
  - Takes 2D images, smooths and thresholds them, cleans the binary mask with morphology and hole filling, then extracts connected components as 2D ROIs with measurements.
- **Detect 2D particles as 2D ROIs** · support 1 · canvas-note-section · `cluster-0320`
  - 6 nodes / 5 edges — Apply ImageJ auto-contrast per slice -> Median filter 2D -> Laplacian of Gaussian 2D -> Fast image arithmetics 2D -> Auto threshold 2D (8-bit) -> Find partic…
  - Takes 2D images and produces particle 2D ROIs by enhancing contrast, denoising with a median filter, applying a Laplacian-of-Gaussian filter, thresholding, and extracting connected components with measurements.
- **RGB image with 2D ROI overlays** · support 1 · canvas-note-section · `cluster-0323`
  - 5 nodes / 4 edges — Histogram-based contrast enhancer -> Set 2D ROI properties -> Set LUT (color map) -> Apply LUT -> Convert 2D ROI to RGB
  - Takes an image and a list of 2D ROIs, enhances contrast, applies a color map to create an RGB image, and draws the 2D ROIs as colored outlines, fills, or labels, producing a single RGB visualization image.

### filaments (14)

- **Extract and measure 2D filaments from images** · support 8 · canvas-note-section · `cluster-0034`
  - 12 nodes / 11 edges — Morphological operation 2D -> Morphological operation 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological skeletonize 2D -> Set to value (gr…
  - Takes greyscale images and produces 2D filaments by applying morphological operations, Gaussian blur, auto-thresholding, and skeletonization, then filters and measures filament components. It outputs a mask image of the filaments and a merged table containing filament measurements and annotation columns.
- **Extract and render 2D filaments from binary images** · support 5 · geometric-cluster · `cluster-0055`
  - 7 nodes / 7 edges — File list -> Import image -> Morphological skeletonize 2D -> Binary skeleton to 2D filaments -> Smooth filaments -> Set filament vertex radius from image -> …
  - Takes image files and produces an RGB visualization of line-like structures. It imports each image, skeletonizes it, converts the skeleton into smoothed 2D filaments, sets each filament vertex radius from the image, and renders the filaments onto an RGB image.
- **Split 3D filaments into component masks** · support 5 · geometric-cluster · `cluster-0063`
  - 3 nodes / 1 edges — Convert filaments to mask -> Split filaments into connected components -> Convert filaments to mask
  - Takes 3D filament data, splits it into connected components, and converts each component into a greyscale mask image, optionally using a reference image to set mask dimensions.
- **Create 2D filament mask from Cellpose probability** · support 4 · canvas-note-section · `cluster-0082`
  - 10 nodes / 10 edges — Pretrained Cellpose 2.x model -> Cellpose segmentation (2.x) -> Auto threshold 2D (8-bit) -> Euclidean distance transform 2D -> Morphological skeletonize 2D …
  - Takes an image and a pretrained Cellpose model, runs Cellpose to obtain a probability image, thresholds and skeletonizes it, then converts the skeleton into smoothed 2D filaments with radius from a distance transform, producing a colored 2D filament mask.
- **Create RGB images showing 3D filaments** · support 4 · canvas-note-section · `cluster-0083`
  - 10 nodes / 11 edges — Set LUT (color map) -> Set LUT (color map) -> Convert filaments to RGB -> Convert filaments to mask -> Convert filaments to mask -> Blend images -> Blend ima…
  - Takes 3D filament data and reference images, renders the filaments as RGB and mask images, applies color maps to the reference images, and blends the layers into final RGB visualization images.
- **Measure filament components and plot their properties** · support 4 · canvas-note-section · `cluster-0084`
  - 10 nodes / 9 edges — Split filaments into connected components -> Filter filament components -> Find linear filament vertex order/distance -> Measure filament vertices -> Filter …
  - Takes a filament graph and produces measurement tables for filtered connected components, along with multiple XY scatter plots. It splits filaments into components, filters components, orders linear vertices, measures vertices, filters the resulting tables, and plots selected measurements.
- **Create filament mask from 3D time-lapse** · support 3 · geometric-cluster · `cluster-0126`
  - 3 nodes / 3 edges — Z-Project (classic) -> Find and track filaments 3D+T (TSOAX) -> Convert filaments to mask
  - Takes a 3D time-lapse image, projects it along Z, and uses TSOAX to extract and track filaments, producing a greyscale mask image where detected filaments are drawn as solid pixels.
- **Extract and clean 2D filaments from binary images** · support 3 · compartment · `cluster-0135`
  - 8 nodes / 7 edges — Euclidean distance transform 2D -> Morphological skeletonize 2D -> Binary skeleton to 2D filaments -> Smooth filaments -> Fix overlapping filaments (non-bran…
  - Takes a binary image and produces cleaned 2D filaments by skeletonizing the mask, smoothing and separating merged non-branching filaments, assigning vertex radii from a Euclidean distance transform, and filtering out border or unwanted components.
- **Detect line-like structures in microscopy images** · support 2 · geometric-cluster · `cluster-0179`
  - 7 nodes / 5 edges — Folder list -> Bio-Formats importer -> Annotate with image properties -> Filter by annotation -> Split channels -> Find or subtract background 2D -> Ridge de…
  - Takes folders of microscopy image files and produces a binary mask of detected ridges or lines. It imports the images, annotates them with image properties, filters by those annotations, splits channels, subtracts background from a selected channel, and applies a 2D ridge detector.
- **Create a tidy filament measurement table** · support 2 · compartment · `cluster-0219`
  - 3 nodes / 2 edges — Measure filament components -> Remove table column -> Rename table column
  - Takes a filament graph and computes per-component measurements such as length, vertex/edge counts, centroid, and radius, then removes and renames selected columns to produce a standardized results table.
- **Fix filament overlaps and create 2D/3D ROIs** · support 1 · geometric-cluster · `cluster-0259`
  - 3 nodes / 2 edges — Fix overlapping filaments (non-branching) -> Set 2D overlay -> Convert filaments to 3D ROI
  - Takes non-branching filament data, separates filaments that are merged at junctions, and outputs a 2D ROI overlay for display plus a 3D ROI list for downstream analysis.
- **Detect line segments in images as 2D ROIs** · support 1 · geometric-cluster · `cluster-0266`
  - 5 nodes / 5 edges — File list -> Import image -> Binarize -> Detect line segments 2D (Hough) -> Set 2D overlay
  - Takes image files, binarizes each image, and detects line segments with a Hough transform, producing the same images with the detected segments attached as a 2D ROI overlay.
- **Measure and export filament metrics** · support 1 · canvas-note-section · `cluster-0285`
  - 8 nodes / 7 edges — Measure filament components -> Remove table column -> Add annotations as columns -> Rename table column -> Apply expression per row -> Filter table rows -> M…
  - Takes a filament graph and produces a table of per-component measurements such as length, vertex/edge counts, radius, and centroid, then cleans, renames, filters, and merges the table before exporting it as CSV or XLSX.
- **Render filaments and 2D ROI text annotations as RGB images** · support 1 · canvas-note-section · `cluster-0292`
  - 8 nodes / 7 edges — Table to 2D text ROI -> Convert filaments to labels (legacy) -> Convert labels to RGB -> Blend images -> Export image -> Export image -> Convert 2D ROI to RG…
  - Takes filaments and a table of text coordinates, converts the filaments to a colored labels image, blends it, and draws 2D text ROIs from the table over the result. It exports the resulting RGB images.

### machine-learning (21)

- **Segment images with Cellpose and visualize filtered 2D ROIs** · support 31 · geometric-cluster · `cluster-0004`
  - 4 nodes / 3 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB
  - Takes an image and a pretrained Cellpose model, runs Cellpose segmentation to produce 2D ROIs, keeps only 2D ROIs matching a statistical filter, and outputs an RGB image showing the remaining 2D ROIs as colored outlines or fills.
- **Segment cells into 2D/3D ROIs with a Cellpose model** · support 21 · geometric-cluster · `cluster-0009`
  - 3 nodes / 2 edges — File list -> Import Cellpose model from file -> Cellpose segmentation (3.x)
  - Loads a Cellpose model from a file and runs it on input images, producing segmented 2D/3D ROIs and optional label, flow, and probability images.
- **Cellpose cell segmentation with 2D ROI masks** · support 20 · canvas-note-section · `cluster-0010`
  - 7 nodes / 6 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Convert 2D ROI to mask -…
  - Segments cells in input images using a pretrained Cellpose model, then filters the resulting 2D ROIs by measured statistics and converts them to an RGB overlay and a binary mask. The mask is cleaned with a 2D morphological operation and merged into a C/Z/T hyperstack.
- **Segment objects with Cellpose and draw 2D ROIs** · support 7 · geometric-cluster · `cluster-0037`
  - 9 nodes / 9 edges — Ridge detector -> File list -> Import Cellpose model from file -> Gaussian blur 2D -> Histogram-based contrast enhancer -> Fast image arithmetics 2D -> Cellp…
  - Takes an image and a Cellpose model file, applies ridge detection, smoothing, contrast enhancement, and image arithmetic, then runs Cellpose inference to produce 2D ROIs and renders them as RGB images.
- **Cellpose cell segmentation with 2D ROI visualization** · support 7 · geometric-cluster · `cluster-0038`
  - 3 nodes / 2 edges — Morphological operation 2D -> Cellpose prediction (2.x) -> Convert 2D ROI to RGB
  - Takes an image, applies a 2D morphological operation, runs Cellpose inference to create 2D ROIs of segmented cells, and converts those ROIs into an RGB image with colored outlines or fills.
- **Train and test Cellpose segmentation to produce 2D ROIs** · support 6 · canvas-note-section · `cluster-0044`
  - 12 nodes / 11 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation training (3.x) -> Remove annotation -> Cellpose segmentation (3.x) -> Gaussian blur 2D ->…
  - Takes a pretrained Cellpose model and labeled images, trains a new model, then runs inference and post-processes the probability image with blur, thresholding, morphology, and watershed to output 2D ROIs and RGB ROI visualizations.
- **Segment images with Cellpose and render 2D ROIs** · support 6 · canvas-note-section · `cluster-0046`
  - 6 nodes / 5 edges — Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Cellpose segmentation (3.x) -> Con…
  - It imports pretrained Cellpose 3.x models, runs Cellpose segmentation on image inputs, and converts the resulting 2D ROI lists into RGB images that draw the segmented regions.
- **Train a Cellpose model from 2D image-label pairs** · support 6 · geometric-cluster · `cluster-0050`
  - 7 nodes / 7 edges — Illumination correction 2D -> Scale 2D image -> Annotate with data -> Split data randomly (percentage) -> Image to OME Image -> Cellpose training (2.x) -> Im…
  - Takes 2D raw images and matching label images, corrects illumination and scales them, attaches each label to its raw image, and randomly splits the pairs into training, test, and validation sets. It converts the test images to OME images and trains a Cellpose model.
- **Cellpose 2D ROI segmentation with probability overlay** · support 6 · geometric-cluster · `cluster-0053`
  - 4 nodes / 3 edges — Pretrained Cellpose 2.x model -> Cellpose segmentation (2.x) -> Auto threshold 2D (8-bit) -> Blend images
  - Takes an input image and a pretrained Cellpose model, runs Cellpose inference to generate 2D ROIs and a probability image, thresholds the probability image, and blends it into an RGB overlay showing likely cell regions.
- **Train a Cellpose model and visualize 2D ROI predictions** · support 4 · compartment · `cluster-0081`
  - 10 nodes / 11 edges — Remove overlay -> File list -> Import Cellpose model -> Annotate with data -> Split data randomly (percentage) -> Cellpose training (2.x) -> Cellpose predict…
  - Takes labeled images, removes overlays, annotates them with paired label data, and splits them into train/validation/test sets to train and export a Cellpose model. It then predicts cells on the test set with an imported Cellpose model and renders the predicted 2D ROIs as RGB images.
- **Segment objects and create 2D ROI ring masks** · support 4 · canvas-note-section · `cluster-0093`
  - 12 nodes / 14 edges — Divide by maximum -> Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x segmentation model -> CLIJ2 Gaussian Blur 2D -> Cellpose segmentat…
  - Takes a greyscale image, normalizes and blurs it, runs Cellpose to segment objects into 2D ROIs, filters and enlarges those ROIs, then converts the original and enlarged ROIs to binary masks and combines them to produce a ring-shaped mask around each object.
- **Double Cellpose cell segmentation with 2D ROI visualization** · support 4 · canvas-note-section · `cluster-0097`
  - 7 nodes / 6 edges — File list -> Pretrained Cellpose 3.x segmentation model -> Import Cellpose model from file -> Cellpose segmentation (3.x) -> Cellpose segmentation (3.x) -> F…
  - For an input image, it loads a pretrained Cellpose model and a model from a file list, runs Cellpose twice using the first pass's flow output as input to the second, filters the resulting 2D ROIs by measured statistics, and produces an RGB image showing the remaining 2D ROIs.
- **Cellpose cell segmentation to 3D ROIs** · support 4 · canvas-note-section · `cluster-0104`
  - 12 nodes / 12 edges — Set to color (RGB) -> Pretrained Cellpose 3.x segmentation model -> Divide by maximum -> Set LUT (color map) -> Histogram-based contrast enhancer -> Cellpose…
  - Takes an image, runs a pretrained Cellpose model to segment cells, and produces 3D ROIs for the detected objects. It normalizes and contrast-enhances the image, draws the per-plane 2D ROIs as an RGB overlay, and converts the 2D ROI masks into 3D connected-component labels.
- **Cell segmentation from image files** · support 3 · geometric-cluster · `cluster-0130`
  - 3 nodes / 2 edges — File list -> Import image -> Omnipose prediction (0.x)
  - Takes a list of image files, imports each as an image, and runs Omnipose inference to segment cells. It produces segmentation labels, probability/flow images, and cell ROIs (2D/3D depending on the input).
- **Train and predict with a generative model** ⚠ unregistered id · support 3 · geometric-cluster · `cluster-0134`
  - 3 nodes / 2 edges — Create generation model (JIPipe2AI) -> JIPipe2AI train generation -> JIPipe2AI predict generation
  - Creates a generative model, trains it, and then uses the trained model to produce predictions.
- **Segment cells and annotate 2D ROI properties** · support 2 · geometric-cluster · `cluster-0159`
  - 5 nodes / 6 edges — Merge channels (classic) -> Histogram-based contrast enhancer -> Cellpose segmentation (3.x) -> Set 2D overlay -> Annotate with 2D ROI properties
  - Takes image planes, merges them into a multichannel image, enhances contrast, and runs Cellpose to segment cells into 2D ROIs. It overlays those 2D ROIs on the image and extracts their properties as annotations.
- **Denoise and segment cells into 2D ROIs** · support 2 · compartment · `cluster-0190`
  - 12 nodes / 11 edges — Pretrained Cellpose 3.x image restoration model -> Find or subtract background 2D -> Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x im…
  - Takes an image, cleans it with background subtraction, median and Gaussian filtering, and Cellpose restoration, then runs Cellpose segmentation to produce 2D ROIs, filters those ROIs by measured statistics, and draws them as an RGB overlay.
- **Classify pixels in a set of images** · support 2 · geometric-cluster · `cluster-0214`
  - 4 nodes / 3 edges — File list -> Annotate with file/directory name -> Import image -> Ilastik pixel classification
  - Takes a list of image file names, annotates each path with its file/directory name, imports the images, and runs Ilastik pixel classification to produce per-image pixel label data.
- **Train and predict image segmentation** ⚠ unregistered id · support 2 · compartment · `cluster-0216`
  - 5 nodes / 5 edges — Create segmentation model (JIPipe2AI) -> Annotate with data -> Split data randomly (percentage) -> JIPipe2AI train segmentation -> JIPipe2AI predict segmenta…
  - Takes image data and a paired annotation set, attaches the annotations to each image, randomly splits the annotated data into training and validation subsets, trains a segmentation model, and outputs predicted segmentations.
- **Train a deep-learning model from labeled images** ⚠ unregistered id · support 1 · geometric-cluster · `cluster-0254`
  - 6 nodes / 6 edges — File list -> Annotate with file/directory name -> Import image -> Annotate with data -> Split data randomly (percentage) -> Train model (labeled images)
  - Takes image files and paired labels, imports the images, and attaches the labels to each image. It then randomly splits the labeled images into training and test sets and trains a deep-learning image model.
- **Segment images with a Labkit model** · support 1 · geometric-cluster · `cluster-0265`
  - 7 nodes / 6 edges — File list -> Folder list -> List files -> Import JSON -> Annotate with file/directory name -> Import image -> Labkit inference
  - Takes a Labkit JSON model and image files from folders, imports the images, and runs Labkit inference to produce segmentation or probability maps.

### tracking (7)

- **Track spots and summarize measurements** · support 14 · canvas-note-section · `cluster-0016`
  - 8 nodes / 7 edges — LAP Tracker -> Mask detector -> Detect spots -> Annotate with source slot -> Track spots -> Filter tracks -> Measure tracks -> JFreeChart Histogram plot
  - Takes an image, detects spots with a mask detector, links them into tracks using a LAP tracker, filters and measures the tracks, and produces a histogram of the track measurements.
- **Track and measure objects in 2D ROI masks** · support 12 · canvas-note-section · `cluster-0021`
  - 11 nodes / 10 edges — Mask detector -> LAP Tracker -> Convert 2D ROI to mask -> Annotate with source slot -> Morphological operation 2D -> Merge hyperstack C/Z/T (single input) ->…
  - Takes 2D ROIs, converts them to masks, merges the masks into an image, detects and tracks objects within the masked regions, filters and measures the resulting tracks, and plots a histogram of the track measurements.
- **Visualize tracked spots and trajectories** · support 5 · compartment · `cluster-0056`
  - 3 nodes / 1 edges — Convert spots to RGB -> Render track scheme -> Convert tracks to RGB
  - Takes a collection of tracked spots and trajectories, optionally with a reference RGB image, and produces RGB images showing the spots, the trajectories, and a diagram of the connections over time.
- **Track, filter, and annotate spots** · support 3 · geometric-cluster · `cluster-0116`
  - 4 nodes / 3 edges — Track spots -> Filter tracks -> Rename annotation -> Set annotation (expression)
  - Takes detected spots, links them into tracks with TrackMate, keeps only tracks matching a filter expression, and renames or sets text annotations on the resulting tracks.
- **Track measurement summary** · support 2 · geometric-cluster · `cluster-0138`
  - 11 nodes / 11 edges — Measure tracks -> Apply expression to columns -> JFreeChart Histogram plot -> Annotate data with table values -> Export image -> Add table column -> Table co…
  - Takes a set of tracks, measures them, applies column expressions and adds derived columns, then generates histogram and line plots exported as images and measurement tables exported as multi-sheet XLSX files.
- **Track objects in image sequences** · support 1 · geometric-cluster · `cluster-0244`
  - 6 nodes / 5 edges — File list -> Import image -> Detect spots -> Filter spots -> Track spots -> Filter tracks
  - Takes a list of image files, imports each image, detects and filters spots, links the spots into tracks, and filters the resulting tracks. It produces a collection of object tracks from the input image sequence.
- **RGB track visualization and branch statistics** · support 1 · geometric-cluster · `cluster-0311`
  - 4 nodes / 3 edges — Visualize tracked spots -> Branch analysis -> Merge channels (classic) -> Combine channel slices to RGB
  - Takes tracked spots, renders each track over time, and merges the rendered images into RGB, while also outputting a table of branch predecessor/successor counts and durations.

### tables (8)

- **Merge multiple parameter definitions** · support 12 · group-node · `cluster-0020`
  - 6 nodes / 4 edges — Define multiple parameters -> Define multiple parameters -> Define multiple parameters -> Group input -> Merge parameters -> Group output
  - Takes several parameter definitions and merges them into a single parameter set for downstream multi-parameter algorithms. It is wrapped as a reusable group that outputs the merged parameters.
- **Clean and merge annotation tables** · support 8 · geometric-cluster · `cluster-0030`
  - 5 nodes / 4 edges — Annotate data with table values -> Convert to annotation table -> Remove table column -> Merge table columns (simple) -> Rename table column
  - Takes data and a source table, annotates the data with table values, converts the resulting annotations into a table, then removes selected columns, merges the table with another table, and renames columns to produce a cleaned, merged annotation table.
- **Filter data by label** · support 5 · geometric-cluster · `cluster-0062`
  - 3 nodes / 0 edges — Filter by annotation (If else) -> Filter by annotation (If else) -> Filter by annotation (If else)
  - Splits a data set into subsets based on annotation values. Each of the three filters outputs the data that matches the annotation condition and the data that does not.
- **Merge two tables after column cleanup** · support 3 · canvas-note-section · `cluster-0111`
  - 4 nodes / 3 edges — Rename & modify columns (expression) -> Remove table column -> Rename table column -> Merge table columns (simple)
  - Takes two data tables, renames/removes selected columns in each to align their fields, and merges them into a single table containing columns from both.
- **Tidy a results table** · support 3 · canvas-note-section · `cluster-0112`
  - 3 nodes / 2 edges — Remove table column -> Apply expression per row -> Sort table columns
  - Takes a results or measurement table and produces a cleaned table by removing selected columns, applying a per-row expression to compute or adjust values, and reordering the remaining columns.
- **Filter, sort, and rename a table** · support 2 · geometric-cluster · `cluster-0165`
  - 3 nodes / 2 edges — Select table rows -> Sort table rows -> Rename table column
  - Takes a results table, selects specific rows, sorts the remaining rows by chosen columns, and renames columns, producing a cleaned and ordered table.
- **Import and complete results tables** · support 1 · geometric-cluster · `cluster-0260`
  - 3 nodes / 2 edges — File list -> Import results table -> Add missing rows (series)
  - Takes a list of CSV/XLSX results-table files, imports each file into a table, and inserts missing rows for a numeric series such as time points. It produces completed results tables ready for downstream analysis.
- **Create pixel table from generated image** · support 1 · geometric-cluster · `cluster-0306`
  - 4 nodes / 3 edges — Generate image from math expression -> Get pixels as table -> Rename table column -> Remove table column
  - Generates an image from a mathematical expression, extracts its pixel coordinates and values into a table, then renames and removes selected columns. It produces a cleaned table of pixel values for downstream analysis.

### statistics (15)

- **Colocalization within segmented cell 2D ROIs** · support 14 · canvas-note-section · `cluster-0014`
  - 9 nodes / 8 edges — Auto threshold 2D (8-bit) -> Set annotation (expression) -> Auto threshold 2D (8-bit) -> Gaussian blur 2D -> Gaussian blur 2D -> Cellpose prediction (2.x) ->…
  - Takes two 8-bit image channels, applies Gaussian blur and auto-thresholding, sets an annotation, runs Cellpose to produce cell 2D ROIs, and computes Coloc 2 colocalization statistics for the two channels within those ROIs.
- **Colocalization metrics and histograms** · support 8 · canvas-note-section · `cluster-0033`
  - 5 nodes / 4 edges — Coloc 2 -> Add annotations as columns -> Merge table rows -> JFreeChart Histogram plot -> JFreeChart Histogram plot
  - Takes image pairs and computes pixel-based colocalization coefficients (e.g., Pearson, Manders, Costes) with significance testing. It converts the result annotations into table columns, merges the per-item results, and plots histograms of the colocalization metrics.
- **Colocalization statistics from 2D ROIs** · support 6 · canvas-note-section · `cluster-0052`
  - 6 nodes / 5 edges — Mask to 2D ROI -> Coloc 2 -> Add annotations as columns -> Merge table rows -> JFreeChart Histogram plot -> JFreeChart Histogram plot
  - Takes a thresholded mask and image channels, converts the mask into a 2D ROI, and runs Coloc 2 to compute pixel-intensity colocalization statistics such as Pearson and Manders coefficients. It then adds the results as table columns, merges them into one table, and plots the values as histograms.
- **Pairwise 3D ROI statistics and histogram** · support 5 · canvas-note-section · `cluster-0060`
  - 4 nodes / 3 edges — Extract pairwise IJ3D ROI statistics -> Export data -> JFreeChart Histogram plot -> Export image
  - Takes 3D ROIs and computes all pairwise measurements between them, producing a statistics table. It exports that table and a histogram plot image of the measurements.
- **Measure overlap between two mask images** · support 4 · geometric-cluster · `cluster-0086`
  - 6 nodes / 4 edges — Divide by maximum -> Measure label overlap -> Add annotations as columns -> Add annotations as columns -> Merge table rows -> Export table
  - Takes two label/binary mask images, optionally normalizes pixel values by the global maximum, and computes overlap, Jaccard, Dice, volume similarity, false negative, and false positive metrics. It converts the measurements into table rows, merges multiple result tables, and exports them to CSV/XLSX.
- **Measure 2D ROIs in a mask** · support 4 · canvas-note-section · `cluster-0094`
  - 9 nodes / 8 edges — Connected components labeling 2D -> Filter labels by statistics 2D -> Extract label statistics 2D -> Rename table column -> Export data -> Add annotations as…
  - Takes a 2D mask and produces an exported table of measurements for each detected 2D ROI. It labels connected components, filters labels by statistics, extracts label statistics, renames and annotates table columns, merges rows, and exports the table.
- **Measure total edge length in 2D binary images** · support 4 · canvas-note-section · `cluster-0106`
  - 5 nodes / 4 edges — Morphological skeletonize 2D -> Extract image statistics (Expression) -> Merge table rows -> Apply expression per row -> Export table as multi-sheet XLSX
  - Takes 2D binary images, skeletonizes each slice, extracts image statistics, merges the rows, applies an expression to compute total edge length, and exports the results as a multi-sheet XLSX table.
- **Edge width histogram from images** · support 4 · canvas-note-section · `cluster-0108`
  - 9 nodes / 8 edges — Local maxima 2D -> Auto threshold 2D (8-bit) -> Set to value (grayscale) -> Image to pixel value/count histogram -> Filter table rows -> JFreeChart XY line p…
  - Summarizes edge widths from input images by detecting local maxima, auto-thresholding the result, and converting it to a grayscale image. It then filters the pixel value/count histogram, renames columns, and exports the histogram table as XLSX and the XY line plot as an image.
- **3D ROI nearest-neighbor distance histogram** · support 3 · canvas-note-section · `cluster-0121`
  - 7 nodes / 6 edges — IJ3D Find particles 3D -> Extract IJ3D ROI statistics -> Export data -> Run Python script (iterating) -> JFreeChart Histogram plot -> Export data -> Export i…
  - Takes a binary mask image, finds 3D particles as 3D ROIs, extracts their 3D ROI measurements, and computes nearest-neighbor distances with a Python script to produce a histogram and exported data/plot files.
- **Measure pairwise distances between 3D ROIs** · support 2 · compartment · `cluster-0184`
  - 10 nodes / 6 edges — Extract IJ3D ROI statistics -> Extract pairwise IJ3D ROI statistics -> Connected components labeling 3D -> Extract pairwise IJ3D ROI statistics -> Extract IJ…
  - Takes a 3D mask, labels connected components, converts the labels to 3D ROIs, and extracts individual and pairwise 3D ROI statistics such as distances into a results table, with Python scripts used to post-process the measurements.
- **Measure tracked objects and export histograms** · support 2 · compartment · `cluster-0197`
  - 11 nodes / 8 edges — Measure spots -> Measure tracks -> Measure edges -> JFreeChart Histogram plot -> Iteration step slicer -> Iteration step slicer -> Export image -> JFreeChart…
  - Takes TrackMate spot, track, and edge data, computes measurement tables, and exports histogram images of track measurements.
- **Export image statistics as a table** · support 1 · geometric-cluster · `cluster-0235`
  - 5 nodes / 4 edges — Extract image statistics -> Add annotations as columns -> Merge table rows -> Rename table column -> Export data (path input)
  - Takes image data and optional annotations, extracts whole-image or masked intensity statistics, adds annotation columns, merges per-image rows into one table, renames columns, and exports the combined table to a file path.
- **2D distance transform histogram** · support 1 · geometric-cluster · `cluster-0263`
  - 3 nodes / 3 edges — Generate image from math expression -> Euclidean distance transform 2D -> Key/Value Histogram 5D
  - Produces a histogram of distances from a 2D binary mask. It applies a pixel-wise math expression to an image, computes a 2D Euclidean distance transform, and bins the resulting distance values.
- **Filter data by median 2D ROI size** · support 1 · canvas-note-section · `cluster-0281`
  - 4 nodes / 3 edges — Find particles 2D -> Summarize table -> Annotate data with table values -> Filter by annotation (If else)
  - Produces a data stream filtered by the median size of 2D ROIs found in thresholded images. It finds 2D ROIs from the images, summarizes their measurements to a median value, annotates the incoming data with that value, and splits the data into matching and non-matching outputs.
- **Estimate thickness of binary objects** · support 1 · canvas-note-section · `cluster-0290`
  - 12 nodes / 12 edges — Split & filter by annotation -> Euclidean distance transform 2D -> Get pixels as table -> Filter table rows -> JFreeChart Histogram plot -> Summarize table -…
  - Takes binary images, splits them by annotation, computes a 2D Euclidean distance transform, and summarizes the resulting distance values into thickness statistics and a histogram. It converts the summary into parameters that can drive a 2D morphological operation.

### plotting (17)

- **Export histogram plot as image** · support 5 · geometric-cluster · `cluster-0059`
  - 3 nodes / 2 edges — JFreeChart Histogram plot -> Create montage -> Export image
  - Takes tabular data, renders a histogram plot, passes it through a montage step to form an image, and exports that image to a standard file format.
- **Plot histogram of filament measurements** · support 4 · canvas-note-section · `cluster-0069`
  - 4 nodes / 3 edges — Measure filament components -> Filter tables -> Filter table rows -> JFreeChart Histogram plot
  - Takes a filament dataset, computes per-component measurements such as length and radius, filters the resulting measurement table to the desired rows, and produces a histogram plot of the selected values.
- **Create and export summary plots and table** · support 4 · canvas-note-section · `cluster-0073`
  - 11 nodes / 10 edges — Merge table columns (simple) -> Merge table rows -> Apply expression per row -> JFreeChart Statistical line category plot -> Annotate with source slot -> JFr…
  - Produces summary line and bar plots and an exported table from multiple input tables. It merges the tables' columns and rows, applies a per-row expression, adds source labels, and exports the table and plot images.
- **Plot tabular data with scatter, box, and line charts** · support 4 · geometric-cluster · `cluster-0074`
  - 3 nodes / 0 edges — JFreeChart XY scatter plot -> JFreeChart Box plot -> JFreeChart XY line plot
  - Takes tabular data and produces an XY scatter plot, a box-and-whisker plot, and an XY line plot for visualizing numeric variables.
- **Summarize and plot table data as an XY line chart** · support 4 · geometric-cluster · `cluster-0079`
  - 4 nodes / 3 edges — Summarize table -> Add annotations as columns -> Merge table rows -> JFreeChart XY line plot
  - Takes one or more tables, computes summary statistics for selected columns, adds annotation values as columns, merges the resulting rows, and creates an XY line plot of the summarized data.
- **Plot pixel values from a masked image** · support 4 · canvas-note-section · `cluster-0107`
  - 9 nodes / 8 edges — Morphological operation 2D -> Set to color (RGB) -> Get pixels as table -> Filter table rows -> Apply expression per row -> JFreeChart XY scatter plot -> JFr…
  - Takes an image, applies a 2D morphological operation to create a mask, sets the masked pixels to a color, extracts the pixel values into a table, filters and computes row expressions, and produces multiple XY scatter plots.
- **Grouped histograms from multiple tables** · support 3 · geometric-cluster · `cluster-0124`
  - 5 nodes / 4 edges — Merge table columns (simple) -> Add annotations as columns -> Merge table rows -> Split table by columns -> JFreeChart Histogram plot
  - Takes multiple tables, merges their columns and rows, adds annotation columns, splits the combined table by selected columns, and produces a histogram plot for each resulting group.
- **Plot pairwise 2D ROI statistics from spots** · support 2 · compartment · `cluster-0137`
  - 4 nodes / 4 edges — Convert spots to ROI -> Extract pairwise 2D ROI statistics -> JFreeChart Histogram plot -> Export image
  - Takes TrackMate spots, converts each spot into a 2D ROI, extracts all pairwise 2D ROI statistics, and exports a histogram image of those statistics.
- **2D ROI measurement histogram montage** · support 2 · geometric-cluster · `cluster-0154`
  - 5 nodes / 4 edges — Extract 2D ROI statistics -> JFreeChart Histogram plot -> Convert image to RGB -> Create montage -> Export image
  - Produces an exported montage image of histogram plots summarizing 2D ROI measurements. It takes 2D ROI statistics measurements, creates histogram plots, converts the plots to RGB, combines them into a montage, and exports the montage as an image.
- **Export a line plot of image statistics** · support 2 · canvas-note-section · `cluster-0162`
  - 4 nodes / 3 edges — Remove annotation -> Extract image statistics -> JFreeChart XY line plot -> Export image
  - Takes images, removes selected text annotations, extracts statistics for the whole image or a masked region, plots the measurements as an XY line chart, and exports the chart as an image file.
- **Export XY scatter plot as image** · support 2 · geometric-cluster · `cluster-0195`
  - 3 nodes / 2 edges — JFreeChart XY scatter plot -> Render to RGB -> Export image
  - Creates an XY scatter plot from tabular data, renders the plot to RGB, and exports it as a standard image file.
- **Plot histograms of 2D ROI statistics** · support 2 · geometric-cluster · `cluster-0200`
  - 6 nodes / 5 edges — Extract 2D ROI statistics -> Set/Edit annotations -> JFreeChart Histogram plot -> JFreeChart Histogram plot -> Split & filter by annotation -> Split & filter…
  - Takes 2D ROIs (with an optional reference image) and computes ROI statistics, sets/edits annotations, and creates histogram plots of the statistics, then splits/filters the plotted data by annotation value.
- **Pairwise 2D ROI statistics histogram** · support 2 · geometric-cluster · `cluster-0202`
  - 9 nodes / 10 edges — Extract pairwise 2D ROI statistics -> Filter table rows -> Table column to histogram -> Filter table rows -> Apply expression to columns -> Annotate data wit…
  - Takes 2D ROIs and produces histogram plots of selected pairwise ROI measurements. It extracts pairwise 2D ROI statistics, filters and transforms the resulting table, annotates the data with the computed values, and plots the selected column as histograms.
- **Plot 2D ROI properties as an XY line chart** · support 1 · canvas-note-section · `cluster-0229`
  - 3 nodes / 2 edges — Annotate with 2D ROI properties -> Convert to annotation table -> JFreeChart XY line plot
  - Takes data rows containing 2D ROI lists, extracts selected ROI properties as annotations, converts them into a table of values, and draws an XY line plot from those values.
- **Plot histogram of table measurements** · support 1 · geometric-cluster · `cluster-0230`
  - 6 nodes / 5 edges — IO Interface -> Remove table column -> Apply expression per row -> Sort table columns -> Merge table rows -> JFreeChart Histogram plot
  - Takes a measurements table, removes selected columns, applies a per-row expression, sorts columns, merges rows, and produces a histogram plot of the resulting values.
- **Plot histogram from category/value images** · support 1 · geometric-cluster · `cluster-0262`
  - 4 nodes / 3 edges — Key/Value Histogram 5D -> Merge table columns (simple) -> Apply expression per row -> JFreeChart XY line plot
  - Takes two same-sized images that store a category and a numeric value for each pixel, builds a histogram table, applies row-level calculations, and produces an XY line plot.
- **Create and export a histogram from merged tables** · support 1 · canvas-note-section · `cluster-0301`
  - 3 nodes / 2 edges — Merge table rows -> JFreeChart Histogram plot -> Export image
  - Takes multiple tables, merges their rows into a single table, creates a histogram plot of a numeric column, and exports the plot as an image file.

### export (15)

- **Export merged summary tables as XLSX** · support 8 · compartment · `cluster-0026`
  - 5 nodes / 2 edges — Export table as multi-sheet XLSX -> Merge table rows -> Add annotations as columns -> Merge table rows -> Export table as multi-sheet XLSX
  - Takes multiple result tables, merges their rows into summary tables, adds column annotations as extra columns, and writes the result to a multi-sheet XLSX workbook.
- **Export cleaned annotation table** · support 6 · geometric-cluster · `cluster-0047`
  - 11 nodes / 10 edges — Convert to annotation table -> Remove table column -> Remove table column -> Remove table column -> Remove table column -> Remove table column -> Remove tabl…
  - Takes data rows with annotations, converts them into an annotation table, removes configured columns and applies a Python table modification, then saves the resulting table as CSV or XLSX.
- **Batch export image files** · support 5 · geometric-cluster · `cluster-0065`
  - 3 nodes / 2 edges — File list -> Import image -> Export image
  - Takes a list of image files, imports each image, and writes them out as standard image files such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export 3D ROIs as a labeled image** · support 4 · geometric-cluster · `cluster-0071`
  - 6 nodes / 4 edges — Set IJ3D ROI properties -> Run Python script (iterating) -> Create empty image -> Convert IJ3D ROI to RGB -> Copy physical dimensions -> Export image
  - Takes a list of 3D ROIs and a blank image, updates the ROIs' properties, converts them into a labeled image, copies the physical pixel dimensions, and exports the image as a file.
- **Export multiple image channels as separate files** · support 4 · geometric-cluster · `cluster-0076`
  - 7 nodes / 6 edges — IO Interface -> Annotate with source slot -> Annotate with source slot -> Annotate with source slot -> Export image -> Export image -> Export image
  - Takes several image inputs, labels each with its source input name, and writes each out as a separate image file in a standard format.
- **Export computed results and histogram image** · support 4 · canvas-note-section · `cluster-0090`
  - 5 nodes / 4 edges — Run Python script (iterating) -> JFreeChart Histogram plot -> Export data -> Render to RGB -> Export image
  - Exports the output of an iterating Python script as a data file and a histogram image. The script's table is plotted as a histogram, rendered to RGB, and saved, while the table is also exported directly.
- **Export contrast-enhanced images** · support 3 · geometric-cluster · `cluster-0122`
  - 3 nodes / 2 edges — Copy physical dimensions -> Histogram-based contrast enhancer -> Export image
  - Takes input images, copies their physical pixel sizes, applies histogram-based contrast enhancement, and writes them out as standard image files such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export 3D mask as a 3D mesh** · support 2 · geometric-cluster · `cluster-0136`
  - 3 nodes / 2 edges — Generate structure element (3D) -> Mask to 3D scene -> Export 3D scene
  - Takes a 3D mask, generates a 3D structure element for the conversion, converts the mask into a 3D surface mesh using marching cubes, and writes the result to a Collada .dae file for viewing or downstream analysis.
- **Export image montage** · support 2 · geometric-cluster · `cluster-0155`
  - 3 nodes / 2 edges — Create montage -> Render to RGB -> Export image
  - Takes a set of input images, combines them into a single montage, renders the result to RGB while preserving LUT/contrast settings, and writes it out as an image file such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export corrected and enhanced 2D images** · support 2 · geometric-cluster · `cluster-0196`
  - 4 nodes / 3 edges — Illumination correction 2D -> Histogram-based contrast enhancer -> Set LUT (color map) -> Export image
  - Produces export-ready 2D images by applying illumination correction and histogram-based contrast enhancement, then setting a color map. It writes the processed images to common file formats such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export 2D ROI visualization images** · support 2 · compartment · `cluster-0218`
  - 11 nodes / 9 edges — Set 2D overlay -> JFreeChart Histogram plot -> Morphological operation 2D -> Connected components labeling 2D -> Render overlay -> Convert labels to RGB -> S…
  - Takes an image, a list of 2D ROIs, and a mask, and produces exported visualization images: a montage combining a rendered 2D ROI overlay with a histogram plot, and a blended image of colored connected-component labels and the mask.
- **Export 3D ROIs as colored 3D meshes** · support 1 · geometric-cluster · `cluster-0227`
  - 3 nodes / 2 edges — Color IJ3D ROI by statistics -> IJ3D ROI to 3D scene -> Export 3D scene
  - Takes a list of 3D ROIs and an optional reference image, colors each ROI by a selected measurement, converts the ROIs into 3D meshes, and writes the resulting 3D scene to a Collada .dae file.
- **Upload images and result tables to OMERO** · support 1 · geometric-cluster · `cluster-0274`
  - 3 nodes / 2 edges — Create OMERO dataset -> Upload image to OMERO dataset -> Attach table to OMERO image
  - Creates an OMERO dataset, uploads each OME image as an OME-TIFF into that dataset, and attaches a results table to the corresponding uploaded image.
- **Upload images from folders to OMERO** · support 1 · geometric-cluster · `cluster-0275`
  - 5 nodes / 4 edges — Folder list -> List files -> Annotate with file/directory name -> Import image -> Upload image to OMERO dataset
  - Takes a list of folders, lists the image files in them, adds a name label to each file, imports each image, and uploads it as an OME-TIFF to a chosen OMERO dataset, producing references to the uploaded OMERO images.
- **Batch export OME-ZARR images from folders** ⚠ unregistered id · support 1 · geometric-cluster · `cluster-0318`
  - 3 nodes / 2 edges — Folder list -> Import OME ZARR from directory -> Export OME ZARR
  - Takes a list of folders containing OME-ZARR image datasets, imports each dataset, and writes it out as an OME-ZARR file or ZIP, producing URI references to the exported images.

## Rejected (false-negative check)

59 families the model cut. The high-support / good-title ones
are where a false negative would hide.

- `cluster-0051` support 6 — **gate=workflow_plumbing** — 11n/0e
  - titles: ['Saving']
- `cluster-0075` support 4 — **gate=noise** — 4n/0e
- `cluster-0105` support 4 — **gate=noise** — 8n/4e
- `cluster-0109` support 4 — **gate=noise** — 4n/3e
  - titles: ['Combined methods: NOT USED']
- `cluster-0113` support 3 — **gate=workflow_plumbing** — 12n/9e
  - titles: ['Statistics']
- `cluster-0143` support 2 — **gate=noise** — 7n/4e
- `cluster-0144` support 2 — **gate=workflow_plumbing** — 7n/4e
- `cluster-0149` support 2 — **gate=workflow_plumbing** — 4n/3e
- `cluster-0150` support 2 — **gate=workflow_plumbing** — 9n/8e
- `cluster-0151` support 2 — **gate=workflow_plumbing** — 11n/10e
- `cluster-0157` support 2 — **gate=workflow_plumbing** — 3n/0e
  - titles: ['Peroxisomal GFP']
- `cluster-0160` support 2 — **gate=noise** — 7n/3e
- `cluster-0163` support 2 — **gate=workflow_plumbing** — 11n/11e
  - titles: ['PredSpectra', 'ValSpectra']
- `cluster-0166` support 2 — **gate=workflow_plumbing** — 7n/5e
- `cluster-0167` support 2 — **gate=workflow_plumbing** — 8n/7e
- `cluster-0168` support 2 — **gate=workflow_plumbing** — 8n/8e
  - titles: ['Pores enclosing hyphae']
- `cluster-0178` support 2 — **gate=workflow_plumbing** — 8n/7e
  - titles: ['Ganztier', 'Organe']
- `cluster-0180` support 2 — **gate=noise** — 11n/11e
- `cluster-0185` support 2 — **gate=noise** — 4n/1e
- `cluster-0188` support 2 — **gate=workflow_plumbing** — 12n/11e
  - titles: ['Time dependence']
- `cluster-0191` support 2 — **gate=workflow_plumbing** — 5n/3e
  - titles: ['File handling']
- `cluster-0199` support 2 — **gate=workflow_plumbing** — 7n/5e
  - titles: ['Preprocessing']
- `cluster-0203` support 2 — **gate=workflow_plumbing** — 9n/9e
- `cluster-0205` support 2 — **gate=noise** — 3n/1e
- `cluster-0226` support 2 — **gate=workflow_plumbing** — 4n/3e
  - titles: ['Adrian Feile data']
- `cluster-0232` support 1 — **gate=workflow_plumbing** — 4n/4e
  - titles: ['Import/Edit Mask']
- `cluster-0233` support 1 — **gate=workflow_plumbing** — 6n/5e
  - titles: ['Import Paths']
- `cluster-0234` support 1 — **gate=workflow_plumbing** — 6n/5e
  - titles: ['Pre-processing']
- `cluster-0238` support 1 — **gate=workflow_plumbing** — 8n/7e
  - titles: ['Quantification']
- `cluster-0240` support 1 — **gate=workflow_plumbing** — 5n/4e
  - titles: ['Visualization']
- `cluster-0241` support 1 — **gate=noise** — 3n/2e
- `cluster-0243` support 1 — **gate=workflow_plumbing** — 3n/0e
  - titles: ['Measurements']
- `cluster-0245` support 1 — **gate=noise** — 3n/1e
- `cluster-0247` support 1 — **gate=workflow_plumbing** — 8n/6e
  - titles: ['Postprocessing']
- `cluster-0251` support 1 — **gate=workflow_plumbing** — 8n/7e
- `cluster-0253` support 1 — **gate=noise** — 7n/6e
- `cluster-0255` support 1 — **gate=workflow_plumbing** — 3n/2e
- `cluster-0261` support 1 — **gate=workflow_plumbing** — 4n/3e
- `cluster-0269` support 1 — **gate=workflow_plumbing** — 8n/7e
- `cluster-0270` support 1 — **gate=noise** — 3n/0e
- `cluster-0271` support 1 — **gate=noise** — 3n/0e
- `cluster-0277` support 1 — **gate=workflow_plumbing** — 5n/4e
- `cluster-0278` support 1 — **gate=workflow_plumbing** — 3n/3e
  - titles: ['Connect with yeast heads']
- `cluster-0284` support 1 — **gate=workflow_plumbing** — 10n/9e
  - titles: ['Determine approx. yeast size by PBS']
- `cluster-0289` support 1 — **gate=workflow_plumbing** — 3n/3e
  - titles: ['Classify for yeast cells and hyphae']
- `cluster-0293` support 1 — **gate=workflow_plumbing** — 12n/7e
  - titles: ['Hyphae analysis - AnaMorf']
- `cluster-0294` support 1 — **gate=workflow_plumbing** — 11n/9e
  - titles: ['File handler [1] -1st tile', 'File handler [3] - 1th tile']
- `cluster-0298` support 1 — **gate=workflow_plumbing** — 5n/4e
- `cluster-0299` support 1 — **gate=workflow_plumbing** — 6n/5e
  - titles: ['Find CZI and metadata extraction']
- `cluster-0307` support 1 — **gate=workflow_plumbing** — 5n/3e
- `cluster-0308` support 1 — **gate=noise** — 4n/0e
  - titles: ['CellPose']
- `cluster-0310` support 1 — **gate=noise** — 3n/1e
- `cluster-0312` support 1 — **gate=workflow_plumbing** — 6n/4e
- `cluster-0313` support 1 — **gate=workflow_plumbing** — 4n/3e
- `cluster-0314` support 1 — **gate=noise** — 3n/2e
- `cluster-0317` support 1 — **gate=workflow_plumbing** — 4n/3e
- `cluster-0319` support 1 — **gate=workflow_plumbing** — 9n/8e
  - titles: ['3D inputs']
- `cluster-0321` support 1 — **gate=workflow_plumbing** — 6n/5e
  - titles: ['2D inputs']
- `cluster-0324` support 1 — **gate=workflow_plumbing** — 6n/4e

## Flags

**Same name, different graph** (legal — the browser dedups on (name, graph)):
- 'Detect 2D particle ROIs in images' × 2: cluster-0002, cluster-0114
- 'Load images from folders' × 2: cluster-0101, cluster-0228
- 'Overlay 2D ROIs on an image' × 2: cluster-0128, cluster-0256
- 'Render 2D ROIs as an RGB image' × 2: cluster-0204, cluster-0099
- 'Segment 2D images into 2D ROIs' × 2: cluster-0239, cluster-0316

**Mixed 2D+3D ROI graph (decision 4: keep + renamed to state the span):**
- `cluster-0104` 'Cellpose cell segmentation to 3D ROIs'
- `cluster-0103` 'Create 2D/3D particle ROIs and label edge images'

