package org.hkijena.acaq5.extensions.annotation;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.annotation.algorithms.*;
import org.hkijena.acaq5.extensions.annotation.datasources.AnnotationTableFromFile;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.resultanalysis.ResultsTableDataSlotRowUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides data types and algorithms to modify data annotations
 */
@Plugin(type = ACAQJavaExtension.class)
public class AnnotationsExtension extends ACAQPrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Annotation data types and algorithms";
    }

    @Override
    public String getDescription() {
        return "Provides data types and algorithms to modify data annotations";
    }

    @Override
    public void register() {
        registerDataTypes();
        registerAlgorithms();
    }

    private void registerDataTypes() {
        registerDatatype("annotation-table",
                AnnotationTableData.class,
                ResourceUtils.getPluginResource("icons/data-types/annotation-table.png"),
                ResultsTableDataSlotRowUI.class,
                null);
    }

    private void registerAlgorithms() {
        registerAlgorithm("annotation-table-from-file", AnnotationTableFromFile.class);
        registerAlgorithm("annotate-all", AnnotateAll.class, UIUtils.getAlgorithmIconURL("tags.png"));
        registerAlgorithm("annotate-remove-by-type", RemoveAnnotationByType.class, UIUtils.getAlgorithmIconURL("trash.png"));
        registerAlgorithm("annotate-remove-by-value", RemoveAnnotationByValue.class, UIUtils.getAlgorithmIconURL("trash.png"));
        registerAlgorithm("annotate-split-by-annotation", SplitByAnnotation.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("data-to-annotation-table", ConvertToAnnotationTable.class, UIUtils.getAlgorithmIconURL("annotation-table.png"));
        registerAlgorithm("annotate-with-data", AnnotateWithDataString.class, UIUtils.getAlgorithmIconURL("data-type.png"));
        registerAlgorithm("extract-and-replace-annotation", ExtractAndReplaceAnnotation.class, UIUtils.getAlgorithmIconURL("edit-find-replace.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:annotations";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
