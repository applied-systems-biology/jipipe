import argparse
from pathlib import Path
import numpy as np
from PIL import Image
import colorsys
from skimage.color import rgb2lab


def is_mostly_monochrome(img: Image.Image, threshold_std=30, sample_size=5000) -> bool:
    img = img.convert("RGBA")
    pixels = np.array(img)
    alpha_mask = pixels[..., 3] > 0
    rgb_pixels = pixels[..., :3][alpha_mask]

    if len(rgb_pixels) == 0:
        return False

    if len(rgb_pixels) > sample_size:
        idx = np.random.choice(len(rgb_pixels), sample_size, replace=False)
        rgb_pixels = rgb_pixels[idx]

    lab_pixels = rgb2lab(rgb_pixels[np.newaxis, :, :].astype(np.uint8)).reshape(-1, 3)
    std = np.std(lab_pixels, axis=0)
    return np.mean(std) < threshold_std


def recolor_based_on_variant(img: Image.Image, new_color: tuple[int, int, int],
                             sat_threshold: float, variant: str) -> Image.Image:
    img = img.convert("RGBA")
    arr = np.array(img).astype(np.uint8)

    for y in range(arr.shape[0]):
        for x in range(arr.shape[1]):
            r, g, b, a = arr[y, x]
            if a == 0:
                continue

            h, s, v = colorsys.rgb_to_hsv(r / 255., g / 255., b / 255.)

            if s < sat_threshold:
                # Base pixel brightness
                if (variant == "light" and v < 0.5) or (variant == "dark" and v > 0.5):
                    arr[y, x, 0:3] = new_color

    return Image.fromarray(arr, 'RGBA')



def process_icons_recursive(source_dir: Path, target_dir: Path, new_color: tuple[int, int, int],
                            sat_threshold: float, std_threshold: float, valid_exts: set[str], variant: str):
    source_dir = source_dir.resolve()
    target_dir = target_dir.resolve()

    for file_path in source_dir.rglob("*"):
        if not file_path.is_file() or file_path.suffix.lower()[1:] not in valid_exts:
            continue

        relative_path = file_path.relative_to(source_dir)
        output_path = target_dir / relative_path
        output_path.parent.mkdir(parents=True, exist_ok=True)

        try:
            img = Image.open(file_path)

            if is_mostly_monochrome(img, threshold_std=std_threshold):
                img = recolor_based_on_variant(img, new_color, sat_threshold, variant)
                img.save(output_path)
                print(f"[✓] Recolored: {relative_path}")
            else:
                img.save(output_path)
                print(f"[=] Skipped (colorful): {relative_path}")
        except Exception as e:
            print(f"[!] Error with {relative_path}: {e}")


def parse_hex_color(s: str) -> tuple[int, int, int]:
    s = s.strip().lstrip('#')
    if len(s) != 6:
        raise argparse.ArgumentTypeError("Color must be in hex format like '#RRGGBB' or 'RRGGBB'.")
    try:
        return tuple(int(s[i:i+2], 16) for i in (0, 2, 4))
    except ValueError:
        raise argparse.ArgumentTypeError("Invalid hex color format.")


def main():
    parser = argparse.ArgumentParser(description="Recolor mostly monochrome icons, preserving color accents.")
    parser.add_argument("source", type=Path, help="Source directory (recursive).")
    parser.add_argument("target", type=Path, help="Target output directory.")
    parser.add_argument("--color", required=True, type=parse_hex_color,
                        help="New primary color in hex format (e.g. '#DC143C').")
    parser.add_argument("--sat-threshold", type=float, default=0.25,
                        help="Saturation threshold below which pixels are recolored (default: 0.25).")
    parser.add_argument("--std-threshold", type=float, default=30,
                        help="LAB color std-dev threshold for monochrome detection (default: 30).")
    parser.add_argument("--ext", type=str, default="png,jpg,jpeg",
                        help="Comma-separated list of valid extensions (default: png,jpg,jpeg).")
    parser.add_argument("--input-variant", choices=["light", "dark"], required=True,
                    help="Which variant the input icons are: 'light' = dark gray base for light themes, "
                         "'dark' = light gray base for dark themes.")


    args = parser.parse_args()
    valid_exts = set(ext.strip().lower() for ext in args.ext.split(","))

    process_icons_recursive(
        source_dir=args.source,
        target_dir=args.target,
        new_color=args.color,
        sat_threshold=args.sat_threshold,
        std_threshold=args.std_threshold,
        valid_exts=valid_exts,
        variant=args.input_variant
    )


if __name__ == "__main__":
    main()
