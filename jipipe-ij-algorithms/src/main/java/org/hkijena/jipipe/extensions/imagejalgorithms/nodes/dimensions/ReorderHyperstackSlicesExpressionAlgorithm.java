package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.dimensions;

import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.Image5DExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Reorder hyperstack slices (Expression)", description = "Uses an expression that iterates through all hyperstack slices to assign a new location within the output hyperstack. " +
        "Can also filter slices based on the location.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class ReorderHyperstackSlicesExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;

    private DefaultExpressionParameter newLocationExpression = new DefaultExpressionParameter("ARRAY(c, z, t)");

    private boolean silentlyOverride = false;


    public ReorderHyperstackSlicesExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public ReorderHyperstackSlicesExpressionAlgorithm(ReorderHyperstackSlicesExpressionAlgorithm other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.newLocationExpression = new DefaultExpressionParameter(other.newLocationExpression);
        this.silentlyOverride = other.silentlyOverride;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        Map<ImageSliceIndex, ImageProcessor> slices = ImageJUtils.splitIntoSlices(inputImage);
        Map<ImageProcessor, ImageSliceIndex> newSliceIndices = new IdentityHashMap<>();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customVariables.writeToVariables(variables, true, "custom.", true, "custom");
        variables.set("width", inputImage.getWidth());
        variables.set("height", inputImage.getHeight());
        variables.set("num_c", inputImage.getNChannels());
        variables.set("num_z", inputImage.getNSlices());
        variables.set("num_t", inputImage.getNFrames());
        variables.set("num_d", inputImage.getNDimensions());

        for (Map.Entry<ImageSliceIndex, ImageProcessor> entry : slices.entrySet()) {
            variables.set("c", entry.getKey().getC());
            variables.set("z", entry.getKey().getZ());
            variables.set("t", entry.getKey().getT());

            Object result = newLocationExpression.evaluate(variables);
            ImageSliceIndex newIndex;
            if(result instanceof Boolean) {
                if(((Boolean) result).booleanValue()) {
                    newIndex = entry.getKey();
                }
                else {
                    continue;
                }
            }
            else if(result instanceof Collection) {
                ImmutableList<?> objects = ImmutableList.copyOf((Collection<?>) result);
                if(objects.isEmpty()) {
                    continue;
                }
                else {
                    newIndex = new ImageSliceIndex((int) StringUtils.objectToDouble(objects.get(0)),
                            (int) StringUtils.objectToDouble(objects.get(1)),
                            (int) StringUtils.objectToDouble(objects.get(2)));
                }
            }
            else {
                throw new UnsupportedOperationException("Unsupported expression result: " + result);
            }

            newSliceIndices.put(entry.getValue(), newIndex);
        }

        Map<ImageSliceIndex, ImageProcessor> sliceMapping = ImageJUtils.deduplicateSliceMappingByOverwriting(newSliceIndices, silentlyOverride);
        ImagePlus resultImage = ImageJUtils.combineSlices(sliceMapping);
        resultImage.copyScale(inputImage);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    @JIPipeDocumentation(name = "New slice location", description = "Expression that returns an array with the new Z, channel, and frame location (zero-based) of the slice. " +
            "If the expression returns a boolen TRUE, the original location is preserved. If the expression returns a boolean FALSE or an empty array, " +
            "the slice is discarded. Locations do not need to be consecutive and positive (compensation by the algorithm). Duplicate locations are not supported and will be overwritten by one of the " +
            "affected images.")
    @JIPipeParameter(value = "new-location-expression",important = true)
    @ExpressionParameterSettings(hint = "per slice ARRAY(c, z, t)")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(fromClass = Image5DExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "c", name = "Current channel location", description = "Current channel location of the slice (zero-based)")
    @ExpressionParameterSettingsVariable(key = "z", name = "Current Z location", description = "Current Z location of the slice (zero-based)")
    @ExpressionParameterSettingsVariable(key = "t", name = "Current frame location", description = "Current frame location of the slice (zero-based)")
    public DefaultExpressionParameter getNewLocationExpression() {
        return newLocationExpression;
    }

    @JIPipeParameter("new-location-expression")
    public void setNewLocationExpression(DefaultExpressionParameter newLocationExpression) {
        this.newLocationExpression = newLocationExpression;
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }

    @JIPipeDocumentation(name = "Silently override duplicate indices", description = "If enabled, duplicate plane indices are ignored and silently overriden")
    @JIPipeParameter("silently-override")
    public boolean isSilentlyOverride() {
        return silentlyOverride;
    }

    @JIPipeParameter("silently-override")
    public void setSilentlyOverride(boolean silentlyOverride) {
        this.silentlyOverride = silentlyOverride;
    }
}
