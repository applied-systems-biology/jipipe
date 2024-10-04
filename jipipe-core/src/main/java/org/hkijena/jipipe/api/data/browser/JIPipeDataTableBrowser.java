package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import java.io.Closeable;
import java.util.concurrent.Future;

public interface JIPipeDataTableBrowser extends Closeable, AutoCloseable {

    /**
     * Downloads the full data table
     * @return the data table
     */
    Future<JIPipeDataTable> getDataTable();

    /**
     * Gets the serialized data table info
     * @return the data table info
     */
    Future<JIPipeDataTableInfo> getDataTableInfo();

    /**
     * Created when the data table browser is in some way updated
     */
    class UpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeDataTableBrowser browser;
        private final String type;
        public UpdatedEvent(Object source, JIPipeDataTableBrowser browser, String type) {
            super(source);
            this.browser = browser;
            this.type = type;
        }

        public JIPipeDataTableBrowser getBrowser() {
            return browser;
        }

        public String getType() {
            return type;
        }
    }

    interface UpdatedEventListener {
        void onDataTableBrowserUpdated(UpdatedEvent event);
    }

    class UpdatedEventEmitter extends JIPipeEventEmitter<UpdatedEvent, UpdatedEventListener> {
        @Override
        protected void call(UpdatedEventListener updatedEventListener, UpdatedEvent event) {
            updatedEventListener.onDataTableBrowserUpdated(event);
        }
    }
}
