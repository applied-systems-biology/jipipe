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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;

@JIPipeDocumentation(name = "Split into connected components", description = "Algorithm that extracts connected components across one or multiple dimensions. The output consists of multiple ROI lists, one for each connected component.")
@JIPipeNode(menuPath = "Split", nodeTypeCategory = RoiNodeTypeCategory.class)
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Components", autoCreate = true)
public class SplitRoiConnectedComponentsAlgorithm extends JIPipeIteratingAlgorithm {
    private DimensionOperation dimensionZOperation = DimensionOperation.Split;
    private DimensionOperation dimensionCOperation = DimensionOperation.Merge;
    private DimensionOperation dimensionTOperation = DimensionOperation.Follow;
    private OptionalAnnotationNameParameter componentNameAnnotation = new OptionalAnnotationNameParameter("Component", true);
    private DefaultExpressionParameter overlapFilter = new DefaultExpressionParameter();
    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private DefaultExpressionParameter graphPostprocessing = new DefaultExpressionParameter();
    private boolean splitAtJunctions = false;
    private boolean trySolveJunctions = true;

    private boolean measureInPhysicalUnits = true;

    public SplitRoiConnectedComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitRoiConnectedComponentsAlgorithm(SplitRoiConnectedComponentsAlgorithm other) {
        super(other);
        this.dimensionZOperation = other.dimensionZOperation;
        this.dimensionCOperation = other.dimensionCOperation;
        this.dimensionTOperation = other.dimensionTOperation;
        this.componentNameAnnotation = new OptionalAnnotationNameParameter(other.componentNameAnnotation);
        this.overlapFilter = new DefaultExpressionParameter(other.overlapFilter);
        this.overlapFilterMeasurements = new ImageStatisticsSetParameter(other.overlapFilterMeasurements);
        this.splitAtJunctions = other.splitAtJunctions;
        this.trySolveJunctions = other.trySolveJunctions;
        this.graphPostprocessing = new DefaultExpressionParameter(other.graphPostprocessing);
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData input = (ROIListData) dataBatch.getInputData("Input", ROIListData.class, progressInfo).duplicate(progressInfo);
        DefaultUndirectedGraph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int i = 0; i < input.size(); i++) {
            // Add to graph
            graph.addVertex(i);

            // De-associate
            input.get(i).setImage(null);
        }

        boolean withFiltering = !StringUtils.isNullOrEmpty(overlapFilter.getExpression());
        ROIListData temp = new ROIListData();
        ExpressionVariables variableSet = new ExpressionVariables();

        // Write annotations map
        Map<String, String> annotations = new HashMap<>();
        for (Map.Entry<String, JIPipeTextAnnotation> entry : dataBatch.getMergedTextAnnotations().entrySet()) {
            annotations.put(entry.getKey(), entry.getValue().getValue());
        }
        variableSet.set("annotations", annotations);

        ResultsTableData measurements = null;
        ImagePlus referenceImage = null;
        if (withFiltering) {
            // Generate measurements
            ImagePlusData referenceImageData = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
            if (referenceImageData != null) {
                referenceImage = referenceImageData.getImage();
                // This is needed, as measuring messes with the image
                referenceImage = ImageJUtils.duplicate(referenceImage);
            }
            measurements = input.measure(referenceImage, overlapFilterMeasurements, false, measureInPhysicalUnits);
        }
        int currentProgress = 0;
        int currentProgressPercentage = 0;
        int maxProgress = (input.size() * input.size()) / 2;
        JIPipeProgressInfo subProgress = progressInfo.resolve("Overlap tests");
        for (int i = 0; i < input.size(); i++) {
            for (int j = i + 1; j < input.size(); j++) {
                ++currentProgress;
                int newProgressPercentage = (int) (1.0 * currentProgress / maxProgress * 100);
                if (newProgressPercentage != currentProgressPercentage) {
                    subProgress.log(newProgressPercentage + "%");
                    currentProgressPercentage = newProgressPercentage;
                }
                if (progressInfo.isCancelled()) {
                    return;
                }
                Roi roi1 = input.get(i);
                Roi roi2 = input.get(j);

                int z1 = roi1.getZPosition();
                int c1 = roi1.getCPosition();
                int t1 = roi1.getTPosition();
                int z2 = roi2.getZPosition();
                int c2 = roi2.getCPosition();
                int t2 = roi2.getTPosition();

                if (!canTestOverlap(z1, z2, dimensionZOperation))
                    continue;
                if (!canTestOverlap(c1, c2, dimensionCOperation))
                    continue;
                if (!canTestOverlap(t1, t2, dimensionTOperation))
                    continue;

                // Calculate overlap
                Roi overlap = calculateOverlap(temp, roi1, roi2);
                if (overlap != null) {
                    if (withFiltering) {
                        putMeasurementsIntoVariable(measurements, i, j, referenceImage, variableSet, overlap, temp, roi1, roi2);
                        if (!overlapFilter.test(variableSet))
                            continue;
                    }
                    graph.addEdge(i, j);
                }
            }
        }

