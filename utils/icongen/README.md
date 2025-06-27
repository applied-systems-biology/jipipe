# IconGen

## Environment setup

```bash
conda env create -f environment.yml
```

## Running the generators

```bash
conda activate icon-recolor
python generate-icon-variants.py --color 6c707e --input-variant light inputs/light outputs/light
python generate-icon-variants.py --color ced0d6 --input-variant dark inputs/dark outputs/dark
```
