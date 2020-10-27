package com.example.locpub;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;

public class MainActivity extends AppCompatActivity {
    TextView textSummary;
    Button buttonSave;
    EditText editTextDescription;
    Location lastlocation = null;
    FusedLocationProviderClient mFusedLocationClient;
    String posturl = "http://developer.kensnz.com/api/addlocdata";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonSave = findViewById(R.id.buttonSave);
        textSummary = findViewById(R.id.textSummary);
        editTextDescription = findViewById(R.id.editTextDescription);
        mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        getLocation();
    }

    private void getLocation() {
        buttonSave.setEnabled(false);
        textSummary.setText("In getting location...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            lastlocation = location;
                            buttonSave.setEnabled(true);
                            textSummary.setText(String.format("(%f,%f)",
                                    location.getLatitude(),
                                    location.getLongitude()));
                        }
                    }
                });
    }

    public void onClickButtonSave(View view) {
        getLocation();
        sendDataToWeb();
    }

    private void sendDataToWeb( ) {
        if (lastlocation == null) return;
        try {
            final String description = editTextDescription.getText().toString();
            StringRequest request
                    = new StringRequest(Request.Method.POST, posturl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String s) {
                            textSummary.setText(s);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            textSummary.setText(MessageFormat.format("Error sending to web service\n{0}", volleyError.getMessage()));
                        }
                    })
            {
                @Override
                protected Map<String, String> getParams()
                        throws AuthFailureError {
                    Map<String, String> parameters
                            = new HashMap<String, String>();
                    parameters.put("userid", "1");
                    parameters.put("latitude",
                            Double.toString(lastlocation.getLatitude()));
                    parameters.put("longitude",
                            Double.toString(lastlocation.getLongitude()));
                    parameters.put("description", description);
                    return parameters;
                }
            };
            RequestQueue rQueue = Volley.newRequestQueue(MainActivity.this);
            rQueue.add(request);
        }
        catch (SecurityException eS) {
            textSummary.setText(eS.getMessage());
        }
    }
}