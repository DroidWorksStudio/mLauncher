#!/bin/bash

# Create output directory if it doesn't exist
mkdir -p patched-fonts/

# Loop through each file in the unpatched-fonts directory
for font in unpatched-fonts/*; do
    # Run the font-patcher command on the current file
    python3 font-patcher --complete --outputdir patched-fonts/ "$font" --custom ~/.local/share/fonts/siji.ttf
done

echo "All fonts have been patched successfully!"

