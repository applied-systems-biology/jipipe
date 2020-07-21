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
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
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
    public StringList getDependencyCitations() {
        return new StringList();
    }

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
        registerAlgorithm("annotate-all", AnnotateAll.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerAlgorithm("annotate-remove-by-type", RemoveAnnotationByType.class, UIUtils.getIconURLFromResources("actions/entry-delete.png"));
        registerAlgorithm("annotate-remove-by-value", RemoveAnnotationByValue.class, UIUtils.getIconURLFromResources("actions/entry-delete.png"));
        registerAlgorithm("annotate-split-by-annotation", SplitByAnnotation.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerAlgorithm("data-to-annotation-table", ConvertToAnnotationTable.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));
        registerAlgorithm("annotate-with-data", AnnotateWithDataString.class, UIUtils.getIconURLFromResources("data-types/data-type.png"));
        registerAlgorithm("extract-and-replace-annotation", ExtractAndReplaceAnnotation.class, UIUtils.getIconURLFromResources("actions/edit-find-replace.png"));
        registerAlgorithm("modify-annotation-script", ModifyAnnotationScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerAlgorithm("annotate-split-by-annotation-script", SplitByAnnotationScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerAlgorithm("annotation-merge", MergeAnnotations.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerAlgorithm("annotate-with-annotation-table", AnnotateWithAnnotationTable.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));
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
