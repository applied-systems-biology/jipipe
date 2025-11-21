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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.process;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TDoubleObjectMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipePercentageProgressInfo;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.HyperstackDimension;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsSetParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.*;

@SetJIPipeDocumentation(name = "Copy 2D ROI across Z/C/T", description = "Copies the ROIs within the input list across another C/Z/T dimension. " +
        "For example, if a ROI only exists within one channel, and different ROIs are required for each channel, this node then can copy the ROIs to the other channel locations with a unique name. " +
        "If you just want measurements across multiple channels, use 'Change 2D ROI properties' to set the channel location to zero, which yields a behavior consistent with ImageJ.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Process")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", optional = true, create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class CopyRoi2DAcrossZCTAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension dimension = HyperstackDimension.Frame;
    private OptionalJIPipeExpressionParameter filter = new OptionalJIPipeExpressionParameter(false, "roi.T <= 1");
    private JIPipeExpressionParameter locations = new JIPipeExpressionParameter("MAKE_SEQUENCE(0, num_t)");
    private JIPipeExpressionParameter namingFunction = new JIPipeExpressionParameter("roi.name + \"_\" + target.t");
    private OutputMode outputMode = OutputMode.Merge;
    private boolean measureInPhysicalUnits = true;

    public CopyRoi2DAcrossZCTAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CopyRoi2DAcrossZCTAlgorithm(CopyRoi2DAcrossZCTAlgorithm other) {
        super(other);
        this.dimension = other.dimension;
        this.locations = new JIPipeExpressionParameter(other.locations);
        this.filter = new OptionalJIPipeExpressionParameter(other.filter);
        this.outputMode = other.outputMode;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData inputs = iterationStep.getInputData("Input", ROI2DListData.class, progressInfo);
        ROI2DListData outputs;
        ImagePlus referenceImage = ImageJUtils.unwrap(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));
        if(referenceImage == null) {
            referenceImage = inputs.createDummyImage();
        }

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);

        // Extract reference images
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variablesMap, referenceImage, iterationStep.getMergedTextAnnotations().values());

        // Apply measurements
        progressInfo.log("Measuring ...");
        ResultsTableData measurements = inputs.measure(referenceImage, new ImageJMeasurementsSetParameter(), true, measureInPhysicalUnits);
        measurements.addColumn("roi.name", measurements.getColumnReference("Name"), false);

        // Find the source rois
        progressInfo.log("Filtering sources ...");
        ROI2DListData sources = new ROI2DListData();
        for (int i = 0; i < inputs.size(); i++) {
            Roi roi = inputs.get(i);
            if (filter.isEnabled()) {
                putRoiMeasurementsInVariables(i, measurements, variablesMap);
            } else {
                sources.add(roi);
            }
        }
        progressInfo.log("Filtering sources ... " + sources.size() + " found");

        // Determine the outputs
        switch (outputMode) {
            case Merge:
                outputs = new ROI2DListData(sources);
                break;
            case OnlyNew:
                outputs = new ROI2DListData();
                break;
            case OnlyNewAndOther:
                outputs = new ROI2DListData();
                for (Roi roi : inputs) {
                    if(!sources.contains(roi)) {
                        outputs.add(roi);
                    }
                }
                break;
        }

        // Find the start vertices
        Set<FilamentVertex> startVertices = VertexMaskParameter.filter(vertexMask.getFilter(), outputs, outputs.vertexSet(), variablesMap);
        progressInfo.log(startVertices.size() + " starting vertices will be processed");

        ImmutableList<FilamentVertex> startVerticesList = ImmutableList.copyOf(startVertices);
        Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap = new HashMap<>();
        Multimap<Double, FilamentVertex> newVerticesForLocationsMap = HashMultimap.create();
        for (int i = 0; i < startVerticesList.size(); i++) {
            FilamentVertex startVertex = startVerticesList.get(i);
            JIPipeProgressInfo vertexProgress = progressInfo.resolveAndLog("Vertex " + startVertex.getUuid(), i, startVerticesList.size());
            FilamentVertexVariablesInfo.writeToVariables(outputs, startVertex, variablesMap, "");

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
                copyStartingVertexAcrossFrame(startVertex, requestedLocationsArray, perLocation, newVerticesForLocationsMap, outputs);
            } else if (dimension == HyperstackDimension.Channel) {
                copyStartingVertexAcrossChannel(startVertex, requestedLocationsArray, perLocation, newVerticesForLocationsMap, outputs);
            } else if (dimension == HyperstackDimension.Depth) {
                copyStartingVertexAcrossDepth(startVertex, requestedLocationsArray, perLocation, newVerticesForLocationsMap, outputs);
            } else {
                throw new RuntimeException("Unknown dimension: " + dimension);
            }
        }

        // Copy starting vertices relationships
        if (copyOriginalEdges) {
            progressInfo.log("Copying original edges");
            copyOriginalEdges(startVerticesList, verticesForLocationsMap, inputs, outputs, progressInfo);
        }

        // Connect new vertices to their start vertices
        if (connectNewVerticesToStart) {
            progressInfo.log("Connecting start vertices directly to new vertices");
            connectNewVerticesToStart(startVerticesList, verticesForLocationsMap, outputs, variablesMap, progressInfo);
        }

        // Create linear connection over related vertices
        if (connectOverDimensionLinear) {
            progressInfo.log("Creating linear connections");
            connectVerticesLinear(startVerticesList, verticesForLocationsMap, outputs, variablesMap, progressInfo);
        }

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), outputs, progressInfo);
    }

    private void putRoiMeasurementsInVariables(int row, ResultsTableData measurements, JIPipeExpressionVariablesMap variablesMap) {
        for (int col = 0; col < measurements.getColumnCount(); col++) {
            variablesMap.put(measurements.getColumnName(col), measurements.getValueAt(row, col));
        }
    }

    private void connectVerticesLinear(ImmutableList<FilamentVertex> startVerticesList, Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap, Filaments3DGraphData outputGraph, JIPipeExpressionVariablesMap variablesMap, JIPipeProgressInfo progressInfo) {
        List<FilamentEdgeMetadataEntry> metadataEntries = connectOverDimensionLinearSettings.getMetadata().mapToCollection(FilamentEdgeMetadataEntry.class);
        JIPipePercentageProgressInfo percentage = progressInfo.percentage("Linear connections");
        percentage.log("Start vertices: " + startVerticesList.size());
        for (int j = 0; j < startVerticesList.size(); j++) {
            percentage.logPercentage(j, startVerticesList.size());
            FilamentVertex startVertex = startVerticesList.get(j);

            if (progressInfo.isCanceled()) {
                return;
            }

            TDoubleObjectMap<FilamentVertex> startVertexAtLocations = verticesForLocationsMap.get(startVertex);
            if (startVertexAtLocations == null) {
                continue;
            }

            double[] keys = startVertexAtLocations.keys();
            Arrays.sort(keys);

            for (int i = 0; i < keys.length - 1; i++) {
                if (progressInfo.isCanceled()) {
                    return;
                }

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

    private void connectNewVerticesToStart(ImmutableList<FilamentVertex> startVerticesList, Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap, Filaments3DGraphData outputGraph, JIPipeExpressionVariablesMap variablesMap, JIPipeProgressInfo progressInfo) {
        JIPipePercentageProgressInfo percentage = progressInfo.percentage("Start-to-new connections");
        percentage.log("Start vertices: " + startVerticesList.size());
        List<FilamentEdgeMetadataEntry> metadataEntries = connectNewVerticesToStartSettings.getMetadata().mapToCollection(FilamentEdgeMetadataEntry.class);
        for (int i = 0; i < startVerticesList.size(); i++) {
            percentage.logPercentage(i, startVerticesList.size());
            FilamentVertex startVertex = startVerticesList.get(i);
            TDoubleObjectMap<FilamentVertex> startVertexAtLocations = verticesForLocationsMap.get(startVertex);
            if (startVertexAtLocations == null) {
                continue;
            }
            if (progressInfo.isCanceled()) {
                return;
            }
            for (FilamentVertex newVertex : startVertexAtLocations.valueCollection()) {
                if (newVertex == startVertex) {
                    continue;
                }
                if (progressInfo.isCanceled()) {
                    return;
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

    private void copyOriginalEdges(ImmutableList<FilamentVertex> startVerticesList, Map<FilamentVertex, TDoubleObjectMap<FilamentVertex>> verticesForLocationsMap, Filaments3DGraphData inputGraph, Filaments3DGraphData outputGraph, JIPipeProgressInfo progressInfo) {
        JIPipePercentageProgressInfo percentage = progressInfo.percentage("Copy original edges");
        percentage.log("Start vertices: " + startVerticesList.size());

        for (int i = 0; i < startVerticesList.size(); i++) {
            final FilamentVertex startVertex = startVerticesList.get(i);
            TDoubleObjectMap<FilamentVertex> startVertexAtLocations = verticesForLocationsMap.get(startVertex);
            if (startVertexAtLocations == null) {
                continue;
            }
            if (progressInfo.isCanceled()) {
                return;
            }
            double[] startVertexLocations = startVertexAtLocations.keys();

            JIPipeProgressInfo vertexProgress = progressInfo.resolveAndLog("Vertex " + startVertex.getUuid(), i, startVerticesList.size());
            for (FilamentEdge edge : inputGraph.edgesOf(startVertex)) {

                if (progressInfo.isCanceled()) {
                    return;
                }

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

    @SetJIPipeDocumentation(name = "Locations (in direction)", description = "Expression that determines the locations in the selected direction where the ROI will be present.")
    @JIPipeParameter(value = "locations", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = ImageJMeasurementsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    public JIPipeExpressionParameter getLocations() {
        return locations;
    }

    @JIPipeParameter("locations")
    public void setLocations(JIPipeExpressionParameter locations) {
        this.locations = locations;
    }

    @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @SetJIPipeDocumentation(name = "Output mode", description = "Determines the output of this node. Available options are <ul>" +
            "<li>New and all existing ROIs: the output contains all input and all output ROIs</li>" +
            "<li>Only new ROIs: the output contains only the newly generated ROIs</li>" +
            "<li>New and non-source ROIs: the output contains only the newly generated ROIs and the ones that were not selected as source by the 'Filter sources' parameter</li>" +
            "</ul>")
    @JIPipeParameter("output-mode")
    public OutputMode getOutputMode() {
        return outputMode;
    }

    @JIPipeParameter("output-mode")
    public void setOutputMode(OutputMode outputMode) {
        this.outputMode = outputMode;
    }

    @SetJIPipeDocumentation(name = "Filter sources", description = "If enabled, determine which ROIs act as the starting points (sources) and are copied across the selected dimension")
    @JIPipeParameter("filter")
    public OptionalJIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(OptionalJIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    public enum OutputMode {
        Merge("New and all existing ROIs"),
        OnlyNew("Only new ROIs"),
        OnlyNewAndOther("New and non-source ROIs");

        private final String label;

        OutputMode(String label) {
            this.label = label;
        }


        @Override
        public String toString() {
            return label;
        }
    }
}
