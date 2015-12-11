package com.android.settings.nitrogen;

import com.android.internal.logging.MetricsLogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;

import java.util.Arrays;
import java.util.HashSet;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class MiscSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    private static final int DLG_CAMERA_SOUND = 1;
    private static final String KEY_CAMERA_SOUNDS = "camera_sounds";
    private static final String PROP_CAMERA_SOUND = "persist.sys.camera-sound";
    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    private static final String SCROLLINGCACHE_PREF = "pref_scrollingcache";
    private static final String SCROLLINGCACHE_PERSIST_PROP = "persist.sys.scrollingcache";
    private static final String SCROLLINGCACHE_DEFAULT = "2";
    private static final String PREF_MEDIA_SCANNER_ON_BOOT = "media_scanner_on_boot";

    private SwitchPreference mKillAppLongPressBack;
    private SwitchPreference mCameraSounds;
    private ListPreference mRecentsClearAllLocation;
    private SwitchPreference mRecentsClearAll;
    private ListPreference mScrollingCachePref;
    private ListPreference mMsob;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.nitrogen_settings_misc);

        ContentResolver resolver = getActivity().getContentResolver();

        mCameraSounds = (SwitchPreference) findPreference(KEY_CAMERA_SOUNDS);
        mCameraSounds.setChecked(SystemProperties.getBoolean(PROP_CAMERA_SOUND, true));
        mCameraSounds.setOnPreferenceChangeListener(this);

        // kill-app long press back
        mKillAppLongPressBack = (SwitchPreference) findPreference(KILL_APP_LONGPRESS_BACK);
        mKillAppLongPressBack.setOnPreferenceChangeListener(this);
        int killAppLongPressBack = Settings.Secure.getInt(getContentResolver(),
                KILL_APP_LONGPRESS_BACK, 0);
        mKillAppLongPressBack.setChecked(killAppLongPressBack != 0);

        // clear all recents
        mRecentsClearAll = (SwitchPreference) findPreference(SHOW_CLEAR_ALL_RECENTS);

        mRecentsClearAllLocation = (ListPreference) findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntry());
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);

        mScrollingCachePref = (ListPreference) findPreference(SCROLLINGCACHE_PREF);
        mScrollingCachePref.setValue(SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP,
                SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP, SCROLLINGCACHE_DEFAULT)));
        mScrollingCachePref.setOnPreferenceChangeListener(this);

        mMsob = (ListPreference) findPreference(PREF_MEDIA_SCANNER_ON_BOOT);
        mMsob.setValue(String.valueOf(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MEDIA_SCANNER_ON_BOOT, 0)));
        mMsob.setSummary(mMsob.getEntry());
        mMsob.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mKillAppLongPressBack) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), KILL_APP_LONGPRESS_BACK,
                    value ? 1 : 0);
            return true;
        } else if (preference == mCameraSounds) {
           if ((Boolean) objValue) {
               SystemProperties.set(PROP_CAMERA_SOUND, "1");
               return true;
           } else {
              showDialogInner(DLG_CAMERA_SOUND);
              return true;
        	 }
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) objValue);
            int index = mRecentsClearAllLocation.findIndexOfValue((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntries()[index]);
            return true;
        } else if (preference == mScrollingCachePref) {
            if (objValue != null) {
                SystemProperties.set(SCROLLINGCACHE_PERSIST_PROP, (String) objValue);
            }
            return true;
        } else if (preference == mMsob) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.MEDIA_SCANNER_ON_BOOT,
                    Integer.valueOf(String.valueOf(objValue)));

            mMsob.setValue(String.valueOf(objValue));
            mMsob.setSummary(mMsob.getEntry());
            return true;
	}
	return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        MiscSettings getOwner() {
            return (MiscSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_CAMERA_SOUND:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.camera_sound_warning_dialog_text)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SystemProperties.set(PROP_CAMERA_SOUND, "0");
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_CAMERA_SOUND:
                    getOwner().mCameraSounds.setChecked(true);
                    break;
            }
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MISC_SETTINGS;
    }
}