package com.example.mongohack;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;


import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class AddHashtagActivity extends AppCompatActivity {

    EditText hashtagEditText;
    Button submitButton, dateButton, currentLocationButton;
    ImageButton backButton;

    TextView dateEditText, locationEditText;
    private int mYear, mMonth, mDay;
    private Date tillDate;
    public double lat=0.0;
    public double lng=0.0;

    private StitchAppClient client;
    public static RemoteMongoCollection topics;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_hashtag);

        getSupportActionBar().hide();

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Add a new Local Buzz");
        backButton = findViewById(R.id.back_button);

        hashtagEditText = findViewById(R.id.topicNameEditText);
        dateEditText = findViewById(R.id.dateEditText);
        locationEditText = findViewById(R.id.locationEditText);

        dateButton = findViewById(R.id.dateButton);
        currentLocationButton = findViewById(R.id.currentLocationButton);
        submitButton = findViewById(R.id.submitButton);

        hashtagEditText.setText( "" );

        client= Stitch.getDefaultAppClient();
        topics = HomeFragment.topics;

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(AddHashtagActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                dateEditText.setText(String.valueOf(dayOfMonth) + "-" + String.valueOf(monthOfYear + 1) + "-" + String.valueOf(year));
                                Calendar c1 = GregorianCalendar.getInstance();
                                c1.set(year, monthOfYear, dayOfMonth+1);
                                tillDate = c1.getTime();
                                Log.d("TAG",tillDate.toString());
                            }
                        }, mYear, mMonth, mDay);

                datePickerDialog.show();

            }
        });

        requestPermission();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        currentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ActivityCompat.checkSelfPermission(AddHashtagActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED )  {
                    Log.d("SUC", "Login done.");
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(AddHashtagActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {

                                if (location != null) {

                                    lng = location.getLongitude();
                                    lat = location.getLatitude();
                                    Toast.makeText(getApplicationContext(), "Lat-" + lat + " & Lng-"+lng, Toast.LENGTH_SHORT).show();
                                    locationEditText.setText("Current Location");
                                }
                            }
                        });
            }
        });

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME));
        autocompleteFragment.setHint("Custom Location");

        //autocompleteFragment.getView().setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_rectangle_white));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.

                lat = place.getLatLng().latitude;
                lng = place.getLatLng().longitude;
                Toast.makeText(getApplicationContext(), "Lat-" + lat + " & Lng-"+lng, Toast.LENGTH_SHORT).show();
                locationEditText.setText(place.getName());
//                Toast.makeText(getApplicationContext(), place.getLatLng().toString(), Toast.LENGTH_SHORT).show();
//                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("LOCERR", "An error occurred: " + status);
            }
        });


        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newHashtag = hashtagEditText.getText().toString();
                Log.d("hashtag name",newHashtag);
                if(newHashtag.isEmpty()){
                    hashtagEditText.setError("Buzz Name is empty");
                    hashtagEditText.requestFocus();
                    return;
                }
                if(newHashtag.contains(" ")){
                    hashtagEditText.setError("Buzz Name should not contain any spaces");
                    hashtagEditText.requestFocus();
                    return;
                }
                if(dateEditText.getText().toString().isEmpty()){
                    dateEditText.setError("Date is not selected");
                    dateEditText.requestFocus();
                    return;
                }
                if(locationEditText.getText().toString().isEmpty()){
                    locationEditText.setError("Location is not selected");
                    locationEditText.requestFocus();
                    return;
                }

                Document d = new Document()
                        .append("topic_name",newHashtag)
                        .append(
                                "location",
                                new Document().append(
                                        "type","Point"
                                ).append(
                                "coordinates",
                                        new ArrayList<Double>( Arrays.asList(lng,lat) )
                                )
                        )
                        .append("created_at",new Date())
                        .append("active_till_date",tillDate)
                        .append("user_name", client.getAuth().getUser().getProfile().getName())
                        .append("owner_id",new ObjectId(client.getAuth().getUser().getId()))
                        .append("topic_count", 0);

                Log.d("DOC", d.toString());
                Task findtask = topics.sync().insertOne(d);

                findtask.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful())
                        {
                            Log.d("hashtag adding TAG", task.getResult().toString());
                            Toast.makeText(AddHashtagActivity.this, "Buzz added", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else{
                            Toast.makeText(AddHashtagActivity.this, "Buzz could not be added", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), UserActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
    }
}