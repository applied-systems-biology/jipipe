package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@ACAQDocumentation(name = "Merge ROI lists", description = "Merges multiple ROI lists by using data annotations. " +
        "By default, ROIs with equivalent annotations are put into the same group and merged into one ROI list for each group. " +
        "Use the parameters to control how groups are created. To merge all incoming ROI lists into just one list, set the matching strategy to 'Custom' and leave the list of " +
        "annotation columns empty."  + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(menuPath = "ROI", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ROIListData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ROIListData.class, slotName = "Output")
public class MergeRoiListsAlgorithm extends ACAQMergingAlgorithm {

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public MergeRoiListsAlgorithm(ACAQAlgorithmDeclaration declaration) {
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
    public MergeRoiListsAlgorithm(MergeRoiListsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData result = new ROIListData();
        for (ROIListData rois : dataInterface.getInputData(getFirstInputSlot(), ROIListData.class)) {
            result.mergeWith(rois);
        }
        dataInterface.addOutputData(getFirstOutputSlot(), result);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
