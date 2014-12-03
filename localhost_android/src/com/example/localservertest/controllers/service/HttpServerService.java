package com.example.localservertest.controllers.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLServerSocketFactory;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.example.localservertest.R;
import com.example.localservertest.controllers.modelcontroller.AppServerRestApi;
import com.example.localservertest.controllers.modelcontroller.AppServerRestApi.AppServerRestApiInterface;
import com.example.localservertest.controllers.modelcontroller.AppServerRestApi.ResponseData;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD.Method;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD.Response;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD.Response.Status;
import com.example.localservertest.helpers.Utils;
import com.google.gson.Gson;

public class HttpServerService extends Service implements
		AppServerRestApiInterface
{
	private AppServerRestApi appServerRestApi;
	// ----------------------------------------------
	private static final int PORT = 8080;
	private WebServer server;
	private Handler handler;

	// ----------------------------------------------

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
				appServerRestApi = new AppServerRestApi(this);
				server = new WebServer();
				InputStream in = getResources().openRawResource(R.raw.clientkeystore);
				SSLServerSocketFactory factory = NanoHTTPD
						.makeSSLSocketFactory(in,"123456".toCharArray());
				server.makeSecure(factory);
				server.start();

				final String formatedIpAddress = getIpAddress();
				String msg = "Please access! http://" + formatedIpAddress + ":"
						+ PORT;
				Toast.makeText(this, msg, 1000).show();
				Log.d("DEBUG", msg);
			}
			catch (Exception ioe)
			{
				Log.w("Httpd", "The server could not start.");
			}		
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

			return appServerRestApi.handleRequest(HttpServerService.this, uri,
					method, header, parameters, files);
		}

	}

	public String getIpAddress()
	{
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
		final String formatedIpAddress = String.format("%d.%d.%d.%d",
				(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
		return formatedIpAddress;
	}

	// -------------------------------------------------
	// Api methods

	@Override
	public Response testStream(Method method, Map<String, String> header,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = null;

		if (method == Method.GET && parameters != null)
		{
			if (parameters
					.containsKey(AppServerRestApi.PARAM_API_TEST_STREAM_FILE_NAME))
			{
				String fileName = parameters
						.get(AppServerRestApi.PARAM_API_TEST_STREAM_FILE_NAME);
				InputStream is;
				try
				{
					is = Utils.getFileFromAssets(this, fileName);
					String fileMimeType = Utils.getMimeType(fileName);
					response = new NanoHTTPD.Response(Status.OK, fileMimeType,
							is);
				}
				catch (IOException e)
				{

				}
			}
		}
		return response;
	}

	@Override
	public Response testAjax(Method method, Map<String, String> header,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = null;

		if (method == Method.POST && parameters != null)
		{
			if (parameters
					.containsKey(AppServerRestApi.PARAM_API_TEST_AJAX_FROM_ANDROID))
			{
				String sendResponseText = ResponseData.sendResponse(true,
						"Text From Android");
				response = new NanoHTTPD.Response(Status.OK,
						NanoHTTPD.MIME_PLAINTEXT, sendResponseText);
			}
			else if (parameters
					.containsKey(AppServerRestApi.PARAM_API_TEST_AJAX_TO_ANDROID))
			{
				String txt = parameters
						.get(AppServerRestApi.PARAM_API_TEST_AJAX_TO_ANDROID);
				Message msg = new Message();
				msg.obj = txt;
				handler.sendMessage(msg);

				response = new NanoHTTPD.Response(Status.OK,
						NanoHTTPD.MIME_PLAINTEXT, ResponseData.sendResponse(
								true, "success"));
			}
		}

		return response;
	}

	@Override
	public Response listEntries(Method method, Map<String, String> header,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = null;

		if (method == Method.POST)
		{
			List<ListModel> models = new ArrayList<ListModel>();
			models.add(new ListModel(1, "tea"));
			models.add(new ListModel(2, "coffee"));
			models.add(new ListModel(3, "juice"));
			models.add(new ListModel(4, "ice cream"));
			Gson gson = new Gson();
			String data = gson.toJson(models);

			response = new NanoHTTPD.Response(Status.OK,
					NanoHTTPD.MIME_PLAINTEXT, ResponseData.sendResponse(true,
							data));
		}
		return response;
	}

	// -------------------------------------------------

	public static class ListModel
	{
		int id;
		String name;

		public ListModel(int id, String name)
		{
			super();
			this.id = id;
			this.name = name;
		}

	}
}
