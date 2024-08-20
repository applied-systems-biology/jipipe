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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.generate;

import com.google.common.collect.Multimap;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.environments.TSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.util.TSOAXUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Find and track filaments 2D+T (TSOAX)", description = "TSOAX is an open source software to extract and track the growth and deformation of biopolymer networks from 2D and 3D time-lapse sequences. " +
        "It tracks each filament or network branch from complex network dynamics and works well even if filaments disappear or reappear. The output is a set of tracks for each evolving filament or network segment.\n" +
        "\n" +
        "TSOAX is an extension of SOAX (for network extraction in static images) to network extraction and tracking in time lapse movies.\n" +
        "\n" +
        "TSOAX facilitates quantitative analysis of network dynamics of multi-dimensional biopolymer networks imaged by various microscopic imaging modalities. " +
        "The underlying methods of TSOAX includes multiple Stretching Open Active Contour Models for extraction and a combined local and global graph matching framework to establish temporal correspondence among all extracted structures.\n\n" +
        "Variant of TSOAX that is applied per 2D+T stack of the image and the results are combined.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@AddJIPipeCitation("T. Xu, C. Langouras, M. Adeli Koudehi, B. Vos, N. Wang, G. H. Koenderink, X. Huang and D. Vavylonis, \"Automated Tracking of Biopolymer Growth and Network Deformation with TSOAX\": Scientific Reports 9:1717 (2019)")
@AddJIPipeCitation("Website https://www.lehigh.edu/~div206/tsoax/index.html")
@AddJIPipeCitation("Documentation (SOAX) https://www.lehigh.edu/~div206/soax/doc/soax_manual.pdf")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Image", description = "The image to be analyzed", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Filaments", description = "The snakes extracted as filaments", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Snakes", description = "The snakes extracted as table", create = true)
public class TSOAX2DAlgorithm extends TSOAXAlgorithm {

    private OptionalTextAnnotationNameParameter zAnnotationName = new OptionalTextAnnotationNameParameter("Z", true);

