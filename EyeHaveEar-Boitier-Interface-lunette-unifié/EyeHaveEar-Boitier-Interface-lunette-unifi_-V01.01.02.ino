/*
 * ========================
 * Interface Eye Have Ear 
 * ======================== 
 */
#define author "Xavier Chavanet"
#define version "01.01.02"
/* 
 *  
 * Hardware : 
 *      ESP32 avec Bluetooth Low Energy
 *      Module LORA 866 Mhz - Rx=GPIO16 - Tx=GPIO17
 *      
 * Selection usage boitier via GPIO22 & 23 :
 *       
 *                    +----    GPIO22   --------+
 *                    |      0     |      1     |
 *          ----------+------------+------------+
 *                  0 |     AR1    | Moniteur 1 |
 *           GPIO23 --+------------+------------+
 *                  1 | Moniteur 2 |   ORA-02   |
 *          ----------+------------+------------+
 *          
 * Bluetooth name (pas de mdp)
 *      Moniteur 1 : FFVL Moniteur 1
 *      Moniteur 2 : FFVL Moniteur 2
 *      ORA2       : HC-05
 *      
 * Lunette supportées     
 *      Lunette EyeWear AR1 service uid="0000fff0-0000-1000-8000-00805f9b34fb"
 *      Lunette Optivent ORA-02 Bluetooth server name = "HC-05"
 * 
 * Structure message LORA:
 *      Configuration => [CoNfIg:id:name:color:temporisation:]
 *          avec id= id moniteur; name=nom moniteur; temporisation=en seconde
 *          Exemple: [:CoNfIg:31:Xavier:-2008641025:5:]
 *      
 *      Message =>  [:id:texte:]
 *          exemple : [:12:il fait beau a Dijon:]
 *      
 * Historique
 *  2020-09-24  Xavier Chavanet   V01.01.02   Correction : Crash de l'application si la lunette n'est pas connectée.
 *                                            Mise en place d'un delai 
 *  2020-08-01  Xavier Chavanet   V01.01.01   Correction bug de transmission en mode Moniteur de Lora vers BT
 *  2020-06-30  Xavier Chavanet   V01.01.00   Ajout fonction boitier moniteur 1 & 2
 *  2020-06-17  Xavier Chavanet   V01.00.01   Ajout gestion time out AR1
 *  2020-06-01  Xavier Chavanet   V01.00.00   Version Initiale
 *  
 */
// Pins de sélection du mode 
#define pin22 22
#define pin23 23
int mode = 0; 
String libelleMode[] = {"AltitudeEyeWear AR1","Moniteur 1","Moniteur 2","Optivent ORA-2"};

// AR1 gestion mise en veille automatique au bout de 5mn
#define AR1KeepAliveTimer 540000 // 9 minutes 9*60*1000 millisecondes
int lastAR1WakeUpTime = millis();

// LORA library & object
#include <SoftwareSerial.h>
#define RXPin 16
#define TXPin 17
SoftwareSerial virtuelSerialLora(RXPin,TXPin); //RX/TX

// Bluetooth objects 
#include "BluetoothSerial.h"
String BTNames[] = {"n/a","FFVL Moniteur 1","FFVL Moniteur 2","HC-05"};
BluetoothSerial SerialBT;

// BLE library & objects pour Altitude EyeWear AR1
#include "BLEDevice.h"
static BLEAdvertisedDevice* myDevice;
static BLERemoteCharacteristic* pRemoteCharacteristic;
static BLERemoteCharacteristic* EyeWearLine01;
static BLERemoteCharacteristic* EyeWearLine02;
static BLERemoteCharacteristic* EyeWearLine03;
static BLERemoteCharacteristic* EyeWearLine04;

// The AR1 remote service 
static BLEUUID serviceUUID("0000fff0-0000-1000-8000-00805f9b34fb");

// The characteristic ligne 1 à 4 de l'ecran
static BLEUUID    EyeWearLine01UUID("0000fff1-0000-1000-8000-00805f9b34fb");
static BLEUUID    EyeWearLine02UUID("0000fff2-0000-1000-8000-00805f9b34fb");
static BLEUUID    EyeWearLine03UUID("0000fff3-0000-1000-8000-00805f9b34fb");
static BLEUUID    EyeWearLine04UUID("0000fff4-0000-1000-8000-00805f9b34fb");
static BLEUUID             charUUID("0000fff1-0000-1000-8000-00805f9b34fb");

// Séparateur des messages LORA
char   loraDataSeparator = ':';
char   loraWordSeparator = ' ';

// Durée d'affichage par défaut
int    displayDelay = 10000; //10 secondes

