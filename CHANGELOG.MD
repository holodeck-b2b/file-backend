# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

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


