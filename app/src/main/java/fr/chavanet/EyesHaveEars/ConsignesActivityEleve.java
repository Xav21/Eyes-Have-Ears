package fr.chavanet.EyesHaveEars;

import static java.lang.Thread.sleep;

import static fr.chavanet.EyesHaveEars.Constantes.*;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
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

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

//import com.example.EyesHaveEars.BuildConfig;
//import com.example.EyesHaveEars.R;


public class ConsignesActivityEleve extends AppCompatActivity {
    private static final String TAG = "xavier/C-MQTT-AR1";
    private static final String CHANNEL_ID = "EYES-HAVE-EARS";

    // Variables
    int color = 0;
    int tempo = 0;
    String name = "";
    static int mode = 0;
    String reseau = "";
    boolean vibration;
    boolean notification;
    int dureeVibration;
    boolean orientationPaysage;
    String typeBracelet;
    int textColor;
    int backgroundColor;

    // AR1
    int AR1MaxLineSize = 9;
    int AR1MaxLine = 4;

    // Clear screen AR1
    boolean screenCleared = true;
    long millis = System.currentTimeMillis();
    boolean stopProcessClearScreen = false;

    // Objects
    Vibrator vib;
    TextView tvConsigne;
    TextView tvMoniteur;


    /**
     * **********************************************************************
     * sendMQTTACQ ()
     * ***********************************************************************
     **/
    void sendMQTTAcq(String dataToSend) {
        try {
            Log.i(TAG,"sendMQTTAcq " + dataToSend);
            // Connection MQTT
            if (!MainActivity.mqttAndroidClient.isConnected()) {
                MainActivity.mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            // Formattage MQTT topic
            String topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/acknowledge";

            // Formattage message (payload)
            message.setQos(0);  // une seule fois
            message.setPayload(dataToSend.getBytes());

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
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
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
        Log.i(TAG, "consignesActivityMQTT onCreate");

        // setContentView( R.layout.consignes_activity);
        setContentView( R.layout.eleve_activity );

        // Vibreur
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Récupération des paramètres
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        reseau = prefs.getString( "reseau", "TEST" ).toLowerCase( Locale.ROOT ).trim();
        name = prefs.getString("nom", "");
        tempo = prefs.getInt("temporisation", 7);
        vibration = prefs.getBoolean("vibration", false);
        notification = prefs.getBoolean("notification", false);
        color = prefs.getInt("color", 0);
        dureeVibration = prefs.getInt("dureevibration", 250);
        typeBracelet = prefs.getString("bracelet", "");

        orientationPaysage = prefs.getBoolean("orientation", true);
        textColor = prefs.getInt("textcolorConsigne",0);
        backgroundColor = prefs.getInt("backgroundColorConsigne",0);

        // Mode de fonctionnement
        mode = MainActivity.mode;

        // Orientation de l'écran
        if (orientationPaysage) {
            setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Connexion MQTT
        setupMQTT();

        // Clear screen timer
        new Thread(new clearScreenTimer()).start();

        // Envoi et affichage "En ligne"
        sendMQTTData(name + " en ligne");
        if (mode== modeEleveAR1 || mode== modeEleveAR1BraceletLT716 || mode== modeEleveAR1WearOS){
            sendAR1 (name + " en ligne","");
        }

        tvConsigne = findViewById(R.id.consigne);
        tvConsigne.setText(name + " en ligne" );
        tvConsigne.setBackgroundColor(backgroundColor);
        tvConsigne.setTextColor(textColor);

        tvMoniteur = findViewById(R.id.moniteur);
        tvMoniteur.setText("");
        tvMoniteur.setBackgroundColor(backgroundColor);
        tvMoniteur.setTextColor(textColor);

        // Initialisation timer
        millis = System.currentTimeMillis() + (tempo * 1000L);
        screenCleared= false;


    }
    /**
     * **********************************************************************
     * onBackPressed()
     * ***********************************************************************
     **/
    @Override
    public void onBackPressed() {
        Log.e("xavier", "ConsignesActivityMQTT onBackPressed...");
        sendMQTTData( name + " déconnecté");
        super.onBackPressed();

    }
    /**
     * **********************************************************************
     * onPause
     * ***********************************************************************
     **/
    @Override
    protected void onPause() {
        Log.i("xavier", "ConsignesActivityMQTT onPause...");
        super.onPause();
        //sendMQTTData("Ecran de " + name + " coupé.");
        // ecran android cloneGlassesScreen.cleanUp();
    }
    /**
     * **********************************************************************
     * onStop
     * ***********************************************************************
     **/
    @Override
    protected void onStop() {
        Log.i("xavier", "ConsignesActivityMQTT onStop...");
        super.onStop();
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
        stopProcessClearScreen = true;
        screenCleared = true;
        if (mode== modeEleveAR1 || mode== modeEleveAR1BraceletLT716 || mode== modeEleveAR1WearOS) {
            sendAR1( "", "" );
        }
    }
    /***********************************************************
     Toast on User Interface pour le threads
     ************************************************************ */
    public void setToast(final String value){
        runOnUiThread( () -> Toast.makeText(getApplicationContext(),value,Toast.LENGTH_LONG).show() );
    }
   /**
     * **********************************************************************
     * Setup MQTT
     * ***********************************************************************
     **/
    public void setupMQTT () {
        // MQTT subscribe
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

        // MQTT callbacks
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
                    if (topic.contains("send")) {
                        if (!jObject.getString("name").equals(name)) {

                            // Vibration
                            if (vibration) {
                                vib.vibrate(dureeVibration);
                            }

                            // Affichage Ecran
                            tvConsigne.setText(jObject.getString("data"));
                            tvConsigne.setBackgroundColor(backgroundColor);
                            tvConsigne.setTextColor(textColor);

                            tvMoniteur.setText(jObject.getString("name"));
                            tvMoniteur.setBackgroundColor(backgroundColor);
                            tvMoniteur.setTextColor(textColor);

                            // Envoi de la consigne sur l'AR1 + acquitement après affichage
                            if (mode== modeEleveAR1 || mode== modeEleveAR1BraceletLT716  || mode== modeEleveAR1WearOS) {
                                sendAR1( jObject.getString( "data" ), message.toString() );
                            } else {
                            // sinon, envoi acquitement immédiatement après l'affichage écran
                                sendMQTTAcq(payload);
                                if (!typeBracelet.equals( prefBraceletNone ) )  {MainActivity.vibrateBracelet();}
                            }

                            // Envoi notification sur montre connecté type WearOS
                            if ( mode == modeEleveScreenWearOS || mode == modeEleveAR1WearOS )  {
                                NotificationManager manager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    NotificationChannel channel=new NotificationChannel(notificationChannelId,
                                            notificationChannelName,
                                            NotificationManager.IMPORTANCE_HIGH);
                                    manager.createNotificationChannel(channel);
                                }

                                //Creating the notification object
                                NotificationCompat.Builder notification=new NotificationCompat.Builder(getApplicationContext(),notificationChannelId);
                                notification.setContentTitle(jObject.getString("name"));
                                notification.setContentText(jObject.getString("data"));
                                notification.setSmallIcon(R.drawable.icon);
                                notification.setTimeoutAfter( tempo * 1000L );
                                notification.extend(new NotificationCompat.WearableExtender().setBridgeTag("watch"));
                                manager.notify(1,notification.build());
                            }

                            // Initialisation timer
                            millis = System.currentTimeMillis() + (tempo * 1000L);
                            screenCleared= false;
                        }
                    }

                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "MQTT msg delivered");
            }
        });

    }

    /**
     * **********************************************************************
     * sendMQTTData ()
     * ***********************************************************************
     **/
    void sendMQTTData(String dataToSend) {
        try {
            Log.i(TAG, "sendMQTTData connected? " + MainActivity.mqttAndroidClient.connect());

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
            payload.put("type", "information");
            payload.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            message.setPayload(payload.toString().getBytes());

            final String data = dataToSend;
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
            e.printStackTrace();
            setToast(e.toString());

        }

    }

