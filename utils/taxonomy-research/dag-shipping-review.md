# DAG Pattern Mining — Shipping Review (steps A–C′)

Work item 1324 · branch `1324-node-taxonomy-core` · 2026-10-19

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

**6 shipped patterns reference a node id not registered in the
6.0.0 launcher** (the corpus predates 6.0.0; some IDs were renamed, some
live in plugins the 6.0.0 distribution does not ship). Verified against the
FULL-LAUNCHER registry dump (2026-10-19): the earlier code/test-classpath
dump was blind to the dynamically-registered cLij2/clijx kernels — the
real launcher ships cLij2 2.5.3.5 + clijx 0.32.2.0, which resolved 9 of the
former 15. The launcher guard `everyPatternGraphDeserializes` asserts
every node id resolves, so these FAIL unless remapped or excluded in (D).
See `dag-stale-id-plan.json` for the remap/exclude list.

**Renamable** (all stale ids have a clean current successor): 3
- `cluster-0133` support 3 'Tile multiple images into a grid' → ['ij1-transform-tile']
- `cluster-0318` support 1 'Batch export OME-ZARR images from folders' → ['import-ome-zarr-from-directory-as-imgplus']
- `cluster-0242` support 1 'Detect lines in 2D images' → ['ij1-transform-tile']

**Exclude or plugin-gate** (optional-plugin nodes absent from the default launcher): 3
- `cluster-0134` support 3 'Train and predict with a generative model' → ['create-jipipe2ai-generation-model', 'jipipe2ai-predict-generation', 'jipipe2ai-train-generation-model']
- `cluster-0216` support 2 'Train and predict image segmentation' → ['create-jipipe2ai-segmentation-model', 'jipipe2ai-predict-image', 'jipipe2ai-train-image-model']
- `cluster-0254` support 1 'Train a deep-learning model from labeled images' → ['deep-learning-train-image-model']

Shippable after remap + exclude: **262**.

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

- **Import, split, and apply false colors to microscopy images** · support 26 · compartment · `cluster-0007`
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
- **Export false-color multichannel images with scale bars** · support 6 · canvas-note-section · `cluster-0045`
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
- **Preprocess microscopy image stacks** · support 5 · compartment · `cluster-0058`
  - 7 nodes / 5 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumi…
  - Takes microscopy image files and produces a corrected image stack by importing with Bio-Formats, annotating image properties, splitting a chosen dimension, applying an extended depth-of-focus Sobel projection, correcting 2D illumination, and merging the processed slices back into a stack.
- **Import images and denoise with median filter** · support 4 · compartment · `cluster-0067`
  - 3 nodes / 2 edges — File list -> Import image -> Median filter 2D
  - Takes a list of image files, imports each as an image, and applies a 2D median filter to each channel or slice. It produces denoised images suitable for downstream analysis.
- **Subtract a filament mask from an image to highlight nodules** · support 4 · canvas-note-section · `cluster-0070`
  - 11 nodes / 13 edges — Invert pixel colors/values -> Morphological operation 2D -> Set to color (RGB) -> Set to value (grayscale) -> Euclidean distance transform 2D -> Set filament…
  - Takes an image and filament data, derives a binary mask from the inverted image, computes a distance transform to set each filament vertex radius, converts the filaments to a mask, and subtracts that mask from the inverted image to produce an image highlighting nodule-like structures.
- **Project and correct a multi-dimensional image** · support 4 · group-node · `cluster-0072`
  - 7 nodes / 6 edges — Group input -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumination correction 2D -> Merge …
  - Takes a multi-dimensional image, splits it by a selected dimension, applies an extended depth-of-focus Sobel projection and 2D illumination correction to each part, and merges the results into a 2D image stack.
- **Normalize and combine labeled images** · support 4 · geometric-cluster · `cluster-0085`
  - 9 nodes / 10 edges — Split & filter by annotation -> Filter by annotation (If else) -> Filter by annotation (If else) -> Divide by maximum -> Divide by maximum -> Divide by maxim…
  - Takes image data with categorical labels and separates it into branches by label. It normalizes each branch by its global maximum and applies 2D image arithmetic to produce combined or transformed normalized images.
- **Create extended depth of focus projections from microscopy images** · support 4 · compartment · `cluster-0091`
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
- **Segment 2D images with edge projection and watershed** · support 2 · geometric-cluster · `cluster-0140`
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
- **Compute extended depth-of-focus projection from selected image planes** · support 2 · canvas-note-section · `cluster-0152`
  - 3 nodes / 2 edges — Reduce & split hyperstack (Expression) -> Reorder dimensions -> Cli Jx Extended Depth Of Focus Sobel Projection
  - Takes a multi-plane image, selects/splits planes using expressions and reorders dimensions, then applies an extended depth-of-focus Sobel projection to produce a projected image.
- **Enhance and blend images** · support 2 · compartment · `cluster-0158`
  - 3 nodes / 1 edges — Convert image to 8-bit -> Histogram-based contrast enhancer -> Blend images
  - Takes image inputs, converts them to 8-bit grayscale, applies histogram-based contrast enhancement, and blends the enhanced images into one RGB image.
- **Load microscopy images from folders and convert to 32-bit** · support 2 · compartment · `cluster-0164`
  - 9 nodes / 7 edges — Folder list -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -…
  - Takes a list of folders, annotates each folder entry, imports microscopy image files with Bio-Formats, and converts each imported image to a 32-bit grayscale image.
- **Create filament masks from 3D image stacks** · support 2 · canvas-note-section · `cluster-0169`
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
- **False-colored extended depth-of-focus image projection** · support 2 · geometric-cluster · `cluster-0186`
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
- **Create a false-color distance map from 8-bit images** · support 2 · geometric-cluster · `cluster-0225`
  - 10 nodes / 9 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Invert pixel colors/values -> Euclidean distance transform 2D -> Fast…
  - Takes image files, thresholds and morphologically cleans them, computes a Euclidean distance transform, remaps the result as labels, outputs the result as a false-color RGB image.
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
- **Create 2D/3D particle ROIs and label edge images** · support 4 · canvas-note-section · `cluster-0103`
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
- **Detect 2D particle ROIs from microscopy image stacks** · support 2 · compartment · `cluster-0153`
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

## Derived patterns (C′) — the "what next" siblings, for review

The (C′) derivation step fills the gap the review found in the mined
set: what an analyst would DERIVE from a pattern, not just what the
corpus happened to contain. Two mechanisms:

- **twin-swap** (deterministic, no model): the verified 2D↔3D twin of
  a shipped pattern — the other dimension is derivable by construction,
  so it inherits the source's already-(C)-reviewed name with the
  dimension token swapped.
- **what-next** (model-proposed): a short chain of real nodes extending
  the pattern's outputs. Every proposal is validated fail-closed against
  the real registry (node ids, exact slot names, type assignability,
  no cycles, ≤10 nodes) and gated with the (C) rules (no support
  heuristic — it was never observed).

Provenance lives in `dag-derived.json` only — per the review decision
they ship as normal patterns. The (D) guards apply identically
(§3.4 locations, deserialization, `SystemPatternTemplatesTest` re-pin).

### Funnel

