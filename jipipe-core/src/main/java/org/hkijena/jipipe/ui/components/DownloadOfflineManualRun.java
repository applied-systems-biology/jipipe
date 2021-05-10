package org.hkijena.jipipe.ui.components;

import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.ZipUtils;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Objects;

public class DownloadOfflineManualRun implements JIPipeRunnable {

    public static final String DOWNLOAD_URL = "https://github.com/applied-systems-biology/jipipe/releases/download/2021.5/jipipe_offline_documentation.zip";
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
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        try {
            String[] lastMessage = new String[]{""};
            WebUtils.download(url, targetFile, total -> {
                String message = "Downloaded " + df.format(total / 1024.0 / 1024.0) + " MB";
                if (!Objects.equals(message, lastMessage[0])) {
                    progressInfo.log(message);
                    lastMessage[0] = message;
                }
            }, () -> progressInfo.isCancelled().get());
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e,
                    "Error while downloading!",
                    getTaskLabel(),
                    "There was an error downloading URL '" + url + "' to " + targetFile,
                    "Please check if the URL is valid, an internet connection is available, and the target device has enough space.");
        }

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
