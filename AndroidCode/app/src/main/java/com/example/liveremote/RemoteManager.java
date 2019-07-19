package com.example.liveremote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.InetAddress;


public class RemoteManager extends AppCompatActivity {

    EditText editText;
    Button ipConnect;
    EditText idEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_manager);

        editText = findViewById(R.id.ip);
        ipConnect=findViewById(R.id.IPConnect);

        ipConnect.setOnClickListener(new View.OnClickListener() {
            @Override
                public void onClick(View v) {
                Intent intent = new Intent(RemoteManager.this, RemoteActivity.class);
                Bundle b = new Bundle();
                b.putString("ip", editText.getText().toString());
                b.putString("id", "ipConnect");
                intent.putExtra("b", b);
                startActivity(intent);
                }
        });

        idEditText = findViewById(R.id.ip);
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            idEditText.setText(ip);
        }catch (Exception e){

        }
    }
}
