package com.google.android.gms.samples.vision.ocrreader.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.android.gms.samples.vision.ocrreader.R;
import com.google.android.gms.samples.vision.ocrreader.utils.Configuration;
import com.google.android.gms.samples.vision.ocrreader.utils.QueryUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.nuance.speechkit.Audio;
import com.nuance.speechkit.AudioPlayer;
import com.nuance.speechkit.Language;
import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StartActivity extends AppCompatActivity implements AudioPlayer.Listener {
    private static final int READ_REQUEST_CODE = 42;
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final int SPEECH_REQUEST_FILE_CODE = 145;
    private static final int SPEECH_REQUEST_FILE_TRANSLATED_CODE = 144;

    @BindView(R.id.navigation)
    NavigationView mNavigationView;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.bt_media)
    Button mStartButton;

    @BindView(R.id.rb_rating_bar)
    RatingBar mRatingBar;
    String mUid;
    private TextToSpeech tts;
    private Toast mToast;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // The AUDIOFOCUS_LOSS_TRANSIENT case means that we've lost audio focus for a
                // short amount of time. The AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK case means that
                // our app is allowed to continue playing sound but at a lower volume. We'll treat
                // both cases the same way because our app is playing short sound files.

                // Pause playback and reset player to the start of the file. That way, we can
                // play the word from the beginning when we resume playback.

                mMediaPlayer.pause();
                mMediaPlayer.seekTo(0);
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // The AUDIOFOCUS_GAIN case means we have regained focus and can resume playback.
                mMediaPlayer.start();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // The AUDIOFOCUS_LOSS case means we've lost audio focus and
                // Stop playback and clean up resources
                releaseMediaPlayer();
            }
        }
    };
    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mStartButton.setVisibility(View.INVISIBLE);
            releaseMediaPlayer();
        }
    };
    private State state = State.IDLE;
    private Session ttsSession;
    private Transaction ttsTransaction;
    private DatabaseReference mUsersRatingDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        ButterKnife.bind(this);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ttsSession = Session.Factory.session(this, Configuration.SERVER_URI, Configuration.APP_KEY);
        ttsSession.getAudioPlayer().setListener(this);
        setState(State.IDLE);

        mUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mUsersRatingDatabaseReference = FirebaseDatabase.getInstance().getReference().child("rating").child(mUid);

        setSupportActionBar(mToolbar);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
        }

        TextToSpeech.OnInitListener listener =
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(final int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            Log.d("OnInitListener", "Text to speech engine started successfully.");
                            tts.setLanguage(Locale.ENGLISH);
                            speakTheText("Do you want to read from file or from camera ?");
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            startVoiceTrigger(SPEECH_REQUEST_CODE);

                        } else {
                            Log.d("OnInitListener", "Error starting the text to speech engine.");
                        }
                    }
                };

        tts = new TextToSpeech(this.getApplicationContext(), listener);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                mStartButton.setVisibility(View.GONE);
                stopTts();
                int id = item.getItemId();
                switch (id) {
                    case R.id.action_open_file: {
                        item.setCheckable(true);
                        mDrawerLayout.closeDrawers();
                        openFileExplorerActivity();
                        return true;
                    }

                    case R.id.action_text_detection: {
                        item.setCheckable(true);
                        mDrawerLayout.closeDrawers();
                        openOcrCaptureActivity();
                        return true;
                    }

                    case R.id.action_users: {
                        item.setCheckable(true);
                        mDrawerLayout.closeDrawers();
                        openUsers();
                        return true;
                    }

                    default:
                        return false;
                }
            }
        });

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (state) {
                    case IDLE:
                        //If we are not loading TTS from the server, then we should do so.
                        if (ttsTransaction == null) {
                            mStartButton.setText(getResources().getString(R.string.cancel));
                        }
                        //Otherwise lets attempt to cancel that transaction
                        else {
                            ttsTransaction.cancel();
                        }
                        break;
                    case PLAYING:
                        ttsSession.getAudioPlayer().pause();
                        setState(State.PAUSED);
                        break;
                    case PAUSED:
                        ttsSession.getAudioPlayer().play();
                        setState(State.PLAYING);
                        break;
                }
            }
        });


        setRatingbar();

        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                Log.d("rating", rating + "");
                mUsersRatingDatabaseReference.setValue(rating);
            }
        });

    }

    private void setRatingbar() {
        mUsersRatingDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Float rating = dataSnapshot.getValue(Float.class);

                if (rating != null) {
                    mRatingBar.setRating(rating);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


//    private void showVoicePrompt() {
//        if (isVoiceInteractionRoot() || isVoiceInteraction()) {
//
//            VoiceInteractor.PickOptionRequest.Option cameraOption =
//                    new VoiceInteractor.PickOptionRequest.Option("camera", 0)
//                            .addSynonym("from camera");
//
//            VoiceInteractor.PickOptionRequest.Option cameraTranslatedOption =
//                    new VoiceInteractor.PickOptionRequest.Option("camera", 1)
//                            .addSynonym("from camera translated");
//
//            VoiceInteractor.PickOptionRequest.Option fileOption =
//                    new VoiceInteractor.PickOptionRequest.Option("file", 2)
//                            .addSynonym("from file");
//
//            VoiceInteractor.PickOptionRequest.Option fileTranslatedOption =
//                    new VoiceInteractor.PickOptionRequest.Option("file", 3)
//                            .addSynonym("from file translated");
//
//            VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt("Do you want to read from file or camera?");
//            getVoiceInteractor().submitRequest(new VoiceInteractor.PickOptionRequest(prompt,
//                                                       new VoiceInteractor.PickOptionRequest.Option[]{cameraOption, fileOption}, null) {
//                                                   @Override
//                                                   public void onPickOptionResult(boolean finished, Option[] selections, Bundle result) {
//
//                                                       if (finished) {
//                                                           int index = selections[0].getIndex();
//                                                           Log.d("index", index + "");
//
//                                                           switch (index) {
//                                                               case 0:
//                                                                   openOcrCaptureActivity();
//                                                                   finish();                                                                   break;
//                                                               case 1:
//                                                                   openOcrCaptureTranslatedActivity();
//                                                                   finish();
//                                                               case 2:
//                                                                   openFile();
//                                                                   finish();
//                                                               case 3:
//                                                                   openFileTranslated();
//                                                                   finish();
//                                                           }
//                                                       }
//                                                   }
//                                               }
//
//            );
//
//        }
//
//
//    }

    private void openFileTranslated(String fileName) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + fileName + ".txt";

        Uri fileUri = Uri.fromFile(new File(path));
        Log.d("fileUri", fileUri.toString() + "");
        try {
            readFile(fileUri, true, "txt");
        } catch (IOException e) {
            speakTheText("this file not found");
            e.printStackTrace();
        }
    }

    private void openFile(String fileName) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + fileName + ".txt";

        Uri fileUri = Uri.fromFile(new File(path));
        Log.d("fileUri", fileUri.toString() + "");

        try {
            readFile(fileUri, false, "txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void openUsers() {
        Intent intent = new Intent(this, UsersActivity.class);
        startActivity(intent);
    }

    private void openFileExplorerActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");
        String[] mimetypes = {"application/pdf", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void openOcrCaptureTranslatedActivity() {
        Intent intentA = new Intent(this, OcrCaptureActivity.class);
        intentA.putExtra("translated", true);
        startActivity(intentA);
    }

    private void openOcrCaptureActivity() {
        Intent intentA = new Intent(this, OcrCaptureActivity.class);
        intentA.putExtra("translated", false);
        startActivity(intentA);
    }

    @SuppressLint("StaticFieldLeak")
    private void readFile(Uri uri, final boolean translated, String type) throws IOException {
        if (null == uri) return;

        final StringBuilder result = new StringBuilder();

        String MIMEType;

        if (type == null) {
            MIMEType = getMimeType(uri);
        } else {
            MIMEType = type;
        }

        switch (MIMEType) {
            case "pdf":
                new AsyncTask<Uri, Void, String>() {
                    @Override
                    protected String doInBackground(Uri... uris) {
                        Uri uri = uris[0];
                        InputStream inputStream = null;
                        try {
                            inputStream = getContentResolver().openInputStream(uri);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        try {
                            assert inputStream != null;
                            PdfReader reader = new PdfReader(inputStream);
                            int n = reader.getNumberOfPages();

                            for (int i = 0; i < n - 1; i++) {
                                result.append(PdfTextExtractor.getTextFromPage(reader, i + 1).trim())
                                        .append("\n");
                            }
                            reader.close();
                        } catch (Exception e) {
                            Log.d("catch", "file not found");
                            speakTheText("this file not found please try again");
                            startVoiceTrigger(SPEECH_REQUEST_FILE_CODE);
                            e.printStackTrace();
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (translated) {
                            return QueryUtils.translate(result.toString());
                        } else {
                            return result.toString();
                        }
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        if (!TextUtils.isEmpty(s)) {
                            Log.d("translated", s + "");
                            speakText(s, translated);
                        }
                    }
                }.execute(uri);
            case "txt":
                new AsyncTask<Uri, Void, String>() {
                    @Override
                    protected String doInBackground(Uri... uris) {
                        Uri uri = uris[0];
                        InputStream inputStream = null;
                        try {
                            inputStream = getContentResolver().openInputStream(uri);
                        } catch (FileNotFoundException e) {
                            Log.d("catch", "file not found");
                            speakTheText("this file not found");
                            e.printStackTrace();
                        }
                        try {
                            assert inputStream != null;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                    inputStream, "UTF-8"));

                            String line;

                            while ((line = reader.readLine()) != null) {
                                result.append(line + " ");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (translated) {
                            return QueryUtils.translate(result.toString());
                        } else {
                            return result.toString();
                        }
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        if (!TextUtils.isEmpty(s)) {
                            Log.d("translated", s + "");
                            speakText(s, translated);

                        }
                    }

                }.execute(uri);
            default:
                Log.d("MimeType", "file extension not match!");
        }
    }

//
//    public void convertTextToAudio(final String textToConvert) {
//
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String soundFileName = "WAV_" + timeStamp + "_";
//
//        HashMap<String, String> myHashRender = new HashMap();
//
//        final String destinationFileName = "/sdcard/blind/" + soundFileName + ".wav";
//
//        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, textToConvert);
//
//
//        tts.synthesizeToFile(textToConvert, myHashRender, destinationFileName);
//
//        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
//            @Override
//            public void onStart(String utteranceId) {
//
//            }
//
//            @Override
//            public void onDone(String utteranceId) {
//                if (utteranceId.equals(textToConvert)) {
//                    mStartButton.setVisibility(View.VISIBLE);
//                    mFileUri = Uri.fromFile(new File(destinationFileName));
//                }
//            }
//
//            @Override
//            public void onError(String utteranceId) {
//
//            }
//        });
//
//    }

    private String getMimeType(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void speakText(final String content, boolean translated) {

        Transaction.Options options = new Transaction.Options();
        if (translated) {
            options.setLanguage(new Language(Configuration.LANGUAGE_CODE_ARABIC));
        } else {
            options.setLanguage(new Language(Configuration.LANGUAGE_CODE_ENGLISH));
        }

        if (ttsTransaction == null) {
            ttsTransaction = ttsSession.speakString(content, options, new Transaction.Listener() {
                @Override
                public void onAudio(Transaction transaction, Audio audio) {
                    Log.d("on audio", "audio");
                    ttsTransaction = null;
                }
            });
        }

    }

    private void speakTheText(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "DEFAULT");
        }
    }

//    private void flushTheText(String content) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mStartButton.post(new Runnable() {
//                @Override
//                public void run() {
//                    mStartButton.setVisibility(View.VISIBLE);
//                    mStartButton.setText("stop");
//                }
//            });
//            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "DEFAULT");
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about: {
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            }

            case R.id.action_log_out: {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            }

            case R.id.action_start_voice_trigger: {
                startVoiceTrigger(SPEECH_REQUEST_CODE);
                return true;
            }

            case android.R.id.home: {
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onStop() {
        super.onStop();
        // When the activity is stopped, release the media player resources because we won't
        // be playing any more sounds.
    }

    private void stopTts() {
        if (tts != null) {
            tts.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        releaseMediaPlayer();
        ttsSession.getAudioPlayer().stop();

        super.onDestroy();
    }

    private void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        if (mMediaPlayer != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.
            mMediaPlayer.release();

            // Set the media player back to null. For our code, we've decided that
            // setting the media player to null is an easy way to tell that the media player
            // is not configured to play an audio file at the moment.
            mMediaPlayer = null;

            // Regardless of whether or not we were granted audio focus, abandon it. This also
            // unregisters the AudioFocusChangeListener so we don't get anymore callbacks.
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }
    }

    private void startVoiceTrigger(int id) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        startActivityForResult(intent, id);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                readFile(data.getData(), true, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            String spokenText = results.get(0);

            // Do something with spokenText
            Log.d("spokenText", "text: " + spokenText);
            if (spokenText.contains("Camera Translate") ||
                spokenText.contains("Camera translate") ||
                spokenText.contains("camera Translate"))
            {
                Log.d("catch result", "openOcrCaptureTranslatedActivity");
                openOcrCaptureTranslatedActivity();
            } else if (spokenText.contains("Camera") || spokenText.contains("camera")) {
                //speakTheText(getString(R.string.voice_camera_opened));
                Log.d("catch result", "openOcrCaptureActivity");

                openOcrCaptureActivity();
            } else if (spokenText.contains("File Translate") ||
                    spokenText.contains("file Translate") ||
                    spokenText.contains("File translate") ){
                //speakTheText(getString(R.string.voice_file_explorer_opened));
                //openFileExplorerActivity();
                Log.d("catch result", "file translate");
                speakTheText("which file you want to read");
                startVoiceTrigger(SPEECH_REQUEST_FILE_TRANSLATED_CODE);

            } else if (spokenText.contains("File") || spokenText.contains("file")) {
                //speakTheText(getString(R.string.voice_file_explorer_opened));
                //openFileExplorerActivity();
                speakTheText("which file you want to read");
                startVoiceTrigger(SPEECH_REQUEST_FILE_CODE);

            } else {
                speakTheText(getString(R.string.voice_try_again));
                startVoiceTrigger(SPEECH_REQUEST_CODE);
            }
        }

        if (requestCode == SPEECH_REQUEST_FILE_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            openFile(spokenText);
        }

        if (requestCode == SPEECH_REQUEST_FILE_TRANSLATED_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            openFileTranslated(spokenText);
        }
    }


    @Override
    public void onBeginPlaying(AudioPlayer audioPlayer, Audio audio) {
        setState(State.PLAYING);
        mStartButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFinishedPlaying(AudioPlayer audioPlayer, Audio audio) {
        setState(State.IDLE);
        mStartButton.setVisibility(View.GONE);
    }

    /**
     * Set the state and update the button text.
     */
    private void setState(State newState) {
        state = newState;
        switch (newState) {
            case IDLE:
                // Next possible action is speaking
                mStartButton.setText(getResources().getString(R.string.speak_string));
                break;
            case PLAYING:
                // Next possible action is pausing
                mStartButton.setText(getResources().getString(R.string.pause));
                break;
            case PAUSED:
                // Next possible action is resuming the speech
                mStartButton.setText(getResources().getString(R.string.speak_string));
                break;
        }
    }

    public void openCamera(View view) {
        openOcrCaptureActivity();
    }

    public void openFile(View view) {
        openFileExplorerActivity();
    }

    public void openDrive(View view) {
        // TODO: open drive
    }

    public void openDropbox(View view) {
        PackageManager manager = getPackageManager();
        Intent i = manager.getLaunchIntentForPackage("com.dropbox.android");
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        if (i.resolveActivity(getPackageManager()) != null) {
            startActivity(i);
        }
    }

    private enum State {
        IDLE,
        PLAYING,
        PAUSED
    }
}