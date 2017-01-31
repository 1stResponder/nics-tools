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
package edu.mit.ll.sacore.tools.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.ll.sacore.tools.user.AddArcGISLayers;
import edu.mit.ll.sacore.tools.user.AddUserToPhiApache;
import edu.mit.ll.sacore.tools.user.ChangeUsername;
import edu.mit.ll.sacore.tools.user.CopyUserOrg;
import edu.mit.ll.sacore.tools.user.CreateContact;
import edu.mit.ll.sacore.tools.user.CreateFolderWorkspace;
import edu.mit.ll.sacore.tools.user.CreateUser;
import edu.mit.ll.sacore.tools.user.CreateUserOrg;
import edu.mit.ll.sacore.tools.user.CreateUserOrgWorkspace;
import edu.mit.ll.sacore.tools.user.OpenLDAPHelper;
import edu.mit.ll.sacore.tools.user.RetrievePassword;
import edu.mit.ll.sacore.tools.user.SyncUsers;
import edu.mit.ll.sacore.tools.user.ValidatePassword;

/**
 *
 */
public class UserManagementCLI {

	private Logger log;
	
    public static void main(String[] args){
        UserManagementCLI cli = new UserManagementCLI();
        cli.handleArgs(args);
    }
    
    public UserManagementCLI() {
		log = LoggerFactory.getLogger(UserManagementCLI.class);
	}
    
