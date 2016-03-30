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
import java.sql.SQLException;

/**
 *
 */
public class CreateUserOrg extends UserManagementAction {

   

    /**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public CreateUserOrg(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException {
        super(username, password, host, database);
    }

    public boolean createUserOrgEntry(String username, String orgName, String rolename, boolean enabled, String rank, String unit) throws SQLException{
        Integer userid = findUserIdByName(username);
        Integer roleid = findRoleIdByName(rolename);
        Integer orgid = findOrgIdByName(orgName); 
            
        if(userid == null){
            return false;
        }
        if(roleid == null){
            return false;
        }
        
        return this.insertUserOrg(userid, orgid, roleid, enabled, rank, unit);
    }
    
    public boolean insertUserOrg(int userid, int orgid, int roleid, boolean enabled, String rank, String unit) throws SQLException{
    	PreparedStatement ps = conn.prepareStatement("insert into userorg " +
        		"(userorgid, userid, orgid, systemroleid, enabled, rank, unit, " +
        		"created) values (nextval('user_org_seq'),?,?,?,?,?,?,now())");
        ps.setInt(1, userid);
        ps.setInt(2, orgid);
        ps.setInt(3, roleid);
        ps.setBoolean(4, enabled);
        ps.setString(5, rank);
        ps.setString(6, unit);
        
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("insert into user table did not work properly; return value was {}",
                  res);
            return false;
        }
        return true;
    }
    
}
