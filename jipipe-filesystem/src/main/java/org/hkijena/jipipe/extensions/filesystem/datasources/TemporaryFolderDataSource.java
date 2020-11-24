package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.util.List;

@JIPipeDocumentation(name = "Temporary folder", description = "Generates a temporary folder that will be located within your operating system's temporary directory or " +
        "the directory specified in the JIPipe settings. Please note that there are no guarantees on the actual folder name, as the outcome depends on the operating system. " +
        "The folder is already existing, so a 'Create directories' operation is not needed.")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output", autoCreate = true)
public class TemporaryFolderDataSource extends JIPipeParameterSlotAlgorithm {

    private String baseName = "";

    public TemporaryFolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public TemporaryFolderDataSource(TemporaryFolderDataSource other) {
        super(other);
        this.baseName = other.baseName;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        getFirstOutputSlot().addData(new FileData(RuntimeSettings.generateTempDirectory(baseName)), progressInfo);
    }

    @JIPipeDocumentation(name = "Base name", description = "Optional string that will be put into the directory name.")
    @JIPipeParameter("base-name")
    @StringParameterSettings(monospace = true)
    public String getBaseName() {
        return baseName;
    }

    @JIPipeParameter("base-name")
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }
}
