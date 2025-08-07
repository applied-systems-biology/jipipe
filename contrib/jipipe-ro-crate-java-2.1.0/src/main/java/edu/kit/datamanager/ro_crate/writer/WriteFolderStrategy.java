package edu.kit.datamanager.ro_crate.writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * A class for writing a crate to a folder.
 *
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 */
public class WriteFolderStrategy implements GenericWriterStrategy<String> {

    private static final Logger logger = LoggerFactory.getLogger(WriteFolderStrategy.class);

    protected boolean writePreview = true;

    /**
     * For internal use. Skips the preview generation when writing the crate.
     *
     * @return this instance of WriteFolderStrategy
     *
     * @deprecated May be removed in future versions. Not intended for public use.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public WriteFolderStrategy disablePreview() {
        this.writePreview = false;
        return this;
    }

    @Override
    public void save(Crate crate, String destination) throws IOException {
        File file = new File(destination);
        FileUtils.forceMkdir(file);
        ObjectMapper objectMapper = MyObjectMapper.getMapper();
        JsonNode node = objectMapper.readTree(crate.getJsonMetadata());
        String str = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        InputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));

        File json = new File(destination, "ro-crate-metadata.json");
        FileUtils.copyInputStreamToFile(inputStream, json);
        inputStream.close();
        // save also the preview files to the crate destination
        if (crate.getPreview() != null && this.writePreview) {
            crate.getPreview().saveAllToFolder(file);
        }
        for (var e : crate.getUntrackedFiles()) {
            if (e.isDirectory()) {
                FileUtils.copyDirectoryToDirectory(e, file);
            } else {
                FileUtils.copyFileToDirectory(e, file);
            }
        }
        for (DataEntity dataEntity : crate.getAllDataEntities()) {
            savetoFile(dataEntity, file);
        }
    }

    private void savetoFile(DataEntity entity, File file) throws IOException {
        if (entity.getPath() != null) {
            if (entity.getPath().toFile().isDirectory()) {
                FileUtils.copyDirectory(entity.getPath().toFile(), file.toPath().resolve(entity.getId()).toFile());
            } else {
                FileUtils.copyFile(entity.getPath().toFile(), file.toPath().resolve(entity.getId()).toFile());
            }
        }
    }
}
