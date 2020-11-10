package com.example.cocomelonalphabate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mp;
    SQLiteDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(mp==null){
            mp = MediaPlayer.create(this,R.raw.dancing_baby);
            mp.setLooping(true);
            mp.start();
        }
        createDatabase();
        //printDB();
    }

    private void createDatabase(){
        db = this.openOrCreateDatabase ("myDatabase", Context.MODE_PRIVATE, null);
        //db.execSQL("drop table if exists BarChart;");
        db.execSQL("create table if not exists BarChart(letter text primary key,count number, time number);");
        try{
            for(int i=0;i<26;i++){
                ContentValues cv = new ContentValues();
                cv.put("letter", String.valueOf((char)('A'+i)));
                cv.put("count",0);
                cv.put("time",0);
                db.insert("BarChart",null,cv);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        //db.execSQL("drop table if exists Photo;");
        db.execSQL("create table if not exists Photo(id integer primary key autoincrement,letter text, label text, time number,date text, image blob);");
    }

    public void printDB(){
        Log.v("mytag","-----BarChart Table-----");
        Cursor c = db.rawQuery("SELECT * from BarChart;", null);
        Log.v("mytag","Total rows: "+c.getCount());
        c.moveToFirst();
        for(int i=0;i<c.getCount();i++){
            Log.v("mytag","letter :"+c.getString(0)+" time :"+c.getInt(1));
            c.moveToNext();
        }
    }

    public void startGame(View view) {
        Intent i=new Intent(this,GameActivity.class);
        startActivity(i);
    }

    public void startHistory(View view) {
        Intent i=new Intent(this,History.class);
        startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mp.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mp.start();
    }
}