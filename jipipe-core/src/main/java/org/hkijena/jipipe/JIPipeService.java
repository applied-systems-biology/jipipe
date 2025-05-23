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
import org.hkijena.jipipe.desktop.api.registries.JIPipeCustomMenuRegistry;
import org.scijava.service.Service;

import java.util.List;
import java.util.Set;

/**
 * Contains all JIPipe resources
 */
public interface JIPipeService extends Service, JIPipeValidatable {

    JIPipeRecentProjectsRegistry getRecentProjectsRegistry();

    JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry();

    List<JIPipeDependency> getRegisteredExtensions();

    JIPipeNodeRegistry getNodeRegistry();

    JIPipeNodeTemplateRegistry getNodeTemplateRegistry();

    JIPipeDatatypeRegistry getDatatypeRegistry();

    JIPipeParameterTypeRegistry getParameterTypeRegistry();

    JIPipeCustomMenuRegistry getCustomMenuRegistry();

    JIPipeApplicationSettingsRegistry getApplicationSettingsRegistry();

    JIPipeProjectSettingsRegistry getProjectSettingsRegistry();

    JIPipeMetadataRegistry getMetadataRegistry();

    JIPipeExpressionRegistry getExpressionRegistry();

    JIPipeUtilityRegistry getUtilityRegistry();

    JIPipeExternalEnvironmentRegistry getExternalEnvironmentRegistry();

    JIPipePluginRegistry getPluginRegistry();

    JIPipeProjectTemplateRegistry getProjectTemplateRegistry();

    JIPipeGraphEditorToolRegistry getGraphEditorToolRegistry();

    JIPipeExpressionRegistry getTableOperationRegistry();

    JIPipeArtifactsRegistry getArtifactsRegistry();

    Set<String> getRegisteredExtensionIds();

    JIPipeDependency findExtensionById(String dependencyId);

    DatatypeRegisteredEventEmitter getDatatypeRegisteredEventEmitter();

    ExtensionContentAddedEventEmitter getExtensionContentAddedEventEmitter();

    ExtensionContentRemovedEventEmitter getExtensionContentRemovedEventEmitter();

    ExtensionDiscoveredEventEmitter getExtensionDiscoveredEventEmitter();

    ExtensionRegisteredEventEmitter getExtensionRegisteredEventEmitter();

    NodeInfoRegisteredEventEmitter getNodeInfoRegisteredEventEmitter();

    interface DatatypeRegisteredEventListener {
        void onJIPipeDatatypeRegistered(DatatypeRegisteredEvent event);
    }

    interface PluginContentAddedEventListener {
        void onJIPipePluginContentAdded(ExtensionContentAddedEvent event);
    }

    interface PluginContentRemovedEventListener {
        void onJIPipePluginContentRemoved(ExtensionContentRemovedEvent event);
    }

    interface PluginDiscoveredEventListener {
        void onJIPipePluginDiscovered(ExtensionDiscoveredEvent event);
    }

    interface PluginRegisteredEventListener {
        void onJIPipePluginRegistered(ExtensionRegisteredEvent event);
    }

    interface NodeInfoRegisteredEventListener {
        void onJIPipeNodeInfoRegistered(NodeInfoRegisteredEvent event);
    }

    /**
     * Triggered when a new data type is registered
     */
    class DatatypeRegisteredEvent extends AbstractJIPipeEvent {
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

    class DatatypeRegisteredEventEmitter extends JIPipeEventEmitter<DatatypeRegisteredEvent, DatatypeRegisteredEventListener> {
        @Override
        protected void call(DatatypeRegisteredEventListener datatypeRegisteredEventListener, DatatypeRegisteredEvent event) {
            datatypeRegisteredEventListener.onJIPipeDatatypeRegistered(event);
        }
    }

    /**
     * Generated when content is added to an {@link JIPipeJsonPlugin}
     */
    class ExtensionContentAddedEvent extends AbstractJIPipeEvent {
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

    class ExtensionContentAddedEventEmitter extends JIPipeEventEmitter<ExtensionContentAddedEvent, PluginContentAddedEventListener> {
        @Override
        protected void call(PluginContentAddedEventListener pluginContentAddedEventListener, ExtensionContentAddedEvent event) {
            pluginContentAddedEventListener.onJIPipePluginContentAdded(event);
        }
    }

    /**
     * Generated when content is removed from an {@link JIPipeJsonPlugin}
     */
    class ExtensionContentRemovedEvent extends AbstractJIPipeEvent {
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

    class ExtensionContentRemovedEventEmitter extends JIPipeEventEmitter<ExtensionContentRemovedEvent, PluginContentRemovedEventListener> {

        @Override
        protected void call(PluginContentRemovedEventListener pluginContentRemovedEventListener, ExtensionContentRemovedEvent event) {
            pluginContentRemovedEventListener.onJIPipePluginContentRemoved(event);
        }
    }

    /**
     * Triggered when a new extension was discovered
     */
    class ExtensionDiscoveredEvent extends AbstractJIPipeEvent {
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

    class ExtensionDiscoveredEventEmitter extends JIPipeEventEmitter<ExtensionDiscoveredEvent, PluginDiscoveredEventListener> {

        @Override
        protected void call(PluginDiscoveredEventListener pluginDiscoveredEventListener, ExtensionDiscoveredEvent event) {
            pluginDiscoveredEventListener.onJIPipePluginDiscovered(event);
        }
    }

    /**
     * Triggered by {@link JIPipeService} when an extension is registered
     */
    class ExtensionRegisteredEvent extends AbstractJIPipeEvent {
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

    class ExtensionRegisteredEventEmitter extends JIPipeEventEmitter<ExtensionRegisteredEvent, PluginRegisteredEventListener> {

        @Override
        protected void call(PluginRegisteredEventListener pluginRegisteredEventListener, ExtensionRegisteredEvent event) {
            pluginRegisteredEventListener.onJIPipePluginRegistered(event);
        }
    }

    /**
     * Triggered when an algorithm is registered
     */
    class NodeInfoRegisteredEvent extends AbstractJIPipeEvent {

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

    class NodeInfoRegisteredEventEmitter extends JIPipeEventEmitter<NodeInfoRegisteredEvent, NodeInfoRegisteredEventListener> {

        @Override
        protected void call(NodeInfoRegisteredEventListener nodeInfoRegisteredEventListener, NodeInfoRegisteredEvent event) {
            nodeInfoRegisteredEventListener.onJIPipeNodeInfoRegistered(event);
        }
    }
}
