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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Random;

import org.holodeckb2b.backend.file.mmd.MessageMetaData;
import org.holodeckb2b.backend.file.mmd.PartInfo;
import org.holodeckb2b.common.workers.AbstractWorkerTask;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * This worker reads all MMD documents from the specified directory and submits the corresponding user message to the
 * Holodeck B2B core to trigger the send process.
 * <p>The files to process must have extension <b>mmd</b>. After processing the file, i.e. after the user message has
 * been submitted, the extension will be changed to <b>accepted</b>. When an error occurs on submit the extension will
 * be changed to <b>rejected</b> and information on the error will be written to a file with the same name but
 * with extension <b>err</b>.
 * <p>By default the payload files are removed after successful submission to the Core. This behaviour can be changed
 * per submission by setting the <code>//PayloadInfo/@deleteFilesAfterSubmit</code> attribute in the MMD or globally
 * by setting the _deleteFilesAfterSubmit_ parameter of the worker. If a value is supplied in the MMD it takes
 * precedence over the global value configured in the worker.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SubmitOperation extends AbstractWorkerTask {
    /**
     * The path to the directory to watch for MMD files
     */
    protected String watchPath;
    /**
     * Default setting whether the payload file should be removed upon successful submission
     */
    protected boolean removePayloadsDefault;
    /**
     * Random numbers are used to create unique temp file names
     */
    protected Random randomizer = new Random();

    /**
     * Initialises the worker. This worker has just one parameter, <i>watchPath</i>, which must point to the directory
     * that contains the MMD files.
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
        // Check the watchPath parameter is provided and points to a directory
        final String pathParameter = (String) parameters.get("watchPath");
        if (Utils.isNullOrEmpty(pathParameter)) {
            log.error("Unable to configure task: Missing required parameter \"watchPath\"");
            throw new TaskConfigurationException("Missing required parameter \"watchPath\"");
        } else if (!Paths.get(pathParameter).isAbsolute())
            watchPath = HolodeckB2BCoreInterface.getConfiguration().getHolodeckB2BHome().resolve(pathParameter)
            																								.toString();
        else
            watchPath = pathParameter;

        final File dir = new File(watchPath);
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            log.error("The specified directory to watch for submission [" + watchPath + "] is not accessible to HB2B");
            throw new TaskConfigurationException("Invalid path specified!");
        }

        final String globalDelete = (String) parameters.get("deleteFilesAfterSubmit");
        removePayloadsDefault = globalDelete == null || Utils.isTrue(globalDelete);
        log.info("Configured submitter:\n\tWatched directory = {}\n\tRemove payloads = {}", watchPath,
        			removePayloadsDefault);
    }

    @Override
    public void doProcessing() {
        log.debug("Get list of available MMD files from watched directory: " + watchPath);
        final File   dir = new File(watchPath);
        final File[] mmdFiles = dir.listFiles(new FileFilter() {
                                        @Override
                                        public boolean accept(final File file) {
                                            return file.isFile() && file.getName().toLowerCase().endsWith(".mmd");
                                        }
                                    });
        // A null value indicates the directory could not be read => signal as error
        if (mmdFiles == null) {
            log.error("The specified directory [" + watchPath + "]could not be searched for MMD files!");
            return;
        }

        for(File f : mmdFiles) {
            // Get file name without the extension
            final String  cFileName = f.getAbsolutePath();
            final String  baseFileName = cFileName.substring(0, cFileName.toLowerCase().indexOf(".mmd"));
            final String  tFileName = baseFileName + "_" + randomizer.nextInt() + ".processing";
            final File 	  tFile = new File(tFileName);

	        try {
	            // Directly rename file to prevent processing by another worker
	        	if (!f.exists() || !f.renameTo(tFile)) {
	                // Renaming failed, so file already processed by another worker or externally
	                // changed
	                log.debug(f.getName() + " is not processed because it could not be renamed");
	                continue;
	            }
                // The file can be processed
                log.trace("Read message meta data from " + f.getName());
                final MessageMetaData mmd = MessageMetaData.createFromFile(new File(tFileName));
                log.trace("Succesfully read message meta data from " + f.getName());
                // Convert relative paths in payload references to absolute ones to prevent file not found errors
                convertPayloadPaths(mmd, f);
                HolodeckB2BCoreInterface.getMessageSubmitter().submitMessage(mmd);
                log.info("User message from " + f.getName() + " succesfully submitted to Holodeck B2B");
                if (mmd.shouldDeleteFilesAfterSubmit() != null ? mmd.shouldDeleteFilesAfterSubmit()
                											   : removePayloadsDefault)
                	deletePayloadFiles(mmd);
                // Change extension to reflect success
                Files.move(Paths.get(tFileName), FileUtils.createFileWithUniqueName(baseFileName + ".accepted")
                           , StandardCopyOption.REPLACE_EXISTING);
	        } catch (final Exception e) {
	            // Something went wrong on reading the message meta data
	            log.error("An error occured when processing message meta data from " + f.getName()
	                        + ". Details: " + Utils.getRootCause(e).getMessage());
	            // Change extension to reflect error and write error information
	            try {
	                final Path rejectFilePath = FileUtils.createFileWithUniqueName(baseFileName + ".rejected");
	                Files.move(Paths.get(tFileName), rejectFilePath, StandardCopyOption.REPLACE_EXISTING);
	                writeErrorFile(rejectFilePath, e);
	            } catch (IOException ex) {
	                // The directory where the file was originally found has gone. Nothing we can do about it, so ignore
	                log.error("An error occured while renaming the mmd file or writing the error info to file!");
	            }
	        }
        }
    }

    /**
     * Is a helper method to convert relative payload paths to absolute ones.
     *
     * @param mmd       The message meta-data document for the message to submit
     * @param mmdFile   The file handler for the MMD document, used to base path
     */
    protected void convertPayloadPaths(final MessageMetaData mmd, final File mmdFile) {
        final String basePath = mmdFile.getParent();
        if (!Utils.isNullOrEmpty(mmd.getPayloads()))
            for (final IPayload p : mmd.getPayloads()) {
                final PartInfo pi = (PartInfo) p;
                if (!(Paths.get(pi.getContentLocation()).isAbsolute()))
                    pi.setContentLocation(Paths.get(basePath, pi.getContentLocation()).normalize().toString());
            }
    }

    /**
     * Helper method to delete the payload files included in the MMD.
     *
     * @param mmd	the message meta-data document of the submitted message
     */
    private void deletePayloadFiles(final MessageMetaData mmd) {
	    if (!Utils.isNullOrEmpty(mmd.getPayloads())) {
	    	log.trace("Deleting submitted payload files");
	    	mmd.getPayloads().forEach(p -> {
				try {
					Files.deleteIfExists(Paths.get(p.getContentLocation()));
				} catch (IOException e) {
					log.warn("Payload file ({}) could not be deleted, remove manually!",
							p.getContentLocation());
				}
				log.debug("Deleted submitted payload files");
	    	});
    	}
    }

    /**
     * Writes error information to file when a submission failed.
     *
     * @param rejectFilePath   The path to the renamed mmd file. Used to determine file name for the error file.
     * @param fault            The exception that caused the submission to fail
     */
    protected void writeErrorFile(final Path rejectFilePath, final Exception fault) {
        // Split the given path into name and extension part (if possible)
        String nameOnly = rejectFilePath.toString();
        final int startExt = nameOnly.lastIndexOf(".");
        if (startExt > 0)
            nameOnly = nameOnly.substring(0, startExt);
        final String errFileName = nameOnly + ".err";

        log.trace("Writing submission error to error file: " + errFileName);
        try (PrintWriter errorFile = new PrintWriter(new File(errFileName))) {

            errorFile.write("The message could not be submitted to Holodeck B2B due to an error:\n\n");
            errorFile.write("Error type:    " + fault.getClass().getSimpleName() + "\n");
            errorFile.write("Error message: " + fault.getMessage() + "\n");
            errorFile.write("\n\nError details\n-------------\n");
            errorFile.write("Exception cause: " + (fault.getCause() != null
                                                                ? fault.getCause().toString() : "unknown") + "\n");
            errorFile.write("Stacktrace:\n");
            fault.printStackTrace(errorFile);

            log.debug("Error information written to file");
        } catch (final IOException ioe) {
            log.error("Could not write error information to error file [" + errFileName + "]!");
        }
    }
}
