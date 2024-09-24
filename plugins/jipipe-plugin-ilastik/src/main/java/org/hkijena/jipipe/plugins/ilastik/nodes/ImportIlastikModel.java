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

package org.hkijena.jipipe.plugins.ilastik.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.ilastik.datatypes.IlastikModelData;

@SetJIPipeDocumentation(name = "Import Ilastik project", description = "Imports an *.ilp file into the workflow")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, name = "Project file", description = "The project file", create = true)
@AddJIPipeOutputSlot(value = IlastikModelData.class, name = "Project", description = "The Ilastik project", create = true)
public class ImportIlastikModel extends JIPipeSimpleIteratingAlgorithm {

    private boolean link = true;

    public ImportIlastikModel(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportIlastikModel(ImportIlastikModel other) {
        super(other);
        this.link = other.link;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        IlastikModelData modelData = new IlastikModelData(fileData.toPath(), link);
        iterationStep.addOutputData(getFirstOutputSlot(), modelData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Create link", description = "If enabled, the provided Ilastik model is only linked instead of copied. " +
            "This is required if the model does contain relative paths.")
    @JIPipeParameter(value = "link", important = true)
    public boolean isLink() {
        return link;
    }

    @JIPipeParameter("link")
    public void setLink(boolean link) {
        this.link = link;
    }
}
