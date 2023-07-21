package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

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
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.ROI2DRelationMeasurement;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.Neighborhood3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.vecmath.Point2d;

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

    public static ImageProcessor getMaskProcessorFromMaskOrROI(ImageROITargetArea sourceArea, int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
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
                slotConfiguration.addSlot("ROI", new JIPipeDataSlotInfo(ROIListData.class, JIPipeSlotType.Input), false);
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

    public static ImageProcessor getMaskProcessorFromMaskOrROI(ImageROITargetArea targetArea, JIPipeDataBatch dataBatch, ImageSliceIndex sliceIndex, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
            }
            case InsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMaskProcessor(img.getImage());
                } else {
                    return rois.getMaskForSlice(img.getImage().getWidth(), img.getImage().getHeight(),
                            false, true, 0, sliceIndex).getProcessor();
                }
            }
            case OutsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo);
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
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                if (mask.getStackSize() > 1) {
                    return mask.getStack().getProcessor(sliceIndex.zeroSliceIndexToOneStackIndex(mask));
                } else {
                    return mask.getProcessor();
                }
            }
            case OutsideMask: {
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
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

    public static ImagePlus getMaskFromMaskOrROI(ImageROITargetArea targetArea, JIPipeDataBatch dataBatch, String imageSlotName, JIPipeProgressInfo progressInfo) {
        switch (targetArea) {
            case WholeImage: {
                ImagePlusData img = dataBatch.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                return ImageROITargetArea.createWhiteMask(img.getImage());
            }
            case InsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    return rois.toMask(img.getImage(), true, false, 1);
                }
            }
            case OutsideRoi: {
                ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
                ImagePlusData img = dataBatch.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                if (rois.isEmpty()) {
                    return ImageROITargetArea.createWhiteMask(img.getImage());
                } else {
                    ImagePlus mask = rois.toMask(img.getImage(), true, false, 1);
                    ImageJUtils.forEachIndexedZCTSlice(mask, (ip, index) -> {
                        ip.invert();
                    }, progressInfo.resolve("Invert mask"));
                    return ImageJUtils.ensureEqualSize(mask, img.getImage(), true);
                }
            }
            case InsideMask: {
                ImagePlusData img = dataBatch.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getImage();
                return ImageJUtils.ensureEqualSize(mask, img.getImage(), true);
            }
            case OutsideMask: {
                ImagePlusData img = dataBatch.getInputData(imageSlotName, ImagePlusData.class, progressInfo);
                ImagePlus mask = dataBatch.getInputData("Mask", ImagePlusData.class, progressInfo).getDuplicateImage();
                ImageJUtils.forEachIndexedZCTSlice(mask, (ip, index) -> {
                    ip.invert();
                }, progressInfo.resolve("Invert mask"));
                return ImageJUtils.ensureEqualSize(mask, img.getImage(), true);
            }
        }
        throw new UnsupportedOperationException();
    }

    public static void measureROI(ImagePlus referenceImage, ROIListData roiList, ImageStatisticsSetParameter measurements, boolean physicalUnits, String columnPrefix, ResultsTableData target, JIPipeProgressInfo progressInfo) {
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

    public static void measureROIRelation(ImagePlus referenceImage, ROIListData roi1List, ROIListData roi2List, int measurements, boolean physicalUnits, boolean requireColocalization,
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
                        ROIListData dummy = new ROIListData();
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

            Roi intersection = ImageJUtils.intersectROI(roi1, roi2);
            if (intersection != null) {
                double intersectionArea = ImageJUtils.measureROI(intersection, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
                if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.Colocalization)) {
                    target.setValueAt(intersectionArea, row, columnPrefix + "Colocalization");
                }
                if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.PercentageColocalization)) {
                    double roi1Area = ImageJUtils.measureROI(roi1, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
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
            Roi intersection = ImageJUtils.intersectROI(roi1, roi2);
            double intersectionArea = ImageJUtils.measureROI(intersection, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
            double roi2Area = ImageJUtils.measureROI(roi2, reference, physicalUnits, Measurement.Area).getValueAsDouble(0, "Area");
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
        if (ROI2DRelationMeasurement.includes(measurements, ROI2DRelationMeasurement.IntersectionStats)) {
            Roi intersection = ImageJUtils.intersectROI(roi1, roi2);
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
        ROIListData dummy = new ROIListData();
        dummy.add(roi);
        ResultsTableData forROI = dummy.measure(referenceImage, measurements, true, physicalUnits);
        for (int col = 0; col < forROI.getColumnCount(); col++) {
            target.setValueAt(forROI.getValueAt(0, col), targetRow, columnPrefix + forROI.getColumnName(col));
        }
    }

}
