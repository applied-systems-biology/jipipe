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

package org.hkijena.jipipe;

import net.imagej.ImageJ;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.components.SplashScreen;
import org.hkijena.jipipe.ui.ijupdater.MissingUpdateSiteResolver;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;

/**
 * Command that runs the GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>JIPipe>JIPipe GUI")
public class JIPipeGUICommand implements Command {

    @Parameter
    private PluginService pluginService;

    @Parameter
    private Context context;

    @Override
    public void run() {
        // Update look & feel
        UIUtils.loadLookAndFeelFromSettings();
        if (!JIPipeDefaultRegistry.isInstantiated()) {
            SwingUtilities.invokeLater(() -> SplashScreen.getInstance().showSplash());
        }

        // Run registration
        ExtensionSettings extensionSettings = ExtensionSettings.getInstanceFromRaw();
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        try {
            JIPipeDefaultRegistry.instantiate(context, extensionSettings, issues);
        } catch (Exception e) {
            e.printStackTrace();
            if (!extensionSettings.isSilent())
                UIUtils.openErrorDialog(null, e);
            return;
        }

        // Resolve missing ImageJ dependencies
        if (!extensionSettings.isSilent()) {
            resolveMissingImageJDependencies(issues);

            {
                JIPipeValidityReport report = new JIPipeValidityReport();
                issues.reportValidity(report);
                if (!report.isValid()) {
                    UIUtils.openValidityReportDialog(null, report, true);
                }
            }
        }

        // Run application
        SwingUtilities.invokeLater(() -> {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            ToolTipManager.sharedInstance().setInitialDelay(1000);
            JIPipeProjectWindow window = JIPipeProjectWindow.newWindow(getContext(),
                    JIPipeProjectWindow.getDefaultTemplateProject(),
                    true,
                    true);
            window.setTitle("New project");
        });
    }

    private void resolveMissingImageJDependencies(JIPipeRegistryIssues issues) {
        if (issues.getMissingImageJSites().isEmpty())
            return;
        MissingUpdateSiteResolver resolver = new MissingUpdateSiteResolver(getContext(), issues);
        resolver.revalidate();
        resolver.repaint();
        resolver.setLocationRelativeTo(null);
        resolver.setVisible(true);

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
        SwingUtilities.invokeLater(() -> ij.command().run(JIPipeGUICommand.class, true));
    }
}
