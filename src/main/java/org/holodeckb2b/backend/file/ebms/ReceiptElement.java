package org.holodeckb2b.backend.file.ebms;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.packaging.MessageInfoElement;
import org.holodeckb2b.ebms3.packaging.SignalMessageElement;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;

/**
 * Is a facade to {@link ReceiptElement} from the ebMS3/AS4 module of the main Holodeck B2B project that will replace
 * the content of the <code>Receipt</code> element with a <code>ReceiptChild</code> element that describes the type of
 * receipt that was received.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ReceiptElement extends org.holodeckb2b.ebms3.packaging.ReceiptElement {
    protected static final QName  Q_RECEIPT_CHILD = new QName(
    											"http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild",
    											"ReceiptChild");
    
    /**
     * Creates a new <code>eb:SignalMessage</code> for a <i>Receipt Signal</i> message unit.
     *
     * @param messaging     The parent <code>eb:Messaging</code> element
     * @param receipt       The information to include in the receipt signal
     * @return              The new element representing the receipt signal
     */
    public static OMElement createElement(final OMElement messaging, final IReceipt receipt) {
        // First create the SignalMessage element that is the placeholder for
        // the Receipt element containing the receipt info
        final OMElement signalmessage = SignalMessageElement.createElement(messaging);

        // Create the generic MessageInfo element
        MessageInfoElement.createElement(signalmessage, receipt);

        final OMFactory f = messaging.getOMFactory();
        
        // Create the Receipt element
        final OMElement rcptElement = f.createOMElement(Q_ELEMENT_NAME, signalmessage);
        // Add the specific child element for delivery 
        final OMElement rcptChild = f.createOMElement(Q_RECEIPT_CHILD, rcptElement);
        rcptChild.declareDefaultNamespace(Q_RECEIPT_CHILD.getNamespaceURI());
        
        // Now add the name of the first element of the original content to this new child
        final List<OMElement> content = receipt.getContent();
        if (!Utils.isNullOrEmpty(content)) 
        	rcptChild.setText(content.get(0).getQName().toString());       

        return signalmessage;
    }	
}
