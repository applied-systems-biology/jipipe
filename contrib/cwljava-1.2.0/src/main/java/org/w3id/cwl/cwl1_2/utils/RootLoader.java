package org.w3id.cwl.cwl1_2.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class RootLoader {
  public static Object loadDocument(
      final Map<String, Object> doc, final String baseUri_, final LoadingOptions loadingOptions_) {
    final String baseUri = ensureBaseUri(baseUri_);
    LoadingOptions loadingOptions = loadingOptions_;
    if (loadingOptions == null) {
      loadingOptions = new LoadingOptionsBuilder().setFileUri(baseUri).build();
    }
    return LoaderInstances.union_of_CommandLineTool_or_ExpressionTool_or_Workflow_or_Operation_or_array_of_union_of_CommandLineTool_or_ExpressionTool_or_Workflow_or_Operation.documentLoad(doc, baseUri, loadingOptions);
  }

  public static Object loadDocument(
      final Map<String, Object> doc, final String baseUri) {
    return loadDocument(doc, baseUri, null);
  }

  public static Object loadDocument(final Map<String, Object> doc) {
    return loadDocument(doc, ensureBaseUri(null));
  }

  public static Object loadDocument(final Path path) {
    return loadDocument(readPath(path), path.toUri().toString());
  }

  public static Object loadDocument(final Path path, String baseUri) {
    return loadDocument(readPath(path), baseUri);
  }

  public static Object loadDocument(
      final Path path, LoadingOptions loadingOptions) {
    return loadDocument(readPath(path), loadingOptions);
  }

  public static Object loadDocument(
final Path path, String baseUri, LoadingOptions loadingOptions) {
    return loadDocument(readPath(path), baseUri, loadingOptions);
  }

  public static Object loadDocument(final File file) {
    return loadDocument(file.toPath());
  }

  public static Object loadDocument(final File file, String baseUri) {
    return loadDocument(file.toPath(), baseUri);
  }

  public static Object loadDocument(final File file, LoadingOptions loadingOptions) {
    return loadDocument(file.toPath(), loadingOptions);
  }

  public static Object loadDocument(
      final File file, String baseUri, LoadingOptions loadingOptions) {
    return loadDocument(file.toPath(), baseUri, loadingOptions);
  }

  public static Object loadDocument(final String doc) {
    return loadDocument(doc, ensureBaseUri(null));
  }

  public static Object loadDocument(final String doc, final LoadingOptions loadingOptions) {
    return loadDocument(doc, ensureBaseUri(null), loadingOptions);
  }

  public static Object loadDocument(final String doc, final String uri) {
    return loadDocument(doc, uri, null);
  }

  public static Object loadDocument(
      final String doc, final String uri_, final LoadingOptions loadingOptions_) {
    final String uri = ensureBaseUri(uri_);
    LoadingOptions loadingOptions = loadingOptions_;
    if (loadingOptions == null) {
      loadingOptions = new LoadingOptionsBuilder().setFileUri(uri).build();
    }
    final Map<String, Object> result = YamlUtils.mapFromString(doc);
    loadingOptions.idx.put(uri, result);
    return loadDocument(result, uri, loadingOptions);
  }

  static String readPath(final Path path) {
    try {
      return new String(Files.readAllBytes(path), "UTF8");
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  static String ensureBaseUri(final String baseUri_) {
    String baseUri = baseUri_;
    if(baseUri == null) {
      baseUri = Uris.fileUri(Paths.get(".").toAbsolutePath().normalize().toString()) + "/";
    }
    return baseUri;
  }

}
