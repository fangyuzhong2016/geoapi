package org.opengis.feature.type;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.opengis.feature.AttributeName;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Factory interface for the typing system.
 * 
 * @author Gabriel Roldan, Axios Engineering 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public interface TypeFactory {

	/**
	 * Creates a new attribute type.
	 * 
	 * @param name The name of the type.
	 * @param binding The class with which the type is bound to.
	 * @param isIdentifiable Flag indicating wether type is identifiable.
	 * @param isNillable Flag indicating wether type is nillable.
	 * @param isAbstract Flag indicating wether type is abstract.
	 * @param restrictions Additional restrictions to be placed on attributes 
	 * of the type. Specified as instances of {@link Filter}.
	 * @param superType The super type of this type.
	 */
	AttributeType createAttributeType(
		AttributeName name, Class binding, boolean isIdentifiable, 
		boolean isNillable, boolean isAbstract, Set restrictions, 
		AttributeType superType
	);
	
	/**
	 * Creates a new complex type.
	 * 
	 * @param name The name of the type.
	 * @param schema The collection of attributes that make up the complex type.
	 * @param isIdentifiable Flag indicating wether type is identifiable.
	 * @param isNillable Flag indicating wether type is nillable.
	 * @param isAbstract Flag indicating wether type is abstract.
	 * @param restrictions Additional restrictions to be placed on attributes 
	 * of the type. Specified as instances of {@link Filter}.
	 * @param superType The super type of this type,may be null.
	 */
	ComplexType createComplexType(
		AttributeName name, Collection<AttributeDescriptor> schema, 
		boolean isIdentifiable, boolean isNillable, boolean isAbstract, 
		Set restrictions, ComplexType superType
	);
	
	/**
	 * Creates a new geometry type.
	 * 
	 * @param name The name of the type.
	 * @param binding The class with which the type is bound to.
	 * @param isIdentifiable Flag indicating wether type is identifiable.
	 * @param isNillable Flag indicating wether type is nillable.
	 * @param isAbstract Flag indicating wether type is abstract.
	 * @param crs The coordinate reference system of the type.
	 * @param restrictions Additional restrictions to be placed on attributes 
	 * of the type. Specified as instances of {@link Filter}.
	 * @param superType The parent type, may be null.
	 
	 */
	GeometryType createGeometryType(
		AttributeName name, Class binding, boolean isIdentifiable, 
		boolean isNillable, boolean isAbstract, CoordinateReferenceSystem crs, 
		Set restrictions, AttributeType superType
	);

	/**
	 * Creates a new feature type.
	 *
	 * @param name The name of the type.
	 * @param schema The collection of attributes that make up the type. 
	 * @param defaultGeometry The default geometry of the type.
	 * @param isAbstract Flag indicating wether type is abstract.
	 * @param restrictions Additional restrictions to be placed on attributes 
	 * of the type. Specified as instances of {@link Filter}.
	 * @param superType The parent type, may be null.
	 */
	FeatureType createFeatureType(
		AttributeName name, Collection<AttributeDescriptor> schema,
		GeometryType defaultGeometry, boolean isAbstract, Set restrictions, 
		FeatureType superType
	);

	/**
	 * Creates a new simple feature type.
	 *
	 * @param name The name of the type.
	 * @param schema The ordered list of attribute types making up this type.
	 * @param defaultGeometry The default geometry type.
	 * @param isAbstract Flag indicating wether type is abstract.
	 * @param restrictions Additional restrictions to be placed on attributes 
	 * of the type. Specified as instances of {@link Filter}.
	 * @param superType The parent type, may be null.
	 */
	SimpleFeatureType createSimpleFeatureType(
		AttributeName name,List<AttributeDescriptor> schema,
		GeometryType defaultGeometry, boolean isAbstract, Set restrictions, 
		SimpleFeatureType superType
	);
	
	/**
	 * Creates a new feature collection type.
	 * 
	 * @param name The name of the type.
	 * @param membersTypes The set of feature types in which members of the
	 * collection may implement.
	 * @param schema The collection of attribute types making up this type.
	 * @param defaultGeom The default geometry attribute of the type.
	 * @param restrictions Additional restrictions to be placed on attributes 
	 * of the type. Specified as instances of {@link Filter}.
	 * @param superType The parent type, may be null.
	 * @param isAbstract Flag indicating wether type is abstract.
	 */
	FeatureCollectionType createFeatureCollectionType(
		AttributeName name, Set<AttributeDescriptor> membersTypes, 
		Collection<AttributeDescriptor> schema, GeometryType defaultGeom, 
		Set<Filter> restrictions, FeatureType superType, 
		boolean isAbstract
	);

}