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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.math;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.pipelinej.extensions.imagejalgorithms.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link RankFilters}
 */
@ACAQDocumentation(name = "Replace NaN by local median", description = "Filter that removes NaN values from floating point images by replacing them by the local median. " +
        "If a multi-channel image is provided, the operation is applied to each channel. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Math", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")
public class RemoveNaNFilter2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double radius = 1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RemoveNaNFilter2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public RemoveNaNFilter2DAlgorithm(RemoveNaNFilter2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        RankFilters rankFilters = new RankFilters();
        ImageJUtils.forEachSlice(img, ip -> rankFilters.rank(ip, radius, RankFilters.REMOVE_NAN));
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Radius").checkIfWithin(this, radius, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @ACAQDocumentation(name = "Radius", description = "Radius of the filter kernel. See ImageJ>Process>Filters>Show Circular Masks for a reference.")
    @ACAQParameter("radius")
    public double getRadius() {
        return radius;
    }

    @ACAQParameter("radius")
    public boolean setRadius(double radius) {
        if (radius <= 0) {
            return false;
        }
        this.radius = radius;

        return true;
    }
}
