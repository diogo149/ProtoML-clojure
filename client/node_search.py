from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def node_search_func(query):
    request = dict(
        Query=query,
    )
    return communicate_server.post("node-search", request)

node_search = (Form(node_search_func, name="Node Search", desc=markdown(u"""
Node Search
===========
Search for a node in the input graph.
"""))
    + query_form()
)
