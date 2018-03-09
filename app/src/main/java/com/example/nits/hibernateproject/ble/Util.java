package com.example.nits.hibernateproject.ble;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Environment;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util {

    public final static String APP_DIR = Environment.getExternalStorageDirectory() + "/GOQii-Attendance";
    public final static String REPORT_FILE_NAME = "scanned-report.csv";
    private static ProgressDialog progressDialog;

    public static String Show20Hexes(byte[] bytes) {

        StringBuffer buffer = new StringBuffer();
        for (byte b : bytes)
            buffer.append(Convert2HexAscii(b) + " ");

        return buffer.toString();
    }

    public static String ShowDecimal(byte b) {

        byte divisor = 0xA;
        byte last = (byte) (b % divisor);
        last += 0x30;
        b /= divisor;
        if (b == 0)
            return new String(new byte[]{last});

        byte middle = (byte) (b % divisor);
        middle += 0x30;
        b /= divisor;

        if (b == 0)
            return new String(new byte[]{middle, last});

        byte first = b;

        return new String(new byte[]{first, middle, last});
    }

    ///
    /// Convert hex decimal to hex ASCII string.
    ///
    private static String Convert2HexAscii(byte b) {

        char l_b = (char) (b & 0xF);
        char h_b = (char) ((b & 0xF0) >> 4);

        if (h_b < 10 && l_b < 10) {
            h_b += 0x30;
            l_b += 0x30;

        } else if (h_b >= 10 && l_b >= 10) {
            h_b += 0x37;
            l_b += 0x37;

        } else if (h_b >= 10 && l_b < 10) {
            h_b += 0x37;
            l_b += 0x30;

        } else {
            h_b += 0x30;
            l_b += 0x37;
        }

        return (h_b + "") + (l_b + "");

    }

    public static byte[] CopyByteArray(byte[] source, int index, int length) {

        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {

            b[i] = source[index + i];

        }
        return b;
    }

    public static void CopyByteArray(byte[] source, byte[] destination, int s_index, int d_index, int length) {

        try {

            for (int i = 0; i < length; i++) {

                destination[i + d_index] = source[i + s_index];
            }

        } catch (ArrayIndexOutOfBoundsException e) {

            Log.e("", "Source.Length:" + source.length + ",Destination.Length:" + destination.length);

        }

    }

    public static void putOnArray(byte[] data, int index, int value) {

        try {

            data[index] = (byte) (value & 0xFF);
            data[index + 1] = (byte) (value >>> 0x8 & 0xFF);
            data[index + 2] = (byte) (value >>> 0x10 & 0xFF);
            data[index + 3] = (byte) (value >>> 0x18 & 0xFF);

        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    public static void putOnArray(byte[] data, int index, short value) {

        try {

            data[index] = (byte) (value & 0xFF);
            data[index + 1] = (byte) (value >>> 0x8 & 0xFF);

        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    public static String ShowBytesSeparatedWithChar(byte[] data, char symbol) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < data.length; i++) {

            buffer.append(Convert2HexAscii(data[i]));
            if (i != data.length - 1) {

                buffer.append(symbol);

            }

        }

        return buffer.toString();
    }

    public static int ConvertBCD2Decimal(byte bcd_code) {

        int result = 0;
        result += (bcd_code & 0xF);
        result += (bcd_code >>> 0x4 & 0xF) * 10;
        return result;

    }

    public static int convertByteToInt(byte b) {
        int value = 0;
        value = (value << 8) | b;
        return value;
    }

    public static String ConvertDecToHex(int dec) {
//		return Integer.toString(dec, 16);
        String str = Integer.toHexString(dec);
        if (str.length() == 1)
            str = "0" + str;

        return str.substring((str.length() - 2), (str.length() - 0)).toUpperCase();
    }

    // the decimal range only from 0x0 to 0x63
    public static byte ConvertDecimal2BCD(byte decimal) {

        byte result = 0;
        result += (decimal % 10);
        result += (decimal / 10 << 0x4);
        return result;

    }

    public static byte[] ConverInt2BigEndianByteArray(int value) {

        byte[] b = new byte[4];

        b[0] = (byte) ((value >>> 24) & 0xFF);
        b[1] = (byte) ((value >>> 16) & 0xFF);
        b[2] = (byte) ((value >>> 8) & 0xFF);
        b[3] = (byte) (value & 0xFF);

        return b;
    }

    public static int ConvertBigEndianInt2Int(byte[] data, int index) {

        int val = 0;

        val += ((data[index] << 24) & 0xFF000000);
        val += ((data[index + 1] << 16) & 0x00FF0000);
        val += ((data[index + 2] << 8) & 0x0000FF00);
        val += (data[index + 3] & 0x000000FF);

        return val;
    }


    public static String getCurrentDate() {

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c.getTime());

        return formattedDate;

    }


    public static String getModifiedDate(String todayDate, int modifiedDateCount) {
        String modifieddate = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(todayDate));
            c.add(Calendar.DATE, -modifiedDateCount);
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date resultdate = new Date(c.getTimeInMillis());
            modifieddate = sdf.format(resultdate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modifieddate;
    }

    public static String addInLastSync(String lastSyncDate) {

        final long millisToAdd = 7200000; //two hours

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (!lastSyncDate.equalsIgnoreCase("")) {

                Date d = format.parse(lastSyncDate);
                d.setTime(d.getTime() + millisToAdd);
                return format.format(d.getTime());
            } else {
                return getCurrentDate() + " 00:00:00";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lastSyncDate;
    }

    public static String getCurrentDateAndTime() {

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        String formattedDate = df.format(c.getTime());

        return formattedDate;

    }

    public static boolean isDiffGreaterThan2Hours(String lastSyncTime) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            Date startDate = simpleDateFormat.parse(lastSyncTime);
            Date endDate = simpleDateFormat.parse(Util.getCurrentDateAndTime());

            long difference = endDate.getTime() - startDate.getTime();
            if (difference < 0) {
                Date dateMax = simpleDateFormat.parse("24:00");
                Date dateMin = simpleDateFormat.parse("00:00");
                difference = (dateMax.getTime() - startDate.getTime()) + (endDate.getTime() - dateMin.getTime());
            }
            int days = (int) (difference / (1000 * 60 * 60 * 24));
            int hours = (int) ((difference - (1000 * 60 * 60 * 24 * days)) / (1000 * 60 * 60));
            if (hours >= 2)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }




    public static int printDifference(String currentDate, String lastSyncdate) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        Date d1 = null;
        Date d2 = null;

        try {
            d1 = format.parse(lastSyncdate);
            d2 = format.parse(currentDate);

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);

            return (int) diffDays;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }



    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return "";
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "Just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a min ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " min ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
    }

    public static void showProgressDialog(Activity act, String message) {
        progressDialog = new ProgressDialog(act);
        if (!progressDialog.isShowing()) {
            progressDialog.setMessage(message);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
    }

    public static void hideProgressDialog() {

        if (progressDialog != null)
            progressDialog.hide();
    }

}
