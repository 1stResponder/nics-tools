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
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;

import edu.mit.ll.soa.sso.exception.AuthException;
import edu.mit.ll.soa.sso.exception.InitializationException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: cbudny
 * Date: 7/9/14
 * Time: 7:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpenAmUtils extends OpenAmBaseUtil
{
	private boolean loggedIn = false;
	
	private String password = null;

	public OpenAmUtils(Map<String, String> params) throws InitializationException
	{
		initialize(params);
	}

	public Set<String> getOrganizationNames()
	{
		return subRealms;
	}

	public Set<String> getUserNames()
	{
		return identities;
	}

	/**
	 *
	 * @param orgName Organization name to authenticate against (Realm)
	 * @return
	 */
	public void switchOrganizations(String orgName) throws AuthException
	{
//		doCurrentRealm(orgName);
		try
		{
			token = realmLogin(orgName);
		} catch (SSOException se)
		{
			error(se.getMessage());
			throw new AuthException(se.getMessage());
		} catch (AuthLoginException ale)
		{
			error(ale.getMessage());
			throw new AuthException("Error logging in\n" + ale.getMessage());
		} catch (Exception e)
		{
			error(e.getMessage());
			throw new AuthException(e.getMessage());
		}
	}

	/**
	 * Login to the Identity Provider
	 *
	 * @param userid User ID to login
	 * @param password Password
	 * @return SSO Token
	 * @throws AuthException
	 */
	public String login(String userid, String password) throws AuthException
	{
		return login(userid, password, DEF_REALM);
	}

	public String login(String userid, String password, String realm) throws AuthException
	{
		try
		{
			realmLogin(userid, password, realm);
			final String id = token.getTokenID().toString();
			loggedIn = true;

			return id;
		} catch (SSOException se)
		{
			se.printStackTrace();
//			error(se.getMessage());
			throw new AuthException(se.getMessage());
		} catch (AuthLoginException ale)
		{
			ale.printStackTrace();
//			error(ale.getMessage());
			throw new AuthException("Error logging in\n" + ale.getMessage());
		} catch (Exception e)
		{
			e.printStackTrace();
//			error(e.getMessage());
			throw new AuthException(e.getMessage());
		}
	}
	
	/**
	 * If the user has previously logged in and has a valid token, the token
	 * id is simply returned, otherwise null is returned
	 * 
	 * @return
	 */
	public String getTokenIfExists() {
		String tokenIdString = null;
		if(token != null) { // TODO:SSO what else? Maybe validate it?
			try {
				tokenIdString = token.getTokenID().toString();
				info("Got cached tokenid: " + tokenIdString);
			} catch(Exception e) {
				error("Exception getting token id from SSOToken");
			}
		}
		
		return tokenIdString;
	}

	public void logout() throws AuthException
	{
		try
		{
			doLogout();
		} catch (AuthLoginException ale)
		{
			throw new AuthException("Exception logging out");
		}

	}

	
	/**
	 * Attempts to destroy the Token and associated session with
	 * the specified token ID
	 * 
	 * @param tokenId
	 * 
	 * @return true if the token session was successfully destroyed, false otherwise
	 */
	public boolean destroyToken(String tokenId) {
		boolean status = false;
		SSOToken tokenToDestroy = null;
		
		try {
			tokenToDestroy = createTokenFromID(tokenId);
		} catch(SSOException e) {
			e.printStackTrace();
		}
		
		try {
			if(tokenToDestroy != null) {
				SSOTokenManager.getInstance().destroyToken(tokenToDestroy);
				status = true;
			}
		} catch (SSOException e) {
			
			e.printStackTrace();
		}
		
		return status;
	}
	
	public boolean isTokenValid(String token) throws AuthException
	{
		boolean valid = false;
		try
		{
			if (validateToken(token))
			{
				valid = true;
			}
		} catch (SSOException sse)
		{
			throw new AuthException("Could not validate TokenID");
		}


		return valid;
	}
	
	public boolean refreshSessionToken(String token) throws AuthException{
		try{
			if (validateToken(token)){
				refreshToken(token);
			}else{
				return false;
			}
		} catch (SSOException sse){
			throw new AuthException("Could not refresh TokenID");
		}
		return true;
	}

	/**
	 * Create a new user identity.
	 *
	 * @param email
	 * @param password
	 * @param first
	 * @param last
	 * @param active
	 * @return
	 */
	public boolean createIdentity(String email, String password, String first, String last, boolean active)
			throws AuthException
	{
		AMIdentity newid = createUserIdentity(email, password,  first, last, active);
		if (newid != null)
		{
			try
			{
				debug("Created "+ newid.getName() + " identity, isExists = " + newid.isExists());
				return true;
			} catch (Exception e)
			{
				throw new AuthException("Error accessing new user object");
			}
		}

		return false;
	}

	/**
	 * Creates new organization
	 * @param orgName
	 * @param active
	 * @return
	 */
	public boolean createOrganization(String orgName, boolean active) throws AuthException
	{
		AMIdentity newid = createRealmIdentity(orgName, active);
		if (newid != null)
		{
			try
			{
				debug("Created "+ newid.getName() + " identity, isExists = " + newid.isExists());
				return true;
			} catch (Exception e)
			{
				throw new AuthException("Error accessing new org object");
			}
		}

		return false;
	}

	public boolean createAgent(String name, boolean active)
	{
		// TODO



		return false;
	}
	
	public Map getAttributesForToken(String token) throws AuthException {
		
			return getUserAttributes(token);
	}
	
	public String toggleUserStatus(String email, boolean status) {
		Set<AMIdentity> ids = this.searchIdentities(IdType.USER, email);
		boolean result = false;
		
		if(ids != null && !ids.isEmpty()) {
			// Assuming found identity, MAY be more than one result if we didn't filter well enough
			
			// TODO: should probably return false here, although not sure if duplicate emails are allowed,
			//		should filter or look specifically at uid/name/mail attribute
			if(ids.size() > 1) {
				// Warning: more than one user is being returned...
				System.out.println("WARNING: more than one user found via email: " + email);
				return "Failed: More than one identity returned with email " + email;
			}
			// TODO:SSO use constnats for iNetUserStatus, Active, Inactive, etc...
			Iterator iter = ids.iterator();
			while(iter.hasNext()) {
				AMIdentity id = (AMIdentity)iter.next();
				
				System.out.println("Found identity: " + id.getName());
				if(id.getName().equals(email)) {
					result = setAttribute(id, "iNetUserStatus", (status) ? "Active" : "Inactive");
					if(result) {
						System.out.println("Successfully set user status to: " + ((status) ? "Active" : "Inactive"));
					} else {
						System.out.println("Failed to set user status!");
					}
					break;
				} else {
					System.out.println("Identity " + id.getName() + " doesn't match specified ID: " + email);
				}
			}
		}
		
		return ((result) ? "Success" : "Failed: user status not set");
	}

	public boolean changeUserPassword(String email, String newpw) {
		Set<AMIdentity> ids = this.searchIdentities(IdType.USER, email);
		boolean result = false;
		
		if(ids != null && !ids.isEmpty()) {
			

			if(ids.size() > 1) {
				// Warning: more than one user is being returned...
				System.out.println("WARNING: more than one user found via email: " + email);
				return false;
			}
			Iterator iter = ids.iterator();
			while(iter.hasNext()) {
				AMIdentity id = (AMIdentity)iter.next();
				
				System.out.println("Found identity: " + id.getName());
				if(id.getName().equals(email)) {
					result = setAttribute(id, "userpassword", newpw);
					if(result) {
						System.out.println("Successfully set user password.");
					} else {
						System.out.println("Failed to set user password!");
						return false;
					}
					break;
				} else {
					System.out.println("Identity " + id.getName() + " doesn't match specified ID: " + email);
				}
				
			
			}			
		
		}
		
		return true;
	}
}


