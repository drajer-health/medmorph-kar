package org.sitenv.spring;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.sitenv.spring.configuration.AppConfig;
import org.sitenv.spring.model.DafBundle;
import org.sitenv.spring.model.DafCommunication;
import org.sitenv.spring.model.DafPlanDefinition;
import org.sitenv.spring.service.BundleService;
import org.sitenv.spring.service.CommunicationService;
import org.sitenv.spring.service.PlanDefinitionService;
import org.sitenv.spring.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class MessageHeaderResourceProvider {

	private static final String validatorEndpoint = "http://ecr.drajer.com/ix-fhir-validator/r4/resource/validate";
	
	protected FhirContext r4Context = FhirContext.forR4();

	private static final Logger logger = LoggerFactory.getLogger(MessageHeaderResourceProvider.class);
	
	AbstractApplicationContext context;
	PlanDefinitionService planDefinition;
	BundleService bundleService;
	CommunicationService communicationService;
	
	public MessageHeaderResourceProvider() {
		context = new AnnotationConfigApplicationContext(AppConfig.class);
		planDefinition = (PlanDefinitionService) context.getBean("PlanDefinitionService");
		bundleService = (BundleService) context.getBean("BundleService");
		communicationService = (CommunicationService) context.getBean("CommunicationService");
	}
	
	@Operation(name = "$process-message", idempotent = false)
	public Bundle processMessage(HttpServletRequest theServletRequest, RequestDetails theRequestDetails,
			@OperationParam(name = "content", min = 1, max = 1) @Description(formalDefinition = "The message to process (or, if using asynchronous messaging, it may be a response message to accept)") Bundle theMessageToProcess) {
		logger.info("Validating the Bundle");
		Bundle bundle1 = theMessageToProcess;
		OperationOutcome outcome = new OperationOutcome();
		boolean errorExists = false;
		try {
			outcome = new CommonUtil().validateResource(bundle1,validatorEndpoint, r4Context);
			if (outcome.hasIssue()) {
				List<OperationOutcomeIssueComponent> issueCompList = outcome.getIssue();
				for (OperationOutcomeIssueComponent issueComp : issueCompList) {
					if (issueComp.getSeverity().equals(IssueSeverity.ERROR)) {
						errorExists = true;
					}
				}
			}
			if (!errorExists) {
				DafBundle dafBundle = new DafBundle();
				bundle1.setId(getUUID());
				dafBundle.setData(r4Context.newJsonParser().encodeResourceToString(bundle1));
				dafBundle.setTimestamp(new Date());
				bundleService.createBundle(dafBundle);
				MessageHeader messageHeader = null;
				String patientId = null;
				String commId =null;
				for(BundleEntryComponent entryComp: bundle1.getEntry()) {
					if(entryComp.getResource().getResourceType().name().equals("MessageHeader")) {
						messageHeader = (MessageHeader) entryComp.getResource();
						messageHeader.setId(getUUID());
						Meta meta = messageHeader.getMeta();
						meta.setLastUpdated(new Date());
						messageHeader.setMeta(meta);
					} else if(entryComp.getResource().getResourceType().name().equals("Bundle")) {
						Bundle innerBundle = (Bundle) entryComp.getResource();
						for(BundleEntryComponent bundleEntryComponent:innerBundle.getEntry()) {
							if(bundleEntryComponent.getResource().getResourceType().name().equals("Patient")) {
								patientId = bundleEntryComponent.getResource().getIdElement().getIdPart().toString();
							}
						}
					}
				}
				if(patientId!= null) {
					commId = constructAndSaveCommunication(patientId);	
				}
				if(messageHeader == null) {
					messageHeader = constructMessageHeaderResource();	
				}
				if(commId != null) {
					List<Reference> referenceList = new ArrayList<Reference>();
					Reference commRef = new Reference();
					commRef.setReference("Communication/"+commId);
					referenceList.add(commRef);
					messageHeader.setFocus(referenceList);	
				}
				
				Bundle respbundle = new Bundle();
				respbundle.setId(getUUID());
				List<BundleEntryComponent> entryCompList = new ArrayList<>();
				BundleEntryComponent entryComp = new BundleEntryComponent();
				entryComp.setResource(messageHeader);
				entryCompList.add(entryComp);
				respbundle.setEntry(entryCompList);
				return respbundle;
			} else {
				Bundle responseBundle = new Bundle();
				List<BundleEntryComponent> bundleEntryList = new ArrayList<>();
				BundleEntryComponent entryComp = new BundleEntryComponent();
				entryComp.setResource(outcome);
				bundleEntryList.add(entryComp);
				responseBundle.setEntry(bundleEntryList);
				return responseBundle;
			}
		} catch (Exception e) {
			throw new UnprocessableEntityException("Error in Processing the Bundle");
		}
	}

	private MessageHeader constructMessageHeaderResource() {
		String message = "{\"resourceType\": \"MessageHeader\",\"id\": \"messageheader-example-reportheader\",\"meta\": {\"versionId\": \"1\",\"lastUpdated\": \"2020-11-29T02:03:28.045+00:00\",\"profile\": [\"http://hl7.org/fhir/us/medmorph/StructureDefinition/us-ph-messageheader\"]},\"extension\": [{\"url\": \"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-dataEncrypted\",\"valueBoolean\": false},{\"url\":\"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-messageProcessingCategory\",\"valueCode\": \"consequence\"}],\"eventCoding\": {\"system\": \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\": \"cancer-report-message\"},\"destination\": [{\"name\": \"PHA endpoint\",\"endpoint\": \"http://example.pha.org/fhir\"}],\"source\": {\"name\": \"Healthcare Organization\",\"software\": \"Backend Service App\",\"version\": \"3.1.45.AABB\",\"contact\": {\"system\": \"phone\",\"value\": \"+1 (917) 123 4567\"},\"endpoint\": \"http://example.healthcare.org/fhir\"},\"reason\": {\"coding\": [{\"system\": \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-triggerdefinition-namedevents\",\"code\": \"encounter-close\"}]}}";
		MessageHeader messageHeader = (MessageHeader) r4Context.newJsonParser().parseResource(message);
		messageHeader.setId(getUUID());
		return messageHeader;
	}
	
	private String constructAndSaveCommunication(String patientId) {
		String communication ="{\"resourceType\" : \"Communication\",\"meta\" : {\"versionId\" : \"1\",\"profile\" : [\"http://hl7.org/fhir/us/medmorph/StructureDefinition/us-ph-communication\"]},\"extension\" : [{\"url\" : \"http://hl7.org/fhir/us/medmorph/StructureDefinition/ext-responseMessageStatus\",\"valueCodeableConcept\" : {\"coding\" : [{\"system\" :\"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-response-message-processing-status\",\"code\" : \"RRVS1\"}]}}],\"identifier\" : [{\"system\" : \"http://example.pha.org/\",\"value\" : \"12345\"}],\"status\" : \"completed\",\"category\" : [{\"coding\" : [{\"system\" : \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\" : \"cancer-response-message\"}]}],\"reasonCode\" : [{\"coding\" : [{\"system\" : \"http://hl7.org/fhir/us/medmorph/CodeSystem/us-ph-messageheader-message-types\",\"code\" : \"cancer-report-message\"}]}]}";
		Communication comm = (Communication) r4Context.newJsonParser().parseResource(communication);
		String commId = getUUID();
		comm.setId(commId);
		Meta meta = comm.getMeta();
		meta.setLastUpdated(new Date());
		comm.setMeta(meta);
		comm.setSubject(new Reference("Patient/"+patientId));
		DafCommunication dafCommunication = new DafCommunication();
		dafCommunication.setData(r4Context.newJsonParser().encodeResourceToString(comm));
		dafCommunication.setTimestamp(new Date());
		communicationService.createCommunication(dafCommunication);
		return commId;
	}

	public String getUUID() {
	    UUID uuid = UUID.randomUUID();
	    String randomUUID = uuid.toString();
	    return randomUUID;
	  }

}