| stage | count |
|---|---|
| shipped mined patterns swept | 265 (4 parallel shards) |
| deterministic twin candidates | 26 (all shipped by construction) |
| what-next candidates (validated + deduped) | 159 |
|   → gated shipped | **134** |
|   → gate noise | 25 |
| what-next proposals rejected by fail-closed validation | 52 |
| **total derived shipped** | **160** |

### twin-swap (26) — the dimension siblings

Source pattern → twin. The name/description are the source's,
dimension-renamed — flag any where the rename reads oddly.

- **Filter 3D ROIs by overlap** (3D) — from “Filter 2D ROIs by overlap” (`cluster-0003`)
  - Takes two lists of 3D ROIs and produces filtered 3D ROI lists containing the ROIs that overlap with ROIs in the other list. Chained overlap filters can be used to include or exclude ROIs based on their spatial relationship to reference ROIs.
- **Attach 2D ROI measurements to data** (2D) — from “Attach 3D ROI measurements to data” (`cluster-0005`)
  - Takes 2D ROIs and optionally a reference image, and produces data annotated with 2D ROI statistics. It extracts the ROI measurements, adds a table column, and attaches the resulting table values to the incoming data.
- **Visualize 2D ROIs as colored label images** (2D) — from “Visualize 3D ROIs as colored label images” (`cluster-0008`)
  - Takes a list of 2D ROIs and an optional reference image, colors each ROI by a selected measurement, and converts the colored 2D ROIs into RGB label images for visualization.
- **Color overlay of overlapping 2D ROIs** (2D) — from “Color overlay of overlapping 3D ROIs” (`cluster-0028`)
  - Takes two lists of 2D ROIs and produces an exported RGB image showing which objects overlap. It filters each list to ROIs that overlap the other, rasterizes them onto blank images, blends the two label images in different colors, copies physical pixel sizes, and exports the result.
- **Pairwise 2D ROI distance statistics** (2D) — from “Pairwise 3D ROI distance statistics” (`cluster-0029`)
  - Takes 2D ROIs, extracts all pairwise ROI statistics such as distances, plots the measurements as histograms, and exports the measurement table and rendered plot images.
- **Load and denoise images from folders** (3D) — from “Load and denoise images from folders” (`cluster-0035`)
  - Takes a configured project folder, lists subfolders and image files, annotates and filters the paths, imports each image, and applies a 3D median filter to reduce noise in each channel or slice. It produces denoised image data with folder and file annotations.
- **Pairwise 2D ROI statistics and histogram** (2D) — from “Pairwise 3D ROI statistics and histogram” (`cluster-0060`)
  - Takes 2D ROIs and computes all pairwise measurements between them, producing a statistics table. It exports that table and a histogram plot image of the measurements.
- **Import images and denoise with median filter** (3D) — from “Import images and denoise with median filter” (`cluster-0067`)
  - Takes a list of image files, imports each as an image, and applies a 3D median filter to each channel or slice. It produces denoised images suitable for downstream analysis.
- **Export 2D ROIs as a labeled image** (2D) — from “Export 3D ROIs as a labeled image” (`cluster-0071`)
  - Takes a list of 2D ROIs and a blank image, updates the ROIs' properties, converts them into a labeled image, copies the physical pixel dimensions, and exports the image as a file.
- **Render 3D ROIs as RGB images** (3D) — from “Render 2D ROIs as RGB images” (`cluster-0096`)
  - Takes 3D ROI lists, assigns user-defined properties to the ROIs, and converts them into RGB images that draw the ROIs as colored outlines, fills, and optional labels. It can combine two ROI visualizations by using one rendered image as the reference canvas for the other.
- **Render 3D ROIs as an RGB image** (3D) — from “Render 2D ROIs as an RGB image” (`cluster-0099`)
  - Takes a 3D ROI list and an optional reference image, and produces an RGB image in which the ROIs are drawn as colored outlines, fills, and optional labels.
- **Plot pixel values from a masked image** (3D) — from “Plot pixel values from a masked image” (`cluster-0107`)
  - Takes an image, applies a 3D morphological operation to create a mask, sets the masked pixels to a color, extracts the pixel values into a table, filters and computes row expressions, and produces multiple XY scatter plots.
- **Overlay 3D ROIs on an image** (3D) — from “Overlay 2D ROIs on an image” (`cluster-0128`)
  - Takes an input image and a list of 3D ROIs, applies histogram-based contrast enhancement to the image, and draws the ROIs as colored outlines, fills, and optional labels. It produces an RGB image showing the ROIs overlaid on the enhanced image.
- **Import and scale 3D images** (3D) — from “Import and scale 2D images” (`cluster-0132`)
  - Takes a list of image file names, imports each file as an ImageJ image, and applies a 3D scaling transform to each image or 3D slice, producing rescaled 3D images.
- **Load and filter 3D ROI files** (3D) — from “Load and filter 2D ROI files” (`cluster-0139`)
  - Produces filtered 3D ROI data by importing ROI files from file paths, adding metadata from file/directory names and expressions, and splitting the ROIs based on an annotation value.
- **Summarize 3D ROI measurements by label** (3D) — from “Summarize 2D ROI measurements by label” (`cluster-0141`)
  - Takes 3D ROIs, sets ROI properties from expressions, extracts per-ROI statistics optionally from a reference image, and produces a table summarizing those statistics by label.
- **Create colored images from 3D ROIs** (3D) — from “Create colored images from 2D ROIs” (`cluster-0145`)
  - Takes image paths and matching 3D ROI files, imports the ROIs, and applies a specified color to the image using those ROIs, producing a colored image that visualizes manual annotations.
- **3D ROI measurement histogram montage** (3D) — from “2D ROI measurement histogram montage” (`cluster-0154`)
  - Produces an exported montage image of histogram plots summarizing 3D ROI measurements. It takes 3D ROI statistics measurements, creates histogram plots, converts the plots to RGB, combines them into a montage, and exports the montage as an image.
- **Quick RGB preview of 3D ROIs on images** (3D) — from “Quick RGB preview of 2D ROIs on images” (`cluster-0173`)
  - Takes a 3D ROI list and images, draws the 3D ROIs as colored outlines, applies color maps to the images, and blends them into one RGB image for quick visual inspection.
- **Plot histograms of 3D ROI statistics** (3D) — from “Plot histograms of 2D ROI statistics” (`cluster-0200`)
  - Takes 3D ROIs (with an optional reference image) and computes ROI statistics, sets/edits annotations, and creates histogram plots of the statistics, then splits/filters the plotted data by annotation value.
- **Pairwise 3D ROI statistics histogram** (3D) — from “Pairwise 2D ROI statistics histogram” (`cluster-0202`)
  - Takes 3D ROIs and produces histogram plots of selected pairwise ROI measurements. It extracts pairwise 3D ROI statistics, filters and transforms the resulting table, annotates the data with the computed values, and plots the selected column as histograms.
- **Render 3D ROIs as an RGB image** (3D) — from “Render 2D ROIs as an RGB image” (`cluster-0204`)
  - Takes a 3D ROI list and produces an RGB image that draws the ROIs as colored outlines, fills, or labels. It renders the ROIs to RGB, applies a predefined color map LUT, and renders them again using the first rendered image as the reference canvas.
