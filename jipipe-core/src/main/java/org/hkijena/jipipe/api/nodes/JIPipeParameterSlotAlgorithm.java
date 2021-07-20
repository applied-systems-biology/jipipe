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

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An {@link JIPipeAlgorithm} that has an optional slot that allows to supply parameter data sets.
 */
public abstract class JIPipeParameterSlotAlgorithm extends JIPipeAlgorithm {

    public static final String SLOT_PARAMETERS = "{Parameters}";
    private JIPipeParameterSlotAlgorithmSettings parameterSlotAlgorithmSettings = new JIPipeParameterSlotAlgorithmSettings();

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
        this.parameterSlotAlgorithmSettings = new JIPipeParameterSlotAlgorithmSettings(other.parameterSlotAlgorithmSettings);
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

    public List<JIPipeDataSlot> getNonParameterInputSlots() {
        if (parameterSlotAlgorithmSettings.isHasParameterSlot()) {
            return getInputSlots().stream().filter(s -> s != getParameterSlot()).collect(Collectors.toList());
        } else {
            return getInputSlots();
        }
    }

    @Override
    public List<JIPipeDataSlot> getEffectiveInputSlots() {
        return getNonParameterInputSlots();
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
    @JIPipeParameter(value = "jipipe:parameter-slot-algorithm", collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/wrench.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/wrench.png")
    public JIPipeParameterSlotAlgorithmSettings getParameterSlotAlgorithmSettings() {
        return parameterSlotAlgorithmSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (ParameterUtils.isHiddenLocalParameterCollection(tree, subParameter, "jipipe:parameter-slot-algorithm")) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    /**
     * Returns the parameter slot if enabled
     *
     * @return the parameter slot
     */
    public JIPipeDataSlot getParameterSlot() {
        if (parameterSlotAlgorithmSettings.isHasParameterSlot())
            return getInputSlot(SLOT_PARAMETERS);
        else
            return null;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
            return;
        }
        if (parameterSlotAlgorithmSettings.isHasParameterSlot()) {
            JIPipeDataSlot parameterSlot = getInputSlot(SLOT_PARAMETERS);
            if (parameterSlot.getRowCount() == 0) {
                progressInfo.log("No parameters were passed with enabled parameter slot. Applying default parameters, only.");
                runParameterSet(progressInfo, Collections.emptyList());
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
                    ParametersData data = parameterSlot.getData(row, ParametersData.class, progressInfo);
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
                    if (progressInfo.isCancelled())
                        break;
                    ParametersData data = parameterSlot.getData(row, ParametersData.class, progressInfo);
                    List<JIPipeAnnotation> annotations = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : data.getParameterData().entrySet()) {
                        JIPipeParameterAccess target = tree.getParameters().getOrDefault(entry.getKey(), null);
                        if (target == null) {
                            progressInfo.log("Unable to find parameter '" + entry.getKey() + "' in " + getName() + "! Ignoring.");
                            continue;
                        }
                        target.set(entry.getValue());
                    }
                    if (parameterSlotAlgorithmSettings.isAttachParameterAnnotations()) {
                        for (String key : nonDefaultParameters) {
                            JIPipeParameterAccess target = tree.getParameters().get(key);
                            String annotationName;
                            if (parameterSlotAlgorithmSettings.isParameterAnnotationsUseInternalNames()) {
                                annotationName = key;
                            } else {
                                annotationName = target.getName();
                            }
                            annotationName = parameterSlotAlgorithmSettings.getParameterAnnotationsPrefix() + annotationName;
                            annotations.add(new JIPipeAnnotation(annotationName, "" + target.get(Object.class)));
                        }
                    }
                    runParameterSet(progressInfo.resolve("Parameter set", row, parameterSlot.getRowCount()), annotations);
                }

                // Restore backup
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    entry.getValue().set(parameterBackups.get(entry.getKey()));
                }
            }
        } else {
            runParameterSet(progressInfo, Collections.emptyList());
        }
    }

    /**
     * Runs a parameter set iteration
     *
     * @param progressInfo         progress info from the run
     * @param parameterAnnotations parameter annotations
     */
    public abstract void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations);

    private void updateParameterSlot() {
        if (getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) getSlotConfiguration();
            if (parameterSlotAlgorithmSettings.isHasParameterSlot()) {
                JIPipeDataSlotInfo existing = slotConfiguration.getInputSlots().getOrDefault(SLOT_PARAMETERS, null);
                if (existing != null && existing.getDataClass() != ParametersData.class) {
                    slotConfiguration.removeInputSlot(SLOT_PARAMETERS, false);
                    existing = null;
                }
                if (existing == null) {
                    JIPipeDataSlotInfo slot = slotConfiguration.addSlot(SLOT_PARAMETERS,
                            new JIPipeDataSlotInfo(ParametersData.class, JIPipeSlotType.Input),
                            false);
                    slot.setUserModifiable(false);
                }
            } else {
                slotConfiguration.removeInputSlot(SLOT_PARAMETERS, false);
            }
        }
    }

}
