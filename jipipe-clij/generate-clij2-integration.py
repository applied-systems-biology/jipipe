#!/usr/bin/env python3

from pathlib import Path
import urllib.request
from zipfile import ZipFile
from glob import glob
import javalang
import javalang.tree
from bidict import bidict
from itertools import groupby
import inflection

# This scripts automatically generates JIPipe wrappers around CLIJ2 methods
source_packages = [
    "http://dl.bintray.com/haesleinhuepf/clij/net/haesleinhuepf/clij2_/2.0.0.14/clij2_-2.0.0.14-sources.jar"]
temp_dir = Path("./.clij2-generator").absolute()

if not temp_dir.exists():
    temp_dir.mkdir(parents=True)
    
target_dir = temp_dir / Path("target")
if not target_dir.exists():
    target_dir.mkdir(parents=True)

# Download and extract source packages if they do not exist
src_dir = temp_dir / Path("src")
if not src_dir.exists():
    src_dir.mkdir(parents=True)

for source_package in source_packages:
    filename = src_dir / Path(source_package.split("/")[-1])
    if not filename.exists():
        print("Downloading " + source_package + " into " + str(filename))
        urllib.request.urlretrieve(source_package, filename)

        print("Extracting " + str(filename))
        with ZipFile(filename) as zip:
            zip.extractall(src_dir)


def is_image_type(type):
    return type.name == "ClearCLBuffer" or type.name == "ClearCLImageInterface"


def is_output_parameter(parameter):
    return "out" in parameter.name.lower() or "dst" in parameter.name.lower() or "dest" in parameter.name.lower() or "result" in parameter.name.lower()


def extract_methods(java_file):
    with open(java_file, "r") as f:
        java_file_string = f.read()
    tree = javalang.parse.parse(java_file_string)
    package_name = tree.package.name
    print("Package = " + package_name)

    extracted_methods = []
    for klass in tree.types:
        if not type(klass) is javalang.tree.ClassDeclaration:
            continue
        if klass.implements is None:
            continue
        # if not any(x.name == "CLIJMacroPlugin" for x in klass.implements):
        #     continue
        print("Type = " + klass.name)

        class_id = package_name + "-" + klass.name
        for annotation in klass.annotations:
            if annotation.element is None:
                continue
            for element in annotation.element:
                if element.name == "name" and element.value is not None:
                    class_id = str(element.value.value[1:-1])

        print("Base Id = " + class_id)
        static_methods = [method for method in klass.methods if "static" in method.modifiers]

        for method in static_methods:

            if any([x.name == "Deprecated" for x in method.annotations]):
                print("Skipping deprecated method")
                continue

            print("Method = " + method.name)

            input_slots = bidict()
            output_slots = bidict()
            parameters = bidict()

            for index, parameter in enumerate(method.parameters):
                if parameter.type.name == "CLIJ2":
                    print("Skipping parameter " + parameter.type.name + " (Initial CLIJ2 instance)")
                    continue
                if is_image_type(parameter.type):
                    if index == len(method.parameters) - 1 and len(output_slots) == 0:
                        print("WARNING: No explicit output slot found. Assuming that " + parameter.name + " will contain the output!")
                        output_slots[index] = parameter.name
                    else:
                        if is_output_parameter(parameter):
                            print("Detected output slot " + parameter.name)
                            output_slots[index] = parameter.name
                        else:
                            print("Detected input slot " + parameter.name)
                            input_slots[index] = parameter.name
                else:
                    print("Detected parameter " + parameter.name + " of type " + parameter.type.name)
                    parameters[index] = parameter.name

            if len(output_slots) > 0 and len(input_slots) > 0:
                extracted_methods.append({
                    "package": package_name,
                    "class": klass,
                    "class_id" : class_id,
                    "method": method,
                    "input_slots": input_slots,
                    "output_slots": output_slots,
                    "parameters": parameters
                })

    return extracted_methods


def make_unique_string(s, existing):
    counter = 0
    original = s
    while s in existing:
        counter += 1
        s = original + "-" + str(counter)
    return s


def generate_unique_names(extracted_methods):
    existing_names = set()
    by_class = groupby(extracted_methods, lambda x: x["class_id"])

    for class_id, methods_ in by_class:
        methods = list(methods_)
        if len(methods) == 1:
            id = make_unique_string(class_id, existing_names)
            methods[0]["id"] = id
            existing_names.add(id)
            print("Unique name = " + id)
        else:
            # Chose the one with the most parameters
            methods.sort(key=lambda x: len(x["parameters"]))
            method = methods[-1]
            id = make_unique_string(class_id, existing_names)
            for method in methods:
                method["id"] = None
            methods[-1]["id"] = id
            existing_names.add(id)

            print("Choosing " + str(method) + " (" + str(len(method["parameters"])) + ")")
            print("Available were: " + str(methods))
            print("Available were: " + str([len(x["parameters"]) for x in methods]))


