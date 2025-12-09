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
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
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
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", optional = true, create = true)
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output", create = true)
public class CopyRoi2dAcrossZCTAlgorithm extends JIPipeIteratingAlgorithm {

    private HyperstackDimension dimension = HyperstackDimension.Frame;
    private OptionalJIPipeExpressionParameter filter = new OptionalJIPipeExpressionParameter(false, "roi.T <= 1");
    private JIPipeExpressionParameter locations = new JIPipeExpressionParameter("MAKE_SEQUENCE(1, num_t + 1)");
    private JIPipeExpressionParameter namingFunction = new JIPipeExpressionParameter("roi.name + \"_\" + roi.T");
    private OutputMode outputMode = OutputMode.Merge;
    private boolean measureInPhysicalUnits = true;

    public CopyRoi2dAcrossZCTAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CopyRoi2dAcrossZCTAlgorithm(CopyRoi2dAcrossZCTAlgorithm other) {
        super(other);
        this.dimension = other.dimension;
        this.locations = new JIPipeExpressionParameter(other.locations);
        this.filter = new OptionalJIPipeExpressionParameter(other.filter);
        this.namingFunction = new JIPipeExpressionParameter(other.namingFunction);
        this.outputMode = other.outputMode;
        this.measureInPhysicalUnits = other.measureInPhysicalUnits;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Roi2dListData inputs = iterationStep.getInputData("Input", Roi2dListData.class, progressInfo);
        Roi2dListData outputs;
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
        Roi2dListData sources = new Roi2dListData();
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
                outputs = new Roi2dListData(sources);
                break;
            case OnlyNew:
                outputs = new Roi2dListData();
                break;
            case OnlyNewAndOther:
                outputs = new Roi2dListData();
                for (Roi roi : inputs) {
                    if(!sources.contains(roi)) {
                        outputs.add(roi);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported output mode: " + outputMode);
        }

        Map<Roi, TIntObjectMap<Roi>> roisForLocationsMap = new HashMap<>();
        Multimap<Integer, Roi> newRoisForLocationsMap = HashMultimap.create();
        JIPipePercentageProgressInfo roisProgress = progressInfo.percentage("Processing ROIs");
        for (int i = 0; i < sources.size(); i++) {
            roisProgress.logPercentage(i, sources.size());
            Roi sourceRoi = sources.get(i);
            putRoiMeasurementsInVariables(i, measurements, variablesMap);

            // Preprocess requested locations
            List<Integer> rawRequestedLocations = locations.evaluateToDoubleList(variablesMap).stream().map(Double::intValue).toList();
            int[] requestedLocationsArray = Ints.toArray(rawRequestedLocations);
            Arrays.sort(requestedLocationsArray);

//            roisProgress.log("Will be expanded to " + rawRequestedLocations.size() + " locations (min: " + Doubles.min(requestedLocationsArray) + ", max: " + Doubles.max(requestedLocationsArray) + ")");

            TIntObjectMap<Roi> perLocation = new TIntObjectHashMap<>();
            roisForLocationsMap.put(sourceRoi, perLocation);

            if (dimension == HyperstackDimension.Frame) {
                copyAcrossFrame(sourceRoi, requestedLocationsArray, perLocation, newRoisForLocationsMap, variablesMap, outputs);
            } else if (dimension == HyperstackDimension.Channel) {
                copyAcrossChannel(sourceRoi, requestedLocationsArray, perLocation, newRoisForLocationsMap, variablesMap, outputs);
            } else if (dimension == HyperstackDimension.Depth) {
                copyAcrossDepth(sourceRoi, requestedLocationsArray, perLocation, newRoisForLocationsMap, variablesMap, outputs);
            } else {
                throw new RuntimeException("Unknown dimension: " + dimension);
            }
        }

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), outputs, progressInfo);
    }

    private void putRoiMeasurementsInVariables(int row, ResultsTableData measurements, JIPipeExpressionVariablesMap variablesMap) {
        for (int col = 0; col < measurements.getColumnCount(); col++) {
            variablesMap.put(measurements.getColumnName(col), measurements.getValueAt(row, col));
        }
    }

    private void copyAcrossDepth(Roi roi, int[] requestedLocationsArray, TIntObjectMap<Roi> perLocation, Multimap<Integer, Roi> newVerticesForLocationsMap, JIPipeExpressionVariablesMap variablesMap, Roi2dListData outputs) {
        for (int depth : requestedLocationsArray) {
            if (!perLocation.containsKey(depth)) {
                variablesMap.put("roi.Z", depth);
                String newName = namingFunction.evaluateToString(variablesMap);
                Roi copy = ImageJUtils.copyRoi(roi);
                copy.setName(newName);
                copy.setPosition(roi.getCPosition(), depth, roi.getTPosition());
                perLocation.put(depth, copy);
                newVerticesForLocationsMap.put(depth, copy);
                outputs.add(copy);
            }
        }
    }

    private void copyAcrossChannel(Roi roi, int[] requestedLocationsArray, TIntObjectMap<Roi> perLocation, Multimap<Integer, Roi> newVerticesForLocationsMap, JIPipeExpressionVariablesMap variablesMap, Roi2dListData outputs) {
        for (int channel : requestedLocationsArray) {
            if (!perLocation.containsKey(channel)) {
                variablesMap.put("roi.C", channel);
                String newName = namingFunction.evaluateToString(variablesMap);
                Roi copy = ImageJUtils.copyRoi(roi);
                copy.setName(newName);
                copy.setPosition(channel, roi.getZPosition(), roi.getTPosition());
                perLocation.put(channel, copy);
                newVerticesForLocationsMap.put(channel, copy);
                outputs.add(copy);
            }
        }
    }

    private void copyAcrossFrame(Roi roi, int[] requestedLocationsArray, TIntObjectMap<Roi> perLocation, Multimap<Integer, Roi> newVerticesForLocationsMap, JIPipeExpressionVariablesMap variablesMap, Roi2dListData outputs) {
        for (int frame : requestedLocationsArray) {
            if (!perLocation.containsKey(frame)) {
                variablesMap.put("roi.T", frame);
                String newName = namingFunction.evaluateToString(variablesMap);
                Roi copy = ImageJUtils.copyRoi(roi);
                copy.setName(newName);
                copy.setPosition(roi.getCPosition(), roi.getZPosition(), frame);
                perLocation.put(frame, copy);
                newVerticesForLocationsMap.put(frame, copy);
                outputs.add(copy);
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

    @SetJIPipeDocumentation(name = "Locations (in direction)", description = "Expression that determines the locations in the selected direction where the ROI will be present. " +
            "Please note that ROI locations are one-based, i.e. if a location is zero, it is interpreted to be located on all slices.")
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

    @SetJIPipeDocumentation(name = "Naming function", description = "Expression that determines the new name of the ROI. roi.Z/roi.C/roi.T are replaced by the new location depending on the direction.")
    @JIPipeParameter(value = "naming-function", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = ImageJMeasurementsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per ROI")
    public JIPipeExpressionParameter getNamingFunction() {
        return namingFunction;
    }

    @JIPipeParameter("naming-function")
    public void setNamingFunction(JIPipeExpressionParameter namingFunction) {
        this.namingFunction = namingFunction;
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
