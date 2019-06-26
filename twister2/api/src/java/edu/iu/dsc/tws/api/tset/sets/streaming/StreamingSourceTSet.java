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

package edu.iu.dsc.tws.api.tset.sets.streaming;

import java.util.Random;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.tset.Sink;
import edu.iu.dsc.tws.api.tset.Source;
import edu.iu.dsc.tws.api.tset.TSetEnv;
import edu.iu.dsc.tws.api.tset.fn.FlatMapFunction;
import edu.iu.dsc.tws.api.tset.fn.MapFunction;
import edu.iu.dsc.tws.api.tset.link.streaming.StreamingDirectTLink;
import edu.iu.dsc.tws.api.tset.ops.SourceOp;
import edu.iu.dsc.tws.api.tset.sets.SinkTSet;
import edu.iu.dsc.tws.task.impl.ComputeConnection;

public class StreamingSourceTSet<T> extends StreamingBaseTSet<T> {
  private Source<T> source;

  public StreamingSourceTSet(Config cfg, TSetEnv tSetEnv, Source<T> src, int parallelism) {
    super(cfg, tSetEnv);
    this.source = src;
    this.name = "source-" + new Random(System.nanoTime()).nextInt(10);
    this.parallel = parallelism;
  }

  public <P> StreamingMapTSet<P, T> map(MapFunction<T, P> mapFn) {
    StreamingDirectTLink<T> direct = new StreamingDirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.map(mapFn);
  }

  public <P> StreamingFlatMapTSet<P, T> flatMap(FlatMapFunction<T, P> mapFn) {
    StreamingDirectTLink<T> direct = new StreamingDirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.flatMap(mapFn);
  }

  public SinkTSet<T> sink(Sink<T> sink) {
    StreamingDirectTLink<T> direct = new StreamingDirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.sink(sink);
  }

  @Override
  public boolean baseBuild() {
    tSetEnv.getTSetBuilder().getTaskGraphBuilder().
        addSource(getName(), new SourceOp<T>(source), parallel);
    return true;
  }

  @Override
  public void buildConnection(ComputeConnection connection) {
    throw new IllegalStateException("Build connections should not be called on a TSet");
  }

  @Override
  public StreamingSourceTSet<T> setName(String n) {
    this.name = n;
    return this;
  }
}
