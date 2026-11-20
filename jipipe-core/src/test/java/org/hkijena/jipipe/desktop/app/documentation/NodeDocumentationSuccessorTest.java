package org.hkijena.jipipe.desktop.app.documentation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeDocumentationSuccessorTest {

    @BeforeAll
    static void init() {
        JIPipe.ensureInstance();
    }

    @Test
    void deprecatedNodeWithSuccessorRendersUseThatInstead() {
        List<String> ids = List.copyOf(JIPipe.getNodes().getRegisteredNodeInfos().keySet());
        String targetId = ids.get(0);
        String targetName = JIPipe.getNodes().getRegisteredNodeInfos().get(targetId).getName();

        JIPipeJavaNodeInfo source = new JIPipeJavaNodeInfo("kdb-test:deprecated-with-successor",
                org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode.class);
        source.setDeprecated(true);
        source.setSuccessorNodeIds(List.of(targetId));
        source.setSuccessorReason("Test reason");

        String text = JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation(source).getMarkdown();
        assertTrue(text.contains("**Deprecated.**"), "missing the deprecated marker: " + text);
        assertTrue(text.contains(targetName), "missing the successor display name: " + text);
        assertTrue(text.contains("Test reason"), "missing the successor reason: " + text);
    }

    @Test
    void deprecatedNodeWithoutSuccessorRendersOnlyTheReason() {
        JIPipeJavaNodeInfo source = new JIPipeJavaNodeInfo("kdb-test:deprecated-no-successor",
                org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode.class);
        source.setDeprecated(true);
        source.setSuccessorReason("Internal helper");

        String text = JIPipeDesktopAlgorithmCompendiumUI.buildAlgorithmDocumentation(source).getMarkdown();
        assertTrue(text.contains("**Deprecated.**"), "missing the deprecated marker: " + text);
        assertTrue(text.contains("Internal helper"), "missing the reason: " + text);
        assertFalse(text.contains("instead"), "must not claim a successor exists: " + text);
    }
}
