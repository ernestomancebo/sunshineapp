package com.flectosystems.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.flectosystems.sunshine.app.Utils.JsonForecastUtil;
import com.flectosystems.sunshine.app.models.Constants;
import com.flectosystems.sunshine.app.models.ForecastRequest;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ernesto on 17/02/15.
 */
public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    ArrayAdapter<String> adapter;

    public ForecastFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        List<String> forecastEntry = new ArrayList<>();

        forecastEntry.add("Today, Sunny");
        forecastEntry.add("Tomorrow, Rainy");
        forecastEntry.add("Sat, Sunny");
        forecastEntry.add("Sun, Cloudy");


        adapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                forecastEntry
        );

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = adapter.getItem(position);

                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(detailIntent);
            }
        });
        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        } else if (id == R.id.action_open_location) {

            // Read preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String postCode = preferences.getString(
                    getString(R.string.pref_location_key), getString(R.string.pref_location_default)
            );

            Uri geoLocation = new Uri.Builder()
                    .scheme("geo")
                    .appendEncodedPath("0,0?=" + postCode)
                    .build();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(geoLocation);

            if (null != intent.resolveActivity(getActivity().getPackageManager())) {
                startActivity(intent);
            }

        }

        return super.onOptionsItemSelected(item);
    }

    public void updateWeather() {
        // Read preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String postCode = preferences.getString(
                getString(R.string.pref_location_key), getString(R.string.pref_location_default)
        );

        Log.v(LOG_TAG, "Postal Code: " + postCode);

        ForecastRequest request = new ForecastRequest(postCode);
        FetchWeatherTask task = new FetchWeatherTask();

        task.execute(request);
    }

    class FetchWeatherTask extends AsyncTask<ForecastRequest, String[], String[]> {

        @Override
        protected String[] doInBackground(ForecastRequest... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri.Builder builder = new Uri.Builder();
                builder.encodedPath("http://api.openweathermap.org/data/2.5/forecast/daily");

                ForecastRequest req = params[0];

                builder.appendQueryParameter(Constants.PARAM_QUERY, req.postalCode);
                builder.appendQueryParameter(Constants.PARAM_MODE, req.mode.getMode());
                builder.appendQueryParameter(Constants.PARAM_UNITS, req.unit.getUnit());
                builder.appendQueryParameter(Constants.PARAM_COUNT, String.valueOf(req.count));

                URL url = new URL(builder.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty. No point in parsing.
                    forecastJsonStr = null;
                } else {
                    forecastJsonStr = buffer.toString();
                }

                String[] rv = JsonForecastUtil.getWeatherDataFromJson(forecastJsonStr, 7, getActivity());
                Log.v(LOG_TAG, rv[1]);

                return rv;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);

            adapter.clear();

            // addAll() method is available since api 11, current api is 10. Shit :\
            for (String s : strings)
                adapter.add(s)
                        ;
            adapter.notifyDataSetChanged();
        }
    }
}