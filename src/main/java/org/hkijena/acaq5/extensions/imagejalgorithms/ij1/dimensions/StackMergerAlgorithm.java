package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.api.algorithm.ACAQMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;
import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_3D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Create 3D stack", description = "Merges 2D image planes into a 3D stack. Data annotations are used to put " +
        "images into groups. All images in a group are then merged into a 3D stack. The order of the stack slices is determined by the 'Slice index annotation' " +
        "that is ignored while defining the groups." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlus2DData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlus3DData.class, slotName = "Output")
public class StackMergerAlgorithm extends ACAQMergingAlgorithm {

    private String counterAnnotation = "Slice";

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public StackMergerAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlus2DData.class)
                .addOutputSlot("Output", ImagePlus3DData.class, "Input", TO_3D_CONVERSION)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public StackMergerAlgorithm(StackMergerAlgorithm other) {
        super(other);
        this.counterAnnotation = other.counterAnnotation;
    }


    @Override
    protected Set<String> getIgnoredTraitColumns() {
        if (StringUtils.isNullOrEmpty(counterAnnotation))
            return super.getIgnoredTraitColumns();
        else
            return Collections.singleton(counterAnnotation);
    }

    @Override
    protected void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Set<Integer> inputRows = dataInterface.getInputRows(getFirstInputSlot());
        List<Integer> sortedInputRows;
        if (!StringUtils.isNullOrEmpty(counterAnnotation)) {
            ACAQAnnotation defaultCounter = new ACAQAnnotation(counterAnnotation, "");
            sortedInputRows = inputRows.stream().sorted(Comparator.comparing(row ->
                    (ACAQAnnotation) (getFirstInputSlot().getAnnotationOr(row, counterAnnotation, defaultCounter)))).collect(Collectors.toList());
            dataInterface.removeGlobalAnnotation(counterAnnotation);
        } else {
            sortedInputRows = new ArrayList<>(inputRows);
        }

        if (sortedInputRows.isEmpty())
            return;

        ImagePlus firstSlice = getFirstInputSlot().getData(sortedInputRows.get(0), ImagePlus2DData.class).getImage();
        ImageStack stack = new ImageStack(firstSlice.getWidth(), firstSlice.getHeight(), sortedInputRows.size());
        for (int i = 0; i < sortedInputRows.size(); ++i) {
            ImagePlus copy = getFirstInputSlot().getData(sortedInputRows.get(i), ImagePlus2DData.class).getImage();
            stack.setProcessor(copy.getProcessor(), i + 1);
            stack.setSliceLabel("slice=" + i, i + 1);
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Stack", stack)));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @ACAQDocumentation(name = "Slice index annotation",
            description = "Data annotation that is used as reference for ordering the slices. Annotation values are lexicographically sorted.")
    @ACAQParameter("counter-annotation-type")
    public String getCounterAnnotation() {
        return counterAnnotation;
    }

    @ACAQParameter("counter-annotation-type")
    public void setCounterAnnotation(String counterAnnotation) {
        this.counterAnnotation = counterAnnotation;
    }
}
