from typing import Iterator

from srg.mappings import AbstractMappings


def serialize_srg(mappings: AbstractMappings) -> Iterator[str]:
    """
    A generator that serializes the lines

    I'm not sure if this documentation is correct, since i'm not quite used to thinking of methods as object

    :param mappings: the mappings to serialize
    :return: a generator that serializes the mappings
    :raises ValueError: if the mappings are invalid
    """
    for original, renamed in mappings.classes():
        yield " ".join([
            "CL:",
            original.get_internal_name(),
            renamed.get_internal_name()
        ])
    for original, renamed in mappings.fields():
        yield " ".join([
            "FD:",
            original.get_internal_name(),
            renamed.get_internal_name()
        ])
    for original, renamed in mappings.methods():
        yield " ".join([
            "MD:",
            # Original method
            original.get_internal_name(),
            original.get_method_signature(),
            # Renamed method
            renamed.get_internal_name(),
            renamed.get_method_signature()
        ])
