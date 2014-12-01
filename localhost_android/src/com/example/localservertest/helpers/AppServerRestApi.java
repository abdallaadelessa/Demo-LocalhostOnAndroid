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
	private static final String AUTH_MESSAGE = "Another device has logged in";
	private static final int SESSION_TIEMOUT_SECS = 10;
	private AppServerRestApiInterface api;
	private SessionUser onlineUser;

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
		if (api == null || (pageToLoad == null || pageToLoad.length() == 0))
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

		NanoHTTPD.Response response = authenticateUrl(cxt, pageToLoad, header,
				method, parameters);

		return response;
	}

	private NanoHTTPD.Response authenticateUrl(Context cxt, String pageToLoad,
			Map<String, String> header, Method method,
			Map<String, String> parameters)
	{
		boolean userAuthenticated = isUserAuthenticated(header);
		NanoHTTPD.Response response = null;
		if (pageToLoad != null)
		{
			// Authenticate
			if (AUTHENTICATED_APIS_AND_URLS.contains(pageToLoad) && !userAuthenticated)
			{
				if (isHtmlPage(pageToLoad))
				{
					pageToLoad = LOGIN_HTML;
				}
				else
				{
					return getUnAuthenticatedResponse();
				}
			}
			else
			{
				response = route(cxt, pageToLoad, header, method, parameters);	
			}
		}
		return response;
	}

	private NanoHTTPD.Response route(Context cxt, String pageToLoad,
			Map<String, String> header, Method method,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response;
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

				String answer = Utils.convertHtmlPageToString(cxt, pageToLoad);
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
		return response;
	}

	// --------------------------------------
	// User Utils

	private Response login(Method method, Map<String, String> header,
			Map<String, String> parameters)
	{
		NanoHTTPD.Response response = new NanoHTTPD.Response(Status.OK,
				NanoHTTPD.MIME_PLAINTEXT, "Internal Error");

		if (method == Method.POST && parameters != null)
		{
			if (parameters
					.containsKey(AppServerRestApi.PARAM_API_LOGIN_PASSWORD))
			{
				String remoteIp = header.get(HTTPSession.REMOTE_ADDR);
				if (onlineUser != null)
				{
					if (SessionUser.checkIp(onlineUser, remoteIp, false))
					{
						// another user has logged in
						response = new NanoHTTPD.Response(Status.OK,
								NanoHTTPD.MIME_PLAINTEXT, AUTH_MESSAGE);
					}
					else
					{
						// refresh session
						response = checkInOnlineUser(remoteIp);
					}
				}
				else
				{
					String password = parameters
							.get(AppServerRestApi.PARAM_API_LOGIN_PASSWORD);
					if (password != null
							&& password.equalsIgnoreCase("abdalla123#"))
					{
						// Password is true
						response = checkInOnlineUser(remoteIp);
					}
					else
					{
						// Wrong password
						response = new NanoHTTPD.Response(Status.OK,
								NanoHTTPD.MIME_PLAINTEXT, "false");
					}
				}
			}
		}
		return response;
	}

	private boolean isUserAuthenticated(Map<String, String> header)
	{
		String remoteIp = header.get(HTTPSession.REMOTE_ADDR);
		boolean canMakeReq = false;
		if (onlineUser != null && remoteIp != null)
		{
			// Check Date
			long now = System.currentTimeMillis();
			long lastAccess = onlineUser.getLastAccessDate();
			int diffInSeconds = (int) ((now - lastAccess) / 1000);
			if (diffInSeconds < SESSION_TIEMOUT_SECS)
			{
				boolean isTheSameIp = SessionUser.checkIp(onlineUser, remoteIp,
						true);
				if (isTheSameIp)
				{
					onlineUser.setLastAccessDate(now);
					canMakeReq = true;
				}
			}
			else
			{
				onlineUser = null;
			}
		}
		return canMakeReq;
	}

	// -------------------------------------
	// helpers

	public NanoHTTPD.Response checkInOnlineUser(String remoteIp)
	{
		NanoHTTPD.Response response;
		onlineUser = new SessionUser(remoteIp, System.currentTimeMillis());
		response = new NanoHTTPD.Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT,
				"true");
		return response;
	}

	private Response getUnAuthenticatedResponse()
	{
		return new NanoHTTPD.Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT,
				AUTH_MESSAGE);
	}

	public boolean isHtmlPage(String pageToLoad)
	{
		return pageToLoad.endsWith(".html");
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

	private static class SessionUser
	{
		private String ip;
		private long lastAccessDate;

		private SessionUser(String ip, long lastAccessDate)
		{
			super();
			this.ip = ip;
			this.lastAccessDate = lastAccessDate;
		}

		public String getIp()
		{
			return ip;
		}

		public long getLastAccessDate()
		{
			return lastAccessDate;
		}

		public void setLastAccessDate(long lastAccessDate)
		{
			this.lastAccessDate = lastAccessDate;
		}

		public static boolean checkIp(SessionUser user, String remoteIp,
				boolean theSame)
		{
			return user != null
					&& user.getIp() != null
					&& (theSame ? user.getIp().equalsIgnoreCase(remoteIp)
							: !user.getIp().equalsIgnoreCase(remoteIp));
		}

	}

	// ---------------------------------------> Apis
	// Login
	private static final String API_LOGIN_URL = "api_login_url";
	public static final String PARAM_API_LOGIN_PASSWORD = "api_login_param_password";
	// Test Ajax
	private static final String API_TEST_AJAX_URL = "ajax_app";
	public static final String PARAM_API_TEST_AJAX_TO_ANDROID = "to_android";
	public static final String PARAM_API_TEST_AJAX_FROM_ANDROID = "from_android";
	// Test Stream
	private static final String API_TEST_STREAM_URL = "ajax_app_stream";
	public static final String PARAM_API_TEST_STREAM_FILE_NAME = "filename";
	// list entries
	private static final String API_LIST_ENTRIES_URL = "list_entries_url";
	// ---------------------------------------> Pages
	private static final String MAIN_PAGE = "index.html";
	private static final String LOGIN_HTML = "login.html";
	// ----------------------------------------

	private static final List<String> AUTHENTICATED_APIS_AND_URLS = Arrays
			.asList(API_TEST_AJAX_URL, API_TEST_STREAM_URL,
					API_LIST_ENTRIES_URL, MAIN_PAGE);

}
