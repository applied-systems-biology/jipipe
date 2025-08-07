package edu.kit.datamanager.ro_crate.externalproviders.dataentities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.RoCrate.RoCrateBuilder;
import edu.kit.datamanager.ro_crate.entities.AbstractEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.OrganizationEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.externalproviders.organizationprovider.RorProvider;
import edu.kit.datamanager.ro_crate.externalproviders.personprovider.OrcidProvider;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * class used for importing from a datacite to an ROCrate.
 * The idea is to provide a datacite url and
 * all the entities relevant to it will be imported to the crate.
 */
public class ImportFromDataCite {

  private static final String PROPTERY_DATE_PUBLISHED = "datePublished";

  /**
   * Class has only static methods, therefore forbid instance creation.
   */
  private ImportFromDataCite() {}

  /**
   * This will take a DataCite entry and create a crate from it.
   * 
   * Except of the url, all parameters are optional. If present, they will be
   * added to the crate.
   *
   * @param url           the url of the dataCite entry. ex:
   *                      https://api.datacite.org/application/vnd.datacite.datacite+json/10.1594/pangaea.149669
   * @param name          the name the crate should have.
   * @param description   the description of the crate.
   * @param datePublished the published date of the crate.
   * @param licenseId     the license identifier of the crate.
   * @return the created crate.
   */
  public static Crate createCrateFromDataCiteResource(
      String url,
      Optional<String> name,
      Optional<String> description,
      Optional<String> datePublished,
      Optional<String> licenseId) {

    RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
    name.ifPresent(builder::addName);
    description.ifPresent(builder::addDescription);
    datePublished.ifPresent(builder::addDatePublishedWithExceptions);
    licenseId.ifPresent(builder::setLicense);

    Crate crate = builder.build();
    addDataCiteToCrate(url, crate);
    return crate;
  }

  /**
   * Adding a data cite entry to an existing crate.
   * All the entities from the dataCite will be added to the crate.
   *
   * @param url the url of the DataCite resource.
   * @param crate the existing crate.
   */
  public static void addDataCiteToCrate(String url, Crate crate) {
    var entities =
        getEntitiesFromDataCiteResource(Objects.requireNonNull(getJsonNodeFromDataCite(url)));
    crate.addFromCollection(entities);
  }

  /**
   * Creating a crate from a DataCite resource, this time it is provided as a Json object.
   *
   * @param json the Json object of the DataCite resource.
   * @param name the name of the crate that will be created.
   * @param description the description of the crate that will be created.
   * @param datePublished the published date of the crate.
   * @param licenseId the license identifier of the crate.
   * @return the created crate.
   */
  public static Crate createCrateFromDataCiteJson(
      JsonNode json,
      Optional<String> name,
      Optional<String> description,
      Optional<String> datePublished,
      Optional<String> licenseId) {

    RoCrateBuilder builder = new RoCrate.RoCrateBuilder();
    name.ifPresent(builder::addName);
    description.ifPresent(builder::addDescription);
    datePublished.ifPresent(builder::addDatePublishedWithExceptions);
    licenseId.ifPresent(builder::setLicense);

    Crate crate = builder.build();
    addDataCiteToCrateFromJson(json, crate);
    return crate;
  }

  public static void addDataCiteToCrateFromJson(JsonNode json, Crate crate) {
    var entities = getEntitiesFromDataCiteResource(json);
    crate.addFromCollection(entities);
  }

  private static JsonNode getJsonNodeFromDataCite(String url) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(url);
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    ObjectNode jsonNode;
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      jsonNode = objectMapper.readValue(response.getEntity().getContent(),
          ObjectNode.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return jsonNode;
  }

