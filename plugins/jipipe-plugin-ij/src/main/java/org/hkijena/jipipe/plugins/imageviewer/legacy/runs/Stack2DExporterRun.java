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

package org.hkijena.jipipe.plugins.imageviewer.legacy.runs;

import ij.ImagePlus;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.utils.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Stack2DExporterRun extends AbstractJIPipeRunnable {
    private final JIPipeDesktopLegacyImageViewer viewerPanel;
    private final Path outputFolder;
    private final String baseName;
    private final String formatName;
    private final double magnification;

    public Stack2DExporterRun(JIPipeDesktopLegacyImageViewer viewerPanel, Path outputFolder, String baseName, String formatName) {
        this.viewerPanel = viewerPanel;
        this.outputFolder = outputFolder;
        this.baseName = baseName;
        this.formatName = formatName;
        this.magnification = viewerPanel.getViewerPanel2D().getExportedMagnification();
    }

    @Override
    public String getTaskLabel() {
        return "Image viewer: Export stack";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        if (!Files.isDirectory(outputFolder)) {
            try {
                Files.createDirectories(outputFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ImagePlus image = viewerPanel.getImagePlus();
        progressInfo.setMaxProgress(image.getStackSize());
        for (int c = 0; c < image.getNChannels(); c++) {
            for (int t = 0; t < image.getNFrames(); t++) {
                for (int z = 0; z < image.getNSlices(); z++) {
                    if (progressInfo.isCancelled())
                        return;
                    String fileName = String.format("%sc%d_t%d_z%d.%s", StringUtils.isNullOrEmpty(baseName) ? "" : baseName + "_", c, t, z, formatName.toLowerCase(Locale.ROOT));
                    progressInfo.incrementProgress();
                    progressInfo.log(fileName);
                    BufferedImage bufferedImage = viewerPanel.getViewerPanel2D().generateSlice(c, z,
                            t,
                            magnification, true).getBufferedImage();
                    try {
                        ImageIO.write(bufferedImage, formatName, outputFolder.resolve(fileName).toFile());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public JIPipeDesktopLegacyImageViewer getViewerPanel() {
        return viewerPanel;
    }

    public Path getOutputFolder() {
        return outputFolder;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getFormatName() {
        return formatName;
    }
}
