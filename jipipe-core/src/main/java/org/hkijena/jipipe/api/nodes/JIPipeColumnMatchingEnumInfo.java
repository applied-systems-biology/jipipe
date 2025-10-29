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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;

public class JIPipeColumnMatchingEnumInfo implements JIPipeEnumParameterItemInfo {
    @Override
    public Icon getIcon(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        JIPipeIterationStepTextAnnotationColumMatching columMatching = (JIPipeIterationStepTextAnnotationColumMatching) value;
        switch (columMatching) {
            case Custom:
                return JIPipe.RESOURCES.getIcon16("actions/insert-math-expression.png");
            case MergeAll:
                return JIPipe.RESOURCES.getIcon16("actions/n-to-1.png");
            case PrefixHashUnion:
                return JIPipe.RESOURCES.getIcon16("actions/irc-channel-active.png");
            default:
                return JIPipe.RESOURCES.getIcon16("actions/configure.png");
        }
    }

    @Override
    public String getLabel(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        return StringUtils.orElse(value, "<None selected>");
    }

    @Override
    public String getTooltip(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
        JIPipeIterationStepTextAnnotationColumMatching columMatching = (JIPipeIterationStepTextAnnotationColumMatching) value;
        switch (columMatching) {
            case Custom:
                return "Determine columns via a custom expression";
            case MergeAll:
                return "Merges all data into one batch (if possible)";
            case PrefixHashUnion:
                return "Use columns prefixed with a '#' as reference. Missing annotations are wildcards.";
            case PrefixHashIntersection:
                return "Use columns prefixed with a '#' as reference. Missing annotations are ignored.";
            case SplitAll:
                return "Splits all data into its own batch (rarely used)";
            case Union:
                return "Use all columns as reference. Missing annotations are wildcards. Rarely used.";
            case Intersection:
                return "Use all columns as reference. Missing annotations are ignored. Rarely used.";
            case None:
                return "Multiply all columns with each other (rarely used)";
            default:
                return null;
        }
    }
}
