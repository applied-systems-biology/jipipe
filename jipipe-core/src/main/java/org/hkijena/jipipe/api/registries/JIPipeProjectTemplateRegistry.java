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

package org.hkijena.jipipe.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.project.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JIPipeProjectTemplateRegistry {
    private final JIPipe jiPipe;

    private final TemplatesUpdatedEventEmitter templatesUpdatedEventEmitter = new TemplatesUpdatedEventEmitter();

    private final Map<String, JIPipeProjectTemplate> registeredTemplates = new HashMap<>();

    private final Set<String> blockedTemplateNames = new HashSet<>();

    public JIPipeProjectTemplateRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public Map<String, JIPipeProjectTemplate> getRegisteredTemplates() {
        return Collections.unmodifiableMap(registeredTemplates);
    }

    public TemplatesUpdatedEventEmitter getTemplatesUpdatedEventEmitter() {
        return templatesUpdatedEventEmitter;
    }

    public void register(JIPipeProjectTemplate template) {
        registeredTemplates.put(template.getId(), template);
        templatesUpdatedEventEmitter.emit(new TemplatesUpdatedEvent(this));
        jiPipe.getProgressInfo().log("Registered project template " + template.getId() +
                (template.getZipFile() != null ? " [has ZIP data stored in " + template.getZipFile() + "]" : ""));
        blockedTemplateNames.add(template.getMetadata().getName());
        jiPipe.getProgressInfo().log(" -> Template name '" + template.getMetadata().getName() + "' is marked as blocked for file-based templates (this has only an effect to the GUI)");
    }

    public void register(Path file) throws IOException {
        if (UIUtils.EXTENSION_FILTER_ZIP.accept(file.toFile())) {
            try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(new JIPipeProgressInfo(), file)) {
                Path projectFile = storage.findFileByExtension(".jip").get();
                JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(storage.open(projectFile));
                String id = PathUtils.absoluteToJIPipeUserDirRelative(file) + "";
                JIPipeProjectMetadata templateMetadata = JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(node.get("metadata"));
                JIPipeProjectTemplate template = new JIPipeProjectTemplate(id, node, templateMetadata, file, file);
                register(template);
                templatesUpdatedEventEmitter.emit(new TemplatesUpdatedEvent(this));
            }
        } else {
            JsonNode node = JsonUtils.readFromFile(file, JsonNode.class);
            String id = PathUtils.absoluteToJIPipeUserDirRelative(file) + "";
            JIPipeProjectMetadata templateMetadata = JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(node.get("metadata"));
            JIPipeProjectTemplate template = new JIPipeProjectTemplate(id, node, templateMetadata, file, null);
            register(template);
            templatesUpdatedEventEmitter.emit(new TemplatesUpdatedEvent(this));
        }
    }

    public List<JIPipeProjectTemplate> getSortedRegisteredTemplates() {
        return registeredTemplates.values().stream().sorted(Comparator.comparing((JIPipeProjectTemplate template) -> template.getMetadata().getName())).collect(Collectors.toList());
    }

    public Set<String> getBlockedTemplateNames() {
        return Collections.unmodifiableSet(blockedTemplateNames);
    }

    public interface TemplatesUpdatedEventListener {
        void onJIPipeTemplatesUpdated(TemplatesUpdatedEvent event);
    }

    public static class TemplatesUpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeProjectTemplateRegistry registry;

        public TemplatesUpdatedEvent(JIPipeProjectTemplateRegistry registry) {
            super(registry);
            this.registry = registry;
        }

        public JIPipeProjectTemplateRegistry getRegistry() {
            return registry;
        }
    }

    public static class TemplatesUpdatedEventEmitter extends JIPipeEventEmitter<TemplatesUpdatedEvent, TemplatesUpdatedEventListener> {

        @Override
        protected void call(TemplatesUpdatedEventListener templatesUpdatedEventListener, TemplatesUpdatedEvent event) {
            templatesUpdatedEventListener.onJIPipeTemplatesUpdated(event);
        }
    }
}
