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

package org.hkijena.jipipe.desktop.commons.components.filechoosernative.windows;

import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooser;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooserImplementation;
import org.hkijena.jipipe.desktop.commons.components.filechoosernative.ModernNativeFileChooserResponse;
import org.hkijena.jipipe.utils.PathIOMode;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class ModernNativeFileChooserWindows implements ModernNativeFileChooserImplementation {
    private final ModernNativeFileChooser fileChooser;

    public ModernNativeFileChooserWindows(ModernNativeFileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    @Override
    public ModernNativeFileChooserResponse showFileChooser(Window parent, PathIOMode action) {
        final WindowsFileChooser fc = new WindowsFileChooser(fileChooser.getCurrentDirectory());
        fc.setFilters(fileChooser.getFiltersAsArray());
        fc.setMultiSelectionEnabled(fileChooser.isMultiSelectionEnabled());
        if (!fileChooser.getDefaultFile().isEmpty()) {
            fc.setDefaultFilename(fileChooser.getDefaultFile());
        }

        if (!fileChooser.getDialogTitle().isEmpty()) {
            fc.setTitle(fileChooser.getDialogTitle());
        }

        final ModernNativeFileChooserResponse result = fc.showDialog(parent, action == PathIOMode.Open);
        if (result == ModernNativeFileChooserResponse.OK) {
            fileChooser.setSelectedFiles(fileChooser.isMultiSelectionEnabled() ? fc.getSelectedFiles() : new File[]{fc.getSelectedFile()});
            fileChooser.setCurrentDirectory(fc.getCurrentDirectory());
        }
        return result;
    }

    @Override
    public ModernNativeFileChooserResponse showFolderBrowser(Window parent) {
        final WindowsFolderBrowser fb = new WindowsFolderBrowser();
        if (!StringUtils.isNullOrEmpty(fileChooser.getDialogTitle())) {
            fb.setTitle(fileChooser.getDialogTitle());
        }
        final File file = fb.showDialog(parent);
        if (file != null) {
            fileChooser.setSelectedFiles(new File[]{file});
            fileChooser.setCurrentDirectory(file.getParentFile() != null ?
                    file.getParentFile() : file);
            return ModernNativeFileChooserResponse.OK;
        }

        return ModernNativeFileChooserResponse.Cancelled;
    }
}
