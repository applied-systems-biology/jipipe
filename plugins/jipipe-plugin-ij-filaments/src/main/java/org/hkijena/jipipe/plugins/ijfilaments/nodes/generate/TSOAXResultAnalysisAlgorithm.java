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
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.TSOAXUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SetJIPipeDocumentation(name = "Analyze TSOAX output", description = "Parses existing results generated by TSOAX")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Analyze")
@AddJIPipeCitation("T. Xu, C. Langouras, M. Adeli Koudehi, B. Vos, N. Wang, G. H. Koenderink, X. Huang and D. Vavylonis, \"Automated Tracking of Biopolymer Growth and Network Deformation with TSOAX\": Scientific Reports 9:1717 (2019)")
@AddJIPipeCitation("Website https://www.lehigh.edu/~div206/tsoax/index.html")
@AddJIPipeCitation("Documentation (SOAX) https://www.lehigh.edu/~div206/soax/doc/soax_manual.pdf")
@AddJIPipeOutputSlot(value = Filaments3DData.class, name = "Filaments", description = "The snakes extracted as filaments", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Snakes", description = "The snakes extracted as table", create = true)
@AddJIPipeInputSlot(value = StringData.class, name = "Raw", description = "The raw TSOAX output to be analyzed", create = true)
public class TSOAXResultAnalysisAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean splitByTrack = true;
    private OptionalTextAnnotationNameParameter trackAnnotationName = new OptionalTextAnnotationNameParameter("Track", true);

    public TSOAXResultAnalysisAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TSOAXResultAnalysisAlgorithm(TSOAXResultAnalysisAlgorithm other) {
        super(other);
        this.splitByTrack = other.splitByTrack;
        this.trackAnnotationName = new OptionalTextAnnotationNameParameter(other.trackAnnotationName);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        String rawData = iterationStep.getInputData(getFirstInputSlot(), StringData.class, progressInfo).getData();
        Path scratch = getNewScratch();
        Path resultsFile = scratch.resolve("results.txt");
        try {
            Files.write(resultsFile, rawData.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Extract results
        progressInfo.log("Reading results from " + resultsFile);

        // Read and parse tracks
        List<List<Integer>> tracks = TSOAXUtils.parseSnakesTracks(resultsFile, progressInfo);
        Multimap<Integer, Integer> snakeToTrackIdMap = TSOAXUtils.assignSnakesIdsToTrackIds(tracks);

        // Read the snakes
        ResultsTableData snakesResult = TSOAXUtils.parseSnakesAsTable(resultsFile, snakeToTrackIdMap, progressInfo);
        iterationStep.addOutputData("Snakes", snakesResult, progressInfo);

        // Extract the filaments
        if (splitByTrack) {
            Set<Integer> knownTrackIds = TSOAXUtils.findTrackIds(snakesResult);
            for (int knownTrackId : knownTrackIds) {
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                trackAnnotationName.addAnnotationIfEnabled(annotations, String.valueOf(knownTrackId));
                Filaments3DData filaments3DData = TSOAXUtils.extractFilaments(snakesResult, knownTrackId, true, progressInfo);
                iterationStep.addOutputData("Filaments", filaments3DData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            }
        } else {
            // Single filament
            Filaments3DData filaments3DData = TSOAXUtils.extractFilaments(snakesResult, -1, false, progressInfo);
            iterationStep.addOutputData("Filaments", filaments3DData, progressInfo);
        }

        // Delete the scratch
        PathUtils.deleteDirectoryRecursively(scratch, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Split by track", description = "If enabled, split the filaments by their track ID (if available)")
    @JIPipeParameter("split-by-track")
    public boolean isSplitByTrack() {
        return splitByTrack;
    }

    @JIPipeParameter("split-by-track")
    public void setSplitByTrack(boolean splitByTrack) {
        this.splitByTrack = splitByTrack;
    }

    @SetJIPipeDocumentation(name = "Annotate with track ID", description = "If enabled, add the track ID as annotation to the generated filaments")
    @JIPipeParameter("track-annotation-name")
    public OptionalTextAnnotationNameParameter getTrackAnnotationName() {
        return trackAnnotationName;
    }

    @JIPipeParameter("track-annotation-name")
    public void setTrackAnnotationName(OptionalTextAnnotationNameParameter trackAnnotationName) {
        this.trackAnnotationName = trackAnnotationName;
    }
}
