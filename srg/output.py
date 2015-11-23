def serialize_srg(mappings):
    """
    A generator that serializes the lines

    I'm not sure if this documentation is correct, since i'm not quite used to thinking of methods as object

    :param mappings: the mappings to serialize
    :return: a generator that serializes the mappings
    :raises ValueError: if the mappings are invalid
    """
    for original, renamed in mappings.methods.items():
        yield " ".join([
            "MD:",
            # Original method
            original.get_internal_name(),
            original.get_method_signature(),
            # Renamed method
            renamed.get_internal_name(),
            renamed.get_method_signature()
        ])
    for original, renamed in mappings.fields.items():
        yield " ".join([
            "FD:",
            original.get_internal_name(),
            renamed.get_internal_name()
        ])
    for original, renamed in mappings.classes.items():
        yield " ".join([
            "CL:",
            original.get_internal_name(),
            renamed.get_internal_name()
        ])