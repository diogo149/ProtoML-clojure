from formcreator import Text, Integer, TextArea, File

def transform_id_form():
    return Text("Transform Id", "The unique id / alias for a node corresponding to a transform, its input, and its parameters.", cmd_opt="transform_id")

def data_namespace_form():
    return Text("Data Namespace", "The namespace of the data to be used for the input of the transform.", cmd_opt="data_namespace")

def train_namespace_form():
    return Text("Train Namespace", "The namespace of the data for the transform and its ancestors to be trained on.", cmd_opt="train_namespace")

def alias_form():
    return Text("Alias", "Optional. A unique shortcut that can be used to refer to this node.", cmd_opt="alias")

def tags_form():
    return Text("Tags", "Optional. White space separated tags to aid search.", cmd_opt="tags")

def query_form():
    return Text(cmd_opt="query")

def random_seed_form():
    return Integer("Random Seed", "Seed for initialization of random number generators.", cmd_opt="random_seed")

def parameters_form():
    return TextArea("Parameters", "JSON encoded input parameters to the transform.", cmd_opt="parameters")

def data_ids_form():
    return Text("Data Ids", "Whitespace separated data ids as input to the transform. Note that order matters.", cmd_opt="data_ids")

def transform_form():
    return Text("Transform", "The name of the transform to apply.", cmd_opt="transform")

def input_data_form():
    return File("Input Data", "External data to add as input.", upload_directory="uploads", cmd_opt="input_data")

def data_type_form():
    return Text("Data Type", "The major exclusive type of the input data. (e.g. Categorical, Ordinal, etc.)", cmd_opt="data_type")

def number_columns_form():
    return Integer("Number of Columns", "Have -1 for not specified, 0 for none, or a positive number for a specific number.", cmd_opt="number_columns")
