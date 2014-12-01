package com.example.localservertest.helpers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import android.content.Context;
import com.example.localservertest.helpers.NanoHTTPD.HTTPSession;
import com.example.localservertest.helpers.NanoHTTPD.Method;
import com.example.localservertest.helpers.NanoHTTPD.Response;
import com.example.localservertest.helpers.NanoHTTPD.Response.Status;

public class AppServerRestApi
{
	private static final int SESSION_TIEMOUT_SECS = 10;
	private AppServerRestApiInterface api;
	private OnlineUser onlineUser;

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
				pageToLoad = MAIN_PAGE;
			}
		}

		NanoHTTPD.Response response = handleUrl(cxt, pageToLoad, header,
				method, parameters);

		return response;
	}

	private NanoHTTPD.Response handleUrl(Context cxt, String pageToLoad,
			Map<String, String> header, Method method,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = null;

		if (pageToLoad != null)
		{
			// Authenticate
			if (!UNAUTHENTICATED_URLS.contains(pageToLoad))
			{
				if (!canMakeRequest(header))
				{
					pageToLoad = LOGIN_HTML;
				}
			}

			// parameters
			switch (pageToLoad)
			{
				case API_LOGIN_URL:
				{
					response = login(method, header, parameters);
					break;
				}
				case API_TEST_AJAX_URL:
				{
					response = api.testAjax(method, header, parameters);
					break;
				}
				case API_TEST_STREAM_URL:
				{
					response = api.testStream(method, header, parameters);
					break;
				}
				case API_LIST_ENTRIES_URL:
				{
					response = api.listEntries(method, header, parameters);
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

	private Response login(Method method, Map<String, String> header,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = new NanoHTTPD.Response(Status.OK,
				NanoHTTPD.MIME_PLAINTEXT, "Internal Error");

		if (method == Method.POST && parameters != null)
		{
			if (parameters
					.containsKey(AppServerRestApi.API_LOGIN_PASSWORD_PARAM))
			{
				String remoteIp = header.get(HTTPSession.REMOTE_ADDR);

				Utils.log("remoteIp " + remoteIp);
				if (onlineUser != null)
				{
					Utils.log("Online User Ip " + onlineUser.getIp());
				}

				if (onlineUser != null && onlineUser.getIp() != null
						&& !onlineUser.getIp().equalsIgnoreCase(remoteIp))
				{
					response = new NanoHTTPD.Response(Status.OK,
							NanoHTTPD.MIME_PLAINTEXT,
							"Another device has logged in");
				}
				else
				{
					String password = parameters
							.get(AppServerRestApi.API_LOGIN_PASSWORD_PARAM);
					if (password != null
							&& password.equalsIgnoreCase("abdalla123#"))
					{
						onlineUser = new OnlineUser(remoteIp,
								System.currentTimeMillis());
						response = new NanoHTTPD.Response(Status.OK,
								NanoHTTPD.MIME_PLAINTEXT, "true");
					}
					else
					{
						response = new NanoHTTPD.Response(Status.OK,
								NanoHTTPD.MIME_PLAINTEXT, "false");
					}
				}
			}
		}
		return response;
	}

	private boolean canMakeRequest(Map<String, String> header)
	{
		boolean canMakeReq = false;
		String remoteIp = header.get(HTTPSession.REMOTE_ADDR);
		if (onlineUser != null)
		{
			String onlineUserIp = onlineUser.getIp();
			if (onlineUserIp != null && onlineUserIp.equalsIgnoreCase(remoteIp))
			{
				// Check Date
				long now = System.currentTimeMillis();
				long lastAccess = onlineUser.lastAccessDate;
				int diffInSeconds = (int) ((now - lastAccess) / 1000);
				if (diffInSeconds < SESSION_TIEMOUT_SECS)
				{
					canMakeReq = true;
				}
				else
				{
					onlineUser = null;
				}
			}
		}
		return canMakeReq;
	}

	// -------------------------------------

	public interface AppServerRestApiInterface
	{
		public Response testAjax(Method method, Map<String, String> header,
				Map<String, String> parameters);

		public Response testStream(Method method, Map<String, String> header,
				Map<String, String> parameters);

		public Response listEntries(Method method, Map<String, String> header,
				Map<String, String> parameters);
	}

	class OnlineUser
	{
		private String ip;
		private long lastAccessDate;

		private OnlineUser(String ip, long lastAccessDate)
		{
			super();
			this.ip = ip;
			this.lastAccessDate = lastAccessDate;
		}

		public String getIp()
		{
			return ip;
		}

		public void setIp(String ip)
		{
			this.ip = ip;
		}

		public long getLastAccessDate()
		{
			return lastAccessDate;
		}

		public void setLastAccessDate(long lastAccessDate)
		{
			this.lastAccessDate = lastAccessDate;
		}

		@Override
		public boolean equals(Object o)
		{
			boolean isEqual = false;
			if (o != null && o instanceof OnlineUser)
			{
				OnlineUser aUser = (OnlineUser) o;
				String aUserIp = aUser.getIp();
				if (getIp() != null && aUserIp.equalsIgnoreCase(getIp()))
				{
					isEqual = true;
				}
			}
			return isEqual;
		}
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
	// list entries
	private static final String API_LIST_ENTRIES_URL = "list_entries_url";
	// ----->

	// ---------------------------------------> Apis
	private static final String MAIN_PAGE = "index.html";
	private static final String LOGIN_HTML = "login.html";
	private static final List<String> UNAUTHENTICATED_URLS = Arrays.asList(
			LOGIN_HTML, API_LOGIN_URL);
}
