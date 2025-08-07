# hasPart/isPartOf Structure in this crate

This documents the hierarchy in this json file for the hasPart property.
"isPartOf" occurences are handled the inverse way (as if it was a hasPart property the other way around).

- ./
  - sub-a.file
  - sub-b.file
    - sub-b-sub-a.file
      - looper.file --> sub-b.file  
    - sub-b-sub-b.file (isPartOf)
    - looper-isPartOf.file --> sub-b.file