/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.client.model;

import com.google.speedtracer.client.util.JSOArray;

import java.util.TreeMap;

/**
 * Class used to hold an address to symbol map for V8 Profile data.
 */
public class V8SymbolTable {
  /**
   * A single block of code occupies a span of address space represented by this
   * data structure.
   */
  static class AddressSpan implements Comparable<AddressSpan> {
    int addressLength;
    double address;

    public AddressSpan(double address, int addressLength) {
      this.address = address;
      this.addressLength = addressLength;
    }

    /**
     * This comparison function doesn't search for exact equality. Any overlap
     * between the two address spans is considered a match.
     */
    public int compareTo(AddressSpan compareAddress) {

      double aStart = compareAddress.address;
      double aEnd = aStart + compareAddress.addressLength;
      double bStart = address;
      double bEnd = bStart + addressLength;

      if (bStart >= aStart && bStart <= aEnd) {
        return 0;
      }
      if (aStart >= bStart && aStart <= bEnd) {
        return 0;
      }
      if (aStart < bStart) {
        return -1;
      }
      return 1;
    }

    public double getAddress() {
      return address;
    }

    public int getLength() {
      return addressLength;
    }

    public String toString() {
      return "0x" + Long.toHexString((long) address) + "-0x"
          + Long.toHexString((long) address + addressLength);
    }
  }

  /**
   * Stores a address used as context to decompress address fields from the log.
   */
  static class AddressTag {
    public final String name;
    public double prevAddress = 0;

    AddressTag(String name) {
      this.name = name;
    }

    public double get() {
      return prevAddress;
    }
  }

  /**
   * Associates a symbol or action with a numeric constant. This is useful in
   * the compressed log because multiple strings map to the same type.
   */
  static class AliasableEntry {
    private final String name;
    private final int value;

    protected AliasableEntry(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return this.name;
    }

    public int getValue() {
      return this.value;
    }

    public String toString() {
      return (this.name + ":" + this.value);
    }
  }

  /**
   * code-creation entries in the log create these symbols to be used to lookup
   * program counter entries in the tick data.
   */
  static class V8Symbol {
    private final AddressSpan addressSpan;
    // TODO(zundel): Use JsSymbol here?
    private final String name;
    private final int resourceLineNumber;
    private final String resourceUrl;
    private final AliasableEntry symbolType;

    V8Symbol(String name, AliasableEntry symbolType, double address,
        int addressLength) {
      JSOArray<String> vals = JSOArray.splitString(name, " ");
      if (vals.size() <= 1) {
        this.name = name;
        this.resourceUrl = "";
        resourceLineNumber = 0;
      } else {
        this.name = vals.get(0);
        int colonOffset = vals.get(1).lastIndexOf(":");
        this.resourceLineNumber = findLineNumber(vals.get(1), colonOffset);
        if (resourceLineNumber > 0) {
          this.resourceUrl = vals.get(1).substring(0, colonOffset);
        } else {
          this.resourceUrl = vals.get(1);
        }
      }
      this.symbolType = symbolType;
      this.addressSpan = new AddressSpan(address, addressLength);
    }

    public AddressSpan getAddressSpan() {
      return this.addressSpan;
    }

    public String getName() {
      return this.name;
    }

    public int getResourceLineNumber() {
      return this.resourceLineNumber;
    }

    public String getResourceUrl() {
      return this.resourceUrl;
    }

    public AliasableEntry getSymbolType() {
      return this.symbolType;
    }

    public String toString() {
      return name + " : " + addressSpan.toString();
    }

    private int findLineNumber(String resource, int colonOffset) {
      if (colonOffset <= 0) {
        return 0;
      }
      try {
        String lineNumberString = resource.substring(colonOffset + 1);
        return Integer.parseInt(lineNumberString);
      } catch (NumberFormatException ex) {
        return 0;
      }
    }
  }

  private TreeMap<AddressSpan, V8Symbol> table = new TreeMap<AddressSpan, V8Symbol>();

  public V8SymbolTable() {
  }

  /**
   * Add a symbol to the table.
   * 
   * Note collisions overwrite the previous value.
   */
  public void add(V8Symbol toAdd) {
    table.put(toAdd.getAddressSpan(), toAdd);
  }

  /**
   * A dump of the data stored in this symbol table intended only for debugging.
   */
  public void debugDumpHtml(StringBuilder output) {
    output.append("<table>");
    output.append("<tr><th>Name</th><th>Address</th><th>Length</th></tr>");
    for (V8Symbol child : table.values()) {
      output.append("<tr>");
      output.append("<td>" + child.getName() + "</td>");
      output.append("<td>"
          + Long.toHexString((long) child.getAddressSpan().getAddress())
          + "</td>");
      output.append("<td>"
          + Long.toString(child.getAddressSpan().addressLength) + "</td>");
      output.append("</tr>");
    }
    output.append("</table>");
  }

  public V8Symbol lookup(double address) {
    return table.get(new AddressSpan(address, 0));
  }

  public void remove(V8Symbol toRemove) {
    table.remove(toRemove.getAddressSpan());
  }
}