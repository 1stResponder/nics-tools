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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class CreateContact extends UserManagementAction {

    
    
    /**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public CreateContact(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException {
        super(username, password, host, database);
    }

    public boolean createContactEntry(String username, String contactType,
            String value) throws SQLException {
        Integer userId = findUserIdByName(username);
        Integer contactTypeId = findContactTypeIdByName(contactType);
        
        if(userId == null){
            return false;
        }
        if(contactTypeId == null){
            return false;
        }
        
        PreparedStatement ps =
                conn.prepareStatement("insert into contact (contactid, userid, contacttypeid, " +
                		"value, enabled, created) values (nextval('contact_seq'),?,?,?,true,now())");
        ps.setInt(1, userId);
        ps.setInt(2, contactTypeId);
        ps.setString(3, value);
        
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("insert into contact table did not work properly; return value was {}",
                  res);
            return false;
        }
        return true;
        
    }
    
    private Integer findContactTypeIdByName(String typename) throws SQLException {
        PreparedStatement getContactTypeId = conn.prepareStatement("select contacttypeid from contacttype where type = ?");
        getContactTypeId.setString(1, typename);
        ResultSet contactTypeId = getContactTypeId.executeQuery();
        if(contactTypeId.next()){
            return contactTypeId.getInt(1);            
        }
        else{
            getLogger().error("nothing found for contact type " + typename);
            return null;
        }
    }
}
