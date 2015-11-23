# SrgLib
A python library for handling srg files and related stuff

## Features
- Correctly Parses and Outputs srg files
  - Is able to [fully reproduce](test.py#21) the original srg file from its internal representation
- Well documented
  - All public methods have complete documentation
- Object oriented
  - Types have objects, which have useful utility methods
- Fast
  - Parses 27,000 lines in under a second (on pypy)
  - Takes slightly over a second on python 3.5
- Compatible with python 2.7 and 3.5
  - Also compatible with pypy
