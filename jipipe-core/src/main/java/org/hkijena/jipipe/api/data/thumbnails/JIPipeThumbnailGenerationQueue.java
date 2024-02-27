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

package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;

import javax.swing.*;
import java.lang.ref.WeakReference;

public class JIPipeThumbnailGenerationQueue {
    private static JIPipeThumbnailGenerationQueue INSTANCE;
    private final JIPipeRunnerQueue runnerQueue = new JIPipeRunnerQueue("Thumbnails");
    private final ThumbnailGeneratedEventEmitter thumbnailGeneratedEventEmitter = new ThumbnailGeneratedEventEmitter();

    public JIPipeThumbnailGenerationQueue() {

    }
    public static synchronized JIPipeThumbnailGenerationQueue getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new JIPipeThumbnailGenerationQueue();
        }
        return INSTANCE;
    }

    public ThumbnailGeneratedEventEmitter getThumbnailGeneratedEventEmitter() {
        return thumbnailGeneratedEventEmitter;
    }

    public void enqueue(JIPipeDataItemStore dataItemStore, int width, int height) {

        ThumbnailGenerationRun run = new ThumbnailGenerationRun(this, dataItemStore, width, height);

        JIPipeData data = dataItemStore.get();
        if(data != null) {
            if(data.getClass().getAnnotation(JIPipeFastThumbnail.class) != null) {
                // Schedule using Swing
                SwingUtilities.invokeLater(run);
                return;
            }
        }

        // Schedule new task
        runnerQueue.enqueue(run);
    }

    public JIPipeRunnerQueue getRunnerQueue() {
        return runnerQueue;
    }

    public static class ThumbnailGenerationRun extends AbstractJIPipeRunnable {
        private final JIPipeThumbnailGenerationQueue queue;
        private final WeakReference<JIPipeDataItemStore> dataStoreReference;
        private final int width;
        private final int height;

        public ThumbnailGenerationRun(JIPipeThumbnailGenerationQueue queue, JIPipeDataItemStore dataItemStore, int width, int height) {
            this.queue = queue;
            this.dataStoreReference = new WeakReference<>(dataItemStore);
            this.width = width;
            this.height = height;
        }

        @Override
        public String getTaskLabel() {
            return "Generate thumbnail";
        }

        @Override
        public void run() {
            JIPipeDataItemStore dataItemStore = dataStoreReference.get();
            if(dataItemStore != null) {
                JIPipeData data = dataItemStore.get();
                if(data != null) {
                    JIPipeThumbnailData thumbnail = data.createThumbnail(width, height, getProgressInfo());
                    dataItemStore.setThumbnail(thumbnail);
                    queue.thumbnailGeneratedEventEmitter.emit(new ThumbnailGeneratedEvent(dataItemStore));
                }
            }
        }

        public JIPipeThumbnailGenerationQueue getQueue() {
            return queue;
        }

        public WeakReference<JIPipeDataItemStore> getDataStoreReference() {
            return dataStoreReference;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public static class ThumbnailGeneratedEvent extends AbstractJIPipeEvent {
        private final WeakReference<JIPipeDataItemStore> storeReference;
        public ThumbnailGeneratedEvent(JIPipeDataItemStore source) {
            super(new WeakReference<>(source));
            this.storeReference = new WeakReference<>(source);
        }

        public JIPipeDataItemStore getStore() {
            return storeReference.get();
        }
    }

    public interface ThumbnailGeneratedEventListener {
        void onThumbnailGenerated(ThumbnailGeneratedEvent event);
    }

    public static class ThumbnailGeneratedEventEmitter extends JIPipeEventEmitter<ThumbnailGeneratedEvent, ThumbnailGeneratedEventListener> {

        @Override
        protected void call(ThumbnailGeneratedEventListener thumbnailGeneratedEventListener, ThumbnailGeneratedEvent event) {
            thumbnailGeneratedEventListener.onThumbnailGenerated(event);
        }
    }
}
