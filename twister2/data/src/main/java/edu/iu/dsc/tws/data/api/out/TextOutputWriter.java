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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import edu.iu.dsc.tws.api.data.FSDataOutputStream;
import edu.iu.dsc.tws.api.data.FileSystem;
import edu.iu.dsc.tws.api.data.Path;

/**
 * Write a text file, every record is written to a new line
 */
public class TextOutputWriter extends FileOutputWriter<String> {
  private Map<Integer, PrintWriter> writerMap = new HashMap<>();

  private PrintWriter pw;

  public TextOutputWriter(FileSystem.WriteMode writeMode, Path outPath) {
    super(writeMode, outPath);
  }

  @Override
  public void createOutput(int partition, FSDataOutputStream out) {
    if (!writerMap.containsKey(partition)) {
      writerMap.put(partition, new PrintWriter(out));
    }
  }

  @Override
  public void writeRecord(int partition, String data) {
    if (writerMap.containsKey(partition)) {
      writerMap.get(partition).println(data);
    }
  }

  @Override
  protected void writeRecord(FSDataOutputStream out, String data) {
    pw = new PrintWriter(out);
    pw.write(data);
  }

  @Override
  protected void writeRecord(String data) {
  }

  @Override
  public void close() {
    if (!writerMap.isEmpty()) {
      for (PrintWriter pw1 : writerMap.values()) {
        pw1.close();
      }
      writerMap.clear();
    } else {
      pw.close();
    }
    super.close();
  }
}
