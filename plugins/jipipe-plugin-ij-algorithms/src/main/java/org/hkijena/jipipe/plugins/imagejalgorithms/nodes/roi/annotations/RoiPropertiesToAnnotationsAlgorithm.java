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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.annotations;

import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with ROI properties", description = "Extracts properties of the ROI lists and stores them into annotations. " +
        "This will create a list of annotation values if there are multiple ROI per list, unless you choose the option to only return the first entry. " +
        "By default, one value per ROI is generated. Enable de-duplication to disable this behavior.")
@ConfigureJIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For ROI")
@AddJIPipeInputSlot(value = ROIListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Output", create = true)
public class RoiPropertiesToAnnotationsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter countAnnotation = new OptionalTextAnnotationNameParameter("Count", true);
    private OptionalTextAnnotationNameParameter nameAnnotation = new OptionalTextAnnotationNameParameter("Name", false);
    private OptionalTextAnnotationNameParameter locationXAnnotation = new OptionalTextAnnotationNameParameter("X", false);
    private OptionalTextAnnotationNameParameter locationYAnnotation = new OptionalTextAnnotationNameParameter("Y", false);
    private OptionalTextAnnotationNameParameter locationZAnnotation = new OptionalTextAnnotationNameParameter("Z", false);
    private OptionalTextAnnotationNameParameter locationCAnnotation = new OptionalTextAnnotationNameParameter("C", false);
    private OptionalTextAnnotationNameParameter locationTAnnotation = new OptionalTextAnnotationNameParameter("T", false);
    private OptionalTextAnnotationNameParameter fillColorAnnotation = new OptionalTextAnnotationNameParameter("Fill color", false);
    private OptionalTextAnnotationNameParameter lineColorAnnotation = new OptionalTextAnnotationNameParameter("Line color", false);
    private OptionalTextAnnotationNameParameter lineWidthAnnotation = new OptionalTextAnnotationNameParameter("Line width", false);
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;
    private boolean deduplicate = false;
    private boolean onlyFirst = false;

    public RoiPropertiesToAnnotationsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RoiPropertiesToAnnotationsAlgorithm(RoiPropertiesToAnnotationsAlgorithm other) {
        super(other);
        this.countAnnotation = new OptionalTextAnnotationNameParameter(other.countAnnotation);
        this.nameAnnotation = new OptionalTextAnnotationNameParameter(other.nameAnnotation);
        this.locationXAnnotation = new OptionalTextAnnotationNameParameter(other.locationXAnnotation);
        this.locationYAnnotation = new OptionalTextAnnotationNameParameter(other.locationYAnnotation);
        this.locationZAnnotation = new OptionalTextAnnotationNameParameter(other.locationZAnnotation);
        this.locationCAnnotation = new OptionalTextAnnotationNameParameter(other.locationCAnnotation);
        this.locationTAnnotation = new OptionalTextAnnotationNameParameter(other.locationTAnnotation);
        this.fillColorAnnotation = new OptionalTextAnnotationNameParameter(other.fillColorAnnotation);
        this.lineColorAnnotation = new OptionalTextAnnotationNameParameter(other.lineColorAnnotation);
        this.lineWidthAnnotation = new OptionalTextAnnotationNameParameter(other.lineWidthAnnotation);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.deduplicate = other.deduplicate;
        this.onlyFirst = other.onlyFirst;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData rois = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
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

        iterationStep.addOutputData(getFirstOutputSlot(), rois, annotations, annotationMergeStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Annotate with list size", description = "Adds the size of the ROI list as annotation")
    @JIPipeParameter("count-annotation")
    public OptionalTextAnnotationNameParameter getCountAnnotation() {
        return countAnnotation;
    }

    @JIPipeParameter("count-annotation")
    public void setCountAnnotation(OptionalTextAnnotationNameParameter countAnnotation) {
        this.countAnnotation = countAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with names", description = "Adds the ROI names as annotation")
    @JIPipeParameter("name-annotation")
    public OptionalTextAnnotationNameParameter getNameAnnotation() {
        return nameAnnotation;
    }

    @JIPipeParameter("name-annotation")
    public void setNameAnnotation(OptionalTextAnnotationNameParameter nameAnnotation) {
        this.nameAnnotation = nameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with X locations", description = "Adds the ROI X locations (top left corner) as annotation")
    @JIPipeParameter("x-annotation")
    public OptionalTextAnnotationNameParameter getLocationXAnnotation() {
        return locationXAnnotation;
    }

    @JIPipeParameter("x-annotation")
    public void setLocationXAnnotation(OptionalTextAnnotationNameParameter locationXAnnotation) {
        this.locationXAnnotation = locationXAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with Y locations", description = "Adds the ROI Y locations (top left corner) as annotation")
    @JIPipeParameter("y-annotation")
    public OptionalTextAnnotationNameParameter getLocationYAnnotation() {
        return locationYAnnotation;
    }

    @JIPipeParameter("y-annotation")
    public void setLocationYAnnotation(OptionalTextAnnotationNameParameter locationYAnnotation) {
        this.locationYAnnotation = locationYAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with Z locations", description = "Adds the ROI Z locations as annotation. The first index is 1. A value of 0 indicates that the ROI is located on all planes.")
    @JIPipeParameter("z-annotation")
    public OptionalTextAnnotationNameParameter getLocationZAnnotation() {
        return locationZAnnotation;
    }

    @JIPipeParameter("z-annotation")
    public void setLocationZAnnotation(OptionalTextAnnotationNameParameter locationZAnnotation) {
        this.locationZAnnotation = locationZAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with C locations", description = "Adds the ROI C locations as annotation. The first index is 1. A value of 0 indicates that the ROI is located on all planes.")
    @JIPipeParameter("c-annotation")
    public OptionalTextAnnotationNameParameter getLocationCAnnotation() {
        return locationCAnnotation;
    }

    @JIPipeParameter("c-annotation")
    public void setLocationCAnnotation(OptionalTextAnnotationNameParameter locationCAnnotation) {
        this.locationCAnnotation = locationCAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with T locations", description = "Adds the ROI T locations as annotation. The first index is 1. A value of 0 indicates that the ROI is located on all planes.")
    @JIPipeParameter("t-annotation")
    public OptionalTextAnnotationNameParameter getLocationTAnnotation() {
        return locationTAnnotation;
    }

    @JIPipeParameter("t-annotation")
    public void setLocationTAnnotation(OptionalTextAnnotationNameParameter locationTAnnotation) {
        this.locationTAnnotation = locationTAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with fill colors", description = "Adds the ROI fill colors as annotation. No annotation is generated if the fill color is not explicitly set.")
    @JIPipeParameter("fill-color-annotation")
    public OptionalTextAnnotationNameParameter getFillColorAnnotation() {
        return fillColorAnnotation;
    }

    @JIPipeParameter("fill-color-annotation")
    public void setFillColorAnnotation(OptionalTextAnnotationNameParameter fillColorAnnotation) {
        this.fillColorAnnotation = fillColorAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with line colors", description = "Adds the ROI line colors as annotation. No annotation is generated if the fill color is not explicitly set.")
    @JIPipeParameter("line-color-annotation")
    public OptionalTextAnnotationNameParameter getLineColorAnnotation() {
        return lineColorAnnotation;
    }

    @JIPipeParameter("line-color-annotation")
    public void setLineColorAnnotation(OptionalTextAnnotationNameParameter lineColorAnnotation) {
        this.lineColorAnnotation = lineColorAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotate with line widths", description = "Adds the ROI line widths as annotation. 0 if not explicity set.")
    @JIPipeParameter("line-width-annotation")
    public OptionalTextAnnotationNameParameter getLineWidthAnnotation() {
        return lineWidthAnnotation;
    }

    @JIPipeParameter("line-width-annotation")
    public void setLineWidthAnnotation(OptionalTextAnnotationNameParameter lineWidthAnnotation) {
        this.lineWidthAnnotation = lineWidthAnnotation;
    }

    @SetJIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how the newly generated annotations are merged with existing annotations.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @SetJIPipeDocumentation(name = "Deduplicate", description = "If enabled, duplicate values for names, locations, etc are removed.")
    @JIPipeParameter("deduplicate")
    public boolean isDeduplicate() {
        return deduplicate;
    }

    @JIPipeParameter("deduplicate")
    public void setDeduplicate(boolean deduplicate) {
        this.deduplicate = deduplicate;
    }

    @SetJIPipeDocumentation(name = "Only first ROI", description = "If enabled, only global properties and the properties of the first ROI are exported.")
    @JIPipeParameter("only-first")
    public boolean isOnlyFirst() {
        return onlyFirst;
    }

    @JIPipeParameter("only-first")
    public void setOnlyFirst(boolean onlyFirst) {
        this.onlyFirst = onlyFirst;
    }
}
