from srg.output import serialize_srg
from srg.parser import parse_srg

with open('joined.srg') as file:
    lines = file.read().splitlines(keepends=False)

mappings = parse_srg(lines)

output_text = list(serialize_srg(mappings))