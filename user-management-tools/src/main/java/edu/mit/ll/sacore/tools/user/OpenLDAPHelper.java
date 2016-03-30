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
package edu.mit.ll.sacore.tools.user;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;

import edu.mit.ll.nics.common.security.encrypt.PasswordEncrypter;
import edu.mit.ll.nics.sso.util.SSOUtil;

/**
 * Helper class for pulling user data from the NICS DB to create
 * ldif output for OpenAM/OpenLDAP to add and modify users, and
 * add users to groups.
 * 
 * The ldif files produced must be executed separately on the NICS
 * Identity instance that houses our OpenLDAP instance.
 * 
 * TODO: Currently only supports creating two LDIF files, one for
 * 		adding users in the NICS db to LDAP, and one for adding
 * 		the aforementioned users to the "nics" group on the LDAP
 * 		server.
 * 
 * TODO: Make all the lidf values configurable
 * TODO: Make the group configurable
 * 
 * @author jp
 *
 */
public class OpenLDAPHelper extends DbAction {

	private Connection conn;
    private Logger log;
    private PasswordEncrypter encrypter;
    private boolean active;
    private boolean enabled;
    private String user;
    
    private String modifyGroup = "";
    private static final String modifyGroupHeader = "\ndn: cn=nics,ou=Groups,dc=NICS,dc=LL,dc=MIT,dc=EDU\n" +
    		"changetype: modify\n" +
    		"add: member\n" +
    		"member: ";

    public OpenLDAPHelper() {
    	
    }
    
    public OpenLDAPHelper(String username, String password, String host,
            String database, boolean active, boolean enabled, String user) 
            		throws SQLException, ClassNotFoundException, NoSuchAlgorithmException {
        this.conn = getConnection(username, password, host, database);
        this.log = getLogger();
        this.encrypter = new PasswordEncrypter();
        this.active = active;
        this.enabled = enabled;
        this.user = user;
        
    }
    
    /**
     * Writes out two ldif files containing the data necessary to add NICS users to
     * OpenLDAP, as well as add those same users to the "nics" group
     * 
     * TODO: Rename to be more appropriate, especially when more features are added
     * 
     * @return
     */
    public String writeLDIFFile() {
    	boolean status = false;
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss");
    	String timestamp = sdf.format(new Date(System.currentTimeMillis()));
    	String addUsersFilename = "ldapHelper-nicsUsers-" + timestamp + ".ldif";
    	String addToGroupFilename = "ldapHelper-addToGroup-" + timestamp + ".ldif";
    	    	
    	FileOutputStream out = null;
    	try {    		
			out = new FileOutputStream(addUsersFilename);
			out.write(getAddUserInfo().getBytes());
			out.flush();
			out.close();
			
			out = new FileOutputStream(addToGroupFilename);
			out.write(modifyGroup.getBytes());
			out.flush();
			out.close();
			
			status = true;
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    	
    	if(status) {
    		return "\n\nSuccessfully wrote ldif info to files:\n\t" + addUsersFilename + 
    				"\n\t" + addToGroupFilename + "\n\n";
    	} else {
    		return "\n\nThere was a problem writing ldif info to files!\n\n";
    	}
    }
    
    private String getAddUserInfo() {
        StringBuilder sb = new StringBuilder();
    	
    	try {
        	String query = "select username, firstname, lastname, passwordencrypted " + 
        			"from \"user\" where enabled = ? and active = ?";
        	
        	String usersWithEnabledUserorgWorkspace = "select username, firstname, lastname, " +
        			"passwordencrypted from \"user\" where userid in " +
        			"(select distinct uo.userid from userorg_workspace uw, userorg uo " +
        			"where uw.userorgid=uo.userorgid and uw.enabled='t') and enabled='t' and active='t';";
        	
        	String userQuery = "select username, firstname, lastname, passwordencrypted " + 
        			"from \"user\" where username = ?";
        	
        	PreparedStatement ps = null; 
        			
        	if(this.user != null && !this.user.isEmpty()) {
        		ps = conn.prepareStatement(userQuery);
        		ps.setString(1, this.user);
        	} else {
        		ps = conn.prepareStatement(usersWithEnabledUserorgWorkspace);
        	}            
        	
        	//PreparedStatement ps = conn.prepareStatement(query);
            //ps.setBoolean(1, enabled);
            //ps.setBoolean(2, active);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
               
            	String user = rs.getString("username");
            	String first = rs.getString("firstname");
            	String last = rs.getString("lastname");
            	String pass = null;
            	
            	byte[] pw = rs.getBytes("passwordencrypted");
                if (pw != null) {
                    pass = encrypter.decrypt(pw);
                }
                
                if(pass != null) {
                	log.info("Building LDIF info for user " + user);
                	sb.append(appendLdifInfo(user, first, last, pass));
                } else {
                	log.warn("User's password was null, so not adding: " + user);
                }
                
                count++;
            } 
            
            log.info("Processed " + count + " users\n\n");
            
        } catch (SQLException e) {
            log.error("problem retrieving password", e);
            return null;
        }
    	
    	return sb.toString();
    }
    
    
    private String appendLdifInfo(String user, String first, String last, String pass) {
    	/*
    	 	dn: uid=test1,ou=People,dc=NICS,dc=LL,dc=MIT,dc=EDU
			objectClass: inetOrgPerson
			uid: test1
			sn: Testerson
			givenName: Tester
			cn: Tester Testerson
			displayName: Tester Testerson 1st
			userPassword: TestPassword!
			mail: test.testerson@ll.mit.edu
    	 */
    	
    	StringBuilder sb = new StringBuilder();
    	
    	appendUserAddToGroup(user);    	
    	
    	sb.append("\ndn: uid=").append(user).append(",ou=People,dc=NICS,dc=LL,dc=MIT,dc=EDU\n");
    	sb.append("objectClass: inetOrgPerson\n");
    	sb.append("uid: ").append(user).append("\n");
    	sb.append("sn: ").append(last).append("\n");
    	sb.append("givenName: ").append(first).append("\n");
    	sb.append("cn: ").append(first).append(" ").append(last).append("\n");
    	sb.append("displayName: ").append(first).append(" ").append(last).append("\n");
    	sb.append("userPassword: ").append(pass).append("\n");
    	sb.append("mail: ").append(user).append("\n");
    	
    	return sb.toString();
    }
    
