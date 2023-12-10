package org.lineageos.settings.device.dac.utils;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String DAC_SWITCH_KEY = "quaddac_switch";
    public static final String DIGITAL_FILTER_KEY = "digital_filter_dropdown";
    public static final String BALANCE_KEY = "balance";
    public static final String RESET_COEFFICIENTS_KEY = "reset_coefficients";
    public static final String HIFI_DOP_KEY = "hifi_dop_dropdown";
    public static final String HIFI_MODE_KEY = "hifi_mode_dropdown";
    public static final String AVC_VOLUME_KEY = "avc_volume_seekbar";

    public static final String   CUSTOM_FILTER_SHAPE_KEY = "custom_filter_shape";
    public static final String   CUSTOM_FILTER_SYMMETRY_KEY = "custom_filter_symmetry";
    public static final String[] CUSTOM_FILTER_COEFF_KEYS = { "custom_filter_coeff0", "custom_filter_coeff1", "custom_filter_coeff2",
            "custom_filter_coeff3", "custom_filter_coeff4", "custom_filter_coeff5", "custom_filter_coeff6", "custom_filter_coeff7",
            "custom_filter_coeff8", "custom_filter_coeff9", "custom_filter_coeff10", "custom_filter_coeff11", "custom_filter_coeff12",
            "custom_filter_coeff13" };

    public static final String SET_DAC_ON_COMMAND = "hifi_dac=on";
    public static final String SET_DAC_OFF_COMMAND = "hifi_dac=off";
    public static final String SET_HIFI_DAC_DOP_COMMAND = "hifi_dac_dop=";
    public static final String SET_DIGITAL_FILTER_COMMAND = "ess_filter=";
    public static final String SET_LEFT_BALANCE_COMMAND = "hifi_dac_l_volume=";
    public static final String SET_RIGHT_BALANCE_COMMAND = "hifi_dac_r_volume=";

    public static final String   PROPERTY_ESS_MODE = "persist.audio.ess.mode";
    public static final String   PROPERTY_ESS_STATUS = "persist.audio.hifi.enabled";
    public static final String   PROPERTY_DIGITAL_FILTER = "persist.audio.ess.digitalFilter"; // not yet implemented in audio hal
    public static final String   PROPERTY_LEFT_BALANCE = "persist.audio.ess.left_balance"; // not yet implemented in audio hal
    public static final String   PROPERTY_RIGHT_BALANCE = "persist.audio.ess.right_balance"; // not yet implemented in audio hal
    public static final String   PROPERTY_HIFI_DAC_DOP = "persist.audio.ess.dop"; // not yet implemented in audio hal
    public static final String   PROPERTY_HIFI_DAC_MODE = "persist.audio.ess.dacmode"; // not yet implemented in audio hal
    public static final String   PROPERTY_HIFI_DAC_AVC_VOLUME = "persist.audio.ess.avc_volume"; // not yet implemented in audio hal
    public static final String   PROPERTY_CUSTOM_FILTER_SHAPE = "persist.audio.ess.customFilterShape";
    public static final String   PROPERTY_CUSTOM_FILTER_SYMMETRY = "persist.audio.ess.customFilterSymmetry";
    public static final String[] PROPERTY_CUSTOM_FILTER_COEFFS = { "persist.audio.ess.customFilterCoeff0", "persist.audio.ess.customFilterCoeff1",
            "persist.audio.ess.customFilterCoeff2", "persist.audio.ess.customFilterCoeff3", "persist.audio.ess.customFilterCoeff4",
            "persist.audio.ess.customFilterCoeff5", "persist.audio.ess.customFilterCoeff6", "persist.audio.ess.customFilterCoeff7",
            "persist.audio.ess.customFilterCoeff8", "persist.audio.ess.customFilterCoeff9", "persist.audio.ess.customFilterCoeff10",
            "persist.audio.ess.customFilterCoeff11", "persist.audio.ess.customFilterCoeff12", "persist.audio.ess.customFilterCoeff13" };

    public static final String AVC_VOLUME_SYSFS = "/sys/kernel/es9218_dac/avc_volume";
    public static final String ESS_FILTER_SYSFS = "/sys/kernel/es9218_dac/ess_filter";
    public static final String ESS_BALANCE_LEFT_SYSFS = "/sys/kernel/es9218_dac/left_volume";
    public static final String ESS_BALANCE_RIGHT_SYSFS = "/sys/kernel/es9218_dac/right_volume";
    public static final String ESS_CUSTOM_FILTER_SYSFS = "/sys/kernel/es9218_dac/ess_custom_filter";


    public static final HashMap<Double, Integer> balanceHashMap = new HashMap<>();
    public static final HashMap<Integer, Double> balanceHashMapReverse = new HashMap<>();

    static {
        balanceHashMap.put(0.0, 0);
        balanceHashMap.put(-0.5, 1);
        balanceHashMap.put(-1.0, 2);
        balanceHashMap.put(-1.5, 3);
        balanceHashMap.put(-2.0, 4);
        balanceHashMap.put(-2.5, 5);
        balanceHashMap.put(-3.0, 6);
        balanceHashMap.put(-3.5, 7);
        balanceHashMap.put(-4.0, 8);
        balanceHashMap.put(-4.5, 9);
        balanceHashMap.put(-5.0, 10);
        balanceHashMap.put(-5.5, 11);
        balanceHashMap.put(-6.0, 12);

        for (Map.Entry<Double, Integer> entry : balanceHashMap.entrySet()) {
            balanceHashMapReverse.put(entry.getValue(), entry.getKey());
        }

    }

}
