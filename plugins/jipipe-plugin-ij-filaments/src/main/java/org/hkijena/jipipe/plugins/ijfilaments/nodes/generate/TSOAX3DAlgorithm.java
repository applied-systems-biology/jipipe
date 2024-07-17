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
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.environments.TSOAXEnvironment;
import org.hkijena.jipipe.plugins.ijfilaments.util.TSOAXUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SetJIPipeDocumentation(name = "Find and track filaments 3D+T (TSOAX)", description = "TSOAX is an open source software to extract and track the growth and deformation of biopolymer networks from 2D and 3D time-lapse sequences. " +
        "It tracks each filament or network branch from complex network dynamics and works well even if filaments disappear or reappear. The output is a set of tracks for each evolving filament or network segment.\n" +
        "\n" +
        "TSOAX is an extension of SOAX (for network extraction in static images) to network extraction and tracking in time lapse movies.\n" +
        "\n" +
        "TSOAX facilitates quantitative analysis of network dynamics of multi-dimensional biopolymer networks imaged by various microscopic imaging modalities. The underlying methods of TSOAX includes multiple Stretching Open Active Contour Models for extraction and a combined local and global graph matching framework to establish temporal correspondence among all extracted structures.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@AddJIPipeCitation("T. Xu, C. Langouras, M. Adeli Koudehi, B. Vos, N. Wang, G. H. Koenderink, X. Huang and D. Vavylonis, \"Automated Tracking of Biopolymer Growth and Network Deformation with TSOAX\": Scientific Reports 9:1717 (2019)")
@AddJIPipeCitation("Website https://www.lehigh.edu/~div206/tsoax/index.html")
@AddJIPipeCitation("Documentation (SOAX) https://www.lehigh.edu/~div206/soax/doc/soax_manual.pdf")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Image", description = "The image to be analyzed", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Filaments", description = "The snakes extracted as filaments", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Snakes", description = "The snakes extracted as table", create = true)
@AddJIPipeOutputSlot(value = StringData.class, name = "Raw", description = "The raw TSOAX output", create = true)
public class TSOAX3DAlgorithm extends TSOAXAlgorithm {
    public TSOAX3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TSOAX3DAlgorithm(TSOAX3DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        if(img.getType() == ImagePlus.GRAY32) {
            progressInfo.log("Received 32-bit image, which is not supported. Converting to 16-bit.");
            img = ImageJUtils.convertToGrayscale16UIfNeeded(img);
        }
        Path workDirectory = getNewScratch();

        // Save parameter file
        Path parameterFile = workDirectory.resolve("parameters.txt");
        saveParameterFile(parameterFile);

        // Save image in required structure
        Path inputDir = workDirectory.resolve("input");
        PathUtils.createDirectories(inputDir.resolve("img"));

        ImageJUtils.forEachIndexedCTStack(img, (subStack, index, ctProgress) -> {
            Path imageFile = inputDir.resolve("img").resolve("c" + index.getC() + "t" + index.getT() + ".tif");
            IJ.saveAsTiff(subStack, imageFile.toString());
        }, progressInfo.resolve("Writing inputs"));

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

        environment.runExecutable(args, environmentVariables, false, progressInfo);

        // Extract results
        Path resultsFile = outputDir.resolve("img.txt");
        progressInfo.log("Reading results from " + resultsFile);
        try {
            iterationStep.addOutputData("Raw", new StringData(new String(Files.readAllBytes(resultsFile), StandardCharsets.UTF_8)), progressInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Read and parse tracks
        List<List<Integer>> tracks = TSOAXUtils.parseSnakesTracks(resultsFile, progressInfo);
        Multimap<Integer, Integer> snakeToTrackIdMap = TSOAXUtils.assignSnakesIdsToTrackIds(tracks);

        // Read the snakes
        ResultsTableData snakesResult = TSOAXUtils.parseSnakesAsTable(resultsFile, snakeToTrackIdMap, progressInfo);
        iterationStep.addOutputData("Snakes", snakesResult, progressInfo);

        // Extract the filaments
        if (isSplitByTrack()) {
            Set<Integer> knownTrackIds = TSOAXUtils.findTrackIds(snakesResult);
            for (int knownTrackId : knownTrackIds) {
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                getTrackAnnotationName().addAnnotationIfEnabled(annotations, String.valueOf(knownTrackId));
                Filaments3DGraphData filaments3DGraphData = TSOAXUtils.extractFilaments(snakesResult, knownTrackId, true, progressInfo);
                iterationStep.addOutputData("Filaments", filaments3DGraphData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        } else {
            // Single filament
            Filaments3DGraphData filaments3DGraphData = TSOAXUtils.extractFilaments(snakesResult, -1, false, progressInfo);
            iterationStep.addOutputData("Filaments", filaments3DGraphData, progressInfo);
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
}
