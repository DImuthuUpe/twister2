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
package edu.iu.dsc.tws.rsched.schedulers.k8s.master;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.apache.curator.framework.CuratorFramework;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.exceptions.Twister2RuntimeException;
import edu.iu.dsc.tws.api.faulttolerance.FaultToleranceContext;
import edu.iu.dsc.tws.api.scheduler.SchedulerContext;
import edu.iu.dsc.tws.common.logging.LoggingHelper;
import edu.iu.dsc.tws.common.zk.ZKContext;
import edu.iu.dsc.tws.common.zk.ZKEventsManager;
import edu.iu.dsc.tws.common.zk.ZKPersStateManager;
import edu.iu.dsc.tws.common.zk.ZKUtils;
import edu.iu.dsc.tws.master.server.JobMaster;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.system.job.JobAPI;
import edu.iu.dsc.tws.rsched.schedulers.k8s.K8sEnvVariables;
import edu.iu.dsc.tws.rsched.schedulers.k8s.KubernetesContext;
import edu.iu.dsc.tws.rsched.schedulers.k8s.KubernetesController;
import edu.iu.dsc.tws.rsched.schedulers.k8s.driver.K8sScaler;
import edu.iu.dsc.tws.rsched.schedulers.k8s.worker.K8sWorkerUtils;
import edu.iu.dsc.tws.rsched.utils.JobUtils;

import static edu.iu.dsc.tws.api.config.Context.JOB_ARCHIVE_DIRECTORY;
import static edu.iu.dsc.tws.rsched.schedulers.k8s.KubernetesConstants.POD_MEMORY_VOLUME;

public final class JobMasterStarter {
  private static final Logger LOG = Logger.getLogger(JobMasterStarter.class.getName());

  private JobMasterStarter() { }

  public static void main(String[] args) {
    // we can not initialize the logger fully yet,
    // but we need to set the format as the first thing
    LoggingHelper.setLoggingFormat(LoggingHelper.DEFAULT_FORMAT);

    // get environment variables
    String jobName = System.getenv(K8sEnvVariables.JOB_NAME + "");
    String encodedNodeInfoList = System.getenv(K8sEnvVariables.ENCODED_NODE_INFO_LIST + "");
    String hostIP = System.getenv(K8sEnvVariables.HOST_IP + "");

    // load the configuration parameters from configuration directory
    String configDir = POD_MEMORY_VOLUME + "/" + JOB_ARCHIVE_DIRECTORY;

    Config config = K8sWorkerUtils.loadConfig(configDir);

    // read job description file
    String jobDescFileName = SchedulerContext.createJobDescriptionFileName(jobName);
    jobDescFileName = POD_MEMORY_VOLUME + "/" + JOB_ARCHIVE_DIRECTORY + "/" + jobDescFileName;
    JobAPI.Job job = JobUtils.readJobFile(null, jobDescFileName);
    LOG.info("Job description file is loaded: " + jobDescFileName);

    // add any configuration from job file to the config object
    // if there are the same config parameters in both,
    // job file configurations will override
    config = JobUtils.overrideConfigs(job, config);
    config = JobUtils.updateConfigs(job, config);

    // init logger
    K8sWorkerUtils.initLogger(config, "jobMaster");

    LOG.info("JobMaster is starting. Current time: " + System.currentTimeMillis());
    LOG.info("Number of configuration parameters: " + config.size());

    // get podIP from localhost
    InetAddress localHost = null;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Cannot get localHost.", e);
    }
    String podIP = localHost.getHostAddress();

    // construct nodeInfo for Job Master
    JobMasterAPI.NodeInfo nodeInfo = KubernetesContext.nodeLocationsFromConfig(config)
        ? KubernetesContext.getNodeInfo(config, hostIP)
        : K8sWorkerUtils.getNodeInfoFromEncodedStr(encodedNodeInfoList, hostIP);

    LOG.info("NodeInfo for JobMaster: " + nodeInfo);

    JobTerminator jobTerminator = new JobTerminator(config);

    KubernetesController controller = new KubernetesController();
    controller.init(KubernetesContext.namespace(config));
    K8sScaler k8sScaler = new K8sScaler(config, job, controller);

    JobMasterAPI.JobMasterState initialState = determineInitialState(config, jobName, podIP);

    // start JobMaster
    JobMaster jobMaster =
        new JobMaster(config, podIP, jobTerminator, job, nodeInfo, k8sScaler, initialState);
    jobMaster.addShutdownHook(false);
    jobMaster.startJobMasterBlocking();

    // wait to be deleted by K8s master
    K8sWorkerUtils.waitIndefinitely();
  }

  /**
   * Job Master is either starting for the first time, or it is coming from failure
   * We return either JobMasterState.JM_STARTED or JobMasterState.JM_RESTARTED
   * TODO: If ZooKeeper is not used,
   *   currently we just return JM_STARTED. We do not determine real initial status.
   * @return
   */
  public static JobMasterAPI.JobMasterState determineInitialState(Config config,
                                                                  String jobName,
                                                                  String jmAddress) {

    if (ZKContext.isZooKeeperServerUsed(config)) {
      String zkServerAddresses = ZKContext.serverAddresses(config);
      int sessionTimeoutMs = FaultToleranceContext.sessionTimeout(config);
      CuratorFramework client = ZKUtils.connectToServer(zkServerAddresses, sessionTimeoutMs);
      String rootPath = ZKContext.rootNode(config);

      try {
        if (ZKPersStateManager.initJobMasterPersState(client, rootPath, jobName, jmAddress)) {
          ZKEventsManager.initEventCounter(client, rootPath, jobName);
          return JobMasterAPI.JobMasterState.JM_RESTARTED;
        }

        return JobMasterAPI.JobMasterState.JM_STARTED;

      } catch (Exception e) {
        throw new Twister2RuntimeException(e);
      }
    }

    return JobMasterAPI.JobMasterState.JM_STARTED;
  }

}
