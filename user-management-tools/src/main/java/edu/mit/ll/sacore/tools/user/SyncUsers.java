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
import java.util.List;

/**
 *
 */
public class SyncUsers extends UserManagementAction {
	
	private Connection conn2; 
	
	private CreateUser createUser;
	private CreateUserOrg createUserOrg;
	private CreateUserOrgWorkspace createUserOrgWorkspace;
	private SyncOrgs syncOrgs;
	private List<Integer> workspaceids;
	
	/**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public SyncUsers(String username1, String password1, String host1,
            String database1, String username2, String password2, String host2,
            String database2) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        super(username1, password1, host1, database1);
        
        this.conn2 = getConnection(username2, password2, host2, database2);
        
        //Class that will connect to second database and create missing user
        this.createUser = new CreateUser(username2, password2, host2, database2);
        
        //Class that will connect to second database and create missing userorg
        this.createUserOrg = new CreateUserOrg(username2, password2, host2, database2);
        
        this.createUserOrgWorkspace = new CreateUserOrgWorkspace(username2, password2, host2, database2);
        
        this.workspaceids = this.createUserOrgWorkspace.getWorkspaceIds();
        
        this.syncOrgs = new SyncOrgs(username1, password1, host1,
                database1, username2, password2, host2,
                database2);
        
        this.syncOrgs.syncDatabases(); //Make sure the orgs are set up first
    }
    
    public void listUsers(){
    	try{
	    	PreparedStatement ps =
	                conn
	                        .prepareStatement("select * from \"user\" where enabled='t';");
	        ResultSet user_rs = ps.executeQuery();
	        
	        while(user_rs.next()){
	        	String username = user_rs.getString("username");
	        	System.out.println(username);
	        	/*PreparedStatement get_user =
	                    conn2
	                            .prepareStatement("select username from \"user\" where username=?");
	        	
	        	get_user.setString(1, username);
	        	ResultSet user_exists = get_user.executeQuery();
	        	
	        	if(!user_exists.next()){
	        		System.out.println(user_rs.getString("username"));
	        	}*/
	        }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    public void syncDatabases(){
    	try{
    		PreparedStatement ps =
                    conn
                            .prepareStatement("select * from \"user\" where enabled='t';");
	        ResultSet user_rs = ps.executeQuery();
	        
	        while(user_rs.next()){
	        	String username = user_rs.getString("username");
	        	
	        	PreparedStatement get_user =
	                    conn2
	                            .prepareStatement("select username from \"user\" where username=?");
	        	
	        	get_user.setString(1, username);
	        	ResultSet user_exists = get_user.executeQuery();
	        	
	        	if(!user_exists.next()){
	        		int userid = user_rs.getInt("userid");
		        	
	        		System.out.println("Create Missing User <" + username + ">");
	        		this.createUser.createUserEntry(
	        				username, 
	        				this.createUser.decryptPassword(user_rs.getBytes("passwordencrypted")), 
	        				user_rs.getBoolean("enabled"), 
	        				user_rs.getString("firstname"),
	        				user_rs.getString("lastname"), 
	        				user_rs.getString("created"),
	        				false);
	        		
	        		//Get userorgs from the first database to add to the second
	        		PreparedStatement ps_userorgs =
		                    conn
		                            .prepareStatement("select * from userorg where userid=?");
		        	
	        		ps_userorgs.setInt(1,  userid);
	        		
	        		ResultSet userOrg = ps_userorgs.executeQuery();
		        	while(userOrg.next()){
		        		//Search DB1 for org name
		        		String orgName = findOrgNameById(userOrg.getInt("orgid"));
		        		System.out.println("Adding Missing User <" + username + "> to organization <" + orgName + ">");
		        		
		        		//Search db2 for Org Id using name
			        	this.createUserOrg.createUserOrgEntry(
	        					username, orgName,
	        					findRoleNameById(userOrg.getInt("systemroleid")), 
	        					userOrg.getBoolean("enabled"),
	        					userOrg.getString("rank"), 
	        					userOrg.getString("unit"));
			        }
	        		
	        		//Get contact entries from the first database to add to the second
		        	PreparedStatement ps_contacts =
		                    conn
		                            .prepareStatement("select * from contact where userid=?");
		        	
		        	ps_contacts.setInt(1, userid);
		        	
		        	ResultSet contacts = ps_contacts.executeQuery();
	        		while(contacts.next()){
	        			String value = contacts.getString("value");
	        			System.out.println("Create contact for <" + username + "> with value <" + value + ">");
	        			this.createUser.createUserContact(username, contacts.getInt("contacttypeid"), contacts.getBoolean("enabled"), value);
	        		}
	        		
	        		/*****************************************************************************************/
	        		PreparedStatement get_new_user =
		                    conn2
		                            .prepareStatement("select userid from \"user\" where username=?");
		        	
		        	get_new_user.setString(1, username);
		        	ResultSet new_user_exists = get_new_user.executeQuery();
		        	if(new_user_exists.next()){
		        		int new_userid = new_user_exists.getInt("userid");
		        		
		        		//Add new userorgs to userorg_workspace
		        		PreparedStatement get_user_orgs =
			                    conn2
			                            .prepareStatement("select userorgid from userorg where userid=?");
			        	
			        	get_user_orgs.setInt(1, new_userid);
			        	ResultSet user_orgs = get_user_orgs.executeQuery();
			        	
			        	while(user_orgs.next()){
			        		int userorgid = user_orgs.getInt("userorgid");
			        		for(Integer id : this.workspaceids){
				            	this.createUserOrgWorkspace.createUserOrgWorkspace(id.intValue(), userorgid, false);
				            }
			        	}
		        	}
		        	/*****************************************************************************************/
	        	}
	        }
	    }catch(Exception e){
    		e.printStackTrace();
    	}
    }
}