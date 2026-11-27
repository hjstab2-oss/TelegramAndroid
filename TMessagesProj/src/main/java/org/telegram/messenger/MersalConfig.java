package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MersalConfig {

    private static final String PREFS_NAME = "mersal_config";

    public static final String KEY_ANTI_DELETE = "mersal_anti_delete";
    public static final String KEY_DOWNLOAD_BOOST = "mersal_download_boost";
    public static final String KEY_FONT_MODE = "mersal_font_mode";
    public static final String KEY_THEME_MODE = "mersal_theme_mode";

    public static final String FONT_IBM_PLEX_ARABIC = "ibm_plex_arabic";
    public static final String FONT_SYSTEM = "system";

    public static final String THEME_DEFAULT = "default";
    public static final String THEME_ONYX_DARK = "onyx_dark";
    public static final String THEME_CRYSTAL_LIGHT = "crystal_light";

    public static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isAntiDeleteEnabled() {
        return getPrefs().getBoolean(KEY_ANTI_DELETE, true);
    }

    public static void setAntiDeleteEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_ANTI_DELETE, enabled).apply();
    }

    public static boolean isDownloadBoostEnabled() {
        return getPrefs().getBoolean(KEY_DOWNLOAD_BOOST, true);
    }

    public static void setDownloadBoostEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_DOWNLOAD_BOOST, enabled).apply();
    }

    public static String getFontMode() {
        return getPrefs().getString(KEY_FONT_MODE, FONT_IBM_PLEX_ARABIC);
    }

    public static boolean isIbmPlexFontEnabled() {
        return !FONT_SYSTEM.equals(getFontMode());
    }

    public static void setFontMode(String mode) {
        if (!FONT_SYSTEM.equals(mode)) {
            mode = FONT_IBM_PLEX_ARABIC;
        }
        getPrefs().edit().putString(KEY_FONT_MODE, mode).commit();
        AndroidUtilities.clearMersalTypefaces();
        Theme.refreshTypefaces();
    }

    public static String getThemeMode() {
        return getPrefs().getString(KEY_THEME_MODE, THEME_DEFAULT);
    }

    public static void setThemeMode(String mode) {
        if (!THEME_ONYX_DARK.equals(mode) && !THEME_CRYSTAL_LIGHT.equals(mode)) {
            mode = THEME_DEFAULT;
        }
        getPrefs().edit().putString(KEY_THEME_MODE, mode).apply();
    }

    public static void applyThemeMode(String mode, Activity activity) {
        setThemeMode(mode);
        if (THEME_ONYX_DARK.equals(mode)) {
            applyBundledTheme("mersal_liquid_glass_dark.attheme", "Onyx Liquid Glass", true);
        } else if (THEME_CRYSTAL_LIGHT.equals(mode)) {
            applyBundledTheme("mersal_liquid_glass_light.attheme", "Crystal Liquid Glass", false);
        } else {
            Theme.ThemeInfo fallback = Theme.getTheme(Theme.isCurrentThemeDark() ? "Dark Blue" : "Blue");
            if (fallback == null) {
                fallback = Theme.getTheme("Blue");
            }
            if (fallback != null) {
                Theme.applyTheme(fallback);
            }
        }
        if (activity != null) {
            try {
                activity.recreate();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private static void applyBundledTheme(String assetFileName, String themeName, boolean nightTheme) {
        try {
            File outFile = new File(ApplicationLoader.getFilesDirFixed(), assetFileName);
            try (InputStream in = ApplicationLoader.applicationContext.getAssets().open(assetFileName);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            if (!SharedConfig.chatBlurEnabled()) {
                SharedConfig.toggleChatBlur();
            }
            Theme.ThemeInfo info = Theme.applyThemeFile(outFile, themeName, null, false);
            if (info != null) {
                SharedPreferences.Editor editor = ApplicationLoader.applicationContext
                        .getSharedPreferences("themeconfig", Activity.MODE_PRIVATE).edit();
                editor.putString(nightTheme ? "lastDarkTheme" : "lastDayTheme", info.getKey());
                editor.apply();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static long lastBlockedToastTime;

    public static void showBlockedMiniAppToast() {
        long now = System.currentTimeMillis();
        if (now - lastBlockedToastTime < 1200) {
            return;
        }
        lastBlockedToastTime = now;
        AndroidUtilities.runOnUIThread(() -> {
            try {
                Toast.makeText(
                        ApplicationLoader.applicationContext,
                        "تطبيقات الويب والألعاب المصغرة معطلة في مرسال",
                        Toast.LENGTH_SHORT
                ).show();
            } catch (Exception ignore) {}
        });
    }
}
