arguments: ['mkdir -p foo

    touch baz.txt

    touch foo/bar.txt

    ']
baseCommand: [bash, -c]
class: CommandLineTool
cwlVersion: v1.2
inputs: []
outputs:
- id: stuff
  outputBinding: {glob: $(runtime.outdir)}
  type: Directory
requirements: []