- **Import images with scale bar and 3D ROI overlay** (3D) — from “Import images with scale bar and 2D ROI overlay” (`cluster-0224`)
  - Takes a list of image files, imports them as ImageJ images, sets their physical dimensions, draws a scale bar, and applies a 3D ROI overlay, producing calibrated images ready for display or further analysis.
- **Smooth images with Gaussian blur** (3D) — from “Smooth images with Gaussian blur” (`cluster-0250`)
  - Takes image data and produces smoothed images by applying a 3D Gaussian blur to each slice.
- **Create image montage from files** (3D) — from “Create image montage from files” (`cluster-0267`)
  - Takes a list of image files, imports each image, scales and converts it to 8-bit RGB, and combines the results into a single montage image.
- **RGB image with 3D ROI overlays** (3D) — from “RGB image with 2D ROI overlays” (`cluster-0323`)
  - Takes an image and a list of 3D ROIs, enhances contrast, applies a color map to create an RGB image, and draws the 3D ROIs as colored outlines, fills, or labels, producing a single RGB visualization image.

### what-next (128) — grouped by function

Each extends a mined pattern (its source is named); the chain is
verified to type-check against the real node slots.

#### annotations (19)

- **Annotate data with XML metadata** — extends “Extract OME image metadata as annotations” (`cluster-0019`) · 2n/1e
  - Takes XML/text data and a data stream, extracts selected XML values into a table, and attaches those values as annotations to the data. This adds file or experiment metadata to the incoming data.
- **Filter data by OME metadata annotation** — extends “Extract OME image metadata as annotations” (`cluster-0019`) · 2n/1e
  - Takes data items, annotates them with OME metadata, and keeps only items whose annotation satisfies a specified condition. Outputs the selected items with their annotations preserved.
- **Annotate data with table and image metadata** — extends “Segment and measure 3D ROIs from an image” (`cluster-0027`) · 2n/1e
  - Takes data records and enriches them with values from a table and properties derived from an image. It produces annotated data carrying both table-based and image-based metadata.
- **Filter data by table-derived annotation** — extends “Clean and merge annotation tables” (`cluster-0030`) · 3n/2e
  - Enriches data with values from a table, derives or adjusts an annotation, and splits the data into matching and non-matching subsets.
- **Filter data items by table values** — extends “Clean and merge annotation tables” (`cluster-0030`) · 2n/1e
  - Takes data items and a table, adds table values as annotations, then keeps only items whose annotation satisfies a condition, producing filtered annotated data.
- **Data annotation table with origin labels** — extends “Filter data by label” (`cluster-0062`) · 2n/1e
  - Takes a data set, labels each row with its origin, and converts the rows into an annotation table. It produces a table of annotations that can be used to inspect or export the data's metadata.
- **Annotate files with name and Bio-Formats metadata** — extends “Batch export image files” (`cluster-0065`) · 2n/1e
  - Takes input files, adds annotations derived from file names and Bio-Formats metadata, and produces annotated files for downstream analysis.
- **Extract object centroids from a binary image** — extends “Create a binary mask from an image” (`cluster-0095`) · 3n/2e
  - Takes a binary image, labels connected objects, and computes their centroids. It produces a point list of object centers, converted from GPU to a standard CPU data type.
- **Annotate data with merged table values** — extends “Merge two tables after column cleanup” (`cluster-0111`) · 2n/1e
  - Takes a table and a data set, merges selected table columns into annotations, and applies those annotations to the data. Produces data carrying the merged table-derived annotations.
- **Add scale bar and convert image to 8-bit grayscale** — extends “Tile multiple images into a grid” (`cluster-0133`) · 2n/1e
  - Takes an image, draws a scale bar, and converts the image to 8-bit grayscale, producing a display-ready image with a scale bar.
- **Annotate data with custom table values** — extends “Compute 2D ROI statistics from image stacks” (`cluster-0161`) · 2n/1e
  - Takes data and one or more tables, uses a Python script to modify or merge the table contents, and then attaches the resulting values as annotations to the data. It produces annotated data enriched with custom table-derived fields.
- **Annotate data with computed table values** — extends “Filter, sort, and rename a table” (`cluster-0165`) · 2n/1e
  - Takes a table and a set of data items, computes a new value for each table row, and attaches that value to the matching data items, producing annotated data.
- **Annotate data with image and table values** — extends “Filter 2D ROIs by local variance and extract statistics” (`cluster-0175`) · 2n/1e
  - Takes a data set, adds annotations derived from an image and from a table, and produces the enriched data set.
- **Filter data by image-derived annotation** — extends “Load three-channel images from folders and combine them into RGB” (`cluster-0187`) · 2n/1e
  - Takes data items, adds annotations derived from image properties, and keeps only items that match a specified annotation condition. Produces a filtered data set with the annotations preserved.
- **Copy matching annotations between data sets** — extends “Create 2D ROI segmentation image and measurements table” (`cluster-0198`) · 2n/1e
  - It copies annotations from a source data set to a target data set when the annotations match. The source data is converted to an annotation table, and matching annotations are merged into the target data set.
- **Annotate data with image statistics** — extends “Skeletonize binary image and visualize 2D ROI as RGB” (`cluster-0211`) · 2n/1e
  - Takes images and a data collection, computes whole-image or masked intensity statistics, and attaches those statistics to the data as annotations.
- **Add table information to data** — extends “Create a tidy filament measurement table” (`cluster-0219`) · 2n/1e
  - Takes a table and a data set, adds table-level information to the table, then copies those values onto the data items. Produces data annotated with table-derived information.
- **Annotate data with imported measurement table** — extends “Measure and export filament metrics” (`cluster-0285`) · 2n/1e
  - Takes an external CSV or XLSX results table and incoming data, then attaches matching table values to the data items. It produces data enriched with imported measurements.
- **Add scale bar and open images in ImageJ** — extends “RGB track visualization and branch statistics” (`cluster-0311`) · 2n/1e
  - Takes images, draws a scale bar overlay onto them, and opens the annotated images in ImageJ for visual inspection.

#### images (11)

- **Import images with metadata from a table** — extends “Export merged summary tables as XLSX” (`cluster-0026`) · 3n/2e
  - Takes image files and a results table, imports both, and copies the first row of the table into the image metadata. It produces image data enriched with tabular metadata.
- **16-bit distance map from binary image** — extends “Segment objects from images by thresholding and labeling” (`cluster-0031`) · 2n/1e
  - Takes a binary image, computes a 2D distance map for each slice, and converts the result to 16-bit. This produces a compact distance map image suitable for visualization or downstream image analysis.
- **Color image with median smoothing and scale bar** — extends “Visualize tracked spots and trajectories” (`cluster-0056`) · 3n/2e
  - Takes a 2D or 3D image, converts it to RGB color, applies a 3x3 median filter to reduce noise, and adds a scale bar. It produces a cleaned color image with a scale bar for display or export.
- **Fill holes in Cellpose label images** — extends “Preprocess microscopy image stacks” (`cluster-0058`) · 4n/3e
  - Takes a Cellpose label image, applies GPU-accelerated hole filling to the segmentation mask, and returns a standard image containing the filled labels.
- **Create filled binary mask from image** — extends “Project and correct a multi-dimensional image” (`cluster-0072`) · 4n/3e
  - Takes an image, applies automatic thresholding to create a binary mask, and fills holes in that mask. It produces a cleaned binary image suitable for downstream segmentation or measurement.
