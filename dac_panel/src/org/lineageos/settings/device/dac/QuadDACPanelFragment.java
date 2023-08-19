package org.lineageos.settings.device.dac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;
import androidx.preference.SeekBarPreference;
import android.util.Log;
import android.view.MenuItem;


import org.lineageos.settings.device.dac.ui.BalancePreference;
import org.lineageos.settings.device.dac.utils.Constants;
import org.lineageos.settings.device.dac.utils.QuadDAC;

public class QuadDACPanelFragment extends PreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "QuadDACPanelFragment";

    private SwitchPreference quaddac_switch;
    private ListPreference digital_filter_list, dop_list, mode_list;
    private BalancePreference balance_preference;
    private SeekBarPreference avc_volume;

    private HeadsetPluggedFragmentReceiver headsetPluggedFragmentReceiver;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.quaddac_panel);
        headsetPluggedFragmentReceiver = new HeadsetPluggedFragmentReceiver();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if(preference instanceof SwitchPreference) {

            boolean set_dac_on = (boolean) newValue;

            if (set_dac_on) {
                enableExtraSettings();
                QuadDAC.enable();
                return true;
            } else {
                disableExtraSettings();
                QuadDAC.disable();
                return true;
            }
        }
        if(preference instanceof ListPreference)
        {
            if(preference.getKey().equals(Constants.HIFI_MODE_KEY))
            {
                ListPreference lp = (ListPreference) preference;

                int mode = lp.findIndexOfValue((String) newValue);
                QuadDAC.setDACMode(mode);
                return true;

            } else if(preference.getKey().equals(Constants.DIGITAL_FILTER_KEY))
            {
                ListPreference lp = (ListPreference) preference;

                int digital_filter = lp.findIndexOfValue((String) newValue);
                QuadDAC.setDigitalFilter(digital_filter);
                return true;

            }
            return false;
        }

        if(preference instanceof SeekBarPreference)
        {
            if(preference.getKey().equals(Constants.AVC_VOLUME_KEY))
            {
                if (newValue instanceof Integer) {                
                    Integer avc_vol = (Integer) newValue;

                    //avc_volume.setSummary( ((double)avc_vol) + " db");

                    QuadDAC.setAVCVolume(avc_vol);
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        getActivity().registerReceiver(headsetPluggedFragmentReceiver, filter);
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(headsetPluggedFragmentReceiver);
        super.onPause();
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        // Initialize preferences
        AudioManager am = getContext().getSystemService(AudioManager.class);

        quaddac_switch = (SwitchPreference) findPreference(Constants.DAC_SWITCH_KEY);
        quaddac_switch.setOnPreferenceChangeListener(this);

        digital_filter_list = (ListPreference) findPreference(Constants.DIGITAL_FILTER_KEY);
        digital_filter_list.setOnPreferenceChangeListener(this);

        mode_list = (ListPreference) findPreference(Constants.HIFI_MODE_KEY);
        mode_list.setOnPreferenceChangeListener(this);

        avc_volume = (SeekBarPreference) findPreference(Constants.AVC_VOLUME_KEY);
        avc_volume.setOnPreferenceChangeListener(this);
        avc_volume.setValue(QuadDAC.getAVCVolume());

        balance_preference = (BalancePreference) findPreference(Constants.BALANCE_KEY);

        if(am.isWiredHeadsetOn()) {
            quaddac_switch.setEnabled(true);
            if(QuadDAC.isEnabled())
            {
                quaddac_switch.setChecked(true);
                enableExtraSettings();
            } else {
                quaddac_switch.setChecked(false);
                disableExtraSettings();
            }
        } else {
            quaddac_switch.setEnabled(false);
            disableExtraSettings();
            if(QuadDAC.isEnabled())
            {
                quaddac_switch.setChecked(true);
            }
        }
    }

    private void enableExtraSettings()
    {
        digital_filter_list.setEnabled(true);
        mode_list.setEnabled(true);
        avc_volume.setEnabled(true);
        balance_preference.setEnabled(true);
    }

    private void disableExtraSettings()
    {
        digital_filter_list.setEnabled(false);
        mode_list.setEnabled(false);
        avc_volume.setEnabled(false);
        balance_preference.setEnabled(false);
    }

    private class HeadsetPluggedFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch(state)
                {
                    case 1: // Headset plugged in
                        quaddac_switch.setEnabled(true);
                        if(quaddac_switch.isChecked())
                        {
                            enableExtraSettings();
                        }
                        break;
                    case 0: // Headset unplugged
                        quaddac_switch.setEnabled(false);
                        disableExtraSettings();
                        break;
                    default: break;
                }
            }
        }
    }

}
