class: Operation
cwlVersion: v1.2
doc: Reverse each line
id: reverse
inputs:
- {id: input, type: File}
outputs:
- {id: output, type: File}
requirements:
- {class: InlineJavascriptRequirement}
