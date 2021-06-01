# DeepLearningToolbox

[![website](https://img.shields.io/website?url=https%3A%2F%2Fwww.jipipe.org)](https://www.jipipe.org/)

A python toolbox for easy use of deep learning techniques with a graphical programming language called **JIPipe**, which eliminates the need for any programming and is based only on the creation of a flowchart.

This code was written by Jan-Philipp Praetorius and Ruman Gerst. To learn more about JIPipe please visit the [website](https://www.jipipe.org/). See the [tutorials](https://www.jipipe.org/examples/) to learn more about using the depp learning toolbox and JIPipe. 

## System requirements

This toolbox is supported for Linux and Windows. To run this toolbox with GPU support, please note that you need an nvidia graphics card. 
Make sure you are using compatible **TensorFlow** and **CUDA** versions and refer to the following TensorFlow and CUDA version compatibility table:

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

1. The faster of the two is done directly via the **package manager of JIPipe** and installs all software prerequisite miniconda, creates a new environment with all dependencies included. Follow the instructions in the [JIPipe tutorials](https://www.jipipe.org/tutorials/).  
2. In addition, you can also install this toolbox yourself, which should usually take no more than 10 minutes:
   - [ ] Install [miniconda](https://docs.conda.io/en/latest/miniconda.html) for your ceorresponding Windows or Linux operating system with **python 3.7**
   - [ ] Clone this [repository](https://asb-git.hki-jena.de/JPraetor/deeplearningtoolbox.git) to create a new conda environment with the the [`environment.yml`](https://asb-git.hki-jena.de/JPraetor/deeplearningtoolbox/-/raw/master/environment.yml)file
   - [ ] Open anaconda command line with conda for python 3 in the path
   - [ ] Navigate to the directory where the `environment.yml` is located and run `conda env create -f environment.yml`
   - [ ] Run `conda activate DeepLearningToolbox` to activate this new environment. You should see `(DeepLearningToolbox)` on the left side of the terminal line.

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