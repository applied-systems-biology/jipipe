#!/usr/bin/env/python3

import numpy as np
import argparse 
import os
import json
from cellpose import utils, io

parser = argparse.ArgumentParser(description="Extracts data from a Cellpose *.npy file for the use in JIPipe.")
parser.add_argument("input_files", help="The *.npy file or a directory that contains the *.npy files")
parser.add_argument("output_dir", help="Directory where the outputs will be stored")

args = parser.parse_args()

output_dir = args.output_dir
if not os.path.isdir(output_dir):
    os.makedirs(output_dir)
npy_files = []


def process_restored_img(npy_data, npy_base_output_path):
    print(" - Extracting restored image")
    img_restore = npy_data.item().get("img_restore")
    io.imsave(npy_base_output_path + "_restored.tif", img_restore)

def process_additional_info(npy_data, npy_base_output_path):
    print(" - Extracting info")
    json_data = {
        "chan_choose": npy_data.item().get("chan_choose"),
        "est_diam": npy_data.item().get("diameter")
    }
    with open(npy_base_output_path + "_info.json", "w") as f:
        json.dump(json_data, f, indent=4)

def process_npy_file(npy_file):
    npy_base_output_path = output_dir + "/" + os.path.basename(npy_file)[:-4]

    print("Loading " + npy_file)
    npy_data = np.load(npy_file, allow_pickle=True)
    print("Available data: " + str(npy_data.item().keys()))

    process_restored_img(npy_data, npy_base_output_path)
    process_additional_info(npy_data, npy_base_output_path)

def main():
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
        process_npy_file(npy_file)

main()