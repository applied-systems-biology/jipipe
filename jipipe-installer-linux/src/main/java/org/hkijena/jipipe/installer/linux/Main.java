package org.hkijena.jipipe.installer.linux;

import org.hkijena.jipipe.installer.linux.api.InstallerRun;
import org.hkijena.jipipe.installer.linux.ui.MainWindow;
import org.hkijena.jipipe.installer.linux.ui.utils.ArrowLessScrollBarUI;
import org.hkijena.jipipe.installer.linux.ui.utils.ModernMetalTheme;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        List<String> argsList = Arrays.asList(args);

        if(argsList.contains("help") || argsList.contains("--help") || argsList.contains("-help")) {
            System.out.println("JIPipe installer for linux");
            System.out.println();
            System.out.println("GUI: java -jar jipipe-installer-linux.jar");
            System.out.println("CLI: java -jar jipipe-installer-linux.jar --installationDir <Installation Directory> [--noCreateLauncher]");
            return;
        }

        String installationPath = null;
        boolean createLaunchers = true;

        for (int i = 0; i < args.length; i++) {
            if("--noCreateLauncher".equals(args[i]))
                createLaunchers = false;
            else if("--installationDir".equals(args[i]) && (i + 1) < args.length)
                installationPath = args[i + 1];
        }

        if(installationPath != null) {
            runCLIInstaller(installationPath, createLaunchers);
        }
        else {
            try {
                MetalLookAndFeel.setCurrentTheme(new ModernMetalTheme());
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                UIManager.put("ScrollBarUI", ArrowLessScrollBarUI.class.getName());
            } catch (IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            MainWindow window = new MainWindow();
            window.pack();
            window.setSize(1024, 768);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        }
    }

    private static void runCLIInstaller(String installationPath, boolean createLaunchers) {
        InstallerRun run = new InstallerRun();
        run.setInstallationPath(Paths.get(installationPath));
        run.setCreateLauncher(createLaunchers);
        run.run(status -> System.out.println(status.getProgress() + "/" + status.getMaxProgress() + ": " + status.getMessage()), () -> false);
    }
}
