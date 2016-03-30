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
package edu.mit.ll.sacore.tools.user;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import edu.mit.ll.nics.common.rabbitmq.admin.RabbitAdmin;
import edu.mit.ll.nics.common.security.encrypt.PasswordEncrypter;
import edu.mit.ll.nics.common.security.hash.SaltedHash;

public class ChangeUsername extends UserManagementAction {

	private SaltedHash hasher;
	private RetrievePassword passwordRetriever;
	
    private String rabbitCookie = "AJDAHNYKRUYOPIHGTHYX";
    private String rabbitNode = "rabbit@localhost";

	public ChangeUsername(String username, String password, String host,
			String database) throws SQLException, ClassNotFoundException, NoSuchAlgorithmException {
		super(username, password, host, database);
		this.passwordRetriever = new RetrievePassword(username, password, host, database);
        this.hasher = new SaltedHash("SHA");
	}
	
	public boolean changeUsername(String oldName, String newName) throws SQLException {
		String pw = passwordRetriever.findPasswordByUserName(oldName);
		Integer userid = findUserIdByName(oldName);
		String hashed = hasher.hash(pw, newName);
				
		PreparedStatement ps = conn.prepareStatement("update \"user\" set passwordhash = ?, username = ? where userid = ?");
		ps.setString(1, hashed);
		ps.setString(2, newName);
		ps.setInt(3, userid);
		int res = ps.executeUpdate();
		if (res != 1) {
            log.error("update of username did not complete properly; return value was {}",
                  res);
            return false;
        }
		
		RabbitAdmin admin = new RabbitAdmin(this.rabbitCookie, this.rabbitNode);
        if(admin.add_user(newName, pw)){
        	if(admin.set_permissions(newName, ".*", ".*", ".*")){
        		if(!admin.delete_user(oldName)){
        			log.warn("could not remove " + oldName + " from rabbit users. try it manually. everything else successful.");
        		}
        		return true;
        	}
        }
        // TODO should probably do some rollback...
		
        return true;
	}
	
	public void setRabbitCookie(String rabbitCookie) {
		this.rabbitCookie = rabbitCookie;
	}

	public void setRabbitNode(String rabbitNode) {
		this.rabbitNode = rabbitNode;
	}

}
