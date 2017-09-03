package com.example.nutic.mapogram;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

  private String url = "http://mapogram.dejan7.com/api/users";
    Double latitude;
    Double longitude;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile);

      Intent intent = getIntent();

      latitude = intent.getDoubleExtra("latitude", 43.321349);
      longitude = intent.getDoubleExtra("longitude", 21.895758);

    loadProfile();
  }


  private void loadProfile() {
    Intent intent = getIntent();
    String username = intent.getStringExtra("username");

    url = url + "/" + username;

    JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {

        ((TextView) findViewById(R.id.textViewUsername)).setText(response.optString("username"));
        ((TextView) findViewById(R.id.textViewEmail)).setText(response.optString("email"));
        ((TextView) findViewById(R.id.textViewFirstName)).setText(response.optString("first_name") == "null" || response.optString("first_name") == null ? "" : response.optString("first_name"));
        ((TextView) findViewById(R.id.textViewLastName)).setText(response.optString("last_name") == "null" || response.optString("last_name") == null ? "" : response.optString("last_name"));
        ((TextView) findViewById(R.id.textViewPhone)).setText(response.optString("phone_number") == "null" || response.optString("phone_number") == null ? "" : response.optString("phone_number"));

        DownloadImageTask task = new DownloadImageTask((ImageView) findViewById(R.id.imageView));

        if (response.optString("avatar") == null || response.optString("avatar") == "null")
          task.execute("https://www.android.com/static/2016/img/aife/homepage/history/2010_1x.jpg");
        else
          task.execute(response.optString("avatar"));

        loadPhotos();

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
    });

    RequestQueue queue = Volley.newRequestQueue(this);
    queue.add(jsObjRequest);
  }

  private void loadPhotos() {
      url = url + "/photos";
      JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {
                  JSONArray photos = response.optJSONArray("photos");
                  final LinearLayout layout = (LinearLayout) findViewById(R.id.profileStream);

                  for (int i = 0; i < photos.length(); i++) {
                      try {
                          JSONObject photo = photos.getJSONObject(i);

                          View imageView = getLayoutInflater().inflate(R.layout.image, null);
                          layout.addView(imageView);

                          final ImageView imageViewGrid = (ImageView) imageView.findViewById(R.id.imageViewGrid);
                          final String imageID = photo.optString("id");
                              DownloadImageTask task = new DownloadImageTask(imageViewGrid);
                              task.execute( photo.optString("url"));


                          imageViewGrid.setOnClickListener(new View.OnClickListener() {
                              @Override
                              public void onClick(View view) {
                                  Intent intent = new Intent(ProfileActivity.this, PhotoActivity.class);
                                  intent.putExtra("latitude", latitude);
                                  intent.putExtra("longitude", longitude);
                                  intent.putExtra("photoId", imageID);
                                  ProfileActivity.this.startActivity(intent);
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
                  //SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                  //headers.put("Authorization", "Bearer " + settings.getString("token", null));
                  return headers;
              }
          };

          RequestQueue queue = Volley.newRequestQueue(this);
          queue.add(jsObjRequest);

  }


}

class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
  ImageView bmImage;

  public DownloadImageTask(ImageView bmImage) {
    this.bmImage = bmImage;
  }

  protected Bitmap doInBackground(String... urls) {
    String urldisplay = urls[0];
    Bitmap mIcon11 = null;
    try {
      InputStream in = new java.net.URL(urldisplay).openStream();
      mIcon11 = BitmapFactory.decodeStream(in);
    } catch (Exception e) {
      Log.e("Error", e.getMessage());
      e.printStackTrace();
    }
    return mIcon11;
  }

  protected void onPostExecute(Bitmap result) {
    bmImage.setImageBitmap(result);
  }
}



class ImageAdapter extends BaseAdapter {
  private Context mContext;
  private ArrayList<String> mThumbIds = new ArrayList<String>();

  public ImageAdapter(Context c, JSONArray photos) {
    mContext = c;

    for (int i = 0; i < photos.length(); i++) {
      JSONObject row = null;
      try {
        row = photos.getJSONObject(i);
        mThumbIds.add(row.getString("url"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  public int getCount() {
    return mThumbIds.size();
  }

  public Object getItem(int position) {
    return null;
  }

  public long getItemId(int position) {
    return 0;
  }

  // create a new ImageView for each item referenced by the Adapter
  public View getView(int position, View convertView, ViewGroup parent) {
    ImageView imageView;
     if (convertView == null) {
      // if it's not recycled, initialize some attributes
      imageView = new ImageView(mContext);
      imageView.setLayoutParams(new GridView.LayoutParams(320, 240));
      imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
      imageView.setPadding(2, 2, 2, 2);
    } else {
      imageView = (ImageView) convertView;
    }

    DownloadImageTask task = new DownloadImageTask(imageView);
    task.execute(mThumbIds.get(position));

    return imageView;
  }
}
