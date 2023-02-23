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

package org.hkijena.jipipe.ui.cache.exporters;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class JIPipeDataTableToZIPExporterRun extends JIPipeWorkbenchPanel implements JIPipeRunnable {

    private final Path outputZipFile;
    private JIPipeDataTable dataTable;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    /**
     * @param workbench     the workbench
     * @param outputZipFile the output zip file
     * @param dataTable     the data table to save
     */
    public JIPipeDataTableToZIPExporterRun(JIPipeWorkbench workbench, Path outputZipFile, JIPipeDataTable dataTable) {
        super(workbench);
        this.outputZipFile = outputZipFile;
        this.dataTable = dataTable;
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    @Override
    public void run() {
        try {
            progressInfo.setMaxProgress(1);

            try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, outputZipFile)) {
                dataTable.exportData(storage, progressInfo);
            } catch (Exception e) {
                IJ.handleException(e);
                progressInfo.log(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }

            progressInfo.incrementProgress();
        }
        finally {
            dataTable = null;
        }
    }

    @Subscribe
    public void onFinished(RunWorkerFinishedEvent event) {
        if (event.getRun() == this) {
            if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                    "The data was successfully exported to " + outputZipFile + ". Do you want to open the file?",
                    "Export data table as *.zip",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                UIUtils.openFileInNative(outputZipFile);
            }
        }
    }

    @Subscribe
    public void onInterrupted(RunWorkerInterruptedEvent event) {
        if (event.getRun() == this) {
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Could not export slot data to " + outputZipFile + ". Please take a look at the log (Tools > Logs) to find out more.", "Export slot data", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JIPipeDataTable getDataTable() {
        return dataTable;
    }

    public Path getOutputZipFile() {
        return outputZipFile;
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
        return "Export data table to *.zip";
    }
}
