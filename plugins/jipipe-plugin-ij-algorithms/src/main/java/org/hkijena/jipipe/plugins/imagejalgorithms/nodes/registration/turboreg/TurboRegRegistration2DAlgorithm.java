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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.turboreg;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.AlignedImage5DSliceIndexExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.TurboRegResult;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.TurboRegTransformationInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.TurboRegTransformationType;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.turboreg.TurboRegUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.strings.JsonData;
import org.hkijena.jipipe.utils.GraphUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Port of {@link register_virtual_stack.Register_Virtual_Stack_MT}
 */
@SetJIPipeDocumentation(name = "TurboReg registration 2D (simple)", description = "Aligns the target image to the source image using methods " +
        "implemented by TurboReg and MultiStackReg. Due to the variation in registration tasks, this node follows a 'rules' approach where each slice of the input image is matched to one rule that " +
        "determines how the slice is aligned. There are three modes: (1) register to a reference slice, (2) use the same transformation calculated for another slice, and (3) apply no registration. " +
        "Use the examples provided via the 'Tools' menu (top right of the parameter window) to get started with configuring the rules.\n\n" +
        "Handling of RGB images: as the underlying alignment methods were only designed for greyscale images, RGB slices are automatically converted to greyscale prior to registration (using ImageJ methods). " +
        "The resulting transformation is then applied per RGB channel, which is then output.\n\n" +
        "Please note that if an RGB image is present, the output will be RGB. You may lose greyscale precision (16 bit/ 32 bit) in other slices. " +
        "To prevent this, convert RGB to greyscale prior to registration (e.g. by splitting RGB channels into hyperstack channels).")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Registration")
@AddJIPipeCitation("Based on TurboReg")
@AddJIPipeCitation("Based on MultiStackReg")
@AddJIPipeCitation("https://bigwww.epfl.ch/thevenaz/turboreg/")
@AddJIPipeCitation("https://github.com/miura/MultiStackRegistration/")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "The reference image. Can have fewer slices/channels/frames than the target image", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", description = "The target image that will be registered to the reference", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Reference", description = "Copy of the reference image", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Registered", description = "The registered image", create = true)
@AddJIPipeOutputSlot(value = JsonData.class, name = "Transform", description = "The transform serialized in JSON format", create = true)
public class TurboRegRegistration2DAlgorithm extends JIPipeIteratingAlgorithm {

    private final AdvancedTurboRegParameters advancedTurboRegParameters;
    private TurboRegTransformationType transformationType = TurboRegTransformationType.RigidBody;
    private ParameterCollectionList rules = ParameterCollectionList.containingCollection(Rule.class);


    public TurboRegRegistration2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.advancedTurboRegParameters = new AdvancedTurboRegParameters();
        registerSubParameter(advancedTurboRegParameters);

