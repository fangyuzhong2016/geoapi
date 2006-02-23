/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $Source$
 **
 ** Copyright (C) 2003 Open GIS Consortium, Inc. All Rights Reserved. http://www.opengis.org/Legal/
 **
 *************************************************************************************************/
package org.opengis.referencing;


/**
 * Thrown when an {@linkplain AuthorityFactory authority factory} can't find
 * the requested authority code.
 *
 * @author <A HREF="http://www.opengis.org">OpenGIS&reg; consortium</A>
 * @version 1.0
 *
 * @see org.opengis.referencing.datum.DatumAuthorityFactory
 * @see org.opengis.referencing.crs.CRSAuthorityFactory
 */
public class NoSuchAuthorityCodeException extends FactoryException {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1573748311981746573L;

    /**
     * The authority.
     */
    private final String authority;

    /**
     * The invalid authority code.
     */
    private final String code;

    /**
     * Construct an exception with the specified detail message and authority code.
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     * @param  authority The authority.
     * @param  code The invalid authority code.
     */
    public NoSuchAuthorityCodeException(String message, String authority, String code) {
        super(message);
        this.authority = authority;
        this.code = code;
    }

    /**
     * Returns the authority.
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Returns the invalid authority code.
     */
    public String getAuthorityCode() {
        return code;
    }
}
