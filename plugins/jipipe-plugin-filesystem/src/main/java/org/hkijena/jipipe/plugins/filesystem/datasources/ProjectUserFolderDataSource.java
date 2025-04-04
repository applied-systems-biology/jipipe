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

package org.hkijena.jipipe.plugins.filesystem.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.filesystem.JIPipeFilesystemPluginApplicationSettings;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@SetJIPipeDocumentation(name = "Project user directory", description = "Returns a project user (Project > Project settings > User directories) folder as data")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = FolderData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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
        iterationStep.addOutputData(getFirstOutputSlot(), new FolderData(Objects.requireNonNull(projectDataDirs.get(key))), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Project user directory key", description = "The key of the project user directory. You can configure directories by navigating to Project > Project settings > User directories (or alternatively to Project > Project overview > User directories)")
    @JIPipeParameter(value = "key", important = true)
    @StringParameterSettings(monospace = true)
    public String getKey() {
        return key;
    }

    @JIPipeParameter("key")
    public void setKey(String key) {
        this.key = key;

        JIPipeFilesystemPluginApplicationSettings settings = JIPipeFilesystemPluginApplicationSettings.getInstance();
        if (settings != null && settings.isAutoLabelOutputWithFileName()) {
            String name = StringUtils.nullToEmpty(key);
            if (!Objects.equals(getFirstOutputSlot().getInfo().getCustomName(), name)) {
                getFirstOutputSlot().getInfo().setCustomName(name);
                getNodeSlotsChangedEventEmitter().emit(new NodeSlotsChangedEvent(this));
            }
        }
    }
}