        if (splitAtJunctions) {
            Comparator<Integer> comparator = null;
            if (trySolveJunctions) {
                if (dimensionZOperation == DimensionOperation.Follow) {
                    comparator = Comparator.comparing(i -> input.get(i).getZPosition());
                }
                if (dimensionCOperation == DimensionOperation.Follow) {
                    if (comparator == null)
                        comparator = Comparator.comparing(i -> input.get(i).getCPosition());
                    else
                        comparator = comparator.thenComparing(i -> input.get(i).getCPosition());
                }
                if (dimensionTOperation == DimensionOperation.Follow) {
                    if (comparator == null)
                        comparator = Comparator.comparing(i -> input.get(i).getTPosition());
                    else
                        comparator = comparator.thenComparing(i -> input.get(i).getTPosition());
                }
            }
            for (int roi : graph.vertexSet()) {
                if (isJunction(roi, input, graph)) {
                    if (trySolveJunctions && comparator != null) {
                        List<Integer> neighbors = Graphs.neighborListOf(graph, roi);
                        neighbors.sort(comparator);
                        while (isJunction(roi, input, graph)) {
                            graph.removeEdge(roi, neighbors.get(0));
                            neighbors.remove(0);
                        }
                    }
                }

                // No solution found: Decompose
                if (isJunction(roi, input, graph))
                    graph.removeAllEdges(ImmutableList.copyOf(graph.edgesOf(roi)));
            }
        }

        if (!StringUtils.isNullOrEmpty(graphPostprocessing.getExpression())) {
            ExpressionVariables postprocessingVariableSet = new ExpressionVariables();
            postprocessingVariableSet.set("KEEP", "KEEP");
            postprocessingVariableSet.set("ISOLATE", "ISOLATE");
            postprocessingVariableSet.set("REMOVE", "REMOVE");
            for (Integer index : ImmutableList.copyOf(graph.vertexSet())) {
                Roi roi = input.get(index);
                postprocessingVariableSet.set("z", roi.getZPosition());
                postprocessingVariableSet.set("c", roi.getCPosition());
                postprocessingVariableSet.set("t", roi.getTPosition());
                postprocessingVariableSet.set("name", StringUtils.nullToEmpty(roi.getName()));
                postprocessingVariableSet.set("degree", graph.degreeOf(index));
                Object result = graphPostprocessing.evaluate(postprocessingVariableSet);
                if (result instanceof Boolean) {
                    if (!(boolean) result) {
                        graph.removeVertex(index);
                    }
                } else if ("KEEP".equals(result)) {
                    // Do nothing
                } else if ("ISOLATE".equals(result)) {
                    graph.removeAllEdges(graph.edgesOf(index));
                } else if ("REMOVE".equals(result)) {
                    graph.removeVertex(index);
                } else {
                    throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new GraphNodeValidationReportContext(this),
                            "Unsupported return value: " + result,
                            "Graph postprocessing expressions only can return one of following values: KEEP, ISOLATE, or REMOVE. Boolean values are also allowed (true = KEEP, false = REMOVE)",
                            "Check if your expression is correct."));
                }
            }
        }

