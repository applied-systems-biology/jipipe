/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.ui.registries.JIPipeUIAlgorithmRegistry;

import java.net.URL;

/**
 * Registers a Java algorithm
 */
public class JIPipeJavaAlgorithmRegistrationTask extends JIPipeDefaultAlgorithmRegistrationTask {

    private JIPipeDependency source;
    private String id;
    private Class<? extends JIPipeGraphNode> algorithmClass;
    private URL icon;
    private boolean alreadyRegistered = false;

    /**
     * Creates a new registration task
     *
     * @param id             The id
     * @param algorithmClass The algorithm class
     * @param source         The dependency the registers the algorithm
     * @param icon
     */
    public JIPipeJavaAlgorithmRegistrationTask(String id, Class<? extends JIPipeGraphNode> algorithmClass, JIPipeDependency source, URL icon) {
        this.source = source;
        this.id = id;
        this.algorithmClass = algorithmClass;
        this.icon = icon;

        for (JIPipeInputSlot slot : algorithmClass.getAnnotationsByType(JIPipeInputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
        for (JIPipeOutputSlot slot : algorithmClass.getAnnotationsByType(JIPipeOutputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
    }

    @Override
    public void register() {
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        JIPipeJavaNodeInfo info = new JIPipeJavaNodeInfo(id, algorithmClass);
        JIPipeNodeRegistry.getInstance().register(info, source);
        if (icon != null)
            JIPipeUIAlgorithmRegistry.getInstance().registerIcon(info, icon);
    }

    @Override
    public String toString() {
        return id + " @ " + algorithmClass;
    }
}