- **Connected component labeling** — extends “Create a binary mask from an image” (`cluster-0095`) · 2n/1e
  - Takes an image, assigns a unique label to each connected component, and outputs a regular CPU image of the labeled components.
- **Create 32-bit 2D connected component labels** — extends “Dilate, fill holes, and erode a binary mask” (`cluster-0110`) · 2n/1e
  - Takes a 2D binary or multi-value mask, assigns a unique label to each connected component, and outputs a 32-bit grayscale label image for downstream analysis.
- **Background-corrected smoothed 8-bit image** — extends “Compute extended depth-of-focus projection from selected image planes” (`cluster-0152`) · 3n/2e
  - Prepares an image for display by removing background and smoothing it, then outputs an 8-bit grayscale image. Technically, it applies slice-wise rolling-ball background subtraction and Gaussian blur before converting intensities to 0–255.
- **Denoise and invert RGB images** — extends “Quick RGB preview of 2D ROIs on images” (`cluster-0173`) · 2n/1e
  - Takes RGB color images, applies a 3x3 median filter to reduce noise, then inverts the colors, producing denoised inverted RGB images.
- **8-bit connected component labeling** — extends “Create binary masks from images” (`cluster-0220`) · 2n/1e
  - Takes a binary mask and assigns each connected object a unique label value, then converts the resulting label image to 8-bit grayscale. This produces a compact label image in which every separate object in the mask is individually identified, ready for measurement, visualization, or downstream tools that expect 8-bit data.
- **Background-subtracted 2D images with scale bar** — extends “Create Z-projections from image files” (`cluster-0252`) · 2n/1e
  - Takes 2D images or image stacks, removes uneven background from each slice, and adds a scale bar overlay. It produces display-ready background-corrected images with a visible scale reference.

#### rois (17)

- **Denoised 3D cell ROIs** — extends “Import, split, and apply false colors to microscopy images” (`cluster-0007`) · 2n/1e
  - Takes a microscopy image, applies Cellpose restoration to reduce noise or blur, then segments cells to produce 3D ROIs.
- **Convert 2D ROI to a labeled mask** — extends “Detect 2D particle ROIs from images” (`cluster-0024`) · 2n/1e
  - Takes 2D ROI data, converts it to filaments, and then rasterizes it into a labels image where each ROI is assigned a label. This produces a labeled mask that can be used for downstream image analysis.
- **Import 2D ROIs from a results table** — extends “Export merged summary tables as XLSX” (`cluster-0026`) · 3n/3e
  - Takes a CSV or XLSX results table, creates rectangular or oval 2D ROIs from its rows, and attaches table columns as ROI metadata. It produces a set of 2D ROIs ready for downstream image analysis.
- **Create 2D point ROIs from a table** — extends “Export cleaned annotation table” (`cluster-0047`) · 3n/3e
  - Takes a CSV or XLSX table of point coordinates and optional fields, creates a list of 2D point ROIs, and attaches the corresponding table row as metadata to each ROI.
- **Cleaned filament 2D ROI set from JSON** — extends “Extract and render 2D filaments from binary images” (`cluster-0055`) · 4n/3e
  - Takes a filament JSON file, removes duplicate vertices and filters connected components by properties, then produces 2D line or point ROIs. The output is a cleaned 2D ROI set for downstream analysis.
- **Filament 3D ROI preparation** — extends “Extract and render 2D filaments from binary images” (`cluster-0055`) · 4n/3e
  - Takes filament data from a JSON file, removes duplicate vertices and filters connected components by properties, then outputs 3D ROIs for the remaining filaments.
- **Create 2D ROIs from filament vertices** — extends “Subtract a filament mask from an image to highlight nodules” (`cluster-0070`) · 2n/1e
  - Takes filament vertices, connects nearby vertices into filaments, and outputs them as 2D line ROIs. Single unconnected vertices are output as point ROIs.
- **Count filtered 2D ROIs** — extends “Render 2D ROIs as RGB images” (`cluster-0096`) · 2n/1e
  - It selects 2D ROIs by name or basic properties and summarizes how many remain. The output is a table of ROI counts, grouped by annotation when present.
- **2D ROI extraction from watershed-separated mask** — extends “Dilate, fill holes, and erode a binary mask” (`cluster-0110`) · 2n/1e
  - Takes a binary mask, separates touching objects using a distance-transform watershed, and converts the result into 2D ROIs with per-object measurements.
- **Merged 2D ROI metadata table** — extends “Export filtered 2D ROI image” (`cluster-0123`) · 2n/1e
  - Combines several 2D ROI lists into a single or grouped ROI list and then writes the properties of each ROI into a table. The result is a consolidated 2D ROI metadata table.
- **Create 3D ROIs from cleaned unbranched filaments** — extends “Create filament mask from 3D time-lapse” (`cluster-0126`) · 2n/1e
  - Takes 3D filament data, fixes merged unbranched filaments, and outputs 3D ROIs representing filament segments and points. It prepares filament structures for ROI-based visualization or downstream analysis.
- **Denoise and segment images into 2D ROI or 3D ROI data** — extends “Train and predict with a generative model” (`cluster-0134`) · 4n/3e
  - Takes image data, applies a pretrained Cellpose restoration model to denoise, deblur, or upsample it, then applies a pretrained Cellpose segmentation model to produce segmented 2D ROI or 3D ROI outputs.
- **Simplified filament 2D ROIs** — extends “Extract and clean 2D filaments from binary images” (`cluster-0135`) · 2n/1e
  - Takes filament traces, removes redundant intermediate points, and converts the simplified traces into 2D line ROIs, with isolated points becoming point ROIs.
- **Export selected 2D ROIs to a table** — extends “Create RGB montage of 2D ROIs” (`cluster-0201`) · 2n/1e
  - Takes a set of 2D ROIs, keeps only those matching a name or basic property, and produces a table of their coordinates and metadata. Technically, it filters an ROI list and converts the selected ROIs into tabular rows.
- **Create 2D point ROIs from a table with metadata** — extends “Create a tidy filament measurement table” (`cluster-0219`) · 2n/1e
  - Takes a table of point coordinates and optional metadata, creates a list of 2D point ROIs, and attaches the table's metadata to each ROI. It produces 2D point ROIs that can be overlaid on images or used in downstream ROI analysis.
- **Color and label 2D ROIs by measurements** — extends “Create RGB image from 2D ROIs” (`cluster-0322`) · 2n/1e
  - Takes a list of 2D ROIs with measurements, colors them by a selected measurement, and generates a name for each 2D ROI from an expression using annotations and statistics. Produces 2D ROIs with color coding and generated names.
- **Filter 2D ROIs by computed name** — extends “Create RGB image from 2D ROIs” (`cluster-0322`) · 2n/1e
  - Takes a list of 2D ROIs, assigns each a new name using an expression over annotations or statistics, and keeps only ROIs whose names match the filter. It outputs a filtered list of 2D ROIs.

#### filaments (24)

- **Merge 2D ROIs into filaments** — extends “Filter 2D ROIs by overlap” (`cluster-0003`) · 2n/1e
  - Takes multiple 2D ROI lists, combines them into one or grouped ROI lists, and converts the merged 2D ROIs into filament objects. It is a data-preparation step for filament analysis.
