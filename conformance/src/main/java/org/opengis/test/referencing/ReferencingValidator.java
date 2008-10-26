/*$************************************************************************************************
 **
 ** $Id$
 **
 ** $URL$
 **
 ** Copyright (C) 2008 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.test.referencing;

import java.util.Collection;

import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.util.GenericName;

import org.opengis.test.Validator;
import org.opengis.test.ValidatorContainer;


/**
 * Validates {@link IdentifiedObject} and related objects from the {@code org.opengis.referencing}
 * package. This class should not be used directly; use the {@link org.opengis.Validators} convenience
 * static methods instead.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since GeoAPI 2.2
 */
public abstract class ReferencingValidator extends Validator {
    /**
     * Creates a new validator.
     *
     * @param container   The container of this validator.
     * @param packageName The name of the package containing the classes to be validated.
     */
    public ReferencingValidator(final ValidatorContainer container, final String packageName) {
        super(container, packageName);
    }

    /**
     * Delegates to a {@code validate} method expecting a more specific argument.
     *
     * @param object The object to validate, or {@code null}.
     */
    public final void dispatchObject(final IdentifiedObject object) {
        if (object instanceof CoordinateReferenceSystem) {
            container.crs.dispatch((CoordinateReferenceSystem) object);
        } else if (object instanceof CoordinateSystem) {
            container.cs.dispatch((CoordinateSystem) object);
        } else if (object instanceof CoordinateSystemAxis) {
            container.cs.validate((CoordinateSystemAxis) object);
        } else if (object instanceof Datum) {
            container.datum.dispatch((Datum) object);
        } else if (object instanceof Ellipsoid) {
            container.datum.validate((Ellipsoid) object);
        } else if (object instanceof PrimeMeridian) {
            container.datum.validate((PrimeMeridian) object);
        } else if (object instanceof GeneralParameterDescriptor) {
            container.parameter.dispatch((GeneralParameterDescriptor) object);
        } else if (object instanceof CoordinateOperation) {
            container.coordinateOperation.dispatch((CoordinateOperation) object);
        } else if (object instanceof OperationMethod) {
            container.coordinateOperation.validate((OperationMethod) object);
        } else if (object instanceof ReferenceSystem) {
            validateReferenceSystem((ReferenceSystem) object);
        } else if (object != null) {
            validateIdentifiedObject(object);
        }
    }

    /**
     * Ensures that the given identifier has a {@linkplain ReferenceIdentifier#getCode code}.
     *
     * @param object The object to validate, or {@code null}.
     */
    public void validate(final ReferenceIdentifier object) {
        if (object == null) {
            return;
        }
        mandatory("ReferenceIdentifier: must have a code.", object.getCode());
        container.citation.validate(object.getAuthority());
    }

    /**
     * Performs the validation that are common to all reference systems. This method is
     * invoked by {@code validate} methods after they have determined the type of their
     * argument.
     *
     * @param object The object to validate (can not be null).
     */
    final void validateReferenceSystem(final ReferenceSystem object) {
        validateIdentifiedObject(object);
        container.naming.validate(object.getScope());
        container.extent.validate(object.getDomainOfValidity());
    }

    /**
     * Performs the validation that are common to all identified objects. This method is
     * invoked by {@code validate} methods after they have determined the type of their
     * argument.
     *
     * @param object The object to validate (can not be null).
     */
    final void validateIdentifiedObject(final IdentifiedObject object) {
        validate(object.getName());
        final Collection<ReferenceIdentifier> identifiers = object.getIdentifiers();
        if (identifiers != null) {
            for (final ReferenceIdentifier id : identifiers) {
                assertNotNull("IdentifiedObject: getIdentifiers() can not contain null element.", id);
                validate(id);
            }
        }
        final Collection<GenericName> alias = object.getAlias();
        if (alias != null) {
            for (final GenericName name : alias) {
                assertNotNull("IdentifiedObject: getAlias() can not contain null element.", alias);
                container.naming.dispatch(name);
            }
        }
        container.naming.validate(object.getRemarks());
    }
}
