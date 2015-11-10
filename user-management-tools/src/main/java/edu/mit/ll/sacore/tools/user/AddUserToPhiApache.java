/**
 * Copyright (c) 2008-2015, Massachusetts Institute of Technology (MIT)
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
/**
 * 
 */
package edu.mit.ll.sacore.tools.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.mit.ll.nics.common.ws.client.JSONRequest;

import org.apache.commons.codec.binary.Base64;

/**
 *
 */
public class AddUserToPhiApache extends UserManagementAction {
	
	RetrievePassword rp;
    
    /**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public AddUserToPhiApache(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        super(username, password, host, database);

    	rp = new RetrievePassword(username, password, host, database);
    }
    
    public String getPassword(String username)
            throws SQLException {
    	try{
    		MessageDigest SHA1 = MessageDigest.getInstance("SHA1");
            String password = this.rp.findPasswordByUserName(username);
            if(password != null){
	    		try {
	             	SHA1.reset();
	                 byte[] toEncrypt = password.getBytes("UTF-8");
	                 SHA1.update(password.getBytes());
	             } catch (Exception e) {
	                 e.printStackTrace();
	             }
	
	             byte[] encryptedRaw = SHA1.digest();
	             byte[] encoded = Base64.encodeBase64(encryptedRaw);
	
	             try {
	                 String encrypted = new String(encoded, "UTF-8");
	                 System.out.println("Encrypted: " + encrypted);
	         		 return encrypted;
	             } catch (Exception e) {
	                 e.printStackTrace();
	             }
            }
		}catch(Exception e){
    		e.printStackTrace();
    	}
    	return null;
    }
    
    
    /**
     * @throws SQLException
     */
    public boolean addUsers()
            throws SQLException {
    	PreparedStatement ps =
                conn
                        .prepareStatement("select * from \"user\" where userid not in (select user_id from phi_apache) and active='t';");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
        	try{
        		int userid= rs.getInt("userid");
	            String passwordHash = rs.getString("passwordhash");
	            String username = rs.getString("username");
	            
	            this.createPhiAapacheEntry(userid, passwordHash, username);
	        
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }
    	
        return true;
    }
    
    /**
     * @throws SQLException
     */
    private boolean createPhiAapacheEntry(int userid, String passwordHash, String username)
            throws SQLException {
    	
    	String password = this.getPassword(username);
    	if(password != null){
	        PreparedStatement ps =
	                conn.prepareStatement("insert into phi_apache "
	                       + "(user_id, username, password) "
	                       + "values (?,?,?)");
	        ps.setInt(1, userid);
	        ps.setString(2, username);
	        ps.setString(3, password);
	        int res = ps.executeUpdate();
	        if (res != 1) {
	            log.error("There was an error adding the user " + username);
	            return false;
	        }
	       return true;
    	}else{
    		System.out.println("Could not add user: " + username);
    	}
    	return false;
    }
}