  private static List<AbstractEntity> getEntitiesFromDataCiteResource(JsonNode jsonNode) {

    List<AbstractEntity> entityList = new ArrayList<>();
    final ObjectMapper objectMapper = MyObjectMapper.getMapper();
    final Map<String, String> relationType = Stream.of(new String[][]{
        {"IsCitedBy", "isPartOf"},
        {"Cites", "citation"},
        {"IsSupplementTo", "isPartOf"},
        {"IsSupplementedBy", "isPartOf"},
        {"IsContinuedBy", "isPartOf"},
        {"Continues", "isBasedOn"},
        {"IsDescribedBy", "hasPart"},
        {"Describes", "isPartOf"},
        {"HasMetadata", "hasPart"},
        {"IsMetadataFor", "isPartOf"},
        {"HasVersion", "hasPart"},
        {"IsVersionOf", "isPartOf"},
        {"IsNewVersionOf", "isBasedOn"},
        {"IsPreviousVersionOf", "PredecessorOf"},
        {"IsPartOf", "isPartOf"},
        {"HasPart", "hasPart"},
        {"IsPublishedIn", "isPartOf"},
        {"IsReferencedBy", "isPartOf"},
        {"References", "citation"},
        {"IsDocumentedBy", "hasPart"},
        {"Documents", "isPartOf"},
        {"IsCompiledBy", "hasPart"},
        {"Compiles", "isPartOf"},
        {"IsVariantFormOf", "isBasedOn"},
        {"IsOriginalFormOf", "sameAs"},
        {"IsIdenticalTo", "sameAs"},
        {"IsReviewedBy", "hasPart"},
        {"Reviews", "isPartOf"},
        {"IsDerivedFrom", "isBasedOn"},
        {"IsSourceOf", "isBasedOn"},
        {"IsRequiredBy", "isPartOf"},
        {"Requires", "hasPart"},
        {"IsObsoletedBy", "hasPart"},
        {"Obsoletes", "isBasedOn"}
    }).collect(Collectors.collectingAndThen(
        Collectors.toMap(data -> data[0], data -> data[1]),
        Collections::<String, String>unmodifiableMap));

    final Collection<AbstractEntity> citation = new HashSet<>();
    final Collection<AbstractEntity> hasPart = new HashSet<>();
    final Collection<AbstractEntity> IsBasedOn = new HashSet<>();
    final Collection<AbstractEntity> IsPartOf = new HashSet<>();
    final Collection<AbstractEntity> sameAs = new HashSet<>();


    // the id of the main entity
    String id = null;
    var identifiers = jsonNode.get("identifiers");
    if (identifiers != null) {
      if (identifiers.isEmpty()) {
        id = jsonNode.get("id").asText();
      } else {
        id = identifiers.get(0).get("identifier").asText();
      }
    }

    // main entity type
    String type = "CreativeWork";
    JsonNode types = jsonNode.get("types");
    var schemaType = types.get("schemaOrg");
    if (schemaType != null) {
      type = schemaType.asText();
    }

    // the title
    String title;
    JsonNode titles = jsonNode.get("titles");
    if (titles == null || titles.isEmpty()) {
      // no title
      title = null;
    } else {
      title = titles.get(0).get("title").asText();
    }

    // publicationYear
    String publicationYear;
    JsonNode pubYear = jsonNode.get("publicationYear");
    publicationYear = pubYear != null ? pubYear.asText() : null;

    // the creators + their affiliations
    JsonNode creators = jsonNode.get("creators");
    List<AbstractEntity> creatorsList = new ArrayList<>();
    if (creators != null) {
      for (var e : creators) {
        var person = addPersonToListOfEntities(e, entityList);
        creatorsList.add(person);
      }
    }

    // the publisher
    OrganizationEntity publisher = null;
    JsonNode publisherNode = jsonNode.get("publisher");
    if (publisherNode != null && publisherNode.isTextual()) {
      publisher = new OrganizationEntity.OrganizationEntityBuilder()
          .addProperty("name", publisherNode.asText()).build();
      entityList.add(publisher);
    }

    // contributors
    JsonNode contributors = jsonNode.get("contributors");
    List<AbstractEntity> contributorsList = new ArrayList<>();
    if (contributors != null) {
      for (var e : contributors) {
        var contributor = addPersonToListOfEntities(e, entityList);
        contributorsList.add(contributor);
      }
    }

    // description
    ArrayNode descriptionArray = objectMapper.createArrayNode();
    JsonNode descriptionNode = jsonNode.get("descriptions");
    if (descriptionNode != null) {
      for (var e : descriptionNode) {
        var desc = e.get("description");
        descriptionArray.add(desc.asText());
      }
    }

    // dates
    String dateCreated = null;
    String dateModified = null;
    String datePublished = null;
    JsonNode dates = jsonNode.get("dates");
    if (dates != null) {
      for (var el : dates) {
        String dataType = el.get("dateType").asText();
        String dateString = el.get("date").asText();
        switch (dataType) {
          case "Created":
            dateCreated = dateString;
            break;
          case "Updated":
            dateModified = dateString;
            break;
          case "Available":
            datePublished = dateString;
            break;
          case "Issued":
            if (datePublished == null) {
              datePublished = dateString;
            }
            break;
          default:
            dateCreated = LocalDateTime.now().toString();
        }
      }
    }

    // subjects which are mapped to keywords
    JsonNode subjects = jsonNode.get("subjects");
    ArrayNode keyWordsArray = objectMapper.createArrayNode();
    if (subjects != null) {
      for (var sub : subjects) {
        keyWordsArray.add(sub.get("subject").asText());
      }
    }

    Consumer<DataEntity> addRelatedEntity = (entity) -> {
      var mapEntry = relationType.get(entity.getProperty("keywords").asText());
      if (mapEntry != null) {
        switch (mapEntry) {
          case "citation":
            citation.add(entity);
            break;
          case "isBasedOn":
            IsBasedOn.add(entity);
            break;
          case "isPartOf":
            IsPartOf.add(entity);
            break;
          case "sameAs":
            sameAs.add(entity);
            break;
          default:
            hasPart.add(entity);
            break;
        }
      }
    };

    // relatedIdentifiers
    JsonNode related = jsonNode.get("relatedIdentifiers");
    if (related != null) {
      for (var element : related) {
        String relatedId = element.get("relatedIdentifier").asText();
        String relatedIdType = element.get("relatedIdentifierType").asText();
        String relationTypeString = element.get("relationType").asText();
        DataEntity relatedItem = new DataEntity.DataEntityBuilder()
            .addType("CreativeWork")
            .setId(relatedId)
            .addProperty("description", relatedIdType)
            .addProperty("keywords", relationTypeString)
            .build();
        entityList.add(relatedItem);

        addRelatedEntity.accept(relatedItem);
      }
    }


    // geolocations
    JsonNode geo = jsonNode.get("geoLocations");
    Collection<AbstractEntity> geoSet = new HashSet<>();
    if (geo != null) {
      for (var geoThing : geo) {
        // geo point
        ContextualEntity geoEntity = new ContextualEntity.ContextualEntityBuilder()
            .addType("Place")
            .build();

        if (geoThing.get("geoLocationPlace") != null) {
          var placeName = geoThing.get("geoLocationPlace").asText();
          geoEntity.addProperty("name", placeName);
        }

        if (geoThing.get("geoLocationPoint") != null) {
          var point = geoThing.get("geoLocationPoint");
          var geoCoordinate = new ContextualEntity.ContextualEntityBuilder()
              .addType("GeoCoordinates")
              .addProperty("latitude", point.get("pointLatitude"))
              .addProperty("longitude", point.get("pointLongitude"))
              .build();
          entityList.add(geoCoordinate);
          geoEntity.addIdProperty("geo", geoCoordinate.getId());
        }
        String boxString = null;
        String polygonString = null;
        if (geoThing.get("geoLocationBox") != null) {
          var box = geoThing.get("geoLocationBox");
          String bottomLeftLongitude = box.get("westBoundLongitude").asText();
          String bottomLeftLatitude = box.get("southBoundLatitude").asText();

          String topRightLongitude = box.get("eastBoundLongitude").asText();
          String topRightLatitude = box.get("northBoundLatitude").asText();

          boxString = bottomLeftLongitude + "," + bottomLeftLatitude + " "
              + topRightLongitude + "," + topRightLatitude;

        }
        if (geoThing.get("geoLocationPolygon") != null) {
          StringBuilder stringBuilder = new StringBuilder();
          for (var polygonEl : geoThing.get("geoLocationPolygon")) {
            var point = polygonEl.get("polygonPoint");
            String longitude = point.get("pointLongitude").asText();
            String latitude = point.get("pointLatitude").asText();
            stringBuilder.append(longitude).append(",").append(latitude).append(" ");
          }
          polygonString = stringBuilder.toString();
        }

        if (boxString != null || polygonString != null) {
          ContextualEntity shapeEntity = new ContextualEntity.ContextualEntityBuilder()
              .addType("GeoShape")
              .addProperty("box", boxString)
              .addProperty("box", polygonString)
              .build();
          entityList.add(shapeEntity);
          geoEntity.addIdProperty("geo", shapeEntity.getId());
        }
        entityList.add(geoEntity);
        geoSet.add(geoEntity);
      }
    }

    // language
    String language = null;
    if (jsonNode.get("language") != null) {
      language = jsonNode.get("language").asText();
    }

    // alternateIdentifier
    ArrayNode alternate = objectMapper.createArrayNode();
    if (jsonNode.get("alternateIdentifiers") != null) {
      for (var el : jsonNode) {
        alternate.add(el.get("alternateIdentifier").asText());
      }
    }

    // size
    ArrayNode sizes = null;
    if (jsonNode.get("sizes") != null) {
      sizes = (ArrayNode) jsonNode.get("sizes");
    }

    // format
    ArrayNode formats = null;
    if (jsonNode.get("formats") != null) {
      formats = (ArrayNode) jsonNode.get("formats");
    }

    // rights
    Set<AbstractEntity> licenses = new HashSet<>();
    if (jsonNode.get("rightsList") != null) {
      for (var element : jsonNode.get("rightsList")) {
        String licenseName = null;
        if (element.get("rights") != null) {
          licenseName = element.get("rights").asText();
        }

        String rightsUri = null;
        if (element.get("rightsUri") != null) {
          rightsUri = element.get("rightsUri").asText();
        }

        String rightsIdentifier = null;
        if (element.get("rightsIdentifier") != null) {
          rightsIdentifier = element.get("rightsIdentifier").asText();
        }

        ContextualEntity license = new ContextualEntity.ContextualEntityBuilder()
            .addType("CreativeWork")
            .addProperty("name", licenseName)
            .setId(rightsUri)
            .addProperty("identifier", rightsIdentifier)
            .build();

        entityList.add(license);
        licenses.add(license);
      }
    }

    // funding
    String fundingNameString = "fundingReferences";
    Set<AbstractEntity> funderSet = new HashSet<>();
    Set<AbstractEntity> grandSet = new HashSet<>();
    if (jsonNode.get(fundingNameString) != null) {

      for (var el : jsonNode.get(fundingNameString)) {
        var funderId = el.get("funderIdentifierType");
        PersonEntity funder;
        if (funderId != null && funderId.asText().equals("ORCID")) {
          funder = OrcidProvider.getPerson(funderId.asText());
        } else {
          funder = new PersonEntity.PersonEntityBuilder()
              .addProperty("name", el.get("funderName"))
              .setId(el.get("funderIdentifier") == null
                  ? null : el.get("funderIdentifier").asText())
              .build();
        }
        funderSet.add(funder);
        entityList.add(funder);

        // grants
        var awardUri = el.get("awardUri");
        var awardTitle = el.get("awardTitle");
        var awardNumber = el.get("awardNumber");
        if (awardNumber != null || awardTitle != null || awardUri != null) {
          ContextualEntity grant = new ContextualEntity.ContextualEntityBuilder()
              .addType("Grant")
              .setId(awardUri == null ? null : awardUri.asText())
              .addProperty("name", awardTitle)
              .addProperty("identifier", awardNumber)
              .addIdProperty("funder", funder)
              .addIdProperty("fundedItem", id)
              .build();
          entityList.add(grant);
          grandSet.add(grant);
        }
      }
    }

    // relatedItem
    var relatedItems = jsonNode.get("relatedItems");
    if (relatedItems != null) {
      for (var relatedItem : relatedItems) {
        DataEntity entity = getFromRelatedItem(relatedItem);
        entityList.add(entity);
        addRelatedEntity.accept(entity);
      }
    }
    DataEntity dataEntity = new DataEntity.DataEntityBuilder()
        .setId(id)
        .addType(type)
        .addProperty("title", title)
        .addProperty("publicationYear", publicationYear)
        .addIdProperty("publisher", publisher)
        .addIdFromCollectionOfEntities("creator", creatorsList)
        .addIdFromCollectionOfEntities("contributor", contributorsList)
        .addIdFromCollectionOfEntities("spatialCoverage", geoSet)
        .addIdFromCollectionOfEntities("license", licenses)
        .addIdFromCollectionOfEntities("funder", funderSet)
        .addIdFromCollectionOfEntities("funding", grandSet)
        .addIdFromCollectionOfEntities("isPartOf", IsPartOf)
        .addIdFromCollectionOfEntities("citation", citation)
        .addIdFromCollectionOfEntities("hasPart", hasPart)
        .addIdFromCollectionOfEntities("isBasedOn", IsBasedOn)
        .addIdFromCollectionOfEntities("sameAs", sameAs)
        .addProperty("description", descriptionArray)
        .addProperty("dateCreated", dateCreated)
        .addProperty("dateModified", dateModified)
        .addProperty(PROPTERY_DATE_PUBLISHED, datePublished)
        .addProperty("keywords", keyWordsArray)
        .addProperty("inLanguage", language)
        .addProperty("identifier", alternate)
        .addProperty("size", sizes)
        .addProperty("encodingFormat", formats)
        .addProperty("version", jsonNode.get("version"))
        .build();

    entityList.add(dataEntity);
    return entityList;
  }

