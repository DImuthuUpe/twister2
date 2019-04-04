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
package edu.iu.dsc.tws.comms.dfw.io.allgather;

import edu.iu.dsc.tws.comms.dfw.TreeBroadcast;
import edu.iu.dsc.tws.comms.dfw.io.gather.BaseGatherBatchReceiver;

public class AllGatherBatchFinalReceiver extends BaseGatherBatchReceiver {
  private TreeBroadcast gatherReceiver;

  public AllGatherBatchFinalReceiver(TreeBroadcast bCast) {
    this.gatherReceiver = bCast;
  }

  @Override
  protected boolean sendSyncForward(boolean needsFurtherProgress, int target) {
    return false;
  }

  @Override
  protected boolean handleMessage(int task, Object message, int flags, int dest) {
    if (gatherReceiver.send(task, gatheredValuesMap.get(task), flags)) {
      gatheredValuesMap.put(task, null);
      onFinish(task);
    } else {
      return false;
    }
    return true;
  }
}
