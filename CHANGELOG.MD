# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 2.0.0-SNAPSHOT
##### 
### Changed
* Refactored for new Holodeck B2B 7.0.0 interfaces
* Shorten exception messages

## 1.3.0
##### 2023-05-30
### Added
* PMode.id included on delivery of User Messages (implements #9)

### Fixed
* Requested delivery format ignored (fixes #10)

## 1.2.0
##### 2023-03-08
### Added
* Support for asynchronous delivery 

### Changed
* Refactored code to adapt to changes in HB2B 6.0.0

### Deprecated
* `org.holodeckb2b.deliverymethod.file.FileDeliveryFactory`, use `org.holodeckb2b.backend.file.NotifyAndDeliverOperation` instead

## 1.1.0
##### 2021-12-14
### Added
* Configuration setting to specify default behaviour for payload files removal [#7](https://github.com/holodeck-b2b/file-backend/issues/7)

### Changed
* Refactored code to use the generic utility classes from https://github.com/holodeck-b2b/generic-utils
* Refactored code to adapt to changes in HB2B 5.1.0 Core 

### Fixed
* Incorrect namespace URI of import in the MMD schema

## 1.0.1
##### 2020-09-16
### Changed
* Use a temporary extension when writing meta-data files [#5](https://github.com/holodeck-b2b/file-backend/issues/5)

### Fixed
* Namespace declararion missing in single XML delivery [#4](https://github.com/holodeck-b2b/file-backend/issues/4)
* Duplicate occurrence of eb3:PayloadInfo elements in single XML delivery [#6](https://github.com/holodeck-b2b/file-backend/issues/6)

## 1.0.0
##### 2020-09-03
NOTE: Although this is the initial release of the project this change log also contains the
_Changed_ and _Fixed_ sections to document changes compared to the version contained before
in the main Holodeck B2B project. 

### Added
* Initial release split from the main Holodeck B2B project.

### Changed
* Default is now to delete the payload files after successful submission. [#2](https://github.com/holodeck-b2b/file-backend/issues/2) 

### Fixed  
* Concurrency issue in `SubmitFromFile` worker that may caused a single MMD to be submitted multiple times.
* Delivery operation does not use HB2B home as base for relative path parameter [#3](https://github.com/holodeck-b2b/file-backend/issues/3) 


