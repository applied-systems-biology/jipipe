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

package org.hkijena.jipipe.desktop.app.plugins;

import net.imagej.ui.swing.updater.ImageJUpdater;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeRemoteArtifact;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class JIPipeDesktopManagePluginsButton extends JButton implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private boolean dismissedNewExtensions = false;

    public JIPipeDesktopManagePluginsButton(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        updateIcon();
    }

    private void updateIcon() {
        if(hasNewExtensions()) {
            setIcon(UIUtils.getIconFromResources("emblems/emblem-important-blue.png"));
        }
        else {
            setIcon(UIUtils.getIconFromResources("actions/preferences-plugin.png"));
        }
    }

    private boolean hasNewExtensions() {
        return !dismissedNewExtensions && !JIPipe.getInstance().getExtensionRegistry().getNewExtensions().isEmpty();
    }

    private void initialize() {
        UIUtils.setStandardButtonBorder(this);
        setText("Plugins");
        setIcon(UIUtils.getIconFromResources("actions/preferences-plugin.png"));
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadPopupMenu);
    }

    private void reloadPopupMenu() {
        popupMenu.removeAll();
        popupMenu.add(UIUtils.createMenuItem("JIPipe plugins",
                "Opens the JIPipe plugin manager",
                hasNewExtensions() ? UIUtils.getIconFromResources("emblems/emblem-important-blue.png") : UIUtils.getIconFromResources("apps/jipipe.png"),
                this::openJIPipePluginManager));
        popupMenu.add(UIUtils.createMenuItem("External dependencies", "Opens the artifacts manager",UIUtils.getIconFromResources("actions/run-install.png"), this::openArtifactManager));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("ImageJ plugins", "Opens the ImageJ update manager",UIUtils.getIconFromResources("apps/imagej.png"), this::openImageJPluginManager));
    }

    private void openImageJPluginManager() {
        ImageJUpdater updater = new ImageJUpdater();
        JIPipe.getInstance().getContext().inject(updater);
        updater.run();
    }

    private void openArtifactManager() {
        JIPipeDesktopArtifactManagerUI.show(getDesktopWorkbench());
    }

    private void openJIPipePluginManager() {
        dismissedNewExtensions = true;
        JIPipe.getInstance().getExtensionRegistry().dismissNewExtensions();
        updateIcon();

    }

    @Override
    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }
}
