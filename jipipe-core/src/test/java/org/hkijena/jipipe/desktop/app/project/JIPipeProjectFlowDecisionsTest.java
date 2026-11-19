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

package org.hkijena.jipipe.desktop.app.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure, window-free decisions that the GUI prompts and the automation
 * project lifecycle operations share (issue #1379): the save-format resolution, the
 * open-format mappings, and the extraction/output-folder conventions. These are the
 * contracts that keep the interactive and the automated flows from forking.
 * <p>
 * The prompt dialogs themselves are verified by eye in the app (test policy: no
 * Swing interaction tests).
 */
public class JIPipeProjectFlowDecisionsTest {

    // ---- save format resolution (JIPipeProjectSaveTarget.resolveSaveTarget) ----

    @Test
    void autoHonorsTheTargetExtension() {
        JIPipeProjectSaveTarget.ResolvedTarget legacy = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project.jip"), null);
        assertFalse(legacy.modern(), "a .jip target saves in the legacy format");
        assertEquals(Path.of("/data/Project.jip"), legacy.path(), "the legacy target is kept verbatim");

        JIPipeProjectSaveTarget.ResolvedTarget modern = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project.jipx"), null);
        assertTrue(modern.modern(), "a .jipx target saves in the modern format");
        assertEquals(Path.of("/data/Project.jipx"), modern.path(), "the modern target is kept verbatim");
    }

    @Test
    void autoKeepsAnUnknownExtensionVerbatim() {
        JIPipeProjectSaveTarget.ResolvedTarget resolved = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project"), null);
        assertFalse(resolved.modern(), "an unrecognized extension keeps the unchanged interactive routing (the legacy writer)");
        assertEquals(Path.of("/data/Project"), resolved.path(), "the path is kept verbatim (the automation ops default it to .jipx themselves)");
    }

    @Test
    void modernReplacesTheLegacyExtension() {
        JIPipeProjectSaveTarget.ResolvedTarget resolved = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project.jip"), JIPipeProjectSaveTarget.Format.MODERN);
        assertTrue(resolved.modern());
        assertEquals(Path.of("/data/Project.jipx"), resolved.path(), "the legacy extension is replaced, the base name is kept");

        JIPipeProjectSaveTarget.ResolvedTarget extensionLess = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project"), JIPipeProjectSaveTarget.Format.MODERN);
        assertTrue(extensionLess.modern());
        assertEquals(Path.of("/data/Project.jipx"), extensionLess.path(), "the .jipx extension is appended");
    }

    @Test
    void legacyEnsuresTheJipExtension() {
        JIPipeProjectSaveTarget.ResolvedTarget resolved = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project"), JIPipeProjectSaveTarget.Format.LEGACY);
        assertFalse(resolved.modern());
        assertEquals(Path.of("/data/Project.jip"), resolved.path(), "the .jip extension is appended");
    }

    @Test
    void legacyKeepsAJipxTargetModern() {
        JIPipeProjectSaveTarget.ResolvedTarget resolved = JIPipeProjectSaveTarget.resolveSaveTarget(
                null, Path.of("/data/Project.jipx"), JIPipeProjectSaveTarget.Format.LEGACY);
        assertTrue(resolved.modern(), "a .jipx target cannot be forced into the legacy writer: it keeps its format");
        assertEquals(Path.of("/data/Project.jipx"), resolved.path(), "the target is kept verbatim (no double extension)");
    }

    // ---- open format mappings (JIPipeProjectFlowDecisions) ----

    @Test
    void siblingChoiceFollowsTheOpenFormat() {
        assertEquals(JIPipeProjectFlowDecisions.SiblingChoice.OPEN_CRATE,
                JIPipeProjectFlowDecisions.siblingChoiceFor(JIPipeProjectFlowDecisions.OpenFormat.AUTO),
                "auto opens the .jipx sibling (the prompt's default)");
        assertEquals(JIPipeProjectFlowDecisions.SiblingChoice.OPEN_CRATE,
                JIPipeProjectFlowDecisions.siblingChoiceFor(JIPipeProjectFlowDecisions.OpenFormat.JIPX));
        assertEquals(JIPipeProjectFlowDecisions.SiblingChoice.OPEN_LEGACY,
                JIPipeProjectFlowDecisions.siblingChoiceFor(JIPipeProjectFlowDecisions.OpenFormat.LEGACY),
                "legacy opens the legacy file");
    }

    @Test
    void crateRoutingFollowsTheCrateName() {
        Path crate = Path.of("/data/Workflow.crate.zip");
        Path file = Path.of("/data/Project.jip");

        assertTrue(JIPipeProjectFlowDecisions.routesToCrateImport(crate, JIPipeProjectFlowDecisions.OpenFormat.AUTO),
                "auto routes a .crate.zip file through the RO-Crate import (the GUI behavior)");
        assertTrue(JIPipeProjectFlowDecisions.routesToCrateImport(crate, JIPipeProjectFlowDecisions.OpenFormat.CRATE),
                "crate forces the RO-Crate import for a .crate.zip file");
        assertFalse(JIPipeProjectFlowDecisions.routesToCrateImport(crate, JIPipeProjectFlowDecisions.OpenFormat.JIPX),
                "jipx does not route a .crate.zip file through the RO-Crate import");
        assertFalse(JIPipeProjectFlowDecisions.routesToCrateImport(file, JIPipeProjectFlowDecisions.OpenFormat.CRATE),
                "crate does not route a project file through the RO-Crate import (the caller refuses it)");
        assertFalse(JIPipeProjectFlowDecisions.routesToCrateImport(file, JIPipeProjectFlowDecisions.OpenFormat.AUTO));
    }

    @Test
    void crateExtractTargetIsAUniqueDirectoryNamedAfterTheCrate(@TempDir Path tempDir) throws Exception {
        Path crate = tempDir.resolve("My Workflow.crate.zip");
        Files.createFile(crate);

        Path target = JIPipeProjectFlowDecisions.crateExtractTarget(crate, tempDir);
        assertEquals(tempDir.resolve("My Workflow.crate"), target,
                "the target is named after the crate file (the .zip suffix stripped)");

        Files.createDirectory(target);
        Path second = JIPipeProjectFlowDecisions.crateExtractTarget(crate, tempDir);
        assertFalse(Files.exists(second), "the second target does not exist yet");
        assertEquals(tempDir.resolve("My Workflow.crate-1"), second, "the unique suffix follows the makeUniqueString convention");
    }

    @Test
    void outputFolderResolutionPrefersTheLegacyName(@TempDir Path tempDir) throws Exception {
        Path withJip = JIPipeProjectFlowDecisions.uniqueSubDirectory(tempDir, "with-jip");
        Files.createDirectory(withJip);
        Files.createFile(withJip.resolve("project.jip"));
        assertEquals(withJip.resolve("project.jip"),
                JIPipeProjectFlowDecisions.resolveOutputFolderProjectFile(withJip),
                "project.jip wins when both conventions exist (the open flow tries the legacy name first)");

        Path withJipx = JIPipeProjectFlowDecisions.uniqueSubDirectory(tempDir, "with-jipx");
        Files.createDirectory(withJipx);
        Files.createFile(withJipx.resolve("project.jipx"));
        assertEquals(withJipx.resolve("project.jipx"),
                JIPipeProjectFlowDecisions.resolveOutputFolderProjectFile(withJipx),
                "project.jipx is used when no legacy file exists");

        Path withNeither = JIPipeProjectFlowDecisions.uniqueSubDirectory(tempDir, "with-neither");
        Files.createDirectory(withNeither);
        assertEquals(withNeither.resolve("project.jip"),
                JIPipeProjectFlowDecisions.resolveOutputFolderProjectFile(withNeither),
                "the legacy name is the fallthrough (the load reports it as missing)");
    }

    // ---- template data mode (JIPipeProjectFlowDecisions.decideTemplateDataMode) ----

    @Test
    void templateDataModeHasNoEffectWithoutZipData() {
        assertEquals(JIPipeProjectFlowDecisions.TemplateDataMode.PIPELINE_ONLY,
                JIPipeProjectFlowDecisions.decideTemplateDataMode(false, JIPipeProjectFlowDecisions.TemplateDataMode.WITH_DATA),
                "a template without zipped data always loads the pipeline alone");
        assertEquals(JIPipeProjectFlowDecisions.TemplateDataMode.PIPELINE_ONLY,
                JIPipeProjectFlowDecisions.decideTemplateDataMode(false, null));
    }

    @Test
    void templateDataModeHonorsTheExplicitChoice() {
        assertEquals(JIPipeProjectFlowDecisions.TemplateDataMode.WITH_DATA,
                JIPipeProjectFlowDecisions.decideTemplateDataMode(true, JIPipeProjectFlowDecisions.TemplateDataMode.WITH_DATA));
        assertEquals(JIPipeProjectFlowDecisions.TemplateDataMode.PIPELINE_ONLY,
                JIPipeProjectFlowDecisions.decideTemplateDataMode(true, JIPipeProjectFlowDecisions.TemplateDataMode.PIPELINE_ONLY));
        assertEquals(JIPipeProjectFlowDecisions.TemplateDataMode.CANCELLED,
                JIPipeProjectFlowDecisions.decideTemplateDataMode(true, null),
                "a dismissed prompt cancels the load");
    }
}
