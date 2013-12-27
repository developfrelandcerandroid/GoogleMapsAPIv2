/**
 * 
 */

package nguyen.tienhai.googlemaps_v2;

/**
 * @author Admin
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Address;
import android.util.Log;

/**
 * A class for handling geocoding and reverse geocoding. Geocoding is the
 * process of transforming a street address or other description of a location
 * into a (latitude, longitude) coordinate. Reverse geocoding is the process of
 * transforming a (latitude, longitude) coordinate into a (partial) address. The
 * amount of detail in a reverse geocoded location description may vary, for
 * example one might contain the full street address of the closest building,
 * while another might contain only a city name and postal code. This is a
 * replacement of built-in Geocoder which is not always available. This
 * implementation parses just a formatted address and is guaranteed to work.
 */
public final class MyGeoCoder
{

    private static final String PREFERENCES_GEOCODER = MyGeoCoder.class.getName() + ".GEOCODER";
    private static final String KEY_ALLOW = MyGeoCoder.class.getName() + ".KEY_ALLOW";

    /*
     * Status codes which we handle
     */

    /**
     * Indicates that no errors occurred; the address was successfully parsed
     * and at least one geocode was returned.
     */
    private static final String STATUS_OK = "OK";

    /**
     * Indicates that you are over your quota.
     */
    private static final String STATUS_OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";

    private final Context mContext;

    /**
     * Constructs a MyGeoCoder whose responses will be localized for the default
     * system Locale.
     * 
     * @param context the Context of the calling Activity
     */
    public MyGeoCoder(Context context)
    {
        this.mContext = context;
    }

