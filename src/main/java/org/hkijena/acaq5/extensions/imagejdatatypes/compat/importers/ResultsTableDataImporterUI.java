package org.hkijena.acaq5.extensions.imagejdatatypes.compat.importers;

import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * UI for a {@link org.hkijena.acaq5.extensions.imagejdatatypes.compat.ResultsTableDataImageJAdapter}
 */
public class ResultsTableDataImporterUI extends ImageJDatatypeImporterUI {
    /**
     * @param importer the importer
     */
    public ResultsTableDataImporterUI(ImageJDatatypeImporter importer) {
        super(importer);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("<Active results table>");
        label.setToolTipText("The results table that is currently active");
        add(label, BorderLayout.CENTER);
    }
}
