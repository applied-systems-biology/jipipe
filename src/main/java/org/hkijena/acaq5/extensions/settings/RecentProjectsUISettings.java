package org.hkijena.acaq5.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.collections.PathListParameter;

import java.nio.file.Path;

/**
 * Remembers the last projects
 */
public class RecentProjectsUISettings implements ACAQParameterCollection {

    public static String ID = "recent-projects-ui";

    private EventBus eventBus = new EventBus();
    private PathListParameter recentProjects = new PathListParameter();
    private PathListParameter recentJsonExtensionProjects = new PathListParameter();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Recent projects", description = "List of recent projects")
    @ACAQParameter("recent-projects")
    public PathListParameter getRecentProjects() {
        return recentProjects;
    }

    @ACAQParameter("recent-projects")
    public void setRecentProjects(PathListParameter recentProjects) {
        this.recentProjects = recentProjects;
        getEventBus().post(new ParameterChangedEvent(this, "recent-projects"));
    }

    @ACAQDocumentation(name = "Recent JSON extension projects", description = "List of recent JSON extension projects")
    @ACAQParameter("recent-json-extension-projects")
    public PathListParameter getRecentJsonExtensionProjects() {
        return recentJsonExtensionProjects;
    }

    @ACAQParameter("recent-json-extension-projects")
    public void setRecentJsonExtensionProjects(PathListParameter recentJsonExtensionProjects) {
        this.recentJsonExtensionProjects = recentJsonExtensionProjects;
        getEventBus().post(new ParameterChangedEvent(this, "recent-json-extension-projects"));
    }

    /**
     * Adds a project file to the list of recent projects
     *
     * @param fileName Project file
     */
    public void addRecentProject(Path fileName) {
        int index = recentProjects.indexOf(fileName);
        if (index == -1) {
            recentProjects.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-projects"));
        } else if (index != 0) {
            recentProjects.remove(index);
            recentProjects.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-projects"));
        }
    }

    /**
     * Adds a JSON extension file to the list of recent JSON extensions
     *
     * @param fileName JSON extension file
     */
    public void addRecentJsonExtension(Path fileName) {
        int index = recentJsonExtensionProjects.indexOf(fileName);
        if (index == -1) {
            recentJsonExtensionProjects.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-json-extension-projects"));
        } else if (index != 0) {
            recentJsonExtensionProjects.remove(index);
            recentJsonExtensionProjects.add(0, fileName);
            eventBus.post(new ParameterChangedEvent(this, "recent-json-extension-projects"));
        }
    }

    public static RecentProjectsUISettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, RecentProjectsUISettings.class);
    }
}
