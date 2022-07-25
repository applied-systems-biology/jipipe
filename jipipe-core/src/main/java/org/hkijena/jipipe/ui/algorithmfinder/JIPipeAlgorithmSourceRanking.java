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
 */

package org.hkijena.jipipe.ui.algorithmfinder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.search.RankingFunction;

/**
 * Ranks {@link JIPipeNodeInfo} or {@link JIPipeGraphNode} instances
 * It ranks by following values:
 * - search string (node name)
 * - search string (node description)
 * - data type compatibility (0 = not, -1=nontrivial, -2=trivial, -3=exact match)
 * - already occupied or not (no = 0, yes = 1)
 */
public class JIPipeAlgorithmSourceRanking implements RankingFunction<Object> {
    private final JIPipeDataSlot targetSlot;

    public JIPipeAlgorithmSourceRanking(JIPipeDataSlot targetSlot) {
        this.targetSlot = targetSlot;
    }

    @Override
    public int[] rank(Object value, String[] filterStrings) {
        if (value == null)
            return null;
        int[] ranks = new int[4];
        String nameHayStack;
        String descriptionHayStack;
        if (value instanceof JIPipeGraphNode) {
            JIPipeGraphNode node = ((JIPipeGraphNode) value);
            nameHayStack = node.getName();
            descriptionHayStack = StringUtils.orElse(node.getCustomDescription().getBody(), node.getInfo().getDescription().getBody());
        } else if (value instanceof JIPipeNodeInfo) {
            JIPipeNodeInfo info = (JIPipeNodeInfo) value;
            if (info.isHidden())
                return null;
            nameHayStack = StringUtils.orElse(info.getName(), "").toLowerCase();
            descriptionHayStack = StringUtils.orElse(info.getDescription().getBody(), "").toLowerCase();
        } else {
            return null;
        }

        if (nameHayStack == null)
            nameHayStack = "";
        if (descriptionHayStack == null)
            descriptionHayStack = "";

        if (filterStrings != null && filterStrings.length > 0) {
            nameHayStack = nameHayStack.toLowerCase();
            descriptionHayStack = descriptionHayStack.toLowerCase();

            for (String string : filterStrings) {
                if (nameHayStack.contains(string.toLowerCase()))
                    --ranks[0];
                if (descriptionHayStack.contains(string.toLowerCase()))
                    --ranks[1];
            }

            // Name/description does not match -> ignore
            if (ranks[0] == 0 && ranks[1] == 0)
                return null;
        }

        // Rank by data type compatibility
        if (value instanceof JIPipeGraphNode) {
            JIPipeGraphNode node = ((JIPipeGraphNode) value);
            for (JIPipeDataSlot sourceSlot : node.getOutputSlots()) {
                int compatibilityRanking = 0;
                if (sourceSlot.getAcceptedDataType() == targetSlot.getAcceptedDataType()) {
                    compatibilityRanking = -3;
                } else if (JIPipeDatatypeRegistry.isTriviallyConvertible(targetSlot.getAcceptedDataType(), sourceSlot.getAcceptedDataType())) {
                    compatibilityRanking = -2;
                } else if (JIPipe.getDataTypes().isConvertible(sourceSlot.getAcceptedDataType(), targetSlot.getAcceptedDataType())) {
                    compatibilityRanking = -1;
                }
                ranks[2] = Math.min(compatibilityRanking, ranks[2]);
                if (!targetSlot.getNode().getParentGraph().getOutputOutgoingTargetSlots(sourceSlot).isEmpty()) {
                    ranks[3] = 1;
                }
            }
        } else {
            JIPipeNodeInfo info = (JIPipeNodeInfo) value;
            if (info.isHidden() || !info.getCategory().userCanCreate())
                return null;
            for (JIPipeOutputSlot outputSlot : info.getOutputSlots()) {
                int compatibilityRanking = 0;
                if (outputSlot.value() == targetSlot.getAcceptedDataType()) {
                    compatibilityRanking = -3;
                } else if (JIPipeDatatypeRegistry.isTriviallyConvertible(outputSlot.value(), targetSlot.getAcceptedDataType())) {
                    compatibilityRanking = -2;
                } else if (JIPipe.getDataTypes().isConvertible(outputSlot.value(), targetSlot.getAcceptedDataType())) {
                    compatibilityRanking = -1;
                }
                ranks[2] = Math.min(compatibilityRanking, ranks[2]);
            }
        }

        // Not compatible to slot
        if (ranks[2] == 0)
            return null;

        return ranks;
    }
}
