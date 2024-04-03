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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Random;

import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestMessageSubmitter;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SubmitOperationTest {

	private static HolodeckB2BTestCore testCore;
    private static final Path testDir = TestUtils.getTestResource("submissions");

    @BeforeAll
    static void prepareTestDir() throws IOException {
    	if (!Files.exists(testDir))
    		Files.createDirectory(testDir);

    	testCore = new HolodeckB2BTestCore(testDir);
    	HolodeckB2BCoreInterface.setImplementation(testCore);
    }

    @AfterAll
    static void removeTestDit() throws IOException {
    	FileUtils.cleanDirectory(testDir);
    	Files.deleteIfExists(testDir);
    }

    @BeforeEach
    public void prepareTest() throws IOException {
    	FileUtils.cleanDirectory(testDir);
    	testCore.cleanStorage();
    	((TestMessageSubmitter) testCore.getMessageSubmitter()).clear();
    	Files.copy(TestUtils.getTestResource("payloads/dandelion.jpg"), testDir.resolve("dandelion.jpg"));
    }

    @Test
    public void testKeepPayloads() {
    	createMMD(1, false);

        SubmitOperation worker = new SubmitOperation();

        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", testDir.toString());

        assertDoesNotThrow(() -> worker.setParameters(params));
        assertDoesNotThrow(() -> worker.run());

        assertEquals(1, ((TestMessageSubmitter) testCore.getMessageSubmitter()).getAllSubmitted().size());
        assertTrue(Files.exists(testDir.resolve("dandelion.jpg")));
    }

    @Test
    public void testRemovePayloads() {
    	createMMD(1, true);

    	SubmitOperation worker = new SubmitOperation();

    	HashMap<String, Object> params = new HashMap<>();
    	params.put("watchPath", testDir.toString());

    	assertDoesNotThrow(() -> worker.setParameters(params));
    	assertDoesNotThrow(() -> worker.run());

    	assertEquals(1, ((TestMessageSubmitter) testCore.getMessageSubmitter()).getAllSubmitted().size());
    	assertFalse(Files.exists(testDir.resolve("dandelion.jpg")));
    }

    @Test
    public void testMultipleWorkers() {
    	final int numOfMMDs = new Random().nextInt(100);
    	createMMD(numOfMMDs, false);

    	final int numOfWorkers = Math.max(1, new Random().nextInt(5));
        HashMap<String, Object> params = new HashMap<>();
        params.put("watchPath", testDir.toString());

        final Thread[] workers = new Thread[numOfWorkers];
        for (int i = 0; i < numOfWorkers; i++) {
        	SubmitOperation worker = new SubmitOperation();
        	assertDoesNotThrow(() -> worker.setParameters(params));
        	workers[i] =  new Thread(worker);
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

    private void createMMD(int numOfMMDs, boolean deleteFiles) {
        for(int i = 0; i < numOfMMDs; i++) {
        	try (FileWriter fw = new FileWriter(testDir.resolve("submission_" + i + ".mmd").toFile())) {
        		fw.write(
	    				"<MessageMetaData xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
	    				" xmlns=\"http://holodeck-b2b.org/schemas/2014/06/mmd\">" +
	    				"    <CollaborationInfo>" +
	    				"        <AgreementRef pmode=\"ex-pm-push\"/>" +
	    				"        <ConversationId>org:holodeckb2b:test:conversation</ConversationId>" +
	    				"    </CollaborationInfo>" +
	    				"    <PayloadInfo deleteFilesAfterSubmit=\"" + deleteFiles + "\">" +
	    				"        <PartInfo containment=\"attachment\" mimeType=\"image/jpeg\"" +
	    				"					location=\"dandelion.jpg\"/>" +
	    				"    </PayloadInfo>" +
	    				"</MessageMetaData>");
        	} catch (IOException ex) {
        		fail("Could not create the MMD files for testing");
        	}
        }
    }


}
