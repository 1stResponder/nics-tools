/**
 * Copyright (c) 2008-2016, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.soa.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;


/**
 * Created with IntelliJ IDEA.
 * User: cbudny
 * Date: 7/3/14
 * Time: 7:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpenAmUtil
{

	private static StandardPBEStringEncryptor encryptor = null;

	    private static final String URL_PROP = "openam.url";
	    private static final String CREATE_PROP = "openam.rest.create";
	    private static final String UPDATAE_PROP = "openam.rest.update";
	    private static final String DELETE_PROP = "openam.rest.delete";
	    private static final String SEARCH_PROP = "openam.rest.search";
	    private static final String AUTH_PROP = "openam.rest.authenticate";
	    private static final String ID_NAME_PROP = "openam.identity.name";
	    private static final String ID_ATTR_NAMES_PROP = "openam.identity.attribute.names";
	    private static final String ID_ATTR_VALUES_PREFIX_PROP = "openam.identity.attribute.values.prefix";
	    private static final String ID_REALM_PROP = "openam.identity.realm";
	    private static final String ID_TYPE_PROP = "openam.identity.type";
	    private static final String ADMIN_TOKEN_ID_PROP = "openam.token.admin";
	    private static final String AUTH_USERNAME_PROP = "openam.identity.auth.username";
	    private static final String AUTH_PASSWORD_PROP = "openam.identity.auth.password";
	    private static final String TOKEN_VALIDATE_URL_PROP = "openam.rest.token.validate";
	    private static final String ID_VALUE_SN = "openam.identity.attribute.values.sn";
	    private static final String ID_VALUE_CN = "openam.identity.attribute.values.cn";
	    private static final String ID_VALUE_UP = "openam.identity.attribute.values.userpassword";
	    private static final String ID_VALUE_MAIL = "openam.identity.attribute.values.mail";
	    private static final String ID_VALUE_ACTIVE = "openam.identity.attribute.values.active";
	    private static final String ID_VALUE_FIRSTNAME = "openam.identity.attribute.values.firstname";
		private static final String URI_PARAM_PROP = "openam.default.uri.param";
		private static final String URI_VALUE_PROP = "openam.default.uri.value";
	    private static final String REALM_VALUE = "openam.user.realm";
	    private static final String INACTIVE_PROP = "openam.values.inactive";
	    private static final String ACTIVE_PROP = "openam.values.active";

	    private static final String ADMIN_CREATOR_USERNAME_PROP = "openam.user.creator.username";
	    private static final String ADMIN_CREATOR_PASS_PROP = "openam.user.creator.password";
	    private static final String OPENAM_DEFAULT_TEMP_PASS_PROP = "openam.user.default.temp.password";

		private static final String DECODE_PROPERTY = "jasypt.password";
		private static final String ALGORITHM = "algorithm";
		private static final String AMPERSAND = "&";
		private static final String PERIOD = ".";
		private static final String GET = "GET";
		private static final String AT = "@";
		private static final String ID_TYPE = "user";
		private static final String ENCODE_TYPE = "UTF-8";

		private String token = null;
		private static final String FIRSTNAME = "EP";
		private static final String LASTNAME = "User";
		private Properties props;

		private static String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


		private static Log _log = LogFactory.getLog(OpenAmUtil.class);

		public OpenAmUtil()
		{
			init();
		}

		private void init()
		{
			 props = new Properties();
	         InputStream in = null;

	         try
	         {
	//                 in = new FileInputStream("openam-tools.properties");
	        	 	in = Thread.currentThread().getContextClassLoader().getResourceAsStream("openam-tools.properties");

	                 if (in != null)
	                 {
	                         props.load(in);
	                         _log.info("Loaded properties");
	                 } else
	                 {
	                	 _log.info("Cannot load properties");
	                 }
	         } catch (IOException ioe)
	         {
	                 ioe.printStackTrace();
	         } finally
	         {
	                 if (in != null)
	                 {
	                         try
	                         {
	                                 in.close();
	                         } catch (IOException ioe)
	                         {
	                                 ioe.printStackTrace();
	                         }
	                 }
	         }

		}

	//	public void serveResource(ResourceRequest req, ResourceResponse res)
	//			throws PortletException, IOException
	//	{
	//		final String id = req.getResourceID(),
	//				u = req.getParameter("userPrefix"),
	//				i = req.getParameter("userIndex"),
	//				n = req.getParameter("numAccounts"),
	//				e = req.getParameter("email");
	//
	//		boolean create = Boolean.valueOf(req.getParameter("create"));
	//
	//		_log.info("Vals: "+ u +", "+ i +", "+ n +", "+ e);
	//
	//		try
	//		{
	//			final int idx = Integer.valueOf(i);
	//			final int num = Integer.valueOf(n);
	//			_log.info("ID: "+ id);
	//			if (id.equalsIgnoreCase("cusers"))
	//			{
	//				if (idx >= 0 && num > 0 && (e != null && e != ""))
	//				{
	//					OutputStream os = null;
	//
	//					try
	//					{
	//						HSSFWorkbook workbook = new HSSFWorkbook();
	//						HSSFSheet sheet = workbook.createSheet("Credentials");
	//
	//						createExcelHeaders(sheet);
	//						int userCount = 0;
	//
	//						for (int j = idx, k=0; k < num; k++, j++)
	//						{
	//							// create user account name
	//							String email = u + j + AT + e;
	//							email = email.toLowerCase();
	//
	////							String result = RandomStringUtils.random(10, chars.toCharArray());
	////							_log.info("would be generating this password: " + result);
	////							addRow(sheet, email, result, userCount++);
	//
	//							if (create)
	//							{
	//								_log.info("Adding user email to openam: " + email);
	//								addUserToOpenAm(email, FIRSTNAME, LASTNAME + j);
	//							}
	//						}
	//
	//						// was a one time run to get a specific list of users into the system
	////						String mccUsers = getProperty("mccUsers");
	////						if (mccUsers != null)
	////						{
	////							String[] users = mccUsers.split(",");
	////							for (String s : users)
	////							{
	////								String email = s + AT + e;
	////								email = email.toLowerCase();
	////
	////								String result = RandomStringUtils.random(10, chars.toCharArray());
	//////								_log.info("would be generating this password: " + result);
	////								addRow(sheet, email, result, userCount);
	////								//addUserToOpenAm(email, "MCC", "User" + userCount);
	////								userCount++;
	////							}
	////						}
	//
	//						String path = getPortletContext().getRealPath("/");
	//						path += "credentials.xls";
	//						File f = new File(path);
	//						FileOutputStream fos = null;
	//						try
	//						{
	//							fos = new FileOutputStream(path);
	//							workbook.write(fos);
	//							_log.info("Wrote to file: " + path);
	//						} catch (Exception ex)
	//						{
	//							ex.printStackTrace();
	//						} finally
	//						{
	//							if (fos != null)
	//								fos.close();
	//						}
	//
	//						ExportUtil.exportXLS(req, res, workbook);
	//
	////						os = res.getPortletOutputStream();
	////						res.addProperty(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate");
	////						res.setContentType("application/xls");
	////						res.addProperty(HttpHeaders.CONTENT_TYPE, "application/xls");
	////						res.addProperty("Content-Disposition", "attachment; filename=Credentials.xls");
	////
	////						workbook.write(os);
	////						_log.info("Wrote workbook to stream");
	////						os.close();
	//						return;
	//					} catch (Exception ex)
	//					{
	//						ex.printStackTrace();
	//					} finally
	//					{
	//						if (os != null)
	//							os.close();
	//					}
	//				}
	//			} else if (id.equalsIgnoreCase("XLS Upload"))
	//			{
	//
	//			}
	//
	//			SessionMessages.add(req, "success");
	//		} catch (NumberFormatException nfe)
	//		{
	//
	//		}
	//
	//	}

		private void createExcelHeaders(HSSFSheet sheet)
		{
			HSSFRow row = sheet.createRow(0);
			HSSFCell cell = row.createCell(0);
			cell.setCellValue("Username");

			cell = row.createCell(1);
			cell.setCellValue("Password");
		}

		public void addRow(HSSFSheet sheet, String user, String pass, int count)
		{
			HSSFRow row = sheet.createRow(count);
			HSSFCell cell = row.createCell(0);
			cell.setCellValue(user);

			cell = row.createCell(1);
			cell.setCellValue(pass);
		}

		private String generateUrl(String base, String... params)
		{
			String url = null;
			if (base != null && params.length > 0)
			{
				url = base ;
				for (String s : params)
				{
					url += AMPERSAND;
					url += s;
				}
			}

			return url;
		}

		public void updateCredentials(FileOutputStream fos, String email, String pass)
		{

			final String base = getProperty(URL_PROP),
					auth = getProperty(AUTH_PROP),
					update = getProperty(UPDATAE_PROP),
					search = getProperty(SEARCH_PROP),
					name = getProperty(ID_NAME_PROP),
					keyPrefix = getProperty(ID_ATTR_NAMES_PROP),
					valPrefix = getProperty(ID_ATTR_VALUES_PREFIX_PROP),
					realm = getProperty(ID_REALM_PROP),
					idType = getProperty(ID_TYPE_PROP),
					tokenid = getProperty(ADMIN_TOKEN_ID_PROP);


			if (base != null && update != null && name != null && keyPrefix != null)
			{
				final String updateUrl = base + update;
				final String uri_param = "uri=realm%3D/%26service%3DldapService";


				if ( !checkToken() )
				{	// authenticating the system user capable of updating identities
					if (!authenticate(base + auth, getUsername(), getPassword(), getProperty(URI_PARAM_PROP)))
					{
						_log.error("Could not authenticate System user, check configured values.");
						return;
					}
				}

				if (this.token == null)
				{
	//				log(fos, "Couldn't authenticate UserCreator account, cannot update credentials for user: " + email);
					_log.error("Couldn't auth userCreator account, can't update " + email);
					return;
				}

				final String nameParam = name +"="+ encode(email);
				final String passParam = keyPrefix +"="+ getProperty(ID_VALUE_UP) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_UP) +"="+ encode(pass);
				final String mailParam = keyPrefix +"="+ getProperty(ID_VALUE_MAIL) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_MAIL) +"="+ encode(email);
				final String activeParam = keyPrefix +"="+ getProperty(ID_VALUE_ACTIVE) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_ACTIVE) +"="+ encode(getProperty(ACTIVE_PROP));
				final String adminParam = tokenid +"="+ this.token;

				String url = generateUrl(updateUrl + nameParam, passParam, mailParam, activeParam, adminParam);
	//			log(fos, "Updating user: "+ email+", with password: " + pass);
	//			log(fos, "Using url: " + url);
				_log.info("Updating user: " + email+" with pass: " + pass);

				String res = executeOpenAmRequest(url);
				if (res != null)
				{
					_log.info("OpenAm Response: " + res);
	//				log(fos, "OpenAM results: " + res);
				}

	//			log(fos, "=====================================================");
	//			log(fos, " "); // newline
			} else
			{

			}


		}

		private void log(FileOutputStream fos, String content)
		{
			try
			{
				fos.write(content.getBytes());
				fos.write("\n".getBytes());
	//			fos.write(content);
	//			fos.newLine();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		public void updateOpenAmUsers(Map<String, String> map)
		{
			String path = "";
			path += "Credential-log.txt";
			File f = new File(path);



			FileWriter fw = null;
			BufferedWriter bw = null;
			FileOutputStream fos = null;

			try
			{

	//			fw = new FileWriter(f.getAbsoluteFile());
	//			bw = new BufferedWriter(fw);
				fos = new FileOutputStream(f);
				_log.info("Map size: " + map.size());
				Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
				while (it.hasNext())
				{
					Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
	//				_log.info("Credential: " + pairs.getKey() +", " + pairs.getValue());

					updateCredentials(fos, pairs.getKey(), pairs.getValue());
				}

				_log.info("Wrote to file: " + path);
			} catch (Exception ex)
			{
				ex.printStackTrace();
			} finally
			{
				try {
					if (fos != null)
						fos.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	//	public void processAction(ActionRequest aReq, ActionResponse aResp)
	//			throws IOException, PortletException
	//	{
	//		final String i = aReq.getParameter("userIndex"),
	//					n = aReq.getParameter("numAccounts"),
	//					e = aReq.getParameter("email"),
	//					u = aReq.getParameter("userPrefix");
	//		ThemeDisplay themeDisplay;
	//
	//
	//		try
	//		{
	//			themeDisplay = (ThemeDisplay)aReq.getAttribute(WebKeys.THEME_DISPLAY);
	//
	//			if (themeDisplay != null)
	//			{
	//				String uploadType = aReq.getParameter("uploadType");
	//				String uploadDir = getPortletContext().getRealPath("/");
	//				Map<String, String> credentials = ImportUtil.importExcel(themeDisplay, aReq, aResp, uploadDir);
	//				if (credentials != null)
	//				{
	//					_log.info("Updating openam users");
	//					updateOpenAmUsers(credentials);
	//				}
	//			}
	//		} catch (Exception ex)
	//		{
	//			_log.error("Themedisplay failed to initialize");
	//		}
	//
	//	}

		private void addUserToOpenAm(String email, String firstname, String lastname)
	    {
			addUserToOpenAm(email, firstname, lastname, getDefaultTempPass());
	    } // addUserToOpenAm

		private void addUserToOpenAm(String email, String firstname, String lastname, String password)
	    {
	    	final String base = getProperty(URL_PROP),
					create = getProperty(CREATE_PROP),
					auth = getProperty(AUTH_PROP),
					update = getProperty(UPDATAE_PROP),
					search = getProperty(SEARCH_PROP),
					name = getProperty(ID_NAME_PROP),
					keyPrefix = getProperty(ID_ATTR_NAMES_PROP),
					valPrefix = getProperty(ID_ATTR_VALUES_PREFIX_PROP),
					realm = getProperty(ID_REALM_PROP),
					idType = getProperty(ID_TYPE_PROP),
					tokenid = getProperty(ADMIN_TOKEN_ID_PROP);

			if (base != null && create != null && name != null && keyPrefix != null)
			{
				final String createUrl = base + create;
				final String searchUrl = base + search;
				final String username = getUsername();
				final String pass = getPassword();
				final String uri_param = "uri=realm%3D/%26service%3DldapService";

				if ( !checkToken() )
				{					if (!authenticate(base + auth, username, pass, uri_param))
					{
						_log.error("Could not authenticate System user, check configured values.");
						return;
					}
				}


				final String nameParam = name +"="+ encode(email);

				final String passParam = keyPrefix +"="+ getProperty(ID_VALUE_UP) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_UP) +"="+ encode(password);
				final String snParam = keyPrefix +"="+ getProperty(ID_VALUE_SN) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_SN) +"="+ encode(lastname);
				final String cnParam = keyPrefix +"="+ getProperty(ID_VALUE_CN) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_CN) +"="+ encode(firstname +" "+ lastname);
				final String mailParam = keyPrefix +"="+ getProperty(ID_VALUE_MAIL) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_MAIL) +"="+ encode(email);
				final String activeParam = keyPrefix +"="+ getProperty(ID_VALUE_ACTIVE) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_ACTIVE) +"="+ encode(getProperty(ACTIVE_PROP));
				final String fnParam = keyPrefix +"="+ getProperty(ID_VALUE_FIRSTNAME) +AMPERSAND+
						valPrefix + getProperty(ID_VALUE_FIRSTNAME) +"="+ encode(firstname);
				final String realmParam = getProperty(ID_REALM_PROP) +"="+ getProperty(REALM_VALUE) +AMPERSAND+
						idType +"="+ ID_TYPE;
				final String adminParam = tokenid +"="+ this.token;


				final String urlStr = createUrl + nameParam +AMPERSAND+ passParam +AMPERSAND+ snParam +AMPERSAND+ cnParam +AMPERSAND+ mailParam
						+AMPERSAND+ activeParam +AMPERSAND+ fnParam +AMPERSAND+ realmParam +AMPERSAND+ adminParam;
	//    	    	_log.info("Creating new user w/ url string: "+ urlStr);
				String res = executeOpenAmRequest(urlStr);
				if (res != null)
				{
					_log.info("Results: " + res);
				}
			}
			else
			{
				_log.error("Got null properties... must add them to portal-ext.properties file");
			}


	    } // addUserToOpenAm

		private String encode(String val)
	    {
	    	try
	    	{
				return URLEncoder.encode(val, ENCODE_TYPE);
			} catch (UnsupportedEncodingException e)
			{
	//			e.printStackTrace();
				_log.error("Caught UnsupportedEncodingException, couldn't encode value");
			}

	    	return val;
	    }

	    private boolean authenticate(final String url, final String username, final String password, String uri_val)
	    {
	    	final String username_param = getProperty(AUTH_USERNAME_PROP) +"="+ username;
			final String password_param = getProperty(AUTH_PASSWORD_PROP) +"="+ password;
		    if (uri_val == null)
		    {
			    uri_val = getProperty(URI_VALUE_PROP);
		    }
		    final String uri_param = getProperty(URI_PARAM_PROP) +"="+ uri_val;

			final String authUrl = url + username_param + AMPERSAND + password_param + AMPERSAND + uri_param;

			String res = executeOpenAmRequest(authUrl);
			if (res != null)
			{
				_log.info("Response: " + res);

				// set token
				String[] parts = res.split("=");
				if (parts.length == 2)
				{
					this.token = parts[1];
					return true;
				}
	//				return true;
			}

	    	return false;
	    }

		public String getAuthToken(String url, final String username, final String password, String uri_val)
		    {
			    if (url == null)
			    {
				    url = getProperty(URL_PROP) + getProperty(AUTH_PROP);
			    }
		    	final String username_param = getProperty(AUTH_USERNAME_PROP) +"="+ username;
				final String password_param = getProperty(AUTH_PASSWORD_PROP) +"="+ password;
			    if (uri_val == null)
			    {
				    uri_val = getProperty(URI_VALUE_PROP);
			    }
			    final String uri_param = getProperty(URI_PARAM_PROP) +"="+ uri_val;

				final String authUrl = url + username_param + AMPERSAND + password_param + AMPERSAND + uri_param;

				String res = executeOpenAmRequest(authUrl);
				if (res != null)
				{
					_log.info("Response: " + res);

					// set token
					String[] parts = res.split("=");
					if (parts.length == 2)
					{
						this.token = parts[1];
						return this.token;
					}
		//				return true;
				}

		    	return null;
		    }

	/**
	 * Need something like this
	 *
	 *   Cookie: iPlanetDirectoryPro=AQIC5wM2LY4SfcyghoFGVp3ecnpJIk5gegRDJ5Mmdqznpto.*AAJTSQACMDE.*; AMAuthCookie=AQIC5wM2LY4SfcyghoFGVp3ecnpJIk5gegRDJ5Mmdqznpto.*AAJTSQACMDE.*
	 *
	 * @param token
	 * @return
	 */
	public Map setTokenHeaderMap(String token)
	{
		if (token == null && this.token == null)
			 return null;
		else if (token == null && this.token != null)
			token = this.token;

	 	Map<String, String> headers = new HashMap<String, String>();


	 	String authString = "iPlanetDirectoryPro="+ token +"; AMAuthCookie="+ token;
		headers.put("Cookie", authString);

		return headers;
	}

	    private boolean checkToken()
	    {
	    	if (token != null)
	    	{
	    		final String tokenid = getProperty(ADMIN_TOKEN_ID_PROP);
				final String url = getProperty(URL_PROP) + getProperty(TOKEN_VALIDATE_URL_PROP) + tokenid +"="+ this.token;

				String res = executeOpenAmRequest(url);
				if (res != null)
				{
					return true;
				}
	    	}

	    	return false;
	    }

	    private String parseResponse(InputStream is)
	    {
	    	if ( is == null)
	    		return null;

	    	byte[] bytes = new byte[1024];
	    	int read = 0;
	    	StringBuffer res = new StringBuffer();

	    	try
	    	{
	    		while (read != -1)
	    		{
	    			read = is.read(bytes);
	    			if (read > 0)
	    				res.append(new String(bytes, 0, read));
	    		}
	    	} catch (IOException ioe)
	    	{
	    		ioe.printStackTrace();
	    	} finally
	    	{
	    		try
	    		{
	    			// close stream
					is.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
	    	}

	    	return res.toString();
	    }

	    private String executeOpenAmRequest(String urlStr)
	    {
	    	HttpURLConnection conn = null;

	    	try
	    	{
				URL url = new URL(urlStr);
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(30000);
			    conn.setReadTimeout(30000);
				conn.setRequestMethod(GET);
				conn.connect();

				if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
				{
					return parseResponse(conn.getInputStream());
				} else
				{
					// not successful
					_log.error("[ERROR] request to "+urlStr+"  failed, HTTP Error code: " + conn.getResponseCode());
				}
			} catch (MalformedURLException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			} finally
			{
				if (conn != null)
				{
					conn.disconnect();
				}
			}

	    	return null;
	    }

	public String decrypt(String prop)
	{
		initEncryptor();
		return encryptor.decrypt(prop);
	}

		private void initEncryptor()
	    {
	    	try
			{
				if (encryptor == null)
				{
					encryptor = new StandardPBEStringEncryptor();

					String prop = System.getProperty(DECODE_PROPERTY);
					if (prop == null) prop = "secretpassword";
					final String decode = prop;

					if (decode != null)
					{
						encryptor.setPassword(decode);
						encryptor.setAlgorithm(getProperty(ALGORITHM));

						if (!encryptor.isInitialized())
						{
							encryptor.initialize();
						}
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
	    }

	    private String getUsername()
	    {
	    	initEncryptor();

	    	String username = null;
			username = getProperty(ADMIN_CREATOR_USERNAME_PROP);
			if (username != null)
			{
				return encryptor.decrypt(username);
			}

	    	return null;
	    }

	    private String getPassword()
	    {
	    	initEncryptor();

	    	String pass = null;
	    	pass = getProperty(ADMIN_CREATOR_PASS_PROP);
			if (pass != null)
			{
				return encryptor.decrypt(pass);
			}


	    	return null;
	    }

	    private String getDefaultTempPass()
	    {
	    	initEncryptor();

	    	String pass = null;
	    	pass = getProperty(OPENAM_DEFAULT_TEMP_PASS_PROP);
			if (pass != null)
			{
				return encryptor.decrypt(pass);
			}


	    	return null;
	    }

	    private String getProperty(String prop)
	    {
	    	if (prop != null)
	    		return props.getProperty(prop, "");

	    	return null;
	    }

		public static void main(String[] args)
		{
			OpenAmUtil helper = new OpenAmUtil();
			String e = "mail.mil";

	//		String first = "FDNY",
	//				last = "IC",
	//				email = "fdny.incident.command@mail.mil";

	//		helper.addUserToOpenAm(email, first, last, "USCGex2014!");

			for (int j = 1, k=0; k < 6; k++, j++)
			{
				// create user account name
				String email = "safety" + j + AT + e;
				email = email.toLowerCase();

	//			_log.info("Adding user to openam: " + email);
	//			helper.addUserToOpenAm(email, first, LASTNAME + j, "USCGex2014!");

				_log.info("Updating user in openam: " + email);
				helper.updateCredentials(null, email, "USCGex2014!");

			}

		}

}
