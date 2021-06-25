package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class OMEImageDataImportIntoImageJOperation implements JIPipeDataImportOperation {

    private Set<OMEImageResultImportRun> knownRuns = new HashSet<>();

    public OMEImageDataImportIntoImageJOperation() {
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    @Override
    public String getName() {
        return "Bio formats import (ImageJ viewer)";
    }

    @Override
    public String getDescription() {
        return "Imports the file via Bio formats using JIPipe's default settings";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/bioformats.png");
    }

    @Override
    public String getId() {
        return "jipipe:import-image-bio-formats-into-imagej";
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTableRow row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        OMEImageResultImportRun run = new OMEImageResultImportRun(slot, row, rowStorageFolder, compartmentName, algorithmName, displayName, workbench);
        knownRuns.add(run);
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return null;
    }

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() instanceof OMEImageResultImportRun) {
            OMEImageResultImportRun run = (OMEImageResultImportRun) event.getRun();
            if (!knownRuns.contains(run))
                return;
            run.getImage().display(run.getDisplayName(), run.getWorkbench(), new JIPipeResultSlotDataSource(run.getSlot(), run.getRow(), run.getRowStorageFolder()));
        }
    }
}
