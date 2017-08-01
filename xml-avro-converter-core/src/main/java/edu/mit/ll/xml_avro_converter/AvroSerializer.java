/**
 * Copyright 2016-2017 MIT Lincoln Laboratory, Massachusetts Institute of Technology
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This material is based upon work supported by the Federal Aviation
 * Administration under Air Force Contract No. FA8721-05-C-0002 and/or
 * FA8702-15-D-0001. Any opinions, findings, conclusions or recommendations
 * expressed in this material are those of the author(s) and do not
 * necessarily reflect the views of the Federal Aviation Administration.
 *
 * Delivered to the U.S. Government with Unlimited Rights, as defined in
 * DFARS Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright
 * notice, U.S. Government rights in this work are defined by DFARS
 * 252.227-7013 or DFARS 252.227-7014 as detailed above. Use of this work
 * other than as specifically authorized by the U.S. Government may violate
 * any copyrights that exist in this work.
 */

package edu.mit.ll.xml_avro_converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;

public class AvroSerializer<T> {

  private final Schema schema;
  private final ReflectData reflectData;

  public AvroSerializer(Schema schema) {
    this(schema, null);
  }

  public AvroSerializer(Schema schema, ReflectData reflectData) {
    this.schema = schema;
    if (reflectData == null) {
      this.reflectData = ReflectData.get();
    } else {
      this.reflectData = reflectData;
    }
  }

  // Writes obj to an output stream os
  public void writeToAvro(OutputStream os, T obj) throws IOException {
    DatumWriter<T> writer = reflectData.createDatumWriter(schema);
    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(os, null);
    writer.write(obj, encoder);
    encoder.flush();
    os.flush();
  }

  // Reads an object from an Avro input stream
  public T readFromAvro(InputStream is) throws IOException {
    DatumReader<T> reader = reflectData.createDatumReader(schema);
    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(is, null);
    return reader.read(null, decoder);
  }

  public Schema getSchema() {
    return schema;
  }
}
