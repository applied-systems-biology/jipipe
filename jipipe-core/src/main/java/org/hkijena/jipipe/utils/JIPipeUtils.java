package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JIPipeUtils {

    public static List<JIPipeAlgorithm> filterAlgorithmsList(Collection<? extends JIPipeGraphNode> collection) {
        List<JIPipeAlgorithm> algorithms = new ArrayList<>();
        for (JIPipeGraphNode node : collection) {
            if (node instanceof JIPipeAlgorithm) {
                algorithms.add((JIPipeAlgorithm) node);
            }
        }
        return algorithms;
    }
}