def generate_jipipe_class(method, class_name):
    documentation_name = " ".join([x.capitalize() for x in method["id"].replace("clij2:", "CLIJ2-").split("-")])\
        .replace("2d", "2D").replace("3d", "3D").replace("Clij2", "CLIJ2")
    documentation_description = ""

    if len(method["input_slots"]) > 1:
        base_class = "JIPipeIteratingAlgorithm"
    else:
        base_class = "JIPipeSimpleIteratingAlgorithm"

    slot_annotations = ""
    for name in method["input_slots"].values():
        slot_annotations += "@AlgorithmInputSlot(value = CLIJImageData.class, " \
                            "slotName = \"{name}\", autoCreate = true)\n".format(name=name)
    for name in method["output_slots"].values():
        slot_annotations += "@AlgorithmOutputSlot(value = CLIJImageData.class, " \
                            "slotName = \"{name}\", autoCreate = true)\n".format(name=name)

    class_code = """package org.hkijena.jipipe.extensions.clij2.algorithms;
    
    import org.hkijena.jipipe.api.parameters.JIPipeParameter;
    import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
    import org.hkijena.jipipe.api.JIPipeDocumentation;
    import org.hkijena.jipipe.api.JIPipeOrganization;
    import org.hkijena.jipipe.api.algorithm.*;
    import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
    import org.hkijena.jipipe.api.JIPipeValidityReport;
    import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
    import net.haesleinhuepf.clij2.CLIJ2;
    import java.util.function.Consumer;
    import java.util.function.Supplier;
    import {imported};
    
    @JIPipeDocumentation(name = "{name}", description="{description}")
    @JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
    {slots}
    public class {class_name} extends {base_class} {{
    """.format(name=documentation_name,
               description=documentation_description,
               slots=slot_annotations,
               class_name=class_name,
               base_class=base_class,
               imported=method["package"] + "." + method["class"].name)

    # Write parameter variables
    for index, parameter_name in method["parameters"].items():
        parameter_type = method["method"].parameters[index].type.name.lower()
        class_code += "{type} {name};\n".format(type=parameter_type, name=parameter_name)

    # Write constructors
    class_code += """\n\npublic {class_name}(JIPipeAlgorithmDeclaration declaration) {{
        super(declaration);
    }}
    
    public {class_name}({class_name} other) {{  
        super(other);  
    """.format(class_name=class_name)
    for index, parameter_name in method["parameters"].items():
        class_code += "this.{name} = other.{name};\n".format(name=parameter_name)
    class_code += "}\n\n"

    # Write workload
    class_code += "@Override\nprotected void runIteration(JIPipeDataBatch dataInterface," \
                  " JIPipeRunnerSubStatus subProgress," \
                  " Consumer<JIPipeRunnerSubStatus> algorithmProgress," \
                  " Supplier<Boolean> isCancelled) {\n"
    class_code += "CLIJ2 clij2 = CLIJ2.getInstance();\n"
    for parameter_name in method["input_slots"].values():
        class_code += 'ClearCLBuffer {name} = dataInterface.' \
                      'getInputData(getInputSlot("{name}"), CLIJImageData.class).getImage();\n'.format(name=parameter_name)
    reference_input_name = list(method["input_slots"].values())[0]
    for parameter_name in method["output_slots"].values():
        class_code += 'ClearCLBuffer {name} = clij2.create({reference});\n'.format(reference=reference_input_name,
                                                                                  name=parameter_name)
    class_code += method["class"].name + "." + method["method"].name + "(" + ", ".join([x.name for x in method["method"].parameters]) + ");\n\n"

    for parameter_name in method["output_slots"].values():
        class_code += 'dataInterface.addOutputData(getOutputSlot("{name}"),' \
                      ' new CLIJImageData({name}));\n'.format(name=parameter_name)

    class_code += "}\n\n"

    # Write getters & setters
    for index, parameter_name in method["parameters"].items():
        parameter_type = method["method"].parameters[index].type.name.lower()
        parameter_key = inflection.underscore(parameter_name).replace("_", "-")
        class_code += """@JIPipeParameter("{key}")
        public {type} get{cname}() {{
            return {name};
        }}
        
        @JIPipeParameter("{key}")
        public void set{cname}({type} value) {{
            this.{name} = value;
        }}
        
        """.format(type=parameter_type, name=parameter_name, cname=parameter_name[0].upper() + parameter_name[1:], key=parameter_key)

    class_code += "}"

    # Write to file
    with open(target_dir / Path(class_name + ".java"), "w") as f:
        f.write(class_code)

def generate_jipipe_classes(extracted_methods):
    registration_code = ""
    for method in extracted_methods:
        if method["id"] is None:
            continue
        method["id"] = "clij2:" + inflection.underscore(method["id"].replace("CLIJ2_", ""))\
            .replace("_", "-")\
            .replace("2-d", "-2d").replace("3-d", "-3d")
        class_name = "".join([x.capitalize() for x in method["id"].replace(":", "-").split("-")])
        registration_code += "registerAlgorithm(\"{id}\", " \
                             "{class_name}.class, " \
                             "UIUtils.getAlgorithmIconURL(\"clij.png\"));\n".format(id=method["id"],
                                                                                  class_name=class_name)
        generate_jipipe_class(method, class_name)

    with open(target_dir / Path("registration.java"), "w") as f:
        f.write(registration_code)


# Process all java files
extracted_methods = []
for java_file in glob(str(src_dir) + "/**/*.java", recursive=True):
    print(java_file)
    extracted_methods.extend(extract_methods(java_file))
generate_unique_names(extracted_methods)
generate_jipipe_classes(extracted_methods)
