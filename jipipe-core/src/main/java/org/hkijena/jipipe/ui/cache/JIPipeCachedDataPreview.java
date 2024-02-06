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

import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailGenerationQueue;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;

import javax.swing.*;
import java.awt.*;

/**
 * A component that acts as table cell (or other component) that generates a preview of a cached {@link org.hkijena.jipipe.api.data.JIPipeData}
 * Preview generation is put into its own thread. On finishing the thread, the parent component's repaint method is called
 */
public class JIPipeCachedDataPreview extends JPanel implements JIPipeThumbnailGenerationQueue.ThumbnailGeneratedEventListener {
    private final Component parentComponent;
    private final Store<JIPipeDataItemStore> data;
    private boolean renderedOrRendering;

    /**
     * Creates a new instance
     *
     * @param parentComponent The parent component that contains the preview
     * @param data            the data
     * @param deferRendering  if true, the preview will not be immediately rendered
     */
    public JIPipeCachedDataPreview(Component parentComponent, JIPipeDataItemStore data, boolean deferRendering) {
        this.parentComponent = parentComponent;
        this.data = new WeakStore<>(data);
        initialize();
        if (!deferRendering) {
            renderPreview();
        }
    }

    /**
     * Creates a new instance
     *
     * @param parentComponent The parent component that contains the preview
     * @param data            the data
     * @param deferRendering  if true, the preview will not be immediately rendered
     */
    public JIPipeCachedDataPreview(Component parentComponent, Store<JIPipeDataItemStore> data, boolean deferRendering) {
        this.parentComponent = parentComponent;
        this.data = data;
        initialize();
        if (!deferRendering) {
            renderPreview();
        }
    }

    public boolean isRenderedOrRendering() {
        return renderedOrRendering;
    }

    /**
     * Renders the preview if it is not already rendered
     */
    public void renderPreview() {
        renderedOrRendering = true;
        JIPipeDataItemStore dataItemStore = data.get();
        if(dataItemStore != null) {
            JIPipeThumbnailData thumbnail = dataItemStore.getThumbnail();
            int previewSize = GeneralDataSettings.getInstance().getPreviewSize();
            boolean success = thumbnail != null;
            if(success) {
                if(thumbnail.hasSize()) {
                    if(thumbnail.getWidth() < previewSize && thumbnail.getHeight() < previewSize) {
                        success = false;
                    }
                }
            }
            if(success) {
                setPreview(thumbnail.renderToComponent(previewSize, previewSize));
            }
            else {
                JIPipeThumbnailGenerationQueue.getInstance().enqueue(dataItemStore, previewSize, previewSize);
            }
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

        JIPipeThumbnailGenerationQueue.getInstance().getThumbnailGeneratedEventEmitter().subscribe(this);
    }

    /**
     * Returns the stored data or null if it was already cleared
     *
     * @return the data
     */
    public JIPipeDataItemStore getData() {
        return data.get();
    }

    @Override
    public void onThumbnailGenerated(JIPipeThumbnailGenerationQueue.ThumbnailGeneratedEvent event) {
        JIPipeDataItemStore dataItemStore = data.get();
        if(dataItemStore != null) {
            if(event.getStore() == dataItemStore) {
                JIPipeThumbnailData thumbnail = dataItemStore.getThumbnail();
                int previewSize = GeneralDataSettings.getInstance().getPreviewSize();
                if(thumbnail != null) {
                    setPreview(thumbnail.renderToComponent(previewSize, previewSize));
                }
                else {
                    setPreview(null);
                }
            }
        }
        else {
            JIPipeThumbnailGenerationQueue.getInstance().getThumbnailGeneratedEventEmitter().unsubscribe(this);
        }
    }
}
