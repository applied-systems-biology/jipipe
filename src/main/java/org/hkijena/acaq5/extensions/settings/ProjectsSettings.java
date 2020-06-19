package org.hkijena.acaq5.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.primitives.PathList;

import java.nio.file.Path;

/**
 * Remembers the last projects
 */
public class ProjectsSettings implements ACAQParameterCollection {

    public static String ID = "projects";

    private EventBus eventBus = new EventBus();
    private PathList recentProjects = new PathList();
    private PathList recentJsonExtensionProjects = new PathList();
    private StarterProject starterProject = StarterProject.PreprocessingAnalysisPostprocessing;

    public static ProjectsSettings getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, ProjectsSettings.class);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQDocumentation(name = "Recent projects", description = "List of recent projects")
    @ACAQParameter("recent-projects")
    public PathList getRecentProjects() {
        return recentProjects;
    }

    @ACAQParameter("recent-projects")
    public void setRecentProjects(PathList recentProjects) {
        this.recentProjects = recentProjects;
        getEventBus().post(new ParameterChangedEvent(this, "recent-projects"));
    }

    @ACAQDocumentation(name = "Recent JSON extension projects", description = "List of recent JSON extension projects")
    @ACAQParameter("recent-json-extension-projects")
    public PathList getRecentJsonExtensionProjects() {
        return recentJsonExtensionProjects;
    }

    @ACAQParameter("recent-json-extension-projects")
    public void setRecentJsonExtensionProjects(PathList recentJsonExtensionProjects) {
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

    @ACAQDocumentation(name = "Empty project configuration", description = "Determines how empty projects are created")
    @ACAQParameter("starter-project")
    public StarterProject getStarterProject() {
        return starterProject;
    }

    @ACAQParameter("starter-project")
    public void setStarterProject(StarterProject starterProject) {
        this.starterProject = starterProject;
        eventBus.post(new ParameterChangedEvent(this, "starter-project"));
    }

    /**
     * Defines available starter projects
     */
    public enum StarterProject {
        SingleCompartment,
        PreprocessingAnalysisPostprocessing
    }
}
