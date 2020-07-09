/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.settings;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.primitives.PathList;

import java.nio.file.Path;

/**
 * Remembers the last projects
 */
public class ProjectsSettings implements JIPipeParameterCollection {

    public static String ID = "projects";

    private EventBus eventBus = new EventBus();
    private PathList recentProjects = new PathList();
    private PathList recentJsonExtensionProjects = new PathList();
    private StarterProject starterProject = StarterProject.PreprocessingAnalysisPostprocessing;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Recent projects", description = "List of recent projects")
    @JIPipeParameter("recent-projects")
    public PathList getRecentProjects() {
        return recentProjects;
    }

    @JIPipeParameter("recent-projects")
    public void setRecentProjects(PathList recentProjects) {
        this.recentProjects = recentProjects;

    }

    @JIPipeDocumentation(name = "Recent JSON extension projects", description = "List of recent JSON extension projects")
    @JIPipeParameter("recent-json-extension-projects")
    public PathList getRecentJsonExtensionProjects() {
        return recentJsonExtensionProjects;
    }

    @JIPipeParameter("recent-json-extension-projects")
    public void setRecentJsonExtensionProjects(PathList recentJsonExtensionProjects) {
        this.recentJsonExtensionProjects = recentJsonExtensionProjects;

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

    @JIPipeDocumentation(name = "Empty project configuration", description = "Determines how empty projects are created")
    @JIPipeParameter("starter-project")
    public StarterProject getStarterProject() {
        return starterProject;
    }

    @JIPipeParameter("starter-project")
    public void setStarterProject(StarterProject starterProject) {
        this.starterProject = starterProject;
        eventBus.post(new ParameterChangedEvent(this, "starter-project"));
    }

    public static ProjectsSettings getInstance() {
        return JIPipeDefaultRegistry.getInstance().getSettingsRegistry().getSettings(ID, ProjectsSettings.class);
    }

    /**
     * Defines available starter projects
     */
    public enum StarterProject {
        SingleCompartment,
        PreprocessingAnalysisPostprocessing
    }
}
