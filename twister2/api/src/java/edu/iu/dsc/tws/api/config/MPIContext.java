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

package edu.iu.dsc.tws.api.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MPIContext extends SchedulerContext {
  public static final String WORKING_DIRECTORY =
      "twister2.resource.scheduler.mpi.working.directory";

  public static final String SLURM_JOB_ID = "twister2.resource.scheduler.mpi.job.id";

  public static final String MPI_SHELL_SCRIPT = "twister2.resource.scheduler.mpi.shell.script";

  public static final String PARTITION = "twister2.resource.scheduler.slurm.partition";
  public static final String MODE = "twsiter2.resource.scheduler.mpi.mode";
  public static final String NODES_FILE = "twister2.resource.scheduler.mpi.nodes.file";
  public static final String MPIRUN_FILE = "twister2.resource.scheduler.mpi.mpirun.file";
  public static final String MPI_MAP_BY = "twister2.resource.scheduler.mpi.mapby";
  public static final String MPI_MAP_BY_PE = "twister2.resource.scheduler.mpi.mapby.use-pe";

  public static final String NODES_ON_SHARED_FS = "twister2.resource.sharedfs";

  public static final String FILE_SYSTEM_MOUNT = "twister2.resource.fs.mount";

  public static final String JIP = "__job_master_ip__";

  public static final String JPORT = "__job_master_port__";

  private static Map<String, Object> runtimeObjects = new HashMap<>();

  public static String workingDirectory(Config config) {
    return TokenSub.substitute(config, config.getStringValue(WORKING_DIRECTORY,
        "${HOME}/.twister2/jobs"), Context.substitutions);
  }

  public static String jobIdFile(Config config) {
    return config.getStringValue(SLURM_JOB_ID, "mpi-job.pid");
  }

  public static String mpiShellScript(Config config) {
    return config.getStringValue(MPI_SHELL_SCRIPT, "mpiworker.sh");
  }

  public static String mpiScriptWithPath(Config config) {
    return new File(conf(config), mpiShellScript(config)).getPath();
  }

  public static String slurmShellScript(Config config) {
    return config.getStringValue(MPI_SHELL_SCRIPT, "mpilauncher.sh");
  }

  public static String slurmScriptWithPath(Config config) {
    return new File(conf(config), slurmShellScript(config)).getPath();
  }

  public static String partition(Config cfg) {
    return cfg.getStringValue(PARTITION);
  }

  public static String mpiMode(Config cfg) {
    return cfg.getStringValue(MODE, "node");
  }

  public static String nodesFile(Config cfg) {
    return cfg.getStringValue(NODES_FILE, "nodes");
  }

  public static String mpiRunFile(Config cfg) {
    return cfg.getStringValue(MPIRUN_FILE, "mpirun");
  }

  public static String mpiMapBy(Config cfg, int cpusPerProc) {
    String mapBy = cfg.getStringValue(MPI_MAP_BY, "node");
    if (mpiMapByUsePE(cfg)) {
      return mapBy + ":PE=" + cpusPerProc;
    }
    return mapBy;
  }

  private static boolean mpiMapByUsePE(Config cfg) {
    return cfg.getBooleanValue(MPI_MAP_BY_PE, false);
  }

  public static boolean isSharedFs(Config cfg) {
    return cfg.getBooleanValue(NODES_ON_SHARED_FS, true);
  }

  public static String fileSystemMount(Config cfg) {
    return TokenSub.substitute(cfg, cfg.getStringValue(FILE_SYSTEM_MOUNT,
        "${TWISTER2_HOME}/persistent/fs/"), Context.substitutions);
  }

  public static void addRuntimeObject(String name, Object value) {
    runtimeObjects.put(name, value);
  }

  public static Object getRuntimeObject(String name) {
    return runtimeObjects.get(name);
  }
}
