package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;

public class NodeTemplateSettings implements JIPipeParameterCollection {

    public static String ID = "node-templates";

    private final EventBus eventBus = new EventBus();
    private JIPipeNodeTemplate.List nodeTemplates = new JIPipeNodeTemplate.List();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Node templates", description = "List of globally available node templates.")
    @JIPipeParameter("node-templates")
    public JIPipeNodeTemplate.List getNodeTemplates() {
        return nodeTemplates;
    }

    @JIPipeParameter("node-templates")
    public void setNodeTemplates(JIPipeNodeTemplate.List nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }

    public static NodeTemplateSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, NodeTemplateSettings.class);
    }

    /**
     * This event should always be triggered into NodeTemplateSettings.getInstance().getEventBus()
     * (even if non-global). Will refresh the {@link org.hkijena.jipipe.extensions.nodetemplate.NodeTemplateBox} instances.
     * Should be triggered if the user added any templates
     */
    public static class NodeTemplatesRefreshedEvent {
    }
}
