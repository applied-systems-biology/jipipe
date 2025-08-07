package edu.kit.datamanager.ro_crate.crate.preview;

import edu.kit.datamanager.ro_crate.writer.CrateWriter;
import edu.kit.datamanager.ro_crate.writer.Writers;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.preview.AutomaticPreview;
import edu.kit.datamanager.ro_crate.preview.CustomPreview;
import edu.kit.datamanager.ro_crate.preview.StaticPreview;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreviewCrateTest {

    @Test
    void testAutomaticPreview(@TempDir Path temp) throws IOException {
        Path location = temp.resolve("ro_crate1");
        RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .setPreview(new AutomaticPreview())
                .build();
        Writers.newFolderWriter()
                .save(crate, location.toFile().getAbsolutePath());
        assertTrue(Files.isRegularFile(location.resolve("ro-crate-preview.html")));
    }

    @Test
    void testAutomaticPreviewAddingLater(@TempDir Path temp) throws IOException {
        Path location = temp.resolve("ro_crate2");
        RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .setPreview(null)//disable preview to allow to compare folders before and after
                .build();
        CrateWriter<String> writer = Writers.newFolderWriter();
        writer.save(crate, location.toFile().toString());
        assertFalse(location.resolve("ro-crate-preview.html").toFile().exists());
        crate.setRoCratePreview(new AutomaticPreview());
        writer.save(crate, location.toFile().toString());
        assertTrue(location.resolve("ro-crate-preview.html").toFile().exists());
    }

    @Test
    void testCustomPreview(@TempDir Path temp) throws IOException {
        Path location = temp.resolve("ro_crate1");
        RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .setPreview(new CustomPreview())
                .build();
        Writers.newFolderWriter()
                .save(crate, location.toFile().getAbsolutePath());
        assertTrue(Files.isRegularFile(location.resolve("ro-crate-preview.html")));
    }

    @Test
    void testStaticPreviewOnlyHtmlFile(@TempDir Path temp) throws IOException {
        Path location = temp.resolve("ro_crate3");
        Path previewFile = temp.resolve("random.html");
        FileUtils.writeStringToFile(previewFile.toFile(), "random html it is not important that it is valid", Charset.defaultCharset());
        RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .setPreview(new StaticPreview(previewFile.toFile()))
                .build();
        Writers.newFolderWriter()
                .save(crate, location.toFile().getAbsolutePath());
        assertTrue(location.resolve("ro-crate-preview.html").toFile().exists());
    }

    @Test
    void testStaticPreviewHtmlFileWithOtherFiles(@TempDir Path temp) throws IOException {
        Path location = temp.resolve("ro_crate4");
        Path previewFile = temp.resolve("random.html");
        FileUtils.writeStringToFile(previewFile.toFile(), "random html it is not important that it is valid", Charset.defaultCharset());
        Path dirHtml = temp.resolve("html_dir");
        Path css_file = dirHtml.resolve("test.css");
        FileUtils.writeStringToFile(css_file.toFile(), "random css it is not important that it is valid", Charset.defaultCharset());
        RoCrate crate = new RoCrate.RoCrateBuilder("name", "description", "2024", "https://creativecommons.org/licenses/by-nc-sa/3.0/au/")
                .setPreview(new StaticPreview(previewFile.toFile(), dirHtml.toFile()))
                .build();
        Writers.newFolderWriter()
                .save(crate, location.toFile().getAbsolutePath());
        assertTrue(location.resolve("ro-crate-preview.html").toFile().exists());
        assertTrue(location.resolve("ro-crate-preview_files").toFile().exists());
        assertTrue(location.resolve("ro-crate-preview_files").resolve("test.css").toFile().exists());
    }

}
