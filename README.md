# Holodeck B2B file based back-end integration
An extension for Holodeck B2B that implements the _Submit_, _Notify_ and _Deliver_ operations using a file based API. Both message meta-data and payloads are saved in files which are polled by Holodeck B2B and back-end system. For the _Notify_ and _Deliver_ operations there is also a choice in the format of the files.

__________________
For more information on using Holodeck B2B visit http://holodeck-b2b.org  
Lead developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/file-backend  
Issue tracker https://github.com/holodeck-b2b/file-backend/issues  

## Installation
Normally there is no need to install this extension manually as it is already included in the default distribution package you can download from the Holodeck B2B website or Github repository of the main project. In case you need to install the extension manually just copy the jar file to the `lib` directory of the Holodeck B2B instance. Please note that the extension requires Holodeck B2B version 5.x.  

## Configuration
### Submit
The Submit operation is implemented as a Holodeck B2B "worker" which is configured in the `workers.xml` configuration file. The worker's implementation class is `org.holodeckb2b.backend.file.SubmitOperation` and has two parameters:
1. _watchPath_ : points to the directory where the back-end writes the meta-data files. It is recommended to specify an absolute path, but in case a relative path is provided it is evaluated with the Holodeck B2B home directory as base path.  
The default distribution package already has this worker configured for submissions to the `«HB2B_HOME»/data/msg_out` directory. If required multiple workers, watching different directories can be configured. 
2. _deleteFilesAfterSubmit_ : should be used to define the default behaviour whether the payload files should be removed after successful submission to
the Holodeck B2B Core. Boolean value. If the parameter is not set the default is to remove payloads after submission. 

### Notify and Deliver
Like all Holodeck B2B _delivery methods_ the notify and deliver operations are configured in the P-Mode that governs the message exchanges. There are several P-Mode parameters where a _delivery method_ can be configured, the most common being the "default" one on a leg which will be used for all received messages on that leg if no specific delivery method has been defined for a specific signal message type. See the P-Mode documentation for more details where delivery methods can be configured. 
To configure the file based integration as delivery method set the class name to `org.holodeckb2b.backend.file.NotifyAndDeliverOperation` and add two parameters to define the path where the files should be written (parameter name=_deliveryDirectoy_) and which format should be used for the meta-data file (parameter name=_format_). The values for the meta-data format are: _mmd_, _ebms_ and _single_xml_. When choosing a format please keep in mind that the _mmd_ format cannot be used for notifications of signals.  

## API Specification
For a full description how the operations are implemented see the [API Specification page](api_specification.md).

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
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/file-backend/issues).  
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## License
This module is licensed under the General Public License V3 (GPLv3) which is included in the LICENSE in the root of the project. 

## Support
Commercial Holodeck B2B support is provided by Chasquis. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.
