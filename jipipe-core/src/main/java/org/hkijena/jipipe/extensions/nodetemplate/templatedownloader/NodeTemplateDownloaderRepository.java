package org.hkijena.jipipe.extensions.nodetemplate.templatedownloader;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

public class NodeTemplateDownloaderRepository {
    private String name;
    private List<NodeTemplateDownloaderPackage> files = new ArrayList<>();

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("files")
    public List<NodeTemplateDownloaderPackage> getFiles() {
        return files;
    }

    @JsonSetter("files")
    public void setFiles(List<NodeTemplateDownloaderPackage> files) {
        this.files = files;
    }
}
