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
