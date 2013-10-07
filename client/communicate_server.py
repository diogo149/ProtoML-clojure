import requests
from os import path

SERVER_URL = "http://localhost:3000"

def post(route, data):
    r = requests.post(path.join(SERVER_URL, route), data=data)
    try:
        return r.json()
    except ValueError, e:
        return {"error": r.text}
