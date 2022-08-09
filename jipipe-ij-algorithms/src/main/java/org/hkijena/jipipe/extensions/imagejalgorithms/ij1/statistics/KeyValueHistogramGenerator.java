package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.statistics;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TDoubleArrayList;
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
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.CoreImageJUtils;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Key/Value Histogram 5D", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. The values assigned to each key are collected and integrated, thus allowing to generate histograms. Allows the generation of normalized and cumulative histograms.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Key", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Histogram", autoCreate = true)
public class KeyValueHistogramGenerator extends JIPipeIteratingAlgorithm {
    private String outputKeyColumn = "key";

    private String outputValueColumn = "value";
    private DefaultExpressionParameter integrationFunction = new DefaultExpressionParameter("SUM(values)");
    private boolean cumulative = false;
    private boolean normalize = false;

    public KeyValueHistogramGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    public KeyValueHistogramGenerator(KeyValueHistogramGenerator other) {
        super(other);
        this.integrationFunction = new DefaultExpressionParameter(other.integrationFunction);
        this.cumulative = other.cumulative;
        this.normalize = other.normalize;
        this.outputKeyColumn = other.outputKeyColumn;
        this.outputValueColumn = other.outputValueColumn;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = dataBatch.getInputData("Key", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = dataBatch.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        TDoubleObjectHashMap<TFloatList> bucketedValues = new TDoubleObjectHashMap<>();

        if(!ImageJUtils.imagesHaveSameSize(keyImage, valueImage)) {
            throw new UserFriendlyRuntimeException("Input images do not have the same size!",
                    "Input images do not have the same size!",
                    getDisplayName(),
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels.",
                    "Please check the input images.");
        }

        // Bucket the pixel values
        ImageJUtils.forEachIndexedZCTSlice(keyImage, (keyIp, index) -> {
            ImageProcessor valueIp = ImageJUtils.getSliceZero(valueImage, index);
            float[] keyPixels = (float[]) keyIp.getPixels();
            float[] valuePixels = (float[]) valueIp.getPixels();
            for (int i = 0; i < keyPixels.length; i++) {
                float key = keyPixels[i];
                float value = valuePixels[i];
                TFloatList list = bucketedValues.get(key);
                if(list == null) {
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
        progressInfo.log("Integrating " + bucketedValues.size() + " buckets ...");
        TDoubleDoubleMap integratedValues = new TDoubleDoubleHashMap();
        for (double key : bucketedValues.keys()) {
            if(progressInfo.isCancelled())
                return;
            TFloatList list = bucketedValues.get(key);
            List<Float> asList = Floats.asList(list.toArray());
            variables.set("values", asList);
            double integrated = integrationFunction.evaluateToDouble(variables);
            integratedValues.put(key, integrated);
        }

        // Get sorted keys
        double[] sortedKeys = integratedValues.keys();
        Arrays.sort(sortedKeys);

        // Cumulative if enabled
        if(cumulative) {
            TDoubleDoubleMap cumulativeIntegratedValues = new TDoubleDoubleHashMap();
            double cumulativeValue = 0;
            for (double key : sortedKeys) {
                if(progressInfo.isCancelled())
                    return;
                double value = integratedValues.get(key);
                double value_ = value + cumulativeValue;
                cumulativeValue += value;
                cumulativeIntegratedValues.put(key, value_);
            }
            integratedValues = cumulativeIntegratedValues;
        }

        // Normalize if enabled
        if(normalize) {
            double max = Double.NEGATIVE_INFINITY;
            for (double value : integratedValues.values()) {
                max = Math.max(max, value);
            }
            for (double key : integratedValues.keys()) {
                integratedValues.put(key, integratedValues.get(key) / max);
            }
        }

        // Convert to table
        ResultsTableData outputTable = new ResultsTableData();
        int outputKeyColumnIndex = outputTable.addNumericColumn(outputKeyColumn);
        int outputValueColumnIndex = outputTable.addNumericColumn(outputValueColumn);
        for (double key : sortedKeys) {
            double value = integratedValues.get(key);
            int row = outputTable.addRow();
            outputTable.setValueAt(key, row, outputKeyColumnIndex);
            outputTable.setValueAt(value, row, outputValueColumnIndex);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @JIPipeDocumentation(name = "Output column (keys)", description = "The table column where the keys will be written to")
    @JIPipeParameter(value = "output-key-column", uiOrder = 100)
    @StringParameterSettings(monospace = true)
    public String getOutputKeyColumn() {
        return outputKeyColumn;
    }

    @JIPipeParameter("output-key-column")
    public void setOutputKeyColumn(String outputKeyColumn) {
        this.outputKeyColumn = outputKeyColumn;
    }

    @JIPipeDocumentation(name = "Output column (integrated values)", description = "The table column where the integrated values will be written to")
    @JIPipeParameter(value = "output-value-column", uiOrder = 110)
    @StringParameterSettings(monospace = true)
    public String getOutputValueColumn() {
        return outputValueColumn;
    }

    @JIPipeParameter("output-value-column")
    public void setOutputValueColumn(String outputValueColumn) {
        this.outputValueColumn = outputValueColumn;
    }

    @JIPipeDocumentation(name = "Integration function", description = "The function that integrates the values assigned to the same key")
    @JIPipeParameter("integration-function")
    @ExpressionParameterSettingsVariable(key = "values", name = "Values", description = "The values to be integrated")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getIntegrationFunction() {
        return integrationFunction;
    }

    @JIPipeParameter("integration-function")
    public void setIntegrationFunction(DefaultExpressionParameter integrationFunction) {
        this.integrationFunction = integrationFunction;
    }

    @JIPipeDocumentation(name = "Cumulative", description = "If enabled, the histogram will be cumulative")
    @JIPipeParameter("cumulative")
    public boolean isCumulative() {
        return cumulative;
    }

    @JIPipeParameter("cumulative")
    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }

    @JIPipeDocumentation(name = "Normalize", description = "If enabled, normalizes the values")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }
}
