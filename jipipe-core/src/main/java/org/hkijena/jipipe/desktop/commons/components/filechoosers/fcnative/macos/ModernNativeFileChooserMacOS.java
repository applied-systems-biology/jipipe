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

import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcnative.ModernNativeFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcnative.ModernNativeFileChooserImplementation;
import org.hkijena.jipipe.desktop.commons.components.filechoosers.fcnative.ModernNativeFileChooserResponse;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Locale;

public class ModernNativeFileChooserMacOS implements ModernNativeFileChooserImplementation {

    private final ModernNativeFileChooser fileChooser;

    public ModernNativeFileChooserMacOS(ModernNativeFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    @Override
    public ModernNativeFileChooserResponse showFileChooser(Window parent, PathIOMode action) {
        int mode = action == PathIOMode.Open ? FileDialog.LOAD : FileDialog.SAVE;
        String title = StringUtils.orElse(fileChooser.getDialogTitle(), action == PathIOMode.Open ? "Open" : "Save");
        FileDialog fd = new FileDialog((Frame) parent, title, mode);
        fd.setMultipleMode(fileChooser.isMultiSelectionEnabled());

        if (!StringUtils.isNullOrEmpty(fileChooser.getDefaultFile())) {
            fd.setFile(fileChooser.getDefaultFile());
        }

        if (fileChooser.getCurrentDirectory() != null) {
            if (fileChooser.getCurrentDirectory().isDirectory()) {
                fd.setDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
            } else {
                fd.setDirectory(fileChooser.getCurrentDirectory().getParent());
            }
        }

        fd.setFilenameFilter((dir, filename) -> {
            if (fileChooser.getFilters().isEmpty()) {
                return true;
            }

            for (FileNameExtensionFilter filter : fileChooser.getFilters()) {
                for (String extension : filter.getExtensions()) {
                    if (filename.toLowerCase(Locale.ROOT).endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
            return false;
        });

        fd.setVisible(true);

        if (fd.getFile() != null) {
            fileChooser.setSelectedFiles(fileChooser.isMultiSelectionEnabled() ? fd.getFiles() : new File[]{new File(fd.getDirectory(), fd.getFile())});
            fileChooser.setCurrentDirectory(new File(fd.getDirectory()));
            return ModernNativeFileChooserResponse.OK;
        }

        return ModernNativeFileChooserResponse.Cancelled;
    }

    @Override
    public ModernNativeFileChooserResponse showFolderBrowser(Window parent) {
        String title = StringUtils.orElse(fileChooser.getDialogTitle(), "Open");
        FileDialog fd = new FileDialog((Frame) parent, title, FileDialog.LOAD);

        try {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            if (fd.getFile() != null) {
                fileChooser.setSelectedFiles(new File[]{new File(fd.getDirectory(), fd.getFile())});
                fileChooser.setCurrentDirectory(new File(fd.getDirectory()));
                return ModernNativeFileChooserResponse.OK;
            }
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
        }

        return ModernNativeFileChooserResponse.Cancelled;
    }
}
