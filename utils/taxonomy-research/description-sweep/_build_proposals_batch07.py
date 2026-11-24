#!/usr/bin/env python3
"""Build proposals-batch-07.json from batch-07.json (description sweep #1404).

Each proposal = a new opening sentence (stating what the node does and
containing all of the row's "missing" content words) + the preserved rest of
the current description body (deprecation notices stay first; the opening
sentence is inserted right after them). Bodies are condensed only where the
500 char cap requires it (needsCondensation rows).
"""
import json
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
MAX_LEN = 500
BATCH = "07"

# nodeId -> (proposed text, [verbatim fragments that must be preserved, or
#           None if the body was condensed/rewritten])
# The full boilerplate used throughout this plugin:
BOILER = ("This algorithm groups the incoming data based on the annotations. "
          "Those groups can consist of one data item per slot.")
BOILER_SHORT = "Groups incoming data by annotations (one data item per slot)."

PROPOSED = {
    # FilterRoi3dListsAlgorithm: expression on the list bounding box
    # (count, x/y/z, width/height/depth), output empty list or nothing.
    "ij3d-filter-roi3d-list": (
        "Filters the 3D ROI lists of the ImageJ 3D suite (IJ3D), passing only the "
        "lists that match a filter expression on the list's bounding box.",
        None),

    # Fast3DFiltersSobelAlgorithm
    "ij3d-filter-sobel-3d": (
        "Applies the fast 3D Sobel filter of the ImageJ 3D suite (IJ3D) using an "
        "ellipsoidal neighbourhood.",
        None),

    # Fast3DFiltersTopHatAlgorithm
    "ij3d-filter-top-hat-3d": (
        "Applies the fast 3D morphological top-hat of the ImageJ 3D suite (IJ3D) "
        "using an ellipsoidal neighbourhood.",
        None),

    # Fast3DFiltersVarianceAlgorithm
    "ij3d-filter-variance-3d": (
        "Applies the fast 3D local variance filter of the ImageJ 3D suite (IJ3D) "
        "in an ellipsoidal neighbourhood.",
        None),

    # DistanceMap3DAlgorithm
    "ij3d-math-distance-map-3d": (
        "Computes the Euclidean distance map of a 3D image in the ImageJ 3D "
        "suite (IJ3D). The calculated distances will be in the calibrated unit.",
        ["The calculated distances will be in the calibrated unit."]),

    # ErodedVolumeFraction3DAlgorithm
    "ij3d-math-eroded-volume-fraction-3d": (
        "Computes the Eroded Volume Fraction (EVF) in 3D with the ImageJ 3D "
        "suite (IJ3D). The EVF (Eroded Volume Fraction) can be regarded as a "
        "normalized EDT (Euclidean Distance Map). The calculated distances will "
        "be in the calibrated unit. " + BOILER,
        ["The EVF (Eroded Volume Fraction) can be regarded as a normalized EDT "
         "(Euclidean Distance Map).",
         "The calculated distances will be in the calibrated unit.",
         BOILER]),

    # Roi3dCalculatorAlgorithm: logicalAnd = intersection (source verified:
    # addVoxelsIntersection), logicalOr = union. The current body said AND
    # creates the union, which is wrong, so that word is corrected.
    "ij3d-roi-calculator": (
        "This ROI calculator applies a logical operation (AND, OR, or XOR) to "
        "the 3D ROI list of the ImageJ 3D suite (IJ3D), combining all ROI into "
        "a single one. The logical operations are applied to the whole list, "
        "meaning that an AND operation will create the intersection of all ROI "
        "in the list. If you want to apply the operation only to a sub-set of "
        "ROI, preprocess using a ROI splitter algorithm.",
        ["The logical operations are applied to the whole list, meaning that an "
         "AND operation will create the intersection of all ROI in the list.",
         "If you want to apply the operation only to a sub-set of ROI, "
         "preprocess using a ROI splitter algorithm."]),

    # ChangeRoi3dPropertiesFromExpressionsAlgorithm
    "ij3d-roi-change-properties-from-expressions": (
        "Sets the properties of every 3D ROI in the ImageJ 3D suite (IJ3D) from "
        "user-written expressions. " + BOILER,
        [BOILER]),

    # ChangeRoi3dPropertiesFromTableAlgorithm
    "ij3d-roi-change-properties-from-table": (
        "Sets the properties of every 3D ROI in the ImageJ 3D suite (IJ3D) from "
        "values extracted from a table. Table rows are matched to their ROI and "
        "the column values are put into expression variables, so the ROI can be "
        "modified. " + BOILER,
        ["Table rows are matched to their ROI and the column values are put "
         "into expression variables, so the ROI can be modified.",
         BOILER]),

    # ColorRoi3dByNameAlgorithm
    "ij3d-roi-color-by-name": (
        "Sets the 3D ROI item colors in the ImageJ 3D suite (IJ3D) by their name.",
        None),

    # Roi2dToRoi3dConverterAlgorithm
    "ij3d-roi-convert-2d-to-3d": (
        "Converts a 2D ROI list into a 3D ROI list in the ImageJ 3D suite "
        "(IJ3D). Please note that you need to enable the fast mode to merge 2D "
        "ROI in the Z-axis.",
        ["Please note that you need to enable the fast mode to merge 2D ROI in "
         "the Z-axis."]),

    # Roi3dToRoi2dConverterAlgorithm
    "ij3d-roi-convert-3d-to-2d": (
        "Converts a 3D ROI list of the ImageJ 3D suite (IJ3D) into a 2D ROI list.",
        None),

    # Roi3dTo3DMeshAlgorithm
    "ij3d-roi-convert-to-3d-mesh": (
        "Takes a list of 3D ROIs from the ImageJ 3D suite (IJ3D) and turns each "
        "into a 3D mesh, producing a 3D scene for visualization or downstream "
        "processing. It can respect physical voxel sizes, force a mesh length "
        "unit, smooth surfaces, add a name prefix, and override mesh color.",
        ["It can respect physical voxel sizes, force a mesh length unit, "
         "smooth surfaces, add a name prefix, and override mesh color."]),

    # ExtractOverlay3DAlgorithm (source verified: extracts the IJ3D overlay
    # ROI lists stored on the image)
    "ij3d-roi-extract-overlay": (
        "Extracts the 3D overlay ROIs of an image in the ImageJ 3D suite "
        "(IJ3D) into a 3D ROI list.",
        None),

    # FilterRoi3dByOverlapAlgorithm
    "ij3d-roi-filter-by-overlap-v2": (
        "Filters the 3D ROI lists of the ImageJ 3D suite (IJ3D) by testing for "
        "mutual overlap. The ROI1 output contains all ROI1 input ROI that "
        "overlap with any of ROI2. The ROI2 output contains all ROI2 input ROI "
        "that overlap with a ROI1 ROI. " + BOILER,
        ["The ROI1 output contains all ROI1 input ROI that overlap with any of "
         "ROI2.",
         "The ROI2 output contains all ROI2 input ROI that overlap with a ROI1 "
         "ROI.",
         BOILER]),

    # MergeRoi3dAlgorithm (source verified: concatenates all input lists)
    "ij3d-roi-merge": (
        "Merges the input 3D ROI lists of the ImageJ 3D suite (IJ3D) into a "
        "single list. This algorithm groups the incoming data based on the "
        "annotations. Those groups can consist of multiple data items. If you "
        "want to group all data into one output, set the matching strategy to "
        "'Custom' and leave 'Data set matching annotations' empty.",
        ["This algorithm groups the incoming data based on the annotations. "
         "Those groups can consist of multiple data items.",
         "If you want to group all data into one output, set the matching "
         "strategy to 'Custom' and leave 'Data set matching annotations' empty."]),

    # RemoveBorderRoi3dAlgorithm (source verified: checks X/Y/Z against the
    # reference image borders, optional border distance)
    "ij3d-roi-remove-border": (
        "Removes the 3D ROI at the image borders in the ImageJ 3D suite (IJ3D), "
        "checked against a reference image. " + BOILER,
        [BOILER]),

    # RemoveOverlay3DAlgorithm (source verified: strips the IJ3D overlay from
    # the image)
    "ij3d-roi-remove-overlay": (
        "Removes the 3D overlay ROIs from an image in the ImageJ 3D suite (IJ3D).",
        None),

    # RenderOverlay3DAlgorithm (source verified: draws the overlay ROI onto
    # the image, RGB output)
    "ij3d-roi-render-overlay": (
        "Renders the 3D overlay to a color (RGB) image in the ImageJ 3D suite "
        "(IJ3D).",
        None),

    # SetRoi3dMetadataFromTableAlgorithm
    "ij3d-roi-set-metadata-from-table": (
        "Sets the metadata (property map) of the 3D ROI in the ImageJ 3D suite "
        "(IJ3D) from a table. The table either has a column that indicates the "
        "ROI index or contains one row per ROI (row index is the ROI index) "
        + BOILER,
        ["The table either has a column that indicates the ROI index or "
         "contains one row per ROI (row index is the ROI index)",
         BOILER]),

    # SetOverlay3DAlgorithm (source verified: replaces the image's IJ3D
    # overlay with the given ROI list)
    "ij3d-roi-set-overlay": (
        "Sets the 3D overlay ROIs of an image in the ImageJ 3D suite (IJ3D). "
        "Please note that 3D overlays are not natively supported by ImageJ. "
        + BOILER,
        ["Please note that 3D overlays are not natively supported by ImageJ.",
         BOILER]),

    # SplitRoi3dByStatisticsAlgorithm
    "ij3d-roi-split-by-statistics": (
        "Splits the incoming 3D ROI lists of the ImageJ 3D suite (IJ3D) by a "
        "classifier value that is calculated based on statistics. " + BOILER,
        [BOILER]),

    # SplitRoi3dIntoConnectedComponentsAlgorithm
    "ij3d-roi-split-into-connected-components": (
        "Splits the input 3D ROI list of the ImageJ 3D suite (IJ3D) into "
        "multiple ROI lists, one per connected component. " + BOILER,
        [BOILER]),

    # HysteresisSegmentation3DAlgorithm
    "ij3d-segmentation-hysteresis": (
        "Performs hysteresis segmentation of a 3D image in the ImageJ 3D suite "
        "(IJ3D) by 3-level thresholding based on two thresholds. The low "
        "threshold will determine background (for values below), the high "
        "threshold the objects cores (for values above) and an intermediate "
        "level for values between low and high threshold.",
        ["The low threshold will determine background (for values below), the "
         "high threshold the objects cores (for values above) and an "
         "intermediate level for values between low and high threshold."]),

    # IterativeThreshold3DAlgorithm
    "ij3d-segmentation-iterative-thresholding": (
        "Segments a 3D image in the ImageJ 3D suite (IJ3D) by iterative "
        "thresholding, building a lineage of the detected objects. Tests all "
        "thresholds and detect objects for all thresholds, it will then try to "
        "build a lineage of the objects detected, linking them from one "
        "threshold to the next threshold, taking possible splits into account. "
        + BOILER,
        ["Tests all thresholds and detect objects for all thresholds, it will "
         "then try to build a lineage of the objects detected, linking them "
         "from one threshold to the next threshold, taking possible splits "
         "into account.",
         BOILER]),

    # NucleiSegmentation3DAlgorithm
    "ij3d-segmentation-nuclei-3d": (
        "Segments nuclei from cell culture (not from tissues) in 3D with the "
        "ImageJ 3D suite (IJ3D). The method is based on a maximum Z-projection "
        "followed by a 2D Segmentation. The segmentation for the 2D projection "
        "is based on a global thresholding. The nuclei are then segmented and "
        "separated using ImageJ watershed.",
        ["The method is based on a maximum Z-projection followed by a 2D "
         "Segmentation.",
         "The segmentation for the 2D projection is based on a global "
         "thresholding.",
         "The nuclei are then segmented and separated using ImageJ watershed."]),

    # ConstantSpotSegmentation3DAlgorithm (condensed: needsCondensation)
    "ij3d-segmentation-spot3d-constant": (
        "Performs the constant spot segmentation of the ImageJ 3D suite "
        "(IJ3D), applying one local threshold to all objects. It works with "
        "two images: one with the object seeds (from local maxima), the other "
        "with signal data. Voxels with values above the local threshold around "
        "each seed are clustered; the threshold is defined by the user as Local "
        "Background. " + BOILER,
        None),

    # DifferenceSpotSegmentation3DAlgorithm (condensed; the en dash in the
    # original "ValueOfSeed – diff" is replaced by a plain hyphen)
    "ij3d-segmentation-spot3d-difference": (
        "Performs spot segmentation in the ImageJ 3D suite (IJ3D) by computing "
        "each local threshold as the seed value minus a constant difference. It "
        "works with two images: one with the object seeds (from local maxima), "
        "the other with signal data. Voxels with values above the local "
        "threshold (threshold = ValueOfSeed - diff) are clustered. " + BOILER,
        None),

    # GaussianFitSpotSegmentation3DAlgorithm (condensed)
    "ij3d-segmentation-spot3d-gaussian-fit": (
        "Performs spot segmentation in the ImageJ 3D suite (IJ3D) by fitting a "
        "Gaussian to each object's radial distribution. It works with two "
        "images: one with the object seeds (local maxima), the other with "
        "signal data. The fit's standard deviation sets the threshold via a "
        "user factor; 1.17 reaches the full width at half maximum, 2 and 3 "
        "cover about 90% and 99% of the curve surface. " + BOILER_SHORT,
        None),

    # LocalMeanSpotSegmentation3DAlgorithm (condensed)
    "ij3d-segmentation-spot3d-local-mean": (
        "Performs spot segmentation in the ImageJ 3D suite (IJ3D) from local "
        "means around each seed. It works with two images: one with the object "
        "seeds (local maxima), the other with signal data. Three circles are "
        "drawn: one inside the object, two outside it. The threshold is a "
        "weighted mean of the object mean (inside) and the background mean "
        "(outside); weight 0.5 by default, higher weights move it toward the "
        "background. " + BOILER_SHORT,
        None),

    # ImportRoi3dAlgorithm
    "import-roi-3d": (
        "Imports a 3D ROI list of the ImageJ 3D suite (IJ3D) from a *.zip file.",
        None),
}


