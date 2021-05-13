package org.sitenv.spring;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;

import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.sitenv.spring.configuration.AppConfig;
import org.sitenv.spring.model.DafBundle;
import org.sitenv.spring.service.BundleService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class BundleResourceProvider implements IResourceProvider {
  public static final String RESOURCE_TYPE = "Bundle";
  
  private static final FhirContext fhirContext = FhirContext.forR4();
  
  AbstractApplicationContext context;
  BundleService service;
  
  public BundleResourceProvider() {
      context = new AnnotationConfigApplicationContext(AppConfig.class);
      service= (BundleService) context.getBean("BundleService");
  }
  
  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Bundle.class;
  }
  
  @Operation(name = "$process-message")
  public Bundle processMessageOperation(@ResourceParam Bundle bundle) {
    System.out.println("Process Bundle Message");
    Bundle retVal = new Bundle();
    return retVal;
  }
  
  @Create
	public MethodOutcome createGroup(@ResourceParam Bundle bundle) {
		String uuid = getUUID();
		bundle.setId(uuid);
		Meta meta = new Meta();
		meta.setLastUpdated(new Date());
		meta.setVersionId("1");
		bundle.setMeta(meta);
		DafBundle dafBundle = new DafBundle();
		dafBundle.setData(fhirContext.newJsonParser().encodeResourceToString(bundle));
		dafBundle.setTimestamp(new Date());
		this.service.createBundle(dafBundle);
		MethodOutcome retVal = new MethodOutcome();
		retVal.setId((IIdType) new IdType("Bundle", uuid, "1"));
		return retVal;
	}
  
  public String getUUID() {
		UUID uuid = UUID.randomUUID();
		String randomUUID = uuid.toString();
		return randomUUID;
	}
}
