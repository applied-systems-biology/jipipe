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

package org.hkijena.jipipe.extensions.parameters.library.images;

import ij.IJ;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.extensions.parameters.library.references.IconRefDesktopParameterEditorUI;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopIconPickerDialog;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JLabel imagePreview = new JLabel();


    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public ImageDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(imagePreview, BorderLayout.WEST);

        JButton importButton = new JButton("Import", UIUtils.getIconFromResources("actions/document-import.png"));
        JPopupMenu importMenu = UIUtils.addPopupMenuToButton(importButton);
        add(importButton, BorderLayout.CENTER);

        JMenuItem openImageItem = new JMenuItem("Open from file ...", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openImageItem.addActionListener(e -> importImageFile());
        importMenu.add(openImageItem);

        JMenuItem importIconItem = new JMenuItem("Load icon", UIUtils.getIconFromResources("actions/view_icon.png"));
        importIconItem.addActionListener(e -> importIcon());
        importMenu.add(importIconItem);
    }

    private void importIcon() {
        String picked = JIPipeDesktopIconPickerDialog.showDialog(this, ResourceUtils.getResourcePath("icons"), IconRefDesktopParameterEditorUI.getAvailableIcons());
        if (picked != null) {
            ImageIcon icon = UIUtils.getIconFromResources(picked);
            BufferedImage bufferedImage = BufferedImageUtils.toBufferedImage(icon.getImage(), BufferedImage.TYPE_INT_ARGB);
            importImage(bufferedImage);
        }
    }

    private void importImageFile() {
        Path path = FileChooserSettings.openFile(getDesktopWorkbench().getWindow(),
                FileChooserSettings.LastDirectoryKey.External,
                "Open image",
                UIUtils.EXTENSION_FILTER_IMAGEIO_IMAGES);
        if (path != null) {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                importImage(image);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
    }

    private void importImage(BufferedImage image) {
        ImageParameterSettings annotation = getParameterAccess().getAnnotationOfType(ImageParameterSettings.class);
        int maxWidth = -1;
        int maxHeight = -1;
        if (annotation != null) {
            maxWidth = annotation.maxWidth();
            maxHeight = annotation.maxHeight();
        }
        double scale = 1.0;
        if (maxWidth > 0) {
            scale = 1.0 * maxWidth / image.getWidth();
        }
        if (maxHeight > 0) {
            scale = Math.min(1.0 * maxHeight / image.getHeight(), scale);
        }
        if (scale != 1.0) {
            Image scaledInstance = image.getScaledInstance((int) (image.getWidth() * scale), (int) (image.getHeight() * scale), Image.SCALE_DEFAULT);
            image = BufferedImageUtils.toBufferedImage(scaledInstance, BufferedImage.TYPE_INT_ARGB);
        }

        ImageParameter parameter = new ImageParameter();
        parameter.setImage(image);
        setParameter(parameter, true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ImageParameter parameter = getParameter(ImageParameter.class);
        if (parameter.getImage() == null) {
            imagePreview.setText("NA");
            imagePreview.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        } else {
            imagePreview.setText("");
            BufferedImage thumbnail = BufferedImageUtils.scaleImageToFit(parameter.getImage(), 64, 64);
            imagePreview.setIcon(new ImageIcon(thumbnail));
        }
    }
}
