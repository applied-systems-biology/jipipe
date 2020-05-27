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
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQTraitParameterSettings;
import org.hkijena.acaq5.extensions.parameters.editors.StringParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Reduce stack", description = "Selects only the specified planes from an image stack.")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class StackReducerAlgorithm extends ImageJ1Algorithm {

    private boolean createAnnotation = true;
    private boolean ignoreMissingSlices = false;
    private String sliceSelection = "";
    private ACAQTraitDeclarationRef annotationType = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("image-index"));

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public StackReducerAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public StackReducerAlgorithm(StackReducerAlgorithm other) {
        super(other);
        this.createAnnotation = other.createAnnotation;
        this.annotationType = new ACAQTraitDeclarationRef(other.annotationType);
        this.ignoreMissingSlices = other.ignoreMissingSlices;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        List<Integer> integers = StringUtils.stringToPositiveInts(sliceSelection);
        if(ignoreMissingSlices) {
            integers.removeIf(i -> i >= inputData.getImage().getStackSize());
        }
        else {
            for (Integer integer : integers) {
                if(integer >= inputData.getImage().getStackSize()) {
                    throw new UserFriendlyRuntimeException("Data does not have slice: " + integer,
                            "Invalid slice requested!",
                            "Algorithm '" + getName() + "'",
                            "The algorithm was set up to select slice " + integer + ", but this slice does not exist. The image only has " + inputData.getImage().getStackSize() + " slices.",
                            "Please check if the incoming data has at least the amount of slices as requested. Please do not forget that the first slice index is zero. " +
                                    "If you are sure what you do, enable 'Ignore missing slices' in the algorithm settings.");
                }
            }
        }
        if(integers.isEmpty()) {
            throw new UserFriendlyRuntimeException("No slices selected!",
                    "No slices selected!",
                    "Algorithm '" + getName() + "'",
                    "You have to select a valid set of slices from the data set.",
                    "Please check if the incoming data has at least the amount of slices as requested.");
        }
        ImagePlus img = inputData.getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        List<Integer> sliceIndices = integers.stream().distinct().sorted().collect(Collectors.toList());
        for (Integer sliceIndex : sliceIndices) {
            stack.addSlice("" + sliceIndex, img.getStack().getProcessor(sliceIndex + 1));
        }
        ImagePlus result = new ImagePlus("Reduced stack", stack);
        List<ACAQTrait> annotations = new ArrayList<>();
        if(createAnnotation) {
            String index = "slice=" + sliceIndices.stream().map(i -> "" + i).collect(Collectors.joining(","));
            annotations.add(annotationType.getDeclaration().newInstance(index));
        }
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), annotations);
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (createAnnotation) {
            if (annotationType != null) {
                report.forCategory("Generated annotation").checkNonNull(annotationType.getDeclaration(), this);
            } else {
                report.forCategory("Generated annotation").checkNonNull(annotationType, this);
            }
        }
        try {
            List<Integer> integers = StringUtils.stringToPositiveInts(sliceSelection);
            if(integers.isEmpty()) {
                report.forCategory("Slice indices").reportIsInvalid("No slices selected!",
                        "You have to select at least one slice.",
                        "Please enter a valid selection (e.g. 10-15)",
                        this);
            }
        }
        catch(NumberFormatException | NullPointerException e) {
            report.forCategory("Slice indices").reportIsInvalid("Wrong slice index format!",
                    "The slice indices must follow a specific pattern. " + "The format is: [range];[range];... where [range] is " +
                         "either a number or a range of numbers notated as [from]-[to] (inclusive). An example is 0-10;12;20-21. The first index is zero.",
                    "Please enter a valid selection (e.g. 10-15)",
                    this);
        }
    }

    @ACAQDocumentation(name = "Create annotation", description = "Annotates the output by which slices have been selected.")
    @ACAQParameter("create-annotation")
    public boolean isCreateAnnotation() {
        return createAnnotation;
    }

    @ACAQParameter("create-annotation")
    public void setCreateAnnotation(boolean createAnnotation) {
        this.createAnnotation = createAnnotation;
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Determines the generated annotation type.")
    @ACAQParameter("annotation-type")
    @ACAQTraitParameterSettings(traitBaseClass = ACAQDiscriminator.class)
    public ACAQTraitDeclarationRef getAnnotationType() {
        return annotationType;
    }

    @ACAQParameter("annotation-type")
    public void setAnnotationType(ACAQTraitDeclarationRef annotationType) {
        this.annotationType = annotationType;
    }

    @ACAQDocumentation(name = "Ignore missing slices", description = "If enabled, slice indices outside of the image dimensions are ignored.")
    @ACAQParameter("ignore-missing-slices")
    public boolean isIgnoreMissingSlices() {
        return ignoreMissingSlices;
    }

    @ACAQParameter("ignore-missing-slices")
    public void setIgnoreMissingSlices(boolean ignoreMissingSlices) {
        this.ignoreMissingSlices = ignoreMissingSlices;
    }

    @ACAQDocumentation(name = "Slice indices", description = "The slice indices to select. Must have following format: [range];[range];... where [range] is " +
            "either a number or a range of numbers notated as [from]-[to] (inclusive). An example is 0-10;12;20-21. The first index is zero.")
    @ACAQParameter("slice-selection")
    @StringParameterSettings(monospace = true)
    public String getSliceSelection() {
        return sliceSelection;
    }

    @ACAQParameter("slice-selection")
    public void setSliceSelection(String sliceSelection) {
        this.sliceSelection = sliceSelection;
    }
}