// Nombre max de caractère par ligne sur la lunette
int    maxLineSize = 9;

// Gestion cnx BLE
static boolean doConnect = false;
static boolean connected = false;
static boolean doScan = false;

// Variables
bool   dataDisplayed = false;
int    timerDisplay = millis();
String dataReceived = "";
String dataToSend = "";
int    timer = millis();
boolean debug = true;


/*************************************************************************************
 * Setup
 ************************************************************************************* 
 */
 void setup() {
  Serial.begin(115200);

  pinMode(pin22, INPUT);
  pinMode(pin23, INPUT);
  
  if (digitalRead(pin22) == HIGH && digitalRead(pin23) == HIGH) {
      // ORA-2
      mode = 3;
  } else if (digitalRead(pin22) == LOW && digitalRead(pin23) == LOW) {
      // AR1
      mode = 0;
  } else if (digitalRead(pin22) == HIGH && digitalRead(pin23) == LOW) {
      // Moniteur 1 
      mode = 1;
  } else {
      // Moniteur 2
      mode = 2;
  }
  mode = 3;
  
  Serial.println("============================");
  Serial.println("==   ESP32 / EyeHaveEar   ==");
  Serial.println("============================");
  Serial.print("Sketch : ");
  String the_path = __FILE__;
  int slash_loc = the_path.lastIndexOf('\\');
  String the_cpp_name = the_path.substring(slash_loc+1);
  //int dot_loc = the_cpp_name.lastIndexOf('.');
  //String the_sketchname = the_cpp_name.substring(0, dot_loc);
  Serial.println(the_cpp_name);
  Serial.print("Date compilation : ");
  Serial.println(String(__DATE__) + " " + String(__TIME__));
  Serial.print("Auteur : ");
  Serial.println(author);
  Serial.print("Version : ");
  Serial.println(version);
  Serial.println("============================");
  Serial.print("     ");
  Serial.println(libelleMode[mode]);
  Serial.println("============================");

  switch (mode) {
    case 0: 
        setupORA2();
        setupAR1();
        break;
    case 1: 
        setupMoniteur();
        break;
    case 2:
        setupMoniteur();
        break;
    case 3:     
        setupORA2();
        break;
  }
  
  if (debug) {Serial.println("Démarrage terminé !");}
  
} // End of setup.

/*
 * ******************************************************************************************
 * Main loop
 * ******************************************************************************************
 */
void loop() {
  switch (mode) {
    case 0: 
        loopAR1();
        delay (25);
        break;
    case 1: 
        loopMoniteur();
        break;
    case 2:
        loopMoniteur();
        break;
    case 3:     
        loopORA2();
        break;
  }
  
} // End of loop


/*
 ***********************************************************************************************************************************************************   
 * Altitude Eyewear AR1
 ************************************************************************************************************************************************************    
 */
//************************************************************************************************************************************************************
//
//======================================================================================
//  Gestion du BLE
//======================================================================================
static void notifyCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify) {
    Serial.print("Notify callback for characteristic ");
    Serial.print(pBLERemoteCharacteristic->getUUID().toString().c_str());
    Serial.print(" of data length ");
    Serial.println(length);
    Serial.print("data: ");
    Serial.println((char*)pData);
}

class MyClientCallback : public BLEClientCallbacks {
  void onConnect(BLEClient* pclient) {
  }

  void onDisconnect(BLEClient* pclient) {
    connected = false;
    Serial.println("onDisconnect");
  }
};

