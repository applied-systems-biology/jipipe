package edu.kit.datamanager.ro_crate.preview;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreviewTest {

    @Test
    void staticPreviewSaveToFolderTest(@TempDir Path dir) throws IOException {
        var file1 = dir.resolve("file.html");
        FileUtils.writeStringToFile(file1.toFile(), "random html, does not need to be valid for this test", Charset.defaultCharset());

        var file2 = dir.resolve("directory");
        var fileInDir = file2.resolve("fileInDir.html");
        FileUtils.writeStringToFile(fileInDir.toFile(), "dajkdlfjdsklafj alksfjdalk fjl", Charset.defaultCharset());
        StaticPreview customPreview = new StaticPreview(file1.toFile(), file2.toFile());

        FileUtils.forceMkdir(dir.resolve("result").toFile());
        customPreview.saveAllToFolder(dir.resolve("result").toFile());

        var e = dir.resolve("result");

        var roPreview = e.resolve("ro-crate-preview.html");
        var roDir = e.resolve("ro-crate-preview_files");
        var roDirFile = roDir.resolve("fileInDir.html");
        assertTrue(Files.isRegularFile(roPreview));
        assertTrue(Files.isDirectory(roDir));
        assertTrue(Files.isRegularFile(roDirFile));

        assertTrue(FileUtils.contentEqualsIgnoreEOL(roPreview.toFile(), file1.toFile(), String.valueOf(Charset.defaultCharset())));
        assertFalse(FileUtils.contentEqualsIgnoreEOL(roPreview.toFile(), fileInDir.toFile(), String.valueOf(Charset.defaultCharset())));

        assertTrue(FileUtils.contentEqualsIgnoreEOL(roDirFile.toFile(), fileInDir.toFile(), String.valueOf(Charset.defaultCharset())));
    }

    @Test
    void staticPreviewSaveToZip(@TempDir Path dir) throws IOException {
        var file1 = dir.resolve("file.html");
        FileUtils.writeStringToFile(file1.toFile(), "random html, does not need to be valid for this test", Charset.defaultCharset());

        var file2 = dir.resolve("directory");
        var fileInDir = file2.resolve("fileInDir.html");
        FileUtils.writeStringToFile(fileInDir.toFile(), "dajkdlfjdsklafj alksfjdalk fjl", Charset.defaultCharset());
        StaticPreview customPreview = new StaticPreview(file1.toFile(), file2.toFile());

        customPreview.saveAllToZip(new ZipFile(dir.resolve("destination.zip").toFile()));
        try (ZipFile zf = new ZipFile(dir.resolve("destination.zip").toFile())) {
            zf.extractAll(dir.resolve("extracted").toAbsolutePath().toString());
        }

        var e = dir.resolve("extracted");
        var roPreview = e.resolve("ro-crate-preview.html");
        var roDir = e.resolve("ro-crate-preview_files");
        var roDirFile = roDir.resolve("fileInDir.html");
        assertTrue(Files.isRegularFile(roPreview));
        assertTrue(Files.isDirectory(roDir));
        assertTrue(Files.isRegularFile(roDirFile));

        assertTrue(FileUtils.contentEqualsIgnoreEOL(roPreview.toFile(), file1.toFile(), String.valueOf(Charset.defaultCharset())));
        assertFalse(FileUtils.contentEqualsIgnoreEOL(roPreview.toFile(), fileInDir.toFile(), String.valueOf(Charset.defaultCharset())));

        assertTrue(FileUtils.contentEqualsIgnoreEOL(roDirFile.toFile(), fileInDir.toFile(), String.valueOf(Charset.defaultCharset())));
    }

    @Test
    void staticPreviewSaveToZipStream(@TempDir Path dir) throws IOException {
        var file1 = dir.resolve("file.html");
        FileUtils.writeStringToFile(file1.toFile(), "random html, does not need to be valid for this test", Charset.defaultCharset());

        var file2 = dir.resolve("directory");
        var fileInDir = file2.resolve("fileInDir.html");
        FileUtils.writeStringToFile(fileInDir.toFile(), "dajkdlfjdsklafj alksfjdalk fjl", Charset.defaultCharset());
        StaticPreview preview = new StaticPreview(file1.toFile(), file2.toFile());

        try (ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(dir.resolve("destination.zip").toFile()))) {
            preview.saveAllToStream(
                    null,  // static preview does not need metadata
                    stream);
            stream.flush();
        }

        try (ZipFile zf = new ZipFile(dir.resolve("destination.zip").toFile())) {
            zf.extractAll(dir.resolve("extracted").toAbsolutePath().toString());
        }

        var e = dir.resolve("extracted");
        var roPreview = e.resolve("ro-crate-preview.html");
        var roDir = e.resolve("ro-crate-preview_files");
        var roDirFile = roDir.resolve("fileInDir.html");
        assertTrue(Files.isRegularFile(roPreview));
        assertTrue(Files.isDirectory(roDir));
        assertTrue(Files.isRegularFile(roDirFile));

        assertTrue(FileUtils.contentEqualsIgnoreEOL(roPreview.toFile(), file1.toFile(), String.valueOf(Charset.defaultCharset())));
        assertFalse(FileUtils.contentEqualsIgnoreEOL(roPreview.toFile(), fileInDir.toFile(), String.valueOf(Charset.defaultCharset())));

        assertTrue(FileUtils.contentEqualsIgnoreEOL(roDirFile.toFile(), fileInDir.toFile(), String.valueOf(Charset.defaultCharset())));
    }

    @Test
    void testAutomaticPreviewAddToFolder(@TempDir Path dir) throws IOException {
        AutomaticPreview automaticPreview = new AutomaticPreview();

        Path crate = dir.resolve("crate");
        // this crate will not have a json file
        FileUtils.forceMkdir(crate.toFile());
        try (InputStream crateJson = PreviewTest.class.getResourceAsStream("/crates/other/idrc_project/ro-crate-metadata.json")) {
            Assertions.assertNotNull(crateJson);
            FileUtils.copyInputStreamToFile(crateJson, crate.resolve("ro-crate-metadata.json").toFile());
        }
        automaticPreview.saveAllToFolder(crate.toFile());

        // there should be a html file generated
        assertTrue(Files.isRegularFile(crate.resolve("ro-crate-preview.html")));
    }

    @Test
    void testAutomaticPreviewZip(@TempDir Path dir) throws IOException {
        AutomaticPreview automaticPreview = new AutomaticPreview();
        Path crate = dir.resolve("crate");
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setFileNameInZip("ro-crate-metadata.json");

        ZipFile zipFile = new ZipFile(dir.resolve("test.zip").toFile());
        try (InputStream crateJson = PreviewTest.class.getResourceAsStream("/crates/other/idrc_project/ro-crate-metadata.json")) {
            zipFile.addStream(crateJson, zipParameters);
        }

        automaticPreview.saveAllToZip(zipFile);

        try {
            // this should trow an exception but not stop the execution
            ZipFile randomZipFile = new ZipFile(dir.resolve("dddd.zip").toFile());
            automaticPreview.saveAllToZip(randomZipFile);
            Assertions.fail("Expected IOException when providing invalid ZIP file for preview.");
        } catch (IOException ex) {
            //ok
        }
        zipFile.extractAll(crate.toString());
        assertTrue(Files.isRegularFile(crate.resolve("ro-crate-preview.html")));
    }

    @Test
    void testAutomaticPreviewZipStream(@TempDir Path dir) throws IOException {
        AutomaticPreview preview = new AutomaticPreview();
        String metadataPath = "/crates/other/idrc_project/ro-crate-metadata.json";
        Path crate = dir.resolve("crate");

        File zipFile = dir.resolve("test.zip").toFile();
        try (
                ZipFile zip = new ZipFile(zipFile);
                InputStream crateJson = PreviewTest.class.getResourceAsStream(metadataPath)
        ) {
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setFileNameInZip("ro-crate-metadata.json");
            zip.addStream(crateJson, zipParameters);
        }

        String metadata;
        try (InputStream metadataStream = PreviewTest.class.getResourceAsStream(metadataPath)) {
            Assertions.assertNotNull(metadataStream);
            metadata = new String(metadataStream.readAllBytes());
        }

        try (ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            preview.saveAllToStream(metadata, stream);
            stream.flush();
        }

        try {
            // this should throw an exception but not stop the execution
            ZipFile randomZipFile = new ZipFile(dir.resolve("dddd.zip").toFile());
            preview.saveAllToZip(randomZipFile);
            Assertions.fail("Expected IOException when providing invalid ZIP file for preview.");
        } catch (IOException ex) {
            //ok
        }

        try (ZipFile zipReader = new ZipFile(zipFile)) {
            zipReader.extractAll(crate.toString());
        }
        assertTrue(Files.isRegularFile(crate.resolve("ro-crate-preview.html")));
    }

    @Test
    void testCustomPreviewAddToFolder(@TempDir Path dir) throws IOException {
        CustomPreview customPreview = new CustomPreview();
        Path crate = dir.resolve("crate");
        Path fakeCrate = dir.resolve("fakeCrate");
        FileUtils.forceMkdir(crate.toFile());

        try (InputStream crateJson = PreviewTest.class.getResourceAsStream("/crates/other/idrc_project/ro-crate-metadata.json")) {
            Assertions.assertNotNull(crateJson);
            FileUtils.copyInputStreamToFile(crateJson, crate.resolve("ro-crate-metadata.json").toFile());
        }

        customPreview.saveAllToFolder(crate.toFile());

        try {
            // this should throw an exception but not stop the execution
            customPreview.saveAllToFolder(fakeCrate.toFile());
            Assertions.fail("Expected IOException when providing invalid ZIP file for preview.");
        } catch (IOException ex) {
            //ok
        }

        assertTrue(Files.isRegularFile(crate.resolve("ro-crate-preview.html")));
    }

    @Test
    void testCustomPreviewZip(@TempDir Path tmp) throws IOException {
        CustomPreview customPreview = new CustomPreview();
        Path crate = tmp.resolve("crate");
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setFileNameInZip("ro-crate-metadata.json");

        try (ZipFile zipFile = new ZipFile(tmp.resolve("test.zip").toFile());
             InputStream crateJson = PreviewTest.class.getResourceAsStream("/crates/other/idrc_project/ro-crate-metadata.json")) {
            zipFile.addStream(crateJson, zipParameters);
            customPreview.saveAllToZip(zipFile);

            try {
                // this should throw an exception but not stop the execution
                ZipFile randomZipFile = new ZipFile(tmp.resolve("dddd.zip").toFile());
                customPreview.saveAllToZip(randomZipFile);
                Assertions.fail("Expected IOException when providing invalid input to preview.");
            } catch (IOException ex) {
                //ok
            }
            zipFile.extractAll(crate.toString());
        }
        assertTrue(Files.isRegularFile(crate.resolve("ro-crate-preview.html")));
    }

    @Test
    void testCustomPreviewZipStream(@TempDir Path tmp) throws IOException {
        CustomPreview preview = new CustomPreview();
        String metadataPath = "/crates/other/idrc_project/ro-crate-metadata.json";
        Path crate = tmp.resolve("crate");
        File zipFile = tmp.resolve("test.zip").toFile();

        try (ZipFile zip = new ZipFile(zipFile);
             InputStream crateJson = PreviewTest.class.getResourceAsStream(metadataPath)) {
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setFileNameInZip("ro-crate-metadata.json");
            zip.addStream(crateJson, zipParameters);
        }

        String metadata;
        try (InputStream metadataStream = PreviewTest.class.getResourceAsStream(metadataPath)) {
            Assertions.assertNotNull(metadataStream);
            metadata = new String(metadataStream.readAllBytes());
        }

        try (ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            preview.saveAllToStream(metadata, stream);
            stream.flush();
        }

        try {
            // this should throw an exception but not stop the execution
            ZipFile randomZipFile = new ZipFile(tmp.resolve("dddd.zip").toFile());
            preview.saveAllToZip(randomZipFile);
            Assertions.fail("Expected IOException when providing invalid input to preview.");
        } catch (IOException ex) {
            //ok
        }

        try (ZipFile zipReader = new ZipFile(zipFile)) {
            zipReader.extractAll(crate.toString());
        }
        assertTrue(Files.isRegularFile(crate.resolve("ro-crate-preview.html")));
    }
}

