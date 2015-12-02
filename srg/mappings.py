from typing import T, Dict, Iterable, Tuple, TypeVar, Callable, Optional
import srg
from srg import MethodData, FieldData
from srg.types import Type


class AbstractMappings(object):
    def get_class(self, original: Type) -> Type:
        """
        Get the new class name

        Returns the original name if no mapping is found

        :param original: the original class name
        :return: the new class name
        """
        return original

    def get_method(self, original: MethodData) -> MethodData:
        """
        Get the new method data

        Returns the original name remapped to the new classes if no mapping is found

        :param original: the original method data
        :return: the new method data
        """
        type = self.get_class(original.type)
        name = original.name
        args = (self.get_class(original_arg) for original_arg in original.args)
        return_type = self.get_class(original.return_type)
        return MethodData(type, name, args, return_type)

    def get_field(self, original: FieldData) -> FieldData:
        """
        Get the new field data

        Returns the original name remapped to the new classes if no mapping is found

        :param original: the original field data
        :return: the new field data
        """
        return FieldData(self.get_class(original.type), original.name)

    def set_class(self, original: Type, renamed: Type):
        """
        Set a class's new name

        :param original: the original class name
        :param renamed: the class's new name
        :raises NotImplementedError: if the mappings are immutable
        """
        raise NotImplementedError()

    def set_method(self, original: MethodData, renamed: MethodData):
        """
        Set a method's new name

        :param original: the original method name
        :param renamed: the method's new name
        """
        raise NotImplementedError()

    def set_field(self, original: FieldData, renamed: FieldData):
        """
        Set a field's new name

        :param original: the original field name
        :param renamed: the field's new name
        :raises : if the mappings are immutable
        """
        raise NotImplementedError()

    def classes(self) -> Iterable[Tuple[Type, Type]]:
        """
        Get a set of the original classes of the mappings

        :return: the original classes
        """
        raise NotImplementedError()

    def methods(self) -> Iterable[Tuple[MethodData, MethodData]]:
        """
        Get a set of the original methods of the mappings

        :return: the methods classes
        """
        raise NotImplementedError()

    def fields(self) -> Iterable[Tuple[FieldData, FieldData]]:
        """
        Get a set of the original fields of the mappings

        :return: the original classes
        """
        raise NotImplementedError()

    def invert(self):
        """
        Invert the mappings, switching the original and renamed
        The inverted mappings are a copy of the original mappings

        :return: the inverted mappings
        """
        inverted_classes = {renamed: original for original, renamed in self.classes()}
        inverted_fields = {renamed: original for original, renamed in self.fields()}
        inverted_methods = {renamed: original for original, renamed in self.methods()}
        return ImmutableMappings(inverted_classes, inverted_fields, inverted_methods)


class ImmutableMappings(AbstractMappings):
    """Mappings which can't be modified"""

    def __init__(self, class_mappings: Dict[Type, Type], field_mappings: Dict[FieldData, FieldData],
                 method_mappings: Dict[MethodData, MethodData]):
        """
        Create an immutable mapping from the specified dictionaries

        :param class_mappings: a dictionary containing mappings from the old class names to the new ones
        :param field_mappings: a dictionary containing mappings from the old fields to the new ones
        :param method_mappings: a dictionary contianing mappings from the old methods to the new ones
        """
        if class_mappings is not None:
            class_mappings = class_mappings.copy()
        else:
            class_mappings = {}

        if field_mappings is not None:
            field_mappings = field_mappings.copy()
        else:
            field_mappings = {}

        if method_mappings is not None:
            method_mappings = method_mappings.copy()
        else:
            method_mappings = {}

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

    def set_class(self, original: Type, renamed: Type):
        raise RuntimeError("Can't modify immutable mappings")

    def set_method(self, original: MethodData, renamed: MethodData):
        raise RuntimeError("Can't modify immutable mappings")

    def set_field(self, original: FieldData, renamed: FieldData):
        raise RuntimeError("Can't modify immutable mappings")

    def methods(self) -> Iterable[Tuple[MethodData, MethodData]]:
        return self._methods.items()

    def fields(self) -> Iterable[Tuple[FieldData, FieldData]]:
        return self._fields.items()

    def classes(self) -> Iterable[Tuple[Type, Type]]:
        return self._classes.items()

    @staticmethod
    def copy_of(other: AbstractMappings) -> "ImmutableMappings":
        if isinstance(other, ImmutableMappings):
            return other
        return ImmutableMappings(dict(other.classes()), dict(other.fields()), dict(other.methods()))


