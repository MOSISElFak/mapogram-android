package com.example.nutic.mapogram;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {

  private String url = "http://mapogram.dejan7.com/api/register";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_register);

    // Handle back button
    final Button backToLoginBtn = (Button) findViewById(R.id.backToLoginBtn);
    backToLoginBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Intent myIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        RegisterActivity.this.startActivity(myIntent);
      }
    });

    // Handle photo button
    final Button uploadPhotoBtn = (Button) findViewById(R.id.uploadPhotoBtn);
    uploadPhotoBtn.setOnClickListener(new View.OnClickListener() {
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
      final ImageView photoView = (ImageView) findViewById(R.id.photoView);
      photoView.setImageBitmap(imageBitmap);
    }
  }

  public void submitForm(View v) {

    EditText usernameTf = (EditText) findViewById(R.id.usernameTf);
    EditText firstNameTf = (EditText) findViewById(R.id.firstNameTf);
    EditText lastNameTf = (EditText) findViewById(R.id.lastNameTf);
    EditText emailTf = (EditText) findViewById(R.id.emailTf);
    EditText phoneTf = (EditText) findViewById(R.id.phoneNumberTf);
    EditText passwordTf = (EditText) findViewById(R.id.passwordTf);
    EditText passwordConfTf = (EditText) findViewById(R.id.passwordConfTf);
    ImageView photoView = (ImageView) findViewById(R.id.photoView);

    Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    byte[] byteArray = stream.toByteArray();
    String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

    if (usernameTf.getText().toString().trim().equals("")) {
      Toast.makeText(getApplicationContext(), "Username is required!", Toast.LENGTH_LONG).show();
    } else if (passwordTf.getText().toString().trim().equals("")) {
      Toast.makeText(getApplicationContext(), "Password is required!", Toast.LENGTH_LONG).show();
    } else {
      Map<String, String> params = new HashMap();

      params.put("username", usernameTf.getText().toString().trim());
      params.put("email", emailTf.getText().toString().trim());
      params.put("first_name", firstNameTf.getText().toString().trim());
      params.put("last_name", lastNameTf.getText().toString().trim());
      params.put("phone", phoneTf.getText().toString().trim());
      params.put("password", passwordTf.getText().toString().trim());
      params.put("password_confirmation", passwordConfTf.getText().toString().trim());
      params.put("img", "data:image/jpeg;base64," + base64);

      // Instantiate the RequestQueue.
      RequestQueue queue = Volley.newRequestQueue(this);

      // Request a string response from the provided URL.
      JsonObjectRequest jsObjRequest = new JsonObjectRequest
        (Request.Method.POST, url, new JSONObject(params), new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Toast.makeText(getApplicationContext(), "Account created, please login now", Toast.LENGTH_LONG).show();
            Intent myIntent = new Intent(RegisterActivity.this, LoginActivity.class);
            RegisterActivity.this.startActivity(myIntent);
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
          headers.put("Content-Type", "application/json");
          return headers;
        }
      };

      // Add the request to the RequestQueue.
      queue.add(jsObjRequest);
    }
  }
}
