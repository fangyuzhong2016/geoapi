/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2008-2012 Open Geospatial Consortium, Inc.
 *    All Rights Reserved. http://www.opengeospatial.org/ogc/legal
 *
 *    Permission to use, copy, and modify this software and its documentation, with
 *    or without modification, for any purpose and without fee or royalty is hereby
 *    granted, provided that you include the following on ALL copies of the software
 *    and documentation or portions thereof, including modifications, that you make:
 *
 *    1. The full text of this NOTICE in a location viewable to users of the
 *       redistributed or derivative work.
 *    2. Notice of any changes or modifications to the OGC files, including the
 *       date changes were made.
 *
 *    THIS SOFTWARE AND DOCUMENTATION IS PROVIDED "AS IS," AND COPYRIGHT HOLDERS MAKE
 *    NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 *    TO, WARRANTIES OF MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT
 *    THE USE OF THE SOFTWARE OR DOCUMENTATION WILL NOT INFRINGE ANY THIRD PARTY
 *    PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.
 *
 *    COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR
 *    CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE SOFTWARE OR DOCUMENTATION.
 *
 *    The name and trademarks of copyright holders may NOT be used in advertising or
 *    publicity pertaining to the software without specific, written prior permission.
 *    Title to copyright in this software and any associated documentation will at all
 *    times remain with copyright holders.
 */
package org.opengis.test.geometry;

import java.util.Arrays;

import org.opengis.geometry.*;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.opengis.test.Validator;
import org.opengis.test.ValidatorContainer;
import static org.opengis.test.Assert.*;


/**
 * Validates {@link Geometry} and related objects from the {@code org.opengis.geometry}
 * package.
 * <p>
 * This class is provided for users wanting to override the validation methods. When the default
 * behavior is sufficient, the {@link org.opengis.test.Validators} static methods provide a more
 * convenient way to validate various kinds of objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 3.0
 * @since   2.2
 */
public class GeometryValidator extends Validator {
    /**
     * Small relative tolerance values for comparisons of floating point numbers.
     * The default value is {@value org.opengis.test.Validator#DEFAULT_TOLERANCE}.
     * Implementors can change this value before to run the tests.
     */
    public double tolerance = DEFAULT_TOLERANCE;

    /**
     * Creates a new validator instance.
     *
     * @param container The set of validators to use for validating other kinds of objects
     *                  (see {@linkplain #container field javadoc}).
     */
    public GeometryValidator(final ValidatorContainer container) {
        super(container, "org.opengis.geometry");
    }

    /**
     * Validates the given envelope.
     *
     * @param object The object to validate, or {@code null}.
     */
    public void validate(final Envelope object) {
        if (object == null) {
            return;
        }
        final int dimension = object.getDimension();
        assertPositive("Envelope: dimension can't be negative.", dimension);
        final CoordinateReferenceSystem crs = object.getCoordinateReferenceSystem();
        container.validate(crs); // May be null.
        /*
         * Validates corners.
         */
        final DirectPosition lower = object.getLowerCorner();
        validate(lower);
        mandatory("Envelope: shall have a lower corner.", lower);
        if (lower != null) {
            assertEquals("Envelope: lower corner dimension shall be equal to the envelope dimension.",
                    dimension, lower.getDimension());
            if (crs != null) {
                CoordinateReferenceSystem check = lower.getCoordinateReferenceSystem();
                if (check != null) {
                    assertSame("Envelope: lower CRS shall be the same than the envelope CRS.", crs, check);
                }
            }
        }
        final DirectPosition upper = object.getUpperCorner();
        validate(upper);
        mandatory("Envelope: must have a upper corner.", upper);
        if (upper != null) {
            assertEquals("Envelope: upper corner dimension shall be equal to the envelope dimension.",
                    dimension, upper.getDimension());
            if (crs != null) {
                CoordinateReferenceSystem check = upper.getCoordinateReferenceSystem();
                if (check != null) {
                    assertSame("Envelope: upper CRS shall be the same than the envelope CRS.", crs, check);
                }
            }
        }
        /*
         * Validates minimal and maximal values against the corners.
         */
        for (int i=0; i<dimension; i++) {
            final double minimum = object.getMinimum(i);
            final double maximum = object.getMaximum(i);
            if (lower != null) {
                assertEquals("Envelope: minimum value shall be equal to the lower corner ordinate.",
                        lower.getOrdinate(i), minimum, 0.0); // No tolerance - we want exact match.
            }
            if (upper != null) {
                assertEquals("Envelope: maximum value shall be equal to the upper corner ordinate.",
                        upper.getOrdinate(i), maximum, 0.0); // No tolerance - we want exact match.
            }
            if (!Double.isNaN(minimum) && !Double.isNaN(maximum)) {
                assertValidRange("Envelope: invalid minimum and maximum ordinate values.", minimum, maximum);
            }
            final double span = maximum - minimum;
            final double eps = span * tolerance;
            assertEquals("Envelope: unexpected span value.", span, object.getSpan(i), eps);
            assertEquals("Envelope: unexpected median value.",
                    (maximum + minimum) / 2, object.getMedian(i), eps);
        }
    }

