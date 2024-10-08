package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.utils.InstantFuture;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

public class JIPipeLocalDataBrowser implements JIPipeDataBrowser, Closeable, AutoCloseable {
    private final JIPipeDataItemStore dataItemStore;
    private final UpdatedEventEmitter updatedEventEmitter = new UpdatedEventEmitter();

    public JIPipeLocalDataBrowser(JIPipeDataItemStore dataItemStore) {
        this.dataItemStore = dataItemStore;
        dataItemStore.addUser(this);
    }

    @Override
    public void close() throws IOException {
        dataItemStore.removeUser(this);
    }

    @Override
    public UpdatedEventEmitter getUpdatedEventEmitter() {
        return updatedEventEmitter;
    }

    @Override
    public <T extends JIPipeData> Future<T> getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        return new InstantFuture<>(dataItemStore.getData(klass, progressInfo));
    }

    @Override
    public Future<String> getDataAsString(JIPipeProgressInfo progressInfo) {
        return new InstantFuture<>(dataItemStore.getStringRepresentation());
    }

    @Override
    public Future<String> getDataAsDetailedString(JIPipeProgressInfo progressInfo) {
        return new InstantFuture<>(dataItemStore.get().toDetailedString());
    }

    @Override
    public Class<? extends JIPipeData> getDataClass() {
        return dataItemStore.getDataClass();
    }
}
