import re
from typing import Dict, Iterable, Tuple

from srg.types import Type


class MethodData(object):
    def __init__(self, type: Type, name: str, args: Iterable[Type], return_type: Type):
        """
        Create a method data object

        :param type: the type containing the method
        :param name: the name of the method
        :param args: the method argument types
        :param return_type: the method return types
        """
        if not is_valid_identifier(name):
            raise ValueError("Invalid name: " + name)
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

    def __repr__(self):
        return self.get_internal_name() + " " + self.get_method_signature()


class FieldData(object):
    def __init__(self, type: Type, name: str):
        """
        Create field data

        :param type: the type containing the field
        :param name: the name of the field
        """
        if not is_valid_identifier(name):
            raise ValueError("Invalid name: " + name)
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

    def __repr__(self):
        return self.get_internal_name()

from srg.mappings import AbstractMappings


class Mappings(AbstractMappings):
    """
    Java obfuscation mappings

    Contains the information to map one set of source identifiers to another
    """

    def __init__(self, class_mappings: Dict[Type, Type], field_mappings: Dict[FieldData, FieldData], method_mappings: Dict[MethodData, MethodData]):
        """
        Create a new mapping

        :param class_mappings: a dictionary containing mappings from the old class names to the new ones
        :param field_mappings: a dictionary containing mappings from the old fields to the new ones
        :param method_mappings: a dictionary contianing mappings from the old methods to the new ones
        """
        self._classes = class_mappings
        self._fields = field_mappings
        self._methods = method_mappings

    def get_class(self, original: Type) -> Type:
        try:
            return self._classes[original]
        except KeyError:
            return super().get_class(original)

    def get_method(self, original: MethodData) -> MethodData:
        try:
            return self._methods[original]
        except KeyError:
            return super().get_method(original)

    def get_field(self, original: FieldData) -> FieldData:
        try:
            return self._fields[original]
        except KeyError:
            return super().get_field(original)

    def copy(self):
        return Mappings(self._classes.copy(), self._fields.copy(), self._methods.copy())

    def methods(self) -> Iterable[Tuple[MethodData, MethodData]]:
        return self._methods.items()

    def fields(self) -> Iterable[Tuple[FieldData, FieldData]]:
        return self._fields.items()

    def classes(self) -> Iterable[Tuple[Type, Type]]:
        return self._classes.items()

    def set_method(self, original: MethodData, renamed: MethodData):
        self._methods[original] = renamed

    def set_class(self, original: Type, renamed: Type):
        self._classes[original] = renamed

    def set_field(self, original: FieldData, renamed: FieldData):
        self._fields[original] = renamed


################
## Validation ##
################

# String Validator

_identifier_regex = re.compile("[\w_<>\$]+")
_type_regex = re.compile('([\w_]+[\$\.])*([\w_]+)')
_package_regex = re.compile('([\w_]+\.)*([\w_]+)?')


def is_valid_identifier(name):
    """
    Ensures the given name is a valid java identifier

    Java identifiers are used for field or method names

    :param name: the name to check
    :return: if valid
    """
    return _identifier_regex.fullmatch(name) is not None


def is_valid_type(type):
    """
    Ensures the given name is a valid class name

    :param type: the name to validate
    :return: true if valid
    """
    return _type_regex.fullmatch(type) is not None


def is_valid_package(name):
    """
    Ensures the given package name is valid

    :param name: the name to check
    :return: if valid
    """
    return _package_regex.fullmatch(name) is not None
