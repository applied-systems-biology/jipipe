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

package org.hkijena.jipipe.extensions.multiparameters.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterlessSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Map;

@SetJIPipeDocumentation(name = "Annotations to parameters", description = "Converts annotations into parameter data.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Parameters")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Output", create = true)
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

    @SetJIPipeDocumentation(name = "Extracted parameters", description = "Add parameter items into following list to extract them from annotations. " +
            "The unique parameter key is used as annotation name. If an annotation does not exist, the value defined here is used.")
    @JIPipeParameter("extracted-parameters")
    public JIPipeDynamicParameterCollection getExtractedParameters() {
        return extractedParameters;
    }

    @SetJIPipeDocumentation(name = "Ignore invalid values", description = "If enabled, invalid annotation values that cannot be loaded as parameters are ignored.")
    @JIPipeParameter("ignore-invalid-valid")
    public boolean isIgnoreInvalidValues() {
        return ignoreInvalidValues;
    }

    @JIPipeParameter("ignore-invalid-valid")
    public void setIgnoreInvalidValues(boolean ignoreInvalidValues) {
        this.ignoreInvalidValues = ignoreInvalidValues;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ParametersData data = new ParametersData();
        for (Map.Entry<String, JIPipeParameterAccess> entry : extractedParameters.getParameters().entrySet()) {
            JIPipeTextAnnotation annotation = iterationStep.getMergedTextAnnotation(entry.getKey());
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
                            throw new JIPipeValidationRuntimeException(e, "Cannot convert value to parameter!",
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
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }
}
