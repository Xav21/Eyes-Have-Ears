package fr.chavanet.EyesHaveEars;

import android.os.Bundle;
import android.util.Log;

//import com.example.EyesHaveEars.R;
import com.rarepebble.colorpicker.ColorPreference;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

//public class SettingsActivity extends AppCompatActivity {
public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "xavier/Settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            //setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Log.i(TAG,"onCreatePreferences");
            setPreferencesFromResource(R.xml.preference, rootKey);

        }


        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            Log.i(TAG,"onDisplayPreferenceDialog");
            if (preference instanceof ColorPreference) {
                ((ColorPreference) preference).showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }



    }


}