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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.convert;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.CycleFinderAlgorithm;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentComponentVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Convert filament cycles to 2D ROI", description = "Finds all cycles in the input filament graph and converts each cycle into a 2D ROI. Please note that the Z position will be set to the median Z/C/T value of all points plus 1.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class ConvertFilamentCyclesToROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private CycleFinderAlgorithm cycleFinderAlgorithm = CycleFinderAlgorithm.PatonCycleBasis;
    private JIPipeExpressionParameter roiName = new JIPipeExpressionParameter("\"Cycle \" + cycle_index");


    public ConvertFilamentCyclesToROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentCyclesToROIAlgorithm(ConvertFilamentCyclesToROIAlgorithm other) {
        super(other);
        this.cycleFinderAlgorithm = other.cycleFinderAlgorithm;
        this.roiName = new JIPipeExpressionParameter(other.roiName);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        ROI2DListData outputData = new ROI2DListData();
        progressInfo.log("Finding cycles ...");
        Set<List<FilamentEdge>> cycles = cycleFinderAlgorithm.findCycles(inputData);
        progressInfo.log("Detected " + cycles.size() + " cycles");

        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());

        String consensusPhysicalSizeUnit = inputData.getConsensusPhysicalSizeUnit();

        int componentId = 0;
        for (List<FilamentEdge> cycle : cycles) {

            if(cycle.isEmpty()) {
                continue;
            }

            // Collect points
            Set<FilamentVertex> vertices = new HashSet<>();
            TFloatList xPoints = new TFloatArrayList();
            TFloatList yPoints = new TFloatArrayList();
            TIntList zPoints = new TIntArrayList();
            TIntList cPoints = new TIntArrayList();
            TIntList tPoints = new TIntArrayList();

            for (FilamentEdge edge : cycle) {
                FilamentVertex edgeSource = inputData.getEdgeSource(edge);
                xPoints.add((float) edgeSource.getSpatialLocation().getX());
                yPoints.add((float) edgeSource.getSpatialLocation().getY());
                zPoints.add((int) edgeSource.getSpatialLocation().getZ());
                cPoints.add(edgeSource.getNonSpatialLocation().getChannel());
                tPoints.add(edgeSource.getNonSpatialLocation().getFrame());
                vertices.add(edgeSource);
            }

            zPoints.sort();
            cPoints.sort();
            tPoints.sort();

            int medianZ = zPoints.get(zPoints.size() / 2);
            int medianC = zPoints.get(cPoints.size() / 2);
            int medianT = tPoints.get(cPoints.size() / 2);

            // Write measurements into variables
            ResultsTableData measurements = new ResultsTableData();
            inputData.measureComponent(measurements, consensusPhysicalSizeUnit, vertices);
            for (int col = 0; col < measurements.getColumnCount(); col++) {
                variablesMap.set(measurements.getColumnName(col), measurements.getValueAt(0, col));
            }
            variablesMap.set("cycle_id", componentId);

            // Create ROI
            PolygonRoi roi = new PolygonRoi(xPoints.toArray(), yPoints.toArray(), xPoints.size(), Roi.POLYGON);
            roi.setPosition(Math.max(0, medianC + 1), Math.max(0, medianZ + 1), Math.max(0, medianT + 1));
            roi.setName(roiName.evaluateToString(variablesMap));

            outputData.add(roi);

            componentId++;

        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Cycle finder algorithm", description = "The algorithm used for finding cycles. See https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/alg/cycle/package-summary.html for more information.")
    @JIPipeParameter("cycle-finder")
    public CycleFinderAlgorithm getCycleFinderAlgorithm() {
        return cycleFinderAlgorithm;
    }

    @JIPipeParameter("cycle-finder")
    public void setCycleFinderAlgorithm(CycleFinderAlgorithm cycleFinderAlgorithm) {
        this.cycleFinderAlgorithm = cycleFinderAlgorithm;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Expression that determines the ROI name")
    @JIPipeParameter("roi-name")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "cycle_index", name = "Cycle index", description = "The index of the cycle")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentComponentVariablesInfo.class)
    public JIPipeExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(JIPipeExpressionParameter roiName) {
        this.roiName = roiName;
    }
}
