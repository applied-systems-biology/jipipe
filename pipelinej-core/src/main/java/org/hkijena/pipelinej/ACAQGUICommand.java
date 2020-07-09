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

package org.hkijena.pipelinej;

import net.imagej.ImageJ;
import org.hkijena.pipelinej.api.ACAQProject;
import org.hkijena.pipelinej.ui.ACAQProjectWindow;
import org.hkijena.pipelinej.ui.components.SplashScreen;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;

/**
 * Command that runs the GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>ACAQ5>ACAQ5 GUI")
public class ACAQGUICommand implements Command {

    @Parameter
    private PluginService pluginService;

    @Parameter
    private Context context;

    @Override
    public void run() {
        if (!ACAQDefaultRegistry.isInstantiated()) {
            SwingUtilities.invokeLater(() -> SplashScreen.getInstance().showSplash());
        }
        ACAQDefaultRegistry.instantiate(context);
        SwingUtilities.invokeLater(() -> {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            ToolTipManager.sharedInstance().setInitialDelay(1000);
            ACAQProjectWindow window = ACAQProjectWindow.newWindow(getContext(), new ACAQProject(), true, true);
            window.setTitle("New project");
        });
    }

    /**
     * @return The context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param args ignored
     */
    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
//        ij.ui().showUI();
        SwingUtilities.invokeLater(() -> ij.command().run(ACAQGUICommand.class, true));
    }
}
