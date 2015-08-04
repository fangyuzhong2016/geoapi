/*
 *    GeoAPI - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2011-2015 Open Geospatial Consortium, Inc.
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
package org.opengis.test.referencing.gigs;

import java.util.List;

import org.opengis.util.Factory;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.test.Configuration;
import org.opengis.test.FactoryFilter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assume.*;
import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertAxisDirectionsEqual;
import static org.opengis.test.referencing.gigs.GIGSTestCase.assertContainsCode;


/**
 * Verifies reference geodetic datums and CRSs bundled with the geoscience software.
 *
 * <table class="gigs" summary="Test description"><tr>
 *   <th>Test method:</th>
 *   <td>Compare geodetic datum and geocentric, geographic 3D and geographic 2D CRS definitions
 *       included in the geoscience software against the EPSG Dataset.</td>
 * </tr><tr>
 *   <th>Test data:</th>
 *   <td>Tests for component logical consistency: for example, if a higher-level library-defined component
 *       such as ED50 datum is selected it should then not be possible to change any of its lower-level
 *       components such as the ellipsoid from the pre-defined value (in this example International 1924).</td>
 * </tr><tr>
 *   <th>Tested API:</th>
 *   <td>{@link DatumAuthorityFactory#createGeodeticDatum(String)},<br>
 *       {@link CRSAuthorityFactory#createGeographicCRS(String)} and<br>
 *       {@link CRSAuthorityFactory#createGeocentricCRS(String)}.</td>
 * </tr><tr>
 *   <th>Expected result:</th>
 *   <td>Definitions bundled with the software should have the same name and associated ellipsoid and prime meridian
 *       as in the EPSG Dataset. CRSs missing from the software or at variance with those in the EPSG Dataset should
 *       be reported.</td>
 * </tr></table>
 *
 * @author  GIGS (IOGP)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 3.1
 * @since   3.1
 */
@RunWith(Parameterized.class)
public class Test2004 extends EPSGTestCase<GeodeticDatum> {
    /**
     * The expected axis directions of two-dimensional geographic CRS.
     */
    private static final AxisDirection[] GEOGRAPHIC_2D = {
        AxisDirection.NORTH,
        AxisDirection.EAST
    };

    /**
     * The expected axis directions of three-dimensional geographic CRS.
     */
    private static final AxisDirection[] GEOGRAPHIC_3D = {
        AxisDirection.NORTH,
        AxisDirection.EAST,
        AxisDirection.UP
    };

    /**
     * The expected axis directions of geocentric CRS.
     */
    private static final AxisDirection[] GEOCENTRIC = {
        AxisDirection.GEOCENTRIC_X,
        AxisDirection.GEOCENTRIC_Y,
        AxisDirection.GEOCENTRIC_Z
    };

    /**
     * The name of the expected ellipsoid.
     */
    public String ellipsoid;

    /**
     * The name of the expected prime meridian.
     */
    public String primeMeridian;

    /**
     * Name of a coordinate reference system using the datum.
     */
    private String crsName;

    /**
     * The datum created by the factory,
     * or {@code null} if not yet created or if datum creation failed.
     *
     * @see #datumAuthorityFactory
     */
    private GeodeticDatum datum;

    /**
     * Factory to use for building {@link GeodeticDatum} instances, or {@code null} if none.
     * This is the factory used by the {@link #getIdentifiedObject()} method.
     */
    protected final DatumAuthorityFactory datumAuthorityFactory;

    /**
     * Factory to use for building {@link GeodeticCRS} instances, or {@code null} if none.
     */
    protected final CRSAuthorityFactory crsAuthorityFactory;

    /**
     * A temporary buffer for creating error message.
     */
    private StringBuilder prefix;

    /**
     * Returns a default set of factories to use for running the tests. Those factories are given
     * in arguments to the constructor when this test class is instantiated directly by JUnit (for
     * example as a {@linkplain org.junit.runners.Suite.SuiteClasses suite} element), instead than
     * subclassed by the implementor. The factories are fetched as documented in the
     * {@link #factories(Class[])} javadoc.
     *
     * @return The default set of arguments to be given to the {@code Test2004} constructor.
     */
    @Parameterized.Parameters
    @SuppressWarnings("unchecked")
    public static List<Factory[]> factories() {
        return factories(FactoryFilter.ByAuthority.EPSG, DatumAuthorityFactory.class, CRSAuthorityFactory.class);
    }

    /**
     * Creates a new test using the given factories. If a given factory is {@code null},
     * then the tests which depend on it will be skipped.
     *
     * @param datumFactory Factory for creating {@link GeodeticDatum} instances.
     * @param crsFactory Factory for creating {@link GeodeticCRS} instances.
     */
    public Test2004(final DatumAuthorityFactory datumFactory, final CRSAuthorityFactory crsFactory) {
        super(datumFactory);
        datumAuthorityFactory = datumFactory;
        crsAuthorityFactory = crsFactory;
    }

    /**
     * Returns information about the configuration of the test which has been run.
     * This method returns a map containing:
     *
     * <ul>
     *   <li>All the following values associated to the {@link org.opengis.test.Configuration.Key} of the same name:
     *     <ul>
     *       <li>{@link #isStandardNameSupported}</li>
     *       <li>{@link #isStandardAliasSupported}</li>
     *       <li>{@link #isDependencyIdentificationSupported}</li>
     *       <li>{@link #datumAuthorityFactory}</li>
     *       <li>{@link #crsAuthorityFactory}</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @return The configuration of the test being run.
     */
    @Override
    public Configuration configuration() {
        final Configuration op = super.configuration();
        assertNull(op.put(Configuration.Key.datumAuthorityFactory, datumAuthorityFactory));
        assertNull(op.put(Configuration.Key.crsAuthorityFactory, crsAuthorityFactory));
        return op;
    }

    /**
     * Returns the datum instance to be tested. When this method is invoked for the first time, it creates the
     * datum to test by invoking the {@link DatumAuthorityFactory#createGeodeticDatum(String)} method with the
     * current {@link #code} value in argument. The created object is then cached and returned in all subsequent
     * invocations of this method.
     *
     * @return The datum instance to test.
     * @throws FactoryException if an error occurred while creating the datum instance.
     */
    @Override
    public GeodeticDatum getIdentifiedObject() throws FactoryException {
        if (datum == null) {
            assumeNotNull(datumAuthorityFactory);
            try {
                datum = datumAuthorityFactory.createGeodeticDatum(String.valueOf(code));
            } catch (NoSuchAuthorityCodeException e) {
                unsupportedCode(GeodeticDatum.class, code);
                throw e;
            }
            if (datum == null) {
                fail("DatumAuthorityFactory.createGeodeticDatum(\"" + code + "\") shall not return null.");
            }
        }
        return datum;
    }

    /**
     * Creates a geodetic datum for the current {@link #code}, then verifies its name and properties.
     */
    private void createAndVerifyDatum() throws FactoryException {
        assumeTrue(datumAuthorityFactory != null || crsAuthorityFactory != null);
        if (datumAuthorityFactory != null) {
            final GeodeticDatum datum = getIdentifiedObject();
            validators.validate(datum);

            prefix = new StringBuilder().append("GeodeticDatum[").append(code).append(']');
            assertNotNull(prefix.toString(), datum);

            prefix.append('.');
            verifyGeodeticDatum(datum);
        }
    }

    /**
     * Creates a geographic CRS for the given code and verify its properties.
     *
     * @param  crsCode The code of the CRS to create.
     * @param  expectedDirections Either {@link #GEOGRAPHIC_2D} or {@link #GEOGRAPHIC_3D}.
     * @throws FactoryException if an error occurred while creating the CRS instance.
     */
    private void createAndVerifyGeographicCRS(final int crsCode, final AxisDirection[] expectedDirections) throws FactoryException {
        if (crsAuthorityFactory != null) {
            final GeographicCRS crs;
            try {
                crs = crsAuthorityFactory.createGeographicCRS(String.valueOf(crsCode));
            } catch (NoSuchAuthorityCodeException e) {
                unsupportedCode(GeographicCRS.class, crsCode);
                throw e;
            }
            if (crs == null) {
                fail("CRSAuthorityFactory.createGeographicCRS(\"" + code + "\") shall not return null.");
            }
            validators.validate(crs);
            verifyGeodeticCRS(crsCode, crs, expectedDirections);
        }
    }

    /**
     * Creates a geodetic CRS for the given code and verify its properties.
     *
     * @param  crsCode The code of the CRS to create.
     * @throws FactoryException if an error occurred while creating the CRS instance.
     */
    private void createAndVerifyGeocentricCRS(final int crsCode) throws FactoryException {
        if (crsAuthorityFactory != null) {
            final GeocentricCRS crs;
            try {
                crs = crsAuthorityFactory.createGeocentricCRS(String.valueOf(crsCode));
            } catch (NoSuchAuthorityCodeException e) {
                unsupportedCode(GeocentricCRS.class, crsCode);
                throw e;
            }
            if (crs == null) {
                fail("CRSAuthorityFactory.createGeocentricCRS(\"" + code + "\") shall not return null.");
            }
            validators.validate(crs);
            verifyGeodeticCRS(crsCode, crs, GEOCENTRIC);
        }
    }

    /**
     * Verifies the given geographic or geocentric CRS.
     *
     * @param crsCode The code of the created CRS.
     * @param crs The CRS to verify.
     * @param expectedDirections Either {@link #GEOGRAPHIC_2D}, {@link #GEOGRAPHIC_3D} or {@link #GEOCENTRIC}.
     */
    private void verifyGeodeticCRS(final int crsCode, final GeodeticCRS crs, final AxisDirection[] expectedDirections) {
        prefix.setLength(0);
        prefix.append("GeodeticCRS[").append(crsCode).append(']');
        assertNotNull(prefix.toString(), crs);

        prefix.append('.');
        assertContainsCode(message(prefix, "getIdentifiers()"), "EPSG", crsCode, crs.getIdentifiers());

        if (isStandardNameSupported) {
            configurationTip = Configuration.Key.isStandardNameSupported;
            assertEquals(message(prefix, "getName()"), crsName, getName(crs));
            configurationTip = null;
        }
        /*
         * Verify the axis directions in the coordinate system.
         */
        final CoordinateSystem cs = crs.getCoordinateSystem();
        final int lengthAfterCRS = prefix.length();
        prefix.append("getCoordinateSystem()");
        assertNotNull(prefix.toString(), cs);

        prefix.append('.');
        assertEquals(message(prefix, "getDimension()"), expectedDirections.length, cs.getDimension());
        assertAxisDirectionsEqual(message(prefix, "axes"), cs, expectedDirections);

        prefix.setLength(lengthAfterCRS);
        prefix.append("getDatum()");
        final GeodeticDatum crsDatum = crs.getDatum();
        assertNotNull(prefix.toString(), crsDatum);

        prefix.append('.');
        verifyGeodeticDatum(crsDatum);
    }

    /**
     * Verifies the name, ellipsoid and prime meridian of the given datum.
     */
    private void verifyGeodeticDatum(final GeodeticDatum toVerify) {
        /*
         * If the datum has been obtained directly from the datum factory (toVerify == datum), test its
         * identifier unconditionally. Otherwise (for all datum obtained indirectly from a CRS), verify
         * the identifier only if the implementation supports identification of associated objects.
         */
        final int lengthAfterDatum = prefix.length();
        if (isDependencyIdentificationSupported || (toVerify == datum)) {
            configurationTip = Configuration.Key.isDependencyIdentificationSupported;
            assertContainsCode(message(prefix, "getIdentifiers()"), "EPSG", code, toVerify.getIdentifiers());

            if (isStandardNameSupported) {
                configurationTip = Configuration.Key.isStandardNameSupported;
                assertEquals(message(prefix, "getName()"), name, getName(toVerify));
            }
            configurationTip = null;
        }
        /*
         * Verify the ellipsoid name.
         */
        final Ellipsoid e = toVerify.getEllipsoid();
        prefix.append("getEllipsoid()");
        assertNotNull(prefix.toString(), e);

        prefix.append('.');
        if (isDependencyIdentificationSupported && isStandardNameSupported) {
            configurationTip = Configuration.Key.isDependencyIdentificationSupported;
            assertEquals(message(prefix, "getName()"), ellipsoid, getName(e));
            configurationTip = null;
        }
        /*
         * Verify the prime meridian name.
         */
        final PrimeMeridian pm = toVerify.getPrimeMeridian();
        prefix.setLength(lengthAfterDatum);
        prefix.append("getPrimeMeridian()");
        assertNotNull(prefix.toString(), pm);

        prefix.append('.');
        if (isDependencyIdentificationSupported && isStandardNameSupported) {
            configurationTip = Configuration.Key.isDependencyIdentificationSupported;
            assertEquals(message(prefix, "getName()"), primeMeridian, getName(pm));
            configurationTip = null;
        }
    }