    public TSOAX2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TSOAX2DAlgorithm(TSOAX2DAlgorithm other) {
        super(other);
        this.zAnnotationName = new OptionalTextAnnotationNameParameter(other.zAnnotationName);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        if (img.getType() == ImagePlus.GRAY32) {
            progressInfo.log("Received 32-bit image, which is not supported. Converting to 16-bit.");
            img = ImageJUtils.convertToGrayscale16UIfNeeded(img);
        }
        Map<ImageSliceIndex, ImageProcessor> slices = ImageJUtils.splitIntoSlices(img);
        Map<Integer, List<Map.Entry<ImageSliceIndex, ImageProcessor>>> byZ = slices.entrySet().stream().collect(Collectors.groupingBy(entry -> entry.getKey().getZ()));

        Filaments3DGraphData allFilaments = new Filaments3DGraphData();
        ResultsTableData allSnakes = new ResultsTableData();

        for (Map.Entry<Integer, List<Map.Entry<ImageSliceIndex, ImageProcessor>>> byZEntry : byZ.entrySet()) {
            JIPipeProgressInfo byZProgress = progressInfo.resolve("z=" + byZEntry.getKey());
            Path workDirectory = getNewScratch();

            // Rebuild image
            Map<ImageSliceIndex, ImageProcessor> byZProcessorMap = new HashMap<>();
            for (Map.Entry<ImageSliceIndex, ImageProcessor> entry : byZEntry.getValue()) {
                byZProcessorMap.put(entry.getKey(), entry.getValue());
            }
            ImagePlus byZImg = ImageJUtils.mergeMappedSlices(byZProcessorMap);

            // Save parameter file
            Path parameterFile = workDirectory.resolve("parameters.txt");
            saveParameterFile(parameterFile);

            // Save image in required structure
            Path inputDir = workDirectory.resolve("input");
            PathUtils.createDirectories(inputDir.resolve("img"));

            ImageJUtils.forEachIndexedCTStack(byZImg, (subStack, index, ctProgress) -> {
                Path imageFile = inputDir.resolve("img").resolve("c" + index.getC() + "t" + index.getT() + ".tif");
                IJ.saveAsTiff(subStack, imageFile.toString());
            }, byZProgress.resolve("Writing inputs"));

            // Create output dir
            Path outputDir = PathUtils.resolveAndMakeSubDirectory(workDirectory, "output");

            // Setup parameters
            List<String> args = new ArrayList<>();
            args.add("--image");
            args.add(inputDir.toString());
            args.add("--snake");
            args.add(outputDir.toString());
            args.add("--parameter");
            args.add(parameterFile.toString());

            // Run TSOAX
            TSOAXEnvironment environment = getConfiguredTSOAXEnvironment();
            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("LANG", "en_US.UTF-8");
            environmentVariables.put("LC_ALL", "en_US.UTF-8");
            environmentVariables.put("LC_CTYPE", "en_US.UTF-8");

            environment.runExecutable(args, environmentVariables, false, byZProgress);

            // Extract results
            Path resultsFile = outputDir.resolve("img.txt");
//            byZProgress.log("Reading results from " + resultsFile);
//            try {
//                iterationStep.addOutputData("Raw", new StringData(new String(Files.readAllBytes(resultsFile), StandardCharsets.UTF_8)), byZProgress);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }

            // Read and parse tracks
            List<List<Integer>> tracks = TSOAXUtils.parseSnakesTracks(resultsFile, byZProgress);
            Multimap<Integer, Integer> snakeToTrackIdMap = TSOAXUtils.assignSnakesIdsToTrackIds(tracks);

            // Read the snakes and fix Z
            ResultsTableData snakesResult = TSOAXUtils.parseSnakesAsTable(resultsFile, snakeToTrackIdMap, byZProgress);
            final int snakesResultZColumnIndex = snakesResult.getColumnIndex("z");
            for (int row = 0; row < snakesResult.getRowCount(); row++) {
                snakesResult.setValueAt(byZEntry.getKey(), row, snakesResultZColumnIndex);
            }
            allSnakes.addRows(snakesResult);


            // Extract the filaments
            if (isSplitByTrack()) {
                Set<Integer> knownTrackIds = TSOAXUtils.findTrackIds(snakesResult);
                for (int knownTrackId : knownTrackIds) {
                    List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                    getTrackAnnotationName().addAnnotationIfEnabled(annotations, String.valueOf(knownTrackId));
                    getzAnnotationName().addAnnotationIfEnabled(annotations, String.valueOf(byZEntry.getKey()));
                    Filaments3DGraphData filaments3DGraphData = TSOAXUtils.extractFilaments(snakesResult, knownTrackId, true, byZProgress);
                    iterationStep.addOutputData("Filaments", filaments3DGraphData, annotations, JIPipeTextAnnotationMergeMode.Merge, byZProgress);
                }
            } else {
                // Single filament
                Filaments3DGraphData filaments3DGraphData = TSOAXUtils.extractFilaments(snakesResult, -1, false, byZProgress);
                allFilaments.mergeWith(filaments3DGraphData);
            }

            // Clean up
            if (isCleanUpAfterwards()) {
                try {
                    PathUtils.deleteDirectoryRecursively(workDirectory,
                            progressInfo.resolve("Cleanup"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        // Output final results
        iterationStep.addOutputData("Snakes", allSnakes, progressInfo);
        if (!isSplitByTrack()) {
            iterationStep.addOutputData("Filaments", allFilaments, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Annotate with Z", description = "If enabled, add the Z of the track as annotation. Only used when generating filaments per Z.")
    @JIPipeParameter("z-annotation-name")
    public OptionalTextAnnotationNameParameter getzAnnotationName() {
        return zAnnotationName;
    }

    @JIPipeParameter("z-annotation-name")
    public void setzAnnotationName(OptionalTextAnnotationNameParameter zAnnotationName) {
        this.zAnnotationName = zAnnotationName;
    }
}
