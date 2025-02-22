# Known Issues

This document lists known issues in the project and possible workarounds.

## 🚨 Issue: `NoSuchMethodError` for `getLauncherUserInfo()`

- **Cause:** The method `getLauncherUserInfo()` is missing in some Android versions.
- **Workaround:** Use `UserManager.isManagedProfile()` with a compatibility check.
  See [this discussion](https://developer.android.com/reference/android/os/UserManager#isManagedProfile()).

## 🛠️ Issue: `isManagedProfile()` Requires API 30+

- **Problem:** `UserManager.isManagedProfile(userHandle)` requires API 30, but our `minSdkVersion` is lower.
- **Solution:** Use reflection for API < 30. See the implementation in [
  `AppDrawerAdapter.kt`](./app/src/main/java/com/github/droidworksstudio/mlauncher/ui/AppDrawerAdapter.kt).

---

### Reporting Issues

If you encounter additional issues, please open a [GitHub Issue](https://github.com/DroidWorksStudio/mLauncher/issues) or contribute a fix.

