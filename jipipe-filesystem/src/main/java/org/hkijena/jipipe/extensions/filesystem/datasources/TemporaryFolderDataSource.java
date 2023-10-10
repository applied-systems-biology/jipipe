package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.utils.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.util.List;

@JIPipeDocumentation(name = "Temporary folder", description = "Generates a temporary folder that will be located within your operating system's temporary directory or " +
        "the directory specified in the JIPipe settings. Please note that there are no guarantees on the actual folder name, as the outcome depends on the operating system. " +
        "The folder is already existing, so a 'Create directories' operation is not needed.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = FolderData.class, slotName = "Output", autoCreate = true)
public class TemporaryFolderDataSource extends JIPipeParameterSlotAlgorithm {

    private String baseName = "";
    private boolean useScratchDirectory = true;

    public TemporaryFolderDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public TemporaryFolderDataSource(TemporaryFolderDataSource other) {
        super(other);
        this.baseName = other.baseName;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        getFirstOutputSlot().addData(new FileData(isUseScratchDirectory() ? getNewScratch() : RuntimeSettings.generateTempDirectory(getBaseName())), progressInfo);
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

    @JIPipeDocumentation(name = "Use scratch directory", description = "If enabled, the temporary directory will be located inside the current output directory if possible.")
    @JIPipeParameter("use-scratch-dir")
    public boolean isUseScratchDirectory() {
        return useScratchDirectory;
    }

    @JIPipeParameter("use-scratch-dir")
    public void setUseScratchDirectory(boolean useScratchDirectory) {
        this.useScratchDirectory = useScratchDirectory;
    }
}
