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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventEmitter;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JIPipeNodeTemplateRegistry {
    private final JIPipe jiPipe;
    private final Set<JIPipeNodeTemplate> pluginTemplates = new HashSet<>();
    private final List<JIPipeNodeTemplate> globalTemplates = new ArrayList<>();
    private final BiMap<JIPipeNodeTemplate, Path> globalTemplatesPaths = HashBiMap.create();
    private final NodeTemplatesRefreshedEventEmitter nodeTemplatesRefreshedEventEmitter = new NodeTemplatesRefreshedEventEmitter();

    public JIPipeNodeTemplateRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public Path getStoragePath() {
        return PathUtils.getJIPipeUserDir().resolve("node-templates");
    }

    public void reloadGlobalTemplates(JIPipeProgressInfo progressInfo) {
        globalTemplates.clear();
        globalTemplatesPaths.clear();

        try {
            Path storagePath = getStoragePath();
            progressInfo.log("Storage path is " + storagePath);
            Files.createDirectories(storagePath);

            for (Path jsonPath : PathUtils.findFilesByExtensionIn(storagePath, ".json")) {
                try {
                    progressInfo.log("Loading " + jsonPath);
                    JIPipeNodeTemplate nodeTemplate = JsonUtils.readFromFile(jsonPath, JIPipeNodeTemplate.class);
                    globalTemplates.add(nodeTemplate);
                    globalTemplatesPaths.put(nodeTemplate, jsonPath);
                } catch (Throwable e) {
                    progressInfo.resolve(jsonPath.toString()).log(ExceptionUtils.getStackTrace(e));
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            progressInfo.log(ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
            IJ.handleException(e);
        }

        emitRefreshedEvent();
    }

    public void addFromPlugin(JIPipeNodeTemplate nodeTemplate) {
        pluginTemplates.add(nodeTemplate);
        nodeTemplate.setSource(JIPipeNodeTemplate.SOURCE_EXTENSION);
        globalTemplates.add(nodeTemplate);
        pluginTemplates.add(nodeTemplate);
    }

    public List<JIPipeNodeTemplate> getGlobalTemplates() {
        return Collections.unmodifiableList(globalTemplates);
    }

    /**
     * Gets all templates
     *
     * @param project the project. can be null (no project templates).
     * @return all templates in a mutable list
     */
    public List<JIPipeNodeTemplate> getAllTemplates(JIPipeProject project) {
        List<JIPipeNodeTemplate> result = new ArrayList<>(globalTemplates);
        if (project != null) {
            result.addAll(project.getMetadata().getNodeTemplates());
        }
        return result;
    }

    public boolean isPluginTemplate(JIPipeNodeTemplate template) {
        return JIPipeNodeTemplate.SOURCE_EXTENSION.equals(template.getSource()) || pluginTemplates.contains(template);
    }

    public boolean addToGlobal_(JIPipeNodeTemplate template) {
        if (isPluginTemplate(template)) {
            return false;
        }
        if (globalTemplates.contains(template)) {
            return false;
        }
        globalTemplates.add(template);
        Path targetPath = getStoragePath().resolve(UUID.randomUUID() + ".json");
        JsonUtils.saveToFile(template, targetPath);
        return true;
    }

    public boolean isInGlobal(JIPipeNodeTemplate template) {
        return globalTemplates.contains(template);
    }

    public boolean isInProject(JIPipeNodeTemplate template, JIPipeProject project) {
        return project.getMetadata().getNodeTemplates().contains(template);
    }

    public boolean addToGlobal(JIPipeNodeTemplate template) {
        if (addToGlobal_(template)) {
            emitRefreshedEvent();
            return true;
        }
        return false;
    }

    public void addToGlobal(Collection<JIPipeNodeTemplate> templates) {
        for (JIPipeNodeTemplate template : templates) {
            addToGlobal_(template);
        }
        emitRefreshedEvent();
    }

    public boolean addToProject_(JIPipeNodeTemplate template, JIPipeProject project) {
        if (isPluginTemplate(template)) {
            return false;
        }
        if (project.getMetadata().getNodeTemplates().contains(template)) {
            return false;
        }

        project.getMetadata().getNodeTemplates().add(template);

        return true;
    }

    public boolean addToProject(JIPipeNodeTemplate template, JIPipeProject project) {
        if (addToProject_(template, project)) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
            emitRefreshedEvent();
            return true;
        }
        return false;
    }

    public void addToProject(Collection<JIPipeNodeTemplate> templates, JIPipeProject project) {
        for (JIPipeNodeTemplate template : templates) {
            addToProject_(template, project);
        }
        project.getMetadata().emitParameterChangedEvent("node-templates");
        emitRefreshedEvent();
    }

    public NodeTemplatesRefreshedEventEmitter getNodeTemplatesRefreshedEventEmitter() {
        return nodeTemplatesRefreshedEventEmitter;
    }

    public void emitRefreshedEvent() {
        nodeTemplatesRefreshedEventEmitter.emit(new NodeTemplatesRefreshedEvent());
    }

    public void editTemplate(JIPipeNodeTemplate target, JIPipeNodeTemplate source, JIPipeProject project) {
        target.copyFrom(source);
        target.setSource(JIPipeNodeTemplate.SOURCE_USER);
        if (project != null) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
        }
        emitRefreshedEvent();
    }

    public boolean removeFromProject(JIPipeNodeTemplate template, JIPipeProject project) {
        if (project.getMetadata().getNodeTemplates().remove(template)) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
            emitRefreshedEvent();
            return true;
        }
        return false;
    }

    public boolean removeFromGlobal(JIPipeNodeTemplate template) {
        if (globalTemplates.contains(template)) {
            globalTemplates.remove(template);
            Path targetPath = globalTemplatesPaths.get(template);
            if (targetPath != null && Files.isRegularFile(targetPath)) {
                try {
                    Files.delete(targetPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            emitRefreshedEvent();
            return true;
        }
        return false;
    }
}
