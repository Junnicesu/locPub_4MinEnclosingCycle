package com.example.locpub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;

import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {
    TextView textSummary;
    Button buttonSave, buttonLoc;
    EditText editTextDescription;
    Location lastlocation = null;
    FusedLocationProviderClient mFusedLocationClient;
    private static final int LOCATION_PERMISSION_CODE = 100;
    String posturl = "http://developer.kensnz.com/api/addlocdata";

    boolean prefAutoUpload = false;
    int prefAutoUploadInterval = 5;
    String prefFullname = "";
    String prefUserid = "";
    String prefLocDescription = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Location Publisher");
        buttonSave = findViewById(R.id.buttonSave);
        buttonLoc = findViewById(R.id.buttonLoc);
        textSummary = findViewById(R.id.textSummary);
        editTextDescription = findViewById(R.id.editTextDescription);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefAutoUpload = prefs.getBoolean("auto_loc_record", false);
        if(prefAutoUpload) {
            String strInterval = prefs.getString("upload_interval", "5");
            String[] parts = strInterval.split(" ", 1);
            Toast.makeText(this, "strInterval: " + strInterval, Toast.LENGTH_LONG).show();
            prefAutoUploadInterval = Integer.parseInt(parts[0]);
        }
        prefFullname = prefs.getString("full_name", "");
        prefUserid = prefs.getString("userid", "");
        prefLocDescription = prefs.getString("default_description", "");
        Toast.makeText(this, "Name: " +  prefFullname, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Userid: " +  prefUserid, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Default description: " +  prefLocDescription, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "You can Modify the settings", Toast.LENGTH_LONG).show();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if(!isNetworkConnected()){
            Toast.makeText(this, "You need internet", Toast.LENGTH_LONG).show(); //sjdb
            new AlertDialog.Builder(this)
                    .setTitle("Internet Connectivity")
                    .setMessage("Don't you have internet?\nThe app is going to quit!")

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            System.exit(0);
                        }
                    })
                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        getLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, Settings.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void getLocation() {
        buttonSave.setEnabled(false);
        textSummary.setText("In permission checking...");

        boolean isFineLocAllowed = ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isCoarseLocAllowed = ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if(isFineLocAllowed || isCoarseLocAllowed) {
            textSummary.setText("Locating...");
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            textSummary.setText("On Success located...");
                            if (location != null) {
                                lastlocation = location;
                                buttonSave.setEnabled(true);
                                textSummary.setText(String.format("(%f,%f)",
                                        location.getLatitude(),
                                        location.getLongitude()));
                            }
                            else {
                                textSummary.setText("location value is null!!");
                            }
                        }
                    });
        }
        else {
            textSummary.setText("No permission for location");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textSummary.setText("request Permission");
                requestPermissions(new String[]{ ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
            }
        }
    }


    public void onClickButtonLocate(View view) {
        getLocation();
    }

    public void onClickButtonSave(View view) {
        if(prefUserid.isEmpty() || prefFullname.isEmpty()){
            Toast.makeText(this, "Please specify your userid and full name", Toast.LENGTH_LONG).show();
            return;
        }
        sendDataToWeb();
    }

    private void sendDataToWeb( ) {
        if (lastlocation == null) {
            textSummary.setText("last location is null");
            return;
        }
        try {
            final String description = editTextDescription.getText().toString();
            final String msgDescr =  description.isEmpty()?  prefLocDescription: description;
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
                    parameters.put("userid", prefUserid);
                    parameters.put("latitude",
                            Double.toString(lastlocation.getLatitude()));
                    parameters.put("longitude",
                            Double.toString(lastlocation.getLongitude()));
                    parameters.put("description", msgDescr);
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