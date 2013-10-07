from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def new_node_func(transform, data_ids, parameters, random_seed, alias, tags):
    request = dict(
        Transform=transform,
        Data=data_ids.split(),
        Parameters=parameters,
        RandomSeed=random_seed,
        Alias=alias,
        Tags=tags.split(),
    )
    return communicate_server.post("new-node", request)

new_node = (Form(new_node_func, name="New Node", desc=markdown(u"""
New Node
========
Adds a new node to the data graph.
"""))
    + transform_form()
    + data_ids_form()
    + parameters_form()
    + random_seed_form()
    + alias_form()
    + tags_form()
)
