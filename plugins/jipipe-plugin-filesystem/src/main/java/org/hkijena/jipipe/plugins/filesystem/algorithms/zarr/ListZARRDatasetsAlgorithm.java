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

package org.hkijena.jipipe.plugins.filesystem.algorithms.zarr;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.janelia.saalfeldlab.n5.ij.N5Importer.PARSERS;

@SetJIPipeDocumentation(name = "List ZARR directory datasets", description = "Lists all available datasets and groups in an ZARR directory")
@ConfigureJIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "ZARR")
@AddJIPipeInputSlot(value = FolderData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ListZARRDatasetsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public ListZARRDatasetsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ListZARRDatasetsAlgorithm(ListZARRDatasetsAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path inputPath = iterationStep.getInputData(getFirstInputSlot(), PathData.class, progressInfo).toPath();
        String n5Path = "zarr://file:" + inputPath.toString();

        // Discover data sets
        progressInfo.log("Reading " + n5Path);
        final N5Reader n5 = new N5Importer.N5ViewerReaderFun().apply(n5Path);
        final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5, N5DatasetDiscoverer.fromParsers(PARSERS),
                Collections.singletonList(new OmeNgffMetadataParser()));
        ResultsTableData result = new ResultsTableData();

        try {
            N5TreeNode rootNode = discoverer.discoverAndParseRecursive("");
            for (N5TreeNode node : N5TreeNode.flattenN5Tree(rootNode).collect(Collectors.toSet())) {
                result.addAndModifyRow().set("Path", node.getPath())
                        .set("Name", node.toString())
                        .set("MetadataClass", node.getMetadata() != null ? node.getMetadata().getClass().getCanonicalName() : null)
                        .set("FullPath", n5Path + "?" + node.getPath())
                        .build();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), result, progressInfo);
    }
}
