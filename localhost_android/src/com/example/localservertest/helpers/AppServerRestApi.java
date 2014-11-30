package com.example.localservertest.helpers;

import java.util.Map;
import android.content.Context;
import com.example.localservertest.helpers.NanoHTTPD.Method;
import com.example.localservertest.helpers.NanoHTTPD.Response;
import com.example.localservertest.helpers.NanoHTTPD.Response.Status;

public class AppServerRestApi
{
	private static final String INDEX_PAGE = "index.html";
	AppServerRestApiInterface api;

	public AppServerRestApi(AppServerRestApiInterface api)
	{
		super();
		this.api = api;
	}

	// -------------------------------------

	public Response handleRequest(Context cxt, String pageToLoad,
			Method method, Map<String, String> header,
			Map<String, String> parameters, Map<String, String> files)
	{
		if (api == null)
		{
			return new NanoHTTPD.Response(Status.INTERNAL_ERROR,
					NanoHTTPD.MIME_HTML, "Internal Error");
		}
		else
		{
			// remove first \
			pageToLoad = pageToLoad.substring(1);
			if (pageToLoad.length() == 0)
			{
				pageToLoad = INDEX_PAGE;
			}
		}

		NanoHTTPD.Response response = handleUrl(cxt, pageToLoad, method,
				parameters);

		return response;
	}

	private NanoHTTPD.Response handleUrl(Context cxt, String pageToLoad,
			Method method, Map<String, String> parameters)
	{
		NanoHTTPD.Response response = null;

		if (pageToLoad != null)
		{
			// parameters
			switch (pageToLoad)
			{
				case API_TEST_AJAX_URL:
				{
					response = api.testAjax(method, parameters);
					break;
				}
				case API_TEST_STREAM_URL:
				{
					response = api.testStream(method, parameters);
					break;
				}
				case API_LOGIN_URL:
				{
					response = api.login(method, parameters);
					break;
				}
				default:
					String answer = Utils.convertHtmlPageToString(cxt,
							pageToLoad);
					if (answer != null)
					{
						String mimeType = Utils.getMimeType(pageToLoad);
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

	// -------------------------------------

	public interface AppServerRestApiInterface
	{
		public Response login(Method method, Map<String, String> parameters);

		public Response testAjax(Method method, Map<String, String> parameters);

		public Response testStream(Method method, Map<String, String> parameters);

		public Response listEntries(Method method,
				Map<String, String> parameters);
	}

	// ---------------------------------------> Apis
	// Login
	private static final String API_LOGIN_URL = "api_login_url";
	public static final String API_LOGIN_PASSWORD_PARAM = "api_login_param_password";
	// ----->
	// Test Ajax
	private static final String API_TEST_AJAX_URL = "ajax_app";
	public static final String API_TEST_AJAX_TO_ANDROID_PARAM = "to_android";
	public static final String API_TEST_AJAX_FROM_ANDROID_PARAM = "from_android";
	// ---->
	// Test Stream
	private static final String API_TEST_STREAM_URL = "ajax_app_stream";
	public static final String API_TEST_STREAM_FILE_NAME_PARAM = "filename";
	// ----->
}
