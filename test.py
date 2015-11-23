import unittest
from unittest import TestCase
from srg.parser import parse_srg
from srg.output import serialize_srg


class TestSrg(TestCase):
    def __init__(self, method_name):
        super().__init__(method_name)
        with open("test.srg") as file:
            self.lines = file.read().splitlines(keepends=False)
            self.lines.sort()

    def test_parse(self):
        parse_srg(self.lines)

    def test_serialize(self):
        mappings = parse_srg(self.lines)
        output_text = list(serialize_srg(mappings))
        output_text.sort()
        self.assertEqual(self.lines, output_text)


if __name__ == '__main__':
    unittest.main()
