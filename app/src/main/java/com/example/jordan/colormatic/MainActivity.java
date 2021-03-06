package com.example.jordan.colormatic;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Creates the camera object and respective variables. Adds the ring as the take picture button,
 * the hamburger icon in the bottom right corner, and supplies/fills the popup menu. It also
 * displays the crosshairs in the middle of the screen and grabs the color at wherever the use
 * is pointing the crosshairs at. We also create the map and read in all the colors.
 *
 * @author ColormaticTeam
 */

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private TextureView textureView;
    private TextView myTextView;

    private String colorName = "";
    private String preset1 = "";
    private String preset2 = "";
    private String preset3 = "";
    String newPresetName = "";
    String newColor = "";

    private boolean p1 = false; // preset1
    private boolean p2 = false; // preset2
    private boolean p3 = false; // preset3

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private File file = null;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private List<Preset> presetList;

    SharedPreferences mPrefs;
    SharedPreferences.Editor prefsEditor;

    private Bitmap bitmap;

    private Map<String, Integer> mColors = new HashMap<>();

    private String ColormaticImageFilePathDirectory;

    /**
     * Initializes the variables that we need, sets up the camera and TextureView, and stores
     * all the buttons and onClickListeners. It also starts the popup menu with that variables that
     * are programmatically changed.
     *
     * @param savedInstanceState A bundle that is given whenever the activity starts.
     *                           Usually used for orientation change, but we don't use that feature.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ColormaticImageFilePathDirectory = createDirectoryAndSaveFile();

        textureView = findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        ImageButton crosshairButton = findViewById(R.id.crosshair_btn);
        assert crosshairButton != null;

        final ImageButton menu = findViewById(R.id.btn_menu); // this is that hamburger icon
        assert menu != null;

        Button takePictureButton = findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;

        presetList = new ArrayList<>();
        setStandardPresets();

        mPrefs = getPreferences(MODE_PRIVATE);
        prefsEditor = mPrefs.edit();

        textureView.setDrawingCacheEnabled(true);
        textureView.buildDrawingCache(true);

        createDatabaseMap(); // read the colors into a map

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, menu);

                popupMenu.getMenu().add(0, 0, 5, "Reset All Presets");
                popupMenu.getMenu().add(0, 0, 4, "Create New Preset");
                popupMenu.getMenu().add(0, 0, 3, preset3);
                popupMenu.getMenu().add(0, 0, 2, preset2);
                popupMenu.getMenu().add(0, 0, 1, preset1);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (Objects.equals("Reset All Presets", menuItem.getTitle())) {
                            resetPresets();
                        }
                        else if (Objects.equals("Create New Preset", menuItem.getTitle())) {
                            //startActivity(new Intent(MainActivity.this, advanceSetting.class));
                            startActivity(new Intent(MainActivity.this, CreatePreset.class)); // just a shortcut until we can have the 2nd and 3rd activity communicate
                        }
                        else if (Objects.equals(preset1, menuItem.getTitle())) {
                            //Toast.makeText(MainActivity.this, "Locate your visual accessibility settings", Toast.LENGTH_LONG).show();
                            //Toast.makeText(MainActivity.this, "Turn on your color adjustment for your specific colorblindness", Toast.LENGTH_LONG).show();
                            //startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
                            if (p1) {
                                p1 = false;
                            }
                            else {
                                p1 = true;
                                p2 = false;
                                p3 = false;
                            }
                        }
                        else if (Objects.equals(preset2, menuItem.getTitle())) {
                            if (p2) {
                                p2 = false;
                            }
                            else {
                                p1 = false;
                                p2 = true;
                                p3 = false;
                            }
                        }
                        else if (Objects.equals(preset3, menuItem.getTitle())) {
                            if (p3) {
                                p3 = false;
                            }
                            else {
                                p1 = false;
                                p2 = false;
                                p3 = true;
                            }
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Something went wrong??", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    /**
     * Creates a directory for pictures from Colormatic app to be saved to
     *
     * @return String containing the file path to be used to save the image taken by Colormatic Application
     */
    //private void createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) {
    private String createDirectoryAndSaveFile() {
        File CameraDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        //File direct = new File(Environment.getExternalStorageDirectory()+ "/Colormatic");

        //this is the directory name that was created depending on which if statment was used below
        String ColormaticImageDirectoryPath = "";

        /**
         * used to Create the new directory for colormatic
         */
        if (CameraDirectory.exists()) {

            //name of the directory to search for and make if not found
            String colormaticFileName = "Colormatic";
            boolean colormaticFileFound = false;

            //colormaticDirectory path to make the file
            File colormaticDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/" + "Colormatic");

            //Creates a list of all directories in the file.
            File[] files = CameraDirectory.listFiles();

            //checks to make sure the directory doesn't already exist. Ovoid overwriting
            for(File fileIterator: files){
                if( ( fileIterator.isDirectory() ) && ( fileIterator.getName() == colormaticFileName ) ){
                    colormaticFileFound = true;
                }
            }

            //create the Colormatic Folder if there was none found from search above.
            if(!colormaticFileFound){
                colormaticDirectory.mkdirs();
            }
            ColormaticImageDirectoryPath = colormaticDirectory.getName();

        }
        else
        {
            File newColormaticDirectory = new File("/sdcard/DirName/");

            //double check directory really doesn't exist creates own directory
            if(!CameraDirectory.exists())
            {
                newColormaticDirectory.mkdir();
            }

            ColormaticImageDirectoryPath = newColormaticDirectory.getName();
        }

        return ColormaticImageDirectoryPath;

       /* File file = new File(new File("/sdcard/DirName/"), fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    /**
     * Adds the standard 3 Presets to the beginning of the list.
     */
    private void setStandardPresets() {
        Preset temp;
        temp = new Preset();
        temp.setName("Deuteranomaly");
        presetList.add(temp);

        temp = new Preset();
        temp.setName("Protanopia");
        presetList.add(temp);

        temp = new Preset();
        temp.setName("Protanomaly");
        presetList.add(temp);
    }

    /**
     * Loads the presets from the Gson. But first, it checks to see if there is anything in it by looking for
     * the count variable that was stored. If there is a count variable, then there is something there, if not,
     * then it skips to the function call: addUserPreset().
     *
     * <p>If there is something that can be grabbed from Shared Preferences, then it loops through and adds each
     * Preset to the list that was destroyed when the activity was changed. </p>
     */
    private void loadPresets() {
        Gson gson = new Gson();
        String jsonCount = mPrefs.getString("Count", "");
        if (mPrefs.contains("Count")) {
            Log.e(TAG, "There was something saved");
            int newCount = gson.fromJson(jsonCount, int.class);
            for (int i = 0; i < newCount; i++) {
                String json = mPrefs.getString("Preset " + i, "");
                Preset p = gson.fromJson(json, Preset.class);
                presetList.add(p);
            }
        }
        addUserPreset();
    }

    /**
     * This functions will take the last 3 Presets in the list and then take the names and store
     * them in global String variables. Later, the popup menu will call those global variables and
     * display them.
     */
    private void setLast3PresetNames() {
        Preset temp;
        Log.e(TAG, "There are "+presetList.size()+" Presets in the list");

        temp = presetList.get(presetList.size()-1);
        preset1 = temp.getName();

        temp = presetList.get(presetList.size()-2);
        preset2 = temp.getName();

        temp = presetList.get(presetList.size()-3);
        preset3 = temp.getName();

        savePresets();

        Log.e(TAG, "List contents are: ");
        for (int i = 0; i < presetList.size(); i++) {
            Log.e(TAG, "         Index: "+i+" - "+presetList.get(i).getName());
        }
    }

    /**
     * This function does 3 things. 1) It clears the list so that there is nothing in it. 2) It loads back in
     * the 3 standard Presets. 3) it sets the last 3 presets to display on the pop-up menu.
     */
    private void resetPresets() {
        presetList.clear();
        setStandardPresets();
        setLast3PresetNames();
    }

    /**
     * Creates a new Gson object to store the Preset in Shared Preferences. It loops through the list and saves
     * each one into the prefsEditor. After it has saved each list, it keeps a count variable that keeps track of
     * how many items are in the list. It seemed to be easier that way instead of converting the list into a set
     * and saving the set.
     */
    private void savePresets() {
        Gson gson = new Gson();
        int i;
        for(i = 0; i < presetList.size(); i++) {
            String json = gson.toJson(presetList.get(i));
            prefsEditor.putString("Preset "+i, json);
        }
        int count = i;
        String json = gson.toJson(count);
        prefsEditor.putString("Count", json);
        prefsEditor.apply();
    }

    public void crosshairButton(View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        int pixel = bitmap.getPixel((bitmap.getWidth() / 2), (bitmap.getHeight() / 2));
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        String hex = String.format("#%02x%02x%02x", r, g, b);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Preset Color", hex);
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);

        Toast.makeText(MainActivity.this, "Copied hex code to clipboard: \n" + colorName + ": " + hex, Toast.LENGTH_SHORT).show();
    }

    /**
     * This function will take a bundle from the CreatePreset activity. After the bundle is received and not null, it creates
     * a new Preset and sets the name and color that was grabbed from the bundle. After that, it adds it the end of the list.
     */
    public void addUserPreset() {
        // get the information from the 3rd activity, CreatePreset
        Bundle extras = getIntent().getExtras();
        //assert extras != null;
        if (extras == null) {
            Log.e(TAG, "Bundle is null");
            return;
        }
        newPresetName = extras.getString("PRESET_NAME");
        newColor = extras.getString("COLOR");
        Preset p = new Preset();
        p.setName(newPresetName);
        p.setColor(newColor);
        Log.e(TAG, "Adding preset: "+p.getName()+" | "+p.getColor());
        presetList.add(p);
    }

    public void createDatabaseMap() {
        mColors.put("Alice Blue", Color.rgb(240,248,255));
        mColors.put("Aqua", Color.rgb(0,255,255));
        mColors.put("Aquamarine", Color.rgb(127,255,212));
        mColors.put("Azure", Color.rgb(240,255,255));
        mColors.put("Beige", Color.rgb(245,245,220));
        mColors.put("Black", Color.rgb(0, 0, 0));
        mColors.put("Blue Violet", Color.rgb(138,43,226));
        mColors.put("Blue", Color.rgb(0, 0, 255));
        mColors.put("Brown", Color.rgb(165, 42, 42));
        mColors.put("Cadet Blue", Color.rgb(95,158,160));
        mColors.put("Chartreuse", Color.rgb(127,255,0));
        mColors.put("Coral", Color.rgb(255,127,80));
        mColors.put("Cornflower Blue", Color.rgb(100,149,237));
        mColors.put("Cornsilk", Color.rgb(255,248,220));
        mColors.put("Crimson", Color.rgb(220,20,60));
        mColors.put("Dark Blue", Color.rgb(0, 0, 139));
        mColors.put("Dark Cyan", Color.rgb(0,139,139));
        mColors.put("Dark Gray", Color.rgb(128, 128, 128));
        mColors.put("Dark Green", Color.rgb(0,100,0));
        mColors.put("Dark Khaki", Color.rgb(189,183,107));
        mColors.put("Dark Magenta", Color.rgb(139,0,139));
        mColors.put("Dark Olive Green", Color.rgb(85,107,47));
        mColors.put("Dark Orange", Color.rgb(255, 140, 0));
        mColors.put("Dark Orchid", Color.rgb(153,50,204));
        mColors.put("Dark Red", Color.rgb(139,0,0));
        mColors.put("Dark Salmon", Color.rgb(233,150,122));
        mColors.put("Dark Sea Green", Color.rgb(143,188,143));
        mColors.put("Dark Slate Blue", Color.rgb(72,61,139));
        mColors.put("Dark Slate Gray", Color.rgb(47,79,79));
        mColors.put("Dark Turquoise", Color.rgb(0,206,209));
        mColors.put("Dark Violet", Color.rgb(148,0,211));
        mColors.put("Deep Pink", Color.rgb(255,20,147));
        mColors.put("Deep Sky Blue", Color.rgb(0, 191, 255));
        mColors.put("Dim Gray", Color.rgb(105,105,105));
        mColors.put("Dodger Blue", Color.rgb(30,144,255));
        mColors.put("Dry Water Stain", Color.rgb(216,191,216));
        mColors.put("Fire Brick", Color.rgb(178,34,34));
        mColors.put("Fuchsia", Color.rgb(255,0,255));
        mColors.put("Ghost White", Color.rgb(248,248,255));
        mColors.put("Gold", Color.rgb(255, 215, 0));
        mColors.put("Goldenrod", Color.rgb(218,165,32));
        mColors.put("Gray", Color.rgb(169,169,169));
        mColors.put("Green Yellow", Color.rgb(173,255,47));
        mColors.put("Green", Color.rgb(0, 128, 0));
        mColors.put("Honeydew", Color.rgb(240,255,240));
        mColors.put("Hot Pink", Color.rgb(255,105,180));
        mColors.put("Indian Red", Color.rgb(205,92,92));
        mColors.put("Indigo", Color.rgb(75, 0, 130));
        mColors.put("Ivory", Color.rgb(255,255,240));
        mColors.put("Khaki", Color.rgb(240,230,140));
        mColors.put("Lavender Blush", Color.rgb(255,240,245));
        mColors.put("Lavender", Color.rgb(230,230,250));
        mColors.put("Light Blue", Color.rgb(173, 216, 230));
        mColors.put("Light Coral", Color.rgb(240,128,128));
        mColors.put("Light Cyan", Color.rgb(224,255,255));
        mColors.put("Light Goldenrod Yellow", Color.rgb(250,250,210));
        mColors.put("Light Gray", Color.rgb(211, 211,211));
        mColors.put("Light Green", Color.rgb(144,238,144));
        mColors.put("Light Pink", Color.rgb(255,182,193));
        mColors.put("Light Salmon", Color.rgb(255,160,122));
        mColors.put("Light Sea Green", Color.rgb(32,178,170));
        mColors.put("Light Sky Blue", Color.rgb(135,206,235));
        mColors.put("Light Slate Gray", Color.rgb(119,136,153));
        mColors.put("Light Steel Blue", Color.rgb(176,196,222));
        mColors.put("Light Wood", Color.rgb(205,133,63));
        mColors.put("Light Yellow", Color.rgb(255,255,224));
        mColors.put("Lime Green", Color.rgb(50,205,50));
        mColors.put("Lime", Color.rgb(0, 255, 0));
        mColors.put("Maroon", Color.rgb(128, 0, 0));
        mColors.put("Medium Aquamarine", Color.rgb(102,205,170));
        mColors.put("Medium Blue", Color.rgb(0,0,205));
        mColors.put("Medium Orchid", Color.rgb(186,85,211));
        mColors.put("Medium Purple", Color.rgb(147,112,219));
        mColors.put("Medium Sea Green", Color.rgb(60,179,113));
        mColors.put("Medium Slate Blue", Color.rgb(123,104,238));
        mColors.put("Medium Turquoise", Color.rgb(72,209,204));
        mColors.put("Medium Violet Red", Color.rgb(199,21,133));
        mColors.put("Midnight Blue", Color.rgb(25,25,112));
        mColors.put("Mint Cream", Color.rgb(245,255,250));
        mColors.put("Misty Rose", Color.rgb(255,228,225));
        mColors.put("Moccasin", Color.rgb(255,228,181));
        mColors.put("Olive Drab", Color.rgb(107,142,35));
        mColors.put("Olive", Color.rgb(128,128,0));
        mColors.put("Orange Brown", Color.rgb(210,105,30));
        mColors.put("Orange Red", Color.rgb(255, 69,0));
        mColors.put("Orange", Color.rgb(255, 165, 0));
        mColors.put("Orchid", Color.rgb(218,112,214));
        mColors.put("Pale Goldenrod", Color.rgb(238,232,170));
        mColors.put("Pale Green", Color.rgb(152,251,152));
        mColors.put("Pale Turquoise", Color.rgb(175,238,238));
        mColors.put("Pale Violet Red", Color.rgb(219,112,147));
        mColors.put("Papaya Whip", Color.rgb(255,239,213));
        mColors.put("Peach Puff", Color.rgb(255,218,185));
        mColors.put("Pink", Color.rgb(255,192,203));
        mColors.put("Plum", Color.rgb(221,160,221));
        mColors.put("Purple", Color.rgb(128,0,128));
        mColors.put("Red", Color.rgb(255, 0, 0));
        mColors.put("Rosy Brown", Color.rgb(188,143,143));
        mColors.put("Royal Blue", Color.rgb(65,105,225));
        mColors.put("Saddle Brown", Color.rgb(139,69,19));
        mColors.put("Salmon", Color.rgb(250,128,114));
        mColors.put("Sandy Brown", Color.rgb(244,164,96));
        mColors.put("Sea Green", Color.rgb(46, 139, 87));
        mColors.put("Sea Green", Color.rgb(46,139,87));
        mColors.put("Seashell", Color.rgb(255,245,238));
        mColors.put("Sienna", Color.rgb(160,82,45));
        mColors.put("Silver", Color.rgb(192,192,192));
        mColors.put("Sky Blue", Color.rgb(135,206,250));
        mColors.put("Slate Blue", Color.rgb(106,90,205));
        mColors.put("Slate Gray", Color.rgb(112,128,144));
        mColors.put("Snow", Color.rgb(255,250,250));
        mColors.put("Spring Green", Color.rgb(0,255,127));
        mColors.put("Steel Blue", Color.rgb(70,130,180));
        mColors.put("Tan", Color.rgb(210, 180, 140));
        mColors.put("Tan", Color.rgb(210,180,140));
        mColors.put("Teal", Color.rgb(0,128,128));
        mColors.put("Tomato", Color.rgb(255,99,71));
        mColors.put("Turquoise", Color.rgb(64,224,208));
        mColors.put("Violet", Color.rgb(238,130,238));
        mColors.put("Wheat", Color.rgb(246,222,179));
        mColors.put("White Smoke", Color.rgb(245,245,245));
        mColors.put("White", Color.rgb(255, 255, 255));
        mColors.put("Yellow Green", Color.rgb(154,205,50));
        mColors.put("Yellow", Color.rgb(255, 255, 0));
    }

    /**
     *
     */
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            myTextView = findViewById(R.id.textView);
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform the image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            bitmap = textureView.getBitmap();

            int pixel = bitmap.getPixel((bitmap.getWidth() / 2), (bitmap.getHeight() / 2));
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            colorName = getBestMatchingColorName(pixel);

            myTextView.setBackgroundColor(Color.rgb(r, g, b)); // set background to black
            myTextView.setText(colorName);
