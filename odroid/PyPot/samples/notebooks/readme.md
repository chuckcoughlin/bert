# Notebooks everywhere

In the [Poppy Project](https://www.poppy-project.org), we are huge fans of [Jupyter Notebooks](http://jupyter.org) both for development and for documentation. Most of our tests, benchs, experiments, and even debugs are now done using notebooks. Thus, we have decided that pypot's documentation and more generally all the software documentation related to the [Poppy Project](https://www.poppy-project.org) will smoothly move towards notebooks.

We strongly believe that they have an amazing potential:
* for writing pedalogical contents - by **mixing code, explanation, results in an integrated form**
* for easily sharing - they can be easily **review online** and then **run locally**
* they can also be run remotely on the robot and thus **no installation at all is required on the client side, you just need a web browser**

We already have written a few tutorial notebooks that you can browse here: https://poppy-project.gitbooks.io/poppy-docs/content/en/programming/notebooks.html

Other notebooks should shorty extend this list!

In the next sections, we decribe how those notebooks can be [viewed](https://github.com/poppy-project/pypot/blob/master/samples/notebooks/readme.md#browse-notebook-online), [installed](https://github.com/poppy-project/pypot/blob/master/samples/notebooks/readme.md#running-notebooks-locally), [run on your robot](https://github.com/poppy-project/pypot/blob/master/samples/notebooks/readme.md#connecting-to-a-remote-notebook) or [locally](https://github.com/poppy-project/pypot/blob/master/samples/notebooks/readme.md#running-notebooks-locally), shared and how you can [contribute](https://github.com/poppy-project/pypot/blob/master/samples/notebooks/readme.md#contribute) by writting your own notebooks!

# Open Source
All the resources content from the Poppy Project is open source. This naturally also extends to the notebooks.

  License     |     Notebooks    |   Library      |
| ----------- | :-------------: | :-------------: |
| Name  | [Creatives Commons BY-SA](http://creativecommons.org/licenses/by-sa/4.0/)  |[GPL v3](http://www.gnu.org/licenses/gpl.html)  |
| Logo  | [![Creative Commons BY-SA](https://i.creativecommons.org/l/by-sa/4.0/88x31.png) ](http://creativecommons.org/licenses/by-sa/4.0/)  |[![GPL V3](https://www.gnu.org/graphics/gplv3-88x31.png)](http://www.gnu.org/licenses/gpl.html)  |

# Browse Notebook Online

One of the great features of the Notebook is that they can be read directly online without needing to install anything on your laptop. The **only thing you need is a web browser!**

Thanks to the [nbviewer](http://nbviewer.ipython.org) website, you can read any Notebook online. You can also directly view them on [Github](https://github.com). The Notebooks from the Poppy Project can be directly found [here](http://nbviewer.ipython.org/github/poppy-project/pypot/tree/master/samples/notebooks/). A list of the "best" Notebook is also maintained [here](https://poppy-project.gitbooks.io/poppy-docs/content/en/programming/notebooks.html).

If you want to run notebooks yourself and play with the code you will have to use one of the other techniques described below.

# Connecting to a Remote Notebook

The Poppy Creature we developed are usually provided with an embedded board (e.g. a [raspberry pi](http://www.raspberrypi.org)) with all the software tools needed to run [pypot](https://github.com/poppy-project/pypot) already installed - this means a python interpreter, the pypot and poppy pacakges and [Jupyter Notebook](http://jupyter.org) server. Thus, you do not need to install anything specific on your personal computer to connect to a remote Notebook.

When plugged, the embedded board of the Poppy Creature should automatically start a Jupyter Notebook server providing access to all the notebooks tutorials.

Assuming that you creature is connected to the same network as your computer (see [here](https://github.com/pierre-rouanet/rasp-poppy) for details), they can be accessed just by connecting to an url such as: http://poppy.local.

**It's important to note here that the notebook will actually run on the embedded board and not on your local machine!** You will thus not be able to easily access a file on your machine using this approach. Similarly, if you need to install an extra python package, you will have to install it directly on the board (e.g. using an [ssh connection](https://github.com/poppy-project/raspoppy)).

# Running Notebooks Locally

If you want to run Notebooks on your local machine, because you are working with the simulator for instance, you will have to start the jupyter notebook server yourself.

To do this, you will need to find your way in the sometimes [confusing](http://captiongenerator.com/30052/Hitler-reacts-to-the-Python-ecosystem) [python packaging system](https://python-packaging-user-guide.readthedocs.org/en/latest/current.html). In details, you will need (this is not the only way to install all the tools but this is probably the most straightforward):
* a [python](https://www.python.org) interpreter (we tested with *2.7*, *3.4* or *pypy-2.5*) see how to install it on your os on [python website](https://www.python.org/downloads/). You can use pre-packaged Python distribution such as [Anaconda](https://store.continuum.io/cshop/anaconda/) or [Spyder](https://github.com/spyder-ide/spyder).
* the [pip tool](https://pip.pypa.io) for installing Python packages - [this documentation describe how to install pip on your system](https://pip.pypa.io/en/latest/installing.html#install-pip).

Now that you have a working Python environment, you can install the [Jupyter Notebook package](http://jupyter.readthedocs.org/en/latest/install.html) using pip (or conda). You only have to run the following line on a command line terminal:

```bash
pip install jupyter
```

Linux users may have to run (depending on their python installation):
```bash
sudo pip install jupyter
```

Then, you need to install pypot and the software for your creature. For instance, to use a PoppyErgoJr:
```bash
pip install pypot poppy-ergo-jr
```

Finally, you can now run the notebook server from a terminal:
```bash
jupyter notebook
```
You can also specify the folder where your notebooks are, for instance on my machine
```bash
jupyter notebook ~/dev/pypot/samples/notebooks/
```

# Support
The [Poppy forum](forum.poppy-project.org) is the best place to ask for help! You can also use the [github issue tracker](https://github.com/poppy-project/pypot/labels/Notebooks) for specific notebooks issues.

# Contribute
First, please report any bug or issue to our [github issue tracker](https://github.com/poppy-project/pypot/labels/Notebooks).

Second, you can discuss, suggest, or even better provide us with other Notebooks. We hope to gather as many and as diversified Notebooks as possible. They could be pedagogical content, experiments or just funny behaviours for robotic creatures. The easiest way to share them with us is trough pull requests.

Finally, as we are not native English speaker [your](http://www.troll.me/images2/grammar-correction-guy/your-youre-welcome.jpg) encouraged to report any spell checking or any sentence which does not make much sense ^^. Translation to other language are also welcomed!
