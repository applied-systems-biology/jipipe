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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io;

import ij.ImagePlus;
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
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Import OME ZARR from directory", description = "Imports images within an OME ZARR directory as ImageJ image. " +
        "Please note that all data will be imported and only XYZCT axes are supported.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FolderData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ImportOMEZARRFromDirectoryAsImagePlusAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ImportOMEZARRFromDirectoryAsImagePlusAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportOMEZARRFromDirectoryAsImagePlusAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path zarrPath = iterationStep.getInputData(getFirstInputSlot(), PathData.class, progressInfo).toPath();
        final N5Factory factory = new N5Factory().cacheAttributes(true);
        try(N5Reader reader = factory.openReader(N5Factory.StorageFormat.ZARR, zarrPath.toString())) {
            List<N5DatasetMetadata> metadataList = new ArrayList<>();
            List<ImagePlus> importedImages = N5Importer.process(reader, "", runContext.getThreadPool().getExecutorService(), metadataList, false, null);
            progressInfo.log("Imported: " + importedImages.size());
        }
    }
}
