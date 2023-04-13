package com.example.bsweatherapp;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.data.SingleRefDataBufferIterator;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout RLWeather;
    private ProgressBar loadingPB;
    private TextView topLocationName;
//    private TextView LocationName;
    private TextView TemperatureTV;
    private TextView ConditionTV;
    private TextInputEditText LocationName;
    private RecyclerView RVWeather;
    private ImageView backgroundImage,IconImage,searchImage;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private int PERMISSION_CODE = 1;

    private String locationCityName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        RLWeather = findViewById(R.id.rlHome);
        loadingPB = findViewById(R.id.idPBLoading);
        LocationName = findViewById(R.id.EditTILocation);
        topLocationName = findViewById(R.id.idTVcityName);
        TemperatureTV = findViewById(R.id.temperature);
        ConditionTV = findViewById(R.id.weatherCondition);
        RVWeather = findViewById(R.id.RVWeather);
        backgroundImage = findViewById(R.id.backgroundImage);
        IconImage = findViewById(R.id.tempIcon);
        searchImage = findViewById(R.id.searchIcon);
        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, this.weatherRVModelArrayList);
        RVWeather.setAdapter(weatherRVAdapter);
        RVWeather.setLayoutManager(new GridLayoutManager(this));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        }
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if(location!=null) {
            locationCityName = getLocationName(location.getLongitude(),location.getLatitude());
        }else {
            locationCityName = getLocationName(44.3894, 79.6903);
        }

        getWeatherInfo(locationCityName);

        searchImage.setOnClickListener(new View.OnClickListener() {
            @NonNull
            @Override
           public  void onClick(View v) {
                String city = LocationName.getText().toString();

                if(city.isEmpty()) {
                    Toast.makeText(MainActivity.this,"Please Enter City Name",Toast.LENGTH_SHORT).show();
                }else {
                    topLocationName.setText(city);
                    getWeatherInfo(city);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode ==PERMISSION_CODE) {
            if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,"Permissions Granted!",Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Please Provide the Permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getLocationName(double longitude, double latitude) {
        String LocationName = locationCityName;
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            try {
                List<Address> addresses = gcd.getFromLocation(latitude,longitude,10);

                for(Address adr : addresses) {
                    if(adr!=null) {
                        String city = adr.getLocality();

                        if(city!=null && !city.equals("")) {
                           LocationName = city;
                        }else {
                            Log.d("TAG","City Not Found");
                            Toast.makeText(this,"User City Not Found..",Toast.LENGTH_SHORT).show();
                        }

                    }
                }

            }catch (IOException e) {
                e.printStackTrace();
            }
        return LocationName;
    }


    private void getWeatherInfo(String LocationName) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=c63d504b71bd425ca7785718231304&q="+LocationName+"&days=10&aqi=yes&alerts=yes";
        topLocationName.setText(LocationName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                RLWeather.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();
                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    TemperatureTV.setText(temperature.concat("°c"));
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(conditionIcon)).into(IconImage);
                    ConditionTV.setText(condition);

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);

                    JSONArray hourArray = forecast0.getJSONArray("hour");

                    for(int i=0; i<hourArray.length(); i++){
                            JSONObject hourObj = hourArray.getJSONObject(i);
                            String time = hourObj.getString("time");
                            String temp = hourObj.getString("temp_c");
                            String wind = hourObj.getString("wind_kph");
                            String img = hourObj.getJSONObject("condition").getString("icon");
                            weatherRVModelArrayList.add(new WeatherRVModel(time,temp,img,wind));
                    }

                    weatherRVAdapter.notifyDataSetChanged();



                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please Enter Valid City Name...", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);

    }
}