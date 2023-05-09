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

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.util.*;

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
        getParameterStructureChangedEventEmitter().emit(new ParameterStructureChangedEvent(this));
    }

    /**
     * Triggers a {@link ParameterUIChangedEvent} on this collection
     */
    default void triggerParameterUIChange() {
        getParameterUIChangedEventEmitter().emit(new ParameterUIChangedEvent(this));
    }

    /**
     * Triggers a {@link ParameterChangedEvent} on this collection
     *
     * @param key the parameter key
     */
    default void triggerParameterChange(String key) {
        getParameterChangedEventEmitter().emit(new ParameterChangedEvent(this, key));
    }

    /**
     * Sets a parameter and triggers the associated events
     *
     * @param key   the parameter key
     * @param value the parameter value
     * @return if the parameter could be set
     */
    default boolean setParameter(String key, Object value) {
        JIPipeParameterTree tree = new JIPipeParameterTree(this);
        return tree.getParameters().get(key).set(value);
    }

    /**
     * Gets an access instance to a parameter of the defined key
     *
     * @param key the parameter key
     * @return the access instance
     */
    default JIPipeParameterAccess getParameterAccess(String key) {
        JIPipeParameterTree tree = new JIPipeParameterTree(this);
        return tree.getParameters().get(key);
    }

    /**
     * Gets a parameter
     *
     * @param key   the parameter key
     * @param klass the parameter class
     * @param <T>   the parameter class
     * @return the current value
     */
    default <T> T getParameter(String key, Class<T> klass) {
        JIPipeParameterTree tree = new JIPipeParameterTree(this);
        return tree.getParameters().get(key).get(klass);
    }

    /**
     * List of context actions that are added to this collection.
     * Does no influence the creation of context actions via {@link JIPipeContextAction}
     *
     * @return list of context actions
     */
    default List<JIPipeParameterCollectionContextAction> getContextActions() {
        return Collections.emptyList();
    }

    /**
     * Emitter for changes in the parameter
     * @return the emitter
     */
    ParameterChangedEventEmitter getParameterChangedEventEmitter();

    /**
     * Emitter for changes in the structure of parameters
     * @return the emitter
     */
    ParameterStructureChangedEventEmitter getParameterStructureChangedEventEmitter();

    /**
     * Emitter for changes in the parameter user interface
     * @return the emitter
     */
    ParameterUIChangedEventEmitter getParameterUIChangedEventEmitter();

    /**
     * Allows to install additional operations into the context menu of each parameter (triangle menu next to the help)
     * Please note that the root parameter collection handles this task for all sub-parameters
     *
     * @param parameterPanel    the parameter panel
     * @param parameterEditorUI the currently handled parameter editor
     * @param menu              the menu
     */
    default void installUIParameterOptions(ParameterPanel parameterPanel, JIPipeParameterEditorUI parameterEditorUI, JPopupMenu menu) {

    }

    /**
     * Allows to replace the UI that is inserted into the parameter panel with a different component
     * Labels are not affected
     * Please note that the root parameter collection handles this task for all sub-parameters
     *
     * @param parameterPanel    the parameter panel
     * @param parameterEditorUI the currently handled parameter editor
     * @return the component to be inserted into the parameter panel
     */
    default JComponent installUIOverrideParameterEditor(ParameterPanel parameterPanel, JIPipeParameterEditorUI parameterEditorUI) {
        return parameterEditorUI;
    }

    /**
     * Triggered when a parameter holder's parameters are changed
     */
    class ParameterChangedEvent extends AbstractJIPipeEvent {
        private final String key;
        private final Set<Object> visitors = new HashSet<>();

        /**
         * @param source event source
         * @param key    parameter key
         */
        public ParameterChangedEvent(Object source, String key) {
            super(source);
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public Set<Object> getVisitors() {
            return visitors;
        }
    }

    /**
     * Emitter for {@link ParameterChangedEvent}
     */
    class ParameterChangedEventEmitter extends JIPipeEventEmitter<ParameterChangedEvent, ParameterChangedEventListener> {
        @Override
        protected void call(ParameterChangedEventListener listener, ParameterChangedEvent event) {
            listener.onParameterChanged(event);
        }
    }

    /**
     * Listener for {@link ParameterChangedEvent}
     */
    interface ParameterChangedEventListener {
        void onParameterChanged(ParameterChangedEvent event);
    }

    /**
     * Triggered by an {@link JIPipeParameterCollection} if the list of available parameters is changed.
     * Please be very careful with this event, as it can trigger infinite loops while loading data from parameters.
     * If possible, use {@link ParameterUIChangedEvent}, if you only want the UI to update.
     */
    class ParameterStructureChangedEvent extends AbstractJIPipeEvent {
        private final Set<Object> visitors = new HashSet<>();

        /**
         * @param source event source
         */
        public ParameterStructureChangedEvent(JIPipeParameterCollection source) {
            super(source);
        }

        public Set<Object> getVisitors() {
            return visitors;
        }
    }

    /**
     * Listener for {@link ParameterStructureChangedEvent}
     */
    interface ParameterStructureChangedEventListener {
        void onParameterStructureChanged(ParameterStructureChangedEvent event);
    }

    /**
     * Emitter for {@link ParameterStructureChangedEvent}
     */
    class ParameterStructureChangedEventEmitter extends JIPipeEventEmitter<ParameterStructureChangedEvent, ParameterStructureChangedEventListener> {

        @Override
        protected void call(ParameterStructureChangedEventListener listener, ParameterStructureChangedEvent event) {
            listener.onParameterStructureChanged(event);
        }
    }

    /**
     * Triggered by an {@link JIPipeParameterCollection} if the parameter UI should be updated
     */
    class ParameterUIChangedEvent extends AbstractJIPipeEvent {
        private final Set<Object> visitors = new HashSet<>();

        /**
         * @param source event source
         */
        public ParameterUIChangedEvent(JIPipeParameterCollection source) {
            super(source);
        }

        public Set<Object> getVisitors() {
            return visitors;
        }
    }

    /**
     * Listener for {@link ParameterUIChangedEvent}
     */
    interface ParameterUIChangedEventListener {
        void onParameterUIChanged(ParameterUIChangedEvent event);
    }

    /**
     * Emitter for {@link ParameterUIChangedEvent}
     */
    class ParameterUIChangedEventEmitter extends JIPipeEventEmitter<ParameterUIChangedEvent, ParameterUIChangedEventListener> {

        @Override
        protected void call(ParameterUIChangedEventListener listener, ParameterUIChangedEvent event) {
            listener.onParameterUIChanged(event);
        }
    }
}
