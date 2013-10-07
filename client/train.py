from formcreator import Form, markdown

from forms import *
import communicate_server
from utils import pformat

@pformat
def train_func(transform_id, data_namespace):
    request = dict(
        TransformId=transform_id,
        DataNamespace=data_namespace,
    )
    return communicate_server.post("train", request)

train = (Form(train_func, name="Train", desc=markdown(u"""
Train
=====
Trains a transform in the data graph, with the input namespace. This operation automatically trains and evaluates it's ancestors in the data graph. This operation is idempotent.
"""))
    + transform_id_form()
    + data_namespace_form()
)
