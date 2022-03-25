package org.hkijena.jipipe.ui.components.pickers;

import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.ui.components.renderers.ImageJDataImporterListCellRenderer;

import java.awt.*;
import java.util.Comparator;

public class ImageJDataImporterPicker extends PickerDialog<ImageJDataImporter> {
    public ImageJDataImporterPicker(Window parent) {
        super(parent);
        setCellRenderer(new ImageJDataImporterListCellRenderer());
        setItemComparator(Comparator.comparing(ImageJDataImporter::getName));
    }

    @Override
    protected String getSearchString(ImageJDataImporter item) {
        return item.getName();
    }
}
