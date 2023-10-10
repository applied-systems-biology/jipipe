package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;

import java.nio.file.Path;
import java.util.Objects;

public class JIPipeExportedDataAnnotation {
    private String name;
    private Path rowStorageFolder;
    private String trueDataType;
    private JIPipeDataTableMetadataRow tableRow;

    public JIPipeExportedDataAnnotation() {

    }

    public JIPipeExportedDataAnnotation(String name, Path rowStorageFolder, String trueDataType, JIPipeDataTableMetadataRow tableRow) {
        this.name = name;
        this.rowStorageFolder = rowStorageFolder;
        this.trueDataType = trueDataType;
        this.tableRow = tableRow;
    }

    /**
     * This is a relative path pointing to the row storage folder of this data annotation
     * The path is relative to the storage path of the parent data table
     *
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

    /**
     * ID of the data type stored in this data annotation
     *
     * @return the data type ID
     */
    @JsonGetter("true-data-type")
    public String getTrueDataType() {
        return trueDataType;
    }

    @JsonSetter("true-data-type")
    public void setTrueDataType(String trueDataType) {
        this.trueDataType = trueDataType;
    }

    /**
     * The name of this data annotation
     *
     * @return the name
     */
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

    public JIPipeDataTableMetadataRow getTableRow() {
        return tableRow;
    }

    public void setTableRow(JIPipeDataTableMetadataRow tableRow) {
        this.tableRow = tableRow;
    }
}
