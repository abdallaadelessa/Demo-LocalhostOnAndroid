package com.example.localservertest.helpers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import android.content.Context;
import com.example.localservertest.helpers.NanoHTTPD.HTTPSession;
import com.example.localservertest.helpers.NanoHTTPD.Method;
import com.example.localservertest.helpers.NanoHTTPD.Response;
import com.example.localservertest.helpers.NanoHTTPD.Response.Status;
import com.google.gson.Gson;

public class AppServerRestApi
{
	private static final int SESSION_TIEMOUT_SECS = 2;
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
		if (api == null || Utils.isStringEmpty(pageToLoad))
		{
			return RESPONSE_ACTION.getErrorResponse(cxt,
					RESPONSE_ACTION.INTERNAL_ERROR);
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
			if (AUTHENTICATED_APIS_AND_URLS.contains(pageToLoad)
					&& !userAuthenticated)
			{
				if (isHtmlPage(pageToLoad))
				{
					pageToLoad = LOGIN_HTML;
				}
				else
				{
					return RESPONSE_ACTION.getErrorResponse(cxt,
							RESPONSE_ACTION.NOT_AUTHENTICATED);
				}
			}
			response = route(cxt, pageToLoad, header, method, parameters);
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
				response = login(cxt, method, header, parameters);
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
				if (!Utils.isStringEmpty(answer))
				{
					String mimeType = Utils.getMimeType(pageToLoad);
					response = new NanoHTTPD.Response(Status.OK, mimeType,
							answer);
				}
				else
				{
					response = RESPONSE_ACTION.getErrorResponse(cxt,
							RESPONSE_ACTION.NOT_FOUND);
				}

				break;
		}

		if (response == null)
		{
			response = RESPONSE_ACTION.getErrorResponse(cxt,
					RESPONSE_ACTION.INTERNAL_ERROR);
		}

		return response;
	}

	// --------------------------------------
	// User Utils

	private Response login(Context cxt, Method method,
			Map<String, String> header, Map<String, String> parameters)
	{
		NanoHTTPD.Response response = RESPONSE_ACTION.getErrorResponse(cxt,
				RESPONSE_ACTION.NOT_FOUND);

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
						response = RESPONSE_ACTION.getErrorResponse(cxt,
								RESPONSE_ACTION.ANOTHER_USER_LOGGED);
					}
					else
					{
						// refresh session
						response = checkInOnlineUser(cxt, remoteIp);
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
						response = checkInOnlineUser(cxt, remoteIp);
					}
					else
					{
						// Wrong password
						response = RESPONSE_ACTION.getErrorResponse(cxt,
								RESPONSE_ACTION.PASSWORD_WRONG);
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

	public NanoHTTPD.Response checkInOnlineUser(Context cxt, String remoteIp)
	{
		onlineUser = new SessionUser(remoteIp, System.currentTimeMillis());
		return RESPONSE_ACTION
				.getErrorResponse(cxt, RESPONSE_ACTION.PASSWORD_RIGHT);
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

	public enum RESPONSE_ACTION
	{
		PASSWORD_RIGHT, PASSWORD_WRONG, NOT_AUTHENTICATED, ANOTHER_USER_LOGGED, NOT_FOUND, INTERNAL_ERROR;

		public static Response getErrorResponse(Context cxt, RESPONSE_ACTION error)
		{
			Response response = null;

			switch (error)
			{
				case PASSWORD_RIGHT:
					response = new NanoHTTPD.Response(Status.OK,
							NanoHTTPD.MIME_PLAINTEXT,
							ResponseData.sendResponse(true, "Success"));
					break;
				case PASSWORD_WRONG:
					response = new NanoHTTPD.Response(Status.OK,
							NanoHTTPD.MIME_PLAINTEXT,
							ResponseData.sendResponse(false,
									"Wrong Password"));
					break;
				case NOT_AUTHENTICATED:
					response = new NanoHTTPD.Response(Status.OK,
							NanoHTTPD.MIME_PLAINTEXT,
							ResponseData.sendResponse(false,
									"Session timeout"));
					break;

				case ANOTHER_USER_LOGGED:
					response = new NanoHTTPD.Response(Status.OK,
							NanoHTTPD.MIME_PLAINTEXT,
							ResponseData.sendResponse(false,
									"Another device has logged in"));
					break;
					
				//----------> Html Mime Type
					
				case INTERNAL_ERROR:
					response = new NanoHTTPD.Response(Status.INTERNAL_ERROR,
							NanoHTTPD.MIME_HTML,"Internal Error");
					break;

				case NOT_FOUND:
					response = new NanoHTTPD.Response(Status.NOT_FOUND,
							NanoHTTPD.MIME_HTML,"Page Not Found");
					break;
				default:
					break;
			}

			return response;
		}
	}

	public static class ResponseData
	{
		public boolean valid = true;
		public String data = null;

		public static String sendResponse(boolean valid, String text)
		{
			ResponseData respData = new ResponseData();
			respData.valid = valid;
			respData.data = text;
			return respData.toJson();
		}

		public String toJson()
		{
			return new Gson().toJson(this);
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
