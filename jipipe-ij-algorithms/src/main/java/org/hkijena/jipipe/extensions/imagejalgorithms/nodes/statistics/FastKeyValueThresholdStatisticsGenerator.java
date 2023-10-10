package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.statistics;

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
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.FloatArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.enums.TableColumnIntegrationParameter;

import java.util.Arrays;

@JIPipeDocumentation(name = "Threshold/Value statistics 5D (fast)", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. The values assigned to each key are collected and then integrated for each threshold in the key image. This variant uses table column integration functions " +
        "that are faster than expression-based operations.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Threshold", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class FastKeyValueThresholdStatisticsGenerator extends JIPipeIteratingAlgorithm {
    private String outputThresholdColumn = "threshold";
    private String outputForegroundColumn = "foreground";
    private String outputBackgroundColumn = "background";
    private TableColumnIntegrationParameter integrationFunctionForeground = new TableColumnIntegrationParameter();

    private TableColumnIntegrationParameter integrationFunctionBackground = new TableColumnIntegrationParameter();

    private boolean invertThreshold = false;

    public FastKeyValueThresholdStatisticsGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    public FastKeyValueThresholdStatisticsGenerator(FastKeyValueThresholdStatisticsGenerator other) {
        super(other);
        this.integrationFunctionForeground = new TableColumnIntegrationParameter(other.integrationFunctionForeground);
        this.integrationFunctionBackground = new TableColumnIntegrationParameter(other.integrationFunctionBackground);
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

        // Integrate buckets
        progressInfo.log("Processing " + bucketedValues.size() + " thresholds ...");
        TDoubleDoubleMap integratedValuesForeground = new TDoubleDoubleHashMap();
        TDoubleDoubleMap integratedValuesBackground = new TDoubleDoubleHashMap();
        TFloatList foregrounds = new TFloatArrayList();
        TFloatList backgrounds = new TFloatArrayList();

        JIPipeExpressionRegistry.ColumnOperationEntry functionForeground = (JIPipeExpressionRegistry.ColumnOperationEntry) integrationFunctionForeground.getValue();
        JIPipeExpressionRegistry.ColumnOperationEntry functionBackground = (JIPipeExpressionRegistry.ColumnOperationEntry) integrationFunctionBackground.getValue();

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

            TableColumn foregroundColum = new FloatArrayTableColumn(foregrounds.toArray(), "Foreground");
            TableColumn backgroundColum = new FloatArrayTableColumn(backgrounds.toArray(), "Background");

            TableColumn foregroundResult = functionForeground.getOperation().apply(foregroundColum);
            TableColumn backgroundResult = functionBackground.getOperation().apply(backgroundColum);

            integratedValuesForeground.put(threshold, foregroundResult.getRows() > 0 ? foregroundResult.getRowAsDouble(0) : Float.NaN);
            integratedValuesBackground.put(threshold, backgroundResult.getRows() > 0 ? backgroundResult.getRowAsDouble(0) : Float.NaN);
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
    public TableColumnIntegrationParameter getIntegrationFunctionForeground() {
        return integrationFunctionForeground;
    }

    @JIPipeParameter("integration-function-foreground")
    public void setIntegrationFunctionForeground(TableColumnIntegrationParameter integrationFunctionForeground) {
        this.integrationFunctionForeground = integrationFunctionForeground;
    }

    @JIPipeDocumentation(name = "Integration function (background)", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function-background")
    public TableColumnIntegrationParameter getIntegrationFunctionBackground() {
        return integrationFunctionBackground;
    }

    @JIPipeParameter("integration-function-background")
    public void setIntegrationFunctionBackground(TableColumnIntegrationParameter integrationFunctionBackground) {
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
