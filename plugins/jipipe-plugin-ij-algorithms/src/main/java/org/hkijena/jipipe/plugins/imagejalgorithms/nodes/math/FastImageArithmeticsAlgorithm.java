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

import com.google.common.collect.Sets;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluatorSyntaxTokenMaker;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.OptionalBitDepth;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

@SetJIPipeDocumentation(name = "Fast image arithmetics 2D", description = "Applies standard arithmetic and logical operations including, addition, subtraction, division, multiplication, GAMMA, EXP, LOG, SQR, SQRT, ABS, AND, OR, XOR, minimum, maximum, and more.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output")
public class FastImageArithmeticsAlgorithm extends JIPipeIteratingAlgorithm {

    public static final Set<String> FUNCTIONS = Sets.newHashSet("MIN", "MAX", "SQR", "SQRT", "EXP", "LN", "LOG", "INVERT", "ABS", "AND", "OR", "XOR", "NOT", "GAMMA", "POW", "MOD",
            "SIN", "SINH", "ASIN", "COS", "COSH", "ACOS", "TAN", "TANH", "ATAN2", "FLOOR", "CEIL", "ROUND", "SIGNUM", "IF_ELSE",
            "SLICE_MAX", "SLICE_MIN", "SLICE_MEAN", "SLICE_MEDIAN", "SLICE_STDDEV", "SLICE_SKEWNESS", "SLICE_MODE");
    public static final Set<String> CONSTANTS = Sets.newHashSet("x", "y", "c", "z", "t", "pi", "e", "width", "height", "numZ", "numC", "numT");
    private final JIPipeExpressionCustomASTParser astParser = new JIPipeExpressionCustomASTParser();
    private OptionalBitDepth bitDepth = OptionalBitDepth.Grayscale32f;
    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("I1 + 5");

