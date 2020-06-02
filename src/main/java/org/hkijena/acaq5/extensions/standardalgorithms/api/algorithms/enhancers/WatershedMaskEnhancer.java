package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.BadForTrait;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies distance transform watershed
 */
@ACAQDocumentation(name = "Distance transform watershed (deprecated)")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Morphology")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Input image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Output image", autoCreate = true)

// Trait matching
@GoodForTrait("bioobject-morphology-round")
@GoodForTrait("bioobject-preparations-labeling")
@BadForTrait("bioobject-preparations-labeling-unlabeled")
@GoodForTrait("bioobject-count-cluster")

// Trait configuration
@RemovesTrait("bioobject-count-cluster")
public class WatershedMaskEnhancer extends ACAQIteratingAlgorithm {

    private int erosionIterations = 0;

    /**
     * @param declaration algorithm declaration
     */
    public WatershedMaskEnhancer(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public WatershedMaskEnhancer(WatershedMaskEnhancer other) {
        super(other);
        this.erosionIterations = other.erosionIterations;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus2DGreyscaleMaskData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlus2DGreyscaleMaskData.class);
        ImagePlus img = inputData.getImage();

        EDM watershed = new EDM();
        ImagePlus result = img.duplicate();
        watershed.toWatershed(result.getProcessor());

        // Optional erosion steps
        Binary binaryFilter = new Binary();
        binaryFilter.setup("erode", null);
        for (int i = 0; i < erosionIterations; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(result));
    }

    @ACAQParameter("erosion-iterations")
    @ACAQDocumentation(name = "Erosion iterations")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @ACAQParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;
        getEventBus().post(new ParameterChangedEvent(this, "erosion-iterations"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}