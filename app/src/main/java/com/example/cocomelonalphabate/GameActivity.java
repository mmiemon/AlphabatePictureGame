package com.example.cocomelonalphabate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends AppCompatActivity implements SensorEventListener {
    Intent music_intent;
    MediaPlayer mp=null;
    SQLiteDatabase db;
    int alphabateRecording[]=new int[27];
    Button currentButton;
    TextView currentTextView;
    ArrayList<Integer> remaining;
    int current;
    int currentIndex;
    View inflatedView;
    int startTime;
    Bitmap scaled;
    ObjectAnimator scaleDown;
    SensorManager sm=null;
    Sensor s;
    float totalAccelerometer=0;
    int numAccelerometer=0;
    float maxAccelerometer=0;

    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private  int imageSizeX;
    private  int imageSizeY;
    private TensorBuffer outputProbabilityBuffer;
    private TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        remaining=new ArrayList<Integer>();
        for(int i=0;i<26;i++) remaining.add(i);
        db = this.openOrCreateDatabase ("myDatabase", Context.MODE_PRIVATE, null);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        inflatedView = getLayoutInflater().inflate(R.layout.activity_game, null);
        LinearLayout gameView= (LinearLayout)  inflatedView.findViewById(R.id.game_view);
        GridLayout grid = (GridLayout) inflatedView.findViewById(R.id.grid);
        createGrid(grid);

        selectRandomButton(0);
        startTimer();

        initTensorflow();
        setContentView(gameView);
    }

    private void startTimer(){
        startTime = 0;
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
                                  @Override
                                  public void run() {
                                      runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {
                                              TextView tv = (TextView) findViewById(R.id.set_time);
                                              tv.setText(startTime+"s");
                                              startTime++;
                                          }
                                      });
                                  }

                              },
                0,
                1000);
    }

    private void createGrid(GridLayout grid){
        for(int i=0;i<26;i++){
            LinearLayout ll=new LinearLayout(this);
            ll.setOrientation(LinearLayout.VERTICAL);
            Button btn=new Button(this);
            btn.setWidth(200);
            btn.setHeight(200);
            btn.setId(i);
            btn.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              Intent w=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                              startActivityForResult(w,1);
                                          }
                                      });
            btn.setClickable(false);

            ImageView iv=new ImageView(this);
            iv.setId(i+100);

            TextView tv=new TextView(this);
            tv.setText(String.valueOf((char)('A'+i)));
            tv.setTextColor(Color.parseColor("#6200EE"));
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv.setId(1000+i);

            ll.addView(btn);
            ll.addView(iv);
            ll.addView(tv);
            grid.addView(ll);
        }
    }

    private void selectRandomButton(int x1){
        if(remaining.size()==0) {
            showCongratulations();
            return;
        }
        Random random=new Random();
        if (x1==0) currentIndex = 1;
        else currentIndex=random.nextInt(remaining.size());
        current=remaining.get(currentIndex);
        currentButton=(Button) inflatedView.findViewById(current);
        currentTextView = (TextView) inflatedView.findViewById(1000+current);
        currentButton.setClickable(true);
        currentButton.setBackgroundColor(Color.parseColor("#94E78D"));

        scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                currentButton,
                PropertyValuesHolder.ofFloat("scaleX", 0.5f),
                PropertyValuesHolder.ofFloat("scaleY", 0.5f)
        );
        scaleDown.setDuration(1000);
        scaleDown.setRepeatCount(ValueAnimator.INFINITE);
        scaleDown.setRepeatMode(ValueAnimator.REVERSE);
        scaleDown.start();

        String recording_file = Character.toString((char)('A'+current)).toLowerCase();
        int x=getResources().getIdentifier(recording_file, "raw", getPackageName());
        sm.registerListener(this, s, 2000000);
        totalAccelerometer=0;
        numAccelerometer=0;
        maxAccelerometer=0;
        mp = MediaPlayer.create(this,x);
        mp.start();
    }

    private void showCongratulations(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations !!!");
        builder.setMessage("You have won.");
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void takeSnap() {
        Intent w=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(w,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("Lab1", "resultCode: " + resultCode+"requestCode: "+requestCode);
        if(requestCode==1 && resultCode==RESULT_OK){
            Bundle extras=data.getExtras();
            Bitmap imageBitmap=(Bitmap) extras.get("data");
            scaled=Bitmap.createScaledBitmap(imageBitmap,180,180,false);

            inputImageBuffer = loadImage(imageBitmap);
            tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());
            showresult();
        }
    }

    private void initTensorflow(){
        try{
            tflite=new Interpreter(loadmodelfile(this));
        }catch (Exception e) {
            e.printStackTrace();
        }
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        int probabilityTensorIndex = 0;
        int[] probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();
    }

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private MappedByteBuffer loadmodelfile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("mobilenet_v1_1.0_224_quant.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private void showresult(){
        try{
            labels = FileUtil.loadLabels(this,"labels_mobilenet_quant_v1_224.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        //float maxValueInMap =(Collections.max(labeledProbability.values()));

        List<Map.Entry<String, Float> > list =
                new LinkedList<Map.Entry<String, Float> >(labeledProbability.entrySet());
        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Float> >() {
            public int compare(Map.Entry<String, Float> o1,
                               Map.Entry<String, Float> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        String detection="Detected object: ";
        if(detectFake(list)){
            detection+="Virtual object";
        }
        else {
            for (int i = 0; i < 3; i++) {
                if (list.get(i).getValue() > 0) {
                    Log.v("mytag", "key :" + list.get(i).getKey() + " Probability :" + list.get(i).getValue());
                    //Log.v("mytag","1"+list.get(i).getKey().toUpperCase().charAt(0)+"2"+(char)('A'+current));
                    if (list.get(i).getKey().toUpperCase().charAt(0) == (char) ('A' + current)) {
                        //Log.v("mytag","removed"+remaining.get(currentIndex));
                        //Database update
                        //Toast.makeText(getApplicationContext(),list.get(i).getKey(),Toast.LENGTH_SHORT).show();
                        TextView tv = (TextView) findViewById(R.id.detected_object);
                        tv.setText("Detected object: " + list.get(i).getKey());
                        String l = String.valueOf((char) ('A' + current));
                        db.execSQL("update BarChart set count=count+1 where letter='" + l + "';");
                        db.execSQL("update BarChart set time=time+" + String.valueOf(startTime) + " where letter='" + l + "';");

                        ContentValues cv = new ContentValues();
                        cv.put("letter", l);
                        cv.put("label", list.get(i).getKey());
                        cv.put("time", startTime);
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        cv.put("date", date);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, bos);
                        byte[] bArray = bos.toByteArray();
                        cv.put("image", bArray);
                        db.insert("Photo", null, cv);

                        remaining.remove(currentIndex);
                        mp = MediaPlayer.create(this, R.raw.applause);
                        mp.start();
                        scaleDown.cancel();
                        ((ViewGroup) currentButton.getParent()).removeView(currentButton);
                        ImageView iv = (ImageView) inflatedView.findViewById(current + 100);
                        iv.setImageBitmap(scaled);

                        ObjectAnimator animation = ObjectAnimator.ofFloat(iv, "rotationY", 0.0f, 360f);
                        animation.setDuration(3000);
                        //animation.setRepeatCount(ObjectAnimator.INFINITE);
                        animation.setInterpolator(new AccelerateDecelerateInterpolator());
                        animation.start();

                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                selectRandomButton(1);
                                                startTime = 0;
                                            }
                                        });
                                    }
                                },
                                3000
                        );
                        return;
                    }
                    if (i < 2) detection += list.get(i).getKey() + ", ";
                    else detection += list.get(i).getKey();
                }
            }
        }

        TextView tv=(TextView) findViewById(R.id.detected_object);
        tv.setText(detection);
        mp = MediaPlayer.create(this,R.raw.wrong_sound);
        mp.start();
        scaleDown.start();
    }

    // I used two checking methods for fake detection. First, whether top ten tag contains the name screen,television,laptop,computer,monitor. Second, whether
    // the maximum accelerometer value exceed a threshold(100).
    private boolean detectFake(List<Map.Entry<String, Float> > list){
        String[] vir={"screen","television","laptop","computer","monitor"};
        for(int i=0;i<10;i++){
            if(list.get(i).getValue()>0){
                Log.v("mytag", "key :" + list.get(i).getKey() + " Probability :" + list.get(i).getValue());
                for(int j=0;j<vir.length;j++){
                    if(list.get(i).getKey().toLowerCase().contains(vir[j])) return true;
                }
            }
        }
        Log.v("mytag","T :"+totalAccelerometer+" N :"+numAccelerometer+ " M: "+maxAccelerometer);
        if(maxAccelerometer < 100) return true;
        return false;
    }

    public void printDB(){
        Log.v("mytag","-----BarChart Table-----");
        Cursor c = db.rawQuery("SELECT * from BarChart;", null);
        Log.v("mytag","Total rows: "+c.getCount());
        c.moveToFirst();
        for(int i=0;i<c.getCount();i++){
            Log.v("mytag","letter :"+c.getString(0)+" count: "+c.getInt(1)+" time :"+c.getInt(2));
            c.moveToNext();
        }
    }

    public void exitGame(View view) {
        Intent i=new Intent(this,MainActivity.class);
        startActivity(i);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float z=sensorEvent.values[0]*sensorEvent.values[0]+sensorEvent.values[1]*sensorEvent.values[1]+sensorEvent.values[2]*sensorEvent.values[2];
        Log.d("sensor","x :"+sensorEvent.values[0]+" y :"+sensorEvent.values[1]+" z :"+sensorEvent.values[2]+" z :"+z);
        numAccelerometer++;
        totalAccelerometer+=z;
        if(z>maxAccelerometer) maxAccelerometer=z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}