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
package edu.iu.dsc.tws.task.impl.ops;

import edu.iu.dsc.tws.api.compute.OperationNames;
import edu.iu.dsc.tws.api.compute.graph.Edge;
import edu.iu.dsc.tws.task.impl.ComputeConnection;

public class PartitionConfig extends AbstractOpsConfig<PartitionConfig> {

  public PartitionConfig(String source, ComputeConnection computeConnection) {
    super(source, OperationNames.PARTITION, computeConnection);
  }

  @Override
  void validate() {
    //nothign to do here for partition
  }

  @Override
  protected Edge updateEdge(Edge newEdge) {
    return newEdge;
  }
}
