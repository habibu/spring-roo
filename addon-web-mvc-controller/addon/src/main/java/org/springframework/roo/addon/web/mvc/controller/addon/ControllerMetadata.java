package org.springframework.roo.addon.web.mvc.controller.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Metadata for {@link RooController}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class ControllerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(ControllerMetadata.class);

  private static final String PROVIDES_TYPE_STRING = ControllerMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final JavaType service;
  private final ControllerType type;
  private final JavaType entity;
  private final JavaType identifierType;
  private final ServiceMetadata serviceMetadata;
  private final Map<JavaType, FieldMetadata> detailsServiceField;
  private final Map<JavaType, ServiceMetadata> detailsServiceMetadata;
  private final String path;
  private final String requestMappingValue;
  private final FieldMetadata identifierField;
  private final JpaEntityMetadata entityMetadata;
  private final List<RelationInfo> detailsFieldInfo;
  private final FieldMetadata serviceField;
  private final FieldMetadata lastDetailsServiceField;
  private final JavaType lastDetailsService;
  private final JavaType lastDetailsEntity;
  private final RelationInfo lastDetailsInfo;


  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Constructor
   *
   * @param identifier
   *            the identifier for this item of metadata (required)
   * @param aspectName
   *            the Java type of the ITD (required)
   * @param controllerValues
   * @param governorPhysicalTypeMetadata
   *            the governor, which is expected to contain a
   *            {@link ClassOrInterfaceTypeDetails} (required)
   * @param entity
   *            Javatype with entity managed by controller
   * @param service
   *            JavaType with service used by controller
   * @param detailsServices
   *            List with all services of every field that will be used as
   *            detail
   * @param path
   *            controllerPath
   * @param type
   *            Indicate the controller type
   * @param identifierType
   *            Indicates the identifier type of the entity which represents
   *            this controller
   * @param serviceMetadata
   *            ServiceMetadata of the service used by controller
   * @param controllerDetailInfo
   *            Contains information relative to detail controller
   */
  public ControllerMetadata(final String identifier, final JavaType aspectName,
      ControllerAnnotationValues controllerValues,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JpaEntityMetadata entityMetadata, final JavaType service, final String path,
      final ControllerType type, ServiceMetadata serviceMetadata,
      Map<JavaType, ServiceMetadata> detailsServiceMetadata, List<RelationInfo> detailsFieldInfo) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.type = type;
    this.service = service;
    this.serviceMetadata = serviceMetadata;
    this.detailsServiceMetadata = detailsServiceMetadata;
    this.entity = entity;
    this.entityMetadata = entityMetadata;
    this.identifierField = entityMetadata.getCurrentIndentifierField();
    this.identifierType = identifierField.getFieldType();
    this.path = path;
    if (detailsFieldInfo == null) {
      this.detailsFieldInfo = null;
    } else {
      this.detailsFieldInfo = Collections.unmodifiableList(detailsFieldInfo);
    }

    switch (this.type) {
      case ITEM:
        this.requestMappingValue =
            StringUtils.lowerCase(path).concat(
                "/{" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "}");
        break;
      case COLLECTION:
        this.requestMappingValue = StringUtils.lowerCase(path);
        break;
      case SEARCH:
        this.requestMappingValue = StringUtils.lowerCase(path).concat("/search");
        break;
      case DETAIL:
        Validate.isTrue(detailsFieldInfo != null & detailsFieldInfo.size() > 0,
            "Missing details information for %s", governorPhysicalTypeMetadata.getType());
        StringBuilder sbuilder = new StringBuilder(path);
        JavaType curEntity = entity;
        for (RelationInfo info : detailsFieldInfo) {
          sbuilder.append("/{" + StringUtils.uncapitalize(curEntity.getSimpleTypeName()) + "}/"
              + info.fieldName);
          curEntity = info.childType;
        }
        this.requestMappingValue = sbuilder.toString();
        break;
      default:
        throw new IllegalArgumentException(String.format("Unsupported @%s.type '%s' on %s",
            RooJavaType.ROO_CONTROLLER, this.type.name(), governorPhysicalTypeMetadata.getType()));
    }

    // Adding service field
    this.serviceField = getFieldFor(this.service);
    ensureGovernorHasField(new FieldMetadataBuilder(serviceField));

    Map<JavaType, FieldMetadata> detailServiceFields = new TreeMap<JavaType, FieldMetadata>();
    FieldMetadata curServiceField;
    if (this.type == ControllerType.DETAIL) {
      // Adding service field
      for (Entry<JavaType, ServiceMetadata> entry : detailsServiceMetadata.entrySet()) {
        curServiceField = getFieldFor(entry.getValue().getDestination());
        ensureGovernorHasField(new FieldMetadataBuilder(curServiceField));
        detailServiceFields.put(entry.getKey(), curServiceField);
      }
      this.detailsServiceField = Collections.unmodifiableMap(detailServiceFields);
      this.lastDetailsInfo = detailsFieldInfo.get(detailServiceFields.size() - 1);
      this.lastDetailsEntity = lastDetailsInfo.childType;
      this.lastDetailsService = detailServiceFields.get(lastDetailsEntity).getFieldType();
      this.lastDetailsServiceField = detailServiceFields.get(lastDetailsService);
    } else {
      this.lastDetailsInfo = null;
      this.lastDetailsEntity = null;
      this.lastDetailsService = null;
      this.lastDetailsServiceField = null;
      this.detailsServiceField = null;
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns service field included on controller that it
   * represents the service spent as parameter
   *
   * @param service
   *            Searched service
   * @return The field that represents the service spent as parameter
   */
  private FieldMetadata getFieldFor(JavaType service) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(service.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), service).build();
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }

  public JavaType getEntity() {
    return this.entity;
  }

  public ControllerType getType() {
    return this.type;
  }

  public JavaType getService() {
    return this.service;
  }

  public String getPath() {
    return this.path;
  }

  public FieldMetadata getIdentifierField() {
    return identifierField;
  }

  public JavaType getIdentifierType() {
    return identifierType;
  }

  public List<RelationInfo> getDetailsFieldInfo() {
    return detailsFieldInfo;
  }

  public String getRequestMappingValue() {
    return requestMappingValue;
  }

  public Map<JavaType, FieldMetadata> getDetailsServiceFields() {
    return detailsServiceField;
  }

  public FieldMetadata getDetailsServiceFields(JavaType entityType) {
    if (entity.equals(entityType)) {
      return serviceField;
    }
    return getDetailsServiceFields().get(entityType);
  }

  public RelationInfo getLastDetailsInfo() {
    return lastDetailsInfo;
  }

  public FieldMetadata getLastDetailServiceField() {
    return lastDetailsServiceField;
  }

  public JavaType getLastDetailService() {
    return lastDetailsService;
  }

  public JavaType getLastDetailEntity() {
    return lastDetailsEntity;
  }

  public FieldMetadata getServiceField() {
    return serviceField;
  }

  public ServiceMetadata getSericeMetadataForEntity(JavaType entityType) {
    if (entity.equals(entityType)) {
      return serviceMetadata;
    }
    return detailsServiceMetadata.get(entityType);
  }
}
