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

package org.hkijena.jipipe.installer.linux.api;

import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.hkijena.jipipe.installer.linux.ui.utils.ResourceUtils;
import org.hkijena.jipipe.installer.linux.ui.utils.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class InstallerRun implements JIPipeRunnable {
    public static final URL FIJI_DOWNLOAD_URL;
    public static final String[] FIJI_UPDATE_SITES = {
            "clij", "clij2", "IJPB-plugins", "ImageScience", "IJ-OpenCV-plugins", "Multi-Template-Matching"
    };
    public static final String[] JIPIPE_DEPENDENCY_URLS = new String[]{
            "https://maven.scijava.org/service/local/repositories/central/content/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/reflections/reflections/0.9.12/reflections-0.9.12.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-pdf-converter/0.62.2/flexmark-pdf-converter-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-toc/0.62.2/flexmark-ext-toc-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar",
            "https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/Image_5D/2.0.2/Image_5D-2.0.2.jar",
            "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-databind/2.11.0/jackson-databind-2.11.0.jar",
            "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-core/2.11.0/jackson-core-2.11.0.jar",
            "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-annotations/2.11.0/jackson-annotations-2.11.0.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/fontbox/2.0.4/fontbox-2.0.4.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/pdfbox/2.0.4/pdfbox-2.0.4.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/fathzer/javaluator/3.0.3/javaluator-3.0.3.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar",
            "https://github.com/ome/omero-insight/releases/download/v5.5.14/omero_ij-5.5.14-all.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/net/java/dev/jna/jna-platform/4.5.2/jna-platform-4.5.2.jar",
            "https://maven.scijava.org/service/local/repositories/releases/content/de/biomedical-imaging/imagej/ij_ridge_detect/1.4.1/ij_ridge_detect-1.4.1.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar",
            "https://maven.scijava.org/service/local/repositories/central/content/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.11.0/jackson-dataformat-yaml-2.11.0.jar"
    };

    static {
        try {
            FIJI_DOWNLOAD_URL = new URL("https://downloads.imagej.net/fiji/latest/fiji-linux64.zip");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final int maxProgress = 7;
    private Path installationPath;
    private boolean createLauncher = true;
    private int progress = 0;
    private StringBuilder log = new StringBuilder();

    public InstallerRun() {
        setInitialInstallationPath();
    }

    private void setInitialInstallationPath() {
        String dataHome = System.getenv().getOrDefault("XDG_DATA_HOME", null);
        if (StringUtils.isNullOrEmpty(dataHome)) {
            dataHome = System.getProperty("user.home") + "/.local/share/";
        }
        installationPath = Paths.get(dataHome).resolve("JIPipe-installer");
    }

    public Path getInstallationPath() {
        return installationPath;
    }

    public void setInstallationPath(Path installationPath) {
        this.installationPath = installationPath;
    }

    public boolean isCreateLauncher() {
        return createLauncher;
    }

    public void setCreateLauncher(boolean createLauncher) {
        this.createLauncher = createLauncher;
    }

    @Override
    public void run(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        Consumer<JIPipeRunnerSubStatus> subStatusConsumer = (sub) -> {
            log.append(progress).append("/").append(maxProgress).append(": ").append(sub.toString()).append("\n");
            onProgress.accept(new JIPipeRunnerStatus(progress, maxProgress, sub.toString()));
        };
        createInstallationDirectory(new JIPipeRunnerSubStatus("Creating installation directory"), subStatusConsumer);
        if (!Files.isDirectory(installationPath.resolve("Fiji.app"))) {
            downloadFiji(new JIPipeRunnerSubStatus("Downloading Fiji"), subStatusConsumer);
            extractFiji(new JIPipeRunnerSubStatus("Extracting Fiji"), subStatusConsumer);
        } else {
            subStatusConsumer.accept(new JIPipeRunnerSubStatus("Fiji.app already exists. Skipping download and extraction."));
            progress += 2;
        }
        installJIPipeDependencies(new JIPipeRunnerSubStatus("Installing dependencies"), subStatusConsumer);
        installJIPipe(new JIPipeRunnerSubStatus("Installing JIPipe"), subStatusConsumer);
        installImageJDependencies(new JIPipeRunnerSubStatus("Installing ImageJ dependencies"), subStatusConsumer);
        if (createLauncher)
            createLaunchers(new JIPipeRunnerSubStatus("Create launchers"), subStatusConsumer);
        else {
            progress += 1;
            subStatusConsumer.accept(new JIPipeRunnerSubStatus("Launcher creation was disabled."));
        }
        subStatusConsumer.accept(new JIPipeRunnerSubStatus("Installation successful."));
    }

    private void createLaunchers(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        Path imageJDir = installationPath.resolve("Fiji.app");
        Path scriptFile = imageJDir.resolve("start-jipipe-linux.sh");
        try (PrintWriter writer = new PrintWriter(scriptFile.toFile())) {
            writer.println("#!/bin/bash");
            writer.println("IMAGEJ_EXECUTABLE=$(find . -name \"ImageJ-*\" -print -quit)");
            writer.println("eval $IMAGEJ_EXECUTABLE --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.setPosixFilePermissions(scriptFile, PosixFilePermissions.fromString("rwxrwxr-x"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String dataHome = System.getenv().getOrDefault("XDG_DATA_HOME", FileUtils.getUserDirectory().toPath().resolve(".local").resolve("share").toString());
        Path applicationsDirectory = Paths.get(dataHome).resolve("applications");
        if (!Files.exists(applicationsDirectory)) {
            try {
                Files.createDirectories(applicationsDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Export icon
        Path iconPath = imageJDir.resolve("jipipe-icon.svg");
        if (!Files.exists(iconPath)) {
            try {
                InputStream reader = ResourceUtils.getPluginResourceAsStream("logo-square.svg");
                Files.copy(reader, iconPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        iconPath = imageJDir.resolve("fiji-icon.png");
        if (!Files.exists(iconPath)) {
            try {
                InputStream reader = ResourceUtils.getPluginResourceAsStream("fiji.png");
                Files.copy(reader, iconPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Write desktop file
        try (PrintWriter writer = new PrintWriter(applicationsDirectory.resolve("jipipe.desktop").toFile())) {
            writer.println("[Desktop Entry]");
            writer.println("Exec=" + "bash -c \"cd '" + imageJDir + "' && '" + scriptFile + "'\"");
            writer.println("Icon=" + imageJDir.resolve("jipipe-icon.svg"));
            writer.println("Name=JIPipe");
            writer.println("Comment=Graphical batch-programming language for ImageJ/Fiji");
            writer.println("NoDisplay=false");
            writer.println("StartupNotify=true");
            writer.println("Type=Application");
            writer.println("Categories=Science;");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (PrintWriter writer = new PrintWriter(applicationsDirectory.resolve("fiji-jipipe.desktop").toFile())) {
            writer.println("[Desktop Entry]");
            writer.println("Exec=" + "bash -c \"cd '" + imageJDir + "' && './ImageJ-linux64'\"");
            writer.println("Icon=" + imageJDir.resolve("fiji-icon.png"));
            writer.println("Name=Fiji+JIPipe");
            writer.println("Comment=ImageJ/Fiji with JIPipe installed");
            writer.println("NoDisplay=false");
            writer.println("StartupNotify=true");
            writer.println("Type=Application");
            writer.println("Categories=Science;");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        ++progress;
        subStatusConsumer.accept(subStatus.resolve("Launchers created."));
    }

    private void installImageJDependencies(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        try {
            Files.setPosixFilePermissions(installationPath.resolve("Fiji.app").resolve("ImageJ-linux64"), PosixFilePermissions.fromString("rwxrwxr-x"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        {
            CommandLine commandLine = new CommandLine(installationPath.resolve("Fiji.app").resolve("ImageJ-linux64").toFile());
            commandLine.addArgument("--pass-classpath");
            commandLine.addArgument("--full-classpath");
            commandLine.addArgument("--main-class");
            commandLine.addArgument("org.hkijena.jipipe.ijupdatercli.Main");
            commandLine.addArgument("activate");
            for (String fijiUpdateSite : FIJI_UPDATE_SITES) {
                commandLine.addArgument(fijiUpdateSite);
            }
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
            LogOutputStream stdOutputStream = new LogOutputStream() {
                @Override
                protected void processLine(String s, int i) {
                    subStatusConsumer.accept(subStatus.resolve(s));
                }
            };
            LogOutputStream stdErrorStream = new LogOutputStream() {
                @Override
                protected void processLine(String s, int i) {
                    subStatusConsumer.accept(subStatus.resolve(s));
                }
            };
            executor.setStreamHandler(new PumpStreamHandler(stdOutputStream, stdErrorStream));
            int result = 0;
            try {
                result = executor.execute(commandLine);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (executor.isFailure(result)) {
                throw new RuntimeException("Installation failed.");
            }
        }
        {
            CommandLine commandLine = new CommandLine(installationPath.resolve("Fiji.app").resolve("ImageJ-linux64").toFile());
            commandLine.addArgument("--pass-classpath");
            commandLine.addArgument("--full-classpath");
            commandLine.addArgument("--main-class");
            commandLine.addArgument("org.hkijena.jipipe.ijupdatercli.Main");
            commandLine.addArgument("update");
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
            LogOutputStream stdOutputStream = new LogOutputStream() {
                @Override
                protected void processLine(String s, int i) {
                    subStatusConsumer.accept(subStatus.resolve(s));
                }
            };
            LogOutputStream stdErrorStream = new LogOutputStream() {
                @Override
                protected void processLine(String s, int i) {
                    subStatusConsumer.accept(subStatus.resolve(s));
                }
            };
            executor.setStreamHandler(new PumpStreamHandler(stdOutputStream, stdErrorStream));
            int result = 0;
            try {
                result = executor.execute(commandLine);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (executor.isFailure(result)) {
                throw new RuntimeException("Installation failed.");
            }
        }
        ++progress;
        subStatusConsumer.accept(subStatus.resolve("ImageJ dependencies were installed."));
    }

    private void installJIPipe(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        if (Files.isDirectory(Paths.get("jipipe-bin"))) {
            try {
                Files.list(Paths.get("jipipe-bin")).forEach(sourcePath -> {
                    Path targetPath = installationPath.resolve("Fiji.app").resolve("plugins").resolve(sourcePath.getFileName());
                    if (!Files.exists(targetPath)) {
                        subStatusConsumer.accept(subStatus.resolve("Installing " + targetPath.getFileName()));
                        try {
                            Files.copy(sourcePath, targetPath);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ++progress;
        subStatusConsumer.accept(subStatus.resolve("JIPipe installed."));
    }

    private void installJIPipeDependencies(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        for (String dependencyUrl : JIPIPE_DEPENDENCY_URLS) {
            String fileName = dependencyUrl.substring(dependencyUrl.lastIndexOf('/') + 1);
            Path targetFile = installationPath.resolve("Fiji.app").resolve("jars").resolve(fileName);
            if (!Files.isRegularFile(targetFile)) {
                try {
                    String[] lastMessage = new String[]{""};
                    WebUtils.download(new URL(dependencyUrl), targetFile, total -> {
                        DecimalFormat df = new DecimalFormat("#.##");
                        String message = "Downloaded " + df.format(total / 1024.0 / 1024.0) + " MB";
                        if (!Objects.equals(message, lastMessage[0])) {
                            subStatusConsumer.accept(subStatus.resolve("Downloading missing dependency " + fileName).resolve(message));
                            lastMessage[0] = message;
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        ++progress;
        subStatusConsumer.accept(subStatus.resolve("Dependencies are ready."));
    }

    private void extractFiji(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        Path sourceFile = installationPath.resolve("fiji-linux64.zip");
        try {
            ZipUtils.unzip(sourceFile, installationPath, subStatus, subStatusConsumer);
            Files.delete(sourceFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ++progress;
        subStatusConsumer.accept(subStatus.resolve("Fiji extracted."));
    }

    private void downloadFiji(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        subStatusConsumer.accept(subStatus.resolve("Downloading from " + FIJI_DOWNLOAD_URL));
        Path targetFile = installationPath.resolve("fiji-linux64.zip");
        try {
            String[] lastMessage = new String[]{""};
            WebUtils.download(FIJI_DOWNLOAD_URL, targetFile, total -> {
                DecimalFormat df = new DecimalFormat("#.##");
                String message = "Downloaded " + df.format(total / 1024.0 / 1024.0) + " MB";
                if (!Objects.equals(message, lastMessage[0])) {
                    subStatusConsumer.accept(subStatus.resolve(message));
                    lastMessage[0] = message;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ++progress;
        subStatusConsumer.accept(subStatus.resolve("Download finished."));
    }

    private void createInstallationDirectory(JIPipeRunnerSubStatus subStatus, Consumer<JIPipeRunnerSubStatus> subStatusConsumer) {
        if (!Files.isDirectory(installationPath)) {
            try {
                Files.createDirectories(installationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ++progress;
        subStatusConsumer.accept(subStatus.resolve(installationPath + " is ready."));
    }


    public StringBuilder getLog() {
        return log;
    }
}
