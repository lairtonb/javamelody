/*
 * Copyright 2008-2010 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Java Melody is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Melody is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Melody.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.bull.javamelody; // NOPMD

import static net.bull.javamelody.HttpParameters.ACTION_PARAMETER;
import static net.bull.javamelody.HttpParameters.CONNECTIONS_PART;
import static net.bull.javamelody.HttpParameters.COUNTER_PARAMETER;
import static net.bull.javamelody.HttpParameters.CURRENT_REQUESTS_PART;
import static net.bull.javamelody.HttpParameters.DATABASE_PART;
import static net.bull.javamelody.HttpParameters.HEAP_HISTO_PART;
import static net.bull.javamelody.HttpParameters.JOB_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.PART_PARAMETER;
import static net.bull.javamelody.HttpParameters.POM_XML_PART;
import static net.bull.javamelody.HttpParameters.PROCESSES_PART;
import static net.bull.javamelody.HttpParameters.REQUEST_PARAMETER;
import static net.bull.javamelody.HttpParameters.SESSIONS_PART;
import static net.bull.javamelody.HttpParameters.SESSION_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.THREAD_ID_PARAMETER;
import static net.bull.javamelody.HttpParameters.WEB_XML_PART;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test unitaire de la classe CollectorServlet.
 * @author Emeric Vernat
 */
public class TestCollectorServlet {
	private static final String REMOTE_ADDR = "127.0.0.1"; // NOPMD
	private static final String TEST = "test";
	private ServletConfig config;
	private ServletContext context;
	private CollectorServlet collectorServlet;

	/**
	 * Initialisation.
	 */
	@Before
	public void setUp() {
		tearDown();
		System.setProperty(Parameters.PARAMETER_SYSTEM_PREFIX + "mockLabradorRetriever", "true");
		config = createNiceMock(ServletConfig.class);
		context = createNiceMock(ServletContext.class);
		expect(config.getServletContext()).andReturn(context).anyTimes();
		collectorServlet = new CollectorServlet();
	}

	/**
	 * Terminaison.
	 */
	@After
	public void tearDown() {
		// on désactive le stop sur le timer JRobin car sinon les tests suivants ne fonctionneront
		// plus si ils utilisent JRobin
		System.setProperty(Parameters.PARAMETER_SYSTEM_PREFIX + "jrobinStopDisabled", "true");
		System.setProperty(Parameters.PARAMETER_SYSTEM_PREFIX + "mockLabradorRetriever", "false");
		if (collectorServlet != null) {
			collectorServlet.destroy();
		}
	}

	/** Test.
	 * @throws ServletException e */
	@Test
	public void testInit() throws ServletException {
		replay(config);
		replay(context);
		collectorServlet.init(config);
		verify(config);
		verify(context);

		setUp();
		expect(
				context.getInitParameter(Parameters.PARAMETER_SYSTEM_PREFIX
						+ Parameter.LOG.getCode())).andReturn("true").anyTimes();
		expect(
				context.getInitParameter(Parameters.PARAMETER_SYSTEM_PREFIX
						+ Parameter.ALLOWED_ADDR_PATTERN.getCode())).andReturn("127\\.0\\.0\\.1")
				.anyTimes();
		replay(config);
		replay(context);
		collectorServlet.init(config);
		verify(config);
		verify(context);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoGet() throws ServletException, IOException {
		doGet("a", null);
		setUp();
		doGet(null, null);
		setUp();
		doGet(".*", null);
		setUp();
		doGet(null, TEST);
	}

	private void doGet(String pattern, String application) throws IOException, ServletException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getRequestURI()).andReturn("/test/request").anyTimes();
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		final FilterServletOutputStream servletOutputStream = new FilterServletOutputStream(
				new ByteArrayOutputStream());
		expect(response.getOutputStream()).andReturn(servletOutputStream).anyTimes();
		if (application != null) {
			expect(request.getParameter("application")).andReturn(application).anyTimes();
		}
		if (pattern != null) {
			expect(
					context.getInitParameter(Parameters.PARAMETER_SYSTEM_PREFIX
							+ Parameter.ALLOWED_ADDR_PATTERN.getCode())).andReturn(pattern)
					.anyTimes();
			expect(request.getRemoteAddr()).andReturn(REMOTE_ADDR);
		}
		replay(config);
		replay(context);
		replay(request);
		replay(response);
		collectorServlet.init(config);
		// note: sans serveur de http, il n'est pas possible d'avoir une application et un collector
		collectorServlet.doGet(request, response);
		verify(config);
		verify(context);
		verify(request);
		verify(response);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoPost() throws ServletException, IOException {
		doPost(null, null, false);
		setUp();
		doPost(null, null, true);
		setUp();
		doPost(TEST, null, true);
		setUp();
		doPost(TEST, "http://localhost:8090/test", true);
		setUp();
		doPost(TEST, "https://localhost:8090/test", true);
		setUp();
		doPost(TEST, "ftp://localhost:8090/test", true);
	}

