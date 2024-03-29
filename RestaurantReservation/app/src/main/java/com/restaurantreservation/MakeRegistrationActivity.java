package com.restaurantreservation;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MakeRegistrationActivity extends AppCompatActivity {

    DatabaseManager dbManager;
    EditText[] registrationForm;
    private SharedPreferences userInfoPref;
    String phoneNumber;

    Calendar calendar = Calendar.getInstance();
    Button selectDateBtn;
    Button timePickerEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_makeregistration);

        dbManager = LoginActivity.dbManager;
        userInfoPref = getSharedPreferences("userInfo", MODE_PRIVATE);

        phoneNumber = userInfoPref.getString("phoneNumber", "");
        registrationForm = new EditText[]{
                findViewById(R.id.txtPeople)
                // Note is not mandatory
                ,findViewById(R.id.txtSpecialNote)
        };
        selectDateBtn = (Button) findViewById(R.id.book_selectDateBtn);
        timePickerEditText = (Button) findViewById(R.id.book_timePickerBtn);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = sdf.format(new Date());
        selectDateBtn.setText(currentDate);
        timePickerEditText.setText(Calendar.HOUR_OF_DAY + ":" + Calendar.MINUTE);
    }



    public void onClickMakeRegistration(View view) {
        // Loop through each required field, request focus if no filled
        // Note is not mandatory, no need to check
        //for (int i = 0; i < registrationForm.length; i++) {
        for (int i = 0; i < 1; i++) {
            if (registrationForm[i].getText().length() == 0) {
                Toast.makeText(this, registrationForm[i].getHint() + " is required.",
                        Toast.LENGTH_SHORT).show();
                registrationForm[i].requestFocus();
                return;
            }
        }

        String noOfPeople = registrationForm[0].getText().toString();
        String registrationDate = selectDateBtn.getText().toString();
        String arrivalTime = timePickerEditText.getText().toString();
        String specialNote = registrationForm[1].getText().toString();

        String tableNumber = ReservationActivity.GetFreeTable();

        // Check if a table is available
        if(tableNumber == null){
            Toast.makeText(this,"No table available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if Reservation already Exists
        if(dbManager.ReservationExists("tbl_reservation", phoneNumber, registrationDate, arrivalTime)){
            Toast.makeText(this,"You already reserved for this time", Toast.LENGTH_SHORT).show();
        }
        else {
            String[] reservationInfoFields = {"phoneNumber", "tableId", "numberOfGuest", "reservationDate", "arrivalTime", "notes"};
            String[] reservationInfoRecords = {phoneNumber, tableNumber, noOfPeople, registrationDate, arrivalTime, specialNote};

            long id = dbManager.addRecord(new ContentValues(), "tbl_reservation", reservationInfoFields, reservationInfoRecords);

            if (id > -1) {
                Toast.makeText(this, phoneNumber + " reserved a table", Toast.LENGTH_LONG).show();

                SendConfirmationMessage(phoneNumber);

                // Reset all the values to null
                registrationForm[0].setText("");
                registrationForm[1].setText("");

                startActivity(new Intent(this,ViewReservationActivity.class));

            } else {
                Toast.makeText(this, "Unable to make reservation.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void SendConfirmationMessage(String phoneNumber){
        MainActivity.smsMessageSender.SendMessage(phoneNumber,
                "You have successfully booked for " + dbManager.SendConfirmationMessage(phoneNumber) + ". Thank you!");
    }
    public void CancelButton(View view){
        startActivity(new Intent(this,ReservationActivity.class));
    }

    public void book_openDatePicker(View view) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(MakeRegistrationActivity.this, onSelectDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(Calendar.DAY_OF_MONTH);
        datePickerDialog.show();
    }

    public void book_openTimePicker(View view) {
        new TimePickerDialog(MakeRegistrationActivity.this, onTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    DatePickerDialog.OnDateSetListener onSelectDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            selectDateBtn.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
        }
    };
    TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            timePickerEditText.setText(hourOfDay + ":" + minute);
        }
    };
}
