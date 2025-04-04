/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.filter.Analyzer;
import ij.process.*;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipePercentageProgressInfo;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.ROI2DRelationMeasurement;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJROIUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.jogamp.vecmath.Point2d;

public class ImageJAlgorithmUtils {

    public static ImagePlus connectedComponents3D(ImagePlus inputImage, Neighborhood3D connectivity, int bitDepth) {
        ImageStack outputStack = BinaryImages.componentsLabeling(inputImage.getImageStack(), connectivity.getNativeValue(), bitDepth);
        ImagePlus outputImage = new ImagePlus("Labels", outputStack);
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());

        double nLabels = LabelImages.findLargestLabel(outputImage);
        outputImage.setDisplayRange(0, nLabels);
        return outputImage;
    }

    public static ByteProcessor getLabelMask(ImageProcessor processor, int label) {
        ByteProcessor result = new ByteProcessor(processor.getWidth(), processor.getHeight());
        byte[] resultPixels = (byte[]) result.getPixels();
        if (processor instanceof ByteProcessor) {
            byte[] pixels = (byte[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if ((pixels[i] & 0xff) == label) {
                    resultPixels[i] = (byte) 255;
                }
            }
        } else if (processor instanceof ShortProcessor) {
            short[] pixels = (short[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if ((pixels[i] & 0xffff) == label) {
                    resultPixels[i] = (byte) 255;
                }
            }
        } else if (processor instanceof FloatProcessor) {
            float[] pixels = (float[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if ((int) pixels[i] == label) {
                    resultPixels[i] = (byte) 255;
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported label type!");
        }
        return result;
    }

    public static ByteProcessor getThresholdMask(ImageProcessor processor, float minThreshold) {
        ByteProcessor result = new ByteProcessor(processor.getWidth(), processor.getHeight());
        byte[] resultPixels = (byte[]) result.getPixels();
        if (processor instanceof ByteProcessor) {
            byte[] pixels = (byte[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if ((pixels[i] & 0xff) >= minThreshold) {
                    resultPixels[i] = (byte) 255;
                }
            }
        } else if (processor instanceof ShortProcessor) {
            short[] pixels = (short[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if ((pixels[i] & 0xffff) >= minThreshold) {
                    resultPixels[i] = (byte) 255;
                }
            }
        } else if (processor instanceof FloatProcessor) {
            float[] pixels = (float[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] >= minThreshold) {
                    resultPixels[i] = (byte) 255;
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported label type!");
        }
        return result;
    }

    public static void removeLabelsExcept(ImageProcessor processor, int[] labelsToKeep) {
        TIntSet labelsToKeep_ = new TIntHashSet();
        for (int i : labelsToKeep) {
            labelsToKeep_.add(i);
        }
        if (processor instanceof ByteProcessor) {
            byte[] pixels = (byte[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if (!labelsToKeep_.contains(pixels[i] & 0xff)) {
                    pixels[i] = 0;
                }
            }
        } else if (processor instanceof ShortProcessor) {
            short[] pixels = (short[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if (!labelsToKeep_.contains(pixels[i] & 0xffff)) {
                    pixels[i] = 0;
                }
            }
        } else if (processor instanceof FloatProcessor) {
            float[] pixels = (float[]) processor.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                if (!labelsToKeep_.contains((int) pixels[i])) {
                    pixels[i] = 0;
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported label type!");
        }
    }

    /**
     * Measures properties of a label image
     *
     * @param label        the label
     * @param image        the reference image
     * @param measurements the measurements
     * @param index        the current image index (zero-based)
     * @param calibration  the calibration (can be null to disable measurements with calibrations)
     * @param progressInfo the progress info
     * @return the measurements
     */
    public static ResultsTableData measureLabels(ImageProcessor label, ImageProcessor image, ImageStatisticsSetParameter measurements, ImageSliceIndex index, Calibration calibration, JIPipeProgressInfo progressInfo) {
        int measurementsNativeValue = measurements.getNativeValue();
        ImageProcessor mask = new ByteProcessor(label.getWidth(), label.getHeight());

        // Ensure the correct type for label
        label = ImageJUtils.convertToGreyscaleIfNeeded(new ImagePlus("", label)).getProcessor();

        // Copy image
        image = image.duplicate();
        image.setRoi((Roi) null);

        int[] allLabels = LabelImages.findAllLabels(label);

        final ResultsTableData result = new ResultsTableData();
        result.addNumericColumn("label_id");

        JIPipePercentageProgressInfo percentageProgress = progressInfo.percentage("Measure labels");
        for (int i = 0; i < allLabels.length; i++) {
            if (progressInfo.isCancelled())
                return null;
            percentageProgress.logPercentage(i, allLabels.length);
            int id = allLabels[i];
            // Update mask
            {
                byte[] maskBytes = (byte[]) mask.getPixels();
                if (label instanceof FloatProcessor) {
                    float[] labelBytes = (float[]) label.getPixels();
                    for (int j = 0; j < maskBytes.length; j++) {
                        maskBytes[j] = (labelBytes[j]) == id ? (byte) 255 : (byte) 0;
                    }
                } else if (label instanceof ShortProcessor) {
                    short[] labelBytes = (short[]) label.getPixels();
                    for (int j = 0; j < maskBytes.length; j++) {
                        maskBytes[j] = (labelBytes[j]) == id ? (byte) 255 : (byte) 0;
                    }
                } else if (label instanceof ByteProcessor) {
                    byte[] labelBytes = (byte[]) label.getPixels();
                    for (int j = 0; j < maskBytes.length; j++) {
                        maskBytes[j] = Byte.toUnsignedInt(labelBytes[j]) == id ? (byte) 255 : (byte) 0;
                    }
                } else {
                    throw new UnsupportedOperationException("Unknown label type!");
                }
            }

            image.setMask(mask);
            ImageStatistics statistics = image.getStatistics();

            ResultsTableData labelResult = new ResultsTableData();
            ImagePlus dummyImage = new ImagePlus("label=" + id, image);
            if (calibration != null)
                dummyImage.setCalibration(calibration);
            Analyzer analyzer = new Analyzer(dummyImage, measurementsNativeValue, labelResult.getTable());
            analyzer.saveResults(statistics, null);

            int labelIdColumn = labelResult.addNumericColumn("label_id");
            for (int j = 0; j < labelResult.getRowCount(); j++) {
                labelResult.setValueAt(id, j, labelIdColumn);
            }
            result.addRows(labelResult);
        }
        if (measurements.getValues().contains(Measurement.StackPosition)) {
            int columnChannel = result.getOrCreateColumnIndex("Ch", false);
            int columnStack = result.getOrCreateColumnIndex("Slice", false);
            int columnFrame = result.getOrCreateColumnIndex("Frame", false);
            for (int row = 0; row < result.getRowCount(); row++) {
                result.setValueAt(index.getC() + 1, row, columnChannel);
                result.setValueAt(index.getZ() + 1, row, columnStack);
                result.setValueAt(index.getT() + 1, row, columnFrame);
            }
        }
        return result;
    }

    public static ImageProcessor getMaskProcessorFromMaskOrROI(ImageROITargetArea sourceArea, int width, int height, ROI2DListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        switch (sourceArea) {
            case WholeImage: {
                return null;
            }
            case InsideRoi: {
                if (rois.isEmpty()) {
                    return null;
                } else {
                    return rois.getMaskForSlice(width, height,
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                if (rois.isEmpty()) {
                    return null;
                } else {
                    ImageProcessor processor = rois.getMaskForSlice(width, height,
                            false, true, 0, sliceIndex).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImageProcessor processor;
                if (mask.getStackSize() > 1) {
                    processor = mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask)).duplicate();
                } else {
                    processor = mask.getProcessor().duplicate();
                }
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    public static void updateROIOrMaskSlot(ImageROITargetArea sourceArea, JIPipeSlotConfiguration configuration) {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) configuration;
        if (sourceArea == ImageROITargetArea.WholeImage) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (sourceArea == ImageROITargetArea.InsideRoi || sourceArea == ImageROITargetArea.OutsideRoi) {
            if (!slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROI2DListData.class, JIPipeSlotType.Input), false);
            }
            if (slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.removeInputSlot("Mask", false);
            }
        } else if (sourceArea == ImageROITargetArea.InsideMask || sourceArea == ImageROITargetArea.OutsideMask) {
            if (slotConfiguration.getInputSlots().containsKey("ROI")) {
                slotConfiguration.removeInputSlot("ROI", false);
            }
            if (!slotConfiguration.getInputSlots().containsKey("Mask")) {
                slotConfiguration.addSlot("Mask", new JIPipeDataSlotInfo(ImagePlusGreyscaleMaskData.class, JIPipeSlotType.Input), false);
            }
        }
    }

    public static ImageProcessor getMaskProcessorFromMaskOrROI(ImageROITargetArea targetArea, JIPipeSingleIterationStep iterationStep, ImageSliceIndex sliceIndex, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
            }
            case InsideRoi: {
                ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
                } else {
                    return rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
                } else {
                    ImageProcessor processor = rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                    processor.invert();
                    return processor;
                }
            }
            case InsideMask: {
                ImagePlus mask = iterationStep.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImagePlus mask = iterationStep.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                ImageProcessor processor;
                if (mask.getStackSize() > 1) {
                    processor = mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask)).duplicate();
                } else {
                    processor = mask.getProcessor().duplicate();
                }
                processor.invert();
                return processor;
            }
        }
        throw new UnsupportedOperationException();
    }

    public static ImagePlus getMaskFromMaskOrROI(ImageROITargetArea targetArea, JIPipeSingleIterationStep iterationStep, String imageSlotName, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = iterationStep.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMask(img.getImage());
            }
            case InsideRoi: {
                ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    return rois.toMask(img.getImage(), true, false, 1);
                }
            }
            case OutsideRoi: {
                ROI2DListData rois = iterationStep.getInputData("ROI", ROI2DListData.class, progressInfo);
                ImagePlusData img = iterationStep.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    ImagePlus mask = rois.toMask(img.getImage(), true, false, 1);
                    ImageJIterationUtils.forEachIndexedZCTSlice(mask, (ip, index) -> {
                        ip.invert();
                    }, progressInfo.resolve("Invert mask"));
                    return ImageJUtils.ensureEqualSize(mask, img.getImage(), true);
                }
            }
            case InsideMask: {
                ImagePlusData img = iterationStep.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                ImagePlus mask = iterationStep.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                return ImageJUtils.ensureEqualSize(mask, img.getImage(), true);
            }
            case OutsideMask: {
                ImagePlusData img = iterationStep.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                ImagePlus mask = iterationStep.getInputData("Mask", ImagePlusData.class, progressInfo).getDuplicateImage();
                ImageJIterationUtils.forEachIndexedZCTSlice(mask, (ip, index) -> {
                    ip.invert();
                }, progressInfo.resolve("Invert mask"));
                return ImageJUtils.ensureEqualSize(mask, img.getImage(), true);
            }
        }
        throw new UnsupportedOperationException();
    }

    public static void measureROI(ImagePlus referenceImage, ROI2DListData roiList, ImageStatisticsSetParameter measurements, boolean physicalUnits, String columnPrefix, ResultsTableData target, JIPipeProgressInfo progressInfo) {
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
            generateROIRowMeasurements(referenceImage, i, roiList.get(i), measurements, physicalUnits, target, row, columnPrefix);
        }
    }

    public static void measureROIRelation(ImagePlus referenceImage, ROI2DListData roi1List, ROI2DListData roi2List, int measurements, boolean physicalUnits, boolean requireColocalization,
                                          boolean preciseColocalization, String columnPrefix, ResultsTableData target, JIPipeProgressInfo progressInfo) {
        int maxItems = roi1List.size() * roi2List.size();
        int currentItems = 0;
        int lastPercentage = 0;
        for (int i = 0; i < roi1List.size(); i++) {
            Roi roi1 = roi1List.get(i);
            for (int j = 0; j < roi2List.size(); j++) {
                Roi roi2 = roi2List.get(j);
                ++currentItems;
                if (progressInfo.isCancelled()) {
                    return;
                }
                int newPercentage = (int) (1.0 * currentItems / maxItems * 100);
                if (lastPercentage != newPercentage) {
                    progressInfo.log(currentItems + "/" + maxItems + " (" + newPercentage + "%)");
                    lastPercentage = newPercentage;
                }

                if (requireColocalization) {
                    if (!roi1.getBounds().intersects(roi2.getBounds())) {
                        continue;
                    }
                    if (preciseColocalization) {
                        ROI2DListData dummy = new ROI2DListData();
                        dummy.add(roi1);
                        dummy.add(roi2);
                        dummy.logicalAnd();
                        if (dummy.isEmpty()) {
                            continue;
                        }
                    }
                }

                int row = target.addRow();
                generateROIRelationRowMeasurements(referenceImage, i, j, measurements, physicalUnits, target, roi1, roi2, row, columnPrefix);
            }
        }
    }

    public static void generateROIRelationRowMeasurements(ImagePlus reference, int roi1Index, int roi2Index, int measurements, boolean physicalUnits, ResultsTableData target, Roi roi1, Roi roi2, int row, String columnPrefix) {

        // Mandatory!
        target.setValueAt(StringUtils.nullToEmpty(roi1.getName()), row, "Current.Name");
        target.setValueAt(StringUtils.nullToEmpty(roi2.getName()), row, "Other.Name");
        target.setValueAt(roi1Index, row, "Current.Index");
        target.setValueAt(roi2Index, row, "Other.Index");

        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.Colocalization) ||
                ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.PercentageColocalization)) {

            Roi intersection = ImageJROIUtils.intersectROI(roi1, roi2);
            if (intersection != null) {
                double intersectionArea = ImageJROIUtils.measureROI(intersection, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
                if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.Colocalization)) {
                    target.setValueAt(intersectionArea, row, columnPrefix + "Colocalization");
                }
                if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.PercentageColocalization)) {
                    double roi1Area = ImageJROIUtils.measureROI(roi1, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
                    double percentage = roi1Area / intersectionArea * 100;
                    if (!Double.isFinite(percentage))
                        percentage = 0;
                    target.setValueAt(percentage, row, columnPrefix + "PercentageColocalization");
                }
            } else {
                if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.Colocalization)) {
                    target.setValueAt(0, row, columnPrefix + "Colocalization");
                }
                if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.PercentageColocalization)) {
                    target.setValueAt(0, row, columnPrefix + "PercentageColocalization");
                }
            }
        }

        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.OverlapsBox)) {
            boolean value = roi1.getBounds().intersects(roi2.getBounds());
            target.setValueAt(value ? 1 : 0, row, columnPrefix + "OverlapsBox");
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.Includes)) {
            Roi intersection = ImageJROIUtils.intersectROI(roi1, roi2);
            double intersectionArea = ImageJROIUtils.measureROI(intersection, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
            double roi2Area = ImageJROIUtils.measureROI(roi2, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
            boolean value = intersectionArea == roi2Area;
            target.setValueAt(value ? 1 : 0, row, columnPrefix + "Includes");
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.IncludesBox)) {
            boolean value = roi1.getBounds().contains(roi2.getBounds());
            target.setValueAt(value ? 1 : 0, row, columnPrefix + "IncludesBox");
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.DistanceCenter)) {
            double[] contourCentroid1 = roi1.getContourCentroid();
            double[] contourCentroid2 = roi2.getContourCentroid();
            Point2d roi1Center = new Point2d(contourCentroid1[0], contourCentroid1[1]);
            Point2d roi2Center = new Point2d(contourCentroid2[0], contourCentroid2[1]);
            if (physicalUnits && reference != null) {
                roi1Center.x /= reference.getCalibration().pixelWidth;
                roi1Center.y /= reference.getCalibration().pixelHeight;
            }
            target.setValueAt(roi1Center.distance(roi2Center), row, columnPrefix + "DistanceCenter");
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.PolygonDistanceStats)) {
            FloatPolygon roi1p = roi1.getFloatPolygon();
            FloatPolygon roi2p = roi2.getFloatPolygon();
            TDoubleList distances = new TDoubleArrayList();
            for (int i = 0; i < roi1p.npoints; i++) {
                double sd = Double.POSITIVE_INFINITY;
                float x1 = roi1p.xpoints[i];
                float y1 = roi1p.ypoints[i];
                for (int j = 0; j < roi2p.npoints; j++) {
                    float x2 = roi2p.xpoints[j];
                    float y2 = roi2p.ypoints[j];
                    double d = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
                    if (d < sd) {
                        sd = d;
                    }
                }

                if (!Double.isInfinite(sd)) {
                    distances.add(Math.sqrt(sd));
                }
            }

            double[] arr = distances.toArray();
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double avg = Double.NaN;
            double sum = 0;
            if (arr.length > 0) {
                for (int i = 0; i < arr.length; i++) {
                    sum += arr[i];
                    min = Math.min(arr[i], min);
                    max = Math.max(arr[i], max);
                }
                avg = sum / arr.length;
            }

            target.setValueAt(min, row, columnPrefix + "PolygonDistanceMin");
            target.setValueAt(max, row, columnPrefix + "PolygonDistanceMax");
            target.setValueAt(avg, row, columnPrefix + "PolygonDistanceAvg");
            target.setValueAt(sum, row, columnPrefix + "PolygonDistanceSum");
            target.setValueAt(arr.length, row, columnPrefix + "PolygonDistanceNumDistances");
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.IntersectionStats)) {
            Roi intersection = ImageJROIUtils.intersectROI(roi1, roi2);
            if (intersection != null) {
                generateROIRowMeasurements(reference, -1, intersection, new ImageStatisticsSetParameter(), physicalUnits, target, row, "Intersection.");
            }
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.CurrentStats)) {
            generateROIRowMeasurements(reference, roi1Index, roi1, new ImageStatisticsSetParameter(), physicalUnits, target, row, "Current.");
        }
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.OtherStats)) {
            generateROIRowMeasurements(reference, roi2Index, roi2, new ImageStatisticsSetParameter(), physicalUnits, target, row, "Other.");
        }
    }

    public static void generateROIRowMeasurements(ImagePlus referenceImage, int index, Roi roi, ImageStatisticsSetParameter measurements, boolean physicalUnits, ResultsTableData target, int targetRow, String columnPrefix) {
        ROI2DListData dummy = new ROI2DListData();
        dummy.add(roi);
        ResultsTableData forROI = dummy.measure(referenceImage, measurements, true, physicalUnits);
        for (int col = 0; col < forROI.getColumnCount(); col++) {
            target.setValueAt(forROI.getValueAt(0, col), targetRow, columnPrefix + forROI.getColumnName(col));
        }
    }

}
