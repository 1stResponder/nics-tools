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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import edu.mit.ll.nics.common.rabbitmq.admin.RabbitAdmin;
import edu.mit.ll.nics.common.rabbitmq.client.RabbitConsumer;

/**
 *
 */
public class ValidatePassword extends DbAction{
    private RabbitAdmin admin;
    private String dbHost;
    private String database;
    private String dbUsername;
    private String dbPassword;
    private Logger log;

    public ValidatePassword(String dbUsername, String dbPassword, String dbHost, String database,
    		String rabbitNode, String cookie) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        
        this.admin = new RabbitAdmin(cookie, rabbitNode);
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.dbHost = dbHost;
        this.database = database;
        this.log = getLogger();
    }

    
    public void syncAllRabbitUsersToInstancePassword(String instancePassword, String createUser) throws NoSuchAlgorithmException {
    	// TODO:SSO query the db for all users, loop through them, and call syncToInstancePassword on each
    	List<String> users = new ArrayList<String>();
    	try {
			UserManagementAction action = new UserManagementAction(dbUsername, dbPassword, dbHost, database);
			users = action.getUsernames(null);
			
			if(users != null) {
				System.out.println("Number of users returned: " + users.size());
				
				for(String user : users) {
					try {
						syncToInstancePassword(user, instancePassword, createUser);
					} catch(Exception e) {
						System.out.println("Caught unhandled exception attempting to sync rabbit password for user: " + 
								user + ": " + e.getMessage());
					}
				}
			} else {
				System.out.println("\nQuery returned no users!\n");
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    public void syncToInstancePassword(String username, String instancePassword, String createUser) throws NoSuchAlgorithmException, SQLException, ClassNotFoundException {
    	RabbitConsumer consumer = null;
    	//RetrievePassword retrievePassword = new RetrievePassword(this.dbUsername, this.dbPassword, this.dbHost, this.database);
    	//String password = retrievePassword.findPasswordByUserName(username);
    	    	
    	try{
	    	consumer = new RabbitConsumer(username, instancePassword, "localhost", 5672, null, null, new ArrayList<String>());
	    	System.out.println("Successfully connected to rabbit with user " + username + ", no modification required");
    	}catch(InstantiationError e){
    		System.out.println("Error connecting to rabbit with username " + username + " and password " + instancePassword);
    	}
    	if(consumer == null && 
				createUser.equalsIgnoreCase(Boolean.TRUE.toString()) &&
				instancePassword != null){
    		this.admin.delete_user(username);
    		this.admin.add_user(username, instancePassword);
    		this.admin.set_permissions(username, ".*", ".*", ".*");
    		System.out.println("Created account for user with new instance password");
    	}
    	if(consumer != null){
    		//Destroy consumer
	    	consumer.destroy();
    	}
    }
    
    public void validateRabbitLogin(String username, String createUser) throws NoSuchAlgorithmException, SQLException, ClassNotFoundException{
    	RabbitConsumer consumer = null;
    	RetrievePassword retrievePassword = new RetrievePassword(this.dbUsername, this.dbPassword, this.dbHost, this.database);
    	String password = retrievePassword.findPasswordByUserName(username);
    	try{
	    	consumer = new RabbitConsumer(username, password, "localhost", 5672, null, null, new ArrayList<String>());
	    	//System.out.println("Successfully connected to rabbit with user " + username);
    	}catch(InstantiationError e){
    		System.out.println("Error connecting to rabbit with username " + username + " and password " + password);
    	}
    	if(consumer == null && 
				createUser.equalsIgnoreCase(Boolean.TRUE.toString()) &&
				password != null){
    		this.admin.delete_user(username);
    		this.admin.add_user(username, password);
    		this.admin.set_permissions(username, ".*", ".*", ".*");
    	}
    	if(consumer != null){
    		//Destroy consumer
	    	consumer.destroy();
    	}
    }
}
