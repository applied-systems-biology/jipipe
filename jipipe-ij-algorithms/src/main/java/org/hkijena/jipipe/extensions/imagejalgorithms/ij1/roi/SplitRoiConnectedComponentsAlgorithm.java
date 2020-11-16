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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.File;
import java.util.*;

@JIPipeDocumentation(name = "Split into connected components", description = "Algorithm that extracts connected components across one or multiple dimensions. The output consists of multiple ROI lists, one for each connected component.")
@JIPipeOrganization(menuPath = "Split", nodeTypeCategory = RoiNodeTypeCategory.class)
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Components")
public class SplitRoiConnectedComponentsAlgorithm extends ImageRoiProcessorAlgorithm {
    public static final String DIMENSION_OPERATION_DESCRIPTION = "There are three different modes: <ul><li>Followed dimensions will be tracked</li>" +
            "<li>ROI can be split across a dimension. The components are then generated per plane in this dimension.</li>" +
            "<li>Merging is the opposite of splitting: If a dimension is merged, it will be collapsed during the calculation, meaning that all associated ROI will be put together</li></ul>";
    private DimensionOperation dimensionZOperation = DimensionOperation.Split;
    private DimensionOperation dimensionCOperation = DimensionOperation.Merge;
    private DimensionOperation dimensionTOperation = DimensionOperation.Follow;
    private OptionalAnnotationNameParameter componentNameAnnotation = new OptionalAnnotationNameParameter("Component", true);
    private DefaultExpressionParameter overlapFilter = new DefaultExpressionParameter();
    private ImageStatisticsSetParameter overlapFilterMeasurements = new ImageStatisticsSetParameter();
    private boolean splitAtJunctions = false;
    private boolean trySolveJunctions = true;

