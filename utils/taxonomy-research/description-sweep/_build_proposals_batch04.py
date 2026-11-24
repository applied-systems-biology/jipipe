#!/usr/bin/env python3
"""Build proposals-batch-04.json from batch-04.json (description sweep #1404)."""
import json, re, sys

BASE = "/data/src/jipipe-4/utils/taxonomy-research/description-sweep"

# nodeId -> (proposed full description, bucket). The new opening sentence
# REPLACES the first sentence of the current description; the preserved rest
# of the body follows (verbatim, except rows in CONDENSED).
OP = {
"ij1-math-extended-minima-3d": (
    "Computes the extended minima of a 3D stack as a binary mask of voxels with intensity drops below the configured dynamic threshold.",
    "gate"),
"ij1-math-generate-from-expression": (
    "Generates an image from a user-written math expression that is evaluated for each pixel. The value is written into the image.",
    "gate"),
"ij1-math-generate-missing-from-expression": (
    "Generates a replacement image with a user-written math expression when one cannot be matched to a reference in the same iteration step. Applies a mathematical operation to each pixel. The value is written into the image. This algorithm groups the incoming data based on the annotations. Those groups can consist of one or multiple data item per slot. If items are missing, they will be generated according to this node's generator function. Otherwise data will be passed through.",
    "gate"),
"ij1-math-imagecalculator2d-merging": (
    "Legacy variant: applies a mathematical operation between multiple 2D images: f(f(f(image 1, image 2), image 3), image 4), ... The order is set by JIPipe's merging and cannot currently be controlled. For higher-dimensional data, it is applied per 2D slice. This algorithm groups the incoming data by annotations. Those groups can consist of multiple items. To group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    "gate"),
"ij1-math-impose-maxima-2d": (
    "Imposes the given maxima mask on a 2D image by repeatedly dilating it until its regional maxima fit the mask. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-math-impose-maxima-3d": (
    "3D variant of 'Impose maxima 2D': imposes the given maxima mask on a volume by repeatedly dilating it until its regional maxima fit the mask. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-math-local-imagecalculator2d-expression": (
    "2D image calculator variant: evaluates a user-written expression pixel-wise, producing a single output image. Available are both the current pixel values, as well as the local areas around these pixels. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-math-math2d-expression": (
    "Applies a user-written expression to each pixel of a greyscale image. Please note that is node has a high computational cost. Check if you can replace it with 'Fast image arithmetics'.",
    "gate"),
"ij1-math-math2d-expression-color": (
    "Applies a user-written expression to each pixel of a color image. The three available channels can be addressed individually. Please note that is node has a high computational cost.",
    "gate"),
"ij1-math-regional-maxima-2d": (
    "Returns the regional maxima of a 2D image: regions with a constant intensity value that are surrounded by lower intensity values. If higher-dimensional data is provided, the filter is applied to each 2D slice.",
    "gate"),
"ij1-math-regional-maxima-3d": (
    "Returns the regional minima of a 3D volume: regions with a constant intensity value that are surrounded by higher intensity values.",
    "gate"),
"ij1-math-regional-minima-2d": (
    "Returns the regional minima of a 2D image: regions with a constant intensity value that are surrounded by higher intensity values. If higher-dimensional data is provided, the filter is applied to each 2D slice.",
    "gate"),
"ij1-math-regional-minima-3d": (
    "Returns the regional minima of a 3D volume: regions with a constant intensity value that are surrounded by higher intensity values.",
    "gate"),
"ij1-math-round-32f": (
    "Rounds the values in a 32-bit float image to a specific number of decimals.",
    "gate"),
"ij1-math-vector-expression": (
    "Applies a user-written expression to each vector in the image. One dimension is defined as the dimension that contains the vector components. For example, you can make the channel the vector components. The algorithm then would iterate through all X, Y, Z, and T pixels and provide you with an array of all channel pixel values for this position. The output is a scalar image.",
    "gate"),
"ij1-ml-labkit-segmentation": (
    "Runs Labkit inference to segment or calculate the probability map of the input images. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-binary-skeletonize3d": (
    "Performs a morphological skeletonization of a 3D binary (8-bit) image.",
    "gate"),
"ij1-morph-grayscale-attribute-filtering-2d": (
    "Grayscale attribute filtering for 2D images: removes components by an attribute such as size rather than by intensity, often used to clean-up segmentation results. For grayscale input, each gray level is treated as a binary image, attribute opening is applied per level, and the results are unioned, so bright structures below the attribute threshold disappear. Closing and White/Black top-hat variants are available. This algorithm groups the incoming data by annotations.",
    "gate"),
"ij1-morph-grayscale-attribute-filtering-3d": (
    "Grayscale attribute filtering for 3D images: removes components by an attribute such as size rather than by intensity, often used to clean-up segmentation results. For grayscale input, each gray level is treated as a binary image, attribute opening is applied per level, and the results are unioned, so bright structures below the attribute threshold disappear. Closing and White/Black top-hat variants are available. This algorithm groups the incoming data by annotations.",
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
    "Converts a 2D ROI to a table by writing the point coordinates and other metadata of the incoming ROI into a table.",
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
    "Filters and merges 2D ROIs by statistics: executes a script with full control over the input ROI lists. The Python script gets a variable 'row_lists' of dictionaries with entries 'roi_list' (a list of dictionaries), 'annotations', 'data' (the ImageJ ROI) and 'stats' (the extracted statistics). This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
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
    "Sets a 2D ROI name for each individual ROI in the supplied ROI lists, computed by an expression with access to annotations and measurements. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-roi-interpolate": (
    "Interpolates a 2D ROI into a sub-pixel resolution ROI of floating-point coordinates spaced interval pixels apart.",
    "gate"),
"ij1-roi-merge-pairwise-or": (
    "Merges the 2D ROI lists pairwise: each individual ROI in Target is merged with each individual ROI in Source, generating all pairwise combinations of all ROIs. This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    "gate"),
"ij1-roi-outline-concave-hull-moreira-santos": (
    "Outlines 2D ROIs by calculating their concave hull with the algorithm by Moreira and Santos.",
    "gate"),
}
OP.update(OP2)

# Rows whose preserved body had to be condensed (rule 5) instead of kept verbatim.
CONDENSED = {
    "ij1-math-imagecalculator2d-merging",
    "ij1-morph-grayscale-attribute-filtering-2d",
    "ij1-morph-grayscale-attribute-filtering-3d",
    "ij1-roi-filter-and-merge-statistics-script",
}


def stems(word):
    s = {word}
    if word.endswith("ies") and len(word) > 4:
        s.add(word[:-3] + "y")
    elif word.endswith("es") and len(word) > 3:
        s.add(word[:-2])
        s.add(word[:-1])
    elif word.endswith("s") and len(word) > 3:
        s.add(word[:-1])
    if word.endswith("ing") and len(word) > 5:
        s.add(word[:-3])
        s.add(word[:-3] + "e")
    if word.endswith("ed") and len(word) > 4:
        s.add(word[:-2])
        s.add(word[:-2] + "e")
    if word.endswith("e") and len(word) > 2:
        s.add(word[:-1])
    return {x for x in s if len(x) >= 2}


def word_stems(text):
    out = set()
    for w in re.findall(r"[a-z0-9]+", text.lower()):
        out |= stems(w)
    return out


def first_sentence(text):
    m = re.match(r"^.*?[.!?](?:\s+|$)", text)
    return m.group(0) if m else text


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
            errors.append(f"MISSING proposed text for: {nid}")
            continue
        proposed, bucket = OP[nid]
        current = row["current"]
        if nid not in CONDENSED:
            opening = first_sentence(current)
            tail = current[len(opening):]
            if tail.strip() and tail not in proposed:
                errors.append(f"{nid}: preserved tail not found verbatim")
        # All missing words must be covered, case-insensitive, word-form aware.
        have = word_stems(proposed)
        for w in row["missing"]:
            if not (stems(w.lower()) & have):
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
    for p in proposals:
        print(f"  {p['nodeId']}: {len(p['proposed'])} chars")
    if errors:
        print("ERRORS:")
        for e in errors:
            print("  -", e)
        sys.exit(1)
    print("all checks passed")


if __name__ == "__main__":
    main()

