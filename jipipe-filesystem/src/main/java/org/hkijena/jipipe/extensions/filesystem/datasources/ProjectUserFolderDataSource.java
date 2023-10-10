package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@JIPipeDocumentation(name = "Project user directory", description = "Returns a project user (Project > Project settings > User directories) folder as data")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output", autoCreate = true)
public class ProjectUserFolderDataSource extends JIPipeSimpleIteratingAlgorithm {

    private String key = "";

    public ProjectUserFolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public ProjectUserFolderDataSource(ProjectUserFolderDataSource other) {
        super(other);
        this.key = other.key;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<String, Path> projectDataDirs;
        if (getRuntimeProject() != null) {
            projectDataDirs = getRuntimeProject().getDirectoryMap();
        } else {
            projectDataDirs = Collections.emptyMap();
        }
        if (!projectDataDirs.containsKey(key)) {
            throw new JIPipeValidationRuntimeException(new IndexOutOfBoundsException("Unable to find '" + key + "' in " + String.join(", ", projectDataDirs.keySet())),
                    "No project user directory '" + key + "'",
                    "You tried to return the project user directory with the key '" + key + "', but only the following keys are available: " + String.join(", ", projectDataDirs.keySet()),
                    "Check if the key is correct or navigate to Project > Project settings > User directories (or alternatively to Project > Project overview > User directories) and configure the directories");
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new FolderData(Objects.requireNonNull(projectDataDirs.get(key))), progressInfo);
    }

    @JIPipeDocumentation(name = "Project user directory key", description = "The key of the project user directory. You can configure directories by navigating to Project > Project settings > User directories (or alternatively to Project > Project overview > User directories)")
    @JIPipeParameter(value = "key", important = true)
    @StringParameterSettings(monospace = true)
    public String getKey() {
        return key;
    }

    @JIPipeParameter("key")
    public void setKey(String key) {
        this.key = key;
    }
}