    private void appendUserAddToGroup(String member) {
    	
    	modifyGroup += modifyGroupHeader;
    	modifyGroup += "uid=" + member + ",ou=People,dc=NICS,dc=LL,dc=MIT,dc=EDU\n";
    	
    }
    
    public String setUserStatus(String user, boolean status) {
    	SSOUtil ssoUtil = new SSOUtil(); //new SSOUtil("https", "nics-identity1.nics.ll.mit.edu", "443", "openam");
    	ssoUtil.loginAsAdmin();
    	String userStatusResult = ssoUtil.toggleUserStatus(user, status);
    	return userStatusResult;
    }
    
    public boolean createUser(String user, String first, String last, String pass) {
    	boolean status = false;
    	
    	SSOUtil ssoUtil = new SSOUtil("https", "nics-identity1.nics.ll.mit.edu", "443", "openam");
    	
    	log.info("Testing SSOUtil");
    	
    	log.info("Does SSOUtil have a token yet?: " + ssoUtil.getTokenIfExists());
    	
    	//String token = ssoUtil.login("amAdmin", "nics!openam$");
    	String token = ssoUtil.login("nicsadmin", "TestPassword!", "/");
    	//String token = ssoUtil.login("usercreator@nics.ll.mit.edu", "TestPassword!", "/");
    	//String token = ssoUtil.login("UrlAccessAgent", "nics!agent$", "/");
    	//String token = ssoUtil.login("jpullen@ll.mit.edu", "TestPassword!", "/");
    	
    	if(token != null) {
    		log.info("Got token: " + token);
    		status = true;
    	} else {
    		log.info("Failed to get token!");
    	}
    	
    	/*log.info("\nGetting attributes!:\n");
    	try {
	    	//Map map = ssoUtil.getAttributes();
    		Map map = ssoUtil.getUserAttributes(token);
    		if(map != null) {
    			Iterator iter = map.keySet().iterator();
    			while(iter.hasNext()) {
    	    		String key = (String)iter.next();
    	    		ssoUtil.info("key:val | " + key + " : " + map.get(key));
    	    	}
    		} else {
    			log.info("getUserAttributes came back NULL");
    		}
	    	
    	} catch(Exception e) {
    		log.error("Got exception trying to fetch attributes: " + e.getMessage());
    	}*/
    	
    	log.info("\n\n============================================\n\n");
    	log.info("Creating user: user("+user+"), pass("+pass+"), first("+first+"), last("+last+")");
    	try {
    		ssoUtil.createUser(user, pass, first, last, true);
    	} catch(Exception e) {
    		log.error("Got exception attempting to create user: " + e.getMessage());
    		e.printStackTrace();
    	}
    	
    	return status;
    }
    
    private static StandardPBEStringEncryptor encryptor = null;
    
    public static String encrypt(String text) {
    	initEncryptor();
    	
    	return encryptor.encrypt(text);
    }
    
    public static String decrypt(String text) {
    	initEncryptor();
    	
    	return encryptor.decrypt(text);
    }
    
    private static void initEncryptor()
	{
		try
		{
			if (encryptor == null)
			{
				encryptor = new StandardPBEStringEncryptor();

				String prop = System.getProperty("jasypt.password");
				if (prop == null) prop = "secretpassword";
				final String decode = prop;

				if (decode != null)
				{
					encryptor.setPassword(decode);
					encryptor.setAlgorithm("PBEWITHMD5ANDDES");

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
}
