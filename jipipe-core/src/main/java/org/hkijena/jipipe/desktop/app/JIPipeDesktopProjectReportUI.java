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

package org.hkijena.jipipe.desktop.app;

import com.google.common.collect.Sets;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectDirectories;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunnableQueueButton;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeDesktopProjectReportUI extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private final ReportSettings reportSettings = new ReportSettings();
    private final JIPipeDesktopMarkdownReader markdownReader = new JIPipeDesktopMarkdownReader(true, new MarkdownText());
    private final JIPipeRunnableQueue queue = new JIPipeRunnableQueue("Report");

    public JIPipeDesktopProjectReportUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        reportSettings.getParameterChangedEventEmitter().subscribe(this);
        queue.getFinishedEventEmitter().subscribe(this);
        rebuildReport();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), reportSettings, new MarkdownText(), JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.DOCUMENTATION_BELOW),
                markdownReader,
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(300, true));
        add(splitPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> rebuildReport());
        markdownReader.getToolBar().add(refreshButton);
        markdownReader.getToolBar().add(new JIPipeDesktopRunnableQueueButton(getDesktopWorkbench(), queue));
        for (Component component : markdownReader.getToolBar().getComponents()) {
            if (component instanceof JButton) {
                UIUtils.setStandardButtonBorder((AbstractButton) component);
            }
        }

    }

    private void rebuildReport() {
        queue.cancelAll();
        queue.enqueue(new RebuildReportRun(getDesktopProjectWorkbench().getProject(), reportSettings));
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof RebuildReportRun) {
            markdownReader.setDocument(new MarkdownText(((RebuildReportRun) event.getRun()).stringBuilder.toString()));
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        rebuildReport();
    }

    public static class RebuildReportRun extends AbstractJIPipeRunnable {

        private final Escaper escaper = HtmlEscapers.htmlEscaper();
        private final JIPipeProject project;
        private final ReportSettings reportSettings;
        private final StringBuilder stringBuilder = new StringBuilder();

        public RebuildReportRun(JIPipeProject project, ReportSettings reportSettings) {
            this.project = project;
            this.reportSettings = reportSettings;
        }

        public StringBuilder getStringBuilder() {
            return stringBuilder;
        }

        @Override
        public String getTaskLabel() {
            return "Rebuild report";
        }

        @Override
        public void run() {

            final String NEW_LINE = "<br/>";
            if (reportSettings.isAddTitle()) {
                stringBuilder.append("<h1>").append(StringUtils.orElse(project.getMetadata().getName(), "JIPipe project")).append("</h1>");
            }
            if (reportSettings.isAddAuthors() && !project.getMetadata().getAuthors().isEmpty()) {
                JIPipeAuthorMetadata.List authorsList = project.getMetadata().getAuthors();
                renderAuthorsList(authorsList);
            }
            if (reportSettings.isAddLicence() && !StringUtils.isNullOrEmpty(project.getMetadata().getLicense())) {
                stringBuilder.append("<h2>License</h2>");
                stringBuilder.append(escaper.escape(project.getMetadata().getLicense()));
                stringBuilder.append(NEW_LINE);
            }
            if (reportSettings.isAddWebsite() && !StringUtils.isNullOrEmpty(project.getMetadata().getWebsite())) {
                stringBuilder.append("<h2>Website</h2>");
                stringBuilder.append("<a href=\"").append(project.getMetadata().getWebsite()).append("\">");
                stringBuilder.append(escaper.escape(project.getMetadata().getWebsite()));
                stringBuilder.append("</a>");
                stringBuilder.append(NEW_LINE);
            }
            if (reportSettings.isAddCitation() && !StringUtils.isNullOrEmpty(project.getMetadata().getCitation())) {
                stringBuilder.append("<h2>Citation (this project)</h2>");
                stringBuilder.append(escaper.escape(project.getMetadata().getCitation()));
                stringBuilder.append(NEW_LINE);
            }
            if (reportSettings.isAddSummary() && !StringUtils.isNullOrEmpty(project.getMetadata().getSummary().getBody())) {
                stringBuilder.append("<h2>Summary</h2>");
                stringBuilder.append(project.getMetadata().getSummary().getBody());
                stringBuilder.append(NEW_LINE);
            }
            if (reportSettings.isAddAcknowledgements() && !project.getMetadata().getAcknowledgements().isEmpty()) {
                stringBuilder.append("<h2>Acknowledgements</h2>");
                JIPipeAuthorMetadata.List authorsList = project.getMetadata().getAcknowledgements();
                renderAuthorsList(authorsList);
            }
            if (reportSettings.isAddDescription() && !StringUtils.isNullOrEmpty(project.getMetadata().getDescription().getBody())) {
                stringBuilder.append("<h2>Description</h2>");
                String body = project.getMetadata().getDescription().getBody()
                        .replace("<h1>", "<h3>")
                        .replace("<h2>", "<h4>")
                        .replace("</h1>", "</h3>")
                        .replace("</h2>", "</h4>")
                        .replace("\n", "");
                stringBuilder.append(body);
                stringBuilder.append(NEW_LINE);
            }
            if (reportSettings.isAddListOfUsedNodes()) {
                renderListOfUtilizedNodes();
            }
            if (reportSettings.isAddDependencies()) {
                renderDependencies();
            }
            if (reportSettings.isAddDependencyCitations()) {
                renderDependencyCitations();
            }
            if (reportSettings.isAddUserDirectories()) {
                renderUserDirectories();
            }
            if (reportSettings.isAddGlobalParameters()) {
                renderGlobalParameters();
            }
            if (reportSettings.isAddPipelineTextDescription()) {
                renderPipelineTextDescription();
            }
        }

        private void renderGlobalParameters() {
            if (!project.getMetadata().getGlobalParameters().getParameters().isEmpty()) {
                stringBuilder.append("<h2>Project-wide parameters</h2>");
                stringBuilder.append("<table>");
                stringBuilder.append("<tr><th>Key</th><th>Name</th><th>Description</th><th>Value</th></tr>");
                for (Map.Entry<String, JIPipeParameterAccess> entry : project.getMetadata().getGlobalParameters().getParameters().entrySet()) {
                    stringBuilder.append("<tr>");
                    stringBuilder.append("<td>").append(entry.getKey()).append("</td>");
                    stringBuilder.append("<td>").append(entry.getValue().getName()).append("</td>");
                    stringBuilder.append("<td>").append(entry.getValue().getDescription()).append("</td>");
                    stringBuilder.append("<td>").append(HtmlEscapers.htmlEscaper().escape(JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(entry.getValue().get(Object.class)))).append("</td>");
                    stringBuilder.append("</tr>");
                }
                stringBuilder.append("</table>");
            }

        }

        private void renderUserDirectories() {
            List<JIPipeProjectDirectories.DirectoryEntry> directories = project.getMetadata().getDirectories().getDirectoriesAsInstance();
            if (!directories.isEmpty()) {
                stringBuilder.append("<h2>Project-wide directories</h2>");
                stringBuilder.append("<table>");
                stringBuilder.append("<tr><th>Key</th><th>Name</th><th>Description</th><th>Path</th><th>Must exist</th></tr>");
                for (JIPipeProjectDirectories.DirectoryEntry directoryEntry : directories) {
                    stringBuilder.append("<tr>");
                    stringBuilder.append("<td>").append(directoryEntry.getKey()).append("</td>");
                    stringBuilder.append("<td>").append(directoryEntry.getName()).append("</td>");
                    stringBuilder.append("<td>").append(directoryEntry.getDescription()).append("</td>");
                    stringBuilder.append("<td>").append(directoryEntry.getPath()).append("</td>");
                    stringBuilder.append("<td>").append(directoryEntry.isMustExist() ? "Yes" : "No").append("</td>");
                    stringBuilder.append("</tr>");
                }
                stringBuilder.append("</table>");
            }
        }

        private void renderPipelineTextDescription() {
            stringBuilder.append("<h2>Pipeline text description</h2>");
            StringBuilder tempBuilder = new StringBuilder();
            project.getTextDescription(tempBuilder, 3);
            stringBuilder.append(tempBuilder.toString().replace("</ul><ul>", ""));
        }

        private void renderDependencyCitations() {
            stringBuilder.append("<h2>Citations (dependencies)</h2>");

            Set<String> citations = new HashSet<>(project.getMetadata().getDependencyCitations());

            // Collect dependencies
            for (JIPipeDependency dependency : project.getSimplifiedMinimalDependencies()) {
                JIPipePlugin fullInstance = JIPipe.getInstance().getPluginRegistry().getKnownPluginById(dependency.getDependencyId());
                if (!StringUtils.isNullOrEmpty(fullInstance.getMetadata().getCitation())) {
                    citations.add(fullInstance.getMetadata().getCitation());
                }
                for (String citation : fullInstance.getMetadata().getDependencyCitations()) {
                    if (!StringUtils.isNullOrEmpty(citation)) {
                        citations.add(citation);
                    }
                }
            }

            // Collect nodes
            Set<JIPipeNodeInfo> nodeInfos = new HashSet<>();
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                nodeInfos.add(graphNode.getInfo());
            }
            for (JIPipeNodeInfo nodeInfo : nodeInfos) {
                citations.addAll(nodeInfo.getAdditionalCitations());
            }

            stringBuilder.append("<ul>");
            for (String citation : citations) {
                stringBuilder.append("<li>").append(escaper.escape(citation)).append("</li>");
            }
            stringBuilder.append("</ul>");
        }

        private void renderDependencies() {
            stringBuilder.append("<h2>Dependencies</h2>");
            stringBuilder.append("<h3>JIPipe extensions</h3>");
            stringBuilder.append("<table>");
            stringBuilder.append("<tr><th>Name</th><th>Version</th><th>Author(s)</th></tr>");
            for (JIPipeDependency dependency : project.getSimplifiedMinimalDependencies()) {
                JIPipePlugin fullInstance = JIPipe.getInstance().getPluginRegistry().getKnownPluginById(dependency.getDependencyId());
                stringBuilder.append("<tr>");
                stringBuilder.append("<td><strong>").append(escaper.escape(fullInstance.getMetadata().getName())).append("</strong></td>");
                stringBuilder.append("<td>").append(escaper.escape(fullInstance.getDependencyVersion())).append("</td>");
                stringBuilder.append("<td>").append(escaper.escape(fullInstance.getMetadata().getAuthors().stream().map(JIPipeAuthorMetadata::toString).collect(Collectors.joining(", ")))).append("</td>");
                stringBuilder.append("</tr>");
            }
            stringBuilder.append("</table>");

            stringBuilder.append("<h3>ImageJ update sites</h3>");
            stringBuilder.append("<table>");
            stringBuilder.append("<tr><th>Name</th><th>URL</th></tr>");
            for (JIPipeDependency dependency : project.getSimplifiedMinimalDependencies()) {
                JIPipePlugin fullInstance = JIPipe.getInstance().getPluginRegistry().getKnownPluginById(dependency.getDependencyId());
                for (JIPipeImageJUpdateSiteDependency siteDependency : fullInstance.getImageJUpdateSiteDependencies()) {
                    stringBuilder.append("<tr>");
                    stringBuilder.append("<td><strong>").append(escaper.escape(siteDependency.getName())).append("</strong></td>");
                    stringBuilder.append("<td>").append(escaper.escape(siteDependency.getUrl())).append("</td>");
                    stringBuilder.append("</tr>");
                }
            }
            stringBuilder.append("</table>");

            List<JIPipeEnvironment> externalEnvironments = new ArrayList<>();
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                graphNode.getEnvironmentDependencies(externalEnvironments);
            }
            if (!externalEnvironments.isEmpty()) {
                stringBuilder.append("<h3>External environments</h3>");
                stringBuilder.append("<table>");
                stringBuilder.append("<tr><th>Type</th><th>Name</th><th>Version</th><th>Source/URL</th></tr>");
                for (JIPipeEnvironment externalEnvironment : Sets.newHashSet(externalEnvironments)) {
                    stringBuilder.append("<tr>");
                    stringBuilder.append("<td>").append(escaper.escape(externalEnvironment.getClass().getSimpleName())).append("</td>");
                    stringBuilder.append("<td><strong>").append(escaper.escape(externalEnvironment.getName())).append("</strong></td>");
                    stringBuilder.append("<td>").append(escaper.escape(externalEnvironment.getVersion())).append("</td>");
                    stringBuilder.append("<td>").append(escaper.escape(externalEnvironment.getSource())).append("</td>");
                    stringBuilder.append("</tr>");
                }
                stringBuilder.append("</table>");
            }
        }

        private void renderListOfUtilizedNodes() {
            stringBuilder.append("<h2>Utilized nodes</h2>");
            stringBuilder.append("<table>");
            stringBuilder.append("<tr><th>Name</th><th>Description</th><th>Signature</th></tr>");
            Set<JIPipeNodeInfo> nodeInfos = new HashSet<>();
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                nodeInfos.add(graphNode.getInfo());
            }
            nodeInfos.stream().sorted(Comparator.comparing(JIPipeNodeInfo::getName)).forEach(info -> {
                stringBuilder.append("<tr>");
                stringBuilder.append("<td><strong>").append(escaper.escape(info.getName())).append("</strong></td>");
                stringBuilder.append("<td>").append(info.getDescription().getBody()).append("</td>");
                stringBuilder.append("<td>");
                {
                    stringBuilder.append("(");
                    stringBuilder.append(info.getInputSlots().stream().map(slot -> "<i>" + JIPipeDataInfo.getInstance(slot.value()).getName() + "</i> " +
                            StringUtils.orElse(slot.name(), "[unnamed]")).collect(Collectors.joining(", ")));
                    stringBuilder.append(")");
                }
                stringBuilder.append(" -&gt; ");
                {
                    stringBuilder.append("(");
                    stringBuilder.append(info.getOutputSlots().stream().map(slot -> "<i>" + JIPipeDataInfo.getInstance(slot.value()).getName() + "</i> " +
                            StringUtils.orElse(slot.name(), "[unnamed]")).collect(Collectors.joining(", ")));
                    stringBuilder.append(")");
                }
                stringBuilder.append("</td>");
                stringBuilder.append("</tr>");
            });
            stringBuilder.append("</table>");
        }

        private void renderAuthorsList(JIPipeAuthorMetadata.List authorsList) {
            final String NEW_LINE = "<br/>";
            List<String> affiliations = new ArrayList<>();
            for (JIPipeAuthorMetadata author : authorsList) {
                for (String affiliation : author.getAffiliations()) {
                    if (!affiliations.contains(affiliation)) {
                        affiliations.add(affiliation);
                    }
                }
            }
            stringBuilder.append("<p>");
            for (int i = 0; i < authorsList.size(); i++) {
                if (i > 0) {
                    stringBuilder.append(NEW_LINE);
                }
                JIPipeAuthorMetadata author = authorsList.get(i);
                stringBuilder.append(escaper.escape(author.toString()));
                if (!author.getAffiliations().isEmpty()) {
                    stringBuilder.append(" [");
                    StringList authorAffiliations = author.getAffiliations();
                    for (int j = 0; j < authorAffiliations.size(); j++) {
                        String affiliation = authorAffiliations.get(j);
                        if (j > 0) {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(affiliations.indexOf(affiliation) + 1);
                    }
                    stringBuilder.append("]");
                }
            }
            stringBuilder.append("</p>");
            stringBuilder.append(NEW_LINE);
            stringBuilder.append("<p>");
            for (int i = 0; i < affiliations.size(); i++) {
                String affiliation = affiliations.get(i);
                if (i > 0) {
                    stringBuilder.append(NEW_LINE);
                }
                stringBuilder.append("[").append(affiliations.indexOf(affiliation) + 1).append("] ").append(escaper.escape(affiliation));
            }
            stringBuilder.append("</p>");
            stringBuilder.append(NEW_LINE);
        }
    }

    public static class ReportSettings extends AbstractJIPipeParameterCollection {
        private boolean addTitle = true;
        private boolean addAuthors = true;
        private boolean addDescription = true;
        private boolean addSummary = true;
        private boolean addWebsite = true;
        private boolean addCitation = true;
        private boolean addLicence = true;
        private boolean addAcknowledgements = true;
        private boolean addDependencies = true;
        private boolean addDependencyCitations = true;
        private boolean addListOfUsedNodes = true;
        private boolean addPipelineTextDescription = true;
        private boolean addGlobalParameters = true;
        private boolean addUserDirectories = true;

        @SetJIPipeDocumentation(name = "Project-wide parameters", description = "If enabled, summarize the project-wide parameters")
        @JIPipeParameter("add-global-parameters")
        public boolean isAddGlobalParameters() {
            return addGlobalParameters;
        }

        @JIPipeParameter("add-global-parameters")
        public void setAddGlobalParameters(boolean addGlobalParameters) {
            this.addGlobalParameters = addGlobalParameters;
        }

        @SetJIPipeDocumentation(name = "User directories", description = "If enabled, add the user directories")
        @JIPipeParameter("add-user-directories")
        public boolean isAddUserDirectories() {
            return addUserDirectories;
        }

        @JIPipeParameter("add-user-directories")
        public void setAddUserDirectories(boolean addUserDirectories) {
            this.addUserDirectories = addUserDirectories;
        }

        @SetJIPipeDocumentation(name = "Title", description = "If enabled, add the project name")
        @JIPipeParameter(value = "add-title", uiOrder = -100)
        public boolean isAddTitle() {
            return addTitle;
        }

        @JIPipeParameter("add-title")
        public void setAddTitle(boolean addTitle) {
            this.addTitle = addTitle;
        }

        @SetJIPipeDocumentation(name = "Authors", description = "If enabled, add the project authors")
        @JIPipeParameter(value = "add-authors", uiOrder = -99)
        public boolean isAddAuthors() {
            return addAuthors;
        }

        @JIPipeParameter("add-authors")
        public void setAddAuthors(boolean addAuthors) {
            this.addAuthors = addAuthors;
        }

        @SetJIPipeDocumentation(name = "Description", description = "If enabled, add the project description")
        @JIPipeParameter(value = "add-description", uiOrder = -94)
        public boolean isAddDescription() {
            return addDescription;
        }

        @JIPipeParameter("add-description")
        public void setAddDescription(boolean addDescription) {
            this.addDescription = addDescription;
        }

        @SetJIPipeDocumentation(name = "Summary", description = "If enabled, add the project summary")
        @JIPipeParameter(value = "add-summary", uiOrder = -95)
        public boolean isAddSummary() {
            return addSummary;
        }

        @JIPipeParameter("add-summary")
        public void setAddSummary(boolean addSummary) {
            this.addSummary = addSummary;
        }

        @SetJIPipeDocumentation(name = "Website", description = "If enabled, add the website URL")
        @JIPipeParameter(value = "add-website", uiOrder = -97)
        public boolean isAddWebsite() {
            return addWebsite;
        }

        @JIPipeParameter("add-website")
        public void setAddWebsite(boolean addWebsite) {
            this.addWebsite = addWebsite;
        }

        @SetJIPipeDocumentation(name = "Citation (project)", description = "If enabled, add the project citation field")
        @JIPipeParameter(value = "add-citation", uiOrder = -96)
        public boolean isAddCitation() {
            return addCitation;
        }

        @JIPipeParameter("add-citation")
        public void setAddCitation(boolean addCitation) {
            this.addCitation = addCitation;
        }

        @SetJIPipeDocumentation(name = "License", description = "If enabled, add the project license")
        @JIPipeParameter(value = "add-license", uiOrder = -98)
        public boolean isAddLicence() {
            return addLicence;
        }

        @JIPipeParameter("add-license")
        public void setAddLicence(boolean addLicence) {
            this.addLicence = addLicence;
        }

        @SetJIPipeDocumentation(name = "Acknowledgements", description = "If enabled, add the project acknowledgements")
        @JIPipeParameter(value = "add-acknowledgements", uiOrder = -94)
        public boolean isAddAcknowledgements() {
            return addAcknowledgements;
        }

        @JIPipeParameter("add-acknowledgements")
        public void setAddAcknowledgements(boolean addAcknowledgements) {
            this.addAcknowledgements = addAcknowledgements;
        }

        @SetJIPipeDocumentation(name = "Dependencies", description = "If enabled, add the project dependencies")
        @JIPipeParameter(value = "add-dependencies", uiOrder = -92)
        public boolean isAddDependencies() {
            return addDependencies;
        }

        @JIPipeParameter("add-dependencies")
        public void setAddDependencies(boolean addDependencies) {
            this.addDependencies = addDependencies;
        }

        @SetJIPipeDocumentation(name = "Citations (dependencies)", description = "If enabled, add citations associated to dependencies")
        @JIPipeParameter(value = "add-dependency-citations", uiOrder = -91)
        public boolean isAddDependencyCitations() {
            return addDependencyCitations;
        }

        @JIPipeParameter("add-dependency-citations")
        public void setAddDependencyCitations(boolean addDependencyCitations) {
            this.addDependencyCitations = addDependencyCitations;
        }

        @SetJIPipeDocumentation(name = "List of utilized nodes", description = "If enabled, generate a list of all utilized nodes")
        @JIPipeParameter(value = "add-list-of-used-nodes", uiOrder = -93)
        public boolean isAddListOfUsedNodes() {
            return addListOfUsedNodes;
        }

        @JIPipeParameter("add-list-of-used-nodes")
        public void setAddListOfUsedNodes(boolean addListOfUsedNodes) {
            this.addListOfUsedNodes = addListOfUsedNodes;
        }

        @SetJIPipeDocumentation(name = "Text description of the pipeline", description = "If enabled, generate a text description of the pipeline")
        @JIPipeParameter(value = "add-pipeline-text-description", uiOrder = -90)
        public boolean isAddPipelineTextDescription() {
            return addPipelineTextDescription;
        }

        @JIPipeParameter("add-pipeline-text-description")
        public void setAddPipelineTextDescription(boolean addPipelineTextDescription) {
            this.addPipelineTextDescription = addPipelineTextDescription;
        }
    }
}
