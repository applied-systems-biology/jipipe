package org.hkijena.jipipe.ui.components.pickers;

import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.ui.components.renderers.ImageJDataExporterListCellRenderer;

import java.awt.*;
import java.util.Comparator;

public class ImageJDataExporterPicker extends PickerDialog<ImageJDataExporter> {
    public ImageJDataExporterPicker(Window parent) {
        super(parent);
        setCellRenderer(new ImageJDataExporterListCellRenderer());
        setItemComparator(Comparator.comparing(ImageJDataExporter::getName));
    }

    @Override
    protected String getSearchString(ImageJDataExporter item) {
        return item.getName();
    }
}
