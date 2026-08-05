package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.VersionUtils;

public class JIPipeVersionStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "jipipe-version"; }
    @Override
    public String getName() { return "JIPipe version"; }
    @Override
    public String getDescription() { return "The JIPipe version string"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        return TextNode.valueOf(VersionUtils.getJIPipeVersion());
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }

    @Override
    public String getIcon32() { return "actions/help-about.png"; }
}
