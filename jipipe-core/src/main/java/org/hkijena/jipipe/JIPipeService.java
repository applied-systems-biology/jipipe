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

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.ui.registries.JIPipeCustomMenuRegistry;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all JIPipe resources
 */
public interface JIPipeService extends Service, JIPipeValidatable {

    JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry();

    List<JIPipeDependency> getRegisteredExtensions();

    JIPipeNodeRegistry getNodeRegistry();

    JIPipeDatatypeRegistry getDatatypeRegistry();

    JIPipeParameterTypeRegistry getParameterTypeRegistry();

    JIPipeCustomMenuRegistry getCustomMenuRegistry();

    JIPipeSettingsRegistry getSettingsRegistry();

    JIPipeExpressionRegistry getExpressionRegistry();

    JIPipeUtilityRegistry getUtilityRegistry();

    JIPipeExternalEnvironmentRegistry getExternalEnvironmentRegistry();

    JIPipeExtensionRegistry getExtensionRegistry();

    JIPipeProjectTemplateRegistry getProjectTemplateRegistry();

    JIPipeGraphEditorToolRegistry getGraphEditorToolRegistry();

    JIPipeExpressionRegistry getTableOperationRegistry();

    Set<String> getRegisteredExtensionIds();

    JIPipeDependency findExtensionById(String dependencyId);

    DatatypeRegisteredEventEmitter getDatatypeRegisteredEventEmitter();

    ExtensionContentAddedEventEmitter getExtensionContentAddedEventEmitter();

    ExtensionContentRemovedEventEmitter getExtensionContentRemovedEventEmitter();

    ExtensionDiscoveredEventEmitter getExtensionDiscoveredEventEmitter();

    ExtensionRegisteredEventEmitter getExtensionRegisteredEventEmitter();

    NodeInfoRegisteredEventEmitter getNodeInfoRegisteredEventEmitter();

    public interface DatatypeRegisteredEventListener {
        void onJIPipeDatatypeRegistered(DatatypeRegisteredEvent event);
    }

    public interface ExtensionContentAddedEventListener {
        void onJIPipeExtensionContentAdded(ExtensionContentAddedEvent event);
    }

    public interface ExtensionContentRemovedEventListener {
        void onJIPipeExtensionContentRemoved(ExtensionContentRemovedEvent event);
    }

    public interface ExtensionDiscoveredEventListener {
        void onJIPipeExtensionDiscovered(ExtensionDiscoveredEvent event);
    }

    public interface ExtensionRegisteredEventListener {
        void onJIPipeExtensionRegistered(ExtensionRegisteredEvent event);
    }

    public interface NodeInfoRegisteredEventListener {
        void onJIPipeNodeInfoRegistered(NodeInfoRegisteredEvent event);
    }

    /**
     * Triggered when a new data type is registered
     */
    public static class DatatypeRegisteredEvent extends AbstractJIPipeEvent {
        private final JIPipe registry;
        private final String id;

