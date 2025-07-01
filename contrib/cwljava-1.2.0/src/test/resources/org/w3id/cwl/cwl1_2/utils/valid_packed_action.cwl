arguments: [echo, $(inputs.initial_file.basename)]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: initial_file, type: File}
- {id: out_file_name, type: string}
outputs:
- {id: processed_file, type: stdout}
requirements:
- {class: InlineJavascriptRequirement}
stdout: $(inputs.out_file_name)
