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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class SyncOrgs extends UserManagementAction {
	
	private Connection conn2; 
	
	private CreateOrg createOrg;
	
	/**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public SyncOrgs(String username1, String password1, String host1,
            String database1, String username2, String password2, String host2,
            String database2) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        super(username1, password1, host1, database1);
        
        this.conn2 = getConnection(username2, password2, host2, database2);
        
        //Class that will connect to second database and create missing user
        this.createOrg = new CreateOrg(username2, password2, host2, database2);
    }
    
    public void syncDatabases(){
    	try{
    		PreparedStatement ps =
                    conn
                            .prepareStatement("select * from org;");
	        ResultSet org_rs = ps.executeQuery();
	        
	        while(org_rs.next()){
	        	String orgname = org_rs.getString("name");
	        	
	        	PreparedStatement get_org =
	                    conn2
	                            .prepareStatement("select name from org where name=?");
	        	
	        	get_org.setString(1, orgname);
	        	ResultSet org_exists = get_org.executeQuery();
	        	
	        	if(!org_exists.next()){
	        		System.out.println("Create Missing Org <" + orgname + ">");
	        		this.createOrg.createOrgEntry(
	        				org_rs.getString("name"), 
	        				org_rs.getString("county"), 
	        				org_rs.getString("state"), 
	        				org_rs.getString("prefix"), 
	        				org_rs.getString("distribution"), 
	        				org_rs.getDouble("defaultlatitude"), 
	        				org_rs.getDouble("defaultlongitude"), 
	        				org_rs.getString("country"), 
	        				org_rs.getDate("created"));

	        	}
	        }
	    }catch(Exception e){
    		e.printStackTrace();
    	}
    }
}
