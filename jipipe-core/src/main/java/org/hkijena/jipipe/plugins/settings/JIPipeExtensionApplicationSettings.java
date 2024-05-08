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

package org.hkijena.jipipe.plugins.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeApplicationSettingsRegistry;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.util.Map;

/**
 * Settings related to how algorithms are executed
 */
public class JIPipeExtensionApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:extensions";
    private boolean validateImageJDependencies = true;
    private boolean validateNodeTypes = true;

    private boolean ignorePreActivationChecks = false;
    private boolean silent = false;

    /**
     * Creates a new instance
     */
    public JIPipeExtensionApplicationSettings() {
    }

    public static JIPipeExtensionApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeExtensionApplicationSettings.class);
    }

    /**
     * Gets an instance from the raw properties file.
     * It works before settings sheets are registered
     *
     * @return the instance
     */
    public static JIPipeExtensionApplicationSettings getInstanceFromRaw() {
        JIPipeExtensionApplicationSettings result = new JIPipeExtensionApplicationSettings();
        try {
            JsonNode node = JIPipeApplicationSettingsRegistry.getRawNode();
            if (node != null && !node.isMissingNode()) {
                JIPipeParameterTree tree = new JIPipeParameterTree(result);
                for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                    JsonNode entryNode = node.path(ID).path(entry.getKey());
                    if(!entryNode.isMissingNode()) {
                        Object value = JsonUtils.getObjectMapper().readerFor(entry.getValue().getFieldClass()).readValue(entryNode);
                        entry.getValue().set(value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SetJIPipeDocumentation(name = "Validate ImageJ dependencies", description = "If enabled, JIPipe will check if ImageJ dependencies are installed.")
    @JIPipeParameter("validate-imagej-dependencies")
    public boolean isValidateImageJDependencies() {
        return validateImageJDependencies;
    }

    @JIPipeParameter("validate-imagej-dependencies")
    public void setValidateImageJDependencies(boolean validateImageJDependencies) {
        this.validateImageJDependencies = validateImageJDependencies;
    }

    @SetJIPipeDocumentation(name = "Validate node types", description = "If enabled, JIPipe will test all node types if they can be initialized and only contain known parameter types.")
    @JIPipeParameter("validate-node-types")
    public boolean isValidateNodeTypes() {
        return validateNodeTypes;
    }

    @JIPipeParameter("validate-node-types")
    public void setValidateNodeTypes(boolean validateNodeTypes) {
        this.validateNodeTypes = validateNodeTypes;
    }

    @SetJIPipeDocumentation(name = "Silent", description = "If enabled, no dialogs are shown")
    @JIPipeParameter("silent")
    public boolean isSilent() {
        return silent;
    }

    @JIPipeParameter("silent")
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @SetJIPipeDocumentation(name = "Ignore pre-activation checks", description = "If enabled, extensions will be loaded even if pre-activation checks indicated issues")
    @JIPipeParameter("ignore-pre-activation-checks")
    public boolean isIgnorePreActivationChecks() {
        return ignorePreActivationChecks;
    }

    @JIPipeParameter("ignore-pre-activation-checks")
    public void setIgnorePreActivationChecks(boolean ignorePreActivationChecks) {
        this.ignorePreActivationChecks = ignorePreActivationChecks;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/plugins.png");
    }

    @Override
    public String getName() {
        return "Extensions";
    }

    @Override
    public String getDescription() {
        return "Settings regarding the validation of extensions";
    }
}
