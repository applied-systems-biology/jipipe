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

package org.hkijena.jipipe.desktop.commons.components.project;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeDefaultEnvironmentsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.project.JIPipeDefaultEnvironmentsProjectSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtifactUpgradeUtils {

    public static void showUpgradeDialog(JIPipeDesktopWorkbench workbench, List<ArtifactUpgrade> upgrades) {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        List<JComboBox<String>> comboBoxes = new ArrayList<>();

        formPanel.addToForm(UIUtils.createJLabel("New version", 16), UIUtils.createJLabel("Old version", 16));
        formPanel.addWideToForm(new JSeparator(JSeparator.HORIZONTAL));

        for (ArtifactUpgrade artifactUpgrade : upgrades) {
            JComboBox<String> comboBox = new JComboBox<>();
            for (JIPipeArtifact revisionUpgrade : artifactUpgrade.getRevisionUpgrades()) {
                comboBox.addItem(revisionUpgrade.getFullId());
            }
            for (JIPipeArtifact accelerationUpgrade : artifactUpgrade.getAccelerationUpgrades()) {
                comboBox.addItem(accelerationUpgrade.getFullId());
            }
            comboBox.addItem("Keep as-is");
            comboBoxes.add(comboBox);

            formPanel.addToForm(comboBox, new JLabel(artifactUpgrade.getCurrent().getFullId(), JIPipe.RESOURCES.getIcon16("actions/run-build-install.png"), JLabel.LEFT));
        }
        int numSuccesses = 0;
        if (JIPipeDesktopFormPanel.showDialog(workbench.getWindow(), formPanel, "Update third-party artifacts")) {
            for (int i = 0; i < upgrades.size(); i++) {
                ArtifactUpgrade upgrade = upgrades.get(i);
                JComboBox<String> comboBox = comboBoxes.get(i);
                String selectedItem = StringUtils.nullToEmpty(comboBox.getSelectedItem());

                if (!StringUtils.isNullOrEmpty(selectedItem) && !"Keep as-is".equals(selectedItem)) {
                    upgrade.getEnvironment().setArtifactQuery(new JIPipeArtifactQueryParameter(selectedItem));
                    ++numSuccesses;
                }
            }

            if (numSuccesses > 0) {
                JOptionPane.showMessageDialog(workbench.getWindow(), StringUtils.wrapHtml(StringUtils.formatPluralS(numSuccesses, "artifact") + " were updated.<br/>JIPipe will automatically take care of downloading and setting up the artifacts."));
            }
        }
    }

    public static void findAndShowUpgradeDialog(JIPipeDesktopWorkbench workbench, JIPipeGraphNode node, Class<? extends JIPipeArtifactEnvironment> environmentClass) {
        JIPipeArtifactEnvironment environment = resolveActualEnvironment(node, environmentClass);
        if (environment == null) {
            JOptionPane.showMessageDialog(workbench.getWindow(),
                    "Could not find the environment in the project settings.\nPlease update the Cellpose artifact manually.",
                    "Update Cellpose", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ArtifactUpgrade> upgrades = findAvailableUpgrades(environment);
        if (upgrades.isEmpty()) {
            JOptionPane.showMessageDialog(workbench.getWindow(),
                    "No newer Cellpose artifacts were found.\nPlease install a newer Cellpose artifact manually.",
                    "Update Cellpose", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showUpgradeDialog(workbench, upgrades);
    }

    private static JIPipeArtifactEnvironment resolveActualEnvironment(JIPipeGraphNode node, Class<? extends JIPipeArtifactEnvironment> environmentClass) {
        JIPipeEnvironmentsServiceComponent.EnvironmentInfo info = JIPipe.getInstance().getEnvironments().getInfoByClass(environmentClass);
        if (info == null) {
            return null;
        }

        // 1. Node-level override
        if (node != null) {
            JIPipeParameterAccess access = node.getEnvironmentOverrides().get(info.getId());
            if (access != null) {
                JIPipeOptionalParameter<?> parameter = access.get(JIPipeOptionalParameter.class);
                if (parameter != null && parameter.isEnabled()
                        && parameter.getContent() instanceof JIPipeArtifactEnvironment env
                        && environmentClass.isAssignableFrom(env.getClass())) {
                    return env;
                }
            }
        }

        // 2. Project-level override
        JIPipeProject project = node != null ? node.getProject() : null;
        if (project != null) {
            JIPipeDefaultEnvironmentsProjectSettings settings = project.getSettingsSheet(JIPipeDefaultEnvironmentsProjectSettings.class);
            if (settings != null) {
                JIPipeParameterAccess access = settings.get(info.getId());
                if (access != null) {
                    JIPipeOptionalParameter<?> parameter = access.get(JIPipeOptionalParameter.class);
                    if (parameter != null && parameter.isEnabled()
                            && parameter.getContent() instanceof JIPipeArtifactEnvironment env
                            && environmentClass.isAssignableFrom(env.getClass())) {
                        return env;
                    }
                }
            }
        }

        // 3. Application-level override
        JIPipeDefaultEnvironmentsApplicationSettings appSettings = JIPipe.getSettings().getByType(JIPipeDefaultEnvironmentsApplicationSettings.class);
        if (appSettings != null) {
            JIPipeParameterAccess access = appSettings.get(info.getId());
            if (access != null) {
                JIPipeOptionalParameter<?> parameter = access.get(JIPipeOptionalParameter.class);
                if (parameter != null && parameter.isEnabled()
                        && parameter.getContent() instanceof JIPipeArtifactEnvironment env
                        && environmentClass.isAssignableFrom(env.getClass())) {
                    return env;
                }
            }
        }

        return null;
    }

    public static List<ArtifactUpgrade> findAvailableUpgrades(JIPipeArtifactEnvironment environment) {
        List<ArtifactUpgrade> upgrades = new ArrayList<>();
        if (environment.isLoadFromArtifact() && !StringUtils.isNullOrEmpty(environment.getArtifactQuery().getQuery())) {
            JIPipeArtifact queriedArtifact = environment.getArtifactQuery().toArtifact();
            if (queriedArtifact.getResolutionStatus() == JIPipeArtifact.ResolutionStatus.GroupNameVersion || queriedArtifact.getResolutionStatus() == JIPipeArtifact.ResolutionStatus.Full) {
                try {
                    JIPipeArtifact current = JIPipe.getArtifacts().queryPreferredCachedArtifact(environment.getArtifactQuery().getQuery());
                    List<JIPipeArtifact> candidates = JIPipe.getArtifacts().queryCachedArtifacts(queriedArtifact.getFullId(JIPipeArtifact.ResolutionStatus.GroupName));
                    List<JIPipeArtifact> revisionUpgrades = new ArrayList<>();
                    if (current != null) {
                        int revisionVersion = current.getVersionRevision();
                        String baseVersion = current.getVersionWithoutRevision();

                        Set<String> alreadyAdded = new HashSet<>();
                        for (JIPipeArtifact candidate : candidates) {
                            if (candidate.isCompatible()) {
                                String candidateBaseVersion = candidate.getVersionWithoutRevision();
                                int candidateRevision = candidate.getVersionRevision();
                                boolean isUpgrade = false;
                                if (StringUtils.compareVersions(candidateBaseVersion, baseVersion) == 0) {
                                    if (candidateRevision > revisionVersion && !candidate.getFullId().equals(current.getFullId())) {
                                        isUpgrade = true;
                                    }
                                } else if (StringUtils.compareVersions(candidateBaseVersion, baseVersion) > 0) {
                                    isUpgrade = true;
                                }
                                if (isUpgrade) {
                                    JIPipeArtifact candidate1 = new JIPipeArtifact(candidate);
                                    candidate1.setClassifier("*");
                                    String candidate1Query = candidate1.getFullId(JIPipeArtifact.ResolutionStatus.GroupNameVersion);
                                    if (!alreadyAdded.contains(candidate1Query)) {
                                        revisionUpgrades.add(candidate1);
                                        alreadyAdded.add(candidate1Query);
                                    }
                                }
                            }
                        }
                    }
                    if (!revisionUpgrades.isEmpty()) {
                        upgrades.add(new ArtifactUpgrade(environment, current, revisionUpgrades, Collections.emptyList()));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return upgrades;
    }
}
