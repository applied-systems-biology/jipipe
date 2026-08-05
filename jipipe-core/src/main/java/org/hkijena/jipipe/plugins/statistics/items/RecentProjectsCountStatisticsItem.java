package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class RecentProjectsCountStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "recent-projects-count"; }
    @Override
    public String getName() { return "Recent projects"; }
    @Override
    public String getDescription() { return "Number of projects in the recent projects list"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
    @Override
    public JsonNode serialize() {
        int count = JIPipe.getInstance().getRecentProjects().getRecentProjects().size();
        return IntNode.valueOf(count);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
