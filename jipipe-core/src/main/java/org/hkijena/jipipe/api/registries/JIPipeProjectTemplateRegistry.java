package org.hkijena.jipipe.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.SerializationUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeProjectTemplateRegistry {
    private final JIPipe jiPipe;

    private final Map<String, JIPipeProjectTemplate> registeredTemplates = new HashMap<>();

    public JIPipeProjectTemplateRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public Map<String, JIPipeProjectTemplate> getRegisteredTemplates() {
        return Collections.unmodifiableMap(registeredTemplates);
    }

    public void register(JIPipeProjectTemplate template) {
        registeredTemplates.put(template.getId(), template);
        jiPipe.getEventBus().post(new TemplatesUpdatedEvent(this));
        jiPipe.getProgressInfo().log("Registered project template " + template.getId());
    }

    public void register(Path file) throws IOException {
        JsonNode node = JsonUtils.readFromFile(file, JsonNode.class);
        String id = PathUtils.absoluteToImageJRelative(file) + "";
        JIPipeProjectMetadata templateMetadata = JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(node.get("metadata"));
        JIPipeProjectTemplate template = new JIPipeProjectTemplate(id, node, templateMetadata);
        register(template);
        jiPipe.getEventBus().post(new TemplatesUpdatedEvent(this));
    }
    public List<JIPipeProjectTemplate> getSortedRegisteredTemplates() {
        return registeredTemplates.values().stream().sorted(Comparator.comparing((JIPipeProjectTemplate template) -> template.getMetadata().getName())).collect(Collectors.toList());
    }

    public static class TemplatesUpdatedEvent {
        private final JIPipeProjectTemplateRegistry registry;

        public TemplatesUpdatedEvent(JIPipeProjectTemplateRegistry registry) {
            this.registry = registry;
        }

        public JIPipeProjectTemplateRegistry getRegistry() {
            return registry;
        }
    }
}
