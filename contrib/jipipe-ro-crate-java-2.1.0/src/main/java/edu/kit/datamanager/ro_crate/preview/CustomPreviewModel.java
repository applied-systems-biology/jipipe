package edu.kit.datamanager.ro_crate.preview;

import java.util.List;

/**
 *
 * @author jejkal
 */
public class CustomPreviewModel {

    protected ROCrate crate;
    protected List<Dataset> datasets;
    protected List<File> files;

    public ROCrate getCrate() {
        return crate;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public List<File> getFiles() {
        return files;
    }

    public static class ROCrate {

        protected String name;
        protected String description;
        protected String type;
        protected String license;
        protected String datePublished;
        protected List<Part> hasPart;

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getLicense() {
            return license;
        }

        public String getDatePublished() {
            return datePublished;
        }

        public List<Part> getHasPart() {
            return hasPart;
        }

    }

    public static class Part {

        protected String id;
        protected String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

    }

    public static class Dataset {

        protected String id;
        protected String name;
        protected String description;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

    }

    public static class File {

        protected String id;
        protected String name;
        protected String description;
        protected String contentSize;
        protected String encodingFormat;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getContentSize() {
            return contentSize;
        }

        public String getEncodingFormat() {
            return encodingFormat;
        }

    }
}
