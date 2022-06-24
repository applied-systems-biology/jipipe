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

import java.awt.*;
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

    /**
     * Selects the best fitting thumbnail entry with respect to the size
     *
     * @param size the selected size
     * @return the thumbnail or null if no thumbnail is available
     */
    public Thumbnail selectBestThumbnail(Dimension size) {
        Thumbnail selected = null;
        final int targetWidth = size.width;
        final int targetHeight = size.height;
        int selectedWidth = 0;
        int selectedHeight = 0;
        for (Thumbnail thumbnail : thumbnails) {
            if ((selectedWidth < targetWidth && thumbnail.size.width >= targetWidth) || (selectedHeight < targetHeight && thumbnail.size.height >= targetHeight)) {
                selected = thumbnail;
                selectedWidth = thumbnail.size.width;
                selectedHeight = thumbnail.size.height;
            }
        }
        return selected;
    }

    public static class Thumbnail {
        private String name;
        private Dimension size;

        private Path imageFile;

        private List<Path> additionalFiles;

        public Thumbnail() {
        }

        public Thumbnail(String name, Dimension size, Path imageFile, List<Path> additionalFiles) {
            this.name = name;
            this.size = size;
            this.imageFile = imageFile;
            this.additionalFiles = additionalFiles;
        }

        public Thumbnail(Thumbnail other) {
            this.name = other.name;
            this.size = other.size;
            this.imageFile = other.imageFile;
            this.additionalFiles = new ArrayList<>(other.additionalFiles);
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

        @JsonGetter("image-file")
        public Path getImageFile() {
            return imageFile;
        }

        @JsonSetter("image-file")
        public void setImageFile(Path imageFile) {
            this.imageFile = imageFile;
        }

        @JsonGetter("additional-files")
        public List<Path> getAdditionalFiles() {
            return additionalFiles;
        }

        @JsonSetter("additional-files")
        public void setAdditionalFiles(List<Path> additionalFiles) {
            this.additionalFiles = additionalFiles;
        }
    }
}
