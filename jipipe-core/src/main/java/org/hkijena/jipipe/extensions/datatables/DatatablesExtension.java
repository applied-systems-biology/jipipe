package org.hkijena.jipipe.extensions.datatables;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeDataTableData;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.datatables.algorithms.ExtractTableAlgorithm;
import org.hkijena.jipipe.extensions.datatables.algorithms.MergeDataToTableAlgorithm;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class DatatablesExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Data table data";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides data types and algorithms for working with data tables");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:data-tables";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.5";
    }

    @Override
    public void register() {
        registerDatatype("jipipe:data-table",
                JIPipeDataTableData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-table.png"),
                null,
                null);
        registerNodeType("merge-data-to-table", MergeDataToTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/data-table.png"));
        registerNodeType("extract-table-to-data", ExtractTableAlgorithm.class, UIUtils.getIconURLFromResources("data-types/data.png"));
    }
}