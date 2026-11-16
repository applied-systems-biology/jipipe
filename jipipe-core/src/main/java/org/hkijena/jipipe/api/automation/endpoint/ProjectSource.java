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

package org.hkijena.jipipe.api.automation.endpoint;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the projects a JIPipe instance currently exposes to the automation layer.
 * Project keys are stable within a session; the MCP broker routes tool calls with
 * {@code <instanceId>:<key>}.
 */
@FunctionalInterface
public interface ProjectSource {

    List<MCPProjectEntry> getProjects();

    /**
     * A source that lists the currently open project windows (key = window session id,
     * name = project metadata name, path = project work directory).
     */
    static ProjectSource openWindows() {
        return () -> {
            List<MCPProjectEntry> projects = new ArrayList<>();
            for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
                MCPProjectEntry entry = new MCPProjectEntry();
                entry.setKey(window.getSessionId().toString());
                entry.setName(window.getProject() != null && window.getProject().getMetadata() != null
                        ? window.getProject().getMetadata().getName() : "Untitled");
                entry.setPath(window.getProject() != null && window.getProject().getWorkDirectory() != null
                        ? window.getProject().getWorkDirectory().toString() : null);
                projects.add(entry);
            }
            return projects;
        };
    }
}
