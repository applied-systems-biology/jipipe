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

package org.hkijena.pipelinej.api.algorithm;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.data.*;
import org.hkijena.pipelinej.api.events.ParameterChangedEvent;
import org.hkijena.pipelinej.api.parameters.*;
import org.hkijena.pipelinej.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ACAQAlgorithm} that has an optional slot that allows to supply parameter data sets.
 */
public abstract class ACAQParameterSlotAlgorithm extends ACAQAlgorithm {

    public static final String SLOT_PARAMETERS = "Parameters";
    private ParameterSlotAlgorithmSettings parameterSlotAlgorithmSettings = new ParameterSlotAlgorithmSettings();

    public ACAQParameterSlotAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
        registerParameterSettings();
    }

    public ACAQParameterSlotAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        registerParameterSettings();
    }

    public ACAQParameterSlotAlgorithm(ACAQParameterSlotAlgorithm other) {
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
    public ACAQDataSlot getFirstInputSlot() {
        ACAQDataSlot firstInputSlot = super.getFirstInputSlot();
        if (Objects.equals(firstInputSlot.getName(), SLOT_PARAMETERS)) {
            return getInputSlots().get(1);
        } else {
            return firstInputSlot;
        }
    }

    @ACAQDocumentation(name = "Multi-parameter settings", description = "This algorithm supports running with multiple parameter sets. Just enable 'Multiple parameters' and " +
            "connect parameter data to the newly created slot. The algorithm is then automatically repeated for all parameter sets.")
    @ACAQParameter(value = "acaq:parameter-slot-algorithm", visibility = ACAQParameterVisibility.Visible)
    public ParameterSlotAlgorithmSettings getParameterSlotAlgorithmSettings() {
        return parameterSlotAlgorithmSettings;
    }

    /**
     * Returns the parameter slot if enabled
     *
     * @return the parameter slot
     */
    public ACAQDataSlot getParameterSlot() {
        if (parameterSlotAlgorithmSettings.hasParameterSlot)
            return getInputSlot(SLOT_PARAMETERS);
        else
            return null;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        if (isPassThrough() && canPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }
        if (parameterSlotAlgorithmSettings.hasParameterSlot) {
            ACAQDataSlot parameterSlot = getInputSlot(SLOT_PARAMETERS);
            if (parameterSlot.getRowCount() == 0) {
                algorithmProgress.accept(subProgress.resolve("No parameters were passed with enabled parameter slot. Applying default parameters, only."));
                runParameterSet(subProgress, algorithmProgress, isCancelled, Collections.emptyList());
            } else {
                // Create backups
                Map<String, Object> parameterBackups = new HashMap<>();
                ACAQParameterTree tree = new ACAQParameterTree(this);
                for (Map.Entry<String, ACAQParameterAccess> entry : tree.getParameters().entrySet()) {
                    parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
                }

                // Collect different parameters
                Set<String> nonDefaultParameters = new HashSet<>();
                for (int row = 0; row < parameterSlot.getRowCount(); row++) {
                    ParametersData data = parameterSlot.getData(row, ParametersData.class);
                    for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
                        ACAQParameterAccess target = tree.getParameters().getOrDefault(entry.getKey(), null);
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
                    List<ACAQAnnotation> annotations = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
                        ACAQParameterAccess target = tree.getParameters().getOrDefault(entry.getKey(), null);
                        if (target == null) {
                            algorithmProgress.accept(subProgress.resolve("Unable to find parameter '" + entry.getKey() + "' in " + getName() + "! Ignoring."));
                            continue;
                        }
                        target.set(entry.getValue());
                    }
                    if (parameterSlotAlgorithmSettings.attachParameterAnnotations) {
                        for (String key : nonDefaultParameters) {
                            ACAQParameterAccess target = tree.getParameters().get(key);
                            String annotationName;
                            if (parameterSlotAlgorithmSettings.parameterAnnotationsUseInternalNames) {
                                annotationName = key;
                            } else {
                                annotationName = target.getName();
                            }
                            annotationName = parameterSlotAlgorithmSettings.parameterAnnotationsPrefix + annotationName;
                            annotations.add(new ACAQAnnotation(annotationName, "" + target.get(Object.class)));
                        }
                    }


                    runParameterSet(subProgress.resolve("Parameter set " + (row + 1) + " / " + parameterSlot.getRowCount()), algorithmProgress, isCancelled, annotations);
                }

                // Restore backup
                for (Map.Entry<String, ACAQParameterAccess> entry : tree.getParameters().entrySet()) {
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
    public abstract void runParameterSet(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<ACAQAnnotation> parameterAnnotations);

    private void updateParameterSlot() {
        if (getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getSlotConfiguration();
            if (parameterSlotAlgorithmSettings.hasParameterSlot) {
                ACAQSlotDefinition existing = slotConfiguration.getInputSlots().getOrDefault(SLOT_PARAMETERS, null);
                if (existing != null && existing.getDataClass() != ParametersData.class) {
                    slotConfiguration.removeInputSlot(SLOT_PARAMETERS, false);
                    existing = null;
                }
                if (existing == null) {
                    slotConfiguration.addSlot(SLOT_PARAMETERS,
                            new ACAQSlotDefinition(ParametersData.class, ACAQSlotType.Input, null),
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
    public static class ParameterSlotAlgorithmSettings implements ACAQParameterCollection {
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

        @ACAQDocumentation(name = "Multiple parameters", description = "If enabled, there will be an additional slot that consumes " +
                "parameter data sets. The algorithm then will be applied for each of this parameter sets.")
        @ACAQParameter(value = "has-parameter-slot", visibility = ACAQParameterVisibility.Visible)
        public boolean isHasParameterSlot() {
            return hasParameterSlot;
        }

        @ACAQParameter("has-parameter-slot")
        public void setHasParameterSlot(boolean hasParameterSlot) {
            this.hasParameterSlot = hasParameterSlot;
        }

        @ACAQDocumentation(name = "Attach parameter annotations", description = "If multiple parameters are allowed, attach the parameter values as annotations.")
        @ACAQParameter(value = "attach-parameter-annotations", visibility = ACAQParameterVisibility.Visible)
        public boolean isAttachParameterAnnotations() {
            return attachParameterAnnotations;
        }

        @ACAQParameter("attach-parameter-annotations")
        public void setAttachParameterAnnotations(boolean attachParameterAnnotations) {
            this.attachParameterAnnotations = attachParameterAnnotations;
        }

        @ACAQDocumentation(name = "Attach only non-default parameter annotations", description = "If multiple parameters are allowed, " +
                "attach only parameter annotations that have different values from the current settings. Requries 'Attach parameter annotations' to be enabled.")
        @ACAQParameter(value = "attach-only-non-default-parameter-annotations", visibility = ACAQParameterVisibility.Visible)
        public boolean isAttachOnlyNonDefaultParameterAnnotations() {
            return attachOnlyNonDefaultParameterAnnotations;
        }

        @ACAQParameter("attach-only-non-default-parameter-annotations")
        public void setAttachOnlyNonDefaultParameterAnnotations(boolean attachOnlyNonDefaultParameterAnnotations) {
            this.attachOnlyNonDefaultParameterAnnotations = attachOnlyNonDefaultParameterAnnotations;
        }

        @ACAQDocumentation(name = "Parameter annotations use internal names", description = "Generated parameter annotations use their internal unique names.")
        @ACAQParameter(value = "parameter-annotations-use-internal-names", visibility = ACAQParameterVisibility.Visible)
        public boolean isParameterAnnotationsUseInternalNames() {
            return parameterAnnotationsUseInternalNames;
        }

        @ACAQParameter("parameter-annotations-use-internal-names")
        public void setParameterAnnotationsUseInternalNames(boolean parameterAnnotationsUseInternalNames) {
            this.parameterAnnotationsUseInternalNames = parameterAnnotationsUseInternalNames;
        }

        @ACAQDocumentation(name = "Parameter annotation prefix", description = "Text prefixed to generated parameter annotations.")
        @ACAQParameter(value = "parameter-annotations-prefix", visibility = ACAQParameterVisibility.Visible)
        @StringParameterSettings(monospace = true)
        public String getParameterAnnotationsPrefix() {
            return parameterAnnotationsPrefix;
        }

        @ACAQParameter("parameter-annotations-prefix")
        public void setParameterAnnotationsPrefix(String parameterAnnotationsPrefix) {
            this.parameterAnnotationsPrefix = parameterAnnotationsPrefix;
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }
    }
}
