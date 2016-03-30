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
package edu.mit.ll.soa.test.openam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;

import edu.mit.ll.soa.sso.exception.AuthException;
import edu.mit.ll.soa.sso.exception.InitializationException;
import edu.mit.ll.soa.sso.util.OpenAmInitUtil;
import edu.mit.ll.soa.sso.util.OpenAmUtilConstants;
import edu.mit.ll.soa.sso.util.OpenAmUtils;
import edu.mit.ll.soa.util.OpenAmClient;

/**
	 * Created with IntelliJ IDEA.
	 * User: cbudny
	 * Date: 7/10/14
	 * Time: 12:27 PM
	 * To change this template use File | Settings | File Templates.
	 */
	public class LoginTest
	{

		private String loginIndexName;
		    private String orgName;
		    private String locale;

		    private LoginTest(String loginIndexName, String orgName) {
		        this.loginIndexName = loginIndexName;
		        this.orgName = orgName;
		    }

		    private LoginTest(String loginIndexName, String orgName, String locale) {
		        this.loginIndexName = loginIndexName;
		        this.orgName = orgName;
		        this.locale = locale;
		    }

		    protected AuthContext getAuthContext()
		        throws AuthLoginException {
		        AuthContext lc = new AuthContext(orgName);
		        AuthContext.IndexType indexType = AuthContext.IndexType.MODULE_INSTANCE;
		        if (locale == null || locale.length() == 0) {
		            lc.login(indexType, loginIndexName);
		        } else {
		            lc.login(indexType, loginIndexName, locale);
		        }
		        debugMessage(loginIndexName + ": Obtained login context");
		        return lc;
		    }

		    private void addLoginCallbackMessage(Callback[] callbacks)
		    throws UnsupportedCallbackException
		    {
		        int i = 0;
		        try {
		            for (i = 0; i < callbacks.length; i++) {
		                if (callbacks[i] instanceof TextOutputCallback) {
		                    handleTextOutputCallback((TextOutputCallback)callbacks[i]);
		                } else if (callbacks[i] instanceof NameCallback) {
		                    handleNameCallback((NameCallback)callbacks[i]);
		                } else if (callbacks[i] instanceof PasswordCallback) {
		                    handlePasswordCallback((PasswordCallback)callbacks[i]);
		                } else if (callbacks[i] instanceof TextInputCallback) {
		                    handleTextInputCallback((TextInputCallback)callbacks[i]);
		                } else if (callbacks[i] instanceof ChoiceCallback) {
		                    handleChoiceCallback((ChoiceCallback)callbacks[i]);
		                }
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		            throw new UnsupportedCallbackException(callbacks[i],e.getMessage());
		        }
		    }

		    private void handleTextOutputCallback(TextOutputCallback toc) {
		        debugMessage("Got TextOutputCallback");
		        // display the message according to the specified type

		        switch (toc.getMessageType()) {
		            case TextOutputCallback.INFORMATION:
		                debugMessage(toc.getMessage());
		                break;
		            case TextOutputCallback.ERROR:
		                debugMessage("ERROR: " + toc.getMessage());
		                break;
		            case TextOutputCallback.WARNING:
		                debugMessage("WARNING: " + toc.getMessage());
		                break;
		            default:
		                debugMessage("Unsupported message type: " +
		                    toc.getMessageType());
		        }
		    }

		    private void handleNameCallback(NameCallback nc)
		        throws IOException {
		        // prompt the user for a username
		        System.out.print(nc.getPrompt());
		        System.out.flush();
		        nc.setName((new BufferedReader
		            (new InputStreamReader(System.in))).readLine());
		    }

		    private void handleTextInputCallback(TextInputCallback tic)
		        throws IOException {
		        // prompt for text input
		        System.out.print(tic.getPrompt());
		        System.out.flush();
		        tic.setText((new BufferedReader
		            (new InputStreamReader(System.in))).readLine());
		    }

		    private void handlePasswordCallback(PasswordCallback pc)
		        throws IOException {
		        // prompt the user for sensitive information
		        System.out.print(pc.getPrompt());
		        System.out.flush();
		        String passwd = (new BufferedReader(new InputStreamReader(System.in))).
		            readLine();
		        pc.setPassword(passwd.toCharArray());
		    }

		    private void handleChoiceCallback(ChoiceCallback cc)
		        throws IOException {
		        // ignore the provided defaultValue
		        System.out.print(cc.getPrompt());

		        String[] strChoices = cc.getChoices();
		        for (int j = 0; j < strChoices.length; j++) {
		            System.out.print("choice[" + j + "] : " + strChoices[j]);
		        }
		        System.out.flush();
		        cc.setSelectedIndex(Integer.parseInt((new BufferedReader
		            (new InputStreamReader(System.in))).readLine()));
		    }

		    protected boolean login(AuthContext lc)
		        throws UnsupportedCallbackException {
		        boolean succeed = false;
		        Callback[] callbacks = null;

		        // get information requested from module
		        while (lc.hasMoreRequirements()) {
		            callbacks = lc.getRequirements();
		            if (callbacks != null) {
		                addLoginCallbackMessage(callbacks);
		                lc.submitRequirements(callbacks);
		            }
		        }

		        if (lc.getStatus() == AuthContext.Status.SUCCESS) {
		            System.out.println("Login succeeded.");
		            succeed = true;
		        } else if (lc.getStatus() == AuthContext.Status.FAILED) {
		            System.out.println("Login failed.");
		        } else {
		            System.out.println("Unknown status: " + lc.getStatus());
		        }

		        return succeed;
		    }

		    protected void logout(AuthContext lc)
		        throws AuthLoginException {
		        lc.logout();
		        System.out.println("Logged Out!!");
		    }

		    static void debugMessage(String msg) {
		        System.out.println(msg);
		    }

		    public static void main(String[] args) {
		        String token = null;
			    OpenAmUtils test = null;
			    try {
		            System.out.print("Realm (e.g. /): ");
		            String orgName = (new BufferedReader(
		                new InputStreamReader(System.in))).readLine();

		            System.out.print("Login username: ");
		            String username = (new BufferedReader(
		                new InputStreamReader(System.in))).readLine();

		            System.out.print("Password: ");
		            String password = (new BufferedReader(
		                new InputStreamReader(System.in))).readLine();

			        System.out.print("Protocol (e.g. http): ");
                        String protocol = (new BufferedReader(
                            new InputStreamReader(System.in))).readLine();


			        System.out.print("host FQDN: ");
	                    String host = (new BufferedReader(
	                        new InputStreamReader(System.in))).readLine();

			        System.out.print("port: (e.g. 80, 8080): ");
                        String port = (new BufferedReader(
                            new InputStreamReader(System.in))).readLine();

			        System.out.print("Deployment Path (e.g. openam): ");
                        String path = (new BufferedReader(
                            new InputStreamReader(System.in))).readLine();
                        
                    System.out.print("AuthIndex (e.g. DataStore): ");
                    	String authIndex = (new BufferedReader(
                    			new InputStreamReader(System.in))).readLine();
                    System.setProperty("default.store", authIndex);
                        
                    System.out.print("Property Path: ");
                        String propPath = (new BufferedReader(
                            new InputStreamReader(System.in))).readLine();
                    System.setProperty("openamPropertiesPath", propPath);
                    
                    /*System.out.println("username? : " + System.getProperty("com.sun.identity.agents.app.username"));
                    System.setProperty("com.sun.identity.agents.app.username", "UrlAccessAgent");
                    
                    System.out.println("password? : " + System.getProperty("com.iplanet.am.service.password"));
                    System.setProperty("com.iplanet.am.service.password", "");
                    
                    System.out.println("secret? : " + System.getProperty("com.iplanet.am.service.secret"));
                    System.setProperty("com.iplanet.am.service.secret", "AQIC24u86rq9RRYRJUcHdMPPMjHu75JuajbZ");
                    */
                        

			        // TODO need to update test
			        Map<String, String> vals = OpenAmInitUtil.createPropertiesMap(protocol, host, port, path);
			        test = new OpenAmUtils(vals);
			        token = test.login(username, password, orgName);
					System.out.println("SSOToken: " + token.toString());
		        } catch (IOException e) {
		            e.printStackTrace();
		        } catch (AuthException ae)
		        {
			        ae.printStackTrace();
		        } catch (InitializationException ie)
		        {
			        ie.printStackTrace();
		        }

			    // try to create identity
			    try
			    {
				 	if (test != null)
					{
						System.out.println("Continuing with test");
						if (test.createIdentity("chris1@ll.mit.edu", "chris123", "Chris", "Budny", true))
							System.out.println("Created Identity!");

						System.out.println("Trying to create realm");
						if (test.createOrganization("testRealm", true))
							System.out.println("Created org!");
					}
			    } catch (Exception e)
			    {
				    e.printStackTrace();
			    }



		        System.exit(0);
		    }

	}
