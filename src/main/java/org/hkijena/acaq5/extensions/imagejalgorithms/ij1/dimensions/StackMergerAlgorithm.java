package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_3D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Create 3D stack", description = "Merges 2D image planes into a 3D stack. Data annotations are used to put " +
        "images into groups. All images in a group are then merged into a 3D stack. The order of the stack slices is determined by the 'Slice index annotation' " +
        "that is ignored while defining the groups.")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlus2DData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlus3DData.class, slotName = "Output")
public class StackMergerAlgorithm extends ACAQMergingAlgorithm {

    private ACAQTraitDeclarationRef counterAnnotation = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("image-index"));

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
        this.counterAnnotation = new ACAQTraitDeclarationRef(other.counterAnnotation);
    }


    @Override
    protected Set<ACAQTraitDeclaration> getIgnoredTraitColumns() {
        if (counterAnnotation.getDeclaration() == null)
            return super.getIgnoredTraitColumns();
        else
            return Collections.singleton(counterAnnotation.getDeclaration());
    }

    @Override
    protected void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Set<Integer> inputRows = dataInterface.getInputRows(getFirstInputSlot());
        List<Integer> sortedInputRows;
        if (counterAnnotation.getDeclaration() != null) {
            ACAQTrait defaultCounter = (ACAQTrait) counterAnnotation.getDeclaration().newInstance("");
            sortedInputRows = inputRows.stream().sorted(Comparator.comparing(row ->
                    (ACAQTrait) (getFirstInputSlot().getAnnotationOr(row, counterAnnotation.getDeclaration(), defaultCounter)))).collect(Collectors.toList());
            dataInterface.removeGlobalAnnotation(counterAnnotation.getDeclaration());
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
    public ACAQTraitDeclarationRef getCounterAnnotation() {
        return counterAnnotation;
    }

    @ACAQParameter("counter-annotation-type")
    public void setCounterAnnotation(ACAQTraitDeclarationRef counterAnnotation) {
        this.counterAnnotation = counterAnnotation;
    }
}
