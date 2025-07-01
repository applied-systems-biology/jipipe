package org.w3id.cwl.cwl1_1.utils;

public class AnonymousRecordTest {

  @org.junit.Test
  public void test_record_with_anonymous_type_record() throws Exception {
    java.net.URL url = getClass().getResource("record-in-format.cwl.json");
    java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
    RootLoader.loadDocument(resPath);
  }
}
