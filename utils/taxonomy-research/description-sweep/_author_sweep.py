#!/usr/bin/env python3
"""Author proposal files for the description quality sweep (#1404), batches 14-17.
Reads each batch, attaches an authored 'proposed' string per nodeId, validates,
and writes proposals-batch-NN.json. nodeId/file/class/current are copied verbatim
from the batch rows.
"""
import json, re, sys, os

BASE = os.path.dirname(os.path.abspath(__file__))

# Authored proposals keyed by nodeId.
P = {
    # ---------------- Batch 14 ----------------
    "import-imagej-roi-from-file":
        "Imports a ROI list from a file. The file can be either a single ROI (.roi extension) or a list of ROI (.zip extension).",

    "ij1-contrast-calibrate":
        "Adjusts the displayed contrast of the incoming image by re-calibrating its color range in ImageJ, without changing the pixel data. "
        "The displayed contrast is stored into the image itself. If you want per-slice contrast, convert to 8-bit or 16-bit with "
        "'Convert image to 8-bit (ImageJ auto-contrast)' or 'Convert image to 16-bit (ImageJ auto-contrast)', or use "
        "'Apply ImageJ auto-contrast per slice' for 32-bit images.",

    "extract-imagej-roi-from-ome-image":
        "Extracts a ROI list from an OME image.",

    "annotate-data-with-image-properties":
        "Annotates any data with annotations generated from the image's properties. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",

    "annotate-with-bioformats":
        "Annotates each file with OME metadata obtained from Bio-Formats, without importing the file itself.",

    "export-imagej-bioformats":
        "Deprecated. Please use the new node. This legacy Bio-Formats exporter writes the OME images into an *.ome.tif file.",

    "export-imagej-bioformats-v2":
        "Writes the OME images into an *.ome.tif file via the Bio-Formats exporter.",

    "ij1-export-ome-zarr":
        "Exports 2D or 3D OME image data by writing each as an OME-ZARR dataset in a folder or ZIP file, then outputting a URI reference to the result. "
        "It can create a multi-resolution pyramid, select compression, and define chunk sizes.",

    "import-imagej-lut-from-file-as-rgb":
        "Imports an ImageJ *.lut file and renders it as an RGB image.",

    "import-ome-xml-from-bioformats":
        "Reads the OME XML from Bio-Formats without importing the file itself.",

    "set-imagej-bioformats-settings":
        "Configures the Bio-Formats exporter setup, defining the OME image settings used to write the image into an *.ome.tif file.",

    # ---------------- Batch 15 ----------------
    "distribute-data":
        "Splits the incoming data randomly, sending a certain percentage of it to the specified output slot. "
        "Negative weights are replaced by zero, and each data item goes to only one slot (no overlaps). "
        "The data is grouped by annotations; to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",

    "data-batch-slicer":
        "This slicer merges the incoming data into merging iteration steps, outputting only the items within the index range. "
        "You can use this to remove duplicates. Annotations are not modified (merged annotations are not copied into the output). "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. "
        "If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",

    "data-table-push-annotations":
        "Pushes the annotations of a data table into the table itself.",

    "extract-table-to-data":
        "Unpacks a data table, extracting the data items stored in its rows into the output. If multiple tables are supplied, the rows are merged.",

    "check-iteration-step-multi":
        "Checks that iteration steps with multiple data items per input slot are created correctly, filtering out incomplete steps. "
        "This node is designed for creating iteration steps where multiple data items can be assigned to an input slot. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. "
        "If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",

    "crasher-on-input":
        "Crashes the current run as soon as any data is processed. Use this node to inform users about unwanted conditions in your pipeline.",

    "data-table-pull-annotations":
        "Pulls text and data annotations from inside each data table and exposes them on the table's output.",

    "distribute-data-by-count":
        "Splits the incoming data randomly, giving each output slot the specified count of data items. "
        "Each data item goes to only one slot (no overlaps); if there is not enough data, the slots with the lower order (left/top) are preferred. "
        "The data is grouped by annotations; to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",

    "generate-placeholder-for-missing-data":
        "Creates an iteration step from the reference and data inputs, where a placeholder is used for any reference whose data is missing. "
        "If the data is missing for a reference, it is extracted from the items in the placeholder slot. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one or multiple data item per slot. "
        "If items are missing, they will be generated according to this node's generator function. Otherwise data will be passed through.",

    "jipipe-output-get-slot-folder":
        "Gets the output folder of a specific slot from a JIPipe output. Use the 'Set output slot' button to select the correct parameters.",

    "jipipe-project-parameters-from-paths":
        "Stores the incoming paths as JIPipe project parameters. This node supports path and string parameters. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",

    "run-process-iterating":
        "Runs an external process once per data item (the iterating variant). "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",

    "run-process-merging":
        "Runs an external process once per group of data items (the merging variant). "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. "
        "If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",

    # ---------------- Batch 16 ----------------
    "mask-to-3d-mesh":
        "Converts a 3D mask into a 3D scene mesh by applying the 'Marching cubes' algorithm.",

    "export-3d-scene-to-collada":
        "Exports a 3D scene to a Collada 1.4.1 .dae file, producing a file you can open in 3D viewers or use in later analysis. "
        "It can name files from metadata and optionally index meshes to reduce file size. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",

    "merge-scene-3d":
        "Merges multiple input 3D scenes into a single scene. "
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. "
        "If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",

    "scene-3d-create-line":
        "Creates a 3D scene containing a line mesh.",

    "scene-3d-create-sphere":
        "Creates a 3D scene containing a sphere mesh at the specified location.",

    # ---------------- Batch 17 ----------------
    "tesseract-ocr":
        "Finds text in the image using Tesseract OCR. We highly recommend to preprocess the image by applying thresholding, "
        "otherwise Tesseract will apply its own Otsu-based method that may yield unsatisfactory results. "
        "If higher-dimensional data is provided, the OCR is applied per plane.",
}

