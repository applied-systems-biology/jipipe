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
| **(C) shipped** | 324 | **267** |
| (C) rejected | | 57 ({'gate_noise': 19, 'gate_workflow_plumbing': 38}) |

## The (C) naming-review fixes (measured)

| issue (from the (C) review) | before (v1) | after (this run) |
|---|---|---|
| bare "ROI" names (no 2D/3D) | 57 | **0** |
| JIPipe-internal jargon in names | 8 | **0** |
| descriptions with no in/out anchor | 197 | **10** |
| shipped count | 270 | 267 |

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
| 6 | 16 | 54 |
| 5 | 11 | 65 |
| 4 | 39 | 104 |
| 3 | 26 | 130 |
| 2 | 73 | 203 |
| 1 | 64 | 267 |

**The (D) curation call (your decision 1):** the plan specifies no support
floor (the §1 rule is name-derivability, which the gate enforces). If you
want a tighter library:

| option | ships |
|---|---|
| no floor (plan literal) | 267 |
| support ≥ 2 | 203 |
| support ≥ 3 | 130 |
| support ≥ 5 | 65 |

## Shipped by function

| function | shipped |
|---|---|
| data | 8 |
| annotations | 6 |
| images | 89 |
| rois | 69 |
| filaments | 14 |
| machine-learning | 21 |
| tracking | 7 |
| tables | 6 |
| statistics | 15 |
| plotting | 15 |
| export | 16 |
| misc | 1 |

## (D) hard blocker: patterns on unregistered node ids

**14 shipped patterns reference a node id not in the 6.0.0 launcher
classpath** (the corpus predates 6.0.0; some IDs were renamed, some live in
optional plugins). The launcher guard `everyPatternGraphDeserializes` asserts
every node id resolves, so these FAIL unless remapped or excluded in (D).
See `dag-stale-id-plan.json` for the remap/exclude list.

**Renamable** (all stale ids have a clean current successor): 3
- `cluster-0133` support 3 'Create tiled image previews from files' → ['ij1-transform-tile']
- `cluster-0318` support 1 'Export OME-ZARR images from folders' → ['import-ome-zarr-from-directory-as-imgplus']
- `cluster-0242` support 1 'Detect lines in 2D images' → ['ij1-transform-tile']

