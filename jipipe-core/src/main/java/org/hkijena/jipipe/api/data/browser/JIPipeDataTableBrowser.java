package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Future;

public interface JIPipeDataTableBrowser extends Closeable, AutoCloseable {

    /**
     * Creates a data browser
     * @param row the row
     * @return the browser
     */
    default JIPipeDataBrowser browse(int row) {
        return browse(row, null);
    }

    /**
     * Creates a data browser (optionally supports a data annotation column)
     * @param row the row
     * @param dataAnnotationColumn the data annotation column (null for no column)
     * @return the browser
     */
    JIPipeDataBrowser browse(int row, String dataAnnotationColumn);

    /**
     * Gets the data table info
     * @return the info
     */
    Future<JIPipeDataTableInfo> getDataTableInfo();

    /**
     * Gets the whole data table
     * @return the data table
     */
    Future<JIPipeDataTable> getDataTable(JIPipeProgressInfo progressInfo);

    /*
    Future<List<String>> getDataAnnotationColumns();

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
