/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - trek global													   *
 * - hengsin                         								   *
 **********************************************************************/
package com.trekglobal.gapi.client.process;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.base.IGridTabImporter;
import org.adempiere.base.equinox.EquinoxExtensionLocator;
import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridTable;
import org.compiere.model.GridWindow;
import org.compiere.model.MAuthorizationAccount;
import org.compiere.model.MLookup;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CopySheetToAnotherSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;

/**
 * 
 * @author hengsin
 *
 */
@SuppressWarnings("deprecation")
public class ImportDataFromGoogleSheet extends SvrProcess implements DataStatusListener {
	
	private int p_AD_Window_ID;
	private int p_AD_Tab_ID;
	private String p_Charset;
	private String p_ImportMode;
	private String p_URL;
	private int p_AD_User_ID;
	private int p_AD_AuthorizationAccount_ID;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("AD_Window_ID"))
				p_AD_Window_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_Tab_ID"))
				p_AD_Tab_ID = para[i].getParameterAsInt();
			else if (name.equals("Charset"))
				p_Charset = (String) para[i].getParameter();
			else if (name.equals("ImportMode"))
				p_ImportMode = (String) para[i].getParameter();
			else if (name.equals("URL"))
				p_URL = (String) para[i].getParameter();
			else if (name.equals("AD_User_ID"))
				p_AD_User_ID = para[i].getParameterAsInt();
			else if (name.equals(MAuthorizationAccount.COLUMNNAME_AD_AuthorizationAccount_ID))
				p_AD_AuthorizationAccount_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}
	
	GridWindow m_gridWindow = null;
	GridTab m_gridTab = null;
	List<GridTab> m_Childs = null;

	@Override
	protected String doIt() throws Exception {
		try {
			initGridTab();
			IGridTabImporter csvImport = initImporter();
			importFile(csvImport, m_gridTab, m_Childs);
		} finally {
			Env.clearWinContext(-1);
		}

		return "@OK@";
	}
	
	protected void initGridTab() throws Exception {
		m_gridWindow = GridWindow.get(getCtx(), -1, p_AD_Window_ID);
		Env.setContext(getCtx(), -1, "IsSOTrx", m_gridWindow.isSOTrx());
		m_Childs = new ArrayList<GridTab>();
		for (int i = 0; i < m_gridWindow.getTabCount(); i++) {
			GridTab gridtab = m_gridWindow.getTab(i);
			if (!gridtab.isLoadComplete())
				m_gridWindow.initTab(i);
			if (m_gridWindow.getTab(i).getAD_Tab_ID() == p_AD_Tab_ID) {
				m_gridTab  = m_gridWindow.getTab(i);
			} else {
				if (m_gridTab != null && gridtab.getTabLevel() > m_gridTab.getTabLevel())
					m_Childs.add(gridtab);
			}
		}
		
		if (m_gridTab == null)
			throw new Exception("No Active Tab");
		m_gridTab.addDataStatusListener(this);
		for (GridTab childTab : m_Childs)
			childTab.addDataStatusListener(this);
	}
	
	protected IGridTabImporter initImporter() throws Exception {
		IGridTabImporter csvImport = null;
		List<IGridTabImporter> importerList = EquinoxExtensionLocator.instance().list(IGridTabImporter.class).getExtensions();
		for (IGridTabImporter importer : importerList){
			if ("csv".equals(importer.getFileExtension())) {
				csvImport = importer;
				break;
			}
		}

		if (csvImport == null)
			throw new Exception ("No CSV importer");

		return csvImport;
	}
	
	protected void importFile(IGridTabImporter csvImporter, GridTab activeTab, List<GridTab> childTabs) throws Exception {
		MAuthorizationAccount account = new MAuthorizationAccount(getCtx(), p_AD_AuthorizationAccount_ID, get_TrxName());
		if (account.getAD_User_ID() != p_AD_User_ID)
			throw new Exception ("User/Contact does not match the User/Contact of the User Upload Account");
		
		InputStream is = downloadMedia(account);
		Charset charset = Charset.forName(p_Charset);
		java.io.File outFile = csvImporter.fileImport(activeTab, childTabs, is, charset, p_ImportMode, processUI);
		
		if (processUI != null) {
			processUI.download(outFile);
		} else if (getProcessInfo() != null) {
			ProcessInfo m_pi = getProcessInfo();
			m_pi.setExport(true);
			m_pi.setExportFile(outFile);
			m_pi.setExportFileExtension("csv");
		}
		
		is.close();
	}
	
	public InputStream downloadMedia(MAuthorizationAccount account) {
		try {
			account.refresh();
			String accessToken = account.getAccessToken();
			GoogleCredential credential = new GoogleCredential.Builder().setJsonFactory(JacksonFactory.getDefaultInstance()).build();
			credential.setAccessToken(accessToken);
			
			String keywords = "/spreadsheets/d/";
			int index = p_URL.indexOf(keywords);
			if (index == -1)
				throw new IllegalArgumentException("Spreadsheet ID not found");
			String spreadsheetId = p_URL.substring(index + keywords.length());
			if (spreadsheetId.indexOf("/") != -1)
				spreadsheetId = spreadsheetId.substring(0, spreadsheetId.indexOf("/"));
			
			keywords = "#gid=";
			index = p_URL.indexOf(keywords);
			if (index == -1) {
				keywords = "&gid=";
				index = p_URL.indexOf(keywords);
			}
			
			if (!spreadsheetId.matches("([a-zA-Z0-9-_]+)"))
				throw new IllegalArgumentException("Invalid Spreadsheet ID");
			
			if (index == -1) {
				Drive.Builder driveBuilder = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential);
				driveBuilder.setHttpRequestInitializer(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest httpRequest) throws IOException {
						credential.initialize(httpRequest);
			            httpRequest.setConnectTimeout(300 * 60000);  // 300 minutes connect timeout
			            httpRequest.setReadTimeout(300 * 60000);  // 300 minutes read timeout
					}
				});
				Drive drive = driveBuilder.build();
				return drive.files().export(spreadsheetId, "text/csv").executeAsInputStream();
			} else {
				String sheetId = p_URL.substring(index + keywords.length());
				if (sheetId.indexOf("#") != -1)
					sheetId = sheetId.substring(0, sheetId.indexOf("#"));
				if (sheetId.indexOf("&") != -1)
					sheetId = sheetId.substring(0, sheetId.indexOf("&"));
				
				if (!sheetId.matches("([0-9]+)"))
					throw new IllegalArgumentException("Invalid Sheet ID");
				
				Sheets.Builder sheetsBuilder = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential);
				sheetsBuilder.setHttpRequestInitializer(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest httpRequest) throws IOException {
						credential.initialize(httpRequest);
			            httpRequest.setConnectTimeout(300 * 60000);  // 300 minutes connect timeout
			            httpRequest.setReadTimeout(300 * 60000);  // 300 minutes read timeout
					}
				});
				
				Sheets sheets = sheetsBuilder.build();				
				Spreadsheet tmp = sheets.spreadsheets().create(new Spreadsheet()).execute();
				String tmpSpreadsheetId = tmp.getSpreadsheetId();
				Integer tmpSheetId = tmp.getSheets().get(0).getProperties().getSheetId();
				
				CopySheetToAnotherSpreadsheetRequest copyToRequest = new CopySheetToAnotherSpreadsheetRequest();
				copyToRequest.setDestinationSpreadsheetId(tmpSpreadsheetId);
				sheets.spreadsheets().sheets().copyTo(spreadsheetId, Integer.parseInt(sheetId), copyToRequest).execute();
				
				List<Request> requests = new ArrayList<Request>();
				BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest();
				DeleteSheetRequest deleteRequest = new DeleteSheetRequest();
				deleteRequest.setSheetId(tmpSheetId);
				requests.add(new Request().setDeleteSheet(deleteRequest));
				batchUpdateRequest.setRequests(requests);
				sheets.spreadsheets().batchUpdate(tmpSpreadsheetId, batchUpdateRequest).execute();
				
				Drive.Builder driveBuilder = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential);
				driveBuilder.setHttpRequestInitializer(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest httpRequest) throws IOException {
						credential.initialize(httpRequest);
			            httpRequest.setConnectTimeout(300 * 60000);  // 300 minutes connect timeout
			            httpRequest.setReadTimeout(300 * 60000);  // 300 minutes read timeout
					}
				});
				Drive drive = driveBuilder.build();
				InputStream is = drive.files().export(tmpSpreadsheetId, "text/csv").executeAsInputStream();
				
				drive.files().delete(tmpSpreadsheetId).execute();
				tmpSpreadsheetId = null;
				
				return is;
			}	
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}

	@Override
	public void dataStatusChanged(DataStatusEvent e) {
		int col = e.getChangedColumn();
        if (col < 0)
        	return;

        GridTab l_gridTab = null;
        if (e.getSource() != null && e.getSource() instanceof GridTable) {
        	GridTable gt = (GridTable) e.getSource();
        	l_gridTab = m_gridWindow.getTab(gt.getTabNo());
        	if (l_gridTab.getAD_Table_ID() != e.AD_Table_ID)
        		throw new RuntimeException("Table doesn't match with updated tab");
        }
        if (log.isLoggable(Level.CONFIG)) log.config("(" + l_gridTab + ") Col=" + col + ": " + e.toString());

        //  Process Callout
        GridField mField = l_gridTab.getField(col);
        if (mField != null
            && (mField.getCallout().length() > 0
            		|| (Core.findCallout(l_gridTab.getTableName(), mField.getColumnName())).size()>0
            		|| l_gridTab.hasDependants(mField.getColumnName())))
        {
            String msg = l_gridTab.processFieldChange(mField);     //  Dependencies & Callout
            if (msg.length() > 0)
            {
            	log.warning(msg);
            }

            // Refresh the list on dependant fields
    		for (GridField dependentField : l_gridTab.getDependantFields(mField.getColumnName()))
    		{
    			//  if the field has a lookup
    			if (dependentField != null && dependentField.getLookup() instanceof MLookup)
    			{
    				MLookup mLookup = (MLookup)dependentField.getLookup();
    				//  if the lookup is dynamic (i.e. contains this columnName as variable)
    				if (mLookup.getValidation().indexOf("@"+mField.getColumnName()+"@") != -1)
    				{
    					mLookup.refresh();
    				}
    			}
    		}   //  for all dependent fields

        }
	}
}
