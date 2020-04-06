package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.Thresholder;
import ij.process.AutoThresholder;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.AddsTrait;
import org.hkijena.acaq5.api.data.traits.BadForTrait;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Segmenter node that thresholds via an auto threshold
 */
@ACAQDocumentation(name = "Auto threshold segmentation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmentation)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)

// Trait matching
@GoodForTrait("image-quality-brightness-uniform")
@BadForTrait("image-quality-brightness-nonuniform")

// Trait configuration
@RemovesTrait("image-quality")
@AddsTrait("bioobject-count-cluster")
public class AutoThresholdSegmenter extends ACAQIteratingAlgorithm {

    private AutoThresholder.Method method = AutoThresholder.Method.Default;

    /**
     * @param declaration the declaration
     */
    public AutoThresholdSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AutoThresholdSegmenter(AutoThresholdSegmenter other) {
        super(other);
        this.method = other.method;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus2DGreyscaleData inputData = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputData.getImage();
        ImagePlus result = img.duplicate();

        Thresholder.setMethod(method.name());
        Prefs.blackBackground = true;
        ImageJUtils.runOnImage(result, new Thresholder());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(result));
    }

    @ACAQParameter("method")
    @ACAQDocumentation(name = "Method")
    public AutoThresholder.Method getMethod() {
        return method;
    }

    @ACAQParameter("method")
    public void setMethod(AutoThresholder.Method method) {
        this.method = method;
        getEventBus().post(new ParameterChangedEvent(this, "method"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