    public SplitRoiConnectedComponentsAlgorithm(JIPipeNodeInfo info) {
        super(info, ROIListData.class, "Components");
        this.setPreferAssociatedImage(false);
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
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        ROIListData input = (ROIListData) dataBatch.getInputData("ROI", ROIListData.class).duplicate();
        DefaultUndirectedGraph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (int i = 0; i < input.size(); i++) {
            // Add to graph
            graph.addVertex(i);

            // De-associate
            input.get(i).setImage(null);
        }

        boolean withFiltering = !StringUtils.isNullOrEmpty(overlapFilter.getExpression());
        ROIListData temp = new ROIListData();
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        ResultsTableData measurements = null;
        ImagePlus referenceImage = null;
        if (withFiltering) {
            // Generate measurements
            Map<ImagePlusData, ROIListData> referenceImages = getReferenceImage(dataBatch, progress);
            if (referenceImages.size() != 1) {
                throw new UserFriendlyRuntimeException("Require unique reference image!",
                        "Algorithm " + getName(),
                        "Different reference images!",
                        "For the statistics, each ROI must be associated to the same image. This is not the case.",
                        "Try to remove the images associated to the ROI.");
            }
            referenceImage = referenceImages.keySet().iterator().next().getImage();
            measurements = input.measure(referenceImage, overlapFilterMeasurements);
        }
        int currentProgress = 0;
        int currentProgressPercentage = 0;
        int maxProgress = (input.size() * input.size()) / 2;
        JIPipeProgressInfo subProgress = progress.resolve("Overlap tests");
        for (int i = 0; i < input.size(); i++) {
            for (int j = i + 1; j < input.size(); j++) {
                ++currentProgress;
                int newProgressPercentage = (int)(1.0 * currentProgress / maxProgress * 100);
                if(newProgressPercentage != currentProgressPercentage) {
                    subProgress.log(newProgressPercentage + "%");
                    currentProgressPercentage = newProgressPercentage;
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
                        putMeasurementsIntoVariable(measurements, i, j, referenceImage, variableSet, overlap, temp);
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
                if(isJunction(roi, input, graph)) {
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

        DOTExporter<Integer, DefaultEdge> dotExporter = new DOTExporter<>();
        dotExporter.setVertexAttributeProvider(index -> {
            Map<String, Attribute> result = new HashMap<>();
            Roi points = input.get(index);
            result.put("label", new DefaultAttribute<>(points.getXBase() + "," + points.getYBase() + " z=" + points.getZPosition(), AttributeType.STRING));
            return result;
        });
        dotExporter.exportGraph(graph, new File("graph.dot"));

        ConnectivityInspector<Integer, DefaultEdge> connectivityInspector = new ConnectivityInspector<>(graph);
        int outputIndex = 0;
        for (Set<Integer> set : connectivityInspector.connectedSets()) {
            ROIListData rois = new ROIListData();
            for (Integer index : set) {
                rois.add(input.get(index));
            }
            if (componentNameAnnotation.isEnabled()) {
                dataBatch.addOutputData(getFirstOutputSlot(), rois, Collections.singletonList(new JIPipeAnnotation(componentNameAnnotation.getContent(), outputIndex + "")), JIPipeAnnotationMergeStrategy.Merge);
            } else {
                dataBatch.addOutputData(getFirstOutputSlot(), rois);
            }
            ++outputIndex;
        }
    }

    /**
     * Determines if the roi is a junction ROI
     * @param index the roi index
     * @param list list
     * @param graph the roi graph
     * @return if is a junction
     */
    private boolean isJunction(int index, ROIListData list, DefaultUndirectedGraph<Integer, DefaultEdge> graph) {
        List<Integer> neighbors = Graphs.neighborListOf(graph, index);
        
        // Only one or no neighbor -> No junction
        if(neighbors.size() <= 1)
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
                if(z0 == 0 && z1 != 0)
                    z0 = z1;
                if(z0 != 0 && z1 == 0)
                    z1 = z0;
                if(c0 == 0 && c1 != 0)
                    c0 = c1;
                if(c0 != 0 && c1 == 0)
                    c1 = c0;
                if(t0 == 0 && t1 != 0)
                    t0 = t1;
                if(t0 != 0 && t1 == 0)
                    t1 = t0;

                if(dimensionZOperation == DimensionOperation.Follow && z0 == z1)
                    return true;
                if(dimensionCOperation == DimensionOperation.Follow && c0 == c1)
                    return true;
                if(dimensionTOperation == DimensionOperation.Follow && t0 == t1)
                    return true;
            }
        }
        return false;
    }

    private void putMeasurementsIntoVariable(ResultsTableData inputMeasurements, int first, int second, ImagePlus referenceImage, StaticVariableSet<Object> variableSet, Roi overlap, ROIListData temp) {
        for (int col = 0; col < inputMeasurements.getColumnCount(); col++) {
            variableSet.set("First." + inputMeasurements.getColumnName(col), inputMeasurements.getValueAt(first, col));
            variableSet.set("Second." + inputMeasurements.getColumnName(col), inputMeasurements.getValueAt(second, col));
        }

        // Measure overlap
        temp.clear();
        temp.add(overlap);
        ResultsTableData overlapMeasurements = temp.measure(referenceImage, overlapFilterMeasurements);
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

    @Override
    public boolean isPreferAssociatedImage() {
        return super.isPreferAssociatedImage();
    }

    @Override
    public void setPreferAssociatedImage(boolean preferAssociatedImage) {
        super.setPreferAssociatedImage(preferAssociatedImage);
    }

    @JIPipeDocumentation(name = "Dimension Z", description = "Operation for the Z (Slice) dimension. " + DIMENSION_OPERATION_DESCRIPTION)
    @JIPipeParameter("operation-dimension-z")
    public DimensionOperation getDimensionZOperation() {
        return dimensionZOperation;
    }

    @JIPipeParameter("operation-dimension-z")
    public void setDimensionZOperation(DimensionOperation dimensionZOperation) {
        this.dimensionZOperation = dimensionZOperation;
    }

    @JIPipeDocumentation(name = "Dimension C", description = "Operation for the C (Channel) dimension. " + DIMENSION_OPERATION_DESCRIPTION)
    @JIPipeParameter("operation-dimension-c")
    public DimensionOperation getDimensionCOperation() {
        return dimensionCOperation;
    }

    @JIPipeParameter("operation-dimension-c")
    public void setDimensionCOperation(DimensionOperation dimensionCOperation) {
        this.dimensionCOperation = dimensionCOperation;
    }

    @JIPipeDocumentation(name = "Dimension T", description = "Operation for the T (Time) dimension. " + DIMENSION_OPERATION_DESCRIPTION)
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

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Component index annotation").report(componentNameAnnotation);
    }

    @JIPipeDocumentation(name = "Overlap filter", description = "This filter is applied to any combination of ROIs that have an overlap. Please open the expression builder to see a list of all available variables. If the filter is empty, " +
            "no filtering is applied.")
    @JIPipeParameter("overlap-filter")
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

    public enum DimensionOperation {
        Merge,
        Follow,
        Split
    }
}
