package org.hkijena.jipipe.api.data.documentation;

/**
 * CommonFormats defines string constants for widely used
 * data, image, 3D, and general file formats, including their MIME types
 * where applicable.
 */
public final class EncodingFormats {

    // ==== DATA & TEXT FORMATS ====
    public static final String JSON = "application/json";
    public static final String XML = "application/xml";
    public static final String YAML = "application/x-yaml";
    public static final String CSV = "text/csv";
    public static final String TSV = "text/tab-separated-values";
    public static final String TXT = "text/plain";
    public static final String HTML = "text/html";
    public static final String PDF = "application/pdf";
    public static final String RTF = "application/rtf";
    public static final String MARKDOWN = "text/markdown";
    // ==== IMAGE FORMATS ====
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String GIF = "image/gif";
    public static final String BMP = "image/bmp";
    public static final String TIFF = "image/tiff";
    public static final String WEBP = "image/webp";
    public static final String SVG = "image/svg+xml";
    public static final String HEIF = "image/heif";
    public static final String ICO = "image/x-icon";
    // ==== AUDIO / VIDEO FORMATS ====
    public static final String MP3 = "audio/mpeg";
    public static final String WAV = "audio/wav";
    public static final String OGG = "audio/ogg";
    public static final String FLAC = "audio/flac";
    public static final String MP4 = "video/mp4";
    public static final String WEBM = "video/webm";
    public static final String AVI = "video/x-msvideo";
    public static final String MOV = "video/quicktime";
    // ==== 3D FORMATS ====
    public static final String OBJ = "model/obj";               // unofficial
    public static final String STL = "model/stl";               // unofficial
    public static final String FBX = "application/octet-stream"; // no official MIME
    public static final String GLTF = "model/gltf+json";
    public static final String GLB = "model/gltf-binary";
    public static final String COLLADA = "model/vnd.collada+xml";
    public static final String THREE_DS = "model/3ds";          // unofficial
    public static final String PLY = "model/ply";               // unofficial
    // ==== ARCHIVES & BINARY CONTAINERS ====
    public static final String ZIP = "application/zip";
    public static final String TAR = "application/x-tar";
    public static final String GZIP = "application/gzip";
    public static final String SEVEN_ZIP = "application/x-7z-compressed";
    public static final String RAR = "application/vnd.rar";
    // ==== MISC / GENERAL ====
    public static final String BINARY = "application/octet-stream";
    public static final String UNKNOWN = "application/octet-stream";
    public static final String EXE = "application/vnd.microsoft.portable-executable";
    public static final String WASM = "application/wasm";
    // ==== OFFICE & DOCUMENT FORMATS ====
    public static final String DOC = "application/msword";
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String XLS = "application/vnd.ms-excel";
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PPT = "application/vnd.ms-powerpoint";
    public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    // ==== SCRIPT FORMATS ====
    public static final String PYTHON = "text/x-python";
    public static final String R_SCRIPT = "text/x-r-source";
    public static final String IMAGEJ_MACRO = "text/plain"; // .ijm files, typically plain text
    public static final String JAVASCRIPT = "application/javascript";
    public static final String SHELL_SCRIPT = "application/x-sh";
    public static final String POWERSHELL = "application/x-powershell";
    public static final String PERL = "text/x-perl";
    public static final String RUBY = "text/x-ruby";
    public static final String MATLAB = "text/x-matlab";
    public static final String PHP = "application/x-httpd-php";
    public static final String SQL = "application/sql";
    private EncodingFormats() {
        // Prevent instantiation
    }
}
