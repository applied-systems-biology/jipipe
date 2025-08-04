# JIPipe Dockerfiles

This folder contains Dockerfiles for different JIPipe releases.

Based on https://github.com/bengreenier/docker-xvfb

## Build instructions

Example for version 5.3.0

```bash
docker build -t jipipe:5.3.0 -f jipipe-5.3.0.Dockerfile .
docker run --rm -v $(pwd):/data jipipe:5.3.0 run --project /data/project.jip --output-folder /data/output
```

