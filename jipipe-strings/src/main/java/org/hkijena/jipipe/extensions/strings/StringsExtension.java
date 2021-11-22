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

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.core.data.OpenInNativeApplicationDataImportOperation;
import org.hkijena.jipipe.extensions.core.data.OpenTextInJIPipeDataOperation;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.strings.datasources.StringDefinitionDataSource;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

/**
 * Provides data types dor handling strings
 */
@Plugin(type = JIPipeJavaExtension.class)
public class StringsExtension extends JIPipePrepackagedDefaultJavaExtension {
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
    public void register() {
        registerDatatype("string", StringData.class, UIUtils.getIconURLFromResources("data-types/string.png"), null, null,
                new OpenInNativeApplicationDataImportOperation(".txt"), new OpenTextInJIPipeDataOperation(".txt"));
        registerDatatype("xml", XMLData.class, UIUtils.getIconURLFromResources("data-types/xml.png"), null, null,
                new OpenInNativeApplicationDataImportOperation(".xml"), new OpenTextInJIPipeDataOperation(".xml"));
        registerDatatype("json", JsonData.class, UIUtils.getIconURLFromResources("data-types/json.png"), null, null,
                new OpenInNativeApplicationDataImportOperation(".json"), new OpenTextInJIPipeDataOperation(".json"));
        registerDatatypeConversion(new StringDataConverter(XMLData.class));
        registerDatatypeConversion(new StringDataConverter(JsonData.class));

        registerNodeType("define-string", StringDefinitionDataSource.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:strings";
    }

    @Override
    public String getDependencyVersion() {
        return "1.50.0";
    }
}
