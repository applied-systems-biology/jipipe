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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

/**
 * Groups parameter slot settings
 */
public class JIPipeParameterSlotAlgorithmSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private boolean hasParameterSlot = false;
    private boolean attachParameterAnnotations = true;
    private boolean attachOnlyNonDefaultParameterAnnotations = true;
    private boolean parameterAnnotationsUseInternalNames = false;
    private String parameterAnnotationsPrefix = "";

    public JIPipeParameterSlotAlgorithmSettings() {
    }

    public JIPipeParameterSlotAlgorithmSettings(JIPipeParameterSlotAlgorithmSettings other) {
        this.hasParameterSlot = other.hasParameterSlot;
        this.attachParameterAnnotations = other.attachParameterAnnotations;
        this.attachOnlyNonDefaultParameterAnnotations = other.attachOnlyNonDefaultParameterAnnotations;
        this.parameterAnnotationsUseInternalNames = other.parameterAnnotationsUseInternalNames;
        this.parameterAnnotationsPrefix = other.parameterAnnotationsPrefix;
    }

    @JIPipeDocumentation(name = "Multiple parameters", description = "If enabled, there will be an additional slot that consumes " +
            "parameter data sets. The algorithm then will be applied for each of this parameter sets.")
    @JIPipeParameter(value = "has-parameter-slot")
    public boolean isHasParameterSlot() {
        return hasParameterSlot;
    }

    @JIPipeParameter("has-parameter-slot")
    public void setHasParameterSlot(boolean hasParameterSlot) {
        this.hasParameterSlot = hasParameterSlot;
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
