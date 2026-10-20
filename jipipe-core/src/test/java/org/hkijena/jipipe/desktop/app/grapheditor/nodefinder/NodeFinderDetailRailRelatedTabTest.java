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

package org.hkijena.jipipe.desktop.app.grapheditor.nodefinder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodeByInfoDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.relations.NodeRelations;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The merged "Related" tab contract of the node finder's detail rail: the
 * three former relation tabs (Similar / Previous steps / Next steps) are ONE
 * tab hosting three collapsible card-list sections, and the pill row keeps
 * the AI monitor's padding (the border is set before the tabs are added —
 * {@code JIPipeDesktopTabPaneBorderTest} covers the propagation itself).
 */
class NodeFinderDetailRailRelatedTabTest {

    private static final String STUB_ID = "finder-rail-test:stub-node";

    private static NodeFinderDetailRail rail;
    private static JIPipeNodeDatabaseEntry entry;

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
            }
            JIPipe.ensureInstance();
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT/JIPipe not available: " + t);
        }
        // A namespaced stub node so the entry can be built from the live
        // registry (the jipipe-core test JVM does not load all plugin nodes).
        if (!JIPipe.getNodes().hasNodeInfoWithId(STUB_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(STUB_ID, StubNode.class),
                new JIPipeMutableDependency("finder-rail-test", "1.0.0", "finder-rail-test"));
        }
        entry = new CreateNewNodeByInfoDatabaseEntry(STUB_ID, JIPipe.getNodes().getInfoById(STUB_ID));

        // A stub database with fixed relations: similar EMPTY, previous and
        // next non-empty — so one section must collapse and two expand.
        JIPipeNodeDatabase db = new JIPipeNodeDatabase() {
            @Override
            public NodeRelations relations(JIPipeNodeDatabaseEntry e, int maxPerList) {
                return new NodeRelations(List.of(), List.of(entry), List.of(entry, entry));
            }
        };
        rail = new NodeFinderDetailRail(db, e -> {
        });
        rail.setEntry(entry);
    }

    @Test
    void theRelationTabsAreMergedIntoOneRelatedTab() {
        JIPipeDesktopTabPane tabPane = rail.tabPaneForTest();
        List<String> titles = new ArrayList<>();
        tabPane.getTabs().forEach(tab -> titles.add(tab.getTitle()));
        assertThat(titles)
            .as("Documentation + Related only — no Similar/Previous/Next tabs")
            .containsExactly("Documentation", "Related");
    }

    @Test
    void theRelatedTabHostsTheThreeRelationSections() {
        JIPipeDesktopTabPane tabPane = rail.tabPaneForTest();
        Component related = relatedTabContent(tabPane);
        assertThat(related).isNotNull();

        // The three section card lists (in page order) all live in the ONE
        // Related tab — the former three tabs are gone.
        JList<JIPipeNodeDatabaseEntry> similar = rail.similarListForTest();
        JList<JIPipeNodeDatabaseEntry> previous = rail.previousListForTest();
        JList<JIPipeNodeDatabaseEntry> next = rail.nextListForTest();
        for (JList<?> list : new JList[]{similar, previous, next}) {
            assertThat(ancestorOf(list, related))
                .as("the %s card list is inside the Related tab", list == similar ? "similar"
                    : list == previous ? "previous" : "next")
                .isTrue();
        }

        // The lists hold the relation entries
        assertThat(similar.getModel().getSize()).isZero();
        assertThat(previous.getModel().getSize()).isEqualTo(1);
        assertThat(next.getModel().getSize()).isEqualTo(2);

        // Content state follows the relations: the empty Similar section is
        // collapsed (its list host hidden), the non-empty Previous/Next
        // sections are expanded.
        assertThat(similar.getParent()).isNotNull();
        assertThat(similar.getParent().isVisible())
            .as("the empty Similar section is collapsed").isFalse();
        assertThat(previous.getParent().isVisible())
            .as("the non-empty Previous steps section is expanded").isTrue();
        assertThat(next.getParent().isVisible())
            .as("the non-empty Next steps section is expanded").isTrue();
    }

    @Test
    void thePillsCarryTheAiMonitorPadding() {
        JIPipeDesktopTabPane tabPane = rail.tabPaneForTest();
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            Component pill = tabPane.getTabbedPane().getTabComponentAt(i);
            assertThat(pill).isInstanceOf(JComponent.class);
            Border border = ((JComponent) pill).getBorder();
            assertThat(border).isInstanceOf(EmptyBorder.class);
            // The intended pill padding (12,4,12,4 — the AI monitor's
            // setTabPanelBorder), NOT the thin default (4,0,4,0) the
            // historical call-order bug produced (no horizontal padding).
            Insets insets = border.getBorderInsets(pill);
            assertThat(insets.left).as("pill %d horizontal padding", i).isGreaterThan(0);
            assertThat(insets.top).as("pill %d vertical padding", i).isGreaterThan(4);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Component relatedTabContent(JIPipeDesktopTabPane tabPane) {
        return tabPane.getTabs().stream()
            .filter(tab -> "Related".equals(tab.getTitle()))
            .map(JIPipeDesktopTabPane.DocumentTab::getContent)
            .findFirst()
            .orElse(null);
    }

    /** True when {@code ancestor} is this component or one of its parents. */
    private static boolean ancestorOf(Component component, Component ancestor) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (c == ancestor) {
                return true;
            }
        }
        return false;
    }

    /** A minimal node type for the rail's test entries (same pattern as the
     * node-browser tree controller test). */
    public static class StubNode extends JIPipeGraphNode {
        public StubNode(JIPipeNodeInfo info) {
            super(info);
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }

        @Override
        public void reportValidity(JIPipeValidationReportContext reportContext,
                                   JIPipeValidationReportSettings reportSettings,
                                   JIPipeValidationReport report,
                                   JIPipeProgressInfo progressInfo) {
        }
    }
}
