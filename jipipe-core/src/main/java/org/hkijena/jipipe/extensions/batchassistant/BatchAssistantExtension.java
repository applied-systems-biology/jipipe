package org.hkijena.jipipe.extensions.batchassistant;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaExtension.class)
public class BatchAssistantExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Data batch assistant types";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Types that are used in the data batch assistant");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:batch-assistant";
    }

    @Override
    public String getDependencyVersion() {
        return "1.70.1";
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        registerDatatype("jipipe:data-batch-status", DataBatchStatusData.class, UIUtils.getIconURLFromResources("actions/help-info.png"));
    }
}
