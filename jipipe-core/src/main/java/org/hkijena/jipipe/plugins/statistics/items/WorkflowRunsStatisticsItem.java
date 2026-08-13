package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class WorkflowRunsStatisticsItem implements JIPipeStatisticsItem {
    private int count = 0;
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "workflow-runs"; }
    @Override
    public String getName() { return "Workflow runs"; }
    @Override
    public String getDescription() { return "Number of times the user ran a workflow"; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
    @Override
    public JsonNode serialize() { return IntNode.valueOf(count); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) count = node.asInt(); }
    @Override
    public void reset() { count = 0; }
    @Override
    public boolean isTimeTracked() { return true; }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this::onRunFinished);
    }

    private void onRunFinished(JIPipeRunnable.FinishedEvent event) {
        if ((event.getRun() instanceof JIPipeGraphRun run && run.getParent() == null)
                || event.getRun() instanceof JIPipeDesktopQuickRun) {
            count++;
            if (service != null) service.saveLater();
        }
    }

    @Override
    public String getIcon32() { return "actions/debug-run.png"; }
    @Override
    public org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant getCardVariant() {
        return org.hkijena.jipipe.desktop.commons.components.cards.JIPipeDesktopCardVariant.Success;
    }
    @Override
    public int getDefaultColumnSpan() { return 6; }
}
