package com.example.mohamedabdelaziz.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String source , Destination ;
    LatLng mylocation=null ;
    ArrayList<Route> arrayList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
         SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().isZoomControlsEnabled();
        mMap.getUiSettings().isMyLocationButtonEnabled();
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                if(mylocation!=null) {
                    source=mylocation.latitude+","+mylocation.longitude ;
                    Destination=latLng.latitude+","+latLng.longitude ;
                    new getroutePoints().execute();
                }
                else
                    Toast.makeText(MapsActivity.this, "check your current location", Toast.LENGTH_SHORT).show();
            }
        });
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {

                mylocation=getmylocation();

                return true;
            }
        });
    }

    LatLng getmylocation()
    {
        try {
            Location mylocation = mMap.getMyLocation();
            LatLng latLng = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            mMap.addMarker(new MarkerOptions().position(latLng)).showInfoWindow();
            return latLng;
        }catch (Exception e)
        {}
     return null ;
    }
    public void Button_getroute(View view) throws UnsupportedEncodingException {
        EditText editText1=(EditText) findViewById(R.id.source) ;
        EditText editText2=(EditText) findViewById(R.id.destination) ;
        source=URLEncoder.encode(editText1.getText().toString(),"utf-8");
        Destination= URLEncoder.encode(editText2.getText().toString(),"utf-8");
        if(!source.isEmpty() && ! Destination.isEmpty())
            new getroutePoints().execute() ;
        else
            Toast.makeText(this, "No Data Entered", Toast.LENGTH_SHORT).show();

    }
    class getroutePoints extends AsyncTask<Void,Void,Void>
    {

        @Override
        protected void onPreExecute() {
        arrayList=new ArrayList<>() ;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InputStream IStream = null;
            BufferedReader BReader = null;
            StringBuffer SBuffer = new StringBuffer();
            String line, data;
            HttpsURLConnection httpsURLConnection = null;


            URL url = null;
            try {
                url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=" + source + "&destination=" + Destination + "&AIzaSyAcpRCgcRtOljQxeUn4_LYRjy1pAC10EBQ");
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setRequestMethod("GET");
                httpsURLConnection.connect();
                IStream = httpsURLConnection.getInputStream();
                BReader = new BufferedReader(new InputStreamReader(IStream));
                while ((line = BReader.readLine()) != null)
                    SBuffer.append(line + "\n");
                data = SBuffer.toString();

                JSONObject jsonObject = new JSONObject(data);
                JSONArray jsonArray = jsonObject.getJSONArray("routes");



                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    Route route = new Route();

                    JSONObject overview_polylineJson = jsonObject1.getJSONObject("overview_polyline");
                    JSONArray jsonLegs = jsonObject1.getJSONArray("legs");
                    JSONObject jsonLeg = jsonLegs.getJSONObject(0);
                    JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
                    JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
                    JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
                    JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");

                    route.distance = new Distance(jsonDistance.getString("text"));
                    route.duration = new Duration(jsonDuration.getString("text"));
                    route.endAddress = jsonLeg.getString("end_address");
                    route.startAddress = jsonLeg.getString("start_address");
                    route.startLocation = new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng"));
                    route.endLocation = new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng"));
                    route.points = decodePolyLine(overview_polylineJson.getString("points"));
                    arrayList.add(route);
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            mMap.clear();
            PolylineOptions polylineOptions = new PolylineOptions();
            try {

                polylineOptions.addAll(arrayList.get(0).points);
                polylineOptions
                        .width(10)
                        .color(Color.GREEN);
                mMap.addPolyline(polylineOptions);
                TextView textView= (TextView) findViewById(R.id.details);
                textView.setText(arrayList.get(0).duration.text + "," + "" + arrayList.get(0).distance.text);
                mMap.addMarker(new MarkerOptions().position(arrayList.get(0).endLocation).title("" + (arrayList.get(0).endAddress)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(arrayList.get(0).endLocation, 10));
                mMap.addMarker(new MarkerOptions().position(arrayList.get(0).startLocation).title(("" + arrayList.get(0).startAddress))).showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(arrayList.get(0).startLocation, 10));
            } catch (Exception e) {
                try {
                    Toast.makeText(MapsActivity.this, "An Error Happened", Toast.LENGTH_SHORT).show();
                }catch (Exception ee)
                {}
            }
        }
    }

    private List<LatLng> decodePolyLine(String points) {

        int len = points.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = points.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = points.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }
        return decoded;

    }
}
