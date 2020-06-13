package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import ij.gui.Roi;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Split into individual ROI lists", description = "Splits the ROI in a ROI list into individual ROI lists.")
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class ExplodeRoiAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private String generatedAnnotation = "ROI index";

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ExplodeRoiAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ExplodeRoiAlgorithm(ExplodeRoiAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = dataInterface.getInputData(getFirstInputSlot(), ROIListData.class);
        for (int i = 0; i < data.size(); i++) {
            Roi roi = data.get(i);
            List<ACAQAnnotation> traits = new ArrayList<>();
            if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
                traits.add(new ACAQAnnotation(generatedAnnotation, "index=" + i + ";name=" + roi.getName()));
            }
            ROIListData output = new ROIListData();
            output.add(roi);
            dataInterface.addOutputData(getFirstOutputSlot(), output, traits);
        }
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "ROI index annotation", description = "Optional. Annotation that is added to each individual ROI list. Contains the value index=[index];name=[name].")
    @ACAQParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}
