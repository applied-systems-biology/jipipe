package org.hkijena.jipipe.ui;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeProjectReportUI extends JIPipeProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    public static final List<String> CSS_RULES = Arrays.asList("body { font-family: \"Sans-serif\"; }",
            "pre { background-color: #f5f2f0; border: 3px #f5f2f0 solid; }",
            "code { background-color: #ffffff; border: none; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "tr { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }");
    public static final List<String> CSS_RULES_DARK = Arrays.asList("body { font-family: \"Sans-serif\"; color: #eeeeee; }",
            "pre { background-color: #333333; border: 3px #333333 solid; }",
            "code { background-color: #121212; border: none; }",
            "a { color: #65a4e3; }",
            "h2 { padding-top: 30px; }",
            "h3 { padding-top: 30px; }",
            "tr { border-bottom: 1px solid #c8c8c8; }",
            ".toc-list { list-style: none; }");

    private final ReportSettings reportSettings = new ReportSettings();
    private final MarkdownReader markdownReader = new MarkdownReader(true, new MarkdownDocument(), CSS_RULES, CSS_RULES_DARK);
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Report");

    public JIPipeProjectReportUI(JIPipeProjectWorkbench workbench) {
        super(workbench);
        initialize();
        reportSettings.getParameterChangedEventEmitter().subscribe(this);
        queue.getFinishedEventEmitter().subscribe(this);
        rebuildReport();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                new ParameterPanel(getWorkbench(), reportSettings, new MarkdownDocument(), ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.DOCUMENTATION_BELOW),
                markdownReader,
                AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> rebuildReport());
        markdownReader.getToolBar().add(refreshButton);
    }

    private void rebuildReport() {
        queue.cancelAll();
        queue.enqueue(new RebuildReportRun(getProjectWorkbench().getProject(), reportSettings));
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if(event.getRun() instanceof RebuildReportRun) {
            markdownReader.setDocument(new MarkdownDocument(((RebuildReportRun) event.getRun()).stringBuilder.toString()));
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
            if(reportSettings.isAddTitle()) {
                stringBuilder.append("<h1>").append(StringUtils.orElse(project.getMetadata().getName(), "JIPipe project")).append("</h1>");
            }
            if(reportSettings.isAddAuthors() && !project.getMetadata().getAuthors().isEmpty()) {
                JIPipeAuthorMetadata.List authorsList = project.getMetadata().getAuthors();
                renderAuthorsList(authorsList);
            }
            if(reportSettings.isAddLicence() && !StringUtils.isNullOrEmpty(project.getMetadata().getLicense())) {
                stringBuilder.append("<h2>License</h2>");
                stringBuilder.append(escaper.escape(project.getMetadata().getLicense()));
                stringBuilder.append(NEW_LINE);
            }
            if(reportSettings.isAddWebsite() && !StringUtils.isNullOrEmpty(project.getMetadata().getWebsite())) {
                stringBuilder.append("<h2>Website</h2>");
                stringBuilder.append("<a href=\"").append(project.getMetadata().getWebsite()).append("\">");
                stringBuilder.append(escaper.escape(project.getMetadata().getWebsite()));
                stringBuilder.append("</a>");
                stringBuilder.append(NEW_LINE);
            }
            if(reportSettings.isAddCitation() && !StringUtils.isNullOrEmpty(project.getMetadata().getCitation())) {
                stringBuilder.append("<h2>Citation (this project)</h2>");
                stringBuilder.append(escaper.escape(project.getMetadata().getCitation()));
                stringBuilder.append(NEW_LINE);
            }
            if(reportSettings.isAddSummary() && !StringUtils.isNullOrEmpty(project.getMetadata().getSummary().getBody())) {
                stringBuilder.append("<h2>Summary</h2>");
                stringBuilder.append(project.getMetadata().getSummary().getBody());
                stringBuilder.append(NEW_LINE);
            }
            if(reportSettings.isAddAcknowledgements() && !project.getMetadata().getAcknowledgements().isEmpty()) {
                stringBuilder.append("<h2>Acknowledgements</h2>");
                JIPipeAuthorMetadata.List authorsList = project.getMetadata().getAcknowledgements();
                renderAuthorsList(authorsList);
            }
            if(reportSettings.isAddDescription() && !StringUtils.isNullOrEmpty(project.getMetadata().getDescription().getBody())) {
                stringBuilder.append("<h2>Description</h2>");
                stringBuilder.append(project.getMetadata().getDescription().getBody()
                        .replace("<h1>", "<h3>")
                        .replace("<h2>", "<h4>")
                        .replace("</h1>", "</h3>")
                        .replace("</h2>", "</h4>"));
                stringBuilder.append(NEW_LINE);
            }
            if(reportSettings.isAddListOfUsedNodes()) {
                renderListOfUtilizedNodes();
            }
            if(reportSettings.isAddDependencies()) {
                renderDependencies();
            }
            if(reportSettings.isAddDependencyCitations()) {
                renderDependencyCitations();
            }
            if(reportSettings.isAddPipelineTextDescription()) {
                renderPipelineTextDescription();
            }
        }

        private void renderPipelineTextDescription() {
            stringBuilder.append("<h2>Pipeline text description</h2>");
            project.getTextDescription(stringBuilder, 3);
        }

        private void renderDependencyCitations() {
            stringBuilder.append("<h2>Citations (dependencies)</h2>");
            Set<String> citations = new HashSet<>();

            // Collect dependencies
            for (JIPipeDependency dependency : project.getSimplifiedMinimalDependencies()) {
                JIPipeExtension fullInstance = JIPipe.getInstance().getExtensionRegistry().getKnownExtensionById(dependency.getDependencyId());
                if(!StringUtils.isNullOrEmpty(fullInstance.getMetadata().getCitation())) {
                    citations.add(fullInstance.getMetadata().getCitation());
                }
                for (String citation : fullInstance.getMetadata().getDependencyCitations()) {
                    if(!StringUtils.isNullOrEmpty(citation)) {
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
                JIPipeExtension fullInstance = JIPipe.getInstance().getExtensionRegistry().getKnownExtensionById(dependency.getDependencyId());
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
                JIPipeExtension fullInstance = JIPipe.getInstance().getExtensionRegistry().getKnownExtensionById(dependency.getDependencyId());
                for (JIPipeImageJUpdateSiteDependency siteDependency : fullInstance.getImageJUpdateSiteDependencies()) {
                    stringBuilder.append("<tr>");
                    stringBuilder.append("<td><strong>").append(escaper.escape(siteDependency.getName())).append("</strong></td>");
                    stringBuilder.append("<td>").append(escaper.escape(siteDependency.getUrl())).append("</td>");
                    stringBuilder.append("</tr>");
                }
            }
            stringBuilder.append("</table>");

            List<JIPipeExternalEnvironment> externalEnvironments = new ArrayList<>();
            for (JIPipeGraphNode graphNode : project.getGraph().getGraphNodes()) {
                graphNode.getExternalEnvironments(externalEnvironments);
            }
            if(!externalEnvironments.isEmpty()) {
                stringBuilder.append("<h3>External environments</h3>");
                stringBuilder.append("<table>");
                stringBuilder.append("<tr><th>Type</th><th>Name</th><th>Version</th><th>Source/URL</th></tr>");
                for (JIPipeExternalEnvironment externalEnvironment : externalEnvironments) {
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
                            StringUtils.orElse(slot.slotName(), "[unnamed]")).collect(Collectors.joining(", ")));
                    stringBuilder.append(")");
                }
                stringBuilder.append(" -&gt; ");
                {
                    stringBuilder.append("(");
                    stringBuilder.append(info.getOutputSlots().stream().map(slot -> "<i>" + JIPipeDataInfo.getInstance(slot.value()).getName() + "</i> " +
                            StringUtils.orElse(slot.slotName(), "[unnamed]")).collect(Collectors.joining(", ")));
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
                    if(!affiliations.contains(affiliation)) {
                        affiliations.add(affiliation);
                    }
                }
            }
            stringBuilder.append("<p>");
            for (int i = 0; i < authorsList.size(); i++) {
                if(i > 0) {
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
                if(i > 0) {
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

        @JIPipeDocumentation(name = "Title",description = "If enabled, add the project name")
        @JIPipeParameter(value = "add-title", uiOrder = -100)
        public boolean isAddTitle() {
            return addTitle;
        }

        @JIPipeParameter("add-title")
        public void setAddTitle(boolean addTitle) {
            this.addTitle = addTitle;
        }

        @JIPipeDocumentation(name = "Authors",description = "If enabled, add the project authors")
        @JIPipeParameter(value = "add-authors", uiOrder = -99)
        public boolean isAddAuthors() {
            return addAuthors;
        }

        @JIPipeParameter("add-authors")
        public void setAddAuthors(boolean addAuthors) {
            this.addAuthors = addAuthors;
        }

        @JIPipeDocumentation(name = "Description", description = "If enabled, add the project description")
        @JIPipeParameter(value = "add-description", uiOrder = -94)
        public boolean isAddDescription() {
            return addDescription;
        }

        @JIPipeParameter("add-description")
        public void setAddDescription(boolean addDescription) {
            this.addDescription = addDescription;
        }

        @JIPipeDocumentation(name = "Summary", description = "If enabled, add the project summary")
        @JIPipeParameter(value = "add-summary", uiOrder = -95)
        public boolean isAddSummary() {
            return addSummary;
        }

        @JIPipeParameter("add-summary")
        public void setAddSummary(boolean addSummary) {
            this.addSummary = addSummary;
        }

        @JIPipeDocumentation(name = "Website", description = "If enabled, add the website URL")
        @JIPipeParameter(value = "add-website", uiOrder = -97)
        public boolean isAddWebsite() {
            return addWebsite;
        }

        @JIPipeParameter("add-website")
        public void setAddWebsite(boolean addWebsite) {
            this.addWebsite = addWebsite;
        }

        @JIPipeDocumentation(name = "Citation (project)", description = "If enabled, add the project citation field")
        @JIPipeParameter(value = "add-citation", uiOrder = -96)
        public boolean isAddCitation() {
            return addCitation;
        }

        @JIPipeParameter("add-citation")
        public void setAddCitation(boolean addCitation) {
            this.addCitation = addCitation;
        }

        @JIPipeDocumentation(name = "License", description = "If enabled, add the project license")
        @JIPipeParameter(value = "add-license", uiOrder = -98)
        public boolean isAddLicence() {
            return addLicence;
        }

        @JIPipeParameter("add-license")
        public void setAddLicence(boolean addLicence) {
            this.addLicence = addLicence;
        }

        @JIPipeDocumentation(name = "Acknowledgements", description = "If enabled, add the project acknowledgements")
        @JIPipeParameter("add-acknowledgements")
        public boolean isAddAcknowledgements() {
            return addAcknowledgements;
        }

        @JIPipeParameter("add-acknowledgements")
        public void setAddAcknowledgements(boolean addAcknowledgements) {
            this.addAcknowledgements = addAcknowledgements;
        }

        @JIPipeDocumentation(name = "Dependencies", description = "If enabled, add the project dependencies")
        @JIPipeParameter(value = "add-dependencies", uiOrder = -92)
        public boolean isAddDependencies() {
            return addDependencies;
        }

        @JIPipeParameter("add-dependencies")
        public void setAddDependencies(boolean addDependencies) {
            this.addDependencies = addDependencies;
        }

        @JIPipeDocumentation(name = "Citations (dependencies)", description = "If enabled, add citations associated to dependencies")
        @JIPipeParameter(value = "add-dependency-citations", uiOrder = -91)
        public boolean isAddDependencyCitations() {
            return addDependencyCitations;
        }

        @JIPipeParameter("add-dependency-citations")
        public void setAddDependencyCitations(boolean addDependencyCitations) {
            this.addDependencyCitations = addDependencyCitations;
        }

        @JIPipeDocumentation(name = "List of utilized nodes", description = "If enabled, generate a list of all utilized nodes")
        @JIPipeParameter(value = "add-list-of-used-nodes", uiOrder = -93)
        public boolean isAddListOfUsedNodes() {
            return addListOfUsedNodes;
        }

        @JIPipeParameter("add-list-of-used-nodes")
        public void setAddListOfUsedNodes(boolean addListOfUsedNodes) {
            this.addListOfUsedNodes = addListOfUsedNodes;
        }

        @JIPipeDocumentation(name = "Text description of the pipeline", description = "If enabled, generate a text description of the pipeline")
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
