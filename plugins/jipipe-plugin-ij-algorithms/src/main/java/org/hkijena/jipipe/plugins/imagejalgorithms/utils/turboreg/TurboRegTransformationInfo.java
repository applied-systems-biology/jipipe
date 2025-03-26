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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2dParameter;

import java.util.ArrayList;
import java.util.List;

public class TurboRegTransformationInfo {

    private List<Entry> entries = new ArrayList<>();

    public TurboRegTransformationInfo() {
    }

    public TurboRegTransformationInfo(TurboRegTransformationInfo other) {
        this.entries = other.entries;
    }

    @JsonGetter("entries")
    public List<Entry> getEntries() {
        return entries;
    }

    @JsonSetter("entries")
    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public static class Entry {
        private ImageSliceIndex sourceImageIndex;
        private ImageSliceIndex targetImageIndex;
        private TurboRegTransformationType transformationType;
        private Vector2dParameter.List sourcePoints = new Vector2dParameter.List();
        private Vector2dParameter.List targetPoints = new Vector2dParameter.List();

        public Entry() {
            while (sourcePoints.size() < TurboRegPointHandler.NUM_POINTS) {
                sourcePoints.add(new Vector2dParameter());
            }
            while (targetPoints.size() < TurboRegPointHandler.NUM_POINTS) {
                targetPoints.add(new Vector2dParameter());
            }
        }

        public Entry(ImageSliceIndex sourceImageIndex, ImageSliceIndex targetImageIndex, TurboRegTransformationType transformationType, double[][] sourcePoints, double[][] targetPoints) {
            this.sourceImageIndex = sourceImageIndex;
            this.targetImageIndex = targetImageIndex;
            this.transformationType = transformationType;
            for (double[] sourcePoint : sourcePoints) {
                this.sourcePoints.add(new Vector2dParameter(sourcePoint[0], sourcePoint[1]));
            }
            for (double[] targetPoint : targetPoints) {
                this.targetPoints.add(new Vector2dParameter(targetPoint[0], targetPoint[1]));
            }
        }

        public Entry(Entry other) {
            this.sourceImageIndex = new ImageSliceIndex(other.sourceImageIndex);
            this.targetImageIndex = new ImageSliceIndex(other.targetImageIndex);
            this.transformationType = other.transformationType;
            this.sourcePoints = new Vector2dParameter.List(other.sourcePoints);
            this.targetPoints = new Vector2dParameter.List(other.targetPoints);
        }

        @JsonGetter("transformation-type")
        public TurboRegTransformationType getTransformationType() {
            return transformationType;
        }

        @JsonSetter("transformation-type")
        public void setTransformationType(TurboRegTransformationType transformationType) {
            this.transformationType = transformationType;
        }

        @JsonGetter("source-image-index")
        public ImageSliceIndex getSourceImageIndex() {
            return sourceImageIndex;
        }

        @JsonSetter("source-image-index")
        public void setSourceImageIndex(ImageSliceIndex sourceImageIndex) {
            this.sourceImageIndex = sourceImageIndex;
        }

        @JsonGetter("target-image-index")
        public ImageSliceIndex getTargetImageIndex() {
            return targetImageIndex;
        }

        @JsonSetter("target-image-index")
        public void setTargetImageIndex(ImageSliceIndex targetImageIndex) {
            this.targetImageIndex = targetImageIndex;
        }

        @JsonGetter("source-points")
        public Vector2dParameter.List getSourcePoints() {
            return sourcePoints;
        }

        @JsonSetter("source-points")
        public void setSourcePoints(Vector2dParameter.List sourcePoints) {
            this.sourcePoints = sourcePoints;
        }

        @JsonGetter("target-points")
        public Vector2dParameter.List getTargetPoints() {
            return targetPoints;
        }

        @JsonSetter("target-points")
        public void setTargetPoints(Vector2dParameter.List targetPoints) {
            this.targetPoints = targetPoints;
        }

        public double[][] getSourcePointsAsArray() {
            double[][] result = new double[sourcePoints.size()][2];
            for (int i = 0; i < sourcePoints.size(); i++) {
                result[i][0] = sourcePoints.get(i).getX();
                result[i][1] = sourcePoints.get(i).getY();
            }
            return result;
        }

        public double[][] getTargetPointsAsArray() {
            double[][] result = new double[targetPoints.size()][2];
            for (int i = 0; i < targetPoints.size(); i++) {
                result[i][0] = targetPoints.get(i).getX();
                result[i][1] = targetPoints.get(i).getY();
            }
            return result;
        }
    }
}
