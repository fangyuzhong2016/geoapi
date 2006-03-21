package org.opengis.feature.xml;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.filter.Filter;

/**
 * Indicates a ComplexType that indicates a required ordering of attributes.
 * <p>
 * You may mark your sequence as "inline" as dictated by the needs of your XML Schema.
 * </p>
 * 
 * @author Jody Garnett, Refractions Research
 */
public interface SequenceType extends ComplexType {
	
	/** Indicates required ordering for Attributes */
///	List<AttributeDescriptor> getAttributes();
}