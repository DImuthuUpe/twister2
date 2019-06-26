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
package edu.iu.dsc.tws.rsched.schedulers.standalone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.iu.dsc.tws.api.checkpointing.CheckpointingClient;
import edu.iu.dsc.tws.api.exceptions.TimeoutException;
import edu.iu.dsc.tws.api.resource.IWorkerController;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;

public class MPIJobWorkerController implements IWorkerController {
  private Map<String, Object> runtimeObjects = new HashMap<>();

  private IWorkerController delegate;

  public MPIJobWorkerController(IWorkerController original) {
    this.delegate = original;
  }

  public void add(String name, Object value) {
    runtimeObjects.put(name, value);
  }

  @Override
  public JobMasterAPI.WorkerInfo getWorkerInfo() {
    return delegate.getWorkerInfo();
  }

  @Override
  public JobMasterAPI.WorkerInfo getWorkerInfoForID(int id) {
    return delegate.getWorkerInfoForID(id);
  }

  @Override
  public int getNumberOfWorkers() {
    return delegate.getNumberOfWorkers();
  }

  @Override
  public List<JobMasterAPI.WorkerInfo> getJoinedWorkers() {
    return delegate.getJoinedWorkers();
  }

  @Override
  public List<JobMasterAPI.WorkerInfo> getAllWorkers() throws TimeoutException {
    return delegate.getAllWorkers();
  }

  @Override
  public void waitOnBarrier() throws TimeoutException {
    delegate.waitOnBarrier();
  }

  @Override
  public Object getRuntimeObject(String name) {
    return runtimeObjects.get(name);
  }

  @Override
  public CheckpointingClient getCheckpointingClient() {
    return this.delegate.getCheckpointingClient();
  }
}
