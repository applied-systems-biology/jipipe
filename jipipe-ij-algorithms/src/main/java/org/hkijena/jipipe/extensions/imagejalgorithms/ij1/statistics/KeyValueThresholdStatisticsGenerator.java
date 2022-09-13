package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics;

import com.google.common.primitives.Floats;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Threshold/Value statistics 5D", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. The values assigned to each key are collected and then integrated for each threshold in the key image.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Threshold", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Histogram", autoCreate = true)
public class KeyValueThresholdStatisticsGenerator extends JIPipeIteratingAlgorithm {
    private String outputThresholdColumn = "threshold";
    private String outputForegroundColumn = "foreground";
    private String outputBackgroundColumn = "background";
    private DefaultExpressionParameter integrationFunctionForeground = new DefaultExpressionParameter("SUM(foreground_values)");

    private DefaultExpressionParameter integrationFunctionBackground = new DefaultExpressionParameter("SUM(background_values)");

    private boolean invertThreshold = false;

    public KeyValueThresholdStatisticsGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    public KeyValueThresholdStatisticsGenerator(KeyValueThresholdStatisticsGenerator other) {
        super(other);
        this.integrationFunctionForeground = new DefaultExpressionParameter(other.integrationFunctionForeground);
        this.integrationFunctionBackground = new DefaultExpressionParameter(other.integrationFunctionBackground);
        this.invertThreshold = other.invertThreshold;
        this.outputThresholdColumn = other.outputThresholdColumn;
        this.outputBackgroundColumn = other.outputBackgroundColumn;
        this.outputForegroundColumn = other.outputForegroundColumn;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = dataBatch.getInputData("Threshold", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = dataBatch.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        TDoubleObjectHashMap<TFloatList> bucketedValues = new TDoubleObjectHashMap<>();

        if (!ImageJUtils.imagesHaveSameSize(keyImage, valueImage)) {
            throw new UserFriendlyRuntimeException("Input images do not have the same size!",
                    "Input images do not have the same size!",
                    getDisplayName(),
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels.",
                    "Please check the input images.");
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
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

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

        dataBatch.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @JIPipeDocumentation(name = "Output column (keys)", description = "The table column where the keys will be written to")
    @JIPipeParameter(value = "output-key-column", uiOrder = 100)
    @StringParameterSettings(monospace = true)
    public String getOutputThresholdColumn() {
        return outputThresholdColumn;
    }

    @JIPipeParameter("output-key-column")
    public void setOutputThresholdColumn(String outputThresholdColumn) {
        this.outputThresholdColumn = outputThresholdColumn;
    }

    @JIPipeDocumentation(name = "Output column (integrated foreground)", description = "The table column where the integrated foreground values will be written to")
    @JIPipeParameter(value = "output-foreground-column", uiOrder = 110)
    @StringParameterSettings(monospace = true)
    public String getOutputForegroundColumn() {
        return outputForegroundColumn;
    }

    @JIPipeParameter("output-foreground-column")
    public void setOutputForegroundColumn(String outputForegroundColumn) {
        this.outputForegroundColumn = outputForegroundColumn;
    }

    @JIPipeDocumentation(name = "Output column (integrated background)", description = "The table column where the integrated background values will be written to")
    @JIPipeParameter(value = "output-background-column", uiOrder = 120)
    @StringParameterSettings(monospace = true)
    public String getOutputBackgroundColumn() {
        return outputBackgroundColumn;
    }

    @JIPipeParameter("output-background-column")
    public void setOutputBackgroundColumn(String outputBackgroundColumn) {
        this.outputBackgroundColumn = outputBackgroundColumn;
    }

    @JIPipeDocumentation(name = "Integration function (foreground)", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function-foreground")
    @ExpressionParameterSettingsVariable(key = "values_foreground", name = "Values (Foreground)", description = "The values to be integrated")
    @ExpressionParameterSettingsVariable(key = "values_background", name = "Values (Background)", description = "The values to be integrated")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getIntegrationFunctionForeground() {
        return integrationFunctionForeground;
    }

    @JIPipeParameter("integration-function-foreground")
    public void setIntegrationFunctionForeground(DefaultExpressionParameter integrationFunctionForeground) {
        this.integrationFunctionForeground = integrationFunctionForeground;
    }

    @JIPipeDocumentation(name = "Integration function (background)", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function-background")
    @ExpressionParameterSettingsVariable(key = "values_foreground", name = "Values (Foreground)", description = "The values to be integrated")
    @ExpressionParameterSettingsVariable(key = "values_background", name = "Values (Background)", description = "The values to be integrated")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getIntegrationFunctionBackground() {
        return integrationFunctionBackground;
    }

    @JIPipeParameter("integration-function-background")
    public void setIntegrationFunctionBackground(DefaultExpressionParameter integrationFunctionBackground) {
        this.integrationFunctionBackground = integrationFunctionBackground;
    }

    @JIPipeDocumentation(name = "Thresholding mode", description = "Determines how the thresholding behaves.")
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