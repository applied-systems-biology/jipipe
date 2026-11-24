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
    "Legacy variant: applies a mathematical operation between multiple 2D images: f(f(f(image 1, image 2), image 3), image 4), ... The order is determined by JIPipe's merging and cannot currently be controlled. For higher-dimensional data, the operation is applied to each 2D slice. This algorithm groups the incoming data based on the annotations. Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and leave 'Data set matching annotations' empty.",
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
    "Grayscale attribute filtering for 2D images: removes components based on an attribute such as their size or bounding-box diagonal, rather than on intensity, often to clean-up segmentation results. For grayscale input, the image is thresholded at each gray level, attribute opening is applied per binary image, and the results are unioned, so bright structures with an attribute below the threshold disappear; contours are better preserved than with a structuring element. Closing and White/Black top-hat variants are available. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-grayscale-attribute-filtering-3d": (
    "Grayscale attribute filtering for 3D images: removes components based on an attribute such as their size or bounding-box diagonal, rather than on intensity, often to clean-up segmentation results. For grayscale input, the image is thresholded at each gray level, attribute opening is applied per binary image, and the results are unioned, so bright structures with an attribute below the threshold disappear; contours are better preserved than with a structuring element. Closing and White/Black top-hat variants are available. This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-reconstruct-2d": (
    "Performs a morphological (geodesic) reconstruction in 2D by repeating conditional dilations or erosions until idempotence. Two images are required: the marker image, used to initialize the reconstruction, an the mask image, used to constrain the reconstruction. More information: https://imagej.net/plugins/morpholibj This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
"ij1-morph-reconstruct-3d": (
    "Performs a morphological (geodesic) reconstruction in 3D by repeating conditional dilations or erosions until idempotence. Two images are required: the marker image, used to initialize the reconstruction, an the mask image, used to constrain the reconstruction. More information: https://imagej.net/plugins/morpholibj This algorithm groups the incoming data based on the annotations. Those groups can consist of one data item per slot.",
    "gate"),
}
OP2 = {}
