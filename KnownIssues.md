# Known Issues

This document lists known issues in the project and possible workarounds.

## 🚨 Issue: `NoSuchMethodError` for `getLauncherUserInfo()`

- **Cause:** The method `getLauncherUserInfo()` is missing in some Android versions.
- **Fix Coming Soon:** Use `UserManager.isManagedProfile()` with a compatibility check.
  See [this discussion](https://developer.android.com/reference/android/os/UserManager#isManagedProfile()).

## 🛠️ Issue: `isManagedProfile()` Requires API 30+

- **Problem:** `UserManager.isManagedProfile(userHandle)` requires API 30, but our `minSdkVersion` is lower.
- **Fix Coming Soon:** Use reflection for API < 30. See the implementation in [
  `AppDrawerAdapter.kt`](./app/src/main/java/com/github/droidworksstudio/mlauncher/ui/AppDrawerAdapter.kt).

## 📱 Issue: 3-Button Navigation Doesn't Leave Space at the Bottom of Settings

- **Cause:** When 3-button navigation is enabled, the bottom area of the settings screen is not properly adjusted, leading to content being cut off orobscured.
- **Solution:** Use **gesture navigation** as a workaround, which doesn't have the same issue with screen space. To enable gesture navigation, go to **Settings > System > Gestures > System navigation** and choose **Gesture navigation**.
- **Fix Coming Soon:** A fix is in progress, and we will add a spacer to the bottom of the pages to resolve this issue.

---

### Reporting Issues

If you encounter additional issues, please open a [GitHub Issue](https://github.com/DroidWorksStudio/mLauncher/issues) or contribute a fix.
