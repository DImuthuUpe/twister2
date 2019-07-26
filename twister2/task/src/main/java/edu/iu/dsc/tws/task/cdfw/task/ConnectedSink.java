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
package edu.iu.dsc.tws.task.cdfw.task;

import java.util.Iterator;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.task.IMessage;
import edu.iu.dsc.tws.api.task.TaskContext;
import edu.iu.dsc.tws.api.task.modifiers.Collector;
import edu.iu.dsc.tws.api.task.nodes.BaseSink;
import edu.iu.dsc.tws.dataset.partition.CollectionPartition;

public class ConnectedSink extends BaseSink implements Collector {
  /**
   * The name of the data set
   */
  private String outName;

  /**
   * The partition to use
   */
  private CollectionPartition<Object> partition;

  public ConnectedSink() {
  }

  public ConnectedSink(String outName) {
    this.outName = outName;
  }

  @Override
  public DataPartition<Object> get() {
    return partition;
  }

  @Override
  public DataPartition<Object> get(String name) {
    if (name.equals(outName)) {
      return partition;
    } else {
      throw new RuntimeException("Un-expected name: " + name);
    }
  }

  @Override
  public boolean execute(IMessage message) {
    if (message.getContent() instanceof Iterator) {
      Iterator<Object> itr = (Iterator<Object>) message.getContent();
      while (itr.hasNext()) {
        partition.add(itr.next());
      }
    } else {
      partition.add(message.getContent());
    }
    return true;
  }

  @Override
  public void prepare(Config cfg, TaskContext ctx) {
    super.prepare(cfg, ctx);
    partition = new CollectionPartition<>(ctx.taskIndex());
  }
}
