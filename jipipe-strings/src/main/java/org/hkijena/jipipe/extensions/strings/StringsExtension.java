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

package org.hkijena.jipipe.extensions.strings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.strings.datasources.ImportJsonAlgorithm;
import org.hkijena.jipipe.extensions.strings.datasources.ImportStringAlgorithm;
import org.hkijena.jipipe.extensions.strings.datasources.ImportXMLAlgorithm;
import org.hkijena.jipipe.extensions.strings.datasources.StringDefinitionDataSource;
import org.hkijena.jipipe.extensions.strings.nodes.json.AnnotateWithJsonDataAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.json.ExtractJsonDataAsTableAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.json.ExtractTextFromJsonAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.text.AnnotateWithTextDataAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.text.ProcessTextDataAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.text.TableToCSVTextAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.text.TextDataToTableAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.xml.AnnotateWithXPathDataAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.xml.ExtractTextFromXMLAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.xml.ExtractXPathDataAsTableAlgorithm;
import org.hkijena.jipipe.extensions.strings.nodes.xml.PrettifyXMLAlgorithm;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Provides data types dor handling strings
 */
@Plugin(type = JIPipeJavaExtension.class)
public class StringsExtension extends JIPipePrepackagedDefaultJavaExtension {

    /**
     * Dependency instance to be used for creating the set of dependencies
     */
    public static final JIPipeDependency AS_DEPENDENCY = new JIPipeMutableDependency("org.hkijena.jipipe:strings",
            JIPipe.getJIPipeVersion(),
            "Strings");

    public StringsExtension() {
        getMetadata().addCategories(PluginCategoriesEnumParameter.CATEGORY_SCRIPTING);
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Strings";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides support for string data types");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("string", StringData.class, UIUtils.getIconURLFromResources("data-types/string.png"),
                new OpenInNativeApplicationDataImportOperation(".txt"));
        registerDatatype("xml", XMLData.class, UIUtils.getIconURLFromResources("data-types/xml.png"),
                new OpenInNativeApplicationDataImportOperation(".xml"));
        registerDatatype("json", JsonData.class, UIUtils.getIconURLFromResources("data-types/json.png"),
                new OpenInNativeApplicationDataImportOperation(".json"));
        registerDatatypeConversion(new StringDataConverter(XMLData.class));
        registerDatatypeConversion(new StringDataConverter(JsonData.class));

        registerNodeType("define-string", StringDefinitionDataSource.class);
        registerNodeType("import-string-from-file", ImportStringAlgorithm.class);
        registerNodeType("import-xml-from-file", ImportXMLAlgorithm.class);
        registerNodeType("import-json-from-file", ImportJsonAlgorithm.class);

        registerNodeType("json-annotate-with-json-data", AnnotateWithJsonDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("json-extract-data-as-table-json-path", ExtractJsonDataAsTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/table.png"));
        registerNodeType("json-extract-text-from-json-path", ExtractTextFromJsonAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));

        registerNodeType("json-annotate-with-text-data", AnnotateWithTextDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("text-process-expression", ProcessTextDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("table-to-csv-text", TableToCSVTextAlgorithm.class, UIUtils.getIconURLFromResources("actions/reload.png"));
        registerNodeType("text-to-table-expression", TextDataToTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/table.png"));

        registerNodeType("xml-annotate-with-xml-data", AnnotateWithXPathDataAlgorithm.class, UIUtils.getIconURLFromResources("actions/tag.png"));
        registerNodeType("xml-extract-text-from-xpath", ExtractTextFromXMLAlgorithm.class, UIUtils.getIconURLFromResources("actions/insert-math-expression.png"));
        registerNodeType("xml-extract-data-as-table-xpath", ExtractXPathDataAsTableAlgorithm.class, UIUtils.getIconURLFromResources("actions/table.png"));
        registerNodeType("xml-prettify", PrettifyXMLAlgorithm.class, UIUtils.getIconURLFromResources("actions/format-justify-left.png"));
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:strings";
    }

}
