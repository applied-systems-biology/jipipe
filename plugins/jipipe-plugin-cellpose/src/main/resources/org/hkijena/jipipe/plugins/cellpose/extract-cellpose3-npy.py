#!/usr/bin/env/python3

import numpy as np
import argparse 
import os
import json
from cellpose import utils, io

parser = argparse.ArgumentParser(description="Extracts data from a Cellpose *.npy file for the use in JIPipe.")
parser.add_argument("input_files", help="The *.npy file or a directory that contains the *.npy files")
parser.add_argument("output_dir", help="Directory where the outputs will be stored")
parser.add_argument("--skip-roi", help="Skips the extraction of ROI", action="store_true")

args = parser.parse_args()

output_dir = args.output_dir
if not os.path.isdir(output_dir):
    os.makedirs(output_dir)
npy_files = []


def process_probabilities(npy_data, npy_base_output_path):
    print(" - Extracting probabilities")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_probabilities.tif", flows[1])

def process_flows_rgb(npy_data, npy_base_output_path):
    print(" - Extracting flows (x, y, RGB)")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_flows_rgb.tif", flows[0])

def process_flows_z(npy_data, npy_base_output_path):
    print(" - Extracting flows (Z)")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_flows_z.tif", flows[1])

def process_flows_dz_dy_dx(npy_data, npy_base_output_path):
    print(" - Extracting flows (dz, dy, dx)")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_flows_dz_dy_dx.tif", flows[4])

def process_labels(npy_data, npy_base_output_path):
    print(" - Extracting labels/masks")
    masks = npy_data.item().get("masks")
    if masks.dtype != np.short and masks.dtype != np.uint8:
        masks = masks.astype(np.float32)
    io.imsave(npy_base_output_path + "_labels.tif", masks)

def process_roi(npy_data, npy_base_output_path):
    print(" - Extracting ROI")
    masks = npy_data.item().get("masks")
    roi_list = []
    if masks.ndim == 3:
        for z in range(masks.shape[0]):
            coords_list = utils.outlines_list(masks[z,:,:])
            for coords in coords_list:
                roi = dict(z=z, coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])
                roi_list.append(roi)
    else:
        coords_list = utils.outlines_list(masks)
        for coords in coords_list:
            roi = dict(coords=[ dict(x=int(x[0]), y=int(x[1])) for x in coords ])
            roi_list.append(roi)
    with open(npy_base_output_path + "_roi.json", "w") as f:
        json.dump(roi_list, f, indent=4)

def process_additional_info(npy_data, npy_base_output_path):
    print(" - Extracting info")
    json_data = {
        "chan_choose": npy_data.item().get("chan_choose"),
        "est_diam": npy_data.item().get("diameter")
    }
    with open(npy_base_output_path + "_info.json", "w") as f:
        json.dump(json_data, f, indent=4)

def process_npy_file(npy_file, skip_roi=False):
    npy_base_output_path = output_dir + "/" + os.path.basename(npy_file)[:-4]

    print("Loading " + npy_file)
    npy_data = np.load(npy_file, allow_pickle=True)
    print("Available data: " + str(npy_data.item().keys()))

    process_probabilities(npy_data, npy_base_output_path)
    process_flows_rgb(npy_data, npy_base_output_path)
    process_flows_z(npy_data, npy_base_output_path)
    process_flows_dz_dy_dx(npy_data, npy_base_output_path)
    process_labels(npy_data, npy_base_output_path)
    if not skip_roi:
        process_roi(npy_data, npy_base_output_path)
    else:
        print(" - Extracting ROI ... skipped")
    process_additional_info(npy_data, npy_base_output_path)

def main():
    skip_roi = args.skip_roi
    if os.path.isfile(args.input_files):
        npy_files.append(args.input_files)
    else:
        for f in os.listdir(args.input_files):
            if f.endswith(".npy"):
                npy_files.append(args.input_files + "/" + f)
    print("Detected the following *.npy files:")
    for f in npy_files:
        print(" - " + f)

    for npy_file in npy_files:
        process_npy_file(npy_file, skip_roi=skip_roi)

main()