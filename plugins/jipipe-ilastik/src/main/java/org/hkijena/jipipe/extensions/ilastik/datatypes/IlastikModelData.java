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

package org.hkijena.jipipe.extensions.ilastik.datatypes;

import ij.IJ;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.extensions.ilastik.IlastikPlugin;
import org.hkijena.jipipe.extensions.ilastik.IlastikSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.notifications.GenericNotificationInboxUI;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

@SetJIPipeDocumentation(name = "Ilastik project", description = "An Ilastik project")
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

            JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
            progressInfo.setLogToStdOut(true);
            workbench.sendStatusBarText("Launching Ilastik ...");
            IlastikPlugin.runIlastik(null, Collections.singletonList(outputFile.toString()), progressInfo, true);
        }
        else {
            JIPipeNotificationInbox inbox = new JIPipeNotificationInbox();
            IlastikPlugin.createMissingIlastikNotificationIfNeeded(inbox);
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
