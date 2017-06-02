package org.dcm4chee.xds.infoset.v30.ws;

import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.1-b03-
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "DocumentRepository_Service", targetNamespace = "urn:ihe:iti:xds-b:2007", wsdlLocation = "file:/E:/xdsb-schema/wsdl/XDS.b_DocumentRepository.wsdl")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public class DocumentRepositoryService
    extends Service
{
	private static Logger log = LoggerFactory.getLogger(DocumentRepositoryService.class);
	
	private final static URL DOCUMENTREPOSITORYSERVICE_WSDL_LOCATION;

    static {
        DOCUMENTREPOSITORYSERVICE_WSDL_LOCATION = DocumentRegistryService.class.getResource("/wsdl/repository.wsdl");
    }

    public DocumentRepositoryService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public DocumentRepositoryService() {
        super(DOCUMENTREPOSITORYSERVICE_WSDL_LOCATION, new QName("urn:ihe:iti:xds-b:2007", "DocumentRepository_Service"));
    }

    /**
     * 
     * @return
     *     returns DocumentRepositoryPortType
     */
    @WebEndpoint(name = "DocumentRepository_Port_Soap11")
    public DocumentRepositoryPortType getDocumentRepositoryPortSoap11() {
        return (DocumentRepositoryPortType)super.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRepository_Port_Soap11"), DocumentRepositoryPortType.class);
    }

    /**
     * 
     * @return
     *     returns DocumentRepositoryPortType
     */
    @WebEndpoint(name = "DocumentRepository_Port_Soap12")
    public DocumentRepositoryPortType getDocumentRepositoryPortSoap12() {
        return (DocumentRepositoryPortType)super.getPort(new QName("urn:ihe:iti:xds-b:2007", "DocumentRepository_Port_Soap12"), DocumentRepositoryPortType.class);
    }
}
