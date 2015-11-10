/**
 * Copyright (c) 2008-2015, Massachusetts Institute of Technology (MIT)
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
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.mit.ll.nics.common.ws.client.JSONRequest;

/**
 *
 */
public class AddArcGISLayers extends UserManagementAction {
    
    /**
     * @param username
     * @param password
     * @param host
     * @param database
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public AddArcGISLayers(String username, String password, String host,
            String database) throws SQLException, ClassNotFoundException,
            NoSuchAlgorithmException {
        super(username, password, host, database);
    }
    
    public boolean createLayers(String url, String folderName, String parentFolderId)
            throws SQLException {
    	try{
	    	JSONRequest request = new JSONRequest();
	    	
	    	String response = (String) request.getRequest(url + "?f=json&pretty=true");
	    	
	    	JSONObject json = new JSONObject(response);
			
			JSONArray layers = (JSONArray) json.get("layers");
			
			if(layers.length() > 0){

		    	String datasourceId = String.valueOf(UUID.randomUUID());
		    	
		    	String folderId = this.createFolderEntry(folderName, parentFolderId);
				if(folderId != null){
					System.out.println("Creating datasource...");
			    	if(createDatasourceEntry(datasourceId, url)){
	
			    		for(int i=0; i<layers.length(); i++){
			    			JSONObject layer = (JSONObject) layers.get(i);
			    			String datalayerSourceId = String.valueOf(UUID.randomUUID());
			    			String datalayerId = String.valueOf(UUID.randomUUID());
			    			
			    			//Build Layername
			    			StringBuffer layername = new StringBuffer();;
			    			int parentId = layer.getInt("parentLayerId");
			    			String layerId = new Integer(layer.getInt("id")).toString();
			    			
			    			//Check to see if sublayers exist
			    			JSONArray subLayers = null;
			    			try{
			    				subLayers = (JSONArray) layer.get("subLayerIds");
			    			}catch(Exception e){}
			    			
			    			if(parentId != -1){
			    				layername.append("show:");
			    				layername.append(layerId);
			    			}else if(subLayers != null){
			    				layername.append("show:");
			    				layername.append(layerId);
			    				//Don't think you need to add each sublayer
			    				/*for(int j=0; j<subLayers.length(); j++){
			    					layername.append(",");
			    					layername.append(new Integer(subLayers.getInt(j)).toString());
			    				}*/
			    			}else{
			    				layername.append(layerId);
			    			}
			    			
			    			String displayName = layer.getString("name");
			    			
			    			System.out.println("Creating datalayersource...");
			    			if(this.createDatalayersourceEntry(datalayerSourceId, datasourceId, layername.toString())){
						    	System.out.println("Creating datalayer..");
			    				if(this.createDatalayerEntry(datalayerId, datalayerSourceId, displayName)){
			    					System.out.println("Creating datlayerfolder...");
			    					this.createDatalayerFolderEntry(datalayerId, folderId, i);
				    			}
			    			}
			    			
						}
			    	}else{
			    		log.error("There was an error creating the folder");
			    	}
			    }else{
			    	log.error("No datalayers were returned.");
			    }
			}
		}catch(Exception e){
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }
    
    
    /**
     * @throws SQLException
     */
    private boolean createDatasourceEntry(String id, String url)
            throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement("insert into datasource(datasourceid, internalurl, "
                       + "datasourcetypeid) "
                       + "values (?,?,?)");
        ps.setString(1, id);
        ps.setString(2, url);
        ps.setInt(3, 7);
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("There was an error adding the datasource.", res);
            return false;
        }
       return true;
    }
    
    /**
     * @throws SQLException
     */
    private boolean createDatalayersourceEntry(String id, String datasourceId, String layername)
            throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement("insert into datalayersource ("
                       + "datalayersourceid, created, datasourceid, layername, nativeprojection, "
                       + "refreshrate, usersessionid) "
                       + "values (?,now(),?,?,'EPSG:3857',0,250)");
        ps.setString(1, id);
        ps.setString(2, datasourceId);
        ps.setString(3, layername);
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("There was an error adding the datalayersource.", res);
            return false;
        }
       return true;
    }
    
    /**
     * @throws SQLException
     */
    private boolean createDatalayerEntry(String id, String datalayersourceId, String displayName)
            throws SQLException {
        PreparedStatement ps =
                conn.prepareStatement("insert into datalayer (datalayerid, baselayer, "
                       + "created, datalayersourceid, displayname, globalview, usersessionid) "
                       + "values (?,'f',now(),?,?,'f',250)");
        ps.setString(1, id);
        ps.setString(2, datalayersourceId);
        ps.setString(3, displayName);
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("There was an error adding the datalayer.", res);
            return false;
        }
       return true;
    }
    
    /**
     * @throws SQLException
     */
    private String createFolderEntry(String folderName, String parentFolderId)
            throws SQLException {
    
    	String folderId = String.valueOf(UUID.randomUUID());
    	
    	int index = 0;
    	PreparedStatement maxIndex = 
                conn.prepareStatement("select max(index) from folder where parentfolderid=?");
    	maxIndex.setString(1, parentFolderId);
    	
    	ResultSet rs = maxIndex.executeQuery();
    	if(rs.next()){
    		index = rs.getInt("max") + 1;
    	}
    	
    	int workspaceid = 1;
    	PreparedStatement ws = 
                conn.prepareStatement("select workspaceid from folder where folderid=?");
    	ws.setString(1, parentFolderId);
    	
    	ResultSet rs2 = ws.executeQuery();
    	if(rs2.next()){
    		workspaceid = rs2.getInt("workspaceid");
    	}
    	
        PreparedStatement ps =
                conn.prepareStatement("insert into folder ("
                       + "folderid, foldername, parentfolderid, index, workspaceid) "
                       + "values (?,?,?,?,?)");
        ps.setString(1, folderId);
        ps.setString(2, folderName);
        ps.setString(3, parentFolderId);
        ps.setInt(4, index);
        ps.setInt(5, workspaceid);
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("There was an error adding the datalayerfolder.", res);
            return null;
        }
       return folderId;
    }
    
    /**
     * @throws SQLException
     */
    private boolean createDatalayerFolderEntry(String datalayerId, String folderId, int index)
            throws SQLException {
    	
    	PreparedStatement ps =
                conn.prepareStatement("insert into datalayerfolder ("
                       + "datalayerfolderid, datalayerid, folderid, index) "
                       + "values (nextval('hibernate_sequence'),?,?,?)");
        ps.setString(1, datalayerId);
        ps.setString(2, folderId);
        ps.setInt(3, index);
        int res = ps.executeUpdate();
        if (res != 1) {
            log.error("There was an error adding the datalayerfolder.", res);
            return false;
        }
       return true;
    }
}
