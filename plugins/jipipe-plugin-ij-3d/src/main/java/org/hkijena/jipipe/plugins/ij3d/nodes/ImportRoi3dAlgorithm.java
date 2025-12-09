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

package org.hkijena.jipipe.plugins.ij3d.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.ij3d.datatypes.Ij3dSuiteRoiListData;

import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Import IJ3D ROI", description = "Imports a 3D ROI list from a *.zip file")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, name = "Input", create = true, description = "A *.zip file")
@AddJIPipeOutputSlot(value = Ij3dSuiteRoiListData.class, name = "Output", create = true)
public class ImportRoi3dAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ImportRoi3dAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportRoi3dAlgorithm(ImportRoi3dAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path path = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        Ij3dSuiteRoiListData roi3D = Ij3dSuiteRoiListData.importData(path, progressInfo.resolve("Import Roi3d"));
        iterationStep.addOutputData(getFirstOutputSlot(), roi3D, progressInfo);
    }
}
