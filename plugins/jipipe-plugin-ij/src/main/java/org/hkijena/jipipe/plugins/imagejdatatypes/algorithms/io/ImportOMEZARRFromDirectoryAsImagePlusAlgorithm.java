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
import net.imglib2.Interval;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterSettings;
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
import org.hkijena.jipipe.plugins.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.janelia.saalfeldlab.n5.ij.N5Importer.PARSERS;

@SetJIPipeDocumentation(name = "Import OME ZARR from directory", description = "Imports images within an OME ZARR (NGFF) directory as ImageJ image. " +
        "Please note that all data contained within the ZARR directory will be imported and only XYZCT axes are supported due to ImageJ limitations.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FolderData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class ImportOMEZARRFromDirectoryAsImagePlusAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalJIPipeExpressionParameter datasetFilter = new OptionalJIPipeExpressionParameter();
    private OptionalTextAnnotationNameParameter datasetNameAnnotation = new OptionalTextAnnotationNameParameter();

    public ImportOMEZARRFromDirectoryAsImagePlusAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportOMEZARRFromDirectoryAsImagePlusAlgorithm(ImportOMEZARRFromDirectoryAsImagePlusAlgorithm other) {
        super(other);
        this.datasetFilter = new OptionalJIPipeExpressionParameter(other.datasetFilter);
        this.datasetNameAnnotation = new OptionalTextAnnotationNameParameter(other.datasetNameAnnotation);
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
        final List<N5TreeNode> toImport = new ArrayList<>();
        try {
            N5TreeNode rootNode = discoverer.discoverAndParseRecursive("");
            for (N5TreeNode node : N5TreeNode.flattenN5Tree(rootNode).collect(Collectors.toSet())) {
                if(node.getMetadata() != null) {
                    if(node.getMetadata().getClass().getSimpleName().equalsIgnoreCase("NgffSingleScaleAxesMetadata") && node.getMetadata() instanceof N5DatasetMetadata) {
                        progressInfo.log("- Detected OME NGFF Image " + node.getMetadata() + " at " + node.getPath());
                        toImport.add(node);
                    }
                    else if(node.getMetadata().getClass().getSimpleName().equalsIgnoreCase("OmeNgffMetadata")) {
                        progressInfo.log("- Detected OME NGFF " + node.getMetadata() + " at " + node.getPath());
                    }
                    else {
                        progressInfo.log("- Detected unknown " + node.getMetadata() + " at " + node.getPath());
                    }
                }
                else {
                    progressInfo.log("- Detected group at " + node.getPath());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(datasetFilter.isEnabled()) {
            JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
            variablesMap.putCustomVariables(getDefaultCustomExpressionVariables());
            variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());
            toImport.removeIf(node -> !datasetFilter.getContent().evaluateToBoolean(variablesMap));
        }

        // Import the datasets
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            for (N5TreeNode treeNode : toImport) {
                progressInfo.log("Loading " + treeNode.getPath());
                N5DatasetMetadata metadata = (N5DatasetMetadata) treeNode.getMetadata();
                List<ImagePlus> importedImages = N5Importer.process(n5, treeNode.getPath(), executorService, Collections.singletonList(metadata), false, false, null);
                progressInfo.log("Loaded " + importedImages.size() + " images from " + treeNode.getPath());
                for (ImagePlus image : importedImages) {
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                    datasetNameAnnotation.addAnnotationIfEnabled(annotations, treeNode.getPath());
                    iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(image), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                }
            }
        }
        finally {
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
