package fr.chavanet.EyesHaveEars;

class Constantes {

    // MQTT access
    public final static String defaultMQTTServername = "mqttservername";
    public final static String defaultMQTTServerPort = "8883";

    public final static String defaultMQTTUsername = "mqttuser";
    public final static String defaultMQTTPassword = "pswd";


    // Valeur des Prefs
    public final static String ar1Glass = "0";
    public final static String androidScreen = "1";

    public final static String comMQTT = "0";

    public final static String prefModeMoniteur = "0";
    public final static String prefModeEleve = "1";

    public final static String prefBraceletNone = "0";
    public final static String prefBraceletWearOS = "1";
    public final static String prefBraceletLT717 = "2";

    public final static String notificationChannelName = "Consignes moniteur";
    public final static String notificationChannelId = notificationChannelName;


    // Modes de fonctionnement
    public final static int modeNotSupported = -1;
    public final static int modeMoniteur = 1;
    public final static int modeEleveAR1 = 2;
    public final static int modeEleveScreen = 3;
    public final static int modeEleveAR1BraceletLT716 = 102;
    public final static int modeEleveScreenBraceletLT716 = 103;
    public final static int modeEleveAR1WearOS = 104;
    public final static int modeEleveScreenWearOS = 105;

    // EyeWear AR1 Bluetooth Low Energy name, services & characteristics
    public final static String ar1Name = "EyeWear";
    public final static String AR1Service = "0000fff0-0000-1000-8000-00805f9b34fb";
    public final static String[] ar1LignesCharacteristics = {
            "0000fff1-0000-1000-8000-00805f9b34fb",
            "0000fff2-0000-1000-8000-00805f9b34fb",
            "0000fff3-0000-1000-8000-00805f9b34fb",
            "0000fff4-0000-1000-8000-00805f9b34fb"
    };
    public static final long timeOut_AR1 = 120000; //(en millisecondes = 2mn Coupure automatique de la lunette)


    // Vibrations - LT716 Watch
    public final static String lt716Name = "LT716";
    public final static String lt716VibrateCharacteristic = "6e400002-b5a3-f393-e0a9-e50e24dcca9d";
    public final static byte [] LT716VibrateValue = {(byte)0xCD,(byte)0x00,(byte)0x06,(byte)0x12,(byte)0x01,(byte)0x0B,(byte)0x00,(byte)0x01,(byte)0x01};

}
