package com.example.nutic.mapogram;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddPhotoActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

  private String url = "http://mapogram.dejan7.com/api/photos";
  public static final String PREFS_NAME = "MapogramPrefs";
  Double latitude;
  Double longitude;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_photo);

    Intent intent = getIntent();
    latitude = intent.getDoubleExtra("latitude", 43.321349);
    longitude = intent.getDoubleExtra("longitude", 21.895758);

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    // Handle photo button
    final Button takePhotoBtn = (Button) findViewById(R.id.takePhotoBtn);
    takePhotoBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivityForResult(intent, 0);
      }
    });

  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 0 && resultCode == RESULT_OK) {
      Bundle extras = data.getExtras();
      Bitmap imageBitmap = (Bitmap) extras.get("data");
      final ImageView imageView2 = (ImageView) findViewById(R.id.imageView2);
      imageView2.setImageBitmap(imageBitmap);
    }
  }

  public void submitForm(View v)
  {
    EditText description   = (EditText) findViewById(R.id.descriptionTextView);
    EditText hashtags   = (EditText) findViewById(R.id.hashtagsTextEdit);
    ImageView imageView2 = (ImageView) findViewById(R.id.imageView2);

    Bitmap bitmap = ((BitmapDrawable)imageView2.getDrawable()).getBitmap();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    byte[] byteArray = stream.toByteArray();
    String base64 = Base64.encodeToString(byteArray , Base64.DEFAULT);

    Map<String, String> params = new HashMap();

    params.put("description",    description.getText().toString().trim());
    params.put("categories",    hashtags.getText().toString().trim());
    params.put("img", "data:image/jpeg;base64," + base64);
    params.put("lng", longitude.toString());
    params.put("lat", latitude.toString());

    // Request a string response from the provided URL.
    JsonObjectRequest jsObjRequest = new JsonObjectRequest
      (Request.Method.POST, url, new JSONObject(params), new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
          Toast.makeText(getApplicationContext(), "Photo added!", Toast.LENGTH_LONG).show();
          Intent myIntent = new Intent(AddPhotoActivity.this, MapsActivity.class);
          AddPhotoActivity.this.startActivity(myIntent);
        }
      }, new Response.ErrorListener() {

        @Override
        public void onErrorResponse(VolleyError error) {
          String message = "";
          try {
            JSONObject errResponse = new JSONObject(new String(error.networkResponse.data));
            message = errResponse.getString("error");
          } catch (Exception e) {
            e.printStackTrace();
          }
          Toast.makeText(getApplicationContext(), "Error: " + error.toString() , Toast.LENGTH_LONG).show();
        }
      }) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        headers.put("Authorization", "Bearer " + settings.getString("token", null));
        return headers;
      }
    };

    // Add the request to the RequestQueue.
    RequestQueue queue = Volley.newRequestQueue(this);
    queue.add(jsObjRequest);
  }


  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int id = item.getItemId();

    switch (id) {
      case R.id.map: {
        Intent intent = new Intent(AddPhotoActivity.this, MapsActivity.class);
        AddPhotoActivity.this.startActivity(intent);
      }
      case R.id.nav_add_friend: {
        Intent intent = new Intent(AddPhotoActivity.this, MapsActivity.class);
        AddPhotoActivity.this.startActivity(intent);
        return true;
      }
      case R.id.myprofile: {
        Intent intent = new Intent(AddPhotoActivity.this, ProfileActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        intent.putExtra("username", settings.getString("username", null));
        AddPhotoActivity.this.startActivity(intent);
        return true;
      }
      case R.id.nav_add_photo: {
        Intent intent = new Intent(AddPhotoActivity.this, AddPhotoActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        AddPhotoActivity.this.startActivity(intent);
        return true;
      }
      case R.id.nav_top_list: {
        Intent intent = new Intent(AddPhotoActivity.this, TopListActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        AddPhotoActivity.this.startActivity(intent);
        return true;
      }
      default: {
        return false;
      }
    }
  }

}
