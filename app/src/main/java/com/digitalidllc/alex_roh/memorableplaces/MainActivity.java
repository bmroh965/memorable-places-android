package com.digitalidllc.alex_roh.memorableplaces;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.sax.StartElementListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ListView placesLV;
    private ArrayList<FavoritePlace> placesList;
    private ArrayAdapter<FavoritePlace> arrayAdapter;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int mapRequestCode = 1;

    static class FavoritePlace {
        private Location mPlace;
        private String mAddress;

        @Override
        public String toString() {
            return mAddress;
        }

        public FavoritePlace(@NonNull Location place, @NonNull String address) {
            this.mPlace = place;
            this.mAddress = address;
        }

        public Location getLocation() {
            return mPlace;
        }

        public String getAddress() {
            return mAddress;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up location manager+listener
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("Current Location", location.toString());
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                try {
                    List<Address> addressesList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    if (addressesList != null && addressesList.size() > 0) {
                        StringBuilder address = new StringBuilder();

                        if (addressesList.get(0).getThoroughfare() != null) {
                            address.append(addressesList.get(0).getThoroughfare() + " ");
                        }

                        if (addressesList.get(0).getLocality() != null) {
                            address.append(addressesList.get(0).getLocality() + "\n");
                        }

                        if (addressesList.get(0).getPostalCode() != null) {
                            address.append(addressesList.get(0).getPostalCode() + " ");
                        }

                        if (addressesList.get(0).getAdminArea() != null) {
                            address.append(addressesList.get(0).getAdminArea());
                        }

                       Log.i("Address: ", address.toString());

                        updateCurrentLocation(location, address.toString());
                    } else {
                        Log.e("Address Gathering", "Failed");
                        updateCurrentLocation(location, "Address Unknown");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationListen();
        }
        //set up list
        setUpListView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationListen();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == mapRequestCode && resultCode==RESULT_OK){
            if(data==null) Log.e("Data is empty!!!!","no!!!!");
            ArrayList<String>  latitudes =  data.getStringArrayListExtra("latitudes");
            ArrayList<String>  longitudes =  data.getStringArrayListExtra("longitudes");
            ArrayList<String> addresses = data.getStringArrayListExtra("addresses");

            //add to sharedprefs
            SharedPreferences sharedPreferences = this.getSharedPreferences("com.digitalidllc.alex_roh.sharedpreferences", Context.MODE_PRIVATE);
            try {
                sharedPreferences.edit().putString("latitudes",ObjectSerializer.serialize(latitudes)).apply(); Log.i("new latitudes",ObjectSerializer.serialize(latitudes));
                sharedPreferences.edit().putString("longitudes",ObjectSerializer.serialize(longitudes)).apply(); Log.i("new longitudes",ObjectSerializer.serialize(longitudes));
                sharedPreferences.edit().putString("addresses",ObjectSerializer.serialize(addresses)).apply(); Log.i("new addresses",ObjectSerializer.serialize(addresses));
            } catch (IOException e) {
                e.printStackTrace();
            }

            //update local list
            for(int i=0; i<latitudes.size();++i){
                Location newPlace = new Location(LocationManager.GPS_PROVIDER);
                newPlace.setLatitude(Double.parseDouble(latitudes.get(i)));
                newPlace.setLongitude(Double.parseDouble(longitudes.get(i)));
                Log.i("New Place Added", newPlace.toString());
                FavoritePlace favoritePlace = new FavoritePlace(newPlace, addresses.get(i));

                placesList.add(favoritePlace);
            }

            refreshList();
        }
    }

    private void setUpListView() {
        placesList = new ArrayList<>();

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

        //add current location as first element
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        FavoritePlace currentPlace = new FavoritePlace(lastKnownLocation, "Find a place...");
        placesList.add(currentPlace);

        //retrieve data from sharedprefs
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.digitalidllc.alex_roh.sharedpreferences", Context.MODE_PRIVATE);
        try{
        ArrayList<String> storedLatitudes = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("latitudes", ObjectSerializer.serialize(new ArrayList<String>()))); Log.i("stored latitudes", storedLatitudes.toString());
        ArrayList<String> storedLongitudes = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("longitudes", ObjectSerializer.serialize(new ArrayList<String>()))); Log.i("stored longitudes", storedLongitudes.toString());
        ArrayList<String> storedAddresses = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("addresses", ObjectSerializer.serialize(new ArrayList<String>()))); Log.i("stored addresses", storedAddresses.toString());

            //update local list
            for(int i=0; i<storedLatitudes.size();++i){
                Location storedPlace = new Location(LocationManager.GPS_PROVIDER);
                storedPlace.setLatitude(Double.parseDouble(storedLatitudes.get(i)));
                storedPlace.setLongitude(Double.parseDouble(storedLongitudes.get(i)));
                Log.i("Stored Place Added", storedPlace.toString());
                FavoritePlace favoritePlace = new FavoritePlace(storedPlace, storedAddresses.get(i));

                placesList.add(favoritePlace);
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        //set up adapter
        placesLV = findViewById(R.id.placesLV);
        arrayAdapter = new ArrayAdapter<FavoritePlace>(this, android.R.layout.simple_list_item_1,placesList);
        placesLV.setAdapter(arrayAdapter);

        //set up onclick
        placesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                double latitude = placesList.get(i).getLocation().getLatitude();
                double longitude = placesList.get(i).getLocation().getLongitude();
                String address = placesList.get(i).getAddress();

                Intent mapIntent = new Intent(getApplicationContext(), MapsActivity.class);
                mapIntent.putExtra("latitude", latitude);
                mapIntent.putExtra("longitude", longitude);
                mapIntent.putExtra("address", address);

                startActivityForResult(mapIntent, mapRequestCode);
            }
        });


    }

    private void startLocationListen(){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 5, locationListener);
            }
    }

    private void updateCurrentLocation(Location location, String address){
        FavoritePlace currentPlace = new FavoritePlace(location, address);
        placesList.set(0,currentPlace);

        refreshList();
    }

    private void refreshList(){
        arrayAdapter.setNotifyOnChange(true);
        arrayAdapter.notifyDataSetChanged();
    }

}
