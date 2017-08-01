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

package edu.mit.ll.xml_avro_converter.examples.polymorphism;

import edu.mit.ll.xml_avro_converter.AvroSchemaGenerator;
import edu.mit.ll.xml_avro_converter.AvroSerializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;

public class PolymorphismExample {

  public static void main(String[] args) throws IOException {
    AvroSchemaGenerator schemaGenerator = new AvroSchemaGenerator();
    Schema departureQueueSchema = schemaGenerator.generateSchema(
            DepartureQueue.class);

    System.out.println("Schema without subtypes: "
            + departureQueueSchema.toString(true));

    schemaGenerator.declarePolymorphicType(
            CargoAircraft.class,
            PassengerAircraft.class);
    departureQueueSchema = schemaGenerator.generateSchema(
            DepartureQueue.class);
    System.out.println("Schema with subtypes added: "
            + departureQueueSchema.toString(true));

    DepartureQueue queue = getSampleData();
    AvroSerializer<DepartureQueue> serializer = new AvroSerializer(
            departureQueueSchema);
    ByteArrayOutputStream departureQueueAvroStream
            = new ByteArrayOutputStream();
    serializer.writeToAvro(departureQueueAvroStream, queue);
    System.out.println("Serialized sample data to "
            + departureQueueAvroStream.toByteArray().length + " bytes");
  }

  private static DepartureQueue getSampleData() {
    DepartureQueue departureQueue = new DepartureQueue();
    departureQueue.name = "sample departure queue";
    departureQueue.aircraft = new ArrayList<>();

    CargoAircraft sampleCargoAircraft = new CargoAircraft();
    sampleCargoAircraft.identifier = "sample cargo aircraft";
    sampleCargoAircraft.cargoCapacity = 1800.0;
    departureQueue.aircraft.add(sampleCargoAircraft);

    PassengerAircraft samplePassengerAircraft = new PassengerAircraft();
    samplePassengerAircraft.identifier = "sample passenger aircraft";
    samplePassengerAircraft.passengerCapacity = 150;
    departureQueue.aircraft.add(samplePassengerAircraft);

    return departureQueue;
  }

  private static class DepartureQueue {

    public String name;
    public List<Aircraft> aircraft;
  }

  private static abstract class Aircraft {

    public String identifier;
  }

  private static class CargoAircraft extends Aircraft {

    public double cargoCapacity;
  }

  private static class PassengerAircraft extends Aircraft {

    public int passengerCapacity;
  }
}
