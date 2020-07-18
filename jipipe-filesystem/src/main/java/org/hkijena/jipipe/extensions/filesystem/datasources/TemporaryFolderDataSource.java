package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Temporary folder", description = "Generates a temporary folder that will be located within your operating system's temporary directory or " +
        "the directory specified in the JIPipe settings. Please note that there are no guarantees on the actual folder name, as the outcome depends on the operating system. " +
        "The folder is already existing, so a 'Create directories' operation is not needed.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
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
    public void runParameterSet(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<JIPipeAnnotation> parameterAnnotations) {
        getFirstInputSlot().addData(new FileData(RuntimeSettings.generateTempDirectory(baseName)));
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
