# Architecture and Toolchain

This document describes the tools used to develop "Bert" and summarizes the construction process, both hardware and software.
In addition we discuss the core-architecture and interfaces between the main components.

***************************************************************
## Table of Contents <a id="table-of-contents"></a>
  * [Source](#source)
  * [Skeleton](#skeleton)
*********************************************************
## Source <a id="source"></a>
[toc](#table-of-contents)
[GenerationRobots](https://www.generationrobots.com/en/278-poppy-humanoid-robot) has provided their Poppy project (www.poppy-project.org)](https://www.poppy-project.org/) in open-source format, both hardware and software. Version 1.0.2 source as at: https://github.com/poppy-project/poppy-humanoid. A full authors reference may be found at: https://github.com/poppy-project/poppy-humanoid/doc/authors.md. The repository
contains full assembly instructions.

*********************************************************
## Skeleton <a id="skeleton"></a>
[toc](#table-of-contents)
The skeletal print-files provided by GenerationRobots are in .STL format. This form is printable directly, but not conducive to modification.
We make use of the following freely-available CAD applications:
  * [OpenSCAD](http://www.openscad.org) - Construct geometric parts in script. Export in .stl format.
  * [MeshLab](http://www.meshlab.net) - Use this tool to optimize and convert between 3D formats.
  * [Sculptris](http://pixologic.com/sculptris) - Sculpt objects free-form. This is useful for molding irregular, rounded or textured parts. Export in .obj format.

The following sections describe modifications to the original parts.

#### Head <a id="skeleton-head"></a>
#### Torso <a id="skeleton-torso"></a>
#### Legs <a id="skeleton-legs"></a>
#### Arms <a id="skeleton-arms"></a>
