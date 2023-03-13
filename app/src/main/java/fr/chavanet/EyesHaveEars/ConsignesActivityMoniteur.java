package fr.chavanet.EyesHaveEars;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import static android.speech.SpeechRecognizer.isRecognitionAvailable;

//import com.example.EyesHaveEars.BuildConfig;
//import com.example.EyesHaveEars.R;

public class ConsignesActivityMoniteur extends AppCompatActivity {

    int color = 0;
    int tempo = 0;
    String name = "";
    String mode = "";
    String reseau = "";
    int dureeVibration;

    private static final String TAG = "xavier/C-MQTT";

    boolean vibration;

    Vibrator vib;

    ListView liste;
    ConsignesAdaptater itemAdaptater;
    ImageView microphone;
    List<ConsignesModel> listConsignes = new ArrayList<>();

    Dictionnaire correcteur;
    private SpeechRecognizer sr;
    CloneGlassesScreen cloneGlassesScreen;

    /**
     * **********************************************************************
     * formattage heure ()
     * ***********************************************************************
     **/
    String getHeure() {
        Date date = new Date();
        SimpleDateFormat formatDate = new SimpleDateFormat("HH:mm:ss");
        return formatDate.format(date);
    }

    /**
     * **********************************************************************
     * Action utilisateur sur les touches du téléphone
     * ***********************************************************************
     **/
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Log.i(TAG,"Keycode="+keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                methodeSendVoice();
                return true;
            default:
                //Toast.makeText(getApplicationContext(), String.valueOf(keyCode), Toast.LENGTH_LONG).show();
                return super.dispatchKeyEvent(event);
        }
    }

    /**
     * **********************************************************************
     * sendMQTTData ()
     * ***********************************************************************
     **/
    void sendMQTTData(String dataToSend, String type) {

        // Mise à jour de l'écran
        if (!listConsignes.isEmpty() && listConsignes.get(0).getProgresBar() == View.VISIBLE) {
            ConsignesModel item  = listConsignes.get(0);
            item.setIcons(3);
        }

        listConsignes.add(0, new ConsignesModel(getHeure(), name, "", dataToSend, getResources().getColor( R.color.colorConsigne),1));
        itemAdaptater.notifyDataSetChanged();
        cloneGlassesScreen.displayConsigne(dataToSend,tempo);

        try {
            // Connection MQTT
            if (!MainActivity.mqttAndroidClient.isConnected()) {
                MainActivity.mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            // Formattage MQTT topic
            String topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/send";
            // Formattage message (payload)
            message.setQos(0);  // une seule fois

            JSONObject payload = new JSONObject();
            payload.put("name", name);
            payload.put("data", dataToSend);
            payload.put("color", color);
            payload.put("temporisation", tempo);
            payload.put("type", type );
            payload.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            message.setPayload(payload.toString().getBytes());

            // Envoi MQTT
            MainActivity.mqttAndroidClient.publish(topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT publish succeed! ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "MQTT publish failed!");
                }
            });
        } catch (MqttException | JSONException e) {
            Log.e(TAG, e.toString());
            setToast(e.toString());
        }

    }

    /***********************************************************
     Toast on User Interface pour le threads
     ************************************************************ */
    public void setToast(final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),value,Toast.LENGTH_LONG).show();
            }
        });
    }
    /**
     * **********************************************************************
     * Lancement de ma reconnaissance vocale Google
     * ***********************************************************************
     **/
    void methodeSendVoice() {
        Log.i(TAG, "MethodeSendVoice");
        if (isRecognitionAvailable(getApplicationContext())) {
            try {
                Log.i(TAG, "MethodeSendVoice intentSpeech");
                Intent intentSpeech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intentSpeech.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1);
                intentSpeech.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intentSpeech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intentSpeech.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intentSpeech.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez...");
                intentSpeech.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                intentSpeech.putExtra(RecognizerIntent.EXTRA_RESULTS, 1);
                //intentSpeech.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, 1);
                //sr.cancel();
                sr.startListening(intentSpeech);
            } catch (Exception e) {
                setToast(e.toString());
            }
        }
    }

    /**
     * **********************************************************************
     * onCreate
     * ***********************************************************************
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.moniteur_activity );
        Log.i(TAG, "consignesActivityMQTT onCreate");

        // On cache le bouton Synchroniser
        ImageButton buttonSync;
        buttonSync = (ImageButton) findViewById(R.id.synchroniser);
        buttonSync.setVisibility(View.GONE);

        // Vibreur
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Récupération des paramètres
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mode = prefs.getString("mode", "0");
        reseau = prefs.getString( "reseau", "TEST" ).toLowerCase( Locale.ROOT ).trim();
        name = prefs.getString("nom", "");
        tempo = prefs.getInt("temporisation", 7);
        vibration = prefs.getBoolean("vibration", false);
        color = prefs.getInt("color", 0);
        dureeVibration = prefs.getInt("duree", 250);

        // Gestion de la liste de consignes
        liste = findViewById(R.id.ListeMessage);
        itemAdaptater = new ConsignesAdaptater( ConsignesActivityMoniteur.this, listConsignes);
        liste.setAdapter(itemAdaptater);

        // Démarrage du clone de l'ecran de l'AR1
        cloneGlassesScreen = new CloneGlassesScreen(
                prefs.getString("lunettes","0"),
                (TextView) findViewById(R.id.fullConsigne),
                (TextView) findViewById(R.id.ligne0),
                (TextView) findViewById(R.id.ligne1),
                (TextView) findViewById(R.id.ligne2),
                (TextView) findViewById(R.id.ligne3),
                (ProgressBar)  findViewById(R.id.progressBar1),
                listConsignes,
                itemAdaptater);


        // Initialisation Google Speech
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new listenerRecongnition());
        microphone = findViewById(R.id.microphone);

        // Récupération du dictionnaire
        correcteur = new Dictionnaire(getApplicationContext());

        // Connexion MQTT
        setupMQTT();
        //sendMQTTData(name + " en ligne","information");

    }
    /**
     * **********************************************************************
     * onResume
     * ***********************************************************************
     **/
    @Override
    protected void onResume() {
        super.onResume();
        Log.i( "xavier", "ConsignesActivityMQTT onPause..." );
        sendMQTTData( name + " en ligne", "information" );
    }
    /**
     * **********************************************************************
     * onPause
     * ***********************************************************************
     **/
    @Override
    protected void onPause() {
        Log.i("xavier", "ConsignesActivityMQTT onPause...");
        sendMQTTData(name + " hors ligne","information");
        super.onPause();
    }
    /**
     * **********************************************************************
     * onDestroy
     * ***********************************************************************
     **/
    @Override
    protected void onDestroy() {
        Log.i("xavier", "ConsignesActivityMQTT onDestroy...");
        super.onDestroy();
        sr.destroy();
        cloneGlassesScreen.cleanUp();
    }

    /**
     * **********************************************************************
     * Résultats de la reconnaissance vocale
     * ***********************************************************************
     **/
    class listenerRecongnition implements RecognitionListener {
        int nbRecognition = 0;

        public void onReadyForSpeech(Bundle params) {
            Log.i(TAG, "onReadyForSpeech");
            microphone.setVisibility(View.VISIBLE);
        }

        public void onBeginningOfSpeech() {
            Log.i(TAG, "onBeginningOfSpeech");
            nbRecognition = 0;
        }

        public void onRmsChanged(float rmsdB) {
            //Log.i(TAG, "onRmsChanged");
        }

        public void onBufferReceived(byte[] buffer) {
            Log.i(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.i(TAG, "onEndofSpeech");
            microphone.setVisibility(View.INVISIBLE);
            nbRecognition = 0;
        }

        public void onError(int error) {
            Log.i(TAG, "error " + error);
            if ( error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                microphone.setVisibility(View.INVISIBLE);
            }
        }

        public void onResults(Bundle results) {
            String str;
            Log.i(TAG, "onResults " + results + " nbRecognition=" + nbRecognition);
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (data != null && nbRecognition == 0) {
                str = data.get(0);
                str = correcteur.replaceText(str);
                sendMQTTData(str,"consigne");
            }
            nbRecognition++;
        }

        public void onPartialResults(Bundle partialResults) {
            List<String> partialResultList = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.i(TAG, "onPartialResults " + partialResults.describeContents() + "->" + partialResultList.toString());

        }

        public void onEvent(int eventType, Bundle params) {
            Log.i(TAG, "onEvent " + eventType);
        }
    }

    /**
     * **********************************************************************
     * Setup MQTT
     * ***********************************************************************
     **/
    public void setupMQTT ()  {
        Log.i(TAG, "MQTT setCallBacks");

        try {
            // Abonnements MQTT (subscribe)
            //-----------------------------
            String topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/acknowledge";
            MainActivity.mqttAndroidClient.subscribe(topic, 0, null);
            topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/send";
            MainActivity.mqttAndroidClient.subscribe(topic, 0, null);
        } catch (MqttException e) {
            e.printStackTrace();
            setToast(e.toString());
        }

        MainActivity.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.i(TAG, "MQTT connectComplete");
                try {
                    // Abonnements MQTT (subscribe)
                    //-----------------------------
                    String topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/acknowledge";
                    MainActivity.mqttAndroidClient.subscribe(topic, 0, null);
                    topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/send";
                    MainActivity.mqttAndroidClient.subscribe(topic, 0, null);
                } catch (MqttException e) {
                    e.printStackTrace();
                    setToast(e.toString());
                }
                //if (mode.equals("1")) {
                    sendMQTTData(name + " en ligne","information");
                //}
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.i(TAG, "MQTT connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                Log.i(TAG, "MQTT topic: " + topic + ", msg: " + payload);

                try {
                    JSONObject jObject = new JSONObject(payload);
                    if (topic.contains("acknowledge")) {
                        ConsignesModel item;
                        item = listConsignes.get(0);
                        // si moniteur et message précedents identiques, acquitement
                        if (jObject.getString("name").equals(item.getName()) && jObject.getString("data").equals(item.getText())) {
                            if (vibration) {
                                vib.vibrate(dureeVibration);
                            }
                            item.setColor(jObject.getInt("color"));
                            item.setNokIcon(View.INVISIBLE);
                            item.setNokIcon(View.INVISIBLE);
                            item.setOkIcon(View.VISIBLE);
                            itemAdaptater.notifyDataSetChanged();
                            cloneGlassesScreen.clearProgressBar();
                        }
                    } else {
                        if (!jObject.getString("name").equals(name)) {
                            if (vibration) {
                                vib.vibrate(dureeVibration);
                            }
                            String type = jObject.getString("type");
                            if (!listConsignes.isEmpty() && listConsignes.get(0).getProgresBar() == View.VISIBLE) {
                                ConsignesModel item  = listConsignes.get(0);
                                item.setProgresBar(View.INVISIBLE);
                                item.setNokIcon(View.VISIBLE);
                                item.setOkIcon(View.INVISIBLE);
                            }

                            if ( type.equals("consigne") ) {
                                listConsignes.add(0, new ConsignesModel(getHeure(), jObject.getString("name"), "", jObject.getString("data"), getResources().getColor(R.color.colorConsigne),1));
                                cloneGlassesScreen.displayConsigne(jObject.getString("data"),jObject.getInt("temporisation"));
                            } else if ( type.equals("information") ) {
                                listConsignes.add(0, new ConsignesModel(getHeure(), jObject.getString("name"), "", jObject.getString("data"), jObject.getInt("color"),0));
                                cloneGlassesScreen.displayConsigne(jObject.getString("data"),jObject.getInt("temporisation"));
                                cloneGlassesScreen.clearProgressBar();
                            } else if ( type.equals("lunettes") )  {
                                listConsignes.add(0, new ConsignesModel(getHeure(), jObject.getString("name"), "", jObject.getString("data"), jObject.getInt("color"),0));
                            }
                            itemAdaptater.notifyDataSetChanged();
                        }
                    }

                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                    setToast(e.toString());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "MQTT msg delivered");
            }
        });

    }

}



