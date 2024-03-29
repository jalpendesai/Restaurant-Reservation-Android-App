package com.restaurantreservation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "RestaurantReservation.db";
    private static final int DATABASE_VERSION = 1;

    private String[] tables;
    private String[] tableCreatorString;

    // Constructor
    public DatabaseManager(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Initialize database
    public void InitDatabase(String[] tables, String tableCreatorString[]){
        this.tables = tables;
        this.tableCreatorString = tableCreatorString;
    }

    // Create the database
    public void CreateDatabase(Context context){
        SQLiteDatabase database;
        database = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
    }

    // Delete the database
    public void deleteDatabase(Context context){
        context.deleteDatabase(DATABASE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Drop existing tables
        for(int i = 0; i < tables.length; i++){
            db.execSQL("DROP TABLE IF EXISTS " + tables[i]);
        }

        // Create tables
        for(int i = 0; i < tables.length; i++){
            db.execSQL(tableCreatorString[i]);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public boolean userExists(String tableName, String phoneNumber){
        String selectQuery = "SELECT phoneNumber FROM " + tableName + " WHERE phoneNumber = '" + phoneNumber + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.getCount() > 0){
            return true;
        }
        return  false;
    }

    public boolean requireAuthentication(String phoneNumber){
        String selectQuery = "SELECT * FROM tbl_authentication WHERE phoneNumber = '" + phoneNumber + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst())
        {
            String expiryTime = cursor.getString(2);
            Calendar c = Calendar.getInstance();
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            String currentTime = format.format(c.getTime());

            int expiryMin = Integer.parseInt((expiryTime.split(":"))[1]);
            int currentMin = Integer.parseInt((currentTime.split(":"))[1]);

            if(currentMin < 5){
                currentMin += 60;
            }

            if(currentMin >= expiryMin){
                return true;
            }
        }

        return false;
    }

    // Add record
    public long addRecord(ContentValues values, String tableName, String fields[], String record[]){
        SQLiteDatabase db = this.getWritableDatabase();

        for(int i = 0; i < record.length; i++){
            values.put(fields[i], record[i]);
        }

        long rowId = db.insert(tableName, null, values);
        db.close();
        return rowId;
    }

    // Read all records
    public List getTable(String tableName) {
        List table = new ArrayList(); //to store all rows
        // Select all records
        String selectQuery = "SELECT * FROM " + tableName;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        //scroll over rows and store each row in an array list object
        if (cursor.moveToFirst())
        {
            do
            { // for each row
                ArrayList row = new ArrayList(); //to store one row
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    row.add(cursor.getString(i));
                }

                table.add(row); //add row to the list

            } while (cursor.moveToNext());
        }

        // return table as a list
        return table;
    }

    public String getAuthenticationCode(String phoneNumber){
        String selectQuery = "SELECT * FROM tbl_authentication WHERE phoneNumber = '" + phoneNumber + "'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            return cursor.getString(1);
        }

        return "-1";
    }

    public void generateNewAuthenticationCode(String phoneNumber, int length, int expiryInMin){
        String code = AuthenticationActivity.GenerateCode(length);
        // For simple implementation, only use the local time and calculation
        Calendar c = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        c.add(Calendar.MINUTE, expiryInMin);
        String expiryTime = format.format(c.getTime());
        String[] fields = { "phoneNumber", "code", "expiryTime" };
        String[] records = { phoneNumber, code, expiryTime};
        updateRecord(new ContentValues(), "tbl_authentication", fields, records);
    }

    public boolean getLoginStatus(Context context, String tableName, String phoneNumber, String password){
        String selectQuery = "SELECT * FROM " + tableName + " WHERE phoneNumber = '" + phoneNumber + "'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            if(cursor.getString(1).equals(password)){
                return true;
            }
            else {
                Toast.makeText(context, "Wrong password.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        else {
            Toast.makeText(context, "Account does not exist.", Toast.LENGTH_SHORT).show();
        }
        return  false;
    }

    public boolean TableAvaliable(int tableNumber){
        String selectQuery = "SELECT * FROM " + "tbl_reservation" + " WHERE tableId = " + tableNumber;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() > 0){
            return false;
        }
        return true;
    }

    // check if Reservation Already Exists at the Specific Time
    public boolean ReservationExists(String tableName, String phoneNumber, String date, String time){
        String selectQuery = "SELECT * FROM " + tableName + " WHERE phoneNumber = " + phoneNumber + " AND reservationDate = '" + date + "' AND arrivalTime = '" + time + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() > 0){
           return true;
        }

        return  false;
    }

    // Display the Reservation Table Details
    public ArrayList<Reservation> GetReservationFromPhoneNumber(String phoneNumber){
        String displayQuery = " SELECT * FROM tbl_reservation WHERE phoneNumber =" + phoneNumber;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(displayQuery, null);

        ArrayList<Reservation> reservations = new ArrayList<>();

        if(cursor.moveToFirst()){
            do{
                Reservation r = new Reservation(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3), cursor.getString(4), cursor.getString(5));
                reservations.add(r);
            }while (cursor.moveToNext());
        }
        return reservations;
    }

    // Message Display
    public String SendConfirmationMessage(String phoneNumber){
        String sendQuery = "SELECT * FROM tbl_reservation WHERE phoneNumber =" + phoneNumber;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sendQuery, null);

        cursor.moveToLast();
            return cursor.getString(1) + " for " + cursor.getString(2) + "," +cursor.getString(3);

    }

    // Update a record
    public boolean updateRecord(ContentValues values, String tableName, String fields[], String record[]) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (int i = 1; i < record.length; i++){
            values.put(fields[i], record[i]);
        }

        // updating row with given id = record[0]
        int rows = db.update(tableName, values, fields[0] + " = ?", new String[] { record[0] });
        if(rows > 0){
            return true;
        }
        else {
            return false;
        }
    }

    // Delete a record with a given id
    public int deleteRecord(String tableName, String idName, String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(tableName, idName + " = ?",
                new String[] { id });
        db.close();
        return rows;
    }
}
