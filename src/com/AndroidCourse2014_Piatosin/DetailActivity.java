package com.AndroidCourse2014_Piatosin;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Dark Keeper on 18.05.14.
 */
public class DetailActivity extends ActionBarActivity {

    private static final String DEBUG_TAG = "DetailActivity Debuging";
    private int id;
    StringBuilder builder = new StringBuilder();
    JSONObject jsonResponse = null;
    private String description = null;
    private TextView textViewDescription;

    private static final String TAG_DESCRIPTION = "description";

    JSONArray vacancy = null;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Home");
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        TextView textView = (TextView) findViewById(R.id.desc_text_item);
        ImageView imageView = (ImageView) findViewById(R.id.desc_image_item);
        textViewDescription = (TextView) findViewById(R.id.desc_description);



        Intent startIntent = getIntent();

        id = startIntent.getExtras().getInt(MainActivity.TAG_ID);


        textView.setText(startIntent.getExtras().getString(MainActivity.TAG_NAME)+"\n"
                +startIntent.getExtras().getString("area "+MainActivity.TAG_NAME)+"\n"
                +startIntent.getExtras().getString("employer "+MainActivity.TAG_EMPLOYER_NAME)+"\n"
                +startIntent.getExtras().getString(MainActivity.TAG_PUBLISHED));

        if (startIntent.getExtras().get(MainActivity.TAG_EMPLOYER_LOGO)!=null)
        {
            Drawable d = new BitmapDrawable(getResources(),(Bitmap)startIntent.getExtras().get(MainActivity.TAG_EMPLOYER_LOGO));
            imageView.setImageDrawable(d);
        }   else {
            Log.d("Adapter","default employer_logo");
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.default_user_image));
        }

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadJsonTask().execute();
        } else {
            createNetErrorDialog();
        }






       // textViewDescription.setText(startIntent.getExtras().getString(MainActivity.TAG_DESCRIPTION));
    }
    public void goToWebPage(){
        String url = "http://www.hh.ru/vacancy/"+id;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private class DownloadJsonTask extends AsyncTask<Void, Void, JSONObject> {

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
            URL url = new URL("https://api.hh.ru/vacancies/"+id);
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

    private void parseJsonObject(JSONObject jObj){
        try {
            description = jObj.getString(TAG_DESCRIPTION);

            String tmpHtml = "<html>"+description+"</html>";
            String htmlTextStr = Html.fromHtml(tmpHtml).toString();

            textViewDescription.setText(htmlTextStr);

        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
        catch (NullPointerException e){

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {

            case android.R.id.home:
                //   super.onBackPressed();
                Intent intent3 = new Intent(DetailActivity.this,MainActivity.class);
                startActivity(intent3);
                return true;
            case R.id.detail_browser:
                goToWebPage();
                return true;
            case R.id.detail_back:
                onBackPressed();
                return true;
        }


        return super.onOptionsItemSelected(item);
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

}