package org.hkijena.jipipe.ui.components;

import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.ZipUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadOfflineManualRun implements JIPipeRunnable {

    public static final String DOWNLOAD_URL = "https://github.com/applied-systems-biology/jipipe/releases/download/1.49.1/jipipe_offline_documentation.zip";
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
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        Path targetDirectory = imageJDir.resolve("jipipe").resolve("offline-manual");
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
            ZipUtils.unzip(targetFile, targetFile.getParent(), progressInfo.resolve("Extracting"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        progressInfo.setProgress(2);
    }
}
