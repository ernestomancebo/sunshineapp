package com.flectosystems.sunshine.app.models;

/**
 * Created by ernesto on 19/02/15.
 */
public class ForecastRequest {

    public String postalCode;
    public RequestMode mode = RequestMode.JSON;
    public RequestUnit unit = RequestUnit.METRIC;
    public int count = 7;

    public ForecastRequest(String p) {
        postalCode = p;
    }

    public enum RequestMode {
        JSON("json"), XML("xml");

        String mode;

        RequestMode(String m) {
            mode = m;
        }

        public String getMode() {
            return mode;
        }
    }

    public enum RequestUnit {
        METRIC("metric"), IMPERIAL("imperial");

        String unit;

        RequestUnit(String u) {
            unit = u;
        }

        public String getUnit() {
            return unit;
        }
    }


}
