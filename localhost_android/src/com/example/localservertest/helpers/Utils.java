package com.example.localservertest.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class Utils
{
	private static final String LOG_TAG = "DEBUG";

	/**
	 * Log Msg to logcat
	 */
	public static void log(String msg)
	{

			if (msg == null)
			{
				Log.i(LOG_TAG, "--------------------------");
			}
			else
			{
				Log.i(LOG_TAG, msg);
			}
	}
	
	public static boolean isStringEmpty(CharSequence input)
	{
		return TextUtils.isEmpty(input);
	}
	
	public static String getMimeType(String url)
	{
		String type = null;
		String array[] = splitPathToFileNameAndExtByLastDot(url);
		if (array != null && array.length == 2)
		{
			String extension = array[1];
			if (extension != null && extension.length() > 0)
			{
				extension = extension.toLowerCase(Locale.ENGLISH);
				MimeTypeMap mime = MimeTypeMap.getSingleton();
				type = mime.getMimeTypeFromExtension(extension);
			}
		}
		return type;
	}
	
	private static String[] splitPathToFileNameAndExtByLastDot(String path)
	{
		String[] fileNameAndExt = null;
		File tempFile = new File(path);
		String fileName = tempFile.getName();
		int lastIndexOfDot = fileName.lastIndexOf(".");
		if (lastIndexOfDot != -1)
		{
			// Has Extension
			String name = fileName.substring(0, lastIndexOfDot);
			String ext = fileName.substring(lastIndexOfDot + 1);
			fileNameAndExt = new String[] { name, ext };
		}
		else
		{
			// Has No Extension
			fileNameAndExt = new String[] { fileName, null };

		}
		return fileNameAndExt;
	}

	//----------------------
	
	public static String convertHtmlPageToString(Context cxt ,String pageName)
	{
		String answer = "";
		try
		{
			InputStream stream = getFileFromAssets(cxt,pageName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				answer += line+"\n";
			}
			reader.close();
		}
		catch (IOException ioe)
		{
			Log.w("Httpd", ioe.toString());
		}
		return answer;
	}
	
	public static InputStream getFileFromAssets(Context cxt , String fileName) throws IOException
	{
		InputStream stream = cxt.getAssets().open(fileName);
		return stream;
	}


}
