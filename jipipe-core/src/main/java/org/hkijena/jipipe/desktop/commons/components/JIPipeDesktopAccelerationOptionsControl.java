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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeApplicationSettingsRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopApplicationSettingsUI;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactAccelerationPreference;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopAccelerationOptionsControl extends JButton implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JIPipeArtifactApplicationSettings settings = JIPipeArtifactApplicationSettings.getInstance();

    public JIPipeDesktopAccelerationOptionsControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        updateText();
        settings.getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setToolTipText("Setup the preferred acceleration method");
        setIcon(UIUtils.getIconFromResources("actions/speedometer.png"));
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    private void reloadMenu() {
        popupMenu.removeAll();
        for (JIPipeArtifactAccelerationPreference value : JIPipeArtifactAccelerationPreference.values()) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(value.toString(), settings.getAccelerationPreference() == value);
            menuItem.addActionListener(e -> {
                settings.setAccelerationPreference(value);
                updateText();
                JIPipe.getSettings().save();
            });
            popupMenu.add(menuItem);
        }
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Configure", "Opens the application settings", UIUtils.getIconFromResources("actions/configure.png"), this::openApplicationSettings));
    }

    private void openApplicationSettings() {
        workbench.openApplicationSettings("/General/Artifacts");
    }

    private void updateText() {
        if(settings.getAccelerationPreference() != JIPipeArtifactAccelerationPreference.CPU && (settings.getAccelerationPreferenceVersions().getX() > 0 || settings.getAccelerationPreferenceVersions().getY() > 0)) {
         setText(String.format("%s (%s - %s)", settings.getAccelerationPreference().toString(),
                 settings.getAccelerationPreferenceVersions().getX() > 0 ? Integer.toString(settings.getAccelerationPreferenceVersions().getX()) : "*",
                 settings.getAccelerationPreferenceVersions().getY() > 0 ? Integer.toString(settings.getAccelerationPreferenceVersions().getY()) : "*"));
        }
        else {
            setText(settings.getAccelerationPreference().toString());
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        updateText();
    }
}
