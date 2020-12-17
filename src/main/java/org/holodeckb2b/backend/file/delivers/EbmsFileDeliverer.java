/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.backend.file.delivers;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.backend.file.NotifyAndDeliverOperation;
import org.holodeckb2b.backend.file.ebms.ReceiptElement;
import org.holodeckb2b.backend.file.ebms.UserMessageElement;
import org.holodeckb2b.backend.file.mmd.MessageMetaData;
import org.holodeckb2b.backend.file.mmd.Property;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.ebms3.packaging.ErrorSignalElement;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;

/**
 * Is the {@link IMessageDeliverer} that implements the <i>"ebms"</i> format of the file delivery method.
 * <p>It delivers the message unit to the business application by writing the message unit info to a file using the same
 * format as in the ebMS messaging header as defined in the xml schema definition
 * <code>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/</code>.
 * <p>As only Receipt and Error signals are delivered (notified in ebMS terminology) to the business application the
 * <code>eb:SignalMessage</code> element can only have the <code>eb:Receipt</code> and <code>eb:Error</code> child
 * elements (in addition to the required <code>eb:MessageInfo</code> element).
 * <p>For Receipts the actual content of the <code>eb:Receipt</code> element as included in the ebMS message is replaced
 * with a single child element <code>ReceiptChild</code> that contains a identifier of the type of Receipt. See the XML
 * schema definition <code>http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild</code>.
 * <p>For user messages the payloads are copied to the same directory and referred to through a <i>additional</i> part
 * property named "<i>org:holodeckb2b:location</i>" in <code>eb:PartProperties/eb:Property</code>.
 * <p><b>Examples</b>
 * <p><u>User message</u>
 * <p>For a received user message unit containing two payloads there will be one XML file containing the message info
 * and two files containing the payload content. The message info file is like this:
<pre>
{@code
<eb3:Messaging xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
    <eb3:UserMessage>
        <eb3:MessageInfo>
            <eb3:Timestamp>2014-08-14T17:50:00.000+02:00</eb3:Timestamp>
            <eb3:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb3:MessageId>
        </eb3:MessageInfo>
        <eb3:PartyInfo>
            <eb3:From>
                <eb3:PartyId type="org:holodeckb2b:test">Party_1</eb3:PartyId>
                <eb3:Role>Sender</eb3:Role>
            </eb3:From>
            <eb3:To>
                <eb3:PartyId type="org:holodeckb2b:test">Party_2</eb3:PartyId>
                <eb3:Role>Receiver</eb3:Role>
            </eb3:To>
        </eb3:PartyInfo>
        <eb3:CollaborationInfo>
            <eb3:Service type="org:holodeckb2b:test">service</eb3:Service>
            <eb3:Action>Test</eb3:Action>
            <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>
        </eb3:CollaborationInfo>
        <eb3:PayloadInfo>
            <eb3:Payload>
                <eb3:PartProperties>
                    <eb3:Property name="originalFileName">simpletest.xml</eb3:Property>
                    <eb3:Property name="org:holodeckb2b:location">
                        /Users/safi/holodeck-test/pl-d6a19bec-4ffa-4e9c-862b-6dc127b00077_mountain-lion.fritz.box-body-16.xml
                    </eb3:Property>
                </eb3:PartProperties>
            </eb3:Payload>
        </eb3:PayloadInfo>
    </eb3:UserMessage>
</eb3:Messaging>
}</pre>
 * <p><u>Receipt</u>
 * <p>For a received Receipt message unit containing an AS4 non repudiation receipt the message info file is like this:
<pre>
{@code
<eb3:Messaging xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
    <eb3:SignalMessage>
        <eb3:MessageInfo>
            <eb3:Timestamp>2014-08-14T17:50:00.000+02:00</eb3:Timestamp>
            <eb3:MessageId>d6a19bec-4ffa-4e9c-862b-6dc127b00077@mountain-lion.fritz.box</eb3:MessageId>
        </eb3:MessageInfo>
        <eb3:Receipt>
            <ReceiptChild xmlns="http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild"
            >{http://docs.oasis-open.org/ebxml-bp/ebbp-signals-2.0}NonRepudiationInformation</ReceiptChild>
        </eb3:Receipt>
    </eb3:SignalMessage>
</eb3:Messaging>
}</pre>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see NotifyAndDeliverOperation
 */
public class EbmsFileDeliverer extends AbstractFileDeliverer {

    /**
     * The QName of the container element that contains the message info, i.e. the <code>eb:Messaging</code> element
     */
    private static final QName ROOT_QNAME = new QName(EbMSConstants.EBMS3_NS_URI, "Messaging",
                                                           EbMSConstants.EBMS3_NS_PREFIX);