bool connectToServer() {
    Serial.print("Connection to AR1 @");
    Serial.println(myDevice->getAddress().toString().c_str());
    
    BLEClient*  pClient  = BLEDevice::createClient();
    Serial.println(" - Client crée");

    pClient->setClientCallbacks(new MyClientCallback());

    // Connect to the remove BLE Server.
    pClient->connect(myDevice);  // if you pass BLEAdvertisedDevice instead of address, it will be recognized type of peer device address (public or private)
    Serial.println(" - Connecté au server BLE");

    // Obtain a reference to the service we are after in the remote BLE server.
    BLERemoteService* pRemoteService = pClient->getService(serviceUUID);
    if (pRemoteService == nullptr) {
      Serial.print("Failed to find our service UUID: ");
      Serial.println(serviceUUID.toString().c_str());
      pClient->disconnect();
      return false;
    }
    Serial.println(" - services BLE/AR1 trouvés");

    // Obtain a reference to the characteristic in the service of the remote BLE server.
    pRemoteCharacteristic = pRemoteService->getCharacteristic(charUUID);
    if (pRemoteCharacteristic == nullptr) {
      Serial.print("Failed to find our characteristic UUID: ");
      Serial.println(charUUID.toString().c_str());
      pClient->disconnect();
      return false;
    }

    // EyeWear ligne 1 à 4
    EyeWearLine01 = pRemoteService->getCharacteristic(EyeWearLine01UUID);
    if (EyeWearLine01 == nullptr) {
      Serial.print("Failed to find our characteristic UUID: ");
      Serial.println(EyeWearLine01UUID.toString().c_str());
      pClient->disconnect();
      return false;
    }
    EyeWearLine02 = pRemoteService->getCharacteristic(EyeWearLine02UUID);
    if (EyeWearLine02 == nullptr) {
      Serial.print("Failed to find our characteristic UUID: ");
      Serial.println(EyeWearLine02UUID.toString().c_str());
      pClient->disconnect();
      return false;
    }
        EyeWearLine03 = pRemoteService->getCharacteristic(EyeWearLine03UUID);
    if (EyeWearLine03 == nullptr) {
      Serial.print("Failed to find our characteristic UUID: ");
      Serial.println(EyeWearLine03UUID.toString().c_str());
      pClient->disconnect();
      return false;
    }
        EyeWearLine04 = pRemoteService->getCharacteristic(EyeWearLine04UUID);
    if (EyeWearLine04 == nullptr) {
      Serial.print("Failed to find our characteristic UUID: ");
      Serial.println(EyeWearLine04UUID.toString().c_str());
      pClient->disconnect();
      return false;
    }
    Serial.println(" - Caractéristiques prévues trouvées sur la paire de lunettes AR1");
 

    // Read the value of the characteristic.
    if(pRemoteCharacteristic->canRead()) {
      std::string value = pRemoteCharacteristic->readValue();
      Serial.print("The characteristic value was: ");
      Serial.println(value.c_str());
    }

    if(pRemoteCharacteristic->canNotify())
      pRemoteCharacteristic->registerForNotify(notifyCallback);

    connected = true;
}
/**
 * Scan for BLE servers and find the first one that advertises the service we are looking for.
 */
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
 /**
   * Called for each advertising BLE server.
   */
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    Serial.print("BLE Advertised Device found: ");
    Serial.println(advertisedDevice.toString().c_str());

    // We have found a device, let us now see if it contains the service we are looking for.
    if (advertisedDevice.haveServiceUUID() && advertisedDevice.isAdvertisingService(serviceUUID)) {

      BLEDevice::getScan()->stop();
      myDevice = new BLEAdvertisedDevice(advertisedDevice);
      doConnect = true;
      doScan = true;

    } // Found our server
  } // onResult
}; // MyAdvertisedDeviceCallbacks

//========================================================
// Envoi des données sur la lunette 
//========================================================
void sendDataToEyeWear (String fullDataToSend){
    String mot;
    String ligne;
    String txt;
    
    // Isolation du texte
    dataToSend = split (fullDataToSend,loraDataSeparator, 2, 1024);

    // Nettoyage et formattage du texte
    dataToSend.trim();
    dataToSend.replace ("  "," ");
    dataToSend.replace ("-"," ");   // pour maximaliser l'affichage
    dataToSend = removeAccents (dataToSend); 
    //dataToSend.toUpperCase(); 

    // Construction des 4 lignes à envoyer à la lunette
    int i = 0;
    int nbligne = 0;
    mot = split (dataToSend,loraWordSeparator, i, maxLineSize);   
    while (mot != "" ){        
        if (i == 0) {txt = mot;} else {txt = ligne + " " + mot;}
        if (txt.length() > maxLineSize) {
            displayLine (ligne, nbligne);
            ligne = mot;
            txt = "";
            nbligne ++;
        }
        else {
            if (i != 0) {ligne += " ";}
            ligne += mot;
        }  
      i++;
      mot = split (dataToSend,loraWordSeparator, i, maxLineSize);       
    }

    // Affichage du restant
    if (ligne != "" ) {displayLine (ligne, nbligne);}

    //Effacement des lignes non actives si non déjà fait.
    if (dataDisplayed) {
      for (int j=nbligne+1; j<4; j++ ){
        displayLine (" ", j);
        }
     }
    
    // Initialisation timer d'effacement des données sur la lunette (paramètre CoNfIg)
    //================================================================================
    dataDisplayed = true;
    timerDisplay = millis();
}
//
//========================================================
// Display ligne
//========================================================
void displayLine (String ligne, int nbligne) {
  
    // Mémorisation heure dernier affichage
    lastAR1WakeUpTime = millis();
    
    // Affichage des 4 lignes Les autres sont ignorées.
    if (nbligne ==0) {EyeWearLine01->writeValue(ligne.c_str(),ligne.length());}
    if (nbligne ==1) {EyeWearLine02->writeValue(ligne.c_str(),ligne.length());}
    if (nbligne ==2) {EyeWearLine03->writeValue(ligne.c_str(),ligne.length());}
    if (nbligne ==3) {EyeWearLine04->writeValue(ligne.c_str(),ligne.length());}

    // Debug
    if (debug) {
        Serial.print("ligne ");
        Serial.print(nbligne);
        Serial.print(" >>");
        Serial.print(ligne);
        Serial.print("<< len=");      
        Serial.println(ligne.length()  );      
    }
}
//========================================================
// split 
//========================================================
String split (String data, char separator, int index, int maxLen)
{
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    if ((strIndex[1] - strIndex[0]) > maxLen ) {strIndex[1] = strIndex[0] + maxLen; }
    
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}

