package org.hkijena.jipipe.extensions.ij3d.nodes.binary;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.GaussianBlur3D;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.distanceMap3d.EDT;
import mcib3d.image3d.processing.FastFilters3D;
import mcib3d.image3d.regionGrowing.Watershed3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Watershed 3D splitting", description = "The main application of watershed in ImageJ is the 2D splitting of merged objects.\n" +
        "\n" +
        "This splitting is based on the computation of the Distance Map inside the mask of the merged objects. " +
        "The seeds are then the local maxima of the distance map, the farthest points from the boundaries, hence corresponding to the centres of the objects. " +
        "The bigger the object, the higher the values of the distance map at the centre, then the faster the growing of the seeds and the larger the resulting object.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Seeds", autoCreate = true, optional = true, description = "Optional seeds")
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Labels", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Distance transform", autoCreate = true)
public class Watershed3DSplittingAlgorithm extends JIPipeIteratingAlgorithm {

    private int radius = 2;

    public Watershed3DSplittingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Watershed3DSplittingAlgorithm(Watershed3DSplittingAlgorithm other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData("Input", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
        ImagePlus seedsImage = ImageJUtils.unwrap(dataBatch.getInputData("Seeds", ImagePlusGreyscaleMaskData.class, progressInfo));

        Map<ImageSliceIndex, ImageProcessor> labelMap = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> edtMap = new HashMap<>();
        IJ3DUtils.forEach3DIn5DIO(inputImage, (mask3D, index, ctProgress) -> {

            ImagePlus binaryMask = mask3D.getImagePlus();
            ImagePlus seedPlus = seedsImage != null ? ImageJUtils.extractCTStack(seedsImage, index.getC(), index.getT()) : null;

            float resXY = 1;
            float resZ = 1;
            float radXY = radius;
            float radZ = radius;
            Calibration cal = binaryMask.getCalibration();
            if (cal != null) {
                resXY = (float) cal.pixelWidth;
                resZ = (float) cal.pixelDepth;
                radZ = radXY * (resXY / resZ);
            }
            IJ.log("Computing EDT");
            ImageInt imgMask = ImageInt.wrap(binaryMask);
            ImageFloat edt = EDT.run(imgMask, 0, resXY, resZ, false, 0);
            ImageHandler edt16 = edt.convertToShort(true);
            IJ.log("Smoothing EDT");
            ImagePlus edt16Plus = edt16.getImagePlus();
            GaussianBlur3D.blur(edt16Plus, 2.0, 2.0, 2.0);
            edt16 = ImageInt.wrap(edt16Plus);
            edt16.intersectMask(imgMask);

            // seeds
            ImageHandler seedsImg;
            if (seedPlus == null) {
                IJ.log("computing seeds as max local of EDT");
                seedsImg = FastFilters3D.filterImage(edt16, FastFilters3D.MAXLOCAL, radXY, radXY, radZ, 0, false);
            } else {
                seedsImg = ImageInt.wrap(seedPlus);
            }
            IJ.log("Computing watershed");
            Watershed3D water = new Watershed3D(edt16, seedsImg, 0, 0);
            water.setLabelSeeds(true);
            water.setAnim(false);

            ImageHandler watershedImage3D = water.getWatershedImage3D();

            IJ3DUtils.putToMap(watershedImage3D, index.getC(), index.getT(), labelMap);
            IJ3DUtils.putToMap(edt16, index.getC(), index.getT(), edtMap);

        }, progressInfo);

        ImagePlus outputLabels = ImageJUtils.mergeMappedSlices(labelMap);
        outputLabels.copyScale(inputImage);
        dataBatch.addOutputData("Labels", new ImagePlusGreyscaleData(outputLabels), progressInfo);
        ImagePlus outputEDT = ImageJUtils.mergeMappedSlices(edtMap);
        outputEDT.copyScale(inputImage);
        dataBatch.addOutputData("Distance transform", new ImagePlusGreyscaleData(outputEDT), progressInfo);
    }

    @JIPipeDocumentation(name = "Radius (pixel)")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(int radius) {
        this.radius = radius;
    }
}
