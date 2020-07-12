package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AutomaticThreshold;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AutomaticThreshold}
 */
@JIPipeDocumentation(name = "CLIJ2 Automatic Threshold", description = "The automatic thresholder utilizes the threshold methods from ImageJ on a histogram determined on " + "the GPU to create binary images as similar as possible to ImageJ 'Apply Threshold' method." + " Enter one " + "of these methods in the method text field:" + "[Default, Huang, Intermodes, IsoData, IJ_IsoData, Li, MaxEntropy, Mean, MinError, Minimum, Moments, Otsu, Percentile, RenyiEntropy, Shanbhag, Triangle, Yen] Works for following image dimensions: 2D, 3D. Developed by Robert Haase based on work by G. Landini and W. Rasband. Licensed under The code for the automatic thresholding methods originates from https://github.com/imagej/imagej1/blob/master/ij/process/AutoThresholder.java" + "Detailed documentation on the implemented methods can be found online: https://imagej.net/Auto_Threshold")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2AutomaticThreshold extends JIPipeSimpleIteratingAlgorithm {
    String userSelectedMethod;
    Float minimumGreyValue;
    Float maximumGreyValue;
    Integer numberOfBins;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2AutomaticThreshold(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AutomaticThreshold(Clij2AutomaticThreshold other) {
        super(other);
        this.userSelectedMethod = other.userSelectedMethod;
        this.minimumGreyValue = other.minimumGreyValue;
        this.maximumGreyValue = other.maximumGreyValue;
        this.numberOfBins = other.numberOfBins;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        AutomaticThreshold.automaticThreshold(clij2, src, dst, userSelectedMethod, minimumGreyValue, maximumGreyValue, numberOfBins);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("user-selected-method")
    public String getUserSelectedMethod() {
        return userSelectedMethod;
    }

    @JIPipeParameter("user-selected-method")
    public void setUserSelectedMethod(String value) {
        this.userSelectedMethod = value;
    }

    @JIPipeParameter("minimum-grey-value")
    public Float getMinimumGreyValue() {
        return minimumGreyValue;
    }

    @JIPipeParameter("minimum-grey-value")
    public void setMinimumGreyValue(Float value) {
        this.minimumGreyValue = value;
    }

    @JIPipeParameter("maximum-grey-value")
    public Float getMaximumGreyValue() {
        return maximumGreyValue;
    }

    @JIPipeParameter("maximum-grey-value")
    public void setMaximumGreyValue(Float value) {
        this.maximumGreyValue = value;
    }

    @JIPipeParameter("number-of-bins")
    public Integer getNumberOfBins() {
        return numberOfBins;
    }

    @JIPipeParameter("number-of-bins")
    public void setNumberOfBins(Integer value) {
        this.numberOfBins = value;
    }

}