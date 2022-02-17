package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ImagePlusDataImportIntoJIPipeOperation implements JIPipeDataImportOperation {

    private Set<ImagePlusResultImportRun> knownRuns = new HashSet<>();

    public ImagePlusDataImportIntoJIPipeOperation() {
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        ImagePlusResultImportRun run = new ImagePlusResultImportRun(slot, row, rowStorageFolder, compartmentName, algorithmName, displayName, workbench);
        knownRuns.add(run);
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return null;
    }

    @Override
    public String getName() {
        return "Import into JIPipe";
    }

    @Override
    public String getDescription() {
        return "Imports the image into the JIPipe image viewer";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }

    @Override
    public String getId() {
        return "jipipe:import-image-into-jipipe";
    }

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() instanceof ImagePlusResultImportRun) {
            ImagePlusResultImportRun run = (ImagePlusResultImportRun) event.getRun();
            if (!knownRuns.contains(run))
                return;
            ImageViewerPanel.showImage(run.getImage(), run.getDisplayName());
        }
    }
}
