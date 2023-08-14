package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.api.*;
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
import java.util.ArrayList;
import java.util.List;

public class JIPipeProjectReportUI extends JIPipeProjectWorkbenchPanel implements JIPipeRunnable.FinishedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {

    private final ReportSettings reportSettings = new ReportSettings();
    private final MarkdownReader markdownReader = new MarkdownReader(true);
    private final JIPipeRunnerQueue queue = new JIPipeRunnerQueue("Report");

    public JIPipeProjectReportUI(JIPipeProjectWorkbench workbench) {
        super(workbench);
        initialize();
        reportSettings.getParameterChangedEventEmitter().subscribe(this);
        queue.getFinishedEventEmitter().subscribe(this);
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
            final String NEW_BLOCK = "\n\n";
            if(reportSettings.isAddTitle()) {
                stringBuilder.append("# ").append(StringUtils.orElse(project.getMetadata().getName(), "JIPipe project")).append(NEW_BLOCK);
            }
            if(reportSettings.addAuthors && !project.getMetadata().getAuthors().isEmpty()) {
                List<String> affiliations = new ArrayList<>();
                for (JIPipeAuthorMetadata author : project.getMetadata().getAuthors()) {
                    for (String affiliation : author.getAffiliations()) {
                        if(!affiliations.contains(affiliation)) {
                            affiliations.add(affiliation);
                        }
                    }
                }
                JIPipeAuthorMetadata.List authors = project.getMetadata().getAuthors();
                for (int i = 0; i < authors.size(); i++) {
                    JIPipeAuthorMetadata author = authors.get(i);
                    stringBuilder.append(author.toString());
                    if (!author.getAffiliations().isEmpty()) {
                        stringBuilder.append(" [");
                        StringList authorAffiliations = author.getAffiliations();
                        for (int j = 0; j < authorAffiliations.size(); j++) {
                            String affiliation = authorAffiliations.get(j);
                            if (j > 0) {
                                stringBuilder.append(", ");
                            }
                            stringBuilder.append(affiliation);
                        }
                        stringBuilder.append("]");
                    }
                    if(i > 0) {
                        stringBuilder.append("\\ ");
                    }
                }
                stringBuilder.append(NEW_BLOCK);
            }

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
        @JIPipeParameter("add-title")
        public boolean isAddTitle() {
            return addTitle;
        }

        @JIPipeParameter("add-title")
        public void setAddTitle(boolean addTitle) {
            this.addTitle = addTitle;
        }

        @JIPipeDocumentation(name = "Authors",description = "If enabled, add the project authors")
        @JIPipeParameter("add-authors")
        public boolean isAddAuthors() {
            return addAuthors;
        }

        @JIPipeParameter("add-authors")
        public void setAddAuthors(boolean addAuthors) {
            this.addAuthors = addAuthors;
        }

        @JIPipeDocumentation(name = "Description", description = "If enabled, add the project description")
        @JIPipeParameter("add-description")
        public boolean isAddDescription() {
            return addDescription;
        }

        @JIPipeParameter("add-description")
        public void setAddDescription(boolean addDescription) {
            this.addDescription = addDescription;
        }

        @JIPipeDocumentation(name = "Summary", description = "If enabled, add the project summary")
        @JIPipeParameter("add-summary")
        public boolean isAddSummary() {
            return addSummary;
        }

        @JIPipeParameter("add-summary")
        public void setAddSummary(boolean addSummary) {
            this.addSummary = addSummary;
        }

        @JIPipeDocumentation(name = "Website", description = "If enabled, add the website URL")
        @JIPipeParameter("add-website")
        public boolean isAddWebsite() {
            return addWebsite;
        }

        @JIPipeParameter("add-website")
        public void setAddWebsite(boolean addWebsite) {
            this.addWebsite = addWebsite;
        }

        @JIPipeDocumentation(name = "Citation (project)", description = "If enabled, add the project citation field")
        @JIPipeParameter("add-citation")
        public boolean isAddCitation() {
            return addCitation;
        }

        @JIPipeParameter("add-citation")
        public void setAddCitation(boolean addCitation) {
            this.addCitation = addCitation;
        }

        @JIPipeDocumentation(name = "License", description = "If enabled, add the project license")
        @JIPipeParameter("add-license")
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
        @JIPipeParameter("add-dependencies")
        public boolean isAddDependencies() {
            return addDependencies;
        }

        @JIPipeParameter("add-dependencies")
        public void setAddDependencies(boolean addDependencies) {
            this.addDependencies = addDependencies;
        }

        @JIPipeDocumentation(name = "Citations (dependencies)", description = "If enabled, add citations associated to dependencies")
        @JIPipeParameter("add-dependency-citations")
        public boolean isAddDependencyCitations() {
            return addDependencyCitations;
        }

        @JIPipeParameter("add-dependency-citations")
        public void setAddDependencyCitations(boolean addDependencyCitations) {
            this.addDependencyCitations = addDependencyCitations;
        }

        @JIPipeDocumentation(name = "List of utilized nodes", description = "If enabled, generate a list of all utilized nodes")
        @JIPipeParameter("add-list-of-used-nodes")
        public boolean isAddListOfUsedNodes() {
            return addListOfUsedNodes;
        }

        @JIPipeParameter("add-list-of-used-nodes")
        public void setAddListOfUsedNodes(boolean addListOfUsedNodes) {
            this.addListOfUsedNodes = addListOfUsedNodes;
        }

        @JIPipeDocumentation(name = "Text description of the pipeline", description = "If enabled, generate a text description of the pipeline")
        @JIPipeParameter("add-pipeline-text-description")
        public boolean isAddPipelineTextDescription() {
            return addPipelineTextDescription;
        }

        @JIPipeParameter("add-pipeline-text-description")
        public void setAddPipelineTextDescription(boolean addPipelineTextDescription) {
            this.addPipelineTextDescription = addPipelineTextDescription;
        }
    }
}