DASHES = set("\u2014\u2013")

# --- faithful port of NodeSearchText (jipipe-core) tokenizer + coverage ---
# Java: [\s_\-/.]+ | (?<=[a-z])(?=[A-Z]) | (?=[A-Z][a-z])  -> split
_SPLIT_RE = re.compile(r"[\s_\-/.]+|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])")
_STOPWORDS = {
    "the", "a", "an", "of", "and", "or", "to", "in", "for", "with", "is", "are",
    "be", "as", "on", "by", "it", "its", "this", "that", "from", "into", "if",
    "you", "your", "can", "will",
}

def split_words(s):
    return [w for w in _SPLIT_RE.split(s or "") if w]

def word_set(text):
    return {w.lower() for w in split_words(text or "")}

def content_words(text):
    out = []
    for w in split_words(text or ""):
        lower = re.sub(r"[^a-z0-9]+$", "", re.sub(r"^[^a-z0-9]+", "", w.lower()))
        if not lower or lower in _STOPWORDS or len(lower) < 2 or re.fullmatch(r"[0-9]+", lower):
            continue
        out.append(lower)
    return out

def covered_by(word, tokens):
    """Mirror of NodeSearchText.coveredBy: exact, prefix either way, stem
    equality (approximated), or substring. The stem step only ever ADDS a
    pass, so skipping it is conservative (never under-reports a real pass for
    the words that matter here; we rely on the exact/prefix/substring rungs)."""
    if not word or not tokens:
        return False
    w = word.lower()
    for t in tokens:
        if t == w:
            return True
    for t in tokens:
        if t.startswith(w) or w.startswith(t):
            return True
    for t in tokens:
        if w in t:
            return True
    return False

