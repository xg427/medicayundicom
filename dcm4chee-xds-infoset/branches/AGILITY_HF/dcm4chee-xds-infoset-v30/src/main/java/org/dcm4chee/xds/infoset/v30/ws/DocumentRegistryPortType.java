package org.dcm4chee.xds.infoset.v30.ws;

import static javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_MTOM_BINDING;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.BindingType;
import javax.xml.ws.addressing.Action;

import org.dcm4chee.xds.infoset.v30.AdhocQueryRequest;
import org.dcm4chee.xds.infoset.v30.AdhocQueryResponse;
import org.dcm4chee.xds.infoset.v30.ObjectFactory;
import org.dcm4chee.xds.infoset.v30.RegistryResponseType;
import org.dcm4chee.xds.infoset.v30.SubmitObjectsRequest;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.1-b03-
 * Generated source version: 2.1
 * 
 */
@WebService(name = "DocumentRegistry_PortType", targetNamespace = "urn:ihe:iti:xds-b:2007")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@BindingType(value = SOAP12HTTP_MTOM_BINDING)
@XmlSeeAlso({
    ObjectFactory.class
})
public interface DocumentRegistryPortType {


    /**
     * 
     * @param body
     * @return
     *     returns org.dcm4chee.xds.infoset.v30.RegistryResponseType
     */
    @WebMethod(operationName = "DocumentRegistry_RegisterDocumentSet-b", action = "urn:ihe:iti:2007:RegisterDocumentSet-b")
    @WebResult(name = "RegistryResponse", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0", partName = "body")
    @Action(input = "urn:ihe:iti:2007:RegisterDocumentSet-b", output = "urn:ihe:iti:2007:RegisterDocumentSet-bResponse")
    public RegistryResponseType documentRegistryRegisterDocumentSetB(
        @WebParam(name = "SubmitObjectsRequest", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:lcm:3.0", partName = "body")
        SubmitObjectsRequest body);

    /**
     * 
     * @param body
     * @return
     *     returns org.dcm4chee.xds.infoset.v30.AdhocQueryResponse
     */
    @WebMethod(operationName = "DocumentRegistry_RegistryStoredQuery", action = "urn:ihe:iti:2007:RegistryStoredQuery")
    @WebResult(name = "AdhocQueryResponse", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0", partName = "body")
    public AdhocQueryResponse documentRegistryRegistryStoredQuery(
        @WebParam(name = "AdhocQueryRequest", targetNamespace = "urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0", partName = "body")
        AdhocQueryRequest body);
}
