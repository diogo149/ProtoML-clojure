from sys import argv
from re import findall

def parse():
    parsed = {}
    for args in argv[1:]:
        key, value = re.findall("^--(.*)=(.*)$")[0]
        value = parse_value(value)
        parsed[key] = value
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
