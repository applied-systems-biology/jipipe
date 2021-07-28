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

package org.hkijena.jipipe.extensions.tools;

import ij.IJ;
import ij.Prefs;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.NoSuchElementException;

public class CreateLaunchersTool extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public CreateLaunchersTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Create launchers");
        setToolTipText("Creates files that can open JIPipe directly (without ImageJ)");
        setIcon(UIUtils.getIconFromResources("apps/jipipe.png"));
        addActionListener(e -> createLaunchers());
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    private void createLaunchers() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        if (SystemUtils.IS_OS_LINUX)
            createLinuxLauncher(imageJDir);
        else if (SystemUtils.IS_OS_WINDOWS)
            createWindowsLauncher(imageJDir);
        else if (SystemUtils.IS_OS_MAC_OSX)
            createOSXLauncher(imageJDir);
        else
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Your operating system is not supported!", "Create launcher", JOptionPane.ERROR_MESSAGE);
    }

    private void createOSXLauncher(Path imageJDir) {
        try (PrintWriter writer = new PrintWriter(imageJDir.resolve("start-jipipe-osx.sh").toFile())) {
            writer.println("#!/bin/bash");
            writer.println("IMAGEJ_EXECUTABLE=$(find . -name \"ImageJ-*\" -print -quit)");
            writer.println("eval $IMAGEJ_EXECUTABLE --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher");
        } catch (FileNotFoundException e) {
            IJ.handleException(e);
        }
        getWorkbench().sendStatusBarText("Created start-jipipe-osx.sh in the application directory.");
    }

    private void createWindowsLauncher(Path imageJDir) {
        Path imageJExecutable;
        try {
            imageJExecutable = Files.list(imageJDir).filter(p -> Files.isRegularFile(p) && p.getFileName().toString().startsWith("ImageJ") && p.getFileName().toString().endsWith(".exe")).findFirst().get();
        } catch (IOException | NoSuchElementException e) {
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Could not find ImageJ executable!", "Create launcher", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Export icon
        Path iconPath = imageJDir.resolve("jipipe-icon.ico");
        if (!Files.exists(iconPath)) {
            try {
                InputStream reader = ResourceUtils.getPluginResourceAsStream("icon.ico");
                Files.copy(reader, iconPath);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }

        ShellLink link = ShellLink.createLink(imageJExecutable.toAbsolutePath().toString());
        link.setCMDArgs("--pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher");
        link.setWorkingDir(imageJDir.toAbsolutePath().toString());
        link.setIconLocation(iconPath.toAbsolutePath().toString());
        link.getHeader().setIconIndex(0);
        try {
            link.saveTo(imageJDir.resolve("JIPipe.lnk").toString());
            getWorkbench().sendStatusBarText("Created JIPipe shortcut in the application directory.");
        } catch (IOException e) {
            IJ.handleException(e);
            return;
        }
        if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                "The launcher was created. Do you want to copy it to the desktop?",
                "Create launchers",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            // This is apparently very Windows-specific
            Path desktopDirectory = FileSystemView.getFileSystemView().getHomeDirectory().toPath();
            try {
                link.saveTo(desktopDirectory.resolve("JIPipe.lnk").toString());
                getWorkbench().sendStatusBarText("Created JIPipe shortcut on the desktop.");
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private void createLinuxLauncher(Path imageJDir) {
        Path scriptFile = imageJDir.resolve("start-jipipe-linux.sh");
        try (PrintWriter writer = new PrintWriter(scriptFile.toFile())) {
            writer.println("#!/bin/bash");
            writer.println("IMAGEJ_EXECUTABLE=$(find . -name \"ImageJ-*\" -print -quit)");
            writer.println("eval $IMAGEJ_EXECUTABLE --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher");
        } catch (FileNotFoundException e) {
            IJ.handleException(e);
            return;
        }
        try {
            Files.setPosixFilePermissions(scriptFile, PosixFilePermissions.fromString("rwxrwxr-x"));
        } catch (IOException e) {
            IJ.handleException(e);
        }
        getWorkbench().sendStatusBarText("Created start-jipipe-linux.sh in the application directory.");
        if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                "The launcher was created. Do you want to add a entry into the application menu?",
                "Create launchers",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            String dataHome = System.getenv().getOrDefault("XDG_DATA_HOME", FileUtils.getUserDirectory().toPath().resolve(".local").resolve("share").toString());
            Path applicationsDirectory = Paths.get(dataHome).resolve("applications");
            if (!Files.exists(applicationsDirectory)) {
                try {
                    Files.createDirectories(applicationsDirectory);
                } catch (IOException e) {
                    IJ.handleException(e);
                }
            }

            // Export icon
            Path iconPath = imageJDir.resolve("jipipe-icon.svg");
            if (!Files.exists(iconPath)) {
                try {
                    InputStream reader = ResourceUtils.getPluginResourceAsStream("logo-square.svg");
                    Files.copy(reader, iconPath);
                } catch (IOException e) {
                    IJ.handleException(e);
                }
            }

            // Write desktop file
            try (PrintWriter writer = new PrintWriter(applicationsDirectory.resolve("jipipe.desktop").toFile())) {
                writer.println("[Desktop Entry]");
                writer.println("Exec=" + "bash -c \"cd '" + imageJDir + "' && '" + scriptFile + "'\"");
                writer.println("Icon=" + imageJDir.resolve("jipipe-icon.svg"));
                writer.println("Name=JIPipe");
                writer.println("Comment=Graphical batch-programming language for ImageJ/Fiji");
                writer.println("NoDisplay=false");
                writer.println("StartupNotify=true");
                writer.println("Type=Application");
                writer.println("Categories=Science;");
                getWorkbench().sendStatusBarText("Created jipipe.desktop in " + applicationsDirectory);
            } catch (FileNotFoundException e) {
                IJ.handleException(e);
            }
        }
    }
}
