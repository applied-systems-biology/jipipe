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

package org.hkijena.jipipe.extensions.imagejdatatypes.viewer;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;

import java.nio.file.Path;

public class ImageViewerPanelRawImageExporterRun implements JIPipeRunnable {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final ImagePlus imagePlus;
    private final Path outputPath;

    public ImageViewerPanelRawImageExporterRun(ImagePlus imagePlus, Path outputPath) {
        this.imagePlus = imagePlus;
        this.outputPath = outputPath;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Export raw image";
    }

    @Override
    public void run() {
        progressInfo.setProgress(0, 1);
        progressInfo.log("Saving " + imagePlus + " to " + outputPath);
        IJ.saveAs(imagePlus, "TIFF", outputPath.toString());
        progressInfo.incrementProgress();
    }
}
