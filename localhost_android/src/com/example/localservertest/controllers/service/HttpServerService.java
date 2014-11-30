package com.example.localservertest.controllers.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.example.localservertest.helpers.NanoHTTPD;
import com.example.localservertest.helpers.NanoHTTPD.Method;
import com.example.localservertest.helpers.NanoHTTPD.Response;
import com.example.localservertest.helpers.NanoHTTPD.Response.Status;

public class HttpServerService extends Service
{
	// Pages
	private static final String INDEX_PAGE = "index.html";
	// ----------------------------------------------
	// Apis
	private static final String AJAX_APP = "ajax_app";
	private static final String AJAX_APP_FILE_NAME_PARAM = "filename";
	// ---->
	private static final String AJAX_APP_STREAM = "ajax_app_stream";
	private static final String AJAX_APP_TO_ANDROID_PARAM = "to_android";
	private static final String AJAX_APP_FROM_ANDROID_PARAM = "from_android";
	// ----------------------------------------------
	private static final int PORT = 8080;
	private WebServer server;
	private Handler handler;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		startHttpServer();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		handler = new Handler(new Callback()
		{
			@Override
			public boolean handleMessage(Message msg)
			{
				if (msg != null && msg.obj != null && msg.obj instanceof String)
				{
					String text = (String) msg.obj;
					Toast.makeText(HttpServerService.this, text,
							Toast.LENGTH_SHORT).show();
				}

				return false;
			}
		});

		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		stopHttpServer();
	}

	// ------------------------------------------------

	private void startHttpServer()
	{
		stopHttpServer();

		if (server == null)
		{
			try
			{
				server = new WebServer();
				server.start();
				WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
				int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
				final String formatedIpAddress = String.format("%d.%d.%d.%d",
						(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
						(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
				String msg = "Please access! http://" + formatedIpAddress + ":"
						+ PORT;
				Toast.makeText(this, msg, 1000).show();
				Log.d("DEBUG", msg);
			}
			catch (IOException ioe)
			{
				Log.w("Httpd", "The server could not start.");
			}
			Log.w("Httpd", "Web server initialized.");
		}
	}

	private void stopHttpServer()
	{
		if (server != null)
		{
			server.stop();
			server = null;
		}
	}

	// -------------------------------------------------
	// Helper methods

	private InputStream getFileFromAssets(String fileName) throws IOException
	{
		InputStream stream = getAssets().open(fileName);
		return stream;
	}

	private String convertHtmlPageToString(String pageName)
	{
		String answer = "";
		try
		{
			InputStream stream = getFileFromAssets(pageName);
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

	private static String getMimeType(String url)
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

	// ------------------------------------------------

	private class WebServer extends NanoHTTPD
	{

		private static final String TAG = "DEBUG";

		public WebServer() throws IOException
		{
			super(null, PORT);
		}

		@Override
		public Response serve(String uri, Method method,
				Map<String, String> header, Map<String, String> parameters,
				Map<String, String> files)
		{
			Log.d(TAG, uri);

			// remove first \
			String webPage = uri.substring(1);
			if (webPage.length() == 0)
			{
				webPage = INDEX_PAGE;
			}

			return prepareResponse(webPage, method, parameters);
		}

	}

	private Response prepareResponse(String pageToLoad, Method method,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = null;

		if (pageToLoad != null)
		{
			// parameters
			switch (pageToLoad)
			{
				case AJAX_APP:
				{
					if (method == Method.POST)
					{
						response = ajax_app_text(parameters);
					}
					break;
				}
				case AJAX_APP_STREAM:
				{
					if (method == Method.GET)
					{
						response = ajax_app_stream(parameters);
					}
					break;
				}
				default:
					String answer = convertHtmlPageToString(pageToLoad);
					if (answer != null)
					{
						String mimeType;
						if (pageToLoad.endsWith(".js"))
						{
							mimeType = "text/javascript";
						}
						else
						{
							mimeType = getMimeType(pageToLoad);
						}

						response = new NanoHTTPD.Response(Status.OK, mimeType,
								answer);
					}
					else
					{
						response = new NanoHTTPD.Response(Status.NOT_FOUND,
								NanoHTTPD.MIME_HTML, "Page Not Found");
					}

					break;
			}
		}

		return response;
	}

	// -------------------------------------------------
	// Api methods

	private Response ajax_app_text(Map<String, String> parameters)
	{
		NanoHTTPD.Response response = new NanoHTTPD.Response(
				Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "BAD_REQUEST");

		if (parameters != null)
		{
			if (parameters.containsKey(AJAX_APP_FROM_ANDROID_PARAM))
			{
				String text = "hello world from android";
				response = new NanoHTTPD.Response(Status.OK,
						NanoHTTPD.MIME_PLAINTEXT, text);
			}
			else if (parameters.containsKey(AJAX_APP_TO_ANDROID_PARAM))
			{
				String txt = parameters.get(AJAX_APP_TO_ANDROID_PARAM);
				Message msg = new Message();
				msg.obj = txt;
				handler.sendMessage(msg);
				response = new NanoHTTPD.Response(Status.OK,
						NanoHTTPD.MIME_PLAINTEXT, "success");
			}
		}

		return response;
	}

	private Response ajax_app_stream(Map<String, String> parameters)
	{
		NanoHTTPD.Response response = new NanoHTTPD.Response(
				Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "BAD_REQUEST");
		if (parameters != null)
		{
			if (parameters.containsKey(AJAX_APP_FILE_NAME_PARAM))
			{
				String fileName = parameters.get(AJAX_APP_FILE_NAME_PARAM);
				InputStream is;
				try
				{
					is = getFileFromAssets(fileName);
					String fileMimeType = getMimeType(fileName);
					response = new NanoHTTPD.Response(Status.OK,
							fileMimeType, is);
				}
				catch (IOException e)
				{

				}
			}
		}
		return response;
	}

	// -------------------------------------------------
}
