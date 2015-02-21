package com.flectosystems.sunshine.app.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by ernesto on 19/02/15.
 */
public class JsonUtil {

    public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
            throws JSONException {
        final String DAY_LIST_ATTRIBUTE = "list";
        final String DAY_TEMP_ATTRIBUTE = "temp";

        JSONObject obj = new JSONObject(weatherJsonStr);
        JSONArray daysArray = obj.getJSONArray(DAY_LIST_ATTRIBUTE);
        JSONObject temperatureObj = daysArray.getJSONObject(dayIndex).getJSONObject(DAY_TEMP_ATTRIBUTE);

        Iterator<String> dayKeys = temperatureObj.keys();

        double currentMax = Double.valueOf(temperatureObj.get(dayKeys.next()).toString());

        while (dayKeys.hasNext()) {
            double aux = Double.valueOf(temperatureObj.get(dayKeys.next()).toString());

            if (aux > currentMax)
                currentMax = aux;
        }
        return currentMax;
    }
}
