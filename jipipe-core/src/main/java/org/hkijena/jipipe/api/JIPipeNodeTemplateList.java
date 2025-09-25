package org.hkijena.jipipe.api;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class JIPipeNodeTemplateList extends JIPipeListParameter<JIPipeNodeTemplate> {

    public JIPipeNodeTemplateList() {
        super(JIPipeNodeTemplate.class);
    }

    public JIPipeNodeTemplateList(JIPipeNodeTemplateList other) {
        super(JIPipeNodeTemplate.class);
        for (JIPipeNodeTemplate template : other) {
            add(new JIPipeNodeTemplate(template));
        }
    }
}
