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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

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
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentUnconnectedEdgeVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Connect filament vertices (Expression)", description = "Connect existing vertices based on customizable criteria. Uses expressions to filter candidate edges, which can be slow.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Mask", optional = true, create = true)
public class ConnectVerticesAlgorithm extends JIPipeIteratingAlgorithm {
    private OptionalJIPipeExpressionParameter sourceVertexFilter = new OptionalJIPipeExpressionParameter(false, "");
    private OptionalJIPipeExpressionParameter targetVertexFilter = new OptionalJIPipeExpressionParameter(false, "");
    private boolean connectAcrossC = false;
    private boolean connectAcrossT = false;
    private boolean requireDirection = false;
    private boolean enable3D = true;
    private JIPipeExpressionParameter filterFunction = new JIPipeExpressionParameter("length < 100 AND source.degree == 1 AND target.degree == 1");
    private OptionalColorParameter newEdgeColor = new OptionalColorParameter(Color.GREEN, true);
    private JIPipeExpressionParameter scoringFunction = new JIPipeExpressionParameter("0");
    private OptionalIntegerParameter limitConnections = new OptionalIntegerParameter(false, 1);
    private boolean enforceEdgesWithinMask = true;
    private boolean findPath = true;
    private boolean ignoreLimitConnectionsForSource = false;
    private boolean isIgnoreLimitConnectionsForTarget = false;
    private StringAndStringPairParameter.List newEdgeMetadata = new StringAndStringPairParameter.List();

    public ConnectVerticesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConnectVerticesAlgorithm(ConnectVerticesAlgorithm other) {
        super(other);
        this.connectAcrossC = other.connectAcrossC;
        this.connectAcrossT = other.connectAcrossT;
        this.enable3D = other.enable3D;
        this.requireDirection = other.requireDirection;
        this.findPath = other.findPath;
        this.filterFunction = new JIPipeExpressionParameter(other.filterFunction);
        this.newEdgeColor = new OptionalColorParameter(other.newEdgeColor);
        this.enforceEdgesWithinMask = other.enforceEdgesWithinMask;
        this.scoringFunction = new JIPipeExpressionParameter(other.scoringFunction);
        this.limitConnections = new OptionalIntegerParameter(other.limitConnections);
        this.sourceVertexFilter = new OptionalJIPipeExpressionParameter(other.sourceVertexFilter);
        this.targetVertexFilter = new OptionalJIPipeExpressionParameter(other.targetVertexFilter);
        this.ignoreLimitConnectionsForSource = other.ignoreLimitConnectionsForSource;
        this.isIgnoreLimitConnectionsForTarget = other.isIgnoreLimitConnectionsForTarget;
        this.newEdgeMetadata = new StringAndStringPairParameter.List(other.newEdgeMetadata);
    }

    @SetJIPipeDocumentation(name = "Bypass connection limit for sources", description = "If enabled, the 'Limit connections' parameter does not apply to source vertices.")
    @JIPipeParameter("ignore-limit-connections-for-source")
    public boolean isIgnoreLimitConnectionsForSource() {
        return ignoreLimitConnectionsForSource;
    }

    @JIPipeParameter("ignore-limit-connections-for-source")
    public void setIgnoreLimitConnectionsForSource(boolean ignoreLimitConnectionsForSource) {
        this.ignoreLimitConnectionsForSource = ignoreLimitConnectionsForSource;
    }

    @SetJIPipeDocumentation(name = "Bypass connection limit for targets", description = "If enabled, the 'Limit connections' parameter does not apply to target vertices.")
    @JIPipeParameter("ignore-limit-connections-for-target")
    public boolean isIgnoreLimitConnectionsForTarget() {
        return isIgnoreLimitConnectionsForTarget;
    }

    @JIPipeParameter("ignore-limit-connections-for-target")
    public void setIgnoreLimitConnectionsForTarget(boolean ignoreLimitConnectionsForTarget) {
        isIgnoreLimitConnectionsForTarget = ignoreLimitConnectionsForTarget;
    }

