/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Imports {@link ResultsTableData} from a file
 */
@JIPipeDocumentation(name = "Import results table", description = "Imports results tables from a file. Following formats are supported: " +
        "<ul>" +
        "<li>CSV</li>" +
        "<li>XLSX</li>" +
        "</ul>")
@JIPipeInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Results table", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ResultsTableFromFile extends JIPipeSimpleIteratingAlgorithm {

    private FileFormat fileFormat = FileFormat.Auto;
    private StringList sheets = new StringList();

    private boolean ignoreMissingSheets = true;
    private OptionalAnnotationNameParameter sheetNameAnnotation = new OptionalAnnotationNameParameter("Sheet", true);

    /**
     * @param info algorithm info
     */
    public ResultsTableFromFile(JIPipeNodeInfo info) {
        super(info);
    }

    public ResultsTableFromFile(ResultsTableFromFile other) {
        super(other);
        this.fileFormat = other.fileFormat;
        this.sheets = new StringList(other.sheets);
        this.ignoreMissingSheets = other.ignoreMissingSheets;
        this.sheetNameAnnotation = new OptionalAnnotationNameParameter(other.sheetNameAnnotation);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FileData fileData = dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        FileFormat format = fileFormat;
        if(format == FileFormat.Auto) {
            if(UIUtils.EXTENSION_FILTER_CSV.accept(fileData.toPath().toFile())) {
                format = FileFormat.CSV;
            }
            else if(UIUtils.EXTENSION_FILTER_TSV.accept(fileData.toPath().toFile())) {
                format = FileFormat.XLSX;
            }
            else if(UIUtils.EXTENSION_FILTER_XLSX.accept(fileData.toPath().toFile())) {
                format = FileFormat.XLSX;
            }
            else {
                throw new UnsupportedOperationException("Unknown file format: " + fileData.getPath());
            }
        }
        switch (format) {
            case CSV: {
                ResultsTableData tableData = ResultsTableData.fromCSV(fileData.toPath());
                dataBatch.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
            }
            break;
            case TSV: {
                ResultsTableData tableData = ResultsTableData.fromCSV(fileData.toPath(), "\t");
                dataBatch.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
            }
            break;
            case XLSX: {
                Map<String, ResultsTableData> map = ResultsTableData.fromXLSX(fileData.toPath());
                Set<String> importedSheetNames = new HashSet<>();
                if(sheets.isEmpty())
                    importedSheetNames.addAll(map.keySet());
                else
                    importedSheetNames.addAll(sheets);
                for (String sheetName : importedSheetNames) {
                    ResultsTableData data = map.getOrDefault(sheetName, null);
                    if(data == null && !ignoreMissingSheets) {
                        throw new UserFriendlyNullPointerException("Unable to find sheet '" + sheetName + "' in " + String.join(", ", map.keySet()),
                                "Unable to find sheet '" + sheetName + "'",
                                getDisplayName(),
                                "Tried to import Excel sheet '" + sheetName + "', but it is not there.",
                                "Please check if the sheet exists. You can also ignore missing sheets.");
                    }
                    if(data == null)
                        continue;
                    List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
                    sheetNameAnnotation.addAnnotationIfEnabled(annotationList, sheetName);
                    dataBatch.addOutputData(getFirstOutputSlot(), data, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
                }
            }
            break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @JIPipeDocumentation(name = "File format", description = "Allows to set the file format. If set to 'Auto', the format is detected from the file extension." +
            "<ul>" +
            "<li>CSV: Comma separated values</li>" +
            "<li>TSV: Tab separated values (variant of CSV with tabs instead of commas)</li>" +
            "<li>XLSX: Excel table (please ensure that the first row is a heading and that the table begins at A1)</li>" +
            "</ul>")
    @JIPipeParameter("file-format")
    public FileFormat getFileFormat() {
        return fileFormat;
    }

    @JIPipeParameter("file-format")
    public void setFileFormat(FileFormat fileFormat) {
        this.fileFormat = fileFormat;
    }

    @JIPipeDocumentation(name = "Restrict to sheets", description = "If the file contains multiple tables, specifies which sheets should be imported. <strong>If empty, all sheets are imported</strong>")
    @JIPipeParameter("sheets")
    public StringList getSheets() {
        return sheets;
    }

    @JIPipeParameter("sheets")
    public void setSheets(StringList sheets) {
        this.sheets = sheets;
    }

    @JIPipeDocumentation(name = "Annotate with sheet name", description = "If enabled, the tables are annotated with the sheet names. No annotation is generated for file formats without sheets (e.g., CSV).")
    @JIPipeParameter("sheet-name-annotation")
    public OptionalAnnotationNameParameter getSheetNameAnnotation() {
        return sheetNameAnnotation;
    }

    @JIPipeParameter("sheet-name-annotation")
    public void setSheetNameAnnotation(OptionalAnnotationNameParameter sheetNameAnnotation) {
        this.sheetNameAnnotation = sheetNameAnnotation;
    }

    @JIPipeDocumentation(name = "Ignore missing sheets", description = "If enabled, missing sheets are ignored. Otherwise an error is raised if a sheet is missing.")
    @JIPipeParameter("ignore-missing-sheets")
    public boolean isIgnoreMissingSheets() {
        return ignoreMissingSheets;
    }

    @JIPipeParameter("ignore-missing-sheets")
    public void setIgnoreMissingSheets(boolean ignoreMissingSheets) {
        this.ignoreMissingSheets = ignoreMissingSheets;
    }

    public enum FileFormat {
        Auto,
        CSV,
        TSV,
        XLSX
    }
}
