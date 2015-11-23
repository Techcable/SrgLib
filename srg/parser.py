from srg import types, Mappings
from srg import MethodData, FieldData


def parse_srg_file(filename):
    """
    Read the srg file and parses into a Mappings object

    A utility function wrapping 'parse_srg'

    :param filename: the file to read from
    :return: the parsed data
    :raises ValueError: if parsing error occurs
    :raises OSError: if an error occurs reading the file
    """
    with open(filename, "rt") as file:
        return parse_srg(file.readlines())


def parse_srg(lines):
    """
    Parse the lines of a srg file into a Mappings object

    Package data is not parsed

    :param lines: the lines of the srg file
    :return: the parsed Mappings object
    :raises ValueError: if parsing error occurs
    """
    methods = dict()
    fields = dict()
    classes = dict()
    for line in lines:
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


def parse_method(name, signature):
    """
    Parse the method with the given name and signature into a MethodData object

    :param name: the asm-format name
    :param signature: the asm-format signature
    :return: the method as a MethodData object
    :raises ValueError: if the data is invalid
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
    name = _split_name(name)
    return MethodData(type=types.parse_internal_name(name[0]), name=name[1], args=args, return_type=return_type)


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
