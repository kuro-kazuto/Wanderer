package com.untirta.wanderer;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.untirta.wanderer.trilateration.NonLinearLeastSquaresSolver;
import com.untirta.wanderer.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    WifiManager wifiManager;
    Button btnScan, btnNLS, btnChangeBg;
    TextView txtWifiName1,
            txtWifiName2,
            txtWifiName3,
            txtWifi1X,
            txtWifi2X,
            txtWifi3X,
            txtWifi1Y,
            txtWifi2Y,
            txtWifi3Y,
            txtWifi1R,
            txtWifi2R,
            txtWifi3R,
            tv_da, tv_db, tv_dc;
    private ArrayAdapter adpStrength;
    private List<ScanResult> results;
    private ArrayList<String> alStrength = new ArrayList<>();
    double[][] mPositions;
    double[] mDistances;

    private static final String TAG = "MainActivity";

    //add PointsGraphSeries of DataPoint type
    PointsGraphSeries<DataPoint> xySeries;

    private Button btnAddPt;

    private TextView mX, mY, tv_resY, tv_resX;

    GraphView mScatterPlot;

    //make xyValueArray global
    private ArrayList<com.untirta.wanderer.XYValue> xyValueArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ListView lv = findViewById(R.id.listview);
        btnNLS = findViewById(R.id.btnNLS);
        btnChangeBg = findViewById(R.id.btnChangeBg);


        btnAddPt = (Button) findViewById(R.id.btnAddPt);
        // mX = (EditText) findViewById(R.id.numX);
        //mY = (EditText) findViewById(R.id.numY);

        mX = findViewById(R.id.tvmx);
        mY = findViewById(R.id.tvmy);

        tv_resX = findViewById(R.id.tv_resX);
        tv_resY = findViewById(R.id.tv_resY);


        mScatterPlot = findViewById(R.id.scatterPlot);
        xyValueArray = new ArrayList<>();


        txtWifi1X = findViewById(R.id.txtWifi1X);
        txtWifi2X = findViewById(R.id.txtWifi2X);
        txtWifi3X = findViewById(R.id.txtWifi3X);

        txtWifi1Y = findViewById(R.id.txtWifi1Y);
        txtWifi2Y = findViewById(R.id.txtWifi2Y);
        txtWifi3Y = findViewById(R.id.txtWifi3Y);

        txtWifi1R = findViewById(R.id.txtWifi1R);
        txtWifi2R = findViewById(R.id.txtWifi2R);
        txtWifi3R = findViewById(R.id.txtWifi3R);

        txtWifiName1 = findViewById(R.id.txtWifiName1);
        txtWifiName2 = findViewById(R.id.txtWifiName2);
        txtWifiName3 = findViewById(R.id.txtWifiName3);

        tv_da = findViewById(R.id.tv_da);
        tv_db = findViewById(R.id.tv_db);
        tv_dc = findViewById(R.id.tv_dc);

        mDistances = new double[3];
        mPositions = new double[3][2];


        GraphView scatterPlot = (GraphView) findViewById(R.id.scatterPlot);
        for (View touchable : scatterPlot.getTouchables())
        {
            touchable.setEnabled(false);
        }

        btnChangeBg.setOnClickListener(view ->{
            Dialog dialog = new Dialog(MainActivity.this);

            //Memasang Title / Judul pada Custom Dialog
            dialog.setTitle("Change Map");

            //Memasang Desain Layout untuk Custom Dialog
            dialog.setContentView(R.layout.dialog);

            Button Outdoor = dialog.findViewById(R.id.OutdoorMap);
            Button Indoor = dialog.findViewById(R.id.IndoorMap);
            Outdoor.setOnClickListener(v -> {
                dialog.dismiss();
                scatterPlot.setBackgroundResource(R.drawable.gradient);
            });
            Indoor.setOnClickListener(v -> {
                dialog.dismiss();
                scatterPlot.setBackgroundResource(R.drawable.map2);
            });

            dialog.show();
        });

        //btnTri =findViewById(R.id.btnTriliteration);
        btnScan = findViewById(R.id.scanBtn);
        btnScan.setOnClickListener(view -> {
            lv.removeAllViewsInLayout();
            scanWifi();
            txtWifi1R.setText(null);
            txtWifiName1.setText(null);

            txtWifi2R.setText(null);
            txtWifiName2.setText(null);

            txtWifi3R.setText(null);
            txtWifiName3.setText(null);

            txtWifi1X.setText(null);
            txtWifi1Y.setText(null);

            txtWifi2X.setText(null);
            txtWifi2Y.setText(null);

            txtWifi3X.setText(null);
            txtWifi3Y.setText(null);


            Toast.makeText(MainActivity.this, "Scanning WiFi ... Please Wait 5 Second To Catch Distance", Toast.LENGTH_SHORT).show();

            int waktu_loading = 5000;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (alStrength.size() < 3) {
                        Toast.makeText(MainActivity.this, "Signal Wi-Fi < 3", Toast.LENGTH_SHORT).show();
                    }

                    else {
                        String da = lv.getItemAtPosition(0).toString();
                        String db = lv.getItemAtPosition(1).toString();
                        String dc = lv.getItemAtPosition(2).toString();
                        String[] splitDistances1 = da.split("-");
                        String s1 = splitDistances1[1];
                        String nAP1 = splitDistances1[2];
                        String[] splitDistances2 = db.split("-");
                        String s2 = splitDistances2[1];
                        String nAP2 = splitDistances2[2];
                        String[] splitDistances3 = dc.split("-");
                        String s3 = splitDistances3[1];
                        String nAP3 = splitDistances3[2];


                        //POSISI UTAMA
                        String x_AP1 = "2.00";
                        String y_AP1 = "8.00";

                        String x_AP2 = "2.00";
                        String y_AP2 = "2.00";

                        String x_AP3 = "8.00";
                        String y_AP3 = "2.00";

                        String x_AP4 = "8.00";
                        String y_AP4 = "8.00";

                        String x_AP5 = "3.00";
                        String y_AP5 = "5.00";

                        String x_AP6 = "7.00";
                        String y_AP6 = "5.00";


                        switch (nAP1) {
                            case "AP_1": {
                                txtWifi1X.setText(x_AP1);
                                txtWifi1Y.setText(y_AP1);
                                txtWifi1R.setText(s1);
                                txtWifiName1.setText(nAP1);
                                break;
                            }
                            case "AP_2": {
                                txtWifi1X.setText(x_AP2);
                                txtWifi1Y.setText(y_AP2);
                                txtWifi1R.setText(s1);
                                txtWifiName1.setText(nAP1);
                                break;
                            }
                            case "AP_3": {
                                txtWifi1X.setText(x_AP3);
                                txtWifi1Y.setText(y_AP3);
                                txtWifi1R.setText(s1);
                                txtWifiName1.setText(nAP1);
                                break;
                            }
                            case "AP_4": {
                                txtWifi1X.setText(x_AP4);
                                txtWifi1Y.setText(y_AP4);
                                txtWifi1R.setText(s1);
                                txtWifiName1.setText(nAP1);
                                break;
                            }
                            case "AP_5": {
                                txtWifi1X.setText(x_AP5);
                                txtWifi1Y.setText(y_AP5);
                                txtWifi1R.setText(s1);
                                txtWifiName1.setText(nAP1);
                                break;
                            }
                            case "AP_6": {
                                txtWifi1X.setText(x_AP6);
                                txtWifi1Y.setText(y_AP6);
                                txtWifi1R.setText(s1);
                                txtWifiName1.setText(nAP1);
                                break;
                            }
                            default:
                                Toast.makeText(MainActivity.this, "Null", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        switch (nAP2) {
                            case "AP_1": {
                                txtWifi2X.setText(x_AP1);
                                txtWifi2Y.setText(y_AP1);
                                txtWifi2R.setText(s2);
                                txtWifiName2.setText(nAP2);
                                break;
                            }
                            case "AP_2": {
                                txtWifi2X.setText(x_AP2);
                                txtWifi2Y.setText(y_AP2);
                                txtWifi2R.setText(s2);
                                txtWifiName2.setText(nAP2);
                                break;
                            }
                            case "AP_3": {
                                txtWifi2X.setText(x_AP3);
                                txtWifi2Y.setText(y_AP3);
                                txtWifi2R.setText(s2);
                                txtWifiName2.setText(nAP2);
                                break;
                            }
                            case "AP_4": {
                                txtWifi2X.setText(x_AP4);
                                txtWifi2Y.setText(y_AP4);
                                txtWifi2R.setText(s2);
                                txtWifiName2.setText(nAP2);
                                break;
                            }
                            case "AP_5": {
                                txtWifi2X.setText(x_AP5);
                                txtWifi2Y.setText(y_AP5);
                                txtWifi2R.setText(s2);
                                txtWifiName2.setText(nAP2);
                                break;
                            }
                            case "AP_6": {
                                txtWifi2X.setText(x_AP6);
                                txtWifi2Y.setText(y_AP6);
                                txtWifi2R.setText(s2);
                                txtWifiName2.setText(nAP2);
                                break;
                            }
                            default:
                                Toast.makeText(MainActivity.this, "Null", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        switch (nAP3) {
                            case "AP_1": {
                                txtWifi3X.setText(x_AP1);
                                txtWifi3Y.setText(y_AP1);
                                txtWifi3R.setText(s3);
                                txtWifiName3.setText(nAP3);
                                break;
                            }
                            case "AP_2": {
                                txtWifi3X.setText(x_AP2);
                                txtWifi3Y.setText(y_AP2);
                                txtWifi3R.setText(s3);
                                txtWifiName3.setText(nAP3);
                                break;
                            }
                            case "AP_3": {
                                txtWifi3X.setText(x_AP3);
                                txtWifi3Y.setText(y_AP3);
                                txtWifi3R.setText(s3);
                                txtWifiName3.setText(nAP3);
                                break;
                            }
                            case "AP_4": {
                                txtWifi3X.setText(x_AP4);
                                txtWifi3Y.setText(y_AP4);
                                txtWifi3R.setText(s3);
                                txtWifiName3.setText(nAP3);
                                break;
                            }
                            case "AP_5": {
                                txtWifi3X.setText(x_AP5);
                                txtWifi3Y.setText(y_AP5);
                                txtWifi3R.setText(s3);
                                txtWifiName3.setText(nAP3);
                                break;
                            }
                            case "AP_6": {
                                txtWifi3X.setText(x_AP6);
                                txtWifi3Y.setText(y_AP6);
                                txtWifi3R.setText(s3);
                                txtWifiName3.setText(nAP3);
                                break;
                            }
                            default:
                                Toast.makeText(MainActivity.this, "Null", Toast.LENGTH_SHORT).show();
                                break;
                        }

                       // txtWifi1R.setText(s1);
                       // txtWifiName1.setText(nAP1);

                        //txtWifi2R.setText(s2);
                       // txtWifiName2.setText(nAP2);

                       // txtWifi3R.setText(s3);
                      //  txtWifiName3.setText(nAP3);

                    }
                }
            }, waktu_loading);

        });

        plotMap();


/*
        btnTri.setOnClickListener(v ->{
            triliteration();
        });

 */

        btnNLS.setOnClickListener(v -> {
            nonlinearLSQ();
        });

        adpStrength = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alStrength);
        lv.setAdapter(adpStrength);
        //scanWifi();
    }

    private void scanWifi() {
        alStrength.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (int i = 0; i < results.size(); i++) {
                String s = String.format("%.3f", calculateDistance(results.get(i).level, results.get(i).frequency));
                String st = s.replace(",",".");
                String level = Double.toString(WifiManager.calculateSignalLevel(results.get(i).level, 10));
                String ssid = results.get(i).SSID ;
                alStrength.add(level+"-"+st+"-"+ssid);
                Collections.sort(alStrength, Collections.reverseOrder());
                alStrength.removeIf(n->(n.charAt(10)=='K'));
                alStrength.removeIf(n->(n.charAt(10)=='U'));
                adpStrength.notifyDataSetChanged();

            }
        }
    };

    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        //Distance (m) = 10(Free Space Path Loss – 27.55 – 20log10(f))/20
        return Math.pow(10.0, exp);
    }

    private void nonlinearLSQ() {
        if (    txtWifi1X.getText().toString().trim().isEmpty() ||
                txtWifi1Y.getText().toString().trim().isEmpty() ||
                txtWifi2X.getText().toString().trim().isEmpty() ||
                txtWifi2Y.getText().toString().trim().isEmpty() ||
                txtWifi3X.getText().toString().trim().isEmpty() ||
                txtWifi3Y.getText().toString().trim().isEmpty() ||
                txtWifi3R.getText().toString().trim().isEmpty() ||
                txtWifi3R.getText().toString().trim().isEmpty() ||
                txtWifi3R.getText().toString().trim().isEmpty()){

            Toast.makeText(getApplicationContext(), "Isi Kolom dengan Benar !", Toast.LENGTH_LONG).show();
        }
        else{

            mPositions[0][0] = Double.parseDouble(txtWifi1X.getText().toString());
            mPositions[0][1] = Double.parseDouble(txtWifi1Y.getText().toString());
            mPositions[1][0] = Double.parseDouble(txtWifi2X.getText().toString());

            mPositions[1][1] = Double.parseDouble(txtWifi2Y.getText().toString());
            mPositions[2][0] = Double.parseDouble(txtWifi3X.getText().toString());
            mPositions[2][1] = Double.parseDouble(txtWifi3Y.getText().toString());

            mDistances[0] = Double.parseDouble(txtWifi1R.getText().toString());
            mDistances[1] = Double.parseDouble(txtWifi2R.getText().toString());
            mDistances[2] = Double.parseDouble(txtWifi3R.getText().toString());


            double[][] positions = mPositions;
            double[] distances = mDistances;

            //Non Linear summon class
            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum = solver.solve();

            // the answer
            double[] centroid = optimum.getPoint().toArray();
            DecimalFormat df = new DecimalFormat("#.##");




            String xdf = df.format(centroid[0]);
            String ydf = df.format(centroid[1]);
            xdf = xdf.replace(",",".");
            ydf = ydf.replace(",",".");
            mX.setText(xdf);
            mY.setText(ydf);
            tv_resX.setText("X = "+xdf+" meter");
            tv_resY.setText("Y = "+ydf+" meter");

        }

    }

    //=========this content is private method for coordinate========

    private void plotMap(){
        //declare the xySeries Object
        xySeries = new PointsGraphSeries<>();
        btnAddPt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mX.getText().toString().equals("") && !mY.getText().toString().equals("") ){
                    //dummy text
                    double x = Double.parseDouble(mX.getText().toString()); //masukan x disini
                    double y = Double.parseDouble(mY.getText().toString()); //masukan y disini
                    Log.d(TAG, "onClick: Adding a new point. (x,y): (" + x + "," + y + ")" );
                    xyValueArray.add(new com.untirta.wanderer.XYValue(x,y));
                    plotMap();

                }else {
                    toastMessage("You must fill out both fields!");
                }
            }
        });




        //little bit of exception handling for if there is no data.
        if(xyValueArray.size() != 0){
            createScatterPlot();
        }else{
            Log.d(TAG, "onCreate: No data to plot.");
        }
    }

    private void createScatterPlot() {
        Log.d(TAG, "createScatterPlot: Creating scatter plot.");

        //sort the array of xy values
        xyValueArray = sortArray(xyValueArray);

        //add the data to the series
        for(int i = 0;i <xyValueArray.size(); i++){
            try{
                double x = xyValueArray.get(i).getX();
                double y = xyValueArray.get(i).getY();
                xySeries.appendData(new DataPoint(x,y),true, 1000);
            }catch (IllegalArgumentException e){
                Log.e(TAG, "createScatterPlot: IllegalArgumentException: " + e.getMessage() );
            }
        }

        //set some properties
        //xySeries.setShape(PointsGraphSeries.Shape.TRIANGLE);
        xySeries.setColor(Color.BLUE);
        xySeries.setSize(20f);

        //set Scrollable and Scaleable
        mScatterPlot.getViewport().setScalable(false);
        mScatterPlot.getViewport().setScalableY(false);
        mScatterPlot.getViewport().setScrollable(false);
        mScatterPlot.getViewport().setScrollableY(false);

        //set manual x bounds
        mScatterPlot.getViewport().setYAxisBoundsManual(true);
        mScatterPlot.getViewport().setMaxY(10);
        mScatterPlot.getViewport().setMinY(0);

        //set manual y bounds
        mScatterPlot.getViewport().setXAxisBoundsManual(true);
        mScatterPlot.getViewport().setMaxX(10);
        mScatterPlot.getViewport().setMinX(0);

        mScatterPlot.addSeries(xySeries);
    }

    /**
     * Sorts an ArrayList<XYValue> with respect to the x values.
     * @param array
     * @return
     */
    private ArrayList<com.untirta.wanderer.XYValue> sortArray(ArrayList<com.untirta.wanderer.XYValue> array){
        /*
        //Sorts the xyValues in Ascending order to prepare them for the PointsGraphSeries<DataSet>
         */
        int factor = Integer.parseInt(String.valueOf(Math.round(Math.pow(array.size(),2))));
        int m = array.size() - 1;
        int count = 0;
        Log.d(TAG, "sortArray: Sorting the XYArray.");


        while (true) {
            m--;
            if (m <= 0) {
                m = array.size() - 1;
            }
            Log.d(TAG, "sortArray: m = " + m);
            try {
                //print out the y entrys so we know what the order looks like
                //Log.d(TAG, "sortArray: Order:");
                //for(int n = 0;n < array.size();n++){
                //Log.d(TAG, "sortArray: " + array.get(n).getY());
                //}
                double tempY = array.get(m - 1).getY();
                double tempX = array.get(m - 1).getX();
                if (tempX > array.get(m).getX()) {
                    array.get(m - 1).setY(array.get(m).getY());
                    array.get(m).setY(tempY);
                    array.get(m - 1).setX(array.get(m).getX());
                    array.get(m).setX(tempX);
                } else if (tempX == array.get(m).getX()) {
                    count++;
                    Log.d(TAG, "sortArray: count = " + count);
                } else if (array.get(m).getX() > array.get(m - 1).getX()) {
                    count++;
                    Log.d(TAG, "sortArray: count = " + count);
                }
                //break when factorial is done
                if (count == factor) {
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "sortArray: ArrayIndexOutOfBoundsException. Need more than 1 data point to create Plot." +
                        e.getMessage());
                break;
            }
        }
        return array;
    }

    /**
     * customizable toast
     * @param message
     */
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }


    //========end section =====

}