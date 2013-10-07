from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def delete_node_func(transform_id, data_namespace):
    request = dict(
        TransformId=transform_id,
        DataNamespace=data_namespace,
    )
    return communicate_server.post("delete-node", request)

delete_node = (Form(delete_node_func, name="Delete Node", desc=markdown(u"""
Delete Node
===========
Removes all data for a node in the data graph, as well as that node's descendants, for a given namespace.
"""))
    + transform_id_form()
    + data_namespace_form()
)
