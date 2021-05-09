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
import java.text.NumberFormat;

import org.compiere.util.CLogger;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

/**
 * 
 * @author hengsin
 *
 */
public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {
	
	private static final CLogger log = CLogger.getCLogger(FileUploadProgressListener.class);
	
	@Override
	public void progressChanged(MediaHttpUploader uploader) throws IOException {
		switch (uploader.getUploadState()) {
		case INITIATION_STARTED:
			log.fine("Upload Initiation has started.");
			break;
		case INITIATION_COMPLETE:
			log.fine("Upload Initiation is Complete.");
			break;
		case MEDIA_IN_PROGRESS:
			log.fine("Upload is In Progress: " + NumberFormat.getPercentInstance().format(uploader.getProgress()));
			break;
		case MEDIA_COMPLETE:
			log.fine("Upload is Complete!");
			break;
		default:
			break;
		}
	}
}

