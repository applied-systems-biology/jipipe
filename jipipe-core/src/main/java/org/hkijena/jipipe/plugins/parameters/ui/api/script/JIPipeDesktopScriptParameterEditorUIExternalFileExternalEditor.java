package org.hkijena.jipipe.plugins.parameters.ui.api.script;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class JIPipeDesktopScriptParameterEditorUIExternalFileExternalEditor extends JIPipeDesktopScriptParameterEditorUIExternalEditor {

    private Path targetDirectory;
    private Path targetFile;
    private WatchService watchService;

    private Timer timer;

    public JIPipeDesktopScriptParameterEditorUIExternalFileExternalEditor(JIPipeDesktopWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterCollection, parameterKey, parameterTree, parameterAccess);
        initialize();
    }

    private void initialize() {
        JIPipeScriptParameter parameter = getParameter(JIPipeScriptParameter.class);
        targetDirectory = JIPipe.getTemporaryDirectory("script-editor");
        targetFile = targetDirectory.resolve("script" + parameter.getExtension());
        try {
            Files.write(targetFile, StringUtils.nullToEmpty(parameter.getCode()).getBytes(StandardCharsets.UTF_8));
            watchService = FileSystems.getDefault().newWatchService();
            targetDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        timer = new Timer(1000, e -> updateWatchService());
        timer.setRepeats(true);
        timer.start();

        // Open the standard editor
        UIUtils.desktopOpenFile(targetFile.toFile());
    }

    private void updateWatchService() {
        WatchKey key = watchService.poll();
        if (key != null) {
            boolean changed = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    changed = true;
                }
            }
            key.reset();
            if (changed) {
                updateScriptFromFile();
            }
        }
    }

    private void updateScriptFromFile() {
        try {
            String code = new String(Files.readAllBytes(targetFile), StandardCharsets.UTF_8);
            JIPipeScriptParameter parameter = getParameter(JIPipeScriptParameter.class);
            parameter.setCode(code);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            timer.stop();
            watchService.close();
            updateScriptFromFile();
            SwingUtilities.invokeLater(() -> PathUtils.deleteDirectoryRecursively(targetDirectory, new JIPipeProgressInfo()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        super.close();
    }
}
