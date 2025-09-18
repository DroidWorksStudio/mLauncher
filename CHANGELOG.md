# Changelog

All notable changes to this project will be documented in this file. See [conventional commits](https://www.conventionalcommits.org/) for commit guidelines.

## [Coming Soon](https://github.com/DroidWorksStudio/mLauncher/tree/main) - TBD

### Refactors:

* Rename screen time string resource ([44a17131](https://github.com/DroidWorksStudio/mLauncher/commit/44a17131))

### Documentation & Language:

* Updated Language Files. ([#899](https://github.com/DroidWorksStudio/mLauncher/pull/899)) ([703ced86](https://github.com/DroidWorksStudio/mLauncher/commit/703ced86))

## [1.11.1.1](https://github.com/DroidWorksStudio/mLauncher/tree/1.11.1.1) - (17, September 2025)

### Enhancements:

* Enhance Profile Handling and App List Filtering ([2cdb6900](https://github.com/DroidWorksStudio/mLauncher/commit/2cdb6900))

### Refactors:

* Improve Home Apps Widget Layout and Logic ([b3a917cc](https://github.com/DroidWorksStudio/mLauncher/commit/b3a917cc))
* Standardize widget update and FAB action handling ([66641117](https://github.com/DroidWorksStudio/mLauncher/commit/66641117))

### Build:

* Bump rexml in the bundler group across 1 directory ([#897](https://github.com/DroidWorksStudio/mLauncher/pull/897)) ([abc9f006](https://github.com/DroidWorksStudio/mLauncher/commit/abc9f006))

### Feature Removals:

* Remove unused onStatusChanged method ([dbf85b34](https://github.com/DroidWorksStudio/mLauncher/commit/dbf85b34))

## [1.11.1.0](https://github.com/DroidWorksStudio/mLauncher/tree/1.11.1.0) - (15, September 2025)

### Enhancements:

* Introduce BaseFragment and SystemBarObserver ([56066cf4](https://github.com/DroidWorksStudio/mLauncher/commit/56066cf4))

### Bug Fixes:

* Correct app label display in HomeAppsWidgetProvider ([43c169f1](https://github.com/DroidWorksStudio/mLauncher/commit/43c169f1))

### Refactors:

* Remove package attribute from AndroidManifest.xml ([3d35e4a6](https://github.com/DroidWorksStudio/mLauncher/commit/3d35e4a6))
* Improve Discord release message formatting ([1eaa194b](https://github.com/DroidWorksStudio/mLauncher/commit/1eaa194b))
* Improve app list sorting ([543106e6](https://github.com/DroidWorksStudio/mLauncher/commit/543106e6))
* Replace Word of the Day AlarmManager with WorkManager ([a61c988d](https://github.com/DroidWorksStudio/mLauncher/commit/a61c988d))
* Remove SecurityService and FLAG_SECURE ([ca71b87d](https://github.com/DroidWorksStudio/mLauncher/commit/ca71b87d))
* Update cleanMessage regex to include 'docs' ([1bedc196](https://github.com/DroidWorksStudio/mLauncher/commit/1bedc196))
* Simplify Discord release message ([840197f7](https://github.com/DroidWorksStudio/mLauncher/commit/840197f7))

### Documentation & Language:

* Updated Language Files. ([#893](https://github.com/DroidWorksStudio/mLauncher/pull/893)) ([a4943d6c](https://github.com/DroidWorksStudio/mLauncher/commit/a4943d6c))
* Update Discord invite link ([04db3aca](https://github.com/DroidWorksStudio/mLauncher/commit/04db3aca))

### Build:

* Update Kotlin and KSP versions and increment patch version ([b9814bb2](https://github.com/DroidWorksStudio/mLauncher/commit/b9814bb2))

## [1.11.0.2](https://github.com/DroidWorksStudio/mLauncher/tree/1.11.0.2) - (14, September 2025)

### Enhancements:

* Implement Discord release notifier script ([bbc375f8](https://github.com/DroidWorksStudio/mLauncher/commit/bbc375f8))
* Enhance home apps widget with icons and improve update mechanism ([a2c66fc6](https://github.com/DroidWorksStudio/mLauncher/commit/a2c66fc6))
* Implement Home, FAB, and Word of the Day widgets ([612d8879](https://github.com/DroidWorksStudio/mLauncher/commit/612d8879))
* Add Czech language support ([31a0be38](https://github.com/DroidWorksStudio/mLauncher/commit/31a0be38))

### Bug Fixes:

* Correct Language Enum Order ([2121c458](https://github.com/DroidWorksStudio/mLauncher/commit/2121c458))

### Refactors:

* Reorganize commit groups in changelog ([ea6309c3](https://github.com/DroidWorksStudio/mLauncher/commit/ea6309c3))
* Order changelog groups by commit type ([7f41df17](https://github.com/DroidWorksStudio/mLauncher/commit/7f41df17))
* Move UI adapters to dedicated package ([c2ac5fc4](https://github.com/DroidWorksStudio/mLauncher/commit/c2ac5fc4))

### Documentation & Language:

* Updated Language Files. ([#885](https://github.com/DroidWorksStudio/mLauncher/pull/885)) ([c6f46f39](https://github.com/DroidWorksStudio/mLauncher/commit/c6f46f39))

### Build:

* Bump AndroidX libraries ([5fb96e33](https://github.com/DroidWorksStudio/mLauncher/commit/5fb96e33))

## [1.11.0.1](https://github.com/DroidWorksStudio/mLauncher/tree/1.11.0.1) - (09, September 2025)

### Enhancements:

* Add Mojeek, Qwant, Seznam, and Yandex search engines ([59c553d6](https://github.com/DroidWorksStudio/mLauncher/commit/59c553d6))
* Add StartPage as a search engine option ([314f75b8](https://github.com/DroidWorksStudio/mLauncher/commit/314f75b8))
* Add haptic feedback for UI interactions ([efc8715f](https://github.com/DroidWorksStudio/mLauncher/commit/efc8715f))
* Add SecurityService to detect debug mode and set FLAG_SECURE ([5637c5da](https://github.com/DroidWorksStudio/mLauncher/commit/5637c5da))

### Refactors:

* Centralize search engine URLs and logging ([62dbf6cc](https://github.com/DroidWorksStudio/mLauncher/commit/62dbf6cc))
* Consolidate permission requests and remove unused code ([f30016f9](https://github.com/DroidWorksStudio/mLauncher/commit/f30016f9))
* Improve conventional commit type removal ([dcddfbf9](https://github.com/DroidWorksStudio/mLauncher/commit/dcddfbf9))
* Enhance changelog generation and formatting ([331e3af5](https://github.com/DroidWorksStudio/mLauncher/commit/331e3af5))
* Update changelog script path in workflow ([8738766d](https://github.com/DroidWorksStudio/mLauncher/commit/8738766d))
* Automate changelog generation with custom script ([5634945d](https://github.com/DroidWorksStudio/mLauncher/commit/5634945d))

### Documentation & Language:

* Updated Language Files. ([#883](https://github.com/DroidWorksStudio/mLauncher/pull/883)) ([4c8a3260](https://github.com/DroidWorksStudio/mLauncher/commit/4c8a3260))

### Build:

* Bump com.google.android.material:material ([#881](https://github.com/DroidWorksStudio/mLauncher/pull/881)) ([2f3ecb2c](https://github.com/DroidWorksStudio/mLauncher/commit/2f3ecb2c))
* Bump the all-actions group with 3 updates ([#882](https://github.com/DroidWorksStudio/mLauncher/pull/882)) ([f15f5ac2](https://github.com/DroidWorksStudio/mLauncher/commit/f15f5ac2))

## [1.11.0.0](https://github.com/DroidWorksStudio/mLauncher/tree/1.11.0.0) - (07, September 2025)

### Enhancements:

* Add Contact Search and Calling Functionality ([0f7708c7](https://github.com/DroidWorksStudio/mLauncher/commit/0f7708c7))

### Bug Fixes:

* Conditional private space receiver unregistration ([cdb88c1c](https://github.com/DroidWorksStudio/mLauncher/commit/cdb88c1c))

### Refactors:

* Enhance Contact List and App Drawer Functionality ([fab60dc8](https://github.com/DroidWorksStudio/mLauncher/commit/fab60dc8))
* Use app name for label in app drawer ([58be2d44](https://github.com/DroidWorksStudio/mLauncher/commit/58be2d44))

### Documentation & Language:

* Updated Language Files. ([#878](https://github.com/DroidWorksStudio/mLauncher/pull/878)) ([c63c99a1](https://github.com/DroidWorksStudio/mLauncher/commit/c63c99a1))

## [1.10.9.2](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.9.2) - (05, September 2025)

### Bug Fixes:

* Add null check for view in HomeFragment.onResume ([66425a6a](https://github.com/DroidWorksStudio/mLauncher/commit/66425a6a))

### Refactors:

* Add manual location and temperature unit settings for weather ([858c38be](https://github.com/DroidWorksStudio/mLauncher/commit/858c38be))
* Extract weather logic to WeatherHelper ([0928fe69](https://github.com/DroidWorksStudio/mLauncher/commit/0928fe69))
* Optimize HomeFragment lifecycle and receivers ([fe430ad3](https://github.com/DroidWorksStudio/mLauncher/commit/fe430ad3))

### Documentation & Language:

* Updated Language Files. ([#874](https://github.com/DroidWorksStudio/mLauncher/pull/874)) ([631a5fd2](https://github.com/DroidWorksStudio/mLauncher/commit/631a5fd2))

## [1.10.9.1](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.9.1) - (03, September 2025)

### Bug Fixes:

* Improve app tag handling for multi-user support ([ffe8e4b4](https://github.com/DroidWorksStudio/mLauncher/commit/ffe8e4b4))

### Refactors:

* Enhance multi-profile support for app tags ([dccfa472](https://github.com/DroidWorksStudio/mLauncher/commit/dccfa472))
* Improve Fuzzy Finder and Search Logic ([63ce32da](https://github.com/DroidWorksStudio/mLauncher/commit/63ce32da))

### Documentation & Language:

* Updated Language Files. ([#871](https://github.com/DroidWorksStudio/mLauncher/pull/871)) ([6cc7db96](https://github.com/DroidWorksStudio/mLauncher/commit/6cc7db96))

### Build:

* Bump actions/setup-java in the all-actions group ([#863](https://github.com/DroidWorksStudio/mLauncher/pull/863)) ([b4a1c08c](https://github.com/DroidWorksStudio/mLauncher/commit/b4a1c08c))

## [1.10.8.7](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.7) - (22, August 2025)

### Enhancements:

* Disable inclusion of dependency info in APKs and Bundles ([d56a4ead](https://github.com/DroidWorksStudio/mLauncher/commit/d56a4ead))

### Refactors:

* Improve icon loading and caching logic ([cc34e843](https://github.com/DroidWorksStudio/mLauncher/commit/cc34e843))
* Optimize icon loading logic in AppDrawerAdapter ([e96a832c](https://github.com/DroidWorksStudio/mLauncher/commit/e96a832c))
* Optimize icon handling and profile indicators in AppDrawerAdapter ([b5e03e95](https://github.com/DroidWorksStudio/mLauncher/commit/b5e03e95))
* Standardize theme and language selection logic ([b660a685](https://github.com/DroidWorksStudio/mLauncher/commit/b660a685))
* Use application context for Prefs initialization ([4ed52b1a](https://github.com/DroidWorksStudio/mLauncher/commit/4ed52b1a))

### Build:

* Bump the all-gradle group with 13 updates ([#861](https://github.com/DroidWorksStudio/mLauncher/pull/861)) ([3b41fc11](https://github.com/DroidWorksStudio/mLauncher/commit/3b41fc11))
* Bump actions/checkout in the all-actions group ([#860](https://github.com/DroidWorksStudio/mLauncher/pull/860)) ([188c8e87](https://github.com/DroidWorksStudio/mLauncher/commit/188c8e87))
* Bump actions/cache in the all-actions group ([#858](https://github.com/DroidWorksStudio/mLauncher/pull/858)) ([79642fc7](https://github.com/DroidWorksStudio/mLauncher/commit/79642fc7))
* Bump the all-gradle group with 12 updates ([#856](https://github.com/DroidWorksStudio/mLauncher/pull/856)) ([f679d4dd](https://github.com/DroidWorksStudio/mLauncher/commit/f679d4dd))
* Bump orhun/git-cliff-action in the all-actions group ([#857](https://github.com/DroidWorksStudio/mLauncher/pull/857)) ([d3e515fa](https://github.com/DroidWorksStudio/mLauncher/commit/d3e515fa))
* Add null safety check for output in build.gradle.kts ([a8537d01](https://github.com/DroidWorksStudio/mLauncher/commit/a8537d01))
* Remove unnecessary safe call in output file naming ([5179b278](https://github.com/DroidWorksStudio/mLauncher/commit/5179b278))
* Bump org.apache.commons:commons-text ([#854](https://github.com/DroidWorksStudio/mLauncher/pull/854)) ([8fea090e](https://github.com/DroidWorksStudio/mLauncher/commit/8fea090e))
* Bump orhun/git-cliff-action in the all-actions group ([#855](https://github.com/DroidWorksStudio/mLauncher/pull/855)) ([66e93095](https://github.com/DroidWorksStudio/mLauncher/commit/66e93095))
* Bump the all-gradle group with 4 updates ([#853](https://github.com/DroidWorksStudio/mLauncher/pull/853)) ([90b3f972](https://github.com/DroidWorksStudio/mLauncher/commit/90b3f972))

### Meta:

* Increment build number ([1bb43a05](https://github.com/DroidWorksStudio/mLauncher/commit/1bb43a05))
* Increment build number ([1c0eeb45](https://github.com/DroidWorksStudio/mLauncher/commit/1c0eeb45))

## [1.10.8.5](https://github.com/DroidWorksStudio/mLauncher/tree/1.10.8.5) - (15, July 2025)

### Refactors:

* Simplify top padding logic in setTopPadding ([6511d97b](https://github.com/DroidWorksStudio/mLauncher/commit/6511d97b))
* Defer status/navigation bar visibility changes until view is ready ([3f4aa309](https://github.com/DroidWorksStudio/mLauncher/commit/3f4aa309))

### Meta:

* Bump build number to 5 ([b478d957](https://github.com/DroidWorksStudio/mLauncher/commit/b478d957))

---
> Generated by DroidWorksStudio