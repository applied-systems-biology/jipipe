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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Split into individual ROI lists", description = "Splits the ROI in a ROI list into individual ROI lists.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Split")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ExplodeRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter generatedAnnotation = new OptionalStringParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ExplodeRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ROIListData.class)
                .addOutputSlot("Output", "", ROIListData.class, null)
                .seal()
                .build());
        generatedAnnotation.setContent("ROI index");
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ExplodeRoiAlgorithm(ExplodeRoiAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData data = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            if (generatedAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(generatedAnnotation.getContent())) {
                annotations.add(new JIPipeTextAnnotation(generatedAnnotation.getContent(), "index=" + i + ";name=" + roi.getName()));
            }
            ROIListData output = new ROIListData();
            output.add(roi);
            dataBatch.addOutputData(getFirstOutputSlot(), output, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @JIPipeDocumentation(name = "ROI index annotation", description = "Optional. Annotation that is added to each individual ROI list. Contains the value index=[index];name=[name].")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(OptionalStringParameter generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}