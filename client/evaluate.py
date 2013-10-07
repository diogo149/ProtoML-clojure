from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def evaluate_func(transform_id, data_namespace, train_namespace):
    request = dict(
        TransformId=transform_id,
        DataNamespace=data_namespace,
        TrainNamespace=train_namespace,
    )
    return communicate_server.post("evaluate", request)

evaluate = (Form(evaluate_func, name="Evaluate", desc=markdown(u"""
Evaluate
========
Evaluates an input node in the data graph, for the input namespace, using the model trained in the input training namespace. This operation automatically evaluates it's ancestors in the data graph. This operation is idempotent.
"""))
    + transform_id_form()
    + data_namespace_form()
    + train_namespace_form()
)
