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

package org.hkijena.jipipe.api.nodes.algorithm;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An {@link JIPipeAlgorithm} that has an optional slot that allows to supply parameter data sets.
 */
public abstract class JIPipeParameterSlotAlgorithm extends JIPipeAlgorithm {

    public static final String SLOT_PARAMETERS = "{Parameters}";
    private JIPipeParameterSlotAlgorithmSettings parameterSlotAlgorithmSettings = new JIPipeParameterSlotAlgorithmSettings();

    public JIPipeParameterSlotAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        registerSubParameter(parameterSlotAlgorithmSettings);
    }

    public JIPipeParameterSlotAlgorithm(JIPipeNodeInfo info) {
        super(info);
        registerSubParameter(parameterSlotAlgorithmSettings);
    }

    public JIPipeParameterSlotAlgorithm(JIPipeParameterSlotAlgorithm other) {
        super(other);
        this.parameterSlotAlgorithmSettings = new JIPipeParameterSlotAlgorithmSettings(other.parameterSlotAlgorithmSettings);
        registerSubParameter(parameterSlotAlgorithmSettings);
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);
        if (event.getSource() == parameterSlotAlgorithmSettings && "has-parameter-slot".equals(event.getKey())) {
            updateParameterSlot();
        }
    }

    /**
     * Returns the number of input slots that are not parameter slots.
     *
     * @return the number of input slots that are not parameter slots.
     */
    public int getDataInputSlotCount() {
        int effectiveSlotSize = getInputSlots().size();
        if (parameterSlotAlgorithmSettings.isHasParameterSlot())
            --effectiveSlotSize;
        return effectiveSlotSize;
    }

    public List<JIPipeInputDataSlot> getNonParameterInputSlots() {
        if (parameterSlotAlgorithmSettings.isHasParameterSlot()) {
            return getInputSlots().stream().filter(s -> s != getParameterSlot()).collect(Collectors.toList());
        } else {
            return getInputSlots();
        }
    }

    @Override
    public List<JIPipeInputDataSlot> getDataInputSlots() {
        return getNonParameterInputSlots();
    }

    @Override
    public JIPipeInputDataSlot getFirstInputSlot() {
        JIPipeInputDataSlot firstInputSlot = super.getFirstInputSlot();
        if (Objects.equals(firstInputSlot.getName(), SLOT_PARAMETERS)) {
            return getInputSlots().get(1);
        } else {
            return firstInputSlot;
        }
    }

    @SetJIPipeDocumentation(name = "Multi-parameter settings", description = "This algorithm supports running with multiple parameter sets. Just enable 'Multiple parameters' and " +
            "connect parameter data to the newly created slot. The algorithm is then automatically repeated for all parameter sets.")
    @JIPipeParameter(value = "jipipe:parameter-slot-algorithm", hidden = true,
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
    public Dimension getUIInputSlotIconBaseDimensions(String slotName) {
        return new Dimension(16, 16);
    }

    @Override
    public ImageIcon getUIInputSlotIcon(String slotName) {
        if (slotName.equals(SLOT_PARAMETERS)) {
            return UIUtils.getIconInvertedFromResources("actions/reload.png");
        }
        return super.getUIInputSlotIcon(slotName);
    }

    @Override
    public void createUIInputSlotIconDescriptionMenuItems(String slotName, List<ViewOnlyMenuItem> target) {
        super.createUIInputSlotIconDescriptionMenuItems(slotName, target);
        if (slotName.equals(SLOT_PARAMETERS)) {
            target.add(new ViewOnlyMenuItem("<html>Repeated per parameter<br/><small>The workload of the node is repeated for each parameter item</small>",
                    UIUtils.getIconFromResources("actions/reload.png")));
        }
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(runContext, progressInfo);
            return;
        }
        if (parameterSlotAlgorithmSettings.isHasParameterSlot()) {
            JIPipeDataSlot parameterSlot = getInputSlot(SLOT_PARAMETERS);
            if (parameterSlot.getRowCount() == 0) {
                progressInfo.log("No parameters were passed with enabled parameter slot. Applying default parameters, only.");
                runParameterSet(runContext, progressInfo, Collections.emptyList());
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
                            if (!parameterSlotAlgorithmSettings.isAttachOnlyNonDefaultParameterAnnotations() || !Objects.equals(existing, newValue)) {
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
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
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
                            annotations.add(new JIPipeTextAnnotation(annotationName, "" + target.get(Object.class)));
                        }
                    }
                    runParameterSet(runContext, progressInfo.resolve("Parameter set", row, parameterSlot.getRowCount()), annotations);
                }

                // Restore backup
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    entry.getValue().set(parameterBackups.get(entry.getKey()));
                }
            }
        } else {
            runParameterSet(runContext, progressInfo, Collections.emptyList());
        }
    }

    /**
     * Runs a parameter set iteration
     *
     * @param runContext           the run context
     * @param progressInfo         progress info from the run
     * @param parameterAnnotations parameter annotations
     */
    public abstract void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations);

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
                    JIPipeDataSlotInfo slotInfo = new JIPipeDataSlotInfo(ParametersData.class, JIPipeSlotType.Input);
                    slotInfo.setRole(JIPipeDataSlotRole.ParametersLooping);
                    JIPipeDataSlotInfo slot = slotConfiguration.addSlot(SLOT_PARAMETERS,
                            slotInfo,
                            false);
                    slot.setUserModifiable(false);
                }
            } else {
                slotConfiguration.removeInputSlot(SLOT_PARAMETERS, false);
            }
        }
    }

}
