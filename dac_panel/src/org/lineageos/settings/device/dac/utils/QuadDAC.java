package org.lineageos.settings.device.dac.utils;

import android.media.AudioSystem;
import android.os.SystemProperties;
import android.util.Log;

import org.lineageos.hardware.util.FileUtils;
import org.lineageos.settings.device.dac.QuadDACPanelFragment;

public class QuadDAC {

    private static final String TAG = "QuadDAC";

    private QuadDAC() {}

    public static void enable()
    {
        SystemProperties.set(Constants.PROPERTY_ESS_STATUS, "true");
    }

    public static void enabledSetup()
    {
        int i;
        int mode = getDACMode();
        int left_balance = getLeftBalance();
        int right_balance = getRightBalance();
        int digital_filter = getDigitalFilter();
        int avc_vol = getAVCVolume();
        int custom_filter_shape = getCustomFilterShape();
        int custom_filter_symmetry = getCustomFilterSymmetry();
        int[] custom_filter_coefficients = new int[14];
        for(i = 0; i < 14; i++) {
            custom_filter_coefficients[i] = getCustomFilterCoeff(i);
        }

        SystemProperties.set(Constants.PROPERTY_ESS_MODE, Integer.toString(mode));
        setDACMode(mode);
        setLeftBalance(left_balance);
        setRightBalance(right_balance);
        setDigitalFilter(digital_filter);
        setAVCVolume(avc_vol);
        setCustomFilterShape(custom_filter_shape);
        setCustomFilterSymmetry(custom_filter_symmetry);
        for(i = 0; i < 14; i++) {
            setCustomFilterCoeff(i, custom_filter_coefficients[i]);
        }
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
        if(filter == 3) { /* Custom filter */
            /*
             * If it's a custom filter, we need to apply its settings. Any of the functions
             * below should suffice since it'll load all settings from memory by parsing its
             * data.
             */
            setCustomFilterShape(getCustomFilterShape());
        }
    }

    public static int getDigitalFilter()
    {
        return SystemProperties.getInt(Constants.PROPERTY_DIGITAL_FILTER, 0);
    }

    public static void setCustomFilterShape(int shape) {
        if(shape <=4)
            SystemProperties.set(Constants.PROPERTY_CUSTOM_FILTER_SHAPE, Integer.toString(shape));
        else /* Filter 5 (counting from 0) is enumerated 6 on es9218.h, so anything after receives +1 as well */
            SystemProperties.set(Constants.PROPERTY_CUSTOM_FILTER_SHAPE, Integer.toString(shape+1));
        FileUtils.writeLine(Constants.ESS_CUSTOM_FILTER_SYSFS, parseUpdatedCustomFilterData());
    }

    public static int getCustomFilterShape() {
        return SystemProperties.getInt(Constants.PROPERTY_CUSTOM_FILTER_SHAPE, 0);
    }

    public static void setCustomFilterSymmetry(int symmetry) {
        SystemProperties.set(Constants.PROPERTY_CUSTOM_FILTER_SYMMETRY, Integer.toString(symmetry));
        FileUtils.writeLine(Constants.ESS_CUSTOM_FILTER_SYSFS, parseUpdatedCustomFilterData());
    }

    public static int getCustomFilterSymmetry() {
        return SystemProperties.getInt(Constants.PROPERTY_CUSTOM_FILTER_SYMMETRY, 0);
    }

    public static void setCustomFilterCoeff(int coeffIndex, int coeff_val) {
        SystemProperties.set(Constants.PROPERTY_CUSTOM_FILTER_COEFFS[coeffIndex], Integer.toString(coeff_val));
        FileUtils.writeLine(Constants.ESS_CUSTOM_FILTER_SYSFS, parseUpdatedCustomFilterData());
    }

    public static int getCustomFilterCoeff(int coeffIndex) {
        return SystemProperties.getInt(Constants.PROPERTY_CUSTOM_FILTER_COEFFS[coeffIndex], 0);
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

    private static String parseUpdatedCustomFilterData() {
        StringBuilder temp_string = new StringBuilder();

        /*
         * Let's build the actual string with the custom filter's shape, symmetry and 14 Stage 2 coefficients
         *
         */
        temp_string.append(SystemProperties.getInt(Constants.PROPERTY_CUSTOM_FILTER_SHAPE, 0)).append(",");
        temp_string.append(SystemProperties.getInt(Constants.PROPERTY_CUSTOM_FILTER_SYMMETRY, 0)).append(",");
        for(int i = 0; i < 14; i++) {
            temp_string.append(SystemProperties.getInt(Constants.PROPERTY_CUSTOM_FILTER_COEFFS[i], 0));
            if(i < 13) /* Last element doesn't need to have a comma appended after it */
                temp_string.append(",");
        }

        return temp_string.toString();
    }

}
