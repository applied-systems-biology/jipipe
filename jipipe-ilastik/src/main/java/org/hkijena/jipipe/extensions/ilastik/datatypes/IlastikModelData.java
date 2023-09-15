package org.hkijena.jipipe.extensions.ilastik.datatypes;

import ij.IJ;
import jdk.nashorn.internal.scripts.JO;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.ilastik.IlastikExtension;
import org.hkijena.jipipe.extensions.ilastik.IlastikSettings;
import org.hkijena.jipipe.extensions.processes.ProcessEnvironment;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.notifications.GenericNotificationInboxUI;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ProcessUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Ilastik project", description = "An Ilastik project")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A *.ilp project file",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/ilastik-model-data.schema.json")
public class IlastikModelData implements JIPipeData {

    private final byte[] data;
    private final String name;
    public IlastikModelData(Path file) {
        this.name = file.getFileName().toString();
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IlastikModelData(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public IlastikModelData(IlastikModelData other) {
        this.data = other.data;
        this.name = other.name;
    }

    public static IlastikModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".ilp");
        if(file == null) {
            throw new RuntimeException("Unable to find *.ilp file in " + storage.getFileSystemPath());
        }
        progressInfo.log("Importing *.ilp from " + file);
        return new IlastikModelData(file);
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (!forceName)
            name = this.name;
        try {
            if(StringUtils.isNullOrEmpty(name))
                name = "project";
            Files.write(storage.getFileSystemPath().resolve(PathUtils.ensureExtension(Paths.get(name), ".ilp")), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new IlastikModelData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        if(IlastikSettings.environmentSettingsAreValid()) {

            // Export project to a tmp file
            Path outputFile = RuntimeSettings.generateTempFile("ilastik", ".ilp");
            try {
                Files.write(outputFile, data, StandardOpenOption.CREATE);
            }
            catch (Exception e) {
                IJ.handleException(e);
            }

            ProcessEnvironment environment = IlastikSettings.getInstance().getEnvironment();
            ExpressionVariables variables = new ExpressionVariables();
            variables.set("cli_parameters", Collections.singleton(outputFile.toString()));
            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            progressInfo.setLogToStdOut(true);
            workbench.sendStatusBarText("Launching Ilastik ...");
            ProcessUtils.launchProcess(environment, variables, progressInfo);
        }
        else {
            JIPipeNotificationInbox inbox = new JIPipeNotificationInbox();
            IlastikExtension.createMissingIlastikNotificationIfNeeded(inbox);
            GenericNotificationInboxUI ui = new GenericNotificationInboxUI(workbench, inbox);
            JFrame dialog = new JFrame();
            dialog.setTitle("View Ilastik project");
            dialog.setContentPane(ui);
            dialog.setIconImage(UIUtils.getJIPipeIcon128());
            dialog.pack();
            dialog.setSize(800,600);
            dialog.setLocationRelativeTo(workbench.getWindow());
            dialog.setVisible(true);
        }
    }

    @Override
    public String toString() {
        return "Ilastik model: " + name + " (" + (data.length / 1024 / 1024) + " MB)";
    }
}
