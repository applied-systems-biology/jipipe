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

package org.hkijena.jipipe.plugins.filesystem.algorithms.local;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMutableIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ImportDataTableArchive}: the internal path parameter
 * must make data tables inside archive subfolders importable, and a
 * missing data-table.json must produce an error that lets the user fix
 * the archive access themselves (issue #1350).
 */
class ImportDataTableArchiveTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    /**
     * Exports a one-row data table (one FileData row pointing at a payload
     * file) into a directory in the standardized data table layout
     * (data-table.json + row folders) and packs it into a ZIP archive.
     *
     * @param workDir       target directory
     * @param subFolderName if non-null, all ZIP entries are placed inside a
     *                      folder of this name (archives that wrap the export
     *                      into a directory); if null, entries are placed at
     *                      the ZIP root
     * @return path of the generated ZIP archive
     */
    private static Path createTableArchive(Path workDir, String subFolderName) throws IOException {
        Path payload = workDir.resolve("source-payload.txt");
        Files.writeString(payload, "payload");

        Path tableDir = Files.createDirectories(workDir.resolve("table-export"));
        JIPipeDataTable table = new JIPipeDataTable(FileData.class);
        table.addData(new FileData(payload), new JIPipeProgressInfo());
        table.exportData(new JIPipeFileSystemWriteDataStorage(JIPipeProgressInfo.SILENT, tableDir), JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(tableDir.resolve("data-table.json")), "fixture must contain data-table.json");

        Path zip = workDir.resolve("table-archive.zip");
        try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(zip))) {
            List<Path> children = Files.walk(tableDir).filter(p -> !p.equals(tableDir)).sorted().toList();
            for (Path child : children) {
                String entryName = tableDir.relativize(child).toString().replace('\\', '/');
                if (subFolderName != null) {
                    entryName = subFolderName + "/" + entryName;
                }
                if (Files.isDirectory(child)) {
                    entryName += "/";
                }
                zipStream.putNextEntry(new ZipEntry(entryName));
                if (Files.isRegularFile(child)) {
                    Files.copy(child, zipStream);
                }
                zipStream.closeEntry();
            }
        }
        return zip;
    }

    /**
     * Populates the node's Archive slot with one file and returns a manually
     * wired iteration step that references row 0.
     */
    private static JIPipeSingleIterationStep wireArchive(ImportDataTableArchive node, Path zip) {
        node.getInputSlot("Archive").addData(new FileData(zip), new JIPipeMutableDataContext(), new JIPipeProgressInfo());
        JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(node);
        iterationStep.setInputData(node.getInputSlot("Archive"), 0);
        return iterationStep;
    }

    /**
     * A data table at the ZIP root is imported without an internal path.
     */
    @Test
    void importsTableFromArchiveRootWithoutInternalPath(@TempDir Path workDir) throws IOException {
        Path zip = createTableArchive(workDir, null);
        ImportDataTableArchive node = JIPipe.createNode(ImportDataTableArchive.class);
        node.runIteration(wireArchive(node, zip), new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());
        assertEquals(1, node.getFirstOutputSlot().getRowCount());
        FileData output = node.getFirstOutputSlot().getData(0, FileData.class, new JIPipeProgressInfo());
        assertEquals("source-payload.txt", output.toPath().getFileName().toString());
    }

    /**
     * If the data table is inside a folder of the archive, setting the
     * internal path parameter to that folder makes the import work.
     */
    @Test
    void internalPathResolvesTableInsideSubfolder(@TempDir Path workDir) throws IOException {
        Path zip = createTableArchive(workDir, "wrapped");
        ImportDataTableArchive node = JIPipe.createNode(ImportDataTableArchive.class);
        node.setInternalPath("wrapped");
        node.runIteration(wireArchive(node, zip), new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo());
        assertEquals(1, node.getFirstOutputSlot().getRowCount());
        FileData output = node.getFirstOutputSlot().getData(0, FileData.class, new JIPipeProgressInfo());
        assertEquals("source-payload.txt", output.toPath().getFileName().toString());
    }

    /**
     * Without an internal path, a data table inside a subfolder fails with
     * an error that names the Internal path parameter and the folders that
     * contain candidate data tables, so the user can fix the setup.
     */
    @Test
    void missingDataTableJsonErrorNamesInternalPathParameter(@TempDir Path workDir) throws IOException {
        Path zip = createTableArchive(workDir, "wrapped");
        ImportDataTableArchive node = JIPipe.createNode(ImportDataTableArchive.class);
        JIPipeValidationRuntimeException exception = assertThrows(JIPipeValidationRuntimeException.class,
                () -> node.runIteration(wireArchive(node, zip), new JIPipeMutableIterationContext(0, 1), new JIPipeGraphNodeRunContext(), new JIPipeProgressInfo()));
        boolean mentionsParameter = false;
        boolean mentionsFolder = false;
        for (JIPipeValidationReportEntry entry : exception.getReport()) {
            String text = entry.getTitle() + " " + entry.getExplanation() + " " + entry.getSolution();
            mentionsParameter |= text.contains("Internal path");
            mentionsFolder |= text.contains("wrapped");
        }
        assertTrue(mentionsParameter, "the error must name the Internal path parameter");
        assertTrue(mentionsFolder, "the error must list the folder that contains the data table");
    }
}