- **2D ROI filament overlay** — extends “Detect 2D particles and render 2D ROIs” (`cluster-0006`) · 4n/3e
  - Takes a thresholded image, detects 2D particle ROIs, converts those ROIs into filaments, and overlays the filaments on the image.
- **Cellpose 2D ROI to confidence-scored filaments** — extends “Segment cells into 2D/3D ROIs with a Cellpose model” (`cluster-0009`) · 3n/3e
  - Takes an image, segments cells with Cellpose, converts the resulting 2D ROIs into filaments, and assigns each filament vertex the Cellpose probability as a confidence score.
- **2D ROI filament visualization** — extends “Segment objects with Cellpose and draw 2D ROIs” (`cluster-0037`) · 2n/1e
  - Takes 2D ROIs, converts them into filament representations, and renders them as an RGB image. It produces a visual filament view of the input 2D ROIs.
- **2D ROI mask image** — extends “Segment objects with Cellpose and draw 2D ROIs” (`cluster-0037`) · 2n/1e
  - Turns 2D ROI annotations into a pixel mask image. It converts the 2D ROIs to filament data and then renders the filaments as solid pixels in a greyscale mask.
- **Convert selected 2D ROIs to filaments** — extends “2D ROI overlap counts and overlays” (`cluster-0040`) · 2n/1e
  - It takes a set of 2D ROIs, keeps the ones matching a name or basic property, and converts them into filament data. This is useful when 2D ROIs represent filaments and need to be used in filament-based analysis.
- **Repair non-branching filament traces** — extends “Create 3D masks from measured 3D ROIs” (`cluster-0043`) · 2n/1e
  - Takes preliminary filament points or traces, connects nearby points into continuous filaments, and resolves overlapping non-branching filaments. It produces cleaned filament paths for downstream filament analysis.
- **Cellpose 2D ROI filament visualization** — extends “Segment images with Cellpose and render 2D ROIs” (`cluster-0046`) · 3n/3e
  - Segments an image with Cellpose, converts the resulting 2D ROIs into filaments, and renders those filaments onto the Cellpose flow image. It produces an RGB visualization of the segmented objects as filaments.
- **2D filament skeleton to RGB image** — extends “Detect 3D particle ROIs and skeletonize their masks” (`cluster-0066`) · 2n/1e
  - Takes a binary 2D skeleton image, converts each slice into filament data, and renders the filaments as an RGB image. The result is a visual RGB representation of the filament network.
- **Labeled 2D filament image from skeleton** — extends “Detect 3D particle ROIs and skeletonize their masks” (`cluster-0066`) · 2n/1e
  - Takes a binary skeleton image and produces a labeled filament image, processing each 2D slice independently. Each filament is assigned a distinct label for downstream analysis.
- **2D ROI line segments to intensity-annotated filaments** — extends “Find boundary regions of selected 2D ROIs” (`cluster-0068`) · 3n/2e
  - Detects line-like structures in an image and converts them into filaments whose vertices carry the corresponding image intensity. Technically, it applies Hough line-segment detection to 2D slices, converts the resulting 2D ROIs to filaments, and samples vertex intensities from the image.
- **Vessel-like structure enhancement** — extends “Project and correct a multi-dimensional image” (`cluster-0072`) · 3n/2e
  - Takes a 2D or 3D image, converts it to 32-bit, applies a 2D vesselness filter to highlight vessel- or neurite-like structures slice by slice, and converts the result to 8-bit. It produces an 8-bit image with tubular structures enhanced.
- **Extract and track filaments from a binarized image with vertex intensities** — extends “Measure 2D ROIs in a mask” (`cluster-0094`) · 3n/3e
  - Takes a greyscale image, converts it to a binary mask, extracts and tracks filament segments, and assigns each filament vertex an intensity from the binary mask. Produces filament tracks with vertex intensity values.
- **Extract and overlay filament networks** — extends “Label connected components in 3D images” (`cluster-0119`) · 4n/4e
  - Takes an image or time-lapse stack, converts it to 32-bit grayscale, enhances vessel-like structures with a 2D vesselness filter, extracts filament networks with TSOAX, and returns an image with those filaments overlaid on the original data.
- **Labeled tracked filaments** — extends “Label connected components in 3D images” (`cluster-0119`) · 3n/3e
  - Takes a time-lapse image and produces a labeled image of tracked filaments. It converts the image to 32-bit, runs TSOAX filament tracking, and converts the resulting filaments into a labels image.
- **2D ROI to overlay filaments** — extends “Export 2D ROI overlay images” (`cluster-0127`) · 2n/1e
  - Takes 2D ROIs, converts them into equivalent filaments, and sets those filaments as an overlay. It produces overlay filaments derived from the input 2D ROIs for visualization.
- **Order and measure linear filament segments** — extends “Extract and clean 2D filaments from binary images” (`cluster-0135`) · 2n/1e
  - Takes a filament graph, orders the vertices of linear filament segments, and measures the edges and associated vertices. It produces a table of edge measurements for each linear filament.
- **Vesselness feature image** — extends “Background-subtracted image stack” (`cluster-0183`) · 2n/1e
  - Takes a 2D or 3D image, converts it to 32-bit grayscale, and applies a 2D vesselness filter to each slice to produce a vesselness/neurite-like feature image.
- **2D ROI border-filtered mask** — extends “Overlay 2D ROIs on corrected images” (`cluster-0192`) · 3n/2e
  - Takes 2D ROIs and produces a mask image of the corresponding filaments after removing components near the image borders. Technically, it converts 2D ROIs to filaments, filters border components, and rasterizes the remaining filaments into a greyscale mask.
- **2D ROI to filament conversion** — extends “Create RGB montage of 2D ROIs” (`cluster-0201`) · 2n/1e
  - Takes a collection of 2D ROIs, separates them into individual ROI entries, and converts each 2D ROI into equivalent filament data. It produces a filament dataset for downstream filament analysis.
- **Convert 2D ROIs to connected filaments** — extends “Skeletonize binary image and visualize 2D ROI as RGB” (`cluster-0211`) · 2n/1e
  - Takes 2D ROIs and turns them into filaments, then links nearby vertices to form continuous filament paths. It produces a filament dataset where separate ROI segments are joined into connected structures.
- **Track and visualize filaments** — extends “Best-focus projection and bleach correction” (`cluster-0249`) · 4n/5e
  - It takes an image, extracts and tracks filaments, and produces an RGB image showing the tracked filaments with vertex intensities sampled from the original image. It first converts the input to 8-bit grayscale, runs TSOAX 2D filament tracking, assigns vertex intensities, and renders the filaments onto the reference image.
- **Export corrected filament 3D ROIs** — extends “Fix filament overlaps and create 2D/3D ROIs” (`cluster-0259`) · 3n/2e
  - Takes 3D filament data, corrects merged non-branching filaments, and exports the resulting 3D ROIs as files. It converts filament segments and points into 3D line/point ROIs before writing them out.
- **2D ROI to filament label and mask images** — extends “RGB image with 2D ROI overlays” (`cluster-0323`) · 3n/2e
  - Takes 2D ROIs, converts them into filaments, and produces a filament label image and a solid filament mask image. The mask is greyscale and can be sized to a reference image.

#### machine-learning (5)

