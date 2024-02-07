package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import com.google.common.collect.ImmutableList;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.parameters.ImageQueryExpressionVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;
import java.util.Collection;

@JIPipeDocumentation(name = "Add border 2D", description = "Adds a border around the image. If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Canvas Size... (advanced)")
public class AddBorder2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private BorderMode borderMode = BorderMode.Constant;
    private JIPipeExpressionParameter borderColor = new JIPipeExpressionParameter("0");

    private JIPipeExpressionParameter marginLeft = new JIPipeExpressionParameter("10");

    private JIPipeExpressionParameter marginTop = new JIPipeExpressionParameter("10");

    private JIPipeExpressionParameter marginRight = new JIPipeExpressionParameter("10");

    private JIPipeExpressionParameter marginBottom = new JIPipeExpressionParameter("10");

    public AddBorder2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AddBorder2DAlgorithm(AddBorder2DAlgorithm other) {
        super(other);
        this.borderMode = other.borderMode;
        this.borderColor = new JIPipeExpressionParameter(other.borderColor);
        this.marginLeft = new JIPipeExpressionParameter(other.marginLeft);
        this.marginTop = new JIPipeExpressionParameter(other.marginTop);
        this.marginRight = new JIPipeExpressionParameter(other.marginRight);
        this.marginBottom = new JIPipeExpressionParameter(other.marginBottom);
    }

    public static ImagePlus addBorder(ImagePlus img, int left, int top, int right, int bottom, BorderMode borderMode, double colorGreyscale, Color colorRGB, JIPipeProgressInfo progressInfo) {
        int width = img.getWidth() + left + right;
        int height = img.getHeight() + top + bottom;
        ImagePlus result = IJ.createHyperStack(img.getTitle() + "-border", width, height, img.getNChannels(), img.getNSlices(), img.getNFrames(), img.getBitDepth());

        ImageJUtils.forEachIndexedZCTSlice(result, (resultIp, index) -> {
            ImageProcessor sourceIp = ImageJUtils.getSliceZero(img, index);
            if (borderMode == BorderMode.Constant) {
                // Constant: Fill first
                if (resultIp instanceof ColorProcessor) {
                    resultIp.setColor(colorRGB);
                } else {
                    resultIp.setColor(colorGreyscale);
                }
                resultIp.fill();
            }

            // Copy source to target
            resultIp.insert(sourceIp, left, top);

            if (borderMode != BorderMode.Constant) {
                int originalWidth = img.getWidth();
                int originalHeight = img.getHeight();

                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        if (x >= left && x < originalWidth + left && y >= top && y < originalHeight + top)
                            continue;
                        int targetColor;
                        if (borderMode == BorderMode.Repeat) {
                            int sourceX = Math.max(left, Math.min(originalWidth + left - 1, x));
                            int sourceY = Math.max(top, Math.min(originalHeight + top - 1, y));
                            targetColor = resultIp.get(sourceX, sourceY);
                        } else if (borderMode == BorderMode.Tile) {
                            int sourceX = x;
                            int sourceY = y;

                            while (sourceX < left || sourceX >= originalWidth + left) {
                                if (sourceX < left) {
                                    sourceX += originalWidth;
                                }
                                if (sourceX >= originalWidth + left) {
                                    sourceX = sourceX - originalWidth;
                                }
                            }

                            while (sourceY < top || sourceY >= originalHeight + top) {
                                if (sourceY < top) {
                                    sourceY += originalHeight;
                                }
                                if (sourceY >= originalHeight + top) {
                                    sourceY = sourceY - originalHeight;
                                }
                            }

                            targetColor = resultIp.get(sourceX, sourceY);
                        } else if (borderMode == BorderMode.Mirror) {
                            int sourceX = x - left;
                            int sourceY = y - top;

                            while (sourceX < 0 || sourceX >= originalWidth) {
                                if (sourceX < 0)
                                    sourceX = -sourceX - 1;
                                if (sourceX >= originalWidth)
                                    sourceX = (originalWidth - 1) - (sourceX - originalWidth);
                            }
                            while (sourceY < 0 || sourceY >= originalHeight) {
                                if (sourceY < 0)
                                    sourceY = -sourceY - 1;
                                if (sourceY >= originalHeight)
                                    sourceY = (originalHeight - 1) - (sourceY - originalHeight);
                            }
                            targetColor = resultIp.get(sourceX + left, sourceY + top);
                        } else {
                            throw new UnsupportedOperationException();
                        }

                        resultIp.set(x, y, targetColor);
                    }
                }
            }

        }, progressInfo);

        result.copyScale(img);
        return result;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        ImageQueryExpressionVariablesInfo.buildVariablesSet(img, variables);

        int left = (int) (marginLeft.evaluateToNumber(variables));
        int top = (int) (marginTop.evaluateToNumber(variables));
        int right = (int) (marginRight.evaluateToNumber(variables));
        int bottom = (int) (marginBottom.evaluateToNumber(variables));

        double colorGreyscale = 0;
        Color colorRGB = Color.BLACK;

        if (borderMode == BorderMode.Constant) {
            Object obj = borderColor.evaluate(variables);
            if (obj instanceof Number) {
                colorGreyscale = ((Number) obj).doubleValue();
                colorRGB = new Color((int) colorGreyscale, (int) colorGreyscale, (int) colorGreyscale);
            } else if (obj instanceof Collection) {
                ImmutableList<?> objects = ImmutableList.copyOf((Collection<?>) obj);
                int r = ((Number) (objects.get(0))).intValue();
                int g = ((Number) (objects.get(1))).intValue();
                int b = ((Number) (objects.get(2))).intValue();
                colorGreyscale = (r + g + b) / 3;
                colorRGB = new Color(r, g, b);
            }
        }

        ImagePlus withBorder = addBorder(img, left, top, right, bottom, borderMode, colorGreyscale, colorRGB, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(withBorder), progressInfo);
    }

    @JIPipeDocumentation(name = "Mode", description = "Determines how border values are generated")
    @JIPipeParameter("border-mode")
    public BorderMode getBorderMode() {
        return borderMode;
    }

    @JIPipeParameter("border-mode")
    public void setBorderMode(BorderMode borderMode) {
        this.borderMode = borderMode;
    }

    @JIPipeDocumentation(name = "Border color", description = "Only applicable if the mode is 'Constant'. The value of the border. Return a number to return a greyscale value. " +
            "Return an array to set the border to an RGB color.")
    @JIPipeParameter("border-color")
    @JIPipeExpressionParameterSettings(variableSource = TextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getBorderColor() {
        return borderColor;
    }

    @JIPipeParameter("border-color")
    public void setBorderColor(JIPipeExpressionParameter borderColor) {
        this.borderColor = borderColor;
    }

    @JIPipeDocumentation(name = "Margin left", description = "Pixels to add to the left of the image")
    @JIPipeParameter("margin-left")
    @JIPipeExpressionParameterSettings(variableSource = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = ImageQueryExpressionVariablesInfo.class)
    public JIPipeExpressionParameter getMarginLeft() {
        return marginLeft;
    }

    @JIPipeParameter("margin-left")
    public void setMarginLeft(JIPipeExpressionParameter marginLeft) {
        this.marginLeft = marginLeft;
    }

    @JIPipeDocumentation(name = "Margin top", description = "Pixels to add to the top of the image")
    @JIPipeParameter("margin-top")
    @JIPipeExpressionParameterSettings(variableSource = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = ImageQueryExpressionVariablesInfo.class)
    public JIPipeExpressionParameter getMarginTop() {
        return marginTop;
    }

    @JIPipeParameter("margin-top")
    public void setMarginTop(JIPipeExpressionParameter marginTop) {
        this.marginTop = marginTop;
    }

    @JIPipeDocumentation(name = "Margin right", description = "Pixels to add to the right of the image")
    @JIPipeParameter("margin-right")
    @JIPipeExpressionParameterSettings(variableSource = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = ImageQueryExpressionVariablesInfo.class)
    public JIPipeExpressionParameter getMarginRight() {
        return marginRight;
    }

    @JIPipeParameter("margin-right")
    public void setMarginRight(JIPipeExpressionParameter marginRight) {
        this.marginRight = marginRight;
    }

    @JIPipeDocumentation(name = "Margin bottom", description = "Pixels to add to the bottom of the image")
    @JIPipeParameter("margin-bottom")
    @JIPipeExpressionParameterSettings(variableSource = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = ImageQueryExpressionVariablesInfo.class)
    public JIPipeExpressionParameter getMarginBottom() {
        return marginBottom;
    }

    @JIPipeParameter("margin-bottom")
    public void setMarginBottom(JIPipeExpressionParameter marginBottom) {
        this.marginBottom = marginBottom;
    }
}
