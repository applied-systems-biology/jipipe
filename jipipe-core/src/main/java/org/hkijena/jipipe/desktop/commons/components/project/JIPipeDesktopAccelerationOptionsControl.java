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
import org.hkijena.jipipe.api.acceleration.JIPipeHardwareAccelerationMode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.plugins.settings.application.JIPipeHardwareAccelerationApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopAccelerationOptionsControl extends JButton implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JIPipeHardwareAccelerationApplicationSettings settings = JIPipeHardwareAccelerationApplicationSettings.getInstance();

    public JIPipeDesktopAccelerationOptionsControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        updateText();
        settings.getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setToolTipText("Setup the preferred acceleration method");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/speedometer.png"));
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    private void reloadMenu() {
        popupMenu.removeAll();
        for (JIPipeHardwareAccelerationMode value : JIPipeHardwareAccelerationMode.values()) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(value.toString(), settings.getAccelerationPreference() == value);
            menuItem.addActionListener(e -> {
                settings.setAccelerationPreference(value);
                updateText();
                JIPipe.getSettings().save();
            });
            popupMenu.add(menuItem);
        }
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Configure ...", "Opens the application settings", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openApplicationSettings));
    }

    private void openApplicationSettings() {
        workbench.openApplicationSettings("/General/Hardware acceleration");
    }

    private void updateText() {
        if (settings.getAccelerationPreference() != JIPipeHardwareAccelerationMode.CPU && (settings.getAccelerationPreferenceVersions().getX() > 0 || settings.getAccelerationPreferenceVersions().getY() > 0)) {
            setText(String.format("%s (%s - %s)", settings.getAccelerationPreference().toString(),
                    settings.getAccelerationPreferenceVersions().getX() > 0 ? Integer.toString(settings.getAccelerationPreferenceVersions().getX()) : "*",
                    settings.getAccelerationPreferenceVersions().getY() > 0 ? Integer.toString(settings.getAccelerationPreferenceVersions().getY()) : "*"));
        } else {
            setText(settings.getAccelerationPreference().toString());
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        updateText();
    }
}
