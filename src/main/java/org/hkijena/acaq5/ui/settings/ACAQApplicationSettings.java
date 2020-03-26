package org.hkijena.acaq5.ui.settings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import ij.IJ;
import ij.Prefs;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ACAQApplicationSettings {

    private static ACAQApplicationSettings instance;
    private EventBus eventBus = new EventBus();

    private List<Path> recentProjects = new ArrayList<>();
    private List<Path> recentJsonExtensions = new ArrayList<>();

    public ACAQApplicationSettings() {

    }

    public EventBus getEventBus() {
        return eventBus;
    }

    @JsonGetter("recent-projects")
    public List<Path> getRecentProjects() {
        return recentProjects.stream().filter(p -> Files.exists(p)).collect(Collectors.toList());
    }

    @JsonSetter("recent-projects")
    public void setRecentProjects(List<Path> recentProjects) {
        this.recentProjects = recentProjects;
        eventBus.post(new ParameterChangedEvent(this, "recent-projects"));
    }

    public void clearRecentProjects() {
        recentProjects.clear();
        save();
        eventBus.post(new ParameterChangedEvent(this, "recent-projects"));
    }

    public void clearRecentJsonExtensions() {
        recentJsonExtensions.clear();
        save();
        eventBus.post(new ParameterChangedEvent(this, "recent-json-extensions"));
    }

    public void addRecentProject(Path fileName) {
        int index = recentProjects.indexOf(fileName);
        if (index == -1) {
            recentProjects.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-projects"));
            save();
        } else if (index != 0) {
            recentProjects.remove(index);
            recentProjects.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-projects"));
            save();
        }
    }

    public void addRecentJsonExtension(Path fileName) {
        int index = recentJsonExtensions.indexOf(fileName);
        if (index == -1) {
            recentJsonExtensions.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-json-extensions"));
            save();
        } else if (index != 0) {
            recentJsonExtensions.remove(index);
            recentJsonExtensions.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-json-extensions"));
            save();
        }
    }

    public void save() {
        File targetFile = getPropertyFile();
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(targetFile, this);
        } catch (IOException e) {
            IJ.handleException(e);
        }
    }

    @JsonGetter("recent-json-extensions")
    public List<Path> getRecentJsonExtensions() {
        return recentJsonExtensions;
    }

    @JsonGetter("recent-json-extensions")
    public void setRecentJsonExtensions(List<Path> recentJsonExtensions) {
        this.recentJsonExtensions = recentJsonExtensions;
    }

    public static ACAQApplicationSettings getInstance() {
        if (instance == null) {
            File targetFile = getPropertyFile();
            if (targetFile.exists()) {
                try {
                    instance = JsonUtils.getObjectMapper().readerFor(ACAQApplicationSettings.class).readValue(targetFile);
                } catch (IOException e) {
                    instance = new ACAQApplicationSettings();
                    IJ.handleException(e);
                }
            } else {
                instance = new ACAQApplicationSettings();
            }
        }
        return instance;
    }

    public static File getPropertyFile() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return imageJDir.resolve("acaq5.properties.json").toFile();
    }
}
