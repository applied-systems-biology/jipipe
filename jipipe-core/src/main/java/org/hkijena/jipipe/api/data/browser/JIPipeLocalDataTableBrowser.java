package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableInfo;
import org.hkijena.jipipe.utils.InstantFuture;

import java.io.IOException;
import java.util.concurrent.Future;

public class JIPipeLocalDataTableBrowser implements JIPipeDataTableBrowser {
    private final JIPipeDataTable dataTable;

    public JIPipeLocalDataTableBrowser(JIPipeDataTable dataTable) {
        this.dataTable = dataTable;
    }

    @Override
    public Future<JIPipeDataTable> getDataTable() {
        return new InstantFuture<>(dataTable);
    }

    @Override
    public Future<JIPipeDataTableInfo> getDataTableInfo() {
        return new InstantFuture<>(new JIPipeDataTableInfo(dataTable));
    }

    @Override
    public void close() throws IOException {

    }
}
