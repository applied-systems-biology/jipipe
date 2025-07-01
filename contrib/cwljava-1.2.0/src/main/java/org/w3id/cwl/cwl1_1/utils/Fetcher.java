package org.w3id.cwl.cwl1_1.utils;

public interface Fetcher {

  public abstract String urlJoin(final String baseUrl, final String url);

  public abstract String fetchText(final String url);
}
