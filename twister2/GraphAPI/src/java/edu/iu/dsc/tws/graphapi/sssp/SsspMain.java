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
package edu.iu.dsc.tws.graphapi.sssp;

import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.Twister2Submitter;
import edu.iu.dsc.tws.api.job.Twister2Job;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.data.utils.DataObjectConstants;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;

public class SsspMain {
  private static final Logger LOG = Logger.getLogger(SsspMain.class.getName());

  public static void main(String[] args) throws ParseException, org.apache.commons.cli.ParseException {
    LOG.log(Level.INFO, "Single source shorest path Clustering Job");

    // first load the configurations from command line and config files
    Config config = ResourceAllocator.loadConfig(new HashMap<>());

    Options options = new Options();
    options.addOption(DataObjectConstants.WORKERS, true, "Workers");
    options.addOption(DataObjectConstants.DSIZE, true, "Size of the graph file");
    options.addOption(DataObjectConstants.NUMBER_OF_FILES, true, "Number of files");
    options.addOption(DataObjectConstants.SHARED_FILE_SYSTEM, false, "Shared file system");
    options.addOption(DataObjectConstants.PARALLELISM_VALUE, true, "parallelism");
    options.addOption(DataObjectConstants.SOURCE_VERTEX, true, "soruce vertex");


    options.addOption(DataObjectConstants.DINPUT_DIRECTORY,
        true, "Data points Input directory");

    options.addOption(DataObjectConstants.FILE_SYSTEM,
        true, "file system");

    CommandLineParser commandLineParser = new DefaultParser();
    CommandLine cmd = commandLineParser.parse(options, args);

    int workers = Integer.parseInt(cmd.getOptionValue(DataObjectConstants.WORKERS));
    int dsize = Integer.parseInt(cmd.getOptionValue(DataObjectConstants.DSIZE));
    int numFiles = Integer.parseInt(cmd.getOptionValue(DataObjectConstants.NUMBER_OF_FILES));
    int parallelismValue = Integer.parseInt(cmd.getOptionValue(
        DataObjectConstants.PARALLELISM_VALUE));

    String soruceVertex = cmd.getOptionValue(DataObjectConstants.SOURCE_VERTEX);

    String dataDirectory = cmd.getOptionValue(DataObjectConstants.DINPUT_DIRECTORY);

    String fileSystem = cmd.getOptionValue(DataObjectConstants.FILE_SYSTEM);

    boolean shared =
        Boolean.parseBoolean(cmd.getOptionValue(DataObjectConstants.SHARED_FILE_SYSTEM));

    // build JobConfig
    JobConfig jobConfig = new JobConfig();

    jobConfig.put(DataObjectConstants.DINPUT_DIRECTORY, dataDirectory);
    jobConfig.put(DataObjectConstants.FILE_SYSTEM, fileSystem);
    jobConfig.put(DataObjectConstants.DSIZE, Integer.toString(dsize));
    jobConfig.put(DataObjectConstants.WORKERS, Integer.toString(workers));
    jobConfig.put(DataObjectConstants.NUMBER_OF_FILES, Integer.toString(numFiles));
    jobConfig.put(DataObjectConstants.PARALLELISM_VALUE, Integer.toString(parallelismValue));
    jobConfig.put(DataObjectConstants.SHARED_FILE_SYSTEM, shared);
    jobConfig.put(DataObjectConstants.SOURCE_VERTEX,soruceVertex);

    Twister2Job.Twister2JobBuilder jobBuilder = Twister2Job.newBuilder();
    jobBuilder.setJobName("KMeans-job");
    jobBuilder.setWorkerClass(SingleSourceShortestPathWorker.class.getName());
    jobBuilder.addComputeResource(2, 512, 1.0, workers);
    jobBuilder.setConfig(jobConfig);

    // now submit the job
    Twister2Submitter.submitJob(jobBuilder.build(), config);
  }
}
