/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestMessageSubmitter;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SubmitOperationTest {

    private static final Path basePath = TestUtils.getTestBasePath().resolve("submission");    
    
    private int numOfMMDs;
    
    @BeforeEach
    public void setUp() throws Exception {
    	HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore(basePath.toString()));

        File submitDir = basePath.toFile();
        if (submitDir.exists())
        	FileUtils.deleteDirectory(submitDir);        
        FileUtils.forceMkdir(submitDir);
        
        numOfMMDs = new Random().nextInt(100);
        for(int i = 0; i < numOfMMDs; i++) { 
        	try (FileWriter fw = new FileWriter(basePath.resolve("test_submission_" + i + ".mmd").toFile())) {
        		fw.write(
	    				"<MessageMetaData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + 
	    				" xmlns=\"http://holodeck-b2b.org/schemas/2014/06/mmd\">" + 
	    				"    <CollaborationInfo>" + 
	    				"        <AgreementRef pmode=\"ex-pm-push\"/>" + 
	    				"        <ConversationId>org:holodeckb2b:test:conversation</ConversationId>" + 
	    				"    </CollaborationInfo>" + 
	    				"    <PayloadInfo>" + 
	    				"        <PartInfo containment=\"attachment\" mimeType=\"image/jpeg\"" +
	    				"					location=\"dandelion.jpg\"/>" + 
	    				"    </PayloadInfo>" + 
	    				"</MessageMetaData>");
        	} catch (IOException ex) {
        		fail("Could not create the MMD files for testing");
        	}        	
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
    	File submitDir = basePath.toFile();
        if (submitDir.exists())
        	FileUtils.deleteDirectory(submitDir);                
    }

    @Test
    public void testSingleWorker() {
        SubmitOperation worker = new SubmitOperation();

        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", basePath.toString());
        try {
            worker.setParameters(params);
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
        
        worker.run();

        assertEquals(numOfMMDs, ((TestMessageSubmitter) HolodeckB2BCoreInterface.getMessageSubmitter()).getAllSubmitted().size());
    }
    
    @Test
    public void testMultipleWorkers() {
        
    	final int numOfWorkers = Math.max(1, new Random().nextInt(5));    	
    	
        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", basePath.toString());
        
        final Thread[] workers = new Thread[numOfWorkers];
        try {
            for (int i = 0; i < numOfWorkers; i++) {
            	SubmitOperation worker = new SubmitOperation();
            	worker.setParameters(params);
            	workers[i] =  new Thread(worker);
            }        	            
        } catch (TaskConfigurationException e) {
            fail(e.getMessage());
        }
        
        try {
    		for (final Thread w : workers)  
    			w.start();    			    		
			for (final Thread w : workers)
				w.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("Not all workers finished!");
		}
        assertEquals(numOfMMDs, ((TestMessageSubmitter) HolodeckB2BCoreInterface.getMessageSubmitter()).getAllSubmitted().size());
    }
    
}
