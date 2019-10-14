//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.examples.internal.rsched;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.exceptions.TimeoutException;
import edu.iu.dsc.tws.api.resource.IAllJoinedListener;
import edu.iu.dsc.tws.api.resource.IPersistentVolume;
import edu.iu.dsc.tws.api.resource.IReceiverFromDriver;
import edu.iu.dsc.tws.api.resource.IScalerListener;
import edu.iu.dsc.tws.api.resource.ISenderToDriver;
import edu.iu.dsc.tws.api.resource.IVolatileVolume;
import edu.iu.dsc.tws.api.resource.IWorker;
import edu.iu.dsc.tws.api.resource.IWorkerController;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.system.job.JobAPI;
import edu.iu.dsc.tws.proto.utils.WorkerInfoUtils;
import edu.iu.dsc.tws.proto.utils.WorkerResourceUtils;
import edu.iu.dsc.tws.rsched.core.WorkerRuntime;

public class BasicK8sWorker
    implements IWorker, IAllJoinedListener, IScalerListener, IReceiverFromDriver {

  private static final Logger LOG = Logger.getLogger(BasicK8sWorker.class.getName());

  private ISenderToDriver senderToDriver;

  private Object waitObject = new Object();

  // set this flag to true after getting scaledUp event
  private boolean scaledUp = false;

  @Override
  public void execute(Config config,
                      int workerID,
                      IWorkerController workerController,
                      IPersistentVolume persistentVolume,
                      IVolatileVolume volatileVolume) {

    WorkerRuntime.addAllJoinedListener(this);
    WorkerRuntime.addReceiverFromDriver(this);
    WorkerRuntime.addScalerListener(this);

    senderToDriver = WorkerRuntime.getSenderToDriver();

    LOG.info("BasicK8sWorker started. Current time: " + System.currentTimeMillis());

    if (volatileVolume != null) {
      String volatileDirPath = volatileVolume.getWorkerDir().getPath();
      LOG.info("Volatile Volume Directory: " + volatileDirPath);
    }

    if (persistentVolume != null) {
      // create worker directory
      String persVolumePath = persistentVolume.getWorkerDir().getPath();
      LOG.info("Persistent Volume Directory: " + persVolumePath);
    }

    List<JobMasterAPI.WorkerInfo> workerList = initSynch(workerController);
    if (workerList == null) {
      return;
    }

    Map<String, List<JobMasterAPI.WorkerInfo>> workersPerNode =
        WorkerResourceUtils.getWorkersPerNode(workerList);
    printWorkersPerNode(workersPerNode);

    testScaling(workerController);
//    listHdfsDir();
//    sleepSomeTime(50);
//    echoServer(workerController.getWorkerInfo());
  }

  private List<JobMasterAPI.WorkerInfo> initSynch(IWorkerController workerController) {
    // wait all workers to join the job
    List<JobMasterAPI.WorkerInfo> workerList = null;
    try {
      workerList = workerController.getAllWorkers();
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
      return null;
    }
    if (workerList == null) {
      LOG.severe("Can not get all workers to join. Something wrong. Exiting ....................");
      return null;
    }

    LOG.info(workerList.size() + " workers joined. ");

    // syncs with all workers
    LOG.info("Waiting on a barrier ........................ ");
    try {
      workerController.waitOnBarrier();
    } catch (TimeoutException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }

    LOG.info("Proceeded through the barrier ........................ ");
    return workerList;
  }

  /**
   * wait for all workers to join the job
   * this can be used for waiting initial worker joins or joins after scaling up the job
   */
  private void waitAllWorkersToJoin() {
    synchronized (waitObject) {
      try {
        LOG.info("Waiting for all workers to join the job... ");
        waitObject.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }
    }
  }

  public void allWorkersJoined(List<JobMasterAPI.WorkerInfo> workerList) {
    LOG.info("All workers joined: " + WorkerInfoUtils.workerListAsString(workerList));

    synchronized (waitObject) {
      waitObject.notify();
    }
  }

  @Override
  public void workersScaledUp(int instancesAdded) {
    LOG.info("Workers scaled up. Instances added: " + instancesAdded);

    scaledUp = true;
  }

  @Override
  public void workersScaledDown(int instancesRemoved) {
    LOG.info("Workers scaled down. Instances removed: " + instancesRemoved);
  }

  @Override
  public void driverMessageReceived(Any anyMessage) {

    if (anyMessage.is(JobMasterAPI.NodeInfo.class)) {
      try {
        JobMasterAPI.NodeInfo nodeInfo = anyMessage.unpack(JobMasterAPI.NodeInfo.class);
        LOG.info("Received Broadcast message. NodeInfo: " + nodeInfo);

        senderToDriver.sendToDriver(nodeInfo);

      } catch (InvalidProtocolBufferException e) {
        LOG.log(Level.SEVERE, "Unable to unpack received protocol buffer message as broadcast", e);
      }
    } else if (anyMessage.is(JobAPI.ComputeResource.class)) {
      try {
        JobAPI.ComputeResource computeResource = anyMessage.unpack(JobAPI.ComputeResource.class);
        LOG.info("Received Broadcast message. ComputeResource: " + computeResource);

        senderToDriver.sendToDriver(computeResource);

      } catch (InvalidProtocolBufferException e) {
        LOG.log(Level.SEVERE, "Unable to unpack received protocol buffer message as broadcast", e);
      }
    }

  }

  /**
   * an echo server.
   */
  public static void echoServer(JobMasterAPI.WorkerInfo workerInfo) {

    // create socket
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(workerInfo.getPort());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Could not start ServerSocket.", e);
    }

    LOG.info("Echo Server started on port " + workerInfo.getPort());

    // repeatedly wait for connections, and process
    while (true) {

      try {
        // a "blocking" call which waits until a connection is requested
        Socket clientSocket = serverSocket.accept();
        LOG.info("Accepted a connection from the client:" + clientSocket.getInetAddress());

        InputStream is = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        out.println("hello from the server: " + workerInfo);
        out.println("Will echo your messages:");

        String s;
        while ((s = reader.readLine()) != null) {
          out.println(s);
        }

        // close IO streams, then socket
        LOG.info("Closing the connection with client");
        out.close();
        reader.close();
        clientSocket.close();

      } catch (IOException ioe) {
        throw new IllegalArgumentException(ioe);
      }
    }
  }

  /**
   * a test method to make the worker wait some time
   */
  public void sleepSomeTime(long sleepSeconds) {
    try {
      LOG.info("BasicK8sWorker will sleep: " + sleepSeconds + " seconds.");
      Thread.sleep(sleepSeconds * 1000);
      LOG.info("BasicK8sWorker sleep completed.");
    } catch (InterruptedException e) {
      LOG.log(Level.WARNING, "Thread sleep interrupted.", e);
    }
  }

  /**
   * a test method to make the worker wait some time
   */
  public void sleepRandomTime(long maxTimeMS) {
    try {
      long sleepTime = (long) (Math.random() * maxTimeMS);
//      LOG.info("BasicK8sWorker will sleep: " + sleepTime + " ms.");
      Thread.sleep(sleepTime);
//      LOG.info("BasicK8sWorker sleep completed.");
    } catch (InterruptedException e) {
      LOG.log(Level.WARNING, "Thread sleep interrupted.", e);
    }
  }

  public void printWorkersPerNode(Map<String, List<JobMasterAPI.WorkerInfo>> workersPerNode) {

    StringBuffer toPrint = new StringBuffer();
    for (String nodeIP : workersPerNode.keySet()) {
      toPrint.append("\n" + nodeIP + ": ");
      for (JobMasterAPI.WorkerInfo workerInfo : workersPerNode.get(nodeIP)) {
        toPrint.append(workerInfo.getWorkerID() + ", ");
      }
    }

    LOG.info("Workers per node: " + toPrint.toString());
  }

  /**
   * a method to test hdfs access from workers
   */
  public void listHdfsDir() {

    String directory = "/user/hadoop/kmeans/";
//    String directory = ".";
    LOG.info("************************************ Will list hdfs directory: " + directory);

    System.setProperty("HADOOP_USER_NAME", "hadoop");
    String hdfsPath = "hdfs://<ip>:9000";
    Configuration conf = new Configuration();
    conf.set("fs.defaultFS", hdfsPath);
    conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
    conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

    try {
      FileSystem fileSystem = FileSystem.get(conf);

      Path path = new Path(directory);
      if (!fileSystem.exists(path)) {
        LOG.info("Directory [" + directory + "] does not exists");
        return;
      }
      LOG.info("Files in the directory: " + path.getName());
      int i = 0;

      for (FileStatus fileStatus : fileSystem.listStatus(path)) {
        LOG.info(i++ + ": " + fileStatus.getPath().toUri() + "\t" + fileStatus.getLen() + " bytes");
      }
      fileSystem.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * test scaling up and down workers in the job
   * after each scaleup event, all workers wait in a barrier
   */
  private void testScaling(IWorkerController workerController) {

    // we assume the worker is doing some computation
    // we simulate the computation by sleeping the worker for some time

    while (true) {

      // do some computation
      sleepRandomTime(300);

      // check for scaled up event
      if (scaledUp) {
        // reset the flag
        scaledUp = false;

        List<JobMasterAPI.WorkerInfo> workerList = initSynch(workerController);
        if (workerList == null) {
          return;
        }

      }

    }

  }
}
