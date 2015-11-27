#!/usr/bin/env python

from setuptools import setup
import sys

requirements = []

if sys.version_info < (3, 4):
      requirements.append('enum34')

setup(name='SrgLib',
      version='1.0.1',
      description='A python library for handling srg files and related stuff)',
      author='Techcable',
      author_emails='Techcable@outlook.com',
      packages=["srg"],
      requires=requirements
      )