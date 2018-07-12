package com.example.eden62.GENSMobile.Activities.MapsActivities.Releve;

import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.eden62.GENSMobile.Activities.Historiques.Releves.HistoryReleveActivity;
import com.example.eden62.GENSMobile.Activities.MapsActivities.MainActivity;
import com.example.eden62.GENSMobile.Database.ReleveDatabase.Releve;
import com.example.eden62.GENSMobile.R;
import com.example.eden62.GENSMobile.Tools.Utils;
import com.example.eden62.GENSMobile.Tools.XY;
import com.google.gson.Gson;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Activité carte pour les relevés de terrain
 */
public class MainActivityRel extends MainActivity {

    protected ImageButton lineButton, polygonButton, pointButton;

    protected Button finReleve, mesReleve;

    protected Releve releveToAdd;

    protected Handler handler;
    protected Runnable pointsTaker;

    protected Polyline polyline;
    protected double lineLength;
    protected double polygonPerimeter, polygonArea;
    protected Polygon polygon;
    protected List<LatLong> latLongs;
    protected LatLong lastMarkerPosition;
    protected Layer previousLayer;

    protected int currentReleve;
    protected final int POLYGON = 2;
    protected final int LINE = 1;
    protected final int NO_RELEVE = 0;

    protected PointsTakerService mService;
    protected boolean mBound = false;

