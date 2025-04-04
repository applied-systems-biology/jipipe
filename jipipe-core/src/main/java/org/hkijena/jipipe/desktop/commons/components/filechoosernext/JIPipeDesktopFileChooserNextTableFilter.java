package org.hkijena.jipipe.desktop.commons.components.filechoosernext;


import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class JIPipeDesktopFileChooserNextTableFilter extends RowFilter<TableModel, Integer> {

    private final FileNameExtensionFilter extensionFilter;
    private final String textFilter;

    public JIPipeDesktopFileChooserNextTableFilter(FileNameExtensionFilter extensionFilter, String textFilter) {
        this.extensionFilter = processExtensionFilter(extensionFilter);
        this.textFilter = textFilter;
    }

    private static FileNameExtensionFilter processExtensionFilter(FileNameExtensionFilter extensionFilter) {
        if (extensionFilter == null) {
            return null;
        }
        if (extensionFilter.getExtensions().length == 0) {
            return null;
        }
        if (extensionFilter.getExtensions()[0].equals("*")) {
            return null;
        }
        return extensionFilter;
    }

    @Override
    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
        Path path = (Path) entry.getValue(0);
        String fileNameLc = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if (extensionFilter != null && !Files.isDirectory(path)) {
            boolean found = false;
            for (String extension : extensionFilter.getExtensions()) {
                if (fileNameLc.endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        if (!StringUtils.isNullOrEmpty(textFilter)) {
            if (!fileNameLc.contains(textFilter.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        return true;
    }
}
