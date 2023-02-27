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

import mcib3d.geom.*;
import mcib3d.image3d.ImageHandler;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ij3d.utils.Measurement3D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IJ3DUtils {

    public static ImageHandler wrapImage(ImagePlusData imagePlusData) {
        if(imagePlusData != null) {
            return ImageHandler.wrap(imagePlusData.getImage());
        }
        else {
            return null;
        }
    }

    /**
     * Duplicates an {@link Object3D}
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

    public static void measure(ImageHandler referenceImage, ROI3DListData roiList, int measurements, boolean physicalUnits, ResultsTableData target, JIPipeProgressInfo progressInfo) {
        int lastPercentage = 0;
        for (int i = 0; i < roiList.size(); i++) {
            int newPercentage = (int)(1.0 * i / roiList.size() * 100);
            if(lastPercentage != newPercentage) {
                progressInfo.log( i + "/" + roiList.size() +  " (" + newPercentage + "%)");
                lastPercentage = newPercentage;
            }
            measure(referenceImage, i, roiList.get(i), measurements, physicalUnits, target);
        }
    }

    public static void measure(ImageHandler referenceImage, int index, ROI3D roi3D, int measurements, boolean physicalUnits, ResultsTableData target) {
        Object3D object3D = roi3D.getObject3D();
        int row = target.addRow();
        if(Measurement3D.includes(measurements, Measurement3D.Index)) {
            target.setValueAt(index, row, "Index");
        }
        if(Measurement3D.includes(measurements, Measurement3D.Name)) {
            target.setValueAt(StringUtils.nullToEmpty(object3D.getName()), row, "Name");
        }
        if(Measurement3D.includes(measurements, Measurement3D.Comment)) {
            target.setValueAt(StringUtils.nullToEmpty(object3D.getComment()), row, "Comment");
        }
        if(Measurement3D.includes(measurements, Measurement3D.Location)) {
            target.setValueAt(StringUtils.nullToEmpty(roi3D.getChannel()), row, "Channel");
            target.setValueAt(StringUtils.nullToEmpty(roi3D.getFrame()), row, "Frame");
        }
        if(Measurement3D.includes(measurements, Measurement3D.Color)) {
            target.setValueAt(ColorUtils.colorToHexString(roi3D.getFillColor()), row, "FillColor");
        }
        if(Measurement3D.includes(measurements, Measurement3D.CustomMetadata)) {
            for (Map.Entry<String, String> entry : roi3D.getMetadata().entrySet()) {
                target.setValueAt(entry.getValue(), row, "Metadata." + entry.getKey());
            }
        }
        if(Measurement3D.includes(measurements, Measurement3D.Area)) {
            double value;
            if(physicalUnits) {
                value = object3D.getAreaUnit();
            }
            else {
                value = object3D.getAreaPixels();
            }
            target.setValueAt(value, row, "Area");
        }
        if(Measurement3D.includes(measurements, Measurement3D.Volume)) {
            double value;
            if(physicalUnits) {
                value = object3D.getVolumeUnit();
            }
            else {
                value = object3D.getVolumePixels();
            }
            target.setValueAt(value, row, "Volume");
        }
        if(Measurement3D.includes(measurements, Measurement3D.Center)) {
            Vector3D value;
            if(physicalUnits) {
                value = object3D.getCenterAsVectorUnit();
            }
            else {
                value = object3D.getCenterAsVector();
            }
            target.setValueAt(value.getX(), row, "CenterX");
            target.setValueAt(value.getY(), row, "CenterY");
            target.setValueAt(value.getZ(), row, "CenterZ");

            if(referenceImage != null) {
                double centerValue = object3D.getPixCenterValue(referenceImage);
                target.setValueAt(centerValue, row, "CenterPixelValue");
            }
            else {
                target.setValueAt(Double.NaN, row, "CenterPixelValue");
            }
        }
        if(Measurement3D.includes(measurements, Measurement3D.ShapeMeasurements)) {
            target.setValueAt(object3D.getCompactness(), row, "Compactness");
            target.setValueAt(object3D.getSphericity(), row, "Sphericity");
            target.setValueAt(object3D.getFeret(), row, "Feret");
            target.setValueAt( object3D.getMainElongation(), row, "MainElongation");
            target.setValueAt( object3D.getMedianElongation(), row, "MedianElongation");
            target.setValueAt( object3D.getRatioBox(), row, "RatioBox");
            target.setValueAt( object3D.getRatioEllipsoid(), row, "RatioEllipsoid");
        }
        if(Measurement3D.includes(measurements, Measurement3D.BoundingBox)) {
            target.setValueAt(object3D.getXmin(), row, "BoundingBoxMinX");
            target.setValueAt(object3D.getXmax(), row, "BoundingBoxMaxX");
            target.setValueAt(object3D.getYmin(), row, "BoundingBoxMinY");
            target.setValueAt(object3D.getYmax(), row, "BoundingBoxMaxY");
            target.setValueAt(object3D.getZmin(), row, "BoundingBoxMinZ");
            target.setValueAt(object3D.getZmax(), row, "BoundingBoxMaxZ");
        }
        if(Measurement3D.includes(measurements, Measurement3D.DistCenterStats)) {
            double max, mean, sigma;
            if(physicalUnits) {
                max = object3D.getDistCenterMax();
            }
            else {
                max = object3D.getDistCenterMaxPixel();
            }
            if(physicalUnits) {
                mean = object3D.getDistCenterMean();
            }
            else {
                mean = object3D.getDistCenterMeanPixel();
            }
            if(physicalUnits) {
                sigma = object3D.getDistCenterSigma();
            }
            else {
                sigma = object3D.getDistCenterSigmaPixel();
            }
            target.setValueAt(max, row, "DistCenterMax");
            target.setValueAt(mean, row, "DistCenterMean");
            target.setValueAt(sigma, row, "DistCenterSigma");
        }
        if(Measurement3D.includes(measurements, Measurement3D.PixelValueStats)) {
            if(referenceImage != null) {
                double max = object3D.getPixMaxValue(referenceImage);
                double min = object3D.getPixMinValue(referenceImage);
                double mean = object3D.getPixMeanValue(referenceImage);
                double sigma = object3D.getPixStdDevValue(referenceImage);
                double mode = object3D.getPixModeValue(referenceImage);
                double modeNonZero = object3D.getPixModeNonZero(referenceImage);
                double median = object3D.getPixMedianValue(referenceImage);
                double intDen = object3D.getIntegratedDensity(referenceImage);
                target.setValueAt(max, row, "PixelValueMax");
                target.setValueAt(min, row, "PixelValueMin");
                target.setValueAt(mean, row, "PixelValueMean");
                target.setValueAt(median, row, "PixelValueMedian");
                target.setValueAt(sigma, row, "PixelValueStdDev");
                target.setValueAt(mode, row, "PixelValueMode");
                target.setValueAt(modeNonZero, row, "PixelValueModeNonZero");
                target.setValueAt(intDen, row, "PixelValueIntDen");
            }
            else {
                target.setValueAt(Double.NaN, row, "PixelValueMax");
                target.setValueAt(Double.NaN, row, "PixelValueMin");
                target.setValueAt(Double.NaN, row, "PixelValueMean");
                target.setValueAt(Double.NaN, row, "PixelValueMedian");
                target.setValueAt(Double.NaN, row, "PixelValueStdDev");
                target.setValueAt(Double.NaN, row, "PixelValueMode");
                target.setValueAt(Double.NaN, row, "PixelValueModeNonZero");
                target.setValueAt(Double.NaN, row, "PixelValueIntDen");
            }
        }
        if(Measurement3D.includes(measurements, Measurement3D.ContourPixelValueStats)) {
            if(referenceImage != null) {
                double mean = object3D.getPixMeanValueContour(referenceImage);
                target.setValueAt(mean, row, "ContourPixelValueMean");
            }
            else {
                target.setValueAt(Double.NaN, row, "ContourPixelValueMean");
            }
        }
        if(Measurement3D.includes(measurements, Measurement3D.Calibration)) {
            target.setValueAt(object3D.getResXY(), row, "ResolutionXY");
            target.setValueAt(object3D.getResZ(), row, "ResolutionZ");
            target.setValueAt(object3D.getUnits(), row, "ResolutionUnit");
        }
        if(Measurement3D.includes(measurements, Measurement3D.MassCenter)) {
            if(referenceImage != null) {
                double x = object3D.getMassCenterX(referenceImage);
                double y = object3D.getMassCenterY(referenceImage);
                double z = object3D.getMassCenterZ(referenceImage);
                target.setValueAt(x, row, "MassCenterX");
                target.setValueAt(y, row, "MassCenterY");
                target.setValueAt(z, row, "MassCenterZ");
            }
            else {
                target.setValueAt(Double.NaN, row, "MassCenterX");
                target.setValueAt(Double.NaN, row, "MassCenterY");
                target.setValueAt(Double.NaN, row, "MassCenterZ");
            }
        }
    }

}
