/* Cédric KI & Cédric Le Logeais
* Projet Android
* IMR3
* */

/* Fait par Cédric KI & Cédric LE LOGEAIS
* IMR3 2018-2019
* Projet Android
* */
package com.example.kilelogeais.miniprojet;


import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;


import java.util.List;

import com.example.kilelogeais.miniprojet.*;




public class MainActivity extends AppCompatActivity {


    private static final String VIDEO_SAMPLE =
            "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";
    private static final String TAG = "MainActivity";
    private final String PROGRESS_WEB_VIEW = "Progress web view";

    private VideoView mVideoView;
    private TextView mBufferingTextView;
    private int mCurrentPosition = 0;
    private WebView webview;

    //private int position = 0;
    private ProgressDialog progressDialog;
    private MediaController mediaControls;

    private Handler mHandler;
    private Thread mThread;

    private String currentWebViewTitle = "Intro";
    private String Url = "https://en.wikipedia.org/wiki/Big_Buck_Bunny";
    private MetadataManager metadataManager;
    private static final String PLAYBACK_TIME = "play_time";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = findViewById(R.id.videoview);
        mBufferingTextView = findViewById(R.id.buffering_textview);

        if (savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getInt(PLAYBACK_TIME);
        }

        MediaController controller = new MediaController(this);
        controller.setMediaPlayer(mVideoView);
        mVideoView.setMediaController(controller);


        //Remove the notification bar (full screen)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //MetadataManager
        metadataManager = new MetadataManager();
        metadataManager.add(0, "Big Buck", "");
        metadataManager.add(28, "Plot", "Plot");
        metadataManager.add(2 * 60 + 40, "Production", "Production_history");
        metadataManager.add(4 * 60 + 50, "Release", "Release");
        metadataManager.add(60 + 15, "Characters", "Characters");
        metadataManager.add(8 * 60 + 15, "See also", "See_also");


        // Create a progressbar
        progressDialog = new ProgressDialog(MainActivity.this);
        // Set progressbar title
        //progressDialog.setTitle("Load bar");
        // Set progressbar message
        //progressDialog.setMessage("Loading...");

        progressDialog.setCancelable(false);
        // Show progressbar
        //progressDialog.show();

        //WebView
        webview = findViewById(R.id.web_view);
        webview.setWebViewClient(new WebViewClient());
        webview.loadUrl("https://en.wikipedia.org/wiki/Big_Buck_Bunny");

        //Button
        Button buttonBigBuck = findViewById(R.id.buttonBigBuck);
        Button buttonPlot = findViewById(R.id.buttonPlot);
        Button buttonProduction = findViewById(R.id.buttonProduction);
        Button buttonRelease = findViewById(R.id.buttonRelease);
        Button buttonCharacters = findViewById(R.id.buttonCharacters);
        Button buttonSeeAlso = findViewById(R.id.buttonSeeAlso);

        buttonBigBuck.setTag("Big Buck");
        buttonPlot.setTag("Plot");
        buttonProduction.setTag("Production");
        buttonRelease.setTag("Release");
        buttonCharacters.setTag("Characters");
        buttonSeeAlso.setTag("See also");
        buttonBigBuck.setOnClickListener(myOnlyHandler);
        buttonPlot.setOnClickListener(myOnlyHandler);
        buttonProduction.setOnClickListener(myOnlyHandler);
        buttonRelease.setOnClickListener(myOnlyHandler);
        buttonCharacters.setOnClickListener(myOnlyHandler);
        buttonSeeAlso.setOnClickListener(myOnlyHandler);