    /**
     * Returns an array of Addresses that are known to describe the area
     * immediately surrounding the given latitude and longitude. The returned
     * addresses will be localized for the locale provided to this class's
     * constructor.
     * <p>
     * The returned values may be obtained by means of a network lookup. The
     * results are a best guess and are not guaranteed to be meaningful or
     * correct. It may be useful to call this method from a thread separate from
     * your primary UI thread.
     * 
     * @param latitude the latitude a point for the search
     * @param longitude the longitude a point for the search
     * @param maxResults max number of addresses to return. Smaller numbers (1
     *            to 5) are recommended
     * @return a list of Address objects. Returns null or empty list if no
     *         matches were found or there is no backend service available.
     * @throws IllegalArgumentException if latitude is less than -90 or greater
     *             than 90
     * @throws IllegalArgumentException if longitude is less than -180 or
     *             greater than 180
     * @throws IOException if the network is unavailable or any other I/O
     *             problem occurs
     */
    public List<Address> getFromLocation(double latitude, double longitude,
            int maxResults) throws IOException, LimitExceededException
    {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude == " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude == " + longitude);
        }

        if (isLimitExceeded(mContext)) {
            throw new LimitExceededException();
        }

        final List<Address> results = new ArrayList<Address>();

        final StringBuilder url = new StringBuilder(
                "http://maps.googleapis.com/maps/api/geocode/json?sensor=true&latlng=");
        url.append(latitude);
        url.append(',');
        url.append(longitude);
        url.append("&language=");
        url.append(Locale.getDefault().getLanguage());

        final String data = getHttpClientResults(url.toString());
        if (data != null) {
            this.parseJson(results, maxResults, data);
        }
        return results;
    }

    /**
     * Returns an array of Addresses that are known to describe the named
     * location, which may be a place name such as "Dalvik,
     * Iceland", an address such as "1600 Amphitheatre Parkway, Mountain View,
     * CA", an airport code such as "SFO", etc.. The returned addresses will be
     * localized for the locale provided to this class's constructor.
     * <p>
     * The query will block and returned values will be obtained by means of a
     * network lookup. The results are a best guess and are not guaranteed to be
     * meaningful or correct. It may be useful to call this method from a thread
     * separate from your primary UI thread.
     * 
     * @param locationName a user-supplied description of a location
     * @param maxResults max number of results to return. Smaller numbers (1 to
     *            5) are recommended
     * @return a list of Address objects. Returns null or empty list if no
     *         matches were found or there is no backend service available.
     * @throws IllegalArgumentException if locationName is null
     * @throws IOException if the network is unavailable or any other I/O
     *             problem occurs
     */
    public List<Address> getFromLocationName(String locationName, int maxResults)
            throws IOException, LimitExceededException
    {
        if (locationName == null) {
            throw new IllegalArgumentException("locationName == null");
        }

        if (isLimitExceeded(mContext)) {
            throw new LimitExceededException();
        }

        final List<Address> results = new ArrayList<Address>();

        final StringBuilder request = new StringBuilder(
                "http://maps.googleapis.com/maps/api/geocode/json?sensor=false");
        request.append("&language=").append(Locale.getDefault().getLanguage());
        request.append("&address=").append(
                URLEncoder.encode(locationName, "UTF-8"));

        String data = getHttpClientResults(request.toString());
        if (data != null) {
            try {
                this.parseJson(results, maxResults, data);
            } catch (LimitExceededException e) {
                // LimitExceededException could be thrown if too many calls per
                // second
                // If after two seconds, it is thrown again - then it means
                // there are too much calls per 24 hours
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    return results;
                }
                data = getHttpClientResults(request.toString());
                if (data != null) {
                    try {
                        this.parseJson(results, maxResults, data);
                    } catch (LimitExceededException lee) {
                        // available in 24 hours
                        setAllowedDate(mContext, System.currentTimeMillis() + 86400000L);
                        throw lee;
                    }
                }
            }
        }
        return results;
    }

    private String getHttpClientResults(String urlStr)
    {
        String response = "";
        HttpClient client = new DefaultHttpClient();

        Log.d("MyDebugLog", urlStr);
        try {
            HttpGet httpGet = new HttpGet(urlStr);
            HttpResponse httpResponse = client.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
                Log.d("MyDebugLog", "getHttpClientResults - check BufferedReader br:" + br.toString());
                String buff = null;
                while ((buff = br.readLine()) != null)
                    response += buff;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("MyDebugLog", "getHttpClientResults - check value of response:" + response);
        return response;
    }

    private void parseJson(List<Address> address, int maxResults, String data)
            throws LimitExceededException
    {
        try {
            final JSONObject jsonObject = new JSONObject(data);
            final String status = jsonObject.getString("status");
            if (status.equals(STATUS_OK)) {

                final JSONArray responseArray = jsonObject.getJSONArray("results");
                Log.d("MyDebugLog", "parseJson:responseArray length:" + responseArray.length() + " result(s)");
                for (int i = 0; i < maxResults && i < responseArray.length(); i++) {
                    final Address current = new Address(Locale.getDefault());
                    final JSONObject item = responseArray.getJSONObject(i);

                    current.setFeatureName(item.getString("formatted_address"));
                    final JSONObject location = item.getJSONObject("geometry").getJSONObject("location");
                    current.setLatitude(location.getDouble("lat"));
                    current.setLongitude(location.getDouble("lng"));

                    address.add(current);
                }

            } else if (status.equals(STATUS_OVER_QUERY_LIMIT)) {

                throw new LimitExceededException();

            }
        } catch (LimitExceededException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    /**
     * Returns true if limit is exceeded and next query is not allowed
     * 
     * @param context Current context
     * @return true if limit is exceeded and next query is not allowed; false
     *         otherwise
     */
    private static boolean isLimitExceeded(Context context)
    {
        return System.currentTimeMillis() <= getAllowedDate(context);
    }

    /**
     * Sets date after which next geocoding query is allowed
     * 
     * @param context Current context
     * @param date the date after which next geocoding query is allowed
     */
    private static void setAllowedDate(Context context, long date)
    {
        final SharedPreferences p = context.getSharedPreferences(
                PREFERENCES_GEOCODER, Context.MODE_PRIVATE);
        final Editor e = p.edit();
        e.putLong(KEY_ALLOW, date);
        e.commit();
    }

    /**
     * Returns date after which the next geocoding query is allowed
     * 
     * @param context Current context
     * @return date after which the next geocoding query is allowed
     */
    private static long getAllowedDate(Context context)
    {
        final SharedPreferences p = context.getSharedPreferences(
                PREFERENCES_GEOCODER, Context.MODE_PRIVATE);
        return p.getLong(KEY_ALLOW, 0);
    }

    /**
     * Is thrown when the query was over limit before 24 hours
     */
    public static final class LimitExceededException extends Exception
    {
        private static final long serialVersionUID = -1243645207607944474L;
    }

}
