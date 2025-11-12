package com.oracle.demo.timg.iot.iotsonnenuploader.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PasswordConverter {
	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			byte[] argBytes = args[i].getBytes();
			String argBytesString = "";
			for (int j = 0; j < argBytes.length; j++) {
				if (j > 0) {
					argBytesString += ",";
				}
				argBytesString += argBytes[j];
			}
			// String b64 =
			// Base64.getEncoder().encodeToString(args[i].getBytes(StandardCharsets.UTF_8));
			String b64 = new String(Base64.getEncoder().encode(args[i].getBytes(StandardCharsets.UTF_8)),
					StandardCharsets.UTF_8);
			System.out.println("Arg[" + i + "] has value " + args[i] + " as bytes " + args[i].getBytes()
					+ ", as byte array " + argBytesString + ", as base 64 " + b64);
		}
	}

}
