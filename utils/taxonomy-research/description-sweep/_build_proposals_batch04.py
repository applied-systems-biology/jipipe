#!/usr/bin/env python3
"""Build proposals-batch-04.json from batch-04.json (description sweep #1404)."""
import json, re, sys

BASE = "/data/src/jipipe-4/utils/taxonomy-research/description-sweep"

# nodeId -> (opening sentence, bucket). The opening sentence REPLACES the first
# sentence of the current description; the preserved rest follows verbatim.
OP = {
"ij1-math-extended-minima-3d": (
    "Computes the extended minima of a 3D stack as a binary mask of voxels with intensity drops below the configured dynamic threshold.",
    "gate"),
"ij1-math-generate-from-expression": (
    "Generates an image from a user-written math expression that is evaluated for each pixel.",
    "gate"),
"ij1-math-generate-missing-from-expression": (
    "Generates a replacement image with a user-written math expression when one cannot be matched to a reference in the same iteration step; matched images pass through unchanged. This algorithm groups the incoming data based on the annotations. Those groups can consist of one or multiple data item per slot. If items are missing, they will be generated according to this node's generator function. Otherwise data will be passed through.",
    "gate"),
"ij1-math-imagecalculator2d-merging": (
    "Legacy variant: applies a mathematical operation between multiple 2D images by iterating the operation in the order f(f(f(image 1, image 2), image 3), image 4), ...; the order is determined by how JIPipe calculates the merging and currently cannot be controlled. If higher-dimensional data is provided, the operation is applied to each 2D slice. This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    "gate"),
"ij1-math-impose-maxima-2d": (
    "Imposes the given maxima mask on a 2D image by repeatedly dilating it until its regional maxima fit the mask. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-math-impose-maxima-3d": (
    "Imposes the given maxima mask on a 3D volume by repeatedly dilating it until its regional maxima fit the mask. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-math-local-imagecalculator2d-expression": (
    "2D image calculator variant: evaluates a user-written expression pixel-wise to produce a single output image, with access to the current pixel values and the local areas around these pixels. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-math-math2d-expression": (
    "Applies a user-written expression to each pixel of a greyscale image.",
    "gate"),
"ij1-math-math2d-expression-color": (
    "Applies a user-written expression to each pixel of a color image.",
    "gate"),
"ij1-math-regional-maxima-2d": (
    "Returns the regional maxima of a 2D image: regions with a constant intensity value that are surrounded by lower intensity values.",
    "gate"),
"ij1-math-regional-maxima-3d": (
    "Returns the regional minima of a 3D volume: regions with a constant intensity value that are surrounded by higher intensity values.",
    "gate"),
"ij1-math-regional-minima-2d": (
    "Returns the regional minima of a 2D image: regions with a constant intensity value that are surrounded by higher intensity values.",
    "gate"),
"ij1-math-regional-minima-3d": (
    "Returns the regional minima of a 3D volume: regions with a constant intensity value that are surrounded by higher intensity values.",
    "gate"),
"ij1-math-round-32f": (
    "Rounds the values in a 32-bit float image to a specific number of decimals.",
    "gate"),
"ij1-math-vector-expression": (
    "Applies a user-written expression to each vector in the image.",
    "gate"),
"ij1-ml-labkit-segmentation": (
    "Runs Labkit inference to segment or calculate the probability map of the input images. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-binary-skeletonize3d": (
    "Performs a morphological skeletonization of a 3D binary (8-bit) image.",
    "gate"),
"ij1-morph-grayscale-attribute-filtering-2d": (
    "Grayscale attribute filtering for 2D images: removes bright or dark structures based on an attribute such as their size, preserving contours better than opening with a structuring element. Attribute filters aim at removing components of an image based on a certain size criterion, rather than on intensity. The most common and useful criterion is the number of pixels/voxels (i.e., the area or volume). For example, a morphological size opening operation with a threshold value of 20 will remove all blobs containing fewer than 20 voxels. The length of the diagonal of the bounding box can also be of interest to discriminate elongated versus round component shapes. Application to binary images: when applied to a binary image, attribute opening identifies each connected component, computes the attribute measurement of each component, and retains only the connected components whose measurement is above a specified value. This kind of processing is often used to clean-up segmentation results. Application to grayscale images: when applied to a grayscale image, attribute opening generates a series of binary images by thresholding at each distinct gray level in the image. The binary attribute opening is then applied independently to each binary image and the grayscale output is computed as the union of the binary results. The final output is a grayscale image whose bright structures with the attribute below a given value have disappeared. As for classical morphological filters, grayscale attribute closing or tophat can be defined. Grayscale attribute closing consists in removing dark connected components whose size is smaller than a specified value. White [resp. Black] Attribute Top-Hat considers the difference of the attribute opening [resp. closing] with the original image, and can help identifying bright [resp. dark] structures with small size. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-grayscale-attribute-filtering-3d": (
    "Grayscale attribute filtering for 3D images: removes bright or dark structures based on an attribute such as their size, preserving contours better than opening with a structuring element. Attribute filters aim at removing components of an image based on a certain size criterion, rather than on intensity. The most common and useful criterion is the number of pixels/voxels (i.e., the area or volume). For example, a morphological size opening operation with a threshold value of 20 will remove all blobs containing fewer than 20 voxels. The length of the diagonal of the bounding box can also be of interest to discriminate elongated versus round component shapes. Application to binary images: when applied to a binary image, attribute opening identifies each connected component, computes the attribute measurement of each component, and retains only the connected components whose measurement is above a specified value. This kind of processing is often used to clean-up segmentation results. Application to grayscale images: when applied to a grayscale image, attribute opening generates a series of binary images by thresholding at each distinct gray level in the image. The binary attribute opening is then applied independently to each binary image and the grayscale output is computed as the union of the binary results. The final output is a grayscale image whose bright structures with the attribute below a given value have disappeared. As for classical morphological filters, grayscale attribute closing or tophat can be defined. Grayscale attribute closing consists in removing dark connected components whose size is smaller than a specified value. White [resp. Black] Attribute Top-Hat considers the difference of the attribute opening [resp. closing] with the original image, and can help identifying bright [resp. dark] structures with small size. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-reconstruct-2d": (
    "Performs a morphological (geodesic) reconstruction in 2D by repeating conditional dilations or erosions until idempotence. Two images are required: the marker image, used to initialize the reconstruction, an the mask image, used to constrain the reconstruction. More information: https://imagej.net/plugins/morpholibj This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-reconstruct-3d": (
    "Performs a morphological (geodesic) reconstruction in 3D by repeating conditional dilations or erosions until idempotence. Two images are required: the marker image, used to initialize the reconstruction, an the mask image, used to constrain the reconstruction. More information: https://imagej.net/plugins/morpholibj This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
}
OP2 = {
"ij1-noise-despeckle2d": (
    "Despeckles the image by applying a median filter with radius=1. If a multi-channel image is provided, the operation is applied to each channel. If higher-dimensional data is provided, the filter is applied to each 2D slice.",
    "gate"),
"ij1-overlay": (
    "Inserts the source image into the target image, overlaying it according to a mask or ROI. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-color-statistics": (
    "Colors 2D ROIs by statistics: sets the ROI item colors from their measurements. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-convert-to-table": (
    "Converts a 2D ROI list to a table by writing the point coordinates and other metadata of the incoming ROI into a table.",
    "gate"),
"ij1-roi-crop-list": (
    "Crops each 2D ROI in the list so that its bounding rectangle is located at x=0 and y=0 (default). Also allows to crop the channel, time, and Z locations.",
    "gate"),
"ij1-roi-dimension-reorder": (
    "Reorders the dimensions of all 2D ROIs contained in the ROI lists. Unlike the equivalent method for images, this node allows to have non-unique mappings.",
    "gate"),
"ij1-roi-draw-oval": (
    "Old variant: draws one or multiple oval/ellipse ROIs. We recommend to use the 'Draw 2D rectangle/oval/line ROI' algorithm. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-draw-rectangle": (
    "Old variant: draws one or multiple rectangular ROIs. Also supports the drawing of rounded rectangles. We recommend to use the 'Draw 2D rectangle/oval/line ROI' algorithm. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-draw-scalebar": (
    "Draws a 2D scale bar ROI. Compared to 'Draw scale bar', this node allows better control over overlay. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-draw-text": (
    "Draws a 2D text ROI and adds it to an existing 2D ROI list, or starts a new list if none is provided. It positions the text relative to an optional reference image or the current ROI bounds, with font, angle, and optional background settings, and outputs the updated 2D ROI list. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-extract-metadata": (
    "Extracts the metadata (properties map) of each 2D ROI and writes them into a table.",
    "gate"),
"ij1-roi-extract-profile": (
    "Extracts the 2D pixel intensity profile along the ROI if a straight or irregular line ROI are given. If a rotated rectangle is processed, it is converted into a straight line.If any other ROI type is given, either the row or column average of the bounding rectangle is calculated. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-filter-and-merge-statistics-script": (
    "Filters and merges 2D ROIs by statistics: executes a script that has full control over the input ROI lists. The Python script contains a variable 'row_lists' that contains dictionaries with following entries: 'roi_list' is a list of dictionaries, 'annotations' is a dictionary containing the annotations. Each 'data' dictionary has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' with the extracted statistics. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-filter-by-overlap-v2": (
    "Filters 2D ROIs by overlap: only returns candidate ROIs that overlap with at least N ROIs from the filter set. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-filter-slice": (
    "Slices the 2D ROI list by extracting a sublist of ROI from the input.",
    "gate"),
"ij1-roi-filter-statistics-script": (
    "Filters 2D ROI list elements via statistics. The Python script contains a variable 'roi_list' that contains dictionaries. Each one has an item 'data' containing the ImageJ ROI, and a dictionary 'stats' containing the statistics. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-flatten": (
    "Flattens 2D ROIs by removing the Z/C/T coordinates. Equivalent to setting Z/C/T to zero.",
    "gate"),
"ij1-roi-flood-fill": (
    "Runs a 2D flood fill (magic wand) operation starting at the location of the ROI. Multiple operations are applied for each point within the ROI. Returns the generated outlines. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-from-table-circle": (
    "Converts data from a table to 2D circular ROIs. The ROIs are created to be centered at the provided locations. If you require more options, utilize 'Table to rectangular/oval ROIs' instead.",
    "gate"),
"ij1-roi-from-table-line": (
    "Converts data from a table to 2D line ROIs.",
    "gate"),
"ij1-roi-from-table-rectangular": (
    "Converts data from a table to 2D rectangular or oval ROIs. This node provides more options than the 'Table to circular ROIs' node.",
    "gate"),
"ij1-roi-generate-missing": (
    "Generates empty 2D ROI lists for data that are not paired with a matching ROI in the same iteration step. This algorithm groups the incoming data based on the annotations. Those groups can consist of one or multiple data item per slot. If items are missing, they will be generated according to this node's generator function. Otherwise data will be passed through.",
    "gate"),
"ij1-roi-generate-name": (
    "Sets a 2D ROI name for each individual ROI in the supplied ROI lists from an expression with access to annotations and measurements. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-interpolate": (
    "Interpolates a 2D ROI into a sub-pixel resolution ROI of floating-point coordinates spaced interval pixels apart.",
    "gate"),
"ij1-roi-merge-pairwise-or": (
    "Merges the 2D ROI lists pairwise: each individual ROI in Target is merged with each individual ROI in Source, generating all pairwise combinations of all ROIs. This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    "gate"),
"ij1-roi-outline-concave-hull-moreira-santos": (
    "Outlines 2D ROIs by calculating their concave hull using the algorithm by Moreira and Santos.",
    "gate"),
}
OP.update(OP2)


def words_of(text):
    return set(re.findall(r"[a-z0-9]+", text.lower()))


def main():
    with open(BASE + "/batch-04.json", encoding="utf-8") as f:
        batch = json.load(f)
    rows = batch["rows"]
    proposals = []
    errors = []
    seen = set()
    for row in rows:
        nid = row["nodeId"]
        if nid in seen:
            errors.append(f"DUPLICATE nodeId: {nid}")
        seen.add(nid)
        if nid not in OP:
            errors.append(f"MISSING opening for: {nid}")
            continue
        proposed, bucket = OP[nid]
        current = row["current"]
        # The opening must replace the first sentence(s): verify the preserved
        # tail of the current description is present verbatim in the proposal.
        tail = re.sub(r"^.*?[.!?](?:\s|$)", "", current, count=1)
        if tail.strip() and tail not in proposed:
            errors.append(f"{nid}: preserved tail not found verbatim")
        # Missing words must be covered case-insensitively.
        have = words_of(proposed)
        for w in row["missing"]:
            if w.lower() not in have:
                errors.append(f"{nid}: missing word not covered: {w}")
        if len(proposed) > 500:
            errors.append(f"{nid}: length {len(proposed)} > 500")
        if "\u2014" in proposed or "\u2013" in proposed:
            errors.append(f"{nid}: em/en dash present")
        proposals.append({
            "nodeId": nid,
            "file": row["file"],
            "class": row["class"],
            "bucket": bucket,
            "current": current,
            "proposed": proposed,
        })
    out = {
        "_note": "Authored opening sentences for the description sweep (#1404), batch 04.",
        "proposals": proposals,
    }
    with open(BASE + "/proposals-batch-04.json", "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"rows in batch: {len(rows)}")
    print(f"proposals written: {len(proposals)}")
    if errors:
        print("ERRORS:")
        for e in errors:
            print("  -", e)
        sys.exit(1)
    print("all checks passed")


if __name__ == "__main__":
    main()
