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

package org.hkijena.jipipe.plugins.tables.nodes.statistics;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Apply DBSCAN clustering on rows", description = "Applies DBSCAN (density-based spatial clustering of applications with noise) clustering on the table by extracting a feature vector per row and writes the cluster ID into a new column.")
@AddJIPipeCitation("Based on org.apache.commons.math3.ml.clustering.DBSCANClusterer")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Clustering")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Centers", create = true, description = "The cluster centers")
public class DBSCANClusteringAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final OutputSettings outputSettings;
    private final ClusteringSettings clusteringSettings;
    private int epsilon = 0;
    private int minNumPoints = 0;
    private TableColumnSourceExpressionParameter.List inputColumns = new TableColumnSourceExpressionParameter.List();


    public DBSCANClusteringAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputSettings = new OutputSettings();
        this.clusteringSettings = new ClusteringSettings();
        registerSubParameters(outputSettings, clusteringSettings);
    }

    public DBSCANClusteringAlgorithm(DBSCANClusteringAlgorithm other) {
        super(other);
        this.epsilon = other.epsilon;
        this.minNumPoints = other.minNumPoints;
        this.inputColumns = new TableColumnSourceExpressionParameter.List(other.inputColumns);

        this.outputSettings = new OutputSettings(other.outputSettings);
        this.clusteringSettings = new ClusteringSettings(other.clusteringSettings);
        registerSubParameters(outputSettings, clusteringSettings);
    }

    public static double estimateEpsilon(List<IndexedDoublePoint> points) {
        if (points.isEmpty()) return 0.0;

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (IndexedDoublePoint point : points) {
            double[] coords = point.getPoint();
            minX = Math.min(minX, coords[0]);
            maxX = Math.max(maxX, coords[0]);
            minY = Math.min(minY, coords[1]);
            maxY = Math.max(maxY, coords[1]);
        }

        // Bounding box diagonal as reference
        double diagonal = Math.sqrt(Math.pow(maxX - minX, 2) + Math.pow(maxY - minY, 2));
        return diagonal * 0.05; // 5% of diagonal as epsilon
    }

    public static int estimateMinPoints(List<IndexedDoublePoint> points) {
        int dimensions = points.get(0).getPoint().length;
        return Math.max(3, dimensions + 1); // Dimensionality + 1 rule
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData tableData = new ResultsTableData(iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo));

        // Grab the input columns
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);
        List<TableColumnData> inputColumns_ = new ArrayList<>();
        for (TableColumnSourceExpressionParameter inputColumn : inputColumns) {
            TableColumnData columnData = inputColumn.pickOrGenerateColumn(tableData, variablesMap);
            if (columnData != null) {
                inputColumns_.add(columnData);
            } else {
                throw new RuntimeException("Unable to find/generate column " + inputColumn.getValue().getExpression());
            }
        }

        // Create the point array
        List<IndexedDoublePoint> points = new ArrayList<>();
        for (int row = 0; row < tableData.getRowCount(); row++) {
            double[] arr = new double[inputColumns_.size()];
            for (int col = 0; col < inputColumns_.size(); col++) {
                TableColumnData columnData = inputColumns_.get(col);
                double value = columnData.getRowAsDouble(row);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    value = 0;
                }
                arr[col] = value;
            }
            points.add(new IndexedDoublePoint(row, arr));
        }

        // Create the clusterer
        double epsilon_ = epsilon > 0 ? epsilon : estimateEpsilon(points);
        int minNumPoints_ = minNumPoints > 0 ? minNumPoints : estimateMinPoints(points);

        progressInfo.log("DBSCAN parameters: epsilon=" + epsilon_ + ", minNumPoints=" + minNumPoints_);

        DBSCANClusterer<IndexedDoublePoint> clusterer = new DBSCANClusterer<>(epsilon_, minNumPoints_, clusteringSettings.distanceMeasure.getDistanceMeasure());

        // Table with cluster centers
        ResultsTableData clusterCentersTable = new ResultsTableData();
        for (TableColumnData columnData : inputColumns_) {
            clusterCentersTable.addNumericColumn(StringUtils.makeUniqueString("center." + columnData.getLabel(), ".", clusterCentersTable.getColumnNames()));
        }
        clusterCentersTable.addNumericColumn("size");

        // Add columns
        int outputColumnClusterId = outputSettings.outputColumnClusterId.isEnabled() ? tableData.addNumericColumn(outputSettings.outputColumnClusterId.getContent()) : -1;
        int outputColumnClusterSize = outputSettings.outputColumnClusterSize.isEnabled() ? tableData.addNumericColumn(outputSettings.outputColumnClusterSize.getContent()) : -1;

        List<Cluster<IndexedDoublePoint>> centroid = clusterer.cluster(points);
        for (int i = 0; i < centroid.size(); i++) {
            final int clusterId = i + 1;
            Cluster<IndexedDoublePoint> cluster = centroid.get(i);

            // Write into original
            for (IndexedDoublePoint point : cluster.getPoints()) {
                if (outputColumnClusterId >= 0) {
                    tableData.setValueAt(clusterId, point.getSourceRow(), outputColumnClusterId);
                }
                if (outputColumnClusterSize >= 0) {
                    tableData.setValueAt(cluster.getPoints().size(), point.getSourceRow(), outputColumnClusterSize);
                }
            }

            // Write into centers
            {
                int row = clusterCentersTable.addRow();
                double[] center = IndexedDoublePoint.calculateCenter(cluster.getPoints());

                for (int j = 0; j < center.length; j++) {
                    double v = center[j];
                    clusterCentersTable.setValueAt(v, row, j);
                }

                clusterCentersTable.setValueAt(cluster.getPoints().size(), row, "size");
            }
        }

        // Output table
        iterationStep.addOutputData("Output", tableData, progressInfo);
        iterationStep.addOutputData("Centers", clusterCentersTable, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        if (inputColumns.isEmpty()) {
            report.report(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    reportContext,
                    "Input columns cannot be empty!",
                    "The list of input columns cannot be empty.",
                    "Add at least one column."));
        }
    }

    @SetJIPipeDocumentation(name = "Outputs", description = "The generated outputs")
    @JIPipeParameter("output-settings")
    public OutputSettings getOutputSettings() {
        return outputSettings;
    }

    @SetJIPipeDocumentation(name = "Clustering", description = "Advanced clustering settings")
    @JIPipeParameter("clustering-settings")
    public ClusteringSettings getClusteringSettings() {
        return clusteringSettings;
    }

    @SetJIPipeDocumentation(name = "Epsilon", description = "Maximum radius of the neighborhood to be considered. If set to zero or negative, it will be roughly estimated as 5% of the dataset’s bounding box diagonal. We recommend to tune this value manually.")
    @JIPipeParameter(value = "epsilon", important = true)
    public int getEpsilon() {
        return epsilon;
    }

    @JIPipeParameter("epsilon")
    public void setEpsilon(int epsilon) {
        this.epsilon = epsilon;
    }

    @SetJIPipeDocumentation(name = "Minimum number of points", description = "Minimum number of points needed for a cluster. If set to zero or negative, it will be roughly estimated as the number of dimensions + 1. We recommend to tune this value manually.")
    @JIPipeParameter(value = "min-num-points", important = true)
    public int getMinNumPoints() {
        return minNumPoints;
    }

    @JIPipeParameter("min-num-points")
    public void setMinNumPoints(int minNumPoints) {
        this.minNumPoints = minNumPoints;
    }

    @SetJIPipeDocumentation(name = "Input columns", description = "The list of columns that will be used to create the points. Please note that string columns and NA/infinite values are automatically replaced with zeroes.")
    @JIPipeParameter(value = "input-columns", important = true)
    public TableColumnSourceExpressionParameter.List getInputColumns() {
        return inputColumns;
    }

    @JIPipeParameter("input-columns")
    public void setInputColumns(TableColumnSourceExpressionParameter.List inputColumns) {
        this.inputColumns = inputColumns;
    }

    public static class ClusteringSettings extends AbstractJIPipeParameterCollection {
        private int maxIterations = -1;
        private DistanceMeasures distanceMeasure = DistanceMeasures.Euclidean;

        public ClusteringSettings() {
        }

        public ClusteringSettings(ClusteringSettings other) {
            this.maxIterations = other.maxIterations;
            this.distanceMeasure = other.distanceMeasure;
        }

        @SetJIPipeDocumentation(name = "Maximum iterations", description = "The maximum number of iterations to run the algorithm for. If negative, no maximum will be used.")
        @JIPipeParameter("max-iterations")
        public int getMaxIterations() {
            return maxIterations;
        }

        @JIPipeParameter("max-iterations")
        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        @SetJIPipeDocumentation(name = "Distance measure", description = "The distance measure to use")
        @JIPipeParameter("distance-measure")
        public DistanceMeasures getDistanceMeasure() {
            return distanceMeasure;
        }

        @JIPipeParameter("distance-measure")
        public void setDistanceMeasure(DistanceMeasures distanceMeasure) {
            this.distanceMeasure = distanceMeasure;
        }
    }

    public static class OutputSettings extends AbstractJIPipeParameterCollection {

        private OptionalStringParameter outputColumnClusterId = new OptionalStringParameter("cluster_id", true);
        private OptionalStringParameter outputColumnClusterSize = new OptionalStringParameter("cluster_size", true);

        public OutputSettings() {

        }

        public OutputSettings(OutputSettings other) {
            this.outputColumnClusterId = new OptionalStringParameter(other.outputColumnClusterId);
            this.outputColumnClusterSize = new OptionalStringParameter(other.outputColumnClusterSize);
        }

        @SetJIPipeDocumentation(name = "Cluster ID column", description = "The column where the cluster ID will be written to. The first index will be 1.")
        @JIPipeParameter(value = "output-column-cluster-id", important = true)
        @StringParameterSettings(monospace = true)
        public OptionalStringParameter getOutputColumnClusterId() {
            return outputColumnClusterId;
        }

        @JIPipeParameter("output-column-cluster-id")
        public void setOutputColumnClusterId(OptionalStringParameter outputColumnClusterId) {
            this.outputColumnClusterId = outputColumnClusterId;
        }

        @SetJIPipeDocumentation(name = "Cluster size column", description = "The column where the cluster size will be written to")
        @JIPipeParameter("output-column-cluster-size")
        @StringParameterSettings(monospace = true)
        public OptionalStringParameter getOutputColumnClusterSize() {
            return outputColumnClusterSize;
        }

        @JIPipeParameter("output-column-cluster-size")
        public void setOutputColumnClusterSize(OptionalStringParameter outputColumnClusterSize) {
            this.outputColumnClusterSize = outputColumnClusterSize;
        }
    }
}
