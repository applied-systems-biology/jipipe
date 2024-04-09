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
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.plugins.settings.DataExporterSettings;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JIPipeDesktopDataTableToFilesByMetadataExporterRun extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable {

    private final List<? extends JIPipeDataTable> dataTables;
    private final Settings settings;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Path outputPath;

    /**
     * @param workbench   the workbench
     * @param dataTables  the slots to save
     * @param splitBySlot if slots should be split
     */
    public JIPipeDesktopDataTableToFilesByMetadataExporterRun(JIPipeDesktopWorkbench workbench, List<? extends JIPipeDataTable> dataTables, boolean splitBySlot) {
        super(workbench);
        this.dataTables = new ArrayList<>(dataTables);
        this.settings = new Settings();
        settings.splitBySlotName = splitBySlot;
    }

    /**
     * Opens the necessary dialogs
     *
     * @return if setup was confirmed
     */
    public boolean setup() {
        outputPath = FileChooserSettings.openDirectory(this, FileChooserSettings.LastDirectoryKey.Data, "Export data as files");
        if (outputPath == null)
            return false;

        JDialog editorDialog = new JDialog();
        JPanel mainPanel = new JPanel(new BorderLayout());
        JIPipeDesktopParameterPanel parameterPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(), settings, null, JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION);
        mainPanel.add(parameterPanel, BorderLayout.CENTER);
        AtomicBoolean confirmation = new AtomicBoolean(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            confirmation.set(false);
            editorDialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        confirmButton.addActionListener(e -> {
            confirmation.set(true);
            editorDialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        editorDialog.setTitle("Export data as files");
        editorDialog.setContentPane(mainPanel);
        editorDialog.setModal(true);
        editorDialog.pack();
        editorDialog.setSize(800, 600);
        editorDialog.setLocationRelativeTo(null);
        editorDialog.setVisible(true);

        if (confirmation.get() && settings.rememberSettings) {
            try {
                DataExporterSettings.getInstance().copyFrom(settings.exporter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return confirmation.get();
    }

    @Override
    public void run() {
        try {
            Set<String> existing = new HashSet<>();
            progressInfo.setMaxProgress(dataTables.size());
            for (int i = 0; i < dataTables.size(); i++) {
                progressInfo.setProgress(i + 1);
                JIPipeProgressInfo subProgress = progressInfo.resolveAndLog("Slot", i, dataTables.size());
                JIPipeDataTable slot = dataTables.get(i);
                Path targetPath = outputPath;
                if (settings.splitBySlotName) {
                    targetPath = outputPath.resolve(StringUtils.makeUniqueString(slot.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, ""), " ", existing));
                }
                try {
                    if (!Files.isDirectory(targetPath))
                        Files.createDirectories(targetPath);
                    settings.exporter.writeToFolder(slot, targetPath, subProgress.resolve("Slot " + slot.getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "")));
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
        return "Export cached data to files";
    }

    public static class Settings extends AbstractJIPipeParameterCollection {
        private JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter(DataExporterSettings.getInstance());
        private boolean splitBySlotName = false;

        private boolean rememberSettings = false;

        public Settings() {
        }

        @SetJIPipeDocumentation(name = "File name generation", description = "Here you can determine how file names are generated.")
        @JIPipeParameter("exporter")
        public JIPipeDataByMetadataExporter getExporter() {
            return exporter;
        }

        public void setExporter(JIPipeDataByMetadataExporter exporter) {
            this.exporter = exporter;
        }

        @SetJIPipeDocumentation(name = "Split by output name", description = "If enabled, the exporter will attempt to split data by their output name. Has no effect if the exported data table is not an output of a node.")
        @JIPipeParameter("split-by-slot-name")
        public boolean isSplitBySlotName() {
            return splitBySlotName;
        }

        @JIPipeParameter("split-by-slot-name")
        public void setSplitBySlotName(boolean splitBySlotName) {
            this.splitBySlotName = splitBySlotName;
        }

        @SetJIPipeDocumentation(name = "Remember settings", description = "If enabled, remember the file exporter settings for later")
        @JIPipeParameter("remember-settings")
        public boolean isRememberSettings() {
            return rememberSettings;
        }

        @JIPipeParameter("remember-settings")
        public void setRememberSettings(boolean rememberSettings) {
            this.rememberSettings = rememberSettings;
        }
    }
}
