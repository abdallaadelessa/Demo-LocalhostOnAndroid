package com.example.localservertest.test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import com.example.localservertest.controllers.modelcontroller.AppServerRestApi;
import com.example.localservertest.controllers.modelcontroller.AppServerRestApi.RESPONSE_ACTION;
import com.example.localservertest.controllers.modelcontroller.AppServerRestApi.ResponseData;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD.Method;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD.Response;
import com.example.localservertest.controllers.modelcontroller.NanoHTTPD.Response.Status;
import com.example.localservertest.controllers.service.HttpServerService;
import com.example.localservertest.controllers.service.HttpServerService.ListModel;
import com.example.localservertest.helpers.Utils;
import com.google.gson.Gson;

public class ServerServiceTestCase extends ServiceTestCase<HttpServerService>
{

	public ServerServiceTestCase()
	{
		super(HttpServerService.class);
	}

	/**
	 * Test basic startup/shutdown of Service
	 */
	@MediumTest
	public void testStartable() throws Exception
	{
		Intent startIntent = new Intent();
		startIntent.setClass(getContext(), HttpServerService.class);
		startService(startIntent);
		HttpServerService service = getService();

		assertNotNull(service);

		Field field = HttpServerService.class
				.getDeclaredField("appServerRestApi");
		field.setAccessible(true);
		AppServerRestApi obj = (AppServerRestApi) field.get(service);

		testUserNotLoggedIn(service, obj);

		testUserLoggedIn(service, obj);

		testAnotherUserTryingToLogin(service, obj);
	}

	// -----------------------------------------------

	private void testUserNotLoggedIn(HttpServerService service,
			AppServerRestApi obj) throws IOException
	{
		Response resp1 = obj.handleRequest(service, "/index.html", Method.GET,
				new HashMap<String, String>(), new HashMap<String, String>(),
				new HashMap<String, String>());

		assertEquals(Utils.convertHtmlPageToString(service, "login.html"),
				Utils.getString(resp1.getData()));

		Response resp2 = obj.handleRequest(service, "/", Method.GET,
				new HashMap<String, String>(), new HashMap<String, String>(),
				new HashMap<String, String>());

		assertEquals(Utils.convertHtmlPageToString(service, "login.html"),
				Utils.getString(resp2.getData()));

		Response resp3 = obj.handleRequest(service, "/login.html", Method.GET,
				new HashMap<String, String>(), new HashMap<String, String>(),
				new HashMap<String, String>());

		assertEquals(Utils.convertHtmlPageToString(service, "login.html"),
				Utils.getString(resp3.getData()));

		Response resp4 = obj.handleRequest(service, "/list_entries_url",
				Method.POST, new HashMap<String, String>(),
				new HashMap<String, String>(), new HashMap<String, String>());

		Response expected = RESPONSE_ACTION.getErrorResponse(service,
				RESPONSE_ACTION.NOT_AUTHENTICATED);

		assertEquals(Utils.getString(expected.getData()),
				Utils.getString(resp4.getData()));
	}

	private void testUserLoggedIn(HttpServerService service,
			AppServerRestApi obj) throws IOException
	{

		HashMap<String, String> header = new HashMap<String, String>();
		header.put(AppServerRestApi.HEADER_PARAM_REMOTE_ADDR, "192.168.1.1");

		HashMap<String, String> params = new HashMap<String, String>();
		params.put(AppServerRestApi.PARAM_API_LOGIN_PASSWORD, "abdalla123#");

		Response loginResp = obj.handleRequest(service, "/"
				+ AppServerRestApi.API_LOGIN_URL, Method.POST, header, params,
				new HashMap<String, String>());

		assertEquals(
				Utils.getString(RESPONSE_ACTION.getErrorResponse(service,
						RESPONSE_ACTION.PASSWORD_RIGHT).getData()),
				Utils.getString(loginResp.getData()));

		Response resp1 = obj.handleRequest(service, "/index.html", Method.GET,
				header, new HashMap<String, String>(),
				new HashMap<String, String>());

		assertEquals(Utils.convertHtmlPageToString(service, "index.html"),
				Utils.getString(resp1.getData()));

		Response resp2 = obj.handleRequest(service, "/", Method.GET, header,
				new HashMap<String, String>(), new HashMap<String, String>());

		assertEquals(Utils.convertHtmlPageToString(service, "index.html"),
				Utils.getString(resp2.getData()));

		Response resp3 = obj.handleRequest(service, "/login.html", Method.GET,
				header, new HashMap<String, String>(),
				new HashMap<String, String>());

		assertEquals(Utils.convertHtmlPageToString(service, "login.html"),
				Utils.getString(resp3.getData()));

		Response resp4 = obj.handleRequest(service, "/list_entries_url",
				Method.POST, header, new HashMap<String, String>(),
				new HashMap<String, String>());

		List<ListModel> models = new ArrayList<ListModel>();
		models.add(new ListModel(1, "tea"));
		models.add(new ListModel(2, "coffee"));
		models.add(new ListModel(3, "juice"));
		models.add(new ListModel(4, "ice cream"));
		Gson gson = new Gson();
		String data = gson.toJson(models);

		Response expected = new NanoHTTPD.Response(Status.OK,
				NanoHTTPD.MIME_PLAINTEXT, ResponseData.sendResponse(true, data));

		String actual = Utils.getString(resp4.getData());
		assertEquals(Utils.getString(expected.getData()), actual);
	}

	private void testAnotherUserTryingToLogin(HttpServerService service,
			AppServerRestApi obj) throws IOException
	{
		HashMap<String, String> header = new HashMap<String, String>();
		header.put(AppServerRestApi.HEADER_PARAM_REMOTE_ADDR, "192.168.1.2");

		HashMap<String, String> params = new HashMap<String, String>();
		params.put(AppServerRestApi.PARAM_API_LOGIN_PASSWORD, "abdalla123#");

		String expected = Utils.getString(RESPONSE_ACTION.getErrorResponse(
				service, RESPONSE_ACTION.ANOTHER_USER_LOGGED).getData());

		Response loginResp = obj.handleRequest(service, "/"
				+ AppServerRestApi.API_LOGIN_URL, Method.POST, header, params,
				new HashMap<String, String>());

		String actual = Utils.getString(loginResp.getData());

		assertEquals(expected, actual);

		Response resp4 = obj.handleRequest(service, "/list_entries_url",
				Method.POST, header, new HashMap<String, String>(),
				new HashMap<String, String>());

		String actual4 = Utils.getString(resp4.getData());
		assertEquals(expected, actual4);
	}
}
