# Theme Folder

This folder contains exported themes for the mLauncher. Themes are stored as `.mtheme` files, which are JSON-based and contain the theme-related
`SharedPreferences` settings.

## Exporting a Theme

1. Open the mLauncher settings.
2. Navigate to the advanced section.
3. Clcik `Theme Creations` then Export.
4. Choose a filename (must have the `.mtheme` extension).
5. Save the file to your desired location.

## Importing a Theme

1. Open the mLauncher settings.
2. Navigate to the advanced section.
3. Clcik `Theme Creations` then Import.
4. Select an `.mtheme` file from your device.
5. The theme settings will be applied after verification.

## `.mtheme` File Format

- The `.mtheme` file is an XML file containing key-value pairs corresponding to `SharedPreferences` settings.
- Keys follow the format `keyName` to ensure compatibility with the launcher.
- Example:
    - [Download Dracula Theme](dracula.mtheme)

## Notes

- Only valid `.mtheme` files will be imported.
- If an invalid file is detected, an error message will be shown.

For any issues, refer to the launcher documentation or contact support.
