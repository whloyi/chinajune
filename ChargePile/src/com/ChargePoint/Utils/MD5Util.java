package com.ChargePoint.Utils;

import java.security.MessageDigest;

public class MD5Util {

	private static String byteArrayToHexString(byte b[]) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
			resultSb.append(byteToHexString(b[i]));

		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n += 256;
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}

	public static String GetMD5(String origin) {
		String resultString = null;
		try {
			if(null != origin && !origin.equals("")){
				resultString = new String(origin);
				MessageDigest md = MessageDigest.getInstance("MD5");
					resultString = byteArrayToHexString(md.digest(resultString
							.getBytes())).toUpperCase();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return resultString;
	}

	private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

}
