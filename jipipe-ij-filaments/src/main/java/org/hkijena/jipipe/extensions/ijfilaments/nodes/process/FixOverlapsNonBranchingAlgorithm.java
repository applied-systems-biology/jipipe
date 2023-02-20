package org.hkijena.jipipe.extensions.ijfilaments.nodes.process;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdgeVariableSource;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentUnconnectedEdgeVariableSource;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageDimensions;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Fix overlapping filaments (non-branching)", description = "Algorithm that attempts to fix filaments that are merged together by junctions. " +
        "Please note that this operation assumes that all filaments are non-branching.")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", optional = true, autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class FixOverlapsNonBranchingAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean enforceSameComponent = true;

    private boolean ensureNoPathExists = true;

    private boolean connectAcrossC = false;

    private boolean connectAcrossT = false;

    private boolean excludeExistingEndpoints = true;
    private boolean enable3D = true;

    private DefaultExpressionParameter filterFunction = new DefaultExpressionParameter("length < 50");

    private DefaultExpressionParameter scoringFunction = new DefaultExpressionParameter("default");

    private final CustomExpressionVariablesParameter customExpressionVariables;

    private OptionalColorParameter newEdgeColor = new OptionalColorParameter(Color.GREEN, true);

    private boolean enforceEdgesWithinMask = true;

    public FixOverlapsNonBranchingAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
    }

    public FixOverlapsNonBranchingAlgorithm(FixOverlapsNonBranchingAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.enforceSameComponent = other.enforceSameComponent;
        this.ensureNoPathExists = other.ensureNoPathExists;
        this.connectAcrossC = other.connectAcrossC;
        this.connectAcrossT = other.connectAcrossT;
        this.enable3D = other.enable3D;
        this.excludeExistingEndpoints = other.excludeExistingEndpoints;
        this.scoringFunction = new DefaultExpressionParameter(other.scoringFunction);
        this.filterFunction = new DefaultExpressionParameter(other.filterFunction);
        this.newEdgeColor = new OptionalColorParameter(other.newEdgeColor);
        this.enforceEdgesWithinMask = other.enforceEdgesWithinMask;
    }

    @JIPipeDocumentation(name = "Prevent edges outside mask", description = "If enabled and a mask is available, check if edges crosses outside the mask boundaries and exclude those from being marked as candidate edge")
    @JIPipeParameter("enforce-edges-within-mask")
    public boolean isEnforceEdgesWithinMask() {
        return enforceEdgesWithinMask;
    }

    @JIPipeParameter("enforce-edges-within-mask")
    public void setEnforceEdgesWithinMask(boolean enforceEdgesWithinMask) {
        this.enforceEdgesWithinMask = enforceEdgesWithinMask;
    }

    @JIPipeDocumentation(name = "Color new edges", description = "Allows to color newly made edges")
    @JIPipeParameter("new-edge-color")
    public OptionalColorParameter getNewEdgeColor() {
        return newEdgeColor;
    }

    @JIPipeParameter("new-edge-color")
    public void setNewEdgeColor(OptionalColorParameter newEdgeColor) {
        this.newEdgeColor = newEdgeColor;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }

    @JIPipeDocumentation(name = "Candidate edge filter", description = "Filter expression that determines if an edge is considered as candidate")
    @JIPipeParameter("filter-function")
    @ExpressionParameterSettings(hint = "per candidate edge")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = FilamentUnconnectedEdgeVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "source.direction", name = "Source direction", description = "Vector that contains the direction of the source vertex")
    @ExpressionParameterSettingsVariable(key = "target.direction", name = "Target direction", description = "Vector that contains the direction of the target vertex")
    @ExpressionParameterSettingsVariable(key = "angle", name = "Angle", description = "The angle between the source and target directions")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    public DefaultExpressionParameter getFilterFunction() {
        return filterFunction;
    }

    @JIPipeParameter("filter-function")
    public void setFilterFunction(DefaultExpressionParameter filterFunction) {
        this.filterFunction = filterFunction;
    }

    @JIPipeDocumentation(name = "Candidate edge scoring", description = "Expression that calculates the score of a candidate edge. The higher the score, the more likely the edge is chosen. The default score is the negative of the " +
            "dot product of the two normalized directions.")
    @JIPipeParameter("scoring-function")
    @ExpressionParameterSettings(hint = "per candidate edge")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = FilamentUnconnectedEdgeVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "source.direction", name = "Source direction", description = "Vector that contains the direction of the source vertex")
    @ExpressionParameterSettingsVariable(key = "target.direction", name = "Target direction", description = "Vector that contains the direction of the target vertex")
    @ExpressionParameterSettingsVariable(key = "angle", name = "Angle", description = "The angle between the source and target directions")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    public DefaultExpressionParameter getScoringFunction() {
        return scoringFunction;
    }

    @JIPipeParameter("scoring-function")
    public void setScoringFunction(DefaultExpressionParameter scoringFunction) {
        this.scoringFunction = scoringFunction;
    }

    @JIPipeDocumentation(name = "Exclude existing endpoints", description = "If enabled, existing endpoints are excluded from being new sources or targets.")
    @JIPipeParameter("exclude-existing-endpoints")
    public boolean isExcludeExistingEndpoints() {
        return excludeExistingEndpoints;
    }

    @JIPipeParameter("exclude-existing-endpoints")
    public void setExcludeExistingEndpoints(boolean excludeExistingEndpoints) {
        this.excludeExistingEndpoints = excludeExistingEndpoints;
    }

    @JIPipeDocumentation(name = "Prevent cross-object edges", description = "If enabled, new edges will never be created across two different objects. Disable this option if there are broken filaments in the input graph.")
    @JIPipeParameter("enforce-same-component")
    public boolean isEnforceSameComponent() {
        return enforceSameComponent;
    }

    @JIPipeParameter("enforce-same-component")
    public void setEnforceSameComponent(boolean enforceSameComponent) {
        this.enforceSameComponent = enforceSameComponent;
    }

    @JIPipeDocumentation(name = "Prevent duplicate paths", description = "If enabled, new edges will only be creates if there is not already an existing path between two end points.")
    @JIPipeParameter("ensure-no-path-exists")
    public boolean isEnsureNoPathExists() {
        return ensureNoPathExists;
    }

    @JIPipeParameter("ensure-no-path-exists")
    public void setEnsureNoPathExists(boolean ensureNoPathExists) {
        this.ensureNoPathExists = ensureNoPathExists;
    }

    @JIPipeDocumentation(name = "Connect across channels", description = "If enabled, the algorithm considers also endpoints between different channel planes.")
    @JIPipeParameter("connect-across-c")
    public boolean isConnectAcrossC() {
        return connectAcrossC;
    }

    @JIPipeParameter("connect-across-c")
    public void setConnectAcrossC(boolean connectAcrossC) {
        this.connectAcrossC = connectAcrossC;
    }

    @JIPipeDocumentation(name = "Connect across frames", description = "If enabled, the algorithm considers also endpoints between different frame planes.")
    @JIPipeParameter("connect-across-t")
    public boolean isConnectAcrossT() {
        return connectAcrossT;
    }

    @JIPipeParameter("connect-across-t")
    public void setConnectAcrossT(boolean connectAcrossT) {
        this.connectAcrossT = connectAcrossT;
    }

    @JIPipeDocumentation(name = "Connect in 3D", description = "If enabled, the algorithm will look for endpoints in other Z planes.")
    @JIPipeParameter("enable-3d")
    public boolean isEnable3D() {
        return enable3D;
    }

    @JIPipeParameter("enable-3d")
    public void setEnable3D(boolean enable3D) {
        this.enable3D = enable3D;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData("Input", FilamentsData.class, progressInfo);
        FilamentsData outputData = new FilamentsData(inputData);

        ImagePlus mask;
        if(enforceEdgesWithinMask) {
            ImagePlusGreyscaleMaskData maskData = dataBatch.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo);
            if(maskData != null) {
                mask = maskData.getImage();
            }
            else {
                mask = null;
            }
        }
        else {
            mask = null;
        }

        Map<FilamentVertex, Integer> components = outputData.findComponentIds();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom", true, "custom");

        // Find the existing endpoints
        Set<FilamentVertex> existingEndpoints = outputData.vertexSet().stream().filter(vertex -> outputData.degreeOf(vertex) == 1).collect(Collectors.toSet());

        // Remove all junction nodes (degree > 2)
        outputData.removeVertexIf(vertex -> outputData.degreeOf(vertex) > 2);

        // Detect candidates to be connected (degree == 1)
        Set<FilamentVertex> candidateEndpoints = outputData.vertexSet().stream().filter(vertex -> outputData.degreeOf(vertex) == 1).collect(Collectors.toSet());
        if(excludeExistingEndpoints) {
            candidateEndpoints.removeAll(existingEndpoints);
        }

        // For each candidate: search for best matching vertex within radius that is within the same original component
        // The other candidate should also not be connected to the current candidate
        // Score: abs(scalar product of normalized vectors)
        Set<EdgeCandidate> candidates = new TreeSet<>();
        ConnectivityInspector<FilamentVertex, FilamentEdge> outputDataInspector = new ConnectivityInspector<>(outputData);
        for (FilamentVertex current : candidateEndpoints) {

            Vector3d currentV1 = current.getCentroid().toVector3d();
            Vector3d currentV2 = Graphs.neighborSetOf(outputData, current).iterator().next().getCentroid().toVector3d();
            if(!enable3D) {
                currentV1.z = 0;
                currentV2.z = 0;
            }
            Vector3d currentDirection = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
            currentDirection.normalize();

            outer:
            for (FilamentVertex other : candidateEndpoints) {
                // TODO: Auto-detect radius (based on connections to removed edges?) -> could be imprecise
                if(other != current) {
                    if(!enable3D && !current.getCentroid().sameZ(other.getCentroid())) {
                        continue;
                    }
                    if(!connectAcrossC && !current.getCentroid().sameC(other.getCentroid())) {
                        continue;
                    }
                    if(!connectAcrossT && !current.getCentroid().sameT(other.getCentroid())) {
                        continue;
                    }
                    if(enforceSameComponent && !Objects.equals(components.get(current), components.get(other))) {
                        continue;
                    }
                    if(ensureNoPathExists && outputDataInspector.pathExists(current, other)) {
                        continue;
                    }
                    if(mask != null) {
                        Vector3d difference = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
                        double length = difference.length();
                        int nSteps = (int) length;
                        Vector3d step = new Vector3d(difference);
                        step.scale(1.0 / nSteps);
                        for (int i = 1; i < nSteps; i++) {
                            Vector3d testLocation = new Vector3d(currentV1.x + i * step.x,
                                    currentV1.y + i * step.y,
                                    currentV1.z + i * step.z);
                            int x = Math.max(0, Math.min (mask.getWidth() - 1, (int)Math.round(testLocation.x)));
                            int y = (int)Math.round(testLocation.y);
                            int z = Math.max(0, (int)Math.round(testLocation.z));

                            ImageProcessor ip = ImageJUtils.getSliceZero(mask, 0, z, 0);
                            if(ip.get(x,y) == 0) {
                                continue outer;
                            }
                        }
                    }

                    // Calculate other direction
                    Vector3d otherV1 = other.getCentroid().toVector3d();
                    Vector3d otherV2 = Graphs.neighborSetOf(outputData, other).iterator().next().getCentroid().toVector3d();
                    if(!enable3D) {
                        otherV1.z = 0;
                        otherV2.z = 0;
                    }
                    Vector3d otherDirection = new Vector3d(otherV2.x - otherV1.x, otherV2.y - otherV1.y, otherV2.z - otherV1.z);
                    otherDirection.normalize();

                    // Write variables
                    FilamentUnconnectedEdgeVariableSource.writeToVariables(outputData, current, other, variables, "");
                    variables.set("angle", Math.toDegrees(currentDirection.angle(otherDirection)));

                    // Check via filter
                    if(!filterFunction.test(variables)) {
                        continue;
                    }

                    // Idea is: opposite directions -> scalar product is -1.
                    double score = -currentDirection.dot(otherDirection);
                    variables.set("default", score);
                    score = scoringFunction.evaluateToDouble(variables);

                    // Add as candidate
                    EdgeCandidate candidate = new EdgeCandidate(current, other, score);
                    candidates.add(candidate);

                }
            }
        }

        // Apply connect
        int successes = 0;
        Set<FilamentVertex> processedCandidateVertices = new HashSet<>();
        for (EdgeCandidate candidate : candidates) {
            if(!processedCandidateVertices.contains(candidate.source) && !processedCandidateVertices.contains(candidate.target)) {
                // Connect
                FilamentEdge edge = outputData.addEdge(candidate.source, candidate.target);
                if(newEdgeColor.isEnabled())
                    edge.setColor(newEdgeColor.getContent());

                // Mark as used
                processedCandidateVertices.add(candidate.source);
                processedCandidateVertices.add(candidate.target);
                ++successes;
            }
        }

        progressInfo.log("Successfully created " + successes + " edges.");

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    public static class EdgeCandidate implements Comparable<EdgeCandidate> {
        private final FilamentVertex source;
        private final FilamentVertex target;
        private final double score;

        public EdgeCandidate(FilamentVertex source, FilamentVertex target, double score) {
            this.source = source;
            this.target = target;
            this.score = score;
        }

        public FilamentVertex getSource() {
            return source;
        }

        public FilamentVertex getTarget() {
            return target;
        }

        public double getScore() {
            return score;
        }

        @Override
        public int compareTo(@NotNull FixOverlapsNonBranchingAlgorithm.EdgeCandidate o) {
            return -Double.compare(score, o.score);
        }
    }

}
