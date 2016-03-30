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
/**
 * 
 */
package edu.mit.ll.sacore.tools.user;

import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;

import edu.mit.ll.nics.common.rabbitmq.admin.RabbitAdmin;
import edu.mit.ll.nics.common.security.encrypt.PasswordEncrypter;
import edu.mit.ll.nics.common.security.hash.SaltedHash;

/**
 *
 */
public class CreateUser extends UserManagementAction {

    private PasswordEncrypter encrypter;
    private SaltedHash hasher;

    private String rabbitCookie = "AJDAHNYKRUYOPIHGTHYX";
    private String rabbitNode = "rabbit@localhost";
    
    /**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public CreateUser(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        super(username, password, host, database);
        this.encrypter = new PasswordEncrypter();
        this.hasher = new SaltedHash("SHA");
    }
    
    public boolean createUserEntry(String username, String password, boolean enabled, 
    		String firstname, String lastname, String datetime)
            throws SQLException {
    	return this.createUserEntry(username, password, enabled, firstname, lastname, datetime, true);
    }
    
    
    /**
     * @param username
     * @param password
     * @param enabled
     * @throws SQLException
     */
    public boolean createUserEntry(String username, String password, boolean enabled, 
    		String firstname, String lastname, String datetime, boolean createRabbitUser)
            throws SQLException {
        String hashed = hasher.hash(password, username);
        byte[] enc = encrypter.encrypt(password);
        Timestamp timeObj;
        if(datetime == null || datetime.isEmpty()){
            timeObj = new Timestamp(System.currentTimeMillis());
        }
        else{
            try {
                timeObj = Timestamp.valueOf(datetime);
            } catch (IllegalArgumentException e) {
                log.error("could not parse date: {}", e);
                return false;
            }
        }
        
        PreparedStatement ps =
                conn.prepareStatement("insert into \"user\" (userid, username, "
                       + "passwordhash, passwordencrypted, lastupdated, created, enabled, "
                       + "firstname, lastname) "
                       + "values (nextval('user_seq'),?,?,?,now(),?,?,?,?)");
        ps.setString(1, username);
        ps.setString(2, hashed);
        ps.setBytes(3, enc);
        ps.setTimestamp(4, timeObj);
        ps.setBoolean(5, enabled);
        ps.setString(6, firstname);
        ps.setString(7, lastname);
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("insert into user table did not work properly; return value was {}",
                  res);
            return false;
        }
        
        if(createRabbitUser){
	        RabbitAdmin admin = new RabbitAdmin(this.rabbitCookie, this.rabbitNode);
	        if(admin.add_user(username, password)){
	        	if(admin.set_permissions(username, ".*", ".*", ".*")){
	        		return true;
	        	}
	        }
        }
        // TODO should probably do some rollback...
        
        return false;
    }
    
    public void createUserContact(String username, int contacttypeid, boolean enabled, String value){
    	try{
    		Integer userid = findUserIdByName(username);
    		
    		PreparedStatement ps_insert =
	                conn.prepareStatement("insert into contact ("
	                		+ "contactid, created, userid, contacttypeid, enabled, value) "
	                       + "values ((select nextval('contact_seq')),now(), ?, ?, ?, ?);");
	        ps_insert.setInt(1, userid);
	        ps_insert.setInt(2, contacttypeid);
	        ps_insert.setBoolean(3, enabled);
	        ps_insert.setString(4, value);
	        ps_insert.executeUpdate();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    public String decryptPassword(byte[] password){
    	return this.encrypter.decrypt(password);
    }

	public void setRabbitCookie(String rabbitCookie) {
		this.rabbitCookie = rabbitCookie;
	}

	public void setRabbitNode(String rabbitNode) {
		this.rabbitNode = rabbitNode;
	}
}
