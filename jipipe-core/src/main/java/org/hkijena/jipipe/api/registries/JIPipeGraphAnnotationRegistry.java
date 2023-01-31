package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.graphannotation.JIPipeGraphAnnotationTool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeGraphAnnotationRegistry {
    private final JIPipe jiPipe;
    private final List<Class<? extends JIPipeGraphAnnotationTool>> registeredTools = new ArrayList<>();

    public JIPipeGraphAnnotationRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public List<Class<? extends JIPipeGraphAnnotationTool>> getRegisteredTools() {
        return Collections.unmodifiableList(registeredTools);
    }
}
