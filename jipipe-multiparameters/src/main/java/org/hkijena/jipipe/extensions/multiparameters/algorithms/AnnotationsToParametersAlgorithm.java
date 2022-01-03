package org.hkijena.jipipe.extensions.multiparameters.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterlessSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Map;

@JIPipeDocumentation(name = "Annotations to parameters", description = "Converts annotations into parameter data.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Parameters")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Output", autoCreate = true)
public class AnnotationsToParametersAlgorithm extends JIPipeParameterlessSimpleIteratingAlgorithm {

    private JIPipeDynamicParameterCollection extractedParameters = new JIPipeDynamicParameterCollection(true,
            JIPipe.getParameterTypes().getRegisteredParameters().values());
    private boolean ignoreInvalidValues = false;

    public AnnotationsToParametersAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(extractedParameters);
    }

    public AnnotationsToParametersAlgorithm(AnnotationsToParametersAlgorithm other) {
        super(other);
        this.extractedParameters = new JIPipeDynamicParameterCollection(other.extractedParameters);
        this.ignoreInvalidValues = other.ignoreInvalidValues;
        registerSubParameter(extractedParameters);
    }

    @JIPipeDocumentation(name = "Extracted parameters", description = "Add parameter items into following list to extract them from annotations. " +
            "The unique parameter key is used as annotation name. If an annotation does not exist, the value defined here is used.")
    @JIPipeParameter("extracted-parameters")
    public JIPipeDynamicParameterCollection getExtractedParameters() {
        return extractedParameters;
    }

    @JIPipeDocumentation(name = "Ignore invalid values", description = "If enabled, invalid annotation values that cannot be loaded as parameters are ignored.")
    @JIPipeParameter("ignore-invalid-valid")
    public boolean isIgnoreInvalidValues() {
        return ignoreInvalidValues;
    }

    @JIPipeParameter("ignore-invalid-valid")
    public void setIgnoreInvalidValues(boolean ignoreInvalidValues) {
        this.ignoreInvalidValues = ignoreInvalidValues;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ParametersData data = new ParametersData();
        for (Map.Entry<String, JIPipeParameterAccess> entry : extractedParameters.getParameters().entrySet()) {
            JIPipeAnnotation annotation = dataBatch.getMergedAnnotation(entry.getKey());
            if (annotation != null) {
                try {
                    Object value = JsonUtils.getObjectMapper().readerFor(entry.getValue().getFieldClass()).readValue(annotation.getValue());
                    data.getParameterData().put(entry.getKey(), value);
                } catch (JsonProcessingException e) {

                    // Maybe we need to add quotes
                    try {
                        Object value = JsonUtils.getObjectMapper().readerFor(entry.getValue().getFieldClass()).readValue("\"" + annotation.getValue() + "\"");
                        data.getParameterData().put(entry.getKey(), value);
                    } catch (JsonProcessingException e1) {
                        if (ignoreInvalidValues) {
                            progressInfo.log("Cannot load parameter of type " + entry.getValue().getFieldClass().getCanonicalName() + " from " + annotation.getValue());
                        } else {
                            throw new UserFriendlyRuntimeException(e, "Cannot convert value to parameter!",
                                    getName(),
                                    String.format("The node attempted to load a parameter from an annotation '%s' with value '%s', but this value is not valid.", entry.getKey(), annotation.getValue()),
                                    String.format("Check if the parameter type of the unique key '%s' is correct.", entry.getKey()));
                        }
                    }
                }
            } else {
                Object value = entry.getValue().get(Object.class);
                // Duplicate the value
                value = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()).duplicate(value);
                data.getParameterData().put(entry.getKey(), value);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }
}
