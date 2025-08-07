package edu.kit.datamanager.ro_crate.preview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.ro_crate.util.ZipStreamUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class generates a custom preview without requiring external
 * dependencies, i.e., rochtml. Therefore, the FreeMarker template located under
 * resources/templates/custom_preview.ftl is used.
 *
 * @author jejkal
 */
public class CustomPreview implements CratePreview {

    private final static Logger logger = LoggerFactory.getLogger(CustomPreview.class);

    private Template template = null;

    public CustomPreview() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setClassForTemplateLoading(CustomPreview.class, "/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        try {
            template = cfg.getTemplate("templates/custom_preview.ftl");
        } catch (IOException ex) {
            logger.error("Failed to read template for CustomPreview.", ex);
        }
    }

    private CustomPreviewModel mapFromJson(String metadata) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readValue(metadata, JsonNode.class);
        JsonNode graph = root.get("@graph");
        CustomPreviewModel.ROCrate crate = new CustomPreviewModel.ROCrate();
        List<CustomPreviewModel.Dataset> datasets = new ArrayList<>();
        List<CustomPreviewModel.File> files = new ArrayList<>();

        if (graph.isArray()) {

            for (JsonNode node : graph) {
                String id = node.path("@id").asText();
                List<String> types = new LinkedList<>();
                if (node.path("@type").isArray()) {

                    Collections.addAll(types, mapper.convertValue(node.path("@type"), String[].class));
                } else {
                    types.add(node.path("@type").asText());
                }

                if (types.contains("Dataset") && "./".equals(id)) {
                    crate.name = node.path("name").asText();
                    crate.description = node.path("description").asText(null);
                    crate.type = "Dataset";
                    crate.license = node.path("license").path("@id").asText(node.path("license").asText(null));
                    crate.datePublished = node.path("datePublished").asText(null);
                    crate.hasPart = new ArrayList<>();

                    for (JsonNode part : node.path("hasPart")) {
                        CustomPreviewModel.Part p = new CustomPreviewModel.Part();
                        String tmpId = part.path("@id").asText(part.asText());
                        p.id = tmpId;
                        p.name = tmpId; // Name will be replaced later
                        crate.hasPart.add(p);
                    }
                } else if (types.contains("Dataset")) {
                    CustomPreviewModel.Dataset dataset = new CustomPreviewModel.Dataset();
                    dataset.id = id;
                    dataset.name = node.path("name").asText();
                    dataset.description = node.path("description").asText();
                    datasets.add(dataset);
                } else if (types.contains("File")) {
                    CustomPreviewModel.File file = new CustomPreviewModel.File();
                    file.id = id;
                    file.name = node.path("name").asText(null);
                    file.description = node.path("description").asText(null);
                    file.contentSize = node.path("contentSize").asText(null);
                    file.encodingFormat = node.path("encodingFormat").asText(null);
                    files.add(file);
                }
            }
        }

        // Update Part names using dataset and file lists
        if (crate.hasPart != null) {
            for (CustomPreviewModel.Part part : crate.hasPart) {
                for (CustomPreviewModel.Dataset dataset : datasets) {
                    if (dataset.id.equals(part.id) && dataset.name != null) {
                        part.name = dataset.name;
                    }
                }
                for (CustomPreviewModel.File file : files) {
                    if (file.id.equals(part.id) && file.name != null) {
                        part.name = file.name;
                    }
                }
            }
        }

        CustomPreviewModel model = new CustomPreviewModel();
        model.crate = crate;
        model.datasets = datasets;
        model.files = files;
        return model;
    }

    @Override
    public void saveAllToZip(ZipFile zipFile) throws IOException {
        if (template == null) {
            throw new IOException("Preview template did not load. Unable to proceed.");
        }
        if (zipFile == null) {
            throw new IOException("Argument zipFile must not be null.");
        }
        try {
            zipFile.extractFile("ro-crate-metadata.json", "temp");
        } catch (ZipException ex) {
            throw new IOException("ro-crate-metadata.json not found in provided ZIP.", ex);
        }

        String metadata = FileUtils.readFileToString(new File("temp/ro-crate-metadata.json"), "UTF-8");
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("crateModel", mapFromJson(metadata));

            try (Writer out = new OutputStreamWriter(new FileOutputStream("temp/ro-crate-preview.html"))) {
                template.process(dataModel, out);
                out.flush();
            }
            zipFile.addFile("temp/ro-crate-preview.html");
        } catch (TemplateException ex) {
            throw new IOException("Failed to generate preview.", ex);
        } finally {
            try {
                FileUtils.deleteDirectory(new File("temp"));
            } catch (IOException ex) {
                //ignore
            }
        }
    }

    @Override
    public void saveAllToFolder(File folder) throws IOException {
        if (template == null) {
            throw new IOException("Preview template did not load. Unable to proceed.");
        }
        if (folder == null || !folder.exists()) {
            throw new IOException("Preview target folder " + folder + " does not exist.");
        }
        String metadata = FileUtils.readFileToString(new File(folder, "ro-crate-metadata.json"), "UTF-8");
        try {
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("crateModel", mapFromJson(metadata));
            try (Writer out = new OutputStreamWriter(new FileOutputStream(new File(folder, "ro-crate-preview.html")))) {
                template.process(dataModel, out);
                out.flush();
            }
        } catch (TemplateException ex) {
            throw new IOException("Failed to generate preview.", ex);
        }
    }

    @Override
    public void saveAllToStream(String metadata, ZipOutputStream stream) throws IOException {
        if (template == null) {
            throw new IOException("Preview template did not load. Unable to proceed.");
        }
        try {
            //prepare metadata for template
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("crateModel", mapFromJson(metadata));

            //prepare output folder and writer
            FileUtils.forceMkdir(new File("temp"));
            //load and process template
            try (FileWriter writer = new FileWriter("temp/ro-crate-preview.html")) {
                //load and process template
                template.process(dataModel, writer);
                writer.flush();
            }

            ZipStreamUtil.addFileToZipStream(stream, new File("temp/ro-crate-preview.html"), "ro-crate-preview.html");
        } catch (TemplateException ex) {
            throw new IOException("Failed to generate preview.", ex);
        } finally {
            try {
                FileUtils.deleteDirectory(new File("temp"));
            } catch (IOException ex) {
                //ignore
            }
        }
    }

}
