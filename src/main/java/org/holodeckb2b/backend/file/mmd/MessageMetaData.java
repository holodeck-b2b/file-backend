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
package org.holodeckb2b.backend.file.mmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.ICollaborationInfo;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.processingmodel.IMessageUnitProcessingState;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Represents the root element <code>MessageMetaData</code> from the <i>message meta data</i> (MMD for short) XML
 * document that can be used to exchange User Message message units between business application and Holodeck B2B. The
 * structure of the MMD documents is defined by XML schema <i>http://holodeck-b2b.org/schemas/2014/06/mmd</i>.
 * <p>New since version 2.0-rc2 is the indicator whether the files containing the payload data should be removed after
 * successful submission to the Holodeck B2B Core. (Note that this class only enables exchange of the indicator, it is
 * upto submitter implementations to use it!). This new attribute is defined in version 1.1 of the XSD.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Root(name = "MessageMetaData",strict = false)
@Namespace(reference="http://holodeck-b2b.org/schemas/2014/06/mmd")
public class MessageMetaData implements IUserMessage {

    /*
     * When the business application submits a user message it is recommended to
     * only supply a <i>RefToMessageId</i> when needed. Therefor, when such
     * reference is not needed the complete <i>MessageInfo</i> element can be
     * skipped.
     */
    @Element(name = "MessageInfo", required = false)
    private MessageInfo         messageInfo;

    @Element(name = "PartyInfo", required = false)
    private PartyInfo           partyInfo;

    @Element(name = "CollaborationInfo", required = false)
    private CollaborationInfo   collabInfo;

    @ElementList(name = "MessageProperties", entry = "Property", type = Property.class, required = false)
    private ArrayList<IProperty> msgProperties;

    @Element(name = "PayloadInfo", required=false)
    private PayloadInfo  payloadInfo;

    /**
     * Default constructor to create an empty <code>MessageMetaData</object>.
     */
    public MessageMetaData() {}

    /**
     * Create a new <code>MessageMetaData</code> object for the user message unit
     * described by the given {@see IUserMessage} object.
     *
     * @param msgData       The meta data of the user message unit to create the
     *                      MMD for
     */
    public MessageMetaData(final IUserMessage msgData) {
        setMPC(msgData.getMPC());
        setTimestamp(msgData.getTimestamp());
        setMessageId(msgData.getMessageId());
        setRefToMessageId(msgData.getRefToMessageId());
        setSender(msgData.getSender());
        setReceiver(msgData.getReceiver());
        setCollaborationInfo(msgData.getCollaborationInfo(), msgData.getPModeId());
        setMessageProperties(msgData.getMessageProperties());
        setPayloads(msgData.getPayloads());
    }

    /**
     * Creates a new <code>MessageMetaData</code> object for the user message unit
     * described by the XML document in the specified {@see File}.
     *
     * @param  mmdFile      A handle to file that contains the meta data
     * @return              A <code>MessageMetaData</code> for the message meta
     *                      data contained in the given file
     * @throws Exception    When the specified file is not found, readable or
     *                      does not contain a MMD document.
     */
    public static MessageMetaData createFromFile(final File mmdFile) throws Exception {
        if( !mmdFile.exists() || !mmdFile.canRead())
            // Given file must exist and be readable to be able to read MMD
            throw new Exception("Specified MMD file ["+mmdFile.getAbsolutePath()+"] not found or no permission to read!");

        try (FileInputStream fis = new FileInputStream(mmdFile)) {
        	return createFromStream(fis);
    	} catch (IOException readError) {
    		throw new Exception("Could not parse MMD from " + mmdFile.getAbsolutePath(), readError.getCause());
    	}
    }

    /**
     * Creates a new <code>MessageMetaData</code> object for the user message unit
     * described by the XML document in the specified {@see InputStream}.
     *
     * @param  is 			The input stream that contains the meta data
     * @return              A <code>MessageMetaData</code> for the message meta
     *                      data contained in the given file
     * @throws IOException  When the specified file is not found, readable or
     *                      does not contain a MMD document.
     */
    public static MessageMetaData createFromStream(final InputStream is) throws IOException {
        try {
        	return new Persister().read(MessageMetaData.class, is);
        } catch (Throwable parseError) {
        	throw new IOException("Could not parse MMD from stream", parseError);
        }
    }

    /**
     * Writes the MMD document to a file.
     *
     * @param mmdFile       The path where the MMD document should be saved.
     * @throws IOException  When the MMD document could not be saved to the specified path
     */
    public void writeToFile(final File mmdFile) throws IOException {
        if( mmdFile.exists() && !mmdFile.canWrite())
            // If the given file exists, it must be writeable to be able to save the MMD
            throw new IOException("Specified MMD file ["
                                    + mmdFile.getAbsolutePath() + "] already exists but is not writeable!");

        final Serializer serializer = new Persister();
        try {
            serializer.write(this, mmdFile);
        } catch (final Exception ex) {
            // The MMD could not be saved to the specified file
            throw new IOException("Problem writing MMD to " + mmdFile.getAbsolutePath(), ex);
        }

    }


    @Override
    public String getMPC() {
        if(messageInfo != null)
            return messageInfo.getMpc();
        else
            return null;
    }

