package com.antigravity.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int clickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView titleText = findViewById(R.id.titleText);
        final Button actionButton = findViewById(R.id.actionButton);

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCount++;
                titleText.setText("Clicked " + clickCount + " times!");
                Toast.makeText(MainActivity.this, "Action triggered by Antigravity App!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
