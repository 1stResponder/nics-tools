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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class CopyUserOrg extends UserManagementAction {

	private CreateUserOrg createUserOrg = null;
	private CreateUserOrgWorkspace createUserOrgWorkspace = null;

    /**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public CopyUserOrg(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException {
        super(username, password, host, database);
        
        this.createUserOrg = new CreateUserOrg(username, password, host, database);
        this.createUserOrgWorkspace = new CreateUserOrgWorkspace(username, password, host, database);
    }

    public boolean copyFrom(String fromUsername, String toUsername) throws SQLException{
        Integer fromUserid = findUserIdByName(fromUsername);
        Integer toUserid = findUserIdByName(toUsername);
            
        if(fromUserid == null){
            return false;
        }
        if(toUserid == null){
            return false;
        }
        
        PreparedStatement ps =
                conn.prepareStatement("select * from userorg where userid=?");
        ps.setInt(1, fromUserid);
        ResultSet from_userorgs = ps.executeQuery();
        
        List<Integer> toOrgIds = this.getOrgIds(toUserid);
        
        while(from_userorgs.next()){
        	Integer orgId = from_userorgs.getInt("orgid");
        	if(!toOrgIds.contains(orgId)){
        		createUserOrg.insertUserOrg(
        				toUserid, 
        				from_userorgs.getInt("orgid"), 
        				from_userorgs.getInt("systemroleid"), 
        				from_userorgs.getBoolean("enabled"), 
        				from_userorgs.getString("rank"),
        				from_userorgs.getString("unit"));
        	}
        }
        
        List<Integer> newUserOrgIds = this.getUserOrgIds(toUserid);
        List<Integer> workspaceIds = this.createUserOrgWorkspace.getWorkspaceIds();
        
        for(Iterator<Integer> itr = newUserOrgIds.iterator(); itr.hasNext();){
        	int userorgid = itr.next();
        	for(Iterator<Integer> wsItr = workspaceIds.iterator(); wsItr.hasNext();){
        		this.createUserOrgWorkspace.createUserOrgWorkspace(wsItr.next(), userorgid, true);
        	}
        }
        
        return true;
    }
    
    private List<Integer> getUserOrgIds(int userid) throws SQLException{
    	List<Integer> ids = new ArrayList<Integer>();
    	
    	PreparedStatement ps =
                conn.prepareStatement("select userorgid from userorg where userid=?");
        ps.setInt(1, userid);
        ResultSet userorg_ids = ps.executeQuery();
        while(userorg_ids.next()){
        	int userorgid = userorg_ids.getInt("userorgid");
        	ids.add(userorgid);
        }
        return ids;
    }
    
    private List<Integer> getOrgIds(int userid) throws SQLException{
    	List<Integer> ids = new ArrayList<Integer>();
    	
    	PreparedStatement ps =
                conn.prepareStatement("select orgid from userorg where userid=?");
        ps.setInt(1, userid);
        ResultSet org_ids = ps.executeQuery();
        while(org_ids.next()){
        	int orgid = org_ids.getInt("orgid");
        	ids.add(orgid);
        }
        return ids;
    }
    
}
