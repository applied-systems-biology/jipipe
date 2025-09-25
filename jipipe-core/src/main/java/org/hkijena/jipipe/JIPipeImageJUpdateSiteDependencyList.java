package org.hkijena.jipipe;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * the List class
 */
public class JIPipeImageJUpdateSiteDependencyList extends JIPipeListParameter<JIPipeImageJUpdateSiteDependency> {
    public JIPipeImageJUpdateSiteDependencyList() {
        super(JIPipeImageJUpdateSiteDependency.class);
    }

    public JIPipeImageJUpdateSiteDependencyList(JIPipeImageJUpdateSiteDependencyList other) {
        super(JIPipeImageJUpdateSiteDependency.class);
        for (JIPipeImageJUpdateSiteDependency dependency : other) {
            add(new JIPipeImageJUpdateSiteDependency(dependency));
        }
    }
}
