package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.extensions.parameters.api.enums.EnumItemInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeColumnMatchingEnumInfo implements EnumItemInfo {
    @Override
    public Icon getIcon(Object value) {
        JIPipeColumMatching columMatching = (JIPipeColumMatching) value;
        switch (columMatching) {
            case Custom:
                return UIUtils.getIconFromResources("actions/insert-math-expression.png");
            case MergeAll:
                return UIUtils.getIconFromResources("actions/n-to-1.png");
            case PrefixHashUnion:
                return UIUtils.getIconFromResources("actions/irc-channel-active.png");
            default:
                return UIUtils.getIconFromResources("actions/configure.png");
        }
    }

    @Override
    public String getLabel(Object value) {
        return StringUtils.orElse(value, "<None selected>");
    }

    @Override
    public String getTooltip(Object value) {
        JIPipeColumMatching columMatching = (JIPipeColumMatching) value;
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
