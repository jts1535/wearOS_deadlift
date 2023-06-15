package com.samsung.sdc21.deadlift;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    ImageView trainingImageView;
    TextView trainingNameTextView;
    TextView weightTextView;
    TextView weightUnitTextView;
    TextView minusWeightTextView;
    TextView plusWeightTextView;

    TextView repeatTextView;
    TextView minusRepeatTextView;
    TextView plusRepeatTextView;

    TextView nextSet;
    TextView nextTraining;


    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice bluetoothDevice;
    Handler bluetoothHandler;
    ConnectedBluetoothThread connectedBluetoothThread;
    BluetoothSocket bluetoothSocket;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_BLUETOOTH = 2;
    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final static String NAME = "Bluetooth";
    final static String TAG = "ERROR";
    ArrayList<String> trainingIdList = new ArrayList<>();
    int nextTrainingIndex;
    String selectedDeviceAddress;

    double weight;
    int repeat;
    double unitWeight;
    int unitRepeat;

    ArrayList<ArrayList<String>> trainingRecord;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        initValue();

        trainingIdList.add("ChinUp");
        trainingIdList.add("MachineFly");
        trainingIdList.add("PullUp");
        trainingIdList.add("LatPullDown");
        trainingIdList.add("startTraining");
        trainingRecord = new ArrayList<>();
        nextTraining();

        nextTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(nextTrainingIndex < trainingIdList.size() - 1) {
                    nextTraining();
                } else {
                    finishTraining();
                }
                if(nextTrainingIndex == trainingIdList.size() - 1) {
                    nextTraining.setText("운동 종료");
                }
            }
        });
        ActivityResultLauncher<Intent> startActivityResultCountRepeat = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            int repeatNum = data.getIntExtra("repeatNum", 0);
                            repeat = repeatNum;
                            repeatTextView.setText(String.valueOf(repeatNum));
                        }
                    }
                }
        );

        nextSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int repeat = Integer.parseInt(repeatTextView.getText().toString());
                if(repeat != 0) {
                    nextSet();
                    Intent intent = new Intent(getApplicationContext(), Deadlift.class);
                    startActivityResultCountRepeat.launch(intent);
                }
            }
        });

        plusWeightTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double weightNum = Double.parseDouble(weightTextView.getText().toString());
                weightNum += unitWeight;
                weightTextView.setText(String.valueOf(weightNum));
            }
        });

        minusWeightTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double weightNum = Double.parseDouble(weightTextView.getText().toString());
                weightNum -= unitWeight;
                weightTextView.setText(String.valueOf(weightNum > 0 ? weightNum : 0));
            }
        });

        weightUnitTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(weightUnitTextView.getText().toString().compareTo("kg") == 0) {
                    weightUnitTextView.setText("lb");
                } else {
                    weightUnitTextView.setText("kg");
                }
            }
        });

        plusRepeatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatNum = Integer.parseInt(repeatTextView.getText().toString());
                repeatTextView.setText(String.valueOf(repeatNum + 1));
            }
        });

        minusRepeatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatNum = Integer.parseInt(repeatTextView.getText().toString());
                if(repeatNum > 0)  repeatTextView.setText(String.valueOf(repeatNum - 1));
            }
        });
    }


    private void findView() {
        trainingImageView = findViewById(R.id.trainingImageView);

        weightTextView = findViewById(R.id.weightTextView);
        weightUnitTextView = findViewById(R.id.weightUnitTextView);
        minusWeightTextView = findViewById(R.id.minusWeightTextView);
        plusWeightTextView = findViewById(R.id.plusWeightTextView);

        repeatTextView = findViewById(R.id.repeatTextView);
        minusRepeatTextView = findViewById(R.id.minusRepeatTextView);
        plusRepeatTextView = findViewById(R.id.plusRepeatTextView);

        nextSet = findViewById(R.id.nextSetTextView);
        nextTraining = findViewById(R.id.nextTrainingTextView);
    }

    private void initValue() {
        nextTrainingIndex = 0;
        weight = 0;
        weightTextView.setText(String.valueOf(weight));
        unitWeight = 5;
        plusWeightTextView.setText("+5");
        minusWeightTextView.setText("-5");
        repeat = 0;
        repeatTextView.setText(String.valueOf(repeat));
        unitRepeat = 1;
        plusRepeatTextView.setText("+1");
        minusRepeatTextView.setText("-1");

    }

    private void nextSet() {
        String string = weightTextView.getText().toString() + "," + weightUnitTextView.getText().toString() + "," + repeatTextView.getText().toString();

        repeat = 0;
        repeatTextView.setText(String.valueOf(repeat));
    }

    private void nextTraining(){
        String trainingId = trainingIdList.get(nextTrainingIndex);
        trainingRecord.add(new ArrayList<>());

        Bitmap resized = DataProcessing.getTrainingImage(this, trainingId);
        trainingImageView.setImageBitmap(resized);

        //trainingNameTextView.setText(trainingId);
        weight = 0;
        weightTextView.setText(String.valueOf(weight));
        repeat = 0;
        repeatTextView.setText(String.valueOf(repeat));
        nextTrainingIndex++;
    }

    private void finishTraining() {
        ArrayList<String> trainingRecordList = new ArrayList<>();
        for(int i = 0; i < trainingIdList.size() - 1; i++) {
            for(int j = 0; j < trainingRecord.get(i).size(); j++) {
                String data = trainingIdList.get(i) + "," + i + "," + j + "," + trainingRecord.get(j);
                trainingRecordList.add(data);
            }
        }
        //Bluetooth bluetooth = new Bluetooth(this, selectedDeviceAddress, trainingRecordList);
    }


    private boolean selectDevice(String address) {
        for (BluetoothDevice device : devices) {
            if (address.equals(device.getAddress())) {
                bluetoothDevice = device;
                SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = connectedWearable.edit();
                editor.putString("address", address);
                editor.apply();
                return true;
            }
        }

        return false;
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        bluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        trainingIdList.add(buffer.toString());
                        if(buffer.toString().compareTo("startTraining") == 0) {
                            return ;
                        } else {

                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
