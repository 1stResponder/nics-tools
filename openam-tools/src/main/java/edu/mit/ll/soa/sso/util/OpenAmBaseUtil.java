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
package edu.mit.ll.soa.sso.util;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.*;

import edu.mit.ll.soa.sso.exception.AuthException;
import edu.mit.ll.soa.sso.exception.InitializationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.json.JSONObject;

import javax.security.auth.callback.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created with IntelliJ IDEA.
 * User: cbudny
 * Date: 7/21/14
 * Time: 8:10 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OpenAmBaseUtil
{
	static final String DEF_REALM = "/";

	private String userid = null;
	private String password = null;

	private String creatorId = null;
	private String creatorPass = null;

	protected AuthContext lc = null;
	private String userID = null;
	private boolean initialized = false;

	protected SSOToken token = null;
	protected SSOTokenManager manager = null;
	protected AMIdentityRepository idRepo = null;
	protected Set subRealms = null;
	protected Set identities = null;

	protected Properties props = null;

	private static Log _log = LogFactory.getLog(OpenAmBaseUtil.class);
	private static StandardPBEStringEncryptor encryptor = null;

	private static final String ADMIN_CREATOR_USERNAME_PROP = "openam.user.creator.username";
	private static final String ADMIN_CREATOR_PASS_PROP = "openam.user.creator.password";
	private static final String OPENAM_DEFAULT_TEMP_PASS_PROP = "openam.user.default.temp.password";
	private static final String DECODE_PROPERTY = "jasypt.password";
	private static final String ALGORITHM = "algorithm";
	private static final String DATASTORE_PROP = "default.store";
	
	private static final String OPENAM_PROPERTIES_PATH_PROP = "openamPropertiesPath";
	private static final String OPENAM_TOOLS_PROPERTIES = "openam-tools.properties";
	private static final String AMCONFIG_PROPERTIES = "AMConfig.properties";

	private String forgotpasswordUrl = null;

	private boolean checkInitParams(Map<String, String> params)
	{
		return (params.containsKey(OpenAmUtilConstants.IDP_PATH) &&
				params.containsKey(OpenAmUtilConstants.IDP_HOST) &&
				params.containsKey(OpenAmUtilConstants.IDP_PROTOCOL) &&
				params.containsKey(OpenAmUtilConstants.IDP_PORT));

	}

	// TODO generate the openam properties

	protected void initialize(Map<String, String> params) throws InitializationException
	{
		if (!checkInitParams(params))
		{
			throw new InitializationException("Invalid initialization parameters");
		}

		// set properties
		Properties p = new Properties();
		InputStream in = null;

		String propFile = null;
		
		try
		{
			propFile = buildPropertyFilePath(AMCONFIG_PROPERTIES);
			
			if(propFile != null) {
				_log.debug("Using " + AMCONFIG_PROPERTIES + ": " + propFile);
				in = new FileInputStream(new File(propFile));
			} else {				
				_log.warn("Must set property '" + OPENAM_PROPERTIES_PATH_PROP + "' and have valid " + 
						AMCONFIG_PROPERTIES + " file");
			}

			if (in != null)
			{
				p.load(in);
				_log.info("Loaded AMConfig.properties");
				_log.info(p.toString());
			} else
			{
				_log.info("Cannot load AMConfig.properties");
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

		// set properties
		setAmConfigProperties(p, params);

		
		try
		{
			propFile = buildPropertyFilePath(OPENAM_TOOLS_PROPERTIES);
			
			if(propFile != null) {
				_log.info("Using " + OPENAM_TOOLS_PROPERTIES + ": " + propFile);
				in = new FileInputStream(new File(propFile));
			} else {
				//in = Thread.currentThread().getContextClassLoader().getResourceAsStream("openam-tools.properties");
				_log.warn("Must set property '" + OPENAM_PROPERTIES_PATH_PROP + "' and have valid " + 
						OPENAM_TOOLS_PROPERTIES + " file");
			}
			
			if (in != null)
			{
				props = new Properties();
				props.load(in);
				info("Loaded " + OPENAM_TOOLS_PROPERTIES);
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

		this.initialized = true;
	}
	
	
	/**
	 * Combines the OPENAM_PROPERTIES_PATH_PROP property with the specified property file name
	 * to see if the property file exists at the path. If it does, the full path and file
	 * name is returned, otherwise null is returned. Null is also returned if the
	 * 'openamPropertyPath' is not set.
	 * 
	 * @param propertyFilename
	 * @return
	 */
	private String buildPropertyFilePath(String propertyFilename) {
		
		Path path = Paths.get(System.getProperty(OPENAM_PROPERTIES_PATH_PROP), propertyFilename);
		
		if(Files.isReadable(path)) {
			return path.toString();
		} else {
			return null;
		}
	}
	
	protected Map getUserAttributes(String tokenID) throws AuthException
	{
		AuthContext.IndexType authType = AuthContext.IndexType.MODULE_INSTANCE;
		AMIdentityRepository repo = null;
		AMIdentity id = null;

		try
		{
			SSOToken ssoToken = createTokenFromID(tokenID);
			repo = new AMIdentityRepository(ssoToken, DEF_REALM);
			id = new AMIdentity(ssoToken);
			/*TODO:cleanup
					"\n\trealm: " + id.getRealm() +
					"\n\tid: " + id.getUniversalId() +
					"\n\tattrs?: " + ((id.getAttributes() == null) ? "none" : "count(" + id.getAttributes().size() + ")\n")
					);
			//id = repo.getRealmIdentity();
			
			Map attrs = id.getAttributes();
			
			if(attrs != null) {
				Iterator<String> iter = attrs.keySet().iterator();
				while(iter.hasNext()) {
					String attrKey = iter.next();
					if(attrKey != null) {
						System.out.println("\n\t\tattr: " + attrKey + ":" + attrs.get(attrKey));
					}
				}

			}*/
			
			/* DEBUG seeing what the supported types are:
			Set<IdType> idTypes = repo.getSupportedIdTypes();
			System.out.println("Supported ID Types:");
			if(idTypes == null || idTypes.isEmpty()) {
				System.out.println("\tNONE");
			} else {				
				Iterator<IdType> iter = idTypes.iterator();
				while(iter.hasNext()) {
					IdType idType = iter.next();
					if(idType != null) {
						System.out.println("\ttype: " + idType.getName());
					}
				}
			}*/
			
		} catch (IdRepoException ire)
		{
			error("IdRepoException for '" + DEF_REALM + "': " + ire.getMessage());
			throw new AuthException(ire.getMessage());
		} catch (SSOException sse)
		{
			error("SSOException getting AMIdentityRepository for '" + DEF_REALM + "': " + sse.getMessage());
			throw new AuthException(sse.getMessage());
		}

		return getAttributes(id);
	}

	protected void doCurrentRealm(String realm) throws AuthException
	{
		try
		{
			info("Getting Identity Repository with token to realm: " + token.getTokenID().toString() + ", " + realm);
			idRepo = new AMIdentityRepository(token, realm);
		} catch (IdRepoException ire)
		{
			error("IdRepoException for '" + realm + "': " + ire.getMessage());
			throw new AuthException(ire.getMessage());
		} catch (SSOException sse)
		{
			error("SSOException getting AMIdentityRepository for '" + realm + "': " + sse.getMessage());
			throw new AuthException(sse.getMessage());
		}

		debug("Getting subrealms for " + realm);

		try
		{
			subRealms = (idRepo.searchIdentities(IdType.REALM, "*", new IdSearchControl())).getSearchResults();
			debug("Subrealm size: " + subRealms.size());
		} catch (SSOException sse)
		{
			error("SSOException getting AMIdentityRepository for '" + realm + "': " + sse.getMessage());
			throw new AuthException(sse.getMessage());
		} catch (IdRepoException ire)
		{
			error("IdRepoException for '" + realm + "': " + ire.getMessage());
			throw new AuthException(ire.getMessage());
		}
	}

	protected SSOToken realmLogin(String realmid)
			throws SSOException, AuthLoginException, Exception
	{

		return realmLogin(userid, password, realmid);
	}


	protected SSOToken realmLogin(String userid, String password)
			throws SSOException, AuthLoginException, Exception
	{
		this.userid = userid;
		this.password = password;

		return realmLogin(userid, password, DEF_REALM);
	}

	protected SSOToken realmLogin(String userid, String password, String realmid)
			throws SSOException, AuthLoginException, Exception
	{
		String adminDN;
		String adminPassword;
		SSOToken ssoToken = null;
		AuthContext.IndexType authType = AuthContext.IndexType.MODULE_INSTANCE;

		try
		{
			System.out.println("\n!!!Creating authcontext with realm: " + realmid);
			lc = new AuthContext(realmid);
		} catch (AuthLoginException le)
		{
			System.err.println(
					"IdRepoSampleUtils: could not get AuthContext for realm " +
							realmid);
			throw le;
		}

		try
		{
			System.out.println("\n!!!Attempting login with authTYpe: " + authType.toString() + ", DataStoreProp: " + 
					getProperty(DATASTORE_PROP) + "\n!!!\n");
			lc.login(authType, getProperty(DATASTORE_PROP));
		} catch (AuthLoginException le)
		{
			System.err.println("IdRepoSampleUtils: Failed to start login " +
					"for default authmodule");
			throw le;
		}

		userID = userid;
		Callback[] callbacks = null;
		Hashtable values = new Hashtable();
		values.put(AuthXMLTags.NAME_CALLBACK, userid);
		values.put(AuthXMLTags.PASSWORD_CALLBACK, password);

		while (lc.hasMoreRequirements())
		{
			callbacks = lc.getRequirements();
			try
			{
				fillCallbacks(callbacks, values);
				lc.submitRequirements(callbacks);
			} catch (Exception e)
			{
				System.err.println("Failed to submit callbacks!");
				e.printStackTrace();
				return null;
			}
		}

		AuthContext.Status istat = lc.getStatus();
		if (istat == AuthContext.Status.SUCCESS)
		{
			debug("==>Authentication SUCCESSFUL for user " + userid);
		} else if (istat == AuthContext.Status.COMPLETED)
		{
			System.out.println("==>Authentication Status for user " +
					userid + " = " + istat);
			return null;
		}

		try
		{
			token = lc.getSSOToken();
			manager = SSOTokenManager.getInstance();
			ssoToken = token;
			info("Token: " + ssoToken.getTokenID().toString());
		} catch (Exception e)
		{
			System.err.println("Failed to get SSO token!  " + e.getMessage());
			throw e;
		}

		// get id repo
		doCurrentRealm(realmid);

		return ssoToken;
	}

	protected void doLogout() throws AuthLoginException
	{
		try
		{
			lc.logout();
		} catch (AuthLoginException alexc)
		{
			System.err.println("IdRepoSampleUtils: logout failed for user '" +
					userID + "'");
			throw alexc;
		}
	}

	protected boolean validateToken(String tokenID) throws SSOException
	{
		boolean valid = false;

		token = createTokenFromID(tokenID);

		if (manager.isValidToken(token))
		{
			debug("Created valid token fromk TOKENID: " + tokenID);
			// display values from token
			String host = token.getHostName();
			java.security.Principal principal = token.getPrincipal();
			String authType = token.getAuthType();
			InetAddress ipAddress = token.getIPAddress();

			// other data that might be useful
			int level = token.getAuthLevel();
			long maxTime = token.getMaxSessionTime();
			long idleTime = token.getIdleTime();
			long maxIdleTime = token.getMaxIdleTime();

			info("SSOToken hostname: " + host);
			info("SSOToken Principal name: " + principal.getName());
			info("SSOToken Auth type user: " + authType);
			info("IP addr of host: " + ipAddress.getHostAddress());

			valid = true;
		}


		return valid;
	}
	
	protected boolean refreshToken(String tokenID) throws SSOException
	{
		token = createTokenFromID(tokenID);
		manager.refreshSession(token);
		return true;
	}

	protected SSOToken createTokenFromID(String tokenID) throws SSOException
	{
		if (manager == null)
			manager = SSOTokenManager.getInstance();

		boolean valid = false;
		return manager.createSSOToken(tokenID);
	}


	private AMIdentity createIdentity(IdType typeId, String id, HashMap attrs) throws IdRepoException, SSOException
	{
		System.out.println("\ncreateIdentity: TypeID: " + typeId.getName() + ", ID: " + id + "\n");
		if(idRepo == null) {
			System.out.println("idRepo is null!");
		} else {
			AMIdentity realmId = idRepo.getRealmIdentity();
			if(realmId != null) {
				System.out.println("Valid realmId: \n" + 
					"\tDN: " + realmId.getDN() + 
					"\n\tNAME: " + realmId.getName() +
					"\n\tREALM: " + realmId.getRealm() +
					"\n\tuniversalId: " + realmId.getUniversalId()					
				);
				
				/*Set members = realmId.getMembers(IdType.USER);
				if(members != null && !members.isEmpty()) {
					
					System.out.println("\n!!!Members: " + members.size());					
				} else {
					System.out.println("\n!!!No Members!");
				}*/
				
				
			} else {
				System.out.println("Realm Identity is null!");
			}
		}
		
		return idRepo.createIdentity(typeId, id, attrs);
	}

	protected AMIdentity createUserIdentity(String email, String pass,
	                                        String first, String last, boolean active)
	{
		Map attrs = new HashMap();
		
		Set vals = new HashSet();		
		vals.add(email);
		attrs.put("uid", vals);

		vals = new HashSet();
		vals.add(pass);
		attrs.put("userpassword", vals);
		vals = new HashSet();
		vals.add(last);
		attrs.put("sn", vals);
		vals = new HashSet();
		vals.add(first + " " + last);
		attrs.put("cn", vals);
		vals = new HashSet();
		vals.add(first);
		attrs.put("givenname", vals); // "full name"
		vals = new HashSet();
		vals.add(email);
		attrs.put("mail", vals);
		vals = new HashSet();
		if (active) {
			vals.add("Active");
		} else {
			vals.add("Inactive");
		}
		attrs.put("inetuserstatus", vals);

		System.out.println("createIdentity Attributes:\n");
		Iterator iter = attrs.keySet().iterator();
		while(iter.hasNext()) {
			String key = (String)iter.next();
			System.out.println("\tkey: " + key + " | " + attrs.get(key));
		}
		
		try
		{
			return createIdentity(IdType.USER, email, (HashMap) attrs);
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		}

		return null;
	}

	protected AMIdentity createRealmIdentity(String realmid, boolean active)
	{
		Map attrs = new HashMap();
		Set vals = new HashSet();

		if (active)
			vals.add("Active");
		else
			vals.add("Inactive");
		attrs.put("sunOrganizationStatus", vals);

		try
		{
			return createIdentity(IdType.REALM, realmid, (HashMap) attrs);
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		}

		return null;
	}

	protected Set<AMIdentity> searchIdentities(IdType typeId, String pattern)
	{
		Set set = new HashSet<AMIdentity>();
		IdSearchControl isc = new IdSearchControl();
		isc.setAllReturnAttributes(true);

		try
		{
			IdSearchResults res = idRepo.searchIdentities(typeId, pattern, isc);
			set = res.getSearchResults();
			identities = set;
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		}

		return set;
	}

	protected Set<AMIdentity> searchUsers(String pattern)
	{

		return searchIdentities(IdType.USER, pattern);
	}

	protected Set<AMIdentity> searchOrgs(String pattern)
	{
		return searchIdentities(IdType.REALM, pattern);
	}

	public Map getUserAttributes()
	{
		try
		{
			if(idRepo == null) {
				error("idRepo is null, did you not login?");
				return null;
			}
			return getAttributes(idRepo.getRealmIdentity());
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		}

		return null;
	}

	public Map setUserAttribute(String key, String val)
	{   try
		{
			setAttribute(idRepo.getRealmIdentity(), key, val);
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		}

		return getUserAttributes();
	}
	
	public Map setUserAttributes(Map attrs) {
		try {
			if(setAttributes(idRepo.getRealmIdentity(), attrs)) {
				return getUserAttributes();
			}
		} catch(IdRepoException ire) {
			ire.printStackTrace();
		} catch (SSOException sse) {
			sse.printStackTrace();
		}
		
		return null;
	}

	protected boolean setAttribute(AMIdentity amid, String key, String value)
	{
		Set val = new HashSet();
		val.add(value);
		return setAttribute(amid, key, val);
	}

	protected boolean setAttribute(AMIdentity amid, String key, Set vals)
	{
		Map attrs = new HashMap();
		try
		{
			attrs.put(key, vals);
			amid.setAttributes(attrs);
			amid.store();
			return true;
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		}

		return false;
	}

	protected boolean setAttributes(AMIdentity amid, Map attrs) {
		try {
			amid.setAttributes(attrs);
			amid.store();
			return true;
		} catch(IdRepoException ire) {
			ire.printStackTrace();
		} catch(SSOException sse) {
			sse.printStackTrace();
		}
		
		return false;
	}
	
	protected Map getAttributes(AMIdentity amid)
	{
		Map attrs = null;
		try
		{
			System.out.println("\n\n!!!Getting attributes on AMIdentity type: " + amid.getType() +
					"\n\n!!!\n");
			attrs = amid.getAttributes();
			System.out.println("Got attributes: " + ((attrs != null) ? attrs.size() : "none/null"));
		} catch (IdRepoException ire)
		{
			ire.printStackTrace();
		} catch (SSOException sse)
		{
			sse.printStackTrace();
		} catch(Exception e){
			System.out.println("Unhandled exception getting attributes for AMIdentity: " + e.getMessage());
			e.printStackTrace();
		}


		return attrs;
	}

	protected void fillCallbacks(Callback[] callbacks, Hashtable values)
			throws Exception
	{
		for (int i = 0; i < callbacks.length; i++)
		{
			if (callbacks[i] instanceof NameCallback)
			{
				NameCallback nc = (NameCallback) callbacks[i];
				nc.setName((String) values.get(AuthXMLTags.NAME_CALLBACK));
			} else if (callbacks[i] instanceof PasswordCallback)
			{
				PasswordCallback pc = (PasswordCallback) callbacks[i];
				pc.setPassword(((String) values.get(
						AuthXMLTags.PASSWORD_CALLBACK)).toCharArray());
			} else if (callbacks[i] instanceof TextInputCallback)
			{
				TextInputCallback tic = (TextInputCallback) callbacks[i];
				tic.setText((String) values.get(
						AuthXMLTags.TEXT_INPUT_CALLBACK));
			} else if (callbacks[i] instanceof ChoiceCallback)
			{
				ChoiceCallback cc = (ChoiceCallback) callbacks[i];
				cc.setSelectedIndex(Integer.parseInt((String) values.get(
						AuthXMLTags.CHOICE_CALLBACK)));
			}
		}
	}

	public String getLine()
	{
		StringBuffer buf = new StringBuffer(80);
		int c;

		try
		{
			while ((c = System.in.read()) != -1)
			{
				char ch = (char) c;
				if (ch == '\r')
				{
					continue;
				}
				if (ch == '\n')
				{
					break;
				}
				buf.append(ch);
			}
		} catch (IOException e)
		{
			System.err.println("getLine: " + e.getMessage());
		}
		return (buf.toString());
	}

	protected String getLine(String prompt)
	{
		System.out.print(prompt);
		return (getLine());
	}

	protected String getLine(String prompt, String defaultVal)
	{
		System.out.print(prompt + " [" + defaultVal + "]: ");
		String tmp = getLine();
		if (tmp.length() == 0)
		{
			tmp = defaultVal;
		}
		return (tmp);
	}

	/*
				 *  return integer value of String sVal; -1 if error
				 */
	protected int getIntValue(String sVal)
	{
		int i = -1;
		try
		{
			i = Integer.parseInt(sVal);
		} catch (NumberFormatException e)
		{
			System.err.println("'" + sVal +
					"' does not appear to be an integer.");
		}
		return i;
	}

	/*
				 *  can only create or delete AMIdentities of IdType user, agentgroup,
				 *  agentonly
				 */
	protected IdType getIdTypeToCreateOrDelete()
	{
		IdType tType = null;
		System.out.println("    Supported IdTypes:\n" +
				"\t0: user\n\t1: agent\n\t2: agentonly\n\t3: agentgroup\n\t4: realm\n\t5: No selection");
		String answer = getLine("Select type: [0..3]: ");
		int i = getIntValue(answer);
		switch (i)
		{
			case 0:  // user
				tType = IdType.USER;
				break;
			case 1:  // agent
				tType = IdType.AGENT;
				break;
			case 2:  // agentonly
				tType = IdType.AGENTONLY;
				break;
			case 3:  // agentgroup
				tType = IdType.AGENTGROUP;
				break;
			case 4:  // realm
				tType = IdType.REALM;
				break;
			case 5:  // no selection
				break;
			default:  // invalid selection
				System.err.println(answer + " is an invalid selection.");
		}
		return tType;
	}

	/*
				 *  get the IdType selected from the list of supported IdTypes for
				 *  this AMIdentityRepository object.  can be "null" if no selection
				 *  made.
				 */
	protected IdType getIdType(AMIdentityRepository idRepo)
	{
		IdType tType = null;
		String realmName = null;
		try
		{
			realmName = idRepo.getRealmIdentity().getRealm();
			Set types = idRepo.getSupportedIdTypes();
			Object[] idtypes = types.toArray();
			System.out.println("    Supported IdTypes:");
			int i = 0;
			for (i = 0; i < idtypes.length; i++)
			{
				tType = (IdType) idtypes[i];
				System.out.println("\t" + i + ": " + tType.getName());
			}
			System.out.println("\t" + i + ": No selection");

			String answer = getLine("Select type: [0.." +
					idtypes.length + "]: ");
			i = getIntValue(answer);

			tType = (IdType) idtypes[0];
			if (i == idtypes.length)
			{
				return (null);
			} else if ((i >= 0) && (i < idtypes.length))
			{
				tType = (IdType) idtypes[i];
			} else
			{
				System.err.println(answer + " is an invalid selection.");
				return (null);
			}
		} catch (IdRepoException ire)
		{
			System.err.println("getIdType: IdRepoException" +
					" getting Supported IdTypes for '" + realmName + "': " +
					ire.getMessage());
		} catch (SSOException ssoe)
		{
			System.err.println("getIdType: SSOException" +
					" getting Supported IdTypes for '" + realmName + "': " +
					ssoe.getMessage());
		}
		return (tType);
	}

	/*
				 *  print out elements in the Set "results".  header and trailer
				 *  titling Strings.  more generic (i.e., usually expecting Strings)
				 *  than other printResults(String, Set).
				 */
	public void printResults(
			String header,
			Set results,
			String trailer)
	{
		if (results.isEmpty())
		{
			System.out.println(header + " has no " + trailer);
		} else
		{
			System.out.println(header + " has " + results.size() + " " +
					trailer + ":");
			for (Iterator it = results.iterator(); it.hasNext(); )
			{
				System.out.println("    " + it.next());
			}
		}
		System.out.println("");
		return;
	}

	/*
				 *  print out elements in the Set "results".  header and trailer
				 *  titling Strings.  more generic (i.e., usually expecting Strings)
				 *  than other printResults(String, Set).
				 */
	public void printResultsRealm(
			String header,
			Set results,
			String trailer)
	{
		if (results.isEmpty())
		{
			System.out.println(header + " has no " + trailer);
		} else
		{
			System.out.println(header + " has " + results.size() + " " +
					trailer + ":");
			for (Iterator it = results.iterator(); it.hasNext(); )
			{
				AMIdentity amid = (AMIdentity) it.next();
				System.out.println("    " + amid.getRealm());
			}
		}
		System.out.println("");
		return;
	}

	/*
				 *  for the Set of IdTypes specified in "results", get and print
				 *    1. the IdTypes it can be a member of
				 *    2. the IdTypes it can have as members
				 *    3. the IdTypes it can add to itself
				 */
	public void printIdTypeResults(
			String header,
			Set results,
			String trailer)
	{
		if (results.isEmpty())
		{
			System.out.println(header + " has no " + trailer);
		} else
		{
			System.out.println(header + " has " + results.size() + " " +
					trailer + ":");
			IdType itype = null;
			Set idSet = null;
			for (Iterator it = results.iterator(); it.hasNext(); )
			{
				itype = (IdType) it.next();
				System.out.println("    IdType " + itype.getName());
				idSet = itype.canBeMemberOf();
				printIdTypeSet("BE a member of IdType(s):", idSet);

				idSet = itype.canHaveMembers();
				printIdTypeSet("HAVE a member of IdType(s):", idSet);

				idSet = itype.canAddMembers();
				printIdTypeSet("ADD members of IdType(s):", idSet);
			}
		}
		System.out.println("");
		return;
	}

	/*
				 *  used by printIdTypeResults(), above, to print out
				 *  AMIdentity names of elements in the Set.
				 */
	private void printIdTypeSet(
			String header,
			Set idSet)
	{
		System.out.print("\tcan " + header);
		if (idSet.size() > 0)
		{
			for (Iterator it = idSet.iterator(); it.hasNext(); )
			{
				System.out.print(" " + ((IdType) it.next()).getName());
			}
			System.out.print("\n");
		} else
		{
			System.out.println(" [NONE]");
		}
	}

	/*
				 *  print the objects (String or AMIdentity.getName()) in the
				 *  specified Array, and return the index of the one selected.
				 *  -1 if none selected.
				 */

	public int selectFromArray(
			Object[] objs,
			String hdr,
			String prompt)
	{
		AMIdentity amid = null;
		String ans = null;
		boolean isIdType = false;
		boolean isString = false;

		if (objs.length <= 0)
		{
			return (-1);
		}

		System.out.println(hdr);
		int i = -1;

		String objclass = objs[0].getClass().getName();
		if (objclass.indexOf("AMIdentity") >= 0)
		{
			isIdType = true;
		} else if (objclass.indexOf("String") >= 0)
		{
			isString = true;
		}

		for (i = 0; i < objs.length; i++)
		{
			if (isIdType)
			{
				amid = (AMIdentity) objs[i];
				System.out.println("\t" + i + ": " + amid.getName());
			} else if (isString)
			{
				System.out.println("\t" + i + ": " + (String) objs[i]);
			} else
			{
				System.out.println("\t" + i + ": Class = " + objclass);
			}
		}
		System.out.println("\t" + i + ": No Selection");
		ans = getLine(prompt + ": [0.." + objs.length + "]: ");
		i = getIntValue(ans);

		return i;
	}


	/*
				 *  print the objects (String or AMIdentity.getName()) in the
				 *  specified Set, and return the object of the one selected.
				 *  null if none selected.
				 */
	public Object selectFromSet(Set itemSet)
	{
		Object[] objs = itemSet.toArray();
		AMIdentity amid = null;
		AMIdentity amid2 = null;
		int setsize = itemSet.size();
		int i;
		boolean isAMId = false;
		boolean isString = false;
		String str = null;

		if (setsize <= 0)
		{
			return null;
		}

		String objclass = objs[0].getClass().getName();
		if (objclass.indexOf("AMIdentity") >= 0)
		{
			isAMId = true;
		} else if (objclass.indexOf("String") >= 0)
		{
			isString = true;
		}

		if (setsize > 0)
		{
			System.out.println("Available selections:");
			for (i = 0; i < setsize; i++)
			{
				if (isAMId)
				{
					amid = (AMIdentity) objs[i];
					System.out.println("\t" + i + ": " + amid.getName());
				} else if (isString)
				{
					System.out.println("\t" + i + ": " + (String) objs[i]);
				} else
				{
					System.out.println("\t" + i + ": Class = " + objclass);
				}
			}
			System.out.println("\t" + i + ": No selection");

			String answer = getLine("Select identity: [0.." + setsize + "]: ");
			int ians = getIntValue(answer);
			if ((ians >= 0) && (ians < setsize))
			{
				return (objs[ians]);
			} else if (ians == setsize)
			{
			} else
			{
				System.err.println("'" + answer +
						"' is invalid.");
			}
		}
		return null;
	}


	public void waitForReturn()
	{
		waitForReturn("Hit <return> when ready: ");
		String answer = getLine();
	}

	public void waitForReturn(String prompt)
	{
		System.out.print(prompt);
		String answer = getLine();
	}

	protected void userCreatorLogin()
	{


	}

	public void info(String msg)
	{
		_log.info(msg);
	}

	public void debug(String msg)
	{
		_log.debug(msg);
	}

	public void error(String msg)
	{
		_log.error(msg);
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

	private void setForgotpasswordUrl(String path)
	{
		final String forgotContext = "json/users?_action=forgotPassword";
		if (path.trim().endsWith("/"))
		{
			this.forgotpasswordUrl = path + forgotContext;
		} else
		{
			this.forgotpasswordUrl = path +"/"+ forgotContext;
		}
	}

	private void setAmConfigProperties(Properties p, Map<String, String> params)
	{
		String path = params.get(OpenAmUtilConstants.IDP_PATH);
		String host = params.get(OpenAmUtilConstants.IDP_HOST);
		String protocol = params.get(OpenAmUtilConstants.IDP_PROTOCOL);
		String port = params.get(OpenAmUtilConstants.IDP_PORT);
		String fullPath = protocol + "://" + host + ":" + port;
		if (path.startsWith("/"))
			fullPath += path;
		else
			fullPath += "/" + path;

		setForgotpasswordUrl(fullPath);

		setAmServerProps(p, protocol, host, port, path);
		setCertProp(p, host);
		setLibertyHandlerProp(p, fullPath);
		setLoginProp(p, fullPath);
		setLibertyAuthProp(p, fullPath);
		setNamingUrlProp(p, fullPath);

		System.setProperty("com.sun.identity.agents.app.username", 
				p.getProperty("com.sun.identity.agents.app.username"));
        System.setProperty("com.iplanet.am.service.password", 
        		p.getProperty("com.iplanet.am.service.password"));
        System.setProperty("com.iplanet.am.service.secret", 
        		p.getProperty("com.iplanet.am.service.secret"));
		
		// save user creator info if we need to create info's outside of user's role (e.g. request account)
		if (params.containsKey(OpenAmUtilConstants.CREATOR_USER))
			creatorId = params.get(OpenAmUtilConstants.CREATOR_USER);
		if (params.containsKey(OpenAmUtilConstants.CREATOR_PASS))
			creatorPass = params.get(OpenAmUtilConstants.CREATOR_PASS);
	}

	private void setAmServerProps(Properties p, String protocol, String host, String port, String path)
	{
		System.setProperty("com.iplanet.am.server.protocol", protocol);
		System.setProperty("com.iplanet.am.server.host", host);
		System.setProperty("com.iplanet.am.server.port", port);
		System.setProperty("com.iplanet.am.services.deploymentDescriptor", path);
	}

	private void setCertProp(Properties p, String host)
	{
		String val = "test:SunSTS|test:" + host;
		System.setProperty("com.sun.identity.liberty.ws.trustedca.certaliases", val);
	}

	private void setLibertyHandlerProp(Properties p, String fullPath)
	{
		String url = "";
		if (fullPath.endsWith("/"))
			url = fullPath + "WSPRedirectHandler";
		else
			url = fullPath + "/WSPRedirectHandler";

		System.setProperty("com.sun.identity.liberty.interaction.wspRedirectHandler", url);
	}

	private void setLoginProp(Properties p, String fullPath)
	{
		String url = "";
		if (fullPath.endsWith("/"))
			url = fullPath + "UI/Login";
		else
			url = fullPath + "/UI/Login";
		System.setProperty("com.sun.identity.loginurl", url);
	}

	private void setLibertyAuthProp(Properties p, String fullPath)
	{
		String url = "";
		if (fullPath.endsWith("/"))
			url = fullPath + "Liberty/authnsvc";
		else
			url = fullPath + "/Liberty/authnsvc";
		System.setProperty("com.sun.identity.liberty.authnsvc.url", url);
	}

	private void setNamingUrlProp(Properties p, String fullPath)
	{
		String url = "";
		if (fullPath.endsWith("/"))
			url = fullPath + "namingservice";
		else
			url = fullPath + "/namingservice";
		System.setProperty("com.iplanet.am.naming.url", url);
	}

	public boolean resetPassword(final String username)
	{
		try
		{
			_log.info("Using forgotpassword url: " + forgotpasswordUrl);
			URL forgotpasswordURL = new URL(forgotpasswordUrl);
			HttpURLConnection httpURLConnection = (HttpURLConnection) forgotpasswordURL.openConnection();
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setRequestProperty("Content-type",
					"application/json");

			OutputStreamWriter osw = new OutputStreamWriter(httpURLConnection.getOutputStream());
			JSONObject json = new JSONObject();
			json.put("username", username);
			osw.write(json.toString());
			osw.flush();

			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				_log.debug("Password Reset Request Successful.");
				return true;
			} else {
				_log.debug("Password Reset Request Failued; Response "
						+ "Code: " + responseCode);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}
}

