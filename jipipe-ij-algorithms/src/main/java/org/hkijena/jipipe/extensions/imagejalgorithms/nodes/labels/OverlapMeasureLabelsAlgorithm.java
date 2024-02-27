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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.ResultsBuilder;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.OverlapStatistics;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.OverlapStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@SetJIPipeDocumentation(name = "Measure label overlap", description = "Compares two label or binary images and calculates " +
        "measurements of their overlap error or agreement. Measurements include: " +
        "<ul>" +
        "<li>Overlap</li>" +
        "<li>Jaccard index</li>" +
        "<li>Dice coefficient</li>" +
        "<li>Volume Similarity</li>" +
        "<li>False Negative Error</li>" +
        "<li>False Positive Error</li>" +
        "</ul>")
@AddJIPipeCitation("See https://imagej.net/plugins/morpholibj#label-overlap-measures for the formulas")
@ConfigureJIPipeNode(menuPath = "Labels\nMeasure", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image 1", create = true)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image 2", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Total", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Per label", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images", aliasName = "Label Overlap Measures")
public class OverlapMeasureLabelsAlgorithm extends JIPipeIteratingAlgorithm {

    private OverlapStatisticsSetParameter measurements = new OverlapStatisticsSetParameter();

    public OverlapMeasureLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public OverlapMeasureLabelsAlgorithm(OverlapMeasureLabelsAlgorithm other) {
        super(other);
        this.measurements = new OverlapStatisticsSetParameter(other.measurements);
    }

    @SetJIPipeDocumentation(name = "Measurements", description = "The measurements that will be stored in to the output table")
    @JIPipeParameter(value = "measurements", important = true)
    public OverlapStatisticsSetParameter getMeasurements() {
        return measurements;
    }

    @JIPipeParameter("measurements")
    public void setMeasurements(OverlapStatisticsSetParameter measurements) {
        this.measurements = measurements;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus sourceImage = iterationStep.getInputData("Image 1", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus targetImage = iterationStep.getInputData("Image 2", ImagePlus3DGreyscaleData.class, progressInfo).getImage();

        ResultsBuilder rb = new ResultsBuilder();
        ResultsTable totalTable = new ResultsTable();
        totalTable.incrementCounter();
        totalTable.setPrecision(6);

        if (measurements.getValues().contains(OverlapStatistics.TotalOverlap)) // Overlap
        {
            rb.addResult(LabelImages.getTargetOverlapPerLabel(sourceImage, targetImage));
            totalTable.addValue("TotalOverlap", LabelImages.getTotalOverlap(sourceImage, targetImage));
        }

        if (measurements.getValues().contains(OverlapStatistics.JaccardIndex)) // Jaccard index
        {
            rb.addResult(LabelImages.getJaccardIndexPerLabel(sourceImage, targetImage));
            totalTable.addValue("JaccardIndex", LabelImages.getJaccardIndex(sourceImage, targetImage));
        }

        if (measurements.getValues().contains(OverlapStatistics.DiceCoefficient)) // Dice coefficient
        {
            rb.addResult(LabelImages.getDiceCoefficientPerLabel(sourceImage, targetImage));
            totalTable.addValue("DiceCoefficient", LabelImages.getDiceCoefficient(sourceImage, targetImage));
        }

        if (measurements.getValues().contains(OverlapStatistics.VolumeSimilarity)) // Volume similarity
        {
            rb.addResult(LabelImages.getVolumeSimilarityPerLabel(sourceImage, targetImage));
            totalTable.addValue("VolumeSimilarity", LabelImages.getVolumeSimilarity(sourceImage, targetImage));
        }

        if (measurements.getValues().contains(OverlapStatistics.FalseNegativeError)) // False negative error
        {
            rb.addResult(LabelImages.getFalseNegativeErrorPerLabel(sourceImage, targetImage));
            totalTable.addValue("FalseNegativeError", LabelImages.getFalseNegativeError(sourceImage, targetImage));
        }

        if (measurements.getValues().contains(OverlapStatistics.FalsePositiveError)) // False positive error
        {
            rb.addResult(LabelImages.getFalsePositiveErrorPerLabel(sourceImage, targetImage));
            totalTable.addValue("FalsePositiveError", LabelImages.getFalsePositiveError(sourceImage, targetImage));
        }

        rb.getResultsTable().setPrecision(6);
        iterationStep.addOutputData("Total", new ResultsTableData(totalTable), progressInfo);
        iterationStep.addOutputData("Per label", new ResultsTableData(rb.getResultsTable()), progressInfo);
    }
}
