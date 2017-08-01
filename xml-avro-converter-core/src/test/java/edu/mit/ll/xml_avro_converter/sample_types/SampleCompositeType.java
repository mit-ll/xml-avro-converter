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

package edu.mit.ll.xml_avro_converter.sample_types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// A sample composite type
public class SampleCompositeType {

  public static SampleCompositeType getSampleComposite() {
    SampleCompositeType sampleComposite = new SampleCompositeType();
    sampleComposite.baseType1 = new SampleBaseType();
    sampleComposite.baseType1.baseVal = 5;
    sampleComposite.baseType2 = new SampleBaseType();
    sampleComposite.baseType2.baseVal = 6;
    sampleComposite.sampleBaseTypeArray1 = new ArrayList<>();
    sampleComposite.sampleBaseTypeArray1.add(sampleComposite.baseType1);
    sampleComposite.sampleBaseTypeArray2 = new ArrayList<>();
    sampleComposite.sampleBaseTypeArray2.add(sampleComposite.baseType2);
    sampleComposite.sampleBaseTypeMap1 = new HashMap<>();
    sampleComposite.sampleBaseTypeMap1.put("1", sampleComposite.baseType1);
    sampleComposite.sampleBaseTypeMap2 = new HashMap<>();
    sampleComposite.sampleBaseTypeMap2.put("2", sampleComposite.baseType2);
    sampleComposite.sampleRecursiveType1 = new SampleRecursiveType();
    sampleComposite.sampleRecursiveType1.subtypeVal = 7;
    sampleComposite.sampleRecursiveType2 = new SampleRecursiveType();
    sampleComposite.sampleRecursiveType2.subtypeVal = 8;
    sampleComposite.sampleRecursiveType1.recursiveSubtypeRef1
            = new SampleRecursiveType();
    sampleComposite.sampleRecursiveType1.recursiveSubtypeRef1.subtypeVal = 9;
    sampleComposite.sampleRecursiveType1.recursiveSubtypeRef2
            = new SampleRecursiveType();
    sampleComposite.sampleRecursiveType1.recursiveSubtypeRef2.subtypeVal = 10;
    sampleComposite.sampleRecursiveType2.recursiveSubtypeRef1
            = new SampleRecursiveType();
    sampleComposite.sampleRecursiveType2.recursiveSubtypeRef1.subtypeVal = 11;
    sampleComposite.sampleRecursiveType2.recursiveSubtypeRef2
            = new SampleRecursiveType();
    sampleComposite.sampleRecursiveType2.recursiveSubtypeRef2.subtypeVal = 12;
    return sampleComposite;
  }

  public SampleBaseType baseType1, baseType2;
  public List<SampleBaseType> sampleBaseTypeArray1, sampleBaseTypeArray2;
  public Map<String, SampleBaseType> sampleBaseTypeMap1, sampleBaseTypeMap2;
  public SampleRecursiveType sampleRecursiveType1, sampleRecursiveType2;

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 47 * hash + Objects.hashCode(this.baseType1);
    hash = 47 * hash + Objects.hashCode(this.baseType2);
    hash = 47 * hash + Objects.hashCode(this.sampleBaseTypeArray1);
    hash = 47 * hash + Objects.hashCode(this.sampleBaseTypeArray2);
    hash = 47 * hash + Objects.hashCode(this.sampleBaseTypeMap1);
    hash = 47 * hash + Objects.hashCode(this.sampleBaseTypeMap2);
    hash = 47 * hash + Objects.hashCode(this.sampleRecursiveType1);
    hash = 47 * hash + Objects.hashCode(this.sampleRecursiveType2);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SampleCompositeType other = (SampleCompositeType) obj;
    if (!Objects.equals(this.baseType1, other.baseType1)) {
      return false;
    }
    if (!Objects.equals(this.baseType2, other.baseType2)) {
      return false;
    }
    if (!Objects.equals(this.sampleBaseTypeArray1, other.sampleBaseTypeArray1)) {
      return false;
    }
    if (!Objects.equals(this.sampleBaseTypeArray2, other.sampleBaseTypeArray2)) {
      return false;
    }
    if (!Objects.equals(this.sampleBaseTypeMap1, other.sampleBaseTypeMap1)) {
      return false;
    }
    if (!Objects.equals(this.sampleBaseTypeMap2, other.sampleBaseTypeMap2)) {
      return false;
    }
    if (!Objects.equals(this.sampleRecursiveType1, other.sampleRecursiveType1)) {
      return false;
    }
    if (!Objects.equals(this.sampleRecursiveType2, other.sampleRecursiveType2)) {
      return false;
    }
    return true;
  }
}
