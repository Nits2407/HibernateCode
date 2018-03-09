package com.example.nits.hibernateproject.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class Tools {
	public static int OTAtype = 0;
	public static BluetoothDevice device = null;
	public static RFLampDevice mDevice = null;
	public static boolean isPick;
	private static Bitmap after;
	
	public static int getRectWidth(Context context) {
		int width;
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		width = manager.getDefaultDisplay().getWidth();
		return width / 12;
	}

	/**
	 * 锟街斤拷锟斤拷锟斤拷转锟斤拷锟街凤拷
	 * 
	 * @param data
	 * @return
	 */
	public static String byte2Hex(byte[] data) {

		if (data != null && data.length > 0) {
			StringBuilder sb = new StringBuilder(data.length);
			
			for (byte tmp : data) {
				sb.append(String.format("%02X ", tmp));
			}
			return sb.toString();
		}
		return "no data";
	}

	// ********************锟斤拷菁锟斤拷锟斤拷锟斤拷锟斤拷**********************************


	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 锟斤拷取系统时锟斤拷
	 * 
	 * @return
	 */
	public static byte[] getSystemTime() {
		byte[] cal = new byte[7];
		Calendar calendar = Calendar.getInstance();
		cal[0] = (byte) (calendar.get(Calendar.YEAR) & 0xff);
		cal[1] = (byte) (calendar.get(Calendar.YEAR) >> 8 & 0xff);
		cal[2] = (byte) ((calendar.get(Calendar.MONTH) + 1) & 0xff);
		cal[3] = (byte) (calendar.get(Calendar.DAY_OF_MONTH) & 0xff);
		cal[4] = (byte) (calendar.get(Calendar.HOUR_OF_DAY) & 0xff);
		cal[5] = (byte) (calendar.get(Calendar.MINUTE) & 0xff);
		cal[6] = (byte) (calendar.get(Calendar.SECOND) & 0xff);
		Log.e("getSystemTime   " ,"" + Tools.byte2Hex(cal));
		return cal;
	}

	/**
	 * sd锟斤拷锟角凤拷锟斤拷锟�
	 * 
	 * @return
	 */
	public static boolean existSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static class dir {
		@SuppressLint("SdCardPath")
		public static final String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/funlight";
		/** 锟斤拷锟斤拷锟斤拷息图片锟斤拷目录 */
		public static final String pushImage = baseDir + "/images";
		/** speaker目录 */
		public static final String speaker = baseDir + "/media/";
		/** speaker锟侥硷拷 */
		public static final String speakerFile = speaker + "/" + "record.amr";

		/** Log锟斤拷志 **/
		public static final String log = baseDir + "/Log";
		/** 6锟斤拷锟斤拷锟斤拷Log锟斤拷志 **/
		public static final String btCommandLog = log + "/btCommandLog";
	}

	public static List<UUID> parseUuids(byte[] advertisedData) {
		List<UUID> uuids = new ArrayList<UUID>();
		ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
		while (buffer.remaining() > 2) {
			byte length = buffer.get();
			if (length == 0)
				break;

			byte type = buffer.get();
			switch (type) {
			case 0x02: // Partial list of 16-bit UUIDs
			case 0x03: // Complete list of 16-bit UUIDs
				while (length >= 2) {
					uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
					length -= 2;
				}
				break;

			case 0x06: // Partial list of 128-bit UUIDs
			case 0x07: // Complete list of 128-bit UUIDs
				while (length >= 16) {
					long lsb = buffer.getLong();
					long msb = buffer.getLong();
					uuids.add(new UUID(msb, lsb));
					length -= 16;
				}
				break;

			default:
				buffer.position(buffer.position() + length - 1);
				break;
			}
		}

		return uuids;
	}
}