    @SetJIPipeDocumentation(name = "Scoring function", description = "Expression executed per edge candidate to generate a score for limited connections. " +
            "Higher scores are selected first.")
    @JIPipeParameter("scoring-function")
    @JIPipeExpressionParameterSettings(hint = "per candidate edge")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentUnconnectedEdgeVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "source.direction", name = "Source direction", description = "Vector that contains the direction of the source vertex")
    @AddJIPipeExpressionParameterVariable(key = "target.direction", name = "Target direction", description = "Vector that contains the direction of the target vertex")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @AddJIPipeExpressionParameterVariable(key = "path_exists", name = "Path exists", description = "Returns true if there is already a path between the two candidate vertices (false if there is no path)")
    @AddJIPipeExpressionParameterVariable(key = "path_length", name = "Path length", description = "Existing path length in number of edges between the vertices (NaN if there is no path)")
    @AddJIPipeExpressionParameterVariable(key = "dot_product", name = "Vertices direction dot product", description = "The dot product of source and target directions. " +
            "-1 if the directions are opposite and 1 if they point at the same direction (NaN if not available)")
    @AddJIPipeExpressionParameterVariable(key = "angle", name = "Angle (degrees)", description = "The angle in degrees between the source and target directions (NaN if not available).")
    public JIPipeExpressionParameter getScoringFunction() {
        return scoringFunction;
    }

    @JIPipeParameter("scoring-function")
    public void setScoringFunction(JIPipeExpressionParameter scoringFunction) {
        this.scoringFunction = scoringFunction;
    }

    @SetJIPipeDocumentation(name = "Limit created edges (source/target)", description = "If enabled, limit the number of created edges per vertex. Uses a scoring function to determine an order.")
    @JIPipeParameter("limit-connections")
    public OptionalIntegerParameter getLimitConnections() {
        return limitConnections;
    }

    @JIPipeParameter("limit-connections")
    public void setLimitConnections(OptionalIntegerParameter limitConnections) {
        this.limitConnections = limitConnections;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Source vertex filter", description = "Allows to pre-select source vertices")
    @JIPipeParameter("source-vertex-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public OptionalJIPipeExpressionParameter getSourceVertexFilter() {
        return sourceVertexFilter;
    }

    @JIPipeParameter("source-vertex-filter")
    public void setSourceVertexFilter(OptionalJIPipeExpressionParameter sourceVertexFilter) {
        this.sourceVertexFilter = sourceVertexFilter;
    }

    @SetJIPipeDocumentation(name = "Target vertex filter", description = "Allows to pre-select target vertices")
    @JIPipeParameter("target-vertex-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public OptionalJIPipeExpressionParameter getTargetVertexFilter() {
        return targetVertexFilter;
    }

    @JIPipeParameter("target-vertex-filter")
    public void setTargetVertexFilter(OptionalJIPipeExpressionParameter targetVertexFilter) {
        this.targetVertexFilter = targetVertexFilter;
    }

    @SetJIPipeDocumentation(name = "Find path between candidate vertices", description = "If enabled, find the path between the candidate vertices. Impacts the performance.")
    @JIPipeParameter("find-path")
    public boolean isFindPath() {
        return findPath;
    }

    @JIPipeParameter("find-path")
    public void setFindPath(boolean findPath) {
        this.findPath = findPath;
    }

    @SetJIPipeDocumentation(name = "Set new edges metadata", description = "Allows to set the metadata of newly created edges")
    @JIPipeParameter("new-edge-metadata")
    public StringAndStringPairParameter.List getNewEdgeMetadata() {
        return newEdgeMetadata;
    }

    @JIPipeParameter("new-edge-metadata")
    public void setNewEdgeMetadata(StringAndStringPairParameter.List newEdgeMetadata) {
        this.newEdgeMetadata = newEdgeMetadata;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == this && "scoring-function".equals(access.getKey())) {
            return limitConnections.isEnabled();
        }
        return super.isParameterUIVisible(tree, access);
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

    @SetJIPipeDocumentation(name = "Require direction", description = "Only considers vertices that have a degree of 1, which allows the calculation of a direction")
    @JIPipeParameter("require-direction")
    public boolean isRequireDirection() {
        return requireDirection;
    }

    @JIPipeParameter("require-direction")
    public void setRequireDirection(boolean requireDirection) {
        this.requireDirection = requireDirection;
    }

    @SetJIPipeDocumentation(name = "Candidate edge filter", description = "Filter expression that determines if an edge is considered as candidate")
    @JIPipeParameter("filter-function")
    @JIPipeExpressionParameterSettings(hint = "per candidate edge")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentUnconnectedEdgeVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "source.direction", name = "Source direction", description = "Vector that contains the direction of the source vertex")
    @AddJIPipeExpressionParameterVariable(key = "target.direction", name = "Target direction", description = "Vector that contains the direction of the target vertex")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @AddJIPipeExpressionParameterVariable(key = "path_exists", name = "Path exists", description = "Returns true if there is already a path between the two candidate vertices (false if there is no path)")
    @AddJIPipeExpressionParameterVariable(key = "path_length", name = "Path length", description = "Existing path length in number of edges between the vertices (NaN if there is no path)")
    @AddJIPipeExpressionParameterVariable(key = "dot_product", name = "Vertices direction dot product", description = "The dot product of source and target directions. " +
            "-1 if the directions are opposite and 1 if they point at the same direction (NaN if not available)")
    @AddJIPipeExpressionParameterVariable(key = "angle", name = "Angle", description = "The angle between the source and target directions (NaN if not available).")
    public JIPipeExpressionParameter getFilterFunction() {
        return filterFunction;
    }

    @JIPipeParameter("filter-function")
    public void setFilterFunction(JIPipeExpressionParameter filterFunction) {
        this.filterFunction = filterFunction;
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
        Filaments3DGraphData inputData = iterationStep.getInputData("Input", Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

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

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        progressInfo.log("Filtering connection sources ...");
        Set<FilamentVertex> sources = VertexMaskParameter.filter(sourceVertexFilter, outputData, null, variables);
        progressInfo.log("Filtering connection targets ...");
        Set<FilamentVertex> targets = VertexMaskParameter.filter(targetVertexFilter, outputData, null, variables);

        HashSet<EdgeCandidate> candidates = new HashSet<>();
        DijkstraShortestPath<FilamentVertex, FilamentEdge> outputDataInspector = findPath ? new DijkstraShortestPath<>(outputData) : null;
        for (FilamentVertex current : sources) {

            Vector3d currentV1 = current.getSpatialLocation().toSciJavaVector3d();
            Vector3d currentV2;
            boolean hasDirection;

            {
                Set<FilamentVertex> neighbors = Graphs.neighborSetOf(outputData, current);
                if (neighbors.size() == 1) {
                    currentV2 = neighbors.iterator().next().getSpatialLocation().toSciJavaVector3d();
                    hasDirection = true;
                } else {
                    currentV2 = currentV1;
                    hasDirection = false;
                }
            }

            if (requireDirection && !hasDirection) {
                continue;
            }

            if (!enable3D) {
                currentV1.z = 0;
                currentV2.z = 0;
            }

            Vector3d currentDirection = new Vector3d(currentV2.x - currentV1.x, currentV2.y - currentV1.y, currentV2.z - currentV1.z);
            if (hasDirection) {
                currentDirection.normalize();
            }

            outer:
            for (FilamentVertex other : targets) {
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
                    if (progressInfo.isCancelled()) {
                        return;
                    }

                    if (mask != null) {
                        if (hasDirection) {
                            // Iterate through centers
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
                        } else {
                            // Just test at vertex location
                            Vector3d testLocation = new Vector3d(currentV1.x,
                                    currentV1.y,
                                    currentV1.z);
                            int x = Math.max(0, Math.min(mask.getWidth() - 1, (int) Math.round(testLocation.x)));
                            int y = (int) Math.round(testLocation.y);
                            int z = Math.max(0, (int) Math.round(testLocation.z));

                            ImageProcessor ip = ImageJUtils.getSliceZero(mask, 0, z, 0);
                            if (ip.get(x, y) == 0) {
                                continue;
                            }
                        }
                    }

                    if (progressInfo.isCancelled()) {
                        return;
                    }

                    // Calculate other direction
                    Vector3d otherV1 = other.getSpatialLocation().toSciJavaVector3d();
                    Vector3d otherV2;

                    if (hasDirection) {
                        Set<FilamentVertex> neighbors = Graphs.neighborSetOf(outputData, other);
                        if (neighbors.size() == 1) {
                            otherV2 = neighbors.iterator().next().getSpatialLocation().toSciJavaVector3d();
                        } else {
                            otherV2 = otherV1;
                        }
                    } else {
                        otherV2 = otherV1;
                    }

                    if (!enable3D) {
                        otherV1.z = 0;
                        otherV2.z = 0;
                    }

                    Vector3d otherDirection = new Vector3d(otherV2.x - otherV1.x, otherV2.y - otherV1.y, otherV2.z - otherV1.z);
                    if (hasDirection) {
                        otherDirection.normalize();
                    }

                    // Write variables
                    FilamentUnconnectedEdgeVariablesInfo.writeToVariables(outputData, current, other, variables, "");

                    // Path check
                    GraphPath<FilamentVertex, FilamentEdge> path = null;
                    if (findPath && outputDataInspector != null) {
                        path = outputDataInspector.getPath(current, other);
                    }
                    if (path != null) {
                        variables.set("path_exists", true);
                        variables.set("path_length", path.getLength());
                    } else {
                        variables.set("path_exists", false);
                        variables.set("path_length", Double.NaN);
                    }

                    // Direction-related variables
                    if (hasDirection) {
                        variables.set("angle", Math.toDegrees(currentDirection.angle(otherDirection)));
                        variables.set("dot_product", currentDirection.dot(otherDirection));
                        variables.set("source.direction", Arrays.asList(currentDirection.x, currentDirection.y, currentDirection.z));
                        variables.set("target.direction", Arrays.asList(otherDirection.x, otherDirection.y, otherDirection.z));
                    } else {
                        variables.set("angle", Double.NaN);
                        variables.set("dot_product", Double.NaN);
                        variables.set("source.direction", Arrays.asList(0, 0, 0));
                        variables.set("target.direction", Arrays.asList(0, 0, 0));
                    }

                    // Check via filter
                    if (!filterFunction.test(variables)) {
                        continue;
                    }

                    double score;
                    if (limitConnections.isEnabled()) {
                        score = scoringFunction.evaluateToDouble(variables);
                    } else {
                        score = 0;
                    }

                    // Add as candidate
                    EdgeCandidate candidate = new EdgeCandidate(current, other, score);
                    candidates.add(candidate);

                }
            }
        }

        // Apply connect
        int successes = 0;
        Map<FilamentVertex, Integer> connectionCount = new HashMap<>();
        for (EdgeCandidate candidate : candidates.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {

            if (progressInfo.isCancelled()) {
                return;
            }

            if (limitConnections.isEnabled()) {
                int c1 = connectionCount.getOrDefault(candidate.source, 0);
                int c2 = connectionCount.getOrDefault(candidate.target, 0);
                if (ignoreLimitConnectionsForSource) {
                    c1 = 0;
                }
                if (isIgnoreLimitConnectionsForTarget) {
                    c2 = 0;
                }
                if (c1 < limitConnections.getContent() && c2 < limitConnections.getContent()) {
                    connectionCount.put(candidate.source, c1 + 1);
                    connectionCount.put(candidate.target, c2 + 1);
                } else {
                    continue;
                }
            }

            // Connect
            FilamentEdge edge = outputData.addEdge(candidate.source, candidate.target);
            if (edge != null) {
                if (newEdgeColor.isEnabled()) {
                    edge.setColor(newEdgeColor.getContent());
                }
                for (StringAndStringPairParameter item : newEdgeMetadata) {
                    edge.setMetadata(item.getKey(), item.getValue());
                }

            }
            ++successes;
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
        public int compareTo(@NotNull EdgeCandidate o) {
            return -Double.compare(score, o.score);
        }
    }

}
