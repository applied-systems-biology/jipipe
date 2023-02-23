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
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.annotations;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with ROI properties", description = "Extracts properties of the ROI lists and stores them into annotations. " +
        "This will create a list of annotation values if there are multiple ROI per list, unless you choose the option to only return the first entry. " +
        "By default, one value per ROI is generated. Enable de-duplication to disable this behavior.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For ROI")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class RoiPropertiesToAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalAnnotationNameParameter countAnnotation = new OptionalAnnotationNameParameter("Count", true);
    private OptionalAnnotationNameParameter nameAnnotation = new OptionalAnnotationNameParameter("Name", false);
    private OptionalAnnotationNameParameter locationXAnnotation = new OptionalAnnotationNameParameter("X", false);
    private OptionalAnnotationNameParameter locationYAnnotation = new OptionalAnnotationNameParameter("Y", false);
    private OptionalAnnotationNameParameter locationZAnnotation = new OptionalAnnotationNameParameter("Z", false);
    private OptionalAnnotationNameParameter locationCAnnotation = new OptionalAnnotationNameParameter("C", false);
    private OptionalAnnotationNameParameter locationTAnnotation = new OptionalAnnotationNameParameter("T", false);
    private OptionalAnnotationNameParameter fillColorAnnotation = new OptionalAnnotationNameParameter("Fill color", false);
    private OptionalAnnotationNameParameter lineColorAnnotation = new OptionalAnnotationNameParameter("Line color", false);
    private OptionalAnnotationNameParameter lineWidthAnnotation = new OptionalAnnotationNameParameter("Line width", false);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;
    private boolean deduplicate = false;
    private boolean onlyFirst = false;

    public RoiPropertiesToAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RoiPropertiesToAnnotationsAlgorithm(RoiPropertiesToAnnotationsAlgorithm other) {
        super(other);
        this.countAnnotation = new OptionalAnnotationNameParameter(other.countAnnotation);
        this.nameAnnotation = new OptionalAnnotationNameParameter(other.nameAnnotation);
        this.locationXAnnotation = new OptionalAnnotationNameParameter(other.locationXAnnotation);
        this.locationYAnnotation = new OptionalAnnotationNameParameter(other.locationYAnnotation);
        this.locationZAnnotation = new OptionalAnnotationNameParameter(other.locationZAnnotation);
        this.locationCAnnotation = new OptionalAnnotationNameParameter(other.locationCAnnotation);
        this.locationTAnnotation = new OptionalAnnotationNameParameter(other.locationTAnnotation);
        this.fillColorAnnotation = new OptionalAnnotationNameParameter(other.fillColorAnnotation);
        this.lineColorAnnotation = new OptionalAnnotationNameParameter(other.lineColorAnnotation);
        this.lineWidthAnnotation = new OptionalAnnotationNameParameter(other.lineWidthAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.deduplicate = other.deduplicate;
        this.onlyFirst = other.onlyFirst;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData rois = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        countAnnotation.addAnnotationIfEnabled(annotations, rois.size() + "");

        List<String> names = new ArrayList<>();
        List<String> xLocations = new ArrayList<>();
        List<String> yLocations = new ArrayList<>();
        List<String> zLocations = new ArrayList<>();
        List<String> cLocations = new ArrayList<>();
        List<String> tLocations = new ArrayList<>();
        List<String> fillColors = new ArrayList<>();
        List<String> lineColors = new ArrayList<>();
        List<String> lineWidths = new ArrayList<>();

        for (Roi roi : rois) {
            if (roi.getName() != null)
                names.add(roi.getName());
            xLocations.add(roi.getXBase() + "");
            yLocations.add(roi.getYBase() + "");
            zLocations.add(roi.getZPosition() + "");
            cLocations.add(roi.getCPosition() + "");
            tLocations.add(roi.getTPosition() + "");
            if (roi.getFillColor() != null)
                fillColors.add(ColorUtils.colorToHexString(roi.getFillColor()));
            if (roi.getStrokeColor() != null)
                fillColors.add(ColorUtils.colorToHexString(roi.getStrokeColor()));
            lineWidths.add(roi.getStrokeWidth() + "");
            if (onlyFirst)
                break;
        }

        if (deduplicate) {
            names = new ArrayList<>(new LinkedHashSet<>(names));
            xLocations = new ArrayList<>(new LinkedHashSet<>(xLocations));
            yLocations = new ArrayList<>(new LinkedHashSet<>(yLocations));
            zLocations = new ArrayList<>(new LinkedHashSet<>(zLocations));
            cLocations = new ArrayList<>(new LinkedHashSet<>(cLocations));
            tLocations = new ArrayList<>(new LinkedHashSet<>(tLocations));
            fillColors = new ArrayList<>(new LinkedHashSet<>(fillColors));
            lineColors = new ArrayList<>(new LinkedHashSet<>(lineColors));
            lineWidths = new ArrayList<>(new LinkedHashSet<>(lineWidths));
        }

        nameAnnotation.addAnnotationIfEnabled(annotations, names);
        locationXAnnotation.addAnnotationIfEnabled(annotations, xLocations);
        locationYAnnotation.addAnnotationIfEnabled(annotations, yLocations);
        locationZAnnotation.addAnnotationIfEnabled(annotations, zLocations);
        locationCAnnotation.addAnnotationIfEnabled(annotations, cLocations);
        locationTAnnotation.addAnnotationIfEnabled(annotations, tLocations);
        fillColorAnnotation.addAnnotationIfEnabled(annotations, fillColors);
        lineColorAnnotation.addAnnotationIfEnabled(annotations, lineColors);
        lineWidthAnnotation.addAnnotationIfEnabled(annotations, lineWidths);

        dataBatch.addOutputData(getFirstOutputSlot(), rois, annotations, annotationMergeStrategy, progressInfo);
    }

    @JIPipeDocumentation(name = "Annotate with list size", description = "Adds the size of the ROI list as annotation")
    @JIPipeParameter("count-annotation")
    public OptionalAnnotationNameParameter getCountAnnotation() {
        return countAnnotation;
    }

    @JIPipeParameter("count-annotation")
    public void setCountAnnotation(OptionalAnnotationNameParameter countAnnotation) {
        this.countAnnotation = countAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with names", description = "Adds the ROI names as annotation")
    @JIPipeParameter("name-annotation")
    public OptionalAnnotationNameParameter getNameAnnotation() {
        return nameAnnotation;
    }

    @JIPipeParameter("name-annotation")
    public void setNameAnnotation(OptionalAnnotationNameParameter nameAnnotation) {
        this.nameAnnotation = nameAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with X locations", description = "Adds the ROI X locations (top left corner) as annotation")
    @JIPipeParameter("x-annotation")
    public OptionalAnnotationNameParameter getLocationXAnnotation() {
        return locationXAnnotation;
    }

    @JIPipeParameter("x-annotation")
    public void setLocationXAnnotation(OptionalAnnotationNameParameter locationXAnnotation) {
        this.locationXAnnotation = locationXAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with Y locations", description = "Adds the ROI Y locations (top left corner) as annotation")
    @JIPipeParameter("y-annotation")
    public OptionalAnnotationNameParameter getLocationYAnnotation() {
        return locationYAnnotation;
    }

    @JIPipeParameter("y-annotation")
    public void setLocationYAnnotation(OptionalAnnotationNameParameter locationYAnnotation) {
        this.locationYAnnotation = locationYAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with Z locations", description = "Adds the ROI Z locations as annotation. The first index is 1. A value of 0 indicates that the ROI is located on all planes.")
    @JIPipeParameter("z-annotation")
    public OptionalAnnotationNameParameter getLocationZAnnotation() {
        return locationZAnnotation;
    }

    @JIPipeParameter("z-annotation")
    public void setLocationZAnnotation(OptionalAnnotationNameParameter locationZAnnotation) {
        this.locationZAnnotation = locationZAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with C locations", description = "Adds the ROI C locations as annotation. The first index is 1. A value of 0 indicates that the ROI is located on all planes.")
    @JIPipeParameter("c-annotation")
    public OptionalAnnotationNameParameter getLocationCAnnotation() {
        return locationCAnnotation;
    }

    @JIPipeParameter("c-annotation")
    public void setLocationCAnnotation(OptionalAnnotationNameParameter locationCAnnotation) {
        this.locationCAnnotation = locationCAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with T locations", description = "Adds the ROI T locations as annotation. The first index is 1. A value of 0 indicates that the ROI is located on all planes.")
    @JIPipeParameter("t-annotation")
    public OptionalAnnotationNameParameter getLocationTAnnotation() {
        return locationTAnnotation;
    }

    @JIPipeParameter("t-annotation")
    public void setLocationTAnnotation(OptionalAnnotationNameParameter locationTAnnotation) {
        this.locationTAnnotation = locationTAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with fill colors", description = "Adds the ROI fill colors as annotation. No annotation is generated if the fill color is not explicitly set.")
    @JIPipeParameter("fill-color-annotation")
    public OptionalAnnotationNameParameter getFillColorAnnotation() {
        return fillColorAnnotation;
    }

    @JIPipeParameter("fill-color-annotation")
    public void setFillColorAnnotation(OptionalAnnotationNameParameter fillColorAnnotation) {
        this.fillColorAnnotation = fillColorAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with line colors", description = "Adds the ROI line colors as annotation. No annotation is generated if the fill color is not explicitly set.")
    @JIPipeParameter("line-color-annotation")
    public OptionalAnnotationNameParameter getLineColorAnnotation() {
        return lineColorAnnotation;
    }

    @JIPipeParameter("line-color-annotation")
    public void setLineColorAnnotation(OptionalAnnotationNameParameter lineColorAnnotation) {
        this.lineColorAnnotation = lineColorAnnotation;
    }

    @JIPipeDocumentation(name = "Annotate with line widths", description = "Adds the ROI line widths as annotation. 0 if not explicity set.")
    @JIPipeParameter("line-width-annotation")
    public OptionalAnnotationNameParameter getLineWidthAnnotation() {
        return lineWidthAnnotation;
    }

    @JIPipeParameter("line-width-annotation")
    public void setLineWidthAnnotation(OptionalAnnotationNameParameter lineWidthAnnotation) {
        this.lineWidthAnnotation = lineWidthAnnotation;
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how the newly generated annotations are merged with existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Deduplicate", description = "If enabled, duplicate values for names, locations, etc are removed.")
    @JIPipeParameter("deduplicate")
    public boolean isDeduplicate() {
        return deduplicate;
    }

    @JIPipeParameter("deduplicate")
    public void setDeduplicate(boolean deduplicate) {
        this.deduplicate = deduplicate;
    }

    @JIPipeDocumentation(name = "Only first ROI", description = "If enabled, only global properties and the properties of the first ROI are exported.")
    @JIPipeParameter("only-first")
    public boolean isOnlyFirst() {
        return onlyFirst;
    }

    @JIPipeParameter("only-first")
    public void setOnlyFirst(boolean onlyFirst) {
        this.onlyFirst = onlyFirst;
    }
}
