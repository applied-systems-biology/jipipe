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
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.JIPipeRunnerStatus;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JIPipeCachedSlotToOutputExporterRun extends JIPipeWorkbenchPanel implements JIPipeRunnable {

    private JIPipeRunnableInfo info = new JIPipeRunnableInfo();
    private final Path outputPath;
    private final List<JIPipeDataSlot> slots;
    private final boolean splitBySlot;

    /**
     * @param workbench the workbench
     * @param outputPath the output folder
     * @param slots the slots to save
     * @param splitBySlot if slots should be split
     */
    public JIPipeCachedSlotToOutputExporterRun(JIPipeWorkbench workbench, Path outputPath, List<JIPipeDataSlot> slots, boolean splitBySlot) {
        super(workbench);
        this.outputPath = outputPath;
        this.slots = slots;
        this.splitBySlot = splitBySlot;
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    @Override
    public void run() {
        Set<String> existing = new HashSet<>();
        info.setMaxProgress(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            info.setProgress(i);
            info.resolveAndLog("Slot", i, slots.size());
            JIPipeDataSlot slot = slots.get(i);
            Path targetPath = outputPath;
            if(splitBySlot) {
                targetPath = outputPath.resolve(StringUtils.makeUniqueString(slot.getName(), " ", existing));
            }
            Path storagePath = slot.getStoragePath();
            try {
                if(!Files.isDirectory(targetPath))
                    Files.createDirectories(targetPath);
                slot.setStoragePath(targetPath);
                slot.save(null);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                slot.setStoragePath(storagePath);
            }
        }
    }

    @Subscribe
    public void onFinished(RunUIWorkerFinishedEvent event) {
        if(event.getRun() == this) {
            if(JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
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
        if(event.getRun() == this) {
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Could not export slot data to " + outputPath + ". Please take a look at the log (Tools > Logs) to find out more.", "Export slot data", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<JIPipeDataSlot> getSlots() {
        return slots;
    }

    @Override
    public JIPipeRunnableInfo getInfo() {
        return info;
    }

    public void setInfo(JIPipeRunnableInfo info) {
        this.info = info;
    }
}
