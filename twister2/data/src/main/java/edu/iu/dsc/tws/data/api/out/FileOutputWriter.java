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
package edu.iu.dsc.tws.data.api.out;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.data.FSDataOutputStream;
import edu.iu.dsc.tws.api.data.FileSystem;
import edu.iu.dsc.tws.api.data.Path;
import edu.iu.dsc.tws.data.api.OutputWriter;
import edu.iu.dsc.tws.data.utils.FileSystemUtils;

/**
 * Abstract class for writing to file system
 *
 * @param <T> the type of data to write
 */
public abstract class FileOutputWriter<T> implements OutputWriter<T> {
  private static final Logger LOG = Logger.getLogger(FileOutputWriter.class.getName());

  /**
   * File system object
   */
  protected FileSystem fs;

  /**
   * Opened streams
   */
  protected Map<Integer, FSDataOutputStream> openStreams = new HashMap<>();

  /**
   * Write mode of the files
   */
  protected FileSystem.WriteMode writeMode;

  /**
   * File output path
   */
  protected Path outPath;

  public FileOutputWriter(FileSystem.WriteMode writeMode, Path outPath) {
    this.writeMode = writeMode;
    this.outPath = outPath;
    try {
      this.fs = FileSystemUtils.get(outPath.toUri());
    } catch (IOException e) {
      throw new RuntimeException("Failed to create file system for : " + outPath.toUri());
    }
  }

  public void write(int partition, T out) {
    FSDataOutputStream fsOut;
    if (!openStreams.containsKey(partition)) {
      Path path = new Path(outPath, "part-" + partition);
      try {
        fsOut = fs.create(path);
        // lets ask user to create its own output method
        createOutput(partition, fsOut);
        openStreams.put(partition, fsOut);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create output stream for file: " + path, e);
      }
    }
    writeRecord(partition, out);
  }

  public void write(T out) {
    LOG.info("points data:" + out);
    FSDataOutputStream fsOut;
    Path path = new Path(String.valueOf(outPath));
    try {
      fsOut = fs.create(path);
      createOutput(fsOut);
      //openStreams.put(0, fsOut);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create output stream for file: " + path, e);
    }
    writeRecord(out);
  }

  protected abstract void createOutput(FSDataOutputStream out);

  protected abstract void writeRecord(T data);

  /**
   * Create a suitable output
   *
   * @param partition partition id
   * @param out       the out stream
   */
  protected abstract void createOutput(int partition, FSDataOutputStream out);

  /**
   * Write the record to output
   *
   * @param partition partition id
   * @param data      data
   */
  protected abstract void writeRecord(int partition, T data);

  @Override
  public void configure(Config config) {
  }

  @Override
  public void close() {
    for (FSDataOutputStream o : openStreams.values()) {
      try {
        o.close();
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Failed to close the output stream", e);
      }
    }
    openStreams.clear();
  }
}
