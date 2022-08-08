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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.utils.UIUtils;

import java.net.URL;

/**
 * Registers a Java algorithm
 */
public class JIPipeJavaNodeRegistrationTask extends JIPipeDefaultNodeRegistrationTask {

    private final JIPipeDependency source;
    private final String id;
    private final Class<? extends JIPipeGraphNode> nodeClass;
    private final URL icon;
    private boolean alreadyRegistered = false;

    /**
     * Creates a new registration task
     *
     * @param id        The id
     * @param nodeClass The algorithm class
     * @param source    The dependency the registers the algorithm
     * @param icon      the icon
     */
    public JIPipeJavaNodeRegistrationTask(String id, Class<? extends JIPipeGraphNode> nodeClass, JIPipeDependency source, URL icon) {
        this.source = source;
        this.id = id;
        this.nodeClass = nodeClass;
        this.icon = icon;

        for (JIPipeInputSlot slot : nodeClass.getAnnotationsByType(JIPipeInputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
        for (JIPipeOutputSlot slot : nodeClass.getAnnotationsByType(JIPipeOutputSlot.class)) {
            getDependencyDatatypeClasses().add(slot.value());
        }
    }

    @Override
    public void register() {
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        JIPipeJavaNodeInfo info = new JIPipeJavaNodeInfo(id, nodeClass);
        JIPipe.getNodes().register(info, source);
        if(nodeClass.getAnnotation(Deprecated.class) != null) {
            JIPipe.getNodes().registerIcon(info, UIUtils.getIconURLFromResources("emblems/deprecated.png"));
        }
        else {
            if (icon != null)
                JIPipe.getNodes().registerIcon(info, icon);
        }
    }

    @Override
    public String toString() {
        return id + " @ " + nodeClass;
    }
}
