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

package org.hkijena.jipipe.plugins.annotation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.annotation.algorithms.*;
import org.hkijena.jipipe.plugins.annotation.datasources.AnnotationTableFromFile;
import org.hkijena.jipipe.plugins.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.plugins.tables.display.OpenResultsTableInImageJDataDisplayOperation;
import org.hkijena.jipipe.plugins.tables.display.OpenResultsTableInJIPipeTabDataDisplayOperation;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides data types and algorithms to modify data annotations
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class AnnotationsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(AnnotationsPlugin.class, "org/hkijena/jipipe/plugins/annotation");

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:annotations",
            JIPipe.getJIPipeVersion(),
            "Annotation data types and algorithms");

    public AnnotationsPlugin() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_ANNOTATION);
    }

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
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerDataTypes();
        registerAlgorithms();
    }

    private void registerDataTypes() {
        registerDatatype("annotation-table",
                AnnotationTableData.class,
                JIPipe.RESOURCES.getIcon16URL("data-types/annotation-table.png"),
                new OpenResultsTableInImageJDataDisplayOperation(),
                new OpenResultsTableInJIPipeTabDataDisplayOperation(),
                new OpenInNativeApplicationDataImportOperation(".csv"));
        registerDatatypeConversion(new ImplicitResultsTableDataConverter());
    }

    private void registerAlgorithms() {
        registerEnumParameterType("simplify-annotations:removal-mode",
                SimplifyAnnotationsAlgorithm.AnnotationRemovalMode.class,
                "Combined annotation action",
                "Determines how combined annotations are processed.");
        registerParameterType("annotate-split-by-annotation:filter", AnnotationFilterExpression.class, JIPipeParameterArchetype.Value, "Annotation filter", "A filter expression");

        registerNodeType("merge-slots", MergeDataSlots.class);
        registerNodeType("annotation-table-from-file", AnnotationTableFromFile.class);
        registerNodeType("annotate-set", AnnotateByExpression.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("annotate-set-single", SetSingleAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("annotate-remove", RemoveAnnotationAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/entry-delete.png"));
        registerNodeType("annotate-split-by-annotation", SplitByAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/split.png"));
        registerNodeType("annotate-filter-by-annotation", FilterByAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("annotate-filter-by-annotation-if-else", FilterByAnnotationIfElse.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("data-to-annotation-table", ConvertToAnnotationTable.class, JIPipe.RESOURCES.getIcon16URL("data-types/annotation-table.png"));
        registerNodeType("annotate-with-data-string", AnnotateWithDataString.class, JIPipe.RESOURCES.getIcon16URL("data-types/data-type.png"));
        registerNodeType("extract-and-replace-annotation", ExtractAndReplaceAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/edit-find-replace.png"));
        registerNodeType("modify-annotation-script", ModifyAnnotationScript.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("annotate-split-by-annotation-script", SplitByAnnotationScript.class, JIPipe.RESOURCES.getIcon16URL("apps/python.png"));
        registerNodeType("annotation-merge", MergeAnnotations.class, JIPipe.RESOURCES.getIcon16URL("actions/merge.png"));
        registerNodeType("annotate-with-annotation-table", AnnotateWithAnnotationTable.class, JIPipe.RESOURCES.getIcon16URL("data-types/annotation-table.png"));
        registerNodeType("generate-unique-annotation", GenerateUniqueAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/tools-wizard.png"));
        registerNodeType("generate-unique-random-annotation", GenerateRandomUniqueAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/random.png"));
        registerNodeType("remove-array-annotations", RemoveArrayAnnotationsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("remove-na-annotation-columns", RemoveNAAnnotationColumnsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/filter.png"));
        registerNodeType("annotate-with-data", AnnotateWithData.class, JIPipe.RESOURCES.getIcon16URL("actions/database.png"));
        registerNodeType("remove-data-annotations", RemoveDataAnnotations.class, JIPipe.RESOURCES.getIcon16URL("actions/entry-delete.png"));
        registerNodeType("extract-data-annotation", ExtractDataAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/archive-extract.png"));
        registerNodeType("convert-data-annotation-to-string-annotation", DataAnnotationToStringAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/tag.png"));
        registerNodeType("rename-annotation", RenameAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/edit-find-replace.png"));
        registerNodeType("rename-data-annotation", RenameDataAnnotation.class, JIPipe.RESOURCES.getIcon16URL("actions/edit-find-replace.png"));
        registerNodeType("annotate-with-source-slot", AnnotateWithSourceSlot.class, JIPipe.RESOURCES.getIcon16URL("actions/distribute-graph-directed.png"));
        registerNodeType("simplify-annotations", SimplifyAnnotationsAlgorithm.class, JIPipe.RESOURCES.getIcon16URL("actions/image-auto-adjust.png"));
        registerNodeType("overwrite-annotations", OverwriteAnnotations.class, JIPipe.RESOURCES.getIcon16URL("actions/editcopy.png"));
        registerNodeType("annotate-by-project-paths", AnnotateByProjectPaths.class, JIPipe.RESOURCES.getIcon16URL("actions/stock_folder-copy.png"));

        registerNodeExamplesFromResources(RESOURCES, "examples");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:annotations";
    }
}
