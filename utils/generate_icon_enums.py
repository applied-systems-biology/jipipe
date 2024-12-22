#!/usr/bin/env python3

import os
import re

def generate_icon_enum(directory, output_file, class_name="IconEnum", package_name=None):
    """
    Generates a Java enum from PNG files found in the given directory.

    Args:
        directory (str): The root directory to scan for PNG files.
        output_file (str): The file to write the generated Java enum to.
        class_name (str): The name of the generated Java enum class.
        package_name (str, optional): The package name for the generated Java enum class.
    """
    # Function to format enum entry
    def format_enum_entry(relative_path, existing_names):
        # Split the path into components
        components = relative_path.split(os.sep)

        # Generate camelCase name for each component
        def to_camel_case(s):
            return re.sub(r'[^a-zA-Z0-9]', '', s.title())

        # Join all components into a single camelCase name
        base_name = ''.join(to_camel_case(comp) for comp in components[:-1]) + to_camel_case(components[-1].rsplit('.', 1)[0])

        # Ensure the name is unique by appending numbers if necessary
        enum_name = base_name
        counter = 1
        while enum_name in existing_names:
            enum_name = f"{base_name}{counter}"
            counter += 1

        existing_names.add(enum_name)
        
        # Return the formatted enum entry
        return f"    {enum_name}(\"{relative_path}\")"

    enum_entries = []
    existing_names = set()

    # Walk through the directory hierarchy
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".png"):
                # Get the relative path to the file
                relative_path = os.path.relpath(os.path.join(root, file), start=directory).replace("\\", "/")

                # Generate the enum entry
                enum_entries.append(format_enum_entry(relative_path, existing_names))

    # Join entries with a semicolon for the final enum list
    entries_string = ",\n".join(enum_entries) + ";"

    # Generate the package declaration if provided
    package_declaration = f"package {package_name};\n\n" if package_name else ""

    # Generate the full Java enum class
    enum_class = """{package}public enum {class_name} {{

{entries}

    private final String path;

    {class_name}(String path) {{
        this.path = path;
    }}

    public String getPath() {{
        return path;
    }}
}}
""".format(
        package=package_declaration,
        class_name=class_name,
        entries=entries_string
    )

    # Write the output to a file
    with open(output_file, "w") as f:
        f.write(enum_class)

    print(f"Java enum class '{class_name}' has been generated and saved to {output_file}")

# Example usage:
# generate_icon_enum("path/to/icons", "IconEnum.java", class_name="MyIconEnum", package_name="com.example.icons")




generate_icon_enum("../jipipe-core/src/main/resources/org/hkijena/jipipe/icons/", "../jipipe-core/src/main/java/org/hkijena/jipipe/utils/JIPipeIcons16.java", class_name="JIPipeIcons16", package_name="org.hkijena.jipipe.utils")
generate_icon_enum("../jipipe-core/src/main/resources/org/hkijena/jipipe/icons-32/", "../jipipe-core/src/main/java/org/hkijena/jipipe/utils/JIPipeIcons32.java", class_name="JIPipeIcons32", package_name="org.hkijena.jipipe.utils")
