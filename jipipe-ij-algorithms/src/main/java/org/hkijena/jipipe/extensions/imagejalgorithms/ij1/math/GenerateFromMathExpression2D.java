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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.math;

import com.fathzer.soft.javaluator.StaticVariableSet;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.PixelCoordinate2DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@JIPipeDocumentation(name = "Generate from math expression", description = "Applies a mathematical operation to each pixel. " +
        "This node uses JIPipe's expression parameter instead of the functions integrated in ImageJ, which might have a lower performance.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class GenerateFromMathExpression2D extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter function = new DefaultExpressionParameter("x + y");
    private int width = 256;
    private int height = 256;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GenerateFromMathExpression2D(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GenerateFromMathExpression2D(GenerateFromMathExpression2D other) {
        super(other);
        this.function = new DefaultExpressionParameter(other.function);
        this.width = other.width;
        this.height = other.height;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = IJ.createImage("Generated", "32-bit", width, height, 1);
        ImageProcessor ip = img.getProcessor();
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        variableSet.set("width", width);
        variableSet.set("height", height);

        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                variableSet.set("x", (double) x);
                variableSet.set("y", (double) y);
                float value = ((Number) function.evaluate(variableSet)).floatValue();
                ip.setf(x, y, value);
            }
        }

        ImageJUtils.calibrate(img, ImageJCalibrationMode.AutomaticImageJ, 0, 0);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }

    @JIPipeDocumentation(name = "Function", description = "The function that is applied to each pixel. The expression should return a number.")
    @JIPipeParameter("function")
    @ExpressionParameterSettings(variableSource = PixelCoordinate2DExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getFunction() {
        return function;
    }

    @JIPipeParameter("function")
    public void setFunction(DefaultExpressionParameter transformation) {
        this.function = transformation;
    }

    @JIPipeDocumentation(name = "Width", description = "The width of the generated image")
    @JIPipeParameter("width")
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @JIPipeDocumentation(name = "Height", description = "The height of the generated image")
    @JIPipeParameter("height")
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }
}
