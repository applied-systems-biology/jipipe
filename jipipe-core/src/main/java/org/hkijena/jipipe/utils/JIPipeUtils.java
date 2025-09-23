package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class JIPipeUtils {

    /**
     * Selects only {@link JIPipeAlgorithm} from a collection of {@link JIPipeGraphNode}
     * @param collection the input
     * @return only algorithms
     */
    public static List<JIPipeAlgorithm> filterAlgorithmsList(Collection<? extends JIPipeGraphNode> collection) {
        List<JIPipeAlgorithm> algorithms = new ArrayList<>();
        for (JIPipeGraphNode node : collection) {
            if (node instanceof JIPipeAlgorithm) {
                algorithms.add((JIPipeAlgorithm) node);
            }
        }
        return algorithms;
    }

    /**
     * Returns if the ID is a valid extension ID
     * Must have following structure: [group]:[artifact]
     * [group] should be lower-case and be a valid Maven group ID
     * [artifact] should be lower-case and a valid Maven artifact ID
     *
     * @param id the ID
     * @return if the id is a valid extension id
     */
    public static boolean isValidExtensionId(String id) {
        if (!StringUtils.isNullOrEmpty(id) && id.contains(":")) {
            if (!id.equals(id.toLowerCase(Locale.ROOT)))
                return false;
            String[] split = id.split(":");
            if (split.length != 2)
                return false;
            String groupId = split[0];
            if (groupId.startsWith(".") || groupId.endsWith(".") || groupId.contains(".."))
                return false;
            Pattern groupPattern = Pattern.compile("[a-z0-9-.]+");
            if (!groupPattern.matcher(groupId).matches())
                return false;
            String artifactId = split[1];
            Pattern artifactPattern = Pattern.compile("[a-z0-9-]+");
            if (!artifactPattern.matcher(artifactId).matches())
                return false;
            return true;
        }
        return false;
    }
}
