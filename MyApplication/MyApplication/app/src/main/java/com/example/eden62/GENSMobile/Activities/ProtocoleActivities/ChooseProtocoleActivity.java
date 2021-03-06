package com.example.eden62.GENSMobile.Activities.ProtocoleActivities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eden62.GENSMobile.Activities.Historiques.Saisies.HistorySaisiesActivity;
import com.example.eden62.GENSMobile.Activities.ProtocoleActivities.RNF.ChooseTransectActivity;
import com.example.eden62.GENSMobile.Activities.ProtocoleActivities.RNF.RNFSite;
import com.example.eden62.GENSMobile.Database.SaisiesProtocoleDatabase.RNF.Transect;
import com.example.eden62.GENSMobile.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Activité permettant de choisir un protocole et de le réaliser ensuite. Permet également de consulter toutes ses saisies
 */
public class ChooseProtocoleActivity extends AppCompatActivity {

    protected Spinner protoSpinner,siteSpinner;
    protected static final String TOAST_MESSAGE = "Veuillez sélectionner un ";
    protected static final String PROTO_STRING = "protocole";
    protected static final String SITE_STRING = "site";
    protected List<Protocole> protocoles;
    protected List<Site> sites = new ArrayList<>();
    protected ArrayAdapter<Site> sitesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_protocole);

        setListOfProto();

        protoSpinner = (Spinner) findViewById(R.id.protocole_spinner);
        siteSpinner = (Spinner) findViewById(R.id.site_spinner);
        Button mesSaisies = (Button) findViewById(R.id.mesSaisies);

        mesSaisies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChooseProtocoleActivity.this, HistorySaisiesActivity.class));
            }
        });

        ArrayAdapter<Protocole> protoAdapter = new ArrayAdapter<Protocole>(this, android.R.layout.simple_spinner_item, protocoles){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if(convertView == null)
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_item,parent, false);
                TextView t = (TextView) convertView.findViewById(android.R.id.text1);

                Protocole p = getItem(position);
                t.setText(p.getName());

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if(convertView == null)
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_dropdown_item,parent, false);
                TextView t = (TextView) convertView.findViewById(android.R.id.text1);

                Protocole p = getItem(position);
                t.setText(p.getName());

                return convertView;
            }
        };

        setSiteAdapter(sites);

        protoSpinner.setAdapter(protoAdapter);
        Button validProto = (Button) findViewById(R.id.valider_proto);
        final LinearLayout siteContainer = (LinearLayout) findViewById(R.id.choixSiteContainer);

        protoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int visibility;
                if(isASelectedItem(protoSpinner)) {
                    setListOfSites();
                    visibility = View.VISIBLE;
                }
                else
                    visibility = View.GONE;
                siteContainer.setVisibility(visibility);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        validProto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(siteAndProtoAreChosen())
                    launchProto();
                else
                    actionWhenFieldsNotChosen();
            }
        });
    }

    // Définie l'adapter du spinenr représentant les sites
    private void setSiteAdapter(List<Site> sites){
        sitesAdapter = new ArrayAdapter<Site>(this, android.R.layout.simple_spinner_item, sites){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if(convertView == null)
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_item,parent, false);

                TextView t = (TextView) convertView.findViewById(android.R.id.text1);

                Site s = getItem(position);
                t.setText(s.getName());

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if(convertView == null)
                    convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_dropdown_item,parent, false);
                TextView t = (TextView) convertView.findViewById(android.R.id.text1);

                Site s = getItem(position);
                t.setText(s.getName());

                return convertView;
            }
        };
    }

    // Affecte les valeurs possible au spinner des sites
    private void setListOfSites() {
        sites = ((Protocole)protoSpinner.getSelectedItem()).getAvailableSites();
        setSiteAdapter(sites);
        siteSpinner.setAdapter(sitesAdapter);
    }

    /**
     * Affecte la liste des protocoles avec la liste de leurs sites disponibles
     */
    protected void setListOfProto(){
        protocoles = new ArrayList<>();

        ArrayList<Transect> transects = new ArrayList<>();
        transects.add(new Transect("Transect 1",100));
        transects.add(new Transect("Transect 2",103));
        transects.add(new Transect("Transect 3",102));
        transects.add(new Transect("Transect 4",103));
        transects.add(new Transect("Transect 5",100));

        ArrayList<Site> sites = new ArrayList<>();
        //Ajout d'un site de nom vide pour avoir l'item vide dans le spinner
        sites.add(new Site(""));
        sites.add(new RNFSite("Mont Pelé et Hulin",transects));

        //Ajout d'un protocole de nom vide pour avoir l'item vide dans le spinner
        protocoles.add(new Protocole("",null,null));
        protocoles.add(new Protocole(getString(R.string.nomRNF), sites, ChooseTransectActivity.class));
    }

    // Met une erreur sur l'élément selectionné du spinner s
    private void setErrorOnSelectedItem(Spinner s, String spinnerType){
        String msg = TOAST_MESSAGE + spinnerType;
        TextView selectedView = (TextView) s.getSelectedView();
        selectedView.setError(msg);
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();

    }

    // Action réalisé lorsque l'un des spinner n'a pas d'élément sélectionné
    private void actionWhenFieldsNotChosen() {
        if(!isASelectedItem(protoSpinner)){
            setErrorOnSelectedItem(protoSpinner, PROTO_STRING);
            return;
        }
        if(!isASelectedItem(siteSpinner))
            setErrorOnSelectedItem(siteSpinner, SITE_STRING);
    }

    /**
     * Lance l'activité protocolaire correspondante au protocole et site sélectionné
     */
    protected void launchProto(){
        Protocole selectedProto = (Protocole) protoSpinner.getSelectedItem();
        Intent intent = new Intent(this, selectedProto.getActivityToLaunch());
        if(selectedProto.getName().equals(getString(R.string.nomRNF))){
            RNFSite site = (RNFSite) siteSpinner.getSelectedItem();
            intent.putParcelableArrayListExtra("transects",site.getTransects());
            intent.putExtra("nomSite",site.getName());
        }
        startActivity(intent);
    }

    /**
     * Vérifie que le protocole et le site ont été choisis
     *
     * @return <code>True</code> s'ils ont été choisis, <code>false</code> sinon
     */
    protected boolean siteAndProtoAreChosen(){
        return isASelectedItem(protoSpinner) && isASelectedItem(siteSpinner);
    }

    /**
     * Vérifie si un item à été choisi dans le spinner s
     *
     * @param s Le spinner à vérifier
     * @return <code>True</code> si un élément à été choisi, <code>false</code> sinon
     */
    protected boolean isASelectedItem(Spinner s){
        return !s.getSelectedItem().toString().equals("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        protoSpinner.setSelection(0);
    }
}
