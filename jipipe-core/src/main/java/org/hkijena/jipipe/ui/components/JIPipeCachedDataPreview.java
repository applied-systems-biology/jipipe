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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.concurrent.ExecutionException;

/**
 * A component that acts as table cell (or other component) that generates a preview of a cached {@link org.hkijena.jipipe.api.data.JIPipeData}
 * Preview generation is put into its own thread. On finishing the thread, the parent component's repaint method is called
 */
public class JIPipeCachedDataPreview extends JPanel {
    private Component parentComponent;
    private JIPipeData data;

    public JIPipeCachedDataPreview(Component parentComponent, JIPipeData data) {
        this.parentComponent = parentComponent;
        this.data = data;
        initialize();
        renderPreview();
    }

    private void renderPreview() {
        Worker worker = new Worker(this);
        worker.execute();
    }

    private void setPreview(Component component) {
        removeAll();
        if (component == null) {
            add(new JLabel("N/A"), BorderLayout.CENTER);
        } else {
            add(component, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
        if (parentComponent != null) {
            parentComponent.repaint();
        }
    }

    private void initialize() {
        setOpaque(false);
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Please wait ...", UIUtils.getIconFromResources("actions/hourglass-half.png"), JLabel.LEFT);
        add(label, BorderLayout.CENTER);
    }

    /**
     * The worker that generates the component
     */
    private static class Worker extends SwingWorker<Component, Object> {

        private final JIPipeCachedDataPreview parent;
        private final int width = GeneralUISettings.getInstance().getPreviewWidth();
        private final int height = GeneralUISettings.getInstance().getPreviewHeight();

        private Worker(JIPipeCachedDataPreview parent) {
            this.parent = parent;
        }

        @Override
        protected Component doInBackground() throws Exception {
            return parent.data.preview(width, height);
        }

        @Override
        protected void done() {
            try {
                parent.setPreview(get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                parent.setPreview(null);
            }
        }
    }
}
