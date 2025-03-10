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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TDoubleObjectMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.utils.FilamentEdgeMetadataEntry;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdgeVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;

import java.util.*;

@SetJIPipeDocumentation(name = "Copy filaments across Z/C/T", description = "Copies a filament graph to other Z locations, channels, and frames. The newly created vertices can be optionally connected to the neighboring source vertex.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Process")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", optional = true, create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class CopyFilamentsAcrossZCTAlgorithm extends JIPipeIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private final NewEdgesSettings connectOverDimensionLinearSettings;
    private final NewEdgesSettings connectNewVerticesToStartSettings;
    private HyperstackDimension dimension = HyperstackDimension.Frame;
    private JIPipeExpressionParameter locations = new JIPipeExpressionParameter("MAKE_SEQUENCE(0, num_t)");
    private boolean copyOriginalEdges = true;
    private boolean connectOverDimensionLinear = false;
    private boolean connectNewVerticesToStart = false;

    public CopyFilamentsAcrossZCTAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        this.connectOverDimensionLinearSettings = new NewEdgesSettings();
        this.connectNewVerticesToStartSettings = new NewEdgesSettings();
        registerSubParameters(vertexMask, connectOverDimensionLinearSettings, connectNewVerticesToStartSettings);
    }

    public CopyFilamentsAcrossZCTAlgorithm(CopyFilamentsAcrossZCTAlgorithm other) {
        super(other);
        this.dimension = other.dimension;
        this.locations = new JIPipeExpressionParameter(other.locations);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        this.copyOriginalEdges = other.copyOriginalEdges;
        this.connectOverDimensionLinear = other.connectOverDimensionLinear;
        this.connectOverDimensionLinearSettings = new NewEdgesSettings(other.connectOverDimensionLinearSettings);
        this.connectNewVerticesToStart = other.connectNewVerticesToStart;
        this.connectNewVerticesToStartSettings = new NewEdgesSettings(other.connectNewVerticesToStartSettings);
        registerSubParameters(vertexMask, connectOverDimensionLinearSettings, connectNewVerticesToStartSettings);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputGraph = iterationStep.getInputData("Input", Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputGraph = inputGraph.shallowCopy();
        ImagePlusData referenceImageData = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variablesMap,
                referenceImageData != null ? referenceImageData.getImage() : null, iterationStep.getMergedTextAnnotations().values());


        // Find the start vertices
        Set<FilamentVertex> startVertices = VertexMaskParameter.filter(vertexMask.getFilter(), outputGraph, outputGraph.vertexSet(), variablesMap);
        progressInfo.log(startVertices.size() + " starting vertices will be processed");

        ImmutableList<FilamentVertex> startVerticesList = ImmutableList.copyOf(startVertices);
        Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap = new HashMap<>();
        Multimap<Double, FilamentVertex> newVerticesForLocationsMap = HashMultimap.create();
        for (int i = 0; i < startVerticesList.size(); i++) {
            FilamentVertex startVertex = startVerticesList.get(i);
            JIPipeProgressInfo vertexProgress = progressInfo.resolveAndLog("Vertex " + startVertex.getUuid(), i, startVerticesList.size());
            FilamentVertexVariablesInfo.writeToVariables(outputGraph, startVertex, variablesMap, "");

            // Preprocess requested locations
            List<Double> rawRequestedLocations = locations.evaluateToDoubleList(variablesMap);
            TDoubleSet requestedLocationsSet = new TDoubleHashSet();
            if (dimension != HyperstackDimension.Depth) {
                // Apply rounding for non-depth
                rawRequestedLocations.replaceAll(aDouble -> (double) aDouble.intValue());
            }
            // Deduplication and sorting
            requestedLocationsSet.addAll(rawRequestedLocations);
            double[] requestedLocationsArray = Doubles.toArray(rawRequestedLocations);
            Arrays.sort(requestedLocationsArray);

            vertexProgress.log("Will be expanded to " + rawRequestedLocations.size() + " locations (min: " + Doubles.min(requestedLocationsArray) + ", max: " + Doubles.max(requestedLocationsArray) + ")");

            TDoubleObjectMap<FilamentVertex> perLocation = new TDoubleObjectHashMap<>();
            verticesForLocationsMap.put(startVertex, perLocation);

            if (dimension == HyperstackDimension.Frame) {
                copyStartingVertexAcrossFrame(startVertex, requestedLocationsArray, perLocation, newVerticesForLocationsMap, outputGraph);
            } else if (dimension == HyperstackDimension.Channel) {
                copyStartingVertexAcrossChannel(startVertex, requestedLocationsArray, perLocation, newVerticesForLocationsMap, outputGraph);
            } else if (dimension == HyperstackDimension.Depth) {
                copyStartingVertexAcrossDepth(startVertex, requestedLocationsArray, perLocation, newVerticesForLocationsMap, outputGraph);
            } else {
                throw new RuntimeException("Unknown dimension: " + dimension);
            }
        }

        // Copy starting vertices relationships
        if (copyOriginalEdges) {
            progressInfo.log("Copying original edges");
            copyOriginalEdges(progressInfo, startVerticesList, verticesForLocationsMap, inputGraph, outputGraph);
        }

        // Connect new vertices to their start vertices
        if (connectNewVerticesToStart) {
            progressInfo.log("Connecting start vertices directly to new vertices");
            connectNewVerticesToStart(startVerticesList, verticesForLocationsMap, outputGraph, variablesMap);
        }

        // Create linear connection over related vertices
        if (connectOverDimensionLinear) {
            progressInfo.log("Creating linear connections");
            connectVerticesLinear(startVerticesList, verticesForLocationsMap, outputGraph, variablesMap);
        }

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), outputGraph, progressInfo);
    }

    private void connectVerticesLinear(ImmutableList<FilamentVertex> startVerticesList, Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap, Filaments3DGraphData outputGraph, JIPipeExpressionVariablesMap variablesMap) {
        List<FilamentEdgeMetadataEntry> metadataEntries = connectOverDimensionLinearSettings.getMetadata().mapToCollection(FilamentEdgeMetadataEntry.class);
        for (FilamentVertex startVertex : startVerticesList) {
            TDoubleObjectMap<FilamentVertex> startVertexAtLocations = verticesForLocationsMap.get(startVertex);
            if (startVertexAtLocations == null) {
                continue;
            }

            double[] keys = startVertexAtLocations.keys();
            Arrays.sort(keys);

            for (int i = 0; i < keys.length - 1; i++) {
                FilamentVertex current = startVertexAtLocations.get(keys[i]);
                FilamentVertex next = startVertexAtLocations.get(keys[i + 1]);

                // Filter (if enabled)
                FilamentEdgeVariablesInfo.writeToVariables(outputGraph, current, next, variablesMap, "");
                if (connectOverDimensionLinearSettings.filter.isEnabled()) {
                    if (!connectOverDimensionLinearSettings.filter.getContent().evaluateToBoolean(variablesMap)) {
                        continue;
                    }
                }

                // Determine color and metadata
                FilamentEdge edge = new FilamentEdge();
                edge.setColor(connectOverDimensionLinearSettings.color.evaluateToColor(variablesMap));
                for (FilamentEdgeMetadataEntry metadataEntry : metadataEntries) {
                    edge.setMetadata(metadataEntry.getKey(), metadataEntry.getValue().evaluateToString(variablesMap));
                }

                outputGraph.addEdge(current, next, edge);
            }
        }
    }

    private void connectNewVerticesToStart(ImmutableList<FilamentVertex> startVerticesList, Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap, Filaments3DGraphData outputGraph, JIPipeExpressionVariablesMap variablesMap) {
        List<FilamentEdgeMetadataEntry> metadataEntries = connectNewVerticesToStartSettings.getMetadata().mapToCollection(FilamentEdgeMetadataEntry.class);
        for (FilamentVertex startVertex : startVerticesList) {
            TDoubleObjectMap<FilamentVertex> startVertexAtLocations = verticesForLocationsMap.get(startVertex);
            if (startVertexAtLocations == null) {
                continue;
            }
            for (FilamentVertex newVertex : startVertexAtLocations.valueCollection()) {
                if (newVertex == startVertex) {
                    continue;
                }

                // Filter (if enabled)
                FilamentEdgeVariablesInfo.writeToVariables(outputGraph, startVertex, newVertex, variablesMap, "");
                if (connectNewVerticesToStartSettings.filter.isEnabled()) {
                    if (!connectNewVerticesToStartSettings.filter.getContent().evaluateToBoolean(variablesMap)) {
                        continue;
                    }
                }

                // Determine color and metadata
                FilamentEdge edge = new FilamentEdge();
                edge.setColor(connectNewVerticesToStartSettings.color.evaluateToColor(variablesMap));
                for (FilamentEdgeMetadataEntry metadataEntry : metadataEntries) {
                    edge.setMetadata(metadataEntry.getKey(), metadataEntry.getValue().evaluateToString(variablesMap));
                }

                outputGraph.addEdge(startVertex, newVertex, edge);
            }
        }
    }

    private void copyOriginalEdges(JIPipeProgressInfo progressInfo, ImmutableList<FilamentVertex> startVerticesList, Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap, Filaments3DGraphData inputGraph, Filaments3DGraphData outputGraph) {
        for (int i = 0; i < startVerticesList.size(); i++) {
            final FilamentVertex startVertex = startVerticesList.get(i);
            TDoubleObjectMap<FilamentVertex> startVertexAtLocations = verticesForLocationsMap.get(startVertex);
            if (startVertexAtLocations == null) {
                continue;
            }
            double[] startVertexLocations = startVertexAtLocations.keys();

            JIPipeProgressInfo vertexProgress = progressInfo.resolveAndLog("Vertex " + startVertex.getUuid(), i, startVerticesList.size());
            for (FilamentEdge edge : inputGraph.edgesOf(startVertex)) {
                FilamentVertex startSource = inputGraph.getEdgeSource(edge);
                FilamentVertex startTarget = inputGraph.getEdgeTarget(edge);

                // Ensure that startSource is always the startVertex and startTarget is its neighbor
                if (startTarget == startVertex) {
                    FilamentVertex vertex = startSource;
                    startSource = startTarget;
                    startTarget = vertex;
                }

                assert startSource == startVertex;

                // Go through each location that is new and search for the new vertex
                TDoubleObjectMap<FilamentVertex> neighborAtLocation = verticesForLocationsMap.get(startTarget);
                for (double location : startVertexLocations) {
                    FilamentVertex newSource = startVertexAtLocations.get(location);
                    FilamentVertex newTarget = neighborAtLocation.get(location);
                    if (newSource != startSource && newTarget != null) {
                        FilamentEdge edgeCopy = new FilamentEdge(edge);
                        outputGraph.addEdge(newSource, newTarget, edgeCopy);
                    }
                }

            }
        }
    }

    private boolean isAtLocation(FilamentVertex vertex, double location) {
        switch (dimension) {
            case Frame:
                return vertex.getNonSpatialLocation().getFrame() == (int) location;
            case Channel:
                return vertex.getNonSpatialLocation().getChannel() == (int) location;
            case Depth:
                return vertex.getSpatialLocation().getZ() == location;
            default:
                throw new RuntimeException("Unknown dimension: " + dimension);
        }
    }

    private void copyStartingVertexAcrossDepth(FilamentVertex vertex, double[] requestedLocationsArray, TDoubleObjectMap<FilamentVertex> perLocation, Multimap<Double, FilamentVertex> newVerticesForLocationsMap, Filaments3DGraphData filaments) {
        perLocation.put(vertex.getSpatialLocation().getZ(), vertex);
        for (double depth : requestedLocationsArray) {
            if (!perLocation.containsKey(depth)) {
                FilamentVertex copy = new FilamentVertex(vertex);
                copy.getSpatialLocation().setZ(depth);
                perLocation.put(depth, copy);
                newVerticesForLocationsMap.put(depth, copy);

                filaments.addVertex(copy);
            }
        }
    }

    private void copyStartingVertexAcrossChannel(FilamentVertex vertex, double[] requestedLocationsArray, TDoubleObjectMap<FilamentVertex> perLocation, Multimap<Double, FilamentVertex> newVerticesForLocationsMap, Filaments3DGraphData filaments) {
        perLocation.put(vertex.getNonSpatialLocation().getChannel(), vertex);
        for (double location : requestedLocationsArray) {
            int channel = (int) location;
            if (!perLocation.containsKey(channel)) {
                FilamentVertex copy = new FilamentVertex(vertex);
                copy.getNonSpatialLocation().setChannel(channel);
                perLocation.put(channel, copy);
                newVerticesForLocationsMap.put((double) channel, copy);

                filaments.addVertex(copy);
            }
        }
    }

    private void copyStartingVertexAcrossFrame(FilamentVertex vertex, double[] requestedLocationsArray, TDoubleObjectMap<FilamentVertex> perLocation, Multimap<Double, FilamentVertex> newVerticesForLocationsMap, Filaments3DGraphData filaments) {
        perLocation.put(vertex.getNonSpatialLocation().getFrame(), vertex);
        for (double location : requestedLocationsArray) {
            int frame = (int) location;
            if (!perLocation.containsKey(frame)) {
                FilamentVertex copy = new FilamentVertex(vertex);
                copy.getNonSpatialLocation().setFrame(frame);
                perLocation.put(frame, copy);
                newVerticesForLocationsMap.put((double) frame, copy);

                filaments.addVertex(copy);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Direction", description = "The direction in which to expand each vertex.")
    @JIPipeParameter(value = "dimension", important = true)
    public HyperstackDimension getDimension() {
        return dimension;
    }

    @JIPipeParameter("dimension")
    public void setDimension(HyperstackDimension dimension) {
        this.dimension = dimension;
    }

    @SetJIPipeDocumentation(name = "Locations (in direction)", description = "Expression that determines the locations in the selected direction where the vertex will be present.")
    @JIPipeParameter(value = "locations", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per start vertex")
    public JIPipeExpressionParameter getLocations() {
        return locations;
    }

    @JIPipeParameter("locations")
    public void setLocations(JIPipeExpressionParameter locations) {
        this.locations = locations;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Used to filter vertices")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @SetJIPipeDocumentation(name = "Copy original edges", description = "If enabled, copy the original edges to the copied vertices")
    @JIPipeParameter("copy-original-edges")
    public boolean isCopyOriginalEdges() {
        return copyOriginalEdges;
    }

    @JIPipeParameter("copy-original-edges")
    public void setCopyOriginalEdges(boolean copyOriginalEdges) {
        this.copyOriginalEdges = copyOriginalEdges;
    }

    @SetJIPipeDocumentation(name = "Connect new vertices over direction", description = "If enabled, newly created vertices are connected to their source vertices. " +
            "The newly formed connections will be only created between neighboring locations (e.g., z0 to z1 to z4 to z10)")
    @JIPipeParameter("connect-over-dimension-linear")
    public boolean isConnectOverDimensionLinear() {
        return connectOverDimensionLinear;
    }

    @JIPipeParameter("connect-over-dimension-linear")
    public void setConnectOverDimensionLinear(boolean connectOverDimensionLinear) {
        this.connectOverDimensionLinear = connectOverDimensionLinear;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Newly created edges over direction", description = "Settings related to 'Connect new vertices over direction'")
    @JIPipeParameter("connect-over-dimension-linear-settings")
    public NewEdgesSettings getConnectOverDimensionLinearSettings() {
        return connectOverDimensionLinearSettings;
    }

    @SetJIPipeDocumentation(name = "Connect new vertices to start", description = "If enabled, connect newly created vertices to their start vertex")
    @JIPipeParameter("connect-new-vertices-to-start")
    public boolean isConnectNewVerticesToStart() {
        return connectNewVerticesToStart;
    }

    @JIPipeParameter("connect-new-vertices-to-start")
    public void setConnectNewVerticesToStart(boolean connectNewVerticesToStart) {
        this.connectNewVerticesToStart = connectNewVerticesToStart;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Newly created edges to start", description = "Settings related to 'Connect new vertices to start'")
    @JIPipeParameter("connect-new-vertices-to-start-settings")
    public NewEdgesSettings getConnectNewVerticesToStartSettings() {
        return connectNewVerticesToStartSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (subParameter == connectOverDimensionLinearSettings) {
            return connectOverDimensionLinear;
        }
        if (subParameter == connectNewVerticesToStartSettings) {
            return connectNewVerticesToStart;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    public static class NewEdgesSettings extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter color = new JIPipeExpressionParameter("\"green\"");
        private ParameterCollectionList metadata = ParameterCollectionList.containingCollection(FilamentEdgeMetadataEntry.class);
        private OptionalJIPipeExpressionParameter filter = new OptionalJIPipeExpressionParameter();

        public NewEdgesSettings() {

        }

        public NewEdgesSettings(NewEdgesSettings other) {
            this.color = new JIPipeExpressionParameter(other.color);
            this.metadata = new ParameterCollectionList(other.metadata);
            this.filter = new OptionalJIPipeExpressionParameter(other.filter);
        }

        @SetJIPipeDocumentation(name = "Only create edge if ...", description = "If enabled, allows to filter the creation of interconnecting edges.")
        @AddJIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
        @JIPipeParameter("filter")
        public OptionalJIPipeExpressionParameter getFilter() {
            return filter;
        }

        @JIPipeParameter("filter")
        public void setFilter(OptionalJIPipeExpressionParameter filter) {
            this.filter = filter;
        }

        @SetJIPipeDocumentation(name = "Color", description = "Expression that determines the edge color")
        @JIPipeParameter("color")
        @AddJIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
        @JIPipeExpressionParameterSettings(hint = "per edge")
        public JIPipeExpressionParameter getColor() {
            return color;
        }

        @JIPipeParameter("color")
        public void setColor(JIPipeExpressionParameter color) {
            this.color = color;
        }

        @SetJIPipeDocumentation(name = "Metadata", description = "Allows to set/override vertex metadata values")
        @JIPipeParameter("metadata")
        @AddJIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
        @ParameterCollectionListTemplate(FilamentEdgeMetadataEntry.class)
        public ParameterCollectionList getMetadata() {
            return metadata;
        }

        @JIPipeParameter("metadata")
        public void setMetadata(ParameterCollectionList metadata) {
            this.metadata = metadata;
        }
    }
}
