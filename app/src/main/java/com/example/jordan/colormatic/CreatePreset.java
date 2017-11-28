package com.example.jordan.colormatic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

/**
 * Created by Logan on 11/8/2017.
 */

public class CreatePreset extends AppCompatActivity {
    public static final String TAG = "SECOND ACTIVITY USER";

    public static final String APP_PREFS = "APPLICATION_PREFERENCES";
    String _text;
    //private object used to reference userText box : type EditText
    private EditText userText;
    // object used to save string from userText box
    private String userName;
    private String text;
    private ColorPickerDialogBuilder colorPicker;


    private TextView displayText;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_preset);
        Intent intent = getIntent();

        ColorPickerDialogBuilder.with(this); // creates a new ColorPicker object

        _text = intent.getStringExtra(MainActivity.TEST_TEXT);

        //userText boxed is referenced and initialized for use in program
        userText = (EditText) findViewById(R.id.userText);

        //Creates the save button and values
        Button saveBttn = (Button) findViewById(R.id.saveBttn);
        assert saveBttn != null;

        // Finds the object and what the button will do when pressed
        saveBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveText();
                saveColor();
            }
        });
    }

    /******************************************************************
     * SAVE TEXT
     * Called by saveBttn when pressed: saves the data within the
     * EditText (User Input) box object and Updates text in TextView
     * object(Display Box)
    *******************************************************************/
    protected void saveText() {

        String msg = "THIS ";
        String tag = "SECOND ACTIVITY";
        Throwable tr = new Throwable();

        Log.e(tag, msg, tr);

        SharedPreferences sharedPrefs = getSharedPreferences(MainActivity.APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();

        editor.putString(MainActivity.TEST_TEXT, _text);
        Toast.makeText(this, "Saved Text", Toast.LENGTH_SHORT).show();

        userName = userText.getText().toString();
        editor.putString("USER_NAME", userName);
        Toast.makeText(this, "USER's NAME: Saved", Toast.LENGTH_SHORT).show();

        editor.apply();
        message = "Updated text in preset name to:"  + userName;
        displayText.setText(message);


   }

   private void saveColor() {
//    ColorPickerDialogBuilder.setOnColorSelectedListener(new OnColorSelectedListener() {
//        @Override
//        public void onColorSelected(int selectedColor) {
//            Toast("onColorSelected: 0x" + Integer.toHexString(selectedColor));
//            }
//        })
   }
}



