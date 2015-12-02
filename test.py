import unittest
from unittest import TestCase

from srg.mappings import PackageMappings, ChainedMappings
from srg.parser import parse_lines, parse_compact_lines, parse_method, parse_field
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
        self.assertEqual(mappings.methods(), compact_mappings.methods(), "Unexpected methods for compact mappings")
        self.assertEqual(mappings.fields(), compact_mappings.fields(), "Unexpected fields for compact mappings")
        self.assertEqual(mappings.classes(), compact_mappings.classes(), "Unexpected classes for compact mappings")

    def test_serialize(self):
        mappings = parse_lines(self.lines)
        output_text = list(serialize_srg(mappings))
        output_text.sort()
        self.assertEqual(self.lines, output_text)


class TestChaining(TestCase):

    chained = ChainedMappings((
        parse_compact_lines((
            "aa Entity",
            "ab Cow",
            "ac EntityPlayer",
            "ad World",
            "ae Server"
        )),
        parse_lines((
            "CL: af ForgetfulClass",
            "FD: Entity/a Entity/dead",
            "MD: Cow/a (LCow;)V Cow/love (LCow;)V",
            "MD: EntityPlayer/a (Ljava/lang/String;)V EntityPlayer/disconnect (Ljava/lang/String;)V",
            "FD: World/a World/time",
            "MD: World/a ()V World/tick ()V",
            "FD: Server/a Server/ticks",
            "MD: Server/a ()V Server/tick ()V"
        )),
        PackageMappings({"": "net.minecraft.server"})
    ))

    expected_output = parse_lines((
        "CL: aa net/minecraft/server/Entity",
        "CL: ab net/minecraft/server/Cow",
        "CL: ac net/minecraft/server/EntityPlayer",
        "CL: ad net/minecraft/server/World",
        "CL: ae net/minecraft/server/Server",
        "CL: af net/minecraft/server/ForgetfulClass",
        "FD: aa/a net/minecraft/server/Entity/dead",
        "MD: ab/a (Lab;)V net/minecraft/server/Cow/love (Lnet/minecraft/server/Cow;)V",
        "MD: ac/a (Ljava/lang/String;)V net/minecraft/server/EntityPlayer/disconnect (Ljava/lang/String;)V",
        "FD: ad/a net/minecraft/server/World/time",
        "MD: ad/a ()V net/minecraft/server/World/tick ()V",
        "FD: ae/a net/minecraft/server/Server/ticks",
        "MD: ae/a ()V net/minecraft/server/Server/tick ()V"
    ))

    def test_chaining(self):
        self.assertEqual(self.chained.classes(), self.expected_output.classes())
        self.assertEqual(self.chained.methods(), self.expected_output.methods())
        self.assertEqual(self.chained.fields(), self.expected_output.fields())

if __name__ == '__main__':
    unittest.main()
