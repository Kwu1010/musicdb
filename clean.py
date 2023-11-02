import os

def clean(path):
    for x in os.listdir(path):
        if os.path.isdir(x):
            clean(path + "/" + x)
        elif x.endswith(".class"):
            os.remove(path + "/" + x)

clean(".")