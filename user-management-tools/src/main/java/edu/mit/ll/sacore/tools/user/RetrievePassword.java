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

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

import edu.mit.ll.nics.common.security.encrypt.PasswordEncrypter;

/**
 *
 */
public class RetrievePassword extends DbAction {

    private Connection conn;
    private Logger log;
    private PasswordEncrypter encrypter;

    public RetrievePassword(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        this.conn = getConnection(username, password, host, database);
        this.log = getLogger();
        this.encrypter = new PasswordEncrypter();
    }
    
    public boolean locateUser(String username){
    	try{
	    	PreparedStatement get_user =
	                conn
	                        .prepareStatement("select username from \"user\" where username=?");
	    	
	    	get_user.setString(1, username);
	    	ResultSet user_exists = get_user.executeQuery();
	    	
	    	if(!user_exists.next()){
	    		return false;
	    	}else{
	    		return true;
	    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return false;
    }

    public String findPasswordByUserName(String username) {
        try {
            PreparedStatement ps =
                    conn
                            .prepareStatement("select passwordencrypted from \"user\" where username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                byte[] pw = rs.getBytes("passwordencrypted");
                if (pw != null) {
                    return encrypter.decrypt(pw);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error("problem retrieving password", e);
            return null;
        }
    }

}