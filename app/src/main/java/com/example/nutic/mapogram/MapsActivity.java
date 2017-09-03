package com.example.nutic.mapogram;

import android.Manifest.permission;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout.LayoutParams;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.nutic.mapogram.model.Friends;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Line;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
  ConnectionCallbacks, OnConnectionFailedListener, OnMarkerClickListener, LocationListener,
  NavigationView.OnNavigationItemSelectedListener {

    public static final String PREFS_NAME = "MapogramPrefs";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final float DEFAULT_ZOOM = 12;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Marker mCurrentMarker;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private BroadcastReceiver mDeviceDiscoverReceiver;
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<Marker> visibleMarkers = new ArrayList<Marker>();
    private ArrayList<String> selectedCategories = new ArrayList<String>();

    private String urlCategories = "http://mapogram.dejan7.com/api/categories";

    private boolean runPollFriends = false;
    private boolean runPollFriendsWasRunning = false;

    private static HashMap<String, Marker> mFriendsMarkersHashMap = new HashMap<String, Marker>();

    private String urlPhotos = "http://mapogram.dejan7.com/api/photos";

    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        mDeviceDiscoverReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    Toast.makeText(getApplicationContext(), deviceName, Toast.LENGTH_SHORT).show();
                }
            }
        };

        registerReceiver(mDeviceDiscoverReceiver, filter);

        final Button findFriendsBTN = (Button) findViewById(R.id.findFriends);
        findFriendsBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runPollFriends) {
                    stopLocationExchange(true);
                } else {
                    startLocationExchange(true);
                }

            }
        });

        getCategories();

    }

    public void startLocationExchange(Boolean firstTime) {
        final Button findFriendsBTN = (Button) findViewById(R.id.findFriends);
        runPollFriends = true;
        if (firstTime)
            getUsers(mLastLocation);
        else
            pollFriends();
        findFriendsBTN.setText("Turn Off Geolocation");
    }

    public void stopLocationExchange(Boolean removeMarkers) {
        final Button findFriendsBTN = (Button) findViewById(R.id.findFriends);
        runPollFriends = false;
        if (removeMarkers)
            clearFriendMarkers();

        findFriendsBTN.setText("Find Friends!");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mDeviceDiscoverReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMarkerClickListener(this);

        setUpMap();
    }

    private void setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initMap();
        }
    }

    private void initMap() {
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMap();
                } else {
                    return;
                }
                break;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); // intervali osvezavanja
        mLocationRequest.setFastestInterval(50000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        updateCurrentLocation(location);
        getMarkers(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 5000);  //5km
    }

    private void updateCurrentLocation(Location location) {
        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
        }

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        final View markerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.cutom_marker, null);
        final ImageView avatar = (ImageView) markerView.findViewById(R.id.image_avatar);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String avatarURL = settings.getString("avatar", null);
        if (avatarURL == "null")
            avatarURL = "https://www.android.com/static/2016/img/aife/homepage/history/2010_1x.jpg";
        ImageRequest request = new ImageRequest(avatarURL,  // TODO change url
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        avatar.setImageBitmap(bitmap);
                        markerOptions.icon(BitmapDescriptorFactory
                                .fromBitmap(createDrawableFromView(MapsActivity.this, markerView)));
                        markerOptions.position(latLng);


                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        mCurrentMarker = mMap.addMarker(markerOptions);
                        mCurrentMarker.setTag(new CustomMarkerTag("user", settings.getString("username", null)));
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);

    }

    private void getUsers(final Location location) {

        Map<String, String> params = new HashMap();
        params.put("location", String.valueOf(location.getLongitude()) + "," + location.getLatitude());

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST,
                "http://mapogram.dejan7.com/api/location/exchange", new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray array = response.getJSONArray("friends");
                            List<Friends> friendsList = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                if (obj.optString("location") == "false")
                                    continue;
                                Friends friend = new Friends();
                                friend.setUsername(obj.optString("username"));
                                friend.setAvatar(obj.optString("avatar"));
                                friend.setFirstName(obj.optString("first_name"));
                                friend.setLastName(obj.optString("last_name"));
                                friend.setLocation(obj.optString("location"));
                                friendsList.add(friend);
                            }

                            showFriendsOnMap(friendsList);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("tag", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                headers.put("Authorization", "Bearer " + settings.getString("token", null));
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    private void showFriendsOnMap(List<Friends> friendsList) {
        for (final Friends friend : friendsList) {
            final MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(friend.getUsername());
            String[] exploded = friend.getLocation().split(",");

            String lng = exploded[0];
            String lat = exploded[1];

            final LatLng latLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            if (friend.getAvatar() != null) {
                final View markerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.cutom_marker, null);
                final ImageView avatar = (ImageView) markerView.findViewById(R.id.image_avatar);
                ImageRequest request = new ImageRequest(friend.getAvatar(),
                        new Response.Listener<Bitmap>() {
                            @Override
                            public void onResponse(Bitmap bitmap) {
                                avatar.setImageBitmap(bitmap);
                                markerOptions.icon(BitmapDescriptorFactory
                                        .fromBitmap(createDrawableFromView(MapsActivity.this, markerView)));
                                markerOptions.position(latLng);
                                Marker result = mMap.addMarker(markerOptions);
                                result.setTag(new CustomMarkerTag("user", friend.getUsername()));
                                mFriendsMarkersHashMap.put(friend.getUsername(), result);


                            }
                        }, 0, 0, null,
                        new Response.ErrorListener() {
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                RequestQueue queue = Volley.newRequestQueue(this);
                queue.add(request);
            } else {
                markerOptions.position(latLng);
                Marker result = mMap.addMarker(markerOptions);
                result.setTag(new CustomMarkerTag("user", friend.getUsername()));
                mFriendsMarkersHashMap.put(friend.getUsername(), result);
            }


            pollFriends();
        }
    }

    public void pollFriends() {
        runPollFriends = true;
        handler.postDelayed(runnablePollFriends, 3000);
    }

    private Runnable runnablePollFriends = new Runnable() {
        @Override
        public void run() {
            Map<String, String> params = new HashMap();
            params.put("location", String.valueOf(mLastLocation.getLongitude()) + "," + mLastLocation.getLatitude());

            JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST,
                    "http://mapogram.dejan7.com/api/location/exchange", new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray array = response.getJSONArray("friends");
                                List<Friends> friendsList = new ArrayList<>();
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    if (obj.optString("location") == "false")
                                        continue;
                                    //Log.e("tagrrr", obj.optString("location"));
                                    Friends friend = new Friends();
                                    friend.setId(Integer.parseInt(obj.optString("id")));
                                    friend.setLocation(obj.optString("location"));

                                    String[] exploded = friend.getLocation().split(",");
                                    LatLng newPost = new LatLng(Double.parseDouble(exploded[1]), Double.parseDouble(exploded[0]));

                                    Marker setMarker = mFriendsMarkersHashMap.get(obj.optString("username"));

                                    if (setMarker != null) {
                                        setMarker.setPosition(newPost);
                                        Log.e("testMarkera", setMarker.getPosition().toString());
                                    }
                                }
                                    if (runPollFriends)
                                        handler.postDelayed(runnablePollFriends, 3000);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("tag", error.toString());
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("Accept", "application/json");
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    headers.put("Authorization", "Bearer " + settings.getString("token", null));
                    return headers;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            queue.add(jsObjRequest);
        }

    };

    public Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap
                .createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_add_friend: {
                Toast.makeText(this, "ADD FRIEND", Toast.LENGTH_SHORT).show();
                sendFriendRequestViaBlootooth();
                return true;
            }
            case R.id.myprofile: {
                Intent intent = new Intent(MapsActivity.this, ProfileActivity.class);
                intent.putExtra("latitude", mLastLocation.getLatitude());
                intent.putExtra("longitude", mLastLocation.getLongitude());
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                intent.putExtra("username", settings.getString("username", null));
                MapsActivity.this.startActivity(intent);
                return true;
            }
            case R.id.nav_add_photo: {
                Intent intent = new Intent(MapsActivity.this, AddPhotoActivity.class);
                intent.putExtra("latitude", mLastLocation.getLatitude());
                intent.putExtra("longitude", mLastLocation.getLongitude());
                MapsActivity.this.startActivity(intent);
                return true;
            }
            case R.id.nav_top_list: {
                Intent intent = new Intent(MapsActivity.this, TopListActivity.class);
                intent.putExtra("latitude", mLastLocation.getLatitude());
                intent.putExtra("longitude", mLastLocation.getLongitude());
                MapsActivity.this.startActivity(intent);
                return true;
            }
            case R.id.logout: {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.remove("token");
                editor.apply();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                startActivity(intent);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private void sendFriendRequestViaBlootooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                mBluetoothAdapter.startDiscovery();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
                mBluetoothAdapter.startDiscovery();
            }
        }
    }

    private void getCategories() {
        JsonArrayRequest jsObjRequest = new JsonArrayRequest(Request.Method.GET, urlCategories, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                final LinearLayout hScrollView = (LinearLayout) findViewById(R.id.tagsScroll);
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject tagJSON = response.getJSONObject(i);
                        View tagView = getLayoutInflater().inflate(R.layout.tag, null);
                        hScrollView.addView(tagView);

                        final TextView tagBtn = (TextView) tagView.findViewById(R.id.tagButton);

                        tagBtn.setText(tagJSON.getString("name"));

                        tagBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                handleCategoryButtonClick(view);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = "";
                try {
                    JSONObject errResponse = new JSONObject(new String(error.networkResponse.data));
                    message = errResponse.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Error: " + message, Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                headers.put("Authorization", "Bearer " + settings.getString("token", null));
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    private void getMarkers(Double latitude, Double longitude, int distance) {

        String url = urlPhotos + "/" + String.valueOf(longitude) + "," + String.valueOf(latitude) + "/" + String.valueOf(distance);
        clearPhotoMarkers();

        String categories = "/";

        for (String s : selectedCategories)
            categories += s.replace("#", "").trim() + ",";

        url = url + categories.substring(0, categories.length() - 1);

        Log.e("XXXX ==>>", url);

        JsonArrayRequest jsObjRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject photoJSON = response.getJSONObject(i);

                        String location = photoJSON.getString("location");
                        String[] split = location.split(",");

                        LatLng latLng = new LatLng(Double.valueOf(split[1]), Double.valueOf(split[0]));
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.title(photoJSON.getString("description"));

                        Marker marker = mMap.addMarker(markerOptions);

                        marker.setTag(new CustomMarkerTag("photo", photoJSON.getString("id")));
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

                        visibleMarkers.add(marker);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = "";
                try {
                    JSONObject errResponse = new JSONObject(new String(error.networkResponse.data));
                    message = errResponse.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Error: " + message, Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                headers.put("Authorization", "Bearer " + settings.getString("token", null));
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsObjRequest);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        CustomMarkerTag tag = (CustomMarkerTag) marker.getTag();

        if (tag.getType() == "photo") {  // Then is photo marker
            Intent intent = new Intent(MapsActivity.this, PhotoActivity.class);

            intent.putExtra("latitude", mLastLocation.getLatitude());
            intent.putExtra("longitude", mLastLocation.getLongitude());
            intent.putExtra("photoId", tag.getId());

            MapsActivity.this.startActivity(intent);
        }

        if (tag.getType() == "user") {  // Then is photo marker
            Intent intent = new Intent(MapsActivity.this, ProfileActivity.class);
            intent.putExtra("username", tag.getId());
            MapsActivity.this.startActivity(intent);
        }

        return false;
    }

    private void clearPhotoMarkers() {
        for (int i = 0; i < visibleMarkers.size(); i++) {
            visibleMarkers.get(i).remove();
        }
    }

    private void clearFriendMarkers() {

        for (String key : mFriendsMarkersHashMap.keySet()) {
            mFriendsMarkersHashMap.get(key).remove();

        }
    }

  private void handleCategoryButtonClick(View view) {
    Button btn = (Button) view;
    String category = btn.getText().toString();

    if (selectedCategories.contains(category)) {
      selectedCategories.remove(category);
      btn.setBackgroundResource(android.R.drawable.btn_default);
    } else {
      selectedCategories.add(category);
      btn.setBackgroundColor(0xFF5993ff);
    }

    Toast.makeText(getApplicationContext(), btn.getText().toString(), Toast.LENGTH_LONG).show();
  }

  public void handleResetClick(View v)
  {
    selectedCategories.clear();
    EditText radius = (EditText) findViewById(R.id.editText4);
    radius.setText("5000");

    ArrayList<View> allButtons;
    allButtons = ((LinearLayout) findViewById(R.id.tagsScroll)).getTouchables();

    for (View btn : allButtons)
      btn.setBackgroundResource(android.R.drawable.btn_default);

    getMarkers(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 5000);  //5km
  }

  public void handleSubmitQuery(View v)
  {
    EditText radius = (EditText) findViewById(R.id.editText4);
    getMarkers(mLastLocation.getLatitude(), mLastLocation.getLongitude(), Integer.valueOf(radius.getText().toString()));
  }

    @Override
    protected void onPause() {
        super.onPause();

        runPollFriendsWasRunning = runPollFriends;
        stopLocationExchange(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (runPollFriendsWasRunning)
            startLocationExchange(false);
    }
}


class CustomMarkerTag {

  private final String type;
  private final String id;

  CustomMarkerTag(String type, String id) {
    this.type = type;
    this.id = id;
  }

  String getId() {
    return this.id;
  }

  String getType() {
    return this.type;
  }
}
