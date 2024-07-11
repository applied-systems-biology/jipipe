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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;

import java.util.List;

@SetJIPipeDocumentation(name = "Temporary file", description = "Generates a temporary file that will be located within your operating system's temporary directory or " +
        "the directory specified in the JIPipe settings. Please note that there are no guarantees on the actual file name, as the outcome depends on the operating system.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = FileData.class, name = "Output", create = true)
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
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        getFirstOutputSlot().addData(new FileData(JIPipeRuntimeApplicationSettings.getTemporaryFile(prefix, suffix)), JIPipeDataContext.create(this), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Prefix", description = "Optional string that is prepended to file name.")
    @JIPipeParameter("prefix")
    @StringParameterSettings(monospace = true)
    public String getPrefix() {
        return prefix;
    }

    @JIPipeParameter("prefix")
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @SetJIPipeDocumentation(name = "Suffix", description = "Optional string that is appended to file name.")
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
