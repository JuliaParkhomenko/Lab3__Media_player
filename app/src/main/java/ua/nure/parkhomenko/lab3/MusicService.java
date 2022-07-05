package ua.nure.parkhomenko.lab3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final IBinder musicBind = new MusicBinder();
    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songsList;
    //current position
    private int songPosition;
    private String songTitle="";
    private String songAuthor="";
    private static final int NOTIFY_ID=1;

    private boolean shuffle=false;
    private Random rand;
    private boolean repeat=false;

    //Used to pause/resume MediaPlayer
    private int resumePosition;

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    NotificationManagerCompat notificationManagerCompat;
    Notification notification;


    public void onCreate(){
        //create the service
        super.onCreate();
        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //initialize position
        songPosition=0;
        //create player
        player = new MediaPlayer();
        rand=new Random();
        initMusicPlayer();
    }

    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> songs){
        songsList = songs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    // when user exits the app -> we stop the service
    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //if(player.getCurrentPosition()==0){
            mp.reset();
            playNext();
        //}
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //start playback
        mediaPlayer.start();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*Notification.Builder builder = new Notification.Builder(this);
*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("songChannel", "MyChannel", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "songChannel");
        builder.setContentIntent(pendInt)
        .setSmallIcon(R.drawable.music)
                .setContentTitle(songTitle)
                .setContentText(songAuthor)
                .setTicker(songTitle)
                .setOngoing(true);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);

        //work but i need something different
        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("songChannel", "MyChannel", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "songChannel")
                .setSmallIcon(R.drawable.music)
                .setContentTitle(songTitle)
                .setContentText(songAuthor);

        notification = builder.build();

        notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFY_ID, notification);*/
    }

    public void playSong(){
        // reset MeduaPlayer since we will also use this code
        // when the user is playing subsequent songs (типу коли будемо перемикатися на наступну пісню)
        player.reset();

        //get song
        Song playSong = songsList.get(songPosition);
        songTitle=playSong.getTitle();
        songAuthor=playSong.getArtist();
        //get id
        long currSong = playSong.getId();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);

        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        //calling the asynchronous method of the MediaPlayer to prepare it
        player.prepareAsync();
    }

    public void setSong(int songIndex){
        songPosition=songIndex;
    }

    public int getPosition(){
        return player.getCurrentPosition();
    }

    public int getDuration(){
        return player.getDuration();
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        if (player.isPlaying()) {
            player.pause();
            resumePosition = player.getCurrentPosition();
            player.seekTo(resumePosition);
        }
    }

    private void resumePlayer() {
        if (!player.isPlaying()) {
            player.seekTo(resumePosition);
            player.start();
        }
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }
    public void playPrev(){
        songPosition--;
        if(songPosition<0) songPosition=songsList.size()-1;
        playSong();
    }

    //skip to next
    public void playNext(){
        if(shuffle){
            int newSong = songPosition;
            while(newSong==songPosition){
                newSong=rand.nextInt(songsList.size());
            }
            songPosition=newSong;
        }
        else if(!repeat){
            songPosition++;
            if(songPosition==songsList.size()) songPosition=0;
        }

        playSong();
    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    public void setRepeat(){
        if(repeat) repeat=false;
        else repeat=true;
    }

    public boolean isOnRepeat(){
        return repeat;
    }

    public boolean isOnShuffle(){
        return shuffle;
    }

    /**
     * Handle PhoneState changes
     */
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (player != null) {
                            pausePlayer();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (player != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumePlayer();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }
}
