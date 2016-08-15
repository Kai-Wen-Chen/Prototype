package com.example.nthucs.prototype.Activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.example.nthucs.prototype.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by selab on 2016/8/15.
 */
public class HomeActivity extends AppCompatActivity {

    public static final String TAG = "Prototype";
    private TextView exerciseTime,exerciseSteps,exerciseCalories,exerciseDistance;
    public static GoogleApiClient mClient = null;

    int totalSteps = 0;
    Float totalCals = (float)0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initLayout();
        buildFitnessClient();

    }


    @Override
    public void onResume(){
        super.onResume();

    }


    private void initLayout(){
        exerciseTime = (TextView)findViewById(R.id.exerciseTime);
        exerciseSteps = (TextView)findViewById(R.id.exerciseSteps);
        exerciseCalories = (TextView)findViewById(R.id.exerciseCalories);
        exerciseDistance = (TextView)findViewById(R.id.exerciseDistance);
    }


    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                // Now you can make calls to the Fitness APIs.  What to do?
                                // Look at some data!!
                                new InsertAndVerifyDataTask().execute();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(TAG, "Google Play services connection failed. Cause: " +
                                result.toString());

                    }
                })
                .build();
    }

    private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            // Begin by creating the query.
            DataReadRequest readRequest = queryFitnessData();
            // [START read_dataset]
            // Invoke the History API to fetch the data with the query and await the result of
            // the read request.
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
            System.out.println("READRESULT "+dataReadResult);
            // [END read_dataset]

            // For the sake of the sample, we'll print the data so we can see what we just added.
            // In general, logging fitness information should be avoided for privacy reasons.
            printSpeData(dataReadResult);
            //printSpeData(dataReadResult);

            return null;
        }

        @Override
        protected void onPostExecute(Void unused)
        {
            super.onPostExecute(unused);
            exerciseSteps.setText(Integer.toString(totalSteps));
            exerciseCalories.setText((totalCals.toString()));
        }




    }

    public static DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.add(Calendar.WEEK_OF_YEAR, -1);


        // today
        Calendar midNight = new GregorianCalendar();
        midNight.set(Calendar.HOUR_OF_DAY, 0);
        midNight.set(Calendar.MINUTE, 0);
        midNight.set(Calendar.SECOND, 0);
        midNight.set(Calendar.MILLISECOND, 0);
        long startTime = midNight.getTimeInMillis();
        // next day
        midNight.add(Calendar.DAY_OF_MONTH, 1);
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms")
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime,System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();
        // [END build_read_data_request]
        return readRequest;
    }

    public void printSpeData(DataReadResult dataReadResult){

        //DataSet stepData = dataReadResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
        for (Bucket bucket : dataReadResult.getBuckets()) {
            DataSet stepData = bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
            DataSet calData = bucket.getDataSet(DataType.TYPE_CALORIES_EXPENDED);
            for (DataPoint dp : stepData.getDataPoints()) {
                for (Field field : dp.getDataType().getFields()) {
                    int steps = dp.getValue(field).asInt();
                    totalSteps += steps;
                }
            }
            for (DataPoint dp : calData.getDataPoints()) {
                for(Field field : dp.getDataType().getFields()) {
                    Float cals = dp.getValue(field).asFloat();
                    totalCals += cals;
                }
            }
        }

    }
}
