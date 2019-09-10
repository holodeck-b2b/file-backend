/**
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.backend.file.ebms;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.packaging.AgreementRefElement;
import org.holodeckb2b.ebms3.packaging.ServiceElement;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;

/**
 * Is a facade to {@link CollaborationInfoElement} from the ebMS3/AS4 module of the main Holodeck B2B project that can 
 * handle incomplete message meta-data (which can occur when another messaging protocol is used) and leaves out elements 
 * accordingly.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CollaborationInfoElement extends org.holodeckb2b.ebms3.packaging.CollaborationInfoElement {

    /**
     * Creates a <code>CollaborationInfo</code> element and adds it to the given parent element (i.e. this would be the
     * <code>UserMessage</code> element, but not checked)
     *
     * @param umElement     The parent element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement umElement, final ICollaborationInfo data) {
    	if (data == null)
    		return null;
    	
        final OMFactory f = umElement.getOMFactory();

        // Create the element
        final OMElement collabInfo = f.createOMElement(Q_ELEMENT_NAME, umElement);

        // Fill it based on the given data
        if (data.getAgreement() != null)
            AgreementRefElement.createElement(collabInfo, data.getAgreement());

        if (data.getService() != null)
        	ServiceElement.createElement(collabInfo, data.getService());

        final String action = data.getAction();
        if (!Utils.isNullOrEmpty(action)) {
        	final OMElement actionEl = f.createOMElement(Q_ACTION, collabInfo);
        	actionEl.setText(action);
    	}
        final String convId = data.getConversationId();
        if (!Utils.isNullOrEmpty(convId)) {
	        final OMElement convIdEl = f.createOMElement(Q_CONVERSATIONID, collabInfo);
	        convIdEl.setText(convId);
        }

        return collabInfo;
    }	
}
