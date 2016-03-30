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
package edu.mit.ll.nics.sso.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.util.*;
import edu.mit.ll.soa.sso.exception.AuthException;
import edu.mit.ll.soa.sso.exception.InitializationException;
import edu.mit.ll.soa.sso.util.OpenAmInitUtil;
import edu.mit.ll.soa.sso.util.OpenAmUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;


/**
 * Created with IntelliJ IDEA.
 * User: cbudny
 * Date: 7/23/14
 * Time: 8:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class SSOUtil
{
	private Properties props = null;

	private static Log log = LogFactory.getLog(SSOUtil.class);
	private static final String HOST = "openam.host";
	private static final String PROTOCL = "openam.protocol";
	private static final String PORT = "openam.port";
	private static final String PATH = "openam.path";
	private static final String CREATOR_USER = "openam.creator.user";
	private static final String CREATOR_PASS = "openam.creator.pass";
	private static final String ALGORITHM = "algorithm";
	// system property for decoding encrypted values
	private static final String DECODE_PROPERTY = "jasypt.password";
	private static StandardPBEStringEncryptor encryptor = null;

	private OpenAmUtils utils = null;

	public SSOUtil()
	{
		initialize();
	}

	public SSOUtil(String protocol, String host, String port, String path)
	{
		try
		{
			Map<String, String> vals = OpenAmInitUtil.createPropertiesMap(protocol, host, port, path);
			utils = new OpenAmUtils(vals);
		} catch (InitializationException ie)
		{
			ie.printStackTrace();
		}
	}

	private void initialize()
	{
		props = new Properties();
		InputStream is = null;
		String propPath = null, propFile = null;
		try
		{
			propPath = System.getProperty("ssoToolsPropertyPath");
			propFile = propPath + ((propPath.endsWith(File.separator) ? "" : File.separator)) + 
					"sso-tools.properties";
			
			
			if(propPath == null || propPath.equals("")) {
				log.warn("Required property missing: ssoToolsPropertyPath ! Using defaults.");
				is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sso-tools.properties");
			} else {
				log.info("Using sso-tools properties file: " + propFile);
				is = new FileInputStream(new File(propFile));
			}
			
			if (is != null)
			{
				props = new Properties();
				props.load(is);
				info("Loaded sso-tools properties:\n" + //);
						props.toString());
			}
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		} finally
		{
			if (is != null)
			{
				try
				{
					is.close();
				} catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}

		try
		{
			Map<String, String> vals = OpenAmInitUtil.createPropertiesMap(getProperty(PROTOCL),
							getProperty(HOST), getProperty(PORT), getProperty(PATH));

			utils = new OpenAmUtils(vals);
		} catch (InitializationException ie)
		{
			ie.printStackTrace();
		}
	}

	/**
	 * Returns token or null
	 * @param user
	 * @param pass
	 * @return
	 */
	public String login(String user, String pass)
	{
	 	String token = null;

		try
		{
			token = utils.login(user, pass);
		} catch (AuthException ae)
		{
			ae.printStackTrace();
		}

		return token;
	}
	
	/**
	 * Logs the user in with the specified realm
	 * 
	 * @param user
	 * @param pass
	 * @param realm
	 * @return a token string if successfull, null otherwise
	 */
	public String login(String user, String pass, String realm) {
		String token = null;
		
		try {
			token = utils.login(user, pass, realm);
		} catch(AuthException ae) {
			ae.printStackTrace();
		}
		
		return token;
	}
	
	public String getTokenIfExists() {
		return utils.getTokenIfExists();
	}

	public boolean switchOrg(String orgName)
	{
		try
		{
			utils.switchOrganizations(orgName);
			return true;
		} catch (AuthException ae)
		{
			ae.printStackTrace();
		}

		return false;
	}

	public String orgLogin(String user, String pass, String orgName)
	{
		String token = null;

		try
		{
			utils.login(user, pass, orgName);
		} catch (AuthException ae)
		{
			ae.printStackTrace();
		}

		return token;
	}

	public Map getAttributes()
	{
		return utils.getUserAttributes();
	}

	public Map getUserAttributes(String tokenID)
	{
		Map map = null;

		try
		{
			map = utils.getAttributesForToken(tokenID);
		} catch (AuthException ae)
		{
			ae.printStackTrace();
		}

		return map;
	}

	public Map setAttribute(String key, String val)
	{
		return utils.setUserAttribute(key, val);
	}
	
	public Map setAttributes(Map attributes) {
		return utils.setUserAttributes(attributes);
	}

	public String createUser(String email, String pass, String first, String last, boolean active)
	{
		String message = "";
		boolean val = false;
		try
		{
			val = utils.createIdentity(email, pass, first, last, active);
			// do something with result?
			if(val) {
				message = "successfully created identity for " + email;
			} else {
				message = "failed to create identity for " + email;
			}
		} catch (AuthException ae)
		{
			message = "failed to create identity for " + email + ": " + ae.getMessage();
			ae.printStackTrace();
		}

		return "{\"status\":\"" + ((val) ? "success" : "fail") + "\",\"message\":\"" + message + "\"}";
	}

	public boolean createOrg(String org, boolean active)
	{
		boolean val = false;
		try
		{
			val = utils.createOrganization(org, active);
			// do something
		} catch (AuthException ae)
		{
			ae.printStackTrace();
		}
		return val;
	}

	public boolean refreshSessionToken(String token){
		try{
			return utils.refreshSessionToken(token);
		} catch (AuthException ae){
			ae.printStackTrace();
		}
		return false;
	}
	
	public boolean validateToken(String token)
	{
		boolean val = false;

		try
		{
			val = utils.isTokenValid(token);
		} catch (AuthException ae)
		{
			ae.printStackTrace();
		}

		return val;
	}

	public boolean destroyToken(String token) {
		return utils.destroyToken(token);
	}

	public String toggleUserStatus(String email, boolean status) {
		// Requires Log in as admin?, and set attribute on specified user...
		
		return utils.toggleUserStatus(email, status);
	}
	
	public boolean changeUserPassword(String email, String newpw){
		return utils.changeUserPassword(email, newpw);
	}
	
	public boolean loginAsAdmin() {
		return loginAsAdmin("/"); // TODO: use config/constant
	}
	
	public boolean loginAsAdmin(String realm) {
				
		String token = login(getUsername(), getPassword(), realm);
		if(token != null && !token.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	public boolean logout() {
		try {
			utils.logout();
			return true;
		} catch(AuthException e) {
			error("AuthException thrown while attempting to logout!");
			e.printStackTrace();
		}
		
		return false;
	}

	public boolean resetPassword(String username)
	{
		return utils.resetPassword(username);
	}

	public void info(String msg)
	{
		log.info(msg);
	}

	public void debug(String msg)
	{
		log.debug(msg);
	}

	public void error(String msg)
	{
		log.error(msg);
	}

	public String getProperty(String prop)
	{
	 	if (prop != null)
			return props.getProperty(prop, "");

		return null;
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
		username = getProperty(CREATOR_USER);
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
		pass = getProperty(CREATOR_PASS);
		if (pass != null)
		{
			return encryptor.decrypt(pass);
		}


		return null;
	}
}
