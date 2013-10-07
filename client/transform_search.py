from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def transform_search_func(query):
    request = dict(
        Query=query,
    )
    return communicate_server.post("transform-search", request)

transform_search = (Form(transform_search_func, name="Transform Search", desc=markdown(u"""
Transform Search
================
Search for an available transform.
"""))
    + query_form()
)
