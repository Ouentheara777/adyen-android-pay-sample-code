package com.adyen.adyenshop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.adyen.adyenshop.model.Payment;

/**
 * Created by andrei on 11/18/16.
 */

public class PaymentsDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "AndroidPay.db";
    private static final int DATABASE_VERSION = 1;

    public static final String PAYMENTS_TABLE_NAME = "payments";
    public static final String PAYMENTS_COLUMN_ID = "_id";
    public static final String PAYMENTS_COLUMN_DATE = "date";
    public static final String PAYMENTS_COLUMN_AMOUNT = "amount";
    public static final String PAYMENTS_COLUMN_RECURRING = "recurring";
    public static final String PAYMENTS_COLUMN_ALIAS = "alias";
    public static final String PAYMENTS_COLUMN_SHOPPER_EMAIL = "shopperEmail";
    public static final String PAYMENTS_COLUMN_SHOPPER_REFERENCE = "shopperReference";

    public PaymentsDbHelper(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + PAYMENTS_TABLE_NAME + "(" +
                PAYMENTS_COLUMN_ID + " INTEGER PRIMARY KEY, " +
                PAYMENTS_COLUMN_DATE + " TEXT, " +
                PAYMENTS_COLUMN_AMOUNT + " INTEGER, " +
                PAYMENTS_COLUMN_RECURRING + " BOOLEAN, " +
                PAYMENTS_COLUMN_ALIAS + " TEXT, " +
                PAYMENTS_COLUMN_SHOPPER_EMAIL + " TEXT, " +
                PAYMENTS_COLUMN_SHOPPER_REFERENCE + " TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PAYMENTS_TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public boolean insertPayment(Payment payment) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(PAYMENTS_COLUMN_DATE, String.valueOf(payment.getDate()));
        contentValues.put(PAYMENTS_COLUMN_AMOUNT, payment.getAmount());
        contentValues.put(PAYMENTS_COLUMN_RECURRING, payment.isRecurring());
        contentValues.put(PAYMENTS_COLUMN_ALIAS, payment.getAlias());
        contentValues.put(PAYMENTS_COLUMN_SHOPPER_EMAIL, payment.getShopperEmail());
        contentValues.put(PAYMENTS_COLUMN_SHOPPER_REFERENCE, payment.getShopperReference());

        db.insert(PAYMENTS_TABLE_NAME, null, contentValues);

        return true;
    }

    public boolean updatePayment(int id, Payment payment) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(PAYMENTS_COLUMN_DATE, String.valueOf(payment.getDate()));
        contentValues.put(PAYMENTS_COLUMN_AMOUNT, payment.getAmount());
        contentValues.put(PAYMENTS_COLUMN_RECURRING, payment.isRecurring());
        contentValues.put(PAYMENTS_COLUMN_ALIAS, payment.getAlias());
        contentValues.put(PAYMENTS_COLUMN_SHOPPER_EMAIL, payment.getShopperEmail());
        contentValues.put(PAYMENTS_COLUMN_SHOPPER_REFERENCE, payment.getShopperReference());

        db.update(PAYMENTS_TABLE_NAME, contentValues, PAYMENTS_COLUMN_ID + " = ? ", new String[] {Integer.toString(id)});

        return true;
    }

    public Cursor getPayment(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "SELECT * FROM " + PAYMENTS_TABLE_NAME + " WHERE " +
                PAYMENTS_COLUMN_ID + "=?", new String[] { Integer.toString(id) } );
        return res;
    }

    public Cursor getAllPayments() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "SELECT * FROM " + PAYMENTS_TABLE_NAME, null );
        return res;
    }

    public Integer deletePayment(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(PAYMENTS_TABLE_NAME,
                PAYMENTS_COLUMN_ID + " = ? ",
                new String[] { Integer.toString(id) });
    }

}