# Verbatim body fragments that must survive unchanged in the proposed text.
# Only rows where the current description has a real second (or third) sentence
# AND the author did not intentionally reword it (single-sentence rows, the two
# intentionally condensed rows, and the deprecated row without a body are
# omitted). A row absent here is not body-checked.
BODY = {
    # batch 14
    "import-imagej-roi-from-file":
        "The file can be either a single ROI (.roi extension) or a list of ROI (.zip extension).",
    "annotate-data-with-image-properties":
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "ij1-export-ome-zarr":
        "It can create a multi-resolution pyramid, select compression, and define chunk sizes.",
    # batch 15
    "data-batch-slicer":
        "You can use this to remove duplicates. Annotations are not modified (merged annotations are not copied into the output). This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    "extract-table-to-data":
        "If multiple tables are supplied, the rows are merged.",
    "crasher-on-input":
        "Use this node to inform users about unwanted conditions in your pipeline.",
    "generate-placeholder-for-missing-data":
        "If the data is missing for a reference, it is extracted from the items in the placeholder slot. This algorithm groups the incoming data based on the annotations. Those groups can consist of one or multiple data item per slot. If items are missing, they will be generated according to this node's generator function. Otherwise data will be passed through.",
    "jipipe-output-get-slot-folder":
        "Use the 'Set output slot' button to select the correct parameters.",
    "jipipe-project-parameters-from-paths":
        "This node supports path and string parameters. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "run-process-iterating":
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "run-process-merging":
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    # batch 16
    "export-3d-scene-to-collada":
        "It can name files from metadata and optionally index meshes to reduce file size. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "merge-scene-3d":
        "This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
    # batch 17
    "tesseract-ocr":
        "We highly recommend to preprocess the image by applying thresholding, otherwise Tesseract will apply its own Otsu-based method that may yield unsatisfactory results. If higher-dimensional data is provided, the OCR is applied per plane.",
}


def main():
    all_ok = True
    counts = {}
    for n in (14, 15, 16, 17):
        with open(os.path.join(BASE, f"batch-{n}.json")) as f:
            batch = json.load(f)
        proposals = []
        problems = []
        seen = set()
        for row in batch["rows"]:
            nid = row["nodeId"]
            seen.add(nid)
            if nid not in P:
                problems.append(f"[{nid}] NO PROPOSAL AUTHORED")
                continue
            prop = P[nid]
            # 1. length (the gate's cap is 500, matching MAX_DESC_CHARS)
            L = len(prop)
            if L > 500:
                problems.append(f"[{nid}] LENGTH {L} > 500")
            # 2. dashes
            if any(c in DASHES for c in prop):
                problems.append(f"[{nid}] contains em/en dash")
            # 3. EVERY content word of the name must be covered in the proposed
            #    text (the gate runs coveredBy over the full name content words).
            prop_tokens = word_set(prop)
            name_words = content_words(row["name"])
            uncovered = [w for w in name_words if not covered_by(w, prop_tokens)]
            if uncovered:
                problems.append(f"[{nid}] NAME words not covered: {uncovered}")
            # 3b. the row's 'missing' list must be a subset of name content words
            #    (sanity: the gate's missing list is what we were asked to fix)
            for w in row.get("missing", []):
                if not covered_by(w, prop_tokens):
                    problems.append(f"[{nid}] missing word still uncovered: '{w}'")
            # 4. body preservation: the verbatim fragment (if any) must appear
            #    unchanged in the proposed text, and must exist in the current
            #    description (self-check against transcription typos).
            frag = BODY.get(nid)
            if frag is not None:
                if frag not in row["current"]:
                    problems.append(f"[{nid}] BODY self-check failed: fragment not in current text")
                if frag not in prop:
                    problems.append(f"[{nid}] BODY not preserved verbatim (frag={frag[:40]!r})")
            proposals.append({
                "nodeId": row["nodeId"],
                "file": row["file"],
                "class": row["class"],
                "bucket": "gate",
                "current": row["current"],
                "proposed": prop,
            })
        if problems:
            all_ok = False
            print(f"\n=== batch {n}: PROBLEMS ===")
            for p in problems:
                print("  " + p)
        # write
        out = {
            "_note": f"Authored opening sentences for the description sweep (#1404), batch {n}.",
            "proposals": proposals,
        }
        with open(os.path.join(BASE, f"proposals-batch-{n}.json"), "w") as f:
            json.dump(out, f, ensure_ascii=False, indent=2)
            f.write("\n")
        counts[n] = (len(batch["rows"]), len(proposals))
    print("\n=== SUMMARY ===")
    for n in (14, 15, 16, 17):
        rows, props = counts[n]
        print(f"batch {n}: {props} proposals for {rows} rows "
              f"{'OK' if props == rows else 'MISMATCH'}")
    if not all_ok:
        print("\nWARNING: some rows had problems (see above). Files still written.")
        sys.exit(2)
    print("All validations passed.")

if __name__ == "__main__":
    main()
