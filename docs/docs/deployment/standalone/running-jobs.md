---
id: standalone
title: Standalone
sidebar_label: Standalone
---

The standalone mode of deployment is most easiest way to deploy a Twister2 application.

## Requirements

The nodes running the jobs must be able to SSH into each other without requiring a password.

## Running Jobs

In order to run a job you can use the following command

```bash
twister2 submit standalone job-type job-file-name job-class-name [job-args]
```

Here is an example command

```bash
./bin/twister2 submit standalone jar examples/libexamples-java.jar edu.iu.dsc.tws.examples.basic.HelloWorld 8
```

In this mode, the job is killed immediately when you terminate the client using ```Ctrl + C```.

## Configurations

You can configure the nodes by editing the ```nodes``` file found under ```conf/standalone/nodes```.

Here enter the the node address and the number of worker you can run on each of them.

By default we ship the following nodes file.

```bash
localhost slots=16
```

Here it says we can run up to 16 workers in the local machine. You can add more machines with their capacity
to this file in order to run the job on them.

## Installing OpenMPI

When you compile Twister2, it builds OpenMPI 3.1.2 version with it. This version is
used by Twister2 for its standalone deployment by default.

You can use your own OpenMPI installation when running the jobs. In order to do that, you
need to change the following parameter found in ```conf/standalone/resource.yaml``` to point to your OpenMPI installation.

```bash
# mpi run file, this assumes a mpirun that is shipped with the product
# change this to just mpirun if you are using a system wide installation of OpenMPI
# or complete path of OpenMPI in case you have something custom
twister2.resource.scheduler.mpi.mpirun.file: "twister2-core/ompi/bin/mpirun"
```

You can follow the [compiling document](../../compiling/compiling.md) to get instructions on how to install and configure OpenMPI.

## How it works

Standalone uses OpenMPI to start the job. Underneath it uses mpirun command to execute the job. You can change the parameters
of mpirun inside the ```conf/standalone/mpi.sh``` script.

## Deploying and Running on a MPI cluster

Follow the [Guide on Running an MPI Cluster within a LAN](http://mpitutorial.com/tutorials/running-an-mpi-cluster-within-a-lan/) to setup your MPI cluster. 

Twister2 when running in standalone mode, picks the [hostfile](https://www.open-mpi.org/faq/?category=running#mpirun-hostfile) from ```conf/standalone/nodes```

Twister2 by default assumes that all the nodes in MPI cluster have access to a [NFS(Network File System)](https://en.wikipedia.org/wiki/Network_File_System)
This default behaviour can be changed by setting ``twister2.resource.sharedfs`` of `conf/standalone/resource.yml` to ``false``

### Troubleshooting common errors

* Make sure you are running the same MPI version in all nodes. 
* Make sure your executables are residing at the same location on all nodes
* If you have multiple network interfaces in each node, make sure you [specify which interfaces](https://www.open-mpi.org/faq/?category=tcp#tcp-connection-errors) to use by specifying them explicitly
in ``conf/standalone/bootstrap.sh`` and ``conf/standalone/mpi.sh`` 
