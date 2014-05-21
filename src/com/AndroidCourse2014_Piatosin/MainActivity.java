package com.AndroidCourse2014_Piatosin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.AndroidCourse2014_Piatosin.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends ActionBarActivity implements LocationListener {
    private static final String DEBUG_TAG = "DEBUGING";
    /**
     * Called when the activity is first created.
     */

    private String searchText = null;
    private String searchAreaText = null;

    private AbsListView list;
    private BaseAdapter adapter;
    static JSONObject jObj = null;
    private ArrayList<Vacancy> vacanciesArrayList;

    public String[] imageUrlArray;
    public Bitmap employer_logo_bitmap = null;


    StringBuilder builder = new StringBuilder();
    JSONObject jsonResponse = null;

    // JSON Node names
    public static final String TAG_VACANCIES = "items";
    public static final String TAG_NAME = "name";
    public static final String TAG_PUBLISHED = "published_at";
    public static final String TAG_EMPLOYER = "employer";
    public static final String TAG_EMPLOYER_NAME = "name";
    public static final String TAG_EMPLOYER_LOGO = "logo_urls";
    public static final String TAG_EMPLOYER_LOGO_ORIGINAL = "original";
    public static final String TAG_EMPLOYER_LOGO_240 = "240";
    public static final String TAG_ID = "id";
    public static final String TAG_AREA = "area";

    // contacts JSONArray
    JSONArray vacancies = null;

    private LocationManager lm;
    private double lat = 0;
    private double lng = 0;
    private String cityName = null;
    private SharedPreferences prefs;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        lat = prefs.getLong("lat",0);
        lng = prefs.getLong("lng",0);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setTitle("Home");
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        LocationManager locationManager = (LocationManager)
        getSystemService(Context.LOCATION_SERVICE);


        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000,10,this);

        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (locationNet!=null){
             lat = locationNet.getLatitude();
             lng = locationNet.getLongitude();
        }
        if (lat!=0){

            Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = gcd.getFromLocation(lat, lng, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addresses!=null){
                if (addresses.size() > 0)
                {
                    cityName=addresses.get(0).getLocality();
                    Log.d(DEBUG_TAG,"COUNTRYNAME = "+cityName);
                    EditText editText2 = (EditText) findViewById(R.id.textField2);
                    editText2.setHint(cityName);

                }
            }
        }

        list = (AbsListView)findViewById(R.id.list);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(MainActivity.this, DetailActivity.class);

                Vacancy vacancy = (Vacancy) adapter.getItem(position);

                intent.putExtra(TAG_NAME, vacancy.name);
                intent.putExtra("area "+TAG_NAME,vacancy.area_name);
                intent.putExtra(TAG_PUBLISHED, vacancy.published);
                intent.putExtra("employer "+TAG_EMPLOYER_NAME, vacancy.employer_name);
                intent.putExtra(TAG_EMPLOYER_LOGO, vacancy.employer_logo);
                intent.putExtra(TAG_ID,vacancy.id);

                startActivity(intent);
            }
        });

    }

    public void searchButtonAction(View v){
        Log.d(DEBUG_TAG,"clicked");

        EditText editText1 = (EditText)findViewById(R.id.textField1);
        searchText = editText1.getText().toString();
        EditText editText2 = (EditText) findViewById(R.id.textField2);
        searchAreaText = editText2.getText().toString();

        Log.d(DEBUG_TAG,"searchText = "+searchText);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadJsonTask().execute();
        } else {
            createNetErrorDialog();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();
        Log.d(DEBUG_TAG,"Current Coordinates = "+lat+" "+lng);

        prefs.edit().putLong("lat",(long)lat).commit();
        prefs.edit().putLong("lng",(long)lng).commit();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class DownloadJsonTask extends AsyncTask<Void, Void, JSONObject>{

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return downloadJson();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            parseJsonObject(result);
        }
    }

    private JSONObject downloadJson() throws IOException{
        InputStream is = null;

        try {
            URL url = new URL("https://api.hh.ru/vacancies");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();
          //  conn.disconnect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while((line = reader.readLine()) != null){
                builder.append(line);

            }

            try {
                jsonResponse = new JSONObject(builder.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jsonResponse;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
    private String convertToLocalTime(String str){
        String dtStart = "2010-10-15T09:27:37+0400";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            Log.d(DEBUG_TAG,"myDate0 = "+str);
            format.setTimeZone(TimeZone.getTimeZone("GMT+4"));
            Date myDate = format.parse(str);
            Log.d(DEBUG_TAG,"myDate = "+myDate);

            format.setTimeZone(TimeZone.getDefault());
            str = format.format(myDate);
            Log.d(DEBUG_TAG,"str = "+str);

            //    Date date = format.parse(dtStart);
            // str=date.toString();

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
/*        String subStr1 = str.substring(0,10);
        String subStr2 = str.substring(11,19);
        String subStr3 = str.substring(20,24);
        Log.d(DEBUG_TAG," subStr1 = "+subStr1+" subStr2 = "+subStr2+" subStr3 = "+subStr3);*/
        return str;
    }

    private void parseJsonObject(JSONObject jObj){
        try {
            vacancies = jObj.getJSONArray(TAG_VACANCIES);
            Log.d(DEBUG_TAG,"got vacancies");
            vacanciesArrayList = new ArrayList<Vacancy>();
            imageUrlArray = new String[vacancies.length()];

            for(int i = 0; i < vacancies.length(); i++){
                JSONObject c = vacancies.getJSONObject(i);
                Log.d(DEBUG_TAG,"vacancy "+i);

                String name = c.getString(TAG_NAME);
                String published = c.getString(TAG_PUBLISHED);
                published = convertToLocalTime(published);
                int id = c.getInt(TAG_ID);

                JSONObject area = c.getJSONObject(TAG_AREA);
                String area_name = area.getString(TAG_NAME);

                JSONObject employer = c.getJSONObject(TAG_EMPLOYER);
                String employer_name = employer.getString(TAG_EMPLOYER_NAME);

                employer_logo_bitmap = null;
             //   imageUrlArray[i] = new String();
                imageUrlArray[i]=null;

                if (employer.getString(TAG_EMPLOYER_LOGO)!="null"&& employer.getString(TAG_EMPLOYER_LOGO)!=null){
                    JSONObject employer_image = employer.getJSONObject(TAG_EMPLOYER_LOGO);
                    if (employer_image.getString(TAG_EMPLOYER_LOGO_240)!=null){
                        String employer_logo = employer_image.getString(TAG_EMPLOYER_LOGO_240);
                        imageUrlArray[i] = employer_logo;
                    } else {
                        String employer_logo = employer_image.getString(TAG_EMPLOYER_LOGO_ORIGINAL);
                        imageUrlArray[i] = employer_logo;
                    }
                 //   employer_logo_bitmap = getBitmapFromURL(employer_logo);
                }


                Vacancy vacancy = new Vacancy(name,published,employer_name,employer_logo_bitmap,id,area_name);
                if (name.toLowerCase().contains(searchText.toLowerCase())&&area_name.toLowerCase().contains(searchAreaText.toLowerCase())){
                    vacanciesArrayList.add(vacancy);
                }
            }

            adapter = new VacancyAdapter(vacanciesArrayList, getApplicationContext());

            list.setAdapter(adapter);


            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new downloadImageFromUrlTask().execute();
            } else {
                createNetErrorDialog();
            }

        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
        catch (NullPointerException e){

        }
    }



    private class downloadImageFromUrlTask extends AsyncTask<Void, Void, Bitmap[]>{

        @Override
        protected Bitmap[] doInBackground(Void... params) {
            try {
                return getBitmapFromURL(imageUrlArray);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap[] result) {
          for (int i=0;i<vacanciesArrayList.size();i++){
              vacanciesArrayList.get(i).employer_logo = result[i];
              Log.d(DEBUG_TAG,"new employer logo = "+vacanciesArrayList.get(i).employer_logo);
          }
            Log.d(DEBUG_TAG,"new adapter");
            adapter = new VacancyAdapter(vacanciesArrayList, getApplicationContext());

            list.setAdapter(adapter);
            if (vacanciesArrayList.size()==0){
                noResults();
            }
        }
    }

    public Bitmap[] getBitmapFromURL(String[] src) throws IOException {
        InputStream is = null;
        try {
            Bitmap[] myBitmap = new Bitmap[src.length];
            for (int i=0;i < src.length; i++){
                if (src[i]!=null){
                    URL url = new URL(src[i]);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setDoInput(true);
                    connection.connect();
                    is = connection.getInputStream();
                    myBitmap[i] = BitmapFactory.decodeStream(is);
                    Log.d(DEBUG_TAG,"myBitmap["+i+"] = " +myBitmap[i]);
                }
            }
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            connectionLost();
            return null;
        }  finally {
            if (is != null) {
                is.close();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {
            case R.id.main_browser:
                goToWebPage();
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    public void goToWebPage(){
        String url = "http://hh.ru/search/vacancy";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void createNetErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You need a network connection to use this application. Please turn on mobile network or Wi-Fi in Settings.")
                .setTitle("Unable to connect")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                    Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                    startActivity(i);
                                } else {
                                    Intent i = new Intent(Settings.ACTION_SETTINGS);
                                    startActivity(i);
                                }
                            }
                        }
                )
                .setNegativeButton("Cancel", null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void noResults() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Try to use another combination.")
                .setTitle("No Results")
                .setNegativeButton("Ok", null);;
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void connectionLost() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Check your internet connection or try again later.")
                .setTitle("Connection Lost")
                .setCancelable(false)
                .setPositiveButton("Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                    Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                    startActivity(i);
                                } else {
                                    Intent i = new Intent(Settings.ACTION_SETTINGS);
                                    startActivity(i);
                                }
                            }
                        }
                )
                .setNegativeButton("Cancel", null);
        AlertDialog alert = builder.create();
        alert.show();
    }


}