//        DOTExporter<Integer, DefaultEdge> dotExporter = new DOTExporter<>();
//        dotExporter.setVertexAttributeProvider(index -> {
//            Map<String, Attribute> result = new HashMap<>();
//            Roi points = input.get(index);
//            result.put("label", new DefaultAttribute<>(points.getXBase() + "," + points.getYBase() + " z=" + points.getZPosition(), AttributeType.STRING));
//            return result;
//        });
//        dotExporter.exportGraph(graph, new File("graph.dot"));

        ConnectivityInspector<Integer, DefaultEdge> connectivityInspector = new ConnectivityInspector<>(graph);
        int outputIndex = 0;
        for (Set<Integer> set : connectivityInspector.connectedSets()) {
            ROIListData rois = new ROIListData();
            for (Integer index : set) {
                rois.add(input.get(index));
            }
            if (componentNameAnnotation.isEnabled()) {
                dataBatch.addOutputData(getFirstOutputSlot(), rois, Collections.singletonList(new JIPipeTextAnnotation(componentNameAnnotation.getContent(), outputIndex + "")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            } else {
                dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
            }
            ++outputIndex;
        }
    }

    @JIPipeDocumentation(name = "Graph postprocessing", description = "Expression that allows to modify the overlap graph (each node represents a ROI, edges represent an overlap)." +
            " The connected components of the overlap graph are later converted into their respective connected components." +
            "This is applied after all processing steps. If not empty, this expression is executed for each node in the overlap graph. " +
            "Return one of following variables to determine what should be done with the node:" +
            "<ul>" +
            "<li>KEEP leaves the node alone (Alternative: Return boolean true)</li>" +
            "<li>REMOVE removes node (Alternative: Return boolean false)</li>" +
            "<li>ISOLATE removes all edges of the node</li>" +
            "</ul>")
    @JIPipeParameter("graph-postprocessing")
    @ExpressionParameterSettings(variableSource = GraphPostprocessingVariables.class)
    public DefaultExpressionParameter getGraphPostprocessing() {
        return graphPostprocessing;
    }

    @JIPipeParameter("graph-postprocessing")
    public void setGraphPostprocessing(DefaultExpressionParameter graphPostprocessing) {
        this.graphPostprocessing = graphPostprocessing;
    }

    /**
     * Determines if the roi is a junction ROI
     *
     * @param index the roi index
     * @param list  list
     * @param graph the roi graph
     * @return if is a junction
     */
    private boolean isJunction(int index, ROIListData list, DefaultUndirectedGraph<Integer, DefaultEdge> graph) {
        List<Integer> neighbors = Graphs.neighborListOf(graph, index);

        // Only one or no neighbor -> No junction
        if (neighbors.size() <= 1)
            return false;
        for (int i = 0; i < neighbors.size(); i++) {
            for (int j = i + 1; j < neighbors.size(); j++) {
                Roi r0 = list.get(neighbors.get(i));
                Roi r1 = list.get(neighbors.get(j));

                int z0 = r0.getZPosition();
                int z1 = r1.getZPosition();
                int c0 = r0.getCPosition();
                int c1 = r1.getCPosition();
                int t0 = r0.getTPosition();
                int t1 = r1.getTPosition();

                // Resolve non-zero vs zero. Consider zero as "is on this plane"
                if (z0 == 0 && z1 != 0)
                    z0 = z1;
                if (z0 != 0 && z1 == 0)
                    z1 = z0;
                if (c0 == 0 && c1 != 0)
                    c0 = c1;
                if (c0 != 0 && c1 == 0)
                    c1 = c0;
                if (t0 == 0 && t1 != 0)
                    t0 = t1;
                if (t0 != 0 && t1 == 0)
                    t1 = t0;

                if (dimensionZOperation == DimensionOperation.Follow && z0 == z1)
                    return true;
                if (dimensionCOperation == DimensionOperation.Follow && c0 == c1)
                    return true;
                if (dimensionTOperation == DimensionOperation.Follow && t0 == t1)
                    return true;
            }
        }
        return false;
    }

    private void putMeasurementsIntoVariable(ResultsTableData inputMeasurements, int first, int second, ImagePlus referenceImage, ExpressionVariables variableSet, Roi overlap, ROIListData temp, Roi roi1, Roi roi2) {

        variableSet.set("ROI1.z", roi1.getZPosition());
        variableSet.set("ROI1.c", roi1.getCPosition());
        variableSet.set("ROI1.t", roi1.getTPosition());
        variableSet.set("ROI1.name", roi1.getName());
        variableSet.set("ROI2.z", roi2.getZPosition());
        variableSet.set("ROI2.c", roi2.getCPosition());
        variableSet.set("ROI2.t", roi2.getTPosition());
        variableSet.set("ROI2.name", roi2.getName());

        for (int col = 0; col < inputMeasurements.getColumnCount(); col++) {
            variableSet.set("ROI1." + inputMeasurements.getColumnName(col), inputMeasurements.getValueAt(first, col));
            variableSet.set("ROI2." + inputMeasurements.getColumnName(col), inputMeasurements.getValueAt(second, col));
        }

        // Measure overlap
        temp.clear();
        temp.add(overlap);
        ResultsTableData overlapMeasurements = temp.measure(referenceImage, overlapFilterMeasurements, false, measureInPhysicalUnits);
        for (int col = 0; col < overlapMeasurements.getColumnCount(); col++) {
            variableSet.set("Overlap." + overlapMeasurements.getColumnName(col), overlapMeasurements.getValueAt(0, col));
        }
    }

    private boolean canTestOverlap(int x1, int x2, DimensionOperation operation) {
        if (x1 > 0 && x2 > 0) {
            // If split, they should be the same
            // If follow their distance should be 1
            // Otherwise don't care
            if (operation == DimensionOperation.Split) {
                return x1 == x2;
            } else if (operation == DimensionOperation.Follow) {
                return (x2 - x1) == 1;
            } else {
                return true;
            }
        } else {
            // Undefined, so don't care
            return true;
        }
    }

    private Roi calculateOverlap(ROIListData temp, Roi roi1, Roi roi2) {
        temp.clear();
        temp.add(roi1);
        temp.add(roi2);
        temp.logicalAnd();
        if (!temp.isEmpty()) {
            Roi roi = temp.get(0);
            if (roi.getBounds().isEmpty())
                return null;
            return roi;
        }
        return null;
    }

    @JIPipeDocumentation(name = "Dimension Z", description = "Operation for the Z (Slice) dimension. ")
    @JIPipeParameter("operation-dimension-z")
    public DimensionOperation getDimensionZOperation() {
        return dimensionZOperation;
    }

    @JIPipeParameter("operation-dimension-z")
    public void setDimensionZOperation(DimensionOperation dimensionZOperation) {
        this.dimensionZOperation = dimensionZOperation;
    }

    @JIPipeDocumentation(name = "Dimension C", description = "Operation for the C (Channel) dimension. ")
    @JIPipeParameter("operation-dimension-c")
    public DimensionOperation getDimensionCOperation() {
        return dimensionCOperation;
    }

    @JIPipeParameter("operation-dimension-c")
    public void setDimensionCOperation(DimensionOperation dimensionCOperation) {
        this.dimensionCOperation = dimensionCOperation;
    }

    @JIPipeDocumentation(name = "Dimension T", description = "Operation for the T (Time) dimension. ")
    @JIPipeParameter("operation-dimension-t")
    public DimensionOperation getDimensionTOperation() {
        return dimensionTOperation;
    }

    @JIPipeParameter("operation-dimension-t")
    public void setDimensionTOperation(DimensionOperation dimensionTOperation) {
        this.dimensionTOperation = dimensionTOperation;
    }

    @JIPipeDocumentation(name = "Annotate with component", description = "If enabled, an annotation with the numeric component index is generated.")
    @JIPipeParameter("component-name-annotation")
    public OptionalAnnotationNameParameter getComponentNameAnnotation() {
        return componentNameAnnotation;
    }

    @JIPipeParameter("component-name-annotation")
    public void setComponentNameAnnotation(OptionalAnnotationNameParameter componentNameAnnotation) {
        this.componentNameAnnotation = componentNameAnnotation;
    }

    @JIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of ROIs that have an overlap. Please open the expression builder to see a list of all available variables. If the filter is empty, " +
            "no filtering is applied.")
    @JIPipeParameter("overlap-filter")
    @ExpressionParameterSettings(variableSource = RoiOverlapStatisticsVariableSource.class)
    public DefaultExpressionParameter getOverlapFilter() {
        return overlapFilter;
    }

    @JIPipeParameter("overlap-filter")
    public void setOverlapFilter(DefaultExpressionParameter overlapFilter) {
        this.overlapFilter = overlapFilter;
    }

    @JIPipeDocumentation(name = "Overlap filter measurements", description = "Measurements extracted for the overlap filter.")
    @JIPipeParameter("overlap-filter-measurements")
    public ImageStatisticsSetParameter getOverlapFilterMeasurements() {
        return overlapFilterMeasurements;
    }

    @JIPipeParameter("overlap-filter-measurements")
    public void setOverlapFilterMeasurements(ImageStatisticsSetParameter overlapFilterMeasurements) {
        this.overlapFilterMeasurements = overlapFilterMeasurements;
    }

    @JIPipeDocumentation(name = "Split at junctions", description = "If enabled, non-unique connections (between multiple objects) are deleted. The algorithm removes all connections to ROI with a higher z/c/t (depending on the mode)." +
            " This solver can be disabled via the 'Try to solve junctions' parameter." +
            "If no solution can be found this way, all connections around the node are removed (making a single component).")
    @JIPipeParameter("split-at-junctions")
    public boolean isSplitAtJunctions() {
        return splitAtJunctions;
    }

    @JIPipeParameter("split-at-junctions")
    public void setSplitAtJunctions(boolean splitAtJunctions) {
        this.splitAtJunctions = splitAtJunctions;
    }

    @JIPipeDocumentation(name = "Try to solve junctions", description = "If enabled, the 'Split at junctions' function will try to remove connections to ROI with a higher z/c/t (depending on the mode). This is not trivial, as" +
            " ROIs can also be present in any dimension and dimensions can be merged.")
    @JIPipeParameter("try-solve-junctions")
    public boolean isTrySolveJunctions() {
        return trySolveJunctions;
    }

    @JIPipeParameter("try-solve-junctions")
    public void setTrySolveJunctions(boolean trySolveJunctions) {
        this.trySolveJunctions = trySolveJunctions;
    }

    @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
    @JIPipeParameter("measure-in-physical-units")
    public boolean isMeasureInPhysicalUnits() {
        return measureInPhysicalUnits;
    }

    @JIPipeParameter("measure-in-physical-units")
    public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
        this.measureInPhysicalUnits = measureInPhysicalUnits;
    }

    @JIPipeDocumentationDescription(description = "There are three different modes: <ul><li>Followed dimensions will be tracked</li>" +
            "<li>ROI can be split across a dimension. The components are then generated per plane in this dimension.</li>" +
            "<li>Merging is the opposite of splitting: If a dimension is merged, it will be collapsed during the calculation, meaning that all associated ROI will be put together</li></ul>")
    public enum DimensionOperation {
        Merge,
        Follow,
        Split
    }

    public static class GraphPostprocessingVariables implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("Degree", "Degree of the node", "degree"));
            VARIABLES.add(new ExpressionParameterVariable("Z", "The Z location of the ROI (first index is 1, zero indicates no Z constraint)", "z"));
            VARIABLES.add(new ExpressionParameterVariable("C", "The channel (C) location of the ROI (first index is 1, zero indicates no C constraint)", "c"));
            VARIABLES.add(new ExpressionParameterVariable("T", "The frame (T) location of the ROI (first index is 1, zero indicates no T constraint)", "t"));
            VARIABLES.add(new ExpressionParameterVariable("Name", "The name of the ROI (empty string if not set)", "name"));
            VARIABLES.add(new ExpressionParameterVariable("Return: Keep node", "Return value that indicates that the node should be kept.", "KEEP"));
            VARIABLES.add(new ExpressionParameterVariable("Return: Isolate node", "Return value that indicates that the node's edges should be removed.", "ISOLATE"));
            VARIABLES.add(new ExpressionParameterVariable("Return: Remove node", "Return value that indicates that the node should be removed.", "REMOVE"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
