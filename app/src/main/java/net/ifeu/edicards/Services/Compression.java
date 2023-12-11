package net.ifeu.edicards.Services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Compression {

	public static byte[] decompress(byte[] zipContent) throws IOException {
		if (zipContent.length > 4) {
			GZIPInputStream gzipInputStream = new GZIPInputStream(
					new ByteArrayInputStream(zipContent, 4,
							zipContent.length - 4));

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			for (int value = 0; value != -1;) {
				value = gzipInputStream.read();
				if (value != -1) {
					output.write(value);
				}
			}
			gzipInputStream.close();
			output.close();

			return output.toByteArray();
		} else {
			return new byte[0];
		}
	}

	/*
	 * public static byte[] decompress(byte[] compressed) throws IOException {
	 * final int BUFFER_SIZE = 32; ByteArrayInputStream is = new
	 * ByteArrayInputStream(compressed, 4, compressed.length - 4);
	 * GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE); StringBuilder
	 * newString = new StringBuilder(); byte[] data = new byte[BUFFER_SIZE]; int
	 * bytesRead; while ((bytesRead = gis.read(data)) != -1) {
	 * newString.append(new String(data, 0, bytesRead)); } gis.close();
	 * is.close();
	 * 
	 * return newString.toString().getBytes();
	 * 
	 * }
	 */

	/*public static byte[] decompress(byte[] zipContent) throws IOException {

		byte[] bytes = zipContent;
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		GZIPInputStream gzip = new GZIPInputStream(bais);
		try {
			InputStreamReader reader = new InputStreamReader(gzip, "ANSI");
			try {
				String firstLine = new BufferedReader(reader).readLine();

			} finally {
				reader.close();
			}
		} finally {
			gzip.close();
		}

		return bytes;
	}*/
}