- **Cell segmentation label image** — extends “Train a Cellpose model from 2D image-label pairs” (`cluster-0050`) · 2n/1e
  - Segments cells in an image with Cellpose and outputs a cell label image (mask) converted to another image data type.
- **Train and apply a custom cell segmentation model** — extends “Train a Cellpose model from 2D image-label pairs” (`cluster-0050`) · 2n/1e
  - Takes annotated images, trains a Cellpose 2 segmentation model, then uses that model to segment new images into cell masks and optional probability or flow outputs.
- **Cell centroids from Cellpose segmentation** — extends “Project and correct a multi-dimensional image” (`cluster-0072`) · 4n/3e
  - Takes an image, segments cells with Cellpose, and computes the centroid of each segmented cell. It produces a point list of cell locations, using GPU processing for the centroid step.
- **Cellpose segmentation labels as 8-bit image** — extends “Clean and enhance images” (`cluster-0129`) · 2n/1e
  - Takes an image, runs Cellpose segmentation, and converts the resulting label image to an 8-bit grayscale image.
- **Cellpose 2D/3D ROI segmentation** — extends “Train and predict with a generative model” (`cluster-0134`) · 2n/1e
  - Takes input images and a pretrained Cellpose model, runs segmentation inference, and produces 2D or 3D ROIs by default, with optional labels, flows, and probabilities.

#### tracking (1)

- **Track filament networks as overlay** — extends “Segment and measure 3D ROIs from an image” (`cluster-0027`) · 2n/1e
  - Takes time-lapse images of filament networks, extracts and tracks individual filaments or network segments over time, and outputs the tracked filaments as an overlay.

#### tables (4)

- **Project file path metadata table** — extends “Split multichannel images into separate channels” (`cluster-0013`) · 2n/1e
  - Takes input data, adds project and file path information, and produces a flat table of all annotations for inspection or export.
- **Filtered parameter table** — extends “Merge multiple parameter definitions” (`cluster-0020`) · 2n/1e
  - Takes parameter values, converts them into a table, and keeps only rows that match a filter condition. Produces a filtered table.
- **Annotate table with ImageJ results** — extends “Export cleaned annotation table” (`cluster-0047`) · 3n/2e
  - It imports a data table from a ZIP archive and an ImageJ results table from CSV or XLSX, then adds matching values from the results table to the data table. The output is a single table enriched with ImageJ measurements.
- **Download JSON and extract table** — extends “Load JSON files and extract selected fields” (`cluster-0217`) · 4n/3e
  - Downloads a JSON file from a web resource, parses and enriches it, then extracts selected fields into a table.

#### statistics (10)

- **8-bit image intensity histogram** — extends “Color overlay of overlapping 3D ROIs” (`cluster-0028`) · 2n/1e
  - Takes an image, converts it to 8-bit grayscale, and computes an intensity histogram. Produces a table of intensity values and their counts.
- **Image intensity histogram** — extends “Color overlay of overlapping 3D ROIs” (`cluster-0028`) · 2n/1e
  - Takes an image, converts it to 16-bit, and computes a pixel intensity histogram. It produces a table of intensity values and their counts.
- **2D ROI image statistics** — extends “Find boundary regions of selected 2D ROIs” (`cluster-0068`) · 3n/2e
  - Takes a grayscale image, converts it to a binary mask, detects connected 2D particles as 2D ROIs, and computes image statistics for each ROI. It produces a per-ROI statistics table.
- **Average neighbor distance map** — extends “Segment objects and create 2D ROI ring masks” (`cluster-0093`) · 3n/2e
  - Takes an image and produces an image showing the average distance from each location to neighboring features. The calculation is run on the GPU and returned as a standard image.
- **Hue/saturation/brightness histogram** — extends “Plot pixel values from a masked image” (`cluster-0107`) · 2n/1e
  - Takes an image, converts it to hue/saturation/brightness color space, and computes a pixel-value histogram. It produces a table of histogram values and counts.
- **Grayscale image statistics** — extends “Export 2D ROI overlay images” (`cluster-0127`) · 2n/1e
  - Takes a 2D or 3D image, converts it to 8-bit grayscale, and extracts whole-image or masked intensity statistics. It produces a summary of brightness values such as mean, minimum, and maximum.
- **Background-corrected image histogram** — extends “Tile multiple images into a grid” (`cluster-0133`) · 2n/1e
  - Takes an image, removes a rolling-ball background estimate, and produces a histogram table of the resulting pixel intensities.
- **Background-corrected intensity histogram** — extends “Compute extended depth-of-focus projection from selected image planes” (`cluster-0152`) · 3n/2e
  - Takes a 2D image or image stack, removes background and smooths the image, then summarizes the resulting pixel intensity distribution. It produces a histogram table with intensity values and their counts.
- **Cell segmentation 2D ROI intensity statistics** — extends “Composite a resliced, color-mapped image” (`cluster-0171`) · 3n/3e
  - Takes an image, converts it to 8-bit, segments cells with Cellpose, and computes intensity statistics for each resulting 2D ROI. It produces per-cell intensity measures such as mean, min, and max, rather than reliable shape measurements.
- **Image statistics** — extends “Overlay 2D ROIs on an image” (`cluster-0256`) · 2n/1e
  - Takes an image, converts it to 32-bit grayscale, and computes basic intensity statistics for the whole image or a masked region.

#### plotting (1)

- **Parameter value histogram** — extends “Merge multiple parameter definitions” (`cluster-0020`) · 2n/1e
  - Takes parameter data, converts it into a table, and creates a histogram of a chosen column. This shows the distribution of parameter values.

#### export (41)

- **Export filtered annotated data as a table** — extends “Attach 3D ROI measurements to data” (`cluster-0005`) · 2n/1e
  - Takes data items with text annotations, keeps only the items that satisfy an annotation condition, and exports the selected items as a data table directory.
- **Export matched and unmatched data by annotation** — extends “Attach 3D ROI measurements to data” (`cluster-0005`) · 3n/2e
  - Takes annotated data, splits it into records that match an annotation condition and records that do not, and exports the two groups to separate files. The matched group is written as a data table and the unmatched group is written using the standard data exporter.
- **Export data to project folders** — extends “Split multichannel images into separate channels” (`cluster-0013`) · 3n/3e
  - Takes data and writes it to project-defined output folders. It derives the target folder from project path information and exports the data there.
- **Export data filtered by annotation** — extends “Colocalization within segmented cell 2D ROIs” (`cluster-0014`) · 2n/1e
  - Takes data items with text annotations, keeps only those whose annotation satisfies a condition, and exports the matching items. It produces the directory containing the exported files.
- **Export data filtered by annotation** — extends “Import and split microscopy images with color maps and metadata” (`cluster-0023`) · 3n/2e
  - Takes data items with annotations, sets or derives annotation values, keeps only items that satisfy an annotation condition, and exports the matching items to files.
- **Export data to metadata-defined paths** — extends “Import and split microscopy images with color maps and metadata” (`cluster-0023`) · 3n/3e
  - Takes data items with a metadata field containing a destination path, duplicates each item, converts the metadata value to a path, and exports the original data to that path. It produces files at the specified locations.