    public FastImageArithmeticsAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ImagePlusGreyscaleData.class)
                .addInputSlot("I1", "", ImagePlusGreyscaleData.class, true)
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
        astParser.addOperator("+", 5)
                .addOperator("-", 5)
                .addOperator("*", 10)
                .addOperator("/", 10)
                .addOperator("<", 2)
                .addOperator(">", 2)
                .addOperator("<=", 2)
                .addOperator(">=", 2)
                .addOperator("==", 2);
        astParser.addFunctions(FUNCTIONS.toArray(new String[0]));
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
                applyAST(inputImagesMap,
                        astNode,
                        index,
                        referenceImage.getWidth(),
                        referenceImage.getHeight(),
                        referenceImage.getNSlices(),
                        referenceImage.getNChannels(),
                        referenceImage.getNFrames(),
                        finalTargetBitDepth,
                        iterationStep.getMergedTextAnnotations(),
                        progressInfo), progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }

    private ImageProcessor applyAST(Map<String, ImagePlus> inputImagesMap, JIPipeExpressionCustomASTParser.ASTNode astNode, ImageSliceIndex index, int width, int height, int numZ, int numC, int numT
            , int bitDepth, Map<String, JIPipeTextAnnotation> textAnnotationMap, JIPipeProgressInfo progressInfo) {
        // Build a DAG
        DefaultDirectedGraph<JIPipeExpressionCustomASTParser.ASTNode, DefaultEdge> flowGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        addASTNodeToGraph(flowGraph, astNode, null);

        // Find direct predecessors of each vertex
        Map<JIPipeExpressionCustomASTParser.ASTNode, Set<JIPipeExpressionCustomASTParser.ASTNode>> predecessors = new HashMap<>();
        for (JIPipeExpressionCustomASTParser.ASTNode node : flowGraph.vertexSet()) {
            predecessors.put(node, new HashSet<>(Graphs.predecessorListOf(flowGraph, node)));
        }

        // Go through the DAG
        Map<JIPipeExpressionCustomASTParser.ASTNode, Object> storedData = new HashMap<>();
        while (true) {

            if (progressInfo.isCancelled()) {
                return null;
            }

            JIPipeExpressionCustomASTParser.ASTNode nextNode = flowGraph.vertexSet().stream().filter(v -> flowGraph.inDegreeOf(v) == 0 && !storedData.containsKey(v)).findFirst().orElse(null);
//            System.out.println(nextNode);
            if (nextNode != null) {

                // Remove the node
                flowGraph.removeVertex(nextNode);

                if (nextNode instanceof JIPipeExpressionCustomASTParser.NumberNode) {
                    storedData.put(nextNode, ((JIPipeExpressionCustomASTParser.NumberNode) nextNode).getValue());
                } else if (nextNode instanceof JIPipeExpressionCustomASTParser.VariableNode) {
                    JIPipeExpressionCustomASTParser.VariableNode variableNode = (JIPipeExpressionCustomASTParser.VariableNode) nextNode;
                    Object ip;
                    if ("x".equals(variableNode.getName())) {
                        ip = createXVariableProcessor(width, height, bitDepth);
                    } else if ("y".equals(variableNode.getName())) {
                        ip = createYVariableProcessor(width, height, bitDepth);
                    } else if ("z".equals(variableNode.getName())) {
                        ip = index.getZ();
                    } else if ("c".equals(variableNode.getName())) {
                        ip = index.getC();
                    } else if ("t".equals(variableNode.getName())) {
                        ip = index.getT();
                    } else if ("e".equals(variableNode.getName())) {
                        ip = Math.E;
                    } else if ("pi".equals(variableNode.getName())) {
                        ip = Math.PI;
                    } else if ("width".equals(variableNode.getName())) {
                        ip = width;
                    } else if ("height".equals(variableNode.getName())) {
                        ip = height;
                    } else if ("numZ".equals(variableNode.getName())) {
                        ip = numZ;
                    } else if ("numC".equals(variableNode.getName())) {
                        ip = numC;
                    } else if ("numT".equals(variableNode.getName())) {
                        ip = numT;
                    } else if (inputImagesMap.containsKey(variableNode.getName())) {
                        ip = ImageJUtils.getSliceZero(inputImagesMap.get(variableNode.getName()), index);
                    } else if (variableNode.getName().startsWith("custom.") && getDefaultCustomExpressionVariables().containsKey(variableNode.getName().substring("custom.".length()))) {
                        // Custom variable
                        ip = StringUtils.parseDouble(getDefaultCustomExpressionVariables().get(variableNode.getName().substring("custom.".length())).get(Object.class).toString());
                    } else {
                        // Text annotation name
                        JIPipeTextAnnotation textAnnotation = textAnnotationMap.getOrDefault(variableNode.getName(), null);
                        if (textAnnotation == null) {
                            throw new IllegalArgumentException("Unknown text annotation: " + variableNode.getName() + ". If you expected an image input '" + variableNode.getName() + "', please check review the 'Inputs' panel.");
                        }
                        ip = StringUtils.parseDouble(textAnnotation.getValue());
                    }
                    storedData.put(nextNode, ip);
                } else if (nextNode instanceof JIPipeExpressionCustomASTParser.OperationNode) {
                    JIPipeExpressionCustomASTParser.OperationNode operationNode = (JIPipeExpressionCustomASTParser.OperationNode) nextNode;

                    // Calculate output
                    Object ip = applyOperation(storedData.get(operationNode.getLeft()), storedData.get(operationNode.getRight()), operationNode.getOperator(), bitDepth);
                    storedData.put(operationNode, ip);

                    // Clear inputs
                    for (JIPipeExpressionCustomASTParser.ASTNode node : predecessors.get(nextNode)) {
                        storedData.remove(node);
                    }

                } else if (nextNode instanceof JIPipeExpressionCustomASTParser.FunctionNode) {
                    JIPipeExpressionCustomASTParser.FunctionNode functionNode = (JIPipeExpressionCustomASTParser.FunctionNode) nextNode;

                    // Calculate output
                    List<Object> arguments = new ArrayList<>();
                    for (JIPipeExpressionCustomASTParser.ASTNode node : functionNode.getArguments()) {
                        arguments.add(storedData.get(node));
                    }
                    // storedData.keySet().stream().map(storedData::get).collect(Collectors.toList())
                    Object ip = applyFunction(arguments, functionNode.getFunctionName(), bitDepth);
                    storedData.put(functionNode, ip);

                    // Clear inputs
                    for (JIPipeExpressionCustomASTParser.ASTNode node : predecessors.get(nextNode)) {
                        storedData.remove(node);
                    }
                }
            } else {
                break;
            }
        }

        Object result = storedData.get(astNode);
        if (result instanceof Number) {
            result = createConstantProcessor(width, height, bitDepth, ((Number) result).doubleValue());
        }
        return (ImageProcessor) result;
    }

    private Object applyFunction(List<Object> arguments, String functionName, int bitDepth) {
        switch (functionName) {
            case "IF_ELSE":
                checkArgumentCount(functionName, arguments, 3);
                return applyIfElseFunction(arguments.get(0), arguments.get(1), arguments.get(2), bitDepth);
            case "MIN":
                checkArgumentCount(functionName, arguments, 2);
                return applyMinFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "MAX":
                checkArgumentCount(functionName, arguments, 2);
                return applyMaxFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "MOD":
                checkArgumentCount(functionName, arguments, 2);
                return applyModuloFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "SQR":
                checkArgumentCount(functionName, arguments, 1);
                return applyPowFunction(arguments.get(0), 2, bitDepth);
            case "POW":
                checkArgumentCount(functionName, arguments, 2);
                return applyPowFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "GAMMA":
                checkArgumentCount(functionName, arguments, 2);
                return applyGammaFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "SQRT":
                checkArgumentCount(functionName, arguments, 1);
                return applySqrtFunction(arguments.get(0), bitDepth);
            case "EXP":
                checkArgumentCount(functionName, arguments, 1);
                return applyExpFunction(arguments.get(0), bitDepth);
            case "LN":
            case "LOG":
                checkArgumentCount(functionName, arguments, 1);
                return applyLnFunction(arguments.get(0), bitDepth);
            case "ABS":
                checkArgumentCount(functionName, arguments, 1);
                return applyAbsFunction(arguments.get(0), bitDepth);
            case "INVERT":
                checkArgumentCount(functionName, arguments, 1);
                return applyInvertFunction(arguments.get(0), bitDepth);
            case "AND":
                checkArgumentCount(functionName, arguments, 2);
                return applyAndFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "OR":
                checkArgumentCount(functionName, arguments, 2);
                return applyOrFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "XOR":
                checkArgumentCount(functionName, arguments, 2);
                return applyXorFunction(arguments.get(0), arguments.get(1), bitDepth);
            case "NOT":
                checkArgumentCount(functionName, arguments, 1);
                return applyNotFunction(arguments.get(0), bitDepth);
            case "FLOOR":
                checkArgumentCount(functionName, arguments, 1);
                return applyFloorFunction(arguments.get(0), bitDepth);
            case "CEIL":
                checkArgumentCount(functionName, arguments, 1);
                return applyCeilFunction(arguments.get(0), bitDepth);
            case "ROUND":
                checkArgumentCount(functionName, arguments, 1);
                return applyRoundFunction(arguments.get(0), bitDepth);
            case "SIGNUM":
                checkArgumentCount(functionName, arguments, 1);
                return applySignumFunction(arguments.get(0), bitDepth);
            case "SIN":
                checkArgumentCount(functionName, arguments, 1);
                return applySinFunction(arguments.get(0), bitDepth);
            case "SINH":
                checkArgumentCount(functionName, arguments, 1);
                return applySinHFunction(arguments.get(0), bitDepth);
            case "ASIN":
                checkArgumentCount(functionName, arguments, 1);
                return applyASinFunction(arguments.get(0), bitDepth);
            case "COS":
                checkArgumentCount(functionName, arguments, 1);
                return applyCosFunction(arguments.get(0), bitDepth);
            case "COSH":
                checkArgumentCount(functionName, arguments, 1);
                return applyCosHFunction(arguments.get(0), bitDepth);
            case "ACOS":
                checkArgumentCount(functionName, arguments, 1);
                return applyACosFunction(arguments.get(0), bitDepth);
            case "TAN":
                checkArgumentCount(functionName, arguments, 1);
                return applyTanFunction(arguments.get(0), bitDepth);
            case "TANH":
                checkArgumentCount(functionName, arguments, 1);
                return applyTanHFunction(arguments.get(0), bitDepth);
            case "ATAN2":
                checkArgumentCount(functionName, arguments, 2);
                return applyATan2Function(arguments.get(0), arguments.get(1), bitDepth);
            case "SLICE_MIN":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceMinFunction(arguments.get(0), bitDepth);
            case "SLICE_MAX":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceMaxFunction(arguments.get(0), bitDepth);
            case "SLICE_MEAN":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceMeanFunction(arguments.get(0), bitDepth);
            case "SLICE_MEDIAN":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceMedianFunction(arguments.get(0), bitDepth);
            case "SLICE_STDDEV":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceStdDevFunction(arguments.get(0), bitDepth);
            case "SLICE_SKEWNESS":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceSkewnessFunction(arguments.get(0), bitDepth);
            case "SLICE_KURTOSIS":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceKurtosisFunction(arguments.get(0), bitDepth);
            case "SLICE_MODE":
                checkArgumentCount(functionName, arguments, 1);
                return applySliceModeFunction(arguments.get(0), bitDepth);
            default:
                throw new IllegalArgumentException("Unknown function: " + functionName);
        }
    }

    private Object applySliceModeFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStats().mode;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceKurtosisFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStatistics().kurtosis;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceSkewnessFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStatistics().skewness;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceStdDevFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStats().stdDev;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceMedianFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStatistics().median;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceMeanFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStats().mean;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceMaxFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStats().max;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private Object applySliceMinFunction(Object o, int bitDepth) {
        if(o instanceof ImageProcessor) {
            return ((ImageProcessor) o).getStats().min;
        }
        else if(o instanceof Number) {
            return o;
        }
        else {
            throw new UnsupportedOperationException("Unsupported argument: " + o);
        }
    }

    private void checkArgumentCount(String functionName, List<Object> arguments, int numArguments) {
        if (arguments.size() != numArguments) {
            throw new IllegalArgumentException("The function " + functionName + " accepts exactly " + numArguments + " arguments!");
        }
    }

    private Object applyMinFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return Math.min(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            float o1_ = ((Number) o1).floatValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.min(o1_, o2_.getf(i)), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            float o2_ = ((Number) o2).floatValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.min(o1_.getf(i), o2_), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.min(o1_.getf(i), o2_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyMaxFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return Math.max(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            float o1_ = ((Number) o1).floatValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.max(o1_, o2_.getf(i)), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            float o2_ = ((Number) o2).floatValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.max(o1_.getf(i), o2_), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.max(o1_.getf(i), o2_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyIfElseFunction(Object o1, Object o2, Object o3, int bitDepth) {

        if (o1 instanceof Number && o2 instanceof Number) {
            return ((Number) o1).doubleValue() > 0 ? o2 : o3;
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            return ((Number) o1).doubleValue() > 0 ? o2 : o3;
        } else if (o1 instanceof ImageProcessor && o2 instanceof Number && o3 instanceof Number) {
            float o2_ = ((Number) o2).floatValue();
            float o3_ = ((Number) o3).floatValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_.getf(i) > 0 ? o2_ : o3_, bitDepth);
            }
            return result;
        } else if (o1 instanceof ImageProcessor && o2 instanceof Number && o3 instanceof ImageProcessor) {
            float o2_ = ((Number) o2).floatValue();
            ImageProcessor o3_ = ((ImageProcessor) o3);
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_.getf(i) > 0 ? o2_ : o3_.getf(i), bitDepth);
            }
            return result;
        } else if (o1 instanceof ImageProcessor && o2 instanceof ImageProcessor && o3 instanceof Number) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            float o3_ = ((Number) o3).floatValue();
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_.getf(i) > 0 ? o2_.getf(i) : o3_, bitDepth);
            }
            return result;
        } else if (o1 instanceof ImageProcessor && o2 instanceof ImageProcessor && o3 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor o3_ = ((ImageProcessor) o3);
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_.getf(i) > 0 ? o2_.getf(i) : o3_.getf(i), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2 + " " + o3);
        }
    }

    private void setIpValueSafe(ImageProcessor target, int index, float value, int bitDepth) {
        if (bitDepth == 8) {
            if (value < 0) {
                value = 0;
            }
            if (value > 255) {
                value = 255;
            }
        } else if (bitDepth == 16) {
            if (value < 0) {
                value = 0;
            }
            if (value > 65535) {
                value = 65535;
            }
        }
        target.setf(index, value);
    }

    private Object applyPowFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return Math.pow(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            float o1_ = ((Number) o1).floatValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (float) Math.pow(o1_, o2_.getf(i)), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            float o2_ = ((Number) o2).floatValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (float) Math.pow(o1_.getf(i), o2_), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (float) Math.pow(o1_.getf(i), o2_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyGammaFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            double v1 = ((Number) o1).doubleValue();
            double c = ((Number) o2).doubleValue();
            if (v1 > 0) {
                return Math.exp(c * Math.log(v1));
            } else {
                return 0;
            }
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            float o1_ = ((Number) o1).floatValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                double v1 = o1_;
                double c = o2_.getf(i);
                if (v1 > 0) {
                    setIpValueSafe(result, i, (float) Math.exp(c * Math.log(v1)), bitDepth);
                }
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            float o2_ = ((Number) o2).floatValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                double v1 = o1_.getf(i);
                double c = o2_;
                if (v1 > 0) {
                    setIpValueSafe(result, i, (float) Math.exp(c * Math.log(v1)), bitDepth);
                }
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                double v1 = ((ImageProcessor) o1).getf(i);
                double c = o2_.getf(i);
                if (v1 > 0) {
                    setIpValueSafe(result, i, (float) Math.exp(c * Math.log(v1)), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyATan2Function(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            double v1 = ((Number) o1).doubleValue();
            double c = ((Number) o2).doubleValue();
            if (v1 > 0) {
                return Math.atan2(v1, c);
            } else {
                return 0;
            }
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            float o1_ = ((Number) o1).floatValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                double v1 = o1_;
                double c = o2_.getf(i);
                if (v1 > 0) {
                    setIpValueSafe(result, i, (float) Math.atan2(v1, c), bitDepth);
                }
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            float o2_ = ((Number) o2).floatValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                double v1 = o1_.getf(i);
                double c = o2_;
                if (v1 > 0) {
                    setIpValueSafe(result, i, (float) Math.atan2(v1, c), bitDepth);
                }
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                double v1 = ((ImageProcessor) o1).getf(i);
                double c = o2_.getf(i);
                if (v1 > 0) {
                    setIpValueSafe(result, i, (float) Math.atan2(v1, c), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyFloorFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.floor(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.floor(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyCeilFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.ceil(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.ceil(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyRoundFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.round(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.round(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applySignumFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.signum(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.signum(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applySinFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.sin(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.sin(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applySinHFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.sinh(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.sinh(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyASinFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.asin(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.asin(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyCosFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.cos(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.cos(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyCosHFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.cosh(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.cosh(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyACosFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.acos(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.acos(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyTanFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.tan(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.tan(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyTanHFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.tanh(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.tanh(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applySqrtFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.sqrt(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                float v = o_.getf(i);
                if (v > 0) {
                    setIpValueSafe(result, i, (float) Math.sqrt(v), bitDepth);
                }
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyExpFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.exp(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (float) Math.exp(o_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyLnFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.log(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (float) Math.log(o_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyAbsFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return Math.abs(((Number) o).doubleValue());
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                setIpValueSafe(result, i, Math.abs(o_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyInvertFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return applyNotFunction(o, bitDepth);
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = o_.duplicate();
            result.invert();
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyModuloFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return ((Number) o1).intValue() % ((Number) o2).intValue();
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            int o1_ = ((Number) o1).intValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_ % (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            int o2_ = ((Number) o2).intValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o2_ % (int) o1_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (int) o1_.getf(i) % (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyAndFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return ((Number) o1).intValue() & ((Number) o2).intValue();
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            int o1_ = ((Number) o1).intValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_ & (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            int o2_ = ((Number) o2).intValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o2_ & (int) o1_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (int) o1_.getf(i) & (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyOrFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return ((Number) o1).intValue() | ((Number) o2).intValue();
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            int o1_ = ((Number) o1).intValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_ | (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            int o2_ = ((Number) o2).intValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o2_ | (int) o1_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (int) o1_.getf(i) | (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyXorFunction(Object o1, Object o2, int bitDepth) {
        if (o1 instanceof Number && o2 instanceof Number) {
            return ((Number) o1).intValue() ^ ((Number) o2).intValue();
        } else if (o1 instanceof Number && o2 instanceof ImageProcessor) {
            int o1_ = ((Number) o1).intValue();
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor result = ImageJUtils.createProcessor(o2_.getWidth(), o2_.getHeight(), o2_.getBitDepth());
            for (int i = 0; i < o2_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o1_ ^ (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof Number && o1 instanceof ImageProcessor) {
            int o2_ = ((Number) o2).intValue();
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, o2_ ^ (int) o1_.getf(i), bitDepth);
            }
            return result;
        } else if (o2 instanceof ImageProcessor && o1 instanceof ImageProcessor) {
            ImageProcessor o2_ = (ImageProcessor) o2;
            ImageProcessor o1_ = (ImageProcessor) o1;
            ImageProcessor result = ImageJUtils.createProcessor(o1_.getWidth(), o1_.getHeight(), o1_.getBitDepth());
            for (int i = 0; i < o1_.getPixelCount(); i++) {
                setIpValueSafe(result, i, (int) o1_.getf(i) ^ (int) o2_.getf(i), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o1 + " " + o2);
        }
    }

    private Object applyNotFunction(Object o, int bitDepth) {
        if (o instanceof Number) {
            return ~((Number) o).intValue();
        } else if (o instanceof ImageProcessor) {
            ImageProcessor o_ = (ImageProcessor) o;
            ImageProcessor result = ImageJUtils.createProcessor(o_.getWidth(), o_.getHeight(), o_.getBitDepth());
            for (int i = 0; i < o_.getPixelCount(); i++) {
                setIpValueSafe(result, i, ~(int) (o_.getf(i)), bitDepth);
            }
            return result;
        } else {
            throw new UnsupportedOperationException("Unsupported arguments: " + o);
        }
    }

    private Object applyOperation(Object left, Object right, String operator, int bitDepth) {
        if (left instanceof Number && right instanceof Number) {
            return applyOperationNumberNumber(((Number) left).floatValue(), ((Number) right).floatValue(), operator, bitDepth);
        } else if (left instanceof Number && right instanceof ImageProcessor) {
            return applyOperationNumberImage(((Number) left).floatValue(), (ImageProcessor) right, operator, bitDepth);
        } else if (left instanceof ImageProcessor && right instanceof Number) {
            return applyOperationImageNumber((ImageProcessor) left, ((Number) right).floatValue(), operator, bitDepth);
        } else if (left instanceof ImageProcessor && right instanceof ImageProcessor) {
            return applyOperationImageImage((ImageProcessor) left, (ImageProcessor) right, operator, bitDepth);
        } else {
            throw new UnsupportedOperationException("Unsupported operator types: " + left + ", " + right);
        }
    }

    private ImageProcessor applyOperationImageImage(ImageProcessor left, ImageProcessor right, String operator, int bitDepth) {
        switch (operator) {
            case "+": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left.getf(i) + right.getf(i), bitDepth);
                }
                return result;
            }
            case "-": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left.getf(i) - right.getf(i), bitDepth);
                }
                return result;
            }
            case "*": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left.getf(i) * right.getf(i), bitDepth);
                }
                return result;
            }
            case "/": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left.getf(i) / right.getf(i), bitDepth);
                }
                return result;
            }
            case "==": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) == right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case "<": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) < right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case ">": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) > right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case "<=": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) <= right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case ">=": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) >= right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            default:
                throw new UnsupportedOperationException("Unsupported operator type: " + operator);
        }
    }

    private ImageProcessor applyOperationImageNumber(ImageProcessor left, float right, String operator, int bitDepth) {
        switch (operator) {
            case "/": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left.getf(i) / right, bitDepth);
                }
                return result;
            }
            case "-": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left.getf(i) - right, bitDepth);
                }
                return result;
            }
            case "<": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) < right, bitDepth), bitDepth);
                }
                return result;
            }
            case ">": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) > right, bitDepth), bitDepth);
                }
                return result;
            }
            case "<=": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) <= right, bitDepth), bitDepth);
                }
                return result;
            }
            case ">=": {
                ImageProcessor result = ImageJUtils.createProcessor(left.getWidth(), left.getHeight(), left.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left.getf(i) >= right, bitDepth), bitDepth);
                }
                return result;
            }
            default:
                // All other operators are commutative
                return applyOperationNumberImage(right, left, operator, bitDepth);
        }
    }

    private ImageProcessor applyOperationNumberImage(float left, ImageProcessor right, String operator, int bitDepth) {
        switch (operator) {
            case "+": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left + right.getf(i), bitDepth);
                }
                return result;
            }
            case "-": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left - right.getf(i), bitDepth);
                }
                return result;
            }
            case "*": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left * right.getf(i), bitDepth);
                }
                return result;
            }
            case "/": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, left / right.getf(i), bitDepth);
                }
                return result;
            }
            case "==": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left == right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case "<": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left < right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case ">": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left > right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case "<=": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left <= right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            case ">=": {
                ImageProcessor result = ImageJUtils.createProcessor(right.getWidth(), right.getHeight(), right.getBitDepth());
                for (int i = 0; i < result.getPixelCount(); i++) {
                    setIpValueSafe(result, i, booleanToFloat(left >= right.getf(i), bitDepth), bitDepth);
                }
                return result;
            }
            default:
                throw new UnsupportedOperationException("Unsupported operator type: " + operator);
        }
    }

    private float applyOperationNumberNumber(float left, float right, String operator, int bitDepth) {
        switch (operator) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                return left / right;
            case "==":
                return booleanToFloat(left == right, bitDepth);
            case "<":
                return booleanToFloat(left < right, bitDepth);
            case ">":
                return booleanToFloat(left > right, bitDepth);
            case "<=":
                return booleanToFloat(left <= right, bitDepth);
            case ">=":
                return booleanToFloat(left >= right, bitDepth);
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator);
        }
    }

    private float booleanToFloat(boolean b, int bitDepth) {
        if (bitDepth == 8) {
            return b ? 255 : 0;
        } else if (bitDepth == 16) {
            return b ? 65535 : 0;
        } else {
            return b ? 1 : 0;
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
            "<li><code>width</code>, <code>height</code>, <code>numZ</code>, <code>numC</code>, <code>numT</code> contains the size of the input image(s)</li>" +
            "<li>You can use annotations as variables (they will be converted to numerics)</li>" +
            "<li>You can use custom variables (prefix with <code>custom.</code>)</li>" +
            "<li>Numeric constants like <code>0.15</code> can be used</li>" +
            "<li>You can use brackets <code>( )</code> to ensure the correct order</li>" +
            "<li><code>[] + []</code> to add the pixel values</li>" +
            "<li><code>[] - []</code> to subtract the pixel values</li>" +
            "<li><code>[] * []</code> to multiply the pixel values</li>" +
            "<li><code>[] / []</code> to divide the pixel values</li>" +
            "<li><code>[] == []</code> returns 255 (8-bit)/65535 (16-bit)/1 (32-bit) if the left operand is equal to the right operand</li>" +
            "<li><code>[] < []</code> returns 255 (8-bit)/65535 (16-bit)/1 (32-bit) if the left operand is less than the right operand</li>" +
            "<li><code>[] > []</code> returns 255 (8-bit)/65535 (16-bit)/1 (32-bit) if the left operand is more than the right operand</li>" +
            "<li><code>[] <= []</code> returns 255 (8-bit)/65535 (16-bit)/1 (32-bit) if the left operand is less than or equal to the right operand</li>" +
            "<li><code>[] >= []</code> returns 255 (8-bit)/65535 (16-bit)/1 (32-bit) if the left operand is more than or equal to the right operand</li>" +
            "<li><code>IF_ELSE([], [], [])</code> returns the second operand if the first operand is larger than zero. Otherwise returns the third operand</li>" +
            "<li><code>MIN([], [])</code> to calculate the minimum of the operands</li>" +
            "<li><code>MAX([], [])</code> to calculate the maximum of the operands</li>" +
            "<li><code>SQR([])</code> to calculate the square of the operand</li>" +
            "<li><code>POW([], [])</code> to calculate the power</li>" +
            "<li><code>GAMMA([], [])</code> to calculate <code>EXP(LN( [] ) * [])</code></li>" +
            "<li><code>MOD([], [])</code> to the modulo (values are converted to integers)</li>" +
            "<li><code>SQRT([])</code> to calculate the square root of the operand</li>" +
            "<li><code>EXP([])</code> to calculate the e^operand</li>" +
            "<li><code>LN([])</code> or <code>LOG([])</code> to calculate the LN(operand)</li>" +
            "<li><code>ABS([])</code> to calculate absolute value</li>" +
            "<li><code>SIGNUM([])</code> to calculate the sign (-1, 0, or 1)</li>" +
            "<li><code>FLOOR([])</code>, <code>ROUND([])</code>, <code>CEIL([])</code> to round the values</li>" +
            "<li><code>INVERT([])</code> calculates the inverted pixel value (for numbers it will return NOT(x))</li>" +
            "<li><code>AND([], [])</code>, <code>OR([], [])</code>, <code>XOR([], [])</code>, <code>NOT([])</code> to calculate bitwise logical operations. Values are automatically converted to integers to allow the operation.</li>" +
            "<li><code>SIN([])</code>, <code>SINH([])</code>, <code>ASIN([])</code>, <code>COS([])</code>, <code>COSH([])</code>, <code>ACOS([])</code>, <code>TAN([])</code>, <code>TANH([])</code>, <code>ATAN2([])</code> are supported trigonometric functions (behavior according to the Java standard library)</li>" +
            "<li><code>SLICE_MAX([])</code>, <code>SLICE_MIN([])</code>, <code>SLICE_MEAN([])</code>, <code>SLICE_MEDIAN([])</code>, <code>SLICE_STDDEV([])</code>, <code>SLICE_SKEWNESS([])</code>, <code>SLICE_KURTOSIS([])</code>, <code>SLICE_MODE([])</code> calculate the given statistic over all pixel values in the current 2D slice</li>" +
            "</ul>")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterSettings(withoutEditorButton = true, tokenMaker = FastImageArithmeticsTokenMaker.class)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }

    public static class FastImageArithmeticsTokenMaker extends JIPipeExpressionEvaluatorSyntaxTokenMaker {
        @Override
        public TokenMap getWordsToHighlight() {
            TokenMap tokenMap = new TokenMap();
            for (String constant : CONSTANTS) {
                tokenMap.put(constant, Token.RESERVED_WORD);
            }
            for (String function : FUNCTIONS) {
                tokenMap.put(function, Token.FUNCTION);
            }
            tokenMap.put("+", Token.OPERATOR);
            tokenMap.put("-", Token.OPERATOR);
            tokenMap.put("*", Token.OPERATOR);
            tokenMap.put("/", Token.OPERATOR);

            tokenMap.put("<", Token.OPERATOR);
            tokenMap.put(">", Token.OPERATOR);
            tokenMap.put("==", Token.OPERATOR);
            tokenMap.put("<=", Token.OPERATOR);
            tokenMap.put(">=", Token.OPERATOR);
            return tokenMap;
        }
    }
}
