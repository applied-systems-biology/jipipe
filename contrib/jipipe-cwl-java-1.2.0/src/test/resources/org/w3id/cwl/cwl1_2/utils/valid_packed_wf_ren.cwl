class: Workflow
cwlVersion: v1.2
inputs:
- default: {class: File, location: whale.txt}
  id: f1
  type: File
- {default: badger.txt, id: newname, type: string}
outputs: []
requirements:
- {class: StepInputExpressionRequirement}
- {class: InlineJavascriptRequirement}
steps:
- id: rename
  in:
  - {id: f1, source: f1}
  - {id: newname, source: newname}
  out: [out]
  run:
    baseCommand: 'true'
    class: CommandLineTool
    cwlVersion: v1.2
    inputs:
    - {id: srcfile, type: File}
    - {id: newname, type: string}
    outputs:
    - id: outfile
      outputBinding: {glob: $(inputs.newname)}
      type: File
    requirements:
    - class: InitialWorkDirRequirement
      listing:
      - {entry: $(inputs.srcfile), entryname: $(inputs.newname)}
- id: echo
  in:
  - {id: p, source: rename/out}
  - {id: checkname, source: newname}
  out: []
  run:
    arguments: [sh, -c, "name=`basename $(inputs.p.path)`\nls -l $(inputs.p.path)\nif
        test $name = $(inputs.checkname) ; then\n  echo success\nelse\n  echo expected
        basename to be $(inputs.checkname) but was $name\n  exit 1\nfi\n"]
    class: CommandLineTool
    cwlVersion: v1.2
    inputs:
    - {id: p, type: File}
    - {id: checkname, type: string}
    outputs: []
    requirements: []
