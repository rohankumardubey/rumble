/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Irimescu, Can Berker Cikis
 *
 */

package org.rumbledb.types;


import org.rumbledb.exceptions.OurBadException;

import java.io.Serializable;

public class ItemType implements Serializable {

    private static final long serialVersionUID = 1L;
    private String name;
    private int index; // for private matrix operation

    public static final ItemType item = new ItemType("item", 0);
    public static final ItemType atomicItem = new ItemType("atomic", 1);
    public static final ItemType stringItem = new ItemType("string", 2);
    public static final ItemType integerItem = new ItemType("integer", 3);
    public static final ItemType decimalItem = new ItemType("decimal", 4);
    public static final ItemType doubleItem = new ItemType("double", 5);
    public static final ItemType booleanItem = new ItemType("boolean", 6);
    public static final ItemType nullItem = new ItemType("null", 7);
    public static final ItemType durationItem = new ItemType("duration", 8);
    public static final ItemType yearMonthDurationItem = new ItemType("yearMonthDuration", 9);
    public static final ItemType dayTimeDurationItem = new ItemType("dayTimeDuration", 10);
    public static final ItemType dateTimeItem = new ItemType("dateTime", 11);
    public static final ItemType dateItem = new ItemType("date", 12);
    public static final ItemType timeItem = new ItemType("time", 13);
    public static final ItemType hexBinaryItem = new ItemType("hexBinary", 14);
    public static final ItemType anyURIItem = new ItemType("anyURI", 15);
    public static final ItemType base64BinaryItem = new ItemType("base64Binary", 16);
    public static final ItemType JSONItem = new ItemType("json-item", 17);
    public static final ItemType objectItem = new ItemType("object", 18);
    public static final ItemType arrayItem = new ItemType("array", 19);
    public static final ItemType functionItem = new ItemType("function", 20);

    public ItemType() {
    }

    private ItemType(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return this.name;
    }

    public int getIndex() {
        return this.index;
    }