//            myTextView.setText("R(" + r + ")\n" + "G(" + g + ")\n" + "B(" + b + ")");
        }
    };

    private String getBestMatchingColorName(int pixelColor) {
        // largest difference is 255 for every colour component
        int currentDifference = 3 * 255;
        // name of the best matching colour
        String closestColorName = null;
        // get int values for all three colour components of the pixel
        int pixelColorR = Color.red(pixelColor);
        int pixelColorG = Color.green(pixelColor);
        int pixelColorB = Color.blue(pixelColor);

        Iterator<String> colorNameIterator = mColors.keySet().iterator();
        // continue iterating if the map contains a next colour and the difference is greater than zero.
        // a difference of zero means we've found an exact match, so there's no point in iterating further.
        while (colorNameIterator.hasNext() && currentDifference > 0) {
            // this colour's name
            String currentColorName = colorNameIterator.next();
            // this colour's int value
            int color = mColors.get(currentColorName);
            // get int values for all three colour components of this colour
            int colorR = Color.red(color);
            int colorG = Color.green(color);
            int colorB = Color.blue(color);
            // calculate sum of absolute differences that indicates how good this match is
            int difference = Math.abs(pixelColorR - colorR) + Math.abs(pixelColorG - colorG) + Math.abs(pixelColorB - colorB);
            // a smaller difference means a better match, so keep track of it
            if (currentDifference > difference) {
                currentDifference = difference;
                closestColorName = currentColorName;
            }
        }
        return closestColorName;
    }

    /**
     * <p>Returns a CameraDevice.StateCallback</p>
     *
     * Holds "onOpened", "onDisconnected", and "onError".
     *
     * <p>onOpened: calls loadPreset() and setLast3PresetNames(). Afterwards, it assigns the CameraDevice object
     * parameter to the local variable and then calls createCameraPreview().</p>
     *
     * <p>onDisconnected: closes the CameraDevice</p>
     *
     * <p>onError: closes the CameraDevice and sets it to null</p>
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");

            loadPresets();
            setLast3PresetNames(); // fills the list with 3 dummy presets if there aren't already 3 Presets.
            //Log.e(TAG, "called setLast3PresetNames()");

            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    /**
     * Creates a new thread and starts it in the background.
     */
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread, then joins it back into the main thread.
     */
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * When the take picture button is pressed, this function captures what is being previewed and
     * stores it in an ImageReader.
     *
     * <p>Specifically, once the ImageReader is initialized and called through an onClickListener,
     * it renders the image through a byte array. After it is rendered by the specific dimensions,
     * it is saved to a file location.</p>
     */
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/colormaticimage" + "/ColormaticImagepic.jpg");


            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Grabs the textureView from the XML and fills the buffer with pixels. It then asks the camera if
     * this program can use it and then creates a capture sessions, or fills the pixels with the
     * corrects colors to represent the outside world.
     */
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the camera and and sets the boundaries. Then it fills the boundaries with pixels. It
     * then checks the permissions and makes sure that it has already granted the permissions.
     */
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            assert manager != null;
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        if (p1) {
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
//            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
//            // captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, CaptureRequest.COLOR_CORRECTION_GAINS)
//            colorCorrection.transform = [ 0 1 2 3 4 5 6 7 8 9]
        }
        else if (p2) {

        }
        else if (p3) {

        }
        else {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        }
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the CameraDevice and sets it to null.
     */
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * Checks the permissions so that the device can use the camera2 API.
     * @param requestCode A code that is received
     * @param permissions String array
     * @param grantResults int array with the results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * When the camera comes back, the backgroundThread starts and the preview starts.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    /**
     * When the camera closes, it stops the backgroundThread and sets the CameraDevice to null.
     */
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}
