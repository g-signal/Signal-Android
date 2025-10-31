#!/usr/bin/env python3
"""
Convert PNG to Android Vector Drawable
"""
from PIL import Image
import numpy as np

# Read the image
img = Image.open(r'C:\Users\Administrator\Desktop\signal_android_cer\1280X1280.png')

# Convert to RGBA if not already
img = img.convert('RGBA')

# Get image data
width, height = img.size
pixels = np.array(img)

# Get the primary color (most common non-transparent color)
# Flatten the array and filter out transparent pixels
non_transparent = pixels[pixels[:,:,3] > 128]
if len(non_transparent) > 0:
    # Get average color
    avg_color = non_transparent[:,:3].mean(axis=0).astype(int)
    color_hex = '#{:02X}{:02X}{:02X}'.format(avg_color[0], avg_color[1], avg_color[2])
    print(f"Primary color: {color_hex}")
    print(f"Image size: {width}x{height}")

    # Find bounding box of non-transparent pixels
    alpha = pixels[:,:,3]
    rows = np.any(alpha > 128, axis=1)
    cols = np.any(alpha > 128, axis=0)
    ymin, ymax = np.where(rows)[0][[0, -1]]
    xmin, xmax = np.where(cols)[0][[0, -1]]

    print(f"Content bounds: ({xmin}, {ymin}) to ({xmax}, {ymax})")
    print(f"Content size: {xmax-xmin}x{ymax-ymin}")

# Save a simplified version for analysis
img.save(r'E:\code\Signal-Android\temp_analysis.png')
print("\nImage saved for analysis")
