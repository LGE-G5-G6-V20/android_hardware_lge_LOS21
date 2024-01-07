package org.lineageos.settings.device.dac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;
import androidx.preference.SeekBarPreference;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;

import org.lineageos.settings.device.dac.ui.BalancePreference;
import org.lineageos.settings.device.dac.ui.ButtonPreference;
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

    /*** Custom filter UI props ***/
    /* Shape and symmetry selectors */
    private ListPreference custom_filter_shape, custom_filter_symmetry;

    /* Filter stage 2 coefficients (refer to the kernel's es9218.c for more info) */
    private static SeekBarPreference[] custom_filter_coeffs = new SeekBarPreference[14];
    
    /* Button to reset custom filter's coefficients, if needed. */
    private ButtonPreference custom_filter_reset_coeffs_button;

    /* We'll use a notification audio playback to kickstart the dac setup processes */
    private MediaPlayer notificationPlayer;
    private Boolean playerPrepared = false;
    private Handler handler;
    private Runnable playNotifCueRunnable;
    /* Keeps track of changes that require media playback to be paused. */
    private Boolean changeInProgress = false;

    /* Used to get the state of wired headset connection, and media playback */
    private AudioManager audioManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.quaddac_panel);
        headsetPluggedFragmentReceiver = new HeadsetPluggedFragmentReceiver();
        /* Get the standard notification jingle from android to be used as the DAC trigger */
        notificationPlayer = new MediaPlayer();
        try {
            notificationPlayer.setDataSource(getContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        } catch(IOException e) {

        }
        
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if(preference instanceof SwitchPreference) {
            if(!changeInProgress) {
                boolean set_dac_on = (boolean) newValue;

                if (set_dac_on) {
                    QuadDAC.enable();
                    enableExtraSettings();
                    Toast.makeText(getActivity(), Html.fromHtml("<b>Enabling DAC... pause all media playback for a few seconds and do not leave this panel.</b>") ,Toast.LENGTH_LONG).show();
                    changeInProgress = true;
                    handler.postDelayed(playNotifCueRunnable, 5000);
                    return true;
                } else {
                    QuadDAC.disable();
                    disableExtraSettings();
                    Toast.makeText(getActivity(), Html.fromHtml("<b>Disabling DAC... pause all media playback for a few seconds and do not leave this panel.</b>") ,Toast.LENGTH_LONG).show();
                    changeInProgress = true;
                    handler.postDelayed(playNotifCueRunnable, 5000);
                    return true;
                }
            } else {
                return false;
            }

        }
        if(preference instanceof ListPreference)
        {
            if(preference.getKey().equals(Constants.HIFI_MODE_KEY))
            {
                if(!changeInProgress) {
                    ListPreference lp = (ListPreference) preference;

                    int mode = lp.findIndexOfValue((String) newValue);
                    QuadDAC.setDACMode(mode);

                    Toast.makeText(getActivity(), Html.fromHtml("<b>Setting up HIFI Mode... Pause all media playback for a few seconds and do not leave this panel.</b>") ,Toast.LENGTH_LONG).show();
                    changeInProgress = true;
                    handler.postDelayed(playNotifCueRunnable, 5000);
                    return true;
                } else {
                    return false;
                }
                

            } else if(preference.getKey().equals(Constants.DIGITAL_FILTER_KEY))
            {
                ListPreference lp = (ListPreference) preference;

                int digital_filter = lp.findIndexOfValue((String) newValue);

                QuadDAC.setDigitalFilter(digital_filter);

                /* Custom filter panel should only show up with Filter [3] (fourth one) selected */
                if(digital_filter == 3)
                    enableCustomFilter();
                else
                    disableCustomFilter();

                return true;

            } else if(preference.getKey().equals(Constants.CUSTOM_FILTER_SHAPE_KEY))
            {
                ListPreference lp = (ListPreference) preference;

                int filter_shape = lp.findIndexOfValue((String) newValue);
                QuadDAC.setCustomFilterShape(filter_shape);
                return true;

            } else if(preference.getKey().equals(Constants.CUSTOM_FILTER_SYMMETRY_KEY))
            {
                ListPreference lp = (ListPreference) preference;

                int filter_symmetry = lp.findIndexOfValue((String) newValue);
                QuadDAC.setCustomFilterSymmetry(filter_symmetry);
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
            else { /* This assumes the only other seekbars are for the custom filter. Extend as needed. */
                for(int i = 0; i < 14; i++){
                    if(preference.getKey().equals(Constants.CUSTOM_FILTER_COEFF_KEYS[i]))
                    {
                        if (newValue instanceof Integer) {
                            Integer coeffVal = (Integer) newValue;

                            setCoeffSummary(i, coeffVal);

                            QuadDAC.setCustomFilterCoeff(i, coeffVal);
                            QuadDAC.applyCustomFilterCoeffs();
                            return true;
                        } else
                            return false;
                    }
                }
            }
        }

        return false;
    }

    public static void setCoeffSummary(int index, int value) {
        custom_filter_coeffs[index].setValue(value);
        custom_filter_coeffs[index].setSummary("Coefficient " + index + " : 0." + value);
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        getActivity().registerReceiver(headsetPluggedFragmentReceiver, filter);
        super.onResume();

        /* 
         * Hacky workaround for the ESS DAC on AOSP audio hal:
         * 
         * Since we can't get the DAC status without issuing a media playback beforehand,
         * play a small notification stream in order to get what we need, or else there
         * will be no way to know when the DAC is actually ready to apply the panel's 
         * settings.
         */
        handler = new Handler();
        playNotifCueRunnable = new Runnable() {
            @Override
            public void run() {
                if(audioManager.isMusicActive()) {
                    /* Media playback is still active, notify the user and post another job */
                    Toast.makeText(getActivity(), Html.fromHtml("<font color='#BB0000'><b>Media playback detected! Did you pause it as requested? Trying again...</b>") ,Toast.LENGTH_LONG).show();
                    handler.postDelayed(playNotifCueRunnable, 5000);
                }
                else {
                    try {
                        /* Only prepare the player once */
                        if(!playerPrepared) {
                            notificationPlayer.prepare();
                            playerPrepared = true;
                        }
                        /* 
                         * Remove the runnable callback in order to potentially schedule it again if
                         * the dac setup fails.
                         */
                        handler.removeCallbacks(playNotifCueRunnable);
                        notificationPlayer.start();
                        /* Change that requires user to pause media playback were done, so clear its variable */
                        changeInProgress = false;
                    } catch(IOException e) {
    
                    }
                }
            }
        };

        /* After the media has completed playing, we can ascertain the DAC's status */
        notificationPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                /* 
                 * We must check for the following conditions after the notification tone is played:
                 * 
                 * 1 - Both the expected and actual status are true (dac has been enabled successfully)
                 * 2 - Expected status is true but actual status is false (dac couldn't be enabled)
                 * 3 - Both the expected and actual status are false (dac has been disabled successfully)
                 * 4 - Expected status is false but actual status is true (dac couldn't be disabled)
                 * 
                 * 2 and 4 are failure states, so we must try again and notify the user about it.
                 */
                if(SystemProperties.get(Constants.PROPERTY_ESS_ACTUAL_STATUS).equals("true") && SystemProperties.get(Constants.PROPERTY_ESS_STATUS).equals("true")) {
                    QuadDAC.enabledSetup();
                    Toast.makeText(getActivity(), Html.fromHtml("<font color='#00AA00'><b>DAC IS READY!!!</b>") ,Toast.LENGTH_LONG).show();
                }
                else if(SystemProperties.get(Constants.PROPERTY_ESS_ACTUAL_STATUS).equals("false") && SystemProperties.get(Constants.PROPERTY_ESS_STATUS).equals("true")){
                    Toast.makeText(getActivity(), Html.fromHtml("<font color='#BB0000'><b>DAC NOT READY!!! Trying again...</b>") ,Toast.LENGTH_LONG).show();
                    changeInProgress = true;
                    handler.postDelayed(playNotifCueRunnable, 5000);
                }
                else if(SystemProperties.get(Constants.PROPERTY_ESS_ACTUAL_STATUS).equals("false") && SystemProperties.get(Constants.PROPERTY_ESS_STATUS).equals("false")) {
                    Toast.makeText(getActivity(), Html.fromHtml("<font color='#00AA00'><b>DAC DISABLED SUCCESSFULLY!!!</b>") ,Toast.LENGTH_LONG).show();
                }
                else if(SystemProperties.get(Constants.PROPERTY_ESS_ACTUAL_STATUS).equals("true") && SystemProperties.get(Constants.PROPERTY_ESS_STATUS).equals("false")) {
                    Toast.makeText(getActivity(), Html.fromHtml("<font color='#BB0000'><b>DAC NOT DISABLED!!! Trying again...</b>") ,Toast.LENGTH_LONG).show();
                    changeInProgress = true;
                    handler.postDelayed(playNotifCueRunnable, 5000);
                }
            }
        });
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
        audioManager = getContext().getSystemService(AudioManager.class);

        quaddac_switch = (SwitchPreference) findPreference(Constants.DAC_SWITCH_KEY);
        quaddac_switch.setOnPreferenceChangeListener(this);

        digital_filter_list = (ListPreference) findPreference(Constants.DIGITAL_FILTER_KEY);
        digital_filter_list.setOnPreferenceChangeListener(this);

        custom_filter_shape = (ListPreference) findPreference(Constants.CUSTOM_FILTER_SHAPE_KEY);
        custom_filter_shape.setOnPreferenceChangeListener(this);

        custom_filter_symmetry = (ListPreference) findPreference(Constants.CUSTOM_FILTER_SYMMETRY_KEY);
        custom_filter_symmetry.setOnPreferenceChangeListener(this);

        for(int i = 0; i < 14; i++)
        {
            custom_filter_coeffs[i] = (SeekBarPreference) findPreference(Constants.CUSTOM_FILTER_COEFF_KEYS[i]);
            custom_filter_coeffs[i].setOnPreferenceChangeListener(this);
            setCoeffSummary(i, QuadDAC.getCustomFilterCoeff(i));
        }

        custom_filter_reset_coeffs_button = (ButtonPreference) findPreference(Constants.RESET_COEFFICIENTS_KEY);
        custom_filter_reset_coeffs_button.setOnPreferenceChangeListener(this);

        mode_list = (ListPreference) findPreference(Constants.HIFI_MODE_KEY);
        mode_list.setOnPreferenceChangeListener(this);

        avc_volume = (SeekBarPreference) findPreference(Constants.AVC_VOLUME_KEY);
        avc_volume.setOnPreferenceChangeListener(this);
        avc_volume.setValue(QuadDAC.getAVCVolume());

        balance_preference = (BalancePreference) findPreference(Constants.BALANCE_KEY);

        if(audioManager.isWiredHeadsetOn()) {
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
                quaddac_switch.setChecked(true);
        }
    }

    private void enableExtraSettings()
    {
        digital_filter_list.setEnabled(true);
        mode_list.setEnabled(true);
        avc_volume.setEnabled(true);
        balance_preference.setEnabled(true);
        enableCustomFilter();
    }

    private void disableExtraSettings()
    {
        digital_filter_list.setEnabled(false);
        mode_list.setEnabled(false);
        avc_volume.setEnabled(false);
        balance_preference.setEnabled(false);
        disableCustomFilter();
    }

    private void enableCustomFilter() 
    {
        checkCustomFilterVisibility();
        custom_filter_shape.setEnabled(true);
        custom_filter_symmetry.setEnabled(true);
        for(int i = 0; i < 14; i++)
            custom_filter_coeffs[i].setEnabled(true);

        custom_filter_reset_coeffs_button.setEnabled(true);

        /* To apply the custom filter's settings */
        QuadDAC.setCustomFilterShape(QuadDAC.getCustomFilterShape());
    }

    private void disableCustomFilter() {
        checkCustomFilterVisibility();
        custom_filter_shape.setEnabled(false);
        custom_filter_symmetry.setEnabled(false);
        for(int i = 0; i < 14; i++)
            custom_filter_coeffs[i].setEnabled(false);
        
        custom_filter_reset_coeffs_button.setEnabled(false);
    }

    private void checkCustomFilterVisibility() {
        /* 
         * If the selected digital filter is the custom filter,
         * its preferences should be visible. Otherwise, hide them
         * to remove unused preferences from the panel.
        */
        if(QuadDAC.getDigitalFilter() == 3) {
            custom_filter_shape.setVisible(true);
            custom_filter_symmetry.setVisible(true);
            for(int i = 0; i < 14; i++)
                custom_filter_coeffs[i].setVisible(true);

            custom_filter_reset_coeffs_button.setVisible(true);
        }
        else {
            custom_filter_shape.setVisible(false);
            custom_filter_symmetry.setVisible(false);
            for(int i = 0; i < 14; i++)
                custom_filter_coeffs[i].setVisible(false);

            custom_filter_reset_coeffs_button.setVisible(false);
        }
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
                            enableExtraSettings();
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
