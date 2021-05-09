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
package com.trekglobal.gapi.client.service;

import org.adempiere.base.upload.IUploadHandler;
import org.adempiere.base.upload.IUploadService;
import org.osgi.service.component.annotations.Component;

import com.trekglobal.gapi.client.handler.CSVUploadHandler;
import com.trekglobal.gapi.client.handler.GDriveUploadHandler;
import com.trekglobal.gapi.client.handler.XLSUploadHandler;

/**
 * @author hengsin
 *
 */
@Component(name = "com.trekglobal.gapi.client.service.GAPIUploadService", immediate = true, property = {"provider=Google"}, service = IUploadService.class)
public class GAPIUploadService implements IUploadService {

	private static final String PDF = "application/pdf";
	private static final String EXCEL = "application/vnd.ms-excel";
	private static final String TEXT_CSV = "text/csv";
	private static final String APPLICATION_CSV = "application/csv";
	private static final String EXCEL_OPENXML = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	/**
	 * 
	 */
	public GAPIUploadService() {
	}

	@Override
	public IUploadHandler[] getUploadHandlers(String contentType) {
		if (APPLICATION_CSV.equals(contentType) || TEXT_CSV.equals(contentType)) {
			return new IUploadHandler[] {new CSVUploadHandler()};
		} else if (EXCEL.equals(contentType)) {
			return new IUploadHandler[] {new XLSUploadHandler(), new GDriveUploadHandler(EXCEL)};
		} else if (EXCEL_OPENXML.equals(contentType)) {
			return new IUploadHandler[] {new XLSUploadHandler(), new GDriveUploadHandler(EXCEL_OPENXML)};
		} else if (PDF.equals(contentType)) {
			return new IUploadHandler[] {new GDriveUploadHandler(PDF)};
		} else {
			return new IUploadHandler[] {new GDriveUploadHandler(contentType)};
		}
	}
}
