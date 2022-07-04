/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.NamedTextAnnotationGeneratorExpression;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariableSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JIPipeDocumentation(name = "Merge spots", description = "Merges spot lists. Please ensure that the spots are sourced from the same image.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Output",autoCreate = true)
public class MergeSpotsNode extends JIPipeMergingAlgorithm {

    public MergeSpotsNode(JIPipeNodeInfo info) {
        super(info);
    }

    public MergeSpotsNode(MergeSpotsNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        List<SpotsCollectionData> spotCollections = dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        if(spotCollections.isEmpty())
            return;
        if(spotCollections.size() == 1) {
            dataBatch.addOutputData(getFirstOutputSlot(), spotCollections.get(0), progressInfo);
            return;
        }
        SpotsCollectionData newCollection = new SpotsCollectionData(spotCollections.get(0));
        for (int i = 1; i < spotCollections.size(); i++) {
            SpotsCollectionData sourceCollection = spotCollections.get(i);
            for (Spot spot : sourceCollection.getSpots().iterable(true)) {
                int frame = spot.getFeature(Spot.FRAME).intValue();
                newCollection.getSpots().add(spot, frame);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), newCollection, progressInfo);
    }
}
