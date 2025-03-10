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
import ome.xml.meta.OMEXMLMetadata;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ROIHandler;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ZARRUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.strings.URIData;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.janelia.saalfeldlab.n5.ij.N5Importer.PARSERS;

@SetJIPipeDocumentation(name = "Import OME ZARR from URI", description = "Imports OME ZARR (NGFF) images provided as URI as ImageJ image. " +
        "Please note that all data contained within the ZARR ZIP/directory will be imported and only XYZCT axes are supported due to ImageJ limitations. " +
        "Please note that OME metadata cannot be currently loaded due to missing standardization (see https://github.com/ome/ngff/issues/104). " +
        "If you have OME ZARR as local files or as ZIP, use the other reader (supports OME metadata using the mechanism as implemented in QuPath)")
@AddJIPipeCitation("https://ngff.openmicroscopy.org/latest/")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = URIData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = OMEImageData.class, name = "Output", create = true)
public class ImportOMEZARRFromURIAsImagePlusAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalJIPipeExpressionParameter datasetFilter = new OptionalJIPipeExpressionParameter();
    private OptionalTextAnnotationNameParameter datasetNameAnnotation = new OptionalTextAnnotationNameParameter("#Dataset", true);

    public ImportOMEZARRFromURIAsImagePlusAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportOMEZARRFromURIAsImagePlusAlgorithm(ImportOMEZARRFromURIAsImagePlusAlgorithm other) {
        super(other);
        this.datasetFilter = new OptionalJIPipeExpressionParameter(other.datasetFilter);
        this.datasetNameAnnotation = new OptionalTextAnnotationNameParameter(other.datasetNameAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        String n5Path = iterationStep.getInputData(getFirstInputSlot(), StringData.class, progressInfo).getData();

        // Add zarr:// if needed
        if (!n5Path.startsWith("zarr://")) {
            n5Path = "zarr://" + n5Path;
            progressInfo.log("Fixing path: " + n5Path);
        }

        // Discover data sets
        progressInfo.log("Reading " + n5Path);
        final N5Reader n5 = new N5Importer.N5ViewerReaderFun().apply(n5Path);
        final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5, N5DatasetDiscoverer.fromParsers(PARSERS),
                Collections.singletonList(new OmeNgffMetadataParser()));
        final List<N5TreeNode> toImport = new ArrayList<>();
        try {
            N5TreeNode rootNode = discoverer.discoverAndParseRecursive("");
            for (N5TreeNode node : N5TreeNode.flattenN5Tree(rootNode).collect(Collectors.toSet())) {
                ZARRUtils.findImagesToImport(progressInfo, node, toImport);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (datasetFilter.isEnabled()) {
            JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);
            toImport.removeIf(node -> !datasetFilter.getContent().evaluateToBoolean(variablesMap));
        }

        // Import the datasets
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            for (N5TreeNode treeNode : toImport) {
                progressInfo.log("Loading " + treeNode.getPath());
                N5DatasetMetadata metadata = (N5DatasetMetadata) treeNode.getMetadata();
                List<ImagePlus> importedImages = N5Importer.process(n5,
                        treeNode.getPath(),
                        executorService,
                        Collections.singletonList(metadata),
                        false,
                        false,
                        null);
                progressInfo.log("Loaded " + importedImages.size() + " images from " + treeNode.getPath());

//                // Look for OME metadata
//                // Currently not specified https://github.com/ome/ngff/issues/104
//                // But Qupath puts it into OME/METADATA.ome.xml, so we do the same
                OMEXMLMetadata omexmlMetadata = null;
//                try {
//                    Path parentPath = Paths.get(inputPath + treeNode.getPath()).getParent();
//                    omexmlMetadata = ZARRUtils.readOMEXMLFromZARR(parentPath, progressInfo);
//                } catch (Exception e) {
//                    progressInfo.log(e);
//                }

                progressInfo.log("Info: OME XML metadata currently not supported!");

                for (ImagePlus image : importedImages) {
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                    datasetNameAnnotation.addAnnotationIfEnabled(annotations, treeNode.getPath());
                    if (omexmlMetadata != null) {
                        ROI2DListData rois = ROIHandler.openROIs((loci.formats.meta.IMetadata) omexmlMetadata, new ImagePlus[]{image});
                        iterationStep.addOutputData(getFirstOutputSlot(), new OMEImageData(image, rois, omexmlMetadata), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                    } else {
                        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                    }

                }
            }
        } finally {
            executorService.shutdown();
        }
    }

    @SetJIPipeDocumentation(name = "Keep dataset if ...", description = "If enabled, allows to specify which dataset should be imported.")
    @JIPipeParameter("dataset-filter")
    @JIPipeExpressionParameterSettings(hint = "per dataset")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "path", name = "Dataset path", description = "The internal path of the dataset")
    public OptionalJIPipeExpressionParameter getDatasetFilter() {
        return datasetFilter;
    }

    @JIPipeParameter("dataset-filter")
    public void setDatasetFilter(OptionalJIPipeExpressionParameter datasetFilter) {
        this.datasetFilter = datasetFilter;
    }

    @SetJIPipeDocumentation(name = "Annotate with dataset name", description = "If enabled, annotate with the name of the dataset")
    @JIPipeParameter("dataset-name-annotation")
    public OptionalTextAnnotationNameParameter getDatasetNameAnnotation() {
        return datasetNameAnnotation;
    }

    @JIPipeParameter("dataset-name-annotation")
    public void setDatasetNameAnnotation(OptionalTextAnnotationNameParameter datasetNameAnnotation) {
        this.datasetNameAnnotation = datasetNameAnnotation;
    }
}
