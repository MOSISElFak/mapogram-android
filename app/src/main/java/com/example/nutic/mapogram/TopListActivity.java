package com.example.nutic.mapogram;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TopListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

  private String url = "http://mapogram.dejan7.com/api/users/toplist";
  Double latitude;
  Double longitude;
  public static final String PREFS_NAME = "MapogramPrefs";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_top_list);

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    Intent intent = getIntent();
    latitude = intent.getDoubleExtra("latitude", 43.321349);
    longitude = intent.getDoubleExtra("longitude", 21.895758);

    //loadToplist();
      try {
          loadPeople();
      } catch (JSONException e) {
          e.printStackTrace();
      }
  }

    private void loadPeople() throws JSONException {
        JsonArrayRequest jsObjRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                final LinearLayout layout = (LinearLayout) findViewById(R.id.topListPeople);

                for (int i = 0; i < response.length(); i++) {
                    JSONObject comment = null;
                    try {
                        JSONObject user = response.getJSONObject(i);

                        View commentView = getLayoutInflater().inflate(R.layout.comment, null);
                        layout.addView(commentView);

                        final ImageView commentPhoto = (ImageView) commentView.findViewById(R.id.commentPhoto);
                        final TextView commentText = (TextView) commentView.findViewById(R.id.commentText);
                        final TextView commentAuthorText = (TextView) commentView.findViewById(R.id.commentAuthor);
                        if (!(user.optString("avatar") == null ||  user.optString("avatar") == "null")) {
                            DownloadImageTask task = new DownloadImageTask(commentPhoto);
                            task.execute("http://mapogram.dejan7.com/avatars/" + user.optString("avatar"));
                        }
                        final String usernameS = user.getString("username");
                        commentText.setText(user.optString("total_likes") + " total likes");
                        commentAuthorText.setText(String.valueOf(i+1) + ". " + user.getString("username") + ":");
                        Log.e("avatar", user.optString("total_likes"));
                        commentPhoto.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(TopListActivity.this, ProfileActivity.class);
                                intent.putExtra("latitude", latitude);
                                intent.putExtra("longitude", longitude);
                                intent.putExtra("username", usernameS);
                                TopListActivity.this.startActivity(intent);
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


  private void loadToplist() {
    JsonArrayRequest jsObjRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
      @Override
      public void onResponse(JSONArray response) {
        ListView list = (ListView) findViewById(R.id.mListView);
        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);

        // Here, you set the data in your ListView
        list.setAdapter(adapter);

        for (int i = 0; i < response.length(); i++) {
          try {
            JSONObject user = new JSONObject(response.get(i).toString());

            String listItem = user.getString("username") + " (";
            listItem += user.getString("first_name") != "null" ? user.getString("first_name") : "-";
            listItem += user.getString("last_name") != "null" ? user.getString("last_name") : "-";
            listItem += ") \t\t\t\t\t";
            listItem += user.getString("total_likes") + " likes";
            arrayList.add(listItem);

          } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error - Parsing", Toast.LENGTH_LONG).show();
          }
        }
        adapter.notifyDataSetChanged();
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
        Toast.makeText(getApplicationContext(), "Error: " + error.toString(), Toast.LENGTH_LONG).show();
      }
    });

    RequestQueue queue = Volley.newRequestQueue(this);
    queue.add(jsObjRequest);
  }



  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int id = item.getItemId();

    switch (id) {
        case R.id.map: {
            Intent intent = new Intent(TopListActivity.this, MapsActivity.class);
            TopListActivity.this.startActivity(intent);
        }
      case R.id.nav_add_friend: {
        Intent alreadyLoggedInIntent = new Intent(TopListActivity.this, MapsActivity.class);
        TopListActivity.this.startActivity(alreadyLoggedInIntent);
        return true;
      }
      /*case R.id.nav_show_users: {
        Intent alreadyLoggedInIntent = new Intent(TopListActivity.this, MapsActivity.class);
        TopListActivity.this.startActivity(alreadyLoggedInIntent);
        return true;
      }*/
      case R.id.myprofile: {
        Intent intent = new Intent(TopListActivity.this, ProfileActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        intent.putExtra("username", settings.getString("username", null));
        TopListActivity.this.startActivity(intent);
        return true;
      }
      case R.id.nav_add_photo: {
        Intent intent = new Intent(TopListActivity.this, AddPhotoActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        TopListActivity.this.startActivity(intent);
        return true;
      }
      case R.id.nav_top_list: {
        Intent intent = new Intent(TopListActivity.this, TopListActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        TopListActivity.this.startActivity(intent);
        return true;
      }
      default: {
        return false;
      }
    }
  }

}