    /**
     * Validates the given position.
     *
     * @param object The object to validate, or {@code null}.
     */
    public void validate(final DirectPosition object) {
        if (object == null) {
            return;
        }
        /*
         * Checks coordinate consistency.
         */
        final int dimension = object.getDimension();
        assertPositive("DirectPosition: dimension can't be negative.", dimension);
        final double[] coordinate = object.getCoordinate();
        mandatory("DirectPosition: coordinate array can't be null.", coordinate);
        if (coordinate != null) {
            assertEquals("DirectPosition: coordinate array length shall be equal to the dimension.",
                    dimension, coordinate.length);
            for (int i=0; i<dimension; i++) {
                assertEquals("DirectPosition: getOrdinate(i) shall be the same than coordinate[i].",
                        coordinate[i], object.getOrdinate(i), 0.0); // No tolerance - we want exact match.
            }
        }
        /*
         * Checks coordinate validity in the CRS.
         */
        final CoordinateReferenceSystem crs = object.getCoordinateReferenceSystem();
        container.validate(crs); // May be null.
        int hashCode = 0;
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem(); // Assume already validated.
            if (cs != null) {
                assertEquals("DirectPosition: CRS dimension must matches the position dimension.",
                        dimension, cs.getDimension());
                for (int i=0; i<dimension; i++) {
                    final CoordinateSystemAxis axis = cs.getAxis(i); // Assume already validated.
                    if (axis != null) {
                        final double ordinate = coordinate[i];
                        final double minimum  = axis.getMinimumValue();
                        final double maximum  = axis.getMaximumValue();
                        final double eps = (maximum - minimum) * tolerance;
                        assertBetween("DirectPosition: ordinate out of axis bounds.",
                                minimum - eps, maximum + eps, ordinate);
                    }
                }
            }
            hashCode = crs.hashCode();
        }
        /*
         * Tests hash code values. It must be compliant to DirectPosition.hashCode()
         * contract stated in the javadoc.
         */
        hashCode += Arrays.hashCode(coordinate);
        assertEquals("DirectPosition: hashCode shall be compliant to the contract given in javadoc.",
                hashCode, object.hashCode());
        assertTrue("DirectPosition: shall be equal to itself.", object.equals(object));
        /*
         * Ensures that the array returned by DirectPosition.getCoordinate() is a clone.
         */
        for (int i=0; i<dimension; i++) {
            final double oldValue = coordinate[i];
            coordinate[i] *= 2;
            assertEquals("DirectPosition: coordinate array shall be cloned.",
                    oldValue, object.getOrdinate(i), 0.0); // No tolerance - we want exact match.
        }
    }
}