    /**
     * Tests “Abidjan 1987” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6143</b></li>
     *   <li>EPSG datum name: <b>Abidjan 1987</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Abidjan 1987</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAbidjan1987() throws FactoryException {
        important     = true;
        code          = 6143;
        name          = "Abidjan 1987";
        crsName       = "Abidjan 1987";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4143, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Accra” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6168</b></li>
     *   <li>EPSG datum name: <b>Accra</b></li>
     *   <li>Ellipsoid name: <b>War Office</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Accra</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAccra() throws FactoryException {
        important     = true;
        code          = 6168;
        name          = "Accra";
        crsName       = "Accra";
        ellipsoid     = "War Office";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4168, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Australian Geodetic Datum 1966” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6202</b></li>
     *   <li>EPSG datum name: <b>Australian Geodetic Datum 1966</b></li>
     *   <li>Ellipsoid name: <b>Australian National Spheroid</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>AGD66</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAustralianGeodeticDatum1966() throws FactoryException {
        important     = true;
        code          = 6202;
        name          = "Australian Geodetic Datum 1966";
        crsName       = "AGD66";
        ellipsoid     = "Australian National Spheroid";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4202, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Australian Geodetic Datum 1984” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6203</b></li>
     *   <li>EPSG datum name: <b>Australian Geodetic Datum 1984</b></li>
     *   <li>Ellipsoid name: <b>Australian National Spheroid</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>AGD84</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAustralianGeodeticDatum1984() throws FactoryException {
        important     = true;
        code          = 6203;
        name          = "Australian Geodetic Datum 1984";
        crsName       = "AGD84";
        ellipsoid     = "Australian National Spheroid";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4203, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Ain el Abd 1970” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6204</b></li>
     *   <li>EPSG datum name: <b>Ain el Abd 1970</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Ain el Abd</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAinElAbd1970() throws FactoryException {
        important     = true;
        code          = 6204;
        name          = "Ain el Abd 1970";
        crsName       = "Ain el Abd";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4204, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Amersfoort” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6289</b></li>
     *   <li>EPSG datum name: <b>Amersfoort</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Amersfoort</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAmersfoort() throws FactoryException {
        important     = true;
        code          = 6289;
        name          = "Amersfoort";
        crsName       = "Amersfoort";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4289, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Aratu” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6208</b></li>
     *   <li>EPSG datum name: <b>Aratu</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Aratu</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAratu() throws FactoryException {
        important     = true;
        code          = 6208;
        name          = "Aratu";
        crsName       = "Aratu";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4208, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Batavia” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6211</b></li>
     *   <li>EPSG datum name: <b>Batavia</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Batavia</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBatavia() throws FactoryException {
        important     = true;
        code          = 6211;
        name          = "Batavia";
        crsName       = "Batavia";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4211, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Batavia (Jakarta)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6813</b></li>
     *   <li>EPSG datum name: <b>Batavia (Jakarta)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Jakarta</b></li>
     *   <li>CRS using the datum: <b>Batavia (Jakarta)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBataviaJakarta() throws FactoryException {
        important     = true;
        code          = 6813;
        name          = "Batavia (Jakarta)";
        crsName       = "Batavia (Jakarta)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Jakarta";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4813, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Beijing 1954” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6214</b></li>
     *   <li>EPSG datum name: <b>Beijing 1954</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Beijing 1954</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBeijing1954() throws FactoryException {
        important     = true;
        code          = 6214;
        name          = "Beijing 1954";
        crsName       = "Beijing 1954";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4214, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bogota 1975” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6218</b></li>
     *   <li>EPSG datum name: <b>Bogota 1975</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Bogota 1975</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBogota1975() throws FactoryException {
        important     = true;
        code          = 6218;
        name          = "Bogota 1975";
        crsName       = "Bogota 1975";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4218, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Camacupa” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6220</b></li>
     *   <li>EPSG datum name: <b>Camacupa</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Camacupa</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCamacupa() throws FactoryException {
        important     = true;
        code          = 6220;
        name          = "Camacupa";
        crsName       = "Camacupa";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4220, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Campo Inchauspe” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6221</b></li>
     *   <li>EPSG datum name: <b>Campo Inchauspe</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Campo Inchauspe</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCampoInchauspe() throws FactoryException {
        important     = true;
        code          = 6221;
        name          = "Campo Inchauspe";
        crsName       = "Campo Inchauspe";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4221, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Carthage” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6223</b></li>
     *   <li>EPSG datum name: <b>Carthage</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Carthage</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCarthage() throws FactoryException {
        important     = true;
        code          = 6223;
        name          = "Carthage";
        crsName       = "Carthage";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4223, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Chos Malal 1914” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6160</b></li>
     *   <li>EPSG datum name: <b>Chos Malal 1914</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Chos Malal 1914</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testChosMalal1914() throws FactoryException {
        important     = true;
        code          = 6160;
        name          = "Chos Malal 1914";
        crsName       = "Chos Malal 1914";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4160, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Dealul Piscului 1930” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6316</b></li>
     *   <li>EPSG datum name: <b>Dealul Piscului 1930</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Dealul Piscului 1930</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDealulPiscului1930() throws FactoryException {
        important     = true;
        code          = 6316;
        name          = "Dealul Piscului 1930";
        crsName       = "Dealul Piscului 1930";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4316, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Deir ez Zor” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6227</b></li>
     *   <li>EPSG datum name: <b>Deir ez Zor</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Deir ez Zor</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDeirEzZor() throws FactoryException {
        important     = true;
        code          = 6227;
        name          = "Deir ez Zor";
        crsName       = "Deir ez Zor";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4227, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Datum Geodesi Nasional 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6755</b></li>
     *   <li>EPSG datum name: <b>Datum Geodesi Nasional 1995</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>DGN95</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDatumGeodesiNasional1995() throws FactoryException {
        important     = true;
        code          = 6755;
        name          = "Datum Geodesi Nasional 1995";
        crsName       = "DGN95";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4897);
        createAndVerifyGeographicCRS(4898, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4755, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Deutsches Hauptdreiecksnetz” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6314</b></li>
     *   <li>EPSG datum name: <b>Deutsches Hauptdreiecksnetz</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>DHDN</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDeutschesHauptdreiecksnetz() throws FactoryException {
        important     = true;
        code          = 6314;
        name          = "Deutsches Hauptdreiecksnetz";
        crsName       = "DHDN";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4314, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Douala 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6192</b></li>
     *   <li>EPSG datum name: <b>Douala 1948</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Douala 1948</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDouala1948() throws FactoryException {
        important     = true;
        code          = 6192;
        name          = "Douala 1948";
        crsName       = "Douala 1948";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4192, GEOGRAPHIC_2D);
    }

    /**
     * Tests “European Datum 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6230</b></li>
     *   <li>EPSG datum name: <b>European Datum 1950</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ED50</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEuropeanDatum1950() throws FactoryException {
        important     = true;
        code          = 6230;
        name          = "European Datum 1950";
        crsName       = "ED50";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4230, GEOGRAPHIC_2D);
    }

    /**
     * Tests “European Datum 1950(1977)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6154</b></li>
     *   <li>EPSG datum name: <b>European Datum 1950(1977)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ED50(ED77)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEuropeanDatum19501977() throws FactoryException {
        important     = true;
        code          = 6154;
        name          = "European Datum 1950(1977)";
        crsName       = "ED50(ED77)";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4154, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Egypt 1907” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6229</b></li>
     *   <li>EPSG datum name: <b>Egypt 1907</b></li>
     *   <li>Ellipsoid name: <b>Helmert 1906</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Egypt 1907</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEgypt1907() throws FactoryException {
        important     = true;
        code          = 6229;
        name          = "Egypt 1907";
        crsName       = "Egypt 1907";
        ellipsoid     = "Helmert 1906";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4229, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Egypt Gulf of Suez S-650 TL” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6706</b></li>
     *   <li>EPSG datum name: <b>Egypt Gulf of Suez S-650 TL</b></li>
     *   <li>Ellipsoid name: <b>Helmert 1906</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Egypt Gulf of Suez S-650 TL</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEgyptGulfOfSuezS650TL() throws FactoryException {
        important     = true;
        code          = 6706;
        name          = "Egypt Gulf of Suez S-650 TL";
        crsName       = "Egypt Gulf of Suez S-650 TL";
        ellipsoid     = "Helmert 1906";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4706, GEOGRAPHIC_2D);
    }

    /**
     * Tests “European Libyan Datum 1979” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6159</b></li>
     *   <li>EPSG datum name: <b>European Libyan Datum 1979</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ELD79</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEuropeanLibyanDatum1979() throws FactoryException {
        important     = true;
        code          = 6159;
        name          = "European Libyan Datum 1979";
        crsName       = "ELD79";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4159, GEOGRAPHIC_2D);
    }

    /**
     * Tests “European Terrestrial Reference System 1989” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6258</b></li>
     *   <li>EPSG datum name: <b>European Terrestrial Reference System 1989</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ETRS89</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEuropeanTerrestrialReferenceSystem1989() throws FactoryException {
        important     = true;
        code          = 6258;
        name          = "European Terrestrial Reference System 1989";
        crsName       = "ETRS89";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4936);
        createAndVerifyGeographicCRS(4937, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4258, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Fahud” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6232</b></li>
     *   <li>EPSG datum name: <b>Fahud</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Fahud</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFahud() throws FactoryException {
        important     = true;
        code          = 6232;
        name          = "Fahud";
        crsName       = "Fahud";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4232, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Final Datum 1958” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6132</b></li>
     *   <li>EPSG datum name: <b>Final Datum 1958</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>FD58</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFinalDatum1958() throws FactoryException {
        important     = true;
        code          = 6132;
        name          = "Final Datum 1958";
        crsName       = "FD58";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4132, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Geocentric Datum of Australia 1994” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6283</b></li>
     *   <li>EPSG datum name: <b>Geocentric Datum of Australia 1994</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>GDA94</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGeocentricDatumOfAustralia1994() throws FactoryException {
        important     = true;
        code          = 6283;
        name          = "Geocentric Datum of Australia 1994";
        crsName       = "GDA94";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4938);
        createAndVerifyGeographicCRS(4939, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4283, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Geodetic Datum of Malaysia 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6742</b></li>
     *   <li>EPSG datum name: <b>Geodetic Datum of Malaysia 2000</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>GDM2000</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGeodeticDatumOfMalaysia2000() throws FactoryException {
        important     = true;
        code          = 6742;
        name          = "Geodetic Datum of Malaysia 2000";
        crsName       = "GDM2000";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4920);
        createAndVerifyGeographicCRS(4921, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4742, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hungarian Datum 1972” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6237</b></li>
     *   <li>EPSG datum name: <b>Hungarian Datum 1972</b></li>
     *   <li>Ellipsoid name: <b>GRS 1967</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>HD72</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHungarianDatum1972() throws FactoryException {
        important     = true;
        code          = 6237;
        name          = "Hungarian Datum 1972";
        crsName       = "HD72";
        ellipsoid     = "GRS 1967";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4237, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hito XVIII 1963” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6254</b></li>
     *   <li>EPSG datum name: <b>Hito XVIII 1963</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hito XVIII 1963</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHitoXVIII1963() throws FactoryException {
        important     = true;
        code          = 6254;
        name          = "Hito XVIII 1963";
        crsName       = "Hito XVIII 1963";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4254, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Indonesian Datum 1974” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6238</b></li>
     *   <li>EPSG datum name: <b>Indonesian Datum 1974</b></li>
     *   <li>Ellipsoid name: <b>Indonesian National Spheroid</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ID74</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIndonesianDatum1974() throws FactoryException {
        important     = true;
        code          = 6238;
        name          = "Indonesian Datum 1974";
        crsName       = "ID74";
        ellipsoid     = "Indonesian National Spheroid";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4238, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN Astro 1960” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6700</b></li>
     *   <li>EPSG datum name: <b>IGN Astro 1960</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN Astro 1960</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGNAstro1960() throws FactoryException {
        important     = true;
        code          = 6700;
        name          = "IGN Astro 1960";
        crsName       = "IGN Astro 1960";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4700, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Indian 1954” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6239</b></li>
     *   <li>EPSG datum name: <b>Indian 1954</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1937 Adjustment)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Indian 1954</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIndian1954() throws FactoryException {
        important     = true;
        code          = 6239;
        name          = "Indian 1954";
        crsName       = "Indian 1954";
        ellipsoid     = "Everest 1830 (1937 Adjustment)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4239, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Indian 1960” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6131</b></li>
     *   <li>EPSG datum name: <b>Indian 1960</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1937 Adjustment)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Indian 1960</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIndian1960() throws FactoryException {
        important     = true;
        code          = 6131;
        name          = "Indian 1960";
        crsName       = "Indian 1960";
        ellipsoid     = "Everest 1830 (1937 Adjustment)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4131, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Indian 1975” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6240</b></li>
     *   <li>EPSG datum name: <b>Indian 1975</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1937 Adjustment)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Indian 1975</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIndian1975() throws FactoryException {
        important     = true;
        code          = 6240;
        name          = "Indian 1975";
        crsName       = "Indian 1975";
        ellipsoid     = "Everest 1830 (1937 Adjustment)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4240, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kalianpur 1937” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6144</b></li>
     *   <li>EPSG datum name: <b>Kalianpur 1937</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1937 Adjustment)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kalianpur 1937</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKalianpur1937() throws FactoryException {
        important     = true;
        code          = 6144;
        name          = "Kalianpur 1937";
        crsName       = "Kalianpur 1937";
        ellipsoid     = "Everest 1830 (1937 Adjustment)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4144, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kalianpur 1962” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6145</b></li>
     *   <li>EPSG datum name: <b>Kalianpur 1962</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1962 Definition)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kalianpur 1962</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKalianpur1962() throws FactoryException {
        important     = true;
        code          = 6145;
        name          = "Kalianpur 1962";
        crsName       = "Kalianpur 1962";
        ellipsoid     = "Everest 1830 (1962 Definition)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4145, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kalianpur 1975” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6146</b></li>
     *   <li>EPSG datum name: <b>Kalianpur 1975</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1975 Definition)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kalianpur 1975</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKalianpur1975() throws FactoryException {
        important     = true;
        code          = 6146;
        name          = "Kalianpur 1975";
        crsName       = "Kalianpur 1975";
        ellipsoid     = "Everest 1830 (1975 Definition)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4146, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kertau 1968” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6245</b></li>
     *   <li>EPSG datum name: <b>Kertau 1968</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 Modified</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kertau 1968</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKertau1968() throws FactoryException {
        important     = true;
        code          = 6245;
        name          = "Kertau 1968";
        crsName       = "Kertau 1968";
        ellipsoid     = "Everest 1830 Modified";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4245, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kuwait Oil Company” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6246</b></li>
     *   <li>EPSG datum name: <b>Kuwait Oil Company</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>KOC</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKuwaitOilCompany() throws FactoryException {
        important     = true;
        code          = 6246;
        name          = "Kuwait Oil Company";
        crsName       = "KOC";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4246, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Libyan Geodetic Datum 2006” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6754</b></li>
     *   <li>EPSG datum name: <b>Libyan Geodetic Datum 2006</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>LGD2006</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLibyanGeodeticDatum2006() throws FactoryException {
        important     = true;
        code          = 6754;
        name          = "Libyan Geodetic Datum 2006";
        crsName       = "LGD2006";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4899);
        createAndVerifyGeographicCRS(4900, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4754, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Luzon 1911” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6253</b></li>
     *   <li>EPSG datum name: <b>Luzon 1911</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Luzon 1911</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLuzon1911() throws FactoryException {
        important     = true;
        code          = 6253;
        name          = "Luzon 1911";
        crsName       = "Luzon 1911";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4253, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Marco Geocentrico Nacional de Referencia” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6686</b></li>
     *   <li>EPSG datum name: <b>Marco Geocentrico Nacional de Referencia</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>MAGNA-SIRGAS</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMarcoGeocentricoNacionalDeReferencia() throws FactoryException {
        important     = true;
        code          = 6686;
        name          = "Marco Geocentrico Nacional de Referencia";
        crsName       = "MAGNA-SIRGAS";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4996);
        createAndVerifyGeographicCRS(4997, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4686, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Malongo 1987” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6259</b></li>
     *   <li>EPSG datum name: <b>Malongo 1987</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Malongo 1987</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMalongo1987() throws FactoryException {
        important     = true;
        code          = 6259;
        name          = "Malongo 1987";
        crsName       = "Malongo 1987";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4259, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Manoca 1962” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6193</b></li>
     *   <li>EPSG datum name: <b>Manoca 1962</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Manoca 1962</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testManoca1962() throws FactoryException {
        important     = true;
        code          = 6193;
        name          = "Manoca 1962";
        crsName       = "Manoca 1962";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4193, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Mauritania 1999” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6702</b></li>
     *   <li>EPSG datum name: <b>Mauritania 1999</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Mauritania 1999</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMauritania1999() throws FactoryException {
        important     = true;
        code          = 6702;
        name          = "Mauritania 1999";
        crsName       = "Mauritania 1999";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4924);
        createAndVerifyGeographicCRS(4925, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4702, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Militar-Geographische Institut” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6312</b></li>
     *   <li>EPSG datum name: <b>Militar-Geographische Institut</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>MGI</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMilitarGeographischeInstitut() throws FactoryException {
        important     = true;
        code          = 6312;
        name          = "Militar-Geographische Institut";
        crsName       = "MGI";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4312, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Militar-Geographische Institut (Ferro)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6805</b></li>
     *   <li>EPSG datum name: <b>Militar-Geographische Institut (Ferro)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Ferro</b></li>
     *   <li>CRS using the datum: <b>MGI (Ferro)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMilitarGeographischeInstitutFerro() throws FactoryException {
        important     = true;
        code          = 6805;
        name          = "Militar-Geographische Institut (Ferro)";
        crsName       = "MGI (Ferro)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Ferro";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4805, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Mhast (offshore)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6705</b></li>
     *   <li>EPSG datum name: <b>Mhast (offshore)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Mhast (offshore)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMhastOffshore() throws FactoryException {
        important     = true;
        code          = 6705;
        name          = "Mhast (offshore)";
        crsName       = "Mhast (offshore)";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4705, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Mhast (onshore)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6704</b></li>
     *   <li>EPSG datum name: <b>Mhast (onshore)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Mhast (onshore)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMhastOnshore() throws FactoryException {
        important     = true;
        code          = 6704;
        name          = "Mhast (onshore)";
        crsName       = "Mhast (onshore)";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4704, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Minna” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6263</b></li>
     *   <li>EPSG datum name: <b>Minna</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Minna</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMinna() throws FactoryException {
        important     = true;
        code          = 6263;
        name          = "Minna";
        crsName       = "Minna";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4263, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Monte Mario” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6265</b></li>
     *   <li>EPSG datum name: <b>Monte Mario</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Monte Mario</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMonteMario() throws FactoryException {
        important     = true;
        code          = 6265;
        name          = "Monte Mario";
        crsName       = "Monte Mario";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4265, GEOGRAPHIC_2D);
    }

    /**
     * Tests “M'poraloko” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6266</b></li>
     *   <li>EPSG datum name: <b>M'poraloko</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>M'poraloko</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMPoraloko() throws FactoryException {
        important     = true;
        code          = 6266;
        name          = "M'poraloko";
        crsName       = "M'poraloko";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4266, GEOGRAPHIC_2D);
    }

    /**
     * Tests “North American Datum 1927” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6267</b></li>
     *   <li>EPSG datum name: <b>North American Datum 1927</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD27</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNorthAmericanDatum1927() throws FactoryException {
        important     = true;
        code          = 6267;
        name          = "North American Datum 1927";
        crsName       = "NAD27";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4267, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NAD27 Michigan” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6268</b></li>
     *   <li>EPSG datum name: <b>NAD27 Michigan</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866 Michigan</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD27 Michigan</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNAD27Michigan() throws FactoryException {
        important     = true;
        code          = 6268;
        name          = "NAD27 Michigan";
        crsName       = "NAD27 Michigan";
        ellipsoid     = "Clarke 1866 Michigan";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4268, GEOGRAPHIC_2D);
    }

    /**
     * Tests “North American Datum 1983” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6269</b></li>
     *   <li>EPSG datum name: <b>North American Datum 1983</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD83</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNorthAmericanDatum1983() throws FactoryException {
        important     = true;
        code          = 6269;
        name          = "North American Datum 1983";
        crsName       = "NAD83";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4269, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NAD83 Canadian Spatial Reference System” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6140</b></li>
     *   <li>EPSG datum name: <b>NAD83 Canadian Spatial Reference System</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD83(CSRS)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNAD83CanadianSpatialReferenceSystem() throws FactoryException {
        important     = true;
        code          = 6140;
        name          = "NAD83 Canadian Spatial Reference System";
        crsName       = "NAD83(CSRS)";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4954);
        createAndVerifyGeographicCRS(4955, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4617, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NAD83 (High Accuracy Reference Network)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6152</b></li>
     *   <li>EPSG datum name: <b>NAD83 (High Accuracy Reference Network)</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD83(HARN)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNAD83HighAccuracyReferenceNetwork() throws FactoryException {
        important     = true;
        code          = 6152;
        name          = "NAD83 (High Accuracy Reference Network)";
        crsName       = "NAD83(HARN)";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4956);
        createAndVerifyGeographicCRS(4957, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4152, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NAD83 (National Spatial Reference System 2007)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6759</b></li>
     *   <li>EPSG datum name: <b>NAD83 (National Spatial Reference System 2007)</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD83(NSRS2007)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNAD83NationalSpatialReferenceSystem2007() throws FactoryException {
        important     = true;
        code          = 6759;
        name          = "NAD83 (National Spatial Reference System 2007)";
        crsName       = "NAD83(NSRS2007)";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4892);
        createAndVerifyGeographicCRS(4893, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4759, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nahrwan 1967” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6270</b></li>
     *   <li>EPSG datum name: <b>Nahrwan 1967</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Nahrwan 1967</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNahrwan1967() throws FactoryException {
        important     = true;
        code          = 6270;
        name          = "Nahrwan 1967";
        crsName       = "Nahrwan 1967";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4270, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Naparima 1955” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6158</b></li>
     *   <li>EPSG datum name: <b>Naparima 1955</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Naparima 1955</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNaparima1955() throws FactoryException {
        important     = true;
        code          = 6158;
        name          = "Naparima 1955";
        crsName       = "Naparima 1955";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4158, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nord Sahara 1959” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6307</b></li>
     *   <li>EPSG datum name: <b>Nord Sahara 1959</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Nord Sahara 1959</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNordSahara1959() throws FactoryException {
        important     = true;
        code          = 6307;
        name          = "Nord Sahara 1959";
        crsName       = "Nord Sahara 1959";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4307, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nouvelle Triangulation Francaise” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6275</b></li>
     *   <li>EPSG datum name: <b>Nouvelle Triangulation Francaise</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NTF</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNouvelleTriangulationFrancaise() throws FactoryException {
        important     = true;
        code          = 6275;
        name          = "Nouvelle Triangulation Francaise";
        crsName       = "NTF";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4275, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nouvelle Triangulation Francaise (Paris)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6807</b></li>
     *   <li>EPSG datum name: <b>Nouvelle Triangulation Francaise (Paris)</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Paris</b></li>
     *   <li>CRS using the datum: <b>NTF (Paris)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNouvelleTriangulationFrancaiseParis() throws FactoryException {
        important     = true;
        code          = 6807;
        name          = "Nouvelle Triangulation Francaise (Paris)";
        crsName       = "NTF (Paris)";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Paris";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4807, GEOGRAPHIC_2D);
    }

    /**
     * Tests “New Zealand Geodetic Datum 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6167</b></li>
     *   <li>EPSG datum name: <b>New Zealand Geodetic Datum 2000</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NZGD2000</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNewZealandGeodeticDatum2000() throws FactoryException {
        important     = true;
        code          = 6167;
        name          = "New Zealand Geodetic Datum 2000";
        crsName       = "NZGD2000";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4958);
        createAndVerifyGeographicCRS(4959, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4167, GEOGRAPHIC_2D);
    }

    /**
     * Tests “New Zealand Geodetic Datum 1949” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6272</b></li>
     *   <li>EPSG datum name: <b>New Zealand Geodetic Datum 1949</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NZGD49</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNewZealandGeodeticDatum1949() throws FactoryException {
        important     = true;
        code          = 6272;
        name          = "New Zealand Geodetic Datum 1949";
        crsName       = "NZGD49";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4272, GEOGRAPHIC_2D);
    }

    /**
     * Tests “OSGB 1936” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6277</b></li>
     *   <li>EPSG datum name: <b>OSGB 1936</b></li>
     *   <li>Ellipsoid name: <b>Airy 1830</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>OSGB 1936</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testOSGB1936() throws FactoryException {
        important     = true;
        code          = 6277;
        name          = "OSGB 1936";
        crsName       = "OSGB 1936";
        ellipsoid     = "Airy 1830";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4277, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Padang 1884” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6280</b></li>
     *   <li>EPSG datum name: <b>Padang 1884</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Padang</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPadang1884() throws FactoryException {
        important     = true;
        code          = 6280;
        name          = "Padang 1884";
        crsName       = "Padang";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4280, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Padang 1884 (Jakarta)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6808</b></li>
     *   <li>EPSG datum name: <b>Padang 1884 (Jakarta)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Jakarta</b></li>
     *   <li>CRS using the datum: <b>Padang (Jakarta)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPadang1884Jakarta() throws FactoryException {
        important     = true;
        code          = 6808;
        name          = "Padang 1884 (Jakarta)";
        crsName       = "Padang (Jakarta)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Jakarta";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4808, GEOGRAPHIC_2D);
    }

    /**
     * Tests “PDO Survey Datum 1993” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6134</b></li>
     *   <li>EPSG datum name: <b>PDO Survey Datum 1993</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>PSD93</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPDOSurveyDatum1993() throws FactoryException {
        important     = true;
        code          = 6134;
        name          = "PDO Survey Datum 1993";
        crsName       = "PSD93";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4134, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Congo 1960 Pointe Noire” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6282</b></li>
     *   <li>EPSG datum name: <b>Congo 1960 Pointe Noire</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pointe Noire</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCongo1960PointeNoire() throws FactoryException {
        important     = true;
        code          = 6282;
        name          = "Congo 1960 Pointe Noire";
        crsName       = "Pointe Noire";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4282, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Posiciones Geodesicas Argentinas 1994” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6694</b></li>
     *   <li>EPSG datum name: <b>Posiciones Geodesicas Argentinas 1994</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>POSGAR 94</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPosicionesGeodesicasArgentinas1994() throws FactoryException {
        important     = true;
        code          = 6694;
        name          = "Posiciones Geodesicas Argentinas 1994";
        crsName       = "POSGAR 94";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4928);
        createAndVerifyGeographicCRS(4929, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4694, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Posiciones Geodesicas Argentinas 1998” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6190</b></li>
     *   <li>EPSG datum name: <b>Posiciones Geodesicas Argentinas 1998</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>POSGAR 98</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPosicionesGeodesicasArgentinas1998() throws FactoryException {
        important     = true;
        code          = 6190;
        name          = "Posiciones Geodesicas Argentinas 1998";
        crsName       = "POSGAR 98";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4960);
        createAndVerifyGeographicCRS(4961, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4190, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Philippine Reference System 1992” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6683</b></li>
     *   <li>EPSG datum name: <b>Philippine Reference System 1992</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>PRS92</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPhilippineReferenceSystem1992() throws FactoryException {
        important     = true;
        code          = 6683;
        name          = "Philippine Reference System 1992";
        crsName       = "PRS92";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4994);
        createAndVerifyGeographicCRS(4995, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4683, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Provisional South American Datum 1956” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6248</b></li>
     *   <li>EPSG datum name: <b>Provisional South American Datum 1956</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>PSAD56</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testProvisionalSouthAmericanDatum1956() throws FactoryException {
        important     = true;
        code          = 6248;
        name          = "Provisional South American Datum 1956";
        crsName       = "PSAD56";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4248, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pulkovo 1942” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6284</b></li>
     *   <li>EPSG datum name: <b>Pulkovo 1942</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pulkovo 1942</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPulkovo1942() throws FactoryException {
        important     = true;
        code          = 6284;
        name          = "Pulkovo 1942";
        crsName       = "Pulkovo 1942";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4284, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pulkovo 1942(58)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6179</b></li>
     *   <li>EPSG datum name: <b>Pulkovo 1942(58)</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pulkovo 1942(58)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPulkovo194258() throws FactoryException {
        important     = true;
        code          = 6179;
        name          = "Pulkovo 1942(58)";
        crsName       = "Pulkovo 1942(58)";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4179, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pulkovo 1942(83)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6178</b></li>
     *   <li>EPSG datum name: <b>Pulkovo 1942(83)</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pulkovo 1942(83)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPulkovo194283() throws FactoryException {
        important     = true;
        code          = 6178;
        name          = "Pulkovo 1942(83)";
        crsName       = "Pulkovo 1942(83)";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4178, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Qatar 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6286</b></li>
     *   <li>EPSG datum name: <b>Qatar 1948</b></li>
     *   <li>Ellipsoid name: <b>Helmert 1906</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Qatar 1948</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testQatar1948() throws FactoryException {
        important     = true;
        code          = 6286;
        name          = "Qatar 1948";
        crsName       = "Qatar 1948";
        ellipsoid     = "Helmert 1906";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4286, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Qatar 1974” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6285</b></li>
     *   <li>EPSG datum name: <b>Qatar 1974</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Qatar 1974</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testQatar1974() throws FactoryException {
        important     = true;
        code          = 6285;
        name          = "Qatar 1974";
        crsName       = "Qatar 1974";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4285, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Qatar National Datum 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6614</b></li>
     *   <li>EPSG datum name: <b>Qatar National Datum 1995</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>QND95</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testQatarNationalDatum1995() throws FactoryException {
        important     = true;
        code          = 6614;
        name          = "Qatar National Datum 1995";
        crsName       = "QND95";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4614, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Red Geodesica Venezolana” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6189</b></li>
     *   <li>EPSG datum name: <b>Red Geodesica Venezolana</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>REGVEN</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testRedGeodesicaVenezolana() throws FactoryException {
        important     = true;
        code          = 6189;
        name          = "Red Geodesica Venezolana";
        crsName       = "REGVEN";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4962);
        createAndVerifyGeographicCRS(4963, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4189, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau Geodesique Francais 1993” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6171</b></li>
     *   <li>EPSG datum name: <b>Reseau Geodesique Francais 1993</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RGF93</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauGeodesiqueFrancais1993() throws FactoryException {
        important     = true;
        code          = 6171;
        name          = "Reseau Geodesique Francais 1993";
        crsName       = "RGF93";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4964);
        createAndVerifyGeographicCRS(4965, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4171, GEOGRAPHIC_2D);
    }

    /**
     * Tests “South American Datum 1969” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6618</b></li>
     *   <li>EPSG datum name: <b>South American Datum 1969</b></li>
     *   <li>Ellipsoid name: <b>GRS 1967 Modified</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>SAD69</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSouthAmericanDatum1969() throws FactoryException {
        important     = true;
        code          = 6618;
        name          = "South American Datum 1969";
        crsName       = "SAD69";
        ellipsoid     = "GRS 1967 Modified";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4618, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Schwarzeck” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6293</b></li>
     *   <li>EPSG datum name: <b>Schwarzeck</b></li>
     *   <li>Ellipsoid name: <b>Bessel Namibia (GLM)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Schwarzeck</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSchwarzeck() throws FactoryException {
        important     = true;
        code          = 6293;
        name          = "Schwarzeck";
        crsName       = "Schwarzeck";
        ellipsoid     = "Bessel Namibia (GLM)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4293, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Sistema de Referencia Geocentrico para America del Sur 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6170</b></li>
     *   <li>EPSG datum name: <b>Sistema de Referencia Geocentrico para America del Sur 1995</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>SIRGAS 1995</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSistemaDeReferenciaGeocentricoParaAmericaDelSur1995() throws FactoryException {
        important     = true;
        code          = 6170;
        name          = "Sistema de Referencia Geocentrico para America del Sur 1995";
        crsName       = "SIRGAS 1995";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4974);
        createAndVerifyGeographicCRS(4975, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4170, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Sistema de Referencia Geocentrico para las AmericaS 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6674</b></li>
     *   <li>EPSG datum name: <b>Sistema de Referencia Geocentrico para las AmericaS 2000</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>SIRGAS 2000</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSistemaDeReferenciaGeocentricoParaLasAmericaS2000() throws FactoryException {
        important     = true;
        code          = 6674;
        name          = "Sistema de Referencia Geocentrico para las AmericaS 2000";
        crsName       = "SIRGAS 2000";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4988);
        createAndVerifyGeographicCRS(4989, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4674, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tananarive 1925” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6297</b></li>
     *   <li>EPSG datum name: <b>Tananarive 1925</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tananarive</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTananarive1925() throws FactoryException {
        important     = true;
        code          = 6297;
        name          = "Tananarive 1925";
        crsName       = "Tananarive";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4297, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tananarive 1925 (Paris)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6810</b></li>
     *   <li>EPSG datum name: <b>Tananarive 1925 (Paris)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Paris</b></li>
     *   <li>CRS using the datum: <b>Tananarive (Paris)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTananarive1925Paris() throws FactoryException {
        important     = true;
        code          = 6810;
        name          = "Tananarive 1925 (Paris)";
        crsName       = "Tananarive (Paris)";
        ellipsoid     = "International 1924";
        primeMeridian = "Paris";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4810, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Trucial Coast 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6303</b></li>
     *   <li>EPSG datum name: <b>Trucial Coast 1948</b></li>
     *   <li>Ellipsoid name: <b>Helmert 1906</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>TC(1948)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTrucialCoast1948() throws FactoryException {
        important     = true;
        code          = 6303;
        name          = "Trucial Coast 1948";
        crsName       = "TC(1948)";
        ellipsoid     = "Helmert 1906";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4303, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Timbalai 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6298</b></li>
     *   <li>EPSG datum name: <b>Timbalai 1948</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1967 Definition)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Timbalai 1948</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTimbalai1948() throws FactoryException {
        important     = true;
        code          = 6298;
        name          = "Timbalai 1948";
        crsName       = "Timbalai 1948";
        ellipsoid     = "Everest 1830 (1967 Definition)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4298, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Trinidad 1903” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6302</b></li>
     *   <li>EPSG datum name: <b>Trinidad 1903</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1858</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Trinidad 1903</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTrinidad1903() throws FactoryException {
        important     = true;
        code          = 6302;
        name          = "Trinidad 1903";
        crsName       = "Trinidad 1903";
        ellipsoid     = "Clarke 1858";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4302, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Voirol 1875” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6304</b></li>
     *   <li>EPSG datum name: <b>Voirol 1875</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Voirol 1875</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVoirol1875() throws FactoryException {
        important     = true;
        code          = 6304;
        name          = "Voirol 1875";
        crsName       = "Voirol 1875";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4304, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Voirol 1875 (Paris)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6811</b></li>
     *   <li>EPSG datum name: <b>Voirol 1875 (Paris)</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Paris</b></li>
     *   <li>CRS using the datum: <b>Voirol 1875 (Paris)</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVoirol1875Paris() throws FactoryException {
        important     = true;
        code          = 6811;
        name          = "Voirol 1875 (Paris)";
        crsName       = "Voirol 1875 (Paris)";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Paris";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4811, GEOGRAPHIC_2D);
    }

    /**
     * Tests “World Geodetic System 1972” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6322</b></li>
     *   <li>EPSG datum name: <b>World Geodetic System 1972</b></li>
     *   <li>Ellipsoid name: <b>WGS 72</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>WGS 72</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testWorldGeodeticSystem1972() throws FactoryException {
        important     = true;
        code          = 6322;
        name          = "World Geodetic System 1972";
        crsName       = "WGS 72";
        ellipsoid     = "WGS 72";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4984);
        createAndVerifyGeographicCRS(4985, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4322, GEOGRAPHIC_2D);
    }

    /**
     * Tests “WGS 72 Transit Broadcast Ephemeris” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6324</b></li>
     *   <li>EPSG datum name: <b>WGS 72 Transit Broadcast Ephemeris</b></li>
     *   <li>Ellipsoid name: <b>WGS 72</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>WGS 72BE</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testWGS72TransitBroadcastEphemeris() throws FactoryException {
        important     = true;
        code          = 6324;
        name          = "WGS 72 Transit Broadcast Ephemeris";
        crsName       = "WGS 72BE";
        ellipsoid     = "WGS 72";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4986);
        createAndVerifyGeographicCRS(4987, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4324, GEOGRAPHIC_2D);
    }

    /**
     * Tests “World Geodetic System 1984” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6326</b></li>
     *   <li>EPSG datum name: <b>World Geodetic System 1984</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>WGS 84</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testWorldGeodeticSystem1984() throws FactoryException {
        important     = true;
        code          = 6326;
        name          = "World Geodetic System 1984";
        crsName       = "WGS 84";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4978);
        createAndVerifyGeographicCRS(4979, GEOGRAPHIC_3D);
        createAndVerifyGeographicCRS(4326, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Xian 1980” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6610</b></li>
     *   <li>EPSG datum name: <b>Xian 1980</b></li>
     *   <li>Ellipsoid name: <b>IAG 1975</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Xian 1980</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testXian1980() throws FactoryException {
        important     = true;
        code          = 6610;
        name          = "Xian 1980";
        crsName       = "Xian 1980";
        ellipsoid     = "IAG 1975";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4610, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Yemen National Geodetic Network 1996” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6163</b></li>
     *   <li>EPSG datum name: <b>Yemen National Geodetic Network 1996</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Yemen NGN96</b></li>
     *   <li>Particularly important to E&amp;P industry.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testYemenNationalGeodeticNetwork1996() throws FactoryException {
        important     = true;
        code          = 6163;
        name          = "Yemen National Geodetic Network 1996";
        crsName       = "Yemen NGN96";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeocentricCRS(4980);
        createAndVerifyGeographicCRS(4163, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Adindan” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6201</b></li>
     *   <li>EPSG datum name: <b>Adindan</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Adindan</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAdindan() throws FactoryException {
        code          = 6201;
        name          = "Adindan";
        crsName       = "Adindan";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4201, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Afgooye” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6205</b></li>
     *   <li>EPSG datum name: <b>Afgooye</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Afgooye</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAfgooye() throws FactoryException {
        code          = 6205;
        name          = "Afgooye";
        crsName       = "Afgooye";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4205, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Agadez” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6206</b></li>
     *   <li>EPSG datum name: <b>Agadez</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Agadez</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAgadez() throws FactoryException {
        code          = 6206;
        name          = "Agadez";
        crsName       = "Agadez";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4206, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Albanian 1987” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6191</b></li>
     *   <li>EPSG datum name: <b>Albanian 1987</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Albanian 1987</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAlbanian1987() throws FactoryException {
        code          = 6191;
        name          = "Albanian 1987";
        crsName       = "Albanian 1987";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4191, GEOGRAPHIC_2D);
    }

    /**
     * Tests “American Samoa 1962” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6169</b></li>
     *   <li>EPSG datum name: <b>American Samoa 1962</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>American Samoa 1962</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAmericanSamoa1962() throws FactoryException {
        code          = 6169;
        name          = "American Samoa 1962";
        crsName       = "American Samoa 1962";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4169, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Ammassalik 1958” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6196</b></li>
     *   <li>EPSG datum name: <b>Ammassalik 1958</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Ammassalik 1958</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAmmassalik1958() throws FactoryException {
        code          = 6196;
        name          = "Ammassalik 1958";
        crsName       = "Ammassalik 1958";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4196, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Anguilla 1957” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6600</b></li>
     *   <li>EPSG datum name: <b>Anguilla 1957</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Anguilla 1957</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAnguilla1957() throws FactoryException {
        code          = 6600;
        name          = "Anguilla 1957";
        crsName       = "Anguilla 1957";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4600, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Antigua 1943” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6601</b></li>
     *   <li>EPSG datum name: <b>Antigua 1943</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Antigua 1943</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAntigua1943() throws FactoryException {
        code          = 6601;
        name          = "Antigua 1943";
        crsName       = "Antigua 1943";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4601, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Arc 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6209</b></li>
     *   <li>EPSG datum name: <b>Arc 1950</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (Arc)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Arc 1950</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testArc1950() throws FactoryException {
        code          = 6209;
        name          = "Arc 1950";
        crsName       = "Arc 1950";
        ellipsoid     = "Clarke 1880 (Arc)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4209, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Arc 1960” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6210</b></li>
     *   <li>EPSG datum name: <b>Arc 1960</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Arc 1960</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testArc1960() throws FactoryException {
        code          = 6210;
        name          = "Arc 1960";
        crsName       = "Arc 1960";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4210, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Ascension Island 1958” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6712</b></li>
     *   <li>EPSG datum name: <b>Ascension Island 1958</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Ascension Island 1958</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAscensionIsland1958() throws FactoryException {
        code          = 6712;
        name          = "Ascension Island 1958";
        crsName       = "Ascension Island 1958";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4712, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Ancienne Triangulation Francaise (Paris)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6901</b></li>
     *   <li>EPSG datum name: <b>Ancienne Triangulation Francaise (Paris)</b></li>
     *   <li>Ellipsoid name: <b>Plessis 1817</b></li>
     *   <li>Prime meridian name: <b>Paris RGS</b></li>
     *   <li>CRS using the datum: <b>ATF (Paris)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAncienneTriangulationFrancaiseParis() throws FactoryException {
        code          = 6901;
        name          = "Ancienne Triangulation Francaise (Paris)";
        crsName       = "ATF (Paris)";
        ellipsoid     = "Plessis 1817";
        primeMeridian = "Paris RGS";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4901, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Average Terrestrial System 1977” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6122</b></li>
     *   <li>EPSG datum name: <b>Average Terrestrial System 1977</b></li>
     *   <li>Ellipsoid name: <b>Average Terrestrial System 1977</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ATS77</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAverageTerrestrialSystem1977() throws FactoryException {
        code          = 6122;
        name          = "Average Terrestrial System 1977";
        crsName       = "ATS77";
        ellipsoid     = "Average Terrestrial System 1977";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4122, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Australian Antarctic Datum 1998” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6176</b></li>
     *   <li>EPSG datum name: <b>Australian Antarctic Datum 1998</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Australian Antarctic</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAustralianAntarcticDatum1998() throws FactoryException {
        code          = 6176;
        name          = "Australian Antarctic Datum 1998";
        crsName       = "Australian Antarctic";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4176, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Ayabelle Lighthouse” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6713</b></li>
     *   <li>EPSG datum name: <b>Ayabelle Lighthouse</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Ayabelle Lighthouse</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAyabelleLighthouse() throws FactoryException {
        code          = 6713;
        name          = "Ayabelle Lighthouse";
        crsName       = "Ayabelle Lighthouse";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4713, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Azores Central Islands 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6183</b></li>
     *   <li>EPSG datum name: <b>Azores Central Islands 1948</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Azores Central 1948</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAzoresCentralIslands1948() throws FactoryException {
        code          = 6183;
        name          = "Azores Central Islands 1948";
        crsName       = "Azores Central 1948";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4183, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Azores Central Islands 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6665</b></li>
     *   <li>EPSG datum name: <b>Azores Central Islands 1995</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Azores Central 1995</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAzoresCentralIslands1995() throws FactoryException {
        code          = 6665;
        name          = "Azores Central Islands 1995";
        crsName       = "Azores Central 1995";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4665, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Azores Occidental Islands 1939” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6182</b></li>
     *   <li>EPSG datum name: <b>Azores Occidental Islands 1939</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Azores Occidental 1939</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAzoresOccidentalIslands1939() throws FactoryException {
        code          = 6182;
        name          = "Azores Occidental Islands 1939";
        crsName       = "Azores Occidental 1939";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4182, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Azores Oriental Islands 1940” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6184</b></li>
     *   <li>EPSG datum name: <b>Azores Oriental Islands 1940</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Azores Oriental 1940</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAzoresOrientalIslands1940() throws FactoryException {
        code          = 6184;
        name          = "Azores Oriental Islands 1940";
        crsName       = "Azores Oriental 1940";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4184, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Azores Oriental Islands 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6664</b></li>
     *   <li>EPSG datum name: <b>Azores Oriental Islands 1995</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Azores Oriental 1995</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testAzoresOrientalIslands1995() throws FactoryException {
        code          = 6664;
        name          = "Azores Oriental Islands 1995";
        crsName       = "Azores Oriental 1995";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4664, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Barbados 1938” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6212</b></li>
     *   <li>EPSG datum name: <b>Barbados 1938</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Barbados 1938</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBarbados1938() throws FactoryException {
        code          = 6212;
        name          = "Barbados 1938";
        crsName       = "Barbados 1938";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4212, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bermuda 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6762</b></li>
     *   <li>EPSG datum name: <b>Bermuda 2000</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>BDA2000</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBermuda2000() throws FactoryException {
        code          = 6762;
        name          = "Bermuda 2000";
        crsName       = "BDA2000";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4762, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Beduaram” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6213</b></li>
     *   <li>EPSG datum name: <b>Beduaram</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Beduaram</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBeduaram() throws FactoryException {
        code          = 6213;
        name          = "Beduaram";
        crsName       = "Beduaram";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4213, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau National Belge 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6215</b></li>
     *   <li>EPSG datum name: <b>Reseau National Belge 1950</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Belge 1950</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauNationalBelge1950() throws FactoryException {
        code          = 6215;
        name          = "Reseau National Belge 1950";
        crsName       = "Belge 1950";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4215, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau National Belge 1950 (Brussels)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6809</b></li>
     *   <li>EPSG datum name: <b>Reseau National Belge 1950 (Brussels)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Brussels</b></li>
     *   <li>CRS using the datum: <b>Belge 1950 (Brussels)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauNationalBelge1950Brussels() throws FactoryException {
        code          = 6809;
        name          = "Reseau National Belge 1950 (Brussels)";
        crsName       = "Belge 1950 (Brussels)";
        ellipsoid     = "International 1924";
        primeMeridian = "Brussels";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4809, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau National Belge 1972” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6313</b></li>
     *   <li>EPSG datum name: <b>Reseau National Belge 1972</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Belge 1972</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauNationalBelge1972() throws FactoryException {
        code          = 6313;
        name          = "Reseau National Belge 1972";
        crsName       = "Belge 1972";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4313, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bellevue” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6714</b></li>
     *   <li>EPSG datum name: <b>Bellevue</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Bellevue</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBellevue() throws FactoryException {
        code          = 6714;
        name          = "Bellevue";
        crsName       = "Bellevue";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4714, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bermuda 1957” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6216</b></li>
     *   <li>EPSG datum name: <b>Bermuda 1957</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Bermuda 1957</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBermuda1957() throws FactoryException {
        code          = 6216;
        name          = "Bermuda 1957";
        crsName       = "Bermuda 1957";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4216, GEOGRAPHIC_2D);
    }

    /**
     * Tests “CH1903 (Bern)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6801</b></li>
     *   <li>EPSG datum name: <b>CH1903 (Bern)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Bern</b></li>
     *   <li>CRS using the datum: <b>Bern 1898 (Bern)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCH1903Bern() throws FactoryException {
        code          = 6801;
        name          = "CH1903 (Bern)";
        crsName       = "Bern 1898 (Bern)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Bern";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4801, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bern 1938” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6306</b></li>
     *   <li>EPSG datum name: <b>Bern 1938</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Bern 1938</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBern1938() throws FactoryException {
        code          = 6306;
        name          = "Bern 1938";
        crsName       = "Bern 1938";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4306, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bissau” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6165</b></li>
     *   <li>EPSG datum name: <b>Bissau</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Bissau</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBissau() throws FactoryException {
        code          = 6165;
        name          = "Bissau";
        crsName       = "Bissau";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4165, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bogota 1975 (Bogota)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6802</b></li>
     *   <li>EPSG datum name: <b>Bogota 1975 (Bogota)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Bogota</b></li>
     *   <li>CRS using the datum: <b>Bogota 1975 (Bogota)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBogota1975Bogota() throws FactoryException {
        code          = 6802;
        name          = "Bogota 1975 (Bogota)";
        crsName       = "Bogota 1975 (Bogota)";
        ellipsoid     = "International 1924";
        primeMeridian = "Bogota";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4802, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Bukit Rimpah” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6219</b></li>
     *   <li>EPSG datum name: <b>Bukit Rimpah</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Bukit Rimpah</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testBukitRimpah() throws FactoryException {
        code          = 6219;
        name          = "Bukit Rimpah";
        crsName       = "Bukit Rimpah";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4219, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Camp Area Astro” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6715</b></li>
     *   <li>EPSG datum name: <b>Camp Area Astro</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Camp Area Astro</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCampAreaAstro() throws FactoryException {
        code          = 6715;
        name          = "Camp Area Astro";
        crsName       = "Camp Area Astro";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4715, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Cape” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6222</b></li>
     *   <li>EPSG datum name: <b>Cape</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (Arc)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Cape</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCape() throws FactoryException {
        code          = 6222;
        name          = "Cape";
        crsName       = "Cape";
        ellipsoid     = "Clarke 1880 (Arc)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4222, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Cape Canaveral” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6717</b></li>
     *   <li>EPSG datum name: <b>Cape Canaveral</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Cape Canaveral</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCapeCanaveral() throws FactoryException {
        code          = 6717;
        name          = "Cape Canaveral";
        crsName       = "Cape Canaveral";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4717, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Carthage (Paris)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6816</b></li>
     *   <li>EPSG datum name: <b>Carthage (Paris)</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Paris</b></li>
     *   <li>CRS using the datum: <b>Carthage (Paris)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCarthageParis() throws FactoryException {
        code          = 6816;
        name          = "Carthage (Paris)";
        crsName       = "Carthage (Paris)";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Paris";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4816, GEOGRAPHIC_2D);
    }

    /**
     * Tests “CH1903” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6149</b></li>
     *   <li>EPSG datum name: <b>CH1903</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>CH1903</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCH1903() throws FactoryException {
        code          = 6149;
        name          = "CH1903";
        crsName       = "CH1903";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4149, GEOGRAPHIC_2D);
    }

    /**
     * Tests “CH1903+” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6150</b></li>
     *   <li>EPSG datum name: <b>CH1903+</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>CH1903+</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCH1903_plus() throws FactoryException {
        code          = 6150;
        name          = "CH1903+";
        crsName       = "CH1903+";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4150, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Chatham Islands Datum 1971” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6672</b></li>
     *   <li>EPSG datum name: <b>Chatham Islands Datum 1971</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Chatham Islands 1971</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testChathamIslandsDatum1971() throws FactoryException {
        code          = 6672;
        name          = "Chatham Islands Datum 1971";
        crsName       = "Chatham Islands 1971";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4672, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Chatham Islands Datum 1979” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6673</b></li>
     *   <li>EPSG datum name: <b>Chatham Islands Datum 1979</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Chatham Islands 1979</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testChathamIslandsDatum1979() throws FactoryException {
        code          = 6673;
        name          = "Chatham Islands Datum 1979";
        crsName       = "Chatham Islands 1979";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4673, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Swiss Terrestrial Reference Frame 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6151</b></li>
     *   <li>EPSG datum name: <b>Swiss Terrestrial Reference Frame 1995</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>CHTRF95</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSwissTerrestrialReferenceFrame1995() throws FactoryException {
        code          = 6151;
        name          = "Swiss Terrestrial Reference Frame 1995";
        crsName       = "CHTRF95";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4151, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Chua” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6224</b></li>
     *   <li>EPSG datum name: <b>Chua</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Chua</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testChua() throws FactoryException {
        code          = 6224;
        name          = "Chua";
        crsName       = "Chua";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4224, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Cocos Islands 1965” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6708</b></li>
     *   <li>EPSG datum name: <b>Cocos Islands 1965</b></li>
     *   <li>Ellipsoid name: <b>Australian National Spheroid</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Cocos Islands 1965</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCocosIslands1965() throws FactoryException {
        code          = 6708;
        name          = "Cocos Islands 1965";
        crsName       = "Cocos Islands 1965";
        ellipsoid     = "Australian National Spheroid";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4708, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Combani 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6632</b></li>
     *   <li>EPSG datum name: <b>Combani 1950</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Combani 1950</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCombani1950() throws FactoryException {
        code          = 6632;
        name          = "Combani 1950";
        crsName       = "Combani 1950";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4632, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Conakry 1905” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6315</b></li>
     *   <li>EPSG datum name: <b>Conakry 1905</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Conakry 1905</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testConakry1905() throws FactoryException {
        code          = 6315;
        name          = "Conakry 1905";
        crsName       = "Conakry 1905";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4315, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Corrego Alegre 1970-72” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6225</b></li>
     *   <li>EPSG datum name: <b>Corrego Alegre 1970-72</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Corrego Alegre 1970-72</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCorregoAlegre197072() throws FactoryException {
        code          = 6225;
        name          = "Corrego Alegre 1970-72";
        crsName       = "Corrego Alegre 1970-72";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4225, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Centre Spatial Guyanais 1967” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6623</b></li>
     *   <li>EPSG datum name: <b>Centre Spatial Guyanais 1967</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>CSG67</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCentreSpatialGuyanais1967() throws FactoryException {
        code          = 6623;
        name          = "Centre Spatial Guyanais 1967";
        crsName       = "CSG67";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4623, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Dabola 1981” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6155</b></li>
     *   <li>EPSG datum name: <b>Dabola 1981</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Dabola 1981</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDabola1981() throws FactoryException {
        code          = 6155;
        name          = "Dabola 1981";
        crsName       = "Dabola 1981";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4155, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Datum 73” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6274</b></li>
     *   <li>EPSG datum name: <b>Datum 73</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Datum 73</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDatum73() throws FactoryException {
        code          = 6274;
        name          = "Datum 73";
        crsName       = "Datum 73";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4274, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Deception Island” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6736</b></li>
     *   <li>EPSG datum name: <b>Deception Island</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Deception Island</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDeceptionIsland() throws FactoryException {
        code          = 6736;
        name          = "Deception Island";
        crsName       = "Deception Island";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4736, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Diego Garcia 1969” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6724</b></li>
     *   <li>EPSG datum name: <b>Diego Garcia 1969</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Diego Garcia 1969</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDiegoGarcia1969() throws FactoryException {
        code          = 6724;
        name          = "Diego Garcia 1969";
        crsName       = "Diego Garcia 1969";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4724, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Dominica 1945” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6602</b></li>
     *   <li>EPSG datum name: <b>Dominica 1945</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Dominica 1945</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testDominica1945() throws FactoryException {
        code          = 6602;
        name          = "Dominica 1945";
        crsName       = "Dominica 1945";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4602, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Easter Island 1967” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6719</b></li>
     *   <li>EPSG datum name: <b>Easter Island 1967</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Easter Island 1967</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEasterIsland1967() throws FactoryException {
        code          = 6719;
        name          = "Easter Island 1967";
        crsName       = "Easter Island 1967";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4719, GEOGRAPHIC_2D);
    }

    /**
     * Tests “European Datum 1979” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6668</b></li>
     *   <li>EPSG datum name: <b>European Datum 1979</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ED79</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEuropeanDatum1979() throws FactoryException {
        code          = 6668;
        name          = "European Datum 1979";
        crsName       = "ED79";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4668, GEOGRAPHIC_2D);
    }

    /**
     * Tests “European Datum 1987” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6231</b></li>
     *   <li>EPSG datum name: <b>European Datum 1987</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ED87</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEuropeanDatum1987() throws FactoryException {
        code          = 6231;
        name          = "European Datum 1987";
        crsName       = "ED87";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4231, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Egypt 1930” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6199</b></li>
     *   <li>EPSG datum name: <b>Egypt 1930</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Egypt 1930</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEgypt1930() throws FactoryException {
        code          = 6199;
        name          = "Egypt 1930";
        crsName       = "Egypt 1930";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4199, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Estonia 1992” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6133</b></li>
     *   <li>EPSG datum name: <b>Estonia 1992</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>EST92</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEstonia1992() throws FactoryException {
        code          = 6133;
        name          = "Estonia 1992";
        crsName       = "EST92";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4133, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Estonia 1997” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6180</b></li>
     *   <li>EPSG datum name: <b>Estonia 1997</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>EST97</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testEstonia1997() throws FactoryException {
        code          = 6180;
        name          = "Estonia 1997";
        crsName       = "EST97";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4180, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Fatu Iva 72” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6688</b></li>
     *   <li>EPSG datum name: <b>Fatu Iva 72</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Fatu Iva 72</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFatuIva72() throws FactoryException {
        code          = 6688;
        name          = "Fatu Iva 72";
        crsName       = "Fatu Iva 72";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4688, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Faroe Datum 1954” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6741</b></li>
     *   <li>EPSG datum name: <b>Faroe Datum 1954</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>FD54</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFaroeDatum1954() throws FactoryException {
        code          = 6741;
        name          = "Faroe Datum 1954";
        crsName       = "FD54";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4741, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Fiji 1956” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6721</b></li>
     *   <li>EPSG datum name: <b>Fiji 1956</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Fiji 1956</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFiji1956() throws FactoryException {
        code          = 6721;
        name          = "Fiji 1956";
        crsName       = "Fiji 1956";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4721, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Fiji Geodetic Datum 1986” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6720</b></li>
     *   <li>EPSG datum name: <b>Fiji Geodetic Datum 1986</b></li>
     *   <li>Ellipsoid name: <b>WGS 72</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Fiji 1986</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFijiGeodeticDatum1986() throws FactoryException {
        code          = 6720;
        name          = "Fiji Geodetic Datum 1986";
        crsName       = "Fiji 1986";
        ellipsoid     = "WGS 72";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4720, GEOGRAPHIC_2D);
    }

    /**
     * Tests “fk89” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6753</b></li>
     *   <li>EPSG datum name: <b>fk89</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>fk89</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFk89() throws FactoryException {
        code          = 6753;
        name          = "fk89";
        crsName       = "fk89";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4753, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Fort Marigot” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6621</b></li>
     *   <li>EPSG datum name: <b>Fort Marigot</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Fort Marigot</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testFortMarigot() throws FactoryException {
        code          = 6621;
        name          = "Fort Marigot";
        crsName       = "Fort Marigot";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4621, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Gan 1970” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6684</b></li>
     *   <li>EPSG datum name: <b>Gan 1970</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Gan 1970</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGan1970() throws FactoryException {
        code          = 6684;
        name          = "Gan 1970";
        crsName       = "Gan 1970";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4684, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Garoua” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6197</b></li>
     *   <li>EPSG datum name: <b>Garoua</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Garoua</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGaroua() throws FactoryException {
        code          = 6197;
        name          = "Garoua";
        crsName       = "Garoua";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4197, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Greek Geodetic Reference System 1987” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6121</b></li>
     *   <li>EPSG datum name: <b>Greek Geodetic Reference System 1987</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>GGRS87</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGreekGeodeticReferenceSystem1987() throws FactoryException {
        code          = 6121;
        name          = "Greek Geodetic Reference System 1987";
        crsName       = "GGRS87";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4121, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Greenland 1996” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6747</b></li>
     *   <li>EPSG datum name: <b>Greenland 1996</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>GR96</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGreenland1996() throws FactoryException {
        code          = 6747;
        name          = "Greenland 1996";
        crsName       = "GR96";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4747, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Grand Cayman 1959” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6723</b></li>
     *   <li>EPSG datum name: <b>Grand Cayman 1959</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Grand Cayman 1959</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGrandCayman1959() throws FactoryException {
        code          = 6723;
        name          = "Grand Cayman 1959";
        crsName       = "Grand Cayman 1959";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4723, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Grand Comoros” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6646</b></li>
     *   <li>EPSG datum name: <b>Grand Comoros</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Grand Comoros</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGrandComoros() throws FactoryException {
        code          = 6646;
        name          = "Grand Comoros";
        crsName       = "Grand Comoros";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4646, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Greek” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6120</b></li>
     *   <li>EPSG datum name: <b>Greek</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Greek</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGreek() throws FactoryException {
        code          = 6120;
        name          = "Greek";
        crsName       = "Greek";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4120, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Greek (Athens)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6815</b></li>
     *   <li>EPSG datum name: <b>Greek (Athens)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Athens</b></li>
     *   <li>CRS using the datum: <b>Greek (Athens)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGreekAthens() throws FactoryException {
        code          = 6815;
        name          = "Greek (Athens)";
        crsName       = "Greek (Athens)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Athens";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4815, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Grenada 1953” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6603</b></li>
     *   <li>EPSG datum name: <b>Grenada 1953</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Grenada 1953</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGrenada1953() throws FactoryException {
        code          = 6603;
        name          = "Grenada 1953";
        crsName       = "Grenada 1953";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4603, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Guadeloupe 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6622</b></li>
     *   <li>EPSG datum name: <b>Guadeloupe 1948</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Guadeloupe 1948</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGuadeloupe1948() throws FactoryException {
        code          = 6622;
        name          = "Guadeloupe 1948";
        crsName       = "Guadeloupe 1948";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4622, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Guam 1963” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6675</b></li>
     *   <li>EPSG datum name: <b>Guam 1963</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Guam 1963</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGuam1963() throws FactoryException {
        code          = 6675;
        name          = "Guam 1963";
        crsName       = "Guam 1963";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4675, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Gulshan 303” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6682</b></li>
     *   <li>EPSG datum name: <b>Gulshan 303</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1937 Adjustment)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Gulshan 303</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGulshan303() throws FactoryException {
        code          = 6682;
        name          = "Gulshan 303";
        crsName       = "Gulshan 303";
        ellipsoid     = "Everest 1830 (1937 Adjustment)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4682, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hanoi 1972” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6147</b></li>
     *   <li>EPSG datum name: <b>Hanoi 1972</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hanoi 1972</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHanoi1972() throws FactoryException {
        code          = 6147;
        name          = "Hanoi 1972";
        crsName       = "Hanoi 1972";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4147, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hartebeesthoek94” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6148</b></li>
     *   <li>EPSG datum name: <b>Hartebeesthoek94</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hartebeesthoek94</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHartebeesthoek94() throws FactoryException {
        code          = 6148;
        name          = "Hartebeesthoek94";
        crsName       = "Hartebeesthoek94";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4148, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hungarian Datum 1909” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>1024</b></li>
     *   <li>EPSG datum name: <b>Hungarian Datum 1909</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>HD1909</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHungarianDatum1909() throws FactoryException {
        code          = 1024;
        name          = "Hungarian Datum 1909";
        crsName       = "HD1909";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(3819, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Helle 1954” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6660</b></li>
     *   <li>EPSG datum name: <b>Helle 1954</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Helle 1954</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHelle1954() throws FactoryException {
        code          = 6660;
        name          = "Helle 1954";
        crsName       = "Helle 1954";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4660, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Herat North” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6255</b></li>
     *   <li>EPSG datum name: <b>Herat North</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Herat North</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHeratNorth() throws FactoryException {
        code          = 6255;
        name          = "Herat North";
        crsName       = "Herat North";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4255, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hjorsey 1955” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6658</b></li>
     *   <li>EPSG datum name: <b>Hjorsey 1955</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hjorsey 1955</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHjorsey1955() throws FactoryException {
        code          = 6658;
        name          = "Hjorsey 1955";
        crsName       = "Hjorsey 1955";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4658, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hong Kong 1963” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6738</b></li>
     *   <li>EPSG datum name: <b>Hong Kong 1963</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1858</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hong Kong 1963</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHongKong1963() throws FactoryException {
        code          = 6738;
        name          = "Hong Kong 1963";
        crsName       = "Hong Kong 1963";
        ellipsoid     = "Clarke 1858";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4738, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hong Kong 1963(67)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6739</b></li>
     *   <li>EPSG datum name: <b>Hong Kong 1963(67)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hong Kong 1963(67)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHongKong196367() throws FactoryException {
        code          = 6739;
        name          = "Hong Kong 1963(67)";
        crsName       = "Hong Kong 1963(67)";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4739, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hong Kong 1980” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6611</b></li>
     *   <li>EPSG datum name: <b>Hong Kong 1980</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hong Kong 1980</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHongKong1980() throws FactoryException {
        code          = 6611;
        name          = "Hong Kong 1980";
        crsName       = "Hong Kong 1980";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4611, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Croatian Terrestrial Reference System” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6761</b></li>
     *   <li>EPSG datum name: <b>Croatian Terrestrial Reference System</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>HTRS96</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testCroatianTerrestrialReferenceSystem() throws FactoryException {
        code          = 6761;
        name          = "Croatian Terrestrial Reference System";
        crsName       = "HTRS96";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4761, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Hu Tzu Shan 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6236</b></li>
     *   <li>EPSG datum name: <b>Hu Tzu Shan 1950</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Hu Tzu Shan 1950</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testHuTzuShan1950() throws FactoryException {
        code          = 6236;
        name          = "Hu Tzu Shan 1950";
        crsName       = "Hu Tzu Shan 1950";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4236, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGC 1962 Arc of the 6th Parallel South” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6697</b></li>
     *   <li>EPSG datum name: <b>IGC 1962 Arc of the 6th Parallel South</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGC 1962 6th Parallel South</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGC1962ArcOfThe6thParallelSouth() throws FactoryException {
        code          = 6697;
        name          = "IGC 1962 Arc of the 6th Parallel South";
        crsName       = "IGC 1962 6th Parallel South";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4697, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Institut Geographique du Congo Belge 1955” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6701</b></li>
     *   <li>EPSG datum name: <b>Institut Geographique du Congo Belge 1955</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGCB 1955</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testInstitutGeographiqueDuCongoBelge1955() throws FactoryException {
        code          = 6701;
        name          = "Institut Geographique du Congo Belge 1955";
        crsName       = "IGCB 1955";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4701, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Istituto Geografico Militaire 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6670</b></li>
     *   <li>EPSG datum name: <b>Istituto Geografico Militaire 1995</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGM95</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIstitutoGeograficoMilitaire1995() throws FactoryException {
        code          = 6670;
        name          = "Istituto Geografico Militaire 1995";
        crsName       = "IGM95";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4670, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN 1962 Kerguelen” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6698</b></li>
     *   <li>EPSG datum name: <b>IGN 1962 Kerguelen</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN 1962 Kerguelen</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGN1962Kerguelen() throws FactoryException {
        code          = 6698;
        name          = "IGN 1962 Kerguelen";
        crsName       = "IGN 1962 Kerguelen";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4698, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN53 Mare” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6641</b></li>
     *   <li>EPSG datum name: <b>IGN53 Mare</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN53 Mare</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGN53Mare() throws FactoryException {
        code          = 6641;
        name          = "IGN53 Mare";
        crsName       = "IGN53 Mare";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4641, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN56 Lifou” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6633</b></li>
     *   <li>EPSG datum name: <b>IGN56 Lifou</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN56 Lifou</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGN56Lifou() throws FactoryException {
        code          = 6633;
        name          = "IGN56 Lifou";
        crsName       = "IGN56 Lifou";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4633, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN63 Hiva Oa” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6689</b></li>
     *   <li>EPSG datum name: <b>IGN63 Hiva Oa</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN63 Hiva Oa</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGN63HivaOa() throws FactoryException {
        code          = 6689;
        name          = "IGN63 Hiva Oa";
        crsName       = "IGN63 Hiva Oa";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4689, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN72 Grande Terre” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6634</b></li>
     *   <li>EPSG datum name: <b>IGN72 Grande Terre</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN72 Grande Terre</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGN72GrandeTerre() throws FactoryException {
        code          = 6634;
        name          = "IGN72 Grande Terre";
        crsName       = "IGN72 Grande Terre";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4662, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IGN72 Nuku Hiva” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6630</b></li>
     *   <li>EPSG datum name: <b>IGN72 Nuku Hiva</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IGN72 Nuku Hiva</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIGN72NukuHiva() throws FactoryException {
        code          = 6630;
        name          = "IGN72 Nuku Hiva";
        crsName       = "IGN72 Nuku Hiva";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4630, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Iraq-Kuwait Boundary Datum 1992” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6667</b></li>
     *   <li>EPSG datum name: <b>Iraq-Kuwait Boundary Datum 1992</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IKBD-92</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIraqKuwaitBoundaryDatum1992() throws FactoryException {
        code          = 6667;
        name          = "Iraq-Kuwait Boundary Datum 1992";
        crsName       = "IKBD-92";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4667, GEOGRAPHIC_2D);
    }

    /**
     * Tests “IRENET95” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6173</b></li>
     *   <li>EPSG datum name: <b>IRENET95</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>IRENET95</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIRENET95() throws FactoryException {
        code          = 6173;
        name          = "IRENET95";
        crsName       = "IRENET95";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4173, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Islands Net 1993” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6659</b></li>
     *   <li>EPSG datum name: <b>Islands Net 1993</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ISN93</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIslandsNet1993() throws FactoryException {
        code          = 6659;
        name          = "Islands Net 1993";
        crsName       = "ISN93";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4659, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Israel” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6141</b></li>
     *   <li>EPSG datum name: <b>Israel</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Israel</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIsrael() throws FactoryException {
        code          = 6141;
        name          = "Israel";
        crsName       = "Israel";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4141, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Iwo Jima 1945” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6709</b></li>
     *   <li>EPSG datum name: <b>Iwo Jima 1945</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Iwo Jima 1945</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testIwoJima1945() throws FactoryException {
        code          = 6709;
        name          = "Iwo Jima 1945";
        crsName       = "Iwo Jima 1945";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4709, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Jamaica 2001” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6758</b></li>
     *   <li>EPSG datum name: <b>Jamaica 2001</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>JAD2001</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testJamaica2001() throws FactoryException {
        code          = 6758;
        name          = "Jamaica 2001";
        crsName       = "JAD2001";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4758, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Jamaica 1969” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6242</b></li>
     *   <li>EPSG datum name: <b>Jamaica 1969</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>JAD69</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testJamaica1969() throws FactoryException {
        code          = 6242;
        name          = "Jamaica 1969";
        crsName       = "JAD69";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4242, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Jamaica 1875” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6241</b></li>
     *   <li>EPSG datum name: <b>Jamaica 1875</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Jamaica 1875</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testJamaica1875() throws FactoryException {
        code          = 6241;
        name          = "Jamaica 1875";
        crsName       = "Jamaica 1875";
        ellipsoid     = "Clarke 1880";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4241, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Japanese Geodetic Datum 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6612</b></li>
     *   <li>EPSG datum name: <b>Japanese Geodetic Datum 2000</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>JGD2000</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testJapaneseGeodeticDatum2000() throws FactoryException {
        code          = 6612;
        name          = "Japanese Geodetic Datum 2000";
        crsName       = "JGD2000";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4612, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Johnston Island 1961” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6725</b></li>
     *   <li>EPSG datum name: <b>Johnston Island 1961</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Johnston Island 1961</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testJohnstonIsland1961() throws FactoryException {
        code          = 6725;
        name          = "Johnston Island 1961";
        crsName       = "Johnston Island 1961";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4725, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Jouik 1961” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6679</b></li>
     *   <li>EPSG datum name: <b>Jouik 1961</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Jouik 1961</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testJouik1961() throws FactoryException {
        code          = 6679;
        name          = "Jouik 1961";
        crsName       = "Jouik 1961";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4679, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kalianpur 1880” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6243</b></li>
     *   <li>EPSG datum name: <b>Kalianpur 1880</b></li>
     *   <li>Ellipsoid name: <b>Everest (1830 Definition)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kalianpur 1880</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKalianpur1880() throws FactoryException {
        code          = 6243;
        name          = "Kalianpur 1880";
        crsName       = "Kalianpur 1880";
        ellipsoid     = "Everest (1830 Definition)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4243, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kandawala” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6244</b></li>
     *   <li>EPSG datum name: <b>Kandawala</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (1937 Adjustment)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kandawala</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKandawala() throws FactoryException {
        code          = 6244;
        name          = "Kandawala";
        crsName       = "Kandawala";
        ellipsoid     = "Everest 1830 (1937 Adjustment)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4244, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Karbala 1979” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6743</b></li>
     *   <li>EPSG datum name: <b>Karbala 1979</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Karbala 1979</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKarbala1979() throws FactoryException {
        code          = 6743;
        name          = "Karbala 1979";
        crsName       = "Karbala 1979";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4743, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kasai 1953” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6696</b></li>
     *   <li>EPSG datum name: <b>Kasai 1953</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kasai 1953</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKasai1953() throws FactoryException {
        code          = 6696;
        name          = "Kasai 1953";
        crsName       = "Kasai 1953";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4696, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Katanga 1955” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6695</b></li>
     *   <li>EPSG datum name: <b>Katanga 1955</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Katanga 1955</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKatanga1955() throws FactoryException {
        code          = 6695;
        name          = "Katanga 1955";
        crsName       = "Katanga 1955";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4695, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kertau (RSO)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6751</b></li>
     *   <li>EPSG datum name: <b>Kertau (RSO)</b></li>
     *   <li>Ellipsoid name: <b>Everest 1830 (RSO 1969)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kertau (RSO)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKertauRSO() throws FactoryException {
        code          = 6751;
        name          = "Kertau (RSO)";
        crsName       = "Kertau (RSO)";
        ellipsoid     = "Everest 1830 (RSO 1969)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4751, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kartastokoordinaattijarjestelma (1966)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6123</b></li>
     *   <li>EPSG datum name: <b>Kartastokoordinaattijarjestelma (1966)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>KKJ</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKartastokoordinaattijarjestelma1966() throws FactoryException {
        code          = 6123;
        name          = "Kartastokoordinaattijarjestelma (1966)";
        crsName       = "KKJ";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4123, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Geocentric datum of Korea” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6737</b></li>
     *   <li>EPSG datum name: <b>Geocentric datum of Korea</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Korea 2000</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGeocentricDatumOfKorea() throws FactoryException {
        code          = 6737;
        name          = "Geocentric datum of Korea";
        crsName       = "Korea 2000";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4737, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Korean Datum 1985” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6162</b></li>
     *   <li>EPSG datum name: <b>Korean Datum 1985</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Korean 1985</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKoreanDatum1985() throws FactoryException {
        code          = 6162;
        name          = "Korean Datum 1985";
        crsName       = "Korean 1985";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4162, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Korean Datum 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6166</b></li>
     *   <li>EPSG datum name: <b>Korean Datum 1995</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Korean 1995</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKoreanDatum1995() throws FactoryException {
        code          = 6166;
        name          = "Korean Datum 1995";
        crsName       = "Korean 1995";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4166, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kousseri” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6198</b></li>
     *   <li>EPSG datum name: <b>Kousseri</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kousseri</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKousseri() throws FactoryException {
        code          = 6198;
        name          = "Kousseri";
        crsName       = "Kousseri";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4198, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kuwait Utility” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6319</b></li>
     *   <li>EPSG datum name: <b>Kuwait Utility</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>KUDAMS</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKuwaitUtility() throws FactoryException {
        code          = 6319;
        name          = "Kuwait Utility";
        crsName       = "KUDAMS";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4319, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Kusaie 1951” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6735</b></li>
     *   <li>EPSG datum name: <b>Kusaie 1951</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Kusaie 1951</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testKusaie1951() throws FactoryException {
        code          = 6735;
        name          = "Kusaie 1951";
        crsName       = "Kusaie 1951";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4735, GEOGRAPHIC_2D);
    }

    /**
     * Tests “La Canoa” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6247</b></li>
     *   <li>EPSG datum name: <b>La Canoa</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>La Canoa</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLaCanoa() throws FactoryException {
        code          = 6247;
        name          = "La Canoa";
        crsName       = "La Canoa";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4247, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lake” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6249</b></li>
     *   <li>EPSG datum name: <b>Lake</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Lake</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLake() throws FactoryException {
        code          = 6249;
        name          = "Lake";
        crsName       = "Lake";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4249, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lao 1993” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6677</b></li>
     *   <li>EPSG datum name: <b>Lao 1993</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Lao 1993</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLao1993() throws FactoryException {
        code          = 6677;
        name          = "Lao 1993";
        crsName       = "Lao 1993";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4677, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lao National Datum 1997” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6678</b></li>
     *   <li>EPSG datum name: <b>Lao National Datum 1997</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Lao 1997</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLaoNationalDatum1997() throws FactoryException {
        code          = 6678;
        name          = "Lao National Datum 1997";
        crsName       = "Lao 1997";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4678, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Le Pouce 1934” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6699</b></li>
     *   <li>EPSG datum name: <b>Le Pouce 1934</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Le Pouce 1934</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLePouce1934() throws FactoryException {
        code          = 6699;
        name          = "Le Pouce 1934";
        crsName       = "Le Pouce 1934";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4699, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Leigon” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6250</b></li>
     *   <li>EPSG datum name: <b>Leigon</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Leigon</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLeigon() throws FactoryException {
        code          = 6250;
        name          = "Leigon";
        crsName       = "Leigon";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4250, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Liberia 1964” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6251</b></li>
     *   <li>EPSG datum name: <b>Liberia 1964</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Liberia 1964</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLiberia1964() throws FactoryException {
        code          = 6251;
        name          = "Liberia 1964";
        crsName       = "Liberia 1964";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4251, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lisbon 1937” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6207</b></li>
     *   <li>EPSG datum name: <b>Lisbon 1937</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Lisbon</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLisbon1937() throws FactoryException {
        code          = 6207;
        name          = "Lisbon 1937";
        crsName       = "Lisbon";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4207, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lisbon 1937 (Lisbon)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6803</b></li>
     *   <li>EPSG datum name: <b>Lisbon 1937 (Lisbon)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Lisbon</b></li>
     *   <li>CRS using the datum: <b>Lisbon (Lisbon)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLisbon1937Lisbon() throws FactoryException {
        code          = 6803;
        name          = "Lisbon 1937 (Lisbon)";
        crsName       = "Lisbon (Lisbon)";
        ellipsoid     = "International 1924";
        primeMeridian = "Lisbon";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4803, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lisbon 1890” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6666</b></li>
     *   <li>EPSG datum name: <b>Lisbon 1890</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Lisbon 1890</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLisbon1890() throws FactoryException {
        code          = 6666;
        name          = "Lisbon 1890";
        crsName       = "Lisbon 1890";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4666, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lisbon 1890 (Lisbon)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6904</b></li>
     *   <li>EPSG datum name: <b>Lisbon 1890 (Lisbon)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Lisbon</b></li>
     *   <li>CRS using the datum: <b>Lisbon 1890 (Lisbon)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLisbon1890Lisbon() throws FactoryException {
        code          = 6904;
        name          = "Lisbon 1890 (Lisbon)";
        crsName       = "Lisbon 1890 (Lisbon)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Lisbon";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4904, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Little Cayman 1961” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6726</b></li>
     *   <li>EPSG datum name: <b>Little Cayman 1961</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Little Cayman 1961</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLittleCayman1961() throws FactoryException {
        code          = 6726;
        name          = "Little Cayman 1961";
        crsName       = "Little Cayman 1961";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4726, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Latvia 1992” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6661</b></li>
     *   <li>EPSG datum name: <b>Latvia 1992</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>LKS92</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLatvia1992() throws FactoryException {
        code          = 6661;
        name          = "Latvia 1992";
        crsName       = "LKS92";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4661, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lithuania 1994 (ETRS89)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6126</b></li>
     *   <li>EPSG datum name: <b>Lithuania 1994 (ETRS89)</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>LKS94</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLithuania1994ETRS89() throws FactoryException {
        code          = 6126;
        name          = "Lithuania 1994 (ETRS89)";
        crsName       = "LKS94";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4669, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Locodjo 1965” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6142</b></li>
     *   <li>EPSG datum name: <b>Locodjo 1965</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Locodjo 1965</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLocodjo1965() throws FactoryException {
        code          = 6142;
        name          = "Locodjo 1965";
        crsName       = "Locodjo 1965";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4142, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Loma Quintana” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6288</b></li>
     *   <li>EPSG datum name: <b>Loma Quintana</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Loma Quintana</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLomaQuintana() throws FactoryException {
        code          = 6288;
        name          = "Loma Quintana";
        crsName       = "Loma Quintana";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4288, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Lome” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6252</b></li>
     *   <li>EPSG datum name: <b>Lome</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Lome</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLome() throws FactoryException {
        code          = 6252;
        name          = "Lome";
        crsName       = "Lome";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4252, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Luxembourg 1930” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6181</b></li>
     *   <li>EPSG datum name: <b>Luxembourg 1930</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Luxembourg 1930</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testLuxembourg1930() throws FactoryException {
        code          = 6181;
        name          = "Luxembourg 1930";
        crsName       = "Luxembourg 1930";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4181, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Madrid 1870 (Madrid)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6903</b></li>
     *   <li>EPSG datum name: <b>Madrid 1870 (Madrid)</b></li>
     *   <li>Ellipsoid name: <b>Struve 1860</b></li>
     *   <li>Prime meridian name: <b>Madrid</b></li>
     *   <li>CRS using the datum: <b>Madrid 1870 (Madrid)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMadrid1870Madrid() throws FactoryException {
        code          = 6903;
        name          = "Madrid 1870 (Madrid)";
        crsName       = "Madrid 1870 (Madrid)";
        ellipsoid     = "Struve 1860";
        primeMeridian = "Madrid";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4903, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Madzansua” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6128</b></li>
     *   <li>EPSG datum name: <b>Madzansua</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Madzansua</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMadzansua() throws FactoryException {
        code          = 6128;
        name          = "Madzansua";
        crsName       = "Madzansua";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4128, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Mahe 1971” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6256</b></li>
     *   <li>EPSG datum name: <b>Mahe 1971</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Mahe 1971</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMahe1971() throws FactoryException {
        code          = 6256;
        name          = "Mahe 1971";
        crsName       = "Mahe 1971";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4256, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Makassar” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6257</b></li>
     *   <li>EPSG datum name: <b>Makassar</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Makassar</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMakassar() throws FactoryException {
        code          = 6257;
        name          = "Makassar";
        crsName       = "Makassar";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4257, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Makassar (Jakarta)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6804</b></li>
     *   <li>EPSG datum name: <b>Makassar (Jakarta)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Jakarta</b></li>
     *   <li>CRS using the datum: <b>Makassar (Jakarta)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMakassarJakarta() throws FactoryException {
        code          = 6804;
        name          = "Makassar (Jakarta)";
        crsName       = "Makassar (Jakarta)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Jakarta";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4804, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Marcus Island 1952” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6711</b></li>
     *   <li>EPSG datum name: <b>Marcus Island 1952</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Marcus Island 1952</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMarcusIsland1952() throws FactoryException {
        code          = 6711;
        name          = "Marcus Island 1952";
        crsName       = "Marcus Island 1952";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4711, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Marshall Islands 1960” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6732</b></li>
     *   <li>EPSG datum name: <b>Marshall Islands 1960</b></li>
     *   <li>Ellipsoid name: <b>Hough 1960</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Marshall Islands 1960</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMarshallIslands1960() throws FactoryException {
        code          = 6732;
        name          = "Marshall Islands 1960";
        crsName       = "Marshall Islands 1960";
        ellipsoid     = "Hough 1960";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4732, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Martinique 1938” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6625</b></li>
     *   <li>EPSG datum name: <b>Martinique 1938</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Martinique 1938</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMartinique1938() throws FactoryException {
        code          = 6625;
        name          = "Martinique 1938";
        crsName       = "Martinique 1938";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4625, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Massawa” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6262</b></li>
     *   <li>EPSG datum name: <b>Massawa</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Massawa</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMassawa() throws FactoryException {
        code          = 6262;
        name          = "Massawa";
        crsName       = "Massawa";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4262, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Maupiti 83” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6692</b></li>
     *   <li>EPSG datum name: <b>Maupiti 83</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Maupiti 83</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMaupiti83() throws FactoryException {
        code          = 6692;
        name          = "Maupiti 83";
        crsName       = "Maupiti 83";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4692, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Merchich” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6261</b></li>
     *   <li>EPSG datum name: <b>Merchich</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Merchich</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMerchich() throws FactoryException {
        code          = 6261;
        name          = "Merchich";
        crsName       = "Merchich";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4261, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Missao Hidrografico Angola y Sao Tome 1951” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6703</b></li>
     *   <li>EPSG datum name: <b>Missao Hidrografico Angola y Sao Tome 1951</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Mhast 1951</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMissaoHidrograficoAngolaYSaoTome1951() throws FactoryException {
        code          = 6703;
        name          = "Missao Hidrografico Angola y Sao Tome 1951";
        crsName       = "Mhast 1951";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4703, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Midway 1961” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6727</b></li>
     *   <li>EPSG datum name: <b>Midway 1961</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Midway 1961</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMidway1961() throws FactoryException {
        code          = 6727;
        name          = "Midway 1961";
        crsName       = "Midway 1961";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4727, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Monte Mario (Rome)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6806</b></li>
     *   <li>EPSG datum name: <b>Monte Mario (Rome)</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Rome</b></li>
     *   <li>CRS using the datum: <b>Monte Mario (Rome)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMonteMarioRome() throws FactoryException {
        code          = 6806;
        name          = "Monte Mario (Rome)";
        crsName       = "Monte Mario (Rome)";
        ellipsoid     = "International 1924";
        primeMeridian = "Rome";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4806, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Montserrat 1958” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6604</b></li>
     *   <li>EPSG datum name: <b>Montserrat 1958</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Montserrat 1958</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMontserrat1958() throws FactoryException {
        code          = 6604;
        name          = "Montserrat 1958";
        crsName       = "Montserrat 1958";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4604, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Moorea 87” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6691</b></li>
     *   <li>EPSG datum name: <b>Moorea 87</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Moorea 87</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMoorea87() throws FactoryException {
        code          = 6691;
        name          = "Moorea 87";
        crsName       = "Moorea 87";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4691, GEOGRAPHIC_2D);
    }

    /**
     * Tests “MOP78” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6639</b></li>
     *   <li>EPSG datum name: <b>MOP78</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>MOP78</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMOP78() throws FactoryException {
        code          = 6639;
        name          = "MOP78";
        crsName       = "MOP78";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4639, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Mount Dillon” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6157</b></li>
     *   <li>EPSG datum name: <b>Mount Dillon</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1858</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Mount Dillon</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMountDillon() throws FactoryException {
        code          = 6157;
        name          = "Mount Dillon";
        crsName       = "Mount Dillon";
        ellipsoid     = "Clarke 1858";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4157, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Moznet (ITRF94)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6130</b></li>
     *   <li>EPSG datum name: <b>Moznet (ITRF94)</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Moznet</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testMoznetITRF94() throws FactoryException {
        code          = 6130;
        name          = "Moznet (ITRF94)";
        crsName       = "Moznet";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4130, GEOGRAPHIC_2D);
    }

    /**
     * Tests “North American Datum 1927 (1976)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6608</b></li>
     *   <li>EPSG datum name: <b>North American Datum 1927 (1976)</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD27(76)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNorthAmericanDatum19271976() throws FactoryException {
        code          = 6608;
        name          = "North American Datum 1927 (1976)";
        crsName       = "NAD27(76)";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4608, GEOGRAPHIC_2D);
    }

    /**
     * Tests “North American Datum 1927 (CGQ77)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6609</b></li>
     *   <li>EPSG datum name: <b>North American Datum 1927 (CGQ77)</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NAD27(CGQ77)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNorthAmericanDatum1927CGQ77() throws FactoryException {
        code          = 6609;
        name          = "North American Datum 1927 (CGQ77)";
        crsName       = "NAD27(CGQ77)";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4609, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nahrwan 1934” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6744</b></li>
     *   <li>EPSG datum name: <b>Nahrwan 1934</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Nahrwan 1934</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNahrwan1934() throws FactoryException {
        code          = 6744;
        name          = "Nahrwan 1934";
        crsName       = "Nahrwan 1934";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4744, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nakhl-e Ghanem” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6693</b></li>
     *   <li>EPSG datum name: <b>Nakhl-e Ghanem</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Nakhl-e Ghanem</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNakhlEGhanem() throws FactoryException {
        code          = 6693;
        name          = "Nakhl-e Ghanem";
        crsName       = "Nakhl-e Ghanem";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4693, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Naparima 1972” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6271</b></li>
     *   <li>EPSG datum name: <b>Naparima 1972</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Naparima 1972</b></li>
     *   <li>Specific usage / Remarks: <b>Often confused with Naparima 1955.</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNaparima1972() throws FactoryException {
        code          = 6271;
        name          = "Naparima 1972";
        crsName       = "Naparima 1972";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4271, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NEA74 Noumea” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6644</b></li>
     *   <li>EPSG datum name: <b>NEA74 Noumea</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NEA74 Noumea</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNEA74Noumea() throws FactoryException {
        code          = 6644;
        name          = "NEA74 Noumea";
        crsName       = "NEA74 Noumea";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4644, GEOGRAPHIC_2D);
    }

    /**
     * Tests “National Geodetic Network” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6318</b></li>
     *   <li>EPSG datum name: <b>National Geodetic Network</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NGN</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNationalGeodeticNetwork() throws FactoryException {
        code          = 6318;
        name          = "National Geodetic Network";
        crsName       = "NGN";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4318, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NGO 1948” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6273</b></li>
     *   <li>EPSG datum name: <b>NGO 1948</b></li>
     *   <li>Ellipsoid name: <b>Bessel Modified</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NGO 1948</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNGO1948() throws FactoryException {
        code          = 6273;
        name          = "NGO 1948";
        crsName       = "NGO 1948";
        ellipsoid     = "Bessel Modified";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4273, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NGO 1948 (Oslo)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6817</b></li>
     *   <li>EPSG datum name: <b>NGO 1948 (Oslo)</b></li>
     *   <li>Ellipsoid name: <b>Bessel Modified</b></li>
     *   <li>Prime meridian name: <b>Oslo</b></li>
     *   <li>CRS using the datum: <b>NGO 1948 (Oslo)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNGO1948Oslo() throws FactoryException {
        code          = 6817;
        name          = "NGO 1948 (Oslo)";
        crsName       = "NGO 1948 (Oslo)";
        ellipsoid     = "Bessel Modified";
        primeMeridian = "Oslo";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4817, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Nouakchott 1965” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6680</b></li>
     *   <li>EPSG datum name: <b>Nouakchott 1965</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Nouakchott 1965</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNouakchott1965() throws FactoryException {
        code          = 6680;
        name          = "Nouakchott 1965";
        crsName       = "Nouakchott 1965";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4680, GEOGRAPHIC_2D);
    }

    /**
     * Tests “NSWC 9Z-2” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6276</b></li>
     *   <li>EPSG datum name: <b>NSWC 9Z-2</b></li>
     *   <li>Ellipsoid name: <b>NWL 9D</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>NSWC 9Z-2</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testNSWC9Z2() throws FactoryException {
        code          = 6276;
        name          = "NSWC 9Z-2";
        crsName       = "NSWC 9Z-2";
        ellipsoid     = "NWL 9D";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4276, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Observatario” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6129</b></li>
     *   <li>EPSG datum name: <b>Observatario</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Observatario</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testObservatario() throws FactoryException {
        code          = 6129;
        name          = "Observatario";
        crsName       = "Observatario";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4129, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Old Hawaiian” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6135</b></li>
     *   <li>EPSG datum name: <b>Old Hawaiian</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Old Hawaiian</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testOldHawaiian() throws FactoryException {
        code          = 6135;
        name          = "Old Hawaiian";
        crsName       = "Old Hawaiian";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4135, GEOGRAPHIC_2D);
    }

    /**
     * Tests “OS (SN) 1980” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6279</b></li>
     *   <li>EPSG datum name: <b>OS (SN) 1980</b></li>
     *   <li>Ellipsoid name: <b>Airy 1830</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>OS(SN)80</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testOSSN1980() throws FactoryException {
        code          = 6279;
        name          = "OS (SN) 1980";
        crsName       = "OS(SN)80";
        ellipsoid     = "Airy 1830";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4279, GEOGRAPHIC_2D);
    }

    /**
     * Tests “OSGB 1970 (SN)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6278</b></li>
     *   <li>EPSG datum name: <b>OSGB 1970 (SN)</b></li>
     *   <li>Ellipsoid name: <b>Airy 1830</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>OSGB70</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testOSGB1970SN() throws FactoryException {
        code          = 6278;
        name          = "OSGB 1970 (SN)";
        crsName       = "OSGB70";
        ellipsoid     = "Airy 1830";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4278, GEOGRAPHIC_2D);
    }

    /**
     * Tests “OSNI 1952” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6188</b></li>
     *   <li>EPSG datum name: <b>OSNI 1952</b></li>
     *   <li>Ellipsoid name: <b>Airy 1830</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>OSNI 1952</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testOSNI1952() throws FactoryException {
        code          = 6188;
        name          = "OSNI 1952";
        crsName       = "OSNI 1952";
        ellipsoid     = "Airy 1830";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4188, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Palestine 1923” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6281</b></li>
     *   <li>EPSG datum name: <b>Palestine 1923</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (Benoit)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Palestine 1923</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPalestine1923() throws FactoryException {
        code          = 6281;
        name          = "Palestine 1923";
        crsName       = "Palestine 1923";
        ellipsoid     = "Clarke 1880 (Benoit)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4281, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pampa del Castillo” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6161</b></li>
     *   <li>EPSG datum name: <b>Pampa del Castillo</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pampa del Castillo</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPampaDelCastillo() throws FactoryException {
        code          = 6161;
        name          = "Pampa del Castillo";
        crsName       = "Pampa del Castillo";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4161, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Potsdam Datum/83” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6746</b></li>
     *   <li>EPSG datum name: <b>Potsdam Datum/83</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>PD/83</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPotsdamDatum83() throws FactoryException {
        code          = 6746;
        name          = "Potsdam Datum/83";
        crsName       = "PD/83";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4746, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pointe Geologie Perroud 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6637</b></li>
     *   <li>EPSG datum name: <b>Pointe Geologie Perroud 1950</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Perroud 1950</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPointeGeologiePerroud1950() throws FactoryException {
        code          = 6637;
        name          = "Pointe Geologie Perroud 1950";
        crsName       = "Perroud 1950";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4637, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Petrels 1972” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6636</b></li>
     *   <li>EPSG datum name: <b>Petrels 1972</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Petrels 1972</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPetrels1972() throws FactoryException {
        code          = 6636;
        name          = "Petrels 1972";
        crsName       = "Petrels 1972";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4636, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Phoenix Islands 1966” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6716</b></li>
     *   <li>EPSG datum name: <b>Phoenix Islands 1966</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Phoenix Islands 1966</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPhoenixIslands1966() throws FactoryException {
        code          = 6716;
        name          = "Phoenix Islands 1966";
        crsName       = "Phoenix Islands 1966";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4716, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pico de las Nieves 1984” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6728</b></li>
     *   <li>EPSG datum name: <b>Pico de las Nieves 1984</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pico de las Nieves 1984</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPicoDeLasNieves1984() throws FactoryException {
        code          = 6728;
        name          = "Pico de las Nieves 1984";
        crsName       = "Pico de las Nieves 1984";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4728, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pitcairn 1967” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6729</b></li>
     *   <li>EPSG datum name: <b>Pitcairn 1967</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pitcairn 1967</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPitcairn1967() throws FactoryException {
        code          = 6729;
        name          = "Pitcairn 1967";
        crsName       = "Pitcairn 1967";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4729, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pitcairn 2006” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6763</b></li>
     *   <li>EPSG datum name: <b>Pitcairn 2006</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pitcairn 2006</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPitcairn2006() throws FactoryException {
        code          = 6763;
        name          = "Pitcairn 2006";
        crsName       = "Pitcairn 2006";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4763, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Point 58” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6620</b></li>
     *   <li>EPSG datum name: <b>Point 58</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Point 58</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPoint58() throws FactoryException {
        code          = 6620;
        name          = "Point 58";
        crsName       = "Point 58";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4620, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Popular Visualisation Datum” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6055</b></li>
     *   <li>EPSG datum name: <b>Popular Visualisation Datum</b></li>
     *   <li>Ellipsoid name: <b>Popular Visualisation Sphere</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Popular Visualisation CRS</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPopularVisualisationDatum() throws FactoryException {
        code          = 6055;
        name          = "Popular Visualisation Datum";
        crsName       = "Popular Visualisation CRS";
        ellipsoid     = "Popular Visualisation Sphere";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4055, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Porto Santo 1936” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6615</b></li>
     *   <li>EPSG datum name: <b>Porto Santo 1936</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Porto Santo</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPortoSanto1936() throws FactoryException {
        code          = 6615;
        name          = "Porto Santo 1936";
        crsName       = "Porto Santo";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4615, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Porto Santo 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6663</b></li>
     *   <li>EPSG datum name: <b>Porto Santo 1995</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Porto Santo 1995</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPortoSanto1995() throws FactoryException {
        code          = 6663;
        name          = "Porto Santo 1995";
        crsName       = "Porto Santo 1995";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4663, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Puerto Rico” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6139</b></li>
     *   <li>EPSG datum name: <b>Puerto Rico</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Puerto Rico</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPuertoRico() throws FactoryException {
        code          = 6139;
        name          = "Puerto Rico";
        crsName       = "Puerto Rico";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4139, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Pulkovo 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6200</b></li>
     *   <li>EPSG datum name: <b>Pulkovo 1995</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Pulkovo 1995</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testPulkovo1995() throws FactoryException {
        code          = 6200;
        name          = "Pulkovo 1995";
        crsName       = "Pulkovo 1995";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4200, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Parametrop Zemp 1990” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6740</b></li>
     *   <li>EPSG datum name: <b>Parametrop Zemp 1990</b></li>
     *   <li>Ellipsoid name: <b>PZ-90</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>PZ-90</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testParametropZemp1990() throws FactoryException {
        code          = 6740;
        name          = "Parametrop Zemp 1990";
        crsName       = "PZ-90";
        ellipsoid     = "PZ-90";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4740, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Qornoq 1927” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6194</b></li>
     *   <li>EPSG datum name: <b>Qornoq 1927</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Qornoq 1927</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testQornoq1927() throws FactoryException {
        code          = 6194;
        name          = "Qornoq 1927";
        crsName       = "Qornoq 1927";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4194, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Rassadiran” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6153</b></li>
     *   <li>EPSG datum name: <b>Rassadiran</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Rassadiran</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testRassadiran() throws FactoryException {
        code          = 6153;
        name          = "Rassadiran";
        crsName       = "Rassadiran";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4153, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Rauenberg Datum/83” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6745</b></li>
     *   <li>EPSG datum name: <b>Rauenberg Datum/83</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RD/83</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testRauenbergDatum83() throws FactoryException {
        code          = 6745;
        name          = "Rauenberg Datum/83";
        crsName       = "RD/83";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4745, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reunion 1947” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6626</b></li>
     *   <li>EPSG datum name: <b>Reunion 1947</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Reunion 1947</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReunion1947() throws FactoryException {
        code          = 6626;
        name          = "Reunion 1947";
        crsName       = "Reunion 1947";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4626, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reykjavik 1900” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6657</b></li>
     *   <li>EPSG datum name: <b>Reykjavik 1900</b></li>
     *   <li>Ellipsoid name: <b>Danish 1876</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Reykjavik 1900</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReykjavik1900() throws FactoryException {
        code          = 6657;
        name          = "Reykjavik 1900";
        crsName       = "Reykjavik 1900";
        ellipsoid     = "Danish 1876";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4657, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau Geodesique Francais Guyane 1995” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6624</b></li>
     *   <li>EPSG datum name: <b>Reseau Geodesique Francais Guyane 1995</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RGFG95</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauGeodesiqueFrancaisGuyane1995() throws FactoryException {
        code          = 6624;
        name          = "Reseau Geodesique Francais Guyane 1995";
        crsName       = "RGFG95";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4624, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau Geodesique de Nouvelle Caledonie 91-93” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6749</b></li>
     *   <li>EPSG datum name: <b>Reseau Geodesique de Nouvelle Caledonie 91-93</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RGNC91-93</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauGeodesiqueDeNouvelleCaledonie9193() throws FactoryException {
        code          = 6749;
        name          = "Reseau Geodesique de Nouvelle Caledonie 91-93";
        crsName       = "RGNC91-93";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4749, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau Geodesique de la Polynesie Francaise” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6687</b></li>
     *   <li>EPSG datum name: <b>Reseau Geodesique de la Polynesie Francaise</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RGPF</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauGeodesiqueDeLaPolynesieFrancaise() throws FactoryException {
        code          = 6687;
        name          = "Reseau Geodesique de la Polynesie Francaise";
        crsName       = "RGPF";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4687, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau Geodesique de la Reunion 1992” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6627</b></li>
     *   <li>EPSG datum name: <b>Reseau Geodesique de la Reunion 1992</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RGR92</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauGeodesiqueDeLaReunion1992() throws FactoryException {
        code          = 6627;
        name          = "Reseau Geodesique de la Reunion 1992";
        crsName       = "RGR92";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4627, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Reseau de Reference des Antilles Francaises 1991” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6640</b></li>
     *   <li>EPSG datum name: <b>Reseau de Reference des Antilles Francaises 1991</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RRAF 1991</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testReseauDeReferenceDesAntillesFrancaises1991() throws FactoryException {
        code          = 6640;
        name          = "Reseau de Reference des Antilles Francaises 1991";
        crsName       = "RRAF 1991";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4640, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Ross Sea Region Geodetic Datum 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6764</b></li>
     *   <li>EPSG datum name: <b>Ross Sea Region Geodetic Datum 2000</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RSRGD2000</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testRossSeaRegionGeodeticDatum2000() throws FactoryException {
        code          = 6764;
        name          = "Ross Sea Region Geodetic Datum 2000";
        crsName       = "RSRGD2000";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4764, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Stockholm 1938” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6308</b></li>
     *   <li>EPSG datum name: <b>Stockholm 1938</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RT38</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStockholm1938() throws FactoryException {
        code          = 6308;
        name          = "Stockholm 1938";
        crsName       = "RT38";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4308, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Stockholm 1938 (Stockholm)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6814</b></li>
     *   <li>EPSG datum name: <b>Stockholm 1938 (Stockholm)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Stockholm</b></li>
     *   <li>CRS using the datum: <b>RT38 (Stockholm)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStockholm1938Stockholm() throws FactoryException {
        code          = 6814;
        name          = "Stockholm 1938 (Stockholm)";
        crsName       = "RT38 (Stockholm)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Stockholm";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4814, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Rikets koordinatsystem 1990” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6124</b></li>
     *   <li>EPSG datum name: <b>Rikets koordinatsystem 1990</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>RT90</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testRiketsKoordinatsystem1990() throws FactoryException {
        code          = 6124;
        name          = "Rikets koordinatsystem 1990";
        crsName       = "RT90";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4124, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Saint Pierre et Miquelon 1950” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6638</b></li>
     *   <li>EPSG datum name: <b>Saint Pierre et Miquelon 1950</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Saint Pierre et Miquelon 1950</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSaintPierreEtMiquelon1950() throws FactoryException {
        code          = 6638;
        name          = "Saint Pierre et Miquelon 1950";
        crsName       = "Saint Pierre et Miquelon 1950";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4638, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Santo 1965” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6730</b></li>
     *   <li>EPSG datum name: <b>Santo 1965</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Santo 1965</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSanto1965() throws FactoryException {
        code          = 6730;
        name          = "Santo 1965";
        crsName       = "Santo 1965";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4730, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Sapper Hill 1943” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6292</b></li>
     *   <li>EPSG datum name: <b>Sapper Hill 1943</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Sapper Hill 1943</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSapperHill1943() throws FactoryException {
        code          = 6292;
        name          = "Sapper Hill 1943";
        crsName       = "Sapper Hill 1943";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4292, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Scoresbysund 1952” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6195</b></li>
     *   <li>EPSG datum name: <b>Scoresbysund 1952</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Scoresbysund 1952</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testScoresbysund1952() throws FactoryException {
        code          = 6195;
        name          = "Scoresbysund 1952";
        crsName       = "Scoresbysund 1952";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4195, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Gunung Segara” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6613</b></li>
     *   <li>EPSG datum name: <b>Gunung Segara</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Segara</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGunungSegara() throws FactoryException {
        code          = 6613;
        name          = "Gunung Segara";
        crsName       = "Segara";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4613, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Gunung Segara (Jakarta)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6820</b></li>
     *   <li>EPSG datum name: <b>Gunung Segara (Jakarta)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Jakarta</b></li>
     *   <li>CRS using the datum: <b>Segara (Jakarta)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGunungSegaraJakarta() throws FactoryException {
        code          = 6820;
        name          = "Gunung Segara (Jakarta)";
        crsName       = "Segara (Jakarta)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Jakarta";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4820, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Selvagem Grande” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6616</b></li>
     *   <li>EPSG datum name: <b>Selvagem Grande</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Selvagem Grande</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSelvagemGrande() throws FactoryException {
        code          = 6616;
        name          = "Selvagem Grande";
        crsName       = "Selvagem Grande";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4616, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Serindung” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6295</b></li>
     *   <li>EPSG datum name: <b>Serindung</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Serindung</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSerindung() throws FactoryException {
        code          = 6295;
        name          = "Serindung";
        crsName       = "Serindung";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4295, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Sierra Leone Colony 1924” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6174</b></li>
     *   <li>EPSG datum name: <b>Sierra Leone Colony 1924</b></li>
     *   <li>Ellipsoid name: <b>War Office</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Sierra Leone 1924</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSierraLeoneColony1924() throws FactoryException {
        code          = 6174;
        name          = "Sierra Leone Colony 1924";
        crsName       = "Sierra Leone 1924";
        ellipsoid     = "War Office";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4174, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Sierra Leone 1968” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6175</b></li>
     *   <li>EPSG datum name: <b>Sierra Leone 1968</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Sierra Leone 1968</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSierraLeone1968() throws FactoryException {
        code          = 6175;
        name          = "Sierra Leone 1968";
        crsName       = "Sierra Leone 1968";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4175, GEOGRAPHIC_2D);
    }

    /**
     * Tests “System Jednotne Trigonometricke Site Katastralni” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6156</b></li>
     *   <li>EPSG datum name: <b>System Jednotne Trigonometricke Site Katastralni</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>S-JTSK</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSystemJednotneTrigonometrickeSiteKatastralni() throws FactoryException {
        code          = 6156;
        name          = "System Jednotne Trigonometricke Site Katastralni";
        crsName       = "S-JTSK";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4156, GEOGRAPHIC_2D);
    }

    /**
     * Tests “System Jednotne Trigonometricke Site Katastralni (Ferro)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6818</b></li>
     *   <li>EPSG datum name: <b>System Jednotne Trigonometricke Site Katastralni (Ferro)</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Ferro</b></li>
     *   <li>CRS using the datum: <b>S-JTSK (Ferro)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSystemJednotneTrigonometrickeSiteKatastralniFerro() throws FactoryException {
        code          = 6818;
        name          = "System Jednotne Trigonometricke Site Katastralni (Ferro)";
        crsName       = "S-JTSK (Ferro)";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Ferro";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4818, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Slovenia Geodetic Datum 1996” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6765</b></li>
     *   <li>EPSG datum name: <b>Slovenia Geodetic Datum 1996</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Slovenia 1996</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSloveniaGeodeticDatum1996() throws FactoryException {
        code          = 6765;
        name          = "Slovenia Geodetic Datum 1996";
        crsName       = "Slovenia 1996";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4765, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Solomon 1968” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6718</b></li>
     *   <li>EPSG datum name: <b>Solomon 1968</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Solomon 1968</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSolomon1968() throws FactoryException {
        code          = 6718;
        name          = "Solomon 1968";
        crsName       = "Solomon 1968";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4718, GEOGRAPHIC_2D);
    }

    /**
     * Tests “South Georgia 1968” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6722</b></li>
     *   <li>EPSG datum name: <b>South Georgia 1968</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>South Georgia 1968</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSouthGeorgia1968() throws FactoryException {
        code          = 6722;
        name          = "South Georgia 1968";
        crsName       = "South Georgia 1968";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4722, GEOGRAPHIC_2D);
    }

    /**
     * Tests “South Yemen” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6164</b></li>
     *   <li>EPSG datum name: <b>South Yemen</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>South Yemen</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSouthYemen() throws FactoryException {
        code          = 6164;
        name          = "South Yemen";
        crsName       = "South Yemen";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4164, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. George Island” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6138</b></li>
     *   <li>EPSG datum name: <b>St. George Island</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. George Island</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStGeorgeIsland() throws FactoryException {
        code          = 6138;
        name          = "St. George Island";
        crsName       = "St. George Island";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4138, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. Helena 1971” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6710</b></li>
     *   <li>EPSG datum name: <b>St. Helena 1971</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. Helena 1971</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStHelena1971() throws FactoryException {
        code          = 6710;
        name          = "St. Helena 1971";
        crsName       = "St. Helena 1971";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4710, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. Kitts 1955” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6605</b></li>
     *   <li>EPSG datum name: <b>St. Kitts 1955</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. Kitts 1955</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStKitts1955() throws FactoryException {
        code          = 6605;
        name          = "St. Kitts 1955";
        crsName       = "St. Kitts 1955";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4605, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. Lawrence Island” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6136</b></li>
     *   <li>EPSG datum name: <b>St. Lawrence Island</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. Lawrence Island</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStLawrenceIsland() throws FactoryException {
        code          = 6136;
        name          = "St. Lawrence Island";
        crsName       = "St. Lawrence Island";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4136, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. Lucia 1955” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6606</b></li>
     *   <li>EPSG datum name: <b>St. Lucia 1955</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. Lucia 1955</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStLucia1955() throws FactoryException {
        code          = 6606;
        name          = "St. Lucia 1955";
        crsName       = "St. Lucia 1955";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4606, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. Paul Island” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6137</b></li>
     *   <li>EPSG datum name: <b>St. Paul Island</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. Paul Island</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStPaulIsland() throws FactoryException {
        code          = 6137;
        name          = "St. Paul Island";
        crsName       = "St. Paul Island";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4137, GEOGRAPHIC_2D);
    }

    /**
     * Tests “St. Vincent 1945” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6607</b></li>
     *   <li>EPSG datum name: <b>St. Vincent 1945</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (RGS)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>St. Vincent 1945</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testStVincent1945() throws FactoryException {
        code          = 6607;
        name          = "St. Vincent 1945";
        crsName       = "St. Vincent 1945";
        ellipsoid     = "Clarke 1880 (RGS)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4607, GEOGRAPHIC_2D);
    }

    /**
     * Tests “ST71 Belep” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6643</b></li>
     *   <li>EPSG datum name: <b>ST71 Belep</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ST71 Belep</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testST71Belep() throws FactoryException {
        code          = 6643;
        name          = "ST71 Belep";
        crsName       = "ST71 Belep";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4643, GEOGRAPHIC_2D);
    }

    /**
     * Tests “ST84 Ile des Pins” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6642</b></li>
     *   <li>EPSG datum name: <b>ST84 Ile des Pins</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ST84 Ile des Pins</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testST84IleDesPins() throws FactoryException {
        code          = 6642;
        name          = "ST84 Ile des Pins";
        crsName       = "ST84 Ile des Pins";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4642, GEOGRAPHIC_2D);
    }

    /**
     * Tests “ST87 Ouvea” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6750</b></li>
     *   <li>EPSG datum name: <b>ST87 Ouvea</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>ST87 Ouvea</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testST87Ouvea() throws FactoryException {
        code          = 6750;
        name          = "ST87 Ouvea";
        crsName       = "ST87 Ouvea";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4750, GEOGRAPHIC_2D);
    }

    /**
     * Tests “SVY21” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6757</b></li>
     *   <li>EPSG datum name: <b>SVY21</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>SVY21</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSVY21() throws FactoryException {
        code          = 6757;
        name          = "SVY21";
        crsName       = "SVY21";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4757, GEOGRAPHIC_2D);
    }

    /**
     * Tests “SWEREF99” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6619</b></li>
     *   <li>EPSG datum name: <b>SWEREF99</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>SWEREF99</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testSWEREF99() throws FactoryException {
        code          = 6619;
        name          = "SWEREF99";
        crsName       = "SWEREF99";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4619, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tahaa 54” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6629</b></li>
     *   <li>EPSG datum name: <b>Tahaa 54</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tahaa 54</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTahaa54() throws FactoryException {
        code          = 6629;
        name          = "Tahaa 54";
        crsName       = "Tahaa 54";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4629, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tahiti 52” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6628</b></li>
     *   <li>EPSG datum name: <b>Tahiti 52</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tahiti 52</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTahiti52() throws FactoryException {
        code          = 6628;
        name          = "Tahiti 52";
        crsName       = "Tahiti 52";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4628, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tahiti 79” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6690</b></li>
     *   <li>EPSG datum name: <b>Tahiti 79</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tahiti 79</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTahiti79() throws FactoryException {
        code          = 6690;
        name          = "Tahiti 79";
        crsName       = "Tahiti 79";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4690, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tern Island 1961” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6707</b></li>
     *   <li>EPSG datum name: <b>Tern Island 1961</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tern Island 1961</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTernIsland1961() throws FactoryException {
        code          = 6707;
        name          = "Tern Island 1961";
        crsName       = "Tern Island 1961";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4707, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tete” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6127</b></li>
     *   <li>EPSG datum name: <b>Tete</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1866</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tete</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTete() throws FactoryException {
        code          = 6127;
        name          = "Tete";
        crsName       = "Tete";
        ellipsoid     = "Clarke 1866";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4127, GEOGRAPHIC_2D);
    }

    /**
     * Tests “TM65” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6299</b></li>
     *   <li>EPSG datum name: <b>TM65</b></li>
     *   <li>Ellipsoid name: <b>Airy Modified 1849</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>TM65</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTM65() throws FactoryException {
        code          = 6299;
        name          = "TM65";
        crsName       = "TM65";
        ellipsoid     = "Airy Modified 1849";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4299, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Geodetic Datum of 1965” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6300</b></li>
     *   <li>EPSG datum name: <b>Geodetic Datum of 1965</b></li>
     *   <li>Ellipsoid name: <b>Airy Modified 1849</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>TM75</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testGeodeticDatumOf1965() throws FactoryException {
        code          = 6300;
        name          = "Geodetic Datum of 1965";
        crsName       = "TM75";
        ellipsoid     = "Airy Modified 1849";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4300, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tokyo” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6301</b></li>
     *   <li>EPSG datum name: <b>Tokyo</b></li>
     *   <li>Ellipsoid name: <b>Bessel 1841</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tokyo</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTokyo() throws FactoryException {
        code          = 6301;
        name          = "Tokyo";
        crsName       = "Tokyo";
        ellipsoid     = "Bessel 1841";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4301, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Tristan 1968” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6734</b></li>
     *   <li>EPSG datum name: <b>Tristan 1968</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Tristan 1968</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTristan1968() throws FactoryException {
        code          = 6734;
        name          = "Tristan 1968";
        crsName       = "Tristan 1968";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4734, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Taiwan Datum 1967” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>1025</b></li>
     *   <li>EPSG datum name: <b>Taiwan Datum 1967</b></li>
     *   <li>Ellipsoid name: <b>GRS 1967 Modified</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>TWD67</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTaiwanDatum1967() throws FactoryException {
        code          = 1025;
        name          = "Taiwan Datum 1967";
        crsName       = "TWD67";
        ellipsoid     = "GRS 1967 Modified";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(3821, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Taiwan Datum 1997” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>1026</b></li>
     *   <li>EPSG datum name: <b>Taiwan Datum 1997</b></li>
     *   <li>Ellipsoid name: <b>GRS 1980</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>TWD97</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testTaiwanDatum1997() throws FactoryException {
        code          = 1026;
        name          = "Taiwan Datum 1997";
        crsName       = "TWD97";
        ellipsoid     = "GRS 1980";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(3824, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Vanua Levu 1915” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6748</b></li>
     *   <li>EPSG datum name: <b>Vanua Levu 1915</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (international foot)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Vanua Levu 1915</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVanuaLevu1915() throws FactoryException {
        code          = 6748;
        name          = "Vanua Levu 1915";
        crsName       = "Vanua Levu 1915";
        ellipsoid     = "Clarke 1880 (international foot)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4748, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Vientiane 1982” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6676</b></li>
     *   <li>EPSG datum name: <b>Vientiane 1982</b></li>
     *   <li>Ellipsoid name: <b>Krassowsky 1940</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Vientiane 1982</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVientiane1982() throws FactoryException {
        code          = 6676;
        name          = "Vientiane 1982";
        crsName       = "Vientiane 1982";
        ellipsoid     = "Krassowsky 1940";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4676, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Viti Levu 1912” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6752</b></li>
     *   <li>EPSG datum name: <b>Viti Levu 1912</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (international foot)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Viti Levu 1912</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVitiLevu1912() throws FactoryException {
        code          = 6752;
        name          = "Viti Levu 1912";
        crsName       = "Viti Levu 1912";
        ellipsoid     = "Clarke 1880 (international foot)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4752, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Vietnam 2000” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6756</b></li>
     *   <li>EPSG datum name: <b>Vietnam 2000</b></li>
     *   <li>Ellipsoid name: <b>WGS 84</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>VN-2000</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVietnam2000() throws FactoryException {
        code          = 6756;
        name          = "Vietnam 2000";
        crsName       = "VN-2000";
        ellipsoid     = "WGS 84";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4756, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Voirol 1879” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6671</b></li>
     *   <li>EPSG datum name: <b>Voirol 1879</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Voirol 1879</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVoirol1879() throws FactoryException {
        code          = 6671;
        name          = "Voirol 1879";
        crsName       = "Voirol 1879";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4671, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Voirol 1879 (Paris)” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6821</b></li>
     *   <li>EPSG datum name: <b>Voirol 1879 (Paris)</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Paris</b></li>
     *   <li>CRS using the datum: <b>Voirol 1879 (Paris)</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testVoirol1879Paris() throws FactoryException {
        code          = 6821;
        name          = "Voirol 1879 (Paris)";
        crsName       = "Voirol 1879 (Paris)";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Paris";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4821, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Wake Island 1952” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6733</b></li>
     *   <li>EPSG datum name: <b>Wake Island 1952</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Wake Island 1952</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testWakeIsland1952() throws FactoryException {
        code          = 6733;
        name          = "Wake Island 1952";
        crsName       = "Wake Island 1952";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4733, GEOGRAPHIC_2D);
    }

    /**
     * Tests “World Geodetic System 1966” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6760</b></li>
     *   <li>EPSG datum name: <b>World Geodetic System 1966</b></li>
     *   <li>Ellipsoid name: <b>NWL 9D</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>WGS 66</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testWorldGeodeticSystem1966() throws FactoryException {
        code          = 6760;
        name          = "World Geodetic System 1966";
        crsName       = "WGS 66";
        ellipsoid     = "NWL 9D";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4760, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Yacare” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6309</b></li>
     *   <li>EPSG datum name: <b>Yacare</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Yacare</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testYacare() throws FactoryException {
        code          = 6309;
        name          = "Yacare";
        crsName       = "Yacare";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4309, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Yoff” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6310</b></li>
     *   <li>EPSG datum name: <b>Yoff</b></li>
     *   <li>Ellipsoid name: <b>Clarke 1880 (IGN)</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Yoff</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testYoff() throws FactoryException {
        code          = 6310;
        name          = "Yoff";
        crsName       = "Yoff";
        ellipsoid     = "Clarke 1880 (IGN)";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4310, GEOGRAPHIC_2D);
    }

    /**
     * Tests “Zanderij” geodetic datum creation from the factory.
     *
     * <ul>
     *   <li>EPSG datum code: <b>6311</b></li>
     *   <li>EPSG datum name: <b>Zanderij</b></li>
     *   <li>Ellipsoid name: <b>International 1924</b></li>
     *   <li>Prime meridian name: <b>Greenwich</b></li>
     *   <li>CRS using the datum: <b>Zanderij</b></li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating the datum or a CRS from the EPSG code.
     */
    @Test
    public void testZanderij() throws FactoryException {
        code          = 6311;
        name          = "Zanderij";
        crsName       = "Zanderij";
        ellipsoid     = "International 1924";
        primeMeridian = "Greenwich";
        createAndVerifyDatum();
        createAndVerifyGeographicCRS(4311, GEOGRAPHIC_2D);
    }
}