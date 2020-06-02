package org.hkijena.acaq5;

import com.formdev.flatlaf.FlatIntelliJLaf;
import ij.IJ;
import net.imagej.ImageJ;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.extensions.settings.GeneralUISettings;
import org.hkijena.acaq5.ui.ACAQProjectWindow;
import org.hkijena.acaq5.ui.components.SplashScreen;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.awt.Color;
import java.awt.Insets;

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
        if(!ACAQDefaultRegistry.isInstantiated()) {
            SwingUtilities.invokeLater(() -> SplashScreen.getInstance().showSplash());
        }
        ACAQDefaultRegistry.instantiate(context);
        SwingUtilities.invokeLater(() -> {
            if (GeneralUISettings.getInstance().getLookAndFeel() == GeneralUISettings.LookAndFeel.FlatIntelliJLaf) {
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    UIManager.put("TabbedPane.showTabSeparators", false);
//                UIManager.put("TabbedPane.tabSelectionHeight", 2);
                    UIManager.put("TabbedPane.focusColor", new Color(242, 242, 242));
                    UIManager.put("TabbedPane.hoverColor", new Color(242, 242, 242));
                    UIManager.put("TabbedPane.tabInsets", new Insets(0, 0, 0, 1));
                    UIManager.put("TabbedPane.tabAreaInsets", new Insets(0, 0, 0, 0));
                    UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(0, 0, 0, 0));
                } catch (Exception ex) {
                    System.err.println("Failed to initialize LaF");
                    IJ.handleException(ex);
                    System.err.println("ACAQ settings updated to fall back to Metal theme");
                    GeneralUISettings.getInstance().setLookAndFeel(GeneralUISettings.LookAndFeel.Metal);
                }
            }
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            ToolTipManager.sharedInstance().setInitialDelay(1000);
            ACAQProjectWindow window = ACAQProjectWindow.newWindow(this, new ACAQProject());
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
        ij.ui().showUI();
        ij.command().run(ACAQGUICommand.class, true);
    }
}
