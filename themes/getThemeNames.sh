#!/bin/bash

# Loop through all .mtheme files in the current directory
for file in *.mtheme; do
  # Check if the file exists to avoid errors if no .mtheme files are found
  if [ -f "$file" ]; then
    # Output the file in the desired format
    echo "    - [Download $(basename "$file" .mtheme) Theme]($file)"
  fi
done

