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

package org.hkijena.jipipe.ui.documentation;

import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.WebUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadOfflineManualRun implements JIPipeRunnable {

    public static final String DOWNLOAD_URL = "https://github.com/applied-systems-biology/jipipe/releases/download/current/jipipe_offline_documentation.zip";
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

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
        return "Download offline manual";
    }

    @Override
    public void run() {
        progressInfo.setMaxProgress(2);
        Path targetDirectory = PathUtils.getJIPipeUserDir().resolve("jipipe").resolve("offline-manual");
        Path targetFile = targetDirectory.resolve("jipipe_offline_documentation.zip");
        if (!Files.isDirectory(targetDirectory)) {
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        URL url;
        try {
            url = new URL(DOWNLOAD_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        progressInfo.setProgress(0);
        progressInfo.log("Downloading " + url + " into " + targetFile);
        WebUtils.download(url, targetFile, getTaskLabel(), progressInfo);

        // Unzip the target file
        progressInfo.setProgress(1);
        try {
            ArchiveUtils.decompressZipFile(targetFile, targetFile.getParent(), progressInfo.resolve("Extracting"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        progressInfo.setProgress(2);
    }
}
