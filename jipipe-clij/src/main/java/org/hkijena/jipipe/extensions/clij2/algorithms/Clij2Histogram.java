package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Histogram;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Histogram}
 */
@JIPipeDocumentation(name = "CLIJ2 Histogram", description = "Determines the histogram of a given image." + "The histogram image is of dimensions number_of_bins/1/1; a 3D image with height=1 and depth=1. " + "Histogram bins contain the number of pixels with intensity in this corresponding bin. " + "The histogram bins are uniformly distributed between given minimum and maximum grey value intensity. " + "If the flag determine_min_max is set, minimum and maximum intensity will be determined. " + "When calling this operation many times, it is recommended to determine minimum and maximum intensity " + "once at the beginning and handing over these values. Works for following image dimensions: 2D, 3D. Developed by Robert Haase adapted work from Aaftab Munshi, Benedict Gaster, Timothy Mattson, James Fung, Dan Ginsburg. // adapted code from" + "// https://github.com/bgaster/opencl-book-samples/blob/master/src/Chapter_14/histogram/histogram_image.cl" + "//" + "// It was published unter BSD license according to" + "// https://code.google.com/archive/p/opencl-book-samples/" + "//" + "// Book:      OpenCL(R) Programming Guide" + "// Authors:   Aaftab Munshi, Benedict Gaster, Timothy Mattson, James Fung, Dan Ginsburg" + "// ISBN-10:   0-321-74964-2" + "// ISBN-13:   978-0-321-74964-2" + "// Publisher: Addison-Wesley Professional" + "// URLs:      http://safari.informit.com/9780132488006/" + "//            http://www.openclprogrammingguide.com")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "histogram", autoCreate = true)

public class Clij2Histogram extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Histogram(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Histogram(Clij2Histogram other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer histogram = clij2.create(src);
        Histogram.histogram(clij2, src, histogram);

        dataBatch.addOutputData(getOutputSlot("histogram"), new CLIJImageData(histogram), progressInfo);
    }

}