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

import java.util.Collection;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.ebms3.packaging.MessageInfoElement;
import org.holodeckb2b.ebms3.packaging.MessagePropertiesElement;
import org.holodeckb2b.ebms3.packaging.PartyInfoElement.TradingPartner.ElementName;
import org.holodeckb2b.ebms3.packaging.PayloadInfoElement;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is a facade to {@link UserMessageElement} from the ebMS3/AS4 module of the main Holodeck B2B project that can handle
 * incomplete message meta-data (which can occur when another messaging protocol is used) and leaves out elements 
 * accordingly.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class UserMessageElement extends org.holodeckb2b.ebms3.packaging.UserMessageElement {

    /**
     * Creates a <code>UserMessage</code> element and adds it to the given parent element (i.e. the <code>Messaging
     * </code> element, but not checked). 
     *
     * @param messaging     The  element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final OMElement messaging, final IUserMessage data) {
        final OMFactory f = messaging.getOMFactory();

        // Create the element
        final OMElement usermessage = f.createOMElement(Q_ELEMENT_NAME, messaging);

        // Fill it based on the given data

        // MPC attribute only set when not default
        final String mpc = data.getMPC();
        if (mpc != null && !mpc.equals(EbMSConstants.DEFAULT_MPC))
            usermessage.addAttribute(LN_MPC_ATTR, mpc, null);

        // Create the MessageInfo element
        MessageInfoElement.createElement(usermessage, data);
        // Create the PartyInfo element directly without using a packaging class
        final OMElement partyInfo = f.createOMElement(Q_ELEMENT_NAME, usermessage);
        // Add content, i.e. the from and to element
        ToFromElement.createElement(ElementName.FROM, partyInfo, data.getSender());
        ToFromElement.createElement(ElementName.TO, partyInfo, data.getReceiver());       
        // Create the CollaborationInfo element        
        CollaborationInfoElement.createElement(usermessage, data.getCollaborationInfo());
        // Create the MessageProperties element (if there are message properties)
        final Collection<IProperty> msgProps = data.getMessageProperties();
        if (!Utils.isNullOrEmpty(msgProps))
            MessagePropertiesElement.createElement(usermessage, msgProps);

        // Create the eb:PayloadInfo element (if there are payloads)
        PayloadInfoElement.createElement(usermessage, data.getPayloads());

        return usermessage;
    }	
}
