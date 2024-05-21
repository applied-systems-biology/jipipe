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

package org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageBlendMode;
import org.hkijena.jipipe.plugins.imageviewer.ImageViewerPanel2D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ImageViewer2DCompositeLayerEditor extends JPanel {
    private final ImageViewerPanel2D imageViewerPanel;
    private final int targetChannel;

    private final JSlider opacitySlider = new JSlider(0, 100, 100);

    private final JComboBox<ImageBlendMode> blendModeSelection = new JComboBox<>(ImageBlendMode.values());

    public ImageViewer2DCompositeLayerEditor(ImageViewerPanel2D imageViewerPanel, int targetChannel) {
        this.imageViewerPanel = imageViewerPanel;
        this.targetChannel = targetChannel;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                        BorderFactory.createEmptyBorder(4, 4, 0, 4))));

        // Priority controls
        JPanel priorityControlsPanel = new JPanel(new BorderLayout());

        JButton priorityUpButton = new JButton(UIUtils.getIconFromResources("actions/caret-up.png"));
        UIUtils.makeButtonFlat25x25(priorityUpButton);
        priorityUpButton.addActionListener(e -> moveLayerPriorityUp());
        priorityControlsPanel.add(priorityUpButton, BorderLayout.NORTH);

        JLabel channelLabel = new JLabel("C" + (targetChannel + 1));
//        channelLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        channelLabel.setHorizontalAlignment(SwingConstants.CENTER);
        priorityControlsPanel.add(channelLabel, BorderLayout.CENTER);

        JButton priorityDownButton = new JButton(UIUtils.getIconFromResources("actions/caret-down.png"));
        UIUtils.makeButtonFlat25x25(priorityDownButton);
        priorityDownButton.addActionListener(e -> moveLayerPriorityDown());
        priorityControlsPanel.add(priorityDownButton, BorderLayout.SOUTH);

        priorityControlsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4),
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Button.borderColor"))));

        add(priorityControlsPanel, BorderLayout.WEST);

        // Form panel
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
        formPanel.addToForm(opacitySlider, new JLabel("Opacity"));
        formPanel.addToForm(blendModeSelection, new JLabel("Blending"));

        add(formPanel, BorderLayout.CENTER);

        // Set values from blending
        ImageViewerPanel2D.CompositeLayer layer = imageViewerPanel.getCompositeBlendLayers().getOrDefault(targetChannel, null);
        if (layer != null) {
            opacitySlider.setValue((int) (layer.getOpacity() * 100));
            blendModeSelection.setSelectedItem(layer.getBlendMode());
        }

        // Register events
        opacitySlider.addChangeListener(e -> changeLayerOpacity());
        blendModeSelection.addActionListener(e -> changeLayerBlendMode());
    }

    private void changeLayerBlendMode() {
        ImageViewerPanel2D.CompositeLayer layer = imageViewerPanel.getCompositeBlendLayers().getOrDefault(targetChannel, null);
        if (layer != null) {
            layer.setBlendMode((ImageBlendMode) blendModeSelection.getSelectedItem());
            imageViewerPanel.uploadSliceToCanvas();
        }
    }

    private void changeLayerOpacity() {
        ImageViewerPanel2D.CompositeLayer layer = imageViewerPanel.getCompositeBlendLayers().getOrDefault(targetChannel, null);
        if (layer != null) {
            layer.setOpacity(opacitySlider.getValue() / 100.0);
            imageViewerPanel.uploadSliceToCanvas();
        }
    }

    private void moveLayerPriorityDown() {
        imageViewerPanel.moveCompositePriorityDown(targetChannel);
    }

    private void moveLayerPriorityUp() {
        imageViewerPanel.moveCompositePriorityUp(targetChannel);
    }
}
