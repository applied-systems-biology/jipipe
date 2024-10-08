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

package org.hkijena.jipipe.desktop.app.cache.exporters;

import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JIPipeDesktopDataTableToOutputExporterRun extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable {

    private final UUID uuid = UUID.randomUUID();
    private final Path outputPath;
    private final List<? extends JIPipeDataTable> dataTables;
    private final boolean splitBySlot;
    private final boolean clearDirectory;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    /**
     * @param workbench      the workbench
     * @param outputPath     the output folder
     * @param dataTables     the slots to save
     * @param splitBySlot    if slots should be split
     * @param clearDirectory if the directory contents should be cleared
     */
    public JIPipeDesktopDataTableToOutputExporterRun(JIPipeDesktopWorkbench workbench, Path outputPath, List<? extends JIPipeDataTable> dataTables, boolean splitBySlot, boolean clearDirectory) {
        super(workbench);
        this.outputPath = outputPath;
        this.dataTables = new ArrayList<>(dataTables);
        this.splitBySlot = splitBySlot;
        this.clearDirectory = clearDirectory;
    }

    @Override
    public UUID getRunUUID() {
        return uuid;
    }

    @Override
    public void run() {
        try {
            Set<String> existing = new HashSet<>();
            progressInfo.setMaxProgress(dataTables.size());

            if (clearDirectory) {
                try {
                    if (Files.isDirectory(outputPath) && Files.list(outputPath).findAny().isPresent()) {
                        if (Files.isDirectory(outputPath)) {
                            PathUtils.deleteDirectoryRecursively(outputPath, getProgressInfo().resolve("Delete existing files"));
                        }
                        Files.createDirectories(outputPath);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            for (int i = 0; i < dataTables.size(); i++) {
                progressInfo.setProgress(i);
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Slot", i, dataTables.size());
                JIPipeDataTable dataTable = dataTables.get(i);
                Path targetPath = outputPath;
                if (splitBySlot) {
                    targetPath = outputPath.resolve(StringUtils.makeUniqueString(dataTable.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, ""), " ", existing));
                }
                try {
                    if (!Files.isDirectory(targetPath))
                        Files.createDirectories(targetPath);
                    dataTable.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, targetPath), slotProgress);
                } catch (Exception e) {
                    IJ.handleException(e);
                    progressInfo.log(ExceptionUtils.getStackTrace(e));
                    throw new RuntimeException(e);
                }
            }
        } finally {
            dataTables.clear();
        }
    }

    @Override
    public void onFinished(FinishedEvent event) {
        if (event.getRun() == this) {
            if (JOptionPane.showConfirmDialog(getDesktopWorkbench().getWindow(),
                    "The data was successfully exported to " + outputPath + ". Do you want to open the folder?",
                    "Export slot data",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                UIUtils.openFileInNative(outputPath);
            }
        }
    }

    @Override
    public void onInterrupted(InterruptedEvent event) {
        if (event.getRun() == this) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(), "Could not export slot data to " + outputPath + ". Please take a look at the log (Tools > Logs) to find out more.", "Export slot data", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<? extends JIPipeDataTable> getDataTables() {
        return dataTables;
    }

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
        return "Export cached data";
    }
}