- **Export 8-bit image for Ilastik** — extends “Segment objects from images by thresholding and labeling” (`cluster-0031`) · 2n/1e
  - Takes an image, converts it to 8-bit grayscale, and exports it as an Ilastik HDF5 file. This produces an Ilastik-ready image for downstream segmentation or classification.
- **Export annotated data** — extends “Summarize overlapping 2D ROIs into a table” (`cluster-0048`) · 2n/1e
  - It takes data and a table, enriches each data item with matching table values, and writes the enriched data to files. The result is a directory of exported annotated data.
- **Export origin-labeled data tables** — extends “Filter data by label” (`cluster-0062`) · 2n/1e
  - Takes data records, adds a label identifying their origin, and exports them as data table files.
- **Export annotated table to Excel** — extends “Summarize and plot table data as an XY line chart” (`cluster-0079`) · 2n/1e
  - Takes a results table, adds attached column annotations as new columns, and exports the table to an Excel (.xlsx) file.
- **Export table to Excel** — extends “Tidy a results table” (`cluster-0112`) · 2n/1e
  - Takes a results table, adds table-level metadata, and writes it to an Excel spreadsheet.
- **Export image-annotated data table** — extends “Create masks from 2D ROIs in images” (`cluster-0115`) · 2n/1e
  - Takes a data table, adds annotations derived from an image, and exports the annotated table.
- **Export table with unique row labels** — extends “Track, filter, and annotate spots” (`cluster-0116`) · 2n/1e
  - Takes a data table, adds a unique index-based label to each row, and exports the resulting table as a data-table directory.
- **Export 3D ROIs updated from a table** — extends “3D ROI nearest-neighbor distance histogram” (`cluster-0121`) · 2n/1e
  - Takes a 3D ROI list and a matching table, assigns table values to ROI properties, and writes the updated 3D ROIs to ROI file(s).
- **Export filtered 3D ROIs** — extends “3D ROI nearest-neighbor distance histogram” (`cluster-0121`) · 2n/1e
  - Takes a 3D ROI list, keeps only the ROIs that match the filter criteria, and writes the matching 3D ROIs to one or more ROI files.
- **Export 3D masks as a 3D mesh file** — extends “Export 3D mask as a 3D mesh” (`cluster-0136`) · 3n/2e
  - Takes one or more 3D masks, converts them into surface meshes, combines them into a single 3D scene, and writes a Collada (.dae) file for viewing or downstream 3D analysis.
- **Export matched and unmatched annotation subsets** — extends “Track measurement summary” (`cluster-0138`) · 3n/2e
  - Takes annotated data, splits it into records that match an annotation condition and records that do not, and exports each subset to separate output files or folders.
- **Export filtered data to annotation-based file paths** — extends “Load and filter 2D ROI files” (`cluster-0139`) · 4n/4e
  - Takes annotated data, keeps only items that match a filter, and exports those items to file paths generated from an annotation. It produces exported files at the annotation-specified locations.
- **Export matched annotation data as table** — extends “Load and filter 2D ROI files” (`cluster-0139`) · 2n/1e
  - Takes annotated data, selects the items that match an annotation condition, and exports the selected items as a data table.
- **Export data items matching annotation criteria** — extends “Filter and visualize 2D ROIs” (`cluster-0142`) · 3n/2e
  - Takes data items, adds values from a table as annotations, keeps only items whose annotations satisfy a condition, and exports the matching items as data tables.
- **Export data subsets by table values** — extends “Filter and visualize 2D ROIs” (`cluster-0142`) · 4n/3e
  - Takes data and a table, annotates the data with table values, then separates matching and non-matching records and exports each subset to separate output files or folders.
- **Export data split by annotation** — extends “Read text from images and attach it to data” (`cluster-0177`) · 3n/2e
  - Takes annotated data, separates it into records that match an annotation condition and records that do not, then exports each group to separate output files or folders.
- **Export image for Ilastik** — extends “Measure pairwise distances between 3D ROIs” (`cluster-0184`) · 2n/1e
  - Takes an image, converts it to 32-bit grayscale, and exports it as an Ilastik HDF5 file for use in Ilastik.
- **Export matched and unmatched annotation subsets** — extends “Plot histograms of 2D ROI statistics” (`cluster-0200`) · 3n/2e
  - Takes annotated data, splits it by an annotation condition, and exports the matching subset as a table and the non-matching subset as raw files.
- **Export filtered annotated data** — extends “Pairwise 2D ROI statistics histogram” (`cluster-0202`) · 3n/2e
  - Takes annotated data items, keeps only those matching an annotation condition, and exports the selected items as a data table and as output files.
- **Export data table with source file paths** — extends “Normalize image intensity and attach metadata” (`cluster-0206`) · 2n/1e
  - Takes a data table, adds source file path information to each record, and exports the enriched table. The output is a data table directory that preserves the path context for downstream use.
- **Export data and data table** — extends “Train and predict image segmentation” (`cluster-0216`) · 3n/2e
  - Takes an input data set, branches it, and writes both a standard export and a data-table export to output directories.
- **Export annotated OME images to OME-ZARR** — extends “Load images from folders” (`cluster-0228`) · 2n/1e
  - Takes OME microscopy images, adds metadata annotations from their OME metadata, and exports them as OME-ZARR datasets. It produces a URI reference to the exported folder or ZIP file.
- **Export annotated OME-TIFF images** — extends “Load images from folders” (`cluster-0228`) · 3n/2e
  - Takes OME images, attaches metadata annotations and export settings, and writes them out as OME-TIFF files.
- **Export table with computed columns to Excel** — extends “Plot histogram of table measurements” (`cluster-0230`) · 2n/1e
  - Takes a results table, applies a per-row expression to create or update columns, and writes the resulting table to an Excel workbook.
- **Export data to a file path from a metadata field** — extends “Create blank image with computed label” (`cluster-0248`) · 4n/4e
  - Takes data, sets a single metadata field, and exports the data to a file path generated from that field. It sets an annotation, duplicates the data, converts the annotation to a path, and writes the data to that path.
- **Export annotated data table** — extends “Create a thresholded 2D image with statistics” (`cluster-0257`) · 3n/2e
  - Takes data records, adds or adjusts annotations derived from associated images, and exports the annotated records as a data table.
- **Export table with calculated columns to Excel** — extends “Plot histogram from category/value images” (`cluster-0262`) · 2n/1e
  - Takes a results table, adds or updates columns by evaluating an expression for each row, and exports the final table to an XLSX spreadsheet.
- **Export OMERO images as OME-TIFF** — extends “Upload images and result tables to OMERO” (`cluster-0274`) · 2n/1e
  - Retrieves selected OMERO images and saves them as OME-TIFF files, preserving pixel data and available metadata.
- **Export OMERO table data to Excel** — extends “Upload images and result tables to OMERO” (`cluster-0274`) · 2n/1e
  - Takes tables attached to an OMERO image and saves them as an Excel workbook. Multiple tables can be combined into a single multi-sheet file.
- **Export data table filtered by annotation** — extends “Extract an image tile” (`cluster-0279`) · 3n/2e
  - Takes data items, adds a single annotation, keeps only items whose annotation satisfies a condition, and exports the selected items as a data table.
- **Export data to a metadata-defined location** — extends “Extract an image tile” (`cluster-0279`) · 4n/4e
  - Takes data items, sets or reads a metadata field as the destination location, and writes the data there. It produces exported files whose names come from the final component of that location.
