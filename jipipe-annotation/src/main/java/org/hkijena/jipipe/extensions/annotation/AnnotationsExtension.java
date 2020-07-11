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

package org.hkijena.jipipe.extensions.annotation;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.annotation.algorithms.*;
import org.hkijena.jipipe.extensions.annotation.datasources.AnnotationTableFromFile;
import org.hkijena.jipipe.extensions.tables.ResultsTableDataSlotRowUI;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides data types and algorithms to modify data annotations
 */
@Plugin(type = JIPipeJavaExtension.class)
public class AnnotationsExtension extends JIPipePrepackagedDefaultJavaExtension {
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
        registerDatatypeConversion(new ImplicitResultsTableDataConverter());
    }

    private void registerAlgorithms() {
        registerAlgorithm("merge-slots", MergeDataSlots.class);
        registerAlgorithm("annotation-table-from-file", AnnotationTableFromFile.class);
        registerAlgorithm("annotate-all", AnnotateAll.class, UIUtils.getAlgorithmIconURL("tags.png"));
        registerAlgorithm("annotate-remove-by-type", RemoveAnnotationByType.class, UIUtils.getAlgorithmIconURL("trash.png"));
        registerAlgorithm("annotate-remove-by-value", RemoveAnnotationByValue.class, UIUtils.getAlgorithmIconURL("trash.png"));
        registerAlgorithm("annotate-split-by-annotation", SplitByAnnotation.class, UIUtils.getAlgorithmIconURL("split.png"));
        registerAlgorithm("data-to-annotation-table", ConvertToAnnotationTable.class, UIUtils.getAlgorithmIconURL("annotation-table.png"));
        registerAlgorithm("annotate-with-data", AnnotateWithDataString.class, UIUtils.getAlgorithmIconURL("data-type.png"));
        registerAlgorithm("extract-and-replace-annotation", ExtractAndReplaceAnnotation.class, UIUtils.getAlgorithmIconURL("edit-find-replace.png"));
        registerAlgorithm("modify-annotation-script", ModifyAnnotationScript.class, UIUtils.getAlgorithmIconURL("python.png"));
        registerAlgorithm("annotate-split-by-annotation-script", SplitByAnnotationScript.class, UIUtils.getAlgorithmIconURL("python.png"));
        registerAlgorithm("collect-data", CollectDataAlgorithm.class, UIUtils.getAlgorithmIconURL("document-export.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:annotations";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
