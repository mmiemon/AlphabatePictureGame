package com.example.cocomelonalphabate;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class BackgroundMusic extends Service{
    private final String ACTION_PLAY = "com.example.cocomelonalphabate.PLAY";
    MediaPlayer mp=null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals(ACTION_PLAY)){
            mp = MediaPlayer.create(this,R.raw.dancing_baby);
            mp.setLooping(true);
            mp.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

}
