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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.process;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentUnconnectedEdgeVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Fix overlapping filaments (non-branching)", description = "Algorithm that attempts to fix filaments that are merged together by junctions. " +
        "Please note that this operation assumes that all filaments are non-branching.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Mask", optional = true, create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class FixOverlapsNonBranchingAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean enforceSameComponent = true;
    private boolean ensureNoPathExists = true;
    private boolean connectAcrossC = false;
    private boolean connectAcrossT = false;
    private boolean excludeExistingEndpoints = true;
    private boolean enable3D = true;
    private JIPipeExpressionParameter filterFunction = new JIPipeExpressionParameter("length < 50");
    private JIPipeExpressionParameter scoringFunction = new JIPipeExpressionParameter("default");
    private OptionalColorParameter newEdgeColor = new OptionalColorParameter(Color.GREEN, true);

    private boolean enforceEdgesWithinMask = true;

    public FixOverlapsNonBranchingAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FixOverlapsNonBranchingAlgorithm(FixOverlapsNonBranchingAlgorithm other) {
        super(other);
        this.enforceSameComponent = other.enforceSameComponent;
        this.ensureNoPathExists = other.ensureNoPathExists;
        this.connectAcrossC = other.connectAcrossC;
        this.connectAcrossT = other.connectAcrossT;
        this.enable3D = other.enable3D;
        this.excludeExistingEndpoints = other.excludeExistingEndpoints;
        this.scoringFunction = new JIPipeExpressionParameter(other.scoringFunction);
        this.filterFunction = new JIPipeExpressionParameter(other.filterFunction);
        this.newEdgeColor = new OptionalColorParameter(other.newEdgeColor);
        this.enforceEdgesWithinMask = other.enforceEdgesWithinMask;
    }

    @SetJIPipeDocumentation(name = "Prevent edges outside mask", description = "If enabled and a mask is available, check if edges crosses outside the mask boundaries and exclude those from being marked as candidate edge")
    @JIPipeParameter("enforce-edges-within-mask")
    public boolean isEnforceEdgesWithinMask() {
        return enforceEdgesWithinMask;
    }

    @JIPipeParameter("enforce-edges-within-mask")
    public void setEnforceEdgesWithinMask(boolean enforceEdgesWithinMask) {
        this.enforceEdgesWithinMask = enforceEdgesWithinMask;
    }

    @SetJIPipeDocumentation(name = "Color new edges", description = "Allows to color newly made edges")
    @JIPipeParameter("new-edge-color")
    public OptionalColorParameter getNewEdgeColor() {
        return newEdgeColor;
    }

    @JIPipeParameter("new-edge-color")
    public void setNewEdgeColor(OptionalColorParameter newEdgeColor) {
        this.newEdgeColor = newEdgeColor;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Candidate edge filter", description = "Filter expression that determines if an edge is considered as candidate")
    @JIPipeParameter("filter-function")
    @JIPipeExpressionParameterSettings(hint = "per candidate edge")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = FilamentUnconnectedEdgeVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "source.direction", name = "Source direction", description = "Vector that contains the direction of the source vertex")
    @JIPipeExpressionParameterVariable(key = "target.direction", name = "Target direction", description = "Vector that contains the direction of the target vertex")
    @JIPipeExpressionParameterVariable(key = "angle", name = "Angle (degrees)", description = "The angle between the source and target directions")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getFilterFunction() {
        return filterFunction;
    }

    @JIPipeParameter("filter-function")
    public void setFilterFunction(JIPipeExpressionParameter filterFunction) {
        this.filterFunction = filterFunction;
    }

    @SetJIPipeDocumentation(name = "Candidate edge scoring", description = "Expression that calculates the score of a candidate edge. The higher the score, the more likely the edge is chosen. The default score is the negative of the " +
            "dot product of the two normalized directions.")
    @JIPipeParameter("scoring-function")
    @JIPipeExpressionParameterSettings(hint = "per candidate edge")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = FilamentUnconnectedEdgeVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "source.direction", name = "Source direction", description = "Vector that contains the direction of the source vertex")
    @JIPipeExpressionParameterVariable(key = "target.direction", name = "Target direction", description = "Vector that contains the direction of the target vertex")
    @JIPipeExpressionParameterVariable(key = "angle", name = "Angle (degrees)", description = "The angle between the source and target directions")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getScoringFunction() {
        return scoringFunction;
    }

    @JIPipeParameter("scoring-function")
    public void setScoringFunction(JIPipeExpressionParameter scoringFunction) {
        this.scoringFunction = scoringFunction;
    }

    @SetJIPipeDocumentation(name = "Exclude existing endpoints", description = "If enabled, existing endpoints are excluded from being new sources or targets.")
    @JIPipeParameter("exclude-existing-endpoints")
    public boolean isExcludeExistingEndpoints() {
        return excludeExistingEndpoints;
    }

    @JIPipeParameter("exclude-existing-endpoints")
    public void setExcludeExistingEndpoints(boolean excludeExistingEndpoints) {
        this.excludeExistingEndpoints = excludeExistingEndpoints;
    }

    @SetJIPipeDocumentation(name = "Prevent cross-object edges", description = "If enabled, new edges will never be created across two different objects. Disable this option if there are broken filaments in the input graph.")
    @JIPipeParameter("enforce-same-component")
    public boolean isEnforceSameComponent() {
        return enforceSameComponent;
    }

    @JIPipeParameter("enforce-same-component")
    public void setEnforceSameComponent(boolean enforceSameComponent) {
        this.enforceSameComponent = enforceSameComponent;
    }

    @SetJIPipeDocumentation(name = "Prevent duplicate paths", description = "If enabled, new edges will only be creates if there is not already an existing path between two end points.")
    @JIPipeParameter("ensure-no-path-exists")
    public boolean isEnsureNoPathExists() {
        return ensureNoPathExists;
    }

    @JIPipeParameter("ensure-no-path-exists")
    public void setEnsureNoPathExists(boolean ensureNoPathExists) {
        this.ensureNoPathExists = ensureNoPathExists;
    }

    @SetJIPipeDocumentation(name = "Connect across channels", description = "If enabled, the algorithm considers also endpoints between different channel planes.")
    @JIPipeParameter("connect-across-c")
    public boolean isConnectAcrossC() {
        return connectAcrossC;
    }

    @JIPipeParameter("connect-across-c")
    public void setConnectAcrossC(boolean connectAcrossC) {
        this.connectAcrossC = connectAcrossC;
    }

    @SetJIPipeDocumentation(name = "Connect across frames", description = "If enabled, the algorithm considers also endpoints between different frame planes.")
    @JIPipeParameter("connect-across-t")
    public boolean isConnectAcrossT() {
        return connectAcrossT;
    }

    @JIPipeParameter("connect-across-t")
    public void setConnectAcrossT(boolean connectAcrossT) {
        this.connectAcrossT = connectAcrossT;
    }

    @SetJIPipeDocumentation(name = "Connect in 3D", description = "If enabled, the algorithm will look for endpoints in other Z planes.")
    @JIPipeParameter("enable-3d")
    public boolean isEnable3D() {
        return enable3D;
    }

    @JIPipeParameter("enable-3d")
    public void setEnable3D(boolean enable3D) {
        this.enable3D = enable3D;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData("Input", Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        ImagePlus mask;
        if (enforceEdgesWithinMask) {
            ImagePlusGreyscaleMaskData maskData = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo);
            if (maskData != null) {
                mask = maskData.getImage();
            } else {
                mask = null;
            }
        } else {
            mask = null;
        }

        Map<FilamentVertex, Integer> components = outputData.findComponentIds();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        // Find the existing endpoints
        Set<FilamentVertex> existingEndpoints = outputData.vertexSet().stream().filter(vertex -> outputData.degreeOf(vertex) == 1).collect(Collectors.toSet());

        // Remove all junction nodes (degree > 2)
        outputData.removeVertexIf(vertex -> outputData.degreeOf(vertex) > 2);

        // Detect candidates to be connected (degree == 1)
        Set<FilamentVertex> candidateEndpoints = outputData.vertexSet().stream().filter(vertex -> outputData.degreeOf(vertex) == 1).collect(Collectors.toSet());
        if (excludeExistingEndpoints) {
            candidateEndpoints.removeAll(existingEndpoints);
        }

        // For each candidate: search for best matching vertex within radius that is within the same original component
        // The other candidate should also not be connected to the current candidate
        // Score: abs(scalar product of normalized vectors)
        Set<EdgeCandidate> candidates = new HashSet<>();
        ConnectivityInspector<FilamentVertex, FilamentEdge> outputDataInspector = new ConnectivityInspector<>(outputData);
        for (FilamentVertex current : candidateEndpoints) {

            Vector3d currentV1 = current.getSpatialLocation().toSciJavaVector3d();
            Vector3d currentV2 = Graphs.neighborSetOf(outputData, current).iterator().next().getSpatialLocation().toSciJavaVector3d();
            if (!enable3D) {
                currentV1.z = 0;
                currentV2.z = 0;
            }
            Vector3d currentDirection = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
            currentDirection.normalize();

            outer:
            for (FilamentVertex other : candidateEndpoints) {
                // TODO: Auto-detect radius (based on connections to removed edges?) -> could be imprecise
                if (other != current) {
                    if (!enable3D && current.getSpatialLocation().getZ() != other.getSpatialLocation().getZ()) {
                        continue;
                    }
                    if (!connectAcrossC && current.getNonSpatialLocation().getChannel() != other.getNonSpatialLocation().getChannel()) {
                        continue;
                    }
                    if (!connectAcrossT && current.getNonSpatialLocation().getFrame() != other.getNonSpatialLocation().getFrame()) {
                        continue;
                    }
                    if (enforceSameComponent && !Objects.equals(components.get(current), components.get(other))) {
                        continue;
                    }
                    if (ensureNoPathExists && outputDataInspector.pathExists(current, other)) {
                        continue;
                    }
                    if (mask != null) {
                        Vector3d difference = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
                        double length = difference.length();
                        int nSteps = (int) length;
                        Vector3d step = new Vector3d(difference);
                        step.scale(1.0 / nSteps);
                        for (int i = 1; i < nSteps; i++) {
                            Vector3d testLocation = new Vector3d(currentV1.x + i * step.x,
                                    currentV1.y + i * step.y,
                                    currentV1.z + i * step.z);
                            int x = Math.max(0, Math.min(mask.getWidth() - 1, (int) Math.round(testLocation.x)));
                            int y = (int) Math.round(testLocation.y);
                            int z = Math.max(0, (int) Math.round(testLocation.z));

                            ImageProcessor ip = ImageJUtils.getSliceZero(mask, 0, z, 0);
                            if (ip.get(x, y) == 0) {
                                continue outer;
                            }
                        }
                    }

                    // Calculate other direction
                    Vector3d otherV1 = other.getSpatialLocation().toSciJavaVector3d();
                    Vector3d otherV2 = Graphs.neighborSetOf(outputData, other).iterator().next().getSpatialLocation().toSciJavaVector3d();
                    if (!enable3D) {
                        otherV1.z = 0;
                        otherV2.z = 0;
                    }
                    Vector3d otherDirection = new Vector3d(otherV2.x - otherV1.x, otherV2.y - otherV1.y, otherV2.z - otherV1.z);
                    otherDirection.normalize();

                    // Write variables
                    FilamentUnconnectedEdgeVariablesInfo.writeToVariables(outputData, current, other, variables, "");
                    variables.set("angle", Math.toDegrees(currentDirection.angle(otherDirection)));

                    // Check via filter
                    if (!filterFunction.test(variables)) {
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
        for (EdgeCandidate candidate : candidates.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {
            if (!processedCandidateVertices.contains(candidate.source) && !processedCandidateVertices.contains(candidate.target)) {
                // Connect
                FilamentEdge edge = outputData.addEdge(candidate.source, candidate.target);
                if (edge != null) {
                    if (newEdgeColor.isEnabled())
                        edge.setColor(newEdgeColor.getContent());
                }

                // Mark as used
                processedCandidateVertices.add(candidate.source);
                processedCandidateVertices.add(candidate.target);
                ++successes;
            }
        }

        progressInfo.log("Successfully created " + successes + " edges.");

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeCandidate that = (EdgeCandidate) o;
            return Double.compare(score, that.score) == 0 && Objects.equals(source, that.source) && Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target, score);
        }

        @Override
        public int compareTo(@NotNull FixOverlapsNonBranchingAlgorithm.EdgeCandidate o) {
            return -Double.compare(score, o.score);
        }
    }

}