**Exclude or plugin-gate** (optional-plugin nodes absent from the default launcher): 11
- `cluster-0058` support 5 'Generate 2D extended-depth-of-focus projections from 3D image stacks' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0072` support 4 'Create corrected 2D time-series projections' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0091` support 4 'Import images and compute extended depth-of-focus projections' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0134` support 3 'Train and predict with a generative model' → ['create-jipipe2ai-generation-model', 'jipipe2ai-predict-generation', 'jipipe2ai-train-generation-model']
- `cluster-0140` support 2 'Segment objects from images using markers' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0152` support 2 'Compute extended depth-of-focus projection from selected image planes' → ['clij:plugin:net.haesleinhuepf.clijx.clij2wrappers.ExtendedDepthOfFocusSobelProjection']
- `cluster-0169` support 2 'Create filament masks from 3D image stacks' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0186` support 2 'Create a colored depth-of-focus projection overlay' → ['clij:plugin:net.haesleinhuepf.clij2.plugins.ExtendedDepthOfFocusSobelProjection']
- `cluster-0216` support 2 'Train and predict image segmentation' → ['create-jipipe2ai-segmentation-model', 'jipipe2ai-predict-image', 'jipipe2ai-train-image-model']
- `cluster-0153` support 2 'Detect 2D ROIs from image stacks' → ['clij:plugin:net.haesleinhuepf.clijx.clij2wrappers.ExtendedDepthOfFocusSobelProjection']
- `cluster-0254` support 1 'Train image model from labeled images' → ['deep-learning-train-image-model']

Shippable after remap + exclude: **256**.

## All shipped patterns (for review)

Grouped by function, then support (descending). `cluster-XXXX` traces back
into `dag-clusters.json` / the corpus.

### data (8)

- **List files and subfolders in a folder** · support 92 · group-node · `cluster-0001`
  - 9 nodes / 9 edges — Group input -> Annotate with file/directory name -> List subfolders -> Annotate with file/directory name -> Annotate with file/directory name -> List files -…
  - Takes an input folder and produces annotated paths for its subfolders and files by listing subfolders, listing files, and annotating each path with its name. It can also filter the resulting file paths and expose subfolder paths as a separate output.
- **Import images from subfolders with names and properties** · support 14 · geometric-cluster · `cluster-0015`
  - 8 nodes / 7 edges — Folder list -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -> Bio-Formats importe…
  - Takes a root folder, lists its subfolders and files, filters the file paths, and imports the selected images with Bio-Formats. It produces image data annotated with the subfolder/file names and image properties.
- **Load images from files with filename labels** · support 10 · geometric-cluster · `cluster-0025`
  - 3 nodes / 2 edges — File list -> Annotate with file/directory name -> Import image
  - Takes a list of file paths, adds a filename label to each path, and imports each file as an image. It produces image data that is labeled with the source file name.
- **Load images from folders** · support 4 · compartment · `cluster-0100`
  - 4 nodes / 3 edges — Folder list -> Annotate with file/directory name -> List files -> Import image
  - Takes folder paths, lists the files in each folder, and imports each file as an image, attaching the folder name as a label to the resulting images.
- **Load images from folders** · support 4 · geometric-cluster · `cluster-0101`
  - 5 nodes / 4 edges — Folder list -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Import image
  - Takes folder paths, lists the files in each folder, and imports each file as an image, attaching folder and file name annotations to the resulting image data.
- **List files in folders and add name metadata** · support 4 · geometric-cluster · `cluster-0102`
  - 5 nodes / 4 edges — Folder list -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Set/Edit annotations
  - Takes folder paths, lists all files in each folder, and sets annotations from the folder and file names, producing a file list with source folder and file name metadata.
- **Load JSON metadata for files** · support 2 · geometric-cluster · `cluster-0217`
  - 3 nodes / 2 edges — File list -> Import JSON -> Annotate with JSON values
  - Takes a list of file paths, imports the corresponding JSON files, and annotates each file entry with selected JSON values. The result is a file list enriched with metadata fields extracted from the JSON files.
- **Load images from folders** · support 1 · canvas-note-section · `cluster-0228`
  - 3 nodes / 2 edges — Folder list -> List files -> Bio-Formats importer
  - Takes one or more folder paths, lists the files in each folder, and imports the image files with Bio-Formats to produce image data.

### annotations (6)

- **Extract OME image metadata as annotations** · support 12 · group-node · `cluster-0019`
  - 5 nodes / 4 edges — Group input -> Extract OME XML -> Prettify XML -> Annotate with XML values -> Group output
  - Takes OME images, extracts and formats their OME-XML metadata, and attaches selected XPath values as annotations. Produces the input data enriched with metadata fields.
- **Create a clean table of annotations** · support 8 · geometric-cluster · `cluster-0030`
  - 5 nodes / 4 edges — Annotate data with table values -> Convert to annotation table -> Remove table column -> Merge table columns (simple) -> Rename table column
  - Takes data rows and a source table, attaches selected table values to the data as annotations, and converts those annotations into a flat table. It then removes unwanted columns, merges the table with another table, and renames columns to produce a clean annotation table.
- **Add computed annotations to data** · support 8 · canvas-note-section · `cluster-0032`
  - 4 nodes / 3 edges — Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Merge annotations
  - Takes a data item, sets several annotations using expressions, and merges those annotations back onto the same data.
- **Select and filter three inputs by annotation** · support 5 · group-node · `cluster-0061`
  - 5 nodes / 6 edges — Group input -> Split & filter by annotation -> Split & filter by annotation -> Split & filter by annotation -> Group output
  - Takes three input data streams and applies a split/filter-by-annotation step to each. It produces three corresponding output streams that are separated or filtered according to the chosen annotation value.
- **Read text values from images and attach them to data** · support 2 · canvas-note-section · `cluster-0177`
  - 5 nodes / 4 edges — Crop 2D image -> Find text (Tesseract OCR) -> Filter table rows -> Filter tables -> Annotate data with table values
  - Reads text values from images and attaches them to the corresponding data. It crops a 2D image, runs Tesseract OCR per plane, filters the OCR result table, and annotates the input data with the selected text values.
- **Import images and add computed annotations** · support 2 · geometric-cluster · `cluster-0207`
  - 3 nodes / 2 edges — Bio-Formats importer -> Set annotation (expression) -> Set annotation (expression)
  - Takes image files, imports them via Bio-Formats, and produces images annotated with two expression-computed values.

### images (89)

- **Import, split, and color-map multichannel images** · support 26 · compartment · `cluster-0007`
  - 9 nodes / 7 edges — Bio-Formats importer -> Project user path -> Reorder dimensions -> Annotate with image properties -> Set/Edit annotations -> Filter by annotation -> Split ch…
  - Takes image files, imports them via Bio-Formats, reorders their dimensions, and filters them using image-property annotations. It then splits multichannel images into grayscale channels and applies predefined color maps to the selected channels.
- **Split multichannel images into single-channel images** · support 15 · group-node · `cluster-0013`
  - 12 nodes / 28 edges — Group input -> Annotate with image properties -> Filter by annotation -> Filter by annotation -> Filter by annotation -> Filter by annotation -> Split channe…
  - Takes multichannel images and produces separate single-channel grayscale images, automatically handling different channel counts by annotating image properties, filtering on them, and splitting channels.
- **Create cleaned 2D ROI masks from images** · support 13 · compartment · `cluster-0017`
  - 11 nodes / 11 edges — Folder list -> List subfolders -> Annotate with file/directory name -> List files -> List files -> Import ROI -> Annotate with file/directory name -> Import …
  - Takes folders containing image files and 2D ROI files, imports and crops each image, converts the 2D ROIs to masks using the cropped image as reference, and applies a morphological operation to clean the masks. It produces processed 2D mask images ready for downstream analysis.
- **Imported, color-mapped, split, and annotated image stacks** · support 10 · compartment · `cluster-0023`
  - 10 nodes / 5 edges — Set LUT (color map) -> Bio-Formats importer -> Set LUT (color map) -> Set LUT (color map) -> Project user path -> Reduce & split hyperstack (Expression) -> A…
  - Takes image files, imports them with Bio-Formats, applies predefined color maps, and splits them into custom stacks using Z/channel/frame expressions. It attaches image properties as annotations and edits those annotations, producing imported, color-mapped, split, and annotated image stacks.
- **Segment nuclei in images** · support 8 · canvas-note-section · `cluster-0031`
  - 9 nodes / 8 edges — Gaussian blur 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Median filter 2D -> Morphological hole filling 2D -> Connected components labeling 3D…
  - Takes 8-bit images, smooths and auto-thresholds them, cleans the binary mask, labels connected components in 3D, filters labels, and produces a binary mask of nuclei.
- **Load and denoise images from subfolders** · support 7 · compartment · `cluster-0035`
  - 10 nodes / 8 edges — Folder list -> Project user path -> List subfolders -> Annotate with file/directory name -> List files -> Annotate with file/directory name -> Filter paths -…
  - Takes a configured project directory, lists its subfolders and image files, attaches folder and file names as metadata, filters the file list, imports the images, and applies a 2D median filter to each image/channel/slice. Produces denoised images with folder/file metadata.
- **Denoise and threshold 8-bit images** · support 6 · geometric-cluster · `cluster-0039`
  - 5 nodes / 4 edges — File list -> Annotate with file/directory name -> Import image -> Median filter 2D -> Auto threshold 2D (8-bit)
  - Takes image file paths, imports each image, applies a 2D median filter to reduce noise, and auto-thresholds each 8-bit slice to produce thresholded images.
- **Create a 2D image from a pixel-value table** · support 6 · geometric-cluster · `cluster-0041`
  - 6 nodes / 5 edges — File list -> Import results table -> Rename table column -> Convert table to image -> Tile image 2D -> Un-Tile image 2D
  - Takes file paths to CSV or XLSX results tables containing pixel information, renames a column, and converts the table into a 2D image. It then splits the image into 2D tiles and reassembles them to produce the final image.
- **Create particle masks from images** · support 6 · canvas-note-section · `cluster-0042`
  - 7 nodes / 6 edges — Median filter 2D -> Auto threshold 2D (8-bit) -> Median filter 2D -> Orientation features 2D (OrientationJ) -> Auto threshold 2D (8-bit) -> Morphological ope…
  - Takes a 2D image and produces a cleaned binary mask of particles by median filtering, automatic thresholding, extracting orientation energy, re-thresholding, and applying morphological operations and hole filling.
- **Create color composite images with scale bars** · support 6 · canvas-note-section · `cluster-0045`
  - 9 nodes / 10 edges — Split channels -> Set LUT (color map) -> Set LUT (color map) -> Set LUT (color map) -> Blend images -> Copy physical dimensions -> Draw scale bar -> Export i…
  - Takes a multichannel image, splits it into individual channels, assigns a color map to each channel, and blends them into a single RGB image. It copies physical pixel sizes, draws a scale bar, and exports the resulting image.
- **Import images and convert to 32-bit** · support 6 · geometric-cluster · `cluster-0049`
  - 4 nodes / 3 edges — Set/Edit annotations -> Set annotation (expression) -> Bio-Formats importer -> Convert image to 32-bit
  - Takes a data source of image file paths, sets or updates annotations on the entries, imports the images via Bio-Formats, and outputs 32-bit images.
- **Detect tubular structures in 2D images** · support 6 · geometric-cluster · `cluster-0054`
  - 6 nodes / 5 edges — Find or subtract background 2D -> Auto threshold 2D (8-bit) -> Median filter 2D -> Frangi vesselness -> Divide by maximum -> Auto threshold 2D (8-bit)
  - Takes 2D image data and produces a binary mask highlighting tubular or vessel-like structures. It applies background subtraction, thresholding, median filtering, Frangi vesselness, normalization, and a final threshold to isolate line-shaped features.
- **Render tracked spots and tracks as RGB images** · support 5 · compartment · `cluster-0056`
  - 3 nodes / 1 edges — Convert spots to RGB -> Render track scheme -> Convert tracks to RGB
  - Takes tracking results (spots and tracks) and produces RGB images showing the spots, the track scheme, and the connected tracks.
- **Generate 2D extended-depth-of-focus projections from 3D image stacks** ⚠ unregistered id · support 5 · compartment · `cluster-0058`
  - 7 nodes / 5 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumi…
  - Takes an imported image hyperstack, splits it into 3D volumes, computes an extended depth-of-focus Sobel projection for each volume, applies 2D illumination correction, and merges the corrected 2D projections into a 2D hyperstack.
- **Convert images to common file formats** · support 5 · geometric-cluster · `cluster-0065`
  - 3 nodes / 2 edges — File list -> Import image -> Export image
  - Takes a list of image file paths, loads each image, and writes it out as a standard image format such as PNG, JPEG, BMP, AVI, or TIFF.
- **Denoise imported images with a 2D median filter** · support 4 · compartment · `cluster-0067`
  - 3 nodes / 2 edges — File list -> Import image -> Median filter 2D
  - Takes a list of image file paths, imports each image, and applies a 2D median filter to each channel or slice. It produces denoised images suitable for downstream analysis.
- **Remove 3D ROI filament mask from inverted image** · support 4 · canvas-note-section · `cluster-0070`
  - 11 nodes / 13 edges — Invert pixel colors/values -> Morphological operation 2D -> Set to color (RGB) -> Set to value (grayscale) -> Euclidean distance transform 2D -> Set filament…
  - Takes an image and filaments; it inverts the image, applies a 2D morphological operation, computes a 2D Euclidean distance transform, uses that transform to set filament vertex radii, and converts the filaments into a 3D ROI. It then subtracts that 3D ROI from the inverted image to produce an image with filament pixels removed, leaving non-filament structures such as nodules.
- **Create corrected 2D time-series projections** ⚠ unregistered id · support 4 · group-node · `cluster-0072`
  - 7 nodes / 6 edges — Group input -> Annotate with image properties -> Split by dimension -> CLIJ2 Extended Depth Of Focus Sobel Projection -> Illumination correction 2D -> Merge …
  - Takes a multi-dimensional image stack, splits it by time, projects each time point to a 2D extended depth-of-focus image, applies 2D illumination correction, and merges the frames back into a 2D time-series stack.
- **Colorize images and overlay 3D ROI masks** · support 4 · canvas-note-section · `cluster-0083`
  - 10 nodes / 11 edges — Set LUT (color map) -> Set LUT (color map) -> Convert filaments to RGB -> Convert filaments to mask -> Convert filaments to mask -> Blend images -> Blend ima…
  - Takes images and filament data, applies color maps, converts filaments into 3D ROI masks, and blends them into RGB images for visualization.
- **Normalize and combine labeled images** · support 4 · geometric-cluster · `cluster-0085`
  - 9 nodes / 10 edges — Split & filter by annotation -> Filter by annotation (If else) -> Filter by annotation (If else) -> Divide by maximum -> Divide by maximum -> Divide by maxim…
  - Takes image data with categorical labels and separates it into branches by label. It normalizes each branch by its global maximum and applies 2D image arithmetic to produce combined or transformed normalized images.
- **Import images and compute extended depth-of-focus projections** ⚠ unregistered id · support 4 · compartment · `cluster-0091`
  - 7 nodes / 4 edges — Bio-Formats importer -> Custom extended depth of focus (Z/C/T) -> Project user path -> Annotate with image properties -> Set/Edit annotations -> Set LUT (col…
  - Takes image files imported via Bio-Formats, attaches image-property metadata and edited annotations, sets a display color map, and outputs an extended depth-of-focus projection that selects the sharpest pixel across Z/C/T.
- **Export image with rings around labeled objects** · support 4 · canvas-note-section · `cluster-0092`
  - 6 nodes / 5 edges — Get label boundaries -> Dilate labels -> Blend images -> Adjust displayed contrast -> Render to RGB -> Export image
  - Takes an image and a label image and produces an exported RGB image with a ring around each labeled object. It creates a label-boundary mask, dilates it, blends it with the image, adjusts display contrast, renders to RGB, and exports the result.
- **Create a binary mask from an image** · support 4 · canvas-note-section · `cluster-0095`
  - 4 nodes / 3 edges — Divide by maximum -> CLIJ2 Gaussian Blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D
  - Takes a greyscale image and produces a binary mask by normalizing intensities, smoothing, auto-thresholding, and filling holes.
- **Create binary masks from 8-bit images** · support 4 · canvas-note-section · `cluster-0098`
  - 5 nodes / 4 edges — Local auto threshold 2D (Bernsen, 8-bit) -> Median filter 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Set LUT (two colors)
  - Takes 8-bit 2D images and applies local thresholding, median filtering, Gaussian smoothing, and global auto-thresholding to produce a binary mask. It then assigns a two-color display LUT for visualization.
- **Fill holes in a 2D binary mask** · support 3 · group-node · `cluster-0110`
  - 5 nodes / 4 edges — Group input -> Morphological operation 2D -> Morphological hole filling 2D -> Morphological operation 2D -> Group output
  - Takes a 2D binary mask, dilates it, fills enclosed holes, then erodes it, producing a hole-filled mask with approximately restored boundaries.
- **Preprocess and threshold 2D image slices** · support 3 · canvas-note-section · `cluster-0117`
  - 11 nodes / 10 edges — Split by dimension -> Set/Edit annotations -> Set/Edit annotations -> Set LUT (color map) -> Find or subtract background 2D -> Histogram-based contrast enhan…
  - Takes a multi-dimensional image, splits it into 2D slices, sets annotations and a color map, then applies background subtraction, contrast enhancement, thresholding, and median/Gaussian smoothing before merging the slices back into a stack. The result is a cleaned, thresholded 2D image stack ready for downstream analysis.
- **Segment and label objects in images** · support 3 · canvas-note-section · `cluster-0118`
  - 6 nodes / 5 edges — Find or subtract background 2D -> Set LUT (color map) -> Morphological Feature Contrast (MFC) 2D -> Median filter 2D -> Auto threshold 2D (8-bit) -> Connecte…
  - Takes an image, subtracts background, sets a display color map, enhances morphological feature contrast, median-filters, auto-thresholds, and outputs a connected-component label image, with 3D components when 3D data is supplied.
- **Label connected components in 3D images** · support 3 · canvas-note-section · `cluster-0119`
  - 11 nodes / 10 edges — Divide by maximum -> Divide by maximum -> Split stack into 2D -> Z-Project -> Fast image arithmetics 2D -> Merge 2D slices into hyperstack -> Median filter 2…
  - Takes a 3D image stack, normalizes it, combines it with a Z-projection, then thresholds and median-filters the result to produce a 3D label image where each connected object has a unique value.
- **Preprocess imported images** · support 3 · compartment · `cluster-0125`
  - 7 nodes / 3 edges — Set LUT (color map) -> Set LUT (color map) -> Set/Edit annotations -> Folder list -> Bio-Formats importer -> Histogram-based contrast enhancer -> Convert ima…
  - Takes a folder of image files, imports them with Bio-Formats, updates annotations, converts them to 32-bit, enhances contrast, and applies a color map for display.
- **Render and export 2D ROIs as RGB images** · support 3 · canvas-note-section · `cluster-0127`
  - 9 nodes / 8 edges — Merge 2D ROI lists -> Set 2D ROI properties -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Render to RGB -> Export image -> Me…
  - Takes 2D ROI lists, merges and sets their properties, converts them to RGB mask images, and exports the resulting images; it can also merge multiple 2D slices into a hyperstack before export.
- **Clean and enhance images** · support 3 · geometric-cluster · `cluster-0129`
  - 6 nodes / 3 edges — Remove outliers 2D -> Find or subtract background 2D -> Legacy image calculator 2D -> Find or subtract background 2D -> Median filter 3D -> Histogram-based c…
  - Takes image data, removes outliers and background, smooths with a 3D median filter, and enhances contrast, producing cleaned and contrast-adjusted images.
- **Load images and subtract 2D background** · support 3 · geometric-cluster · `cluster-0131`
  - 3 nodes / 2 edges — File list -> Import image -> Find or subtract background 2D
  - Takes image file paths, imports each image, and outputs background-subtracted images by applying a 2D rolling-ball or paraboloid background filter to each slice.
- **Load and scale 2D images** · support 3 · geometric-cluster · `cluster-0132`
  - 3 nodes / 2 edges — File list -> Import image -> Scale 2D image
  - Takes image file paths, imports each file as an image, and applies 2D scaling to each image or 2D slice, producing rescaled images.
- **Create tiled image previews from files** ⚠ unregistered id · support 3 · geometric-cluster · `cluster-0133`
  - 3 nodes / 2 edges — File list -> Import image -> Tile image
  - Takes a list of image file paths, imports each image, and tiles each imported image into a grid preview.
- **Segment objects from images using markers** ⚠ unregistered id · support 2 · geometric-cluster · `cluster-0140`
  - 12 nodes / 12 edges — CLIJ2 Extended Depth Of Focus Sobel Projection: Green -> Filter by annotation -> Filter by annotation -> Histogram-based contrast enhancer -> CLIJ2 Extended …
  - Takes image data and annotation markers, computes red and green extended-depth-of-focus edge projections, enhances contrast, thresholds and median-filters the 2D projections, then applies seeded watershed to produce labeled objects.
- **Create colored images from 2D ROIs** · support 2 · canvas-note-section · `cluster-0145`
  - 3 nodes / 2 edges — Annotate with file/directory name -> Import ROI -> Set to color (RGB)
  - Takes image paths and matching 2D ROI files, imports the ROIs, and applies a specified color to the image using those ROIs, producing a colored image that visualizes manual annotations.
- **Create thresholded image overlay** · support 2 · canvas-note-section · `cluster-0146`
  - 6 nodes / 5 edges — Auto threshold 2D (8-bit) -> Equalize hyperstack dimensions (maximum) -> Median filter 2D -> Equalize hyperstack dimensions (maximum) -> Set LUT (color map) …
  - Takes 8-bit image(s), auto-thresholds and median-filters them into a clean mask, aligns the mask and original image dimensions, assigns a color map to the mask, and blends the mask over the original. Produces a colorized threshold overlay image.
- **Local thresholding with mask overlay** · support 2 · canvas-note-section · `cluster-0147`
  - 10 nodes / 9 edges — Enhance local contrast (CLAHE) -> Equalize hyperstack dimensions (maximum) -> Find or subtract background 2D -> Divide by maximum -> Gaussian blur 2D -> Morp…
  - Takes 2D image slices or stacks and produces a locally thresholded mask overlaid on the original image. It applies CLAHE, dimension equalization, background subtraction, normalization, Gaussian blur, MFC, and Phansalkar local thresholding, then colorizes the threshold mask and blends it with the raw image.
- **Preprocess and register 2D images** · support 2 · geometric-cluster · `cluster-0148`
  - 10 nodes / 10 edges — Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Filter by annotation (If else) -> Bio-Formats importer -> Annota…
  - Takes a selected list of image files, imports them with Bio-Formats, adds image properties, applies median filtering and 2D background subtraction, reorders dimensions, and registers the resulting 2D images to a reference.
- **Compute extended depth-of-focus projection from selected image planes** ⚠ unregistered id · support 2 · canvas-note-section · `cluster-0152`
  - 3 nodes / 2 edges — Reduce & split hyperstack (Expression) -> Reorder dimensions -> Cli Jx Extended Depth Of Focus Sobel Projection
  - Takes a multi-plane image, selects/splits planes using expressions and reorders dimensions, then applies an extended depth-of-focus Sobel projection to produce a projected image.
- **Create a contrast-enhanced blended image** · support 2 · compartment · `cluster-0158`
  - 3 nodes / 1 edges — Convert image to 8-bit -> Histogram-based contrast enhancer -> Blend images
  - Takes images, converts them to 8-bit, applies histogram-based contrast enhancement, and blends the enhanced image with another image/channel to produce a combined 8-bit image.
- **Import images from folders and convert to 32-bit** · support 2 · compartment · `cluster-0164`
  - 9 nodes / 7 edges — Folder list -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -> Set/Edit annotations -…
  - Takes folder paths, adds annotations to each path, imports the images with Bio-Formats, and produces 32-bit images.
- **Overlay 3D ROI mask on an image** · support 2 · canvas-note-section · `cluster-0168`
  - 8 nodes / 8 edges — Filter IJ3D ROI by statistics -> Divide by maximum -> Set LUT (color map) -> IJ3D ROI to mask -> Divide by maximum -> Set LUT (color map) -> Blend images -> …
  - Takes 3D ROIs and an image, filters the 3D ROIs by statistics, converts them to a mask, normalizes the image and mask, applies color maps, and blends them into a single overlay image.
- **Create filament masks from 3D image stacks** ⚠ unregistered id · support 2 · canvas-note-section · `cluster-0169`
  - 7 nodes / 6 edges — Reduce & split hyperstack (Expression) -> Extended DoF Sobel -> Set/Edit annotations -> Merge 2D slices into hyperstack -> Custom auto threshold 2D (16-bit) …
  - Takes a 3D image stack, projects it into 2D slices using an extended depth-of-focus Sobel projection, annotates and reassembles those slices, then thresholds, blurs, and applies morphology to produce a cleaned binary mask of filament-like structures.
- **Create a resliced, colorized image overlay** · support 2 · geometric-cluster · `cluster-0171`
  - 3 nodes / 2 edges — Reslice -> Set LUT (color map) -> Blend images
  - Takes an image, reslices it along a new Z axis, applies a predefined color map, and feeds the result as the top layer into an image blend, producing an overlaid/blended image.
- **Quick color preview with 2D ROI overlay** · support 2 · canvas-note-section · `cluster-0173`
  - 5 nodes / 5 edges — Convert 2D ROI to RGB -> Set LUT (color map) -> Set LUT (color map) -> Blend images -> Blend images
  - Takes images and a 2D ROI list, applies color maps to the images, converts the 2D ROIs into an RGB mask, and blends them into a single color preview image.
- **Create central Z substack and Z projection** · support 2 · group-node · `cluster-0181`
  - 10 nodes / 10 edges — Group input -> Annotate with image properties -> Set annotation (expression) -> Unique annotation -> Set annotation (expression) -> Unique annotation -> Set …
  - Takes an image with a Z axis and produces a smaller Z substack centered on the middle layer plus a Z projection. It annotates the image with its properties, uses expressions to select the central Z slices, splits out that substack, and projects the Z axis.
- **Background-subtracted image stack** · support 2 · canvas-note-section · `cluster-0183`
  - 6 nodes / 7 edges — Run ImageJ Macro -> Illumination correction 2D -> Z-Project -> Equalize hyperstack dimensions (referenced) -> Fast image arithmetics 2D -> Illumination corre…
  - Produces a background-subtracted image stack from an input image stack by normalizing illumination, estimating a background from a Z-projection, matching dimensions, and applying image arithmetic. A final illumination correction normalizes the output.
- **Create a colored depth-of-focus projection overlay** ⚠ unregistered id · support 2 · geometric-cluster · `cluster-0186`
  - 3 nodes / 2 edges — CLIJ2 Extended Depth Of Focus Sobel Projection -> Set LUT (color map) -> Blend images
  - Takes an image stack, computes an extended depth-of-focus Sobel projection, applies a color map, and blends the colored projection with another image to produce an overlay.
- **Load images from folders and merge channels to RGB** · support 2 · geometric-cluster · `cluster-0187`
  - 3 nodes / 1 edges — Folder list -> Import data table (directory) -> Combine channel slices to RGB
  - Takes folder paths, imports image data from each folder's data-table directory, and merges three channel slices into a single RGB image.
- **Preprocess 2D images and compute orientation features** · support 2 · canvas-note-section · `cluster-0189`
  - 4 nodes / 3 edges — Find or subtract background 2D -> Median filter 2D -> Gaussian blur 2D -> Orientation features 2D (OrientationJ)
  - Takes 2D image data, removes background, denoises with a median filter, smooths with a Gaussian filter, and outputs orientation-related image features.
- **Import and split multichannel images into grayscale channels** · support 2 · compartment · `cluster-0191`
  - 5 nodes / 3 edges — Bio-Formats importer -> Folder list -> Reduce & split hyperstack (Expression) -> Split channels -> Remove LUT
  - Takes a folder of multichannel image files, imports them, reduces/splits the image stack by selected planes, splits each multichannel slice into separate grayscale images, and removes LUT information.
- **Create refined 2D ROI mask stacks** · support 2 · canvas-note-section · `cluster-0193`
  - 4 nodes / 3 edges — Merge 2D ROI lists -> Convert 2D ROI to mask -> Morphological operation 2D -> Merge hyperstack C/Z/T (single input)
  - Takes one or more 2D ROI lists, merges them, converts the ROIs to binary mask images, applies a morphological operation to refine the masks, and combines the resulting masks into a single image stack.
- **Load images, split channels, and filter by image property** · support 2 · compartment · `cluster-0199`
  - 7 nodes / 5 edges — Filter paths -> Folder list -> Filter paths -> Import image -> Annotate with image properties -> Split channels -> Split & filter by annotation
  - Takes folder paths, filters them, imports images, annotates image properties, splits multichannel images into greyscale channel images, and filters/splits the annotated images by an image-property annotation.
- **Render 2D ROIs as RGB images** · support 2 · geometric-cluster · `cluster-0204`
  - 3 nodes / 1 edges — Convert 2D ROI to RGB -> Set LUT (color map) -> Convert 2D ROI to RGB
  - Takes 2D ROI lists and a reference image, converts the ROIs into RGB images using their stored line and fill colors, and applies a color map for display.
- **Normalize image intensity and attach metadata** · support 2 · geometric-cluster · `cluster-0206`
  - 3 nodes / 2 edges — Set annotation (expression) -> Annotate with image properties -> Divide by maximum
  - Takes an image, adds a single expression-defined annotation and image-property annotations, then divides greyscale pixel values by the global maximum across all slices. Produces an annotated image with intensity normalized relative to the maximum.
- **Clean and threshold 2D image stacks** · support 2 · geometric-cluster · `cluster-0209`
  - 6 nodes / 5 edges — Folder -> Annotate with file/directory name -> Import image stack -> Morphological operation 2D -> Auto threshold 2D (8-bit) -> Morphological operation 2D
  - Takes a folder of 2D image stacks, imports each stack, applies a morphological operation, auto-thresholds the 8-bit slices, and applies a second morphological operation. It produces cleaned, thresholded 2D image stacks.
- **Render imported images to RGB with a two-color LUT** · support 2 · geometric-cluster · `cluster-0215`
  - 11 nodes / 10 edges — File list -> File list -> File list -> Import image -> Import image -> Import image -> Set LUT (two colors) -> Adjust displayed contrast -> Apply LUT -> IO I…
  - Takes image file paths, imports the images, sets a two-color LUT and adjusts displayed contrast, then applies the LUT to produce RGB images.
- **Create binary object masks from images** · support 2 · compartment · `cluster-0220`
  - 8 nodes / 8 edges — Adjust displayed contrast -> Convert image to 8-bit -> Invert pixel colors/values -> Auto threshold 2D (8-bit) -> Median filter 2D -> Find or subtract backgr…
  - Takes a 2D image, converts it to 8-bit, and combines a direct auto-threshold with a threshold of an inverted, median-filtered, background-subtracted version to output a binary mask of objects.
- **Load multichannel images and split channels** · support 2 · geometric-cluster · `cluster-0221`
  - 3 nodes / 2 edges — Import image -> Split channels -> Remove LUT
  - Loads an image file, splits multichannel images into separate grayscale channel images for each 2D slice, and removes lookup-table (LUT) information.
- **Align 2D images to a reference** · support 2 · geometric-cluster · `cluster-0222`
  - 3 nodes / 3 edges — File list -> Import image -> TurboReg registration 2D (simple, referenced)
  - Takes a list of image file paths, imports the 2D images, and aligns each target image to a reference image using TurboReg, producing registered 2D images.
- **Load image files and split stacks into slice groups** · support 2 · geometric-cluster · `cluster-0223`
  - 3 nodes / 1 edges — File list -> Import image -> Reduce & split stacks (slice)
  - Takes a list of image file paths, imports each file as an image, and splits each image stack into separate stacks according to specified slice index ranges.
- **Import images with physical dimensions and a 2D ROI scale bar** · support 2 · geometric-cluster · `cluster-0224`
  - 5 nodes / 4 edges — File list -> Import image -> Set physical dimensions -> Draw scale bar -> Set 2D overlay
  - Takes a list of image file paths and produces imported images with physical pixel dimensions set and a scale bar represented as a 2D ROI overlay. It uses file listing, image import, physical dimension setting, scale bar drawing, and 2D overlay assignment.
- **Color-coded distance map overlay** · support 2 · geometric-cluster · `cluster-0225`
  - 10 nodes / 9 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Invert pixel colors/values -> Euclidean distance transform 2D -> Fast…
  - Takes image files, thresholds and cleans them, computes a 2D Euclidean distance transform, applies arithmetic to the distance values, converts them into a color-coded label image, and blends the result into an overlay image.
- **Compare 2D auto-thresholding methods** · support 1 · group-node · `cluster-0231`
  - 5 nodes / 5 edges — Group input -> Define multiple parameters -> Auto threshold 2D (8-bit) -> Create montage -> Group output
  - Takes 8-bit 2D images and a table of auto-thresholding parameters, applies the thresholding algorithm using those parameters, and produces thresholded images plus a montage of the thresholded results.
- **Detect lines in 2D images** ⚠ unregistered id · support 1 · geometric-cluster · `cluster-0242`
  - 3 nodes / 2 edges — Scale 2D image -> Tile image -> Ridge detector
  - Takes a 2D image, rescales and tiles it, then applies a ridge detector to produce a mask marking detected lines or ridges.
- **Enhance and correct imported images** · support 1 · compartment · `cluster-0246`
  - 12 nodes / 11 edges — Folder -> List files -> Annotate with file/directory name -> Bio-Formats importer -> Split channels (deprecated) -> Convert -> Enhance local contrast (CLAHE)…
  - Takes a folder of image files, imports them, splits multichannel images into grayscale channels, and applies contrast enhancement, illumination correction, background subtraction, and sharpening to produce cleaned 2D image slices.
- **Extended depth-of-focus projection with bleach correction** · support 1 · geometric-cluster · `cluster-0249`
  - 4 nodes / 3 edges — File list -> Import image -> Custom extended depth of focus (Z/C/T) -> Bleach correction
  - Takes a list of image files, imports each image, projects the Z/C/T dimension by selecting the sharpest/best-scoring pixel from each column to create an extended depth-of-focus image, and then applies per-channel bleach correction. Produces corrected extended depth-of-focus projections.
- **Smooth images with Gaussian blur** · support 1 · geometric-cluster · `cluster-0250`
  - 5 nodes / 2 edges — Tunnel entrance -> Tunnel exit -> Tunnel exit -> Tunnel entrance -> Gaussian blur 2D
  - Takes an image or image stack and produces a smoothed version by applying a 2D Gaussian blur to each slice.
- **Create Z-projections from image files** · support 1 · geometric-cluster · `cluster-0252`
  - 3 nodes / 2 edges — File list -> Import image -> Z-Project
  - Takes a list of image file paths, imports each image, and produces a Z-projected image by collapsing the Z dimension into a single 2D image.
- **Create a thresholded 2D image with statistics** · support 1 · geometric-cluster · `cluster-0257`
  - 5 nodes / 5 edges — Generate image from math expression -> Annotate by image statistics (Expression) -> Auto threshold 2D (8-bit) -> Set/Edit annotations -> Merge annotations
  - Creates an 8-bit image from a pixel expression, computes image statistics, applies 2D auto-thresholding, and merges the resulting annotations with the original image.
- **Compute image derivative features** · support 1 · geometric-cluster · `cluster-0258`
  - 3 nodes / 2 edges — File list -> Import image -> Derivatives features 2D/3D (FeatureJ)
  - Takes image file paths, imports the images, and computes multi-dimensional Gaussian-scaled derivative images for each time frame and channel. It produces derivative feature images that describe local image structure for further analysis.
- **Split 2D labels into connected components** · support 1 · geometric-cluster · `cluster-0264`
  - 3 nodes / 2 edges — File list -> Import image -> Split labels (connected components) 2D
  - Takes image file paths, imports each image, and produces a 2D label image in which disconnected regions that share a label value are assigned distinct labels.
- **Detect line segments in images as 2D ROIs** · support 1 · geometric-cluster · `cluster-0266`
  - 5 nodes / 5 edges — File list -> Import image -> Binarize -> Detect line segments 2D (Hough) -> Set 2D overlay
  - Takes image file paths, imports and binarizes each image, detects straight line segments with a Hough transform, and produces the image with those line segments attached as 2D ROI overlays.
- **Create an RGB montage from image files** · support 1 · geometric-cluster · `cluster-0267`
  - 7 nodes / 6 edges — File list -> Annotate with file/directory name -> Import image -> Scale 2D image -> Convert image to 8-bit -> Render to RGB -> Create montage
  - Takes a list of image file paths and produces one combined RGB montage image by importing each file, scaling 2D slices, converting to 8-bit, and rendering to RGB.
- **Compute morphological feature contrast from images** · support 1 · geometric-cluster · `cluster-0269`
  - 8 nodes / 7 edges — File list -> Define multiple parameters -> Multiply parameters -> Annotate with file/directory name -> Import image -> Morphological operation 2D -> Morpholo…
  - Takes a list of image file paths, imports each image, applies two 2D morphological operations, and calculates the 2D Morphological Feature Contrast (MFC) across multiple parameter combinations. Produces MFC images that emphasize bright and dark structures, annotated with the source file names.
- **Extract top-left image tile** · support 1 · group-node · `cluster-0279`
  - 8 nodes / 8 edges — Group input -> Tile image -> Split & filter by annotation -> Remove annotation -> Remove annotation -> Remove annotation -> Remove annotation -> Group output
  - Takes an image, splits it into fixed-size tiles, selects the top-left tile by annotation, and removes tile-related annotations. It produces a single top-left tile image.
- **Detect and label circular objects in 2D images** · support 1 · group-node · `cluster-0280`
  - 8 nodes / 11 edges — Group input -> Detect circles 2D (Hough, fast) -> Bitwise operation -> Morphological operation 2D -> Distance transform watershed 2D -> Morphological operati…
  - Detects circular 2D objects in an image and produces a labeled component image, a combined mask, the Hough accumulator, and circle measurements. It uses a fast Hough circle detector, optionally bitwise-combines the detected mask with a second input, cleans the mask with morphology and distance-transform watershed, then labels connected components.
- **Clean and label 2D mask objects** · support 1 · canvas-note-section · `cluster-0286`
  - 4 nodes / 3 edges — Despeckle 2D -> Area opening 2D -> Connected components labeling 2D -> Remove border labels
  - Takes a 2D mask, applies median despeckling and area opening to remove noise and small objects, then labels connected components and removes labels touching the border, producing a clean label image of interior objects.
- **Clean and smooth 2D images** · support 1 · canvas-note-section · `cluster-0287`
  - 3 nodes / 2 edges — Find or subtract background 2D -> Morphological operation 2D -> Box filter 2D (3x3)
  - Takes 2D images, subtracts background, applies a morphological operation, and smooths each 2D slice with a 3x3 box filter, producing cleaned images for downstream analysis.
- **Import, calibrate, and crop 2D images** · support 1 · geometric-cluster · `cluster-0297`
  - 6 nodes / 5 edges — Remove annotation -> Remove annotation -> Remove annotation -> Import image -> Set physical dimensions -> Crop 2D image
  - Takes image file entries, removes selected annotations, imports each image, sets its physical dimensions, and crops it, producing calibrated, cropped 2D images.
- **Color and export 2D segmented objects** · support 1 · canvas-note-section · `cluster-0302`
  - 6 nodes / 5 edges — Connected components labeling 2D -> Morphological operation 2D -> Convert labels to RGB -> Set to color (RGB) -> Blend images -> Export image
  - Takes a 2D mask, labels connected components and applies a morphological operation, then colors the components, overlays them on the mask, and exports the final image.
- **Rotate an imported image** · support 1 · geometric-cluster · `cluster-0303`
  - 3 nodes / 2 edges — File -> Import image -> Rotate 2D image (free)
  - Loads an image from a file and rotates it by a specified angle, producing a rotated image with the canvas expanded when needed.
- **Create image montages from stacks** · support 1 · group-node · `cluster-0304`
  - 6 nodes / 5 edges — Group input -> Stack to montage -> Make images equal 2D (single input) -> Create 3D stack -> Stack to montage -> Group output
  - Takes image stacks, converts them into 2D montage images, pads them to a common size, merges the resulting planes into a 3D stack, and outputs a final montage for visual inspection.
- **Blend two images** · support 1 · group-node · `cluster-0305`
  - 5 nodes / 6 edges — Group input -> Legacy image calculator 2D -> Legacy image math operation (scalar) -> Legacy image calculator 2D -> Group output
  - Takes two image inputs and produces a pixel-wise intermediate image by computing a difference, scaling it with a scalar math operation, and adding it to one input. This reusable group node performs linear interpolation/blending of 2D images or image stacks.
- **Import images and render 8-bit RGB images** · support 1 · geometric-cluster · `cluster-0315`
  - 5 nodes / 4 edges — File list -> Import image -> Convert image to 8-bit -> Adjust displayed contrast -> Render to RGB
  - Takes image file paths, imports each image, converts it to 8-bit, calibrates its displayed contrast, and renders it to RGB. It produces viewer-style 8-bit RGB images suitable for display or downstream image processing.
- **Convert 2D ROIs into RGB mask images** · support 1 · canvas-note-section · `cluster-0322`
  - 4 nodes / 3 edges — Split 2D ROI into connected components -> Set 2D ROI properties from expressions -> Merge 2D ROI lists -> Convert 2D ROI to RGB
  - Takes 2D ROI lists, splits them into connected components, sets ROI properties from expressions, merges the component lists, and converts the merged ROIs into RGB mask images for visualization.
- **Render images and 2D ROIs as RGB** · support 1 · canvas-note-section · `cluster-0323`
  - 5 nodes / 4 edges — Histogram-based contrast enhancer -> Set 2D ROI properties -> Set LUT (color map) -> Apply LUT -> Convert 2D ROI to RGB
  - Takes an image and 2D ROIs and produces a display-ready RGB image by enhancing contrast, setting a color map, and converting the 2D ROIs into colored masks. The image supplies the output size, while the 2D ROIs contribute their line and fill colors.
- **Batch convert 2D image stacks to 8-bit and export** · support 1 · geometric-cluster · `cluster-0324`
  - 6 nodes / 4 edges — Folder list -> Folder list -> Annotate with file/directory name -> Import image stack -> Convert image to 8-bit -> Export image
  - Takes folders of 2D image stacks, annotates each path with its name, imports the stacks, converts them to 8-bit, and exports the images to standard file formats.

### rois (69)

- **Detect 2D particle ROIs from images** · support 35 · geometric-cluster · `cluster-0002`
  - 4 nodes / 3 edges — File list -> Import image -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Takes image file paths, imports each image, applies an 8-bit auto threshold, and finds connected particles in each 2D slice, producing 2D ROIs with measurements.
- **Filter 2D ROIs by overlap** · support 34 · compartment · `cluster-0003`
  - 6 nodes / 1 edges — Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Filter 2D ROI by over…
  - Takes two lists of 2D ROIs and produces filtered 2D ROI lists containing the ROIs that overlap with ROIs in the other list. Chained overlap filters can be used to include or exclude ROIs based on their spatial relationship to reference ROIs.
- **Attach 3D ROI measurements to data** · support 31 · geometric-cluster · `cluster-0005`
  - 3 nodes / 1 edges — Extract IJ3D ROI statistics -> Add table column -> Annotate data with table values
  - Takes 3D ROIs and optionally a reference image, and produces data annotated with 3D ROI statistics. It extracts the ROI measurements, adds a table column, and attaches the resulting table values to the incoming data.
- **Detect 2D particles as 2D ROIs and masks** · support 28 · geometric-cluster · `cluster-0006`
  - 4 nodes / 3 edges — Morphological operation 2D -> Auto threshold 2D (8-bit) -> Find particles 2D -> Convert 2D ROI to RGB
  - Takes greyscale 2D images and produces detected particles as 2D ROIs with measurements and an RGB mask. It applies a morphological operation, auto-thresholds each 2D slice, extracts connected particles, and converts the resulting 2D ROIs to an RGB mask.
- **3D ROI label images colored by measurements** · support 22 · canvas-note-section · `cluster-0008`
  - 4 nodes / 3 edges — Color IJ3D ROI by statistics -> Convert IJ3D ROI to RGB -> Convert IJ3D ROI to RGB -> Convert IJ3D ROI to RGB
  - Takes 3D ROIs with measurements, assigns each ROI a color based on a statistic, and converts the colored 3D ROIs into RGB label images. This produces visual label images in which each 3D object's color reflects its measured value.
- **Segment 3D ROIs and create a colored overlay** · support 20 · canvas-note-section · `cluster-0011`
  - 12 nodes / 11 edges — Remove outliers 2D -> Gaussian blur 3D -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Distance transform watershed 2D -> Connected components …
  - Takes an image and produces 3D ROIs by denoising, smoothing, thresholding, watershed segmentation, and 3D particle detection. It outlines the 3D ROIs, converts them to an RGB label image, blends that overlay with the original image, and copies physical dimensions.
- **Segment 2D images into 2D ROIs** · support 18 · canvas-note-section · `cluster-0012`
  - 7 nodes / 6 edges — Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Morphological operation 2D -> Distance transform watershed 2D -> Find parti…
  - Takes 8-bit 2D images, smooths and thresholds them, fills holes, applies morphology and distance-transform watershed, then extracts particle 2D ROIs and converts those ROIs to an RGB mask.
- **Refine 2D ROIs with watershed segmentation** · support 13 · compartment · `cluster-0018`
  - 10 nodes / 10 edges — Convert 2D ROI to mask -> Euclidean distance transform 2D -> Distance transform watershed 2D -> Mask to 2D ROI -> Local maxima 2D -> Convert 2D ROI to RGB ->…
  - Takes initial 2D ROIs and produces refined 2D ROIs and RGB overlay images by separating touching objects with a distance-transform watershed and seeded watershed. It converts the input ROIs to a mask, derives markers from local maxima of the distance transform, and outputs both ROI and label RGB images.
- **Merge 2D ROIs into a mask and measurement table** · support 11 · geometric-cluster · `cluster-0022`
  - 4 nodes / 3 edges — Merge 2D ROI lists -> Convert 2D ROI to RGB -> Extract 2D ROI statistics -> Merge table rows
  - Takes multiple 2D ROI lists, merges them into a consolidated 2D ROI list, renders the ROIs as an RGB mask, and extracts per-ROI statistics. The resulting measurement rows are merged into a single table.
- **Detect and filter 2D particle ROIs** · support 10 · geometric-cluster · `cluster-0024`
  - 7 nodes / 6 edges — Find or subtract background 2D -> Auto threshold 2D (8-bit) -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Find particl…
  - Takes 2D images, applies background subtraction, thresholding, Gaussian smoothing, and hole filling to create particle masks, then converts those masks into measured 2D ROIs and keeps only ROIs matching statistical criteria.
- **Segment 3D ROIs from an image and measure their statistics** · support 8 · canvas-note-section · `cluster-0027`
  - 6 nodes / 5 edges — Auto threshold 2D (8-bit) -> Median filter 2D -> Connected components labeling 3D -> IJ3D Labels to 3D ROI -> Filter IJ3D ROI by statistics -> Extract IJ3D R…
  - Takes an 8-bit image or channel, applies auto thresholding and median filtering, labels connected components in 3D, converts the labels to 3D ROIs, filters the ROIs by statistics, and produces a table of 3D ROI measurements.
- **Export 3D ROI overlap image** · support 8 · canvas-note-section · `cluster-0028`
  - 12 nodes / 12 edges — Create empty image -> Create empty image -> Filter IJ3D ROI by overlap (old) -> Set IJ3D ROI properties -> Set IJ3D ROI properties -> Convert IJ3D ROI to RGB…
  - Takes two 3D ROI lists, keeps only the ROIs that mutually overlap, converts each filtered set to a label image, blends them as a colored overlay, and exports the calibrated image.
- **Pairwise 3D ROI distance summary** · support 8 · compartment · `cluster-0029`
  - 11 nodes / 12 edges — Extract pairwise IJ3D ROI statistics -> Extract pairwise IJ3D ROI statistics -> JFreeChart Histogram plot -> Export data -> JFreeChart Histogram plot -> Expo…
  - Takes 3D ROIs and computes all pairwise distance statistics between them, then produces histogram plots and exports the measurement table and rendered plot images.
- **Evaluate 2D ROI detection quality** · support 6 · compartment · `cluster-0040`
  - 9 nodes / 9 edges — Filter 2D ROI by overlap (old) -> Filter 2D ROI by overlap (old) -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI…
  - Takes two 2D ROI lists, such as ground truth and predicted detections, and filters them by mutual overlap to separate true positives, false positives, and false negatives. It counts each category, converts the ROIs to RGB masks for visualization, and applies a per-row expression to compute summary quality metrics.
- **Create 3D ROI masks with measured statistics** · support 6 · canvas-note-section · `cluster-0043`
  - 4 nodes / 3 edges — Split into individual IJ3D ROI lists -> Extract IJ3D ROI statistics -> Annotate data with table values -> IJ3D ROI to mask
  - Takes a list of 3D ROIs, splits it into individual 3D ROIs, measures each ROI's statistics from a reference image, and converts each annotated 3D ROI into a mask. It produces per-ROI masks carrying the measured statistics.
- **Summarize overlapping 2D ROIs** · support 6 · canvas-note-section · `cluster-0048`
  - 9 nodes / 9 edges — Filter 2D ROI by overlap (old) -> Annotate with data -> Extract data annotations -> Annotate with 2D ROI properties -> Extract 2D ROI statistics -> Add annot…
  - Produces a table summarizing 2D ROIs that overlap a second 2D ROI set. It filters the first ROI list by overlap, annotates the retained ROIs with the overlapping ROIs, extracts ROI properties and ROI statistics, and adds them as table columns.
- **Create cleaned mask from filtered 2D ROIs** · support 5 · geometric-cluster · `cluster-0064`
  - 4 nodes / 3 edges — Filter 2D ROI by statistics -> Convert 2D ROI to mask -> Invert pixel colors/values -> Morphological operation 2D
  - Takes 2D ROIs and a reference image, keeps ROIs that pass a statistical filter, converts them to a binary mask, inverts the mask, and applies a 2D morphological operation, producing a cleaned binary mask image.
- **Measure 2D ROIs after excluding edge-touching objects** · support 4 · geometric-cluster · `cluster-0077`
  - 6 nodes / 5 edges — 2D ROI to Labels (Expression) -> CLIJ2 Exclude Labels On Edges -> Labels to ROI -> Extract 2D ROI statistics -> Add annotations as columns -> Merge table rows
  - Takes 2D ROIs and an optional reference image, converts them to a label image, removes labels touching image edges, converts the remaining labels back to 2D ROIs, extracts ROI statistics, adds annotation columns, and merges the resulting tables into one table.
- **Filter 2D ROIs by overlap and annotate their geometry** · support 4 · geometric-cluster · `cluster-0078`
  - 6 nodes / 3 edges — Rename annotation -> Filter 2D ROI by overlap (old) -> Split into individual 2D ROI lists -> Annotate with 2D ROI properties -> Outline 2D ROI (Hull) -> Set …
  - Takes 2D ROI lists and produces individual 2D ROIs annotated with geometric properties and convex-hull outlines after overlap filtering. It renames annotations, filters overlapping 2D ROIs, splits ROI lists, extracts 2D ROI properties into annotations, computes hull outlines, and sets expression-based annotations.
- **Segment 2D particles into 2D ROIs** · support 4 · compartment · `cluster-0080`
  - 8 nodes / 7 edges — Image box canvas note -> Auto threshold 2D (8-bit) -> Median filter 2D -> Illumination correction 2D -> Auto threshold 2D (8-bit) -> Find particles 2D -> Con…
  - Takes 8-bit 2D images, applies thresholding, median filtering, and illumination correction, then uses Find particles 2D to produce 2D ROIs. It converts the 2D ROIs to an RGB mask and sets that mask as an overlay on the image.
- **Detect 3D particles as 3D ROIs** · support 4 · canvas-note-section · `cluster-0087`
  - 6 nodes / 5 edges — Median filter 2D -> Set LUT (color map) -> Morphological Feature Contrast (MFC) 2D -> Auto threshold 2D (8-bit) -> IJ3D Find particles 3D -> Set IJ3D ROI pro…
  - Takes image stacks, denoises and enhances contrast, thresholds the result, and finds 3D particles, producing 3D ROIs with set properties for the detected particles.
- **Detect 3D particle ROIs from images** · support 4 · canvas-note-section · `cluster-0088`
  - 6 nodes / 5 edges — Median filter 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Connected components labeling 3D -> Filter labels by expression 3D -> IJ3D Find parti…
  - Takes an 8-bit image, applies median filtering and a color map, auto-thresholds each 2D slice, labels connected components in 3D, filters labels by expression, and finds 3D particles, producing 3D ROIs for the detected particles.
- **Extract 3D particle ROIs from label images** · support 4 · canvas-note-section · `cluster-0089`
  - 8 nodes / 7 edges — Convert labels to mask -> Set to color (RGB) -> Median filter 2D -> Set LUT (color map) -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> IJ3D…
  - Takes a label image and produces 3D ROIs for each detected particle by converting labels to a mask, applying color/LUT, median filtering, auto-thresholding, and hole filling, then finding 3D particles and setting their 3D ROI properties.
- **Measure 2D ROI statistics from a mask** · support 4 · canvas-note-section · `cluster-0094`
  - 9 nodes / 8 edges — Connected components labeling 2D -> Filter labels by statistics 2D -> Extract label statistics 2D -> Rename table column -> Export data -> Add annotations as…
  - Takes a 2D mask, labels connected components as 2D ROIs, filters and measures them, and exports a table of 2D ROI statistics. It can also add annotation columns and merge them into the final exported table.
- **Convert 2D ROIs to RGB mask images** · support 4 · canvas-note-section · `cluster-0096`
  - 4 nodes / 3 edges — Set 2D ROI properties -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB
  - Takes 2D ROIs, assigns user-defined properties such as colors, and renders them as RGB mask images using a reference image for output dimensions.
- **Create RGB images from 2D ROIs** · support 4 · geometric-cluster · `cluster-0099`
  - 3 nodes / 0 edges — Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB
  - Takes 2D ROI lists and a reference image for size, and produces RGB mask images by rendering each ROI's stored line and fill colors.
- **Detect 2D particle ROIs in an image** · support 3 · canvas-note-section · `cluster-0114`
  - 4 nodes / 3 edges — Import image -> Median filter 2D -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Takes an image, applies a 2D median filter and 8-bit auto thresholding, then converts the thresholded mask into 2D particle ROIs with measurements.
- **Create 2D ROI masks from images** · support 3 · geometric-cluster · `cluster-0115`
  - 9 nodes / 8 edges — Project user path -> List subfolders -> Annotate with file/directory name -> Filter paths -> List files -> Annotate with file/directory name -> Import image …
  - Takes image files from filtered subfolders, imports each image, removes its overlay 2D ROIs, and converts those 2D ROIs into mask images.
- **Measure and export 3D particle ROI statistics** · support 3 · canvas-note-section · `cluster-0120`
  - 4 nodes / 3 edges — IJ3D Find particles 3D -> Extract IJ3D ROI statistics -> Filter table rows -> Export data
  - Takes a mask image and optional reference image, detects 3D particles as 3D ROIs, extracts 3D ROI statistics such as volume and center, filters the resulting table rows, and exports the filtered measurements.
- **Export filtered 2D ROIs as RGB mask images** · support 3 · geometric-cluster · `cluster-0123`
  - 5 nodes / 4 edges — Split into individual 2D ROI lists -> Filter 2D ROI by statistics -> Merge 2D ROI lists -> Convert 2D ROI to RGB -> Export image
  - Takes 2D ROI lists and produces exported RGB mask images of the ROIs that pass a statistics filter. It splits ROI lists into individual ROIs, filters by ROI statistics, merges the remaining ROIs, converts them to colored mask images, and exports the images.
- **Overlay 2D ROIs on an image** · support 3 · group-node · `cluster-0128`
  - 4 nodes / 4 edges — Group input -> Histogram-based contrast enhancer -> Convert 2D ROI to RGB -> Group output
  - Takes an image and 2D ROIs, enhances the image contrast, and converts the 2D ROIs into an RGB overlay using the enhanced image as the size reference. Produces an RGB image in which the ROIs are visible on the corresponding channel.
- **Load and filter 2D ROI files** · support 2 · geometric-cluster · `cluster-0139`
  - 7 nodes / 6 edges — Annotate with file/directory name -> Import ROI -> Set annotation (expression) -> Set annotation (expression) -> Set annotation (expression) -> Set annotatio…
  - Produces filtered 2D ROI data by importing ROI files from file paths, adding metadata from file/directory names and expressions, and splitting the ROIs based on an annotation value.
- **Summarize 2D ROI measurements by label** · support 2 · geometric-cluster · `cluster-0141`
  - 3 nodes / 2 edges — Set 2D ROI properties from expressions -> Extract 2D ROI statistics -> Apply expression per label
  - Takes 2D ROIs, sets ROI properties from expressions, extracts per-ROI statistics optionally from a reference image, and produces a table summarizing those statistics by label.
- **Filter 2D ROIs and render RGB masks** · support 2 · geometric-cluster · `cluster-0142`
  - 11 nodes / 12 edges — Filter by annotation -> Filter 2D ROI by statistics -> Set 2D ROI properties -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert only 2D ROI to RGB …
  - Takes a 2D ROI list, filters ROIs by annotation and statistics, sets ROI properties, and converts the selected ROIs into RGB mask images while extracting ROI statistics tables.
- **Detect 2D ROIs from image stacks** ⚠ unregistered id · support 2 · compartment · `cluster-0153`
  - 11 nodes / 9 edges — Bio-Formats importer -> Project user path -> Annotate with image properties -> Median filter 2D -> Reduce & split hyperstack (Expression) -> Reorder dimensio…
  - Takes imported image stacks, denoises them with a median filter, reorders/splits dimensions, and combines Z slices into a 2D extended depth-of-focus projection. It auto-thresholds the projection and produces 2D ROIs for detected particles with measurements.
- **Extract 2D particle ROIs from images** · support 2 · canvas-note-section · `cluster-0156`
  - 9 nodes / 8 edges — Divide by maximum -> CLIJ2 Gaussian Blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> CLIJ2 Gaussian Blur 2D -> Illumination correctio…
  - Takes 2D greyscale images and produces 2D particle ROIs by normalizing intensity, blurring, thresholding, filling holes, applying illumination correction, and using Find particles 2D. The resulting ROIs are then set to a specified color.
- **Compute 2D ROI statistics from image stacks** · support 2 · canvas-note-section · `cluster-0161`
  - 5 nodes / 4 edges — Split stack into 2D -> Extract 2D ROI statistics -> Apply expression per row -> Merge table rows -> Split table by columns
  - Takes image stacks and 2D ROIs and produces a table of per-plane ROI measurements. It splits the stack into 2D planes, extracts 2D ROI statistics, applies a row expression, then merges and splits the resulting table by selected columns.
- **Detect 3D ROIs and overlay a colored mask** · support 2 · canvas-note-section · `cluster-0170`
  - 9 nodes / 8 edges — CLIJ2 Add Images -> Morphological operation 3D -> Auto threshold 2D (8-bit) -> Morphological operation 3D -> IJ3D Find particles 3D -> Outline IJ3D ROI -> IJ…
  - Takes one or more slice images, combines them, applies 3D morphological operations and 2D auto-thresholding to find 3D ROIs, then converts the outlined 3D ROIs to a colored mask and blends it over the combined images.
- **Detect and measure 2D particle ROIs** · support 2 · geometric-cluster · `cluster-0172`
  - 10 nodes / 8 edges — Set/Edit annotations -> Apply transformation -> Run ImageJ Macro -> Morphological operation 2D -> Morphological hole filling 2D -> Find particles 2D -> Conve…
  - Takes image data, applies pixel transformations and a custom ImageJ macro, then cleans the image with morphological operations and hole filling before finding 2D particle ROIs. It merges the 2D ROIs, converts them to an RGB mask, and extracts a results table of 2D ROI statistics.
- **Create RGB masks from filtered Cellpose 2D ROIs** · support 2 · canvas-note-section · `cluster-0174`
  - 10 nodes / 14 edges — Filter by annotation -> Cellpose prediction (2.x) -> Set 2D ROI properties from expressions -> Split into individual 2D ROI lists -> Filter 2D ROI by statist…
  - Takes selected images, runs Cellpose to detect cells as 2D ROIs, then filters and merges those ROIs based on extracted statistics and converts the remaining ROIs into an RGB mask image.
- **Refine 2D ROIs and extract their measurements** · support 2 · geometric-cluster · `cluster-0175`
  - 7 nodes / 6 edges — Local variance 2D -> Divide by maximum -> Filter 2D ROI by statistics -> Convert 2D ROI to mask -> Morphological operation 2D -> Mask to 2D ROI -> Extract 2D…
  - Takes an image and a list of 2D ROIs and produces cleaned 2D ROIs plus a statistics table. It filters ROIs using statistics from a normalized local-variance image, converts the selected ROIs to a mask, applies a morphological operation, converts the cleaned mask back to 2D ROIs, and extracts ROI statistics.
- **Render 2D ROIs as a colored RGB image** · support 2 · canvas-note-section · `cluster-0176`
  - 4 nodes / 3 edges — Draw 2D rectangle/oval/line ROI -> Set to color (RGB) -> Convert 2D ROI to RGB -> Merge annotations
  - Takes drawn 2D ROIs and a solid-color background, and produces an RGB image where the ROIs are rendered using their stored line and fill colors. It merges annotations from the rendered ROI image and the background image.
- **Segment and measure 3D ROIs in a selected channel** · support 2 · canvas-note-section · `cluster-0182`
  - 9 nodes / 8 edges — Reduce & split hyperstack (Expression) -> Z-Project -> Gaussian blur 2D -> Equalize hyperstack dimensions (referenced) -> Fast image arithmetics 2D -> Auto t…
  - Takes a hyperstack, isolates a chosen channel, projects and smooths it, aligns it to a reference, thresholds and labels connected components, then converts the labels to 3D ROIs and outputs a table of 3D ROI statistics.
- **Compute pairwise distances between 3D ROIs** · support 2 · compartment · `cluster-0184`
  - 10 nodes / 6 edges — Extract IJ3D ROI statistics -> Extract pairwise IJ3D ROI statistics -> Connected components labeling 3D -> Extract pairwise IJ3D ROI statistics -> Extract IJ…
  - Takes a 3D mask/label image, labels connected components, copies physical dimensions, converts the labels to 3D ROIs, and extracts per-ROI and pairwise 3D ROI statistics. It produces a table of 3D ROI measurements and pairwise distance/statistics values, with Python scripts used to post-process the measurement tables.
- **Segment nuclei into 2D ROIs and masks** · support 2 · compartment · `cluster-0190`
  - 12 nodes / 11 edges — Pretrained Cellpose 3.x image restoration model -> Find or subtract background 2D -> Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x im…
  - Takes fluorescence images and produces 2D ROIs for nuclei plus an RGB mask. It subtracts background, smooths with median and Gaussian filters, denoises with Cellpose restoration, runs Cellpose segmentation, filters the 2D ROIs by statistics, and converts the ROIs to RGB.
- **Visualize 2D ROIs as RGB masks** · support 2 · compartment · `cluster-0192`
  - 10 nodes / 12 edges — Set 2D ROI properties -> Illumination correction 2D -> Set 2D ROI properties -> Adjust displayed contrast -> Filter 2D ROI by overlap (old) -> Convert 2D ROI…
  - Takes 2D ROI lists and an image, sets ROI properties, filters ROIs by mutual overlap, applies illumination correction and display contrast adjustment, and produces RGB mask images of the ROIs.
- **Filter and merge overlapping 2D ROIs** · support 2 · compartment · `cluster-0194`
  - 8 nodes / 4 edges — Filter 2D ROI by overlap (old) -> Merge 2D ROI lists -> Filter 2D ROI by overlap (old) -> Merge 2D ROI lists -> Filter 2D ROI by overlap (old) -> Filter 2D R…
  - Produces 2D ROI lists containing only ROIs that overlap with other ROIs, with overlapping ROIs merged together. It filters 2D ROI lists by mutual overlap and then merges the retained ROIs into combined 2D ROI lists.
- **Create montage of 2D ROI masks** · support 2 · geometric-cluster · `cluster-0201`
  - 11 nodes / 5 edges — Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Merge 2D ROI lists -> Convert 2D ROI to RGB -> Convert 2D ROI to RGB -> Convert 2D…
  - Takes 2D ROI lists and reference images, merges grouped ROI lists, converts them to colored RGB mask images, and produces a labeled montage of those masks.
- **Segment particles into 2D ROIs** · support 2 · compartment · `cluster-0208`
  - 10 nodes / 7 edges — Define multiple parameters -> Remove outliers 2D -> Define multiple parameters -> Enhance local contrast (CLAHE) -> Find or subtract background 2D -> Auto th…
  - Takes 2D images and produces particle 2D ROIs by removing outliers, enhancing local contrast, subtracting background, auto-thresholding, and applying distance-transform watershed segmentation. It then converts the 2D ROIs to an RGB mask and creates a montage of the segmented images.
- **Measure 2D ROI particles in mask images** · support 2 · geometric-cluster · `cluster-0210`
  - 8 nodes / 7 edges — Find particles 2D -> Split 2D ROI into connected components -> Extract 2D ROI statistics -> Summarize table -> Add annotations as columns -> Merge table rows…
  - Takes thresholded mask images and produces a summarized table of 2D ROI particle measurements plus a plot. It finds 2D particles, splits each 2D ROI into connected components, extracts 2D ROI statistics, then summarizes, annotates, merges, filters, and plots the table.
- **Create 2D ROI masks from skeletonized binary images** · support 2 · geometric-cluster · `cluster-0211`
  - 3 nodes / 2 edges — Morphological skeletonize 2D -> Find particles 2D -> Convert 2D ROI to RGB
  - Produces an RGB mask of connected structures in a skeletonized binary image. It takes a binary image, thins it with morphological skeletonization, detects connected components as 2D ROIs, and renders those ROIs as an RGB mask image.
- **Measure 2D ROI components and combined regions** · support 2 · geometric-cluster · `cluster-0212`
  - 11 nodes / 11 edges — Split 2D ROI into connected components -> Extract 2D ROI statistics -> 2D ROI calculator (AND/OR/XOR) -> Extract 2D ROI statistics -> Rename & modify columns…
  - Takes a list of 2D ROIs, splits them into connected components, computes ROI statistics for each component and for the logical combination of all ROIs, then merges and sorts the measurement tables and creates a plot and image montage.
- **Detect 2D particle ROIs from 8-bit images** · support 2 · group-node · `cluster-0213`
  - 4 nodes / 4 edges — Group input -> Auto threshold 2D (8-bit) -> Find particles 2D -> Group output
  - Takes 8-bit 2D images, applies auto thresholding, and converts thresholded regions into 2D particle ROIs with measurements. It produces one 2D ROI per detected particle along with the associated measurement data.
- **Segment particles into colored 2D ROIs** · support 1 · geometric-cluster · `cluster-0236`
  - 9 nodes / 9 edges — File list -> Import image -> Z-Project (classic) -> Distance transform watershed 2D -> Find particles 2D -> 2D ROI to Labels (by name) -> Labels to ROI -> Co…
  - Takes a list of image files, imports each image, collapses it to a 2D Z-projection, and segments particles using distance-transform watershed and Find particles 2D. It produces colored 2D ROIs and an RGB mask by converting ROIs to labels, coloring them by name, and rendering the ROIs to RGB.
- **Create 2D point ROIs from results tables** · support 1 · geometric-cluster · `cluster-0237`
  - 7 nodes / 6 edges — File list -> Annotate with file/directory name -> Import results table -> Filter table rows -> Table to 2D point ROI -> Set 2D overlay -> Set 2D overlay
  - Takes a list of results-table files, annotates each with its file or directory name, filters table rows, and converts the remaining rows into 2D point ROIs that are set as overlays.
- **Segment 2D images into 2D ROIs** · support 1 · compartment · `cluster-0239`
  - 12 nodes / 11 edges — Hessian segmentation 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological hole filling 2D -> Distance transform watershed 2D -> Morphological…
  - Takes a 2D image (or applies per slice to higher-dimensional data) and produces individual 2D ROIs by applying Hessian-based segmentation, Gaussian smoothing, auto-thresholding, hole filling, distance-transform watershed, and morphological cleanup, then converting masks to ROIs and splitting multi-component ROIs.
- **Add 2D rectangle, oval, and text ROIs to an image** · support 1 · geometric-cluster · `cluster-0256`
  - 4 nodes / 3 edges — Draw 2D rectangular ROI (old) -> Draw 2D oval ROI (old) -> Draw 2D text ROI -> Set 2D overlay
  - Takes an image and produces 2D rectangular, oval, and text ROIs, then sets them as the image's 2D overlay.
- **Detect and overlay text in images as 2D text ROIs** · support 1 · geometric-cluster · `cluster-0272`
  - 7 nodes / 7 edges — File list -> Annotate with file/directory name -> Import image -> Find text (Tesseract OCR) -> Filter table rows -> Table to 2D text ROI -> Set 2D overlay
  - Takes a list of image files, imports each image, runs Tesseract OCR to detect text, filters the OCR result table, and converts the remaining text into 2D text ROIs that are set as overlays on the images.
- **Detect 2D particle ROIs in OMERO images** · support 1 · geometric-cluster · `cluster-0273`
  - 6 nodes / 5 edges — List OMERO projects -> List OMERO datasets -> List OMERO images in dataset -> Download image from OMERO -> Auto threshold 2D (8-bit) -> Find particles 2D
  - Takes OMERO project, dataset, and image search criteria and produces 2D particle ROIs with measurements. It lists matching OMERO projects, datasets, and images, downloads the images, applies 8-bit 2D auto-thresholding, and converts the thresholded masks into 2D particle ROIs.
- **Load 2D ROIs from ZIP archives and compute convex hulls** · support 1 · geometric-cluster · `cluster-0277`
  - 5 nodes / 4 edges — File list -> Import data table (archive) -> Set/Edit annotations -> Outline 2D ROI (Hull) -> Set 2D ROI properties
  - Takes file paths to ZIP archives containing data-table.json, imports the 2D ROI data tables, edits annotations, applies a convex-hull/outline operation to the 2D ROIs, and sets ROI properties; produces standardized 2D ROI data.
- **Count 2D ROIs into a summary table** · support 1 · canvas-note-section · `cluster-0282`
  - 5 nodes / 4 edges — Count 2D ROI -> Count 2D ROI -> Add table column -> Add table column -> Merge table rows
  - Takes one or more lists of 2D ROIs and produces a single table summarizing how many ROIs are present in each list. It counts each 2D ROI list, adds the resulting count columns, and merges the rows into one table.
- **Create 2D ROIs from watershed segmentation** · support 1 · canvas-note-section · `cluster-0283`
  - 6 nodes / 5 edges — Seeded watershed -> Manual threshold 2D (8-bit) -> Legacy image calculator 2D -> Find particles 2D -> Find particles 2D -> Set 2D ROI properties
  - Takes a 2D image and seed labels, applies seeded watershed and thresholding, then extracts connected components as 2D ROIs and assigns a common ROI property.
- **Export images with 2D ROI overlays** · support 1 · canvas-note-section · `cluster-0288`
  - 4 nodes / 3 edges — Merge 2D ROI lists -> Set 2D overlay -> Render overlay -> Export image
  - Takes images and multiple 2D ROI lists, merges the ROIs, draws them as an overlay on the images, and exports the rendered RGB images.
- **Detect and filter 2D particle ROIs** · support 1 · canvas-note-section · `cluster-0289`
  - 3 nodes / 3 edges — Find particles 2D -> Filter 2D ROI by statistics -> Filter 2D ROI by overlap (old)
  - Takes thresholded mask images, extracts connected 2D particle ROIs with measurements, then filters them by statistical criteria and overlap with the full detected particle set, producing a cleaned list of 2D ROIs.
- **Detect 2D marker ROIs in images** · support 1 · canvas-note-section · `cluster-0291`
  - 9 nodes / 10 edges — Laplacian of Gaussian 2D -> Custom auto threshold 2D (32-bit) -> Legacy image calculator 2D -> Euclidean distance transform 2D -> Find particles 2D -> Filter…
  - Detects marker-like objects in 2D images and produces filtered 2D marker ROIs plus a mask where each detected marker is assigned a unique label. It applies LoG filtering, thresholding, distance transform, particle detection, and ROI statistics filtering.
- **Filter 2D particle ROIs by size** · support 1 · compartment · `cluster-0295`
  - 5 nodes / 4 edges — Find particles 2D -> JFreeChart Histogram plot -> Summarize table -> Annotate data with table values -> Filter by annotation (If else)
  - Takes thresholded images, extracts 2D particle ROIs and measurements, summarizes size statistics, and filters the ROIs by size; it also plots a histogram of the measurements.
- **Create a 2D ROI mask from detected spots** · support 1 · geometric-cluster · `cluster-0309`
  - 4 nodes / 3 edges — Detect spots -> Filter spots -> Convert spots to ROI -> Convert 2D ROI to RGB
  - Takes an image, detects and filters spots using TrackMate, converts the spots into 2D ROIs, and renders those ROIs as an RGB mask image.
- **Segment 2D images into 2D ROIs** · support 1 · compartment · `cluster-0316`
  - 5 nodes / 4 edges — Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological operation 2D -> Morphological hole filling 2D -> Find particles 2D
  - Takes 2D images, smooths and thresholds them, cleans the binary mask with morphology and hole filling, then extracts connected components as 2D ROIs with measurements.
- **Detect 2D particles as 2D ROIs** · support 1 · canvas-note-section · `cluster-0320`
  - 6 nodes / 5 edges — Apply ImageJ auto-contrast per slice -> Median filter 2D -> Laplacian of Gaussian 2D -> Fast image arithmetics 2D -> Auto threshold 2D (8-bit) -> Find partic…
  - Takes 2D images and produces particle 2D ROIs by enhancing contrast, denoising with a median filter, applying a Laplacian-of-Gaussian filter, thresholding, and extracting connected components with measurements.

### filaments (14)

- **Extract and measure 2D filaments from images** · support 8 · canvas-note-section · `cluster-0034`
  - 12 nodes / 11 edges — Morphological operation 2D -> Morphological operation 2D -> Gaussian blur 2D -> Auto threshold 2D (8-bit) -> Morphological skeletonize 2D -> Set to value (gr…
  - Takes 2D grayscale images, applies morphological filtering, Gaussian blur, auto-thresholding, and skeletonization to produce 2D filaments, then filters and measures filament components. It outputs a merged table of filament measurements and annotations, along with a 3D ROI mask of the filaments.
- **Detect and highlight line-like structures in 2D images** · support 7 · geometric-cluster · `cluster-0036`
  - 9 nodes / 8 edges — Folder list -> Import image -> Annotate with image properties -> Split & filter by annotation -> Crop 2D image -> Ridge detector -> Gaussian blur 2D -> Histo…
  - Produces a 2D image in which line-like or ridge structures are highlighted. It takes image files from folders, annotates and filters them by image properties, crops the selected images, detects ridges as a binary line mask, blurs and contrast-enhances the mask, and combines it with the cropped image.
- **Extract and render 2D filaments from binary images** · support 5 · geometric-cluster · `cluster-0055`
  - 7 nodes / 7 edges — File list -> Import image -> Morphological skeletonize 2D -> Binary skeleton to 2D filaments -> Smooth filaments -> Set filament vertex radius from image -> …
  - Takes image files, skeletonizes each binary image, converts the skeleton into smoothed 2D filaments with vertex radii sampled from the image, and outputs an RGB image with the filaments rendered over the original image.
- **Split filaments into connected 3D ROIs** · support 5 · geometric-cluster · `cluster-0063`
  - 3 nodes / 1 edges — Convert filaments to mask -> Split filaments into connected components -> Convert filaments to mask
  - Takes filament data, separates it into connected components, and outputs each component as a 3D ROI mask.
- **Detect 3D ROIs and create 2D skeletons** · support 4 · geometric-cluster · `cluster-0066`
  - 12 nodes / 12 edges — Filter paths -> Import image stack -> Set physical dimensions -> Auto threshold 2D (8-bit) -> Morphological operation 3D -> Gaussian blur 3D -> Auto threshol…
  - Takes image stacks from a filtered folder, sets physical dimensions, thresholds and cleans the images, finds 3D particle ROIs, converts the 3D ROIs to masks, and outputs 2D skeletonized slice images.
- **Extract filament 3D ROI masks from Cellpose probability images** · support 4 · canvas-note-section · `cluster-0082`
  - 10 nodes / 10 edges — Pretrained Cellpose 2.x model -> Cellpose segmentation (2.x) -> Auto threshold 2D (8-bit) -> Euclidean distance transform 2D -> Morphological skeletonize 2D …
  - Takes a Cellpose probability image and produces a two-color 3D ROI mask of filament-like structures by auto-thresholding the probability, skeletonizing each 2D slice, converting the skeleton to 2D filaments, setting vertex radii from a Euclidean distance transform, smoothing, and converting the filaments to a 3D ROI.
- **Measure and plot filament components** · support 4 · canvas-note-section · `cluster-0084`
  - 10 nodes / 9 edges — Split filaments into connected components -> Filter filament components -> Find linear filament vertex order/distance -> Measure filament vertices -> Filter …
  - Takes filament data, splits it into connected components, filters components, orders linear vertices, and stores vertex measurements in a table, then produces multiple XY scatter plots of the filtered measurements.
- **Extract and clean 2D filaments from binary images** · support 3 · compartment · `cluster-0135`
  - 8 nodes / 7 edges — Euclidean distance transform 2D -> Morphological skeletonize 2D -> Binary skeleton to 2D filaments -> Smooth filaments -> Fix overlapping filaments (non-bran…
  - Takes a binary image and produces cleaned 2D filaments by skeletonizing the mask, smoothing and separating merged non-branching filaments, assigning vertex radii from a Euclidean distance transform, and filtering out border or unwanted components.
- **Detect line structures in a selected image channel** · support 2 · geometric-cluster · `cluster-0179`
  - 7 nodes / 5 edges — Folder list -> Bio-Formats importer -> Annotate with image properties -> Filter by annotation -> Split channels -> Find or subtract background 2D -> Ridge de…
  - Takes a folder of multichannel images, imports and filters them by image properties, splits channels, and applies 2D background subtraction and ridge detection to a selected channel. It produces a binary mask of detected line/ridge structures.
- **Create filament measurement table** · support 2 · compartment · `cluster-0219`
  - 3 nodes / 2 edges — Measure filament components -> Remove table column -> Rename table column
  - Takes filament components and produces a table of per-component measurements such as length, vertex and edge counts, centroid, and radius. It then removes and renames selected columns to tidy the measurement table.
- **Repair merged filaments and create 3D ROIs** · support 1 · geometric-cluster · `cluster-0259`
  - 3 nodes / 2 edges — Fix overlapping filaments (non-branching) -> Set 2D overlay -> Convert filaments to 3D ROI
  - Takes filament objects, separates filaments that have been merged by junctions (assuming non-branching filaments), and produces a 3D ROI for the repaired filaments along with a 2D ROI overlay.
- **Render filaments as a labels image** · support 1 · group-node · `cluster-0278`
  - 3 nodes / 3 edges — Group input -> Convert filaments to labels (legacy) -> Group output
  - Takes filament objects and a reference image, and produces a labels image where each filament is rendered as a distinct label.
- **Quantify filament components** · support 1 · canvas-note-section · `cluster-0285`
  - 8 nodes / 7 edges — Measure filament components -> Remove table column -> Add annotations as columns -> Rename table column -> Apply expression per row -> Filter table rows -> M…
  - Takes a filament graph and produces a per-component measurement table with properties such as length, vertex/edge counts, radius, and centroid. It then tidies, filters, and merges the table and exports the final metrics to CSV/XLSX.
- **Visualize filaments with 2D text ROIs** · support 1 · canvas-note-section · `cluster-0292`
  - 8 nodes / 7 edges — Table to 2D text ROI -> Convert filaments to labels (legacy) -> Convert labels to RGB -> Blend images -> Export image -> Export image -> Convert 2D ROI to RG…
  - Takes filament objects and a table of text annotations, converts the filaments to a colored labels image, blends it with an input image, and renders the 2D text ROIs as an RGB overlay, exporting the resulting images.

### machine-learning (21)

- **Segment objects into 2D ROIs and RGB masks** · support 31 · geometric-cluster · `cluster-0004`
  - 4 nodes / 3 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB
  - Takes an image and a pretrained Cellpose model, runs Cellpose segmentation to produce 2D ROIs, filters those 2D ROIs by statistical criteria, and converts the remaining 2D ROIs into an RGB mask image using a reference image for output dimensions.
- **Segment 2D/3D images into 2D/3D ROIs with Cellpose** · support 21 · geometric-cluster · `cluster-0009`
  - 3 nodes / 2 edges — File list -> Import Cellpose model from file -> Cellpose segmentation (3.x)
  - Takes Cellpose model file paths and image data, imports the model(s), runs Cellpose inference, and produces segmented cell ROIs (2D or 3D) plus optional label/probability images.
- **Cellpose cell segmentation to 2D ROI masks** · support 20 · canvas-note-section · `cluster-0010`
  - 7 nodes / 6 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Filter 2D ROI by statistics -> Convert 2D ROI to RGB -> Convert 2D ROI to mask -…
  - Takes input images and a pretrained Cellpose model, runs Cellpose to segment cells, filters the resulting 2D ROIs by statistics, converts them to RGB and mask images, cleans the masks with a 2D morphological operation, and merges the cleaned slices into a hyperstack.
- **Create Cellpose 2D ROI masks from images** · support 7 · geometric-cluster · `cluster-0037`
  - 9 nodes / 9 edges — Ridge detector -> File list -> Import Cellpose model from file -> Gaussian blur 2D -> Histogram-based contrast enhancer -> Fast image arithmetics 2D -> Cellp…
  - Takes image data and a Cellpose model file, preprocesses the images with ridge detection, Gaussian blur, contrast enhancement, and arithmetic operations, then runs Cellpose inference to produce 2D ROIs and converts those ROIs into RGB mask images.
- **Segment cells into 2D ROI masks** · support 7 · geometric-cluster · `cluster-0038`
  - 3 nodes / 2 edges — Morphological operation 2D -> Cellpose prediction (2.x) -> Convert 2D ROI to RGB
  - Takes microscopy images and produces cell segmentation masks by applying a 2D morphological operation, running Cellpose to generate 2D ROIs, and converting those ROIs into an RGB mask image.
- **Retrain and test a cell segmentation model to produce 2D ROIs** · support 6 · canvas-note-section · `cluster-0044`
  - 12 nodes / 11 edges — Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation training (3.x) -> Remove annotation -> Cellpose segmentation (3.x) -> Gaussian blur 2D ->…
  - Takes annotated images and a pretrained Cellpose model, trains a new segmentation model, removes annotations, runs inference, and converts the probability output into 2D ROIs and mask images.
- **Create 2D ROI masks from Cellpose segmentation** · support 6 · canvas-note-section · `cluster-0046`
  - 6 nodes / 5 edges — Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x segmentation model -> Cellpose segmentation (3.x) -> Cellpose segmentation (3.x) -> Con…
  - Takes input images and pretrained Cellpose 3.x models, runs Cellpose segmentation to produce 2D ROIs, and converts those ROIs into RGB mask images. It can apply multiple models and generate corresponding 2D ROI masks.
- **Train a cell segmentation model from labeled 2D images** · support 6 · geometric-cluster · `cluster-0050`
  - 7 nodes / 7 edges — Illumination correction 2D -> Scale 2D image -> Annotate with data -> Split data randomly (percentage) -> Image to OME Image -> Cellpose training (2.x) -> Im…
  - Takes 2D images and matching label images, normalizes and resizes them, randomly splits the annotated data into training, validation, and test sets, and trains a Cellpose model. It produces a trained cell segmentation model and OME-converted image data.
- **Cellpose 2D ROI cell segmentation with probability overlay** · support 6 · geometric-cluster · `cluster-0053`
  - 4 nodes / 3 edges — Pretrained Cellpose 2.x model -> Cellpose segmentation (2.x) -> Auto threshold 2D (8-bit) -> Blend images
  - Takes an image, imports a pretrained Cellpose 2.x model, and runs Cellpose inference to obtain a cell probability image and 2D ROIs of segmented cells. It then applies 2D auto-thresholding to the probability image and blends the thresholded mask with the image to produce a visual cell-segmentation overlay.
- **Segment 2D cell ROIs and detect spots** · support 5 · geometric-cluster · `cluster-0057`
  - 7 nodes / 7 edges — Split by dimension -> Filter by annotation -> Cellpose prediction (2.x) -> Convert 2D ROI to mask -> Merge 2D slices into hyperstack -> Detect spots -> Filte…
  - Produces filtered spots from an annotated image stack by segmenting selected 2D planes with Cellpose. It splits the stack into 2D slices, converts the Cellpose 2D ROIs to masks, merges the masks back into a stack, and runs TrackMate spot detection with expression filtering.
- **Train and test a Cellpose model, producing 2D ROI masks** · support 4 · compartment · `cluster-0081`
  - 10 nodes / 11 edges — Remove overlay -> File list -> Import Cellpose model -> Annotate with data -> Split data randomly (percentage) -> Cellpose training (2.x) -> Cellpose predict…
  - Takes 2D image data and a Cellpose model file, removes overlay ROIs, annotates the images with companion data, randomly splits them into training, validation, and test sets, trains and exports a Cellpose model, predicts on the test set, and converts the predicted 2D ROIs into RGB mask images.
- **Segment objects with Cellpose and create 2D ROI masks** · support 4 · canvas-note-section · `cluster-0093`
  - 12 nodes / 14 edges — Divide by maximum -> Pretrained Cellpose 3.x segmentation model -> Pretrained Cellpose 3.x segmentation model -> CLIJ2 Gaussian Blur 2D -> Cellpose segmentat…
  - Takes a greyscale image, normalizes and blurs it, runs pretrained Cellpose segmentation, filters and enlarges the resulting 2D ROIs, and converts them to masks. It produces 2D ROI masks and combined mask images for downstream analysis.
- **Two-pass Cellpose segmentation to 2D ROI masks** · support 4 · canvas-note-section · `cluster-0097`
  - 7 nodes / 6 edges — File list -> Pretrained Cellpose 3.x segmentation model -> Import Cellpose model from file -> Cellpose segmentation (3.x) -> Cellpose segmentation (3.x) -> F…
  - Takes image data and Cellpose model files or a pretrained model, runs Cellpose inference twice by feeding the first pass's flow output into the second pass, filters the resulting 2D ROIs by statistics, and converts the filtered 2D ROIs into an RGB mask image.
- **Segment cells with Cellpose and create 3D ROIs** · support 4 · canvas-note-section · `cluster-0104`
  - 12 nodes / 12 edges — Set to color (RGB) -> Pretrained Cellpose 3.x segmentation model -> Divide by maximum -> Set LUT (color map) -> Histogram-based contrast enhancer -> Cellpose…
  - Takes an input image, normalizes and enhances it, runs a pretrained Cellpose model to segment cells, and produces a 3D ROI list of the segmented objects. It converts the Cellpose 2D ROIs into a mask and RGB overlay, applies 2D morphological cleanup, and labels connected components in 3D.
- **Segment images into 2D/3D ROIs with Omnipose** · support 3 · geometric-cluster · `cluster-0130`
  - 3 nodes / 2 edges — File list -> Import image -> Omnipose prediction (0.x)
  - Takes a list of image file paths, imports each image, and runs Omnipose to segment cell-like objects, producing 2D/3D ROIs of the segmented areas (with optional label, flow, and probability outputs).
- **Train and predict with a generative model** ⚠ unregistered id · support 3 · geometric-cluster · `cluster-0134`
  - 3 nodes / 2 edges — Create generation model (JIPipe2AI) -> JIPipe2AI train generation -> JIPipe2AI predict generation
  - Creates a generative model, trains it, and then uses the trained model to produce predictions.
- **Segment cells with Cellpose and annotate 2D ROIs** · support 2 · geometric-cluster · `cluster-0159`
  - 5 nodes / 6 edges — Merge channels (classic) -> Histogram-based contrast enhancer -> Cellpose segmentation (3.x) -> Set 2D overlay -> Annotate with 2D ROI properties
  - Takes multi-channel grayscale images, merges and contrast-enhances them, runs Cellpose segmentation, and produces a 2D ROI overlay plus annotations containing 2D ROI properties.
- **Classify pixels in images using Ilastik** · support 2 · geometric-cluster · `cluster-0214`
  - 4 nodes / 3 edges — File list -> Annotate with file/directory name -> Import image -> Ilastik pixel classification
  - Takes a list of image file paths, imports each image while preserving its file/directory name as an annotation, and runs Ilastik pixel classification to output pixel labels for each image.
- **Train and predict image segmentation** ⚠ unregistered id · support 2 · compartment · `cluster-0216`
  - 5 nodes / 5 edges — Create segmentation model (JIPipe2AI) -> Annotate with data -> Split data randomly (percentage) -> JIPipe2AI train segmentation -> JIPipe2AI predict segmenta…
  - Takes image data and annotation data, attaches the annotations to the images, randomly splits the annotated data into training and validation sets, trains a segmentation model, and produces the trained model along with predicted segmentations.
- **Train image model from labeled images** ⚠ unregistered id · support 1 · geometric-cluster · `cluster-0254`
  - 6 nodes / 6 edges — File list -> Annotate with file/directory name -> Import image -> Annotate with data -> Split data randomly (percentage) -> Train model (labeled images)
  - Takes a list of image files, labels each image from its file name, randomly splits the images into training and test sets, and trains a deep-learning image model.
- **Segment images with a Labkit model** · support 1 · geometric-cluster · `cluster-0265`
  - 7 nodes / 6 edges — File list -> Folder list -> List files -> Import JSON -> Annotate with file/directory name -> Import image -> Labkit inference
  - Takes a folder of images and a Labkit model JSON file, loads each image, and runs Labkit inference to produce segmented images or probability maps.

### tracking (7)

- **Detect and track objects, then summarize measurements** · support 14 · canvas-note-section · `cluster-0016`
  - 8 nodes / 7 edges — LAP Tracker -> Mask detector -> Detect spots -> Annotate with source slot -> Track spots -> Filter tracks -> Measure tracks -> JFreeChart Histogram plot
  - Takes image data (optionally with a mask) and produces tracked objects plus a histogram of track measurements. It detects spots with a mask detector, tracks them with TrackMate, filters and measures tracks, and plots a histogram of the measurements.
- **Track and measure spots from 2D ROI masks** · support 12 · canvas-note-section · `cluster-0021`
  - 11 nodes / 10 edges — Mask detector -> LAP Tracker -> Convert 2D ROI to mask -> Annotate with source slot -> Morphological operation 2D -> Merge hyperstack C/Z/T (single input) ->…
  - Takes 2D ROIs, converts them to masks, applies a 2D morphological operation, and merges the masks into a hyperstack for TrackMate spot detection. It then tracks spots with a LAP tracker, filters and measures tracks, and produces a histogram of track measurements.
- **Track, filter, and annotate spots** · support 3 · geometric-cluster · `cluster-0116`
  - 4 nodes / 3 edges — Track spots -> Filter tracks -> Rename annotation -> Set annotation (expression)
  - Produces filtered, labeled tracks from spot detections. It runs TrackMate tracking, filters tracks by expression, and renames/sets annotations on the resulting tracks.
- **Track filaments and create a 3D ROI** · support 3 · geometric-cluster · `cluster-0126`
  - 3 nodes / 3 edges — Z-Project (classic) -> Find and track filaments 3D+T (TSOAX) -> Convert filaments to mask
  - Takes a 3D image, creates a Z-projection, extracts and tracks filaments with TSOAX, and converts the tracked filaments into a 3D ROI mask.
- **Track measurement summary** · support 2 · geometric-cluster · `cluster-0138`
  - 11 nodes / 11 edges — Measure tracks -> Apply expression to columns -> JFreeChart Histogram plot -> Annotate data with table values -> Export image -> Add table column -> Table co…
  - Takes a set of tracks, measures them, applies column expressions and adds derived columns, then generates histogram and line plots exported as images and measurement tables exported as multi-sheet XLSX files.
- **Track spots in image sequences** · support 1 · geometric-cluster · `cluster-0244`
  - 6 nodes / 5 edges — File list -> Import image -> Detect spots -> Filter spots -> Track spots -> Filter tracks
  - Takes a list of image files, imports the images, detects and filters spots, then links the spots into tracks and filters the resulting tracks. Produces filtered spot tracks for the input image sequence.
- **RGB track visualization and branch statistics** · support 1 · geometric-cluster · `cluster-0311`
  - 4 nodes / 3 edges — Visualize tracked spots -> Branch analysis -> Merge channels (classic) -> Combine channel slices to RGB
  - Takes tracked spots, renders each track over time, and merges the rendered images into RGB, while also outputting a table of branch predecessor/successor counts and durations.

### tables (6)

- **Filter data by label** · support 5 · geometric-cluster · `cluster-0062`
  - 3 nodes / 0 edges — Filter by annotation (If else) -> Filter by annotation (If else) -> Filter by annotation (If else)
  - Splits a data set into subsets based on annotation values. Each of the three filters outputs the data that matches the annotation condition and the data that does not.
- **Merge two tables after standardizing column names** · support 3 · canvas-note-section · `cluster-0111`
  - 4 nodes / 3 edges — Rename & modify columns (expression) -> Remove table column -> Rename table column -> Merge table columns (simple)
  - Takes two tabular datasets, renames or removes columns so their fields have consistent names, and merges them into a single table with the combined columns.
- **Clean and reorder a measurement table** · support 3 · canvas-note-section · `cluster-0112`
  - 3 nodes / 2 edges — Remove table column -> Apply expression per row -> Sort table columns
  - Takes a table of measurements, removes unwanted columns, adds a computed value for each row, and sorts the columns, producing a cleaned and reordered table.
- **Select, sort, and rename table data** · support 2 · geometric-cluster · `cluster-0165`
  - 3 nodes / 2 edges — Select table rows -> Sort table rows -> Rename table column
  - Takes a table, keeps selected rows, orders the rows by chosen columns, and renames a column. It produces a cleaned, reordered table with clearer column names.
- **Import results tables and fill missing series rows** · support 1 · geometric-cluster · `cluster-0260`
  - 3 nodes / 2 edges — File list -> Import results table -> Add missing rows (series)
  - Takes a list of file paths, imports CSV or XLSX results tables from those files, and inserts missing rows for a numeric series such as time points so the output table is complete.
- **Pixel table from math expression** · support 1 · geometric-cluster · `cluster-0306`
  - 4 nodes / 3 edges — Generate image from math expression -> Get pixels as table -> Rename table column -> Remove table column
  - Takes a mathematical expression, generates an image by applying it to each pixel, then extracts the pixel values into a table and renames/removes columns to produce a cleaned pixel table.

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
- **Measure total edge length in 2D binary images** · support 4 · canvas-note-section · `cluster-0106`
  - 5 nodes / 4 edges — Morphological skeletonize 2D -> Extract image statistics (Expression) -> Merge table rows -> Apply expression per row -> Export table as multi-sheet XLSX
  - Takes 2D binary images, skeletonizes each slice, extracts image statistics, merges the rows, applies an expression to compute total edge length, and exports the results as a multi-sheet XLSX table.
- **Edge width histogram from images** · support 4 · canvas-note-section · `cluster-0108`
  - 9 nodes / 8 edges — Local maxima 2D -> Auto threshold 2D (8-bit) -> Set to value (grayscale) -> Image to pixel value/count histogram -> Filter table rows -> JFreeChart XY line p…
  - Takes images and produces a histogram, plot, and XLSX table summarizing edge widths by finding local maxima, auto-thresholding them, converting the threshold mask to a grayscale value image, and filtering the resulting value-count histogram.
- **3D ROI nearest-neighbor distance histogram** · support 3 · canvas-note-section · `cluster-0121`
  - 7 nodes / 6 edges — IJ3D Find particles 3D -> Extract IJ3D ROI statistics -> Export data -> Run Python script (iterating) -> JFreeChart Histogram plot -> Export data -> Export i…
  - Takes a mask image, finds 3D particles as 3D ROIs, extracts 3D ROI statistics, and runs a Python script to compute nearest-neighbor distances. It produces a histogram of those distances and exports the measurement table and plot image.
- **Histogram of pairwise 2D ROI measurements** · support 2 · compartment · `cluster-0137`
  - 4 nodes / 4 edges — Convert spots to ROI -> Extract pairwise 2D ROI statistics -> JFreeChart Histogram plot -> Export image
  - Takes TrackMate spots, converts them to 2D ROIs, and extracts all pairwise 2D ROI statistics. It then plots a histogram of those measurements and exports the histogram as an image.
- **Compute and plot 2D ROI image statistics** · support 2 · canvas-note-section · `cluster-0162`
  - 4 nodes / 3 edges — Remove annotation -> Extract image statistics -> JFreeChart XY line plot -> Export image
  - Takes images with 2D ROIs, removes selected annotations, extracts image statistics for each ROI, and exports an XY line plot of the measurements.
- **Measure tracked objects and export histograms** · support 2 · compartment · `cluster-0197`
  - 11 nodes / 8 edges — Measure spots -> Measure tracks -> Measure edges -> JFreeChart Histogram plot -> Iteration step slicer -> Iteration step slicer -> Export image -> JFreeChart…
  - Takes TrackMate spot, track, and edge data, computes measurement tables, and exports histogram images of track measurements.
- **Export image statistics as a table** · support 1 · geometric-cluster · `cluster-0235`
  - 5 nodes / 4 edges — Extract image statistics -> Add annotations as columns -> Merge table rows -> Rename table column -> Export data (path input)
  - Takes image data and annotation columns, extracts whole-image or masked statistics, merges the resulting rows into one table, renames columns, and exports the table to a specified output path.
- **2D distance transform histogram** · support 1 · geometric-cluster · `cluster-0263`
  - 3 nodes / 3 edges — Generate image from math expression -> Euclidean distance transform 2D -> Key/Value Histogram 5D
  - Produces a histogram of distances from a 2D binary mask. It applies a pixel-wise math expression to an image, computes a 2D Euclidean distance transform, and bins the resulting distance values.
- **Filter data by median 2D ROI size** · support 1 · canvas-note-section · `cluster-0281`
  - 4 nodes / 3 edges — Find particles 2D -> Summarize table -> Annotate data with table values -> Filter by annotation (If else)
  - Produces a data stream filtered by the median size of 2D ROIs found in thresholded images. It finds 2D ROIs from the images, summarizes their measurements to a median value, annotates the incoming data with that value, and splits the data into matching and non-matching outputs.
- **Estimate thickness of binary objects** · support 1 · canvas-note-section · `cluster-0290`
  - 12 nodes / 12 edges — Split & filter by annotation -> Euclidean distance transform 2D -> Get pixels as table -> Filter table rows -> JFreeChart Histogram plot -> Summarize table -…
  - Takes binary images, splits them by annotation, computes a 2D Euclidean distance transform, and summarizes the resulting distance values into thickness statistics and a histogram. It converts the summary into parameters that can drive a 2D morphological operation.

### plotting (15)

- **Export histogram plot as image** · support 5 · geometric-cluster · `cluster-0059`
  - 3 nodes / 2 edges — JFreeChart Histogram plot -> Create montage -> Export image
  - Takes tabular data, renders a histogram plot, passes it through a montage step to form an image, and exports that image to a standard file format.
- **Plot histogram of filament measurements** · support 4 · canvas-note-section · `cluster-0069`
  - 4 nodes / 3 edges — Measure filament components -> Filter tables -> Filter table rows -> JFreeChart Histogram plot
  - Takes a filament graph, measures each filament component (including length, vertex/edge counts, radii, and centroid properties), filters the resulting measurement table, and produces a histogram of the selected measurement.
- **Merge count tables and export summary plots** · support 4 · canvas-note-section · `cluster-0073`
  - 11 nodes / 10 edges — Merge table columns (simple) -> Merge table rows -> Apply expression per row -> JFreeChart Statistical line category plot -> Annotate with source slot -> JFr…
  - Takes multiple count tables, merges their columns and rows, applies a per-row expression, and creates line and bar category plots. It adds labels identifying the originating input table and exports the computed table and plot images.
- **Plot tabular data with scatter, box, and line charts** · support 4 · geometric-cluster · `cluster-0074`
  - 3 nodes / 0 edges — JFreeChart XY scatter plot -> JFreeChart Box plot -> JFreeChart XY line plot
  - Takes tabular data and produces an XY scatter plot, a box-and-whisker plot, and an XY line plot for visualizing numeric variables.
- **Summarize and plot table data as an XY line chart** · support 4 · geometric-cluster · `cluster-0079`
  - 4 nodes / 3 edges — Summarize table -> Add annotations as columns -> Merge table rows -> JFreeChart XY line plot
  - Takes one or more tables, computes summary statistics for selected columns, adds annotation values as columns, merges the resulting rows, and creates an XY line plot of the summarized data.
- **Plot pixel values from a masked image** · support 4 · canvas-note-section · `cluster-0107`
  - 9 nodes / 8 edges — Morphological operation 2D -> Set to color (RGB) -> Get pixels as table -> Filter table rows -> Apply expression per row -> JFreeChart XY scatter plot -> JFr…
  - Takes an image, applies a 2D morphological operation to create a mask, sets the masked pixels to a color, extracts the pixel values into a table, filters and computes row expressions, and produces multiple XY scatter plots.
- **Plot a histogram from table data** · support 3 · compartment · `cluster-0113`
  - 12 nodes / 9 edges — Rename & modify columns (expression) -> Remove table column -> Remove table column -> Annotate data with table values -> Rename table column -> Apply express…
  - Takes one or more tables, cleans and renames columns, applies row-level expressions, merges rows, and produces a histogram of the resulting values.
- **Grouped histograms from multiple tables** · support 3 · geometric-cluster · `cluster-0124`
  - 5 nodes / 4 edges — Merge table columns (simple) -> Add annotations as columns -> Merge table rows -> Split table by columns -> JFreeChart Histogram plot
  - Takes multiple tables, merges their columns and rows, adds annotation columns, splits the combined table by selected columns, and produces a histogram plot for each resulting group.
- **Export 2D ROI measurement histograms** · support 2 · geometric-cluster · `cluster-0154`
  - 5 nodes / 4 edges — Extract 2D ROI statistics -> JFreeChart Histogram plot -> Convert image to RGB -> Create montage -> Export image
  - Takes 2D ROI statistics, plots their measurements as histograms, converts the plots to RGB, combines them into a montage, and exports the montage as an image.
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
- **Plot histogram from category/value images** · support 1 · geometric-cluster · `cluster-0262`
  - 4 nodes / 3 edges — Key/Value Histogram 5D -> Merge table columns (simple) -> Apply expression per row -> JFreeChart XY line plot
  - Takes two same-sized images that store a category and a numeric value for each pixel, builds a histogram table, applies row-level calculations, and produces an XY line plot.
- **Create and export a histogram from merged tables** · support 1 · canvas-note-section · `cluster-0301`
  - 3 nodes / 2 edges — Merge table rows -> JFreeChart Histogram plot -> Export image
  - Takes multiple tables, merges their rows into a single table, creates a histogram plot of a numeric column, and exports the plot as an image file.

### export (16)

- **Export merged summary tables as XLSX** · support 8 · compartment · `cluster-0026`
  - 5 nodes / 2 edges — Export table as multi-sheet XLSX -> Merge table rows -> Add annotations as columns -> Merge table rows -> Export table as multi-sheet XLSX
  - Takes multiple result tables, merges their rows into summary tables, adds column annotations as extra columns, and writes the result to a multi-sheet XLSX workbook.
- **Export cleaned annotation table** · support 6 · geometric-cluster · `cluster-0047`
  - 11 nodes / 10 edges — Convert to annotation table -> Remove table column -> Remove table column -> Remove table column -> Remove table column -> Remove table column -> Remove tabl…
  - Takes data rows with annotations, converts them into a flat annotation table, removes selected columns and applies a Python table modification, then exports the resulting table to CSV/XLSX.
- **Save result tables and images** · support 6 · compartment · `cluster-0051`
  - 11 nodes / 0 edges — Export table -> Export table -> Export image -> Export table -> Export image -> Export image -> Export table -> Export table -> Export image -> Export table …
  - Takes result tables and processed images and writes them to external files. It exports tables as CSV/XLSX and images as common formats such as PNG, TIFF, JPEG, BMP, or AVI.
- **Export 3D ROIs as a label image** · support 4 · geometric-cluster · `cluster-0071`
  - 6 nodes / 4 edges — Set IJ3D ROI properties -> Run Python script (iterating) -> Create empty image -> Convert IJ3D ROI to RGB -> Copy physical dimensions -> Export image
  - After setting 3D ROI properties, it converts the 3D ROIs into an RGB label image on an empty image, copies physical pixel dimensions, and exports the result as an image file.
- **Export multiple images with input labels** · support 4 · geometric-cluster · `cluster-0076`
  - 7 nodes / 6 edges — IO Interface -> Annotate with source slot -> Annotate with source slot -> Annotate with source slot -> Export image -> Export image -> Export image
  - Takes multiple image inputs and writes each one out as an image file, adding the input's label to the exported data. This is useful for saving several image channels or views separately while keeping track of which input each file came from.
- **Export computed results and histogram image** · support 4 · canvas-note-section · `cluster-0090`
  - 5 nodes / 4 edges — Run Python script (iterating) -> JFreeChart Histogram plot -> Export data -> Render to RGB -> Export image
  - Exports the output of an iterating Python script as a data file and a histogram image. The script's table is plotted as a histogram, rendered to RGB, and saved, while the table is also exported directly.
- **Export contrast-enhanced images** · support 3 · geometric-cluster · `cluster-0122`
  - 3 nodes / 2 edges — Copy physical dimensions -> Histogram-based contrast enhancer -> Export image
  - Takes input images, copies their physical pixel sizes, applies histogram-based contrast enhancement, and writes them out as standard image files such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export 3D mask as 3D mesh** · support 2 · geometric-cluster · `cluster-0136`
  - 3 nodes / 2 edges — Generate structure element (3D) -> Mask to 3D scene -> Export 3D scene
  - Takes a 3D mask, generates a 3D structure element, converts the mask into a 3D mesh using marching cubes, and writes the mesh to a Collada DAE file.
- **Export image montage** · support 2 · geometric-cluster · `cluster-0155`
  - 3 nodes / 2 edges — Create montage -> Render to RGB -> Export image
  - Takes a set of input images, combines them into a single montage, renders the result to RGB while preserving LUT/contrast settings, and writes it out as an image file such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export corrected and enhanced 2D images** · support 2 · geometric-cluster · `cluster-0196`
  - 4 nodes / 3 edges — Illumination correction 2D -> Histogram-based contrast enhancer -> Set LUT (color map) -> Export image
  - Produces export-ready 2D images by applying illumination correction and histogram-based contrast enhancement, then setting a color map. It writes the processed images to common file formats such as PNG, JPEG, BMP, AVI, or TIFF.
- **Export 2D image visualizations with 2D ROIs** · support 2 · compartment · `cluster-0218`
  - 11 nodes / 9 edges — Set 2D overlay -> JFreeChart Histogram plot -> Morphological operation 2D -> Connected components labeling 2D -> Render overlay -> Convert labels to RGB -> S…
  - Takes images, 2D ROIs, masks, and label images; renders the 2D ROIs, colors connected-component labels, applies a morphological mask, blends the layers, and combines them with a histogram plot into a montage. It exports the resulting 2D visualization images to standard file formats.
- **Export colored 3D ROIs as a 3D scene** · support 1 · geometric-cluster · `cluster-0227`
  - 3 nodes / 2 edges — Color IJ3D ROI by statistics -> IJ3D ROI to 3D scene -> Export 3D scene
  - Takes 3D ROIs with measurement values, colors each ROI by its statistics, converts the ROIs into a 3D scene, and exports the scene as a Collada (DAE) file.
- **Upload images and tables to OMERO** · support 1 · geometric-cluster · `cluster-0274`
  - 3 nodes / 2 edges — Create OMERO dataset -> Upload image to OMERO dataset -> Attach table to OMERO image
  - Takes an image and a table, creates an OMERO dataset, uploads the image into it, and attaches the table to that image. The result is an OMERO image with its associated table available for sharing.
- **Upload images from a folder to OMERO** · support 1 · geometric-cluster · `cluster-0275`
  - 5 nodes / 4 edges — Folder list -> List files -> Annotate with file/directory name -> Import image -> Upload image to OMERO dataset
  - Uploads all images in a folder to an OMERO dataset. It lists files in the folder, annotates each path with its file name, imports each image, and sends it to the OMERO dataset.
- **Export 2D ROI overlay and mask images** · support 1 · geometric-cluster · `cluster-0296`
  - 7 nodes / 7 edges — Merge 2D ROI lists -> Remove annotation -> Set 2D overlay -> Render overlay -> Convert 2D ROI to mask -> Export image -> Export image
  - Takes images and 2D ROI lists, merges the ROI lists, removes selected annotations, draws the ROIs onto the images, and converts the ROIs to binary masks. It exports both the rendered overlay images and the mask images to standard image files.
- **Export OME-ZARR images from folders** ⚠ unregistered id · support 1 · geometric-cluster · `cluster-0318`
  - 3 nodes / 2 edges — Folder list -> Import OME ZARR from directory -> Export OME ZARR
  - Takes folder paths containing OME-ZARR datasets, imports each dataset as image data, and writes the result out as OME-ZARR files or directories.

### misc (1)

- **Generate parameter combinations** · support 12 · group-node · `cluster-0020`
  - 6 nodes / 4 edges — Define multiple parameters -> Define multiple parameters -> Define multiple parameters -> Group input -> Merge parameters -> Group output
  - Takes several parameter definitions and merges/multiplies them into one parameter set, producing the combinations needed by a multi-parameter algorithm.

## Rejected (false-negative check)

57 families the model cut. The high-support / good-title ones
are where a false negative would hide.

- `cluster-0068` support 4 — **gate=noise** — 4n/3e
- `cluster-0075` support 4 — **gate=noise** — 4n/0e
- `cluster-0103` support 4 — **gate=workflow_plumbing** — 12n/11e
  - titles: ['Green mask']
- `cluster-0105` support 4 — **gate=noise** — 8n/4e
- `cluster-0109` support 4 — **gate=noise** — 4n/3e
  - titles: ['Combined methods: NOT USED']
- `cluster-0143` support 2 — **gate=noise** — 7n/4e
- `cluster-0144` support 2 — **gate=noise** — 7n/4e
- `cluster-0149` support 2 — **gate=workflow_plumbing** — 4n/3e
- `cluster-0150` support 2 — **gate=workflow_plumbing** — 9n/8e
- `cluster-0151` support 2 — **gate=workflow_plumbing** — 11n/10e
- `cluster-0157` support 2 — **gate=workflow_plumbing** — 3n/0e
  - titles: ['Peroxisomal GFP']
- `cluster-0160` support 2 — **gate=noise** — 7n/3e
- `cluster-0163` support 2 — **gate=workflow_plumbing** — 11n/11e
  - titles: ['PredSpectra', 'ValSpectra']
- `cluster-0166` support 2 — **gate=noise** — 7n/5e
- `cluster-0167` support 2 — **gate=workflow_plumbing** — 8n/7e
- `cluster-0178` support 2 — **gate=workflow_plumbing** — 8n/7e
  - titles: ['Ganztier', 'Organe']
- `cluster-0180` support 2 — **gate=workflow_plumbing** — 11n/11e
- `cluster-0185` support 2 — **gate=noise** — 4n/1e
- `cluster-0188` support 2 — **gate=workflow_plumbing** — 12n/11e
  - titles: ['Time dependence']
- `cluster-0198` support 2 — **gate=workflow_plumbing** — 10n/8e
  - titles: ['Segmentation: nuclei']
- `cluster-0203` support 2 — **gate=workflow_plumbing** — 9n/9e
- `cluster-0205` support 2 — **gate=noise** — 3n/1e
- `cluster-0226` support 2 — **gate=workflow_plumbing** — 4n/3e
  - titles: ['Adrian Feile data']
- `cluster-0230` support 1 — **gate=workflow_plumbing** — 6n/5e
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
- `cluster-0248` support 1 — **gate=noise** — 3n/2e
- `cluster-0251` support 1 — **gate=workflow_plumbing** — 8n/7e
- `cluster-0253` support 1 — **gate=workflow_plumbing** — 7n/6e
- `cluster-0255` support 1 — **gate=workflow_plumbing** — 3n/2e
- `cluster-0261` support 1 — **gate=noise** — 4n/3e
- `cluster-0268` support 1 — **gate=workflow_plumbing** — 4n/3e
- `cluster-0270` support 1 — **gate=noise** — 3n/0e
- `cluster-0271` support 1 — **gate=noise** — 3n/0e
- `cluster-0276` support 1 — **gate=workflow_plumbing** — 9n/8e
- `cluster-0284` support 1 — **gate=workflow_plumbing** — 10n/9e
  - titles: ['Determine approx. yeast size by PBS']
- `cluster-0293` support 1 — **gate=workflow_plumbing** — 12n/7e
  - titles: ['Hyphae analysis - AnaMorf']
- `cluster-0294` support 1 — **gate=workflow_plumbing** — 11n/9e
  - titles: ['File handler [1] -1st tile', 'File handler [3] - 1th tile']
- `cluster-0298` support 1 — **gate=workflow_plumbing** — 5n/4e
- `cluster-0299` support 1 — **gate=workflow_plumbing** — 6n/5e
  - titles: ['Find CZI and metadata extraction']
- `cluster-0300` support 1 — **gate=workflow_plumbing** — 5n/4e
  - titles: ['Cell length histogram and filaments']
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

## Flags

**Same name, different graph** (legal — the browser dedups on (name, graph)):
- 'Detect and filter 2D particle ROIs' × 2: cluster-0024, cluster-0289
- 'Load images from folders' × 3: cluster-0100, cluster-0101, cluster-0228
- 'Segment 2D images into 2D ROIs' × 3: cluster-0012, cluster-0239, cluster-0316

**Mixed 2D+3D ROI graph (decision 4: keep + renamed to state the span):**
- `cluster-0104` 'Segment cells with Cellpose and create 3D ROIs'

