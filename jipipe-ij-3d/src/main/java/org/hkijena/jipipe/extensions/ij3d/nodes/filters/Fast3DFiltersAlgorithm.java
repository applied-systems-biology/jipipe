package org.hkijena.jipipe.extensions.ij3d.nodes.filters;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.Blitter;
import ij.process.StackProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageShort;
import mcib3d.image3d.processing.FastFilters3D;
import mcib_plugins.Filter3D.Filter3Dmax;
import mcib_plugins.Filter3D.Filter3DmaxLocal;
import mcib_plugins.Filter3D.Filter3Dmean;
import mcib_plugins.Filter3D.Filter3Dmin;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Fast3DFiltersAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Quantity kernelX = new Quantity(2, "pixels");
    private Quantity kernelY = new Quantity(2, "pixels");
    private Quantity kernelZ = new Quantity(2, "pixels");

    private int numThreads = 1;

    private boolean preferIsotropicFilter = true;

    public Fast3DFiltersAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Fast3DFiltersAlgorithm(Fast3DFiltersAlgorithm other) {
        super(other);
        this.kernelX = new Quantity(other.kernelX);
        this.kernelY = new Quantity(other.kernelY);
        this.kernelZ = new Quantity(other.kernelZ);
        this.numThreads = other.numThreads;
    }

    protected abstract int getFilterIndex();

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        int radiusX = (int) kernelX.convertToPixels(ImageJUtils.getPixelSizeX(inputImage)).getValue();
        int radiusY = (int) kernelY.convertToPixels(ImageJUtils.getPixelSizeY(inputImage)).getValue();
        int radiusZ = (int) kernelZ.convertToPixels(ImageJUtils.getPixelSizeZ(inputImage)).getValue();
        int filter = getFilterIndex();
        boolean canUseIsotropicFilter = ((radiusX == radiusY) && (radiusX == radiusZ) && ((filter == FastFilters3D.MEAN) || (filter == FastFilters3D.MIN) || (filter == FastFilters3D.MAX) ||
                (filter == FastFilters3D.MAXLOCAL) || (filter == FastFilters3D.TOPHAT) || (filter == FastFilters3D.OPENGRAY) || (filter == FastFilters3D.CLOSEGRAY)));
        ImagePlus outputImage = IJ3DUtils.forEach3DIn5DGenerate(inputImage, (ih, index, ctProgress) -> {
            try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(ctProgress)) {
                if (canUseIsotropicFilter && preferIsotropicFilter) {
                    return ImageHandler.wrap(fastFilterIsotropic(ih.getImagePlus(), radiusX, getFilterIndex()));
                } else {
                    ImageStack stack = ih.getImageStack();
                    int depth = inputImage.getBitDepth();
                    ImageStack res;
                    if ((depth == 8) || (depth == 16)) {
                        res = FastFilters3D.filterIntImageStack(stack, filter, radiusX, radiusY, radiusZ, numThreads, true);
                    } else if (depth == 32) {
                        res = FastFilters3D.filterFloatImageStack(stack, filter, radiusX, radiusY, radiusZ, numThreads, true);
                    } else {
                        throw new UnsupportedOperationException("Unsupported bit depth: " + depth);
                    }
                    return ImageHandler.wrap(new ImagePlus(ih.getTitle(), res));
                }
            }
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Radius (X)", description = "Radius of the ellipsoidal kernel in the X direction")
    @JIPipeParameter("kernel-x")
    public Quantity getKernelX() {
        return kernelX;
    }

    @JIPipeParameter("kernel-x")
    public void setKernelX(Quantity kernelX) {
        this.kernelX = kernelX;
    }

    @JIPipeDocumentation(name = "Radius (Y)", description = "Radius of the ellipsoidal kernel in the Y direction")
    @JIPipeParameter("kernel-y")
    public Quantity getKernelY() {
        return kernelY;
    }

    @JIPipeParameter("kernel-y")
    public void setKernelY(Quantity kernelY) {
        this.kernelY = kernelY;
    }

    @JIPipeDocumentation(name = "Radius (Z)", description = "Radius of the ellipsoidal kernel in the Z direction")
    @JIPipeParameter("kernel-z")
    public Quantity getKernelZ() {
        return kernelZ;
    }

    @JIPipeParameter("kernel-z")
    public void setKernelZ(Quantity kernelZ) {
        this.kernelZ = kernelZ;
    }

    @JIPipeDocumentation(name = "Number of threads", description = "The number of threads for parallel processing")
    @JIPipeParameter("num-threads")
    public int getNumThreads() {
        return numThreads;
    }

    @JIPipeParameter("num-threads")
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @JIPipeDocumentation(name = "Prefer isotropic filter (faster)", description = "If possible, utilize a faster implementation using a rolling ball algorithm. " +
            "Requires that the radii are the same. Not supported on all filters. If isotropic filtering is not possible or disabled, the node will fall back to a parallelized algorithm.")
    @JIPipeParameter("prefer-isotropic-filter")
    public boolean isPreferIsotropicFilter() {
        return preferIsotropicFilter;
    }

    @JIPipeParameter("prefer-isotropic-filter")
    public void setPreferIsotropicFilter(boolean preferIsotropicFilter) {
        this.preferIsotropicFilter = preferIsotropicFilter;
    }

    private ImagePlus fastFilterIsotropic(ImagePlus imp, int radius, int selectedFilter) {
        //read image
        //ImagePlus in_image_j = IJ.getImage();
        ImageStack instack = imp.getStack();
        Duplicator dup = new Duplicator();
        final ImagePlus img = dup.run(imp);
        ImageStack orig = img.getStack();
        ImageShort out3d = new ImageShort("out3d", instack.getWidth(), instack.getHeight(), instack.getSize());
        ImageStack out_image = out3d.getImageStack();
        int rad = radius;
        int de = instack.getSize();

        // Parallelisation DOES NOT WORK !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        final AtomicInteger ai = new AtomicInteger(0);
        //final int n_cpus = nbcpus;
        final int n_cpus = 1;
        final int dec = (int) Math.ceil((double) de / (double) n_cpus);
        //Thread[] threads = ThreadUtil.createThreadArray(n_cpus);

        //process filter
        switch (selectedFilter) {
            case FastFilters3D.MEAN:
                Filter3Dmean mean = new Filter3Dmean(instack, out_image, rad);
                mean.filter();
                break;
            case FastFilters3D.MIN: {
                Filter3Dmin min = new Filter3Dmin(instack, out_image, rad);
                min.filter();

                break;
            }
            case FastFilters3D.MAX: {
                Filter3Dmax max = new Filter3Dmax(instack, out_image, rad);
                max.filter();

                break;
            }
            case FastFilters3D.MAXLOCAL: {
                Filter3DmaxLocal max = new Filter3DmaxLocal(instack, out_image, rad);
                max.filter();
                break;
            }
            case FastFilters3D.TOPHAT: {
                Filter3Dmin min = new Filter3Dmin(instack, out_image, rad);
                min.filter();
                // MAXIMUM
                ImageShort out3d2 = new ImageShort("out3d2", instack.getWidth(), instack.getHeight(), instack.getSize());
                ImageStack out_image2 = out3d2.getImageStack();
                Filter3Dmax max = new Filter3Dmax(out_image, out_image2, rad);
                max.filter();

                StackProcessor stackprocess = new StackProcessor(out_image2, null);
                stackprocess.copyBits(orig, 0, 0, Blitter.SUBTRACT);
                break;
            }
        }
        return new ImagePlus(imp.getTitle(), out_image);
    }
}
