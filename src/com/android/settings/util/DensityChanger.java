/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.CMDProcessor.CommandResult;
import com.android.settings.util.Helpers;

public class DensityChanger extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "DensityChanger";

    ListPreference mStockDensity;
    Preference mReboot;
    Preference mClearMarketData;
    Preference mRebootClearData;
    Preference mOpenMarket;
    ListPreference mCustomDensity;

    private static final int MSG_DATA_CLEARED = 500;

    private static final int DIALOG_DENSITY = 101;
    private static final int DIALOG_WARN_DENSITY = 102;

    protected Context mContext;

    int newDensityValue;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DATA_CLEARED:
                    mClearMarketData.setSummary(R.string.clear_market_data_cleared);
                    break;
            }

        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lcd_density_setup);

        mContext = getActivity().getApplicationContext();

        String currentDensity = SystemProperties.get("ro.sf.lcd_density");
        PreferenceScreen prefs = getPreferenceScreen();

        mStockDensity = (ListPreference) findPreference("stock_density");
        mStockDensity.setOnPreferenceChangeListener(this);

        mReboot = findPreference("reboot");
        mClearMarketData = findPreference("clear_market_data");
        mRebootClearData = findPreference("reboot_cleardata");
        mOpenMarket = findPreference("open_market");

        mCustomDensity = (ListPreference) findPreference("lcd_density");
        mCustomDensity.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mReboot) {
            PowerManager pm = (PowerManager) getActivity()
                    .getSystemService(Context.POWER_SERVICE);
            pm.reboot("Resetting density");
            return true;

        } else if (preference == mClearMarketData) {

            new ClearMarketDataTask().execute("");
            return true;

        } else if (preference == mRebootClearData) {
            PowerManager pm = (PowerManager) getActivity()
                    .getSystemService(Context.POWER_SERVICE);
            pm.reboot("Clear market data");
            return true;

        } else if (preference == mOpenMarket) {
            Intent openMarket = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_MARKET)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName activityName = openMarket.resolveActivity(getActivity()
                    .getPackageManager());
            if (activityName != null) {
                mContext.startActivity(openMarket);
            } else {
                preference
                        .setSummary(getResources().getString(R.string.open_market_summary_could_not_open));
            }
            return true;

        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(mContext);

        switch (dialogId) {
            case DIALOG_DENSITY:
                final View textEntryView = factory.inflate(
                        R.layout.alert_dialog_text_entry, null);
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.set_custom_density_title))
                        .setView(textEntryView)
                        .setPositiveButton(getResources().getString(R.string.set_custom_density_set), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText dpi = (EditText) textEntryView.findViewById(R.id.dpi_edit);
                                Editable text = dpi.getText();
                                Log.i(TAG, text.toString());

                                try {
                                    newDensityValue = Integer.parseInt(text.toString());
                                    showDialog(DIALOG_WARN_DENSITY);
                                } catch (Exception e) {
                                    mCustomDensity.setSummary(getResources().getString(R.string.custom_density_summary_invalid));
                                }

                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();
                            }
                        }).create();
            case DIALOG_WARN_DENSITY:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.custom_density_dialog_title))
                        .setMessage(
                                getResources().getString(R.string.custom_density_dialog_summary))
                        .setCancelable(false)
                        .setNeutralButton(getResources().getString(R.string.custom_density_dialog_button_got), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (setLcdDensity(newDensityValue)) {
                                    mCustomDensity.setSummary(newDensityValue + "");
                                }
                            }
                        })
                        .setPositiveButton(getResources().getString(R.string.custom_density_dialog_button_reboot), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (setLcdDensity(newDensityValue)) {
                                    PowerManager pm = (PowerManager) getActivity()
                                            .getSystemService(Context.POWER_SERVICE);
                                    pm.reboot("Resetting density");
                                }
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
        }
        return null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomDensity) {
            String strValue = (String) newValue;
            if (strValue.equals(getResources().getString(R.string.custom_density))) {
                showDialog(DIALOG_DENSITY);
                return true;
            } else {
                newDensityValue = Integer.parseInt((String) newValue);
                showDialog(DIALOG_WARN_DENSITY);
                return true;
            }
        } else if (preference == mStockDensity) {
            newDensityValue = Integer.parseInt((String) newValue);
            if (setLcdDensity(newDensityValue)) {
                mStockDensity.setSummary(getResources().getString(
                    R.string.stock_density_changed_summary) + newDensityValue);
            }
            return true;
        }

        return false;
    }

    private boolean setLcdDensity(int newDensity) {
        Helpers.getMount("rw");
        if (!(new CMDProcessor().su.runWaitFor("busybox sed -i 's|ro.sf.lcd_density=.*|"
                + "ro.sf.lcd_density" + "=" + newDensity + "|' " + "/system/build.prop").success())) {
            showRootDeniedInfoDialog();
            return false;
        }
        Helpers.getMount("ro");
        return true;
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            mHandler.sendEmptyMessage(MSG_DATA_CLEARED);
        }
    }

    private void showRootDeniedInfoDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.su_dialog_error_title);
        alertDialog.setMessage(R.string.su_dialog_error_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alertDialog.create().show();
    }

    private class ClearMarketDataTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... stuff) {
            String vending = "/data/data/com.android.vending/";
            String gms = "/data/data/com.google.android.gms/";
            String gsf = "/data/data/com.google.android.gsf/";

            CommandResult cr = new CMDProcessor().su.runWaitFor("ls " + vending);
            CommandResult cr_gms = new CMDProcessor().su.runWaitFor("ls " + gms);
            CommandResult cr_gsf = new CMDProcessor().su.runWaitFor("ls " + gsf);

            if (cr.stdout == null || cr_gms.stdout == null || cr_gsf.stdout == null)
                return false;

            for (String dir : cr.stdout.split("\n")) {
                if (!dir.equals("lib")) {
                    String c = "rm -r " + vending + dir;
                    // Log.i(TAG, c);
                    if (!new CMDProcessor().su.runWaitFor(c).success())
                        return false;
                }
            }

            for (String dir_gms : cr_gms.stdout.split("\n")) {
                if (!dir_gms.equals("lib")) {
                    String c_gms = "rm -r " + gms + dir_gms;
                    // Log.i(TAG, c);
                    if (!new CMDProcessor().su.runWaitFor(c_gms).success())
                        return false;
                }
            }

            for (String dir_gsf : cr_gsf.stdout.split("\n")) {
                if (!dir_gsf.equals("lib")) {
                    String c_gsf = "rm -r " + gsf + dir_gsf;
                    // Log.i(TAG, c);
                    if (!new CMDProcessor().su.runWaitFor(c_gsf).success())
                        return false;
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (!result) {
                showRootDeniedInfoDialog();
            } else {
                mClearMarketData.setSummary(getResources().getString(R.string.clear_market_data_cleared));
            }
        }
    }
}
