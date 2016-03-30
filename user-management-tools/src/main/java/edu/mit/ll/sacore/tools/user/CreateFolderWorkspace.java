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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class CreateFolderWorkspace extends UserManagementAction {
	
	private Map<String,String> folderMapping = new HashMap<String,String>();
	private int copyWorkspaceid;
	private int insertWorkspaceid;
	
	/**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public CreateFolderWorkspace(String username, String password, String host,
            String database, String copyWorkspace, String insertWorkspace) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        super(username, password, host, database);
        
        this.copyWorkspaceid = new Integer(copyWorkspace);
        this.insertWorkspaceid = new Integer(insertWorkspace);
    }
    
    private void populateRootFolders(){
    	try{
	    	PreparedStatement ps =
	                conn
	                        .prepareStatement("select folderid from rootfolder;");
	    	
	    	ResultSet rs = ps.executeQuery();
	    	while (rs.next()) {
	    		String folderid = rs.getString("folderid");
	    		folderMapping.put(folderid, folderid);
	    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    public boolean createRootFolderWorkspace(){
    	try{
	    	PreparedStatement ps =
	                conn
	                        .prepareStatement("select * from folder join rootfolder using(folderid) where rootfolder.workspaceid=?;");
	    	ps.setInt(1, this.copyWorkspaceid);
	    	
	    	ResultSet rs = ps.executeQuery();
	    	while (rs.next()) {
	    		String folderid = rs.getString("folderid");
	    		String foldername = rs.getString("foldername");
	    		String newfolderid = String.valueOf(UUID.randomUUID());
	    		
	    		PreparedStatement folder_insert =
	                    conn.prepareStatement("insert into folder ("
	                    		+ "folderid, foldername, workspaceid) "
	                           + "values (?, ?, ?);");
	            folder_insert.setString(1, newfolderid);
	            folder_insert.setString(2, foldername);
	            folder_insert.setInt(3, this.insertWorkspaceid);
	            folder_insert.executeUpdate();
	            
	    		//insert into rootfolder
	            PreparedStatement root_folder_insert =
	                    conn.prepareStatement("insert into rootfolder ("
	                    		+ "rootid, folderid, tabname, workspaceid) "
	                           + "values ((select nextval('hibernate_sequence')), ?, ?, ?);");
	            root_folder_insert.setString(1, newfolderid);
	            root_folder_insert.setString(2, foldername);
	            root_folder_insert.setInt(3, this.insertWorkspaceid);
	            root_folder_insert.executeUpdate();
	            
	            this.createDatalayerFolder(folderid, newfolderid);
	    		
	    	}
	    }catch(Exception e){
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }
    
    
    /**
     * 
     * @throws SQLException
     */
    public boolean createFolderWorkspace()
            throws SQLException {
    	
    	//ADD ALL ROOT FOLDERS TO MAPPING SO WE DON'T DUPLICATE
    	this.populateRootFolders();
    	
    	PreparedStatement ps =
                conn.prepareStatement("select * from folder where workspaceid=?");
        ps.setInt(1, this.copyWorkspaceid);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
        	try{
        		String folderid = rs.getString("folderid");
	            String parentfolderid = rs.getString("parentfolderid");
	            String foldername = rs.getString("foldername"); 
	            int index = rs.getInt("index");
	            
	            this.createFolder(folderid, parentfolderid, foldername, index);
	        
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }
    	
        return true;
    }
    
    private String createFolder(String folderid, String parentfolderid, String foldername, int index){ //original values
    	if(folderMapping.get(folderid) == null){
	    	String newfolderid = null;
	    	String newparentid = null;
	    	
	    	try{
	    		if(parentfolderid != null){
	    			//if parent doesn't exist create it first
		    		if(folderMapping.get(parentfolderid) == null){
			         	ResultSet folder = this.getFolder(parentfolderid);
			         	if(folder != null && folder.next()){
			         		newparentid = 
			         				this.createFolder(folder.getString("folderid"), folder.getString("parentfolderid"), folder.getString("foldername"), 
			         						folder.getInt("index"));
			         	}
			        }else{
			        	newparentid = folderMapping.get(parentfolderid);
			        }
	    		}
	    		
	    		if((parentfolderid != null && newparentid != null) || parentfolderid == null){
		    		newfolderid = String.valueOf(UUID.randomUUID());
		    		PreparedStatement ps =
		                    conn.prepareStatement("insert into folder ("
		                    		+ "folderid, parentfolderid, foldername, index, workspaceid) "
		                           + "values (?, ?, ?, ?, ?);");
		            ps.setString(1, newfolderid);
		            ps.setString(2, newparentid);
		            ps.setString(3, foldername);
		            ps.setInt(4, index);
		            ps.setInt(5, this.insertWorkspaceid);
		            ps.executeUpdate();
		            
		            this.createDatalayerFolder(folderid, newfolderid);
		            
		            folderMapping.put(folderid, newfolderid);
	    		}
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
	    	return newfolderid;
    	}else{
    		return folderMapping.get("folderid");
    	}
    }
    
    private ResultSet getFolder(String folderid){
    	try{
	    	PreparedStatement ps =
	                conn.prepareStatement("select * from folder where folderid=?");
	        ps.setString(1, folderid);
	        
	        return ps.executeQuery();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return null;
    }
    
    private void createDatalayerFolder(String folderid, String newfolderid){
    	try{
	    	//Add Datalayerfolder
	        PreparedStatement ps =
	                conn
	                        .prepareStatement("select datalayerid,index from datalayerfolder where folderid=?");
	        ps.setString(1, folderid);
	        
	        ResultSet rs = ps.executeQuery();
	        
	        while(rs.next()){
		        PreparedStatement ps_insert =
		                conn.prepareStatement("insert into datalayerfolder ("
		                		+ "datalayerfolderid, folderid, datalayerid, index) "
		                       + "values ((select nextval('hibernate_sequence')), ?, ?, ?);");
		        ps_insert.setString(1, newfolderid);
		        ps_insert.setString(2, rs.getString("datalayerid"));
		        ps_insert.setInt(3, rs.getInt("index"));
		        ps_insert.executeUpdate();
	        }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
}