- **Export annotated results table to Excel** — extends “Count 2D ROIs into a summary table” (`cluster-0282`) · 2n/1e
  - Takes a results table, adds annotations from selected columns (merging values when they differ), and writes the table to an XLSX file.
- **Export annotated data with image metadata** — extends “Export 2D ROI overlay and mask images” (`cluster-0296`) · 3n/2e
  - Takes data, adds image-derived property and project path annotations, and exports the annotated data to files. The output is an exported dataset enriched with image metadata and project path information.
- **Convert OME-ZARR to OME-TIFF** — extends “Batch export OME-ZARR images from folders” (`cluster-0318`) · 2n/1e
  - Takes an OME-ZARR image URI, loads the image data, and writes it as an OME-TIFF file. This is useful for converting OME-ZARR data into a widely compatible OME-TIFF format.
- **Export ZARR dataset inventory to Excel** — extends “Batch export OME-ZARR images from folders” (`cluster-0318`) · 2n/1e
  - Takes a ZARR URI, lists the available datasets and groups, and exports that inventory as an XLSX spreadsheet.

#### misc (1)

- **Create annotation folders** — extends “List files in folders and extract name-based metadata” (`cluster-0102`) · 2n/1e
  - Takes data with annotations that specify locations, converts each annotation into a filesystem path, and creates the corresponding directories, skipping any that already exist.

### what-next rejected by the gate (false-negative check)

- “Color overlapping 2D ROIs by image statistics and export table” — extends “Filter 2D ROIs by overlap” · 2n — gate=noise
- “Segment cells/nuclei and export label image for Ilastik” — extends “Import, split, and apply false colors to microscopy images” · 2n — gate=noise
- “Split annotated data by rule and export a table” — extends “Add computed annotations to data” · 2n — gate=noise
- “Count 2D ROIs and annotate data” — extends “2D ROI overlap counts and overlays” · 3n — gate=noise
- “Cellpose 3.x 2D ROI measurements” — extends “Segment images with Cellpose and render 2D ROIs” · 2n — gate=noise
- “Compute 2D vesselness and annotate data with image properties” — extends “Import microscopy images and convert to 32-bit grayscale” · 2n — gate=noise
- “Filter data by 2D vesselness image properties” — extends “Import microscopy images and convert to 32-bit grayscale” · 3n — gate=noise
- “Convert rendered track image to HSB” — extends “Visualize tracked spots and trajectories” · 2n — gate=noise
- “Add file path tags and separate matches” — extends “Filter three inputs by annotation” · 2n — gate=noise
- “Add unique IDs and filter by tag” — extends “Filter three inputs by annotation” · 2n — gate=noise
- “Prepare imported 2D images for publication: subtract background, add scale bar, convert to RGB” — extends “Batch export image files” · 3n — gate=noise
- “Segment cells with Cellpose and convert labels to 8-bit” — extends “Project and correct a multi-dimensional image” · 2n — gate=noise
- “Annotate data with HSB pixel histogram” — extends “Plot pixel values from a masked image” · 3n — gate=noise
- “Plot table annotations from merged columns” — extends “Merge two tables after column cleanup” · 2n — gate=noise
- “Connect filament vertices, measure components, and annotate records” — extends “Create filament mask from 3D time-lapse” · 3n — gate=noise
- “...” — extends “Import and scale 2D images” · 3n — gate=noise
- “Label connected components in a 3D mask and export HDF5” — extends “Export 3D mask as a 3D mesh” · 2n — gate=noise
- “Annotate data with pixel histogram measurements and image properties” — extends “2D ROI measurement histogram montage” · 3n — gate=noise
- “Colorized, cleaned, and inverted RGB image” — extends “Quick RGB preview of 2D ROIs on images” · 3n — gate=noise
- “Measure corrected image and annotate data” — extends “Background-subtracted image stack” · 2n — gate=noise
- “Convert Cellpose 2D ROIs to filament labels” — extends “Denoise and segment cells into 2D ROIs” · 3n — gate=noise
- “Assign unique IDs and split bead and sample data by label” — extends “Import images and attach metadata” · 2n — gate=noise
- “Annotated JSON to text” — extends “Load JSON files and extract selected fields” · 4n — gate=noise
- “Segment cells and measure 2D ROI statistics” — extends “Rotate an image” · 2n — gate=noise
- “Add scale bar and export 16-bit montage” — extends “Create image montages for visualization” · 2n — gate=noise

### what-next rejected by fail-closed validation (the grounding gate)

52 model proposals that named a real slot wrong, used an
incompatible type, or cited a node id that does not exist — dropped
before the gate ever saw them (fail-closed by design).

- 15× unverified slot names
- 9× type mismatch imagej-roi !-> jipipe:data on cellpose-2 -> annotate-data-with-image-properties
- 3× import-imagej-results-table-from-file has no output slot named imagej-results-table
- 2× filaments-convert-to-mask has no input slot named Filaments
- 2× filaments-skeleton-to-filaments-3d has no output slot named filaments
- 2× type mismatch imagej-imgplus-greyscale-32f !-> jipipe:data on feature-vesselness-meijering2d -> annotate-data-with-image-properties
- 2× unknown node id cli:plugin:net.haesleinhuepf.clij2.plugins.AutomaticThreshold
- 1× type mismatch imagej-roi !-> jipipe:data on ij1-analyze-find-particles2d -> annotate-data-with-table-values
- 1× type mismatch imagej-roi !-> jipipe:data on cellpose-segmentation-inference-3.x -> annotate-data-with-image-properties
- 1× type mismatch imagej-results-table !-> jipipe:data on ij1-analyze-find-particles2d -> export-data-table
- 1× type mismatch imagej-results-table !-> jipipe:data on ij1-analyze-find-particles2d -> annotate-data-with-image-properties
- 1× annotate-data-with-image-properties has no input slot named data
- 1× type mismatch imagej-imgplus !-> jipipe:data on clij-pull-from-gpu -> annotate-data-with-table-values
- 1× type mismatch imagej-imgplus-2d-greyscale-32f !-> imagej-imgplus-greyscale on ij1-convert-table-column-to-image -> ij1-apply-lut
- 1× duplicate node ids
- 1× type mismatch imagej-imgplus !-> jipipe:data on convert-imagej-image -> annotate-data-with-image-properties
- 1× type mismatch imagej-results-table !-> jipipe:data on ij1-analyze-image-statistics -> export-data-table
- 1× type mismatch imagej-roi !-> jipipe:data on ij-multi-template-matching -> annotate-data-with-table-values
- 1× unknown node id annotate-split-by-annotation-if-else
- 1× annotate-data-with-image-properties has no input slot named jipipe:data
- 1× import-imagej-imgplus-from-file has no output slot named imagej-imgplus
- 1× type mismatch imagej-imgplus-greyscale-8u !-> jipipe:data on ij-convert-image-to-8-bit -> annotate-data-with-image-properties
- 1× type mismatch imagej-imgplus-greyscale-16u !-> jipipe:data on ij-convert-image-to-16-bit -> annotate-data-with-image-properties
- 1× clij:plugin:net.haesleinhuepf.clij2.plugins.BinaryFillHoles has no input slot named Input

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

