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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class CreateUserOrgWorkspace extends UserManagementAction {
	
	private Map<String,String> folderMapping = new HashMap<String,String>();
	
	/**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public CreateUserOrgWorkspace(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException {
        super(username, password, host, database);
    }
    
    public List<Integer> getWorkspaceIds(){
    	List<Integer> list = new ArrayList<Integer>();
    	try{
	    	PreparedStatement ps =
	                conn
	                        .prepareStatement("select workspaceid from workspace;");
	        ResultSet rs = ps.executeQuery();
	        
	        while(rs.next()){
	        	Integer id = rs.getInt("workspaceid");
	        	list.add(id);
	        }
	        
	    }catch(Exception e){
    		e.printStackTrace();
    	}
    	return list;
    }
    
    /**
     * 
     * @throws SQLException
     */
    public boolean createUserOrgWorkspace()
            throws SQLException {
    	
    	List<Integer> workspaceIds = this.getWorkspaceIds();
    	
    	PreparedStatement ps =
                conn
                        .prepareStatement("select * from userorg;");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
        	try{
        		int userorgid = rs.getInt("userorgid");
	            boolean enabled = rs.getBoolean("enabled");
	            
	            for(Integer id : workspaceIds){
	            	this.createUserOrgWorkspace(id.intValue(), userorgid, enabled);
	            }
	        
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }
    	
        return true;
    }
    
    public void createUserOrgWorkspace(int workspaceid, int userorgid, boolean enabled){
    	try{
    		PreparedStatement ps_insert =
	                conn.prepareStatement("insert into userorg_workspace ("
	                		+ "userorg_workspace_id, workspaceid, userorgid, enabled) "
	                       + "values ((select nextval('hibernate_sequence')), ?, ?, ?);");
	        ps_insert.setInt(1, workspaceid);
	        ps_insert.setInt(2, userorgid);
	        ps_insert.setBoolean(3, enabled);
	        ps_insert.executeUpdate();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
}