    public void handleArgs(String args[]) {
        try {
            Options opt = new Options();

            opt.addOption("help", false,
                     "Print help for this application");
            opt.addOption("dbu", "db-username", true, "The username to use for the database");
            opt.addOption("dbp", "dp-password", true, "The password to use for the database");
            opt.addOption("h", "host", true, "The db server url to connect to");
            opt.addOption("db", true, "The schema/database to connect use");
            opt.addOption("dbu2", "db-username", true, "The username to use for the database to copy to");
            opt.addOption("dbp2", "dp-password", true, "The password to use for the database to copy to");
            opt.addOption("h2", "host", true, "The db server url to connect to");
            opt.addOption("db2", true, "The schema/database to connect use");
            opt.addOption("gp", "get-password", true,
                    "Query to find a user's password");
            opt.addOption("c", "create-user", false,
                    "Register a user in the database");
            opt.addOption("u", "username", true,
                    "Used with -c or -o or -con: the username of the user to create");
            opt.addOption("p", "password", true,
                    "Used with -c: the password of the user to create");
            opt.addOption("fn", "firstname", true, "Used with -c: The user's first name");
            opt.addOption("ln", "lastname", true, "Used with -c: The user's last name");
            opt.addOption("dt", "date", true, "Used with -c: The date the account was created");
            opt.addOption("en", "enabled-create", false,
                    "Used with -c or -o: if the user should be enabled");
            opt.addOption("o", "user-org", false, "Register a user with a particular organization");
            opt.addOption("org", "org-name", true, "Used with -o: the name of the org to register with");
            opt.addOption("r", "rolename", true, "Used with -o: the name of the role the user has in the org");
            opt.addOption("rank", true, "Used with -o: the user's rank");
            opt.addOption("unit", true, "Used with -o: the user's unit");
            opt.addOption("n","rabbit-node", true, "The rabbit node name -- rabbit@hostname");
            opt.addOption("ck","rabbit-cookie", true, "The rabbit cookie value (machine specific)");
            opt.addOption("con","create-contact", false, "Create a contact entry for a user");
            opt.addOption("ctype", "contact-type", true, "Used with -con: the contact-type string");
            opt.addOption("cval", "contact-value", true, "Used with -con: the contact value");
            opt.addOption("chu", "change-username", false, "Change a user's username");
            opt.addOption("on", "old-name", true, "Used with -chu (--change-username): old username to change from");
            opt.addOption("nn", "new-name", true, "Used with -chu (--change-username): new username");
            opt.addOption("vp", "validate-password", true, "Validate logging into rabbit with the password from the database");
            opt.addOption("cu", "create-rabbit-user", true, "Create rabbit user if user does not exist");
            opt.addOption("uow", "create-userorg-workspace", true, "Create userorg workspace");
            opt.addOption("fw", "create-folder-workspace", true, "Create folder workspace");
            opt.addOption("rfw", "create-root-folder-workspace", true, "Create folder workspace");
            opt.addOption("su", "sync-two-databases", true, "Sync two different user tables");
            opt.addOption("lu", "list users", true, "List users that do not exist");
            opt.addOption("cws", "copy workspaceid", true, "Workspaceid");
            opt.addOption("iws", "insert workspaceid", true, "Workspaceid");
            opt.addOption("jasypt", false,"Encrypt or decrypt passwords for use with OpenAM");
            opt.addOption("eord", true, "used with -jasypt: 'e' or 'd' for encrypt or decrypt");
            opt.addOption("pass", true, "used with -jasypt: the input to encrypt");
            opt.addOption("sso", false, "SSO Usermanagement Actions");
            opt.addOption("setstatus", true, "used with -sso: 'e' or 'd' for enable or disable SSO user");
            opt.addOption("wldapi", "write-ldap-info", false, "Write ldif file with user info from NICS");
            opt.addOption("user", true, "Used with -wldapi: the username");
            opt.addOption("password", true, "Used with -wldapi: the password");
            opt.addOption("first", true, "Used with -wldapi: User's first name");
            opt.addOption("last", true, "Used with -wldapi: User's last name");
            opt.addOption("proppath", "property-path", true, "Specify property path for SSO properties");
            //opt.getOption("user").setOptionalArg(true);
            opt.addOption("setrp", "set-rabbit-pass", false, "Set every user's rabbit account to the specified password");
            opt.addOption("p", true, "used with -setrp, specify the password to set");
            opt.addOption("copy", "copy-userorg", true, "Copy User Orgs from one user to another");
            opt.addOption("fUser", "from-user", true, "From User");
            opt.addOption("tUser", "to user", true, "To User");
            opt.addOption("arcgis", "argis", true, "ArcGis layers");
            opt.addOption("url", "url", true, "url");
            opt.addOption("folderName", "folder-name", true, "Folder");
            opt.addOption("parentFolderId", "parent-folder-id", true, "Parent Folder");
            opt.addOption("phiP", "phinics-password", true, "Password");
            opt.addOption("apache", "add-to-appache", true, "Add to Phi_apache table");

            GnuParser parser = new GnuParser();
            CommandLine cl = parser.parse(opt, args);            
            
            if (cl.hasOption("help")) {
                HelpFormatter f = new HelpFormatter();
                f.printHelp("-h -db -dbu -dbp [-c -u -p [-en -fn -ln]] [-o -org -u -r [-en]] [-con -u -ctype -cval] [-gp] [-dlw] [-fw] [-uow] [-su] [-cws] [-iws] [-copy -fUser -tUser] [-jasypt] [-wldapi] [-setrp -pass]", opt);
            }
            else {

                if (cl.hasOption('c')) {
                    if (!(cl.hasOption("u") && cl.hasOption("p") && cl.hasOption("n") && cl.hasOption("ck"))) {
                        System.out.println("create-user command requires -u, -p, -n, and -ck values");
                        System.exit(1);
                    }
                    else{
                        // create user
                        CreateUser create = new CreateUser(cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                        create.setRabbitCookie(cl.getOptionValue("ck"));
                        create.setRabbitNode(cl.getOptionValue("n"));
                        boolean res = create.createUserEntry(cl.getOptionValue("u"), cl.getOptionValue("p"), cl.hasOption("en"), cl.getOptionValue("fn"), cl.getOptionValue("ln"), cl.getOptionValue("dt"));
                        if(res){
                            System.out.println("created " + cl.getOptionValue("u"));
                        }
                        else{
                            System.out.println("could not create " + cl.getOptionValue("u"));
                            System.exit(1);
                        }
                    }

                }
                else if (cl.hasOption("o")){
                    if(!(cl.hasOption("org") && cl.hasOption('u') && cl.hasOption('r'))){
                        System.out.println("user-org command requires -org -u and -r values");
                        System.exit(1);
                    }
                    else{
                        //register user with org
                        CreateUserOrg create = new CreateUserOrg(cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                        boolean res = create.createUserOrgEntry(cl.getOptionValue('u'), cl.getOptionValue("org"), cl.getOptionValue('r'), cl.hasOption("en"), cl.getOptionValue("rank"), cl.getOptionValue("unit"));
                        if(res){
                            System.out.println("registered " + cl.getOptionValue('u') + " with org " + cl.getOptionValue("org"));
                        }
                        else{
                            System.out.println("could not register " + cl.getOptionValue('u') + " with org " + cl.getOptionValue("org"));
                            System.exit(1);
                        }
                    }
                }
                else if (cl.hasOption("con")){
                    if(!(cl.hasOption("u") && cl.hasOption("ctype") && cl.hasOption("cval"))){
                        System.out.println("create-contact command requires -u -ctype and -cval values");
                        System.exit(1);
                    }
                    else{
                        // register contact info
                        CreateContact create = new CreateContact(cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                        boolean res = create.createContactEntry(cl.getOptionValue('u'), cl.getOptionValue("ctype"), cl.getOptionValue("cval"));
                        if(res){
                            System.out.println("added contact info for " + cl.getOptionValue('u'));
                        }
                        else{
                            System.out.println("could not add contact info for " + cl.getOptionValue('u'));
                            System.exit(1);
                        }
                    }
                }
                else if (cl.hasOption("gp")){
                    RetrievePassword retrieve = new RetrievePassword(cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                    String password = retrieve.findPasswordByUserName(cl.getOptionValue("gp"));
                    if(password != null){
                        System.out.println(password);
                    }
                    else{
                        System.out.print("nothing found for " + cl.getOptionValue("gp"));
                        System.exit(1);
                    }
                }
                else if (cl.hasOption("lu")){
                    RetrievePassword retrieve = new RetrievePassword(cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                    boolean exists = retrieve.locateUser(cl.getOptionValue("lu"));
                    if(!exists){
                    	System.out.println(cl.getOptionValue("lu"));
                    }
                }
                else if (cl.hasOption("vp")){
                	//String password, String host, String database, String rabbitNode, String cookie
                    ValidatePassword validate = new ValidatePassword(
                    		cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"),
                    		cl.getOptionValue("n"), cl.getOptionValue("ck"));
                    validate.validateRabbitLogin(cl.getOptionValue("vp"), cl.getOptionValue("cu"));
                    System.exit(1);
                }
                else if (cl.hasOption("chu")){
                	ChangeUsername change = new ChangeUsername(cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                	change.setRabbitCookie(cl.getOptionValue("ck"));
                    change.setRabbitNode(cl.getOptionValue("n"));
                    boolean res = change.changeUsername(cl.getOptionValue("on"), cl.getOptionValue("nn"));
                    if(res){
                    	System.out.println("changed username for " + cl.getOptionValue("on") + " to " + cl.getOptionValue("nn"));
                    }
                    else {
                    	System.out.println("couldn't change username for " + cl.getOptionValue("on"));
                    }
                }else if (cl.hasOption("fw") && cl.hasOption("cws") && cl.hasOption("iws")){
                	// datalayer workspace
                    CreateFolderWorkspace create = 
                    		new CreateFolderWorkspace(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"),
                    				cl.getOptionValue("cws"), cl.getOptionValue("iws"));
                    boolean res = create.createFolderWorkspace();
                    if(res){
                        System.out.println("created folder workspace");
                    }
                    else{
                        System.out.println("could not create " + cl.getOptionValue("u"));
                        System.exit(1);
                    }
                }else if (cl.hasOption("rfw") && cl.hasOption("cws") && cl.hasOption("iws")){
                	// datalayer workspace
                    CreateFolderWorkspace create = 
                    		new CreateFolderWorkspace(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"),
                    				cl.getOptionValue("cws"), cl.getOptionValue("iws"));
                    boolean res = create.createRootFolderWorkspace();
                    if(res){
                        System.out.println("created root folder workspace");
                    }
                    else{
                        System.out.println("could not create " + cl.getOptionValue("u"));
                        System.exit(1);
                    }
                }else if (cl.hasOption("uow")){
                	// userorg workspace
                    CreateUserOrgWorkspace create = 
                    		new CreateUserOrgWorkspace(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                    boolean res = create.createUserOrgWorkspace();
                    if(res){
                        System.out.println("created userorg workspace");
                    }
                    else{
                        System.out.println("could not create " + cl.getOptionValue("u"));
                        System.exit(1);
                    }
                }else if (cl.hasOption("su")){
                	// userorg workspace
                    SyncUsers sync = 
                    		new SyncUsers(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue("h"), cl.getOptionValue("db"),
                    				cl.getOptionValue("dbu2"), cl.getOptionValue("dbp2"), cl.getOptionValue("h2"), cl.getOptionValue("db2"));
                    sync.syncDatabases();
                } else if(cl.hasOption("jasypt")) {
                	String which = cl.getOptionValue("eord");
                	String pass = cl.getOptionValue("pass");
                	String resultVal = "";
                	boolean valid = true;
                	if(which.equals("e")) {
                		resultVal = OpenLDAPHelper.encrypt(pass);
                	} else if(which.equals("d")) {
                		resultVal = OpenLDAPHelper.decrypt(pass);
                	} else {
                		log.warn("Unsupported eord parameter: " + which);
                		valid = false;
                	}
                	
                	if(valid) {
                		log.info(((which.equals("e")) ? "En" : "De") + "crypted password: " + resultVal + "\n\n");
                	}
                	
                	System.exit(0);
                	
                } else if(cl.hasOption("wldapi")) {
                	
                	String propPath = cl.getOptionValue("proppath");
                	log.info("Got param proppath: " + propPath);
                	System.setProperty("ssoToolsPropertyPath", propPath);
                	System.setProperty("openamPropertiesPath", propPath);
                	
                	log.info((cl.getOptionValue("user") != null && !cl.getOptionValue("user").isEmpty()) ? 
                			("User specified: " + cl.getOptionValue("user")) : "No user specified");
                	
                	OpenLDAPHelper ldapHelper = new OpenLDAPHelper(
                			cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue("h"), 
                			cl.getOptionValue("db"), true, true, cl.getOptionValue("user")); // TODO: make last two options
                	
                	//log.info(ldapHelper.writeLDIFFile());
                	log.info("Token success: " + ldapHelper.createUser(cl.getOptionValue("user"),                			
                			cl.getOptionValue("first"), cl.getOptionValue("last"),cl.getOptionValue("password")));
                	System.exit(0);
                	
                } else if(cl.hasOption("sso")) {
                	String propPath = cl.getOptionValue("proppath");
                	log.info("Got param proppath: " + propPath);
                	System.setProperty("ssoToolsPropertyPath", propPath);
                	System.setProperty("openamPropertiesPath", propPath);
                	
                	String user = cl.getOptionValue("user");
                	if(user == null || user.isEmpty()) {
                		log.info("Must pass in valid user/email address!");
                		System.exit(1);
                	}
                	
                	String setStatus = cl.getOptionValue("setstatus");
                	boolean status = false; 
                	if(setStatus.toLowerCase().equals("e")) {
                		status = true;
                	} else if(setStatus.toLowerCase().equals("d")) {
                		status = false;
                	} else {
                		// Unknown setstatus parameter, exit
                		log.info("Passed in unknown parameter to -sso -setstatus: " + setStatus + ". Must be 'e' or 'd'\n");
                		System.exit(1);
                	}
                	
                	OpenLDAPHelper ldapHelper = new OpenLDAPHelper();
                	log.info("SSO Set User Status returned with: " + ldapHelper.setUserStatus(user, status));
                	System.exit(0);
                	
                } else if(cl.hasOption("setrp")) {
                	
                	String username = cl.getOptionValue("setrp");
                	String pass = cl.getOptionValue("pass");
                	
                	boolean hasUser = false;
                	if(username == null || username.isEmpty()) {
                		//log.info("\n-setrp must specify a user: -setrp user@email.com\n");
                		//System.exit(1);
                	} else {
                		hasUser = true;
                	}
                	
                	if(pass == null || pass.isEmpty()) {
                		log.info("\n-setrp requires a valid -pass parameter!\n");
                		System.exit(1);
                	}
                	
                    ValidatePassword validate = new ValidatePassword(
                    		cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"),
                    		cl.getOptionValue("n"), cl.getOptionValue("ck"));
                    
                    // TODO:SOO check for option for syncing ALL or just sycning a user
                    if(hasUser) {
                    	log.info("Setting specified user's rabbit password to the specified password...");
                    	validate.syncToInstancePassword(username, cl.getOptionValue("pass"), cl.getOptionValue("cu"));                    	
                    } else {
                    	log.info("Setting all users rabbit passwords to the specified password...");
                    	validate.syncAllRabbitUsersToInstancePassword(cl.getOptionValue("pass"), cl.getOptionValue("cu"));
                    }
                    
                    
                    System.exit(0);
                	
                }else if (cl.hasOption("copy")){
                	//copy user organizations
                    CopyUserOrg copyUserOrg = 
                    		new CopyUserOrg(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                    boolean res = copyUserOrg.copyFrom(cl.getOptionValue("fUser"), cl.getOptionValue("tUser"));
                    if(res){
                        System.out.println("userorgs were copied from " + cl.getOptionValue("fUser") + "'s user account to " + cl.getOptionValue("tUser"));
                    }
                    else{
                        System.out.println("could not create " + cl.getOptionValue("u"));
                        System.exit(1);
                    }
                }else if (cl.hasOption("arcgis")){
                    AddArcGISLayers addLayers = 
                    		new AddArcGISLayers(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                    boolean res = addLayers.createLayers(cl.getOptionValue("url"), cl.getOptionValue("folderName"), cl.getOptionValue("parentFolderId"));
                    if(res){
                        System.out.println("Datalayers were add for " + cl.getOptionValue("aUrl"));
                    }
                    else{
                        System.out.println("Could not create datalayers for " + cl.getOptionValue("aUrl"));
                        System.exit(1);
                    }
                }else if (cl.hasOption("apache")){
                    AddUserToPhiApache apache = 
                    		new AddUserToPhiApache(
                    				cl.getOptionValue("dbu"), cl.getOptionValue("dbp"), cl.getOptionValue('h'), cl.getOptionValue("db"));
                    boolean res = apache.addUsers();
                    if(res){
                    	System.out.println("Users have been added to the phi_apache table.");
                    }else{
                    	System.out.println("There were issues adding users to the phi_apache table");
                    }
                }

            }
        } catch (Exception e) {
            log.error("error completing action", e);
            System.exit(1);
        }
    }

}
