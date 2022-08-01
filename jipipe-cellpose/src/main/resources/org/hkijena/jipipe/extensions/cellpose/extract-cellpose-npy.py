#!/usr/bin/env/python3

# Info: Cellpose 2.0 Flows array: 0 = flows (RGB), 1 = Probabilities, 2 = ???, 3 = ???, 4 = ???

import numpy as np
import argparse 
import os
from cellpose import utils, io

parser = argparse.ArgumentParser(description="Extracts data from a Cellpose *.npy file for the use in JIPipe.")
parser.add_argument("input_files", help="The *.npy file or a directory that contains the *.npy files")
parser.add_argument("output_dir", help="Directory where the outputs will be stored")

args = parser.parse_args()

output_dir = args.output_dir
os.makedirs(output_dir)
npy_files = []


def process_probabilities(npy_data, npy_base_output_path):
    print(" - Extracting probabilities")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_probabilities.tif", flows[1])

def process_flows_rgb(npy_data, npy_base_output_path):
    print(" - Extracting flows (RGB)")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_flows_rgb.tif", flows[0])

def process_flows_multichannel(npy_data, npy_base_output_path):
    print(" - Extracting flows (Multichannel)")
    flows = npy_data.item().get("flows")
    io.imsave(npy_base_output_path + "_flows_multichannel.tif", flows[4])

def process_npy_file(npy_file):
    npy_base_output_path = output_dir + "/" + os.path.basename(npy_file)[:-4]

    print("Loading " + npy_file)
    npy_data = np.load(npy_file, allow_pickle=True)
    print("Available data: " + str(npy_data.item().keys()))
    img = npy_data.item().get("img")
    print("Input image has dimensions " + str(img.shape))

    process_probabilities(npy_data, npy_base_output_path)
    process_flows_rgb(npy_data, npy_base_output_path)
    process_flows_multichannel(npy_data, npy_base_output_path)

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