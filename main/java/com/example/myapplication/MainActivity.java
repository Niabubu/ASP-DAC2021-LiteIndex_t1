package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

public class MainActivity extends AppCompatActivity {

    Button b_read;
    Button b_view;
    DatabaseHelper myDB;
    TextView tv_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b_view = (Button) findViewById(R.id.b_view);
        b_read = (Button) findViewById(R.id.b_read);
        tv_text = (TextView) findViewById(R.id.tv_text);
        myDB = new DatabaseHelper(this);
//
//        b_view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, ViewListContents.class);
//                startActivity(intent);
//            }
//        });
        b_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ParseJSON.class);
                startActivity(intent);

            }
        });

        b_read.setOnClickListener(new View.OnClickListener() {
            @Override
//            public void onClick(View v) {
//                String text = "";
//                try{
//                    InputStream is = getAssets().open("29oe.json");
//                    int size = is.available();
//                    byte[] buffer = new byte[size];
//                    is.read(buffer);
//                    is.close();
//                    text = new String(buffer);
//
//                } catch(IOException ex){
//                    ex.printStackTrace();
//                }
//                tv_text.setText(text);
//            }

            public void onClick(View v) {
                String text = "";
                String str;
                try{
                    Log.d("MyTag","HELLO!");
                    BufferedReader br = null;
                    int i = 1;
                    for(int j = 1; j <= 31; j++) {
                        InputStream is = getAssets().open(String.valueOf(j)+"oe.json");
                        Reader reader = new InputStreamReader(is);
                        br = new BufferedReader(reader);

                        SQLiteDatabase db = myDB.getWritableDatabase();
                        String sql = "INSERT INTO RAWJSON(DID,DATA) VALUES(?,?)";
                        SQLiteStatement statement = db.compileStatement(sql);


                        while ((str = br.readLine()) != null) {
                            Log.d("MyTag", str);
                            text = text + str;
                            statement.clearBindings();
                            statement.bindLong(1, i);
                            statement.bindString(2, str);
                            statement.executeInsert();
                            i++;
                        }
                    }
                    br.close();

                } catch(IOException ex){
                    ex.printStackTrace();
                }
                tv_text.setText(text);
            }
        });





    }
}
