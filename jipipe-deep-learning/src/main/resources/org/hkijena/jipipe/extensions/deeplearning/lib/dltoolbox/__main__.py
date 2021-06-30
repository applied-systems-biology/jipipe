#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""

@author: J-P Praetorius
@email: jan-philipp.praetorius@leibniz-hki.de or p.e.mueller07@gmail.com

Copyright by Jan-Philipp Praetorius

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
https://www.leibniz-hki.de/en/applied-systems-biology.html
HKI-Center for Systems Biology of Infection
Leibniz Institute for Natural Product Research and Infection Biology -
Hans Knöll Insitute (HKI)
Adolf-Reichwein-Straße 23, 07745 Jena, Germany
"""

import argparse
import json

parser = argparse.ArgumentParser(prog="python -m dltoolbox", description="Runs the Deep Learning Toolbox")
parser.add_argument("--operation", "-o", dest="operation", choices=["create-model", "train", "predict", "list-devices",
                                                                    "list-devices-json", "evaluate"], required=True,
                    help="The operation to apply")
parser.add_argument("--model-config", dest="model_config",
                    help="The configuration file that describes the model parameters")
parser.add_argument("--config", dest="config", help="The configuration file for the current operation")
parser.add_argument("--device-config", dest="device_config", help="The configuration file for the device(s) to be used")

args = parser.parse_args()

if args.operation == "create-model":

    with open(args.config, "r") as f:
        config = json.load(f)
    if args.device_config:
        with open(args.device_config, "r") as f:
            device_config = json.load(f)
    else:
        device_config = {}

    import dltoolbox.utils
    dltoolbox.utils.setup_devices(device_config)

    import dltoolbox.models
    dltoolbox.models.build_model(config)
elif args.operation == "train":

    with open(args.config, "r") as f:
        config = json.load(f)
    with open(args.model_config, "r") as f:
        model_config = json.load(f)
    if args.device_config:
        with open(args.device_config, "r") as f:
            device_config = json.load(f)
    else:
        device_config = {}

    import dltoolbox.utils
    dltoolbox.utils.setup_devices(device_config)

    import dltoolbox.training
    dltoolbox.training.train_model(model_config, config)
elif args.operation == "predict":
    with open(args.config, "r") as f:
        config = json.load(f)
    with open(args.model_config, "r") as f:
        model_config = json.load(f)
    if args.device_config:
        with open(args.device_config, "r") as f:
            device_config = json.load(f)
    else:
        device_config = {}

    import dltoolbox.utils
    dltoolbox.utils.setup_devices(device_config)

    import dltoolbox.prediction
    dltoolbox.prediction.predict_data(model_config, config)
elif args.operation == "evaluate":
    with open(args.config, "r") as f:
        config = json.load(f)
    if args.device_config:
        with(open(args.device_config), "r") as f:
            device_config = json.load(f)
    else:
        device_config = {}

    import dltoolbox.utils
    dltoolbox.utils.setup_devices(device_config)

    import dltoolbox.evaluation
    dltoolbox.evaluation.evaluate_data(config)
elif args.operation == "list-devices":
    import tensorflow as tf

    cpus = tf.config.list_physical_devices('CPU')
    gpus = tf.config.list_physical_devices('GPU')

    for id, cpu in enumerate(cpus):
        print(str(id) + "\tCPU\t" + cpu.name)
    for id, gpu in enumerate(gpus):
        print(str(id) + "\tGPU\t" + gpu.name)

elif args.operation == "list-devices-json":
    import tensorflow as tf

    cpus = tf.config.list_physical_devices('CPU')
    gpus = tf.config.list_physical_devices('GPU')

    data = []
    for id, cpu in enumerate(cpus):
        data.append({ "id": id, "type": "CPU", "name": cpu.name })
    for id, gpu in enumerate(gpus):
        data.append({ "id": id, "type": "CPU", "name": gpu.name })

    print(json.dumps(data))

else:
    raise AttributeError("Unsupported operation: " + args.operation)

# TODO: probieren ... visualisierungs modul
# Model visualization
# from keras.utils.visualize_util import plot
# plot(autoencoder, to_file='model.png', show_shapes=True)
