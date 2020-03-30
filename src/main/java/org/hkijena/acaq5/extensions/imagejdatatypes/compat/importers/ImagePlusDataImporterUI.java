package org.hkijena.acaq5.extensions.imagejdatatypes.compat.importers;

import ij.ImagePlus;
import ij.WindowManager;
import org.hkijena.acaq5.api.compat.ImageJDatatypeImporter;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ImagePlusDataImporterUI extends ImageJDatatypeImporterUI {

    private JComboBox<ImagePlus> imageSelection;

    public ImagePlusDataImporterUI(ImageJDatatypeImporter importer) {
        super(importer);
        initialize();
        reloadList();
        onImageSelected();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        imageSelection = new JComboBox<>(new DefaultComboBoxModel<>());
        imageSelection.addItemListener(e -> onImageSelected());
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
            getImporter().setWindowName(img.getTitle());
        } else {
            getImporter().setWindowName(null);
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
