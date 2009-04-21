/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.classifier;


/**
 * A real number feature attribute definition class.
 * <p>
 * @author Spence Koehler
 */
public class RealFeatureAttribute extends NumericFeatureAttribute {
  
  public RealFeatureAttribute(FeatureDictionary featureDictionary, String name) {
    super(featureDictionary, name);
  }

  /**
   * Convert the value to a double, if valid. Unless dictionary is locked, add
   * the value to this attribute definition.
   *
   * @return the Double if the value is valid; otherwise, null.
   */
  public Double toDouble(String value) {
    Double result = null;

    try {
      result = Double.parseDouble(value);
    }
    catch (NumberFormatException e) {
      //nothing to do: result is null.
    }

    return result;
  }

  public String toString() {
    return "R(" + getName() + ")";
  }

  /**
   * Test whether this numeric attribute is a real.
   */
  public final boolean isReal() {
    return true;
  }

  /**
   * Safely downcast this numeric feature attribute as a real if it is a real.
   */
  public final RealFeatureAttribute asReal() {
    return this;
  }
}
