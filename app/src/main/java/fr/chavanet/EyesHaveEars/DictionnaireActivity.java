package fr.chavanet.EyesHaveEars;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

//import com.example.EyesHaveEars.R;

public class DictionnaireActivity extends AppCompatActivity {

    ListView listView ;

    /***********************************************************************
     *  onCreate
     ***********************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("xavier", "DictionnaireActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_dictionnaire);

        // Dictionnaire
        Dictionnaire dico = new Dictionnaire(this);
        List<String> values = dico.getDictionnaireValues();

        // Liste de translation des mots
        listView = (ListView) findViewById(R.id.listDico);

        /*String[] values = new String[] { "Android List View",
                "Adapter implementation",
                "Simple List View In Android",
                "Create List View Android",
                "Android Example",
                "List View Source Code",
                "List View Array Adapter",
                "Android Example List View"
        };*/
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        listView.setAdapter(adapter);
        Log.i("xavier", "DictionnaireActivity listView");


    }

    /***********************************************************************
     *  onCreateOptionsMenu
     ***********************************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        Log.i("xavier", "DictionnaireActivity onCreateOptionsMenu");
        return true;
    }

    /***********************************************************************
     *  onSupportNavigateUp
     ***********************************************************************/
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
}