class ChainedMappings(ImmutableMappings):

    def __init__(self, mappings: Iterable[AbstractMappings], only_first_original=False):
        """
        Chain the mappings together, using the outputed names of one mappings as the input too the next
        :param mappings:
        :param only_first_original: if only the first mapping can define new names, and others must remap old ones
        :raises ValueError: If any of the mappings try to remap the same name twice
                            or (if only_first_original is True) a mapping tries to define a new name
        """
        chained = ImmutableMappings(dict(), dict(), dict())
        originals = dict()
        for mapping in mappings:
            classes = dict()  # Dict[Type, Type]
            fields = dict()  # Dict[FieldData, FieldData]
            methods = dict()  # Dict[MethodData, MethodData]
            inverted = chained.invert()

            for original, renamed in mapping.classes():
                original = inverted.get_class(original)
                if original not in originals:
                    originals[renamed] = original
                    classes[original] = renamed
            for original, renamed in mapping.fields():
                original = inverted.get_field(original)
                if original not in originals:
                    originals[renamed] = original
                    fields[original] = renamed
            for original, renamed in mapping.methods():
                original = inverted.get_method(original)
                if original not in originals:
                    originals[renamed] = original
                    methods[original] = renamed

            for original, renamed in chained.classes():
                renamed = mapping.get_class(renamed)
                classes[original] = renamed
            for original, renamed in chained.fields():
                renamed = mapping.get_field(renamed)
                fields[original] = renamed
            for original, renamed in chained.methods():
                renamed = mapping.get_method(renamed)
                methods[original] = renamed
            chained = ImmutableMappings(classes, fields, methods)

        super().__init__(dict(chained.classes()), dict(chained.fields()), dict(chained.methods()))

    @staticmethod
    def calc_real_originals(chained, mapping):
        originals = dict()

class RenamingMappings(AbstractMappings):
    """
    Mappings which rename classes/methods/fields dynamically

    Unlike other mappings, these mappings have no fields, methods, or classes of their own, they just rename whatever they are given
    Functions are expected to always give the same output for any input
    """

    def __init__(self, class_transformer=None, method_transformer=None, field_transformer=None):
        """
        Create a renaming mapping with the specified transformers

        Using 'None' as a  transformer is equivalent to no transformation

        :param class_transformer: The method to transform the classes
        :param method_transformer: The method to transform the method names
        :param field_transformer:  The method to transform  the field names
        :type class_transformer: None | (Type) -> Type
        :type method_transformer: None | (MethodData) -> str
        :type field_transformer: None | (FieldData) -> str
        """
        self.class_transformer = class_transformer
        self.method_transformer = method_transformer
        self.field_transformer = field_transformer

    def get_field(self, original: FieldData) -> FieldData:
        if self.field_transformer is not None:
            new_name = self.field_transformer(original)
            return FieldData(self.get_class(original.type), new_name)
        else:
            return super().get_field(original)

    def get_method(self, original: MethodData) -> MethodData:
        if self.field_transformer is not None:
            new_name = self.method_transformer(original)
            return MethodData(self.get_class(original.type), new_name, (self.get_class(original_arg) for original_arg in original.args), self.get_class(original.return_type))
        else:
            return super().get_method(original)

    def get_class(self, original: Type) -> Type:
        if original.is_reference_type() and self.class_transformer is not None:
            return self.class_transformer(original)
        else:
            return original

    def methods(self) -> Iterable[Tuple[MethodData, MethodData]]:
        return ()

    def classes(self) -> Iterable[Tuple[Type, Type]]:
        return ()

    def fields(self) -> Iterable[Tuple[FieldData, FieldData]]:
        return ()

    def transform_mappings(self, input: AbstractMappings) -> ImmutableMappings:
        """
        Use the output of the given mappings as the input of these mappings

        :param input: the mappings to use as the input
        :return: the mappings which map the output of the given mappings to the output of these mappings
        """
        classes = dict()  # type: Dict[Type. Type]
        fields = dict()  # type: Dict[FieldData, FieldData]
        methods = dict()  # type: Dict[MethodData,  MethodData]
        for original in dict(input.classes()).values():
            classes[original] = self.get_class(original)
        for original in dict(input.fields()).values():
            fields[original] = self.get_field(original)
        for original in dict(input.methods()).values():
            methods[original]  = self.get_method(original)
        return ImmutableMappings(classes, fields, methods)


class PackageMappings(RenamingMappings):
    """Mappings which remap classes from one package to another"""

    def __init__(self, packages: Dict[str, str]):
        """
        Create mappings which remap all classes in one package into another package

        :param packages: the packages to remap
        """
        def transform_classes(original: Type):
            if original.is_reference_type() and original.get_package() in self.packages:
                new_package = self.packages[original.get_package()]
                if len(new_package) > 0:
                    return Type(new_package + "." + original.get_simple_name())
                else:
                    return Type(original.get_simple_name())
            else:
                return original
        for original, renamed in packages.items():
            if not srg.is_valid_package(original):
                raise ValueError("Invalid package: " + original)
            if not srg.is_valid_package(renamed):
                raise ValueError("Invalid package: " + renamed)
        self.packages = packages
        super().__init__(transform_classes, None, None)
