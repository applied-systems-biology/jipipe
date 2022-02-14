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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

/**
 * A component that acts as table cell (or other component) that generates a preview of a cached {@link org.hkijena.jipipe.api.data.JIPipeData}
 * Preview generation is put into its own thread. On finishing the thread, the parent component's repaint method is called
 */
public class JIPipeCachedDataPreview extends JPanel {
    private Component parentComponent;
    private WeakReference<JIPipeVirtualData> data;
    private Worker worker;

    /**
     * Creates a new instance
     *
     * @param parentComponent The parent component that contains the preview
     * @param data            the data
     * @param deferRendering  if true, the preview will not be immediately rendered
     */
    public JIPipeCachedDataPreview(Component parentComponent, JIPipeVirtualData data, boolean deferRendering) {
        this.parentComponent = parentComponent;
        this.data = new WeakReference<>(data);
        initialize();
        if (!deferRendering)
            renderPreview();
    }

    public boolean isRenderedOrRendering() {
        return worker != null;
    }

    /**
     * Renders the preview if it is not already rendered
     */
    public void renderPreview() {
        if (worker == null) {
            worker = new Worker(this);
            worker.execute();
        }
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
     * Returns the stored data or null if it was already cleared
     *
     * @return the data
     */
    public JIPipeVirtualData getData() {
        return data.get();
    }

    /**
     * The worker that generates the component
     */
    private static class Worker extends SwingWorker<Component, Object> {

        private final JIPipeCachedDataPreview parent;
        private final int width = GeneralDataSettings.getInstance().getPreviewSize();

        private Worker(JIPipeCachedDataPreview parent) {
            this.parent = parent;
        }

        @Override
        protected Component doInBackground() throws Exception {
            JIPipeVirtualData virtualData = parent.data.get();
            if (virtualData != null)
                return virtualData.getData(new JIPipeProgressInfo()).preview(width, width);
            else
                return new JLabel("N/A");
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
