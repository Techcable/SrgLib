from typing import Iterable

from srg import types, Mappings
from srg import MethodData, FieldData


def parse_file(filename: str) -> Mappings:
    """
    Read the srg file and parses into a Mappings object

    A utility function wrapping 'parse_srg'

    :param filename: the file to read from
    :return: the parsed data
    :raises ValueError: if parsing error occurs
    :raises OSError: if an error occurs reading the file
    """
    with open(filename, "rt") as file:
        return parse_lines(file)


def parse_lines(line_source: Iterable[str]) -> Mappings:
    """
    Parse the lines of a srg file into a Mappings object

    Package data is not parsed

    :param line_source: an iterable object containing the lines of the srg file
    :return: the parsed Mappings object
    :raises ValueError: if parsing error occurs
    """
    methods = dict()
    fields = dict()
    classes = dict()
    for line in line_source:
        line = line.strip()
        if line.startswith("#") or len(line) == 0:
            continue
        split = line.split(" ")
        id = split[0][:-1]
        if id == "MD":
            original_name = split[1]
            original_signature = split[2]
            renamed_name = split[3]
            renamed_signature = split[4]
            original_method = parse_method(original_name, original_signature)
            renamed_method = parse_method(renamed_name, renamed_signature)
            methods[original_method] = renamed_method
        elif id == "FD":
            original_name = split[1]
            renamed_name = split[2]
            original_field = parse_field(original_name)
            renamed_field = parse_field(renamed_name)
            fields[original_field] = renamed_field
        elif id == "CL":
            original_name = split[1]
            renamed_name = split[2]
            original_type = types.parse_internal_name(original_name)
            renamed_type = types.parse_internal_name(renamed_name)
            classes[original_type] = renamed_type
        elif id == "PK":
            continue  # Ignore packages, because they are stupid
        else:
            raise ValueError("Invalid id " + id)
    return Mappings(classes, fields, methods)


def parse_compact_lines(line_source: Iterable[str]) -> Mappings:
    """
    Parse the lines of a compact srg file into a Mappings object

    Traditional srg files are preferred over compact srg files
    No matter what they say, the original format is bette

    :param line_source: an iterable object containing the lines of the csrg file
    :return: the parsed Mappings object
    :raises ValueError: if parsing error occurs
    """
    # These only contain type infromation for the original mappings, the renaemd oneshave no type information at all
    # Methods: method data -> new name
    # Fields: field data -> new name
    methods = dict()
    fields = dict()
    # These are the type mappings, which we use to fill-in the type information for the method and field mappings
    classes = dict()
    for line in line_source:
        parts = line.split(' ')
        # Method Mappings
        if len(parts) == 4:
            original_type_name = parts[0]
            original_name = parts[1]
            original_signature = parts[2]
            args, return_type = parse_signature(original_signature)
            original = MethodData(types.parse_internal_name(original_type_name), original_name, args, return_type)
            renamed = parts[3]
            methods[original] = renamed
        # Field mappings
        elif len(parts) == 3:
            original_type_name = parts[0]
            original_name = parts[1]
            original = FieldData(type=types.parse_internal_name(original_type_name), name=original_name)
            renamed = parts[2]
            fields[original] = renamed
        elif len(parts) == 2:
            original_name = parts[0]
            renamed_name = parts[1]
            original = types.parse_internal_name(original_name)
            renamed = types.parse_internal_name(renamed_name)
            classes[original] = renamed
    mappings = Mappings(classes, dict(), dict())
    for original, renamed_name in methods.items():
        renamed_type = mappings.get_class(original.type)
        renamed_args = [mappings.get_class(original_arg) for original_arg in original.args]
        renamed_return_type = mappings.get_class(original.return_type)
        renamed = MethodData(renamed_type, renamed_name, renamed_args, renamed_return_type)
        mappings.set_method(original, renamed)
    for original, renamed_name in fields.items():
        renamed_type = mappings.get_class(original.type)
        renamed = FieldData(renamed_type, renamed_name)
        mappings.set_field(original, renamed)
    return mappings


def parse_method(name, signature):
    """
    Parse the method with the given name and signature into a MethodData object

    :param name: the asm-format name
    :param signature: the asm-format signature
    :return: the method as a MethodData object
    :raises ValueError: if the data is invalid
    """
    name = _split_name(name)
    args, return_type = parse_signature(signature)
    return MethodData(type=types.parse_internal_name(name[0]), name=name[1], args=args, return_type=return_type)


def parse_signature(signature):
    """
    Parse the bytecode-format method signature

    Returns a tuple with a list of argument types and the return type

    :param signature:
    :return: a tuple with the arguments and the return type (in that order)
    :raises ValueError: if the signature is invalid
    """

    args = list()
    i = 0
    descriptor = ""
    last_arg_index = signature.index(")")
    while i < last_arg_index:
        char = signature[i]
        i += 1
        if char == "(":
            continue  # Skip the paran
        elif char == "L":
            descriptor += char
            while True:
                if char == ")":
                    raise ValueError("Reached end of args without object completion " + signature)
                char = signature[i]
                i += 1
                descriptor += char
                if char == ";":
                    break
            args.append(types.parse_descriptor(descriptor))
            descriptor = ""  # Reset the descriptor
        elif char == "[":
            descriptor += char
        else:
            descriptor += char
            args.append(types.parse_descriptor(descriptor))
            descriptor = ""  # Reset the descriptor
    i += 1  # Skip the ')'
    return_type = types.parse_descriptor(signature[i:])
    return args, return_type

def parse_field(name):
    """
    Parse the field with the given name (in asm format) into a FieldData object

    :param name: the asm-format name
    :return: the field as a FieldData object
    :raises ValueError: if the data is invalid
    """
    name = _split_name(name)
    return FieldData(type=types.parse_internal_name(name[0]), name=name[1])


def _split_name(name):
    i = name.rindex("/")
    member_name = name[i + 1:]
    type_name = name[:i]
    return type_name, member_name
