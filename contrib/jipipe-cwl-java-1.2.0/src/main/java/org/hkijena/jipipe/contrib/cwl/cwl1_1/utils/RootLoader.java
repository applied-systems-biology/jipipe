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

package org.hkijena.jipipe.contrib.cwl.cwl1_1.utils;

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
    return LoaderInstances.union_of_CommandLineTool_or_ExpressionTool_or_Workflow_or_array_of_union_of_CommandLineTool_or_ExpressionTool_or_Workflow.documentLoad(doc, baseUri, loadingOptions);
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
