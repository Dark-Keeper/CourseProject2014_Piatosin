package com.AndroidCourse2014_Piatosin;

import android.graphics.Bitmap;

/**
 * Created by Dark Keeper on 17.05.14.
 */
public class Vacancy {


    public final String name;
    public final String published;
    public final String employer_name;
    public Bitmap employer_logo;
    public final int id;
    public final String area_name;

    public Vacancy(String name, String published, String employer_name, Bitmap employer_logo, int id,String area_name){
        this.name = name;
        this.published = published;
        this.employer_name = employer_name;
        this.employer_logo = employer_logo;
        this.id = id;
        this.area_name = area_name;
    }

    @Override
    public String toString() {
        return name + "\n"+area_name+"\n" + employer_name + "\n" + published;
    }
}
