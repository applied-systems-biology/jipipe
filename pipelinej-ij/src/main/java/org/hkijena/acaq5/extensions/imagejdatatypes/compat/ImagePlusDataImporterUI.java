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

package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Imports an {@link ImagePlus}
 */
public class ImagePlusDataImporterUI extends ImageJDatatypeImporterUI {

    private JComboBox<ImagePlus> imageSelection;

    /**
     * @param importer the importer
     */
    public ImagePlusDataImporterUI(ImageJDatatypeImporter importer) {
        super(importer);
        initialize();
        reloadList();
        onImageSelected();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        imageSelection = new JComboBox<>(new DefaultComboBoxModel<>());
        imageSelection.setEditable(true);
        imageSelection.addItemListener(e -> onImageSelected());
        JTextComponent editorComponent = (JTextComponent) imageSelection.getEditor().getEditorComponent();
        editorComponent.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                onImageSelected();
            }
        });
        imageSelection.setToolTipText("The name of the image that should be imported. If you leave this empty, the currently active image will be used.");
        add(imageSelection, BorderLayout.CENTER);

        JButton reloadButton = new JButton(UIUtils.getIconFromResources("refresh.png"));
        reloadButton.setToolTipText("Reload list of available images");
        reloadButton.addActionListener(e -> reloadList());
        UIUtils.makeFlat25x25(reloadButton);
        add(reloadButton, BorderLayout.EAST);
    }

    private void onImageSelected() {
        if (imageSelection.getSelectedItem() instanceof ImagePlus) {
            ImagePlus img = (ImagePlus) imageSelection.getSelectedItem();
            getImporter().setParameters(img.getTitle());
        } else {
            getImporter().setParameters("" + imageSelection.getSelectedItem());
        }
    }

    private void reloadList() {
        Object currentSelection = imageSelection.getSelectedItem();
        DefaultComboBoxModel<ImagePlus> model = (DefaultComboBoxModel<ImagePlus>) imageSelection.getModel();
        model.removeAllElements();
        boolean foundSelection = false;
        boolean foundCurrentImage = false;
        for (int i = 1; i <= WindowManager.getImageCount(); ++i) {
            ImagePlus img = WindowManager.getImage(i);
            model.addElement(img);
            if (img == currentSelection)
                foundSelection = true;
            if (img == WindowManager.getCurrentImage())
                foundCurrentImage = true;
        }
        if (foundSelection) {
            imageSelection.setSelectedItem(currentSelection);
        } else if (WindowManager.getCurrentImage() != null && foundCurrentImage) {
            imageSelection.setSelectedItem(WindowManager.getCurrentImage());
        } else if (model.getSize() > 0) {
            imageSelection.setSelectedIndex(0);
        }
    }
}