        /**
         * @param registry the event source
         * @param id       the data type id
         */
        public DatatypeRegisteredEvent(JIPipe registry, String id) {
            super(registry);
            this.registry = registry;
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class DatatypeRegisteredEventEmitter extends JIPipeEventEmitter<DatatypeRegisteredEvent, DatatypeRegisteredEventListener> {
        @Override
        protected void call(DatatypeRegisteredEventListener datatypeRegisteredEventListener, DatatypeRegisteredEvent event) {
            datatypeRegisteredEventListener.onJIPipeDatatypeRegistered(event);
        }
    }

    /**
     * Generated when content is added to an {@link JIPipeJsonPlugin}
     */
    public static class ExtensionContentAddedEvent extends AbstractJIPipeEvent {
        private final JIPipeJsonPlugin extension;
        private final Object content;

        /**
         * @param extension event source
         * @param content   the new content
         */
        public ExtensionContentAddedEvent(JIPipeJsonPlugin extension, Object content) {
            super(extension);
            this.extension = extension;
            this.content = content;
        }

        public JIPipeJsonPlugin getExtension() {
            return extension;
        }

        public Object getContent() {
            return content;
        }
    }

    public static class ExtensionContentAddedEventEmitter extends JIPipeEventEmitter<ExtensionContentAddedEvent, ExtensionContentAddedEventListener> {
        @Override
        protected void call(ExtensionContentAddedEventListener extensionContentAddedEventListener, ExtensionContentAddedEvent event) {
            extensionContentAddedEventListener.onJIPipeExtensionContentAdded(event);
        }
    }

    /**
     * Generated when content is removed from an {@link JIPipeJsonPlugin}
     */
    public static class ExtensionContentRemovedEvent extends AbstractJIPipeEvent {
        private final JIPipeJsonPlugin extension;
        private final Object content;

        /**
         * @param extension event source
         * @param content   removed content
         */
        public ExtensionContentRemovedEvent(JIPipeJsonPlugin extension, Object content) {
            super(extension);
            this.extension = extension;
            this.content = content;
        }

        public JIPipeJsonPlugin getExtension() {
            return extension;
        }

        public Object getContent() {
            return content;
        }
    }

    public static class ExtensionContentRemovedEventEmitter extends JIPipeEventEmitter<ExtensionContentRemovedEvent, ExtensionContentRemovedEventListener> {

        @Override
        protected void call(ExtensionContentRemovedEventListener extensionContentRemovedEventListener, ExtensionContentRemovedEvent event) {
            extensionContentRemovedEventListener.onJIPipeExtensionContentRemoved(event);
        }
    }

    /**
     * Triggered when a new extension was discovered
     */
    public static class ExtensionDiscoveredEvent extends AbstractJIPipeEvent {
        private final JIPipe registry;
        private final JIPipeDependency extension;

        public ExtensionDiscoveredEvent(JIPipe registry, JIPipeDependency extension) {
            super(registry);
            this.registry = registry;
            this.extension = extension;
        }

        public JIPipe getRegistry() {
            return registry;
        }

        public JIPipeDependency getExtension() {
            return extension;
        }
    }

    public static class ExtensionDiscoveredEventEmitter extends JIPipeEventEmitter<ExtensionDiscoveredEvent, ExtensionDiscoveredEventListener> {

        @Override
        protected void call(ExtensionDiscoveredEventListener extensionDiscoveredEventListener, ExtensionDiscoveredEvent event) {
            extensionDiscoveredEventListener.onJIPipeExtensionDiscovered(event);
        }
    }

    /**
     * Triggered by {@link JIPipeService} when an extension is registered
     */
    public static class ExtensionRegisteredEvent extends AbstractJIPipeEvent {
        private final JIPipeService registry;
        private final JIPipeDependency extension;

        /**
         * @param registry  event source
         * @param extension registered extension
         */
        public ExtensionRegisteredEvent(JIPipeService registry, JIPipeDependency extension) {
            super(registry);
            this.registry = registry;
            this.extension = extension;
        }

        public JIPipeService getRegistry() {
            return registry;
        }

        public JIPipeDependency getExtension() {
            return extension;
        }
    }

    public static class ExtensionRegisteredEventEmitter extends JIPipeEventEmitter<ExtensionRegisteredEvent, ExtensionRegisteredEventListener> {

        @Override
        protected void call(ExtensionRegisteredEventListener extensionRegisteredEventListener, ExtensionRegisteredEvent event) {
            extensionRegisteredEventListener.onJIPipeExtensionRegistered(event);
        }
    }

    /**
     * Triggered when an algorithm is registered
     */
    public static class NodeInfoRegisteredEvent extends AbstractJIPipeEvent {

        private final JIPipeService registry;
        private final JIPipeNodeInfo nodeInfo;

        /**
         * @param registry event source
         * @param nodeInfo the algorithm type
         */
        public NodeInfoRegisteredEvent(JIPipeService registry, JIPipeNodeInfo nodeInfo) {
            super(registry);
            this.registry = registry;
            this.nodeInfo = nodeInfo;
        }

        public JIPipeNodeInfo getNodeInfo() {
            return nodeInfo;
        }
    }

    public static class NodeInfoRegisteredEventEmitter extends JIPipeEventEmitter<NodeInfoRegisteredEvent, NodeInfoRegisteredEventListener> {

        @Override
        protected void call(NodeInfoRegisteredEventListener nodeInfoRegisteredEventListener, NodeInfoRegisteredEvent event) {
            nodeInfoRegisteredEventListener.onJIPipeNodeInfoRegistered(event);
        }
    }
}
