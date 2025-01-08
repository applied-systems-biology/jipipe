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

package org.hkijena.jipipe.plugins.ilastik.datatypes;

import ij.IJ;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.ilastik.IlastikPlugin;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;

@SetJIPipeDocumentation(name = "Ilastik project", description = "An Ilastik project")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A *.ilp project file",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/ilastik-model-data.schema.json")
public class IlastikModelData implements JIPipeData {

    private final byte[] data;
    private final String name;
    private final Path linkedPath;

    public IlastikModelData(Path file, boolean linked) {
        this.name = file.getFileName().toString();
        if (linked) {
            data = null;
            linkedPath = file;
        } else {
            linkedPath = null;
            try {
                data = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public IlastikModelData(IlastikModelData other) {
        this.data = other.data;
        this.name = other.name;
        this.linkedPath = other.linkedPath;
    }

    public static IlastikModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path file = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".ilp");
        if (file == null) {
            throw new RuntimeException("Unable to find *.ilp file in " + storage.getFileSystemPath());
        }
        progressInfo.log("Importing *.ilp from " + file);
        return new IlastikModelData(file, false);
    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public Path getLinkedPath() {
        return linkedPath;
    }

    public boolean isLinked() {
        return linkedPath != null;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        if (!forceName) {
            name = this.name;
        }
        try {
            if (StringUtils.isNullOrEmpty(name)) {
                name = "project";
            }
            Path outputFile = storage.getFileSystemPath().resolve(PathUtils.ensureExtension(Paths.get(name), ".ilp"));
            if (isLinked()) {
                Files.copy(linkedPath, outputFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.write(outputFile, data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new IlastikModelData(this);
    }

    @Override
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        if (isLinked()) {
            // Open project with Ilastik
            IlastikPlugin.launchIlastik(desktopWorkbench, Collections.singletonList(linkedPath.toString()));
        } else {
            // Export project to a tmp file
            Path outputFile = JIPipeRuntimeApplicationSettings.getTemporaryFile("ilastik", ".ilp");
            try {
                Files.write(outputFile, data, StandardOpenOption.CREATE);
            } catch (Exception e) {
                IJ.handleException(e);
            }

            // Open project with Ilastik
            IlastikPlugin.launchIlastik(desktopWorkbench, Collections.singletonList(outputFile.toString()));
        }
    }

    @Override
    public String toString() {
        if (isLinked()) {
            return linkedPath.toString();
        } else {
            return "Ilastik model: " + name + " (" + (data.length / 1024 / 1024) + " MB)";
        }

    }

    public Path exportOrGetLink(Path exportedPath) {
        if (isLinked()) {
            return linkedPath;
        } else {
            try {
                Files.write(exportedPath, getData(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return exportedPath;
        }
    }
}
