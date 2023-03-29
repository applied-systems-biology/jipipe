package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grapheditortool.JIPipeGraphEditorTool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeGraphEditorToolRegistry {
    private final JIPipe jiPipe;
    private final List<Class<? extends JIPipeGraphEditorTool>> registeredTools = new ArrayList<>();

    public JIPipeGraphEditorToolRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public List<Class<? extends JIPipeGraphEditorTool>> getRegisteredTools() {
        return Collections.unmodifiableList(registeredTools);
    }

    public void register(Class<? extends JIPipeGraphEditorTool> klass) {
        registeredTools.add(klass);
    }
}
