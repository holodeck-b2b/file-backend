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
import org.holodeckb2b.ebms3.packaging.PartyInfoElement.TradingPartner;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.ITradingPartner;

/**
 * Is a helper class for creating the <code>To</code> and <code>From</code> child elements of the <code>PartyInfo</code>
 * element. It is based on {@link TradingPartner}, but leaves out the <code>Role</code> element if no role is specified
 * for a trading partner (which can happen if another protocol then ebMS3 is used).
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ToFromElement extends TradingPartner {

    /**
     * Creates a <code>From</code> or <code>To</code> element and includes it in the given parent element (i.e. the
     * <code>PartyInfo</code> element, but again not checked).
     *
     * @param rootName      The name to use for the element, i.e. <i>From</i> or <i>To</i>
     * @param piElement     The parent element this element should be added to
     * @param data          The data to include in the element
     * @return  The new element
     */
    public static OMElement createElement(final ElementName rootName, final OMElement piElement,
                                          final ITradingPartner data) {
        final OMFactory f = piElement.getOMFactory();

        // Create the element
        final OMElement tpInfo = f.createOMElement(rootName == ElementName.FROM ? Q_FROM_PARTY : Q_TO_PARTY,
                                                   piElement);

        // Add content, starting with all party ids
        for(final IPartyId pi : data.getPartyIds()) {
            final OMElement partyId = f.createOMElement(Q_PARTYID, tpInfo);
            partyId.setText(pi.getId());
            final String pidType = pi.getType();
            if (!Utils.isNullOrEmpty(pidType))
                partyId.addAttribute(LN_PARTYID_TYPE, pidType, null);
        }

        // Create the Role element and ensure it has a value.
        final String role = data.getRole();
        if (!Utils.isNullOrEmpty(role)) {
        	final OMElement roleElem = f.createOMElement(Q_ROLE, tpInfo);
        	roleElem.setText(role);
        }

        return tpInfo;
    }	
}
