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

package org.hkijena.jipipe.ui.cache;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataByMetadataExporter;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.running.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JIPipeCachedSlotToFilesByMetadataExporterRun extends JIPipeWorkbenchPanel implements JIPipeRunnable {

    private final List<JIPipeDataSlot> slots;
    private final boolean splitBySlot;
    private final JIPipeDataByMetadataExporter exporter = new JIPipeDataByMetadataExporter();
    private JIPipeProgressInfo info = new JIPipeProgressInfo();
    private Path outputPath;

    /**
     * @param workbench   the workbench
     * @param slots       the slots to save
     * @param splitBySlot if slots should be split
     */
    public JIPipeCachedSlotToFilesByMetadataExporterRun(JIPipeWorkbench workbench, List<JIPipeDataSlot> slots, boolean splitBySlot) {
        super(workbench);
        this.slots = slots;
        this.splitBySlot = splitBySlot;
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    /**
     * Opens the necessary dialogs
     *
     * @return if setup was confirmed
     */
    public boolean setup() {
        outputPath = FileChooserSettings.openDirectory(this, FileChooserSettings.KEY_DATA, "Export data as files");
        if (outputPath == null)
            return false;

        JDialog editorDialog = new JDialog();
        JPanel mainPanel = new JPanel(new BorderLayout());
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), exporter, null, ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
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
        return confirmation.get();
    }

    @Override
    public void run() {
        Set<String> existing = new HashSet<>();
        info.setMaxProgress(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            info.setProgress(i + 1);
            JIPipeProgressInfo subProgress = info.resolveAndLog("Slot", i, slots.size());
            JIPipeDataSlot slot = slots.get(i);
            Path targetPath = outputPath;
            if (splitBySlot) {
                targetPath = outputPath.resolve(StringUtils.makeUniqueString(slot.getName(), " ", existing));
            }
            try {
                if (!Files.isDirectory(targetPath))
                    Files.createDirectories(targetPath);
                exporter.writeToFolder(slot, targetPath, info.resolve("Slot " + slot.getName()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Subscribe
    public void onFinished(RunUIWorkerFinishedEvent event) {
        if (event.getRun() == this) {
            if (JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                    "The data was successfully exported to " + outputPath + ". Do you want to open the folder?",
                    "Export slot data",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                UIUtils.openFileInNative(outputPath);
            }
        }
    }

    @Subscribe
    public void onInterrupted(RunUIWorkerInterruptedEvent event) {
        if (event.getRun() == this) {
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Could not export slot data to " + outputPath + ". Please take a look at the log (Tools > Logs) to find out more.", "Export slot data", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<JIPipeDataSlot> getSlots() {
        return slots;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return info;
    }

    @Override
    public String getTaskLabel() {
        return "Export cached data to files";
    }

    public void setInfo(JIPipeProgressInfo info) {
        this.info = info;
    }
}
