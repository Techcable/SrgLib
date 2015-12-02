# SrgLib
A python library for handling srg files and related stuff

![build status](https://img.shields.io/travis/ProjectTestificate/SrgLib.svg)
![powered by tacos](https://img.shields.io/badge/powered by-tacos-brightgreen.svg)

## Features
- Correctly Parses and Outputs srg files
  - Is able to [fully reproduce](test.py#21) the original srg file from its internal representation
- Well documented
  - All public methods have complete documentation
- Object oriented
  - Types have objects, which have useful utility methods
- Fast
  - Parses 27,000 lines in slightly over a second on python 3.5
- Compatible with python 3.5
  - This library requires the 'typing' module for type hinting
  - Any version before python 3.5 does not have type hinting and won't work