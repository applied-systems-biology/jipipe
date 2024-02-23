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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Extract images inside ROI", description = "Extracts image slices that are marked via ROI and outputs them. The images are annotated with locational information for later reconstruction.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "ROI")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", create = true)
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "ROI images", create = true)
public class ExtractImageInRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;
    private OptionalAnnotationNameParameter annotationXLocation = new OptionalAnnotationNameParameter("X", true);
    private OptionalAnnotationNameParameter annotationYLocation = new OptionalAnnotationNameParameter("Y", true);
    private OptionalAnnotationNameParameter annotationZLocation = new OptionalAnnotationNameParameter("Z", true);
    private OptionalAnnotationNameParameter annotationCLocation = new OptionalAnnotationNameParameter("C", true);
    private OptionalAnnotationNameParameter annotationTLocation = new OptionalAnnotationNameParameter("T", true);

    public ExtractImageInRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }


    public ExtractImageInRoiAlgorithm(ExtractImageInRoiAlgorithm other) {
        super(other);
        this.ignoreZ = other.ignoreZ;
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
        this.annotationXLocation = new OptionalAnnotationNameParameter(other.annotationXLocation);
        this.annotationYLocation = new OptionalAnnotationNameParameter(other.annotationYLocation);
        this.annotationZLocation = new OptionalAnnotationNameParameter(other.annotationZLocation);
        this.annotationCLocation = new OptionalAnnotationNameParameter(other.annotationCLocation);
        this.annotationTLocation = new OptionalAnnotationNameParameter(other.annotationTLocation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);

        List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
    }

    @SetJIPipeDocumentation(name = "Ignore ROI Z location", description = "If enabled, the image will be extracted from all Z slices regardless of the ROI location.")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @SetJIPipeDocumentation(name = "Ignore ROI C location", description = "If enabled, the image will be extracted from all C slices regardless of the ROI location.")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @SetJIPipeDocumentation(name = "Ignore ROI T location", description = "If enabled, the image will be extracted from all T slices regardless of the ROI location.")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }

    @SetJIPipeDocumentation(name = "Annotate with X", description = "If enabled, the extracted images are annotated with their source X location.")
    @JIPipeParameter("annotation-x")
    public OptionalAnnotationNameParameter getAnnotationXLocation() {
        return annotationXLocation;
    }

    @JIPipeParameter("annotation-x")
    public void setAnnotationXLocation(OptionalAnnotationNameParameter annotationXLocation) {
        this.annotationXLocation = annotationXLocation;
    }

    @SetJIPipeDocumentation(name = "Annotate with Y", description = "If enabled, the extracted images are annotated with their source Y location.")
    @JIPipeParameter("annotation-y")
    public OptionalAnnotationNameParameter getAnnotationYLocation() {
        return annotationYLocation;
    }

    @JIPipeParameter("annotation-y")
    public void setAnnotationYLocation(OptionalAnnotationNameParameter annotationYLocation) {
        this.annotationYLocation = annotationYLocation;
    }

    @SetJIPipeDocumentation(name = "Annotate with Z", description = "If enabled, the extracted images are annotated with their source Z location.")
    @JIPipeParameter("annotation-z")
    public OptionalAnnotationNameParameter getAnnotationZLocation() {
        return annotationZLocation;
    }

    @JIPipeParameter("annotation-z")
    public void setAnnotationZLocation(OptionalAnnotationNameParameter annotationZLocation) {
        this.annotationZLocation = annotationZLocation;
    }

    @SetJIPipeDocumentation(name = "Annotate with C", description = "If enabled, the extracted images are annotated with their source C location.")
    @JIPipeParameter("annotation-c")
    public OptionalAnnotationNameParameter getAnnotationCLocation() {
        return annotationCLocation;
    }

    @JIPipeParameter("annotation-c")
    public void setAnnotationCLocation(OptionalAnnotationNameParameter annotationCLocation) {
        this.annotationCLocation = annotationCLocation;
    }

    @SetJIPipeDocumentation(name = "Annotate with T", description = "If enabled, the extracted images are annotated with their source T location.")
    @JIPipeParameter("annotation-t")
    public OptionalAnnotationNameParameter getAnnotationTLocation() {
        return annotationTLocation;
    }

    @JIPipeParameter("annotation-t")
    public void setAnnotationTLocation(OptionalAnnotationNameParameter annotationTLocation) {
        this.annotationTLocation = annotationTLocation;
    }
}
