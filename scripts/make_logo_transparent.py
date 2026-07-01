"""Remove the baked-in checkerboard / light background from the generated
EduPresence logo and produce a cleanly cropped, truly transparent PNG."""
from PIL import Image
import sys

SRC = sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\Johnzyril\.cursor\projects\c-Users-Johnzyril-Desktop-ATTENDANCE-MANAGEMENT-SYSTEM\assets\edupresence-logo.png"
OUT = sys.argv[2] if len(sys.argv) > 2 else r"c:\Users\Johnzyril\Desktop\ATTENDANCE MANAGEMENT SYSTEM\src\main\resources\static\images\edupresence-logo.png"

img = Image.open(SRC).convert("RGBA")
px = img.load()
w, h = img.size

def is_background(r, g, b):
    # checkerboard = white and light-gray squares: light + low saturation (grayish)
    mn, mx = min(r, g, b), max(r, g, b)
    return mn >= 150 and (mx - mn) <= 32

for y in range(h):
    for x in range(w):
        r, g, b, a = px[x, y]
        if is_background(r, g, b):
            px[x, y] = (r, g, b, 0)
        else:
            mn, mx = min(r, g, b), max(r, g, b)
            # soften near-background pixels (anti-alias edges) to reduce halo
            if mn >= 120 and (mx - mn) <= 45:
                px[x, y] = (r, g, b, 90)

# autocrop to the visible logo bounds with a little padding
bbox = img.getbbox()
if bbox:
    pad = 24
    l, t, rr, bb = bbox
    l = max(0, l - pad); t = max(0, t - pad)
    rr = min(w, rr + pad); bb = min(h, bb + pad)
    img = img.crop((l, t, rr, bb))

img.save(OUT)
print(f"Saved transparent logo -> {OUT}  size={img.size}")
