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

package org.hkijena.jipipe.plugins.statistics;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.plugins.statistics.ui.ShowStatisticsTool;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class StatisticsPlugin extends JIPipePrepackagedDefaultJavaPlugin {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Statistics";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Usage statistics collection and reporting");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:statistics";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerEnumParameterType("statistics-privacy-level",
                StatisticsPrivacyLevel.class,
                "Statistics privacy level",
                "Controls what usage statistics JIPipe collects and sends");

        registerApplicationSettingsSheet(new JIPipeStatisticsApplicationSettings());

        registerMenuExtension(ShowStatisticsTool.class);

        // Register statistics items
        var statsService = JIPipe.getInstance().getStatistics();
        var registry = statsService.getRegistry();

        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.MachineIdStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.OperatingSystemStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.TotalRamStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.JIPipeVersionStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.GpuInfoStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RecentProjectsCountStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.WorkflowRunsStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RoCratesCreatedStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.PopularNodesStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectNodesStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectCompartmentsStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NodeMoveDistanceStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NoodleScoreStatisticsItem());
        registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LongestNodeWidthStatisticsItem());
    }
}
