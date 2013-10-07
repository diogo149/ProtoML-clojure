from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def manual_input_func(input_data, data_namespace, data_type, number_columns, alias, tags):
    request = dict(
        Filepath=input_data,
        DataNamespace=data_namespace,
        Type=data_type,
        NCols=number_columns,
        Alias=alias,
        Tags=tags.split(),
    )
    return communicate_server.post("manual-input", request)

manual_input = (Form(manual_input_func, name="Manual Input", desc=markdown(u"""
Manual Input
============
Adds external data as input to the data graph of a given namespace.
"""))
    + input_data_form()
    + data_namespace_form()
    + data_type_form()
    + number_columns_form()
    + alias_form()
    + tags_form()
)
