package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.nio.file.Path;
import java.util.Objects;

public class JIPipeExportedDataAnnotation {
    private String name;
    private Path rowStorageFolder;
    private String trueDataType;
    private JIPipeExportedDataTableRow tableRow;

    /**
     * This is a relative path pointing to the row storage folder of this data annotation
     * The path is relative to the storage path of the parent data table
     * @return the row storage path
     */
    @JsonGetter("row-storage-folder")
    public Path getRowStorageFolder() {
        return rowStorageFolder;
    }

    @JsonSetter("row-storage-folder")
    public void setRowStorageFolder(Path rowStorageFolder) {
        this.rowStorageFolder = rowStorageFolder;
    }

    @JsonGetter("true-data-type")
    public String getTrueDataType() {
        return trueDataType;
    }

    @JsonSetter("true-data-type")
    public void setTrueDataType(String trueDataType) {
        this.trueDataType = trueDataType;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    public boolean nameEquals(JIPipeExportedDataAnnotation annotation) {
        return Objects.equals(getName(), annotation.getName());
    }

    public boolean nameEquals(String name) {
        return Objects.equals(getName(), name);
    }

    public JIPipeExportedDataTableRow getTableRow() {
        return tableRow;
    }

    public void setTableRow(JIPipeExportedDataTableRow tableRow) {
        this.tableRow = tableRow;
    }
}
