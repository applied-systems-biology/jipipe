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

package org.hkijena.jipipe.desktop.commons.components.filechoosernative.macos;

import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooserImplementation;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooserResponse;
import org.hkijena.jipipe.utils.PathIOMode;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ModernNativeFileChooserMacOS implements ModernNativeFileChooserImplementation {

    private final ModernNativeFileChooser fileChooser;

    public ModernNativeFileChooserMacOS(ModernNativeFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    @Override
    public ModernNativeFileChooserResponse showFileChooser(Window parent, PathIOMode action) {
         int mode = action == PathIOMode.Open ? FileDialog.LOAD : FileDialog.SAVE;
        String title = !dialogTitle.isEmpty() ? dialogTitle : (action == PathIOMode.Open ? "Open" : "Save As");
        FileDialog fd = new FileDialog(parent, title, mode);
        fd.setMultipleMode(multiSelectionEnabled);

        if (!defaultFile.isEmpty())
            fd.setFile(defaultFile);

        if (currentDirectory != null) {
            if (currentDirectory.isDirectory())
                fd.setDirectory(currentDirectory.getAbsolutePath());
            else
                fd.setDirectory(currentDirectory.getParent());
        }

        fd.setFilenameFilter((dir, filename) -> {
            if (filters.isEmpty())
                return true;

            // filterSpec is formatted as [ name, ext1, ext2, ext3, ... ]
            for (String[] filterSpec : filters) {
                for (int i = 1; i < filterSpec.length; i++) {
                    String filter = filterSpec[i].toLowerCase();
                    if (filter.startsWith("*")) {
                        if (filename.toLowerCase().endsWith(filter.substring(1)))
                            return true;
                    } else {
                        if (filename.toLowerCase().endsWith(filter))
                            return true;
                    }
                }
            }
            return false;
        });

        fd.setVisible(true);

        if (fd.getFile() != null) {
            selectedFiles = multiSelectionEnabled ?
                    fd.getFiles() : new File[]{new File(fd.getDirectory(), fd.getFile())};
            currentDirectory = new File(fd.getDirectory());
            return ModernNativeFileChooserResponse.OK;
        }

        return ModernNativeFileChooserResponse.Cancelled;
    }

    @Override
    public ModernNativeFileChooserResponse showFolderBrowser(Window parent) {
        String title = !dialogTitle.isEmpty() ? dialogTitle : "Open";
        FileDialog fd = new FileDialog(parent, title, FileDialog.LOAD);
        if (!dialogTitle.isEmpty())
            fd.setTitle(dialogTitle);

        try {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            if (fd.getFile() != null) {
                selectedFiles = new File[]{new File(fd.getDirectory(), fd.getFile())};
                currentDirectory = new File(fd.getDirectory());
                return ModernNativeFileChooserResponse.OK;
            }
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
        }

        return ModernNativeFileChooserResponse.Cancelled;
    }
}
