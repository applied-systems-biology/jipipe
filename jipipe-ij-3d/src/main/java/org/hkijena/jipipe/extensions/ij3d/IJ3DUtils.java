/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ij3d;

import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import mcib3d.geom.*;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.lang3.function.TriFunction;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DMeasurement;
import org.hkijena.jipipe.extensions.ij3d.utils.ROI3DRelationMeasurement;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TriConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IJ3DUtils {

    public static void forEach3DIn5DIO(ImagePlus imagePlus, TriConsumer<ImageHandler, ImageSliceIndex, JIPipeProgressInfo> operation, JIPipeProgressInfo progressInfo) {
        ImageJUtils.forEachIndexedCTStack(imagePlus, (imp, index, ctProgress) -> {
            ImageHandler imageHandler = ImageHandler.wrap(imp);
            operation.accept(imageHandler, index, ctProgress);

            // Copy data back into the imagePlus
            ImagePlus imp2 = imageHandler.getImagePlus();
            ImageStack stack = imp2.getStack();
            for (int i = 0; i < stack.getSize(); i++) {
                ImageProcessor processor = stack.getProcessor(i + 1);
                ImageJUtils.setSliceZero(imagePlus, processor, new ImageSliceIndex(index.getC(), i, index.getT()));
            }

        }, progressInfo);
    }

    public static ImagePlus forEach3DIn5DGenerate(ImagePlus sourceImage, TriFunction<ImageHandler, ImageSliceIndex, JIPipeProgressInfo, ImageHandler> operation, JIPipeProgressInfo progressInfo) {
        Map<ImageSliceIndex, ImageProcessor> mappedOutputSlices = new HashMap<>();
        ImageJUtils.forEachIndexedCTStack(sourceImage, (imp, index, ctProgress) -> {
            ImageHandler input = ImageHandler.wrap(imp);
            ImageHandler result = operation.apply(input, index, ctProgress);

            // Copy data back into the imagePlus
            ImagePlus imp2 = result.getImagePlus();
            ImageStack stack = imp2.getStack();

            for (int i = 0; i < stack.size(); i++) {
                ImageProcessor processor = stack.getProcessor(i + 1);
                ImageSliceIndex targetIndex = new ImageSliceIndex(index.getC(), i, index.getT());
                mappedOutputSlices.put(targetIndex, processor);
            }

        }, progressInfo);
        ImagePlus outputImage = ImageJUtils.mergeMappedSlices(mappedOutputSlices);
        outputImage.setTitle(sourceImage.getTitle());
        outputImage.copyScale(sourceImage);
        return outputImage;
    }

    /**
     * Converts 2D ROI to 3D ROI
     *
     * @param roi2DList    the 2D ROI list
     * @param force2D      force the creation of 2D ROI. always true if the fast mode is inactive
     * @param fast         use masks instead of processing the ROI per component. if false, automatically implies false2D
     * @param neighborhood the neighborhood
     * @param progressInfo the progress info
     * @return the 3D ROI
     */
    public static ROI3DListData roi2DtoRoi3D(ROIListData roi2DList, boolean force2D, boolean fast, Neighborhood3D neighborhood, JIPipeProgressInfo progressInfo) {
        ROI3DListData roi3DList = new ROI3DListData();

        // Put into groups
        Map<ImageSliceIndex, List<Roi>> grouped;
        if (force2D || !fast) {
            grouped = roi2DList.groupByPosition(true, true, true);
        } else {
            grouped = roi2DList.groupByPosition(false, true, true);
        }
        ImmutableList<Map.Entry<ImageSliceIndex, List<Roi>>> groups = ImmutableList.copyOf(grouped.entrySet());

        if (fast) {
            // Use mask-based object generation, but per group
            for (int i = 0; i < groups.size(); i++) {
                if (progressInfo.isCancelled())
                    return null;
                Map.Entry<ImageSliceIndex, List<Roi>> group = groups.get(i);
                progressInfo.resolveAndLog("Slice", i, groups.size());

                ROIListData forGroup = new ROIListData();
                forGroup.addAll(group.getValue());
                ImagePlus mask = forGroup.toMask(new Margin(), true, true, 1);

                progressInfo.log("Detecting connected components ...");
                ImagePlus labels = ImageJAlgorithmUtils.connectedComponents3D(mask, neighborhood, 32);
                ImageFloat imageHandler = new ImageFloat(labels);
                Objects3DPopulation population = new Objects3DPopulation(imageHandler);

                roi3DList.addFromPopulation(population, group.getKey().getC() + 1, group.getKey().getT() + 1);
            }
        } else {
            for (int i = 0; i < groups.size(); i++) {
                if (progressInfo.isCancelled())
                    return null;
                Map.Entry<ImageSliceIndex, List<Roi>> group = groups.get(i);
                JIPipeProgressInfo sliceProgress = progressInfo.resolveAndLog("Slice", i, groups.size());
                List<Roi> rois = group.getValue();
                for (int j = 0; j < rois.size(); j++) {
                    Roi roi = rois.get(j);
                    sliceProgress.resolveAndLog("ROI", j, rois.size());

                    ROIListData singleton = new ROIListData();
                    singleton.add(roi);

                    ImagePlus mask = singleton.toMask(new Margin(), true, true, 1);
                    ImageHandler imageHandler = ImageHandler.wrap(mask);
                    Objects3DPopulation population = new Objects3DPopulation(imageHandler);

                    for (ROI3D roi3D : roi3DList.addFromPopulation(population, group.getKey().getC() + 1, group.getKey().getT() + 1)) {
                        if (roi.getFillColor() != null) {
                            roi3D.setFillColor(roi.getFillColor());
                        }
                        if (!StringUtils.isNullOrEmpty(roi.getName())) {
                            roi3D.getObject3D().setName(roi.getName());
                        }
                    }
                }

            }
        }
        return roi3DList;
    }

    public static ImageHandler wrapImage(ImagePlusData imagePlusData) {
        if (imagePlusData != null) {
            return ImageHandler.wrap(imagePlusData.getImage());
        } else {
            return null;
        }
    }

    /**
     * Duplicates an {@link Object3D}
     *
     * @param other the object to copy
     * @return the copied object
     */
    public static Object3D duplicateObject3D(Object3D other) {
        // TODO: Handle other Object3D cases
        List<Voxel3D> voxels = new ArrayList<>();
        for (Voxel3D voxel : other.getVoxels()) {
            voxels.add(new Voxel3D(voxel.x, voxel.y, voxel.z, voxel.value));
        }
        Object3DVoxels result = new Object3DVoxels(voxels);
        result.setCalibration(other.getResXY(), other.getResZ(), other.getUnits());
        return result;
    }

    public static void measureRoi3d(ImageHandler referenceImage, ROI3DListData roiList, int measurements, boolean physicalUnits, String columnPrefix, ResultsTableData target, JIPipeProgressInfo progressInfo) {
        int lastPercentage = 0;
        for (int i = 0; i < roiList.size(); i++) {
            if (progressInfo.isCancelled()) {
                return;
            }
            int newPercentage = (int) (1.0 * i / roiList.size() * 100);
            if (lastPercentage != newPercentage) {
                progressInfo.log(i + "/" + roiList.size() + " (" + newPercentage + "%)");
                lastPercentage = newPercentage;
            }
            int row = target.addRow();
            generateRoi3dRowMeasurements(referenceImage, i, roiList.get(i), measurements, physicalUnits, target, row, columnPrefix);
        }
    }

    public static void measureRoi3dRelation(ImageHandler referenceImage, ROI3DListData roi1List, ROI3DListData roi2List, int measurements, boolean physicalUnits, boolean requireColocalization, boolean preciseColocalization, boolean ignoreC, boolean ignoreT, String columnPrefix, ResultsTableData target, JIPipeProgressInfo progressInfo) {
        int maxItems = roi1List.size() * roi2List.size();
        int currentItems = 0;
        int lastPercentage = 0;
        for (int i = 0; i < roi1List.size(); i++) {
            ROI3D roi1 = roi1List.get(i);
            for (int j = 0; j < roi2List.size(); j++) {
                ROI3D roi2 = roi2List.get(j);
                ++currentItems;
                if (progressInfo.isCancelled()) {
                    return;
                }
                int newPercentage = (int) (1.0 * currentItems / maxItems * 100);
                if (lastPercentage != newPercentage) {
                    progressInfo.log(currentItems + "/" + maxItems + " (" + newPercentage + "%)");
                    lastPercentage = newPercentage;
                }

                if(!ignoreT) {
                    if(roi1.getFrame() != roi2.getFrame()) {
                        continue;
                    }
                }
                if(!ignoreC) {
                    if(roi1.getChannel() != roi2.getChannel()) {
                        continue;
                    }
                }

                if (requireColocalization) {
                    if (!roi1.getObject3D().overlapBox(roi2.getObject3D())) {
                        continue;
                    }
                    if (preciseColocalization) {
                        if (!roi1.getObject3D().hasOneVoxelColoc(roi2.getObject3D())) {
                            continue;
                        }
                    }
                }

                int row = target.addRow();
                generateRoi3dRelationRowMeasurements(referenceImage, i, j, measurements, physicalUnits, target, roi1, roi2, row, columnPrefix);
            }
        }
    }

    public static void generateRoi3dRelationRowMeasurements(ImageHandler reference, int roi1Index, int roi2Index, int measurements, boolean physicalUnits, ResultsTableData target, ROI3D roi1, ROI3D roi2, int row, String columnPrefix) {
        Object3D object1 = roi1.getObject3D();
        Object3D object2 = roi2.getObject3D();

        double object1ResXY = object1.getResXY();
        double object1ResZ = object1.getResZ();
        String object1Unit = object1.getUnits();
        double object2ResXY = object2.getResXY();
        double object2ResZ = object2.getResZ();
        String object2Unit = object2.getUnits();

        if (!physicalUnits) {
            object1.setResXY(1);
            object1.setResZ(1);
            object1.setUnits("pixels");
            object2.setResXY(1);
            object2.setResZ(1);
            object2.setUnits("pixels");
        }

        // Mandatory!
        target.setValueAt(roi1.getObject3D().getName(), row, "Current.Name");
        target.setValueAt(roi2.getObject3D().getName(), row, "Other.Name");
        target.setValueAt(roi1Index, row, "Current.Index");
        target.setValueAt(roi2Index, row, "Other.Index");


        try {
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.Colocalization)) {
                target.setValueAt(object1.getColoc(object2), row, columnPrefix + "Colocalization");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.PercentageColocalization)) {
                target.setValueAt(object1.pcColoc(object2), row, columnPrefix + "PercentageColocalization");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.OverlapsBox)) {
                boolean value = object1.overlapBox(object2);
                target.setValueAt(value ? 1 : 0, row, columnPrefix + "OverlapsBox");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.Includes)) {
                boolean value = object1.includes(object2);
                target.setValueAt(value ? 1 : 0, row, columnPrefix + "Includes");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.IncludesBox)) {
                boolean value = object1.includesBox(object2);
                target.setValueAt(value ? 1 : 0, row, columnPrefix + "IncludesBox");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.RadiusCenter)) {
                target.setValueAt(object1.radiusCenter(object2), row, columnPrefix + "RadiusCenter");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.RadiusCenterOpposite)) {
                target.setValueAt(object1.radiusCenter(object2, true), row, columnPrefix + "RadiusCenterOpposite");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.DistanceCenter2D)) {
                target.setValueAt(object1.distCenter2DUnit(object2), row, columnPrefix + "DistanceCenter2D");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.DistanceCenter)) {
                if (physicalUnits) {
                    target.setValueAt(object1.distCenterUnit(object2), row, columnPrefix + "DistanceCenter");
                } else {
                    target.setValueAt(object1.distCenterPixel(object2), row, columnPrefix + "DistanceCenter");
                }
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.DistanceHausdorff)) {
                target.setValueAt(object1.distHausdorffUnit(object2), row, columnPrefix + "DistanceHausdorff");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.DistanceBorder)) {
                if (physicalUnits) {
                    target.setValueAt(object1.distBorderUnit(object2), row, columnPrefix + "DistanceBorder");
                } else {
                    target.setValueAt(object1.distBorderPixel(object2), row, columnPrefix + "DistanceBorder");
                }
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.DistanceCenterBorder)) {
                target.setValueAt(object1.distCenterBorderUnit(object2), row, columnPrefix + "DistanceCenterBorder");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.EdgeContactColocalization)) {
                target.setValueAt(object1.edgeContact(object2, 0), row, columnPrefix + "EdgeContactColocalization");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.EdgeContactSide)) {
                target.setValueAt(object1.edgeContact(object2, 1), row, columnPrefix + "EdgeContactSide");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.EdgeContactDiagonal)) {
                target.setValueAt(object1.edgeContact(object2, 2), row, columnPrefix + "EdgeContactDiagonal");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.IntersectionStats)) {
                Object3DVoxels intersectionObject = object1.getIntersectionObject(object2);
                if (intersectionObject != null) {
                    generateRoi3dRowMeasurements(reference, -1, new ROI3D(intersectionObject), 38904, physicalUnits, target, row, "Intersection.");
                }
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.CurrentStats)) {
                generateRoi3dRowMeasurements(reference, roi1Index, roi1, 65536, physicalUnits, target, row, "Current.");
            }
            if (ROI3DRelationMeasurement.includes(measurements, ROI3DRelationMeasurement.OtherStats)) {
                generateRoi3dRowMeasurements(reference, roi2Index, roi2, 65536, physicalUnits, target, row, "Other.");
            }
        } finally {
            // Restore units
            object1.setResXY(object1ResXY);
            object1.setResZ(object1ResZ);
            object1.setUnits(object1Unit);
            object2.setResXY(object2ResXY);
            object2.setResZ(object2ResZ);
            object2.setUnits(object2Unit);
        }
    }

    public static void generateRoi3dRowMeasurements(ImageHandler referenceImage, int index, ROI3D roi3D, int measurements, boolean physicalUnits, ResultsTableData target, int row, String columnPrefix) {
        Object3D object3D = roi3D.getObject3D();

        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Index)) {
            target.setValueAt(index, row, columnPrefix + "Index");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Name)) {
            target.setValueAt(StringUtils.nullToEmpty(object3D.getName()), row, columnPrefix + "Name");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Comment)) {
            target.setValueAt(StringUtils.nullToEmpty(object3D.getComment()), row, columnPrefix + "Comment");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Location)) {
            target.setValueAt(StringUtils.nullToEmpty(roi3D.getChannel()), row, columnPrefix + "Channel");
            target.setValueAt(StringUtils.nullToEmpty(roi3D.getFrame()), row, columnPrefix + "Frame");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Color)) {
            target.setValueAt(ColorUtils.colorToHexString(roi3D.getFillColor()), row, columnPrefix + "FillColor");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.CustomMetadata)) {
            for (Map.Entry<String, String> entry : roi3D.getMetadata().entrySet()) {
                target.setValueAt(entry.getValue(), row, columnPrefix + "Metadata." + entry.getKey());
            }
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Area)) {
            double value;
            if (physicalUnits) {
                value = object3D.getAreaUnit();
            } else {
                value = object3D.getAreaPixels();
            }
            target.setValueAt(value, row, columnPrefix + "Area");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Volume)) {
            double value;
            if (physicalUnits) {
                value = object3D.getVolumeUnit();
            } else {
                value = object3D.getVolumePixels();
            }
            target.setValueAt(value, row, columnPrefix + "Volume");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Center)) {
            Vector3D value;
            if (physicalUnits) {
                value = object3D.getCenterAsVectorUnit();
            } else {
                value = object3D.getCenterAsVector();
            }
            target.setValueAt(value.getX(), row, columnPrefix + "CenterX");
            target.setValueAt(value.getY(), row, columnPrefix + "CenterY");
            target.setValueAt(value.getZ(), row, columnPrefix + "CenterZ");

            if (referenceImage != null) {
                double centerValue = object3D.getPixCenterValue(referenceImage);
                target.setValueAt(centerValue, row, columnPrefix + "CenterPixelValue");
            } else {
                target.setValueAt(Double.NaN, row, columnPrefix + "CenterPixelValue");
            }
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.ShapeMeasurements)) {
            target.setValueAt(object3D.getCompactness(), row, columnPrefix + "Compactness");
            target.setValueAt(object3D.getSphericity(), row, columnPrefix + "Sphericity");
            target.setValueAt(object3D.getFeret(), row, columnPrefix + "Feret");
            target.setValueAt(object3D.getMainElongation(), row, columnPrefix + "MainElongation");
            target.setValueAt(object3D.getMedianElongation(), row, columnPrefix + "MedianElongation");
            target.setValueAt(object3D.getRatioBox(), row, columnPrefix + "RatioBox");
            target.setValueAt(object3D.getRatioEllipsoid(), row, columnPrefix + "RatioEllipsoid");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.BoundingBox)) {
            target.setValueAt(object3D.getXmin(), row, columnPrefix + "BoundingBoxMinX");
            target.setValueAt(object3D.getXmax(), row, columnPrefix + "BoundingBoxMaxX");
            target.setValueAt(object3D.getYmin(), row, columnPrefix + "BoundingBoxMinY");
            target.setValueAt(object3D.getYmax(), row, columnPrefix + "BoundingBoxMaxY");
            target.setValueAt(object3D.getZmin(), row, columnPrefix + "BoundingBoxMinZ");
            target.setValueAt(object3D.getZmax(), row, columnPrefix + "BoundingBoxMaxZ");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.DistCenterStats)) {
            double max, mean, sigma;
            if (physicalUnits) {
                max = object3D.getDistCenterMax();
            } else {
                max = object3D.getDistCenterMaxPixel();
            }
            if (physicalUnits) {
                mean = object3D.getDistCenterMean();
            } else {
                mean = object3D.getDistCenterMeanPixel();
            }
            if (physicalUnits) {
                sigma = object3D.getDistCenterSigma();
            } else {
                sigma = object3D.getDistCenterSigmaPixel();
            }
            target.setValueAt(max, row, columnPrefix + "DistCenterMax");
            target.setValueAt(mean, row, columnPrefix + "DistCenterMean");
            target.setValueAt(sigma, row, columnPrefix + "DistCenterSigma");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.PixelValueStats)) {
            if (referenceImage != null) {
                double max = object3D.getPixMaxValue(referenceImage);
                double min = object3D.getPixMinValue(referenceImage);
                double mean = object3D.getPixMeanValue(referenceImage);
                double sigma = object3D.getPixStdDevValue(referenceImage);
                double mode = object3D.getPixModeValue(referenceImage);
                double modeNonZero = object3D.getPixModeNonZero(referenceImage);
                double median = object3D.getPixMedianValue(referenceImage);
                double intDen = object3D.getIntegratedDensity(referenceImage);
                target.setValueAt(max, row, columnPrefix + "PixelValueMax");
                target.setValueAt(min, row, columnPrefix + "PixelValueMin");
                target.setValueAt(mean, row, columnPrefix + "PixelValueMean");
                target.setValueAt(median, row, columnPrefix + "PixelValueMedian");
                target.setValueAt(sigma, row, columnPrefix + "PixelValueStdDev");
                target.setValueAt(mode, row, columnPrefix + "PixelValueMode");
                target.setValueAt(modeNonZero, row, columnPrefix + "PixelValueModeNonZero");
                target.setValueAt(intDen, row, columnPrefix + "PixelValueIntDen");
            } else {
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueMax");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueMin");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueMean");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueMedian");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueStdDev");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueMode");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueModeNonZero");
                target.setValueAt(Double.NaN, row, columnPrefix + "PixelValueIntDen");
            }
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.ContourPixelValueStats)) {
            if (referenceImage != null) {
                double mean = object3D.getPixMeanValueContour(referenceImage);
                target.setValueAt(mean, row, columnPrefix + "ContourPixelValueMean");
            } else {
                target.setValueAt(Double.NaN, row, columnPrefix + "ContourPixelValueMean");
            }
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.Calibration)) {
            target.setValueAt(object3D.getResXY(), row, columnPrefix + "ResolutionXY");
            target.setValueAt(object3D.getResZ(), row, columnPrefix + "ResolutionZ");
            target.setValueAt(object3D.getUnits(), row, columnPrefix + "ResolutionUnit");
        }
        if (ROI3DMeasurement.includes(measurements, ROI3DMeasurement.MassCenter)) {
            if (referenceImage != null) {
                double x = object3D.getMassCenterX(referenceImage);
                double y = object3D.getMassCenterY(referenceImage);
                double z = object3D.getMassCenterZ(referenceImage);
                target.setValueAt(x, row, columnPrefix + "MassCenterX");
                target.setValueAt(y, row, columnPrefix + "MassCenterY");
                target.setValueAt(z, row, columnPrefix + "MassCenterZ");
            } else {
                target.setValueAt(Double.NaN, row, columnPrefix + "MassCenterX");
                target.setValueAt(Double.NaN, row, columnPrefix + "MassCenterY");
                target.setValueAt(Double.NaN, row, columnPrefix + "MassCenterZ");
            }
        }
    }

    public static void putToMap(ImageHandler imageHandler, int c, int t, Map<ImageSliceIndex, ImageProcessor> sliceMap) {
        ImagePlus imagePlus = imageHandler.getImagePlus();
        ImageStack stack = imagePlus.getStack();
        for (int z = 0; z < stack.getSize(); z++) {
            sliceMap.put(new ImageSliceIndex(c, z, t), stack.getProcessor(z + 1));
        }
    }
}
