## Release v2.0.0

### Major Changes:
* Reverse wire generation are now included in device files. The Extended Info Device files and associated classes have been removed. (#326/#340)
* The versioning numbers for device files have been updated to prevent old device files from being created. As such, device files generated with previous versions of RS2 are incompatible with this release. (#326/#340)

### Minor Changes:
* Updated XDLRCParser to present the names of the parsed tokens for context when parsing the XDLRC. (#326/#340)
* Created a compressered serialized XDLRC format, CXLDRC. This format can be used in place of XDLRCs. (#326/#340)

### Patches / Bug Fixes:
* Fixed a minor bug in the wire connection generation that was leading to connections being generated for wires that had no sources themselves. (#326/#340)

## Release v1.1.1

### Patches / Bug Fixes:
* The instanceX and instanceY site variables are now deserialized properly. (#328)
* The placement of LUT5's now comes before LUT6's in TCP's. (#336)

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