        // Add default config
        rules.addNewInstance();
    }

    public TurboRegRegistration2DAlgorithm(TurboRegRegistration2DAlgorithm other) {
        super(other);
        this.transformationType = other.transformationType;
        this.rules = new ParameterCollectionList(other.rules);
        this.advancedTurboRegParameters = new AdvancedTurboRegParameters(other.advancedTurboRegParameters);
        registerSubParameter(advancedTurboRegParameters);
    }

    @SetJIPipeDocumentation(name = "Transformation", description = "The type of transformation to be used." +
            "<ul>" +
            "<li>Translation. Upon translation, a straight line is mapped to a straight line of identical orientation, with conservation of the distance between any pair of points. A single landmark in each image gives a complete description of a translation. The mapping is of the form x = u + Δu. </li>" +
            "<li>Rigid Body. Upon rigid-body transformation, the distance between any pair of points is conserved. A single landmark is necessary to describe the translational component of the rigid-body transformation, while the rotational component is given by an angle. The mapping is of the form x = { {cos θ, −sin θ}, {sin θ, cos θ} } ⋅ u + Δu.<li>" +
            "<li>Scaled rotation. Upon scaled rotation, a straight line is mapped to a straight line; moreover, the angle between any pair of lines is conserved (this is sometimes called a conformal mapping). A pair of landmarks in each image is needed to give a complete description of a scaled rotation. The mapping is of the form x = λ { {cos θ, −sin θ}, {sin θ, cos θ} } ⋅ u + Δu.<li>" +
            "<li>Affine. Upon affine transformation, a straight line is mapped to a straight line, with conservation of flat angles between lines (parallel or coincident lines remain parallel or coincident). In 2D, a simplex—three landmarks—in each image is needed to give a complete description of an affine transformation. The mapping is of the form x = { {a11, a12}, {a21, a22} } ⋅ u + Δu.<li>" +
            "<li>Bilinear. Upon bilinear transformation, a straight line is mapped to a conic section. In 2D, four landmarks in each image are needed to give a complete description of a bilinear transformation. The mapping is of the form x = { {a11, a12}, {a21, a22} } ⋅ u + b u1 u2 + Δu.<li>" +
            "<li>None. Do not apply any transformation.<li>" +
            "</ul>")
    @JIPipeParameter("transformation-type")
    public TurboRegTransformationType getTransformationType() {
        return transformationType;
    }

    @JIPipeParameter("transformation-type")
    public void setTransformationType(TurboRegTransformationType transformationType) {
        this.transformationType = transformationType;
    }

    @SetJIPipeDocumentation(name = "Rules", description = "List of rules that determine if/how slices are registered. " +
            "The list of rules is tested for each slice in given order until a matching rule is found. " +
            "The rule then determines if the slice should be aligned to the reference image, follows the transformation calculated for another input slice, " +
            "or is ignored (added to the output as-is)." +
            "If no rules matches, the slice 'Ignore' rule is automatically applied.")
    @JIPipeParameter("rules")
    @ParameterCollectionListTemplate(Rule.class)
    public ParameterCollectionList getRules() {
        return rules;
    }

    @JIPipeParameter("rules")
    public void setRules(ParameterCollectionList rules) {
        this.rules = rules;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Advanced parameters", description = "Advanced parameters for TurboReg")
    @JIPipeParameter("advanced-parameters")
    public AdvancedTurboRegParameters getAdvancedTurboRegParameters() {
        return advancedTurboRegParameters;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus target = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getDuplicateImage();
        ImagePlus source = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getDuplicateImage();

        if (transformationType == TurboRegTransformationType.GenericTransformation) {
            progressInfo.log("Transformation set to 'None'. Skipping.");
            iterationStep.addOutputData("Reference", new ImagePlusData(source), progressInfo);
            iterationStep.addOutputData("Target", new ImagePlusData(target), progressInfo);
            iterationStep.addOutputData("Transform", new JsonData(JsonUtils.toPrettyJsonString(new TurboRegTransformationInfo())), progressInfo);
            return;
        }
        if (source.getWidth() != target.getWidth() || source.getHeight() != target.getHeight()) {
            throw new RuntimeException("Source and target images do not have the same width and height!");
        }

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
        variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());

        progressInfo.log("Building transformation graph ...");

        // Build the transformation info graph
        DefaultDirectedGraph<TransformationNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        BiMap<ImageSliceIndex, TransformationNode> transformationMap = HashBiMap.create();

        // Create the vertices
        List<Rule> rules_ = rules.mapToCollection(Rule.class);
        Rule defaultRule = new Rule();
        defaultRule.ruleType = RuleType.Ignore;
        for (int c = 0; c < source.getNChannels(); c++) {
            for (int z = 0; z < source.getNSlices(); z++) {
                for (int t = 0; t < source.getNFrames(); t++) {
                    ImageSliceIndex sliceIndex = new ImageSliceIndex(c, z, t);
                    AlignedImage5DSliceIndexExpressionParameterVariablesInfo.apply(variablesMap, target, source, sliceIndex);

                    // Find matching rule
                    Rule matching = defaultRule;
                    for (Rule rule : rules_) {
                        if (rule.condition.evaluateToBoolean(variablesMap)) {
                            matching = rule;
                            break;
                        }
                    }

                    // Create node
                    TransformationNode node = new TransformationNode(sliceIndex, matching);
                    graph.addVertex(node);
                    transformationMap.put(sliceIndex, node);
                }
            }
        }

        if(progressInfo.isCancelled()) {
            return;
        }

        // Create the edges
        for (TransformationNode node : graph.vertexSet()) {
            if (node.rule.ruleType == RuleType.UseTransformation) {
                AlignedImage5DSliceIndexExpressionParameterVariablesInfo.apply(variablesMap, target, source, node.sourceIndex);
                int usedC = node.rule.referenceCIndex.evaluateToInteger(variablesMap);
                int usedZ = node.rule.referenceZIndex.evaluateToInteger(variablesMap);
                int usedT = node.rule.referenceTIndex.evaluateToInteger(variablesMap);

                // Grab from index
                ImageSliceIndex usedIndex = new ImageSliceIndex(usedC, usedZ, usedT);
                TransformationNode usedNode = transformationMap.getOrDefault(usedIndex, null);

                if (usedNode == null || usedNode == node) {
                    throw new RuntimeException("Unable to find rule for used transformation node at " + usedIndex);
                }

                // Create edge
                graph.addEdge(usedNode, node);
            }
        }

        if(progressInfo.isCancelled()) {
            return;
        }

        progressInfo.log("Transformation graph: " + graph.vertexSet().size() + " vertices, " + graph.edgeSet().size() + " edges");

        // Check the graph
        for (TransformationNode node : graph.vertexSet()) {
            if (graph.inDegreeOf(node) > 1) {
                throw new RuntimeException("Conflicting transformation source for node at " + node.sourceIndex);
            }
            if (node.rule.ruleType == RuleType.UseTransformation && graph.inDegreeOf(node) == 0) {
                throw new RuntimeException("Unresolved input for node at " + node.sourceIndex);
            }
        }
        if (GraphUtils.hasCycle(graph)) {
            throw new RuntimeException("Transformation graph has cycles");
        }

        // Iteratively resolve the graph until everything is handled
        Map<ImageSliceIndex, ImageProcessor> transformedTargetProcessors = new HashMap<>();
        TurboRegTransformationInfo transformation = new TurboRegTransformationInfo();

        TopologicalOrderIterator<TransformationNode, DefaultEdge> topologicalOrderIterator = new TopologicalOrderIterator<>(graph);
        while (topologicalOrderIterator.hasNext()) {

            if(progressInfo.isCancelled()) {
                return;
            }

            TransformationNode node = topologicalOrderIterator.next();
            ImageProcessor sourceIp = ImageJUtils.getSliceZero(source, node.sourceIndex);
            switch (node.rule.ruleType) {
                case Ignore:
                    transformedTargetProcessors.put(node.sourceIndex, sourceIp);
                    break;
                case CalculateTransformation: {
                    // First find the reference slice
                    AlignedImage5DSliceIndexExpressionParameterVariablesInfo.apply(variablesMap, target, source, node.sourceIndex);
                    int targetReferenceC = node.rule.referenceCIndex.evaluateToInteger(variablesMap);
                    int targetReferenceZ = node.rule.referenceZIndex.evaluateToInteger(variablesMap);
                    int targetReferenceT = node.rule.referenceTIndex.evaluateToInteger(variablesMap);

                    ImageSliceIndex targetIndex = ImageJUtils.toSafeZeroIndex(target, new ImageSliceIndex(targetReferenceC, targetReferenceZ, targetReferenceT));
                    progressInfo.log("Aligning input slice " + node.sourceIndex + " to reference " + targetIndex);

                    ImageProcessor targetIp = ImageJUtils.getSliceZero(target, targetIndex);

                    // Auto-convert to greyscale
                    ImagePlus sourceImp = new ImagePlus("source", sourceIp);
                    ImagePlus targetImp = new ImagePlus("target", targetIp);
                    boolean isRGB = sourceImp.getType() == ImagePlus.COLOR_RGB;

                    ImagePlus sourceImp_ = ImageJUtils.convertToGreyscaleIfNeeded(sourceImp);
                    ImagePlus targetImp_ = ImageJUtils.convertToGreyscaleIfNeeded(targetImp);

                    TurboRegResult aligned = TurboRegUtils.alignImage2D(sourceImp_,
                            targetImp_,
                            transformationType,
                            advancedTurboRegParameters);
                    TurboRegTransformationInfo.Entry transformationEntry = aligned.getTransformation().getEntries().get(0);

                    if (isRGB) {
                        // Re-do transformation (using simple method)
                        ImagePlus transformed = TurboRegUtils.transformImage2DSimple(sourceImp,
                                targetImp_.getWidth(),
                                targetImp_.getHeight(),
                                transformationType,
                                transformationEntry.getSourcePointsAsArray(),
                                transformationEntry.getTargetPointsAsArray());
                        transformedTargetProcessors.put(node.sourceIndex, transformed.getProcessor());
                    } else {
                        transformedTargetProcessors.put(node.sourceIndex, aligned.getTransformedTargetImage().getProcessor());
                    }

                    // Modify the transformation

                    transformationEntry.setSourceImageIndex(node.sourceIndex);
                    transformationEntry.setTargetImageIndex(targetIndex);
                    transformation.getEntries().add(transformationEntry);

                    // Save the transformation into the node (required for UseTransform)
                    node.transformationInfo = aligned.getTransformation();
                }
                break;
                case UseTransformation: {

                    // Grab the node's source transformation info
                    TransformationNode useSource = GraphUtils.getFirstPredecessor(graph, node);
                    Objects.requireNonNull(useSource);

                    if (useSource.transformationInfo == null) {
                        // Was set to "Ignore" -> Also ignore
                        progressInfo.log("Ignoring UseTransformation for node at " + node.sourceIndex + ": " +
                                "source at index " + useSource.sourceIndex + " has no transform (probably ignored in the chain)");
                        transformedTargetProcessors.put(node.sourceIndex, sourceIp);
                    } else {
                        progressInfo.log("Transforming node at " + node.sourceIndex + " with transformation from " + useSource.sourceIndex);

                        ImagePlus sourceImp = new ImagePlus("source", sourceIp);

                        TurboRegTransformationInfo.Entry transformEntry = useSource.transformationInfo.getEntries().get(0);
                        double[][] sourcePoints = transformEntry.getSourcePointsAsArray();
                        double[][] targetPoints = transformEntry.getTargetPointsAsArray();

                        ImagePlus transformed = TurboRegUtils.transformImage2DSimple(sourceImp,
                                sourceImp.getWidth(),
                                sourceImp.getHeight(),
                                transformationType,
                                sourcePoints,
                                targetPoints);
                        transformedTargetProcessors.put(node.sourceIndex, transformed.getProcessor());

                        // Modify the transformation
                        TurboRegTransformationInfo.Entry transformationEntry = new TurboRegTransformationInfo.Entry(transformEntry);
                        transformationEntry.setSourceImageIndex(node.sourceIndex);
                        transformation.getEntries().add(transformationEntry);

                        // Save the transformation into the node (required for UseTransform)
                        node.transformationInfo = useSource.transformationInfo;
                    }

                }
                break;
                default:
                    throw new UnsupportedOperationException("Unsupported rule type: " + node.rule.ruleType);
            }
        }

        ImagePlus result = ImageJUtils.mergeMappedSlices(transformedTargetProcessors);
        result.copyScale(target);
        iterationStep.addOutputData("Reference", new ImagePlusData(target), progressInfo);
        iterationStep.addOutputData("Registered", new ImagePlusData(result), progressInfo);
        iterationStep.addOutputData("Transform", new JsonData(JsonUtils.toPrettyJsonString(transformation)), progressInfo);
    }

    public enum RuleType {
        Ignore,
        CalculateTransformation,
        UseTransformation
    }

    private static class TransformationNode {
        private final ImageSliceIndex sourceIndex;
        private final Rule rule;
        private TurboRegTransformationInfo transformationInfo;

        private TransformationNode(ImageSliceIndex sourceIndex, Rule rule) {
            this.sourceIndex = sourceIndex;
            this.rule = rule;
        }

        public Rule getRule() {
            return rule;
        }

        public ImageSliceIndex getSourceIndex() {
            return sourceIndex;
        }

        public TurboRegTransformationInfo getTransformationInfo() {
            return transformationInfo;
        }

        public void setTransformationInfo(TurboRegTransformationInfo transformationInfo) {
            this.transformationInfo = transformationInfo;
        }
    }

    public static class Rule extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter condition = new JIPipeExpressionParameter("true");
        private JIPipeExpressionParameter referenceCIndex = new JIPipeExpressionParameter("c");
        private JIPipeExpressionParameter referenceZIndex = new JIPipeExpressionParameter("z");
        private JIPipeExpressionParameter referenceTIndex = new JIPipeExpressionParameter("t");
        private RuleType ruleType = RuleType.CalculateTransformation;

        public Rule() {
        }

        public Rule(Rule other) {
            this.condition = other.condition;
            this.referenceCIndex = new JIPipeExpressionParameter(other.referenceCIndex);
            this.referenceZIndex = new JIPipeExpressionParameter(other.referenceZIndex);
            this.referenceTIndex = new JIPipeExpressionParameter(other.referenceTIndex);
            this.ruleType = other.ruleType;
        }

        @SetJIPipeDocumentation(name = "Match if ...", description = "Determines whether this condition matches")
        @JIPipeParameter(value = "condition", uiOrder = -99)
        public JIPipeExpressionParameter getCondition() {
            return condition;
        }

        @JIPipeParameter("condition")
        public void setCondition(JIPipeExpressionParameter condition) {
            this.condition = condition;
        }

        @SetJIPipeDocumentation(name = "Reference channel index", description = "Depends from which slice the transformation is sourced.<br/>" +
                "For type 'Calculate transformation' this refers to a Z/C/T slice in the reference image.<br/>" +
                "For type 'Use transformation' this refers to a Z/C/T slice that already has a transformation.<br/>" +
                "Please note unless you only have 'Ignore' type rules there must be some alignment transformation that needs to be calculated.")
        @JIPipeParameter("reference-c-index")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = AlignedImage5DSliceIndexExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterSettings(hint = "per input Z/C/T slice")
        public JIPipeExpressionParameter getReferenceCIndex() {
            return referenceCIndex;
        }

        @JIPipeParameter("reference-c-index")
        public void setReferenceCIndex(JIPipeExpressionParameter referenceCIndex) {
            this.referenceCIndex = referenceCIndex;
        }

        @SetJIPipeDocumentation(name = "Reference frame index", description = "Depends from which slice the transformation is sourced.<br/>" +
                "For type 'Calculate transformation' this refers to a Z/C/T slice in the reference image.<br/>" +
                "For type 'Use transformation' this refers to a Z/C/T slice that already has a transformation.<br/>" +
                "Please note unless you only have 'Ignore' type rules there must be some alignment transformation that needs to be calculated.")
        @JIPipeParameter("reference-t-index")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = AlignedImage5DSliceIndexExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterSettings(hint = "per input Z/C/T slice")
        public JIPipeExpressionParameter getReferenceTIndex() {
            return referenceTIndex;
        }

        @JIPipeParameter("reference-t-index")
        public void setReferenceTIndex(JIPipeExpressionParameter referenceTIndex) {
            this.referenceTIndex = referenceTIndex;
        }

        @SetJIPipeDocumentation(name = "Reference Z index", description = "Depends from which slice the transformation is sourced.<br/>" +
                "For type 'Calculate transformation' this refers to a Z/C/T slice in the reference image.<br/>" +
                "For type 'Use transformation' this refers to a Z/C/T slice that already has a transformation.<br/>" +
                "Please note unless you only have 'Ignore' type rules there must be some alignment transformation that needs to be calculated.")
        @JIPipeParameter("reference-z-index")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = AlignedImage5DSliceIndexExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterSettings(hint = "per input Z/C/T slice")
        public JIPipeExpressionParameter getReferenceZIndex() {
            return referenceZIndex;
        }

        @JIPipeParameter("reference-z-index")
        public void setReferenceZIndex(JIPipeExpressionParameter referenceZIndex) {
            this.referenceZIndex = referenceZIndex;
        }

        @SetJIPipeDocumentation(name = "Type", description = "Determines if the slices matches by this rule are ignored, are aligned to the reference, " +
                "or follow a pre-calculated transformation")
        @JIPipeParameter("rule-type")
        public RuleType getRuleType() {
            return ruleType;
        }

        @JIPipeParameter("rule-type")
        public void setRuleType(RuleType ruleType) {
            this.ruleType = ruleType;
        }
    }

}
