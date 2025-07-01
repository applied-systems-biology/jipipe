class: Workflow
cwlVersion: v1.2
doc: Reverse the lines in a document, then sort those lines.
inputs:
- {doc: The input file to be processed., id: input, type: File}
- {default: true, doc: 'If true, reverse (decending) sort', id: reverse_sort, type: boolean}
outputs:
- {doc: The output with the lines reversed and sorted., id: output, outputSource: sort/sorted,
  type: File}
requirements:
- {class: SubworkflowFeatureRequirement}
- {class: InlineJavascriptRequirement}
steps:
- id: rev
  in:
  - {id: input, source: input}
  out: [output]
  run:
    baseCommand: rev
    class: CommandLineTool
    doc: Reverse each line using the `rev` command
    inputs:
    - id: input
      inputBinding: {}
      type: File
    outputs:
    - id: output
      outputBinding: {glob: output.txt}
      type: File
    requirements:
    - {class: InlineJavascriptRequirement}
    stdout: output.txt
- id: sort
  in:
  - {id: input, source: rev/output}
  - {id: reverse, source: reverse_sort}
  out: [sorted]
  run:
    class: Operation
    doc: Sort the lines of the file
    id: sort
    inputs:
    - {doc: The input file to be sorted., id: input, type: File}
    - {default: true, doc: 'If true, reverse (decending) sort', id: reverse, type: boolean}
    outputs:
    - {doc: The sorted file, id: sorted, type: File}
    requirements:
    - {class: InlineJavascriptRequirement}
