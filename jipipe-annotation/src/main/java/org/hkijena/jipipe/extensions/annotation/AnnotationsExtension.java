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
import org.hkijena.jipipe.extensions.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.extensions.tables.display.OpenResultsTableInImageJDataOperation;
import org.hkijena.jipipe.extensions.tables.display.OpenResultsTableInJIPipeTabDataOperation;
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
    public HTMLText getDescription() {
        return new HTMLText("Provides data types and algorithms to modify data annotations");
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
                null,
                null,
                new OpenResultsTableInImageJDataOperation(),
                new OpenResultsTableInJIPipeTabDataOperation(),
                new OpenInNativeApplicationDataImportOperation(".csv"));
        registerDatatypeConversion(new ImplicitResultsTableDataConverter());
    }

    private void registerAlgorithms() {
        registerNodeType("merge-slots", MergeDataSlots.class);
        registerNodeType("annotation-table-from-file", AnnotationTableFromFile.class);
        registerNodeType("annotate-set", AnnotateByExpression.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("annotate-set-single", SetSingleAnnotation.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("annotate-remove", RemoveAnnotationAlgorithm.class, UIUtils.getIconURLFromResources("actions/entry-delete.png"));
        registerNodeType("annotate-split-by-annotation", SplitByAnnotation.class, UIUtils.getIconURLFromResources("actions/split.png"));
        registerNodeType("data-to-annotation-table", ConvertToAnnotationTable.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));
        registerNodeType("annotate-with-data-string", AnnotateWithDataString.class, UIUtils.getIconURLFromResources("data-types/data-type.png"));
        registerNodeType("extract-and-replace-annotation", ExtractAndReplaceAnnotation.class, UIUtils.getIconURLFromResources("actions/edit-find-replace.png"));
        registerNodeType("modify-annotation-script", ModifyAnnotationScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("annotate-split-by-annotation-script", SplitByAnnotationScript.class, UIUtils.getIconURLFromResources("apps/python.png"));
        registerNodeType("annotation-merge", MergeAnnotations.class, UIUtils.getIconURLFromResources("actions/merge.png"));
        registerNodeType("annotate-with-annotation-table", AnnotateWithAnnotationTable.class, UIUtils.getIconURLFromResources("data-types/annotation-table.png"));
        registerNodeType("generate-unique-annotation", GenerateUniqueAnnotation.class, UIUtils.getIconURLFromResources("actions/tools-wizard.png"));
        registerNodeType("generate-unique-random-annotation", GenerateRandomUniqueAnnotation.class, UIUtils.getIconURLFromResources("actions/random.png"));
        registerNodeType("remove-array-annotations", RemoveArrayAnnotationsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("remove-na-annotation-columns", RemoveNAAnnotationColumnsAlgorithm.class, UIUtils.getIconURLFromResources("actions/filter.png"));
        registerNodeType("annotate-with-data", AnnotateWithData.class, UIUtils.getIconURLFromResources("actions/database.png"));
        registerNodeType("remove-data-annotations", RemoveDataAnnotations.class, UIUtils.getIconURLFromResources("actions/entry-delete.png"));
        registerNodeType("extract-data-annotation", ExtractDataAnnotation.class, UIUtils.getIconURLFromResources("actions/archive-extract.png"));
        registerNodeType("convert-data-annotation-to-string-annotation", DataAnnotationToStringAnnotation.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("rename-annotation", RenameAnnotation.class, UIUtils.getIconURLFromResources("actions/edit-find-replace.png"));
        registerNodeType("rename-data-annotation", RenameDataAnnotation.class, UIUtils.getIconURLFromResources("actions/edit-find-replace.png"));
        registerNodeType("annotate-with-source-slot", AnnotateWithSourceSlot.class, UIUtils.getIconURLFromResources("actions/distribute-graph-directed.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:annotations";
    }

    @Override
    public String getDependencyVersion() {
        return "1.52.1";
    }
}
