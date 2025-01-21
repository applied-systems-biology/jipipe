package org.hkijena.jipipe.api.data.browser;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
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

    public JIPipeDataTable getLocalDataTable() {
        return dataTable;
    }

    public JIPipeDataTableInfo getLocalDataTableInfo() {
        return new JIPipeDataTableInfo(dataTable);
    }

    @Override
    public Future<JIPipeDataTable> getDataTable(JIPipeProgressInfo progressInfo) {
        return new InstantFuture<>(dataTable);
    }

    @Override
    public JIPipeDataBrowser browse(int row, String dataAnnotationColumn) {
        if (dataAnnotationColumn == null) {
            if (row >= 0 && row < dataTable.getRowCount()) {
                return new JIPipeLocalDataBrowser(dataTable.getDataItemStore(row));
            } else {
                return null;
            }
        } else {
            if (row >= 0 && row < dataTable.getRowCount()) {
                JIPipeDataItemStore store = dataTable.getDataAnnotationItemStore(row, dataAnnotationColumn);
                if (store == null) {
                    return null;
                }
                return new JIPipeLocalDataBrowser(store);
            } else {
                return null;
            }
        }
    }

    @Override
    public Future<JIPipeDataTableInfo> getDataTableInfo() {
        return new InstantFuture<>(new JIPipeDataTableInfo(dataTable));
    }

    @Override
    public void close() throws IOException {

    }
}
