package org.hkijena.jipipe.extensions.environments;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class REnvironment implements ExternalEnvironment {
    private final EventBus eventBus = new EventBus();
    private Path RExecutablePath = Paths.get("");
    private Path RScriptExecutablePath = Paths.get("");

    public REnvironment() {
        if(SystemUtils.IS_OS_LINUX) {
            RExecutablePath = Paths.get("/usr/bin/R");
            RScriptExecutablePath = Paths.get("/usr/bin/RScript");
        }
    }

    public REnvironment(REnvironment other) {
        this.RExecutablePath = other.RExecutablePath;
        this.RScriptExecutablePath = other.RScriptExecutablePath;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if(getRExecutablePath() == null || !Files.isRegularFile(getRExecutablePath())) {
            report.forCategory("Executable").reportIsInvalid(
                    "R executable does not exist",
                    "You need to provide a R executable",
                    "Provide a R executable",
                    "R environment"
            );
        }
        if(getRScriptExecutablePath() == null || !Files.isRegularFile(getRScriptExecutablePath())) {
            report.forCategory("Executable").reportIsInvalid(
                    "RScript executable does not exist",
                    "You need to provide a RScript executable",
                    "Provide a RScript executable",
                    "R environment"
            );
        }
    }

    @JIPipeDocumentation(name = "R executable", description = "The main R executable (R.exe on Windows)")
    @JIPipeParameter("r-executable-path")
    @JsonGetter("r-executable-path")
    public Path getRExecutablePath() {
        return RExecutablePath;
    }

    @JIPipeParameter("r-executable-path")
    @JsonSetter("r-executable-path")
    public void setRExecutablePath(Path RExecutablePath) {
        this.RExecutablePath = RExecutablePath;
    }

    @JIPipeDocumentation(name = "RScript executable", description = "The RScript executable (RScript.exe on Windows)")
    @JIPipeParameter("rscript-executable-path")
    @JsonGetter("rscript-executable-path")
    public Path getRScriptExecutablePath() {
        return RScriptExecutablePath;
    }

    @JIPipeParameter("rscript-executable-path")
    @JsonSetter("rscript-executable-path")
    public void setRScriptExecutablePath(Path RScriptExecutablePath) {
        this.RScriptExecutablePath = RScriptExecutablePath;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/rlogo_icon.png");
    }

    @Override
    public String getStatus() {
        return "R";
    }

    @Override
    public String getInfo() {
        return StringUtils.orElse(RExecutablePath, "<Not set>");
    }
}
