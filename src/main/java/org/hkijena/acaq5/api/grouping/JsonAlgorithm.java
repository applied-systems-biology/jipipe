package org.hkijena.acaq5.api.grouping;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;

/**
 * An algorithm that was imported from a Json extension.
 */
public class JsonAlgorithm extends GraphWrapperAlgorithm {

    public JsonAlgorithm(JsonAlgorithmDeclaration declaration) {
        super(declaration, declaration.getGraph());
    }

    public JsonAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
    }
}
