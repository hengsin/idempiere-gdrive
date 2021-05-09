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

import java.util.List;

import org.adempiere.util.Callback;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Window;
import org.compiere.model.MAuthorizationAccount;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Vlayout;

/**
 * @author hengsin
 *
 */
public class WAccountChooser extends Window {

	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -3022705499036390059L;
	private List<MAuthorizationAccount> accounts;
	private Callback<MAuthorizationAccount> callback;

	/**
	 * 
	 * @param accounts
	 * @param callback
	 */
	public WAccountChooser(List<MAuthorizationAccount> accounts, Callback<MAuthorizationAccount> callback) {
		this.accounts = accounts;
		this.callback = callback;
		layout();
	}

	private void layout() {
		Vlayout layout = new Vlayout();
		appendChild(layout);
		layout.setVflex("1");
		layout.setHflex("1");
		Listbox listbox = new Listbox();
		Label label = new Label("Choose Google Account: ");
		label.setVflex("min");
		layout.appendChild(new Separator());
		layout.appendChild(label);
		layout.appendChild(new Separator());
		layout.appendChild(listbox);
		listbox.setHflex("1");
		listbox.setVflex(true);
		listbox.setCheckmark(true);
		listbox.setMultiple(false);
		for(MAuthorizationAccount account : accounts) {
			ListItem item = new ListItem();
			item.setLabel(account.getEMail());
			listbox.appendChild(item);
		}
		listbox.setSelectedIndex(0);
		ConfirmPanel confirmPanel = new ConfirmPanel(true);
		confirmPanel.setHflex("1");
		confirmPanel.setVflex("min");
		layout.appendChild(confirmPanel);
		confirmPanel.addActionListener(evt -> {
			if (evt.getTarget() == confirmPanel.getOKButton()) {
				this.detach();
				int selected = listbox.getSelectedIndex();
				if (selected >= 0 && selected < accounts.size()) {
					callback.onCallback(accounts.get(selected));
				} else {
					callback.onCallback(null);
				}
			} else if (evt.getTarget() == confirmPanel.getButton(ConfirmPanel.A_CANCEL)) {
				this.detach();
				callback.onCallback(null);
			}
		});
	}

}
