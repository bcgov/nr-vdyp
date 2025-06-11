package ca.bc.gov.nrs.vdyp.backend.v1;

import javax.ws.rs.container.ContainerRequestContext;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.glassfish.jersey.internal.OsgiRegistry;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.osgi.framework.SynchronousBundleListener;

import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToDefault;
import com.opencsv.bean.processor.ConvertEmptyOrBlankStringsToNull;

import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.HelpEndpoint;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ProjectionEndpoint;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.RootEndpoint;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.HelpResource;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.ProjectionResource;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.RootResource;
import ca.bc.gov.nrs.vdyp.backend.responses.v1.VdypApiResource;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.FilterParameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessagesInner;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ParameterDetailsMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationParameter;
import ca.bc.gov.nrs.vdyp.ecore.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonRecordBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.CSVYieldTableRowValuesBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.DCSVYieldTableRecordBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.PLOTSYYieldTableRecordBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.TextYieldTableRowValuesBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.UnsupportedYieldTableRecordBean;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
		targets = { XmlAdapter.class, //
				ContainerRequestContext.class, //
				OsgiRegistry.class, //
				SynchronousBundleListener.class, //
				InstanceBinding.class, //
				LocalizationMessages.class, //
				ConvertEmptyOrBlankStringsToNull.class, //
				ConvertEmptyOrBlankStringsToDefault.class, //
				VdypApiResource.class, //
				HelpEndpoint.class, //
				ProjectionEndpoint.class, //
				RootEndpoint.class, //
				FilterParameters.class, //
				MessagesInner.class, //
				ParameterDetailsMessage.class, //
				Parameters.class, //
				ProgressFrequency.class, //
				UtilizationParameter.class, //
				ValidatedParameters.class, //
				HcsvPolygonRecordBean.class, //
				CSVYieldTableRowValuesBean.class, //
				DCSVYieldTableRecordBean.class, //
				PLOTSYYieldTableRecordBean.class, //
				TextYieldTableRowValuesBean.class, //
				UnsupportedYieldTableRecordBean.class, //
				HelpResource.class, //
				ProjectionResource.class, //
				RootResource.class //
		}
)
public class VdypBackendReflectionConfiguration {

}