//========================================================
// removeAccents 
//========================================================
String removeAccents (String data) {
    data.replace ("à","a");
    data.replace ("œ","oe");
    data.replace ("ç","c");
    data.replace ("é","e");
    data.replace ("è","e");
    data.replace ("ë","e");
    data.replace ("ê","e");
    data.replace ("î","i");
    data.replace ("ï","i");
    data.replace ("ô","o");
    data.replace ("ù","u");
    return data;
}

//========================================================
// setupAR1 
//========================================================
void setupAR1() {
// ---------------------------------------------------------------------
// Initialisation BLE
// ---------------------------------------------------------------------
  
  Serial.println("Initialisation BLE ...");
  BLEDevice::init("");

  // Retrieve a Scanner and set the callback we want to use to be informed when we
  // have detected a new device.  Specify that we want active scanning and start the
  // scan to run for 5 seconds.
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setInterval(1000);
  pBLEScan->setWindow(850);
  pBLEScan->setActiveScan(true);
  pBLEScan->start(5, false);

// Initialisation Lora
  virtuelSerialLora.begin(19200);
}
//========================================================
// loopAR1 
//========================================================
void loopAR1() {
  
  // If the flag "doConnect" is true then we have scanned for and found the desired
  // BLE Server with which we wish to connect.  Now we connect to it.  Once we are 
  // connected we set the connected flag to be true.
  //================================================================================
  if (doConnect == true) {
    if (connectToServer()) {
      Serial.println("Lunettes AR1 connectées via Bluetooth Low Energy.");
      dataToSend = "::EyeWear connected to FFVL V" + String(version) + ":";
      sendDataToEyeWear (dataToSend);
    } else {
      Serial.println("We have failed to connect to the server.");
    }
      doConnect = false;
  }

  // Si pas connecté, recherche lunette par scan BLE
  //================================================
  //  if (!connected and doScan){
  if (!connected){
        //BLEDevice::getScan()->start(0);  // this is just example to start scan after disconnect, most likely there is better way to do it in arduino
        BLEDevice::getScan()->start(5, false);
  }
  
 // Lecture des données sur LORA, formatage et envoi sur le BLE
 //============================================================
 //
  if (virtuelSerialLora.available()) {
    dataReceived = "";
    String str;
    unsigned long starttime = millis();
    
    str = (char)virtuelSerialLora.read();    
    if (debug) {
      Serial.print("<<");  
      Serial.print(str);  
    }
    dataReceived += str;    
    // Lecture jusqu'à la fin de texte ']' ou un time out de 200ms  
    while (str != "]" && ((millis() - starttime) < 5000) ){
        if (virtuelSerialLora.available()) {      
            str = (char)virtuelSerialLora.read();      
            dataReceived += str;      
            Serial.print(str);  
        }
    }
    if (debug) {Serial.println("");}  

    //dataReceived = Serial.readString();
    if (debug) {
      Serial.print("Data received from LORA : ");
      Serial.println(dataReceived);
    }
    
    // Si connecté et message bien formatté, envoi vers la lunette EyeWear
    if (connected && str == "]") {    
        String txt;
        txt = split (dataReceived,loraDataSeparator, 1, 1024);   

        // Message de configuration
        //==========================
        if (txt == "CoNfIg") {
          // Structure =>  [:CoNfIg:id:name;color:tempoSeconde:]
          displayDelay = split(dataReceived,loraDataSeparator, 5, maxLineSize).toInt() * 1000;
          dataToSend = "::" + split(dataReceived,loraDataSeparator, 3, maxLineSize) + " en ligne"; 
          sendDataToEyeWear (dataToSend);
          virtuelSerialLora.write(dataReceived.c_str());
        }

        // Message 
        //========
        else {
          // Decodage et envoi
          sendDataToEyeWear (dataReceived);
          // LORA acknowledge
          virtuelSerialLora.write(dataReceived.c_str());
          if (debug) {
            Serial.print(">>");
            Serial.println(dataReceived);
          }
        }
    }
 }

 // Effacement des consignes sur la lunette au bout du timeout
 //================================================================
  if (connected && dataDisplayed && (timerDisplay + displayDelay) < millis() ) {
      dataDisplayed = false;
      dataToSend = "        ";
      EyeWearLine01->writeValue(dataToSend.c_str(),dataToSend.length());
      EyeWearLine02->writeValue(dataToSend.c_str(),dataToSend.length());
      EyeWearLine03->writeValue(dataToSend.c_str(),dataToSend.length());
      EyeWearLine04->writeValue(dataToSend.c_str(),dataToSend.length());
      if (debug) {
        Serial.println("Ecran effacé.");
      }   
  }

 // Prevent AR1 form sleeping
 //===========================
  if (connected && (lastAR1WakeUpTime + AR1KeepAliveTimer) < millis() ) {
      dataToSend = ".";
      EyeWearLine01->writeValue(dataToSend.c_str(),dataToSend.length());
      dataDisplayed = true;
      timerDisplay = millis();
      lastAR1WakeUpTime = millis();
      
      if (debug) {
        Serial.println("AR1 Wake Up.");
      }   
  }
}

