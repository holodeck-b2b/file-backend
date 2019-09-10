# Holodeck B2B file based back-end integration
Is an extension for Holodeck B2B that implements the _Submit_, _Notify_ and _Deliver_ operations using a file based API. 

## API Specification


__________________
For more information on using Holodeck B2B visit http://holodeck-b2b.org  
Lead developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/hb2b-as2  
Issue tracker https://github.com/holodeck-b2b/hb2b-as2/issues  

## Installation
### Prerequisites  
This extension requires that you have already deployed Holodeck B2B version 4.2.0 or later.

### Installation and Configuration
First step is to build the extension or download the latest release package. Copy the resulting `file-backend-«version».jar`file to the `lib` directory of the Holodeck B2B instance to enable the file based interface in the Holodeck B2B instance. Please note that you cannot activate the extension in a running Holodeck B2B instance and will need to restart the server to activate the extension.

No additional configuration is needed for the _Submit_ operation. The _Delivery_ and _Notify_ operations are configured in the
P-Modes by setting the _delivery method_. To use this REST back-end integration set the applicable `DeliveryMethod` element to `org.holodeckb2b.backend.rest.NotifyAndDeliverOperation` and configure its parameters:
1. _URL_ : the URL where the REST service is hosted by the back-end application. As explained above "/deliver" will be added
 to this URL when delivering _User Messages_ and for "/notify/receipt" and "/notify/error" for notification of _Receipt_ respectively _Error_ Signals.
2. _TIMEOUT_ : the time (in milliseconds) the delivery method should wait for the back-end system to accept the delivery and notification. This parameter is optional and when not specified a default timeout of 10 seconds will be used.

## Contributing
We are using the simplified Github workflow to accept modifications which means you should:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code
* commit incrementally with readable and detailed commit messages
* submit a pull-request against the master branch of this repository

If your contribution is more than a patch, please contact us beforehand to discuss which branch you can best submit the pull request to.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/rest-backend/issues).  
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## License
This module is licensed under the General Public License V3 (GPLv3) which is included in the LICENSE in the root of the project. NOTE that this
license only applies to the source code contained in this project and **does not apply to** the implementation of the REST API by **the backend system**.

## Support
Commercial Holodeck B2B support is provided by Chasquis. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.
