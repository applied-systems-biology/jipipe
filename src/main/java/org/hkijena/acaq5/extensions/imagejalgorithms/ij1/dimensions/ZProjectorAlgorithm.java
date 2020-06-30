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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.plugin.ZProjector;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.DECREASE_DIMENSION_CONVERSION;
import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.plugin.ZProjector}
 */
@ACAQDocumentation(name = "Z-Project", description = "Performs a Z-Projection.")
@ACAQOrganization(menuPath = "Dimensions", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class ZProjectorAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private Method method = Method.MaxIntensity;
    private int startSlice = 0;
    private int stopSlice = -1;
    private boolean projectAllHyperstackTimePoints = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public ZProjectorAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output",
                        ImagePlusData.class,
                        "Input",
                        ACAQSlotDefinition.composeRawInheritanceConversions(REMOVE_MASK_QUALIFIER, DECREASE_DIMENSION_CONVERSION))
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ZProjectorAlgorithm(ZProjectorAlgorithm other) {
        super(other);
        this.method = other.method;
        this.startSlice = other.startSlice;
        this.stopSlice = other.stopSlice;
        this.projectAllHyperstackTimePoints = other.projectAllHyperstackTimePoints;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();

        ImagePlus result;
        if (img.getStackSize() > 1) {
            int start = startSlice >= 0 ? startSlice + 1 : 1;
            int end = stopSlice >= 0 ? Math.min(img.getStackSize(), stopSlice + 1) : img.getStackSize();
            result = ZProjector.run(img, method.toString() + (projectAllHyperstackTimePoints ? " all" : ""), start, end);
        } else {
            result = img;
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Start slice").checkIfWithin(this, startSlice, -1, Double.POSITIVE_INFINITY, true, true);
        report.forCategory("Stop slice").checkIfWithin(this, stopSlice, -1, Double.POSITIVE_INFINITY, true, true);
    }

    @ACAQDocumentation(name = "Method", description = "The function that is applied to each stack of pixels.")
    @ACAQParameter("method")
    public Method getMethod() {
        return method;
    }

    @ACAQParameter("method")
    public void setMethod(Method method) {
        this.method = method;
        getEventBus().post(new ParameterChangedEvent(this, "method"));
    }

    @ACAQParameter("start-slice")
    @ACAQDocumentation(name = "Start slice", description = "The slice number to start from. The minimum number is zero.")
    public int getStartSlice() {
        return startSlice;
    }

    @ACAQParameter("start-slice")
    public boolean setStartSlice(int startSlice) {
        if (startSlice < 0) {
            this.startSlice = 0;
            return false;
        }
        this.startSlice = startSlice;
        getEventBus().post(new ParameterChangedEvent(this, "start-slice"));
        return true;
    }

    @ACAQParameter("stop-slice")
    @ACAQDocumentation(name = "Stop slice", description = "Slice index that is included last. This is inclusive. Set to -1 to always include all slices.")
    public int getStopSlice() {
        return stopSlice;
    }

    @ACAQParameter("stop-slice")
    public boolean setStopSlice(int stopSlice) {
        if (stopSlice < -1) {
            this.startSlice = -1;
            return false;
        }
        this.stopSlice = stopSlice;
        getEventBus().post(new ParameterChangedEvent(this, "stop-slice"));
        return true;
    }

    @ACAQDocumentation(name = "Project all hyper stack time points", description = "If true, all time frames are projected")
    @ACAQParameter("all-hyperstack-timepoints")
    public boolean isProjectAllHyperstackTimePoints() {
        return projectAllHyperstackTimePoints;
    }

    @ACAQParameter("all-hyperstack-timepoints")
    public void setProjectAllHyperstackTimePoints(boolean projectAllHyperstackTimePoints) {
        this.projectAllHyperstackTimePoints = projectAllHyperstackTimePoints;
        getEventBus().post(new ParameterChangedEvent(this, "all-hyperstack-timepoints"));
    }

    /**
     * Available transformation functions
     */
    public enum Method {
        AverageIntensity, MaxIntensity, MinIntensity, SumSlices, StandardDeviation, Median
    }
}
