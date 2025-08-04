# JIPipe Dockerfiles

This folder contains Dockerfiles for different JIPipe releases.

Based on https://github.com/bengreenier/docker-xvfb

Example for version 5.3.0

## Building


```bash
docker build -t appsysbiohkijena/jipipe:5.3.0 -f jipipe-5.3.0.Dockerfile .
```

## Running

Please note that you will need to mount a volume that contains the inputs and generated outputs.

```bash
docker run --rm -v $(pwd):/data appsysbiohkijena/jipipe:5.3.0 run --project /data/project.jip --output-folder /data/output
```

## Publishing

```bash
docker push appsysbiohkijena/jipipe:5.3.0
```
