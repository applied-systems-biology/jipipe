package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ImagePlusDataImportIntoImageJOperation implements JIPipeDataImportOperation {

    private Set<ImagePlusResultImportRun> knownRuns = new HashSet<>();

    public ImagePlusDataImportIntoImageJOperation() {
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        ImagePlusResultImportRun run = new ImagePlusResultImportRun(slot, row, rowStorageFolder, compartmentName, algorithmName, displayName, workbench);
        knownRuns.add(run);
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return null;
    }

    @Override
    public String getName() {
        return "Import into ImageJ";
    }

    @Override
    public String getDescription() {
        return "Imports the image into ImageJ";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/imagej.png");
    }

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() instanceof ImagePlusResultImportRun) {
            ImagePlusResultImportRun run = (ImagePlusResultImportRun) event.getRun();
            if (!knownRuns.contains(run))
                return;
            ImagePlusData data = new ImagePlusData(run.getImage());
            data.display(run.getDisplayName(), run.getWorkbench(), new JIPipeResultSlotDataSource(run.getSlot(), run.getRow(), run.getRowStorageFolder()));
        }
    }
}
