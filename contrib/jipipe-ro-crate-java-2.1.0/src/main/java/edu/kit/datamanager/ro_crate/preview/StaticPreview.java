package edu.kit.datamanager.ro_crate.preview;

import edu.kit.datamanager.ro_crate.util.ZipStreamUtil;
import java.io.File;
import java.io.IOException;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;

/**
 * This class adds a static preview to the crate, which consists of a
 * metadataHtml file and a folder containing other files required to render
 * metadataHtml. It will be put unchanged to the writer output, i.e., a zip
 * file, folder, or stream.
 *
 * @author jejkal
 */
public class StaticPreview implements CratePreview {

    private final File metadataHtml;
    private final File otherFiles;

    public StaticPreview(File metadataHtml, File otherFiles) {
        this.metadataHtml = metadataHtml;
        this.otherFiles = otherFiles;
    }

    public StaticPreview(File metadataHtml) {
        this.metadataHtml = metadataHtml;
        this.otherFiles = null;
    }

    @Override
    public void saveAllToZip(ZipFile zipFile) throws IOException {
        if (this.metadataHtml != null) {
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setFileNameInZip("ro-crate-preview.html");
            zipFile.addFile(this.metadataHtml, zipParameters);
        }

        if (this.otherFiles != null) {
            zipFile.addFolder(this.otherFiles);
            zipFile.renameFile(this.otherFiles.getName() + "/", "ro-crate-preview_files/");
        }
    }

    @Override
    public void saveAllToFolder(File folder) throws IOException {
        if (folder == null || !folder.exists()) {
            throw new IOException("Preview target folder " + folder + " does not exist.");
        }
        
        if (this.metadataHtml != null) {
            File fileInCrate = folder.toPath().resolve("ro-crate-preview.html").toFile();
            FileUtils.copyFile(this.metadataHtml, fileInCrate);
        }
        if (this.otherFiles != null) {
            File folderName = folder.toPath().resolve("ro-crate-preview_files").toFile();
            FileUtils.copyDirectory(this.otherFiles, folderName);
        }
    }

    @Override
    public void saveAllToStream(String metadata, ZipOutputStream stream) throws IOException {
        ZipStreamUtil.addFileToZipStream(stream, this.metadataHtml, "ro-crate-preview.html");
        if (this.otherFiles != null) {
            ZipStreamUtil.addFolderToZipStream(stream, this.otherFiles, "ro-crate-preview_files");
        }
    }
}
