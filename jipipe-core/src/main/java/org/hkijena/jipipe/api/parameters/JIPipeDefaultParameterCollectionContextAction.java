package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.net.URL;
import java.util.function.Consumer;

public class JIPipeDefaultParameterCollectionContextAction implements JIPipeParameterCollectionContextAction {

    private final JIPipeDocumentation documentation;
    private final URL iconURL;
    private final Consumer<JIPipeWorkbench> function;

    public JIPipeDefaultParameterCollectionContextAction(JIPipeDocumentation documentation, URL iconURL, Consumer<JIPipeWorkbench> function) {
        this.documentation = documentation;
        this.iconURL = iconURL;
        this.function = function;
    }

    @Override
    public JIPipeDocumentation getDocumentation() {
        return documentation;
    }

    @Override
    public URL getIconURL() {
        return iconURL;
    }

    @Override
    public void accept(JIPipeWorkbench workbench) {
        function.accept(workbench);
    }
}