        //Handler
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String url = msg.getData().getString(PROGRESS_WEB_VIEW);
                Log.d(TAG, "Message received :" + url);
                webview.loadUrl(url);
            }
        };
    }



    private Uri getMedia (String mediaName){
        if (URLUtil.isValidUrl(mediaName)) {
            return Uri.parse(mediaName);
        } else {
            return Uri.parse("android.resource://" + getPackageName() +
                    "/raw/" + mediaName);
        }
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        //Interrupt the current thread (or else, multiple messages are going to be sent to the handler)
        mThread.interrupt();
        //Save the current video position in order to restore
        savedInstanceState.putInt("Position", mVideoView.getCurrentPosition());
        //Pause the video (why not)
        mVideoView.suspend();
    }
    /**
     * In this function, we work on the handler (thread + send message)
     * The handler will (normaly) receive a bundle message
     * The message will be treated in the handleMessage(msg) function
     */


    private void initializePlayer() {

        mBufferingTextView.setVisibility(VideoView.VISIBLE);

        Uri videoUri = getMedia(VIDEO_SAMPLE);
        mVideoView.setVideoURI(videoUri);
        mVideoView.setOnPreparedListener(
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {

                        mBufferingTextView.setVisibility(VideoView.INVISIBLE);

                        if (mCurrentPosition > 0) {
                            mVideoView.seekTo(mCurrentPosition);
                        } else {

                            mVideoView.seekTo(1);
                        }

                        mVideoView.start();
                    }
                });

        mVideoView.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Toast.makeText(MainActivity.this,
                                R.string.toast_message,
                                Toast.LENGTH_SHORT).show();

                        mVideoView.seekTo(0);
                    }
                });
    }

    public void onStart() {
        Log.d(TAG, "OnStart called");
        super.onStart();
        initializePlayer();
        mThread = new Thread(new Runnable() {
            //Le Bundle qui porte les données du Message et sera transmis au Handler
            Bundle messageBundle = new Bundle();
            //Le message échangé entre la Thread et le Handler
            Message myMessage;

            @Override
            public void run() {
                try {
                    while (true) {
                        if (mVideoView.isPlaying()) {
                            Log.d(TAG, "Running, current position : " + mVideoView.getCurrentPosition());
                            Log.d(TAG, "currentWebViewTitle : " + currentWebViewTitle);
                            Log.d(TAG, "metadataManager.getContextByPosition(mVideoView.getCurrentPosition()/1000)) : " + metadataManager.getContextByPosition(mVideoView.getCurrentPosition() / 1000));

                            if (!currentWebViewTitle.equals(metadataManager.getContextByPosition(mVideoView.getCurrentPosition() / 1000))) {

                                currentWebViewTitle = metadataManager.getContextByPosition(mVideoView.getCurrentPosition() / 1000);
                                // Envoyer le message au Handler (la méthode handler.obtainMessage est plus efficace
                                // que créer un message à partir de rien, optimisation du pool de message du Handler)
                                //Instanciation du message (la bonne méthode):
                                myMessage = mHandler.obtainMessage();
                                //Ajouter des données à transmettre au Handler via le Bundle
                                messageBundle.putString(PROGRESS_WEB_VIEW, metadataManager.getUrlByPosition(mVideoView.getCurrentPosition() / 1000));
                                //Ajouter le Bundle au message
                                myMessage.setData(messageBundle);
                                //Envoyer le message
                                mHandler.sendMessage(myMessage);
                            }


                        }
                        //Let other threads to work
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        mThread.start();
        //mVideoView.start();

        }


        @Override
        protected void onPause () {
            super.onPause();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                mVideoView.pause();
            }
        }


        @Override
        protected void onStop () {
            super.onStop();
            releasePlayer();
        }



        private View.OnClickListener myOnlyHandler = new View.OnClickListener() {
            public void onClick(View v) {
                //Just a test
                Log.d(TAG, "Button on clicked, button tag: " + v.getTag());
                Log.d(TAG, "Button on clicked, video goes to : " + metadataManager.getPositionByContext(v.getTag().toString()));
                Log.d(TAG,"Button on clicked, video currentDuration : "+mVideoView.getCurrentPosition());
                try {
                    mVideoView.seekTo(metadataManager.getPositionByContext(v.getTag().toString())*1000);
                } catch (Exception e) {
                    Log.d(TAG, "Exception " + e);
                }

            }
        };

         private void releasePlayer() {
            mVideoView.stopPlayback();
        }

        @Override
        public void onRestoreInstanceState (Bundle savedInstanceState){
            super.onRestoreInstanceState(savedInstanceState);
            mCurrentPosition = savedInstanceState.getInt("Position");
            mVideoView.seekTo(mCurrentPosition);
            mVideoView.isPlaying();
        }
    }

