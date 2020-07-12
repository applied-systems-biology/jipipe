CLIJ2 is a GPU-accelerated image processing library for ImageJ/Fiji, Icy, Matlab and Java. It comes with hundreds of operations for filtering, binarizing, labeling, measuring in images, projections, transformations and mathematical operations for images. While most of these are classical image processing operations, CLIJ2 also allows performing operations on matrices potentially representing neighborhood relationships between cells and pixels.
JIPipe provides a plugin that integrates CLIJ into JIPipe.

# Trouble shooting

CLIJ2 requires a modern graphics card that supports OpenCL 1.2 or higher (lesser versions might work, but can be the cause for lower precision).
A common issue is that the hardware is not properly detected by CLIJ due to missing system libraries.

If you have issues, please update your graphics card drivers. On Linux, CLIJ2 makes use of `libOpencv.so`. On Ubuntu,
this library must be manually installed via the `ocl-icd-opencl-dev` package.

Please visit the CLIJ2 website and forums if you still have issues.

# Citation

Please cite following work if you use CLIJ2:

Robert Haase, Loic Alain Royer, Peter Steinbach, Deborah Schmidt, Alexandr Dibrov, Uwe Schmidt, Martin Weigert, Nicola Maghelli, Pavel Tomancak, Florian Jug, Eugene W Myers. CLIJ: GPU-accelerated image processing for everyone. Nat Methods 17, 5-6 (2020) doi:10.1038/s41592-019-0650-1