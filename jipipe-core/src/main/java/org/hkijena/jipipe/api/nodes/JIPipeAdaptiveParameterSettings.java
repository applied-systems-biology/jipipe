/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings class that allows users to generate expressions with adaptive parameters
 */
public class JIPipeAdaptiveParameterSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean enabled = true;
    private StringQueryExpressionAndStringPairParameter.List overriddenParameters = new StringQueryExpressionAndStringPairParameter.List();
    private boolean attachParameterAnnotations = true;
    private boolean attachOnlyNonDefaultParameterAnnotations = true;
    private boolean parameterAnnotationsUseInternalNames = false;
    private String parameterAnnotationsPrefix = "";

    public JIPipeAdaptiveParameterSettings() {
    }

    public JIPipeAdaptiveParameterSettings(JIPipeAdaptiveParameterSettings other) {
        this.enabled = other.enabled;
        this.overriddenParameters = new StringQueryExpressionAndStringPairParameter.List(other.overriddenParameters);
        this.attachParameterAnnotations = other.attachParameterAnnotations;
        this.attachOnlyNonDefaultParameterAnnotations = other.attachOnlyNonDefaultParameterAnnotations;
        this.parameterAnnotationsUseInternalNames = other.parameterAnnotationsUseInternalNames;
        this.parameterAnnotationsPrefix = other.parameterAnnotationsPrefix;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Enable adaptive parameters", description = "If enabled, you can use custom expressions to generate parameters.")
    @JIPipeParameter("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JIPipeDocumentation(name = "Overridden parameters", description = "Here you can override the value of parameters based on annotations. The 'Value' settings should return a value for the parameter type or a JSON string that can be converted into a valid parameter value. " +
            "The 'Parameter key' setting determines to which parameter this value is written to.")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Parameter key")
    @StringParameterSettings(monospace = true)
    @JIPipeParameter("overridden-parameters")
    public StringQueryExpressionAndStringPairParameter.List getOverriddenParameters() {
        return overriddenParameters;
    }

    @JIPipeParameter("overridden-parameters")
    public void setOverriddenParameters(StringQueryExpressionAndStringPairParameter.List overriddenParameters) {
        this.overriddenParameters = overriddenParameters;
    }

    @JIPipeDocumentation(name = "Attach parameter annotations", description = "If multiple parameters are allowed, attach the parameter values as annotations.")
    @JIPipeParameter(value = "attach-parameter-annotations")
    public boolean isAttachParameterAnnotations() {
        return attachParameterAnnotations;
    }

    @JIPipeParameter("attach-parameter-annotations")
    public void setAttachParameterAnnotations(boolean attachParameterAnnotations) {
        this.attachParameterAnnotations = attachParameterAnnotations;
    }

    @JIPipeDocumentation(name = "Attach only non-default parameter annotations", description = "If multiple parameters are allowed, " +
            "attach only parameter annotations that have different values from the current settings. Requires 'Attach parameter annotations' to be enabled.")
    @JIPipeParameter(value = "attach-only-non-default-parameter-annotations")
    public boolean isAttachOnlyNonDefaultParameterAnnotations() {
        return attachOnlyNonDefaultParameterAnnotations;
    }

    @JIPipeParameter("attach-only-non-default-parameter-annotations")
    public void setAttachOnlyNonDefaultParameterAnnotations(boolean attachOnlyNonDefaultParameterAnnotations) {
        this.attachOnlyNonDefaultParameterAnnotations = attachOnlyNonDefaultParameterAnnotations;
    }

    @JIPipeDocumentation(name = "Parameter annotations use internal names", description = "Generated parameter annotations use their internal unique names.")
    @JIPipeParameter(value = "parameter-annotations-use-internal-names")
    public boolean isParameterAnnotationsUseInternalNames() {
        return parameterAnnotationsUseInternalNames;
    }

    @JIPipeParameter("parameter-annotations-use-internal-names")
    public void setParameterAnnotationsUseInternalNames(boolean parameterAnnotationsUseInternalNames) {
        this.parameterAnnotationsUseInternalNames = parameterAnnotationsUseInternalNames;
    }

    @JIPipeDocumentation(name = "Parameter annotation prefix", description = "Text prefixed to generated parameter annotations.")
    @JIPipeParameter(value = "parameter-annotations-prefix")
    @StringParameterSettings(monospace = true)
    public String getParameterAnnotationsPrefix() {
        return parameterAnnotationsPrefix;
    }

    @JIPipeParameter("parameter-annotations-prefix")
    public void setParameterAnnotationsPrefix(String parameterAnnotationsPrefix) {
        this.parameterAnnotationsPrefix = parameterAnnotationsPrefix;
    }

    @JIPipeDocumentation(name = "Add", description = "Adds an adaptive parameter.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/list-add.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/list-add.png")
    public void addAdaptiveParameterAssistant(JIPipeWorkbench parent) {
        TODO
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("<Annotations>",
                    "Data annotations are available as variables named after their column names (use Update Cache to find the list of annotations)",
                    ""));
            VARIABLES.add(new ExpressionParameterVariable("Default value",
                    "The default value of this parameter",
                    "default"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
