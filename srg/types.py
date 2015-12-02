from enum import Enum, unique

import srg


class Type(object):
    """
    A java class

    By default represents a simple type, like java.lang.String
    Arrays are represented by the subclass ArrayType
    Primitives are represented by the subclass PrimitiveType
    """

    def __init__(self, name=None):
        """
        Create a new simple java class, with the given name

        If the name is none, the type is 'unnamed', and methods will raise NotImplementedError if not overridden

        This method should not be used directly, instead prefer parse_name()
        :param name: the name of the type, in the format returned by Class.getName()
        :raises ValueError: if the name is invalid
        """
        if name is not None:
            if not srg.is_valid_type(name):
                raise ValueError("Invalid name " + name)
            self._name = name

    def get_internal_name(self):
        """
        The internal name of a class is its fully qualified name (as returned by Class.getName(),
        where '.' are replaced by '/'.

        For primitives, this is identical to get_name()

        :return: the internal name
        """
        return self.get_name().replace(".", "/")

    def get_descriptor(self):
        """
        Get the bytecode descriptor of this type

        :return: this type's bytecode descriptor
        """
        return 'L' + self.get_internal_name() + ";"

    def get_name(self):
        """
        Get the name of this type, like that returned by Class.getName()

        :return: the name of this type
        """
        if not hasattr(self, "_name"):
            raise NotImplementedError("This type has not been named")
        return self._name

    def get_simple_name(self):
        """
        Get the name of this type, with the package removed

        Does nothing if this type has no package

        :return: this type's 'simple name'
        """
        if self.is_reference_type():
            index = self.get_name().rfind(".")
            if index > 0:
                return self.get_name()[index + 1:]
            else:
                return self.get_name()
        else:
            return self.get_name()

    def is_primitive(self):
        """
        Return if this type is a primitive

        :return: if this type is a primitive
        """
        return type(self) is PrimitiveKind

    def is_array(self):
        """
        Return if this type is an array

        :return: if it is an array
        """
        return type(self) is ArrayType

    def is_reference_type(self):
        """
        Return if this type is a reference type

        :return: if this is a reference type
        """
        return type(self) is Type

    ## Methods only for reference types

    def _assert_reference(self):
        if not self.is_reference_type():
            raise TypeError("Must be a reference type")

    def get_package(self):
        """
        Get the name of this type's package

        :return: this type's packaage
        :raises TypeError: if not a reference type
        """
        self._assert_reference()
        index = self.get_name().rfind(".")
        if index < 0:
            return ""
        else:
            return self.get_name()[:index]

    ## Overrides

    def __hash__(self):
        return hash(self.get_name())

    def __eq__(self, other):
        return isinstance(other, Type) and other.get_name() == self.get_name()

    def __str__(self):
        return self.get_name()

    def __repr__(self):
        return str(self)


class PrimitiveType(Type):
    def __init__(self, kind):
        """
        Create a new primitive type

        :param kind: the kind of this primitive
        """
        super(PrimitiveType, self).__init__()
        self._kind = kind

    def get_kind(self):
        """
        Get the type of this primitive

        :return: the type
        """
        return self._kind

    def get_descriptor(self):
        return self.get_kind().get_descriptor()

    def get_name(self):
        return self.get_kind().name.lower()

    def get_internal_name(self):
        return self.get_name()


class ArrayType(Type):
    def __init__(self, element_type):
        """
        Create a new array type with the specified array type

        :param element_type: the element type of the array
        """
        super(ArrayType, self).__init__()
        self._type = element_type

    def get_element_type(self):
        """
        Get the element type of this array

        :return: the element type of the array
        """
        return self._type

    def get_name(self):
        return self.get_element_type().get_name() + "[]"

    def get_descriptor(self):
        return "[" + self.get_element_type().get_descriptor()

    def get_internal_name(self):
        return self.get_element_type().get_internal_name() + "[]"


@unique
class PrimitiveKind(Enum):
    """
    An enumeration of java's 8 primitive types and 'VOID'

    void is actually a valid java type (believe it or not)
    """

    BYTE = 'B'
    SHORT = 'S'
    INT = 'I'
    LONG = 'J'
    FLOAT = 'F'
    DOUBLE = 'D'
    CHAR = 'C'
    BOOLEAN = 'Z'
    VOID = 'V'

    def get_descriptor(self):
        """
        Get the bytecode descriptor of this primitive type

        :return: the bytecode descriptor of this primitive type
        """
        return self.value

    @staticmethod
    def from_descriptor(descriptor):
        return PrimitiveKind(descriptor)


def parse_internal_name(name):
    """
    Parse the internal name

    :param name: the internal name of the type
    :return: the name represented as a type
    :raises ValueError: if the internal name can't be parsed
    """
    return parse_name(name.replace("/", "."))

def parse_name(name):
    """
    Parse the given name into a Type

    :param name: the name of the type
    :return: the name represented as a type
    :raises ValueError: if the name can't be parsed
    """
    # Sanity
    if len(name) is 0:
        raise ValueError("Empty name")
    if len(name.replace("[]", "")) == 0:
        raise ValueError("Array of nothing")
    # Handle arrays
    if name.endswith("[]"):
        try:
            return ArrayType(parse_name(name[-2]))
        except ValueError:
            raise ValueError("Couldn't parse name " + name)
    # Parse primitives and classes
    try:
        return PrimitiveKind[name.upper()]
    except:
        pass
    if srg.is_valid_type(name):
        return Type(name)
    # Unable to parse
    raise ValueError("Couldn't parse name " + name)


def parse_descriptor(descriptor):
    """
    Parse the given bytecode descriptor into a type

    :param descriptor: the bytecode descriptor of the type
    :return: the descriptor represented as a type
    :raises ValueError: if unable to parse
    """
    # Sanity
    if len(descriptor) is 0:
        raise ValueError("Empty name")
    if len(descriptor.replace("[", "")) == 0:
        raise ValueError("Array of nothing")
    # Handle arrays
    if descriptor.startswith("["):
        try:
            return ArrayType(parse_descriptor(descriptor[1:]))
        except ValueError:
            raise ValueError("Couldn't parse descriptor " + descriptor)
    if descriptor.startswith("L") and srg.is_valid_type(descriptor[1:-1].replace("/", ".")):
        return Type(descriptor[1:-1].replace('/', "."))
    primitive = PrimitiveKind.from_descriptor(descriptor)
    if primitive is not None:
        return PrimitiveType(primitive)
    # Unable to parse
    raise ValueError("Couldn't parse descriptor " + descriptor)