    public void setMPC(final String mpc) {
        if(messageInfo == null)
           messageInfo = new MessageInfo();
        messageInfo.setMpc(mpc);
    }

    @Override
    public ITradingPartner getSender() {
        if(partyInfo != null)
            return partyInfo.getSender();
        else
            return null;
    }

    public void setSender(final ITradingPartner sender) {
        if (sender != null) {
            if(partyInfo == null)
                partyInfo = new PartyInfo();
            partyInfo.setSender(sender);
        }
    }

    @Override
    public ITradingPartner getReceiver() {
        if(partyInfo != null)
            return partyInfo.getReceiver();
        else
            return null;
    }

    public void setReceiver(final ITradingPartner receiver) {
        if (receiver != null) {
            if(partyInfo == null)
                partyInfo = new PartyInfo();
            partyInfo.setReceiver(receiver);
        }
    }

    @Override
    public ICollaborationInfo getCollaborationInfo() {
        return collabInfo;
    }

    public void setCollaborationInfo(final ICollaborationInfo ci, final String pmodeId) {
    	this.collabInfo = ci != null ? new CollaborationInfo(ci) : new CollaborationInfo();
    	AgreementReference r = (AgreementReference) this.collabInfo.getAgreement();
    	if (r == null) {
    		r = new AgreementReference();
    		this.collabInfo.setAgreement(r);
    	}
    	r.setPModeId(pmodeId);
    }

    @Override
    public Collection<IProperty> getMessageProperties() {
        return msgProperties;
    }

    public void setMessageProperties(final Collection<IProperty> msgProps) {
        if(msgProps != null && msgProps.size() > 0) {
            this.msgProperties = new ArrayList<>(msgProps.size());
            for(final IProperty p : msgProps)
                this.msgProperties.add(new Property(p));
        }
        else
            this.msgProperties = null;
    }

    @Override
    public Collection<PartInfo> getPayloads() {
        return (payloadInfo != null ? payloadInfo.getPayloads() : null);
    }

    public void setPayloads(final Collection<? extends IPayload> pl) {
        if (Utils.isNullOrEmpty(pl))
            this.payloadInfo = null;
        else if (this.payloadInfo == null)
            this.payloadInfo = new PayloadInfo(pl);
        else
            this.payloadInfo.setPayloadInfo(pl);
    }

    /**
     * Gets the indicator whether the payload files should be deleted after successful submission to the Holodeck B2B
     * Core.
     * <p>NOTE:  this class only enables exchange of the indicator, it is upto submitter implementations to use it!
     *
     * @return  <code>true</code> when the files should be deleted, <code>false</code> if not.
     * @since 2.0-rc2
     */
    public Boolean shouldDeleteFilesAfterSubmit() {
        return (payloadInfo != null ? payloadInfo.shouldDeleteFilesAfterSubmit() : null);
    }

    /**
     * Sets the indicator whether the payload files should be deleted after successful submission to the Holodeck B2B
     * Core. Setting the indicator is only useful for meta-data objects that are used for submission with a submitter
     * that supports this indicator.
     *
     * @param delete The new value for the indicator, maybe <code>null</code> if the default setting should be used.
     */
    public void setDeleteFilesAfterSubmit(final Boolean delete) {
    	if (delete != null && this.payloadInfo == null)
            this.payloadInfo = new PayloadInfo();
    	if (payloadInfo != null)
    		this.payloadInfo.setDeleteFilesAfterSubmit(delete);
    }

    @Override
    public Date getTimestamp() {
        if( messageInfo != null)
            return messageInfo.getTimestamp();
        else
            return null;
    }

    public void setTimestamp(final Date ts) {
        if( messageInfo == null)
            messageInfo = new MessageInfo();
        messageInfo.setTimestamp(ts);
    }

    @Override
    public String getMessageId() {
        if( messageInfo != null)
            return messageInfo.getMessageId();
        else
            return null;
    }

    public void setMessageId(final String msgId) {
        if( messageInfo == null)
            messageInfo = new MessageInfo();
        messageInfo.setMessageId(msgId);
    }

    @Override
    public String getRefToMessageId() {
        if( messageInfo != null)
            return messageInfo.getRefToMessageId();
        else
            return null;
    }

    public void setRefToMessageId(final String refId) {
        if( messageInfo == null)
            messageInfo = new MessageInfo();
        messageInfo.setRefToMessageId(refId);
    }

    @Override
    public String getPModeId() {
        // If given, the P-Mode id is contained in CollaborationInfo/AgreementRef/
        String pmodeId = null;
        try {
            pmodeId = getCollaborationInfo().getAgreement().getPModeId();
        } catch (final Throwable t) {
            // If some part of configuration is not there, a NPE would occur which we ignore and just return null
        }
        return pmodeId;
    }

    /**
     * <b>Not supported</b>
     * <p>The direction of the message unit can be derived from how the MMD file is used.
     * @return
     */
    @Override
    public Direction getDirection() {
        return null;
    }

    /**
     * <b>Not supported</b>
     * @return
     */
    @Override
    public List<IMessageUnitProcessingState> getProcessingStates() {
        return null;
    }
}
