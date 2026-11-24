#!/usr/bin/env python3
"""Build proposals-batch-12.json and proposals-batch-13.json from batch-12.json
and batch-13.json (description sweep #1404).

Each proposal = a new opening sentence (stating what the node does and
containing all of the row's "missing" content words) + the preserved rest of
the current description body (deprecation notices stay first; the opening
sentence is inserted right after them).
"""
import json
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
MAX_LEN = 500

# nodeId -> (deprecation prefix kept first, or "", opening sentence,
#           condensed body or None)
OP = {
    # ---------------- batch 12 (cellpose) ----------------
    "import-cellpose-model-v2": (
        "",
        "Imports a Cellpose model from a single model file so it can be used for image segmentation.",
        "It takes a single model file as input and produces the corresponding Cellpose model for downstream steps."),
    "cellpose-2": (
        "Deprecated. Use the node with the same name.",
        "Runs Cellpose 2.x prediction on the input image, segmenting in 3D or on each 2D plane.",
        "Only ROI is activated by default; go to the 'Outputs' parameter section to enable the other outputs"
        " (Labels, Flows XY, Flows Z, Flows d, Probabilities). "
        "Please note that you need to setup a valid Python environment with Cellpose installed. "
        "This algorithm merges all annotations and data annotations. Use the iteration step settings to determine how annotations and data annotations are merged."),
    "import-cellpose-model": (
        "",
        "Imports a legacy Cellpose model from a saved model file for cell segmentation.",
        "It takes one model file as input and produces a legacy Cellpose model that later steps can apply to images."),
    "cellpose": (
        "Deprecated. Use the 'Cellpose prediction (2.x)' node instead.",
        "Runs the original Cellpose implementation on the input image, segmenting in 3D or on each 2D plane.",
        "Only ROI is activated by default; go to the 'Outputs' parameter section to enable the other outputs"
        " (Labels, Flows, Probabilities, Styles). "
        "Please note that you need to setup a valid Python environment with Cellpose installed. "
        "This algorithm merges all annotations and data annotations. Use the iteration step settings to determine how annotations and data annotations are merged."),
    "cellpose-segmentation-training-4.x": (
        "Deprecated: use the 'Cellpose segmentation training (4.x)' node instead.",
        "Trains a segmentation model with Cellpose (the old variant, from an existing model or from scratch).",
        "Incoming images are converted to greyscale; only 2D or 3D images are supported."
        " Requires a greyscale 16-bit or 8-bit label image column annotated to each raw data input (use the node 'Annotate with data')."
        " Does not support the training of image restoration models."
        " Uses Cellpose 3-era training defaults (learning rate 0.2, weight decay 1e-5, 500 epochs) not optimized for Cellpose 4's transformer-based architecture; "
        "the non-deprecated node has correct fine-tuning defaults (1e-5, 0.1, 100 epochs)."),
    "cellpose-training": (
        "Deprecated. Use the 'Cellpose training (2.x)' node instead.",
        "Trains a Cellpose model, starting from an existing model or from scratch.",
        "Incoming images are converted to greyscale; only 2D or 3D images are supported."
        " Requires a greyscale 16-bit or 8-bit label image column annotated to each raw data input (use the node 'Annotate with data')."
        " By default, JIPipe ensures that all connected components of this image are assigned a unique component; you can disable this via the parameters."),

    # ---------------- batch 13 (ij-filaments) ----------------
    "filaments-convert-to-mask": (
        "",
        "Converts 3D filament data into a mask image in which the filaments are drawn as solid pixels.",
        "It takes 3D filament data and an optional reference image that sets the mask size, and outputs a greyscale mask image. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-set-vertex-thickness-from-image": (
        "",
        "Sets the radius of each filament vertex from the given input image.",
        "Please note that if the C/T coordinates are set to zero, the value is extracted from the 0/0 slice. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-smooth-downscale": (
        "",
        "Smooths the filaments by downscaling the vertex locations and applying the 'Remove duplicate vertices' operation.",
        "The positions are then restored."),
    "filaments-set-vertex-intensity-from-image": (
        "",
        "Sets the value/intensity of each filament vertex from the given input image.",
        "Please note that if the C/T coordinates are set to zero, the value is extracted from the 0/0 slice. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-find-linear-vertex-order": (
        "",
        "For all linear filament vertex components (all vertices have a degree less than 3), finds the order of each vertex in that line.",
        "Also can calculate the distance (in pixels) traveled from the start to the end point.The result is stored in each vertex' metadata field."),
    "filaments-convert-to-rgb": (
        "",
        "Converts the filaments to an RGB image by rendering them onto it",
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-set-metadata-to-component-index": (
        "",
        "Sets the metadata of each filament vertex to the index of the connected component it belongs to",
        "(the filament's connected components are found and their index written into a metadata field)"),
    "filaments-tsoax": (
        "",
        "Finds and tracks filaments and network branches in 3D+T time-lapse sequences with TSOAX, yielding a track for each evolving filament or network segment.",
        "TSOAX works well even if filaments disappear or reappear. "
        "It is an extension of SOAX (for network extraction in static images) to network extraction and tracking in time lapse movies, "
        "and facilitates quantitative analysis of network dynamics of multi-dimensional biopolymer networks imaged by various microscopic imaging modalities. "
        "It combines multiple Stretching Open Active Contour Models for extraction with a local and global graph matching framework for temporal correspondence."),
    "filaments-connect-vertices": (
        "",
        "Connects existing filament vertices based on customizable criteria. Uses expressions to filter candidate edges, which can be slow.",
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-connect-vertices-fast": (
        "",
        "Fast variant: connects existing filament vertices based on distance.",
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-convert-to-roi3d": (
        "",
        "Converts 3D filament data into a list of 3D ROIs, optionally drawing filament segments as 3D line ROIs and points as 3D point ROIs.",
        "You can override line thickness or point radius."),
    "filaments-erode-end-vertices": (
        "",
        "Erodes the filament by iteratively removing end vertices, i.e. vertices that have a degree of at most 1",
        None),
    "filaments-fix-overlaps-non-branching": (
        "",
        "Attempts to fix overlapping filaments that are merged together by junctions.",
        "Please note that this operation assumes that all filaments are non-branching. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-remove-vertex-value-backups": (
        "",
        "Removes value backups from the filament vertices",
        None),
    "filaments-restore-vertex-value-backups": (
        "",
        "Restores the value backups of the filament vertices",
        None),
    "filaments-set-edge-properties": (
        "",
        "Sets filament edge properties by overriding various properties of the filament edges using expressions",
        None),
    "filaments-set-edge-properties-2": (
        "",
        "Sets filament edge properties by overriding various properties of the filament edges manually",
        None),
    "filaments-set-vertex-calibration-from-image": (
        "",
        "Sets the physical voxel sizes of all filament vertices to the data provided in the image.",
        "If the image has no calibration data, the filament voxel sizes will be reset to 1 pixel. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-set-vertex-metadata-from-image": (
        "",
        "Sets the vertex metadata of each filament vertex from the given input image.",
        "Please note that if the Z/C/T coordinates are set to zero, the value is extracted from the 0/0/0 slice. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-set-vertex-properties": (
        "",
        "Sets filament vertex properties by overriding various properties of the filament vertices using expressions",
        None),
    "filaments-set-vertex-properties-2": (
        "",
        "Sets filament vertex properties by overriding various properties of the filament vertices manually",
        None),
    "filaments-simplify": (
        "",
        "Simplifies the filaments by removing all intermediate vertices (degree 2)",
        "and thus produces a graph that contains only the endpoints and junctions."),
    "filaments-skeleton-to-filaments-2d3d-simplified": (
        "",
        "Extracts filaments from a binary skeleton by utilizing the Analyze Skeleton (2D/3D) plugin from Fiji.",
        "Please note that the generated filaments are were automatically simplified by the method and might not fully represent anymore all structural information contained within the skeleton. "
        "Use 'Binary skeleton to 2D filaments' or 'Binary skeleton to 3D filaments' to if you are unsure about the functionality of this node. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot."),
    "filaments-subdivide-edges": (
        "",
        "Subdivides the filament edges by inserting a vertex at the center of edges.",
        "The connectivity and shape of the filament is preserved."),
    "filaments-tsoax-2d": (
        "",
        "Finds and tracks filaments and network branches in 2D time-lapse sequences: variant of TSOAX that is applied per 2D+T stack of the image and the results are combined.",
        "TSOAX works well even if filaments disappear or reappear. "
        "It is an extension of SOAX (for network extraction in static images) to network extraction and tracking in time lapse movies, "
        "and facilitates quantitative analysis of network dynamics of multi-dimensional biopolymer networks imaged by various microscopic imaging modalities. "
        "It combines multiple Stretching Open Active Contour Models for extraction with a local and global graph matching framework for temporal correspondence."),
    "tsoax-analyze-snakes": (
        "",
        "Analyzes the TSOAX output by parsing existing results generated by TSOAX",
        None),
}


def build_entry(batch_row):
    nid = batch_row["nodeId"]
    notice, opening, body = OP[nid]
    parts = []
    if notice:
        parts.append(notice)
    parts.append(opening)
    if body is None:
        body = ""
    else:
        body = " ".join(body)
    if body:
        parts.append(body)
    return {
        "nodeId": nid,
        "file": batch_row["file"],
        "class": batch_row["class"],
        "bucket": "gate",
        "current": batch_row["current"],
        "proposed": "".join(parts),
    }


def check(batch, entries, idx):
    ok = True
    rows = {r["nodeId"]: r for r in batch["rows"]}
    by_id = {e["nodeId"]: e for e in entries}
    for nid in rows:
        if nid not in by_id:
            print(f"  batch {idx}: MISSING PROPOSAL for {nid}")
            ok = False
    for nid, e in by_id.items():
        if nid not in rows:
            print(f"  batch {idx}: UNKNOWN nodeId {nid}")
            ok = False
            continue
        row = rows[nid]
        for key in ("nodeId", "file", "class", "current"):
            if e[key] != row[key]:
                print(f"  batch {idx}: {nid} field {key} copied wrong")
                ok = False
        p = e["proposed"]
        if len(p) > MAX_LEN:
            print(f"  batch {idx}: {nid} TOO LONG ({len(p)} > {MAX_LEN})")
            ok = False
        for m in re.findall(r"[\u2013\u2014]", p):
            print(f"  batch {idx}: {nid} has dash {m!r}")
            ok = False
        # a missing word is covered when any word of the proposed text has
        # the stem as a prefix (covers "vertices" for "vertex", "simplifies"
        # for "simplify", "converted" for "convert")
        tokens = re.findall(r"[a-z0-9]+", p.lower())
        for stem in row["missing"]:
            if not any(t.startswith(stem.lower()) for t in tokens):
                print(f"  batch {idx}: {nid} missing word not covered: {stem}")
                ok = False
        # preserved fragments must appear verbatim in the proposed text
        if nid in OP:
            notice, opening, body = OP[nid]
            if body:
                for frag in body:
                    if frag not in p:
                        print(f"  batch {idx}: {nid} body fragment not preserved verbatim: {frag[:60]!r}...")
                        ok = False
            if notice:
                if not p.startswith(notice):
                    print(f"  batch {idx}: {nid} deprecation notice not first")
                    ok = False
            if opening not in p:
                print(f"  batch {idx}: {nid} opening not found")
                ok = False
    return ok


def main():
    all_ok = True
    for idx in ("12", "13"):
        batch = json.load(open(os.path.join(BASE, f"batch-{idx}.json"), encoding="utf-8"))
        entries = [build_entry(r) for r in batch["rows"]]
        out = {
            "_note": f"Authored opening sentences for the description sweep (#1404), batch {idx}.",
            "proposals": entries,
        }
        path = os.path.join(BASE, f"proposals-batch-{idx}.json")
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(out, fh, ensure_ascii=False, indent=2)
            fh.write("\n")
        print(f"batch {idx}: wrote {len(entries)} proposals -> {path}")
        all_ok &= check(batch, entries, idx)
    print("ALL CHECKS PASSED" if all_ok else "CHECKS FAILED")
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
