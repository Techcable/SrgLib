import re
from collections import namedtuple

from srg.types import Type


class MethodData(object):
    def __init__(self, type, name, args, return_type):
        """
        Create a method data object

        :param type: the type containing the method
        :param name: the name of the method
        :param args: the method argument types
        :param return_type: the method return types
        """
        self.type = type
        self.name = name
        self.args = tuple(args)
        self.return_type = return_type

    def get_internal_name(self):
        """
        Get the internal name of this method

        Internal mehod names are in the format ${internal type name}/${method name}

        :return: the internal method name
        """
        return self.type.get_internal_name() + "/" + self.name

    def get_method_signature(self):
        """
        Get the method signature in bytecode format

        :return: this method's signature
        """
        signature = "("
        for arg in self.args:
            signature += arg.get_descriptor()
        signature += ")"
        signature += self.return_type.get_descriptor()
        return signature

    def __hash__(self):
        return hash((self.get_internal_name(), self.args, self.return_type))

    def __eq__(self, other):
        return isinstance(other, MethodData)\
               and self.get_internal_name() == other.get_internal_name()\
               and self.args == other.args\
               and self.return_type == other.return_type

class FieldData(object):
    def __init__(self, type, name):
        """
        Create field data

        :param type: the type containing the field
        :param name: the name of the field
        """
        self.type = type
        self.name = name

    def get_internal_name(self):
        """
        Get the internal name of this field

        Internal mehod names are in the format ${internal type name}/${field name}

        :return: the internal method name
        """
        return self.type.get_internal_name() + "/" + self.name

    def __hash__(self):
        return hash(self.get_internal_name())

    def __eq__(self, other):
        return isinstance(other, FieldData) and self.get_internal_name() == other.get_internal_name()

class Mappings:
    """
    Java obfuscation mappings

    Contains the information to map one set of source identifiers to another
    """

    def __init__(self, class_mappings, field_mappings, method_mappings):
        """
        Create a new mapping

        :param class_mappings: a dictionary containing mappings from the old class names to the new ones
        :param field_mappings: a dictionary containing mappings from the old fields to the new ones
        :param method_mappings: a dictionary contianing mappings from the old methods to the new ones
        """
        for original, renamed in class_mappings.items():
            if type(original, ) != Type:
                raise ValueError(original + " is not a valid type")
            if type(renamed) != Type:
                raise ValueError(renamed + " is not a valid type")
        for original, renamed in field_mappings.items():
            validate_field(original)
            validate_field(renamed)
        for original, renamed in method_mappings.items():
            validate_method(original)
            validate_method(renamed)
        self.classes = class_mappings
        self.fields = field_mappings
        self.methods = method_mappings

    def get_class(self, original):
        """
        Get the new class name

        Returns the original name if no mapping is found

        :param original: the original class name
        :return: the new class name
        """
        renamed = self.classes[original]
        if renamed is None:
            return original
        else:
            return renamed

    def get_method(self, original):
        """
        Get the new method data

        Returns the original name if no mapping is found

        :param original: the original method data
        :return: the new method data
        """
        renamed = self.methods[original]
        if renamed is None:
            return original
        else:
            return renamed

    def get_field(self, original):
        """
        Get the new field data

        Returns the original name if no mapping is found

        :param original: the original field data
        :return: the new field data
        """
        renamed = self.fields[original]
        if renamed is None:
            return original
        else:
            return renamed

    def rename_package(self, original_package, renamed_package):
        """
        Rename all classes with the original package name

        :param original_package: the original package name
        :param renamed_package: the renamed package name
        :return: the old mappings
        """
        old = self.copy()
        for original_class, renamed_class in self.classes.items():
            if original_class.get_package() != original_package:
                continue
            if len(renamed_package) > 0:
                renamed_class = Type(renamed_package + "." + renamed_class.get_simple_name())
            else:
                renamed_class = Type(renamed_class.get_simple_name())
            self.classes[original_class] = renamed_class

        return old

    def invert(self):
        """
        Invert the mappings, switching the original and renamed
        The inverted mappings are a copy of the original mappings

        :return: the inverted mappings
        """
        inverted_classes = {renamed: original for original, renamed in self.classes.items()}
        inverted_fields = {renamed: original for original, renamed in self.fields.items()}
        inverted_methods = {renamed: original for original, renamed in self.methods.items()}
        return Mappings(inverted_classes, inverted_fields, inverted_methods)

    def copy(self):
        return Mappings(self.classes.copy(), self.fields.copy(), self.methods().copy())


################
## Validation ##
################


def validate_field(field):
    """
    Asserts that the given field is valid

    :param field: the field to check
    :raises ValueError: if the specified name is invalid
    """
    if type(field) is not FieldData:
        raise ValueError(str(field) + " must be field data")
    if not is_valid_type(field.type):
        raise ValueError(field.type + " is not a valid class name for field: " + str(field))
    if not is_valid_identifier(field.name):
        raise ValueError(field.name + " is not a valid field name for field: " + str(field))


def validate_method(method):
    """
    Asserts that the given field is valid

    :param method: the field to check
    :raises ValueError: if the specified name is invalid
    """
    if type(method) is not MethodData:
        raise ValueError(str(method) + " must be method data")
    if not is_valid_type(method.type):
        raise ValueError(method.type + " is not a valid class name for method: " + str(method))
    if not is_valid_identifier(method.name):
        raise ValueError(method.name + " is not a valid method name for method: " + str(method))
    for paramType in method.args:
        if not is_valid_type(paramType):
            raise ValueError(paramType + " is not a valid paramater type for method: " + str(method))
    if not is_valid_type(method.return_type):
        raise ValueError(method.return_type + " is not a valid return type for method: " + str(method))


# String Validator

_identifier_regex = re.compile("[\w_\$]+")
_type_regex = re.compile('([\w_]+[\$\.])*([\w_]+)')
_package_regex = re.compile('([\w_]+\.)*([\w_]+)')


def is_valid_identifier(name):
    """
    Ensures the given name is a valid java identifier

    Java identifiers are used for field or method names

    :param name: the name to check
    :return: if valid
    """
    return _identifier_regex.match(name) is not None


def is_valid_type(type):
    """
    Ensures the given name is a valid class name

    :param type: the name to validate
    :return: true if valid
    """
    return isinstance(type, Type) or _type_regex.match(type) is not None


def is_valid_package(name):
    """
    Ensures the given package name is valid

    :param name: the name to check
    :return: if valid
    """
    return _package_regex.match(name) is not None
