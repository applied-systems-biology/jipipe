package org.hkijena.jipipe.extensions.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;

public class NodeTemplateSettings extends AbstractJIPipeParameterCollection {

    public static String ID = "node-templates";
    private JIPipeNodeTemplate.List nodeTemplates = new JIPipeNodeTemplate.List();

    private StringList nodeTemplateDownloadRepositories = new StringList();

    public NodeTemplateSettings() {
        nodeTemplateDownloadRepositories.add("https://github.com/applied-systems-biology/JIPipe-Repositories/raw/main/node-templates/node-templates.json");
    }

    public static NodeTemplateSettings getInstance() {
        return JIPipe.getSettings().getSettings(ID, NodeTemplateSettings.class);
    }

    public static void triggerRefreshedEvent() {
        JIPipe.getInstance().getNodeTemplatesRefreshedEventEmitter().emit(new NodeTemplatesRefreshedEvent());
    }

    @SetJIPipeDocumentation(name = "Node templates", description = "List of globally available node templates.")
    @JIPipeParameter("node-templates")
    public JIPipeNodeTemplate.List getNodeTemplates() {
        return nodeTemplates;
    }

    @JIPipeParameter("node-templates")
    public void setNodeTemplates(JIPipeNodeTemplate.List nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }

    @SetJIPipeDocumentation(name = "Template downloader repositories", description = "List of repositories for the 'Get more templates' feature")
    @JIPipeParameter("template-download-repositories")
    public StringList getNodeTemplateDownloadRepositories() {
        return nodeTemplateDownloadRepositories;
    }

    @JIPipeParameter("template-download-repositories")
    public void setNodeTemplateDownloadRepositories(StringList projectTemplateDownloadRepositories) {
        this.nodeTemplateDownloadRepositories = projectTemplateDownloadRepositories;
    }

}
