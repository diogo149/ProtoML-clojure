# -*- coding: utf-8 -*-
from __future__ import print_function

from formcreator import MainApp

from new_node import new_node
from train import train
from evaluate import evaluate
from manual_input import manual_input
from delete_node import delete_node
from node_search import node_search
from transform_search import transform_search
from display_graph import display_graph


if __name__ == "__main__":
    MainApp('ProtoML', [
        new_node,
        train,
        evaluate,
        train,
        manual_input,
        delete_node,
        node_search,
        transform_search,
        display_graph,
    ], not_public=False).run()
