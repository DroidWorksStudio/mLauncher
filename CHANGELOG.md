# Changelog

All notable changes to this project will be documented in this file. See [conventional commits](https://www.conventionalcommits.org/) for commit guidelines.

## [Coming Soon](https://github.com/DroidWorksStudio/mLauncher/tree/main) - TBD

### Refactoring:
* Refactor(changelog): Update changelog generation script
* Refactor(changelog): Enhance changelog generation and formatting
* Refactor: Update changelog script path in workflow
* Refactor: Automate changelog generation with custom script

## [1.11.0.0](https://github.com/DroidWorksStudio/mLauncher/tree/1.11.0.0) - (07, September 2025)

### Language Support:

* Lang: Updated Language Files. ([#878](https://github.com/DroidWorksStudio/mLauncher/pull/878))

### Refactoring:

* Refactor: Enhance Contact List and App Drawer Functionality
* Refactor: Use app name for label in app drawer

### Implemented Enhancements:

* Feat: Add Contact Search and Calling Functionality

### Bug Fixes:

* Fix: Conditional private space receiver unregistration

## [1.10.9.2](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.9.2) - (05, September 2025)

### Language Support:

* Lang: Updated Language Files. ([#874](https://github.com/DroidWorksStudio/mLauncher/pull/874))

### Refactoring:

* Refactor: Add manual location and temperature unit settings for weather
* Refactor: Extract weather logic to WeatherHelper
* Refactor: Optimize HomeFragment lifecycle and receivers

### Bug Fixes:

* Fix: Add null check for view in HomeFragment.onResume

## [1.10.9.1](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.9.1) - (03, September 2025)

### Language Support:

* Lang: Updated Language Files. ([#871](https://github.com/DroidWorksStudio/mLauncher/pull/871))

### Bug Fixes:

* fix(app): Improve app tag handling for multi-user support

### Refactoring:

* Refactor: Enhance multi-profile support for app tags
* Refactor: Improve Fuzzy Finder and Search Logic

## [1.10.8.7](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.7) - (22, August 2025)

### Releases:

* Release: Increment build number
* Release: Increment build number

### Implemented Enhancements:

* feat: Disable inclusion of dependency info in APKs and Bundles

### Refactoring:

* Refactor: Improve icon loading and caching logic
* Refactor: Optimize icon loading logic in AppDrawerAdapter
* Refactor: Optimize icon handling and profile indicators in AppDrawerAdapter
* Refactor: Standardize theme and language selection logic
* Refactor: Use application context for Prefs initialization

## [1.10.8.5](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.5) - (15, July 2025)

### Refactoring:

* Refactor: Simplify top padding logic in setTopPadding
* Refactor: Defer status/navigation bar visibility changes until view is ready

### Releases:

* Release: Bump build number to 5

## [1.10.8.4](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.4) - (15, July 2025)

### Refactoring:

* Refactor: Improve app drawer icon loading with caching and asynchronous loading
* Refactor: Introduce API 29 specific styles
* Refactor: Improve theme handling and AndroidManifest clarity
* Refactor: Simplify status bar height retrieval for top padding
* Refactor: Improve status bar height calculation and top padding adjustment
* Refactor: Update status bar handling and padding
* Refactor: Remove unused parameter from updatePagesAndAppsPerPage logging
* Refactor: Improve icon loading and fallback in HomeFragment and AppDrawerAdapter

### Bug Fixes:

* Fix: Remove location update timeout in WeatherReceiver
* Fix: Optimize location updates for battery life

### Language Support:

* Lang: Updated Language Files. ([#845](https://github.com/DroidWorksStudio/mLauncher/pull/845))
* Lang: Updated Language Files. ([#844](https://github.com/DroidWorksStudio/mLauncher/pull/844))
* Lang: Updated Language Files. ([#840](https://github.com/DroidWorksStudio/mLauncher/pull/840))

### Implemented Enhancements:

* Feat: Add option to toggle navigation bar visibility
* feat(app-drawer): Add option to open app on Enter key press

### Versioning:

* Version: Increment build number to 4

## [1.10.8.3](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.3) - (13, July 2025)

### Bug Fixes:

* Fix(workflows): Use `GITHUB_TOKEN` for `dependabot` actions
* fix(deps): Remove dependabot group names
* fix(dependabot): Group all dependency updates
* Fix(AppDrawer): Only show "Personal apps" header if other profiles exist

### Feature Removal:

* Remove deprecated workflow

### Continuous Integration (CI):

* CI: Configure dependabot for GitHub Actions

### Versioning:

* Version: Increment build number to 3

### Refactoring:

* Refactor(Onboarding): Implement ViewBinding in OnboardingPageFragment
* Refactor: Nullify views in OnboardingPageFragment

## [1.10.8.2](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.2) - (12, July 2025)

### Bug Fixes:

* Fix(WeatherReceiver): Annotate fields instead of properties in weather data classes
* Fix(AppDrawer): Prevent header items from attempting to load icons

### Chore:

* Chore(Build): Update Kotlin toolchain and increment build number

### Language Support:

* Lang: Updated Language Files. ([#837](https://github.com/DroidWorksStudio/mLauncher/pull/837))

### Implemented Enhancements:

* Feat: Show different toast when home apps locked.

### Refactoring:

* Refactor: Rename "NORMAL" profile type to "SYSTEM"

## [1.10.8.1](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.1) - (11, July 2025)

### Bug Fixes:

* Fix: Prevent crash when opening app drawer with private space unlocked

### Versioning:

* Version: Increment build number to 1

### Implemented Enhancements:

* feat(app_drawer): Implement Profile-Based App Separation

---
> Generated by DroidWorksStudio