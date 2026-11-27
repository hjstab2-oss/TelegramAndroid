package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MersalConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class MersalSettingsActivity extends BaseFragment {

    private static final int ID_ANTI_DELETE = 1;
    private static final int ID_DOWNLOAD_BOOST = 2;
    private static final int ID_FONT_MODE = 3;
    private static final int ID_THEME_MODE = 4;
    private static final int ID_STATUS_INFO = 5;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.MersalSettingsTitle));
        actionBar.setAllowOverlayTitle(true);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setGlowColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, org.telegram.ui.Components.UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.MersalSectionPrivacySpeed)));
        items.add(UItem.asButtonCheck(
                ID_ANTI_DELETE,
                LocaleController.getString(R.string.MersalAntiDeleteTitle),
                LocaleController.getString(R.string.MersalAntiDeleteInfo)
        ).setChecked(MersalConfig.isAntiDeleteEnabled()));

        items.add(UItem.asButtonCheck(
                ID_DOWNLOAD_BOOST,
                LocaleController.getString(R.string.MersalDownloadBoostTitle),
                LocaleController.getString(R.string.MersalDownloadBoostInfo)
        ).setChecked(MersalConfig.isDownloadBoostEnabled()));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(LocaleController.getString(R.string.MersalSectionAppearance)));

        items.add(UItem.asButton(
                ID_FONT_MODE,
                LocaleController.getString(R.string.MersalFontModeTitle),
                getFontModeLabel()
        ));

        items.add(UItem.asButton(
                ID_THEME_MODE,
                LocaleController.getString(R.string.MersalThemeModeTitle),
                getThemeModeLabel()
        ));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(LocaleController.getString(R.string.MersalSectionProtectionStatus)));
        items.add(UItem.asButton(
                ID_STATUS_INFO,
                LocaleController.getString(R.string.MersalProtectionStatusTitle),
                LocaleController.getString(R.string.MersalProtectionStatusValue)
        ).setEnabled(false));
        items.add(UItem.asShadow(LocaleController.getString(R.string.MersalProtectionStatusInfo)));
    }

    private String getFontModeLabel() {
        return MersalConfig.FONT_SYSTEM.equals(MersalConfig.getFontMode())
                ? LocaleController.getString(R.string.MersalFontSystem)
                : LocaleController.getString(R.string.MersalFontIbmPlex);
    }

    private String getThemeModeLabel() {
        String mode = MersalConfig.getThemeMode();
        if (MersalConfig.THEME_ONYX_DARK.equals(mode)) {
            return LocaleController.getString(R.string.MersalThemeOnyxDark);
        } else if (MersalConfig.THEME_CRYSTAL_LIGHT.equals(mode)) {
            return LocaleController.getString(R.string.MersalThemeCrystalLight);
        }
        return LocaleController.getString(R.string.MersalThemeDefault);
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_ANTI_DELETE) {
            boolean next = !item.checked;
            item.checked = next;
            MersalConfig.setAntiDeleteEnabled(next);
            setCellChecked(view, next);
        } else if (item.id == ID_DOWNLOAD_BOOST) {
            boolean next = !item.checked;
            item.checked = next;
            MersalConfig.setDownloadBoostEnabled(next);
            setCellChecked(view, next);
        } else if (item.id == ID_FONT_MODE) {
            String[] options = new String[]{
                    LocaleController.getString(R.string.MersalFontIbmPlex),
                    LocaleController.getString(R.string.MersalFontSystem)
            };
            int selected = MersalConfig.FONT_SYSTEM.equals(MersalConfig.getFontMode()) ? 1 : 0;
            showRadioDialog(LocaleController.getString(R.string.MersalFontModeTitle), options, selected, idx -> {
                String nextMode = idx == 1 ? MersalConfig.FONT_SYSTEM : MersalConfig.FONT_IBM_PLEX_ARABIC;
                if (!nextMode.equals(MersalConfig.getFontMode())) {
                    MersalConfig.setFontMode(nextMode);
                    listView.adapter.update(true);
                    Activity act = getParentActivity();
                    if (act != null) {
                        AndroidUtilities.runOnUIThread(() -> {
                            try {
                                act.recreate();
                            } catch (Exception ignore) {}
                        }, 100);
                    }
                }
            });
        } else if (item.id == ID_THEME_MODE) {
            String[] options = new String[]{
                    LocaleController.getString(R.string.MersalThemeOnyxDark),
                    LocaleController.getString(R.string.MersalThemeCrystalLight),
                    LocaleController.getString(R.string.MersalThemeDefault)
            };
            String cur = MersalConfig.getThemeMode();
            int selected = MersalConfig.THEME_ONYX_DARK.equals(cur) ? 0
                    : (MersalConfig.THEME_CRYSTAL_LIGHT.equals(cur) ? 1 : 2);
            showRadioDialog(LocaleController.getString(R.string.MersalThemeModeTitle), options, selected, idx -> {
                String nextTheme = idx == 0 ? MersalConfig.THEME_ONYX_DARK
                        : (idx == 1 ? MersalConfig.THEME_CRYSTAL_LIGHT : MersalConfig.THEME_DEFAULT);
                MersalConfig.applyThemeMode(nextTheme, getParentActivity());
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            });
        }
    }

    private static void setCellChecked(View view, boolean value) {
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        } else if (view instanceof NotificationsCheckCell) {
            ((NotificationsCheckCell) view).setChecked(value);
        }
    }

    private void showRadioDialog(CharSequence title, String[] options, int selectedIndex, Utilities.Callback<Integer> onSelected) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);

        for (int i = 0; i < options.length; i++) {
            RadioColorCell cell = new RadioColorCell(activity);
            cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            cell.setTag(i);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(options[i], selectedIndex == i);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            linearLayout.addView(cell);

            cell.setOnClickListener(v -> {
                onSelected.run((Integer) v.getTag());
                builder.getDismissRunnable().run();
            });
        }

        builder.setView(linearLayout);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
