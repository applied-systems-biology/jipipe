package org.hkijena.jipipe.api.registries;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectTemplate;

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
    }

    public List<JIPipeProjectTemplate> getSortedRegisteredTemplates() {
        return registeredTemplates.values().stream().sorted(Comparator.comparing((JIPipeProjectTemplate template) -> template.getMetadata().getName())).collect(Collectors.toList());
    }
}