def main():
    batch = json.load(open(os.path.join(BASE, f"batch-{BATCH}.json"),
                           encoding="utf-8"))
    rows = batch["rows"]
    by_id = {r["nodeId"]: r for r in rows}

    ok = True
    if set(PROPOSED) != set(by_id):
        print("nodeId set mismatch:",
              sorted(set(PROPOSED) ^ set(by_id)))
        ok = False

    entries = []
    for row in rows:
        nid = row["nodeId"]
        proposed, fragments = PROPOSED[nid]
        proposed = re.sub(r"\s+", " ", proposed).strip()
        entry = {
            "nodeId": nid,
            "file": row["file"],
            "class": row["class"],
            "bucket": "gate",
            "current": row["current"],
            "proposed": proposed,
        }
        entries.append(entry)

        if len(proposed) > MAX_LEN:
            print(f"  {nid}: TOO LONG ({len(proposed)} > {MAX_LEN})")
            ok = False
        for m in re.findall(r"[\u2013\u2014]", proposed):
            print(f"  {nid}: has dash {m!r}")
            ok = False
        for stem in row["missing"]:
            if not re.search(r"\b" + re.escape(stem) + r"\w*",
                             proposed.lower()):
                print(f"  {nid}: missing word not covered: {stem}")
                ok = False
        for frag in fragments or []:
            if frag not in proposed:
                print(f"  {nid}: body fragment not preserved verbatim: "
                      f"{frag[:60]!r}...")
                ok = False

    out = {
        "_note": f"Authored opening sentences for the description sweep (#1404), "
                 f"batch {BATCH}.",
        "proposals": entries,
    }
    path = os.path.join(BASE, f"proposals-batch-{BATCH}.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    print(f"batch {BATCH}: wrote {len(entries)} proposals -> {path}")
    if not ok:
        # do not trust a failing run; still keep the file for inspection
        print("CHECKS FAILED")
        sys.exit(1)
    print("ALL CHECKS PASSED")


if __name__ == "__main__":
    main()
