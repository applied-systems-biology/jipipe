package org.hkijena.jipipe.ui.project.templatedownloader;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

public class ProjectTemplateDownloaderRepository {
    private String name;
    private List<ProjectTemplateDownloaderPackage> files = new ArrayList<>();

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("files")
    public List<ProjectTemplateDownloaderPackage> getFiles() {
        return files;
    }

    @JsonSetter("files")
    public void setFiles(List<ProjectTemplateDownloaderPackage> files) {
        this.files = files;
    }
}
