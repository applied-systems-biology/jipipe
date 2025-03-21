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

package org.hkijena.jipipe.desktop.app.resultanalysis;

import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeDataExporterApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class JIPipeResultCopyFilesByMetadataExporterRun extends JIPipeDesktopWorkbenchPanel implements JIPipeRunnable {

    private final List<JIPipeDataSlot> slots;
    private final boolean splitBySlot;
    private final JIPipeDataByMetadataExporter exporter;
    private final UUID uuid = UUID.randomUUID();
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private Path outputPath;

    /**
     * @param workbench   the workbench
     * @param slots       the slots to save
     * @param splitBySlot if slots should be split
     */
    public JIPipeResultCopyFilesByMetadataExporterRun(JIPipeDesktopWorkbench workbench, List<JIPipeDataSlot> slots, boolean splitBySlot) {
        super(workbench);
        this.slots = slots;
        this.splitBySlot = splitBySlot;
        this.exporter = new JIPipeDataByMetadataExporter(JIPipeDataExporterApplicationSettings.getInstance());
    }

    /**
     * Opens the necessary dialogs
     *
     * @return if setup was confirmed
     */
    public boolean setup() {
        outputPath = JIPipeDesktop.openDirectory(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export data as files", HTMLText.EMPTY);
        if (outputPath == null)
            return false;

        JDialog editorDialog = new JDialog();
        JPanel mainPanel = new JPanel(new BorderLayout());
        JIPipeDesktopParameterFormPanel parameterPanel = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), exporter, null, JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION);
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

        if (confirmation.get()) {
            try {
                JIPipeDataExporterApplicationSettings.getInstance().copyFrom(exporter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return confirmation.get();
    }

    @Override
    public void run() {
        Set<String> existingSlots = new HashSet<>();
        Set<String> existingFiles = new HashSet<>();
        Set<String> existingMetadata = new HashSet<>();
        progressInfo.setMaxProgress(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            progressInfo.setProgress(i + 1);
            JIPipeProgressInfo subStatus = progressInfo.resolveAndLog("Slot", i, slots.size());
            JIPipeDataSlot slot = slots.get(i);
            Path targetPath = outputPath;
            if (splitBySlot) {
                targetPath = outputPath.resolve(StringUtils.makeUniqueString(slot.getNode().getAliasIdInParentGraph() + "-" + slot.getName(), " ", existingSlots));
                existingFiles.clear();
            }
            existingMetadata.clear();
            try {
                if (!Files.isDirectory(targetPath))
                    Files.createDirectories(targetPath);

                // Load the data table
                JIPipeDataTableInfo dataTable = JIPipeDataTableInfo.loadFromJson(slot.getSlotStoragePath().resolve("data-table.json"));
                for (int row = 0; row < dataTable.getRowCount(); row++) {
                    JIPipeProgressInfo rowSubStatus = subStatus.resolveAndLog("Row", row, dataTable.getRowCount());
                    String metadataString = exporter.generateName(dataTable, row, existingMetadata);

                    Path rowStoragePath = slot.getRowStoragePath(row);
                    Path finalTargetPath = targetPath;
                    Files.walkFileTree(rowStoragePath, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String newFileName = metadataString + "_" + file.getFileName().toString();
                            String uniqueMetadataPath = file.getParent().resolve(newFileName).toString();
                            uniqueMetadataPath = StringUtils.makeUniqueString(uniqueMetadataPath, "_", existingFiles);

                            Path rowInternalPath = rowStoragePath.relativize(file.getParent());
                            Path newTargetPath = finalTargetPath.resolve(rowInternalPath);

                            if (!Files.isDirectory(newTargetPath))
                                Files.createDirectories(newTargetPath);

                            Path copyTarget = newTargetPath.resolve(Paths.get(uniqueMetadataPath).getFileName());
                            rowSubStatus.log("Copying " + file + " to " + copyTarget);
                            Files.copy(file, copyTarget, StandardCopyOption.REPLACE_EXISTING);

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } catch (Exception e) {
                IJ.handleException(e);
                progressInfo.log(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }
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

    public List<JIPipeDataSlot> getSlots() {
        return slots;
    }

    @Override
    public UUID getRunUUID() {
        return uuid;
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
        return "Export result data as files";
    }
}
