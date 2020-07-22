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
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link JIPipeAlgorithm} that has an optional slot that allows to supply parameter data sets.
 */
public abstract class JIPipeParameterSlotAlgorithm extends JIPipeAlgorithm {

    public static final String SLOT_PARAMETERS = "Parameters";
    private ParameterSlotAlgorithmSettings parameterSlotAlgorithmSettings = new ParameterSlotAlgorithmSettings();

    public JIPipeParameterSlotAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        registerParameterSettings();
    }

    public JIPipeParameterSlotAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerParameterSettings();
    }

    public JIPipeParameterSlotAlgorithm(JIPipeParameterSlotAlgorithm other) {
        super(other);
        this.parameterSlotAlgorithmSettings = new ParameterSlotAlgorithmSettings(other.parameterSlotAlgorithmSettings);
        registerParameterSettings();
    }

    private void registerParameterSettings() {
        this.parameterSlotAlgorithmSettings.getEventBus().register(new Object() {
            @Subscribe
            public void onParametersChanged(ParameterChangedEvent event) {
                if ("has-parameter-slot".equals(event.getKey())) {
                    updateParameterSlot();
                }
            }
        });
        registerSubParameter(parameterSlotAlgorithmSettings);
    }

    /**
     * Returns the number of input slots that are not parameter slots.
     *
     * @return the number of input slots that are not parameter slots.
     */
    public int getEffectiveInputSlotCount() {
        int effectiveSlotSize = getInputSlots().size();
        if (parameterSlotAlgorithmSettings.isHasParameterSlot())
            --effectiveSlotSize;
        return effectiveSlotSize;
    }

    @Override
    public JIPipeDataSlot getFirstInputSlot() {
        JIPipeDataSlot firstInputSlot = super.getFirstInputSlot();
        if (Objects.equals(firstInputSlot.getName(), SLOT_PARAMETERS)) {
            return getInputSlots().get(1);
        } else {
            return firstInputSlot;
        }
    }

    @JIPipeDocumentation(name = "Multi-parameter settings", description = "This algorithm supports running with multiple parameter sets. Just enable 'Multiple parameters' and " +
            "connect parameter data to the newly created slot. The algorithm is then automatically repeated for all parameter sets.")
    @JIPipeParameter(value = "jipipe:parameter-slot-algorithm", visibility = JIPipeParameterVisibility.Visible)
    public ParameterSlotAlgorithmSettings getParameterSlotAlgorithmSettings() {
        return parameterSlotAlgorithmSettings;
    }

    /**
     * Returns the parameter slot if enabled
     *
     * @return the parameter slot
     */
    public JIPipeDataSlot getParameterSlot() {
        if (parameterSlotAlgorithmSettings.hasParameterSlot)
            return getInputSlot(SLOT_PARAMETERS);
        else
            return null;
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (isPassThrough() && canPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }
        if (parameterSlotAlgorithmSettings.hasParameterSlot) {
            JIPipeDataSlot parameterSlot = getInputSlot(SLOT_PARAMETERS);
            if (parameterSlot.getRowCount() == 0) {
                algorithmProgress.accept(subProgress.resolve("No parameters were passed with enabled parameter slot. Applying default parameters, only."));
                runParameterSet(subProgress, algorithmProgress, isCancelled, Collections.emptyList());
            } else {
                // Create backups
                Map<String, Object> parameterBackups = new HashMap<>();
                JIPipeParameterTree tree = new JIPipeParameterTree(this);
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
                }

                // Collect different parameters
                Set<String> nonDefaultParameters = new HashSet<>();
                for (int row = 0; row < parameterSlot.getRowCount(); row++) {
                    ParametersData data = parameterSlot.getData(row, ParametersData.class);
                    for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
                        JIPipeParameterAccess target = tree.getParameters().getOrDefault(entry.getKey(), null);
                        if (target != null) {
                            Object existing = target.get(Object.class);
                            Object newValue = entry.getValue();
                            if (!Objects.equals(existing, newValue)) {
                                nonDefaultParameters.add(entry.getKey());
                            }
                        }
                    }
                }

                // Create run
                for (int row = 0; row < parameterSlot.getRowCount(); row++) {
                    ParametersData data = parameterSlot.getData(row, ParametersData.class);
                    List<JIPipeAnnotation> annotations = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
                        JIPipeParameterAccess target = tree.getParameters().getOrDefault(entry.getKey(), null);
                        if (target == null) {
                            algorithmProgress.accept(subProgress.resolve("Unable to find parameter '" + entry.getKey() + "' in " + getName() + "! Ignoring."));
                            continue;
                        }
                        target.set(entry.getValue());
                    }
                    if (parameterSlotAlgorithmSettings.attachParameterAnnotations) {
                        for (String key : nonDefaultParameters) {
                            JIPipeParameterAccess target = tree.getParameters().get(key);
                            String annotationName;
                            if (parameterSlotAlgorithmSettings.parameterAnnotationsUseInternalNames) {
                                annotationName = key;
                            } else {
                                annotationName = target.getName();
                            }
                            annotationName = parameterSlotAlgorithmSettings.parameterAnnotationsPrefix + annotationName;
                            annotations.add(new JIPipeAnnotation(annotationName, "" + target.get(Object.class)));
                        }
                    }


                    runParameterSet(subProgress.resolve("Parameter set " + (row + 1) + " / " + parameterSlot.getRowCount()), algorithmProgress, isCancelled, annotations);
                }

                // Restore backup
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    entry.getValue().set(parameterBackups.get(entry.getKey()));
                }
            }
        } else {
            runParameterSet(subProgress, algorithmProgress, isCancelled, Collections.emptyList());
        }
    }

    /**
     * Runs a parameter set iteration
     *
     * @param subProgress          the progress
     * @param algorithmProgress    the progress consumer
     * @param isCancelled          if the user requested cancellation
     * @param parameterAnnotations parameter annotations
     */
    public abstract void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations);

    private void updateParameterSlot() {
        if (getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
            if (parameterSlotAlgorithmSettings.hasParameterSlot) {
                JIPipeDataSlotInfo existing = slotConfiguration.getInputSlots().getOrDefault(SLOT_PARAMETERS, null);
                if (existing != null && existing.getDataClass() != ParametersData.class) {
                    slotConfiguration.removeInputSlot(SLOT_PARAMETERS, false);
                    existing = null;
                }
                if (existing == null) {
                    slotConfiguration.addSlot(SLOT_PARAMETERS,
                            new JIPipeDataSlotInfo(ParametersData.class, JIPipeSlotType.Input, null),
                            false);
                }
            } else {
                slotConfiguration.removeInputSlot(SLOT_PARAMETERS, false);
            }
        }
    }

    /**
     * Groups parameter slot settings
     */
    public static class ParameterSlotAlgorithmSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private boolean hasParameterSlot = false;
        private boolean attachParameterAnnotations = true;
        private boolean attachOnlyNonDefaultParameterAnnotations = true;
        private boolean parameterAnnotationsUseInternalNames = false;
        private String parameterAnnotationsPrefix = "";

        public ParameterSlotAlgorithmSettings() {
        }

        public ParameterSlotAlgorithmSettings(ParameterSlotAlgorithmSettings other) {
            this.hasParameterSlot = other.hasParameterSlot;
            this.attachParameterAnnotations = other.attachParameterAnnotations;
            this.attachOnlyNonDefaultParameterAnnotations = other.attachOnlyNonDefaultParameterAnnotations;
            this.parameterAnnotationsUseInternalNames = other.parameterAnnotationsUseInternalNames;
            this.parameterAnnotationsPrefix = other.parameterAnnotationsPrefix;
        }

        @JIPipeDocumentation(name = "Multiple parameters", description = "If enabled, there will be an additional slot that consumes " +
                "parameter data sets. The algorithm then will be applied for each of this parameter sets.")
        @JIPipeParameter(value = "has-parameter-slot", visibility = JIPipeParameterVisibility.Visible)
        public boolean isHasParameterSlot() {
            return hasParameterSlot;
        }

        @JIPipeParameter("has-parameter-slot")
        public void setHasParameterSlot(boolean hasParameterSlot) {
            this.hasParameterSlot = hasParameterSlot;
        }

        @JIPipeDocumentation(name = "Attach parameter annotations", description = "If multiple parameters are allowed, attach the parameter values as annotations.")
        @JIPipeParameter(value = "attach-parameter-annotations", visibility = JIPipeParameterVisibility.Visible)
        public boolean isAttachParameterAnnotations() {
            return attachParameterAnnotations;
        }

        @JIPipeParameter("attach-parameter-annotations")
        public void setAttachParameterAnnotations(boolean attachParameterAnnotations) {
            this.attachParameterAnnotations = attachParameterAnnotations;
        }

        @JIPipeDocumentation(name = "Attach only non-default parameter annotations", description = "If multiple parameters are allowed, " +
                "attach only parameter annotations that have different values from the current settings. Requries 'Attach parameter annotations' to be enabled.")
        @JIPipeParameter(value = "attach-only-non-default-parameter-annotations", visibility = JIPipeParameterVisibility.Visible)
        public boolean isAttachOnlyNonDefaultParameterAnnotations() {
            return attachOnlyNonDefaultParameterAnnotations;
        }

        @JIPipeParameter("attach-only-non-default-parameter-annotations")
        public void setAttachOnlyNonDefaultParameterAnnotations(boolean attachOnlyNonDefaultParameterAnnotations) {
            this.attachOnlyNonDefaultParameterAnnotations = attachOnlyNonDefaultParameterAnnotations;
        }

        @JIPipeDocumentation(name = "Parameter annotations use internal names", description = "Generated parameter annotations use their internal unique names.")
        @JIPipeParameter(value = "parameter-annotations-use-internal-names", visibility = JIPipeParameterVisibility.Visible)
        public boolean isParameterAnnotationsUseInternalNames() {
            return parameterAnnotationsUseInternalNames;
        }

        @JIPipeParameter("parameter-annotations-use-internal-names")
        public void setParameterAnnotationsUseInternalNames(boolean parameterAnnotationsUseInternalNames) {
            this.parameterAnnotationsUseInternalNames = parameterAnnotationsUseInternalNames;
        }

        @JIPipeDocumentation(name = "Parameter annotation prefix", description = "Text prefixed to generated parameter annotations.")
        @JIPipeParameter(value = "parameter-annotations-prefix", visibility = JIPipeParameterVisibility.Visible)
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
}