	private void doPost(String appName, String appUrls, boolean allowed) throws IOException,
			ServletException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getRequestURI()).andReturn("/test/request").anyTimes();
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		final FilterServletOutputStream servletOutputStream = new FilterServletOutputStream(
				new ByteArrayOutputStream());
		expect(response.getOutputStream()).andReturn(servletOutputStream).anyTimes();
		expect(request.getParameter("appName")).andReturn(appName).anyTimes();
		expect(request.getParameter("appUrls")).andReturn(appUrls).anyTimes();
		if (!allowed) {
			expect(
					context.getInitParameter(Parameters.PARAMETER_SYSTEM_PREFIX
							+ Parameter.ALLOWED_ADDR_PATTERN.getCode())).andReturn("none")
					.anyTimes();
			expect(request.getRemoteAddr()).andReturn(REMOTE_ADDR);
		}
		replay(config);
		replay(context);
		replay(request);
		replay(response);
		collectorServlet.init(config);
		collectorServlet.doPost(request, response);
		verify(config);
		verify(context);
		verify(request);
		verify(response);
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testDoPart() throws IOException, ServletException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		// partParameter null: monitoring principal
		parameters.put(PART_PARAMETER, null);
		doPart(parameters);
		setUp();
		parameters.put(PART_PARAMETER, WEB_XML_PART);
		doPart(parameters);
		setUp();
		parameters.put(PART_PARAMETER, POM_XML_PART);
		doPart(parameters);
		setUp();
		parameters.put(PART_PARAMETER, CURRENT_REQUESTS_PART);
		doPart(parameters);
		setUp();
		parameters.put(PART_PARAMETER, PROCESSES_PART);
		doPart(parameters);
		setUp();
		final TestDatabaseInformations testDatabaseInformations = new TestDatabaseInformations();
		testDatabaseInformations.setUp();
		try {
			parameters.put(PART_PARAMETER, DATABASE_PART);
			doPart(parameters);
			setUp();
			parameters.put(REQUEST_PARAMETER, "0");
			doPart(parameters);
			setUp();
		} finally {
			testDatabaseInformations.tearDown();
		}
		parameters.put(PART_PARAMETER, CONNECTIONS_PART);
		doPart(parameters);
		setUp();
		parameters.put(PART_PARAMETER, HEAP_HISTO_PART);
		doPart(parameters);
		setUp();
		parameters.put(PART_PARAMETER, SESSIONS_PART);
		doPart(parameters);
		setUp();
	}

	/** Test.
	 * @throws ServletException e
	 * @throws IOException e */
	@Test
	public void testAction() throws IOException, ServletException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put("application", TEST);
		parameters.put(ACTION_PARAMETER, Action.GC.toString());
		doPart(parameters);
		setUp();
		parameters.put(ACTION_PARAMETER, Action.CLEAR_COUNTER.toString());
		parameters.put(COUNTER_PARAMETER, "all");
		doPart(parameters);
		setUp();
		parameters.put(ACTION_PARAMETER, Action.INVALIDATE_SESSION.toString());
		parameters.put(SESSION_ID_PARAMETER, "aSessionId");
		doPart(parameters);
		setUp();
		parameters.put(ACTION_PARAMETER, Action.KILL_THREAD.toString());
		parameters.put(THREAD_ID_PARAMETER, "aThreadId");
		doPart(parameters);
		setUp();
		parameters.put(ACTION_PARAMETER, Action.PAUSE_JOB.toString());
		parameters.put(JOB_ID_PARAMETER, "all");
		doPart(parameters);
		setUp();
		parameters.put(ACTION_PARAMETER, "remove_application");
		doPart(parameters);
		setUp();
	}

	private void doPart(Map<String, String> parameters) throws IOException, ServletException {
		final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
		expect(request.getRequestURI()).andReturn("/test/monitoring").anyTimes();
		expect(request.getHeaders("Accept-Encoding")).andReturn(
				Collections.enumeration(Collections.singleton("text/html"))).anyTimes();
		expect(request.getParameter("appName")).andReturn(TEST).anyTimes();
		expect(request.getParameter("appUrls")).andReturn("http://localhost/test").anyTimes();
		for (final Map.Entry<String, String> entry : parameters.entrySet()) {
			expect(request.getParameter(entry.getKey())).andReturn(entry.getValue()).anyTimes();
		}
		final HttpServletResponse response = createNiceMock(HttpServletResponse.class);
		final FilterServletOutputStream servletOutputStream = new FilterServletOutputStream(
				new ByteArrayOutputStream());
		expect(response.getOutputStream()).andReturn(servletOutputStream).anyTimes();
		replay(config);
		replay(context);
		replay(request);
		replay(response);
		collectorServlet.init(config);
		collectorServlet.doPost(request, response);
		collectorServlet.doGet(request, response);
		verify(config);
		verify(context);
		verify(request);
		verify(response);
	}

	/** Test. */
	@Test
	public void testMainWinstone() {
		try {
			Main.main(new String[] { "--help" });
		} catch (final Exception e) {
			// cela s'arrête sur le jar winstone qui n'est pas disponible en tests unitaires
			assertNotNull("ok", e);
		}
	}
}