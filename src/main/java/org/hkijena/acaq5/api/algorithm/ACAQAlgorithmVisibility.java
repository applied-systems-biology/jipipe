package org.hkijena.acaq5.api.algorithm;

public enum ACAQAlgorithmVisibility {
    All,
    Analysis,
    BatchImporter;

   public boolean isVisibleIn(ACAQAlgorithmVisibility visibility) {
       return this == All || visibility == this;
   }
}
