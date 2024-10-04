package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import java.io.Closeable;
import java.util.concurrent.Future;

public interface JIPipeDataBrowser extends Closeable, AutoCloseable {

    /**
     * Gets the emitter for the updated event
     * @return the emitter
     */
    UpdatedEventEmitter getUpdatedEventEmitter();

    /**
     * Gets the store for the fully downloaded data
     * @return the store. can be null
     */
    Future<JIPipeData> getData();

    /**
     * The data class
     * @return the data class
     */
    Class<? extends JIPipeData> getDataClass();

    /**
     * Created when the data browser is in some way updated
     */
    class UpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeDataBrowser browser;
        private final String type;
        public UpdatedEvent(Object source, JIPipeDataBrowser browser, String type) {
            super(source);
            this.browser = browser;
            this.type = type;
        }

        public JIPipeDataBrowser getBrowser() {
            return browser;
        }

        public String getType() {
            return type;
        }
    }

    interface UpdatedEventListener {
        void onDataBrowserUpdated(UpdatedEvent event);
    }

    class UpdatedEventEmitter extends JIPipeEventEmitter<UpdatedEvent, UpdatedEventListener> {
        @Override
        protected void call(UpdatedEventListener updatedEventListener, UpdatedEvent event) {
            updatedEventListener.onDataBrowserUpdated(event);
        }
    }
}
