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

package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Imports an {@link ImagePlus}
 */
public class ImagePlusWindowImageJImporterUI extends ImageJDataImporterUI {

    private JComboBox<ImagePlus> imageSelection;

    /**
     * @param importer the importer
     */
    public ImagePlusWindowImageJImporterUI(JIPipeDesktopWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
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
        editorComponent.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                onImageSelected();
            }
        });
        imageSelection.setToolTipText("The name of the image that should be imported. If you leave this empty, the currently active image will be used.");
        add(imageSelection, BorderLayout.CENTER);

        JButton reloadButton = new JButton(UIUtils.getIconFromResources("actions/view-refresh.png"));
        reloadButton.setToolTipText("Reload list of available images");
        reloadButton.addActionListener(e -> reloadList());
        UIUtils.makeFlat25x25(reloadButton);
        add(reloadButton, BorderLayout.EAST);
    }

    private void onImageSelected() {
        if (imageSelection.getSelectedItem() instanceof ImagePlus) {
            ImagePlus img = (ImagePlus) imageSelection.getSelectedItem();
            getImporter().setName(img.getTitle());
        } else {
            getImporter().setName(StringUtils.nullToEmpty(imageSelection.getSelectedItem()));
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
