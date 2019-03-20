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
package edu.iu.dsc.tws.examples.ml.svm;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.Twister2Submitter;
import edu.iu.dsc.tws.api.job.Twister2Job;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.data.utils.MLDataObjectConstants;
import edu.iu.dsc.tws.data.utils.WorkerConstants;
import edu.iu.dsc.tws.examples.Utils;
import edu.iu.dsc.tws.examples.ml.svm.job.SvmSgdRunner;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;
import edu.iu.dsc.tws.rsched.core.SchedulerContext;

public final class SVMRunner {

  private static final Logger LOG = Logger.getLogger(SVMRunner.class.getName());

  private static Config config;

  private static Options options;

  private static JobConfig jobConfig;

  private static int workers;

  private static int parallelism;

  private static int ramMb;

  private static int diskGb;

  private static int instances;

  private static int cpus;

  private static int threads;

  private static String jobName; // aka expName

  private SVMRunner() { }

  public static void main(String[] args) {
    LOG.log(Level.INFO, "SVM Simple Config");

    try {
      initCmdArgs(args);
      printArgs();
      submitJob();
    } catch (ParseException e) {
      e.printStackTrace();
    }

  }

  /**
   * This method is used to initialize parameters need to run the program
   * @param args
   * @throws ParseException
   */
  public static void initCmdArgs(String[] args) throws ParseException {
    // first load the configurations from command line and config files
    config = ResourceAllocator.loadConfig(new HashMap<>());
    options = new Options();
    // Job Submission parameters
    options.addOption(WorkerConstants.WORKERS, true, "Workers");
    options.addOption(WorkerConstants.CPUS_PER_NODE, true, "Number of Cpus per Worker");
    options.addOption(WorkerConstants.RAM_MB, true, "RAM Per Worker");
    options.addOption(WorkerConstants.DISK_GB, true, "Disk Size Allocation");
    options.addOption(WorkerConstants.INSTANCES, true, "Instances");
    options.addOption(WorkerConstants.PARALLELISM, true, "Overall Parallelism");
    options.addOption(WorkerConstants.THREADS_PER_WORKER, true, "Threads Per Worker");


    //Directory based Parameters [optional parameters when running the dummy data mode
    options.addOption(Utils.createOption(MLDataObjectConstants.TRAINING_DATA_DIR,
        true, "Training data directory", false));
    options.addOption(Utils.createOption(MLDataObjectConstants.TESTING_DATA_DIR,
        true, "Testing data directory", false));
    options.addOption(Utils.createOption(MLDataObjectConstants.CROSS_VALIDATION_DATA_DIR,
        true, "Training data directory", false));
    options.addOption(Utils.createOption(MLDataObjectConstants.MODEL_SAVE_PATH,
        true, "Model Save Directory", false));

    // optional running choice based params
    options.addOption(MLDataObjectConstants.DUMMY, false, "Dummy data used for experiment");
    options.addOption(MLDataObjectConstants.STREAMING, false, "Streaming mode");
    options.addOption(MLDataObjectConstants.SPLIT, false, "Split Data");
    options.addOption(Utils.createOption(MLDataObjectConstants.RATIO,
        true, "Data Split Ratio [0.60,0.20,0.20]", false));

    // parameters to run the algorithm
    options.addOption(MLDataObjectConstants.SgdSvmDataObjectConstants.ALPHA, true,
        "Learning Rate");
    options.addOption(MLDataObjectConstants.SgdSvmDataObjectConstants.C, true,
        "C (constraint)");
    options.addOption(MLDataObjectConstants.SgdSvmDataObjectConstants.EXP_NAME, true,
        "Experiment Name");
    options.addOption(MLDataObjectConstants.SgdSvmDataObjectConstants.FEATURES, true,
        "Features in a data point");
    options.addOption(MLDataObjectConstants.SgdSvmDataObjectConstants.SAMPLES, true,
        "Samples in the data set");
    options.addOption(Utils.createOption(MLDataObjectConstants.SgdSvmDataObjectConstants.ITERATIONS,
        true, "Iterations", false));

    CommandLineParser commandLineParser = new DefaultParser();
    CommandLine cmd = commandLineParser.parse(options, args);
    // build JobConfig
    jobConfig = new JobConfig();

    workers = Integer.parseInt(cmd.getOptionValue(WorkerConstants.WORKERS));
    cpus = Integer.parseInt(cmd.getOptionValue(WorkerConstants.CPUS_PER_NODE));
    ramMb = Integer.parseInt(cmd.getOptionValue(WorkerConstants.RAM_MB));
    diskGb = Integer.parseInt(cmd.getOptionValue(WorkerConstants.DISK_GB));
    instances = Integer.parseInt(cmd.getOptionValue(WorkerConstants.INSTANCES));
    parallelism = Integer.parseInt(cmd.getOptionValue(WorkerConstants.PARALLELISM));
    threads = Integer.parseInt(cmd.getOptionValue(WorkerConstants.THREADS_PER_WORKER));

    jobConfig.put(WorkerConstants.WORKERS, workers);
    jobConfig.put(WorkerConstants.CPUS_PER_NODE, cpus);
    jobConfig.put(WorkerConstants.RAM_MB, ramMb);
    jobConfig.put(WorkerConstants.DISK_GB, diskGb);
    jobConfig.put(WorkerConstants.INSTANCES, instances);
    jobConfig.put(WorkerConstants.PARALLELISM, parallelism);

    jobConfig.put(MLDataObjectConstants.TRAINING_DATA_DIR,
        cmd.getOptionValue(MLDataObjectConstants.TRAINING_DATA_DIR));
    jobConfig.put(MLDataObjectConstants.TESTING_DATA_DIR,
        cmd.getOptionValue(MLDataObjectConstants.TESTING_DATA_DIR));
    jobConfig.put(MLDataObjectConstants.CROSS_VALIDATION_DATA_DIR,
        cmd.getOptionValue(MLDataObjectConstants.CROSS_VALIDATION_DATA_DIR));
    jobConfig.put(MLDataObjectConstants.MODEL_SAVE_PATH,
        cmd.getOptionValue(MLDataObjectConstants.MODEL_SAVE_PATH));


    jobConfig.put(MLDataObjectConstants.DUMMY, cmd.hasOption(MLDataObjectConstants.DUMMY));
    jobConfig.put(MLDataObjectConstants.STREAMING, cmd.hasOption(MLDataObjectConstants.STREAMING));
    jobConfig.put(MLDataObjectConstants.SPLIT, cmd.hasOption(MLDataObjectConstants.SPLIT));
    jobConfig.put(MLDataObjectConstants.RATIO,
        cmd.getOptionValue(MLDataObjectConstants.RATIO));

    jobConfig.put(MLDataObjectConstants.SgdSvmDataObjectConstants.ALPHA,
        Double.parseDouble(cmd
            .getOptionValue(MLDataObjectConstants.SgdSvmDataObjectConstants.ALPHA)));
    jobConfig.put(MLDataObjectConstants.SgdSvmDataObjectConstants.C,
        Double.parseDouble(cmd
            .getOptionValue(MLDataObjectConstants.SgdSvmDataObjectConstants.C)));
    jobConfig.put(MLDataObjectConstants.SgdSvmDataObjectConstants.FEATURES,
        Integer.parseInt(cmd
            .getOptionValue(MLDataObjectConstants.SgdSvmDataObjectConstants.FEATURES)));
    jobConfig.put(MLDataObjectConstants.SgdSvmDataObjectConstants.SAMPLES,
        Integer.parseInt(cmd
            .getOptionValue(MLDataObjectConstants.SgdSvmDataObjectConstants.SAMPLES)));
    jobConfig.put(MLDataObjectConstants.SgdSvmDataObjectConstants.ITERATIONS,
        Integer.parseInt(cmd
            .getOptionValue(MLDataObjectConstants.SgdSvmDataObjectConstants.ITERATIONS)));

    jobName = cmd.getOptionValue(MLDataObjectConstants.SgdSvmDataObjectConstants.EXP_NAME);
    jobConfig.put(MLDataObjectConstants.SgdSvmDataObjectConstants.EXP_NAME, jobName);

  }

  public static void submitJob() {
    HashMap<String, Object> configurations = new HashMap<>();
    configurations.put(SchedulerContext.THREADS_PER_WORKER, threads);
    //build the job

    Twister2Job.Twister2JobBuilder jobBuilder = Twister2Job.newBuilder();
    jobBuilder.setJobName(jobName);
    jobBuilder.setWorkerClass(SvmSgdRunner.class.getName());
    jobBuilder.addComputeResource(cpus, ramMb, diskGb, instances);
    jobBuilder.setConfig(jobConfig);

    // now submit the job
    Twister2Submitter.submitJob(jobBuilder.build(), config);
  }

  public static void printArgs() {
    LOG.info(jobConfig.toString());
  }

}