/*
    sendAR1

 */
void  sendAR1(final String consigne, final String jsonData){
    Thread t = new Thread((new Runnable() {
        private int retCode = 0;
        public void run() {
            try {
                // Suspend clearScreen thread
                millis = System.currentTimeMillis() + (10000);

                // Formattage de la consigne
                String localConsigne;
                localConsigne = consigne.trim();
                localConsigne = localConsigne.replace("  ", " ");
                localConsigne = localConsigne.replace("-", " ");
                localConsigne = localConsigne.toLowerCase();
                localConsigne = removeAccents(localConsigne);

                // Construction des 4 lignes à envoyer à la lunette
                int nbligne = 0;
                String[] mot;
                String txt;
                StringBuilder ligne = new StringBuilder();
                int nbMots = 0;

                mot = localConsigne.split(" ");
                for (int i = 0; i < mot.length; i++) {
                    if (i == 0) {
                        txt = mot[i];
                        nbMots = 1;
                    } else {
                        txt = ligne + " " + mot[i];
                        nbMots = nbMots + 1;
                    }
                    if (txt.length() > AR1MaxLineSize) {
                        if (nbMots==1) {
                            retCode += displayLigne(txt, nbligne);
                            nbMots = 0;
                        } else {
                            retCode += displayLigne(ligne.toString(), nbligne);
                            ligne = new StringBuilder(mot[i]);
                            nbMots = 1;
                        }
                        nbligne++;
                    } else {
                        if (i != 0) { ligne.append(" ");}
                        ligne.append(mot[i]);
                    }
                }
                // Ecriture du restant
                retCode += displayLigne(ligne.toString(), nbligne);

                // Effacement des anciennes lignes
                for (int j = nbligne + 1; j < 4; j++) {
                    retCode += displayLigne(" ", j);
                }
            }

            finally {
                Log.i(TAG,"sendAR1 finally jsonData="+jsonData+"ret="+retCode);

                // Envoi acquitement et vibration
                if (!jsonData.equals("") && retCode > 0 ) {
                    // Vibration du bracelet
                    sendMQTTAcq(jsonData);
                    if (!typeBracelet.equals( prefBraceletNone ) )  {MainActivity.vibrateBracelet();}
                }
                // Initialisation du timer si consigne affichée
                //if (!consigne.equals("")){
                //    millis = System.currentTimeMillis() + (tempo * 1000L);
                //    screenCleared = false;
                //}

            }
        }}
    ));

    t.start();
}

    /*
     * DisplayLigneAR1
     */
    @SuppressLint("MissingPermission")
    int displayLigne (String ligne, int nbligne) {
        boolean ret;
        int retCode = 0;
        int nbCar = Math.min(ligne.length(),AR1MaxLineSize);
        ligne =  ligne.substring(0,nbCar);

        byte[] bytes = ligne.getBytes(StandardCharsets.UTF_8);
        int i = 0;
        while (!MainActivity.flagOnCharacteristicWrite || i < 50 ){
            try {
                sleep(5);
                i++;
            } catch (InterruptedException e) {
                e.printStackTrace();
                setToast(e.toString());
            }
        }
        if (AR1MaxLine > nbligne) {
            MainActivity.flagOnCharacteristicWrite = false;
            MainActivity.characteristicAr1Lignes[nbligne].setValue(bytes);
            MainActivity.characteristicAr1Lignes[nbligne].setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            ret = MainActivity.ar1Gatt.writeCharacteristic(MainActivity.characteristicAr1Lignes[nbligne]);
            if (ret) retCode = 1;
            else retCode = -1000;
            Log.i(TAG, "send AR1=>" + ligne + " ligne=" + nbligne + " stat=" + ret);
        }
        return retCode;
    }
