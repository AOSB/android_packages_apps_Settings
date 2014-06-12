package com.android.settings.probam;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.WindowManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.android.settings.probam.preference.NumberPickerPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockscreenNotifications extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String KEY_PEEK = "notification_peek";
    private static final String KEY_PEEK_PICKUP_TIMEOUT = "peek_pickup_timeout";
    private static final String KEY_PEEK_WAKE_TIMEOUT = "peek_wake_timeout";
    private static final String KEY_LOCKSCREEN_NOTIFICATIONS = "lockscreen_notifications";
    private static final String KEY_POCKET_MODE = "pocket_mode";
    private static final String KEY_SHOW_ALWAYS = "show_always";
    private static final String KEY_HIDE_LOW_PRIORITY = "hide_low_priority";
    private static final String KEY_HIDE_NON_CLEARABLE = "hide_non_clearable";
    private static final String KEY_DISMISS_ALL = "dismiss_all";
    private static final String KEY_EXPANDED_VIEW = "expanded_view";
    private static final String KEY_FORCE_EXPANDED_VIEW = "force_expanded_view";
    private static final String KEY_WAKE_ON_NOTIFICATION = "wake_on_notification";
    private static final String KEY_NOTIFICATIONS_HEIGHT = "notifications_height";
    private static final String KEY_PRIVACY_MODE = "privacy_mode";
    private static final String KEY_OFFSET_TOP = "offset_top";
    private static final String KEY_CATEGORY_GENERAL = "category_general";
    private static final String KEY_EXCLUDED_APPS = "excluded_apps";
    private static final String KEY_NOTIFICATION_COLOR = "notification_color";
    private static final String KEY_GYROSCOPE = "gyroscope_sensor";
    private static final String KEY_PROXIMITY = "proximity_sensor";

    private static final String PEEK_APPLICATION = "com.jedga.peek";

    private CheckBoxPreference mNotificationPeek;
    private ListPreference mPeekPickupTimeout;
    private ListPreference mPeekWakeTimeout;
    private CheckBoxPreference mLockscreenNotifications;
    private CheckBoxPreference mPocketMode;
    private CheckBoxPreference mShowAlways;
    private CheckBoxPreference mWakeOnNotification;
    private CheckBoxPreference mHideLowPriority;
    private CheckBoxPreference mHideNonClearable;
    private CheckBoxPreference mDismissAll;
    private CheckBoxPreference mExpandedView;
    private CheckBoxPreference mForceExpandedView;
    private NumberPickerPreference mNotificationsHeight;
    private CheckBoxPreference mPrivacyMode;
    private SeekBarPreference mOffsetTop;
    private AppMultiSelectListPreference mExcludedAppsPref;
    private ColorPickerPreference mNotificationColor;
 
    private CheckBoxPreference mGyroscope;
    private CheckBoxPreference mProximity;

    private PackageStatusReceiver mPackageStatusReceiver;
    private IntentFilter mIntentFilter;

    private boolean isPeekAppInstalled() {
        return isPackageInstalled(PEEK_APPLICATION);
    }

    private boolean isPackageInstalled(String packagename) {
        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
           return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_notifications);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver cr = getActivity().getContentResolver();

        mNotificationPeek = (CheckBoxPreference) findPreference(KEY_PEEK);
        mNotificationPeek.setPersistent(false);

        mPeekPickupTimeout = (ListPreference) prefs.findPreference(KEY_PEEK_PICKUP_TIMEOUT);
        int peekPickupTimeout = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT, 10000, UserHandle.USER_CURRENT);
        mPeekPickupTimeout.setValue(String.valueOf(peekPickupTimeout));
        mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntry());
        mPeekPickupTimeout.setOnPreferenceChangeListener(this);

        mPeekWakeTimeout = (ListPreference) prefs.findPreference(KEY_PEEK_WAKE_TIMEOUT);
        int peekWakeTimeout = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.PEEK_WAKE_TIMEOUT, 5000, UserHandle.USER_CURRENT);
        mPeekWakeTimeout.setValue(String.valueOf(peekWakeTimeout));
        mPeekWakeTimeout.setSummary(mPeekWakeTimeout.getEntry());
        mPeekWakeTimeout.setOnPreferenceChangeListener(this);

        mLockscreenNotifications = (CheckBoxPreference) prefs.findPreference(KEY_LOCKSCREEN_NOTIFICATIONS);
        mLockscreenNotifications.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS, 0) == 1);

        mPocketMode = (CheckBoxPreference) prefs.findPreference(KEY_POCKET_MODE);
        mPocketMode.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_POCKET_MODE, 0) == 1);
        mPocketMode.setEnabled(mLockscreenNotifications.isChecked());

        mShowAlways = (CheckBoxPreference) prefs.findPreference(KEY_SHOW_ALWAYS);
        mShowAlways.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_SHOW_ALWAYS, 0) == 1);
        mShowAlways.setEnabled(mPocketMode.isChecked() && mPocketMode.isEnabled());

        mWakeOnNotification = (CheckBoxPreference) prefs.findPreference(KEY_WAKE_ON_NOTIFICATION);
        mWakeOnNotification.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_WAKE_ON_NOTIFICATION, 0) == 1);
        mWakeOnNotification.setEnabled(mLockscreenNotifications.isChecked());

        mHideLowPriority = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideLowPriority.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_LOW_PRIORITY, 0) == 1);
        mHideLowPriority.setEnabled(mLockscreenNotifications.isChecked());

        mHideNonClearable = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_NON_CLEARABLE);
        mHideNonClearable.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE, 0) == 1);
        mHideNonClearable.setEnabled(mLockscreenNotifications.isChecked());

        mDismissAll = (CheckBoxPreference) prefs.findPreference(KEY_DISMISS_ALL);
        mDismissAll.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL, 0) == 1);
        mDismissAll.setEnabled(!mHideNonClearable.isChecked() && mLockscreenNotifications.isChecked());

        mPrivacyMode = (CheckBoxPreference) prefs.findPreference(KEY_PRIVACY_MODE);
        mPrivacyMode.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_PRIVACY_MODE, 0) == 1);
        mPrivacyMode.setEnabled(mLockscreenNotifications.isChecked());

        mExpandedView = (CheckBoxPreference) prefs.findPreference(KEY_EXPANDED_VIEW);
        mExpandedView.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW, 0) == 1);
        mExpandedView.setEnabled(mLockscreenNotifications.isChecked() && !mPrivacyMode.isChecked());

        mForceExpandedView = (CheckBoxPreference) prefs.findPreference(KEY_FORCE_EXPANDED_VIEW);
        mForceExpandedView.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_FORCE_EXPANDED_VIEW, 0) == 1);
        mForceExpandedView.setEnabled(mLockscreenNotifications.isChecked() && mExpandedView.isChecked()
                    && !mPrivacyMode.isChecked());

        mOffsetTop = (SeekBarPreference) prefs.findPreference(KEY_OFFSET_TOP);
        mOffsetTop.setProgress((int)(Settings.System.getFloat(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP, 0.3f) * 100));
        mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + mOffsetTop.getProgress() + "%");
        mOffsetTop.setOnPreferenceChangeListener(this);
        mOffsetTop.setEnabled(mLockscreenNotifications.isChecked());

        //gyroscope && proximity sensor
        mGyroscope = (CheckBoxPreference) findPreference(KEY_GYROSCOPE);
        mGyroscope.setPersistent(false);
        mGyroscope.setEnabled(false);

        mProximity = (CheckBoxPreference) findPreference(KEY_PROXIMITY);
        mProximity.setPersistent(false);
        mProximity.setEnabled(false);

        CheckSensors();

        mNotificationsHeight = (NumberPickerPreference) prefs.findPreference(KEY_NOTIFICATIONS_HEIGHT);
        mNotificationsHeight.setValue(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, 4));
        Point displaySize = new Point();
        ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
        int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                (float)mContext.getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
        mNotificationsHeight.setMinValue(1);
        mNotificationsHeight.setMaxValue(max);
        mNotificationsHeight.setOnPreferenceChangeListener(this);
        mNotificationsHeight.setEnabled(mLockscreenNotifications.isChecked());

        mNotificationColor = (ColorPickerPreference) prefs.findPreference(KEY_NOTIFICATION_COLOR);
        mNotificationColor.setAlphaSliderEnabled(true);
        int color = Settings.System.getInt(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS_COLOR, 0x55555555);
        String hexColor = String.format("#%08x", (0xffffffff & color));
        mNotificationColor.setSummary(hexColor);
        mNotificationColor.setDefaultValue(color);
        mNotificationColor.setNewPreviewColor(color);
        mNotificationColor.setOnPreferenceChangeListener(this);

        mExcludedAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);


        boolean hasProximitySensor = getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);
        if (!hasProximitySensor) {
            PreferenceCategory general = (PreferenceCategory) prefs.findPreference(KEY_CATEGORY_GENERAL);
            general.removePreference(mPocketMode);
            general.removePreference(mShowAlways);
        }

        if (mPackageStatusReceiver == null) {
            mPackageStatusReceiver = new PackageStatusReceiver();
        }
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            mIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        }
        getActivity().registerReceiver(mPackageStatusReceiver, mIntentFilter);

        updateNotificationOptions();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mPackageStatusReceiver, mIntentFilter);
        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mPackageStatusReceiver);
    }

    private void updateState() {
        updatePeekCheckbox();
    }

    private void updatePeekCheckbox() {
        boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.PEEK_STATE, 0) == 1;
        mNotificationPeek.setChecked(enabled && !isPeekAppInstalled());
        mNotificationPeek.setEnabled(!isPeekAppInstalled());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mNotificationPeek) {
            Settings.System.putInt(cr, Settings.System.PEEK_STATE,
                    mNotificationPeek.isChecked() ? 1 : 0);
            updateNotificationOptions();
        } else if (preference == mLockscreenNotifications) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS,
                    mLockscreenNotifications.isChecked() ? 1 : 0);
            mPocketMode.setEnabled(mLockscreenNotifications.isChecked());
            mShowAlways.setEnabled(mPocketMode.isChecked() && mPocketMode.isEnabled());
            mWakeOnNotification.setEnabled(mLockscreenNotifications.isChecked());
            mHideLowPriority.setEnabled(mLockscreenNotifications.isChecked());
            mHideNonClearable.setEnabled(mLockscreenNotifications.isChecked());
            mDismissAll.setEnabled(!mHideNonClearable.isChecked() && mLockscreenNotifications.isChecked());
            mNotificationsHeight.setEnabled(mLockscreenNotifications.isChecked());
            mOffsetTop.setEnabled(mLockscreenNotifications.isChecked());
            mForceExpandedView.setEnabled(mLockscreenNotifications.isChecked() && mExpandedView.isChecked()
                        && !mPrivacyMode.isChecked());
            mExpandedView.setEnabled(mLockscreenNotifications.isChecked() && !mPrivacyMode.isChecked());
            updateNotificationOptions();
        } else if (preference == mPocketMode) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_POCKET_MODE,
                    mPocketMode.isChecked() ? 1 : 0);
            mShowAlways.setEnabled(mPocketMode.isChecked());
        } else if (preference == mShowAlways) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_SHOW_ALWAYS,
                    mShowAlways.isChecked() ? 1 : 0);
        } else if (preference == mWakeOnNotification) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_WAKE_ON_NOTIFICATION,
                    mWakeOnNotification.isChecked() ? 1 : 0);
        } else if (preference == mHideLowPriority) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_LOW_PRIORITY,
                    mHideLowPriority.isChecked() ? 1 : 0);
        } else if (preference == mHideNonClearable) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_HIDE_NON_CLEARABLE,
                    mHideNonClearable.isChecked() ? 1 : 0);
            mDismissAll.setEnabled(!mHideNonClearable.isChecked());
        } else if (preference == mDismissAll) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_DISMISS_ALL,
                    mDismissAll.isChecked() ? 1 : 0);
        } else if (preference == mExpandedView) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_EXPANDED_VIEW,
                    mExpandedView.isChecked() ? 1 : 0);
            mForceExpandedView.setEnabled(mExpandedView.isChecked());
        } else if (preference == mForceExpandedView) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_FORCE_EXPANDED_VIEW,
                    mForceExpandedView.isChecked() ? 1 : 0);
        } else if (preference == mPrivacyMode) {
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_NOTIFICATIONS_PRIVACY_MODE,
                    mPrivacyMode.isChecked() ? 1 : 0);
            mForceExpandedView.setEnabled(mLockscreenNotifications.isChecked() && mExpandedView.isChecked()
                        && !mPrivacyMode.isChecked());
            mExpandedView.setEnabled(mLockscreenNotifications.isChecked() && !mPrivacyMode.isChecked());
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {
        if (pref == mNotificationsHeight) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, (Integer)value);
        } else if (pref == mOffsetTop) {
            Settings.System.putFloat(getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP,
                    (Integer)value / 100f);
            mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + (Integer)value + "%");
            Point displaySize = new Point();
            ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
            int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                    (float)mContext.getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
            mNotificationsHeight.setMaxValue(max);
        } else if (pref == mNotificationColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_COLOR, intHex);
            return true;
        } else if (pref == mPeekPickupTimeout) {
            int index = mPeekPickupTimeout.findIndexOfValue((String) value);
            int peekPickupTimeout = Integer.valueOf((String) value);
            Settings.System.putIntForUser(getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT,
                    peekPickupTimeout, UserHandle.USER_CURRENT);
            mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntries()[index]);
            return true;
        } else if (pref == mPeekWakeTimeout) {
            int index = mPeekWakeTimeout.findIndexOfValue((String) value);
            int peekWakeTimeout = Integer.valueOf((String) value);
            Settings.System.putIntForUser(getContentResolver(),
                Settings.System.PEEK_WAKE_TIMEOUT,
                    peekWakeTimeout, UserHandle.USER_CURRENT);
            mPeekWakeTimeout.setSummary(mPeekWakeTimeout.getEntries()[index]);
            return true;
        } else if (pref == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) value);
            return true;
        } else {
            return false;
        }
        return true;
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded))
            return null;

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
        Settings.System.putString(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS, builder.toString());
    }

    // Auto disable LN if PEEK is enabled
    private void updateNotificationOptions() {
        boolean peekState = Settings.System.getBoolean(getActivity().getContentResolver(),
               Settings.System.PEEK_STATE, false);
        boolean lockNotif = Settings.System.getBoolean(getActivity().getContentResolver(),
               Settings.System.LOCKSCREEN_NOTIFICATIONS, false);

        if (peekState) {
            mLockscreenNotifications.setEnabled(false);
            mLockscreenNotifications.setChecked(false);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS, 0);
        } else {
            mLockscreenNotifications.setEnabled(true);
            if (lockNotif) {
                mNotificationPeek.setEnabled(false);
                // Ensure that PEEK is disable
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.PEEK_STATE, 0);
            } else {
                mNotificationPeek.setEnabled(true);
            }
        }
    }

    public class PackageStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                updatePeekCheckbox();
            } else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                updatePeekCheckbox();
            }
        }
    }

    private void CheckSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Check if there is gyroscope sensor.
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        getStatusColor(getResources().getText(R.string.gyroscope_sensor_summary).toString(), 1, gyroSensor);
        
        // Check if there is proximity sensor.
        Sensor proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null ?
                                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) :
                                sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
           
        getStatusColor(getResources().getText(R.string.proximity_sensor_summary).toString(), 2, proxSensor);
    }

    private void getStatusColor(String title, int type, Sensor success){

        Spannable summary = new SpannableString(title);
        if(type == 1){
            if (success != null) {
                mGyroscope.setChecked(true);
                summary.setSpan( new ForegroundColorSpan( Color.GREEN ), 0, summary.length(), 0 );
            } else {
                mGyroscope.setChecked(false);
                summary.setSpan( new ForegroundColorSpan( Color.RED ), 0, summary.length(), 0 );
            }
            mGyroscope.setSummary(summary);
        }

        if(type == 2){
            if (success != null) {
                mProximity.setChecked(true);
                summary.setSpan( new ForegroundColorSpan( Color.GREEN ), 0, summary.length(), 0 );
            } else {
                mProximity.setChecked(false);
                summary.setSpan( new ForegroundColorSpan( Color.RED ), 0, summary.length(), 0 );
            }
            mProximity.setSummary(summary);
        }
    }

    private void updatePeekTimeoutOptions(Object newValue) {
        int index = mPeekPickupTimeout.findIndexOfValue((String) newValue);
        int value = Integer.valueOf((String) newValue);
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT, value);
        mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntries()[index]);
    }
}
