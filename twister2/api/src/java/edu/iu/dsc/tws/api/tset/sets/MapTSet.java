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

package edu.iu.dsc.tws.api.tset.sets;


import edu.iu.dsc.tws.api.task.nodes.ICompute;
import edu.iu.dsc.tws.api.tset.TSetEnvironment;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.fn.MapFunction;
import edu.iu.dsc.tws.api.tset.fn.Sink;
import edu.iu.dsc.tws.api.tset.link.DirectTLink;
import edu.iu.dsc.tws.api.tset.ops.MapOp;

public class MapTSet<T, P> extends BatchBaseTSet<P> {
  private MapFunction<T, P> mapFn;


  public MapTSet(TSetEnvironment tSetEnv, MapFunction<T, P> mapFunc, int parallelism) {
    super(tSetEnv, TSetUtils.generateName("map"), parallelism);
    this.mapFn = mapFunc;
  }

  public SinkTSet<T> sink(Sink<T> sink) {
    DirectTLink<T> direct = new DirectTLink<>(getTSetEnv(), getParallelism());
    addChildToGraph(direct);
    return direct.sink(sink);
  }

  @Override
  public MapTSet<T, P> setName(String name) {
    rename(name);
    return this;
  }

/*  @Override
  public void build(TSetGraph tSetGraph) {
    boolean isIterable = TSetUtils.isIterableInput(parent, tSetEnv.getTSetBuilder().getOpMode());
    boolean keyed = TSetUtils.isKeyedInput(parent);
    int p = calculateParallelism(parent);
    ComputeConnection connection = tSetEnv.getTSetBuilder().getTaskGraphBuilder().
        addCompute(getName(), new MapOp<>(mapFn, isIterable, keyed), p);
    parent.buildConnection(connection);
  }*/

  @Override
  protected ICompute getTask() {
    return new MapOp<>(mapFn, false, false);
  }
}
