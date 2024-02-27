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

package org.hkijena.jipipe.extensions.imageviewer.plugins2d;

import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel2D;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewer2DCompositeLayerEditor;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CompositeManagerPlugin2D extends GeneralImageViewerPanelPlugin2D {
    public CompositeManagerPlugin2D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImage() != null && getCurrentImage().getNChannels() > 1) {
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Composite display", UIUtils.getIconFromResources("actions/channelmixer.png"));
            JToggleButton compositeToggle = new JToggleButton("Enabled", UIUtils.getIconFromResources("actions/eye.png"));
            compositeToggle.setSelected(getViewerPanel2D().isComposite());
            compositeToggle.addActionListener(e -> getViewerPanel2D().setComposite(compositeToggle.isSelected()));
            headerPanel.addColumn(compositeToggle);

            for (ImageViewerPanel2D.CompositeLayer layer : getViewerPanel2D().getOrderedCompositeBlendLayers()) {
                ImageViewer2DCompositeLayerEditor layerEditor = new ImageViewer2DCompositeLayerEditor(getViewerPanel2D(), layer.getChannel());
                formPanel.addWideToForm(layerEditor);
            }
        }
    }
}
