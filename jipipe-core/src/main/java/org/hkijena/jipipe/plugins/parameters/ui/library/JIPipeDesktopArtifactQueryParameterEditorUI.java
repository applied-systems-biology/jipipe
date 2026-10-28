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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumItemInfoRenderTarget;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterItemInfo;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameterSettings;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parameter editor for {@link JIPipeArtifactQueryParameter}.
 * <p>
 * #1339 item 1: the flat artifact picker is now a
 * {@link JIPipeDesktopRichSelectionBox} — the searchable "new combobox" —
 * instead of a display button + modal enum-pick dialog. The parameter still
 * stores the artifact's full id; the box matches it back to a
 * {@link JIPipeArtifact} for display.
 */
public class JIPipeDesktopArtifactQueryParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeArtifactQueryParameter> {

    private final JIPipeDesktopRichSelectionBox selectionBox;
    private final List<JIPipeArtifact> availableArtifacts = new ArrayList<>();

    public JIPipeDesktopArtifactQueryParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeArtifactQueryParameter.class, parameters);
        selectionBox = new JIPipeDesktopRichSelectionBox("Select artifact", false,
                this::options,
                selected -> {
                    if (selected.isEmpty()) {
                        setParameter(new JIPipeArtifactQueryParameter(), true);
                    } else {
                        setParameter(new JIPipeArtifactQueryParameter(((JIPipeArtifact) selected.get(0).getValue()).getFullId()), true);
                    }
                });
        initialize();
        reloadArtifacts();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(selectionBox, BorderLayout.CENTER);
    }

    private void reloadArtifacts() {
        String[] filters = {"*"};
        JIPipeArtifactQueryParameterSettings annotation = getParameterAccess().getAnnotationOfType(JIPipeArtifactQueryParameterSettings.class);
        if (annotation != null) {
            filters = annotation.getFilters();
        }
        availableArtifacts.clear();
        List<JIPipeArtifact> cachedArtifacts = JIPipe.getArtifacts().queryCachedArtifacts(filters);
        Set<String> alreadyAdded = new HashSet<>();
        for (JIPipeArtifact artifact : cachedArtifacts) {
            String versionPinId = artifact.getFullId(JIPipeArtifact.ResolutionStatus.GroupNameVersion);
            if (!alreadyAdded.contains(versionPinId)) {
                JIPipeArtifact artifact1 = new JIPipeArtifact(artifact);
                artifact1.setClassifier("*");
                availableArtifacts.add(artifact1);
                alreadyAdded.add(versionPinId);
            }
        }
        availableArtifacts.addAll(cachedArtifacts);
    }

    private List<JIPipeDesktopRichSelectionBox.Option<?>> options() {
        ArtifactEnumItemInfo info = new ArtifactEnumItemInfo();
        List<JIPipeArtifact> sorted = new ArrayList<>(availableArtifacts);
        sorted.sort(Comparator.comparing(a -> info.getLabel(a, JIPipeEnumItemInfoRenderTarget.List), String.CASE_INSENSITIVE_ORDER));
        List<JIPipeDesktopRichSelectionBox.Option<?>> result = new ArrayList<>();
        for (JIPipeArtifact artifact : sorted) {
            result.add(new JIPipeDesktopRichSelectionBox.Option<>(artifact,
                    info.getLabel(artifact, JIPipeEnumItemInfoRenderTarget.List),
                    info.getIcon(artifact, JIPipeEnumItemInfoRenderTarget.List),
                    null,
                    info.getTooltip(artifact, JIPipeEnumItemInfoRenderTarget.List)));
        }
        return result;
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        String query = getParameter().getQuery();
        JIPipeArtifact match = null;
        if (query != null && !query.isEmpty()) {
            for (JIPipeArtifact artifact : availableArtifacts) {
                if (artifact.getFullId().equals(query)) {
                    match = artifact;
                    break;
                }
            }
        }
        selectionBox.setSelectedValues(match == null ? List.of() : List.of(match));
        selectionBox.refreshDisplay();
    }

    public static class ArtifactEnumItemInfo implements JIPipeEnumParameterItemInfo {

        @Override
        public Icon getIcon(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
            return JIPipe.RESOURCES.getIcon16("actions/run-install.png");
        }

        @Override
        public String getLabel(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
            if (value instanceof JIPipeArtifact) {
                return ((JIPipeArtifact) value).getFullId();
            }
            return "<None>";
        }

        @Override
        public String getTooltip(Object value, JIPipeEnumItemInfoRenderTarget renderTarget) {
            if (value instanceof JIPipeArtifact) {
                return "<html>Name: " + ((JIPipeArtifact) value).getArtifactId() + "<br/>"
                        + "Publisher: " + ((JIPipeArtifact) value).getGroupId() + "<br/>"
                        + "Version: " + ((JIPipeArtifact) value).getVersion() + "<br/>"
                        + "Classifier: " + ((JIPipeArtifact) value).getClassifier() + "</html>";
            }
            return "";
        }
    }
}