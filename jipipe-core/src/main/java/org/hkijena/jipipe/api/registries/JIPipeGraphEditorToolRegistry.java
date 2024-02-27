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
