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

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.tset.Sink;
import edu.iu.dsc.tws.api.tset.TSetEnv;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.fn.IterableFlatMapFunction;
import edu.iu.dsc.tws.api.tset.fn.IterableMapFunction;
import edu.iu.dsc.tws.api.tset.link.BaseTLink;
import edu.iu.dsc.tws.api.tset.link.DirectTLink;
import edu.iu.dsc.tws.api.tset.ops.IterableFlatMapOp;
import edu.iu.dsc.tws.task.impl.ComputeConnection;

public class IterableFlatMapTSet<I, O> extends BatchBaseTSet<O> {
  private BaseTLink<I> parent;

  private IterableFlatMapFunction<I, O> mapFn;

  public IterableFlatMapTSet(Config cfg, TSetEnv tSetEnv, BaseTLink<I> parent,
                             IterableFlatMapFunction<I, O> mapFunc) {
    this(cfg, tSetEnv, parent, mapFunc, 1);
  }

  public IterableFlatMapTSet(Config cfg, TSetEnv tSetEnv, BaseTLink<I> parent,
                             IterableFlatMapFunction<I, O> mapFunc, int parallelism) {
    super(cfg, tSetEnv);
    this.parent = parent;
    this.mapFn = mapFunc;
    this.parallel = parallelism;
    this.name = "iflatmap-" + parent.getName();
  }

  public <O1> IterableMapTSet<O, O1> map(IterableMapFunction<O, O1> mFn) {
    DirectTLink<O> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.map(mFn);
  }

  public <O1> IterableFlatMapTSet<O, O1> flatMap(IterableFlatMapFunction<O, O1> mFn) {
    DirectTLink<O> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.flatMap(mFn);
  }

  public SinkTSet<O> sink(Sink<O> sink) {
    DirectTLink<O> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.sink(sink);
  }

  public boolean baseBuild() {
    boolean isIterable = TSetUtils.isIterableInput(parent, tSetEnv.getTSetBuilder().getOpMode());
    boolean keyed = TSetUtils.isKeyedInput(parent);

    // lets override the parallelism
    int p = calculateParallelism(parent);
    ComputeConnection connection = tSetEnv.getTSetBuilder().getTaskGraphBuilder().
        addCompute(generateName("i-flat-map", parent),
            new IterableFlatMapOp<>(mapFn, isIterable, keyed), p);
    parent.buildConnection(connection);
    return true;
  }

  @Override
  public void buildConnection(ComputeConnection connection) {
    throw new IllegalStateException("Build connections should not be called on a TSet");
  }

  @Override
  public IterableFlatMapTSet<I, O> setName(String n) {
    this.name = n;
    return this;
  }
}

