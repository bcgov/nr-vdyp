package ca.bc.gov.nrs.vdyp.backend.v1;

import javax.ws.rs.container.ContainerRequestContext;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.glassfish.jersey.internal.OsgiRegistry;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.osgi.framework.SynchronousBundleListener;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
		targets = { XmlAdapter.class, //
				ContainerRequestContext.class, //
				OsgiRegistry.class, //
				SynchronousBundleListener.class, //
				InstanceBinding.class, //
				LocalizationMessages.class, //
		}
)
public class VdypBackendReflectionConfiguration {

}
