package org.lineageos.settings.device.dac.utils;

import android.media.AudioSystem;
import android.os.SystemProperties;
import android.util.Log;

import org.lineageos.hardware.util.FileUtils;

public class QuadDAC {

    private static final String TAG = "QuadDAC";

    private QuadDAC() {}

    public static void enable()
    {
        SystemProperties.set(Constants.PROPERTY_ESS_STATUS, "true");
    }

    public static void enabledSetup()
    {
        int mode = getDACMode();
        int left_balance = getLeftBalance();
        int right_balance = getRightBalance();
        int digital_filter = getDigitalFilter();
        int avc_vol = getAVCVolume();

        SystemProperties.set(Constants.PROPERTY_ESS_MODE, Integer.toString(mode));
        setDACMode(mode);
        setLeftBalance(left_balance);
        setRightBalance(right_balance);
        setDigitalFilter(digital_filter);
        setAVCVolume(avc_vol);
    }

    public static void disable()
    {
        SystemProperties.set(Constants.PROPERTY_ESS_STATUS, "false");
    }

    public static void setHifiDACdop(int dop)
    {
        AudioSystem.setParameters(Constants.SET_HIFI_DAC_DOP_COMMAND + dop);
        SystemProperties.set(Constants.PROPERTY_HIFI_DAC_DOP, Integer.toString(dop));
    }

    public static int getHifiDACdop()
    {
        return SystemProperties.getInt(Constants.PROPERTY_HIFI_DAC_DOP, 0);
    }

    public static void setDACMode(int mode)
    {
        switch(mode)
        {
        case 0:
            SystemProperties.set(Constants.PROPERTY_ESS_MODE, "0");
            break;
        case 1:
            SystemProperties.set(Constants.PROPERTY_ESS_MODE, "1");
            break;
        case 2:
            SystemProperties.set(Constants.PROPERTY_ESS_MODE, "2");
            break;
        default: 
            return;
        }
        SystemProperties.set(Constants.PROPERTY_HIFI_DAC_MODE, Integer.toString(mode));
    }

    public static int getDACMode()
    {
        return SystemProperties.getInt(Constants.PROPERTY_HIFI_DAC_MODE, 0);
    }

    public static void setAVCVolume(int avc_volume)
    {
        FileUtils.writeLine(Constants.AVC_VOLUME_SYSFS, (avc_volume * -1) + "");
        SystemProperties.set(Constants.PROPERTY_HIFI_DAC_AVC_VOLUME, Integer.toString(avc_volume));
    }

    public static int getAVCVolume()
    {
        return SystemProperties.getInt(Constants.PROPERTY_HIFI_DAC_AVC_VOLUME, 0);
    }

    public static void setDigitalFilter(int filter)
    {
        AudioSystem.setParameters(Constants.SET_DIGITAL_FILTER_COMMAND + filter);
        FileUtils.writeLine(Constants.ESS_FILTER_SYSFS, filter + "");
        SystemProperties.set(Constants.PROPERTY_DIGITAL_FILTER, Integer.toString(filter));
    }

    public static int getDigitalFilter()
    {
        return SystemProperties.getInt(Constants.PROPERTY_DIGITAL_FILTER, 0);
    }

    public static void setLeftBalance(int balance)
    {
        /* 
         * Looks like we can keep using integers to set balance volume, its value
         * is halved internally by the es9218/es9218p kernel driver (-1 here equals -0.5dB).
         */
        int setval;
        AudioSystem.setParameters(Constants.SET_LEFT_BALANCE_COMMAND + balance);
        SystemProperties.set(Constants.PROPERTY_LEFT_BALANCE, Integer.toString(balance));
        setval = getLeftBalance();
        FileUtils.writeLine(Constants.ESS_BALANCE_LEFT_SYSFS, setval + "");
    }

    public static int getLeftBalance()
    {
        return SystemProperties.getInt(Constants.PROPERTY_LEFT_BALANCE, 0);
    }

    public static void setRightBalance(int balance)
    {
        int setval;
        AudioSystem.setParameters(Constants.SET_RIGHT_BALANCE_COMMAND + balance);
        SystemProperties.set(Constants.PROPERTY_RIGHT_BALANCE, Integer.toString(balance));
        setval = getRightBalance();
        FileUtils.writeLine(Constants.ESS_BALANCE_RIGHT_SYSFS, setval + "");
    }

    public static int getRightBalance()
    {
        return SystemProperties.getInt(Constants.PROPERTY_RIGHT_BALANCE, 0);
    }

    public static boolean isEnabled()
    {
        String hifi_dac = SystemProperties.get(Constants.PROPERTY_ESS_STATUS);
        return hifi_dac.equals("ON");
    }

}
