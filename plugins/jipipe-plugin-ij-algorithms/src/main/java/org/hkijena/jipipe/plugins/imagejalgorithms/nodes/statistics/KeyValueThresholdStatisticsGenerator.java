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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.statistics;

import com.google.common.primitives.Floats;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
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
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.Arrays;

@SetJIPipeDocumentation(name = "Threshold/Value statistics 5D", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. The values assigned to each key are collected and then integrated for each threshold in the key image.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Threshold", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Value", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class KeyValueThresholdStatisticsGenerator extends JIPipeIteratingAlgorithm {
    private String outputThresholdColumn = "threshold";
    private String outputForegroundColumn = "foreground";
    private String outputBackgroundColumn = "background";
    private JIPipeExpressionParameter integrationFunctionForeground = new JIPipeExpressionParameter("SUM(foreground_values)");

    private JIPipeExpressionParameter integrationFunctionBackground = new JIPipeExpressionParameter("SUM(background_values)");

    private boolean invertThreshold = false;

    public KeyValueThresholdStatisticsGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    public KeyValueThresholdStatisticsGenerator(KeyValueThresholdStatisticsGenerator other) {
        super(other);
        this.integrationFunctionForeground = new JIPipeExpressionParameter(other.integrationFunctionForeground);
        this.integrationFunctionBackground = new JIPipeExpressionParameter(other.integrationFunctionBackground);
        this.invertThreshold = other.invertThreshold;
        this.outputThresholdColumn = other.outputThresholdColumn;
        this.outputBackgroundColumn = other.outputBackgroundColumn;
        this.outputForegroundColumn = other.outputForegroundColumn;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = iterationStep.getInputData("Threshold", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = iterationStep.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        TDoubleObjectHashMap<TFloatList> bucketedValues = new TDoubleObjectHashMap<>();

        if (!ImageJUtils.imagesHaveSameSize(keyImage, valueImage)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        // Bucket the pixel values => which key has which values
        ImageJUtils.forEachIndexedZCTSlice(keyImage, (keyIp, index) -> {
            ImageProcessor valueIp = ImageJUtils.getSliceZero(valueImage, index);
            float[] keyPixels = (float[]) keyIp.getPixels();
            float[] valuePixels = (float[]) valueIp.getPixels();
            for (int i = 0; i < keyPixels.length; i++) {
                float key = keyPixels[i];
                float value = valuePixels[i];
                TFloatList list = bucketedValues.get(key);
                if (list == null) {
                    list = new TFloatArrayList();
                    bucketedValues.put(key, list);
                }
                list.add(value);
            }
        }, progressInfo.resolve("Collect pixels"));

        // Setup values
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        // Integrate buckets
        progressInfo.log("Processing " + bucketedValues.size() + " thresholds ...");
        TDoubleDoubleMap integratedValuesForeground = new TDoubleDoubleHashMap();
        TDoubleDoubleMap integratedValuesBackground = new TDoubleDoubleHashMap();
        TFloatList foregrounds = new TFloatArrayList();
        TFloatList backgrounds = new TFloatArrayList();

        long lastMessageTime = System.currentTimeMillis();
        double[] keys = bucketedValues.keys();
        for (int i = 0; i < keys.length; i++) {
            double threshold = keys[i];
            if (progressInfo.isCancelled())
                return;

            long currentMessageTime = System.currentTimeMillis();
            if (currentMessageTime - lastMessageTime > 1000) {
                lastMessageTime = currentMessageTime;
                progressInfo.log("Threshold " + threshold + " [" + i + "/" + keys.length + "]");
            }

            // Collect all values within the threshold limits from the bucket array
            foregrounds.clear();
            backgrounds.clear();
            for (double key : bucketedValues.keys()) {
                if (invertThreshold) {
                    if (key > threshold) {
                        backgrounds.addAll(bucketedValues.get(key));
                    } else {
                        foregrounds.addAll(bucketedValues.get(key));
                    }
                } else {
                    if (key > threshold) {
                        foregrounds.addAll(bucketedValues.get(key));
                    } else {
                        backgrounds.addAll(bucketedValues.get(key));
                    }
                }
            }

            variables.set("foreground_values", Floats.asList(foregrounds.toArray()));
            variables.set("background_values", Floats.asList(backgrounds.toArray()));

            double integratedForeground = integrationFunctionForeground.evaluateToDouble(variables);
            integratedValuesForeground.put(threshold, integratedForeground);
            double integratedBackground = integrationFunctionBackground.evaluateToDouble(variables);
            integratedValuesBackground.put(threshold, integratedBackground);
        }

        bucketedValues.clear();

        // Get sorted keys
        double[] sortedKeys = integratedValuesForeground.keys();
        Arrays.sort(sortedKeys);

        // Convert to table
        ResultsTableData outputTable = new ResultsTableData();
        int outputKeyColumnIndex = outputTable.addNumericColumn(outputThresholdColumn);
        int outputForegroundColumnIndex = outputTable.addNumericColumn(outputForegroundColumn);
        int outputBackgroundColumnIndex = outputTable.addNumericColumn(outputBackgroundColumn);
        for (double key : sortedKeys) {
            double foreground = integratedValuesForeground.get(key);
            double background = integratedValuesBackground.get(key);
            int row = outputTable.addRow();
            outputTable.setValueAt(key, row, outputKeyColumnIndex);
            outputTable.setValueAt(foreground, row, outputForegroundColumnIndex);
            outputTable.setValueAt(background, row, outputBackgroundColumnIndex);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Output column (keys)", description = "The table column where the keys will be written to")
    @JIPipeParameter(value = "output-key-column", uiOrder = 100)
    @StringParameterSettings(monospace = true)
    public String getOutputThresholdColumn() {
        return outputThresholdColumn;
    }

    @JIPipeParameter("output-key-column")
    public void setOutputThresholdColumn(String outputThresholdColumn) {
        this.outputThresholdColumn = outputThresholdColumn;
    }

    @SetJIPipeDocumentation(name = "Output column (integrated foreground)", description = "The table column where the integrated foreground values will be written to")
    @JIPipeParameter(value = "output-foreground-column", uiOrder = 110)
    @StringParameterSettings(monospace = true)
    public String getOutputForegroundColumn() {
        return outputForegroundColumn;
    }

    @JIPipeParameter("output-foreground-column")
    public void setOutputForegroundColumn(String outputForegroundColumn) {
        this.outputForegroundColumn = outputForegroundColumn;
    }

    @SetJIPipeDocumentation(name = "Output column (integrated background)", description = "The table column where the integrated background values will be written to")
    @JIPipeParameter(value = "output-background-column", uiOrder = 120)
    @StringParameterSettings(monospace = true)
    public String getOutputBackgroundColumn() {
        return outputBackgroundColumn;
    }

    @JIPipeParameter("output-background-column")
    public void setOutputBackgroundColumn(String outputBackgroundColumn) {
        this.outputBackgroundColumn = outputBackgroundColumn;
    }

    @SetJIPipeDocumentation(name = "Integration function (foreground)", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function-foreground")
    @AddJIPipeExpressionParameterVariable(key = "values_foreground", name = "Values (Foreground)", description = "The values to be integrated")
    @AddJIPipeExpressionParameterVariable(key = "values_background", name = "Values (Background)", description = "The values to be integrated")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getIntegrationFunctionForeground() {
        return integrationFunctionForeground;
    }

    @JIPipeParameter("integration-function-foreground")
    public void setIntegrationFunctionForeground(JIPipeExpressionParameter integrationFunctionForeground) {
        this.integrationFunctionForeground = integrationFunctionForeground;
    }

    @SetJIPipeDocumentation(name = "Integration function (background)", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function-background")
    @AddJIPipeExpressionParameterVariable(key = "values_foreground", name = "Values (Foreground)", description = "The values to be integrated")
    @AddJIPipeExpressionParameterVariable(key = "values_background", name = "Values (Background)", description = "The values to be integrated")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getIntegrationFunctionBackground() {
        return integrationFunctionBackground;
    }

    @JIPipeParameter("integration-function-background")
    public void setIntegrationFunctionBackground(JIPipeExpressionParameter integrationFunctionBackground) {
        this.integrationFunctionBackground = integrationFunctionBackground;
    }

    @SetJIPipeDocumentation(name = "Thresholding mode", description = "Determines how the thresholding behaves.")
    @JIPipeParameter("invert-threshold")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "value > threshold", falseLabel = "value < threshold")
    public boolean isInvertThreshold() {
        return invertThreshold;
    }

    @JIPipeParameter("invert-threshold")
    public void setInvertThreshold(boolean invertThreshold) {
        this.invertThreshold = invertThreshold;
    }
}
