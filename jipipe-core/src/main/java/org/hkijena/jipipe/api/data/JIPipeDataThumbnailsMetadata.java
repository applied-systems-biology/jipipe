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
 *
 */

package org.hkijena.jipipe.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.awt.Dimension;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the properties of the available thumbnails
 */
public class JIPipeDataThumbnailsMetadata {

    private List<Thumbnail> thumbnails = new ArrayList<>();
    private Path target;

    public JIPipeDataThumbnailsMetadata() {
    }

    @JsonGetter("thumbnails")
    public List<Thumbnail> getThumbnails() {
        return thumbnails;
    }

    @JsonSetter("thumbnails")
    public void setThumbnails(List<Thumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }

    @JsonGetter("target")
    public Path getTarget() {
        return target;
    }

    @JsonSetter("target")
    public void setTarget(Path target) {
        this.target = target;
    }

    public static class Thumbnail {
        private String name;
        private Dimension size;
        private List<Path> files;

        public Thumbnail() {
        }

        public Thumbnail(String name, Dimension size, List<Path> files) {
            this.name = name;
            this.size = size;
            this.files = files;
        }

        public Thumbnail(Thumbnail other) {
            this.name = other.name;
            this.size = other.size;
            this.files = new ArrayList<>(other.files);
        }

        @JsonGetter("name")
        public String getName() {
            return name;
        }

        @JsonSetter("name")
        public void setName(String name) {
            this.name = name;
        }

        @JsonGetter("size")
        public Dimension getSize() {
            return size;
        }

        @JsonSetter("size")
        public void setSize(Dimension size) {
            this.size = size;
        }

        @JsonGetter("files")
        public List<Path> getFiles() {
            return files;
        }

        @JsonSetter("files")
        public void setFiles(List<Path> files) {
            this.files = files;
        }
    }
}
