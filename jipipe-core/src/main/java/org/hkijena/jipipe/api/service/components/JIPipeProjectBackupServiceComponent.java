package org.hkijena.jipipe.api.service.components;

import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupSessionInfo;
import org.hkijena.jipipe.api.backups.ThinBackupsRun;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for handling project backups
 */
public class JIPipeProjectBackupServiceComponent extends JIPipeServiceComponent implements JIPipeParameterCollection.ParameterChangedEventListener {
    private Timer backupTimer;
    private final JIPipeQueuedRunnableExecutor queue = new JIPipeQueuedRunnableExecutor("Backups");

    public JIPipeProjectBackupServiceComponent(JIPipeService service) {
        super(service);
    }

    public void restartTimer() {
        if(backupTimer != null) {
            backupTimer.stop();
            backupTimer = null;
        }

        // Start backup service
        JIPipeBackupApplicationSettings settings = getSettings();
        if(settings.isEnableBackups()) {
            backupTimer = new Timer(settings.getBackupDelay() * 60 * 1000, e -> backupAll());
            backupTimer.setRepeats(true);
            backupTimer.start();
        }
    }

    private Path getDefaultBackupPath() {
        Path targetDirectory = PathUtils.getJIPipeUserDir().resolve("backups");
        if (!Files.isDirectory(targetDirectory)) {
            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return targetDirectory;
    }

    public void backup(JIPipeDesktopProjectWindow window) {
        if(window.getProject().isDisposed()) {
            return;
        }
        String name = "untitled";
        if (window.getProjectSavePath() != null) {
            name = window.getProjectSavePath().getFileName().toString();
        }
        JIPipe.getInstance().getProjectBackup().getQueue().cancelIf(runnable -> {
            if(runnable instanceof CreateBackupRun createBackupRun) {
                return createBackupRun.window == window;
            }
            return false;
        });
        String finalName = name;
        JIPipeRunnable run = new CreateBackupRun(window, finalName);
        JIPipe.getInstance().getProjectBackup().getQueue().enqueue(run);
    }

    public Path getCurrentBackupPath() {
        JIPipeBackupApplicationSettings settings = getSettings();
        Path directory;
        if (settings.getCustomBackupPath().isEnabled()) {
            if (!Files.isDirectory(settings.getCustomBackupPath().getContent())) {
                directory = settings.getCustomBackupPath().getContent();
                try {
                    Files.createDirectories(settings.getCustomBackupPath().getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                directory = settings.getCustomBackupPath().getContent();
            }
        } else {
            directory = getDefaultBackupPath();
        }
        return directory;
    }

    private JIPipeBackupApplicationSettings getSettings() {
        return getService().getApplicationSettings().getByType(JIPipeBackupApplicationSettings.class);
    }

    public void backupAll() {
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.isVisible()) {
                backup(window);
            }
        }
    }

    public void scheduleCleanup() {
        JIPipeQueuedRunnableExecutor cleanupQueue = getService().getCleanup().getCleanupQueue();
        cleanupQueue.enqueue(new ThinBackupsRun(false));
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        getSettings().getParameterChangedEventEmitter().subscribe(this);
        restartTimer();
        if(getSettings().getCleanupSettings().isEnableAutoCleanupOnStartup()) {
            scheduleCleanup();
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if("enable-backups".equals(event.getKey()) || "backup-delay".equals(event.getKey())) {
            restartTimer();
        }
    }

    public JIPipeQueuedRunnableExecutor getQueue() {
        return queue;
    }

    private class CreateBackupRun extends DefaultJIPipeRunnable {
        private final JIPipeDesktopProjectWindow window;
        private final String finalName;

        public CreateBackupRun(JIPipeDesktopProjectWindow window, String finalName) {
            this.window = window;
            this.finalName = finalName;
        }

        @Override
        public String getTaskLabel() {
            return "Creating backup";
        }

        @Override
        public void run() {
            try {
                Path directory = getCurrentBackupPath();
                directory = directory.resolve(window.getSessionId().toString());
                Files.createDirectories(directory);

                String dateTimeFormatted = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                String baseName = finalName + "_" + dateTimeFormatted.replace(':', '-');
                baseName = StringUtils.makeFilesystemCompatible(baseName);
                Path targetFile = directory.resolve(baseName + ".jip");
                window.getProject().saveProject(targetFile, false);

                SwingUtilities.invokeLater(() -> window.getProjectWorkbench().sendStatusBarText("Saved backup to " + targetFile));

                // Write storage info
                JIPipeProjectBackupSessionInfo info = new JIPipeProjectBackupSessionInfo();
                info.setProjectStoragePath(window.getProjectSavePath() != null ? window.getProjectSavePath().toString() : "");
                info.setProjectSessionId(window.getSessionId().toString());
                info.setLastDateTimeInfo(dateTimeFormatted);
                JsonUtils.saveToFile(info, directory.resolve("backup-info.json"));

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> window.getProjectWorkbench().sendStatusBarText("Failed to save backup: " + e.getMessage()));
                IJ.handleException(e);
                e.printStackTrace();
            }
        }
    }
}
