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

package org.hkijena.jipipe;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.ui.components.ImageViewerPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin(type = Command.class, menuPath = "Plugins>JIPipe>JIPipe Image Viewer")
public class JIPipeImageViewerGUICommand implements Command {
    @Override
    public void run() {
        ImagePlus image = IJ.getImage();
        UIUtils.loadLookAndFeelFromSettings();
        SwingUtilities.invokeLater(() -> {
            ImageViewerPanel dataDisplay = new ImageViewerPanel();
            JFrame frame = new JFrame(image.getTitle());
            frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
            frame.setContentPane(dataDisplay);
            frame.pack();
            frame.setSize(1024, 768);
            frame.setVisible(true);
            SwingUtilities.invokeLater(dataDisplay::fitImageToScreen);
        });
    }
}