    /**
     * Constructs a new deliverer which will write the files to the given directory.
     *
     * @param dir   The directory where file should be written to.
     */
    public EbmsFileDeliverer(final String dir) {
        super(dir);
    }
    
    /*
     * Payloads should be copied to the delivery directory
     */
    @Override
    protected boolean payloadsAsFile() {
    	return true;
    }

    /**
     * Writes the user message meta data to file using the same structure as in the ebMS header.
     *
     * @param mmd           The user message meta data.
     * @return	Path of the file that contains the message (meta-)data
     * @throws IOException  When the information could not be written to disk.
     */
    @Override
    protected String writeUserMessageInfoToFile(final MessageMetaData mmd) throws IOException {
    	
    	// First set the location as additional part property
    	log.trace("Set payload file locations as properties");
        if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
            // We add the local file location as a Part property
            for (final IPayload p : mmd.getPayloads()) {
                final Property locationProp = new Property();
                locationProp.setName("org:holodeckb2b:location");
                locationProp.setValue(p.getContentLocation());
                p.getProperties().add(locationProp);
            }
        }

        log.trace("Create delivery XML document and add message info");
        // Add the information on the user message to the container
        final OMElement  container = createContainerElement();
        final OMElement  usrMsgElement = UserMessageElement.createElement(container, mmd);
        log.trace("Information complete, write XML document to file");        
        return writeXMLDocument(container, mmd.getMessageId());
    }

    /**
     * Writes the signal message meta data to file using the same structure as in the ebMS header. For Receipt signals
     * the content of the receipt element is removed before writing the info to file.
     *
     * @param  sigMsgUnit        The signal message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the signal message to the business
     *                                  application
     */
    @Override
    protected void deliverSignalMessage(final ISignalMessage sigMsgUnit) throws MessageDeliveryException {
        final OMElement   container = createContainerElement();

        if (sigMsgUnit instanceof IReceipt) {
            log.trace("Add receipt meta data to XML");
            ReceiptElement.createElement(container, (IReceipt) sigMsgUnit);
        } else if (sigMsgUnit instanceof IErrorMessage) {
            log.trace("Add error meta data to XML");
            ErrorSignalElement.createElement(container, (IErrorMessage) sigMsgUnit);
        }

        log.trace("Added signal meta data to XML, write to file");
        try {
            writeXMLDocument(container, sigMsgUnit.getMessageId());
            log.debug("Signal message with msgID=" + sigMsgUnit.getMessageId() + " successfully delivered");
        } catch (final IOException ex) {
            log.error("An error occurred while delivering the signal message [" + sigMsgUnit.getMessageId()
                                                                    + "]\n\tError details: " + ex.getMessage());
            // And signal failure
            throw new MessageDeliveryException("Unable to deliver signal message [" + sigMsgUnit.getMessageId()
                                                    + "]. Error details: " + ex.getMessage());
        }
    }

    /**
     * Create the root element of the meta-data document.
     *
     * @return  The root element of the delivery document.
     */
    protected OMElement createContainerElement() {
        final OMFactory   f = OMAbstractFactory.getOMFactory();
        final OMElement rootElement = f.createOMElement(ROOT_QNAME);
        rootElement.declareNamespace(EbMSConstants.EBMS3_NS_URI, EbMSConstants.EBMS3_NS_PREFIX);

        return rootElement;
    }

    /**
     * Helper to write the XML to file. Serializes the given XML element to file named
     * "<code>mi-«<i>message id</i>».xml</code>".
     *
     * @param xml       The xml element to write to file
     * @param msgId     The message id of the message unit the XML is the meta data of
     * @return          The path to the new file containing the XML document
     * @throws IOException When the XML can not be written to disk
     */
    private String writeXMLDocument(final OMElement xml, final String msgId) throws IOException {
        final Path msgFilePath = FileUtils.createFileWithUniqueName(directory + "mi-"
												                + msgId.replaceAll("[^a-zA-Z0-9.-]", "_")
												                + TMP_EXTENSION);

		try {
			final FileWriter fw = new FileWriter(msgFilePath.toString());
			final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fw);
			xml.serialize(writer);
			writer.flush();
			fw.close();
			return changeExt(msgFilePath);
		} catch (final Exception ex) {
			// Can not write the message info XML to file -> delivery not possible
			// Try to remove the already created file
			try {
				Files.deleteIfExists(msgFilePath);
			} catch (IOException io) {
				log.error("Could not remove temp file [" + msgFilePath.toString() + "]! Remove manually.");
			}
			throw new IOException("Error writing message unit info to file!", ex);
		}
    }
}
