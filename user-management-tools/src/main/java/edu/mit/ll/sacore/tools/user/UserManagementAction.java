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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 *
 */
public class UserManagementAction extends DbAction {

    protected Connection conn;
    protected Logger log;

    /**
     * 
     */
    public UserManagementAction(String username, String password, String host, String database) throws SQLException, ClassNotFoundException {
        this.conn = getConnection(username, password, host, database);
        this.log = getLogger();
    }
    
    protected Integer findRoleIdByName(String rolename) throws SQLException {
        PreparedStatement getRoleId = conn.prepareStatement("select systemroleid from systemrole where rolename = ?");
        getRoleId.setString(1, rolename);
        ResultSet roleId = getRoleId.executeQuery();
        if(roleId.next()){
            return roleId.getInt(1);            
        }
        else{
            getLogger().error("nothing found for role " + rolename);
            return null;
        }
    }
    
    protected Integer findUserIdByName(String username) throws SQLException{
        PreparedStatement getUserId = conn.prepareStatement("select userid from \"user\" where username = ?");
        getUserId.setString(1, username);
        ResultSet userId = getUserId.executeQuery();
        if(userId.next()){
            return userId.getInt(1);            
        }
        else{
            getLogger().error("nothing found for user " + username);
            return null;
        }
    }
    
    protected Integer findOrgIdByName(String org) throws SQLException{
        PreparedStatement getOrgId = conn.prepareStatement("select orgid from org where name = ?");
        getOrgId.setString(1, org);
        ResultSet orgId = getOrgId.executeQuery();
        if(orgId.next()){
            return orgId.getInt(1);            
        }
        else{
            getLogger().error("nothing found for org " + org);
            return null;
        }
    }
    
    protected String findOrgNameById(int orgid) throws SQLException{
    	PreparedStatement getOrgId = conn.prepareStatement("select name from org where orgid = ?");
        getOrgId.setInt(1, orgid);
        ResultSet orgId = getOrgId.executeQuery();
        if(orgId.next()){
            return orgId.getString(1);            
        }
        else{
            getLogger().error("nothing found for org " + orgid);
            return null;
        }
    }
    
    protected String findRoleNameById(int roleid) throws SQLException{
    	PreparedStatement getOrgId = conn.prepareStatement("select rolename from systemrole where systemroleid = ?");
        getOrgId.setInt(1, roleid);
        ResultSet systemrole = getOrgId.executeQuery();
        if(systemrole.next()){
            return systemrole.getString(1);            
        }
        else{
            getLogger().error("nothing found for systemroleid " + roleid);
            return null;
        }
    }
    
    protected List<String> getUsernames(Map args) throws SQLException {
    	List<String> usernames = new ArrayList<String>();
    	String opts = " ";
    	// Check args for what kind of conditions to place on the user query
    	
    	if(args != null) {
    		// TODO:SSO process args
    	}
    	
    	PreparedStatement getUsernames = conn.prepareStatement("select username from \"user\"" + opts);
    	ResultSet usersSet = getUsernames.executeQuery();
    	
    	String username = null;
    	if(usersSet == null) {
    		return usernames;
    	}
    	
    	while(usersSet.next()) {
    		username = usersSet.getString(1);
    		if(username != null) {
    			usernames.add(username);    			
    		}
    	}
    	
    	return usernames;
    }
    
}
