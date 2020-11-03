package com.example.cocomelonalphabate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.lang.UCharacter;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.checkerframework.checker.units.UnitsTools.min;

public class History extends AppCompatActivity {
    BarChart barChart=null;
    SQLiteDatabase db;
    View inflatedView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = this.openOrCreateDatabase ("myDatabase", Context.MODE_PRIVATE, null);
        inflatedView = getLayoutInflater().inflate(R.layout.activity_history, null);
        LinearLayout historyView= (LinearLayout)  inflatedView.findViewById(R.id.history_view);
        GridLayout grid = (GridLayout) inflatedView.findViewById(R.id.grid_history);
        createGrid(grid);

        setContentView(historyView);
        barChart=(BarChart) findViewById(R.id.bar);
        createBarchart();
    }

    private void createBarchart(){
        List<BarEntry> entries = new ArrayList<>();
        final ArrayList<String> xAxisLabel = new ArrayList<>();
        for(int i=0;i<26;i++){
            String l=String.valueOf((char)('A'+i));
            Cursor c = db.rawQuery("SELECT * from BarChart where letter='"+l+"';", null);
            c.moveToFirst();
            int count=c.getInt(1);
            int time=c.getInt(2);
            //Log.v("mytag","Letter :"+l+" "+time);
            xAxisLabel.add(l);
            if(count!=0) entries.add(new BarEntry(i,time*1.0f/count));
            else entries.add(new BarEntry(i,0));
        }
        BarDataSet set = new BarDataSet(entries, "Required times");
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabel));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f); // set custom bar width
        barChart.setData(data);
        Description description=new Description();
        description.setText("Time in seconds");
        barChart.setFitBars(true);
        barChart.setDescription(description);
        barChart.invalidate(); // refresh
    }

    private void createGrid(GridLayout grid){
        for(int i=0;i<26;i++){
            LinearLayout ll=new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);

            TextView tv=new TextView(this);
            tv.setText(String.valueOf((char)('A'+i)));
            tv.setTextColor(Color.parseColor("#6200EE"));
            tv.setHeight(200);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setTextSize(20);
            //tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            ll.addView(tv);

            String l=String.valueOf((char)('A'+i));
            Cursor c = db.rawQuery("SELECT * from Photo where letter='"+l+"' ORDER BY date(date) DESC;", null);
            c.moveToFirst();
            //Log.v("mytag","Count"+c.getCount());
            for(int j=0;j<Math.min(3,c.getCount());j++){
                Log.v("mytag","letter :"+c.getString(1)+" label :"+c.getString(2)+" time: "+c.getInt(3)+" date: "+c.getString(4));
                LinearLayout ll2 = new LinearLayout(this);
                ll2.setOrientation(LinearLayout.VERTICAL);
                ImageView imageView1 = new ImageView(this);
                byte[] ba = c.getBlob(5);
                Bitmap imageBitmap = BitmapFactory.decodeByteArray(ba, 0, ba.length);
                //Bitmap scaled = Bitmap.createScaledBitmap(imageBitmap, 200, 200, false);
                imageView1.setImageBitmap(imageBitmap);
                ll2.addView(imageView1);

                TextView label = new TextView(this);
                label.setWidth(200);
                label.setText("Label: "+c.getString(2));
                ll2.addView(label);

                TextView time = new TextView(this);
                time.setWidth(200);
                time.setText("Time: "+c.getInt(3));
                ll2.addView(time);

                TextView takenAt = new TextView(this);
                takenAt.setWidth(200);
                //takenAt.setHeight(40);
                //String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                takenAt.setText("Taken At: "+c.getString(4));
                ll2.addView(takenAt);

                ll.addView(ll2);
                c.moveToNext();
            }
            /*
            for(int j=0;j<3;j++) {
                LinearLayout ll2 = new LinearLayout(this);
                ll2.setOrientation(LinearLayout.VERTICAL);
                ImageView imageView1 = new ImageView(this);
                Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cocmelon2);
                Bitmap scaled = Bitmap.createScaledBitmap(imageBitmap, 200, 200, false);
                imageView1.setImageBitmap(scaled);
                ll2.addView(imageView1);

                TextView label = new TextView(this);
                label.setText("Label: ");
                ll2.addView(label);

                TextView time = new TextView(this);
                time.setText("Time: ");
                ll2.addView(time);

                TextView takenAt = new TextView(this);
                takenAt.setWidth(200);
                //takenAt.setHeight(40);
                String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                takenAt.setText("Taken At: "+currentDateandTime);
                ll2.addView(takenAt);

                ll.addView(ll2);
            }
             */

            grid.addView(ll);
        }
    }
}