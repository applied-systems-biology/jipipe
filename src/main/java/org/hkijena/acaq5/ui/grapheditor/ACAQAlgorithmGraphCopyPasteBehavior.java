package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

import java.util.Set;

/**
 * Provides functions that allows {@link ACAQAlgorithmGraphCanvasUI} and {@link ACAQAlgorithmUI} to trigger copy & paste behavior
 */
public interface ACAQAlgorithmGraphCopyPasteBehavior {
    /**
     * Copies the set of algorithms into the clipboard
     *
     * @param algorithms set of algorithms
     */
    void copy(Set<ACAQGraphNode> algorithms);

    /**
     * Copies the set of algorithms into clipboard and removes them
     *
     * @param algorithms set of algorithms
     */
    void cut(Set<ACAQGraphNode> algorithms);

    /**
     * Pastes algorithms from clipboard
     */
    void paste();
}
