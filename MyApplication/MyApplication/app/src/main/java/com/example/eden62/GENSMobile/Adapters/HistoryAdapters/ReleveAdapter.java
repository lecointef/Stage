package com.example.eden62.GENSMobile.Adapters.HistoryAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.eden62.GENSMobile.Stocker.ReleveStocker;
import com.example.eden62.GENSMobile.Database.ReleveDatabase.Releve;
import com.example.eden62.GENSMobile.R;
import com.example.eden62.GENSMobile.Tools.Utils;

import java.util.List;
import java.util.Map;

/**
 * Adapter de relevés
 */
public class ReleveAdapter extends ItemsAdapter<ReleveStocker,Releve>{

    public ReleveAdapter(Context context, List<Releve> releves, Map exportedReleves) {
        super(context, releves);
        checkedItemsStocker = new ReleveStocker(context,exportedReleves);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_releves,parent, false);
        }

        ReleveViewHolder viewHolder = (ReleveViewHolder) convertView.getTag();
        if(viewHolder == null){
            viewHolder = new ReleveViewHolder();
            viewHolder.nom = (TextView) convertView.findViewById(R.id.nomRel);
            viewHolder.type = (TextView) convertView.findViewById(R.id.typeReleve);
            viewHolder.date = (TextView) convertView.findViewById(R.id.dateReleve);
            viewHolder.heure = (TextView) convertView.findViewById(R.id.heureReleve);
            viewHolder.image = (ImageView) convertView.findViewById(R.id.importStatusImage);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.itemCheckbox);
            allCheckBoxes.add(viewHolder.checkBox);

            convertView.setTag(viewHolder);
        }

        //getItem(position) va récupérer l'item [position] de la liste des relevés
        final Releve rel = getItem(position);

        //il ne reste plus qu'à remplir notre vue
        viewHolder.nom.setText(rel.getNom());
        viewHolder.type.setText(rel.getType());
        viewHolder.date.setText(Utils.printDateWithYearIn2Digit(rel.getDate()));
        viewHolder.heure.setText(rel.getHeure());

        // Affectation de la bonne image en fonction de l'état d'export du relevé
        if(rel.getImportStatus().equals("true"))
            viewHolder.image.setImageResource(R.drawable.check_oui);
        else
            viewHolder.image.setImageResource(R.drawable.to_sync);


        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    checkedItemsStocker.add(rel);
                } else{
                    checkedItemsStocker.remove(rel);
                }
            }
        });
        // Toujours affecter une valeur à toutes les view car la listView recycle les anciennes et peut alors atribuer une
        // mauvaise valeur (En cochant une case et scrollant, on se retrouve avec des lignes cochés alors qu'elles ne devrait pas)
        if(checkedItemsStocker.getCheckedItems().contains(rel))
            viewHolder.checkBox.setChecked(true);
        else
            viewHolder.checkBox.setChecked(false);
        return convertView;
    }

    /**
     * Représente une ligne de la listView
     */
    private class ReleveViewHolder {
        public TextView nom;
        public TextView type;
        public TextView date;
        public TextView heure;
        public ImageView image;
        public CheckBox checkBox;
    }
}
