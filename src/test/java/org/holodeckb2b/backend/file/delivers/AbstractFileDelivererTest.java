/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import org.holodeckb2b.backend.file.mmd.MessageMetaData;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
import org.holodeckb2b.interfaces.messagemodel.ISignalMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractFileDelivererTest {

	private static HolodeckB2BTestCore testCore;
    private static final Path testDir = TestUtils.getTestResource("deliveries");

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
    }


	@Test
	void testNoPayloads() throws IOException {
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(UUID.randomUUID().toString());
        userMessage.setTimestamp(new Date());
//        TradingPartner sender = new TradingPartner();
//        sender.addPartyId(new PartyId("TheSender", null));
//        sender.setRole("Sender");
//        userMessage.setSender(sender);
//        TradingPartner receiver = new TradingPartner();
//        receiver.addPartyId(new PartyId("TheRecipient", null));
//        receiver.setRole("Receiver");
//        userMessage.setReceiver(receiver);
//        CollaborationInfo collabInfo = new CollaborationInfo();
//        collabInfo.setConversationId(UUID.randomUUID().toString());
//        collabInfo.setService(new Service("FileDeliveryTest"));
//        collabInfo.setAction("Deliver");
//        userMessage.setCollaborationInfo(collabInfo);
//    	userMessage.addMessageProperty(new Property("some-meta-data", "description"));

    	assertDoesNotThrow(() -> new TestImpl(testDir).deliver(userMessage));

    	assertEquals(0, Files.list(testDir).count());
	}

	@Test
	void testPayloadWMimeType() throws IOException {
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(UUID.randomUUID().toString());
		userMessage.setTimestamp(new Date());
		TestPayload jpeg = new TestPayload(TestUtils.getTestResource("payloads/test.xml"));
		jpeg.setMimeType("text/xml");
		jpeg.setContainment(Containment.ATTACHMENT);
		jpeg.setPayloadURI(UUID.randomUUID().toString() + "@test.holodeck-b2b.org");
		userMessage.addPayload(jpeg);

		assertDoesNotThrow(() -> new TestImpl(testDir).deliver(userMessage));

		Path savedPl = Files.list(testDir).findFirst().orElse(null);

		assertNotNull(savedPl);

		assertEquals("pl-" + userMessage.getMessageId() + "-"
								+ jpeg.getPayloadURI().substring(0, jpeg.getPayloadURI().indexOf('@')) + ".xml",
					 savedPl.getFileName().toString());
	}

	@Test
	void testPayloadDetectMimeType() throws IOException {
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageId(UUID.randomUUID().toString());
		userMessage.setTimestamp(new Date());
		TestPayload jpeg = new TestPayload(TestUtils.getTestResource("payloads/dandelion.jpg"));
		jpeg.setContainment(Containment.ATTACHMENT);
		jpeg.setPayloadURI(UUID.randomUUID().toString() + "@test.holodeck-b2b.org");
		userMessage.addPayload(jpeg);

		assertDoesNotThrow(() -> new TestImpl(testDir).deliver(userMessage));

		Path savedPl = Files.list(testDir).findFirst().orElse(null);

		assertNotNull(savedPl);

		assertEquals("pl-" + userMessage.getMessageId() + "-"
								+ jpeg.getPayloadURI().substring(0, jpeg.getPayloadURI().indexOf('@')) + ".jpg",
					 savedPl.getFileName().toString());
	}



	class TestImpl extends AbstractFileDeliverer {

		public TestImpl(Path dir) {
			super(dir);
		}

		@Override
		protected String writeUserMessageInfoToFile(MessageMetaData mmd) throws IOException {
			return mmd.getMessageId();
		}

		@Override
		protected boolean payloadsAsFile() {
			return true;
		}

		@Override
		protected void deliverSignalMessage(ISignalMessage sigMsgUnit) throws MessageDeliveryException {
		}
	}
}
