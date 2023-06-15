package com.samsung.sdc21.deadlift;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.health.services.client.ExerciseClient;
import androidx.health.services.client.ExerciseUpdateListener;
import androidx.health.services.client.HealthServices;
import androidx.health.services.client.HealthServicesClient;
import androidx.health.services.client.data.Availability;
import androidx.health.services.client.data.DataPoint;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.ExerciseCapabilities;
import androidx.health.services.client.data.ExerciseConfig;
import androidx.health.services.client.data.ExerciseLapSummary;
import androidx.health.services.client.data.ExerciseState;
import androidx.health.services.client.data.ExerciseType;
import androidx.health.services.client.data.ExerciseUpdate;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Deadlift extends Activity {
    private static final String TAG = "Deadlift Exercise";
    private final ExerciseType exerciseType = ExerciseType.DEADLIFT;
    public DeadliftReader deadliftReader;
    HealthServicesClient healthServicesClient = null;
    private boolean permissionGranted = false;
    private Button butStart;
    private TextView txtReps;
    private TextView txtCalories;
    private TextView txtTime;
    private ExerciseClient exerciseClient;
    private ExerciseConfig.Builder exerciseConfigBuilder;
    private ExerciseUpdateListener exerciseUpdateListener;
    private boolean isMeasurementRunning = false;
    private Set<DataType> exerciseCapabilitiesSet = null;
    int repeatNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestPermissions(new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count_repeat);
        butStart = findViewById(R.id.butStart);
        butStart.setOnClickListener(unused -> onButtonClick());
        txtReps = findViewById(R.id.tvDBReps);
        txtCalories = findViewById(R.id.tvDBCalories);
        txtTime = findViewById(R.id.tvDBTime);
        try {
            healthServicesClient = HealthServices.getClient(this);
            exerciseClient = healthServicesClient.getExerciseClient();
        } catch (Throwable e) {
            Log.e(TAG, "Failed to get Health Services client: ", e);
        }
        deadliftReader = new DeadliftReader();
        exerciseConfigBuilder = ExerciseConfig.builder()
                .setExerciseType(exerciseType);
        checkCapabilities();
        setExerciseUpdateListener();
    }

    public void checkCapabilities() {
        @SuppressWarnings("All")
        ListenableFuture<ExerciseCapabilities> capabilitiesListenableFuture = null;
        capabilitiesListenableFuture = exerciseClient.getCapabilities();
        Futures.addCallback(capabilitiesListenableFuture,
                new FutureCallback<ExerciseCapabilities>() {
                    @Override
                    public void onSuccess(@Nullable ExerciseCapabilities result) {
                        Log.i(TAG, "Got exercise capabilities");
                        try {
                            exerciseCapabilitiesSet = deadliftReader.getExerciseCapabilities(result);
                        } catch (DeadliftException deadliftException) {
                            Log.e(TAG, "Deadlift: ", deadliftException);
                        }
                        setupExerciseConfig();
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.w(TAG, "Could not get capabilities", t);
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    private void onButtonClick() {
        if (!permissionGranted) {
            Toast toast = Toast.makeText(getApplicationContext(), "Could not get activity permissions", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if (!isMeasurementRunning) {
            txtReps.setText(R.string.TVDBReps);
            txtTime.setText(R.string.TVDBTime);
            txtCalories.setText(R.string.TVDBCalories);
            startExercise();
        } else{
            Intent intent = new Intent();
            intent.putExtra("repeatNum", repeatNum);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
            //stopExercise();
    }

    protected void setupExerciseConfig() {
        exerciseConfigBuilder = ExerciseConfig.builder()
                .setExerciseType(exerciseType)
                .setDataTypes(exerciseCapabilitiesSet);
    }

    protected void setExerciseUpdateListener() {
        exerciseUpdateListener =
                new ExerciseUpdateListener() {
                    @Override
                    public void onExerciseUpdate(@NonNull ExerciseUpdate update) {
                        try {
                            updateRepCount(update);
                        } catch (DeadliftException exception) {
                            Log.e(TAG, "Error getting exercise update: ", exception);
                        }
                    }

                    @Override
                    public void onAvailabilityChanged(@NonNull DataType dataType, @NonNull Availability availability) {
                        Log.w(TAG, String.format(
                                "onAvailabilityChanged: dataType=%s, availability=%s", dataType, availability));
                    }

                    @Override
                    public void onLapSummary(@NonNull ExerciseLapSummary summary) {
                        Log.w(TAG, "Got lap summary in dumbbell curl");
                    }
                };
    }

    protected void startExercise() {
        Futures.addCallback(exerciseClient.startExercise(exerciseConfigBuilder.build()),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@NullableDecl Void result) {
                        Log.i(TAG, "Successfully started");
                        try {
                            startExerciseUpdateListener();
                        } catch (DeadliftException exception) {
                            Log.e(TAG, "Error during starting exercise: ", exception);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.w(TAG, "Could not start activity", t);
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    protected void stopExercise() {
        Futures.addCallback(exerciseClient.endExercise(),
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        Log.i(TAG, "Successfully ended exercise");
                        butStart.setText(R.string.butStart);
                        isMeasurementRunning = false;
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.w(TAG, "Error ending exercise: ", t);
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    public void startExerciseUpdateListener() throws DeadliftException {
        if (exerciseClient == null)
            throw new DeadliftException("Exercise Client is null");
        @SuppressWarnings("All")
        ListenableFuture<Void> updateListenableFuture = null;
        updateListenableFuture = exerciseClient.setUpdateListener(exerciseUpdateListener);
        if (updateListenableFuture == null)
            throw new DeadliftException("Update is null");
        Futures.addCallback(updateListenableFuture,
                new FutureCallback<Void>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(@NullableDecl Void result) {
                        Log.i(TAG, "Successfully set update listener");
                        isMeasurementRunning = true;
                        butStart.setText("Stop");
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.w(TAG, "Failed to set listener", t);
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    public void updateRepCount(ExerciseUpdate update) throws DeadliftException {
        @SuppressWarnings("All")
        Map<DataType, List<DataPoint>> map = null;
        if (update == null) {
            throw new DeadliftException("Exercise update is null");
        }
        map = update.getLatestMetrics();
        if (map.isEmpty())
            return;
        if (update.getState() == ExerciseState.USER_ENDING)  //We ignore user ending not to reset values
            return;
        List<DataPoint> repPoints = map.get(DataType.REP_COUNT);
        List<DataPoint> caloriesPoints = map.get(DataType.TOTAL_CALORIES);
        String repValue;
        String caloriesValue;
        if (repPoints != null) {
            repValue = String.format(Locale.ENGLISH, "%d", Iterables.getLast(repPoints).getValue().asLong());
            repeatNum = Integer.parseInt(repValue);
            Log.i(TAG, "Read rep count: " + repValue);
            txtReps.setText(String.format("반복 횟수: %s", repValue));
        }
        if (caloriesPoints != null) {
            caloriesValue = String.format(Locale.ENGLISH, "%.2f", Iterables.getLast(caloriesPoints).getValue().asDouble());
            Log.i(TAG, "Read calories count:" + caloriesValue);
            txtCalories.setText(String.format("칼로리 소모: %s", caloriesValue));
        }
        long duration = update.getActiveDuration().getSeconds();
        txtTime.setText(String.format(Locale.ENGLISH, "시간 경과:\n %02d:%02d:%02d",
                duration / 3600, (duration % 3600) / 60, (duration % 60)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            permissionGranted = true;
            for (int i = 0; i < permissions.length; ++i) {
                if (grantResults[i] == PERMISSION_DENIED) {
                    permissionGranted = false;
                    Toast toast = Toast.makeText(getApplicationContext(), "Could not get activity permissions", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}