from sys import argv
from re import findall

def parse():
    parsed = {}
    inputs = []
    outputs = []
    for arg in argv[1:]:
        key, value = findall("^--(.*)=(.*)$", arg)[0]
        value = parse_value(value)
        if key == "input":
            inputs.append(value)
        elif key == "output":
            outputs.append(value)
        else:
            parsed[key] = value
    parsed["input"] = inputs
    parsed["output"] = outputs
    return parsed


def parse_value(s):
    # tries to parse a value from a string
    if s.lower() == "true":
        return True
    elif s.lower() == "false":
        return False


    try:
        return int(s)
    except ValueError:
        pass

    try:
        return float(s)
    except ValueError:
        pass

    return s