    public static ItemType getItemTypeByName(String name) {
        if (name.equals(objectItem.name)) {
            return objectItem;
        }
        if (name.equals(atomicItem.name)) {
            return atomicItem;
        }
        if (name.equals(stringItem.name)) {
            return stringItem;
        }
        if (name.equals(integerItem.name)) {
            return integerItem;
        }
        if (name.equals(decimalItem.name)) {
            return decimalItem;
        }
        if (name.equals(doubleItem.name)) {
            return doubleItem;
        }
        if (name.equals(booleanItem.name)) {
            return booleanItem;
        }
        if (name.equals(nullItem.name)) {
            return nullItem;
        }
        if (name.equals(arrayItem.name)) {
            return arrayItem;
        }
        if (name.equals(JSONItem.name)) {
            return JSONItem;
        }
        if (name.equals(durationItem.name)) {
            return durationItem;
        }
        if (name.equals(yearMonthDurationItem.name)) {
            return yearMonthDurationItem;
        }
        if (name.equals(dayTimeDurationItem.name)) {
            return dayTimeDurationItem;
        }
        if (name.equals(dateTimeItem.name)) {
            return dateTimeItem;
        }
        if (name.equals(dateItem.name)) {
            return dateItem;
        }
        if (name.equals(timeItem.name)) {
            return timeItem;
        }
        if (name.equals(anyURIItem.name)) {
            return anyURIItem;
        }
        if (name.equals(hexBinaryItem.name)) {
            return hexBinaryItem;
        }
        if (name.equals(base64BinaryItem.name)) {
            return base64BinaryItem;
        }
        if (name.equals(item.name)) {
            return item;
        }
        throw new OurBadException("Type unrecognized: " + name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemType)) {
            return false;
        }
        return this.name.equals(((ItemType) other).getName());
    }


    // Returns true if [this] is a subtype of [superType], any type is considered a subtype of itself
    public boolean isSubtypeOf(ItemType superType) {
        if (superType.equals(item)) {
            return true;
        } else if (superType.equals(JSONItem)) {
            return this.equals(objectItem)
                || this.equals(arrayItem)
                || this.equals(JSONItem);
        } else if (superType.equals(atomicItem)) {
            return this.equals(stringItem)
                || this.equals(integerItem)
                || this.equals(decimalItem)
                || this.equals(doubleItem)
                || this.equals(booleanItem)
                || this.equals(nullItem)
                || this.equals(anyURIItem)
                || this.equals(hexBinaryItem)
                || this.equals(base64BinaryItem)
                || this.equals(dateTimeItem)
                || this.equals(dateItem)
                || this.equals(timeItem)
                || this.equals(durationItem)
                || this.equals(yearMonthDurationItem)
                || this.equals(dayTimeDurationItem)
                || this.equals(atomicItem);
        } else if (superType.equals(durationItem)) {
            return this.equals(yearMonthDurationItem)
                || this.equals(dayTimeDurationItem)
                || this.equals(durationItem);
        } else if (superType.equals(decimalItem)) {
            return this.equals(integerItem) || this.equals(decimalItem);
        }
        return this.equals(superType);
    }

    public ItemType findCommonSuperType(ItemType other) {
        if (other.isSubtypeOf(this)) {
            return this;
        } else if (this.isSubtypeOf(other)) {
            return other;
        } else if (this.isSubtypeOf(durationItem) && other.isSubtypeOf(durationItem)) {
            return durationItem;
        } else if (this.isSubtypeOf(atomicItem) && other.isSubtypeOf(atomicItem)) {
            return atomicItem;
        } else if (this.isSubtypeOf(JSONItem) && other.isSubtypeOf(JSONItem)) {
            return JSONItem;
        } else {
            return item;
        }
    }

    /**
     * Check at static time if [this] could be casted to [other] item type, requires [this] to be an atomic type
     *
     * @param other a strict subtype of atomic item type to which we are trying to cast
     * @return true if it is possible at static time to cast [this] to [other], false otherwise
     */
    public boolean staticallyCastableAs(ItemType other) {
        // anything can be casted to itself
        if (this.equals(other))
            return true;
        // anything can be casted from and to a string (or from one of its supertype)
        if (this.equals(stringItem) || other.equals(stringItem))
            return true;
        // boolean and numeric can be cast between themselves
        if (
            this.equals(booleanItem) || this.equals(integerItem) || this.equals(doubleItem) || this.equals(decimalItem)
        ) {
            if (
                other.equals(integerItem)
                    ||
                    other.equals(doubleItem)
                    ||
                    other.equals(decimalItem)
                    ||
                    other.equals(booleanItem)
            )
                return true;
            else
                return false;
        }
        // base64 and hex can be cast between themselves
        if (this.equals(base64BinaryItem) || this.equals(hexBinaryItem)) {
            if (
                other.equals(base64BinaryItem)
                    ||
                    other.equals(hexBinaryItem)
            )
                return true;
            else
                return false;
        }
        // durations can be cast between themselves
        if (this.isSubtypeOf(durationItem)) {
            if (other.isSubtypeOf(durationItem))
                return true;
            else
                return false;
        }
        // DateTime can be cast also to Date or Time
        if (this.equals(dateTimeItem)) {
            if (other.equals(dateItem) || other.equals(timeItem))
                return true;
            else
                return false;
        }
        // Date can be cast also to DateTime
        if (this.equals(dateItem)) {
            if (other.equals(dateTimeItem))
                return true;
            else
                return false;
        }
        // Otherwise this cannot be casted to other
        return false;
    }

    // return [true] if this is a numeric type (i.e. [integerItem], [decimalItem] or [doubleItem]), false otherwise
    public boolean isNumeric() {
        return this.equals(integerItem) || this.equals(decimalItem) || this.equals(doubleItem);
    }

    // returns [true] if this can be promoted to string
    public boolean canBePromotedToString() {
        return this.equals(stringItem) || this.equals(anyURIItem);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
