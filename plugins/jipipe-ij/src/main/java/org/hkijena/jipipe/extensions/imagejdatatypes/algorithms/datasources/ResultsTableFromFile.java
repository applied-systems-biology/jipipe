/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.datasources;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.*;

/**
 * Imports {@link ResultsTableData} from a file
 */
@SetJIPipeDocumentation(name = "Import results table", description = "Imports results tables from a file. Following formats are supported: " +
        "<ul>" +
        "<li>CSV</li>" +
        "<li>XLSX</li>" +
        "</ul>")
@AddJIPipeInputSlot(value = FileData.class, slotName = "Files", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Results table", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File", aliasName = "Open (CSV/XLSX)")
public class ResultsTableFromFile extends JIPipeSimpleIteratingAlgorithm {

    private FileFormat fileFormat = FileFormat.Auto;
    private StringList sheets = new StringList();

    private boolean ignoreMissingSheets = true;
    private OptionalTextAnnotationNameParameter sheetNameAnnotation = new OptionalTextAnnotationNameParameter("Sheet", true);

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
        this.sheetNameAnnotation = new OptionalTextAnnotationNameParameter(other.sheetNameAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        FileData fileData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
        FileFormat format = fileFormat;
        if (format == FileFormat.Auto) {
            if (UIUtils.EXTENSION_FILTER_CSV.accept(fileData.toPath().toFile())) {
                format = FileFormat.CSV;
            } else if (UIUtils.EXTENSION_FILTER_TSV.accept(fileData.toPath().toFile())) {
                format = FileFormat.XLSX;
            } else if (UIUtils.EXTENSION_FILTER_XLSX.accept(fileData.toPath().toFile())) {
                format = FileFormat.XLSX;
            } else {
                throw new UnsupportedOperationException("Unknown file format: " + fileData.getPath());
            }
        }
        switch (format) {
            case CSV: {
                ResultsTableData tableData = ResultsTableData.fromCSV(fileData.toPath());
                iterationStep.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
            }
            break;
            case TSV: {
                ResultsTableData tableData = ResultsTableData.fromCSV(fileData.toPath(), "\t");
                iterationStep.addOutputData(getFirstOutputSlot(), tableData, progressInfo);
            }
            break;
            case XLSX: {
                Map<String, ResultsTableData> map = ResultsTableData.fromXLSX(fileData.toPath());
                Set<String> importedSheetNames = new HashSet<>();
                if (sheets.isEmpty())
                    importedSheetNames.addAll(map.keySet());
                else
                    importedSheetNames.addAll(sheets);
                for (String sheetName : importedSheetNames) {
                    ResultsTableData data = map.getOrDefault(sheetName, null);
                    if (data == null && !ignoreMissingSheets) {
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                                "Unable to find sheet '" + sheetName + "' in " + String.join(", ", map.keySet()),
                                "Tried to import Excel sheet '" + sheetName + "', but it is not there.",
                                "Please check if the sheet exists. You can also ignore missing sheets."));
                    }
                    if (data == null)
                        continue;
                    List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
                    sheetNameAnnotation.addAnnotationIfEnabled(annotationList, sheetName);
                    iterationStep.addOutputData(getFirstOutputSlot(), data, annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
                }
            }
            break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @SetJIPipeDocumentation(name = "File format", description = "Allows to set the file format. If set to 'Auto', the format is detected from the file extension." +
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

    @SetJIPipeDocumentation(name = "Restrict to sheets", description = "If the file contains multiple tables, specifies which sheets should be imported. <strong>If empty, all sheets are imported</strong>")
    @JIPipeParameter("sheets")
    public StringList getSheets() {
        return sheets;
    }

    @JIPipeParameter("sheets")
    public void setSheets(StringList sheets) {
        this.sheets = sheets;
    }

    @SetJIPipeDocumentation(name = "Annotate with sheet name", description = "If enabled, the tables are annotated with the sheet names. No annotation is generated for file formats without sheets (e.g., CSV).")
    @JIPipeParameter("sheet-name-annotation")
    public OptionalTextAnnotationNameParameter getSheetNameAnnotation() {
        return sheetNameAnnotation;
    }

    @JIPipeParameter("sheet-name-annotation")
    public void setSheetNameAnnotation(OptionalTextAnnotationNameParameter sheetNameAnnotation) {
        this.sheetNameAnnotation = sheetNameAnnotation;
    }

    @SetJIPipeDocumentation(name = "Ignore missing sheets", description = "If enabled, missing sheets are ignored. Otherwise an error is raised if a sheet is missing.")
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
