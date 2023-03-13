package fr.chavanet.EyesHaveEars;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.example.EyesHaveEars.BuildConfig;
//import com.example.EyesHaveEars.R;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import static fr.chavanet.EyesHaveEars.Constantes.*;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "xavier/MainActivity";

    int color = 0;

    // Bluetooth
    static BluetoothGattCharacteristic[] characteristicAr1Lignes = {null, null, null, null};
    static BluetoothGattCharacteristic characteristicBracelet;

    // GAT object
    static BluetoothGatt ar1Gatt;
    static BluetoothGatt braceletGatt;


    // Gestion de la connection BLE
    static boolean lunetteConnected = false;
    static boolean braceletConnected = false;
    static boolean flagOnCharacteristicWrite = true;
    private static boolean stopScanRequested = false;

    // Bluetooth (BLE) object
    private static BluetoothAdapter bluetoothAdapterBLE;

    // MQTT
    static MqttAndroidClient mqttAndroidClient;

    // Les préferences utilisateur
    static int mode;
    String name;
    int tempo;
    String reseau;
    String mqttServer;
    int mqttPort;
    String mqttUsername;
    String mqttPasword;
    String typeBracelet;

    // Screen objects
    Button buttonStart;
    TextView tvMode;
    TextView tvLunettes;
    TextView tvReseau;
    TextView tvNom;
    TextView tvTypeBracelet;
    TextView tvMsgAR1;
    TextView tvMsgMQTT;
    TextView tvMsgBracelet;

    Intent intentConsigneActivityMoniteur;
    Intent intentConsigneActivityEleve;

    Intent intentDictionnaireActivity;
    Intent intentSettingsActivity;
    Intent intentAboutActivity;

    /***********************************************************************
     On Create
     ***********************************************************************/

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        Log.i( TAG, "onCreate" );

        setContentView( R.layout.main_activity );

        // Enable SSL si version < Lollipop (21)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.i( TAG, "Démarrage de SSL pour android version inférieur à LOLLIPOP (sdk version=" + Build.VERSION.SDK_INT + ")" );
            try {
                ProviderInstaller.installIfNeeded( getApplicationContext() );
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            }
        }
        // Toolbar
        Toolbar toolbar = findViewById( R.id.toobar );
        setSupportActionBar( toolbar );

        // Activités
        intentConsigneActivityMoniteur = new Intent( MainActivity.this, ConsignesActivityMoniteur.class );
        intentConsigneActivityEleve = new Intent( MainActivity.this, ConsignesActivityEleve.class );
        intentDictionnaireActivity = new Intent( MainActivity.this, DictionnaireActivity.class );
        intentSettingsActivity = new Intent( MainActivity.this, SettingsActivity.class );
        intentAboutActivity = new Intent( MainActivity.this, AboutActivity.class );

        // Bluetooth event
        IntentFilter filter = new IntentFilter();
        filter.addAction( BluetoothDevice.ACTION_FOUND );
        filter.addAction( BluetoothDevice.ACTION_BOND_STATE_CHANGED );
        filter.addAction( BluetoothDevice.ACTION_ACL_CONNECTED );
        filter.addAction( BluetoothDevice.ACTION_ACL_DISCONNECTED );
        filter.addAction( BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED );

        // Les champs
        tvMode = findViewById( R.id.mode );
        tvLunettes = findViewById( R.id.lunettes );
        tvReseau = findViewById( R.id.reseau );
        tvNom = findViewById( R.id.nom );
        tvMsgAR1 = findViewById( R.id.msgAR1 );
        tvTypeBracelet = findViewById( R.id.typeBracelet );
        tvMsgMQTT = findViewById( R.id.msgMQTT );
        tvMsgBracelet = findViewById( R.id.msgBracelet );

        // Bouton de démarrage
        buttonStart = findViewById( R.id.Start );

        //===================================
        // Permissions
        //===================================
        List<String> permissions = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission( Manifest.permission.RECORD_AUDIO ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.RECORD_AUDIO );
            if (checkSelfPermission( Manifest.permission.BLUETOOTH ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.BLUETOOTH );
            if (checkSelfPermission( Manifest.permission.BLUETOOTH_ADMIN ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.BLUETOOTH_ADMIN );
            if (checkSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.ACCESS_FINE_LOCATION );
            if (checkSelfPermission( Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.ACCESS_COARSE_LOCATION );
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) {
            if (checkSelfPermission( Manifest.permission.BLUETOOTH_SCAN ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.BLUETOOTH_SCAN );
            if (checkSelfPermission( Manifest.permission.BLUETOOTH_CONNECT ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.BLUETOOTH_CONNECT );
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ) {
            if (checkSelfPermission( Manifest.permission.POST_NOTIFICATIONS ) != PackageManager.PERMISSION_GRANTED)
                permissions.add( Manifest.permission.POST_NOTIFICATIONS );
        }

        if (!permissions.isEmpty()) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder( MainActivity.this );
            builder1.setMessage( "Eyes Have Ears utilise le micro du téléphone à la demande pour traduire les consignes vocales du moniteur en texte à destination de l'élève. " +
                    "Le micro est activé pour quelques secondes après l'appui sur une touche volume haut ou bas du téléphone. " +
                    "\n\nEyes Have Ears utilise la technologie de reconnaisance vocale Google." +
                    "\n\nEyes Have Ears utilise l'interface Bluetooth Low Energy pour pouvoir transmettre les consignes aux lunettes AR1. A ce titre, Eyes Have Ears a besoin d'accèder aux données de localisation." +
                    "\n\nMerci de bien vouloir accepter et d'autoriser l'application à acceder aux données de localisation et au microphone" );
            builder1.setCancelable( false );

            builder1.setPositiveButton(
                    "J'accepte",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            String[] params;
                            params = permissions.toArray( new String[0] );
                            requestPermissions( params, 10 );
                        }
                    } );

            builder1.setNegativeButton(
                    "Je refuse",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();
                        }
                    } );

            AlertDialog alert1 = builder1.create();
            alert1.show();

        }

        // ==========================
        // Bouton démarrer
        // ===========================
        mode = getMode();
        setStartButton();
        buttonStart.setOnClickListener( v -> {
            try {
                Log.i( TAG, "startActivity..." );
                switch (mode) {
                    case modeMoniteur:
                        startActivity( intentConsigneActivityMoniteur );
                        break;
                    default:
                        startActivity( intentConsigneActivityEleve );
                        break;
                }
            } catch (Exception e) {
                Toast.makeText( getApplicationContext(), e.toString(), Toast.LENGTH_LONG ).show();
            }
        } );

    }

    /***********************************************************************
     On Start
     ***********************************************************************/
    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        Log.i( TAG, "onStart" );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        // Configuration initiale
        if (prefs.getString( "nom", "-" ).equals( "-" ) || prefs.getString( "nom", "-" ).trim().equals( "" )) {
            Toast.makeText( getApplicationContext(), "Merci de bien vouloir saisir un nom valide.", Toast.LENGTH_LONG ).show();
            startActivity( intentSettingsActivity );
        }

        color = prefs.getInt( "color", 0xff1D70B7 );
        name = prefs.getString( "nom", "" );
        String prefMode = prefs.getString( "mode", prefModeMoniteur );
        String prefLunette = prefs.getString( "lunettes", androidScreen );
        tempo = prefs.getInt( "temporisation", 7 );
        reseau = prefs.getString( "reseau", "TEST" ).toLowerCase( Locale.ROOT ).trim();
        mqttServer = prefs.getString( "mqttservername", defaultMQTTServername );
        mqttPort = Integer.parseInt( Objects.requireNonNull( prefs.getString( "mqttserverport",defaultMQTTServerPort ) ) );
        mqttUsername = prefs.getString( "mqttsuser", defaultMQTTUsername );
        mqttPasword = prefs.getString( "mqttpswd", defaultMQTTPassword);

        typeBracelet = prefs.getString( "bracelet", "0" );

        String display = "Mode : " + getResources().getStringArray( R.array.mode_entries )[Integer.parseInt( prefMode )];
        tvMode.setText( display );

        display = "Affichage :  " + getResources().getStringArray( R.array.lunettes_entries )[Integer.parseInt( prefLunette )];
        tvLunettes.setText( display );

        display = "Vibration :  " + getResources().getStringArray( R.array.bracelet_entries )[Integer.parseInt( typeBracelet )];
        tvTypeBracelet.setText( display );

        display = "Réseau de vol : " + reseau;
        tvReseau.setText( display );

        display = "Nom : " + prefs.getString( "nom", "utilisateur" );
        tvNom.setText( display );

        tvMsgMQTT.setText( "" );
        tvMsgAR1.setText( "" );
        tvMsgBracelet.setText( "" );

        // Clear callbacks MQTT
        if (mqttAndroidClient != null) mqttAndroidClient.unregisterResources();

        // Recherche du mode de fonctionnement
        mode = getMode();

        // Lancement des connexions
        setupMQTTMode();
        setupBLE( mode );
    }

    /***********************************************************************
     *  onStop
     ***********************************************************************/
    @Override
    protected void onStop() {
        Log.i( TAG, "MaintActivity onStop..." );
        super.onStop();
        stopBLEScan();
    }

    /***********************************************************************
     *  onDestroy
     ***********************************************************************/
    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i( TAG, "onDestroy..." );

        try {
            ar1Gatt.disconnect();
            ar1Gatt.close();
        } catch (Exception ignored) {
        }

        try {
            braceletGatt.disconnect();
            braceletGatt.close();

        } catch (Exception ignored) {
        }


    }

    /***********************************************************************
     *  onResume
     ***********************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        Log.i( TAG, "onResume..." );
    }

    /***********************************************************************
     *  onRestart
     ***********************************************************************/
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i( TAG, "onRestart..." );
    }

    /***********************************************************************
     *  onPause
     ***********************************************************************/
    @Override
    protected void onPause() {
        Log.i( TAG, "onPause..." );
        super.onPause();
    }

    /***********************************************************************
     *  onCreateOptionsMenu
     ***********************************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.main_menu, menu );
        return true;
    }

    /***********************************************************************
     *  onOptionsItemSelected
     ***********************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if (id == R.id.action_dictionnaire) {
            startActivity( intentDictionnaireActivity );
        } else if (id == R.id.action_settings) {
            startActivity( intentSettingsActivity );
        } else if (id == R.id.about) {
            startActivity( intentAboutActivity );
        }
        return super.onOptionsItemSelected( item );
    }


    /***********************************************************************
     *  Setup MQTT mode
     ***********************************************************************/
    public void setupMQTTMode() {
        // Update status
        if (mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {
            setInfo( tvMsgMQTT, "Tentative de connexion au réseau " + reseau + "...", false );
        } else {
            setInfo( tvMsgMQTT, "Connecté au réseau " + reseau, true );
        }
        setStartButton();

        // Start MQTT
        String serverUri = "ssl://" + mqttServer + ":" + mqttPort;
        String clientId = reseau + "_" + name + "_" + mode + "_" + UUID.randomUUID().toString();
        mqttAndroidClient = new MqttAndroidClient( this, serverUri, clientId, new MemoryPersistence(), MqttAndroidClient.Ack.AUTO_ACK );
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect( true );
        mqttConnectOptions.setCleanSession( false );
        mqttConnectOptions.setUserName( mqttUsername );
        mqttConnectOptions.setPassword( mqttPasword.toCharArray() );
        //
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mqttConnectOptions.setHttpsHostnameVerificationEnabled( false );
        }
        try {
            mqttAndroidClient.connect( mqttConnectOptions, this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled( true );
                    disconnectedBufferOptions.setBufferSize( 100 );
                    disconnectedBufferOptions.setPersistBuffer( false );
                    disconnectedBufferOptions.setDeleteOldestMessages( true );
                    mqttAndroidClient.setBufferOpts( disconnectedBufferOptions );
                    Log.i( TAG, "MQTT Connect success" );
                    setInfo( tvMsgMQTT, "Connecté au réseau " + reseau, true );
                    setStartButton();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i( TAG, "MQTT Connect failed:" + exception.toString() );
                    exception.printStackTrace();
                    setStartButton();
                }
            } );
        } catch (MqttException e) {
            e.printStackTrace();
        }

        //
    }


    // ===============================================================
    // Set start button
    // ==============================================================
    private void setInfo(final TextView tv, final String text, final boolean b) {
        runOnUiThread( () -> {
            tv.setText( text );
            if (b) {
                tv.setTextColor( getResources().getColor( R.color.colorStartButtonEnable ) );
            } else
                tv.setTextColor( getResources().getColor( R.color.colorStartButtonDisable ) );
        } );
    }

    //===========================================================
    //===========================================================

    public void setStartButton() {
        runOnUiThread( new Runnable() {

            boolean status = false;

            public void run() {
                if (mqttAndroidClient != null) {
                    Log.i( TAG, "setStartButton mode=" + mode + " mqtt=" + mqttAndroidClient.isConnected() + " bracelet=" + braceletConnected + " lunettes=" + lunetteConnected );
                }
                switch (mode) {
                    case modeMoniteur:
                    case modeEleveScreen:
                    case modeEleveScreenWearOS:
                        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                            status = true;
                        }
                        break;
                    case modeEleveScreenBraceletLT716:
                        if (mqttAndroidClient != null && mqttAndroidClient.isConnected() && braceletConnected) {
                            status = true;
                        }
                        break;
                    case modeEleveAR1:
                    case modeEleveAR1WearOS:
                        if (mqttAndroidClient != null && mqttAndroidClient.isConnected() && lunetteConnected) {
                            status = true;
                        }
                        break;
                    case modeEleveAR1BraceletLT716:
                        if (mqttAndroidClient != null && mqttAndroidClient.isConnected() && lunetteConnected && braceletConnected) {
                            status = true;
                        }
                        break;
                    default:
                        break;
                }

                // Progress spinner
                ProgressBar spinner;
                spinner = findViewById( R.id.progressBar1 );
                buttonStart.setEnabled( status );
                if (status) {
                    buttonStart.setText( getString( R.string.start ) );
                    spinner.setVisibility( View.GONE );
                } else {
                    buttonStart.setText( "" );
                    spinner.setVisibility( View.VISIBLE );
                }
            }
        } );
    }

    /**
     * **********************************************************************
     * Setup BLE
     * ***********************************************************************
     **/
    public void setupBLE(int mode) {
        Log.i( TAG, "setupBLE...mode = " + mode );
        // Check rigth
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e( TAG, "BLE not enabled denied." );
            return;
        }
        if (mode == modeEleveScreen || mode == modeMoniteur) {
            Log.i( TAG, "Stop BLE Scan : BLE not required ! mode elève sur écran." );
            return;
        }
        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            Log.e( TAG, "BLE permission denied." );
            return;
        }

        // Affichage info lunette
        if (lunetteConnected && (mode == modeEleveAR1 || mode == modeEleveAR1BraceletLT716 || mode == modeEleveAR1WearOS)) {
            setInfo( tvMsgAR1, "Lunettes connectées " + ar1Gatt.getDevice().getName(), true );
        } else {
            setInfo( tvMsgAR1, "", true );
        }


        // Affichage info bracelet
        if (braceletConnected && (mode == modeEleveAR1BraceletLT716 || mode == modeEleveScreenBraceletLT716) ) {
            setInfo(tvMsgBracelet, "Bracelet connecté " + braceletGatt.getDevice().getName() , true);
        } else {
            setInfo( tvMsgBracelet, "", true );
        }

        // Bouton démarrage ?
        if ( (lunetteConnected && mode == modeEleveAR1) || ( braceletConnected && mode == modeEleveScreenBraceletLT716)) {
            setStartButton();
            return;
        }

        // Tentatives de connection
        if (!lunetteConnected && (mode == modeEleveAR1 || mode == modeEleveAR1BraceletLT716 || mode == modeEleveAR1WearOS) ) {
            Log.i(TAG,"Set info lunettes");
            setInfo(tvMsgAR1, "Tentative de connexion à la paire de lunettes... ", false);
        }
        if (!braceletConnected && (mode == modeEleveAR1BraceletLT716 || mode == modeEleveScreenBraceletLT716)) {
            Log.i(TAG,"Set info bracelet");
            setInfo(tvMsgBracelet, "Tentative de connexion au bracelet... " , false);
        }

        // Ensures Bluetooth is available on the device and it is enabled.
        if (bluetoothAdapterBLE == null || !bluetoothAdapterBLE.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapterBLE = bluetoothManager.getAdapter();
        startBLEScan(mode);

    }

    /**
     * **********************************************************************
     * BLE GATT Callbacks
     * ***********************************************************************
     **/
    BluetoothGattCallback lunettesGattCallback =
            new BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server " + gatt.getDevice().getName() + " " +  gatt.getDevice().getAddress() + " " + gatt.getServices().toString());
                        gatt.discoverServices();
                        flagOnCharacteristicWrite = true;
                        ar1Gatt = gatt;

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                        lunetteConnected = false;
                        flagOnCharacteristicWrite = true;
                        sendMQTTData("Lunettes " + name + " déconnectées" );
                        startBLEScan(mode);
                        setInfo(tvMsgAR1, "Lunettes AR1 déconnectées",false);
                        setStartButton ();
                    }
                }
                @Override
                public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
                    Log.i(TAG, "onCharacteristicWrite " + characteristic.getUuid() + " status="+status);
                    flagOnCharacteristicWrite = true;
                }
                @Override
                public void onReliableWriteCompleted (BluetoothGatt gatt, int status){
                    Log.i(TAG, "onReliableWriteCompleted ");
                    flagOnCharacteristicWrite = true;
                }
                @SuppressLint("MissingPermission")
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.i(TAG,"GATT onServicesDiscovered");
                    int nbServiceFound = 0;
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                Log.i(TAG,characteristic.getUuid().toString() );

                                for (int nbChar = 0; nbChar < ar1LignesCharacteristics.length; nbChar++ ) {
                                    if (characteristic.getUuid().toString().equals( ar1LignesCharacteristics[nbChar])) {
                                        characteristicAr1Lignes[nbChar] = characteristic;
                                        nbServiceFound = nbServiceFound + 1;
                                    }
                                }
                            }
                        }
                        lunetteConnected = true;
                        // Les 4 services ont été trouvés ==> AR1 ok
                        if (nbServiceFound == 4 ) {
                            Log.i(TAG, "BLE Connected");
                            flagOnCharacteristicWrite = true;
                            lunetteConnected = true;
                            sendMQTTData("Lunettes " + name + " connectées" );
                            setInfo(tvMsgAR1, "Lunettes " + ar1Gatt.getDevice().getName() + " connectées",true);
                            setStartButton ();
                            stopBLEScan();

                        }
                    }
                }

            };

    BluetoothGattCallback braceletGattCallback =
            new BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i( TAG, "Connected to GATT server " + gatt.getDevice().getName() + " " + gatt.getDevice().getAddress() + " " + gatt.getServices().toString() );
                        gatt.discoverServices();
                        braceletGatt = gatt;

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i( TAG, "Disconnected from GATT server." );
                        braceletConnected = false;
                        startBLEScan(mode);
                        sendMQTTData("Bracelet vibrant " + name + " déconnecté" );
                        setInfo(tvMsgBracelet, "Bracelet déconnecté",false);
                        setStartButton ();
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.i( this.getClass().getName(), "onCharacteristicWrite " + characteristic.getUuid() );
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    Log.i( TAG, "onReliableWriteCompleted " );
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.i( TAG, "GATT onServicesDiscovered" );
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                Log.i( TAG, characteristic.getUuid().toString() );
                                if (characteristic.getUuid().toString().equals( lt716VibrateCharacteristic )) {
                                    Log.i( TAG, "Create characteristicLT716 = " + characteristic.getUuid().toString() );
                                    characteristicBracelet = characteristic;
                                    vibrateBracelet();
                                    setInfo(tvMsgBracelet, "Bracelet " + braceletGatt.getDevice().getName() + " connecté",true);
                                    braceletConnected = true;
                                    sendMQTTData("Bracelet vibrant " + name + " connecté" );
                                    stopBLEScan();
                                    setStartButton ();
                                }
                            }
                        }
                    }
                }
            };
    /**
     * **********************************************************************
     * BLE Scan
     * ***********************************************************************
     **/
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void startBLEScan(int mode) {

        //if (mode == modeMoniteurMQTT || mode == modeEleveMQTTAR1 || mode == modeEleveMQTTScreen) return;
        switch (mode) {
            case modeEleveAR1:
            case modeEleveAR1BraceletLT716:
            case modeEleveScreenBraceletLT716:
            case modeEleveAR1WearOS:
                break;

            case modeMoniteur:
            case modeEleveScreen:
            default:
                return;

        }


        Log.i(TAG, "startBLEScan : Starts BLE Scan...");

        try {
            stopScanRequested = false;
            bluetoothAdapterBLE.getBluetoothLeScanner().startScan( getLeScanCallback() );
        } catch (Exception e) {
            Log.e(TAG,e.toString());

        }


    }

    /**
     * **********************************************************************
     * BLE Scan Callback
     * ***********************************************************************
     **/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback getLeScanCallback(){

        return new ScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.i( TAG, "Remote device name: " + result.getDevice().getName() + " " + result.getDevice().getAddress());
                if ( stopScanRequested ) {
                    Log.i( TAG,"getLeScanCallback : stop BLE scan !" );
                    bluetoothAdapterBLE.getBluetoothLeScanner().stopScan( this );
                }
                /*
                    Lunettes AR1
                 */
                if (!TextUtils.isEmpty( result.getDevice().getName() ) && result.getDevice().getName().equals( ar1Name )) {
                    if (mode== modeEleveAR1 || mode == modeEleveAR1BraceletLT716  || mode == modeEleveAR1WearOS) {
                        BluetoothDevice device = result.getDevice();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            device.connectGatt( getApplicationContext(), true, lunettesGattCallback, BluetoothDevice.TRANSPORT_AUTO );
                        } else {
                            device.connectGatt( getApplicationContext(), true, lunettesGattCallback );
                        }
                    }
                }
                /*
                    Montre vibrante LT716
                 */
                if (!TextUtils.isEmpty( result.getDevice().getName() ) && result.getDevice().getName().equals( lt716Name ) ) {
                    if (mode== modeEleveScreenBraceletLT716 || mode == modeEleveAR1BraceletLT716) {
                        BluetoothDevice device = result.getDevice();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            device.connectGatt( getApplicationContext(), true, braceletGattCallback, BluetoothDevice.TRANSPORT_AUTO );
                        } else {
                            device.connectGatt( getApplicationContext(), true, braceletGattCallback );
                        }
                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

    }


    void stopBLEScan() {
        Log.i(TAG, "stopBLEScan. Begin braceletConnected=" + braceletConnected + " lunetteConnected=" + lunetteConnected + " stopScanRequested="+ stopScanRequested);
        stopScanRequested = false;
        if (mode == modeMoniteur) stopScanRequested = true;
        if (mode == modeEleveScreen) stopScanRequested = true;
        if (lunetteConnected && mode == modeEleveAR1) stopScanRequested = true;
        if (braceletConnected && mode == modeEleveScreenBraceletLT716) stopScanRequested = true;
        if (lunetteConnected && braceletConnected && mode == modeEleveAR1BraceletLT716) stopScanRequested = true;
        if (lunetteConnected && mode == modeEleveAR1WearOS) stopScanRequested = true;


        Log.i(TAG, "stopBLEScan. End braceletConnected=" + braceletConnected + " lunetteConnected=" + lunetteConnected + " stopScanRequested="+ stopScanRequested);
    }


        /**
         * **********************************************************************
         * sendMQTTData ()
         * ***********************************************************************
         **/
    void sendMQTTData(String dataToSend) {
        try {
            // Connection MQTT
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            // Formattage MQTT topic
            String topic = BuildConfig.APPLICATION_ID + "/" + reseau + "/send";
            // Formattage message (payload)
            message.setQos(0);  // une seule fois

            JSONObject payload = new JSONObject();
            payload.put("name", name);
            payload.put("data", dataToSend);
            payload.put("color", getResources().getColor(R.color.colorInformation));
            payload.put("color", color);
            payload.put("temporisation", tempo);
            payload.put("type", "lunettes" );
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
            e.printStackTrace();
        }
    }
    /**
     * **********************************************************************
     * getMode ()
     * ***********************************************************************
     **/
     int getMode() {
        int retMode;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        String mode = prefs.getString("mode", "0");
        String comm = prefs.getString("comm", "1");
        String lunettes = prefs.getString("lunettes", "3");
        String typeBracelet = prefs.getString("bracelet", "0");

        retMode = modeNotSupported;
        switch (mode) {
            case prefModeMoniteur: // Moniteur
                if (comMQTT.equals( comm )) {  // 4G/MQTT
                    retMode = modeMoniteur;
                }
                break;

            case prefModeEleve:  // Eleve
                switch (lunettes) {
                    case ar1Glass: // AR1
                        if (typeBracelet.equals( prefBraceletNone )) retMode = modeEleveAR1;
                        else if (typeBracelet.equals( prefBraceletLT717 )) retMode = modeEleveAR1BraceletLT716;
                        else retMode = modeEleveAR1WearOS;
                        break;

                    case androidScreen:
                        if (typeBracelet.equals( prefBraceletNone )) retMode = modeEleveScreen;
                        else if (typeBracelet.equals( prefBraceletLT717 )) retMode = modeEleveScreenBraceletLT716;
                        else retMode = modeEleveScreenWearOS;
                        break;
                    default :
                        break;
                }
                break;

            default :
                break;
        }
        Log.i(TAG,"Mode=" + retMode);
        return retMode;
    }

    /**
     * vibrateBracelet()
     */
    @SuppressLint("MissingPermission")
    public static void vibrateBracelet(){
        try {
            Log.i(TAG,"vibrateBracelet " + braceletGatt.getDevice() );
            characteristicBracelet.setValue( LT716VibrateValue);
            braceletGatt.writeCharacteristic( characteristicBracelet );
        } catch (NullPointerException e) {}

    }
}