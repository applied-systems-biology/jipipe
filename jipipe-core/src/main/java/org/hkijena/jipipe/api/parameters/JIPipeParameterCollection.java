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
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

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
     * Adds a listener to the change of a parameter of the specified key.
     * Wrapper around creating an anonymous object
     *
     * @param key      the parameter key
     * @param consumer the listener
     */
    default void addParameterChangeListener(String key, Consumer<ParameterChangedEvent> consumer) {
        getEventBus().register(new Object() {
            @Subscribe
            public void onParameterChanged(ParameterChangedEvent event) {
                if (Objects.equals(key, event.getKey())) {
                    consumer.accept(event);
                }
            }
        });
    }

    /**
     * Adds a listener to the change of any parameter.
     * Wrapper around creating an anonymous object
     *
     * @param consumer the listener
     */
    default void addParameterChangeListener(Consumer<ParameterChangedEvent> consumer) {
        getEventBus().register(new Object() {
            @Subscribe
            public void onParameterChanged(ParameterChangedEvent event) {
                consumer.accept(event);
            }
        });
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
     * Gets the event bus that posts events about the parameters
     *
     * @return The event bus triggering {@link ParameterChangedEvent} and {@link ParameterStructureChangedEvent}
     */
    EventBus getEventBus();

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
    class ParameterChangedEvent {
        private final Object source;
        private final String key;
        private final Set<Object> visitors = new HashSet<>();

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

        public Set<Object> getVisitors() {
            return visitors;
        }
    }

    /**
     * Triggered by an {@link JIPipeParameterCollection} if the list of available parameters is changed.
     * Please be very careful with this event, as it can trigger infinite loops while loading data from parameters.
     * If possible, use {@link ParameterUIChangedEvent}, if you only want the UI to update.
     */
    class ParameterStructureChangedEvent {
        private final JIPipeParameterCollection source;
        private final Set<Object> visitors = new HashSet<>();

        /**
         * @param source event source
         */
        public ParameterStructureChangedEvent(JIPipeParameterCollection source) {
            this.source = source;
        }

        public JIPipeParameterCollection getSource() {
            return source;
        }

        public Set<Object> getVisitors() {
            return visitors;
        }
    }

    /**
     * Triggered by an {@link JIPipeParameterCollection} if the parameter UI should be updated
     */
    class ParameterUIChangedEvent {
        private final JIPipeParameterCollection source;
        private final Set<Object> visitors = new HashSet<>();

        /**
         * @param source event source
         */
        public ParameterUIChangedEvent(JIPipeParameterCollection source) {
            this.source = source;
        }

        public JIPipeParameterCollection getSource() {
            return source;
        }

        public Set<Object> getVisitors() {
            return visitors;
        }
    }
}
