package fr.chavanet.EyesHaveEars;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import static fr.chavanet.EyesHaveEars.Constantes.ar1Glass;
import static fr.chavanet.EyesHaveEars.Constantes.androidScreen;

class CloneGlassesScreen extends AppCompatActivity {
    private static final String TAG = "xavier/cloneAR1";

    private ArrayList<TextView> lignes;

    private int AR1MaxLineSize = 9;
    private long millis ;
    private boolean screenCleared = true;
    private boolean stopThread = false;
    private int displayConsigneDuration;
    private ProgressBar spinner;
    private List<ConsignesModel> listConsignes;
    private ConsignesAdaptater itemAdaptater;
    private TextView fullViewConsigne;
    private String lunettesType;

    public CloneGlassesScreen( String typeGlasses,
                            TextView fullViewById,
                            TextView viewById,
                            TextView viewById1,
                            TextView viewById2,
                            TextView viewById3,
                            ProgressBar progressBar,
                            List<ConsignesModel> list,
                            ConsignesAdaptater adaptater) {
        Log.i(TAG, "constructor CloneGlassesScreen");
        lignes = new ArrayList<>();
        lignes.add(0, viewById);
        lignes.add(1, viewById1);
        lignes.add(2, viewById2);
        lignes.add(3, viewById3);
        listConsignes = list;
        itemAdaptater = adaptater;
        fullViewConsigne = fullViewById;

        millis = System.currentTimeMillis() + (10000);
        screenCleared = true;
        startClearScreenThread();
        spinner = progressBar;

        // Get glasses type
        lunettesType = typeGlasses;
    }


    void displayConsigne(String consigne, int timout){

        displayConsigneDuration = timout;

        switch (lunettesType) {
            /*******************
             *   AR1
             *******************/
            case ar1Glass: {
                // Formattage de la consigne pour la lunette
                String localConsigne;
                localConsigne = consigne.trim();
                localConsigne = localConsigne.replace("  ", " ");
                localConsigne = localConsigne.replace("-", " ");
                localConsigne = localConsigne.toLowerCase();
                localConsigne = ConsignesActivityEleve.removeAccents(localConsigne);

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
                        if (nbMots == 1) {
                            displayLigne(txt, nbligne);
                            nbMots = 0;
                        } else {
                            displayLigne(ligne.toString(), nbligne);

                            ligne = new StringBuilder(mot[i]);
                            nbMots = 1;
                        }
                        nbligne++;
                    } else {
                        if (i != 0) {
                            ligne.append(" ");
                        }
                        ligne.append(mot[i]);
                    }
                }
                // Ecriture du restant
                displayLigne(ligne.toString(), nbligne);

                // Effacement des lignes non utilisées
                for (int j = nbligne + 1; j < 4; j++) {
                    lignes.get(j).setText("");
                }
                break;
            }
            /*************************
             *   ORA-2 ou smartphone
             *************************/
            case androidScreen:{
                    fullViewConsigne.setText(consigne);
            }
        }


        // Timer effecement consigne
        millis = System.currentTimeMillis() + (timout * 1000);
        spinner.setVisibility(View.VISIBLE);
        screenCleared = false;

    }

    void clearProgressBar(){
        Log.i(TAG,"clearProgressBar");
        spinner.setVisibility(View.GONE);
        if (listConsignes.size() != 0) {
            ConsignesModel item = listConsignes.get(0);
            item.setProgresBar(View.INVISIBLE);
            itemAdaptater.notifyDataSetChanged();
        }

        millis = System.currentTimeMillis() + (displayConsigneDuration * 1000);
        Log.i(TAG,"ackReceived timeoutAR1="+ displayConsigneDuration);
        screenCleared = false;
    }

    private void displayLigne (String ligne, int nbligne) {
        if (nbligne<4) {
            int nbCar = Math.min(ligne.length(), AR1MaxLineSize);
            ligne = ligne.substring(0, nbCar);
            lignes.get(nbligne).setText(ligne);
        }
    }

    void cleanUp(){
        stopThread = true;
    }

    private void startClearScreenThread(){
        new Thread(new clearScreenTimer()).start();
    }

    private class clearScreenTimer implements Runnable {
        long lastTimeScreenCleared = System.currentTimeMillis() + displayConsigneDuration;
        public void run() {
            while (!stopThread) {
                // Effacement de l'écran après affichage d'une consigne
                if (!screenCleared && System.currentTimeMillis() > millis) {
                    Log.i(TAG, "Clear cloned screen");
                    screenCleared = true;
                    lastTimeScreenCleared = System.currentTimeMillis();

                    // Effacement des lignes non utilisées
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (lunettesType) {
                                //AR1
                                case ar1Glass: {
                                    for (int j = 0; j < 4; j++) {
                                        lignes.get(j).setText("");
                                    }
                                    break;
                                }
                                //ORA-2 ou smartphone
                                case androidScreen:
                                    {
                                    fullViewConsigne.setText("");
                                    break;
                                }                            }
                            spinner.setVisibility(View.GONE);
                            if (listConsignes.size() != 0) {
                                ConsignesModel item = listConsignes.get(0);
                                if (item.getProgresBar() == View.VISIBLE) {
                                    item.setProgresBar(View.INVISIBLE);
                                    item.setNokIcon(View.VISIBLE);
                                    itemAdaptater.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
