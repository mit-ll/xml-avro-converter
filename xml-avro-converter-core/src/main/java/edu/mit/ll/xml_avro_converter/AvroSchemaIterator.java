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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.avro.Schema;

// Iterates over an Avro schema
public class AvroSchemaIterator implements Iterable<Schema>,
        Iterator<Schema> {

  private Schema currentSchema;
  private final Deque<Schema> nodesToIterate;
  private final Set<Integer> iteratedNodes;

  public AvroSchemaIterator(Schema rootSchema) {
    nodesToIterate = new LinkedList<>();
    iteratedNodes = new HashSet<>();
    nodesToIterate.addFirst(rootSchema);
  }

  @Override
  public Iterator<Schema> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    while (!nodesToIterate.isEmpty()) {
      currentSchema = nodesToIterate.removeFirst();
      Integer objectNum = System.identityHashCode(currentSchema);
      if (!iteratedNodes.contains(objectNum)) {
        iteratedNodes.add(objectNum);
        nodesToIterate.addAll(getChildNodes(currentSchema));
        return true;
      }
    }
    return false;
  }

  @Override
  public Schema next() {
    return currentSchema;
  }

  // Returns the child nodes of schema
  private static List<Schema> getChildNodes(Schema schema) {
    List<Schema> children = new ArrayList<>();

    switch (schema.getType()) {
      case RECORD:
        for (Schema.Field field : schema.getFields()) {
          children.add(field.schema());
        }
        break;
      case UNION:
        children.addAll(schema.getTypes());
        break;
      case ARRAY:
        children.add(schema.getElementType());
        break;
      case MAP:
        children.add(schema.getValueType());
        break;
    }
    return children;
  }
}
