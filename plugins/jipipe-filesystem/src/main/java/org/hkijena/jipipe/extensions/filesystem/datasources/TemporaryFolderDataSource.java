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

package org.hkijena.jipipe.extensions.filesystem.datasources;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.util.List;

@SetJIPipeDocumentation(name = "Temporary folder", description = "Generates a temporary folder that will be located within your operating system's temporary directory or " +
        "the directory specified in the JIPipe settings. Please note that there are no guarantees on the actual folder name, as the outcome depends on the operating system. " +
        "The folder is already existing, so a 'Create directories' operation is not needed.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = FolderData.class, slotName = "Output", create = true)
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
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        getFirstOutputSlot().addData(new FileData(isUseScratchDirectory() ? getNewScratch() : RuntimeSettings.generateTempDirectory(getBaseName())), JIPipeDataContext.create(this), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Base name", description = "Optional string that will be put into the directory name.")
    @JIPipeParameter("base-name")
    @StringParameterSettings(monospace = true)
    public String getBaseName() {
        return baseName;
    }

    @JIPipeParameter("base-name")
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    @SetJIPipeDocumentation(name = "Use scratch directory", description = "If enabled, the temporary directory will be located inside the current output directory if possible.")
    @JIPipeParameter("use-scratch-dir")
    public boolean isUseScratchDirectory() {
        return useScratchDirectory;
    }

    @JIPipeParameter("use-scratch-dir")
    public void setUseScratchDirectory(boolean useScratchDirectory) {
        this.useScratchDirectory = useScratchDirectory;
    }
}
