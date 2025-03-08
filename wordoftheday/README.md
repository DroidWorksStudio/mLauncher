# WordOfTheDay Folder

This folder contains exported WordOfTheDay for the mLauncher. WordOfTheDay are stored as `.json` files, which are JSON-based and contain the WordOfTheDay related
`SharedPreferences` settings.

## Importing a WordOfTheDay

1. Open the mLauncher settings.
2. Navigate to the advanced section.
3. Click `Word of The Day` to Import.
4. Select an `.json` file from your device.
5. The WordOfTheDay settings will be applied after verification.

## `.json` File Format

- The `.json` file is an XML file containing key-value pairs corresponding to `SharedPreferences` settings.
- Keys follow the format `keyName` to ensure compatibility with the launcher.
- Examples:
    - [Download bible WordOfTheDay](bible.json)
    - [Download fortune_cookies WordOfTheDay](fortune_cookies.json)
    - [Download random_facts WordOfTheDay](random_facts.json)
    - [Download stoic WordOfTheDay](stoic.json)


## Notes

- Only valid `.json` files will be imported.
- If an invalid file is detected, an error message will be shown.

For any issues, refer to the launcher documentation or contact support.
