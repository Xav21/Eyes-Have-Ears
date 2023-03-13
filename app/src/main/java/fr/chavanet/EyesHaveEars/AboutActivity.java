package fr.chavanet.EyesHaveEars;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

//import com.example.EyesHaveEars.BuildConfig;
//import com.example.EyesHaveEars.R;

import java.util.Date;
import fr.chavanet.EyesHaveEars.*;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.about_activity);

        TextView tvAbout;
        tvAbout = findViewById(R.id.about);
        tvAbout.setMovementMethod(new ScrollingMovementMethod());

        String versionName = BuildConfig.VERSION_NAME;
        String application_Id = BuildConfig.APPLICATION_ID;
        Date buildDate = new Date(BuildConfig.TIMESTAMP);

        String strAbout = "<h1>Eyes have Ears</h1>" +
                "<p><small>" + application_Id + " version " + versionName + "<br>" + buildDate + "</small><p>" +
                "<br>" +
                "<p>Cette application est conçue pour faciliter le guidage des élèves pilotes de deltaplane ou de parapente lors des séances de pente école ou " +
                "de grand vol avec les limites indiquées ci-dessous.<p>" +
                "<br>" +
                "<b>Manuel utilisateur : </b>https://iot.chavanet.fr/fr.chavanet.EyesHaveEars/manuel-utilisateur.pdf" +
                "<br><p>____________________________________________</p>" +
                "<h4>Equipements requis pour chaque utilisateur<h4>" +
                "&nbsp<li>&nbsp un téléphone Android 7.0 (Lollipop) ou supérieur</b> ;</li>" +
                "&nbsp<li>&nbsp une <b>connexion internet de qualité</b> ;</li>" +
                "&nbsp<li>&nbsp en option l'élève peut être équipé d'une paire de lunettes <b>Altidude EyeWear AR1</b> <br>https://sport.altitude-eyewear.net/infinity-la-lunette-connectee/.</li>" +
                "<br>" +
                "<p>La <b>FFVL</b> peut prêter une paire de lunettes sous réserve de disponibilité.</p>" +
                "<p>____________________________________________</p>" +
                "<h4>Limites d'utilisation</h4>" +
                "<p>Le moniteur ou les personnes mettant en œuvre la solution <b>Eyes Have Ears</b> doivent être conscients des <b>limites techniques</b>" +
                " notamment celles basées sur des connexions 3/4/5G. " + "" +
                "Elles peuvent être interrompues à tout moment ou être de mauvaise qualité selon votre contexte géographique. </p>" +
                "<p>Le système Android peut, dans certains cas, limiter l'usage de cette application comme par exemple un système d'économie d'énergie. </p>" +
                "<br>" +
                "<p>Une vérification de fonctionnement dans votre contexte d'utilisation est indispensable. L'élève ou le pilote guidé doit être familiarisé sur l'utilisation de l'application. " +
                "<font color=\"red\">Dans tous les cas, une procédure de guidage de secours doit être mise en place afin de palier à toute défaillance technique.</font></p>" +
                "<br>" +
                "<p>En cas de doute, contactez les concepteurs avant toute utilisation pouvant exposer des personnes à des risques corporels. </p>" +
                "<p>____________________________________________</p>" +
                "<h4>Droits d'accès & vie privée</h4>" +
                "<p>https://iot.chavanet.fr/fr.chavanet.EyesHaveEars/privacy-policy.html</p>" +
                "<br><p>Cette application requiert les autorisations :</p>" +
                "<li>&nbsp INTERNET & ACCESS_NETWORK_STATE pour accéder au réseau de guidage privé en 3/4/5G;</li>" +
                "<li>&nbsp BLUETOOTH & ACCESS_FINE_LOCATION pour piloter la paire de lunettes AR1 en Bluetooth Low Energy ;</li>" +
                "<li>&nbsp VIBRATE pour permettre la vibration sur la réception de consignes ;</li>" +
                "<li>&nbsp RECORD_AUDIO pour enregistrer les consignes du moniteur ;</li>" +
                "<li>&nbsp WAKE_LOCK pour empêcher l'écran de s'éteindre en cours d'utilisation.</li>" +
                "<br>" +
                "<p>Les préférences de l'utilisateur sont stockées dans le téléphone et " +
                "aucune donnée personnelle n'est transmise via internet hormis les consignes sur le réseau de vol.</p>" +
                "<p>____________________________________________</p>" +
                "<h4>Licence d'utilisation</h4>" +
                "<p>L'application est livrée sans garantie de fonctionnement <b>« as is »</b> conformément à la licence d'utilisation.</p>" +
                "<p>Elle est publiée sous la licence <b>G.N.U.</a></b></p>" +
                "<p><b>GNU GENERAL PUBLIC LICENSE</b></p>" +
                "<p><b>Version 3 - 29 June 2007</b></p>" +
                "<p>https://www.gnu.org/licenses/gpl-3.0.fr.html</p>"
                ;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvAbout.setText(Html.fromHtml(strAbout, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvAbout.setText(Html.fromHtml(strAbout));
        }
    }

}