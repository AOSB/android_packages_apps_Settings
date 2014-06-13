/*
 * Copyright (C) 2013 The ChameleonOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.crdroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import com.android.internal.util.omni.DeviceUtils;

public class ActiveDisplaySettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "ActiveDisplaySettings";

    private static final String KEY_ENABLED = "ad_enable";
    private static final String KEY_POCKET_MODE = "ad_pocket_mode";
    private static final String KEY_REDISPLAY = "ad_redisplay";
    private static final String KEY_EXCLUDED_APPS = "ad_excluded_apps";
    private static final String KEY_TIMEOUT = "ad_timeout";
    private static final String KEY_THRESHOLD = "ad_threshold";
    private static final String KEY_HIDE_LOW_PRIORITY = "hide_low_priority";
    private static final String KEY_HIDE_NON_CLEARABLE = "hide_non_clearable";
    private static final String KEY_QUIET_HOURS = "quiet_hours";
    private static final String KEY_SHOW_TEXT = "ad_text";
    private static final String KEY_SHOW_DATE = "ad_show_date";

    private ContentResolver mResolver;
    private Context mContext;

    private CheckBoxPreference mHideLowPriority;
    private CheckBoxPreference mHideNonClearable;
    private CheckBoxPreference mQuietHours;
    private CheckBoxPreference mShowDatePref;
    private CheckBoxPreference mShowTextPref;
    private SwitchPreference mEnabledPref;
    private ListPreference mDisplayTimeout;
    private ListPreference mPocketModePref;
    private ListPreference mProximityThreshold;
    private ListPreference mRedisplayPref;
    private AppMultiSelectListPreference mExcludedAppsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_display_settings);

        mContext = getActivity().getApplicationContext();
        mResolver = mContext.getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

        mEnabledPref = (SwitchPreference) prefSet.findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ENABLE_ACTIVE_DISPLAY, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        mShowTextPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_TEXT);
        mShowTextPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_TEXT, 0) == 1));

        mShowDatePref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_DATE);
        mShowDatePref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHOW_DATE, 0) == 1));

        mPocketModePref = (ListPreference) prefSet.findPreference(KEY_POCKET_MODE);
        mProximityThreshold = (ListPreference) prefSet.findPreference(KEY_THRESHOLD);

        if (!DeviceUtils.deviceSupportsProximitySensor(mContext)) {
            prefSet.removePreference(mPocketModePref);
            prefSet.removePreference(mProximityThreshold);
        } else {

            int mode = Settings.System.getInt(mResolver,
                   Settings.System.ACTIVE_DISPLAY_POCKET_MODE, 0);
            mPocketModePref.setValue(String.valueOf(mode));
            updatePocketModeSummary(mode);
            mPocketModePref.setOnPreferenceChangeListener(this);

            long threshold = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_THRESHOLD, 5000L);
            mProximityThreshold.setValue(String.valueOf(threshold));
            updateThresholdSummary(threshold);
            mProximityThreshold.setOnPreferenceChangeListener(this);
        }

        mHideLowPriority = (CheckBoxPreference) prefSet.findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideLowPriority.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_HIDE_LOW_PRIORITY_NOTIFICATIONS, 0) == 1);

        mHideNonClearable = (CheckBoxPreference) prefSet.findPreference(KEY_HIDE_NON_CLEARABLE);
        mHideNonClearable.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_HIDE_NON_CLEARABLE, 0) == 1);

        mQuietHours = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS);
        mQuietHours.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_QUIET_HOURS, 0) == 1);

        mRedisplayPref = (ListPreference) prefSet.findPreference(KEY_REDISPLAY);
        long timeout = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, 0);
        mRedisplayPref.setValue(String.valueOf(timeout));
        mRedisplayPref.setSummary(mRedisplayPref.getEntry());
        mRedisplayPref.setOnPreferenceChangeListener(this);

        mDisplayTimeout = (ListPreference) prefSet.findPreference(KEY_TIMEOUT);
        timeout = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, 8000L);
        mDisplayTimeout.setValue(String.valueOf(timeout));
        mDisplayTimeout.setSummary(mDisplayTimeout.getEntry());
        mDisplayTimeout.setOnPreferenceChangeListener(this);

        mExcludedAppsPref = (AppMultiSelectListPreference) prefSet.findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRedisplayPref) {
            int val = Integer.parseInt((String) newValue);
            int index = mRedisplayPref.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, val);
            mRedisplayPref.setSummary(mRedisplayPref.getEntries()[index]);
            return true;
        } else if (preference == mPocketModePref) {
            int mode = Integer.valueOf((String) newValue);
            updatePocketModeSummary(mode);
            return true;
        } else if (preference == mEnabledPref) {
            Settings.System.putInt(mResolver,
                    Settings.System.ENABLE_ACTIVE_DISPLAY,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) newValue);
            return true;
        } else if (preference == mDisplayTimeout) {
            int val = Integer.parseInt((String) newValue);
            int index = mDisplayTimeout.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, val);
            mDisplayTimeout.setSummary(mDisplayTimeout.getEntries()[index]);
            return true;
        } else if (preference == mProximityThreshold) {
            long threshold = Integer.valueOf((String) newValue);
            updateThresholdSummary(threshold);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mHideLowPriority) {
            Settings.System.putInt(mResolver, Settings.System.ACTIVE_DISPLAY_HIDE_LOW_PRIORITY_NOTIFICATIONS,
                    mHideLowPriority.isChecked() ? 1 : 0);
        } else if (preference == mHideNonClearable) {
            Settings.System.putInt(mResolver, Settings.System.ACTIVE_DISPLAY_HIDE_NON_CLEARABLE,
                    mHideNonClearable.isChecked() ? 1 : 0);
        } else if (preference == mQuietHours) {
            Settings.System.putInt(mResolver, Settings.System.ACTIVE_DISPLAY_QUIET_HOURS,
                    mQuietHours.isChecked() ? 1 : 0);
        } else if (preference == mShowDatePref) {
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHOW_DATE,
                    mShowDatePref.isChecked() ? 1 : 0);
        } else if (preference == mShowTextPref) {
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_TEXT,
                    mShowTextPref.isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(mResolver,
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded)) {
            return null;
        }

        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(mResolver,
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS, builder.toString());
    }

    private Set<String> getPrivacyApps() {
        String privacies = Settings.System.getString(mResolver,
                Settings.System.ACTIVE_DISPLAY_PRIVACY_APPS);
        if (TextUtils.isEmpty(privacies)) {
            return null;
        }

        return new HashSet<String>(Arrays.asList(privacies.split("\\|")));
    }

    private void storePrivacyApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(mResolver,
                Settings.System.ACTIVE_DISPLAY_PRIVACY_APPS, builder.toString());
    }

    private void updatePocketModeSummary(int value) {
        mPocketModePref.setSummary(
                mPocketModePref.getEntries()[mPocketModePref.findIndexOfValue("" + value)]);
        Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_POCKET_MODE, value);
    }

    private void updateThresholdSummary(long value) {
        try {
            mProximityThreshold.setSummary(mProximityThreshold.getEntries()[mProximityThreshold.findIndexOfValue("" + value)]);
            Settings.System.putLong(mResolver,
                    Settings.System.ACTIVE_DISPLAY_THRESHOLD, value);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }
}