//========================================================
// removeAccents
//========================================================
static String removeAccents(String data) {
        data = data.replace ("à","a");
        data = data.replace ("œ","oe");
        data = data.replace ("ç","c");
        data = data.replace ("é","e");
        data = data.replace ("ê","e");
        data = data.replace ("ë","e");
        data = data.replace ("è","e");
        data = data.replace ("î","i");
        data = data.replace ("ï","i");
        data = data.replace ("ô","o");
        data = data.replace ("ù","u");
        return data;
    }

    /**
        clearScreenTimer
        ===============
            clear screen on timeout
            prevent glass from power off

     **/
    class clearScreenTimer implements Runnable {
        long lastTimeScreenCleared = System.currentTimeMillis() + timeOut_AR1;

        public void run() {
            while (!stopProcessClearScreen) {
                // Effacement de l'écran après affichage d'une consigne
                if (!screenCleared && System.currentTimeMillis() > millis) {
                    Log.i(TAG, "Clear screen");
                    screenCleared = true;

                    // Effacement de la consigne sur l'AR1
                    if (mode== modeEleveAR1 || mode== modeEleveAR1BraceletLT716 || mode== modeEleveAR1WearOS) {
                        sendAR1( "", "");
                    }

                    // Effacement de la consigne sur l'ecran
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvConsigne.setText("");
                            tvConsigne.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorblack ));
                            tvConsigne.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorblack ));
                            tvMoniteur.setText("");
                            tvMoniteur.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorblack ));
                            tvMoniteur.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorblack ));
                        }
                    });
                }

                // Prevent glass from power off
                else if (lastTimeScreenCleared + timeOut_AR1 < System.currentTimeMillis() ) {
                    if (mode== modeEleveAR1 || mode== modeEleveAR1BraceletLT716 || mode== modeEleveAR1WearOS) {
                        Log.i(TAG, "screen wake up");
                        screenCleared = false;
                        sendAR1( ".", "" );
                    }

                    lastTimeScreenCleared = System.currentTimeMillis();
                }

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    setToast(e.toString());
                }
            }
        }

    }

}