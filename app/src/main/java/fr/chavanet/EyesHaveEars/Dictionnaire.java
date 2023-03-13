package fr.chavanet.EyesHaveEars;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Dictionnaire {
    private static final String TAG = "xavier/Dictionnaire";

    ArrayList<String> textToBeReplaced = new ArrayList<>();
    ArrayList<String> replacedText = new ArrayList<>();

    // Constructeur de la classe: récupération du dictionnaire from web
    public Dictionnaire(Context context){
        Log.i(TAG, "Dictionnaire:constructor...");

        textToBeReplaced.add( "Fresnes");
        replacedText.add( "freine");

        textToBeReplaced.add( "p\u00e9ter");
        replacedText.add( "P.T.U.");

        textToBeReplaced.add("humain");
        replacedText.add( "main");


        //textToBeReplaced.add("xavier");
        //replacedText.add( "jacques");

        // Lecture des paramètres locaux

        /*final SharedPreferences prefs = context.getSharedPreferences("dictionnaire", Context.MODE_PRIVATE);
        String dico = prefs.getString("json","[]");
        populateDicoArrayList(dico);

        // Lecture des paramètres partagés
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest request = new StringRequest(Request.Method.GET, "https://activite.chouetteenvol.tk/dico.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, response);
                populateDicoArrayList(response);
                final SharedPreferences.Editor ed = prefs.edit();
                ed.putString("json",response);
                ed.apply();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, String.valueOf(error));
            }
        });
        queue.add(request);*/
    }


    // Remplacement des textes
    public String replaceText(String text){
        int i = 0;
        for (Object t : textToBeReplaced ){
            text = text.replaceAll("(?i)" + t.toString(), replacedText.get(i));
            i++;
        }
        return text;
    }

    // Populate/fill textToBeReplaced & replacedText ArrayList
    void populateDicoArrayList (String jsonDico){
        textToBeReplaced = new ArrayList<>();
        replacedText = new ArrayList<>();
        try {
            JSONArray jsonarray = new JSONArray(jsonDico);
            for(int i=0; i < jsonarray.length(); i++) {
                JSONObject jsonobject = jsonarray.getJSONObject(i);
                String v0 = jsonobject.getString("v0");
                String v1 = jsonobject.getString("v1");
                textToBeReplaced.add(v0);
                replacedText.add(v1);
            }
            Log.i(TAG, String.valueOf(textToBeReplaced));
            Log.i(TAG, String.valueOf(replacedText));
        } catch(JSONException e) {
            Log.i(TAG, String.valueOf(e));
        }
    }
    // Populate/fill textToBeReplaced & replacedText ArrayList
    public List<String> getDictionnaireValues (){
        List<String> retList = new ArrayList<String>();
        int i = 0;
        for (Object t : textToBeReplaced ){
            retList.add("« "+t.toString() + " » \u279C « " + replacedText.get(i) + " »" );
            i++;
        }
        return retList;
    }

}