  // here one can map also all the other properties of relatedItem if necessary
  private static DataEntity getFromRelatedItem(JsonNode relatedItem) {
    var relatedId = relatedItem.get("relatedIdentifier");
    String relatedIdType = relatedItem.get("relatedIdentifierType").asText();
    String relationTypeString = relatedItem.get("relationType").asText();
    return new DataEntity.DataEntityBuilder()
        .addType("CreativeWork")
        .setId(relatedId == null ? null : relatedId.asText())
        .addProperty("description", relatedIdType)
        .addProperty("keywords", relationTypeString)
        .build();
  }

  private static AbstractEntity addPersonToListOfEntities(JsonNode personNode,
                                                          Collection<AbstractEntity> entities) {
    // if there is an Orcid provider use it to get the entity directly from orcid
    var creatorIdentifiers = personNode.get("nameIdentifiers");
    PersonEntity person = null;
    String personUrl = null;
    if (creatorIdentifiers != null && !creatorIdentifiers.isEmpty()) {
      for (var identifier : creatorIdentifiers) {
        if (identifier.get("nameIdentifierScheme").asText().equals("ORCID")) {
          person = OrcidProvider.getPerson(identifier.get("nameIdentifier").asText());
        } else {
          personUrl = identifier.get("nameIdentifier").asText();
        }
      }
    }

    if (person == null) {
      person = new PersonEntity.PersonEntityBuilder()
          .addProperty("name", personNode.get("creatorName"))
          .addProperty("givenName", personNode.get("givenName"))
          .addProperty("familyName", personNode.get("familyName"))
          .addProperty("url", personUrl)
          .build();
    }

    // get the person affiliations
    Set<OrganizationEntity> organizationEntitySet = new HashSet<>();

    var affiliationArray = personNode.get("affiliation");
    if (affiliationArray != null) {
      for (var affiliationElement : affiliationArray) {
        var affiliationName = affiliationElement.get("name");
        var affiliationId = affiliationElement.get("affiliationIdentifier");
        var scheme = affiliationElement.get("affiliationIdentifierScheme");
        // if the scheme is ROR we can get the entity from there.
        OrganizationEntity organization;
        if (scheme != null && scheme.asText().equals("ROR")) {
          organization = RorProvider.getOrganization(affiliationId.asText());
        } else {
          organization = new OrganizationEntity.OrganizationEntityBuilder()
              .setId(affiliationId.asText())
              .addProperty("name", affiliationName)
              .build();
        }
        organizationEntitySet.add(organization);
      }
    }
    person.addIdListProperties("affiliation",
        organizationEntitySet.stream().map(AbstractEntity::getId).collect(Collectors.toList()));
    entities.addAll(organizationEntitySet);
    entities.add(person);
    return person;
  }
}