package com.example.cortereal.barcodereader;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

public class RunAssayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_assay);
        Button startBtn = (Button) findViewById(R.id.button);
        startBtn.setBackgroundColor(getResources().getColor(R.color.orange));
    }
}
