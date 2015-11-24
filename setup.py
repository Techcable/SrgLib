#!/usr/bin/env python

from setuptools import setup

setup(name='SrgLib',
      version='1.0.0',
      description='A python library for handling srg files and related stuff)',
      author='Techcable',
      author_emails='Techcable@outlook.com',
      packages=["srg"],
      requires=["enum34"]
      )
