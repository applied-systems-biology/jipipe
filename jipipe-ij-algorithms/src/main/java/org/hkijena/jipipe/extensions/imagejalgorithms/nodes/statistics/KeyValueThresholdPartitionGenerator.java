package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.statistics;

import com.google.common.primitives.Floats;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.ImageROITargetArea;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Key/Value threshold statistics 5D", description = "This node consumes two images with the same dimensions that respectively contain the keys and value components of each pixel position. " +
        "The set of value pixels is partitioned into two sets based on whether the key is lower, or equal/higher than the currently processed key. " +
        "One or multiple values can be created for each partitioning.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Statistics")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Key", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Value", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class KeyValueThresholdPartitionGenerator extends JIPipeIteratingAlgorithm {
    private ImageROITargetArea sourceArea = ImageROITargetArea.WholeImage;
    private ParameterCollectionList generatedColumns = ParameterCollectionList.containingCollection(GeneratedColumn.class);

    public KeyValueThresholdPartitionGenerator(JIPipeNodeInfo info) {
        super(info);
        generatedColumns.addFromTemplate(new GeneratedColumn("Key", new JIPipeExpressionParameter("key")));
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public KeyValueThresholdPartitionGenerator(KeyValueThresholdPartitionGenerator other) {
        super(other);
        this.generatedColumns = new ParameterCollectionList(other.generatedColumns);
        this.sourceArea = other.sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus keyImage = iterationStep.getInputData("Key", ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus valueImage = iterationStep.getInputData("Value", ImagePlusGreyscale32FData.class, progressInfo).getImage();

        ROIListData roiInput = null;
        ImagePlus maskInput = null;

        switch (sourceArea) {
            case InsideRoi:
            case OutsideRoi:
                roiInput = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
                break;
            case InsideMask:
            case OutsideMask:
                maskInput = iterationStep.getInputData("Mask", ImagePlusGreyscaleMaskData.class, progressInfo).getImage();
                break;
        }

        ROIListData finalRoiInput = roiInput;
        ImagePlus finalMaskInput = maskInput;

        List<Float> allValues = new ArrayList<>();
        List<Float> allKeys = new ArrayList<>();
        TDoubleObjectHashMap<TFloatList> bucketedValues = new TDoubleObjectHashMap<>();

        if (!ImageJUtils.imagesHaveSameSize(keyImage, valueImage)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        // Bucket the pixel values
        ImageJUtils.forEachIndexedZCTSlice(keyImage, (keyIp, index) -> {
            ImageProcessor mask = getMask(keyIp.getWidth(),
                    keyIp.getHeight(),
                    finalRoiInput,
                    finalMaskInput,
                    index
            );

            ImageProcessor valueIp = ImageJUtils.getSliceZero(valueImage, index);
            byte[] maskPixels = mask != null ? (byte[]) mask.getPixels() : null;
            float[] keyPixels = (float[]) keyIp.getPixels();
            float[] valuePixels = (float[]) valueIp.getPixels();
            for (int i = 0; i < keyPixels.length; i++) {
                if (mask == null || Byte.toUnsignedInt(maskPixels[i]) > 0) {
                    float key = keyPixels[i];
                    float value = valuePixels[i];
                    TFloatList list = bucketedValues.get(key);
                    if (list == null) {
                        list = new TFloatArrayList();
                        bucketedValues.put(key, list);
                    }
                    list.add(value);
                    allValues.add(value);
                    allKeys.add(key);
                }
            }
        }, progressInfo.resolve("Collect pixels"));

        // Setup values
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.set("all.values", allValues);
        variables.set("all.keys", allKeys);

        // Get sorted keys
        double[] sortedKeys = bucketedValues.keys();
        Arrays.sort(sortedKeys);

        List<GeneratedColumn> mappedGeneratedColumns = generatedColumns.mapToCollection(GeneratedColumn.class);

        progressInfo.log("Processing " + sortedKeys.length + " keys ...");
        // Convert to table
        List<Float> class0 = new ArrayList<>();
        List<Float> class1 = new ArrayList<>();
        ResultsTableData outputTable = new ResultsTableData();

        long lastProgressUpdate = System.currentTimeMillis();

        for (int i = 0; i < sortedKeys.length; i++) {
            double key = sortedKeys[i];

            if (progressInfo.isCancelled())
                return;

            variables.set("key", key);
            class0.clear();
            class1.clear();
            for (double bucketedKey : bucketedValues.keys()) {
                if (bucketedKey < key) {
                    class0.addAll(Floats.asList(bucketedValues.get(bucketedKey).toArray()));
                } else {
                    class1.addAll(Floats.asList(bucketedValues.get(bucketedKey).toArray()));
                }
            }
            variables.set("class0", class0);
            variables.set("class1", class1);

            int row = outputTable.addRow();
            for (GeneratedColumn generatedColumn : mappedGeneratedColumns) {
                if (generatedColumn.skipEmpty && (class0.isEmpty() || class1.isEmpty()))
                    continue;
                Object evaluated = generatedColumn.value.evaluate(variables);
                outputTable.setValueAt(evaluated, row, generatedColumn.name);
            }

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastProgressUpdate) > 3000) {
                progressInfo.resolveAndLog(Math.round((1.0 * i / sortedKeys.length) * 100) + "%", i, sortedKeys.length);
                lastProgressUpdate = currentTime;
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    private ImageProcessor getMask(int width, int height, ROIListData rois, ImagePlus mask, ImageSliceIndex sliceIndex) {
        return ImageJAlgorithmUtils.getMaskProcessorFromMaskOrROI(sourceArea, width, height, rois, mask, sliceIndex);
    }

    @SetJIPipeDocumentation(name = "Generated columns", description = "The list of generated columns")
    @JIPipeParameter("generated-columns")
    @ParameterCollectionListTemplate(GeneratedColumn.class)
    public ParameterCollectionList getGeneratedColumns() {
        return generatedColumns;
    }

    @JIPipeParameter("generated-columns")
    public void setGeneratedColumns(ParameterCollectionList generatedColumns) {
        this.generatedColumns = generatedColumns;
    }

    @SetJIPipeDocumentation(name = "Extract values from ...", description = "Determines from which image areas the pixel values used for extracting the values")
    @JIPipeParameter("source-area")
    public ImageROITargetArea getSourceArea() {
        return sourceArea;
    }

    @JIPipeParameter("source-area")
    public void setSourceArea(ImageROITargetArea sourceArea) {
        this.sourceArea = sourceArea;
        ImageJAlgorithmUtils.updateROIOrMaskSlot(sourceArea, getSlotConfiguration());
    }

    public static class GeneratedColumn extends AbstractJIPipeParameterCollection {
        private String name;
        private JIPipeExpressionParameter value = new JIPipeExpressionParameter();

        private boolean skipEmpty = false;

        public GeneratedColumn() {
        }

        public GeneratedColumn(String name, JIPipeExpressionParameter value) {
            this.name = name;
            this.value = value;
        }

        public GeneratedColumn(GeneratedColumn other) {
            this.name = other.name;
            this.value = other.value;
            this.skipEmpty = other.skipEmpty;
        }

        @SetJIPipeDocumentation(name = "Name")
        @JIPipeParameter("name")
        public String getName() {
            return name;
        }

        @JIPipeParameter("name")
        public void setName(String name) {
            this.name = name;
        }

        @SetJIPipeDocumentation(name = "Value")
        @JIPipeParameter("value")
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "key", name = "Key", description = "The current key/threshold")
        @JIPipeExpressionParameterVariable(key = "class0", name = "Class 0", description = "Array of all value pixels with a key less than the current threshold")
        @JIPipeExpressionParameterVariable(key = "class1", name = "Class 1", description = "Array of all value pixels with a key larger or equal to the current threshold")
        @JIPipeExpressionParameterVariable(key = "all.values", name = "All values", description = "Array of all values")
        @JIPipeExpressionParameterVariable(key = "all.keys", name = "All keys", description = "Array of all keys")
        public JIPipeExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(JIPipeExpressionParameter value) {
            this.value = value;
        }

        @SetJIPipeDocumentation(name = "Skip if class0 or class1 are empty")
        @JIPipeParameter("skip-empty")
        public boolean isSkipEmpty() {
            return skipEmpty;
        }

        @JIPipeParameter("skip-empty")
        public void setSkipEmpty(boolean skipEmpty) {
            this.skipEmpty = skipEmpty;
        }
    }
}