    protected Paint paintFill = Utils.createPaint(
            AndroidGraphicFactory.INSTANCE.createColor(Color.GREEN), 2,
            Style.FILL);
    protected Paint paintStroke = Utils.createPaint(
            AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK), 2,
            Style.STROKE);
    public static final int PAINT_STROKE = AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE);

    private static final int BOITE_FIN_RELEVE = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("TOTO","TEST");

        handler = new Handler();

        lineButton = (ImageButton) findViewById(R.id.bouton_releve_ligne);
        polygonButton = (ImageButton) findViewById(R.id.bouton_releve_polygone);
        pointButton = (ImageButton) findViewById(R.id.bouton_releve_point);
        finReleve = (Button) findViewById(R.id.finReleve);
        mesReleve = (Button) findViewById(R.id.mesReleves);

        mesReleve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivityRel.this,HistoryReleveActivity.class);
                startActivity(intent);
            }
        });

        View.OnClickListener stopListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAvertissementDialog(BOITE_FIN_RELEVE).show();
            }
        };

        View.OnClickListener startLineListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(noReleveInProgress()) {
                    clearLayers();
                    setCurrentReleve(LINE);
                    startLine();
                    finReleve.setVisibility(View.VISIBLE);
                }
            }
        };

        View.OnClickListener startPolygonListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(noReleveInProgress()) {
                    clearLayers();
                    setCurrentReleve(POLYGON);
                    startPolygon();
                    finReleve.setVisibility(View.VISIBLE);
                }
            }
        };

        lineButton.setOnClickListener(startLineListener);
        polygonButton.setOnClickListener(startPolygonListener);
        finReleve.setOnClickListener(stopListener);
        pointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearLayers();
                Marker marker = Utils.createMarker(MainActivityRel.this,R.drawable.marker_green,getUsrLatLong());
                lastMarkerPosition = marker.getLatLong();
                addLayer(marker);
                finirReleve();
            }
        });

    }

    //Reset les relevés de terrain
    private void clearLayers(){
        try {
            myMap.getLayers().remove(previousLayer);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Convertit une liste de LatLongs en une liste de position en Lambert
     * @param latLongs La liste à convertir
     * @return La liste convertie
     */
    protected List<XY> convertLatLongsListToL93List(List<LatLong> latLongs){
        List<XY> coordList = new ArrayList<>();

        for(LatLong latLong : latLongs){
            coordList.add(Utils.convertWgs84ToL93(latLong));
        }

        return coordList;
    }

    /**
     * Récupère la surface d'un polygone
     * @param latLongs La liste des positions du polygone
     * @return la surface du polygone
     */
    //Fonctionne mais attention, le point d'origine doit également se retrouver à la fin de la liste
    protected double getArea(List<LatLong> latLongs){
        // transformer chaque latLong en L93
        List<XY> listFormatedCoord = convertLatLongsListToL93List(latLongs);

        int nbCoord = listFormatedCoord.size();
        Iterator<XY> it = listFormatedCoord.iterator();

        if(nbCoord < 3) {
            return 0;
        }

        XY pp = new XY();
        XY cp = it.next();
        XY np = it.next();
        double x0 = cp.x;
        np.x -= x0;
        double sum = 0;

        while(it.hasNext()){
            pp.y = cp.y;
            cp.x = np.x;
            cp.y = np.y;
            np = it.next();
            np.x -= x0;
            sum += cp.x * (np.y - pp.y);
        }
        return Math.abs(-sum /2);
    }

    // Renvoi le releve à insérer dans la base
    private Releve createReleveToInsert(){
        long creatorId = Utils.getCurrUsrId(this);
        String latitude;
        String longitude;
        String formatedLatLongs = "";
        if(currentReleveIsPolygon() ||currentReleveIsLine()){
            StringTuple tuple = getStringsFromLatLongs();
            latitude = tuple.fst();
            longitude = tuple.snd();
            formatedLatLongs = getFormatedLatLongs();
        } else{
            latitude = lastMarkerPosition.getLatitude() + "";
            longitude = lastMarkerPosition.getLongitude() + "";
        }
        return new Releve(creatorId,"",getCurrentReleveType(), latitude, longitude, formatedLatLongs, "false", Utils.getDate(),Utils.getTime(),lineLength,polygonPerimeter,polygonArea);
    }

    // Formattent les latitudes/longitudes pour que le serveur les récupère
    private StringTuple getStringsFromLatLongs(){
        String lats = "";
        String longs = "";
        for(LatLong latLong : latLongs){
            double lat = latLong.getLatitude();
            double lon = latLong.getLongitude();
            lats += lat + ";";
            longs += lon + ";";
        }
        return new StringTuple(lats,longs);
    }

    // Formate la liste de LatLong pour qu'elle puisse être réutilisable après la transmission par un intent
    private String getFormatedLatLongs(){
        Gson gson = new Gson();
        return gson.toJson(latLongs);
    }

    private class StringTuple{

        private String fst;
        private String snd;

        public StringTuple(String fst, String snd) {
            this.fst = fst;
            this.snd = snd;
        }

        public String fst(){
            return fst;
        }

        public String snd(){
            return snd;
        }

    }

    /**
     * Vérifie s'il y a un relevé de terrain en cours
     *
     * @return <code>True</code> si oui, <code>false</code> sinon
     */
    protected boolean noReleveInProgress(){
        return currentReleve == NO_RELEVE;
    }

    /**
     * Vérifie si le relevé en cours est de type ligne
     *
     * @return <code>True</code> si le relevé en cours est de type ligne, <code>false</code> sinon
     */
    protected boolean currentReleveIsLine() {
        return currentReleve == LINE;
    }

    /**
     * Vérifie si le relevé en cours est de type polygone
     *
     * @return <code>True</code> si le relevé en cours est de type polygone, <code>false</code> sinon
     */
    protected boolean currentReleveIsPolygon() {
        return currentReleve == POLYGON;
    }

    /**
     * Récupère le type de relevé courant
     *
     * @return le type de relevé en cours
     */
    protected String getCurrentReleveType(){
        if(currentReleveIsLine()){
            return getString(R.string.line);
        } else if(currentReleveIsPolygon()){
            return getString(R.string.polygon);
        } return getString(R.string.point);
    }

    /**
     * Créé une polyline
     *
     * @return la polyline créée
     */
    public Polyline createPolyline(){
        return new Polyline(Utils.createPaint(
                PAINT_STROKE,
                (int) (4 * myMap.getModel().displayModel.getScaleFactor()),
                Style.STROKE), AndroidGraphicFactory.INSTANCE);
    }

    /**
     * Instantie la polyLine
     */
    protected void instantiatePolyline(){
        polyline = createPolyline();

        latLongs = polyline.getLatLongs();
    }

    /**
     * Mets fin au relevé courant et lance l'activité de nommage
     */
    private void finirReleve(){
        stopReleve();
        setCurrentReleve(NO_RELEVE);
        finReleve.setVisibility(View.INVISIBLE);
        Intent intent = new Intent(this,NameRelevePopup.class);
        intent.putExtra("releveToAdd",releveToAdd);
        startActivity(intent);
    }

    /**
     * Créer un dialog d'avertissement pour éviter les fin de relevé par mégarde
     *
     * @return Un dialog d'avertissement
     */
    protected Dialog createAvertissementDialog(int identifiant){
        AlertDialog box;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (identifiant) {
            case BOITE_FIN_RELEVE:
                builder.setMessage(R.string.confirmMessage);
                builder.setTitle(getString(R.string.avertissement));
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.oui), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finirReleve();
                    }
                });
                builder.setNegativeButton(getString(R.string.non), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        }
        box = builder.create();
        return box;
    }

    /**
     * Démarre le relevé de ligne
     */
    protected void startLine() {
        instantiatePolyline();
        polyline.addPoint(getUsrLatLong());
        addLayer(polyline);
        pointsTaker = new PointsTaker();
        pointsTaker.run();
    }

    /***
     * Stop le relevé en cours d'exécution
     */
    protected void stopReleve(){
        if(currentReleveIsLine()) {
            stopLine();
        }
        else if(currentReleveIsPolygon()) {
            stopPolygon();
        }
        releveToAdd = createReleveToInsert();
    }

    /**
     * Stop le relevé de ligne
     */
    protected void stopLine() {
        handler.removeCallbacks(pointsTaker);
        lineLength = polylineLengthInMeters(latLongs);
    }

    protected double polylineLengthInMeters(List<LatLong> polyline){
        ListIterator<LatLong> it = polyline.listIterator();
        double res = 0;
        LatLong previous = null;
        LatLong current;
        try{
            previous = it.next();
        }catch (Exception e){
            e.printStackTrace();
        }
        while(it.hasNext()){
            current = it.next();
            res += previous.vincentyDistance(current);
            previous = current;
        }
        return res;
    }

    /**
     * Démarre un relevé de polygone
     */
    protected void startPolygon(){
        instantiatePolygon();
        polygon.addPoint(getUsrLatLong());
        addLayer(polygon);
        pointsTaker = new PointsTaker();
        pointsTaker.run();
    }

    /**
     * Arrête le relevé de polygone
     */
    protected void stopPolygon() {
        handler.removeCallbacks(pointsTaker);

        //On ajoute le premier point du polygone si il est différent du dernier
        LatLong firstPoint = latLongs.get(0);
        if(!firstPoint.equals(latLongs.get(latLongs.size() - 1)))
            latLongs.add(firstPoint);

        polygon.requestRedraw();

        polygonPerimeter = getPolygonPerimeter(latLongs);
        polygonArea = getArea(latLongs);
    }

    private double getPolygonPerimeter(List<LatLong> list){
        List<LatLong> tmp = new ArrayList<>(list);

        tmp.add(list.get(0));
        return polylineLengthInMeters(tmp);
    }

    /**
     * Instantie le polygonne
     */
    protected void instantiatePolygon() {
        polygon = new Polygon(paintFill, paintStroke, AndroidGraphicFactory.INSTANCE);
        latLongs = polygon.getLatLongs();
    }

    /**
     * Action qui relève des points toutes les  secondes
     */
    protected class PointsTaker implements Runnable{

        /**
         * Délai entre chaque relevé de point en ms
         */
        protected static final int DELAY = 5000;

        @Override
        public void run() {
            addPointToGoodLayer();
            callGoodRedraw();
            handler.postDelayed(this,DELAY);
        }

        protected void addPointToGoodLayer(){
            if(currentReleveIsLine()){
                polyline.addPoint(getUsrLatLong());
            } else if(currentReleveIsPolygon()){
                polygon.addPoint(getUsrLatLong());
            }
        }

        protected void callGoodRedraw(){
            if(currentReleve == LINE){
                polyline.requestRedraw();
            } else if(currentReleve == POLYGON){
                polygon.requestRedraw();
            }
        }

    }

    @Override
    protected void setView() {
        setContentView(R.layout.activity_main_rel);
    }

    @Override
    protected void setRelocButton(View.OnClickListener listener) {
        Button reloc2 = (Button) findViewById(R.id.reloc2);
        reloc2.setOnClickListener(listener);
    }

    @Override
    protected void displayLayout(){
        ConstraintLayout layout = (ConstraintLayout)findViewById(R.id.releveLayout);
        layout.setVisibility(View.VISIBLE);
    }

    /**
     * Ajoute le layer l à la carte
     *
     * @param l le layer à ajouter
     */
    protected void addLayer(Layer l){
        myMap.getLayers().add(l);
        previousLayer = l;
    }

    /**
     * Met l'identifiant du relevé courant à l'entier currentReleve en paramètre
     *
     * @param currentReleve L'identifiant du relevé en cours
     */
    public void setCurrentReleve(int currentReleve) {
        this.currentReleve = currentReleve;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dao.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!noReleveInProgress()) {
            startService(new Intent(this,PointsTakerService.class));
            bindService(new Intent(this,PointsTakerService.class),mConnection,BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBound){
            List<LatLong> takenPoints = mService.getTakenPoints();
            if(currentReleveIsLine()){
                polyline.addPoints(takenPoints);
                polyline.requestRedraw();
            }else if(currentReleveIsPolygon()){
                polygon.addPoints(takenPoints);
                polygon.requestRedraw();
            }
            unbindService(mConnection);
            mBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PointsTakerService.PointsTakerBinder binder = (PointsTakerService.PointsTakerBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
}