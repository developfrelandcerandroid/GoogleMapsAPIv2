
package nguyen.tienhai.googlemaps_v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import nguyen.tienhai.googlemaps_v2.MyGeoCoder.LimitExceededException;

import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements LocationListener,
        OnMyLocationButtonClickListener, OnMapLongClickListener, InfoWindowAdapter,
        ConnectionCallbacks, OnConnectionFailedListener {

    // Google Map
    private GoogleMap googleMap;

    private LocationManager locationMng;

    MarkerOptions markerOptions;

    LatLng latLng;

    Button btn_find = null;
    OnClickListener findClickListener = null;

    Spinner mSprPlaceType;

    String[] mPlaceType = null;
    String[] mPlaceTypeName = null;

    double mLatitude = 0;
    double mLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MyDebugLog", "Enter onCreate of MainActivity -------------");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MyDebugLog", "after setContentView ----------------");
        try {
            // Loading map
            Log.d("MyDebugLog", "prepare to initilize map ---------");
            initilizeMap();
            locationMng = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationMng.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    20000, 20, this);
            Log.d("MyDebugLog", "comeback to onCreate after init map -----------");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Getting reference to btn_find of the layout activity_main
        btn_find = (Button) findViewById(R.id.btn_find);

        // Defining button click event listener for the find button
        findClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Getting reference to EditText to get the user input location
                EditText etLocation = (EditText) findViewById(R.id.et_location);

                // Getting user input location
                String location = etLocation.getText().toString();

                if (location != null && !location.equals("")) {
                    new GeocoderTask().execute(location);
                }
            }
        };

        // Setting button click event listener for the find button
        btn_find.setOnClickListener(findClickListener);

        // Array of place types
        mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);

        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);

        Button btnFindByPlace;

        // Getting reference to Find Button
        btnFindByPlace = (Button) findViewById(R.id.btn_findByPlace);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if (status != ConnectionResult.SUCCESS) { // Google Play Services are
                                                  // not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        } else {

            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();

            // Getting the name of the best provider
            String provider = locationMng.getBestProvider(criteria, true);

            // Getting Current Location From GPS
            Location location = locationMng.getLastKnownLocation(provider);

            if (location != null) {
                onLocationChanged(location);
            }

            locationMng.requestLocationUpdates(provider, 20000, 0, this);

            // Setting click event lister for the find button
            btnFindByPlace.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                    String type = mPlaceType[selectedPosition];

                    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                    sb.append("location=" + mLatitude + "," + mLongitude);
                    sb.append("&radius=5000");
                    sb.append("&types=" + type);
                    sb.append("&sensor=true");
                    sb.append("&key=AIzaSyDBPDABIrJTK2iKHj4gJSJQtKB4ObbYVfM");
                    Log.d("MyDebugLog", "String Request:" + sb.toString());
                    // Creating a new non-ui thread task to download json data
                    PlacesTask placesTask = new PlacesTask();

                    // Invokes the "doInBackground()" method of the class
                    // PlaceTask
                    placesTask.execute(sb.toString());

                }
            });

        }
    }

    /**
     * function to load map. If map is not created it will create it for you
     */
    private void initilizeMap() {
        if (googleMap == null) {
            SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            googleMap = supportMapFragment.getMap();

            if (googleMap != null) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.setOnMyLocationButtonClickListener(this);
                googleMap.getUiSettings().setCompassEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(true);
                // Setting a LONG click event handler for the map
                googleMap.setOnMapLongClickListener(this);
                googleMap.setInfoWindowAdapter(this);
            }
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }

            Log.d("MyDebugLog", "Process initilizeMap done -----------------------------");
            // CameraPosition cameraPosition = new
            // CameraPosition.Builder().target(new LatLng(21.04091214,
            // 105.7866729)).zoom(15).build();
            // googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    /** A class, to download Google Places */
    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result) {
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }

    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            MyPlaceJSONParser placeJsonParser = new MyPlaceJSONParser();

            try {
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parseJSON(jObject);

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String, String>> list) {

            // Clears all the existing markers
            googleMap.clear();

            for (int i = 0; i < list.size(); i++) {

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);

                // Getting latitude of the place
                double lat = Double.parseDouble(hmPlace.get("lat"));

                // Getting longitude of the place
                double lng = Double.parseDouble(hmPlace.get("lng"));

                // Getting name
                String name = hmPlace.get("place_name");

                // Getting vicinity
                String vicinity = hmPlace.get("vicinity");

                LatLng latLng = new LatLng(lat, lng);

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                markerOptions.title(name + " : " + vicinity);

                // Placing a marker on the touched position
                googleMap.addMarker(markerOptions).showInfoWindow();
            }
        }
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();

        // Getting reference to TextView to show the status
        TextView tvStatus = (TextView) findViewById(R.id.tv_status);

        // Getting status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if (status == ConnectionResult.SUCCESS)
            tvStatus.setText("Google Play Services are available");
        else {
            tvStatus.setText("Google Play Services are not available");
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // [BEGIN] implements OnConnectionCallback and OnConnectionFailedListener
    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnected(Bundle arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub

    } // [END] implements OnConnectionCallback and OnConnectionFailedListener

    // [BEGIN] implements LocationListener
    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        Log.d("MyDebugLog", "Enter onLocationChanged -------------------------------");
        Toast.makeText(getApplicationContext(),
                location.getLatitude() + ", " + location.getLongitude(),
                Toast.LENGTH_LONG).show();

        // googleMap.clear();

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        // Creating an instance of GeoPoint, to display in Google Map
        LatLng latLng = new LatLng(mLatitude, mLongitude);
        // Animating to the currently position
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        // Creating an instance of MarkerOptions to set position
        markerOptions = new MarkerOptions();

        // Setting position on the MarkerOptions
        markerOptions.position(latLng);
        // Setting icon of maker
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        new ReverseGeocodingTask().execute(latLng);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    } // [END] implements LocationListener

    // [BEGIN] implements OnMyLocationButtonClickListener
    @Override
    public boolean onMyLocationButtonClick() {
        // TODO Auto-generated method stub
        // Toast.makeText(this, "MyLocation button clicked",
        // Toast.LENGTH_SHORT).show();
        Location location = googleMap.getMyLocation();
        if (location != null) {
            Toast.makeText(getApplicationContext(),
                    "MyLocation button clicked\n" + location.getLatitude()
                            + ", " + location.getLongitude(), Toast.LENGTH_LONG)
                    .show();

            latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Creating an instance of MarkerOptions to set position
            markerOptions = new MarkerOptions();

            // Setting position on the MarkerOptions
            markerOptions.position(latLng);
            // Setting icon of maker
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            // Animating to the currently position
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
            new ReverseGeocodingTask().execute(latLng);
        }
        return false;
    } // [END] implements OnMyLocationButtonClickListener

    // [BEGIN] implements OnMapLongClickListener
    @Override
    public void onMapLongClick(LatLng arg0) {
        // TODO Auto-generated method stub
        Log.d("MyDebugLog", "vao onMapLongClick -------------");
        // Getting the Latitude and Longitude of the touched location
        latLng = arg0;

        // Clears the previously touched position
        googleMap.clear();

        // Animating to the touched position
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        // Creating a marker
        markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(latLng);

        // Adding Marker on the long touched location with address
        new ReverseGeocodingTask().execute(latLng);
    } // [END] implements OnMapLongClickListener

    private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {

        // Finding address using reverse geocoding
        @Override
        protected String doInBackground(LatLng... params) {
            MyGeoCoder geocoder = new MyGeoCoder(getBaseContext());
            double latitude = params[0].latitude;
            double longitude = params[0].longitude;

            List<Address> addresses = null;
            String addressText = "";

            try {
                Log.d("MyDebugLog", "Enter doInBackground -------" + latitude + ":" + longitude);
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                Log.d("MyDebugLog", "after geocoder.getFromLocation -------" + latitude + ":" + longitude);
            } catch (IOException e) {
                Log.d("MyDebugLog", "if geocoder.getFromLocation get error - before Tracing ------");
                e.printStackTrace();
                Log.d("MyDebugLog", "if geocoder.getFromLocation get error - after Tracing ------");
            } catch (LimitExceededException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);

                addressText = String.format("%s", address.getFeatureName());
            }

            return addressText;
        }

        @Override
        protected void onPostExecute(String addressText) {
            // Setting the title for the marker.
            // This will be displayed on taping the marker
            markerOptions.title(addressText);
            Log.d("MyDebugLog", "At onPostExecute - after set title of makerOptions:[" + addressText + "]:");
            // Placing a marker & Showing InfoWindow on the GoogleMap
            googleMap.addMarker(markerOptions).showInfoWindow();

        }
    }

    // [BEGIN] An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {

        @Override
        protected List<Address> doInBackground(String... locationName) {
            // Creating an instance of Geocoder class
            MyGeoCoder geocoder = new MyGeoCoder(getBaseContext());
            List<Address> addresses = null;

            try {
                Log.d("MyDebugLog", "vao GeocoderTask.doInBackground() ----------" + locationName[0]);
                // Getting a maximum of 3 Address that matches the input text
                addresses = geocoder.getFromLocationName(locationName[0], 3);
                Log.d("MyDebugLog", "sometimes it crashes - FIND ------------");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (LimitExceededException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {

            if (addresses == null || addresses.size() == 0) {
                Toast.makeText(getBaseContext(), "No Location found", Toast.LENGTH_SHORT).show();
            }

            // Clears all the existing markers on the map
            googleMap.clear();

            // Adding Markers on Google Map for each matching address
            for (int i = 0; i < addresses.size(); i++) {

                Address address = addresses.get(i);

                // Creating an instance of GeoPoint, to display in Google Map
                latLng = new LatLng(address.getLatitude(), address.getLongitude());

                String addressText = String.format("%s", address.getFeatureName());

                markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(addressText);
                Log.d("MyDebugLog", "At onPostExecute - after set title of makerOptions:[" + addressText + "]:");
                // Placing a marker & Showing InfoWindow on the GoogleMap
                googleMap.addMarker(markerOptions).showInfoWindow();
                // Locate the first location
                if (i == 0)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        }
    } // [END] AsyncTask class for accessing the GeoCoding Web Service

    @Override
    public View getInfoContents(Marker arg0) {
        // TODO Auto-generated method stub
        // [] Defines the contents of the InfoWindow --------

        // Getting view from the layout file info_window_layout
        View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);

        // Getting the position from the marker
        LatLng latLng = arg0.getPosition();

        // Getting reference to the TextView to set latitude
        TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
        // Getting reference to the TextView to set longitude
        TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);
        // Getting reference to the TextView to set longitude
        TextView tvLocation = (TextView) v.findViewById(R.id.tv_location);

        // Setting the latitude
        tvLat.setText("Latitude:" + latLng.latitude);
        // Setting the longitude
        tvLng.setText("Longitude:" + latLng.longitude);
        // Setting the longitude
        tvLocation.setText(arg0.getTitle());

        // Returning the view containing InfoWindow contents
        return v;
    }

    @Override
    public View getInfoWindow(Marker arg0) {
        // TODO Auto-generated method stub
        // Use default InfoWindow frame
        return null;
    }

}
