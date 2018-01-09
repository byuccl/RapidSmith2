## Release v1.1.0

### Minor Changes:
* Updated Site::setType to check the allowed site types and throw an error if the types don't match. Also added Site::setTypeUnchecked to forgo the check and avoid potential slowdown. (#321)
* Removed BelPin::getSitePins methods. (#260, #319)

### Patches / Bug Fixes:
* After merging static nets in the edif import process, macro pins now point to the corresponding global VCC or global GND net. (#317, #320)
* Fixed Github project language stats to report RapidSmith2 as a Java-based project. (#324)

## Release v1.0.0

This is the first GA (General Availability) release of RapidSmith2 and the API can now be considered stable. 

RapidSmith2 was primarily developed by Travis Haroldsen and Thomas Townsend. The initial public release of RapidSmith2 was made in January 2017. Since this release, RapidSmith2 has essentially been at version 0.y.z and has been in its initial development phase. The API of RapidSmith2 is no longer rapidly changing.

Future version numbers for RapidSmith2 releases will follow the [semantic versioning standard](http://semver.org/).

Release tags will be in this format: MAJOR.MINOR.PATCH.
* The MAJOR version will be incremented when incompatible API changes are made.
* The MINOR version will be incremented when a single feature is added in a backwards-compatible manner.
* The PATCH version will be incremented when a single backwards-compatible bug fix is made.