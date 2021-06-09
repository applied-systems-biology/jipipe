<div align="center">
   <img src="./docs/_static/dl-model-data.svg" alt="DeepLearningToolbox" width="200" height="200" />
</div>

# DeepLearningToolbox

[![website](https://img.shields.io/website?url=https%3A%2F%2Fwww.jipipe.org)](https://www.jipipe.org/)

A python toolbox for easy use of deep learning techniques with a graphical programming language called **JIPipe**, which
eliminates the need for any programming and is based only on the creation of a flowchart.

This code was written by Jan-Philipp Praetorius and Ruman Gerst. To learn more about JIPipe please visit
the [website](https://www.jipipe.org/). See the [tutorials](https://www.jipipe.org/examples/) to learn more about using
the depp learning toolbox and JIPipe.

## System requirements

This toolbox is supported for Linux and Windows. To run this toolbox with GPU support, please note that you need an
nvidia graphics card. Make sure you are using compatible **TensorFlow** and **CUDA** versions and refer to the following
TensorFlow and CUDA version compatibility table:

|       **TensorFlow**        |      **CUDA**      |
| :-------------------------: | :----------------: |
|            2.5.0            |        11.2        |
|            2.4.0            |        11.0        |
| 2.1.0 - 2.3.0 (recommended) | 10.1 (recommended) |
|        1.13.1 - 2.0         |        10.0        |
|       1.5.0 - 1.12.0        |        9.0         |

You can check both by executing the following command:

```bash
nvidia-smi
```

## Installation manual

In general, there are 2 ways to install the DeepLearningToolbox:

1. The faster of the two is done directly via the **package manager of JIPipe** and installs all software prerequisite
   miniconda, creates a new environment with all dependencies included. Follow the instructions in
   the [JIPipe tutorials](https://www.jipipe.org/tutorials/).
2. In addition, you can also install this toolbox yourself, which should usually take no more than 10 minutes:
    - [ ] Install [miniconda](https://docs.conda.io/en/latest/miniconda.html) for your ceorresponding Windows or Linux
      operating system with **python 3.7**
    - [ ] Clone this [repository](https://asb-git.hki-jena.de/JPraetor/deeplearningtoolbox.git) to create a new conda
      environment with the
      the [`environment.yml`](https://asb-git.hki-jena.de/JPraetor/deeplearningtoolbox/-/raw/master/environment.yml)file
    - [ ] Open anaconda command line with conda for python 3 in the path
    - [ ] Navigate to the directory where the `environment.yml` is located and run `conda env create -f environment.yml`
    - [ ] Run `conda activate DeepLearningToolbox` to activate this new environment. You should
      see `(DeepLearningToolbox)` on the left side of the terminal line.

## Dependencies

DeepLearningToolbox has the following package requirements (which are automatically installed with conda if missing):

- [numpy](https://numpy.org/) (>1.14.6,<1.19.4)
- [pandas](https://pandas.pydata.org/)
- [matplotlib](https://matplotlib.org/)
- [scipy](https://www.scipy.org/)
- [scikit-learn](https://sklearn.org/)
- [scikit-image](https://scikit-image.org/) (>0.16)
- [tensorflow-gpu](https://www.tensorflow.org/) (==2.1.0)
- [keras](https://keras.io/) (>=2.3.1)
- [opencv](https://opencv.org/)
- [tqdm](https://tqdm.github.io/)
- [tifffile](https://pypi.org/project/tifffile/)
- [h5py](https://www.h5py.org/)

## Run the following command before using the DeeplearningToolbox:

```bash
export PYTHONPATH="${PYTHONPATH}:$PWD/"
```

## Available network-architectures and models

| CNN         | Unet   | RNNs               | GANs      | Graph-NNs |
| ----------- | ------ | ------------------ | --------- | --------- |
| AlexNet     | SegNet | LSTM               | pix2pix   | ...       |
| GoogleLeNet |        | BILSTM             | InfoGan   |           |
| IsoNet      |        | IndRNN             | BayInfGAN |           |
| ResNet      |        | R-CNN              | ...       |           |
| VGG19       |        | GRU                |           |           |
| VGG16       |        | MCNN (Multi-scale) |           |           |

## Usage

You can use Deep Learning Toolbox as library or CLI tool.

### CLI

Run the `dltoolbox` module via

```bash
python -m dltoolbox
```

#### Creating and training a model

Before you can start with the training, you will need to create the model. The parameters are stored in a configuration
file in JSON format. For example, you can use `examples/model-SegNet-Segmentation.json` to create a SegNet model.

```bash
python -m dltoolbox --operation create-model --config examples/model-SegNet-Segmentation.json
```

This will create the untrained model file at the location specified in the configuration file.

To train this model, you need to provide a folder with TIFF images containing the labels and another folder containing
the raw images. Corresponding images should have the same name. Our example configuration expects the raw images to be
located in a folder `raw` (\*.tif extension) and labels to be located in a folder `labels`.

For the training, you need to supply both the model config and the configuration for the training. Our example is
configured, so the hdf5 file is extracted from `model_output_path` of the model config. Alternatively, you can also
set `model_input_path` inside the training config to always override this.

```bash
python -m dltoolbox --operation train --model-config examples/model-SegNet-Segmentation.json --config examples/training-SegNet.json
```

#### Device configuration

The CLI provides a configuration tool to determine which devices (GPU/CPU) should be available to Tensorflow. 
This configuration is supplied via the `device-config` parameter and contains following items:

```json 
{
  "cpus": "all",
  "gpus": "all",
  "log-device-placement": true
}
```

* `cpus` sets the list of CPUs made available to tensorflow. It can be `all` (default) or a list of numeric IDs
* `gpus` sets the list of GPUs made available to tensorflow. It can be `all` (default) or a list of numeric IDs
* `log-device-placement` allows you to enable or disable printing information on which hardware workloads are executed

To list all devices and their numeric IDs, you can use following CLI commands:

```bash 
# List all CPUs and GPUs
python -m dltoolbox --operation list-devices
```

These methods will print the number of available devices and each device in a separate line, starting with the numeric ID.
If you want to process the device list in a script, you can also output them as JSON:

```bash 
# List all CPUs and GPUs
python -m dltoolbox --operation list-devices-json
```

If you do not supply a device configuration, all CPUs and all GPUs will be made visible to Tensorflow. 

### Library

#### Creating and training a model

```python 
from dltoolbox.models import build_model
from dltoolbox.training import train_model
from dltoolbox.utils import setup_devices
import json

# Load configurations
with open("examples/model-SegNet-Segmentation.json", "r") as f:
   model_config = json.load(f)
with open("examples/train-SegNet.json", "r") as f:
   training_config = json.load(f)
   
# Configure the devices
setup_devices() # = Empty config = All CPUs, All GPUs

# You can disable the saving of models by setting the output paths to empty
model_config["model_output_path"] = ""
# training_config["model_output_path"] = ""

# Create the model
model = build_model(model_config)

# Train the model
# Here we have to set the model parameter as we disabled saving of the untrained model
trained_model = train_model(model_config=model_config, config=training_config, model=model)
```