import argparse
import json

parser = argparse.ArgumentParser(prog="python -m dltoolbox", description="Runs the Deep Learning Toolbox")
parser.add_argument("--operation", "-o", dest="operation", choices=["create-model", "train"], required=True,
                    help="The operation to apply")
parser.add_argument("--model-config", dest="model_config",
                    help="The configuration file that describes the model parameters")
parser.add_argument("--config", dest="config", help="The configuration file for the current operation")

args = parser.parse_args()

if args.operation == "create-model":

    with open(args.config, "r") as f:
        config = json.load(f)

    import dltoolbox.models

    dltoolbox.models.build_model(config)
elif args.operation == "train":

    with open(args.config, "r") as f:
        config = json.load(f)
    with open(args.model_config, "r") as f:
        model_config = json.load(f)

    import dltoolbox.training

    dltoolbox.training.train_model(model_config, config)
else:
    raise AttributeError("Unsupported operation: " + args.operation)
