package org.hkijena.acaq5.ui;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.acaq5.ui.components.ImageLogo;
import org.hkijena.acaq5.ui.components.ImagePlusExternalPreviewer;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ACAQInfoUI extends ACAQUIPanel {

    public ACAQInfoUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader reader = new MarkdownReader(true);
        reader.loadFromResource("documentation/introduction.md");
        add(reader, BorderLayout.CENTER);
    }
}
