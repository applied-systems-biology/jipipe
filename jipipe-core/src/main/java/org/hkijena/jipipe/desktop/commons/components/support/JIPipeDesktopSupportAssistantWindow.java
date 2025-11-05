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

package org.hkijena.jipipe.desktop.commons.components.support;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopSupportAssistantWindow extends JFrame {

    public static final String SUPPORT_EMAIL = "thilo.figge@leibniz-hki.de";
    private final JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.Top);

    public JIPipeDesktopSupportAssistantWindow() {
        initialize();
    }

    public static void show(Component parent) {
        JIPipeDesktopSupportAssistantWindow window = new JIPipeDesktopSupportAssistantWindow();
        window.pack();
        window.setSize(1027, 768);
        window.setLocationRelativeTo(parent);
        window.setVisible(true);
    }

    private void initialize() {
        setTitle("JIPipe - Support");
        setIconImage(UIUtils.getJIPipeIcon128());
        setContentPane(tabPane);
        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));

        initializeWelcomeTab();
        initializeImageAnalysisTab();
        initializeFeatureRequestTab();
        initializeIssueTab();
        initializeResourcesTab();
        initializeContactTab();
    }

    private void initializeContactTab() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        addAnswerHeader("Please do not hesitate to contact us!", "We are always happy to receive questions, feature requests, issue reports etc.", formPanel);
        addAction("Send us an E-Mail", SUPPORT_EMAIL, this::doActionSendEMail, formPanel);
        tabPane.registerSingletonTab("CONTACT",
                "Contact",
                JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
                () -> formPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
    }

    private void doActionSendEMail() {
        UIUtils.copyToClipboard(SUPPORT_EMAIL);
        JOptionPane.showMessageDialog(this, "The E-Mail address '" + SUPPORT_EMAIL + "' was copied.", "Send E-Mail", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initializeResourcesTab() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);

        addAnswerHeader("Everything that might help with your poster/presentation/publication", "", formPanel);

        addAction("Cite JIPipe", "Open the online article for citing options", this::doActionOpenPublication, formPanel);
        addAction("Download JIPipe logos", "Logos are available in SVG and PNG formats", this::doActionDownloadLogos, formPanel);
        addAction("More on our website", "https://jipipe.hki-jena.de/resources/", this::doActionOpenResourcesPage, formPanel);

        tabPane.registerSingletonTab("PUBLICATION_RESOURCES",
                "Resources",
                JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
                () -> formPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
    }

    private void doActionOpenPublication() {
        UIUtils.desktopOpenURL("https://doi.org/10.1038/s41592-022-01744-4", true);
    }

    private void doActionDownloadLogos() {
        UIUtils.desktopOpenURL("https://github.com/applied-systems-biology/jipipe/releases/download/current/JIPipe-Logos-New.zip", true);
    }

    private void doActionOpenResourcesPage() {
        UIUtils.desktopOpenURL("https://jipipe.hki-jena.de/resources/", true);
    }

    private void initializeIssueTab() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        addAnswerHeader("Here are options that allow you to report issues", "", formPanel);

        addAction("Create an issue on GitHub", "", this::doActionGitHubIssues, formPanel);
        addAction("Ask the community", "You can also ask the community on image.sc", this::doActionOpenImageSc, formPanel);
        addAction("You can also contact us via E-Mail", SUPPORT_EMAIL, this::doActionSendEMail, formPanel);

        addQuestionHeader("Please don't forget to provide information that helps with reproducing the issue", "Sample projects, screenshots, JIPipe version, logs, ...", formPanel);

        tabPane.registerSingletonTab("ISSUE_REPORT",
                "Report issue",
                JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
                () -> formPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
    }

    private void doActionOpenImageSc() {
        UIUtils.desktopOpenURL("https://forum.image.sc/tag/jipipe", true);
    }

    private void doActionGitHubIssues() {
        UIUtils.desktopOpenURL("https://github.com/applied-systems-biology/jipipe/issues", true);
    }

    private void initializeFeatureRequestTab() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);

        addAnswerHeader("If you are missing a feature, please tell us!", "We are always happy to improve our software and make it more useful for you", formPanel);

        addAction("Create an issue on GitHub", "", this::doActionGitHubIssues, formPanel);
        addAction("Ask the community", "You can also ask the community on image.sc", this::doActionOpenImageSc, formPanel);
        addAction("You can also contact us via E-Mail", SUPPORT_EMAIL, this::doActionSendEMail, formPanel);

        addAnswerHeader("... or the feature is already in development", "", formPanel);

        addAction("Use a snapshot build", "Your requested feature might already be present in a snapshot build of JIPipe", this::doActionShowSnapshots, formPanel);

        tabPane.registerSingletonTab("FEATURE_REQUEST",
                "Feature request",
                JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
                () -> formPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
    }

    private void doActionShowSnapshots() {
        UIUtils.desktopOpenURL("https://asb-git.hki-jena.de/RGerst/jipipe/-/packages", true);
    }

    private void initializeImageAnalysisTab() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        addAnswerHeader("If you need help with image analysis or data management, you can refer to the following resources:", "", formPanel);

        addAction("Ask the community", "You can always ask the helpful image.sc community", this::doActionOpenImageScRoot, formPanel);
        addAction("Ask the NFDI4BIOIMAGE help desk", "The help desk can guide you to the right resources regarding research data management, image analysis, ...", this::doActionOpenNFDI4BIOIMAGEHelpDesk, formPanel);
        addAction("You can also contact us via E-Mail", SUPPORT_EMAIL, this::doActionSendEMail, formPanel);

        tabPane.registerSingletonTab("IMAGE_ANALYSIS",
                "Image analysis",
                JIPipe.RESOURCES.getIcon16("actions/help-info.png"),
                () -> formPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Present);
    }

    private void doActionOpenNFDI4BIOIMAGEHelpDesk() {
        UIUtils.desktopOpenURL("https://nfdi4bioimage.de/help-desk/", true);
    }

    private void doActionOpenImageScRoot() {
        UIUtils.desktopOpenURL("https://forum.image.sc/", true);
    }

    private void initializeWelcomeTab() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        addQuestionHeader("Please select the topic where you need support", "You can freely switch between the categories at any time.", formPanel);
        addAction("I need help with image analysis / data management", "", "IMAGE_ANALYSIS", formPanel);
        addAction("I want to request a feature", "", "FEATURE_REQUEST", formPanel);
        addAction("I have an issue and want to report it", "", "ISSUE_REPORT", formPanel);
        addAction("I need resources related to making publications/posters/presentations", "", "PUBLICATION_RESOURCES", formPanel);
        addAction("I want to contact you", "", "CONTACT", formPanel);
        tabPane.registerSingletonTab("START",
                "Start",
                JIPipe.RESOURCES.getIcon16("actions/tiny-start.png"),
                () -> formPanel,
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton,
                JIPipeDesktopTabPane.SingletonTabMode.Selected);
    }

    private void addAction(String title, String subtitle, String tabId, JIPipeDesktopFormPanel target) {
        addAction(title, subtitle, () -> tabPane.selectSingletonTab(tabId), target);
    }

    private void addAction(String title, String subtitle, Runnable runnable, JIPipeDesktopFormPanel target) {
        JButton button = UIUtils.createButton("<html><strong>" + title + "</strong><br/>" + subtitle, JIPipe.RESOURCES.getIcon24("actions/stock_right.png"), runnable);
        button.setBorder(BorderFactory.createCompoundBorder(UIUtils.createEmptyBorder(16), UIUtils.createButtonBorder(ThemeUtils.getCurrentStyle().getBorderColor())));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        target.addWideToForm(button);
    }

    private void addQuestionHeader(String title, String description, JIPipeDesktopFormPanel target) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        JLabel iconLabel = new JLabel(JIPipe.RESOURCES.getIcon32("status/dialog-question.png"));
        iconLabel.setBorder(UIUtils.createEmptyBorder(8));
        headerPanel.add(iconLabel, BorderLayout.WEST);
        if (!StringUtils.isNullOrEmpty(description)) {
            headerPanel.add(UIUtils.boxVertical(
                    UIUtils.createJLabel(title, ThemeUtils.getCurrentStyle().getFontSizeLarge()),
                    UIUtils.createJLabel(description, ThemeUtils.getCurrentStyle().getFontSizeNormal())
            ), BorderLayout.CENTER);
        } else {
            headerPanel.add(UIUtils.createJLabel(title, ThemeUtils.getCurrentStyle().getFontSizeLarge()),
                    BorderLayout.CENTER);
        }
        target.addWideToForm(headerPanel);
    }

    private void addAnswerHeader(String title, String description, JIPipeDesktopFormPanel target) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        JLabel iconLabel = new JLabel(JIPipe.RESOURCES.getIcon32("status/dialog-information.png"));
        iconLabel.setBorder(UIUtils.createEmptyBorder(8));
        headerPanel.add(iconLabel, BorderLayout.WEST);
        if (!StringUtils.isNullOrEmpty(description)) {
            headerPanel.add(UIUtils.boxVertical(
                    UIUtils.createJLabel(title, ThemeUtils.getCurrentStyle().getFontSizeLarge()),
                    UIUtils.createJLabel(description, ThemeUtils.getCurrentStyle().getFontSizeNormal())
            ), BorderLayout.CENTER);
        } else {
            headerPanel.add(UIUtils.createJLabel(title, ThemeUtils.getCurrentStyle().getFontSizeLarge()),
                    BorderLayout.CENTER);
        }
        target.addWideToForm(headerPanel);
    }
}
