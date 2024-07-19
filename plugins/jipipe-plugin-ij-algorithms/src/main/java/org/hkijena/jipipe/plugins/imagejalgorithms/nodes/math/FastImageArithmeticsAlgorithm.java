/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.math;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionCustomASTParser;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Fast image arithmetics", description = "Applies standard arithmetic and logical operations including, addition, subtraction, division, multiplication, GAMMA, EXP, LOG, SQR, SQRT, ABS, AND, OR, XOR, minimum, maximum.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output")
public class FastImageArithmeticsAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalBitDepth bitDepth = OptionalBitDepth.Grayscale32f;
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("I1 + I2");
    private final JIPipeExpressionCustomASTParser astParser = new JIPipeExpressionCustomASTParser();

    public FastImageArithmeticsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ImagePlusGreyscaleData.class)
                .addInputSlot("I1", "", ImagePlusGreyscaleData.class, true)
                .addInputSlot("I2", "", ImagePlusGreyscaleData.class, true)
                .addOutputSlot("Output", "", ImagePlusGreyscaleData.class)
                .sealOutput()
                .build());

        buildASTParser();
    }

    public FastImageArithmeticsAlgorithm(FastImageArithmeticsAlgorithm other) {
        super(other);
        this.bitDepth = other.bitDepth;
        this.expression = new JIPipeExpressionParameter(other.expression);

        buildASTParser();
    }

    private void buildASTParser() {
        // Build AST parser
        astParser.addOperator("+", 1)
                .addOperator("-", 1)
                .addOperator("*", 2)
                .addOperator("/", 2);
        astParser.addFunctions("MIN", "MAX", "SQR", "SQRT", "EXP", "LN", "INVERT", "ABS", "AND", "OR", "XOR", "NOT");
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Collect all input images
        Map<String, ImagePlus> inputImagesMap = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            ImagePlusData image = iterationStep.getInputData(inputSlot, ImagePlusGreyscaleData.class, progressInfo);
            if (image != null) {
                inputImagesMap.put(inputSlot.getName(), image.getImage());
            }
        }

        // Check if we have at least 1 image
        if (inputImagesMap.isEmpty()) {
            throw new IllegalArgumentException("No images to process!");
        }

        // Check if the images have the same size
        if (!ImageJUtils.imagesHaveSameSize(inputImagesMap.values())) {
            throw new IllegalArgumentException("Input images do not have the same size.");
        }

        // Convert inputs to target bit-depth
        int targetBitDepth;
        {

            if (bitDepth == OptionalBitDepth.None) {
                targetBitDepth = ImageJUtils.getConsensusBitDepth(inputImagesMap.values());
            } else {
                targetBitDepth = bitDepth.getBitDepth();
                if (targetBitDepth == 24) {
                    targetBitDepth = 32;
                }
            }

            progressInfo.log("Target bit-depth: " + targetBitDepth);

            for (String key : inputImagesMap.keySet()) {
                ImagePlus imagePlus = ImageJUtils.convertToBitDepthIfNeeded(inputImagesMap.get(key), targetBitDepth);
                inputImagesMap.put(key, imagePlus);
            }
        }

        // Parse the expression
        List<String> tokens = JIPipeExpressionParameter.getEvaluatorInstance().tokenize(expression.getExpression(), false, false);
        JIPipeExpressionCustomASTParser.ASTNode astNode = astParser.parse(tokens);

        ImagePlus referenceImage = inputImagesMap.values().iterator().next();
        int finalTargetBitDepth = targetBitDepth;
        ImagePlus outputImage = ImageJUtils.generateForEachIndexedZCTSlice(referenceImage, (referenceIp, index) ->
                applyAST(inputImagesMap, astNode, index, referenceImage.getWidth(), referenceImage.getHeight(), finalTargetBitDepth, progressInfo), progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    private ImageProcessor applyAST(Map<String, ImagePlus> inputImagesMap, JIPipeExpressionCustomASTParser.ASTNode astNode, ImageSliceIndex index, int width, int height, int bitDepth, JIPipeProgressInfo progressInfo) {
        // Build a DAG
        DefaultDirectedGraph<JIPipeExpressionCustomASTParser.ASTNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        addASTNodeToGraph(graph, astNode, null);

        // Go through the DAG
        Map<JIPipeExpressionCustomASTParser.ASTNode, Object> storedData = new HashMap<>();
        while (true) {
            JIPipeExpressionCustomASTParser.ASTNode nextNode = graph.vertexSet().stream().filter(v -> graph.inDegreeOf(v) == 0).findFirst().orElse(null);
            if (nextNode != null) {
                if (nextNode instanceof JIPipeExpressionCustomASTParser.NumberNode) {
                    storedData.put(nextNode, ((JIPipeExpressionCustomASTParser.NumberNode) nextNode).getValue());
                } else if (nextNode instanceof JIPipeExpressionCustomASTParser.VariableNode) {
                    JIPipeExpressionCustomASTParser.VariableNode variableNode = (JIPipeExpressionCustomASTParser.VariableNode) nextNode;
                    ImageProcessor ip;
                    if ("x".equals(variableNode.getName())) {
                        ip = createXVariableProcessor(width, height, bitDepth);
                    } else if ("y".equals(variableNode.getName())) {
                        ip = createYVariableProcessor(width, height, bitDepth);
                    } else if ("z".equals(variableNode.getName())) {
                        ip = createConstantProcessor(width, height, bitDepth, index.getZ());
                    } else if ("c".equals(variableNode.getName())) {
                        ip = createConstantProcessor(width, height, bitDepth, index.getC());
                    } else if ("t".equals(variableNode.getName())) {
                        ip = createConstantProcessor(width, height, bitDepth, index.getT());
                    } else {
                        ip = ImageJUtils.getSliceZero(inputImagesMap.get(variableNode.getName()), index);
                    }
                    storedData.put(nextNode, ip);
                } else if (nextNode instanceof JIPipeExpressionCustomASTParser.OperationNode) {
                    JIPipeExpressionCustomASTParser.OperationNode operationNode = (JIPipeExpressionCustomASTParser.OperationNode) nextNode;

                    // Calculate output
                    Object ip = applyOperation(storedData.get(operationNode.getLeft()), storedData.get(operationNode.getRight()), operationNode.getOperator());
                    storedData.put(operationNode, ip);

                    // Clear inputs
                    for (JIPipeExpressionCustomASTParser.ASTNode node : Graphs.predecessorListOf(graph, nextNode)) {
                        storedData.remove(node);
                    }

                } else if (nextNode instanceof JIPipeExpressionCustomASTParser.FunctionNode) {
                    JIPipeExpressionCustomASTParser.FunctionNode functionNode = (JIPipeExpressionCustomASTParser.FunctionNode) nextNode;

                    // Calculate output
                    Object ip = applyFunction(storedData.keySet().stream().map(storedData::get).collect(Collectors.toList()), functionNode.getFunctionName());
                    storedData.put(functionNode, ip);

                    // Clear inputs
                    for (JIPipeExpressionCustomASTParser.ASTNode node : Graphs.predecessorListOf(graph, nextNode)) {
                        storedData.remove(node);
                    }
                }
            } else {
                break;
            }
        }

        Object result = storedData.get(astNode);
        if(result instanceof Number) {
            result = createConstantProcessor(width, height, bitDepth, ((Number) result).doubleValue());
        }
        return (ImageProcessor) result;
    }

    private Object applyFunction(List<Object> arguments, String functionName) {
        return null;
    }

    private Object applyOperation(Object left, Object right, String operator) {
        if(left instanceof Number && right instanceof Number) {
            return applyOperationNumberNumber(((Number) left).floatValue(), ((Number) right).floatValue(), operator);
        }
        else if(left instanceof Number && right instanceof ImageProcessor) {
            return applyOperationNumberImage(((Number) left).floatValue(), (ImageProcessor)right, operator);
        }
        else if(left instanceof ImageProcessor && right instanceof Number) {
            return applyOperationImageNumber((ImageProcessor)left, ((Number) right).floatValue(), operator);
        }
        else if(left instanceof ImageProcessor && right instanceof ImageProcessor) {
            return applyOperationImageImage((ImageProcessor)left, (ImageProcessor)right, operator);
        }
        else {
            throw new UnsupportedOperationException("Unsupported operator types: " + left + ", " + right);
        }
    }

    private ImageProcessor applyOperationImageImage(ImageProcessor left, ImageProcessor right, String operator) {
        switch (operator) {
            case "+": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left.getf(i) + right.getf(i));
                }
                return result;
            }
            case "-": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left.getf(i) - right.getf(i));
                }
                return result;
            }
            case "*": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left.getf(i) * right.getf(i));
                }
                return result;
            }
            case "/": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left.getf(i) / right.getf(i));
                }
                return result;
            }
            default:
                throw new UnsupportedOperationException("Unsupported operator type: " + operator);
        }
    }

    private ImageProcessor applyOperationImageNumber(ImageProcessor left, float right, String operator) {
        switch (operator) {
            case "/": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left.getf(i) / right);
                }
                return result;
            }
            case "-": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left.getf(i) - right);
                }
                return result;
            }
            default:
                // All other operators are commutative
                return applyOperationNumberImage(right, left, operator);
        }
    }

    private ImageProcessor applyOperationNumberImage(float left, ImageProcessor right, String operator) {
        switch (operator) {
            case "+": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left + right.getf(i));
                }
                return result;
            }
            case "-": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left - right.getf(i));
                }
                return result;
            }
            case "*": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left * right.getf(i));
                }
                return result;
            }
            case "/": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    result.setf(i, left / right.getf(i));
                }
                return result;
            }
            default:
                throw new UnsupportedOperationException("Unsupported operator type: " + operator);
        }
    }

    private float applyOperationNumberNumber(float left, float right, String operator) {
        switch (operator) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                return left / right;
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator);
        }
    }

    private ImageProcessor createConstantProcessor(int width, int height, int bitDepth, double v) {
        ImageProcessor processor = ImageJUtils.createProcessor(width, height, bitDepth);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                processor.setf(x, y, (float) v);
            }
        }
        return processor;
    }

    private ImageProcessor createYVariableProcessor(int width, int height, int bitDepth) {
        ImageProcessor processor = ImageJUtils.createProcessor(width, height, bitDepth);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                processor.setf(x, y, y);
            }
        }
        return processor;
    }

    private ImageProcessor createXVariableProcessor(int width, int height, int bitDepth) {
        ImageProcessor processor = ImageJUtils.createProcessor(width, height, bitDepth);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                processor.setf(x, y, x);
            }
        }
        return processor;
    }

    private void addASTNodeToGraph(DefaultDirectedGraph<JIPipeExpressionCustomASTParser.ASTNode, DefaultEdge> graph, JIPipeExpressionCustomASTParser.ASTNode astNode, JIPipeExpressionCustomASTParser.ASTNode parent) {
        graph.addVertex(astNode);
        if (parent != null) {
            graph.addEdge(astNode, parent);
        }
        if (astNode instanceof JIPipeExpressionCustomASTParser.OperationNode) {
            addASTNodeToGraph(graph, ((JIPipeExpressionCustomASTParser.OperationNode) astNode).getLeft(), astNode);
            addASTNodeToGraph(graph, ((JIPipeExpressionCustomASTParser.OperationNode) astNode).getRight(), astNode);
        } else if (astNode instanceof JIPipeExpressionCustomASTParser.FunctionNode) {
            for (JIPipeExpressionCustomASTParser.ASTNode argument : ((JIPipeExpressionCustomASTParser.FunctionNode) astNode).getArguments()) {
                addASTNodeToGraph(graph, argument, astNode);
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            if (!MacroUtils.isValidVariableName(inputSlot.getName())) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        reportContext,
                        "Invalid input name: " + inputSlot.getName(),
                        "The name of the input slot is not allowed.",
                        "Use only alphanumeric input slot names without spaces."));
            }
        }
        Exception exception = JIPipeExpressionParameter.getEvaluatorInstance().checkSyntax(expression.getExpression());
        if (exception != null) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Invalid expression syntax",
                    exception.getMessage(),
                    "Please fix the expression",
                    ExceptionUtils.getStackTrace(exception)));
        }
    }

    @SetJIPipeDocumentation(name = "Bit depth", description = "Allows to force a specific bit depth. If 'None' is selected, JIPipe automatically chooses the highest bit depth based on the input. " +
            "Please note that 'RGB' will be handled as 32-bit float, as the operations do not support RGB pixel types.")
    @JIPipeParameter("bit-depth")
    public OptionalBitDepth getBitDepth() {
        return bitDepth;
    }

    @JIPipeParameter("bit-depth")
    public void setBitDepth(OptionalBitDepth bitDepth) {
        this.bitDepth = bitDepth;
    }

    @SetJIPipeDocumentation(name = "Expression", description = "The math expression that calculates the output. Applied per pixel. Please note that most standard JIPipe expression functions are not available. Available variables and operations: " +
            "<ul>" +
            "<li>Input slot names reference the pixel value at the current coordinate</li>" +
            "<li><code>x</code>, <code>y</code>, <code>z</code>, <code>c</code>, <code>t</code> will point to the current location of the pixel</li>" +
            "<li>Numeric constants like <code>0.15</code> can be used</li>" +
            "<li>You can use brackets <code>( )</code> to ensure the correct order</li>" +
            "<li><code>[] + []</code> to add the pixel values</li>" +
            "<li><code>[] - []</code> to subtract the pixel values</li>" +
            "<li><code>[] * []</code> to multiply the pixel values</li>" +
            "<li><code>[] / []</code> to divide the pixel values</li>" +
            "<li><code>MIN([], [])</code> to calculate the minimum of the operands</li>" +
            "<li><code>MAX([], [])</code> to calculate the maximum of the operands</li>" +
            "<li><code>SQR([])</code> to calculate the square of the operand</li>" +
            "<li><code>SQRT([])</code> to calculate the square root of the operand</li>" +
            "<li><code>EXP([])</code> to calculate the e^operand</li>" +
            "<li><code>LN([])</code> to calculate the LN(operand)</li>" +
            "<li><code>ABS([])</code> to calculate absolute value</li>" +
            "<li><code>INVERT([])</code> calculates the inverted pixel value</li>" +
            "<li><code>AND([], [])</code>, <code>OR([], [])</code>, <code>XOR([], [])</code>, <code>NOT([])</code> to calculate logical operations </li>" +
            "</ul>")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterSettings(withoutEditorButton = true)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }
}
