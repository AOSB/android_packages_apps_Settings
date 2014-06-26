/*
 * Copyright (C) 2014 AOSB Project
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

package com.android.settings.probam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface; 
import android.content.Intent;
import android.content.Context;  
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import com.android.settings.util.Helpers;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;


public class Bubble extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "Bubble";

    private static final String BUBBLE_MODE = "bubble_mode";
    private static final String CUSTOM_RECENT_MODE = "custom_recent_mode";
    private static final String HTC_RECENT_STYLE = "htc_recent_style";

    private ListPreference mBubbleMode;
    private ListPreference mRecentsCustom;
    private CheckBoxPreference mHTCEffect;

    private Context mContext;  

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.custom_recents);

        PreferenceScreen prefSet = getPreferenceScreen();

        mBubbleMode = (ListPreference) prefSet.findPreference(BUBBLE_MODE);
        int bubble_mode = Settings.System.getInt(getContentResolver(),
                Settings.System.BUBBLE_RECENT, 0);
        mBubbleMode.setValue(String.valueOf(bubble_mode));
        mBubbleMode.setSummary(mBubbleMode.getEntry());
        mBubbleMode.setOnPreferenceChangeListener(this);

        mRecentsCustom = (ListPreference) findPreference(CUSTOM_RECENT_MODE);
        long recent_state = Settings.System.getLong(getContentResolver(),
                Settings.System.CUSTOM_RECENT, 0);
        mRecentsCustom.setValue(String.valueOf(recent_state));
        mRecentsCustom.setSummary(mRecentsCustom.getEntry());
        mRecentsCustom.setOnPreferenceChangeListener(this);

        mHTCEffect = (CheckBoxPreference) findPreference(HTC_RECENT_STYLE);

	    if(recent_state != 1){
		    mHTCEffect.setChecked(false);
		    mHTCEffect.setEnabled(false);
            mBubbleMode.setEnabled(false);
	    }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {

        if (preference == mBubbleMode) {
            int BubbleMode = Integer.valueOf((String) objValue);
            int index = mBubbleMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BUBBLE_RECENT, BubbleMode);
            mBubbleMode.setSummary(mBubbleMode.getEntries()[index]);
            return true;
        } else if (preference == mRecentsCustom) {
            int val = Integer.parseInt((String) objValue);
            int index = mRecentsCustom.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.CUSTOM_RECENT, val);
            mRecentsCustom.setSummary(mRecentsCustom.getEntries()[index]);

            if(index != 1){
		        mHTCEffect.setChecked(false);
		        mHTCEffect.setEnabled(false);
		        mBubbleMode.setEnabled(false);
                Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BUBBLE_RECENT, 0);
            }
            openSlimRecentsWarning();
            return true;
        } else if (preference == mHTCEffect) {
            boolean value = (Boolean) objValue;
	    Settings.System.putInt(getActivity().getContentResolver(),
		        Settings.System.HTC_RECENT_STYLE, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void openSlimRecentsWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.slim_recents_warning_title))
            .setMessage(getResources().getString(R.string.slim_recents_warning_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Helpers.restartSystemUI();
                }
            }).show();
    }

}
