/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $URL$
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.maintenance;

import java.util.Set;
import org.opengis.annotation.UML;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Description of the class of information covered by the information.
 *
 * @version <A HREF="http://www.opengis.org/docs/01-111.pdf">Abstract specification 5.0</A>
 * @author Martin Desruisseaux (IRD)
 * @since GeoAPI 2.0
 */
@UML(identifier="MD_ScopeDescription", specification=ISO_19115)
public interface ScopeDescription {
    /**
     * Attributes to which the information applies.
     */
    @UML(identifier="attributes", obligation=CONDITIONAL, specification=ISO_19115)
    public Set<AttributeType> getAttributes();

    /**
     * Features to which the information applies.
     */
    @UML(identifier="features", obligation=CONDITIONAL, specification=ISO_19115)
    public Set<FeatureType> getFeatures();

    /**
     * Feature instances to which the information applies.
     */
    @UML(identifier="featureInstances", obligation=CONDITIONAL, specification=ISO_19115)
    public Set<FeatureType> getFeatureInstances();

    /**
     * Attribute instances to which the information applies
     */
    @UML(identifier="attributeInstances", obligation=CONDITIONAL, specification=ISO_19115)
    public Set<AttributeType> getAttributeInstances();

    /**
     * Dataset to which the information applies.
     */
    @UML(identifier="dataset", obligation=CONDITIONAL, specification=ISO_19115)
    public String getDataset();

    /**
     * Class of information that does not fall into the other categories to
     * which the information applies.
     */
    @UML(identifier="other", obligation=CONDITIONAL, specification=ISO_19115)
    public String getOther();
}
