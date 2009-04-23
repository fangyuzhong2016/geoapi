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
package org.opengis.coverage.grid;

import java.util.Set;
import org.opengis.coverage.DomainObject;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Point located at the intersection of two or more curves in a {@linkplain Grid grid}.
 *
 * @version ISO 19123:2004
 * @author  Martin Schouwenburg
 * @author  Wim Koolhoven
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 2.1
 */
@UML(identifier="CV_GridPoint", specification=ISO_19123)
public interface GridPoint extends DomainObject<Geometry> {
    /**
     * Returns the set of grid coordinates that specifies the location of the
     * grid point within the {@linkplain Grid grid}.
     *
     * @return The coordinates that specifieds the location of the grid point.
     */
    @UML(identifier="gridCoord", obligation=MANDATORY, specification=ISO_19123)
    GridCoordinates getGridCoordinates();

    /**
     * Returns the {@linkplain Grid grid} of which this grid point is an element.
     *
     * @return The grid of which this grid point is an element.
     *
     * @see Grid#getIntersections
     */
    @UML(identifier="framework", obligation=MANDATORY, specification=ISO_19123)
    Grid getFramework();

    /**
     * Returns the set of {@linkplain GridCell grid cells} for which this grid point is a corner.
     *
     * @return The grid cells for which this grid point is a corner.
     *
     * @see GridCell#getCorners
     */
    @UML(identifier="cell", obligation=MANDATORY, specification=ISO_19123)
    Set<GridCell> getCells();

    /**
     * Returns the representation of the grid point in an external
     * {@linkplain CoordinateReferenceSystem coordinate reference system}.
     *
     * @return The representation of this grid point in an external CRS.
     */
    @UML(identifier="groundPoint", obligation=OPTIONAL, specification=ISO_19123)
    Point getGroundPoint();

    /**
     * Returns the {@linkplain FootPrint foot prints} that represents the sample space in an external
     * {@linkplain CoordinateReferenceSystem coordinate reference system} associated with this grid
     * point. The multiplicity of the association allows for multiple external coordinate reference
     * systems for foot print.
     *
     * @return The foot prints that represents the sample space in an external CRS.
     *
     * @see FootPrint#getCenter
     */
    @UML(identifier="footprint", obligation=OPTIONAL, specification=ISO_19123)
    Set<FootPrint> getFootPrints();
}
