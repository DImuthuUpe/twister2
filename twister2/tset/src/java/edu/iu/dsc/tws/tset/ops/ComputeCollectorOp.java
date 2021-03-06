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


package edu.iu.dsc.tws.tset.ops;

import java.util.Map;

import edu.iu.dsc.tws.api.compute.IMessage;
import edu.iu.dsc.tws.api.tset.fn.ComputeCollectorFunc;
import edu.iu.dsc.tws.api.tset.fn.RecordCollector;
import edu.iu.dsc.tws.api.tset.fn.TFunction;
import edu.iu.dsc.tws.tset.sets.BaseTSet;

/**
 * Performs the compute function on the value received for the imessage and write it to edges
 *
 * @param <O> Collector type
 * @param <I> Input message content type
 */
public class ComputeCollectorOp<O, I> extends BaseComputeOp<I> {

  private ComputeCollectorFunc<O, I> computeFunction;

  public ComputeCollectorOp() {
  }

  public ComputeCollectorOp(ComputeCollectorFunc<O, I> computeFunction, BaseTSet origin,
                            Map<String, String> receivables) {
    super(origin, receivables);
    this.computeFunction = computeFunction;
  }

  @Override
  public boolean execute(IMessage<I> content) {
    computeFunction.compute(content.getContent(), new RecordCollector<O>() {
      @Override
      public void collect(O record) {
        writeToEdges(record);
      }

      @Override
      public void close() {
      }
    });

    writeEndToEdges();
    computeFunction.close();
    return true;
  }

  @Override
  public TFunction getFunction() {
    return computeFunction;
  }
}