/*
 ***********************************************************************************************************************************************************
 *    Optivent ORA-2
 ************************************************************************************************************************************************************
 */
//========================================================
// setupORA2
//========================================================
void setupORA2() {
  virtuelSerialLora.begin(19200);
  SerialBT.begin(BTNames[mode]); 
}

//========================================================
// loopORA2
//   Transfert LORA <==> Bluetooth
//========================================================
void loopORA2() {
  /* while (virtuelSerialLora.available()){
  //if(virtuelSerialLora.available()){
    SerialBT.write(virtuelSerialLora.read());
  }*/
  //----------------------
// Lora ==> Bluetooth
//----------------------
 if (virtuelSerialLora.available()) {
      dataReceived = "";
      String str;
      unsigned long starttime = millis();
      str = (char)virtuelSerialLora.read();       
      dataReceived += str;    
      // Lecture jusqu'à la fin de texte ']' ou un time out de 50ms  
      while (str != "]" && ((millis() - starttime) < 50) ){
          if (virtuelSerialLora.available()) {      
              str = (char)virtuelSerialLora.read();      
              dataReceived += str;      
          }
      }
    Serial.println("");      
    // On envoi la trame.
    for (int i = 0; i < dataReceived.length(); i++)
      {
        SerialBT.write(dataReceived[i]);   // Push each char 1 by 1 on each loop pass
      }
 }
//----------------------
// Bluetooth ==> Lora 
//----------------------
  while (SerialBT.available()){;      
    virtuelSerialLora.write(SerialBT.read());
    delay(2);   // V01.01.02 améliore la qualité des données transmises. 
  }
  
}

/*
 ***********************************************************************************************************************************************************
 *    Moniteur
 ************************************************************************************************************************************************************
 */
//========================================================
// setupMoniteur
//========================================================
void setupMoniteur() {
  virtuelSerialLora.begin(19200);
  SerialBT.begin(BTNames[mode]); 
}

//========================================================
// loopMoniteur
//   Transfert LORA <==> Bluetooth
//========================================================
void loopMoniteur() {

//----------------------
// Lora ==> Bluetooth
//----------------------
  
  //while (virtuelSerialLora.available()){
  //  SerialBT.write(virtuelSerialLora.read());
  //}

 if (virtuelSerialLora.available()) {
      dataReceived = "";
      String str;
      unsigned long starttime = millis();
      str = (char)virtuelSerialLora.read();      
      dataReceived += str;    
      // Lecture jusqu'à la fin de texte ']' ou un time out de 50ms  
      while (str != "]" && ((millis() - starttime) < 50) ){
          if (virtuelSerialLora.available()) {      
              str = (char)virtuelSerialLora.read();      
              dataReceived += str;       
          }
      }
    
    // On envoi la trame.
    for (int i = 0; i < dataReceived.length(); i++)
      {
        SerialBT.write(dataReceived[i]);   // Push each char 1 by 1 on each loop pass
      }
 }

//----------------------
// Bluetooth ==> Lora 
//----------------------
  while (SerialBT.available()){
    virtuelSerialLora.write(SerialBT.read());
  }
  
//----------------------
// On soufle un peu
//----------------------
  delay (50);
  
}
