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

import edu.mit.ll.xml_avro_converter.AvroSerializer;
import edu.mit.ll.xml_avro_converter.sample_types.SampleCompositeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.avro.Schema;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AvroSerializerTest {

  private void testSerialization(Object o, Schema schema)
          throws IOException, JAXBException, ParserConfigurationException {
    AvroSerializer<Object> serializer = new AvroSerializer(schema);
    ByteArrayOutputStream avroOutputStream = new ByteArrayOutputStream();
    serializer.writeToAvro(avroOutputStream, o);
    Object restored = serializer.readFromAvro(new ByteArrayInputStream(
            avroOutputStream.toByteArray()));
    assertTrue(restored.equals(o));
  }

  @Test
  public void testEmptyComposite()
          throws IOException, JAXBException, ParserConfigurationException {
    SampleCompositeType sampleComposite = new SampleCompositeType();
    testSerialization(
            sampleComposite,
            AvroSchemaGeneratorTest.getSamplePolymorphicSchema()
    );
  }

  @Test
  public void testSampleComposite()
          throws IOException, JAXBException, ParserConfigurationException {
    testSerialization(SampleCompositeType.getSampleComposite(),
            AvroSchemaGeneratorTest.getSamplePolymorphicSchema()
    );
  }
}
