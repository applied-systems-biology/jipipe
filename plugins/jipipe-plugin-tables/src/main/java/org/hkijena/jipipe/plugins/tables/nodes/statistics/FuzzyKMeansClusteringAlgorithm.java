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

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer;
import org.apache.commons.math3.random.JDKRandomGenerator;
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

@SetJIPipeDocumentation(name = "Apply Fuzzy K-Means clustering on rows", description = "Applies Fuzzy K-Means clustering on the table by extracting a feature vector per row and writes the best-matching cluster ID and the memberships into a new column.")
@AddJIPipeCitation("Based on org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Clustering")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Centers", create = true, description = "The cluster centers (best-matching)")
public class FuzzyKMeansClusteringAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final OutputSettings outputSettings;
    private final ClusteringSettings clusteringSettings;
    private int k = 3;
    private double fuzziness = 5;
    private TableColumnSourceExpressionParameter.List inputColumns = new TableColumnSourceExpressionParameter.List();


    public FuzzyKMeansClusteringAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.outputSettings = new OutputSettings();
        this.clusteringSettings = new ClusteringSettings();
        registerSubParameters(outputSettings, clusteringSettings);
    }

    public FuzzyKMeansClusteringAlgorithm(FuzzyKMeansClusteringAlgorithm other) {
        super(other);
        this.k = other.k;
        this.fuzziness = other.fuzziness;
        this.inputColumns = new TableColumnSourceExpressionParameter.List(other.inputColumns);

        this.outputSettings = new OutputSettings(other.outputSettings);
        this.clusteringSettings = new ClusteringSettings(other.clusteringSettings);
        registerSubParameters(outputSettings, clusteringSettings);
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

        // Create the clusterer and the point array
        FuzzyKMeansClusterer<IndexedDoublePoint> clusterer = new FuzzyKMeansClusterer<>(k,
                fuzziness,
                clusteringSettings.maxIterations,
                clusteringSettings.distanceMeasure.getDistanceMeasure(),
                clusteringSettings.epsilon,
                new JDKRandomGenerator());

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

        // Table with cluster centers
        ResultsTableData clusterCentersTable = new ResultsTableData();
        for (TableColumnData columnData : inputColumns_) {
            clusterCentersTable.addNumericColumn(StringUtils.makeUniqueString("center." + columnData.getLabel(), ".", clusterCentersTable.getColumnNames()));
        }
        clusterCentersTable.addNumericColumn("size");

        // Add columns (best matches)
        int outputColumnClusterId = outputSettings.outputColumnClusterId.isEnabled() ? tableData.addNumericColumn(outputSettings.outputColumnClusterId.getContent()) : -1;
        int outputColumnClusterSize = outputSettings.outputColumnClusterSize.isEnabled() ? tableData.addNumericColumn(outputSettings.outputColumnClusterSize.getContent()) : -1;
        int outputColumnDistanceToCenter = outputSettings.outputColumnDistanceToCenter.isEnabled() ? tableData.addNumericColumn(outputSettings.outputColumnDistanceToCenter.getContent()) : -1;

        List<CentroidCluster<IndexedDoublePoint>> centroid = clusterer.cluster(points);
        for (int i = 0; i < centroid.size(); i++) {
            final int clusterId = i + 1;
            CentroidCluster<IndexedDoublePoint> cluster = centroid.get(i);

            // Write into original
            for (IndexedDoublePoint point : cluster.getPoints()) {
                if (outputColumnClusterId >= 0) {
                    tableData.setValueAt(clusterId, point.getSourceRow(), outputColumnClusterId);
                }
                if (outputColumnClusterSize >= 0) {
                    tableData.setValueAt(cluster.getPoints().size(), point.getSourceRow(), outputColumnClusterSize);
                }
                if (outputColumnDistanceToCenter >= 0) {
                    double distanceToCenter = clusteringSettings.distanceMeasure.getDistanceMeasure().compute(cluster.getCenter().getPoint(), point.getPoint());
                    tableData.setValueAt(distanceToCenter, point.getSourceRow(), outputColumnDistanceToCenter);
                }
            }

            // Write into centers
            {
                int row = clusterCentersTable.addRow();
                double[] point = cluster.getCenter().getPoint();
                for (int j = 0; j < point.length; j++) {
                    double v = point[j];
                    clusterCentersTable.setValueAt(v, row, j);
                }

                clusterCentersTable.setValueAt(cluster.getPoints().size(), row, "size");
            }
        }

        // Add columns (memberships)
        int outputColumnMembership1 = -1;
        for (int i = 0; i < centroid.size(); i++) {
            if (i > 0) {
                tableData.addNumericColumn(outputSettings.membershipPrefix + (i + 1));
            } else {
                outputColumnMembership1 = tableData.addNumericColumn(outputSettings.membershipPrefix + (i + 1));
            }
        }

        // Write memberships
        RealMatrix membershipMatrix = clusterer.getMembershipMatrix();
        for (int row = 0; row < tableData.getRowCount(); row++) {
            for (int col = 0; col < membershipMatrix.getColumnDimension(); col++) {
                tableData.setValueAt(membershipMatrix.getEntry(row, col), row, col + outputColumnMembership1);
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

    @SetJIPipeDocumentation(name = "K", description = "The number of clusters to split the data into")
    @JIPipeParameter(value = "k", important = true)
    public int getK() {
        return k;
    }

    @JIPipeParameter("k")
    public void setK(int k) {
        this.k = k;
    }

    @SetJIPipeDocumentation(name = "Fuzziness", description = "Determines the level of cluster fuzziness, larger values lead to fuzzier clusters")
    @JIPipeParameter(value = "fuzziness", important = true)
    public double getFuzziness() {
        return fuzziness;
    }

    @JIPipeParameter("fuzziness")
    public void setFuzziness(double fuzziness) {
        this.fuzziness = fuzziness;
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
        private double epsilon = 1e-3;

        public ClusteringSettings() {
        }

        public ClusteringSettings(ClusteringSettings other) {
            this.maxIterations = other.maxIterations;
            this.distanceMeasure = other.distanceMeasure;
            this.epsilon = other.epsilon;
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

        @SetJIPipeDocumentation(name = "Epsilon", description = "The convergence criteria, default is 1e-3")
        @JIPipeParameter("epsilon")
        public double getEpsilon() {
            return epsilon;
        }

        @JIPipeParameter("epsilon")
        public void setEpsilon(double epsilon) {
            this.epsilon = epsilon;
        }
    }

    public static class OutputSettings extends AbstractJIPipeParameterCollection {

        private OptionalStringParameter outputColumnClusterId = new OptionalStringParameter("best_cluster_id", true);
        private OptionalStringParameter outputColumnClusterSize = new OptionalStringParameter("best_cluster_size", true);
        private OptionalStringParameter outputColumnDistanceToCenter = new OptionalStringParameter("best_distance_to_cluster_center", true);
        private String membershipPrefix = "m.";

        public OutputSettings() {

        }

        public OutputSettings(OutputSettings other) {
            this.outputColumnClusterId = new OptionalStringParameter(other.outputColumnClusterId);
            this.outputColumnClusterSize = new OptionalStringParameter(other.outputColumnClusterSize);
            this.outputColumnDistanceToCenter = new OptionalStringParameter(other.outputColumnDistanceToCenter);
            this.membershipPrefix = other.membershipPrefix;
        }

        @SetJIPipeDocumentation(name = "Membership prefix", description = "Prefix for the membership columns that are added into the original table")
        @JIPipeParameter("membership-prefix")
        public String getMembershipPrefix() {
            return membershipPrefix;
        }

        @JIPipeParameter("membership-prefix")
        public void setMembershipPrefix(String membershipPrefix) {
            this.membershipPrefix = membershipPrefix;
        }

        @SetJIPipeDocumentation(name = "Cluster ID column", description = "The column where the best-matching cluster ID will be written to. The first index will be 1.")
        @JIPipeParameter(value = "output-column-cluster-id", important = true)
        @StringParameterSettings(monospace = true)
        public OptionalStringParameter getOutputColumnClusterId() {
            return outputColumnClusterId;
        }

        @JIPipeParameter("output-column-cluster-id")
        public void setOutputColumnClusterId(OptionalStringParameter outputColumnClusterId) {
            this.outputColumnClusterId = outputColumnClusterId;
        }

        @SetJIPipeDocumentation(name = "Cluster size column", description = "The column where the best-matching cluster size will be written to")
        @JIPipeParameter("output-column-cluster-size")
        @StringParameterSettings(monospace = true)
        public OptionalStringParameter getOutputColumnClusterSize() {
            return outputColumnClusterSize;
        }

        @JIPipeParameter("output-column-cluster-size")
        public void setOutputColumnClusterSize(OptionalStringParameter outputColumnClusterSize) {
            this.outputColumnClusterSize = outputColumnClusterSize;
        }

        @SetJIPipeDocumentation(name = "Distance to cluster center column", description = "The column where the distance to the best-matching cluster center will be written to")
        @JIPipeParameter("output-column-distance-to-cluster-center")
        @StringParameterSettings(monospace = true)
        public OptionalStringParameter getOutputColumnDistanceToCenter() {
            return outputColumnDistanceToCenter;
        }

        @JIPipeParameter("output-column-distance-to-cluster-center")
        public void setOutputColumnDistanceToCenter(OptionalStringParameter outputColumnDistanceToCenter) {
            this.outputColumnDistanceToCenter = outputColumnDistanceToCenter;
        }
    }
}
