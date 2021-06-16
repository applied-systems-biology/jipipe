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

package org.hkijena.jipipe.api.parameters;

import com.google.common.eventbus.EventBus;

/**
 * Interfaced for a parameterized object
 */
public interface JIPipeParameterCollection {

    /**
     * Allows to override the visibility of parameters inside the UI
     *
     * @param tree   the parameter tree that is used to access this parameter
     * @param access the parameter
     * @return if the parameter is visible inside the UI
     */
    default boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == this)
            return !access.isHidden();
        else
            return access.getSource().isParameterUIVisible(tree, access);
    }

    /**
     * Allows to override the visibility of sub-parameters inside the UI
     *
     * @param tree         the parameter tree that is used to access this parameter
     * @param subParameter a sub parameter
     * @return if the parameter is visible inside the UI
     */
    default boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        JIPipeParameterTree.Node sourceNode = tree.getSourceNode(subParameter);
        if (sourceNode.getParent() == null || sourceNode.getParent().getCollection() == this) {
            return !sourceNode.isHidden();
        } else {
            return sourceNode.getParent().getCollection().isParameterUIVisible(tree, subParameter);
        }
    }

    /**
     * Triggers a {@link ParameterStructureChangedEvent} on this collection
     */
    default void triggerParameterStructureChange() {
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    /**
     * Triggers a {@link ParameterUIChangedEvent} on this collection
     */
    default void triggerParameterUIChange() {
        getEventBus().post(new ParameterUIChangedEvent(this));
    }

    /**
     * Triggers a {@link ParameterChangedEvent} on this collection
     *
     * @param key the parameter key
     */
    default void triggerParameterChange(String key) {
        getEventBus().post(new ParameterChangedEvent(this, key));
    }

    /**
     * Gets the event bus that posts events about the parameters
     *
     * @return The event bus triggering {@link ParameterChangedEvent} and {@link ParameterStructureChangedEvent}
     */
    EventBus getEventBus();

    /**
     * Triggered when a parameter holder's parameters are changed
     */
    class ParameterChangedEvent {
        private final Object source;
        private final String key;

        /**
         * @param source event source
         * @param key    parameter key
         */
        public ParameterChangedEvent(Object source, String key) {
            this.source = source;
            this.key = key;
        }

        public Object getSource() {
            return source;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * Triggered by an {@link JIPipeParameterCollection} if the list of available parameters is changed.
     * Please be very careful with this event, as it can trigger infinite loops while loading data from parameters.
     * If possible, use {@link ParameterUIChangedEvent}, if you only want the UI to update.
     */
    class ParameterStructureChangedEvent {
        private final JIPipeParameterCollection source;

        /**
         * @param source event source
         */
        public ParameterStructureChangedEvent(JIPipeParameterCollection source) {
            this.source = source;
        }

        public JIPipeParameterCollection getSource() {
            return source;
        }
    }

    /**
     * Triggered by an {@link JIPipeParameterCollection} if the parameter UI should be updated
     */
    class ParameterUIChangedEvent {
        private final JIPipeParameterCollection source;

        /**
         * @param source event source
         */
        public ParameterUIChangedEvent(JIPipeParameterCollection source) {
            this.source = source;
        }

        public JIPipeParameterCollection getSource() {
            return source;
        }
    }
}
