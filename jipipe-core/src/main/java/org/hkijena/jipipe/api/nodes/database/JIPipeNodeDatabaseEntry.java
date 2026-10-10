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

package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JIPipeNodeDatabaseEntry {

    String getId();

    WeightedTokens getTokens();

    boolean exists();

    JIPipeNodeDatabasePipelineVisibility getVisibility();

    String getName();

    HTMLText getDescription();

    Icon getIcon();

    List<String> getLocationInfos();

    Map<String, JIPipeDataSlotInfo> getInputSlots();

    Map<String, JIPipeDataSlotInfo> getOutputSlots();

    Color getFillColor();

    Color getBorderColor();

    Set<JIPipeDesktopGraphNodeUI> addToGraph(JIPipeDesktopGraphCanvasUI canvasUI);

    boolean canAddInputSlots();

    boolean canAddOutputSlots();

    boolean isDeprecatedOrUnstable();

    <T> T getAttachment(Class<T> attachmentClass);

    <T> void attach(T attachment);

    /**
     * Returns the classification of this node, which affects search ranking.
     *
     * @return the node classification
     */
    JIPipeNodeClassification getNodeClassification();

    // --- Node taxonomy facets (Phase 1b) ---

    /**
     * The ids of all taxonomy functions this entry's node appears under
     * (the primary function's id plus the ids of all secondary functions),
     * in declaration order. Entries that do not wrap a node type return
     * an empty set.
     *
     * @return the taxonomy function ids, possibly empty
     */
    default Set<String> getFunctionIds() {
        return Collections.emptySet();
    }

    /**
     * The framework (external software or research project) this entry's
     * node implements.
     *
     * @return the framework; never {@code null}, defaults to
     * {@link NoFramework}
     */
    default JIPipeNodeTaxonomyFramework getFramework() {
        return new NoFramework();
    }

    /**
     * The spatial dimensions this entry's node operates in.
     *
     * @return the declared spatial dimensions, possibly empty
     */
    default Set<JIPipeNodeTaxonomySpatialDimension> getSpatialDimensions() {
        return Collections.emptySet();
    }

    /**
     * The derived input profile of this entry's node, describing what kind
     * of data it consumes.
     *
     * @return the input profile; defaults to
     * {@link JIPipeNodeTaxonomyInputProfile#NA} when the profile cannot be
     * determined
     */
    default JIPipeNodeTaxonomyInputProfile getInputProfile() {
        return JIPipeNodeTaxonomyInputProfile.NA;
    }

    /**
     * The node's merged taxonomy tag set (node-level tags plus the tags of
     * every taxonomy function the node appears under).
     *
     * @return the merged tags, possibly empty
     */
    default Set<String> getTags() {
        return Collections.emptySet();
    }

    /**
     * The id of the node type this entry is created from, if any.
     * <p>
     * Entries that wrap a node type (the "create node by info" entries and
     * their aliases) return the node type's id, so that database queries
     * such as {@link JIPipeNodeDatabase#followUps(JIPipeNodeDatabaseEntry)}
     * can resolve the entry to its place in the node taxonomy (family
     * steps, frameworks, ...). Entries that do not wrap a node type
     * (compartments, templates, existing nodes, examples) return
     * {@code null}.
     *
     * @return the origin node type id, or {@code null} if this entry does
     * not wrap a node type
     */
    default String getOriginNodeId() {
        return null;
    }
}
