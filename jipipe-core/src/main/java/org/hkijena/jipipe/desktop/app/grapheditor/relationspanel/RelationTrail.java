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

package org.hkijena.jipipe.desktop.app.grapheditor.relationspanel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The visitation trail of the Relations panel: the sequence of cards the
 * user navigated through, with back/forward movement and the opacity ramp
 * that fades the previous steps (MR 3 of the navigator overhaul, #1358).
 * <p>
 * Index-based: {@link #navigateTo(String)} appends after the current index
 * (dropping any forward entries) and caps the history at {@code maxSteps}
 * entries by dropping the oldest, so back-stepping beyond the cap is
 * impossible.
 */
public class RelationTrail {

    /**
     * How many steps back the opacity ramp spans: the current node has rank
     * 0, the previous five nodes have ranks 1 to 5, everything else is
     * unranked ({@link #opacityRank(String)} returns -1).
     */
    public static final int OPACITY_RAMP_LENGTH = 5;

    private final int maxSteps;
    private final List<String> history = new ArrayList<>();
    private int index = -1;

    /**
     * @param maxSteps how many nodes the trail history holds at most
     *                 (the panel passes its {@code TRAIL_LENGTH})
     */
    public RelationTrail(int maxSteps) {
        // Fail-safe: a trail always remembers at least the last visited node.
        this.maxSteps = Math.max(1, maxSteps);
    }

    /**
     * Navigates to a node: drops the forward history, appends the node and
     * caps the history at {@code maxSteps} entries by dropping the oldest.
     *
     * @param nodeId the node to navigate to; null is a no-op (null is the
     *               "impossible" sentinel of {@link #goBack()} and
     *               {@link #goForward()}, so it must not enter the history)
     */
    public void navigateTo(String nodeId) {
        if (nodeId == null) {
            return;
        }
        if (index < history.size() - 1) {
            history.subList(index + 1, history.size()).clear();
        }
        history.add(nodeId);
        index = history.size() - 1;
        while (history.size() > maxSteps) {
            history.remove(0);
            --index;
        }
    }

    /**
     * @return whether there is a step before the current one
     */
    public boolean canGoBack() {
        return index > 0;
    }

    /**
     * @return whether there is a step after the current one
     */
    public boolean canGoForward() {
        return index < history.size() - 1;
    }

    /**
     * @return how many nodes the trail currently holds
     */
    public int stepCount() {
        return history.size();
    }

    /**
     * Moves one step back.
     *
     * @return the newly current node, or null when already at the start
     */
    public String goBack() {
        if (!canGoBack()) {
            return null;
        }
        --index;
        return history.get(index);
    }

    /**
     * Moves one step forward.
     *
     * @return the newly current node, or null when already at the end
     */
    public String goForward() {
        if (!canGoForward()) {
            return null;
        }
        ++index;
        return history.get(index);
    }

    /**
     * @return the node at the trail index (the latest visited node when at
     * the end), or null when nothing was visited yet
     */
    public String current() {
        if (history.isEmpty()) {
            return null;
        }
        return history.get(index);
    }

    /**
     * The rank of a node on the opacity ramp: 0 for the node at the current
     * index, 1 to {@link #OPACITY_RAMP_LENGTH} for the distinct nodes before
     * it (walking backward; a node that was visited repeatedly keeps the
     * rank of its occurrence nearest to the current node), and -1 for
     * everything else (unvisited nodes, nodes beyond the ramp, and nodes
     * ahead in the forward history).
     *
     * @param nodeId the node to rank
     * @return the ramp rank, or -1 when the node is unranked
     */
    public int opacityRank(String nodeId) {
        if (index < 0 || index >= history.size()) {
            return -1;
        }
        if (Objects.equals(nodeId, history.get(index))) {
            return 0;
        }
        Set<String> ranked = new HashSet<>();
        ranked.add(history.get(index));
        int rank = 0;
        for (int i = index - 1; i >= 0 && rank < OPACITY_RAMP_LENGTH; --i) {
            String id = history.get(i);
            if (!ranked.add(id)) {
                // Already ranked by its occurrence nearest to the current node.
                continue;
            }
            ++rank;
            if (Objects.equals(nodeId, id)) {
                return rank;
            }
        }
        return -1;
    }
}
