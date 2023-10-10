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
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.util.List;

@JIPipeDocumentation(name = "Temporary file", description = "Generates a temporary file that will be located within your operating system's temporary directory or " +
        "the directory specified in the JIPipe settings. Please note that there are no guarantees on the actual file name, as the outcome depends on the operating system.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = FileData.class, slotName = "Output", autoCreate = true)
public class TemporaryFileDataSource extends JIPipeParameterSlotAlgorithm {

    private String prefix = "";
    private String suffix = "";

    public TemporaryFileDataSource(JIPipeNodeInfo info) {
        super(info);
    }

    public TemporaryFileDataSource(TemporaryFileDataSource other) {
        super(other);
        this.prefix = other.prefix;
        this.suffix = other.suffix;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        getFirstOutputSlot().addData(new FileData(RuntimeSettings.generateTempFile(prefix, suffix)), progressInfo);
    }

    @JIPipeDocumentation(name = "Prefix", description = "Optional string that is prepended to file name.")
    @JIPipeParameter("prefix")
    @StringParameterSettings(monospace = true)
    public String getPrefix() {
        return prefix;
    }

    @JIPipeParameter("prefix")
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @JIPipeDocumentation(name = "Suffix", description = "Optional string that is appended to file name.")
    @JIPipeParameter("suffix")
    @StringParameterSettings(monospace = true)
    public String getSuffix() {
        return suffix;
    }

    @JIPipeParameter("suffix")
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
