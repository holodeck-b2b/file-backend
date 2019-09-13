# Holodeck B2B file based back-end integration

## API Specification
### Submit
As described on the [Holodeck B2B website](http://holodeck-b2b.org/documentation/messaging-configuration/) the information the back-end needs to provide to Holodeck B2B when it wants to send data to a trading partner consists of _meta-data_ which is used to control the message exchange and _payloads_ which contain the actual business data to be send. In this back-end integration both the meta-data and documents to send must be provided as files. The meta-data file is specified by a [XML Schema](src/main/resources/xsd/messagemetadata.xsd). How much meta-data the back-end needs to supply on submssion depends on the P-Mode used. Together it must form a complete set of meta-data required for sending the message to the trading partner.  

When submitting the back-end system should first write the files containing the business data to send and then the meta-data file (this is also the most logical sequence as the meta-data file must contain the locations of the payload files). The meta-data file must have the "mmd" extension to be picked up and submitted to the Holodeck B2B Core. The Core will check if the submission together with the P-Mode creates a complete set of meta-data and if the submission can be accepted. If this is the case the meta-data file's extension is changed to "accepted". The payload files are automatically removed unless the `//PayloadInfo/@deleteAfterSubmit` attribute is set to _false_. 
If there is an error and the submission is rejected the extension is changed to "rejected". Additionally a file with the same name as the meta-data file but with extension "err" is created. It contains information about the cause of the rejection.  

### Deliver
For the delivery of received _User Messages_ this integration offers three options, two of which write the meta-data and payloads of the received message to separate files and one creating one big file containing everything. The difference between the first two options is the format of the meta-data file. This can be either the same structure as used on submission or a copy of the `eb:Messaging` element from the ebMS message. In the latter case each `eb:PartInfo` element has an additional _part property_ (i.e. a `//eb:PartProperties/eb:Property` element) named _org:holodeckb2b:location_ that points to the file containing the payload data.  
**Example:**
```xml
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
                    <eb3:Property name="org:holodeckb2b:location">/Users/safi/holodeck-test/pl-d6a19bec-4ffa-4e9c-862b-6dc127b00077_mountain-lion.fritz.box-body-16.xml
                    </eb3:Property>
                </eb3:PartProperties>
            </eb3:Payload>
        </eb3:PayloadInfo>
    </eb3:UserMessage>
</eb3:Messaging>
```
When the "single file" option is used an XML document is created which contains a copy of the `eb:UserMessage` element from the received message and a `Payload` element for each payload of the message. Again a _part property_, in this case named _org:holodeckb2b:ref_, is used to link the meta-data in the header to the payload. The structure of the document is described in [this XML schema](src/main/resources/xsd/single_xml_delivery.xsd).   
**Example:**
```xml
<ebmsMessage xmlns="http://holodeck-b2b.org/schemas/2018/01/delivery/single_xml">
    <eb3:UserMessage xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
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
            <eb3:Service type="org:holodeckb2b:test">single_xml</eb3:Service>
            <eb3:Action>Test</eb3:Action>
            <eb3:ConversationId>org:holodeckb2b:test:conversation</eb3:ConversationId>
        </eb3:CollaborationInfo>
        <eb3:PayloadInfo>
            <eb3:Payload>
                <eb3:PartProperties>
                    <eb3:Property name="org:holodeckb2b:ref">pl-1</eb3:Property>
                </eb3:PartProperties>
            </eb3:Payload>
        </eb3:PayloadInfo>
    </eb3:UserMessage>
    <Payloads>
        <Payload xml:id="pl-1">«base64 encoded data»</Payload>
    </Payloads>
</ebmsMessage>
```
### Notify
As _Signal Messages_ do not contain business data the notify operation only writes a XML document with the meta-data to a file. Similar to last two options of the the deliver operation for _User Messages_ the XML document contains a copy of the ebMS header with the difference being the root element which is either `eb:Messaging` or the custom `ebmsMessage`. When a _Receipt_ is notified to the back-end the content of the `eb:Receipt` element is replaced with a `ReceiptChild` element that contains the qualified name of the first element of the original content. The `ReceiptChild` element is defined in its own namespace _http://holodeck-b2b.org/schemas/2015/08/delivery/ebms/receiptchild_ (see [this XML Schema](src/main/resources/xsd/delivery_rcpt_child.xsd))
Since the meta-data document used for submissions is tailored specifically to _User Messages_ it cannot be used for the notify operation.  
**Example for Receipt:**
```xml
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
```
