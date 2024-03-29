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
package com.trekglobal.gapi.client.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.adempiere.base.upload.IUploadHandler;
import org.adempiere.base.upload.UploadMedia;
import org.adempiere.base.upload.UploadResponse;
import org.compiere.model.MAuthorizationAccount;
import org.compiere.util.Env;
import org.compiere.util.Msg;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * @author hengsin
 *
 */
@SuppressWarnings("deprecation")
public class CSVUploadHandler implements IUploadHandler {

	private static final String GOOGLE_SHEET = "application/vnd.google-apps.spreadsheet";

	/**
	 * 
	 */
	public CSVUploadHandler() {
	}

	/* (non-Javadoc)
	 * @see org.adempiere.webui.report.IMediaHandler#getLabel()
	 */
	@Override
	public String getLabel() {
		return Msg.getMsg(Env.getCtx(), "Google_Sheets_Open_With");
	}

	@Override
	public UploadResponse uploadMedia(UploadMedia media, MAuthorizationAccount account) {
		try {
			account.refresh();
			String accessToken = account.getAccessToken();
			GoogleCredential credential = new GoogleCredential.Builder().setJsonFactory(JacksonFactory.getDefaultInstance()).build();
			credential.setAccessToken(accessToken);
			
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
			
			try (InputStream stream = media.getInputStream()) {			
				InputStreamContent mediaContent = new InputStreamContent("text/csv", stream);
				if (media.getContentLength() > 0)
					mediaContent.setLength(media.getContentLength());
				
				String fileName = media.getName();
				String name = fileName.lastIndexOf(".") > 0 ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName ;
				String query = "name = '" + name + "' and mimeType = '" + GOOGLE_SHEET + "'";
				FileList fileList = drive.files().list().setQ(query).execute();
				List<File> list = fileList.getFiles();
				File gfile = null;
				if (list != null && list.size() > 0) {
					gfile = list.get(0);
				}
				
				if (gfile == null) {
					gfile = new File();
					gfile.setName(fileName);
					gfile.setMimeType(GOOGLE_SHEET);			    
					Create create = drive.files().create(gfile, mediaContent);
					MediaHttpUploader uploader = create.getMediaHttpUploader();
					uploader.setDirectUploadEnabled(false);
					uploader.setProgressListener(new FileUploadProgressListener());
					gfile = create.execute();
				} else {
					Update update = drive.files().update(gfile.getId(), new File(), mediaContent);
					MediaHttpUploader uploader = update.getMediaHttpUploader();
					uploader.setDirectUploadEnabled(false);
					uploader.setProgressListener(new FileUploadProgressListener());
					gfile = update.execute();
				}
				String link = "https://docs.google.com/spreadsheets/d/" + gfile.getId();
				UploadResponse response = new UploadResponse(link, Msg.getMsg(Env.getCtx(), "Google_Sheet_Created"));
				return response;
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}
}
