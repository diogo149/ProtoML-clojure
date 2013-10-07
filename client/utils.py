import json
import pprint

def pprint_json(m):
  return json.dumps(m, sort_keys=True, indent=2, separators=(',', ': '))


def pformat(func):
    def wrapped(*args, **kwargs):
        result = func(*args, **kwargs)
        return pprint.pformat(result)
    return wrapped
