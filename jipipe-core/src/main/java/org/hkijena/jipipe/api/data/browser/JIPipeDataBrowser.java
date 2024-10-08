package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.JIPipeDefaultJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
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
    default <T extends JIPipeData> Future<T> getData(Class<T> klass) {
        return getData(klass, new JIPipeProgressInfo());
    }


    /**
     * Gets the store for the fully downloaded data
     * @param progressInfo the progress info for the download
     * @return the store. can be null
     */
    <T extends JIPipeData> Future<T> getData(Class<T> klass, JIPipeProgressInfo progressInfo);

    /**
     * Gets the string representation of the data
     * @return the string
     */
    default Future<String> getDataAsString() {
        return getDataAsString(new JIPipeProgressInfo());
    }

    /**
     * Gets the string representation of the data
     * @param progressInfo the progress info
     * @return the string
     */
    Future<String> getDataAsString(JIPipeProgressInfo progressInfo);

    /**
     * Gets the detailed string representation of the data
     * @return the string
     */
    default Future<String> getDataAsDetailedString() {
        return getDataAsDetailedString(new JIPipeProgressInfo());
    }

    /**
     * Gets the detailed string representation of the data
     * @param progressInfo the progress info
     * @return the string
     */
    Future<String> getDataAsDetailedString(JIPipeProgressInfo progressInfo);

    /**
     * The data class
     * @return the data class
     */
    Class<? extends JIPipeData> getDataClass();

    /**
     * The data class info
     * @return the info
     */
    default JIPipeDataInfo getDataTypeInfo() {
        return JIPipeDataInfo.getInstance(getDataClass());
    }

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
