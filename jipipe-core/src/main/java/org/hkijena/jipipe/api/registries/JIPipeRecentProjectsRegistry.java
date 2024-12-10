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
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeRecentProjectsRegistry {
    private final JIPipe jiPipe;
    private final List<Path> recentProjects = new ArrayList<>();
    private final ChangedEventEmitter changedEventEmitter = new ChangedEventEmitter();

    public JIPipeRecentProjectsRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
    }

    public JIPipe getJiPipe() {
        return jiPipe;
    }

    public ChangedEventEmitter getChangedEventEmitter() {
        return changedEventEmitter;
    }

    /**
     * Adds a project file to the list of recent projects
     *
     * @param fileName Project file
     */
    public void add(Path fileName) {
        int index = recentProjects.indexOf(fileName);
        if (index == -1) {
            recentProjects.add(0, fileName);
            save();
            changedEventEmitter.emit(new ChangedEvent(this));
        } else if (index != 0) {
            recentProjects.remove(index);
            recentProjects.add(0, fileName);
            changedEventEmitter.emit(new ChangedEvent(this));
           save();
        }
    }

    public List<Path> getRecentProjects() {
        return Collections.unmodifiableList(recentProjects);
    }

    public void clear() {
        recentProjects.clear();
        save();
        changedEventEmitter.emit(new ChangedEvent(this));
    }


    public void cleanup() {
        List<Path> invalidRecentProjects = recentProjects.stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
        if (!invalidRecentProjects.isEmpty()) {
          recentProjects.removeAll(invalidRecentProjects);
          save();
        }
    }

    public Path getPropertyFile() {
        return JIPipe.getJIPipeUserDir().resolve("recent-projects.txt");
    }

    public void reload() {
        recentProjects.clear();
        if(Files.isRegularFile(getPropertyFile())) {
            try {
                for (String line : Files.readAllLines(getPropertyFile())) {
                    if(!StringUtils.isNullOrEmpty(line)) {
                        try {
                            recentProjects.add(Paths.get(line));
                        }
                        catch (Exception ignored) {
                            jiPipe.getProgressInfo().log("Unable to load recent project " + line);
                        }
                    }
                }
            } catch (IOException e) {
                jiPipe.getProgressInfo().log(e);
            }
        }
    }

    public void save() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Path recentProject : recentProjects) {
            stringBuilder.append(recentProject).append(System.lineSeparator());
        }
        try {
            Files.write(getPropertyFile(), stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void migrateFromLegacy() {
        Path propertyFile = JIPipeApplicationSettingsRegistry.getPropertyFile();
        boolean success = false;
        if(Files.exists(propertyFile)) {
            try {
                JsonNode properties = JsonUtils.getObjectMapper().readValue(propertyFile.toFile(), JsonNode.class);
                JsonNode recentProjectsNode = properties.path(JIPipeProjectDefaultsApplicationSettings.ID).path("recent-projects");
                if(recentProjectsNode.isArray()) {
                    for (JsonNode node : ImmutableList.copyOf(recentProjectsNode.elements())) {
                        try {
                            Path path = Paths.get(node.textValue());
                            if(Files.exists(path) && !recentProjects.contains(path)) {
                                recentProjects.add(path);
                                jiPipe.getProgressInfo().log("- Migrated recent project " + path);
                                success = true;
                            }
                        }
                        catch (Exception ignored) {
                        }
                    }

                }
            }
            catch (Exception ignored) {

            }
        }
        if(success) {
            jiPipe.getProgressInfo().log("Migrated recent projects. Saving application settings.");
            jiPipe.getApplicationSettingsRegistry().save();
            save();
        }
    }

    public void removeAll(Collection<Path> values) {
        boolean success = false;
        for (Path value : values) {
            if(recentProjects.remove(value)) {
                success = true;
            }
        }
        if(success) {
            save();
            changedEventEmitter.emit(new ChangedEvent(this));
        }
    }

    public static class ChangedEvent extends AbstractJIPipeEvent {
        public ChangedEvent(Object source) {
            super(source);
        }
    }

    public interface ChangedEventListener {
        void onRecentProjectsChanged(ChangedEvent event);
    }

    public static class ChangedEventEmitter extends JIPipeEventEmitter<ChangedEvent, ChangedEventListener> {

        @Override
        protected void call(ChangedEventListener changedEventListener, ChangedEvent event) {
            changedEventListener.onRecentProjectsChanged(event);
        }
    }
}
