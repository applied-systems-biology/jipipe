package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.net.URL;
import java.util.function.Consumer;

public class JIPipeDefaultParameterCollectionContextAction implements JIPipeParameterCollectionContextAction {

    private final SetJIPipeDocumentation documentation;
    private final URL iconURL;
    private final Consumer<JIPipeWorkbench> function;

    public JIPipeDefaultParameterCollectionContextAction(SetJIPipeDocumentation documentation, URL iconURL, Consumer<JIPipeWorkbench> function) {
        this.documentation = documentation;
        this.iconURL = iconURL;
        this.function = function;
    }

    @Override
    public SetJIPipeDocumentation getDocumentation() {
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
