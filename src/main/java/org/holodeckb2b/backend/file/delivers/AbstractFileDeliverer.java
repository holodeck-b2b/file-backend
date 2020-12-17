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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.backend.file.mmd.MessageMetaData;
import org.holodeckb2b.backend.file.mmd.PartInfo;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;

/**
 * Is an abstract {@link IMessageDeliverer} implementation that implements the delivery of user messages by writing the
 * the message unit info and payload contents to file. The format of the message meta data file has to be implemented in
 * the subclass.
 * <p>This deliverer also does not implement the delivery of signal messages. If signals have to be delivered to the
 * business application this should also be implemented in the subclass by overriding {@link #deliverSignalMessage(org.holodeckb2b.common.messagemodel.IMessageUnit)}
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public abstract class AbstractFileDeliverer implements IMessageDeliverer {
	/**
	 * Extension to use when writing the files to disk. This extension is used to prevent the back-end from picking up
	 * files that are still being written.
	 */
	protected static final String TMP_EXTENSION = ".processing"; 
			
    /**
     * The where the files should be stored
     */
    protected String  directory = null;

    /**
     * Logger
     */
    protected Logger   log = LogManager.getLogger(this.getClass());

    /**
     * Constructs a new deliverer which will write the files to the given directory.
     *
     * @param dir   The directory where file should be written to.
     */
    public AbstractFileDeliverer(final String dir) {
        this.directory = dir;
    }

    @Override
    public void deliver(final IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
        if (rcvdMsgUnit instanceof IUserMessage)
            deliverUserMessage((IUserMessage) rcvdMsgUnit);
        else // message unit is a signal
            deliverSignalMessage((ISignalMessage) rcvdMsgUnit);
    }

    /**
     * Delivers the user message to business application.
     *
     * @param usrMsgUnit        The user message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the user message to the business
     *                                  application
     */
    protected void deliverUserMessage(final IUserMessage usrMsgUnit) throws MessageDeliveryException {
        log.debug("Delivering user message with msgId=" + usrMsgUnit.getMessageId());

        // We first convert the user message into a MMD document so info can be edited
        final MessageMetaData mmd = new MessageMetaData(usrMsgUnit);        
        final Collection<IPayload>    copiedPLs = new ArrayList<>();
        try {
	        if (!Utils.isNullOrEmpty(mmd.getPayloads()) && payloadsAsFile()) {
	        	log.debug("Copy all payload files to delivery directory");
		        // The payload files should be copied to delivery directory and the MMD info should be
	        	// updated to reference the new location of the payloads
	            for(final IPayload p : mmd.getPayloads()) {
	                final Path newPath = copyPayloadFile(p, mmd.getMessageId());
	                if (newPath != null)
	                        ((PartInfo) p).setContentLocation(newPath.toString());
	                copiedPLs.add(p);
	            }
	            log.trace("Copied all payload files, set as new payload info in MMD");
	            mmd.setPayloads(copiedPLs);
	        }
	            
            log.trace("Write message meta data to file");
            final String outFile = writeUserMessageInfoToFile(mmd);
            log.debug("User message [msgID={}] delivered to {}", mmd.getMessageId(), outFile);
        } catch (final IOException ex) {
            log.error("An error occurred while delivering the user message [" + mmd.getMessageId()
                                                                    + "]\n\tError details: " + ex.getMessage());
            // Something went wrong writing files to the delivery directory, but some payload files
            // may already been copied and should be deleted
            if (!copiedPLs.isEmpty()) {            	
                log.trace("Remove already copied payload files from delivery directory");
                for(final IPayload p : copiedPLs)
                	try {
                		Files.deleteIfExists(Paths.get(p.getContentLocation()));
	                } catch (IOException io) {
	                    log.error("Could not remove temp file [" + p.getContentLocation() + "]! Remove manually.");
	                }
            }
            // And signal failure
            throw new MessageDeliveryException("Unable to deliver user message [" + mmd.getMessageId()
                                                    + "]. Error details: " + ex.getMessage());
        }
    }

    /**
     * Writes the user message data to a file.
     *
     * @param mmd           The user message unit meta data.
     * @return Path of the file that contains the message (meta-)data
     * @throws IOException  When the information could not be written to disk.
     */
    protected abstract String writeUserMessageInfoToFile(MessageMetaData mmd) throws IOException;
    
    /**
     * Indicates whether the payloads included with the user message should be copied to the delivery directory.
     * 
     * @return	<code>true</code> if the payloads should be copied to the delivery directory, <code>false</code> if not
     */
    protected abstract boolean payloadsAsFile(); 
    
    /**
     * Delivers the signal message (Error or Receipt) to business application.
     *
     * @param sigMsgUnit        The signal message message unit to deliver
     * @throws MessageDeliveryException When an error occurs while delivering the signal message to the business
     *                                  application
     */
    protected abstract void deliverSignalMessage(ISignalMessage sigMsgUnit) throws MessageDeliveryException;    
    
    /**
     * Helper method that will change the temporary "processing" extension into the final "xml" extension. As there 
     * could already exist a file with the same name and "xml" extension the method calls {@link 
     * Utils#createFileWithUniqueName(String)} to ensure that the xml file can be written.
     *  
     * @param tmpFilePath	the path to the temp file
     * @return				the path to the xml file
     * @throws IOException	when the file could not be moved
     */
    protected String changeExt(Path tmpFilePath) throws IOException {
    	String filename = tmpFilePath.toString();
    	filename = filename.substring(0, filename.lastIndexOf(TMP_EXTENSION)) + ".xml";
    	
    	return Files.move(tmpFilePath, FileUtils.createFileWithUniqueName(filename), StandardCopyOption.REPLACE_EXISTING)
    				.toString();    	
    }
    
    /**
     * Helper method to copy a the payload content to <i>delivery directory</i>.
     *
     * @param p         The payload for which the content must be copied
     * @param msgId     The message-id of the message that contains the payload, used for name the file
     * @return          The path where the payload content is now stored
     * @throws IOException  When the payload content could not be copied to the <i>delivery directory</i>
     */
    private Path copyPayloadFile(final IPayload p, final String msgId) throws IOException {
        // If payload was external to message, it is not processed by Holodeck B2B, so no content to move
        if (IPayload.Containment.EXTERNAL == p.getContainment())
            return null;

        // Compose a file name for the payload file, based on msgId and href
        String plRef = p.getPayloadURI();
        plRef = (plRef == null || plRef.isEmpty() ? "body" : plRef);
        // If this was a attachment the reference is a a MIME Content-id. As these are also quite lengthy we shorten
        // it to the left part
        if (plRef.indexOf("@") > 0)
            plRef = plRef.substring(0, plRef.indexOf("@"));

        final Path sourcePath = Paths.get(p.getContentLocation());
        // Try to set nice extension based on MIME Type of payload
        String mimeType = p.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            // No MIME type given in message, try to detect from content
            try { mimeType = FileUtils.detectMimeType(sourcePath.toFile()); }
            catch (final IOException ex) { mimeType = null; } // Unable to detect the MIME Type
        }
        final String ext = FileUtils.getExtension(mimeType);

        final Path targetPath = FileUtils.createFileWithUniqueName(directory + "pl-"
                                                               + (msgId + "-" + plRef).replaceAll("[^a-zA-Z0-9.-]", "_")
                                                               + (ext != null ? ext : ""));

        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final Exception ex) {
            // Can not move payload file -> delivery not possible
            // Try to remove the already created file
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException io) {
                log.error("Could not remove temp file [" + targetPath.toString() + "]! Remove manually.");
            }
            throw new IOException("Unable to deliver message because payload file ["
                            + p.getContentLocation() + "] can not be moved!", ex);
        }

        return targetPath.toAbsolutePath();
    }
}