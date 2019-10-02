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
package edu.iu.dsc.tws.examples.batch.kmeans;

import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.compute.IFunction;
import edu.iu.dsc.tws.api.compute.IMessage;
import edu.iu.dsc.tws.api.compute.graph.ComputeGraph;
import edu.iu.dsc.tws.api.compute.nodes.BaseCompute;
import edu.iu.dsc.tws.api.compute.nodes.BaseSink;
import edu.iu.dsc.tws.api.compute.nodes.BaseSource;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.common.config.ConfigLoader;
import edu.iu.dsc.tws.task.impl.ComputeConnection;
import edu.iu.dsc.tws.task.impl.ComputeGraphBuilder;
import edu.iu.dsc.tws.task.impl.TaskConfigurations;

public class TaskGraphBuildTest {

  private static final Logger LOGGER = Logger.getLogger(TaskGraphBuildTest.class.getName());

  @Test
  public void testUniqueSchedules1() {
    ComputeGraph computeGraph = createGraph();
    Assert.assertNotNull(computeGraph);
    Assert.assertEquals(computeGraph.taskEdgeSet().iterator().next().getName(),
        "source");
    Assert.assertEquals(computeGraph.taskEdgeSet().size(), 2);
  }

  @Test
  public void testUniqueSchedules2() {
    ComputeGraph computeGraph = createGraph();
    Assert.assertEquals(computeGraph.getTaskVertexSet().iterator().next().getName(),
        "source");
    Assert.assertEquals(computeGraph.taskEdgeSet().size(), 2);
  }

  private ComputeGraph createGraph() {
    TestSource testSource = new TestSource();
    TestSink1 testCompute = new TestSink1();
    TestSink2 testSink = new TestSink2();

    ComputeGraphBuilder computeGraphBuilder = ComputeGraphBuilder.newBuilder(getConfig());

    computeGraphBuilder.addSource("source", testSource, 4);
    ComputeConnection computeConnection = computeGraphBuilder.addCompute(
        "compute", testCompute, 4);
    computeConnection.partition("source").viaEdge(TaskConfigurations.DEFAULT_EDGE)
        .withDataType(MessageTypes.OBJECT);
    ComputeConnection rc = computeGraphBuilder.addSink("sink", testSink, 1);
    rc.allreduce("compute")
        .viaEdge(TaskConfigurations.DEFAULT_EDGE)
        .withReductionFunction(new Aggregator())
        .withDataType(MessageTypes.OBJECT);
    ComputeGraph graph = computeGraphBuilder.build();
    return graph;
  }

  public class Aggregator implements IFunction {
    private static final long serialVersionUID = -254264120110286748L;

    @Override
    public Object onMessage(Object object1, Object object2) throws ArrayIndexOutOfBoundsException {

      double[] object11 = (double[]) object1;
      double[] object21 = (double[]) object2;
      double[] object31 = new double[object11.length];

      for (int j = 0; j < object11.length; j++) {
        double newVal = object11[j] + object21[j];
        object31[j] = newVal;
      }
      return object31;
    }
  }

  private Config getConfig() {
    String twister2Home = "/home/" + System.getProperty("user.dir")
        + "/twister2/bazel-bin/scripts/package/twister2-0.3.0";
    String configDir = "/home/" + System.getProperty("user.dir")
        + "/twister2/twister2/taskscheduler/tests/conf/";
    String clusterType = "standalone";
    Config config = ConfigLoader.loadConfig(twister2Home, configDir, clusterType);
    return Config.newBuilder().putAll(config).build();
  }

  public static class TestSource extends BaseSource {
    private static final long serialVersionUID = -254264903510284748L;

    @Override
    public void execute() {
    }
  }

  public static class TestSink1 extends BaseCompute {
    private static final long serialVersionUID = -254264903510284748L;

    @Override
    public boolean execute(IMessage message) {
      return false;
    }
  }

  public static class TestSink2 extends BaseSink {
    private static final long serialVersionUID = -254264903510284748L;

    @Override
    public boolean execute(IMessage message) {
      return false;
    }
  }
}
