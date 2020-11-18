package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import com.google.common.eventbus.Subscribe;
import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeResultSlotDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ImageViewerPanel;
import org.hkijena.jipipe.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.ImageJUtils;
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
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        OMEImageResultImportRun run = new OMEImageResultImportRun(slot, row, rowStorageFolder, compartmentName, algorithmName, displayName, workbench);
        knownRuns.add(run);
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return null;
    }

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        if(event.getRun() instanceof OMEImageResultImportRun) {
            OMEImageResultImportRun run = (OMEImageResultImportRun) event.getRun();
            if(!knownRuns.contains(run))
                return;
           run.getImage().display(run.getDisplayName(), run.getWorkbench(), new JIPipeResultSlotDataSource(run.getSlot(), run.getRow(), run.getRowStorageFolder()));
        }
    }
}
