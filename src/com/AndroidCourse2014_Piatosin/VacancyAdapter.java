package com.AndroidCourse2014_Piatosin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import org.json.JSONObject;

public class VacancyAdapter extends BaseAdapter {

    private ArrayList<Vacancy> data;
    private Context context;

    private String imageUrl = null;
    private Drawable d;

    public VacancyAdapter(ArrayList<Vacancy> data, Context context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {

            convertView = LayoutInflater.from(context).inflate(
                    R.layout.list_item, parent, false);


            TextView text = (TextView) convertView.findViewById(R.id.text_item);

            ImageView image = (ImageView) convertView.findViewById(R.id.image_item);

            ViewHolder vh = new ViewHolder(text, image);

            convertView.setTag(vh);

        }


        ViewHolder vh = (ViewHolder) convertView.getTag();

        vh.text.setText(data.get(position).toString());
     //   Log.d("DRAWABLE","URL = "+data.get(position).employer_logo);
        if (data.get(position).employer_logo!=null)
        {
            Log.d("Adapter","new employer_logo");
            d = new BitmapDrawable(context.getResources(),data.get(position).employer_logo);
            vh.image.setImageDrawable(d);
        }   else {
            Log.d("Adapter","default employer_logo");
            vh.image.setImageDrawable(context.getResources().getDrawable(R.drawable.default_user_image));
        }


        return convertView;
    }

    private class ViewHolder{
        public final TextView text;
        public final ImageView image;

        public ViewHolder (TextView text, ImageView image){
            this.text = text;
            this.image = image;
        }


    }


}
