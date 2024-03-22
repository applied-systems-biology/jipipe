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

package org.hkijena.jipipe.extensions.imageviewer.runs;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.AVI_Writer;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.AVICompression;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class Video2DExporterRun implements JIPipeRunnable {
    private final JIPipeImageViewer viewerPanel;
    private final Path outputFile;
    private final ImageSliceIndex referencePosition;
    private final HyperstackDimension followedDimension;
    private final int timePerFrame;
    private final AVICompression compression;
    private final int jpegQuality;
    private final double magnification;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public Video2DExporterRun(JIPipeImageViewer viewerPanel, Path outputFile, ImageSliceIndex referencePosition, HyperstackDimension followedDimension, int timePerFrame, AVICompression compression, int jpegQuality) {
        this.viewerPanel = viewerPanel;
        this.outputFile = outputFile;
        this.referencePosition = referencePosition;
        this.followedDimension = followedDimension;
        this.timePerFrame = timePerFrame;
        this.compression = compression;
        this.jpegQuality = jpegQuality;
        this.magnification = viewerPanel.getViewerPanel2D().getExportedMagnification();
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Image viewer: Export video";
    }

    @Override
    public void run() {
        ImagePlus image = viewerPanel.getImagePlus();
        ImageStack generatedStack = null;

        if (followedDimension == HyperstackDimension.Depth) {
            progressInfo.setMaxProgress(image.getNSlices());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int z = 0; z < image.getNSlices(); z++) {
                if (progressInfo.isCancelled())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("z = " + z);
                BufferedImage bufferedImage = viewerPanel.getViewerPanel2D().generateSlice(referencePosition.getC(), z,
                        referencePosition.getT(),
                        magnification, true).getBufferedImage();
                if (generatedStack == null) {
                    generatedStack = new ImageStack(bufferedImage.getWidth(), bufferedImage.getHeight());
                }
                generatedStack.addSlice(new ColorProcessor(bufferedImage));
            }
        } else if (followedDimension == HyperstackDimension.Channel) {
            progressInfo.setMaxProgress(image.getNChannels());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int c = 0; c < image.getNChannels(); c++) {
                if (progressInfo.isCancelled())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("c = " + c);
                BufferedImage bufferedImage = viewerPanel.getViewerPanel2D().generateSlice(c, referencePosition.getZ(),
                        referencePosition.getT(),
                        magnification, true).getBufferedImage();
                if (generatedStack == null) {
                    generatedStack = new ImageStack(bufferedImage.getWidth(), bufferedImage.getHeight());
                }
                generatedStack.addSlice(new ColorProcessor(bufferedImage));
            }
        } else if (followedDimension == HyperstackDimension.Frame) {
            progressInfo.setMaxProgress(image.getNFrames());
            JIPipeProgressInfo subProgress = progressInfo.resolve("Generating RGB stack");
            for (int t = 0; t < image.getNFrames(); t++) {
                if (progressInfo.isCancelled())
                    return;
                progressInfo.incrementProgress();
                subProgress.log("t = " + t);
                BufferedImage bufferedImage = viewerPanel.getViewerPanel2D().generateSlice(referencePosition.getC(), referencePosition.getZ(),
                        t,
                        magnification, true).getBufferedImage();
                if (generatedStack == null) {
                    generatedStack = new ImageStack(bufferedImage.getWidth(), bufferedImage.getHeight());
                }
                generatedStack.addSlice(new ColorProcessor(bufferedImage));
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

    public JIPipeImageViewer getViewerPanel() {
        return viewerPanel;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public HyperstackDimension getFollowedDimension() {
        return followedDimension;
    }

    public ImageSliceIndex getReferencePosition() {
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
