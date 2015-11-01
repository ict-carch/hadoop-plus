Hadoop+ — A programming framework based on Hadoop which enables user-provided CUDA/OpenCL Map and(or) Reduce functions to be embedded into Hadoop as plugins, therefore CPUs and GPUs can process big data coordinately. 

Copyright 2015 Institute of Computing Technology, Chinese Academy of Sciences
https://github.com/ict-carch/hadoop-plus 

Features
-----------
-Support resource management of CPU cores and GPU cores.
-Manage memory transfer among CPU cores and GPU cores.
-Enable three task running models including ONLY_GPU, ONLY_GPU and BOTH
-Enable users to specify the preferred implementation running on CPU with JAVA (by default) and OMP (OpenMP)
-Provide PMap and PReduce programming interfaces to programmers 

Users APIs
------------
Users can reference the interfaces provided by Hadoop+ under “hadoop-plus/frame”

Publication
--------------
For more details of Hadoop+, you can reference our paper “Modeling and Evaluating the Heterogeneity for MapReduce Applications in Heterogeneous Clusters” accepted by ICS(International Conference on Supercomputing) 2015. It will be published in June, 2015.
