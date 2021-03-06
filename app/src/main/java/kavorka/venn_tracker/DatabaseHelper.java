package kavorka.venn_tracker;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "locations.db";
    private static final String TABLE_NAME = "location_table";
    private static final String COL_1 = "id";
    private static final String COL_2 = "type";
    private static final String COL_3 = "latitude";
    private static final String COL_4 = "longitude";
    private static final String COL_5 = "time";
    private static final String COL_6 = "notes";
    private static DatabaseHelper mInstance = null;


    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    public static DatabaseHelper getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, TYPE STRING," +
                " LATITUDE DOUBLE, LONGITUDE DOUBLE, TIME STRING, NOTES STRING)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    public void addLocation(String type,Double latitude, Double longitude, String description){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, type);
        contentValues.put(COL_3, latitude);
        contentValues.put(COL_4, longitude);
        contentValues.put(COL_6, description);
        db.insert(TABLE_NAME, null, contentValues);
        db.close();
    }


    public void addPolygon (String type, ArrayList<LatLng> polygonPoints){
        removePolygons();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        for (LatLng latLng : polygonPoints){
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_2, type);
            contentValues.put(COL_3, latLng.latitude);
            contentValues.put(COL_4, latLng.longitude);
            db.insert(TABLE_NAME, null, contentValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public ArrayList<ArrayList<LatLng>> getAllHoles() {
        ArrayList<ArrayList<LatLng>> holes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        int count = 6;

        for (int i = 0 ; i <= count ; i++) {
            String sql = "SELECT * FROM " + TABLE_NAME + " WHERE TYPE = 'Temp Hole" + i + "';";
            Cursor cursor = db.rawQuery(sql, null);
            ArrayList<LatLng> hole = new ArrayList<>();

            while (cursor.moveToNext()) {
                double latitude = cursor.getDouble(2);
                double longitude = cursor.getDouble(3);

                hole.add(new LatLng(latitude, longitude));
            }
            cursor.close();
            if (hole.size() > 0) {
                holes.add(hole);
            }
        }

        return holes;
    }


    public void addHole(String type, ArrayList<LatLng> holePoints) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        for (LatLng latLng : holePoints){
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_2, type);
            contentValues.put(COL_3, latLng.latitude);
            contentValues.put(COL_4, latLng.longitude);
            db.insert(TABLE_NAME, null, contentValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }



    // Call this from any method removing anything from the Database
    // Just pass in the sql query
    private void removeFromDb(String sql) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL(sql);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public void removeIntersections() {
        removeFromDb("DELETE FROM " + TABLE_NAME + " WHERE TYPE = 'Intersections';");
    }

    public void removeAllSpawnLocations() {
        removeFromDb("DELETE FROM " + TABLE_NAME + " WHERE TYPE = 'Spawn Location';");
    }

    public void removeSpawnLocation(double latitude, double longitude) {
        removeFromDb("DELETE FROM " + TABLE_NAME +
                " WHERE TYPE = 'Spawn Location'" +
                " AND latitude = " + latitude +
                " AND longitude = " + longitude + ";");
    }

    public void removePolygons() {
        removeFromDb("DELETE FROM " + TABLE_NAME + " WHERE TYPE LIKE 'Temp Polygon%';");
    }

    public void removeCircles() {
        removeFromDb("DELETE FROM " + TABLE_NAME + " WHERE TYPE LIKE 'Temp Circle%';");
    }


    public void removeAllHoles() {
        removeFromDb("DELETE FROM " + TABLE_NAME + " WHERE TYPE LIKE 'Temp Hole%';");
    }


    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME, null);
    }

    public Cursor getAllLocations(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME + " WHERE type = '" + type + "';", null);
    }

    public ArrayList<LatLng> getIntersections() {
        ArrayList<LatLng> intersections= new ArrayList<>();

        Cursor res = getAllLocations("Intersections");
        while(res.moveToNext()) {
            LatLng latLng = new LatLng(res.getDouble(2), res.getDouble(3));
            intersections.add(latLng);
        }
        return intersections;
    }

    public void saveIntersections(ArrayList<LatLng> intersections) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        for (LatLng latLng : intersections){
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_2, "Intersections");
            contentValues.put(COL_3, latLng.latitude);
            contentValues.put(COL_4, latLng.longitude);
            db.insert(TABLE_NAME, null, contentValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public ArrayList<LatLng> getAllRedPolygons(String type) {
        ArrayList<LatLng> redPolygons = new ArrayList<>();

        Cursor res = getAllLocations("Temp Polygon Red");
        while(res.moveToNext()) {
            LatLng latLng = new LatLng(res.getDouble(2), res.getDouble(3));
            redPolygons.add(latLng);
        }
        return redPolygons;
    }

    public void deleteLocation(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try
        {
            db.delete(TABLE_NAME, COL_1 + "=" + id, null);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }



    public void addDescription(String type, Double latitude, Double longitude, String description) {
        // Find 'Spawn Location' that has same latitude and longitude so we can update its description
        String sqlWhere = "TYPE = " + type +
                " AND latitude = " + latitude +
                " AND longitude = " + longitude + ";";

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_6, description);
        db.update(TABLE_NAME, cv, sqlWhere, null);
        db.close();
    }

    public String getDescription(String type, Double latitude, Double longitude) {
        String description = "";

        // Select the description from database of the Spawn Location with the same latitude and longitude
        String sql = "SELECT notes FROM " + TABLE_NAME +
                " WHERE TYPE = " + type +
                " AND latitude = " + latitude +
                " AND longitude = " + longitude + ";";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(sql, null);

        // Grab description from cursor
        while (cursor.moveToNext()) {
            // Make sure it isnt null, would create an error
            if (cursor.getString(0) == null) {
                description = "No Description";
            } else {
                description = cursor.getString(0);
            }
        }
        cursor.close();
        db.close();
        return description;
    }


    public void addTime(double latitude, double longitude, String time) {
        // Find 'Spawn Location' for marker and save time
        String sqlWhere = "TYPE = 'Spawn Location'" +
                " AND latitude = " + latitude +
                " AND longitude = " + longitude + ";";

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_5, time);
        db.update(TABLE_NAME, cv, sqlWhere, null);
        db.close();
    }

    public void loadCsv(File dir, Context ctx) {
        final Context context = ctx;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; ++i) {
                    final File file = files[i];
                    if (file.isDirectory()) {
                        loadCsv(file, context);
                    } else {
                        String filename = file.getName().toLowerCase();
                        if (filename.endsWith(".csv") && filename.contains("spawn")) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder
                                    .setTitle("Import CSV data BETA")
                                    .setMessage("Would you like to import " + file.getName() + "?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            importCSVData(file, context);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                        if (filename.endsWith(".json") && filename.contains("spawn")) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder
                                    .setTitle("Import JSON data BETA")
                                    .setMessage("Would you like to import " + file.getName() + "?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            importJSONData(file, context);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }
                }
            }
        }
    }

    private void importCSVData(File file, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();


        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(file.getAbsoluteFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String[] nextLine = null;
        boolean column = true;
        String description = "No Description";
        int columnLatitude = 0;
        int columnLongitude = 0;
        int columnTime = 0;
        int columnNumber = 0;
        int importCount = 0;
        String type = "Spawn Location";

        try {
            db.beginTransaction();
            assert reader != null;
            while ((nextLine = reader.readNext()) != null) {

                // nextLine[] is an array of values from the line
                if (column) {
                    for (String line : nextLine) {
                        String lineName = line.toLowerCase();
                        if (lineName.contains("lat")) {
                            columnLatitude = columnNumber;
                        } else if (lineName.equals("lng") || lineName.equals("longitude")) {
                            columnLongitude = columnNumber;
                        } else if (lineName.contains("time")) {
                            columnTime = columnNumber;
                        }
                        columnNumber++;
                    }
                    column = false;
                } else {
                    boolean addLocation = true;
                    double latitude = Double.parseDouble(nextLine[columnLatitude]);
                    double longitude = Double.parseDouble(nextLine[columnLongitude]);
                    long time;
                    if (!nextLine[columnTime].equals("")) {
                        time = Long.parseLong(nextLine[columnTime]);
                    }
                    else {
                        // Just needs to be over 60 so it doent get loaded
                        time = 7000;
                    }
                    Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
                    while (res.moveToNext()) {
                        String dbType = res.getString(1);
                        double dbLat = res.getDouble(2);
                        double dbLng = res.getDouble(3);
                        if (dbType.equals("Spawn Location")) {
                            if (dbLat == latitude && dbLng == longitude) {
                                addLocation = false;
                            }
                        }
                    }
                    if (addLocation) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(COL_2, type);
                        contentValues.put(COL_3, latitude);
                        contentValues.put(COL_4, longitude);
                        if (time < 60) {
                            contentValues.put(COL_5, time);
                        } else {
                            contentValues.put(COL_5, "");
                        }
                        contentValues.put(COL_6, description);
                        importCount++;
                        db.insert(TABLE_NAME, null, contentValues);
                    }
                    res.close();
                }
            }
            Toast.makeText(context, "finished importing " + importCount + " Spawn Points", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();

    }

    private String loadJSON(File file) {
        String json = null;
        try {

            InputStream is = new BufferedInputStream(new FileInputStream(file));;

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }

    private void importJSONData(File file, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();

        int importCount = 0;
        String description = "No Description";
        String type = "Spawn Location";
        boolean addLocation = true;
        try {
            db.beginTransaction();
            JSONArray jsonArray = new JSONArray(loadJSON(file));
            for (int i = 0 ; i < jsonArray.length() ; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                double latitude;
                double longitude;

                latitude = jsonObject.getDouble("lat");
                longitude = jsonObject.getDouble("lng");

                Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
                while (res.moveToNext()) {
                    String dbId = res.getString(0);
                    String dbType = res.getString(1);
                    double dbLat = res.getDouble(2);
                    double dbLng = res.getDouble(3);
                    if (dbType.equals("Spawn Location")) {
                        if (dbLat == latitude && dbLng == longitude) {
                            addLocation = false;
                        }
                    }
                }
                if (addLocation) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(COL_2, type);
                    contentValues.put(COL_3, latitude);
                    contentValues.put(COL_4, longitude);
                    contentValues.put(COL_5, "");
                    contentValues.put(COL_6, description);
                    importCount++;
                    db.insert(TABLE_NAME, null, contentValues);
                }
                addLocation = true;
                res.close();
            }
            Toast.makeText(context, "finished importing " + importCount + " Spawn Points", Toast.LENGTH_SHORT).show();




        } catch (JSONException e) {
            e.printStackTrace();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();

    }

    public File exportCsv(Context context){
        File file = null;
        File oldFile = null;
        // If the user has no SD card
        if (Environment.getExternalStorageState() == null) {
            //oldFile = new File(Environment.getDataDirectory() + "/"+Environment.DIRECTORY_DOWNLOADS+ "/", "VennTracker_spawn_data.csv");
            //oldFile.delete();
            file = new File(Environment.getDataDirectory() + "/"+Environment.DIRECTORY_DOWNLOADS+ "/", "VennTracker_spawn_data.csv");
        }
        // If the user has an SD card
        else if (Environment.getExternalStorageState() != null) {
            //oldFile = new File(Environment.getExternalStorageDirectory() + "/"+Environment.DIRECTORY_DOWNLOADS+ "/", "VennTracker_spawn_data.csv");
            //oldFile.delete();
            file = new File(Environment.getExternalStorageDirectory() + "/"+Environment.DIRECTORY_DOWNLOADS+ "/", "VennTracker_spawn_data.csv");
        }
        try {
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file, false));
            file.setWritable(true, false);
            file.createNewFile();

            DatabaseHelper dbhelper = new DatabaseHelper(context.getApplicationContext());
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
            String columns[] = {"Latitude" , "Longitude" , "Time" , "Notes"};
            csvWrite.writeNext(columns);
            while(res.moveToNext())
            {
                //Which column you want to exprort
                if (res.getString(1).equals("Spawn Location")) {
                    String arrStr[] = {String.valueOf(res.getDouble(2)), String.valueOf(res.getDouble(3)), res.getString(4), res.getString(5)};

                    csvWrite.writeNext(arrStr);
                }

            }


            csvWrite.flush();
            csvWrite.close();
            res.close();
            file.setReadable(true, false);

            // Send file via email
            /*Uri u1  =   null;
            u1  =   Uri.fromFile(file);



            Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Venn Tracker Spawn Data");
            sendIntent.putExtra(Intent.EXTRA_STREAM, u1);
            sendIntent.setType("text/html");
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(sendIntent);*/
        } catch (IOException e) {
            Toast.makeText(context, "no file", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(context, "File exported to downloads folder", Toast.LENGTH_SHORT).show();
        return file;
    }

}