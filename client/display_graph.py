from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

def display_graph_func():
    return communicate_server.post("display-graph", {})

display_graph = (Form(display_graph_func, name="Display Graph", desc=markdown(u"""
Display Graph
=============
Show the directed acyclic graph of all nodes.
"""))
)
