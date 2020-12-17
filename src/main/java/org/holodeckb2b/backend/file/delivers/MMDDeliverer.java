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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.holodeckb2b.backend.file.NotifyAndDeliverOperation;
import org.holodeckb2b.backend.file.SubmitOperation;
import org.holodeckb2b.backend.file.mmd.MessageMetaData;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;

/**
 * Is an {@link IMessageDeliverer} implementation that delivers <b>ONLY user message units</b> to the business
 * application by writing the user message info to a MMD file and the payload contents to separate files in the same
 * directory. The payload files are referred to through the <code>location</code> attribute of the <code>PartInfo</code>
 * element in the MMD document.
 * <p>The XML format of the message meta data (MMD) document is the same as for the default file based message submitter
 * ({@link SubmitOperation}) and defined in the <code>http://holodeck-b2b.org/schemas/2014/06/mmd</code> xml schema
 * definition.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see NotifyAndDeliverOperation
 * @see MessageMetaData
 */
public class MMDDeliverer extends AbstractFileDeliverer {

    public MMDDeliverer(final String dir) {
        super(dir);
    }

    @Override
    protected void deliverSignalMessage(final ISignalMessage sigMsgUnit) throws MessageDeliveryException {
        // Not supported, this deliverer only delivers user messages!
    }

    /*
     * The payloads should be written to the delivery directory
     */
    @Override
    protected boolean payloadsAsFile() {
    	return true;
    }
    
    @Override
    protected String writeUserMessageInfoToFile(final MessageMetaData mmd) throws IOException {
        final Path mmdFilePath = FileUtils.createFileWithUniqueName(directory
                                                            + mmd.getMessageId().replaceAll("[^a-zA-Z0-9.-]", "_")
                                                            + ".mmd" + TMP_EXTENSION);
        try {
            mmd.writeToFile(new File(mmdFilePath.toString()));
            return changeExt(mmdFilePath);
        } catch (IOException ex) {
            // Something went wrong on writing the mmd file, try to remove the already created file
            try {
                Files.deleteIfExists(mmdFilePath);
            } catch (IOException io) {
                log.error("Could not remove temp file [" + mmdFilePath.toString() + "]!");
            }
            throw ex;
        }
    }

}
