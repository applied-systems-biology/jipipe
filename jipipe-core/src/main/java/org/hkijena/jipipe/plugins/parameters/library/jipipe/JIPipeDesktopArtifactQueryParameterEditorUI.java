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

package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.artifacts.JIPipeArtifact;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPickEnumValueDialog;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumItemInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JIPipeDesktopArtifactQueryParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JButton button = new JButton();
    private final List<JIPipeArtifact> availableArtifacts = new ArrayList<>();

    public JIPipeDesktopArtifactQueryParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reloadArtifacts();
        reload();
    }

    private void reloadArtifacts() {
        String[] filters = {"*"};
        JIPipeArtifactQueryParameterSettings annotation = getParameterAccess().getAnnotationOfType(JIPipeArtifactQueryParameterSettings.class);
        if (annotation != null) {
            filters = annotation.getFilters();
        }
        availableArtifacts.clear();
        availableArtifacts.addAll(JIPipe.getArtifacts().queryCachedArtifacts(filters));
    }

    private void initialize() {
        setLayout(new BorderLayout());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIcon(UIUtils.getIconFromResources("actions/run-install.png"));
        button.addActionListener(e -> {
            selectArtifact();
        });
        add(button, BorderLayout.CENTER);
    }

    private void selectArtifact() {
        Object selected = JIPipeDesktopPickEnumValueDialog.showDialog(getDesktopWorkbench().getWindow(),
                availableArtifacts,
                new ArtifactEnumItemInfo(),
                null,
                "Select artifact");
        if (selected instanceof JIPipeArtifact) {
            setParameter(new JIPipeArtifactQueryParameter(((JIPipeArtifact) selected).getFullId()), true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        JIPipeArtifactQueryParameter queryParameter = getParameter(JIPipeArtifactQueryParameter.class);
        button.setText(StringUtils.orElse(queryParameter.getQuery(), "<None>"));
    }

    public static class ArtifactEnumItemInfo implements EnumItemInfo {

        @Override
        public Icon getIcon(Object value) {
            return UIUtils.getIconFromResources("actions/run-install.png");
        }

        @Override
        public String getLabel(Object value) {
            if (value instanceof JIPipeArtifact) {
                return ((JIPipeArtifact) value).getFullId();
            }
            return "<None>";
        }

        @Override
        public String getTooltip(Object value) {
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
