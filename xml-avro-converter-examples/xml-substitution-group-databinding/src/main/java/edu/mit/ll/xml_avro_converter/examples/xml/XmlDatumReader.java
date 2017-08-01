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

package edu.mit.ll.xml_avro_converter.examples.xml;

import edu.mit.ll.xml_avro_converter.SchemaGenerationException;
import edu.mit.ll.xml_avro_converter.examples.cities.ObjectFactory;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.io.ResolvingDecoder;
import org.apache.avro.reflect.ReflectDatumReader;

public class XmlDatumReader<T> extends ReflectDatumReader<T> {

  public XmlDatumReader(Schema writer, Schema reader) {
    super(writer, reader);
  }

  public XmlDatumReader(Schema schema) {
    super(schema);
  }

  private Object readCountry(Object old, ResolvingDecoder in)
          throws IOException {
    String typeName = null, typeData = null;
    for (Schema.Field field : in.readFieldOrder()) {
      String fieldName = field.name();
      switch (fieldName) {
        case "typeName":
          typeName = (String) readWithoutConversion(old, field.schema(), in);
          break;
        case "typeData":
          typeData = (String) readWithoutConversion(old, field.schema(), in);
          break;
        default:
          throw new SchemaGenerationException("Unexpected field: "
                  + fieldName);
      }
    }
    if (typeName == null || typeData == null) {
      throw new SchemaGenerationException(
              "typeName and/or typeData was not defined");
    }
    ObjectFactory objf = new ObjectFactory();
    if (typeName.endsWith("CountryIsoCode")) {
      return objf.createCountryIsoCode(typeData);
    }
    if (typeName.endsWith("Country")) {
      return objf.createCountry(typeData);
    }
    throw new SchemaGenerationException("Unhandled type: " + typeName);
  }

  @Override
  protected Object readRecord(Object old, Schema expected,
          ResolvingDecoder in) throws IOException {
    if (!expected.getNamespace().equals(
            "edu.mit.ll.xml_avro_converter.examples.cities")) {
      return super.readRecord(old, expected, in);
    }
    if (expected.getName().equals("Country")) {
      return readCountry(old, in);
    }
    return super.readRecord(old, expected, in);
  }
}
