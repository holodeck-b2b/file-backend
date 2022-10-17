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
package org.holodeckb2b.backend.file;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.backend.file.delivers.AbstractFileDeliverer;
import org.holodeckb2b.backend.file.delivers.EbmsFileDeliverer;
import org.holodeckb2b.backend.file.delivers.MMDDeliverer;
import org.holodeckb2b.backend.file.delivers.SingleXMLDeliverer;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.IDeliveryCallback;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 * Is a file based {@link IDeliveryMethod} implementation. This delivery method writes the meta-data and payloads (for 
 * <i>User Messages</i>) to one or more files. Three formats are included in this implementation, each implemented by a 
 * separate class:<dl>
 * <dt><i>1 - "mmd"</i></dt><dd>writes the message meta as included in the ebMS header and payloads into separate files.
 * 			 	The same MMD format as used by submission operation of this back end (as specified by the
 *              XML schema definition with the namespace <code>http://holodeck-b2b.org/schemas/2014/06/mmd</code>.
 *              <br><b>NOTE :</b> This format can only deliver <i>User Message</i> message units!</dd>
 * <dt><i>2 - "ebms"</i></dt><dd>also writes the message meta data and payloads to separate files but uses the same
 *              format as the ebMS header for the meta data file. To reference the payload file location a specific
 *              <i>Part Property</i> is included in the meta-data of each payload.</dd>
 * <dt><i>3 - "single_xml"</i></dt><dd>writes all data of a message unit, including the payloads of a <i>User Message
 *              </i> message unit to one XML file. Because the payloads can be binary data they will be included
 *              <i>base64</i> encoded. This format is defined by the XML schema definition with namespace <code>
 *              http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml</code></dd>
 * </dl>
 * <p>NOTE: In both the <i>ebms</i> and <i>single_xml</i> format the meta-data on the <i>Receipt</i> content does not
 * include its content as included in the ebMS header but only an indication of what element was included. See the XML
 * schema with namespace <code>http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild</code> for the
 * definition of the element that is included as Receipt content.
 * <p>Which format is requested must be specified when creating the factory using the "<i>format</i>" parameter. If not
 * specified the <i>"ebms"</i> format will be used as default. Furthermore the directory where to write the files MUST 
 * be specified using the "<i>deliveryDirectoy</i>" setting.
 * <p>This delivery method supports the asynchronous delivery of the messages. 
 * 
 * <p>This back-end was originally included in the Holodeck B2B Core project as the default back-end integration. But 
 * since it is a non essential part it has been split into a separate extension.
 *   
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see MMDDeliverer
 * @see EbmsFileDeliverer
 * @see SingleXMLDeliverer
 */
public class NotifyAndDeliverOperation implements IDeliveryMethod {
	private static final Logger   log = LogManager.getLogger(NotifyAndDeliverOperation.class);

	/**
     * The name of the parameter for the delivery directory
     */
    public static final String DELIVERY_DIR_PARAM = "deliveryDirectory";
    /**
     * The name of the parameter for the format
     */
    public static final String FORMAT_PARAM = "format";

    /**
     * The delivery directory path
     */
    protected String deliveryDir = null;
    /**
     * The actual implementation of the delivery
     */
    protected AbstractFileDeliverer		deliverer;
  
    /**
     * Initializes the factory, ensures that a valid delivery directory is specified.
     *
     * @param settings  The settings to use for the factory. MUST contain at least on entry with key
     *                  <code>deliveryDirectoy</code> holding the path to the delivery directory.
     * @throws MessageDeliveryException When there is no directory specified or the specified directory does not exist
     *                                  or is not writable
     */
    @Override
    public void init(final Map<String, ?> settings) throws MessageDeliveryException {
        if (settings != null) {
            try {
               deliveryDir = (String) settings.get(DELIVERY_DIR_PARAM);
            } catch (final ClassCastException ex) {
                throw new MessageDeliveryException("Configuration error! No directory specified!");
            }
        }
        if (!checkDirectory())
            throw new MessageDeliveryException("Configuration error! Specified directory [" + deliveryDir
                                                                        + " does not exits or is not writable!");
        // Ensure directory path ends with separator
        deliveryDir = (deliveryDir.endsWith(FileSystems.getDefault().getSeparator()) ? deliveryDir
                              : deliveryDir + FileSystems.getDefault().getSeparator());        
        if (!checkDirectory()) {
	        // Directory is not valid
        	log.error("The specified directory ({}) is not accessible", deliveryDir);
	        throw new MessageDeliveryException("Specified directory [" + deliveryDir 
	        									+ " does not exits or is not writable!");
        }
        
        // Check if XML format is specified
        String format = (String) settings.get(FORMAT_PARAM);
        switch (format) {
            case "single_xml" :
                deliverer = new SingleXMLDeliverer(deliveryDir);
            case "mmd" :
                deliverer = new MMDDeliverer(deliveryDir);
            case "ebms" :
            default:
            	format = "ebms";
                deliverer = new EbmsFileDeliverer(deliveryDir);
        }        
        log.info("Initialised file delivery method using {} format to {}", format, deliveryDir);        		
    }

    @Override
    public boolean supportsAsyncDelivery() {
    	return true;
    }
    
    @Override
    public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
    	deliverer.deliver(rcvdMsgUnit);
    }
    
    @Override
    public void deliver(IMessageUnit rcvdMsgUnit, IDeliveryCallback callback) throws MessageDeliveryException {
    	if (!checkDirectory()) {
	        // Directory is not valid
        	log.error("The specified directory ({}) is not accessible", deliveryDir);
	        throw new MessageDeliveryException("Specified directory [" + deliveryDir 
	        									+ " does not exits or is not writable!");    		
    	}
    	try {
    		deliver(rcvdMsgUnit);
    		callback.success();
    	} catch (MessageDeliveryException deliveryFailure) {
    		callback.failed(deliveryFailure);
    	}
    }
    
    /**
     * Checks if the directory is still valid, i.e. exists and is writable.
     *
     * @return <code>true</code> when directory is valid,<br><code>false</code> if not
     */
    private boolean checkDirectory() {
        try {
            Path path = Paths.get(deliveryDir);            
            if (!path.isAbsolute())
            	path = HolodeckB2BCoreInterface.getConfiguration().getHolodeckB2BHome().resolve(deliveryDir);
            																								
            // Test if given path exists and is a directory
            if (!Files.isDirectory(path) || !Files.isWritable(path))
                // Not a writable directory!
                return false;

            deliveryDir = path.toString() + "/";
            
    		return true;

        } catch (final Exception ex) {
            return false;
        }

    }
}
