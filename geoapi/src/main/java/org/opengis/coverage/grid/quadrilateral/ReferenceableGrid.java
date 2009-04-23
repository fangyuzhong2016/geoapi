/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $URL$
 **
 ** Copyright (C) 2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.coverage.grid.quadrilateral;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.geometry.DirectPosition;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * A modified copy of {@link org.opengis.coverage.grid.ReferenceableGrid} which does not inherit
 * {@link org.opengis.coverage.grid.Grid}.
 *
 * @issue http://jira.codehaus.org/browse/GEO-82
 *
 * @version ISO 19123:2004
 * @author  Wim Koolhoven
 * @author  Martin Schouwenburg
 * @author  Alexander Petkov
 */
@UML(identifier="CV_ReferenceableGrid", specification=ISO_19123)
public interface ReferenceableGrid extends RectifiableGrid {
    /**
     * This inherited association from GridPositioning specifies the coordinate system into which
     * this object transforms coordinates. ISO 19123 specifies this association directly on this class.
     */
    @UML(identifier="CoordinateReferenceSystem", obligation=MANDATORY, specification=ISO_19123)
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Transforms the specified {@linkplain GridCoordinates} to a location in an external CRS.
     * This method may transform the {@linkplain GridCoordinates} directly,
     * or may transform the associated sample point.
     *
     * @param g The coordinate to transform.
     * @return The transformed coordinate.
     */
    @UML(identifier="coordTransform", obligation=MANDATORY, specification=ISO_19123)
    DirectPosition transformCoordinates(GridCoordinates g);

    /**
     * Transforms from a direct position to the grid coordinates of the nearest grid point.
     *
     * @param p The coordinate to transform.
     * @return The transformed coordinate.
     *
     * @todo Question (Wim): GridCoordinates are always integers, how to get
     *       the not rounded results?<br>
     *       Martin: The legacy OGC specification defined a "gridToCRS" math transform for
     *       that. We may consider to import this element in the proposed set of interfaces.
     */
    @UML(identifier="invCoordTransform", obligation=MANDATORY, specification=ISO_19123)
    GridCoordinates inverseTransformCoordinates(DirectPosition p);
}
