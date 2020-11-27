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

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.AVI_Writer;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.SliceIndex;

import java.io.IOException;
import java.nio.file.Path;

public class ImageViewerVideoExporterRun implements JIPipeRunnable {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final ImageViewerPanel viewerPanel;
    private final Path outputFile;
    private final SliceIndex referencePosition;
    private final HyperstackDimension followedDimension;
    private final int timePerFrame;
    private final AVICompression compression;
    private final int jpegQuality;

    public ImageViewerVideoExporterRun(ImageViewerPanel viewerPanel, Path outputFile, SliceIndex referencePosition, HyperstackDimension followedDimension, int timePerFrame, AVICompression compression, int jpegQuality) {
        this.viewerPanel = viewerPanel;
        this.outputFile = outputFile;
        this.referencePosition = referencePosition;
        this.followedDimension = followedDimension;
        this.timePerFrame = timePerFrame;
        this.compression = compression;
        this.jpegQuality = jpegQuality;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Image viewer: Export stack";
    }

    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public void run() {
        ImagePlus image = viewerPanel.getImage();
        ImageStack generatedStack = new ImageStack(image.getWidth(), image.getHeight());

        if(followedDimension == HyperstackDimension.Depth) {
            progressInfo.setMaxProgress(image.getNSlices());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int z = 0; z < image.getNSlices(); z++) {
                if(progressInfo.isCancelled().get())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("z = " + z);
                generatedStack.addSlice(viewerPanel.generateSlice(z, referencePosition.getC(), referencePosition.getT(), true, true));
            }
        }
        else  if(followedDimension == HyperstackDimension.Channel) {
            progressInfo.setMaxProgress(image.getNChannels());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int c = 0; c < image.getNChannels(); c++) {
                if(progressInfo.isCancelled().get())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("c = " + c);
                generatedStack.addSlice(viewerPanel.generateSlice(referencePosition.getZ(), c, referencePosition.getT(), true, true));
            }
        }
        else  if(followedDimension == HyperstackDimension.Frame) {
            progressInfo.setMaxProgress(image.getNFrames());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int t = 0; t < image.getNFrames(); t++) {
                if(progressInfo.isCancelled().get())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("t = " + t);
                generatedStack.addSlice(viewerPanel.generateSlice(referencePosition.getZ(), referencePosition.getC(), t, true, true));
            }
        }

        ImagePlus combined = new ImagePlus("video", generatedStack);
        combined.getCalibration().fps = 1.0 / timePerFrame * 1000;
        progressInfo.log("Writing AVI with " + Math.round(combined.getCalibration().fps) + "FPS");
        AVI_Writer writer = new AVI_Writer();
        try {
            writer.writeImage(combined, outputFile.toString(), compression.getNativeValue(), jpegQuality);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ImageViewerPanel getViewerPanel() {
        return viewerPanel;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public HyperstackDimension getFollowedDimension() {
        return followedDimension;
    }

    public SliceIndex getReferencePosition() {
        return referencePosition;
    }

    public int getTimePerFrame() {
        return timePerFrame;
    }

    public AVICompression getCompression() {
        return compression;
    }

    public int getJpegQuality() {
        return jpegQuality;
    }
}
