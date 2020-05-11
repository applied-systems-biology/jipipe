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
import org.hkijena.acaq5.api.traits.*;
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
@ACAQDocumentation(name = "Create 3D stack", description = "Merges 2D image planes into a 3D stack")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlus2DData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlus3DData.class, slotName = "Output")
public class StackMergerAlgorithm extends ACAQAlgorithm {

    private ACAQTraitDeclarationRefCollection referenceAnnotations = new ACAQTraitDeclarationRefCollection();
    private ACAQTraitDeclarationRef counterAnnotation = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("image-index"));
    private boolean overrideReferenceAnnotations = false;

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
        this.referenceAnnotations = new ACAQTraitDeclarationRefCollection(other.referenceAnnotations);
        this.counterAnnotation = new ACAQTraitDeclarationRef(other.counterAnnotation);
        this.overrideReferenceAnnotations = other.overrideReferenceAnnotations;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        Set<ACAQTraitDeclaration> currentReferenceAnnotations;
        if (!overrideReferenceAnnotations) {
            // Try to figure out annotations
            ACAQTraitMerger mergedAnnotations = new ACAQTraitMerger();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); ++row) {
                mergedAnnotations.addAll(getFirstInputSlot().getAnnotations(row));
            }
            if (counterAnnotation != null && counterAnnotation.getDeclaration() != null) {
                mergedAnnotations.removeAnnotationType(counterAnnotation.getDeclaration());
            }
            currentReferenceAnnotations = mergedAnnotations.getAnnotationTypes();
        } else {
            currentReferenceAnnotations = referenceAnnotations.stream()
                    .map(ACAQTraitDeclarationRef::getDeclaration).collect(Collectors.toSet());
        }

        if (currentReferenceAnnotations.isEmpty()) {
            ACAQTraitMerger mergedAnnotations = new ACAQTraitMerger();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); ++row) {
                mergedAnnotations.addAll(getFirstInputSlot().getAnnotations(row));
            }
            if (counterAnnotation != null && counterAnnotation.getDeclaration() != null) {
                mergedAnnotations.removeAnnotationType(counterAnnotation.getDeclaration());
            }
            List<ImagePlus2DData> slices = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); ++row) {
                slices.add(getFirstInputSlot().getData(row, ImagePlus2DData.class));
            }
            if (counterAnnotation != null && counterAnnotation.getDeclaration() != null) {
                Map<ImagePlus2DData, ACAQTrait> counterAssignment = new HashMap<>();
                for (int i = 0; i < slices.size(); ++i) {
                    counterAssignment.put(slices.get(i), getFirstInputSlot().getAnnotations(i).stream()
                            .filter(t -> t != null && t.getDeclaration() == counterAnnotation.getDeclaration()).findFirst().orElse(null));
                }
                slices.sort(Comparator.nullsFirst(Comparator.comparing(slice -> (ACAQDiscriminator) counterAssignment.get(slice))));
            }
            // Add slices
            ImagePlus firstSlice = slices.get(0).getImage();
            ImageStack stack = new ImageStack(firstSlice.getWidth(), firstSlice.getHeight(), slices.size());
            for (int i = 0; i < slices.size(); ++i) {
                ImagePlus copy = slices.get(i).getImage();
                stack.setProcessor(copy.getProcessor(), i + 1);
                stack.setSliceLabel("slice=" + i, i + 1);
            }

            getFirstOutputSlot().addData(new ImagePlus3DData(new ImagePlus("Output", stack)), mergedAnnotations.getResult());
        } else {
            // Find rows to do
            Set<Integer> rowsTodo = new HashSet<>();
            for (int i = 0; i < getFirstInputSlot().getRowCount(); ++i) {
                rowsTodo.add(i);
            }

            // Iterate through interfaces
            for (int row = 0; row < getFirstInputSlot().getRowCount(); ++row) {
                if (!rowsTodo.contains(row))
                    continue;
                algorithmProgress.accept(subProgress.resolve("Data row " + (row + 1) + " / " + getFirstInputSlot().getRowCount()));
                ACAQMultiDataInterface dataInterface = new ACAQMultiDataInterface(this,
                        Arrays.asList(getFirstInputSlot()),
                        currentReferenceAnnotations,
                        row);
                rowsTodo.removeAll(dataInterface.getRows(getFirstInputSlot()));

                // Merge slices
                List<ImagePlus2DData> slices = dataInterface.getInputData(getFirstInputSlot(), ImagePlus2DData.class);
                if (slices.isEmpty())
                    continue;

                // Collect annotations
                List<List<ACAQTrait>> sliceAnnotations = dataInterface.getAnnotations(getFirstInputSlot());
                ACAQTraitMerger mergedAnnotations = new ACAQTraitMerger();
                for (List<ACAQTrait> annotation : sliceAnnotations) {
                    mergedAnnotations.addAll(annotation);
                }
                if (counterAnnotation != null && counterAnnotation.getDeclaration() != null) {
                    mergedAnnotations.removeAnnotationType(counterAnnotation.getDeclaration());
                }

                // Apply sorting
                if (counterAnnotation != null && counterAnnotation.getDeclaration() != null) {
                    Map<ImagePlus2DData, ACAQTrait> counterAssignment = new HashMap<>();
                    for (int i = 0; i < slices.size(); ++i) {
                        counterAssignment.put(slices.get(i), sliceAnnotations.get(i).stream()
                                .filter(t -> t != null && t.getDeclaration() == counterAnnotation.getDeclaration()).findFirst().orElse(null));
                    }
                    slices.sort(Comparator.nullsFirst(Comparator.comparing(slice -> (ACAQDiscriminator) counterAssignment.get(slice))));
                }

                // Add slices
                ImagePlus firstSlice = slices.get(0).getImage();
                ImageStack stack = new ImageStack(firstSlice.getWidth(), firstSlice.getHeight(), slices.size());
                for (int i = 0; i < slices.size(); ++i) {
                    ImagePlus copy = slices.get(i).getImage();
                    stack.setProcessor(copy.getProcessor(), i + 1);
                    stack.setSliceLabel("slice=" + i, i + 1);
                }

                getFirstOutputSlot().addData(new ImagePlus3DData(new ImagePlus("Output", stack)), mergedAnnotations.getResult());
            }
        }
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

    @ACAQDocumentation(name = "Reference annotations",
            description = "Data annotation types that are used as reference to group slices.")
    @ACAQParameter("reference-annotation-types")
    public ACAQTraitDeclarationRefCollection getReferenceAnnotations() {
        return referenceAnnotations;
    }

    @ACAQParameter("reference-annotation-types")
    public void setReferenceAnnotations(ACAQTraitDeclarationRefCollection referenceAnnotations) {
        this.referenceAnnotations = referenceAnnotations;
    }

    @ACAQDocumentation(name = "Custom reference annotation", description = "If true, you can change which annotations are referenced when determining the source of slices.")
    @ACAQParameter("override-reference-annotations")
    public boolean isOverrideReferenceAnnotations() {
        return overrideReferenceAnnotations;
    }

    @ACAQParameter("override-reference-annotations")
    public void setOverrideReferenceAnnotations(boolean overrideReferenceAnnotations) {
        this.overrideReferenceAnnotations = overrideReferenceAnnotations;
    }
}
