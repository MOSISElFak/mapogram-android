package com.example.nutic.mapogram;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

  private String url = "http://mapogram.dejan7.com/api/login";
  public static final String PREFS_NAME = "MapogramPrefs";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Restore preferences
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    String jwtToken = settings.getString("token", null);

    if (jwtToken != null) {
      redirectAfterLogin();
    }


    // Handle register button
    final Button registerButton = (Button) findViewById(R.id.registerBtn);
    registerButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Toast.makeText(getApplicationContext(), "Please create account by filling up form!", Toast.LENGTH_LONG).show();
        Intent myIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        LoginActivity.this.startActivity(myIntent);
      }
    });
  }

  public void submitForm(View v) {
    EditText usernameTf = (EditText) findViewById(R.id.usernameTf);
    EditText passwordTf = (EditText) findViewById(R.id.passwordTf);

    if (usernameTf.getText().toString().trim().equals("")) {
      Toast.makeText(getApplicationContext(), "Username is required!", Toast.LENGTH_LONG).show();
    }
    else if (passwordTf.getText().toString().trim().equals("")) {
      Toast.makeText(getApplicationContext(), "Password is required!", Toast.LENGTH_LONG).show();
    }
    else {
      Map<String, String> params = new HashMap();

      params.put("username", usernameTf.getText().toString().trim());
      params.put("password", passwordTf.getText().toString().trim());

      final String username = usernameTf.getText().toString().trim();
      // Instantiate the RequestQueue.
      RequestQueue queue = Volley.newRequestQueue(this);

      // Request a string response from the provided URL.
      JsonObjectRequest jsObjRequest = new JsonObjectRequest
        (Request.Method.POST, url, new JSONObject(params), new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            String token = "";
            try {
              token = response.getString("token");
              final  String usernameS = response.getString("username");
              final String id = response.getString("id");
                final String avatar;
                if (response.getString("avatar") == null || response.getString("avatar") == "null")
                    avatar = "null";
                else
                    avatar = response.getString("avatar");
              // Save token in preferences for long login
              SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
              SharedPreferences.Editor editor = settings.edit();

              editor.putString("token", token);
              editor.putString("username", usernameS);
              editor.putString("id", id);
              editor.putString("avatar", avatar);

            // Commit the edits!
              editor.apply();
            } catch (JSONException e) {
              e.printStackTrace();
            }


            redirectAfterLogin();
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

      // Add the request to the RequestQueue.
      queue.add(jsObjRequest);
    }
  }

  private void redirectAfterLogin() {

    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    String username = settings.getString("username", null);


      Intent alreadyLoggedInIntent = new Intent(LoginActivity.this, MapsActivity.class);
//    Intent alreadyLoggedInIntent = new Intent(LoginActivity.this, ProfileActivity.class);
//    Intent alreadyLoggedInIntent = new Intent(LoginActivity.this, AddPhotoActivity.class);
//    Intent alreadyLoggedInIntent = new Intent(LoginActivity.this, TopListActivity.class);
      //Intent alreadyLoggedInIntent = new Intent(LoginActivity.this, PhotoActivity.class);

     //alreadyLoggedInIntent.putExtra("photoId", "1");

      LoginActivity.this.startActivity(alreadyLoggedInIntent);
  }
}
