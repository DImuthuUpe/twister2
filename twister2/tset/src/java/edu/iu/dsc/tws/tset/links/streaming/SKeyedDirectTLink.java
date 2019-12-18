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

package edu.iu.dsc.tws.tset.links.streaming;

import edu.iu.dsc.tws.api.comms.structs.Tuple;
import edu.iu.dsc.tws.api.compute.OperationNames;
import edu.iu.dsc.tws.api.compute.graph.Edge;
import edu.iu.dsc.tws.api.tset.fn.MapFunc;
import edu.iu.dsc.tws.tset.env.StreamingTSetEnvironment;
import edu.iu.dsc.tws.tset.sets.streaming.SKeyedTSet;

public class SKeyedDirectTLink<K, V> extends StreamingSingleLink<Tuple<K, V>> {

  public SKeyedDirectTLink(StreamingTSetEnvironment tSetEnv, int sourceParallelism) {
    super(tSetEnv, "skdirect", sourceParallelism);
  }

  public SKeyedTSet<K, V> mapToTuple() {
    return super.mapToTuple((MapFunc<Tuple<K, V>, Tuple<K, V>>) input -> input);
  }

  @Override
  public Edge getEdge() {
    return new Edge(getId(), OperationNames.DIRECT, getMessageType());
  }

  @Override
  public SKeyedDirectTLink<K, V> setName(String n) {
    rename(n);
    return this;
  }
}