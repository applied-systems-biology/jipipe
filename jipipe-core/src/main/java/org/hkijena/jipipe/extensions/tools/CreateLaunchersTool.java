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

import com.google.common.base.Charsets;
import ij.IJ;
import ij.Prefs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@JIPipeOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu)
public class CreateLaunchersTool extends MenuExtension {
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

    private void createLaunchers() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        if(SystemUtils.IS_OS_LINUX)
            createLinuxLauncher(imageJDir);
        else if(SystemUtils.IS_OS_WINDOWS)
            createWindowsLauncher(imageJDir);
        else if(SystemUtils.IS_OS_MAC_OSX)
            createOSXLauncher(imageJDir);
        else
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Your operating system is not supported!", "Create launcher", JOptionPane.ERROR_MESSAGE);
    }

    private void createOSXLauncher(Path imageJDir) {
        try(PrintWriter writer = new PrintWriter(imageJDir.resolve("start-jipipe-linux.sh").toFile())) {
            writer.println("#!/bin/bash");
            writer.println("IMAGEJ_EXECUTABLE=$(find . -name \"ImageJ-*\" -print -quit)");
            writer.println("eval $IMAGEJ_EXECUTABLE --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher");
        } catch (FileNotFoundException e) {
            IJ.handleException(e);
        }
    }

    private void createWindowsLauncher(Path imageJDir) {
        JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Your operating system is not supported!", "Create launcher", JOptionPane.ERROR_MESSAGE);
    }

    private void createLinuxLauncher(Path imageJDir) {
        try(PrintWriter writer = new PrintWriter(imageJDir.resolve("start-jipipe-linux.sh").toFile())) {
            writer.println("#!/bin/bash");
            writer.println("IMAGEJ_EXECUTABLE=$(find . -name \"ImageJ-*\" -print -quit)");
            writer.println("eval $IMAGEJ_EXECUTABLE --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher");
        } catch (FileNotFoundException e) {
            IJ.handleException(e);
        }
        if(JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                "The launcher was created. Do you want to add a entry into the application menu?",
                "Create launchers",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            String dataHome = System.getenv().getOrDefault("XDG_DATA_HOME", FileUtils.getUserDirectory().toPath().resolve(".local").resolve("share").toString());
            Path applicationsDirectory = Paths.get(dataHome).resolve("applications");
            if(!Files.exists(applicationsDirectory)) {
                try {
                    Files.createDirectories(applicationsDirectory);
                } catch (IOException e) {
                    IJ.handleException(e);
                }
            }

            // Export icon
            Path iconPath = imageJDir.resolve("jipipe-icon.svg");
            if(!Files.exists(iconPath)) {
                try {
                    InputStream reader = ResourceUtils.getPluginResourceAsStream("logo-square.svg");
                    Files.copy(reader, iconPath);
                } catch (IOException e) {
                    IJ.handleException(e);
                }
            }

            // Write desktop file
            try(PrintWriter writer = new PrintWriter(applicationsDirectory.resolve("jipipe.desktop").toFile())) {
                writer.println("[Desktop Entry]");
                writer.println("Exec=" + "cd \"" + imageJDir + "\" && \"" + imageJDir.resolve("start-jipipe-linux.sh") + "\"");
                writer.println("Icon=\"" + imageJDir.resolve("jipipe-icon.svg") + "\"");
                writer.println("Name=JIPipe");
                writer.println("NoDisplay=false");
                writer.println("StartupNotify=true");
                writer.println("Type=Application");
                writer.println("Categories=Education;Science;");
            } catch (FileNotFoundException e) {
                IJ.handleException(e);
            }
        }
    }
}
