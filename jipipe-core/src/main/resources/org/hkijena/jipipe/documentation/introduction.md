JIPipe is a graphical batch processing programming language for *ImageJ*. It comes with an easy-to-use graphical macro editor that requires no programming knowledge. 
Image processing steps can be added to the user interface and connected to form powerful and complex pipelines that can be easily scaled up and down.

To create a pipeline, switch to one of the already open tabs at the top. If you create a new project, there is
always one tab to manage <img src="image://icons/connect.png"/> <strong>Graph compartments</strong> and multiple 
<img src="image://icons/graph-compartment.png"/> <strong>Compartment</strong> items.
Compartments are a way to organize large pipelines into smaller sub-graphs. You can create as many compartments as you want 
and pass data between them. Please take a look at out online tutorials for step-by-step explanations.

On switching to a <img src="image://icons/graph-compartment.png"/> <strong>Compartment</strong>, you can add algorithms 
into the pipeline. You can find them in the menu or via the <i>Search ...</i> bar. Each algorithm has a set of input and
output <i>slots</i>. Inputs must always have a connection, while outputs can be left alone. JIPipe will automatically save all
generated outputs. 

If you select an algorithm, you can find its parameters on the right-hand side. They allow you to set the name, give a description,
and control the algorithm behavior.

JIPipe is a batch-processing language, meaning that algorithms always work on multiple data items at once. At the beginning, 
we recommend to import your input files or folders by dragging them into a compartment area or by adding file/folder
lists manually via their respective algorithms. You can find all kind of input algorithms in the <img src="image://icons/database.png"/> <strong>Add data</strong> 
menu.

To run a pipeline, click the <img src="image://icons/run.png"/> <strong>Run</strong> button at the top-right corner.
You can also run only a part of the pipeline via the <img src="image://icons/play.png"/> <strong>Quick Run</strong>
feature that can be set up via right-clicking an algorithm node or selecting it.

You can find more detailed descriptions and written/video tutorials, as well as examples online on our website.

**Thank you for using JIPipe!**

<p style="margin-top: 40px;"></p>
<h1>Copyright</h1>
<hr/>

Copyright by Zoltán Cseresnyés, Ruman Gerst

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge

https://www.leibniz-hki.de/en/applied-systems-biology.html

HKI-Center for Systems Biology of Infection

Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)

Adolf-Reichwein-Straße 23, 07745 Jena, Germany


<p style="margin-top: 40px;"></p>
<h1>License</h1>
<hr/>

BSD 2-Clause License

Copyright (c) 2019, Zoltán Cseresnyés, Ruman Gerst
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
