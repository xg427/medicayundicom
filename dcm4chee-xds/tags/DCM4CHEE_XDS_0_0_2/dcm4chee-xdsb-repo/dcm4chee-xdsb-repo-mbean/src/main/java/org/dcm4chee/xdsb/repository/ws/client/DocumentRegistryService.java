
package org.dcm4chee.xdsb.repository.ws.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

import org.dcm4chee.xds.common.ws.DocumentRegistryPortType;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.1-b03-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "DocumentRegistry_Service", targetNamespace = "urn:ihe:iti:xds-b:2007")
public class DocumentRegistryService
    extends Service
{

    private final static URL DOCUMENTREGISTRYSERVICE_WSDL_LOCATION;

    static {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/dcm4chee-xdsb-registry?wsdl");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        DOCUMENTREGISTRYSERVICE_WSDL_LOCATION = url;
    }

    public DocumentRegistryService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public DocumentRegistryService() {
        super(null, new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Service"));
    }

    /**
     * 
     * @return
     *     returns DocumentRegistryPortType
     */
    @WebEndpoint(name = "DocumentRegistry_Port_Soap11")
    public DocumentRegistryPortType getDocumentRegistryPortSoap11() {
        return (DocumentRegistryPortType)super.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Port_Soap11"), DocumentRegistryPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns DocumentRegistryPortType
     */
    @WebEndpoint(name = "DocumentRegistry_Port_Soap11")
    public DocumentRegistryPortType getDocumentRegistryPortSoap11(WebServiceFeature... features) {
        return (DocumentRegistryPortType)super.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Port_Soap11"), DocumentRegistryPortType.class, features);
    }

    /**
     * 
     * @return
     *     returns DocumentRegistryPortType
     */
    @WebEndpoint(name = "DocumentRegistry_Port_Soap12")
    public DocumentRegistryPortType getDocumentRegistryPortSoap12() {
        return (DocumentRegistryPortType)super.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Port_Soap12"), DocumentRegistryPortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns DocumentRegistryPortType
     */
    @WebEndpoint(name = "DocumentRegistry_Port_Soap12")
    public DocumentRegistryPortType getDocumentRegistryPortSoap12(WebServiceFeature... features) {
        return (DocumentRegistryPortType)super.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRegistry_Port_Soap12"), DocumentRegistryPortType.class, features);
    }
    
}
