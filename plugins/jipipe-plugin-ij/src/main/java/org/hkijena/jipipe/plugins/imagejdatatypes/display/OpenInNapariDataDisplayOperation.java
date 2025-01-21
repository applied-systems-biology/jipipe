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

package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.InlinedJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.napari.NapariOverlay;
import org.hkijena.jipipe.plugins.napari.NapariPlugin;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenInNapariDataDisplayOperation implements JIPipeDesktopDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        ImagePlus image;
        List<Object> overlays = new ArrayList<>();
        if (data instanceof ImagePlusData) {
            image = ((ImagePlusData) data).getImage();
            if (image.getOverlay() != null) {
                ROI2DListData rois = new ROI2DListData();
                for (Roi roi : image.getOverlay()) {
                    rois.add(roi);
                }
                overlays.add(rois);
            }
            overlays.addAll(((ImagePlusData) data).getOverlays());
        } else if (data instanceof OMEImageData) {
            image = ((OMEImageData) data).getImage();
            overlays.add(((OMEImageData) data).getRois());
        } else if (JIPipe.getDataTypes().isConvertible(data.getClass(), ImagePlusData.class)) {
            image = JIPipe.getDataTypes().convert(data, ImagePlusData.class, JIPipeProgressInfo.SILENT).getImage();
        } else {
            throw new UnsupportedOperationException();
        }
        image.setTitle(displayName);

        JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Export");
        queue.runInDialog(desktopWorkbench, desktopWorkbench.getWindow(), new InlinedJIPipeRunnable("Exporting for Napari", (progressInfo) -> {
            Path tmpDir = desktopWorkbench.newTempDirectory("napari");
            List<Path> exportFiles = new ArrayList<>();

            progressInfo.setProgress(0, overlays.size() + 1);

            // Export the image itself
            Path exportedRawFile;
            progressInfo.log("Exporting " + image);
            if (image.getType() != ImagePlus.COLOR_RGB) {
                exportedRawFile = tmpDir.resolve("image.ome.tif");
                OMEImageData.simpleOMEExport(image, exportedRawFile);
            } else {
                exportedRawFile = tmpDir.resolve("image.tif");
                IJ.saveAsTiff(image, exportedRawFile.toString());
            }
            exportFiles.add(exportedRawFile);

            progressInfo.incrementProgress();
            progressInfo.log("Writing overlays ...");

            // Export the overlays (if supported)
            for (int i = 0; i < overlays.size(); i++) {
                Object overlay = overlays.get(i);
                if (overlay instanceof NapariOverlay) {
                    exportFiles.addAll(((NapariOverlay) overlay).exportOverlayToNapari(image, tmpDir, i + "", progressInfo.resolve("Overlay " + (i + 1))));
                }
                progressInfo.incrementProgress();
            }

            progressInfo.log("Napari should open in a few seconds ...");
            progressInfo.setLogToStdOut(true);
            NapariPlugin.launchNapari(desktopWorkbench, exportFiles.stream().map(Path::toString).collect(Collectors.toList()), progressInfo, false);

        }));
    }

    @Override
    public String getId() {
        return "jipipe:open-image-in-napari";
    }

    @Override
    public String getName() {
        return "Open in Napari";
    }

    @Override
    public String getDescription() {
        return "Opens the image in Napari";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/napari.png");
    }
}
