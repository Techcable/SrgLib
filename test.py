import unittest
from unittest import TestCase
from srg.parser import parse_lines, parse_compact_lines
from srg.output import serialize_srg


class TestSrg(TestCase):
    def __init__(self, method_name):
        super(self.__class__, self).__init__(method_name)
        with open("test.srg") as file:
            self.lines = file.read().splitlines(keepends=False)
            self.lines.sort()
        with open("test.csrg") as file:
            self.compact_lines = file.read().splitlines(keepends=False)

    def test_parse(self):
        parse_lines(self.lines)

    def test_parse_compact(self):
        mappings = parse_lines(self.lines)
        compact_mappings = parse_compact_lines(self.compact_lines)
        self.assertEqual(mappings.methods, compact_mappings.methods, "Unexpected methods for compact mappings")
        self.assertEqual(mappings.fields, compact_mappings.fields, "Unexpected fields for compact mappings")
        self.assertEqual(mappings.classes, compact_mappings.classes, "Unexpected classes for compact mappings")

    def test_serialize(self):
        mappings = parse_lines(self.lines)
        output_text = list(serialize_srg(mappings))
        output_text.sort()
        self.assertEqual(self.lines, output_text)


if __name__ == '__main__':
    unittest.main()
