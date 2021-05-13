package org.sitenv.spring;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Group;
import org.sitenv.spring.configuration.AppConfig;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.model.DafGroup;
import org.sitenv.spring.service.GroupService;
import org.sitenv.spring.util.SearchParameterMap;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class GroupResourceProvider implements IResourceProvider {
  private static final FhirContext fhirContext = FhirContext.forR4();
  
  
  AbstractApplicationContext context;
  GroupService service; 
  public GroupResourceProvider() {
	  context = new AnnotationConfigApplicationContext(AppConfig.class);
	  service= (GroupService) context.getBean("GroupService");
  }
  
  public static final String RESOURCE_TYPE = "Group";
  
  public static final String VERSION_ID = "1.0";
  
  public Class<? extends IBaseResource> getResourceType() {
    return Group.class;
  }
  
  @Read
	public Group readOrVread(@IdParam IdType theId) {
		String id;
		try {
			id = theId.getIdPart();
		} catch (NumberFormatException e) {
			throw new ResourceNotFoundException(theId);
		}
		DafGroup dafGroup = this.service.getGroupById(id);
		return (Group) fhirContext.newJsonParser().parseResource(dafGroup.getData());
	}

	@Create
	public MethodOutcome createGroup(@ResourceParam Group Group) {
		String uuid = getUUID();
		Group.setId(uuid);
		Meta meta = new Meta();
		meta.setLastUpdated(new Date());
		meta.setVersionId("1");
		Group.setMeta(meta);
		DafGroup dafGroup = new DafGroup();
		dafGroup.setData(fhirContext.newJsonParser().encodeResourceToString((IBaseResource) Group));
		dafGroup.setTimestamp(new Date());
		this.service.createGroups(dafGroup);
		MethodOutcome retVal = new MethodOutcome();
		retVal.setId((IIdType) new IdType("Group", uuid, "1"));
		return retVal;
	}
  
  @Search
  public IBundleProvider search(HttpServletRequest theServletRequest, @Description(shortDefinition = "The resource identity") @OptionalParam(name = "_id") StringAndListParam theId, @Description(shortDefinition = "A Group identifier") @OptionalParam(name = "identifier") TokenAndListParam theIdentifier, @Description(shortDefinition = "Label for Group") @OptionalParam(name = "name") StringParam theName, @IncludeParam(reverse = true, allow = {"*"}) Set<Include> theRevIncludes, @IncludeParam(allow = {"*"}) Set<Include> theIncludes, @Sort SortSpec theSort, @Count Integer theCount) {
    SearchParameterMap paramMap = new SearchParameterMap();
    paramMap.add(Group.SP_RES_ID, theId);
    paramMap.add(Group.SP_IDENTIFIER, theIdentifier);
    paramMap.add("name", theName);
    paramMap.setIncludes(theIncludes);
    paramMap.setSort(theSort);
    paramMap.setCount(theCount);
    final List<DafGroup> results = this.service.search(paramMap);
    return new IBundleProvider() {
        final InstantDt published = InstantDt.withCurrentTime();
        
        public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
          List<IBaseResource> planDefinitionList = new ArrayList<>();
          List<String> ids = new ArrayList<>();
          for (DafGroup DafGroup : results) {
            Group Group = (Group)GroupResourceProvider.fhirContext.newJsonParser().parseResource(DafGroup.getData());
            planDefinitionList.add(Group);
            ids.add(Group.getIdElement().getResourceType() + "/" + Group
                .getIdElement().getIdPart());
          } 
          return planDefinitionList;
        }
        
        public Integer size() {
          return Integer.valueOf(results.size());
        }
        
        public InstantDt getPublished() {
          return this.published;
        }
        
        public Integer preferredPageSize() {
          return null;
        }
        
        public String getUuid() {
          return null;
        }
      };
  }
  
  public String getUUID() {
		UUID uuid = UUID.randomUUID();
		String randomUUID = uuid.toString();
		return randomUUID;
	}
}
